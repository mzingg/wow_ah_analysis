package ch.mrwolf.wow.dbimport.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import ch.mrwolf.wow.dbimport.model.AuctionDuration;
import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;
import ch.mrwolf.wow.dbimport.model.Faction;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class AuctionExportReader implements ReaderCallback {

  private static final String EXPORT_FILE_EXTENSION = "json.bz2";

  private static final int SNAPSHOT_SIZE = 20000;

  private static final DecimalFormat MS_FORMAT = new DecimalFormat("0.000");

  @Setter
  private String directoryPath;

  @Getter
  private int fileCount;

  @Getter
  private int recordCount;

  private Set<String> processedFiles;

  @Setter
  @Autowired
  private ReaderCallback readerCallback;

  private final JsonFactory jsonFactory;

  private int snapshotCount;
  private long snapshotTime;

  public AuctionExportReader() {
    this.jsonFactory = new JsonFactory(new ObjectMapper());
    this.recordCount = 0;
    this.fileCount = 0;
    this.snapshotCount = 0;
    this.snapshotTime = System.currentTimeMillis();
  }

  public void read() {
    if (StringUtils.isEmpty(directoryPath)) {
      log.error("Invalid directory path set.");
      return;
    }

    try {
      init();
      processDirectoy(new File(directoryPath));
    } finally {
      close();
    }
  }

  @Override
  public void init() {
    if (readerCallback != null) {
      readerCallback.init();
    }
    this.processedFiles = getProcessedFiles();

    log.debug("Initialized");
  }

  @Override
  public Set<String> getProcessedFiles() {
    if (readerCallback == null) {
      return new HashSet<>();
    }
    return readerCallback.getProcessedFiles();
  }

  @Override
  public boolean beforeFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    if (processedFiles.contains(snapshotMd5Hash)) {
      log.debug("Skipped file {} because it is in the list of already processed files.", file.getAbsolutePath());
      return false;
    }

    final boolean result = readerCallback != null ? readerCallback.beforeFile(file, snapshotTime, snapshotMd5Hash) : true;
    if (!result) {
      log.debug("Skipped file {} because of callback.", file.getAbsolutePath());
    }

    return result;
  }

  @Override
  public void afterFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    if (readerCallback != null) {
      readerCallback.afterFile(file, snapshotTime, snapshotMd5Hash);
    }
    logRecordStatus(false);
    fileCount++;
    log.info("Processed file {}.", file.getAbsolutePath());
  }

  @Override
  public boolean beforeRecord(final Map<String, Object> recordData, final Calendar snapshotTime, final String fileMd5Hash) {
    final boolean result = readerCallback != null ? readerCallback.beforeRecord(recordData, snapshotTime, fileMd5Hash) : true;
    if (!result) {
      log.debug("Skipped record {} because of callback.", recordData);
    }

    return result;
  }

  @Override
  public void afterRecord(final AuctionExportRecord record) {
    if (readerCallback != null) {
      readerCallback.afterRecord(record);
    }
    logRecordStatus(true);
    recordCount++;
  }

  private void logRecordStatus(final boolean onlyWhenSnapshotSizeReached) {
    if (onlyWhenSnapshotSizeReached && recordCount < snapshotCount + SNAPSHOT_SIZE) {
      return;
    }

    final long newSnapshotTime = System.currentTimeMillis();

    final int count = recordCount - snapshotCount;
    final long duration = newSnapshotTime - snapshotTime;

    log.info("Read {} records. {}ms per record.", count, MS_FORMAT.format(duration * 1d / count));

    snapshotTime = newSnapshotTime;
    snapshotCount = recordCount;
  }

  @Override
  public void close() {
    if (readerCallback != null) {
      readerCallback.close();
    }
    log.debug("Closed");
  }

  private void processDirectoy(final File directory) {

    if (!directory.isDirectory() || !directory.canRead()) {
      log.error("Configured directory path [{}] is not a directory or not readeable.", directoryPath);
      return;
    }

    final String[] fileNames = directory.list(new FilenameFilter() {
      @Override
      public boolean accept(final File parentDirectory, final String fileName) {
        return fileName.endsWith(EXPORT_FILE_EXTENSION);
      }
    });

    final File[] subDirectories = directory.listFiles(new FileFilter() {
      @Override
      public boolean accept(final File file) {
        return file.canRead() && file.isDirectory();
      }
    });

    // Depth first recursion
    for (File subdirectory : subDirectories) {
      processDirectoy(subdirectory);
    }

    for (String fileName : fileNames) {

      final String filePath = directory + File.separator + fileName;
      final File fileToProcess = new File(filePath);

      if (!fileToProcess.exists() || !fileToProcess.canRead()) {
        log.error("Cannot access file {}.", filePath);
        continue;
      }

      String fileMd5Hash = getMd5HashOfFile(fileToProcess);

      final Calendar snapshotTime = Calendar.getInstance();
      snapshotTime.setTimeInMillis(fileToProcess.lastModified());

      if (beforeFile(fileToProcess, snapshotTime, fileMd5Hash)) {
        processRecords(fileToProcess, snapshotTime, fileMd5Hash);
        afterFile(fileToProcess, snapshotTime, fileMd5Hash);
      }
    }
  }

  private void processRecords(final File file, final Calendar snapshotTime, final String fileMd5Hash) {
    try {

      try (BZip2CompressorInputStream inputStream = new BZip2CompressorInputStream(new FileInputStream(file))) {

        try (JsonParser jp = jsonFactory.createParser(inputStream)) {

          if (jp.nextToken() != JsonToken.START_OBJECT) {
            // invalid format
            return;
          }

          if (!jp.nextFieldName(new SerializedString("realm"))) {
            // no realm entry
            return;
          }

          // read realm information
          jp.nextValue();
          final Map<String, Object> realmData = jp.readValueAs(new TypeReference<Map<String, Object>>() {
          });

          while (jp.nextToken() != JsonToken.END_OBJECT) {
            Faction faction = Faction.lookup(jp.getCurrentName());

            if (jp.nextToken() == JsonToken.START_OBJECT) {

              if (!jp.nextFieldName(new SerializedString("auctions"))) {
                // no auctions entry
                return;
              }

              if (jp.nextToken() != JsonToken.START_ARRAY) {
                // auctions is not an object
                return;
              }

              while (jp.nextToken() != JsonToken.END_ARRAY) {
                final Map<String, Object> recordData = jp.readValueAs(new TypeReference<Map<String, Object>>() {
                });

                if (beforeRecord(recordData, snapshotTime, fileMd5Hash)) {
                  final AuctionExportRecord record = createRecord(recordData, faction, (String) realmData.get("slug"), snapshotTime, fileMd5Hash);
                  afterRecord(record);
                }
              }

              // Advance end of 'auctions' object
              jp.nextToken();

            }
          }
        }

      }

    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }

  private AuctionExportRecord createRecord(final Map<String, Object> recordData, final Faction faction, final String realm, final Calendar snapshotTime, final String fileMd5Hash) {
    AuctionExportRecord result = new AuctionExportRecord();
    result.setFaction(faction);
    result.setRealm(realm);
    result.setSnapshotTime(snapshotTime);
    result.setSnapshotHash(fileMd5Hash);

    if (recordData.containsKey("auc")) {
      final Object value = recordData.get("auc");
      if (value instanceof Integer) {
        result.setAuctionId((Integer) value);
      }
    }

    if (recordData.containsKey("item")) {
      final Object value = recordData.get("item");
      if (value instanceof Integer) {
        result.setItemId((Integer) value);
      }
    }

    if (recordData.containsKey("owner")) {
      final Object value = recordData.get("owner");
      if (value instanceof String) {
        result.setOwner((String) value);
      }
    }

    if (recordData.containsKey("bid")) {
      final Object value = recordData.get("bid");
      if (value instanceof Integer) {
        result.setBidAmount((Integer) value);
      }
    }

    if (recordData.containsKey("buyout")) {
      final Object value = recordData.get("buyout");
      if (value instanceof Integer) {
        result.setBuyoutAmount((Integer) value);
      }
    }

    if (recordData.containsKey("quantity")) {
      final Object value = recordData.get("quantity");
      if (value instanceof Integer) {
        result.setQuantity((Integer) value);
      }
    }

    if (recordData.containsKey("bid")) {
      final Object value = recordData.get("bid");
      if (value instanceof Integer) {
        result.setBidAmount((Integer) value);
      }
    }

    if (recordData.containsKey("timeLeft")) {
      final Object value = recordData.get("timeLeft");
      if (value instanceof String) {
        result.setTimeLeft(AuctionDuration.lookup((String) value));
      }
    }

    if (recordData.containsKey("petSpeciesId")) {
      final Object value = recordData.get("petSpeciesId");
      if (value instanceof Integer) {
        result.setPetSpeciesId((Integer) value);
      }
    }

    if (recordData.containsKey("petBreedId")) {
      final Object value = recordData.get("petBreedId");
      if (value instanceof Integer) {
        result.setPetBreedId((Integer) value);
      }
    }

    if (recordData.containsKey("petLevel")) {
      final Object value = recordData.get("petLevel");
      if (value instanceof Integer) {
        result.setPetLevel((Integer) value);
      }
    }

    if (recordData.containsKey("petQualityId")) {
      final Object value = recordData.get("petQualityId");
      if (value instanceof Integer) {
        result.setPetQualityId((Integer) value);
      }
    }

    if (recordData.containsKey("rand")) {
      final Object value = recordData.get("rand");
      if (value instanceof Integer) {
        result.setRand((Integer) value);
      }
    }

    if (recordData.containsKey("seed")) {
      final Object value = recordData.get("seed");
      if (value instanceof Integer) {
        result.setSeed((Integer) value);
      }
    }

    return result;
  }

  private String getMd5HashOfFile(final File file) {
    String result = null;

    try (FileInputStream fileStream = new FileInputStream(file)) {
      result = DigestUtils.md2Hex(fileStream);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return result;
  }

}
