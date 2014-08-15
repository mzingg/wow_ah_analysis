package mrwolf.dbimport.io.mongodb;

import mrwolf.dbimport.io.AsyncQueue;
import mrwolf.dbimport.io.NopProcessingStateCallback;
import mrwolf.dbimport.model.AuctionDuration;
import mrwolf.dbimport.model.AuctionExportRecord;
import mrwolf.dbimport.model.Faction;
import mrwolf.dbimport.model.mongodb.AuctionExportRecordRepository;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProcessingCallback extends NopProcessingStateCallback {

  private final static int DEFAULT_BATCH_SIZE = 5000;
  private final static int DEFAULT_THREAD_COUNT = 8;

  @Autowired
  @Setter
  @Getter(AccessLevel.PROTECTED)
  private AuctionExportRecordRepository repository;

  @Getter(AccessLevel.PROTECTED)
  private final MongoTemplate mongoTemplate;

  @Setter
  @Getter(AccessLevel.PROTECTED)
  private int batchSize;

  @Setter
  @Getter(AccessLevel.PROTECTED)
  private int threadCount;

  private AsyncQueue<AuctionExportRecord> asyncQueue;
  private ExecutorService executor;

  public ProcessingCallback(final MongoTemplate mongoTemplate) {
    super();
    this.mongoTemplate = mongoTemplate;
    this.batchSize = DEFAULT_BATCH_SIZE;
    this.threadCount = DEFAULT_THREAD_COUNT;
  }

  @Override
  public void init() {
    super.init();

    asyncQueue = new MongoAsyncQueue<AuctionExportRecord>(getBatchSize(), getMongoTemplate()) {

      @Override
      protected void process(final Queue<AuctionExportRecord> processingQueue) {
        getMongoTemplate().insert(processingQueue, AuctionExportRecord.class);
      }

    };

    executor = Executors.newFixedThreadPool(getThreadCount());
    for (int i = 1; i <= getThreadCount(); i++) {
      executor.execute(asyncQueue);
    }
  }

  @Override
  public void close() {
    super.close();
    while (asyncQueue.size() > 0) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        log.error(e.getMessage(), e);
      }
    }
    asyncQueue.stopWorking();
    executor.shutdown();
    try {
      executor.awaitTermination(10, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public Set<String> getProcessedFiles() {
    log.info("Retrieving list of already processed files (this may take a while).");
    final long start = System.currentTimeMillis();

    final Set<String> result = getCompletedSnapshots(getMongoTemplate());

    final long end = System.currentTimeMillis();
    log.info("Retrieving processed files took {}ms. Found {} already processed files.", end - start, result.size());

    return result;
  }

  private Set<String> getCompletedSnapshots(final MongoTemplate mongoTemplate) {
    final Set<String> result = new HashSet<>();

    final DBCollection records = mongoTemplate.getCollection(AuctionExportRecordRepository.COLLECTION_NAME);

    final BasicDBObject factionFilter = new BasicDBObject("faction", Faction.SPECIAL.name());
    final BasicDBObject snapshotSelection = new BasicDBObject("_id", 0).append("snapshotHash", 1);

    final DBCursor results = records.find(factionFilter, snapshotSelection);
    while (results.hasNext()) {
      final DBObject currentResult = results.next();
      result.add((String) currentResult.get("snapshotHash"));
    }
    return result;
  }

  @Override
  public void afterRecord(final AuctionExportRecord record) {
    super.afterRecord(record);
    asyncQueue.enqueue(record);
  }

  @Override
  public void afterFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    super.afterFile(file, snapshotTime, snapshotMd5Hash);

    AuctionExportRecord separatorRecord = new AuctionExportRecord();
    separatorRecord.setSnapshotHash(snapshotMd5Hash);
    separatorRecord.setSnapshotTime(snapshotTime.getTimeInMillis());
    separatorRecord.setAuctionId(0);
    separatorRecord.setRealm("system");
    separatorRecord.setFaction(Faction.SPECIAL);
    separatorRecord.setTimeLeft(AuctionDuration.VERY_LONG);
    separatorRecord.setOwner("system");

    asyncQueue.enqueue(separatorRecord);
  }
}
