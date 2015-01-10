package mrwolf.dbimport.executors;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import mrwolf.dbimport.export.AuctionHouseExportRecord;
import mrwolf.dbimport.model.AuctionRecord;
import mrwolf.dbimport.model.Faction;
import mrwolf.dbimport.persistence.AuctionHouseExportFileRepository;
import mrwolf.dbimport.persistence.AuctionRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Accessors(fluent = true)
public class AuctionProcessDispatcher {

  private static final int LOG_DELAY = 1000; // 1 second
  private static final long INCOMING_THRESHHOLD = 50000; // restrict to control the memory footprint of the application
  private static final long PERSIST_THRESHHOLD = 50000; // restrict to control the memory footprint of the application

  @NonNull
  private final String directory;
  private final int persistorBatchSize; // depends on speed of target database
  private final Map<Integer, List<AuctionHouseExportRecord>> incomingById;
  private final Queue<AuctionRecord> persist;
  private final Queue<Exception> errors;
  private long incomingCounter;

  @Autowired
  @Setter
  @Getter
  private AuctionRecordRepository auctionRepository;

  @Autowired
  @Setter
  @Getter
  private AuctionHouseExportFileRepository fileRepository;

  private final FileReader fileReader;
  private final List<AuctionStatisticCollector> collectors;

  public AuctionProcessDispatcher(String directory, int persistorBatchSize) {
    this.directory = directory;
    this.persistorBatchSize = persistorBatchSize;
    this.incomingById = new HashMap<>();
    this.persist = new LinkedList<>();
    this.errors = new LinkedList<>();
    fileReader = new FileReader(this, directory);
    collectors = new ArrayList<>();
  }

  public void start() {
    int nThreads = Runtime.getRuntime().availableProcessors();
    if (nThreads < 4) {
      log.error("Dispatcher can only run on a multi processor system.");
      return;
    }

    log.info("Start processing of directory " + directory);
    log.info("Starting threads ... ");

    ExecutorService executor = Executors.newFixedThreadPool(nThreads);
    executor.execute(fileReader);

    collectors.clear();
    for (int i = 1; i <= nThreads - 3; i++) {
      AuctionStatisticCollector collector = new AuctionStatisticCollector(this);
      collectors.add(collector);
      executor.execute(collector);
    }

    executor.execute(new AuctionPersistor(this, persistorBatchSize));

    executor.shutdown();

    while (!executor.isTerminated()) {
      try {
        Thread.sleep(LOG_DELAY);
      } catch (InterruptedException ignored) {
      }
      logStatus();
    }
  }

  private void logStatus() {
    int fileCount = fileReader.fileCount();
    if (fileCount >= 0) {
      log.info("File {}/{}", fileReader.fileProcessed(), fileCount);
      log.info("Incoming queue size: {}", incomingCounter);
      log.info("Persist queue size: {}", persist.size());
      if (!errors.isEmpty()) {
        synchronized (errors) {
          for (Iterator<Exception> it = errors.iterator(); it.hasNext(); ) {
            Exception exception = it.next();
            log.error(exception.getLocalizedMessage(), exception);
            it.remove();
          }
        }
      }
    }
  }

  public boolean persistorShouldTerminate() {
    return collectorsShouldTerminate() && persist.isEmpty();
  }

  public boolean collectorsShouldTerminate() {
    return fileReader != null && fileReader.delivered() && incomingById.isEmpty();
  }

  /**
   * Takes the first element of the incomingById list queue and returns a list with all records with the same auctionId as this first element.
   * Removes the elements from the incomingById list.
   *
   * @return List
   */
  public List<AuctionHouseExportRecord> popIncoming() {
    List<AuctionHouseExportRecord> result = new LinkedList<>();
    synchronized (incomingById) {
      try {
        int auctionId = incomingById.keySet().iterator().next();
        List<AuctionHouseExportRecord> removedList = incomingById.remove(auctionId);
        result.addAll(removedList);
        incomingCounter -= removedList.size();
      } catch (NoSuchElementException ignored) {
        // empty list
      }
    }

    // Wake up sleeping file reader to deliver new input if input buffer is available
    synchronized (fileReader) {
      fileReader.notify();
    }

    return result;
  }

  public void pushAsIncoming(AuctionHouseExportRecord record) {
    synchronized (incomingById) {
      int auctionId = record.auctionId();
      if (!incomingById.containsKey(auctionId)) {
        incomingById.put(auctionId, new LinkedList<>());
      }
      incomingById.get(auctionId).add(record);
      incomingCounter++;
    }
  }

  public boolean pushToIncomingIsAllowed() {
    return incomingCounter < INCOMING_THRESHHOLD;
  }

  public boolean pushToPersistIsAllowed() {
    return persist.size() < PERSIST_THRESHHOLD;
  }
  public void pushToPersist(AuctionRecord record) {
    synchronized (persist) {
      persist.add(record);
    }
  }

  public void pushError(Exception ex) {
    synchronized (errors) {
      errors.add(ex);
    }
  }

  public List<AuctionRecord> pollAuctions(int batchSize) {
    List<AuctionRecord> result = new LinkedList<>();
    for (int batchCounter = 1; batchCounter < batchSize; batchCounter++) {
      AuctionRecord record;
      synchronized (persist) {
        record = persist.poll();
      }
      if (record == null) {
        break;
      }

      if (record.faction().equals(Faction.END_OF_FILE)) {
        fileReader.triggerFileEnd(record.auctionId());
      } else {
        result.add(record);
      }
    }

    // Wake up sleeping collectors in case threshold is ok again
    for (AuctionStatisticCollector collector : collectors) {
      synchronized (collector) {
        collector.notify();
      }
    }

    return result;
  }

}
