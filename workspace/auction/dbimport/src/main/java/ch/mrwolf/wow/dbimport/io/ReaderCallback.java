package ch.mrwolf.wow.dbimport.io;

import java.io.File;
import java.util.Calendar;
import java.util.Map;

import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;

public interface ReaderCallback {

  public void setRecordProcessingEnabled(final boolean recordProcessingState);

  public boolean isRecordProcessingEnabled();

  public void init();

  public boolean beforeFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash);

  public void afterFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash);

  public boolean beforeRecord(final Map<String, Object> recordData, final Calendar snapshotTime, final String fileMd5Hash);

  public void afterRecord(final AuctionExportRecord record);

  public void close();

}
