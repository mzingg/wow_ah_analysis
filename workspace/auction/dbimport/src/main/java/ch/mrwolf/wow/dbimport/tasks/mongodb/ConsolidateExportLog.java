package ch.mrwolf.wow.dbimport.tasks.mongodb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import ch.mrwolf.wow.dbimport.io.AsyncQueue;
import ch.mrwolf.wow.dbimport.io.mongodb.MongoAsyncQueue;
import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;
import ch.mrwolf.wow.dbimport.model.AuctionExportRecordGroup;
import ch.mrwolf.wow.dbimport.model.Faction;
import ch.mrwolf.wow.dbimport.model.mongodb.AuctionExportRecordRepository;
import ch.mrwolf.wow.dbimport.model.mongodb.AuctionsRepository;
import ch.mrwolf.wow.dbimport.model.mongodb.ItemHistoryRepository;
import ch.mrwolf.wow.dbimport.tasks.Task;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;

@Slf4j
public class ConsolidateExportLog implements Task {

  private static final int FETCH_BATCH_SIZE = 100000;
  private static final int ENQUEUE_BATCH_SIZE = 10000;
  private static final int QUEUE_BATCH_SIZE = 250;
  private static final int THREAD_COUNT = 10;

  private final MongoTemplate mongoTemplate;

  @Autowired
  @Setter
  private ItemHistoryRepository itemHistory;

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
    //final DBCollection historyCollection = mongoTemplate.getCollection(ItemHistoryRepository.COLLECTION_NAME);

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
      final AuctionExportRecord record = converter.read(AuctionExportRecord.class, currentRecord);

      boolean recordBoundaryReached = false;
      if (record.getAuctionId() != currentAuctionId) {
        recordBoundaryReached = true;
        currentAuctionId = record.getAuctionId();
      }

      collectCurrentRecord(record);

      if (recordBoundaryReached) {
        if (counter % ENQUEUE_BATCH_SIZE == 0) {
          handleExpiredRecords(new HashSet<Integer>(idsToProcess));
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

  private void collectCurrentRecord(final AuctionExportRecord record) {
    int auctionId = record.getAuctionId();

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
