package ch.mrwolf.wow.dbimport.io;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;

@CommonsLog
public class AbstractProcessingStateCallback implements ReaderCallback {

  private static final int SNAPSHOT_SIZE = 10000;

  @Getter
  @Setter
  private boolean recordProcessingEnabled;

  @Getter
  @Setter(AccessLevel.PROTECTED)
  private int fileCount;

  @Getter
  @Setter(AccessLevel.PROTECTED)
  private int recordCount;

  @Getter(AccessLevel.PROTECTED)
  @Setter(AccessLevel.PROTECTED)
  private Set<String> processedFiles;

  @Getter(AccessLevel.PROTECTED)
  @Setter(AccessLevel.PROTECTED)
  private Set<Integer> processedRecords;

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
  public void close() {
  }

  @Override
  public boolean beforeFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    return !processedFiles.contains(snapshotMd5Hash);
  }

  @Override
  public void afterFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    fileCount++;
    processedFiles.add(snapshotMd5Hash);
    processedRecords.clear();

    log.info(String.format("Processed file [%s].", file.getName()));
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

    long newSnapshotTime = System.currentTimeMillis();

    int count = recordCount - snapshotCount;
    long duration = newSnapshotTime - snapshotTime;

    log.info(String.format("Processed %d records in %dms.", count, duration));

    snapshotTime = newSnapshotTime;
    snapshotCount = recordCount;
  }

}
