package ch.mrwolf.wow.dbimport.io.mongodb;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Queue;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.mongodb.core.MongoTemplate;

import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;

@Slf4j
public class StorageQueue implements Runnable {

  private static final DecimalFormat MS_FORMAT = new DecimalFormat("0.000");

  private final AuctionExportRecordRepository repository;

  private final MongoTemplate mongoTemplate;

  private final String snapshotCollectionName;

  private final int batchSize;

  private final Queue<AuctionExportRecord> workQueue;

  private boolean running;

  public StorageQueue(final String snapshotCollectionName, final int batchSize, final AuctionExportRecordRepository repository, final MongoTemplate mongoTemplate) {
    this.snapshotCollectionName = snapshotCollectionName;
    this.repository = repository;
    this.mongoTemplate = mongoTemplate;
    this.batchSize = batchSize;
    workQueue = new LinkedList<>();
  }

  public synchronized void enqueue(final AuctionExportRecord record) {
    workQueue.add(record);
  }

  public synchronized Queue<AuctionExportRecord> dequeue() {
    final Queue<AuctionExportRecord> result = new LinkedList<>();
    for (int i = 1; i <= batchSize; i++) {
      if (workQueue.isEmpty()) {
        break;
      }
      result.add(workQueue.poll());
    }
    return result;
  }

  public void stopWorking() {
    running = false;
  }

  public int size() {
    return workQueue.size();
  }

  @Override
  public void run() {
    running = true;
    while (workQueue.size() > 0 || running) {

      if (workQueue.size() > 0) {
        log.info("Remaining queue size: {}", workQueue.size());
      }

      final Queue<AuctionExportRecord> records = dequeue();
      flushQueue(records);

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  private void flushQueue(final Queue<AuctionExportRecord> processingQueue) {
    if (processingQueue.size() == 0) {
      return;
    }

    final int count = processingQueue.size();
    long start = System.currentTimeMillis();

    //DBCollection collection = mongoTemplate.getDb().getCollection(snapshotCollectionName);
    //mongoTemplate.insert(processingQueue, AuctionExportRecord.class);
    repository.save(processingQueue);
    /*
        final String sqlStatement = String.format(INSERT_STATEMENT, tableName);

        try {
          jdbcTemplate.batchUpdate(sqlStatement, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps, final int batchIndex) throws SQLException {

              final AuctionExportRecord record = processingQueue.poll();

              final AuctionDuration timeLeft = record.getTimeLeft();
              final long snapshotMillis = record.getSnapshotTime().getTimeInMillis();
              final Timestamp timestamp = new Timestamp(snapshotMillis);
              final Timestamp expected_end = new Timestamp(snapshotMillis + timeLeft.getOffsetTime());

              ps.setInt(1, record.getAuctionId());
              ps.setByte(2, record.getFaction().getDatabaseId());
              ps.setString(3, record.getRealm());
              ps.setString(4, record.getSnapshotHash());
              ps.setTimestamp(5, timestamp);
              ps.setTimestamp(6, expected_end);
              ps.setInt(7, record.getItemId());
              ps.setString(8, timeLeft.name());

              if (record.getPetSpeciesId() > 0) {
                ps.setInt(9, record.getPetSpeciesId());
              } else {
                ps.setNull(9, Types.INTEGER);
              }

              if (record.getPetBreedId() > 0) {
                ps.setInt(10, record.getPetBreedId());
              } else {
                ps.setNull(10, Types.INTEGER);
              }

              if (record.getPetLevel() > 0) {
                ps.setInt(11, record.getPetLevel());
              } else {
                ps.setNull(11, Types.INTEGER);
              }

              if (record.getPetQualityId() > 0) {
                ps.setInt(12, record.getPetQualityId());
              } else {
                ps.setNull(12, Types.INTEGER);
              }

              ps.setString(13, record.getOwner());
              ps.setLong(14, record.getBidAmount());
              ps.setLong(15, record.getBuyoutAmount());
              ps.setInt(16, record.getQuantity());
              ps.setInt(17, record.getRand());
              ps.setInt(18, record.getSeed());
            }

            @Override
            public int getBatchSize() {
              return processingQueue.size();
            }
          });
        } catch (DuplicateKeyException ex) {
          log.warn("Duplicate key occured - ignoring");
        }
    */
    long end = System.currentTimeMillis();

    final long duration = end - start;

    log.info("Stored {} records. {}ms per record.", count, MS_FORMAT.format(duration * 1d / count));
  }
}
