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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;

import ch.mrwolf.wow.dbimport.io.AbstractJdbcProcessingStateCallback;
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
            "INSERT INTO %s (auction_id, snapshot_hash, timestamp, item_id, time_left, pet_species_id, pet_breed_id, pet_level, pet_quality_id, owner, bid_amount, buyout_amount, quantity, rand, seed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            getTableName());

    getJdbcTemplate().batchUpdate(sqlStatement, new BatchPreparedStatementSetter() {
      @Override
      public void setValues(final PreparedStatement ps, final int batchIndex) throws SQLException {

        final AuctionExportRecord record = processingQueue.poll();

        ps.setInt(1, record.getAuctionId());
        ps.setString(2, record.getSnapshotHash());
        ps.setTimestamp(3, new Timestamp(record.getSnapshotTime().getTimeInMillis()));
        ps.setInt(4, record.getItemId());
        ps.setString(5, record.getTimeLeft().name());

        if (record.getPetSpeciesId() > 0) {
          ps.setInt(6, record.getPetSpeciesId());
        } else {
          ps.setNull(6, Types.INTEGER);
        }

        if (record.getPetBreedId() > 0) {
          ps.setInt(7, record.getPetBreedId());
        } else {
          ps.setNull(7, Types.INTEGER);
        }

        if (record.getPetLevel() > 0) {
          ps.setInt(8, record.getPetLevel());
        } else {
          ps.setNull(8, Types.INTEGER);
        }

        if (record.getPetQualityId() > 0) {
          ps.setInt(9, record.getPetQualityId());
        } else {
          ps.setNull(9, Types.INTEGER);
        }

        ps.setString(10, record.getOwner());
        ps.setInt(11, record.getBidAmount());
        ps.setInt(12, record.getBuyoutAmount());
        ps.setInt(13, record.getQuantity());
        ps.setInt(14, record.getRand());
        ps.setInt(15, record.getSeed());
      }

      @Override
      public int getBatchSize() {
        return processingQueue.size();
      }
    });
  }

  @Override
  public boolean beforeFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    updateRecordState(snapshotMd5Hash);

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

  private void updateRecordState(final String md5Hash) {
    final Set<Integer> processedRecords = getProcessedRecords();
    final String sqlStatement = String.format("SELECT DISTINCT auction_id FROM %s WHERE snapshot_hash = ?", getTableName());
    getJdbcTemplate().query(sqlStatement, new Object[] { md5Hash }, new RowCallbackHandler() {
      @Override
      public void processRow(final ResultSet rs) throws SQLException {
        processedRecords.add(rs.getInt("auction_id"));
      }
    });
    setRecordCount(processedRecords.size());
  }
}
