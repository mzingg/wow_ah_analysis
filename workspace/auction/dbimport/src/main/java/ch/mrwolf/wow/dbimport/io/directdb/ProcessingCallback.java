package ch.mrwolf.wow.dbimport.io.directdb;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.core.RowCallbackHandler;

import ch.mrwolf.wow.dbimport.io.AbstractJdbcProcessingStateCallback;
import ch.mrwolf.wow.dbimport.model.AuctionDuration;
import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;
import ch.mrwolf.wow.dbimport.model.Faction;

@Slf4j
public class ProcessingCallback extends AbstractJdbcProcessingStateCallback {

  private final static int DEFAULT_BATCH_SIZE = 5000;
  private final static int DEFAULT_THREAD_COUNT = 8;

  private final static String SELECT_PROCESSED_FILES_STATEMENT = "SELECT DISTINCT snapshot_hash FROM %s WHERE faction = 99";

  @Setter
  @Getter(AccessLevel.PROTECTED)
  private int batchSize;

  @Setter
  @Getter(AccessLevel.PROTECTED)
  private int threadCount;

  private DatabaseStorageQueue storageQueue;
  private ThreadGroup storageThreads;

  public ProcessingCallback() {
    super();
    this.batchSize = DEFAULT_BATCH_SIZE;
    this.threadCount = DEFAULT_THREAD_COUNT;
  }

  @Override
  public void init() {
    super.init();

    storageQueue = new DatabaseStorageQueue(getJdbcTemplate(), getSnapshotTableName(), batchSize);

    storageThreads = new ThreadGroup("Database Storage Threads");
    for (int i = 1; i <= threadCount; i++) {
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
    final String sqlStatement = String.format(SELECT_PROCESSED_FILES_STATEMENT, getSnapshotTableName());
    getJdbcTemplate().query(sqlStatement, new Object[0], new RowCallbackHandler() {
      @Override
      public void processRow(final ResultSet rs) throws SQLException {
        final String md5Hash = rs.getString("snapshot_hash");
        result.add(md5Hash);
      }
    });

    final long end = System.currentTimeMillis();
    log.info("Retrieving processed files took {}ms", end - start);

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
