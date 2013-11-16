package ch.mrwolf.wow.dbimport.tasks.mongodb;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;
import ch.mrwolf.wow.dbimport.model.mongodb.AuctionExportRecordRepository;
import ch.mrwolf.wow.dbimport.model.mongodb.ItemHistoryRepository;
import ch.mrwolf.wow.dbimport.tasks.Task;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

@Slf4j
public class UpdateItemHistory implements Task {

  private final MongoTemplate mongoTemplate;

  @Autowired
  @Setter
  private ItemHistoryRepository itemHistory;

  @Autowired
  @Setter
  private AuctionExportRecordRepository snapshotRepository;

  public UpdateItemHistory(final MongoTemplate mongoTemplate) {
    if (mongoTemplate == null) {
      throw new IllegalArgumentException("MongoTemplate must not be null.");
    }
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public void execute() {
    final DBCollection snapshotCollection = mongoTemplate.getCollection(AuctionExportRecordRepository.COLLECTION_NAME);
    //final DBCollection historyCollection = mongoTemplate.getCollection(ItemHistoryRepository.COLLECTION_NAME);

    final DBCursor cursor = snapshotCollection.find();
    cursor.batchSize(5000);

    final MongoConverter converter = mongoTemplate.getConverter();

    while (cursor.hasNext()) {
      final DBObject currentRecord = cursor.next();
      final AuctionExportRecord record = converter.read(AuctionExportRecord.class, currentRecord);
      updateHistory(record);
    }

  }

  private void updateHistory(final AuctionExportRecord record) {
    log.debug("Updating History for {}", record.getId());
  }
}
