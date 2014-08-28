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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Accessors(fluent = true)
public class AuctionProcessDispatcher {

  private static final int LOG_DELAY = 1000; // 1 second

  @Autowired
  @Setter
  @Getter
  private AuctionRecordRepository auctionRepository;

  @Autowired
  @Setter
  @Getter
  private AuctionHouseExportFileRepository fileRepository;

  @NonNull
  private final String directory;
  private final int persistorBatchSize;

  private final Queue<AuctionHouseExportRecord> incoming;
  private final Queue<AuctionRecord> persist;
  private final Queue<Exception> errors;
  private FileReader fileReader;

  public AuctionProcessDispatcher(String directory, int persistorBatchSize) {
    this.directory = directory;
    this.persistorBatchSize = persistorBatchSize;
    this.incoming = new LinkedList<>();
    this.persist = new LinkedList<>();
    this.errors = new LinkedList<>();
  }

  public void start() {
    int nThreads = Runtime.getRuntime().availableProcessors();
    ExecutorService executor = Executors.newFixedThreadPool(nThreads);

    fileReader = new FileReader(this, directory);
    executor.execute(fileReader);

    for (int i = 1; i <= nThreads - 1; i++) {
      executor.execute(new AuctionStatisticCollector(this));
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
    log.info("File {}/{}", fileReader.fileProcessed(), fileReader.fileCount());
    log.info("Incoming queue size: {}", incoming.size());
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

  public boolean persistorShouldTerminate() {
    return collectorsShouldTerminate() && persist.isEmpty();
  }

  public boolean collectorsShouldTerminate() {
    return fileReader != null && fileReader.delivered() && incoming.isEmpty();
  }

  /**
   * Takes the first element of the incoming list queue and returns a list with all records with the same auctionId as this first element.
   * Removes the elements from the incoming list.
   *
   * @return List
   */
  public List<AuctionHouseExportRecord> popIncoming() {
    List<AuctionHouseExportRecord> result = new LinkedList<>();
    synchronized (incoming) {
      int auctionId = 0;
      for (Iterator<AuctionHouseExportRecord> records = incoming.iterator(); records.hasNext(); ) {
        AuctionHouseExportRecord record = records.next();
        if (auctionId == 0) {
          auctionId = record.auctionId();
        }
        if (record.auctionId() == auctionId) {
          result.add(record);
          records.remove();
        }
      }
    }
    return result;
  }

  public void pushAsIncoming(List<AuctionHouseExportRecord> records) {
    synchronized (incoming) {
      incoming.addAll(records);
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
    if (!persist.isEmpty()) {
      synchronized (persist) {
        int batchCounter = 1;
        for (Iterator<AuctionRecord> records = persist.iterator(); records.hasNext() && batchCounter < batchSize; ) {
          AuctionRecord record = records.next();
          if (record.faction().equals(Faction.END_OF_FILE)) {
            fileReader.triggerFileEnd(record.auctionId());
          } else {
            result.add(record);
          }
          records.remove();
          batchCounter++;
        }
      }
    }
    return result;
  }
}
