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
  @NonNull
  private final String directory;
  private final int persistorBatchSize;
  private final Map<Integer, List<AuctionHouseExportRecord>> incomingById;
  private final Queue<AuctionRecord> persist;
  private final Queue<Exception> errors;
  @Autowired
  @Setter
  @Getter
  private AuctionRecordRepository auctionRepository;
  @Autowired
  @Setter
  @Getter
  private AuctionHouseExportFileRepository fileRepository;
  private FileReader fileReader;

  public AuctionProcessDispatcher(String directory, int persistorBatchSize) {
    this.directory = directory;
    this.persistorBatchSize = persistorBatchSize;
    this.incomingById = new HashMap<>();
    this.persist = new LinkedList<>();
    this.errors = new LinkedList<>();
  }

  public void start() {
    int nThreads = Runtime.getRuntime().availableProcessors();
    if (nThreads < 4) {
      log.error("Dispatcher can only run on a multi processor system.");
      return;
    }

    ExecutorService executor = Executors.newFixedThreadPool(nThreads);
    fileReader = new FileReader(this, directory);
    executor.execute(fileReader);

    for (int i = 1; i <= nThreads - 3; i++) {
      executor.execute(new AuctionStatisticCollector(this));
    }

    for (int i = 1; i <= 2; i++) {
      executor.execute(new AuctionPersistor(this, persistorBatchSize));
    }

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
      log.info("Incoming queue size: {}", incomingById.size());
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
        result.addAll(incomingById.remove(auctionId));
      } catch (NoSuchElementException ignored) {
        // empty list
      }
    }

    return result;
  }

  public void pushAsIncoming(List<AuctionHouseExportRecord> records) {
    synchronized (incomingById) {
      for (AuctionHouseExportRecord record : records) {
        int auctionId = record.auctionId();
        if (!incomingById.containsKey(auctionId)) {
          incomingById.put(auctionId, new LinkedList<>());
        }
        incomingById.get(auctionId).add(record);
      }
    }
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
      AuctionRecord record = null;
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

    return result;
  }
}
