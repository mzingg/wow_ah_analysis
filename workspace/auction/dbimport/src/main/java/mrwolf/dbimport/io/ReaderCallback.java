package mrwolf.dbimport.io;

import mrwolf.dbimport.model.AuctionExportRecord;

import java.io.File;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

public interface ReaderCallback {

  public void init();

  public Set<String> getProcessedFiles();

  public boolean beforeFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash);

  public void afterFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash);

  public boolean beforeRecord(final Map<String, Object> recordData, final Calendar snapshotTime, final String fileMd5Hash);

  public void afterRecord(final AuctionExportRecord record);

  public void close();

}
