package ch.mrwolf.wow.dbimport.io.mongodb;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import ch.mrwolf.wow.dbimport.io.NopProcessingStateCallback;
import ch.mrwolf.wow.dbimport.model.AuctionDuration;
import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;
import ch.mrwolf.wow.dbimport.model.Faction;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

@Slf4j
public class ProcessingCallback extends NopProcessingStateCallback {

  private final static int DEFAULT_BATCH_SIZE = 5000;
  private final static int DEFAULT_THREAD_COUNT = 8;
  private final static String SNAPSHOT_COLLECTION_NAME = "auctionExportRecord";

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

  private StorageQueue storageQueue;
  private ThreadGroup storageThreads;

  public ProcessingCallback(final MongoTemplate mongoTemplate) {
    super();
    this.mongoTemplate = mongoTemplate;
    this.batchSize = DEFAULT_BATCH_SIZE;
    this.threadCount = DEFAULT_THREAD_COUNT;
  }

  @Override
  public void init() {
    super.init();

    storageQueue = new StorageQueue(SNAPSHOT_COLLECTION_NAME, getBatchSize(), getRepository(), getMongoTemplate());

    storageThreads = new ThreadGroup("Database Storage Threads");
    for (int i = 1; i <= getThreadCount(); i++) {
      Thread t = new Thread(storageThreads, storageQueue);
      t.start();
    }
  }

  @Override
  public void close() {
    super.close();
    storageQueue.stopWorking();
    while (storageThreads.activeCount() > 0) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public Set<String> getProcessedFiles() {
    log.info("Retrieving list of already processed files (this may take a while).");
    final long start = System.currentTimeMillis();

    final Set<String> result = new HashSet<>();

    final DBCollection records = getMongoTemplate().getCollection(SNAPSHOT_COLLECTION_NAME);

    final BasicDBObject factionFilter = new BasicDBObject("faction", "SPECIAL");
    final BasicDBObject snapshotSelection = new BasicDBObject("_id", 0).append("snapshotHash", 1);

    final DBCursor results = records.find(factionFilter, snapshotSelection);
    while (results.hasNext()) {
      DBObject currentResult = results.next();
      result.add((String) currentResult.get("snapshotHash"));
    }

    final long end = System.currentTimeMillis();
    log.info("Retrieving processed files took {}ms. Found {} already processed files.", end - start, result.size());

    return result;
  }

  @Override
  public void afterRecord(final AuctionExportRecord record) {
    super.afterRecord(record);
    storageQueue.enqueue(record);
  }

  @Override
  public void afterFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    super.afterFile(file, snapshotTime, snapshotMd5Hash);

    AuctionExportRecord separatorRecord = new AuctionExportRecord();
    separatorRecord.setSnapshotHash(snapshotMd5Hash);
    separatorRecord.setSnapshotTime(snapshotTime);
    separatorRecord.setAuctionId(0);
    separatorRecord.setRealm("system");
    separatorRecord.setFaction(Faction.SPECIAL);
    separatorRecord.setTimeLeft(AuctionDuration.VERY_LONG);
    separatorRecord.setOwner("system");

    storageQueue.enqueue(separatorRecord);
  }
}
