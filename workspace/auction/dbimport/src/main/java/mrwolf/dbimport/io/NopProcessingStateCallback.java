package mrwolf.dbimport.io;

import mrwolf.dbimport.export.AuctionHouseExportRecord;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class NopProcessingStateCallback implements ReaderCallback {

  @Override
  public void init() {
  }

  @Override
  public void close() {
  }

  @Override
  public Set<String> getProcessedFiles() {
    return new HashSet<>();
  }

  @Override
  public void afterFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
  }

  @Override
  public boolean beforeFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    return true;
  }

  @Override
  public boolean beforeRecord(final Map<String, Object> recordData, final Calendar snapshotTime, final String fileMd5Hash) {
    return true;
  }

  @Override
  public void afterRecord(final AuctionHouseExportRecord record) {
  }

}
