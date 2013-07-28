package ch.mrwolf.wow.dbimport.io.directdb;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import javax.sql.DataSource;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;

import ch.mrwolf.wow.dbimport.io.AbstractJdbcProcessingStateCallback;
import ch.mrwolf.wow.dbimport.model.AuctionDuration;
import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;

@CommonsLog
public class ProcessingCallback extends AbstractJdbcProcessingStateCallback {

  private final static int DEFAULT_BATCH_SIZE = 15000;

  @Setter
  @Getter(AccessLevel.PROTECTED)
  private int batchSize;

  private final Queue<AuctionExportRecord> processingQueue;

  public ProcessingCallback() {
    super();
    this.processingQueue = new LinkedList<AuctionExportRecord>();
    this.batchSize = DEFAULT_BATCH_SIZE;
  }

  @Override
  public void afterRecord(final AuctionExportRecord record) {

    this.processingQueue.offer(record);

    if (processingQueue.size() >= getBatchSize()) {
      flushQueue();
    }

    super.afterRecord(record);
  }

  private void flushQueue() {
    if (processingQueue.size() == 0) {
      return;
    }

    log.info("Flushe Queue: " + processingQueue.size() + " Elemente.");
    final String sqlStatement = String
        .format(
            "INSERT INTO %s (auction_id, faction, realm, snapshot_hash, timestamp, expected_end, item_id, time_left, pet_species_id, pet_breed_id, pet_level, pet_quality_id, owner, bid_amount, buyout_amount, quantity, rand, seed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            getTableName());

    getJdbcTemplate().batchUpdate(sqlStatement, new BatchPreparedStatementSetter() {
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
  }

  @Override
  public boolean beforeFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    this.snapshotCount = getProcessedRecords().size();
    this.snapshotTime = System.currentTimeMillis();

    if (processingQueue.size() > 0) {
      log.error("Processing Queue ist nicht leer!");
      this.processingQueue.clear();
    }

    return super.beforeFile(file, snapshotTime, snapshotMd5Hash);
  }

  @Override
  public void afterFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    flushQueue();
    super.afterFile(file, snapshotTime, snapshotMd5Hash);
  }

  @Override
  public void setDataSource(final DataSource dataSource) {
    super.setDataSource(dataSource);
    updateProcessedFiles();
  }

  private void updateProcessedFiles() {
    log.info("Updating processed files");
    final long start = System.currentTimeMillis();

    final String sqlStatement = String.format("SELECT DISTINCT snapshot_hash FROM %s", getTableName());
    getJdbcTemplate().query(sqlStatement, new Object[0], new RowCallbackHandler() {

      @Override
      public void processRow(final ResultSet rs) throws SQLException {
        final String md5Hash = rs.getString("snapshot_hash");
        final Set<String> processedFiles = getProcessedFiles();
        processedFiles.add(md5Hash);
      }
    });

    final long end = System.currentTimeMillis();
    log.info("Updating took " + (end - start) + "ms");
  }

}
