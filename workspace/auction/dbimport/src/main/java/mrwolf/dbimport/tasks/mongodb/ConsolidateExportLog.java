package mrwolf.dbimport.tasks.mongodb;

import mrwolf.dbimport.io.AsyncQueue;
import mrwolf.dbimport.io.mongodb.MongoAsyncQueue;
import mrwolf.dbimport.export.AuctionHouseExportRecord;
import mrwolf.dbimport.model.AuctionExportRecordGroup;
import mrwolf.dbimport.common.Faction;
import mrwolf.dbimport.model.mongodb.AuctionExportRecordRepository;
import mrwolf.dbimport.model.mongodb.AuctionsRepository;
import mrwolf.dbimport.tasks.Task;
import com.mongodb.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ConsolidateExportLog implements Task {

  private static final int FETCH_BATCH_SIZE = 100000;
  private static final int ENQUEUE_BATCH_SIZE = 10000;
  private static final int QUEUE_BATCH_SIZE = 250;
  private static final int THREAD_COUNT = 10;

  private final MongoTemplate mongoTemplate;

  @Autowired
  @Setter
  private AuctionExportRecordRepository snapshotRepository;

  @Autowired
  @Setter
  private AuctionsRepository auctionsRepository;

  private final Map<Integer, AuctionExportRecordGroup> currentAuctions;

  private final ExecutorService executor;
  private final AsyncQueue<AuctionExportRecordGroup> expiredAuctions;

  public ConsolidateExportLog(final MongoTemplate mongoTemplate) {
    if (mongoTemplate == null) {
      throw new IllegalArgumentException("MongoTemplate must not be null.");
    }
    this.mongoTemplate = mongoTemplate;

    this.currentAuctions = new HashMap<>();
    this.expiredAuctions = new MongoAsyncQueue<AuctionExportRecordGroup>(QUEUE_BATCH_SIZE, this.mongoTemplate) {

      @Override
      protected void process(final Queue<AuctionExportRecordGroup> processingQueue) {

        List<String> objectIdList = new ArrayList<>();
        for (AuctionExportRecordGroup group : processingQueue) {
          auctionsRepository.save(group.getAuctionRecord());

          if (group.isExpired(System.currentTimeMillis())) {
            objectIdList.addAll(group.getHistory());
          }
        }

        if (objectIdList.size() == 0) {
          return;
        }

        log.info("Deleting {} records.", objectIdList.size());

        final DBCollection snapshotCollection = mongoTemplate.getCollection(AuctionExportRecordRepository.COLLECTION_NAME);
        for (String objectId : objectIdList) {
          snapshotCollection.remove(new BasicDBObject("_id", new ObjectId(objectId)), WriteConcern.UNACKNOWLEDGED);
        }

      }

    };

    executor = Executors.newCachedThreadPool();
  }

  @Override
  public void execute() {

    final DBCollection snapshotCollection = mongoTemplate.getCollection(AuctionExportRecordRepository.COLLECTION_NAME);

    final BasicDBObject factionFilter = new BasicDBObject("faction", new BasicDBObject("$ne", Faction.SPECIAL.name()));
    final DBCursor cursor = snapshotCollection.find(factionFilter);
    cursor.sort(new BasicDBObject("auctionId", 1).append("snapshotTime", 1));
    cursor.batchSize(FETCH_BATCH_SIZE);

    final MongoConverter converter = mongoTemplate.getConverter();

    for (int i = 1; i <= THREAD_COUNT; i++) {
      executor.execute(expiredAuctions);
    }

    int counter = 0;
    int currentAuctionId = 0;
    Set<Integer> idsToProcess = new HashSet<>();
    while (cursor.hasNext()) {
      final DBObject currentRecord = cursor.next();
      final AuctionHouseExportRecord record = converter.read(AuctionHouseExportRecord.class, currentRecord);

      boolean recordBoundaryReached = false;
      if (record.auctionId() != currentAuctionId) {
        recordBoundaryReached = true;
        currentAuctionId = record.auctionId();
      }

      collectCurrentRecord(record);

      if (recordBoundaryReached) {
        if (counter % ENQUEUE_BATCH_SIZE == 0) {
          handleExpiredRecords(new HashSet<>(idsToProcess));
          idsToProcess.clear();
        }
        idsToProcess.add(currentAuctionId);
        counter++;
      }

    }

    Set<Integer> oldIds = new HashSet<>(currentAuctions.keySet());
    handleExpiredRecords(oldIds);

    log.info("Finished collecting records. Waiting for threads to finish work.");
    while (expiredAuctions.size() > 0) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        log.error(e.getMessage(), e);
      }
    }
    expiredAuctions.stopWorking();
    executor.shutdown();
    try {
      executor.awaitTermination(10, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
    }
    log.info("Finished.");
  }

  private void collectCurrentRecord(final AuctionHouseExportRecord record) {
    int auctionId = record.auctionId();

    if (!currentAuctions.containsKey(auctionId)) {
      currentAuctions.put(auctionId, new AuctionExportRecordGroup());
    }
    currentAuctions.get(auctionId).collect(record);
  }

  private void handleExpiredRecords(final Set<Integer> oldIds) {
    log.info("Cleaning {} auctions", oldIds.size());
    for (int auctionId : oldIds) {
      AuctionExportRecordGroup group = currentAuctions.get(auctionId);
      expiredAuctions.enqueue(group);
      currentAuctions.remove(auctionId);
    }
  }
}
