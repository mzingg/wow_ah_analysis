package ch.mrwolf.wow.dbimport.io;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;

@Slf4j
public class AbstractProcessingStateCallback implements ReaderCallback {

  private static final int SNAPSHOT_SIZE = 10000;

  @Getter
  @Setter
  private boolean recordProcessingEnabled; // NOPMD

  @Getter
  @Setter(AccessLevel.PROTECTED)
  private int fileCount; // NOPMD

  @Getter
  @Setter(AccessLevel.PROTECTED)
  private int recordCount; // NOPMD

  @Getter(AccessLevel.PROTECTED)
  @Setter(AccessLevel.PROTECTED)
  private Set<String> processedFiles; // NOPMD

  @Getter(AccessLevel.PROTECTED)
  @Setter(AccessLevel.PROTECTED)
  private Set<Integer> processedRecords; // NOPMD

  protected transient long snapshotTime;

  protected transient int snapshotCount;

  public AbstractProcessingStateCallback() {
    this.processedFiles = new HashSet<String>();
    this.processedRecords = new HashSet<Integer>();
    this.recordCount = 0;
    this.fileCount = 0;
    this.recordProcessingEnabled = true;
  }

  @Override
  public void init() {
    snapshotTime = System.currentTimeMillis();
    snapshotCount = recordCount;
  }

  @Override
  public boolean beforeFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    if (processedFiles.contains(snapshotMd5Hash)) {
      log.info("Skipping file [{}]", file.getName());
      return false;
    }
    return true;
  }

  @Override
  public void afterFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    fileCount++;
    processedFiles.add(snapshotMd5Hash);
    processedRecords.clear();

    log.info("Processed file [{}].", file.getName());
  }

  @Override
  public boolean beforeRecord(final Map<String, Object> recordData, final Calendar snapshotTime, final String snapshotMd5Hash) {
    Integer auctionId = 0;
    if (recordData.containsKey("auc")) {
      final Object value = recordData.get("auc");
      if (value instanceof Integer) {
        auctionId = (Integer) value;
      }
    }

    return !processedRecords.contains(auctionId);
  }

  @Override
  public void afterRecord(final AuctionExportRecord record) {
    recordCount++;
    processedRecords.add(record.getAuctionId());

    logRecordStatus();
  }

  private void logRecordStatus() {
    if (recordCount < snapshotCount + SNAPSHOT_SIZE) {
      return;
    }

    final long newSnapshotTime = System.currentTimeMillis();

    final int count = recordCount - snapshotCount;
    final long duration = newSnapshotTime - snapshotTime;

    log.info("Processed {} records in {}ms.", count, duration);

    snapshotTime = newSnapshotTime;
    snapshotCount = recordCount;
  }

  /**
   * Empty default implementation.
   */
  @Override
  public void close() {
  }

}
