package mrwolf.dbimport.io;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mrwolf.dbimport.export.AuctionHouseExportFile;
import mrwolf.dbimport.export.AuctionHouseExportRecord;
import mrwolf.dbimport.model.AuctionDuration;
import mrwolf.dbimport.model.Faction;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.springframework.util.StringUtils;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

@Slf4j
public class AuctionExportReader implements ReaderCallback {

  private static final String EXPORT_FILE_EXTENSION = "json.bz2";

  private static final int SNAPSHOT_SIZE = 20000;

  private static final int DEFAULT_DELAY_MS = 5000;

  private static final DecimalFormat MS_FORMAT = new DecimalFormat("0.000");
  private final JsonFactory jsonFactory;
  @Setter
  private String directoryPath;
  @Getter
  private int fileCount;
  @Getter
  private int recordCount;
  @Setter
  private int delayMillis;
  @Setter
  private boolean disabled;
  private Set<String> processedFiles;
  @Setter
  private ReaderCallback readerCallback;
  private int snapshotCount;
  private long snapshotTime;

  public AuctionExportReader() {
    this.jsonFactory = new JsonFactory(new ObjectMapper());
    this.recordCount = 0;
    this.fileCount = 0;
    this.snapshotCount = 0;
    this.snapshotTime = System.currentTimeMillis();
    this.delayMillis = DEFAULT_DELAY_MS;
    this.disabled = false;
  }

  public void read() {
    if (disabled) {
      log.info("Reader is disabled. Doing nothing.");
      return;
    }

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

    if (delayMillis > 0) {
      try {
        Thread.sleep(delayMillis);
      } catch (InterruptedException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public boolean beforeRecord(final Map<String, Object> recordData, final Calendar snapshotTime, final String fileMd5Hash) {
    final boolean result = readerCallback == null || readerCallback.beforeRecord(recordData, snapshotTime, fileMd5Hash);
    if (!result) {
      log.debug("Skipped record {} because of callback.", recordData);
    }

    return result;
  }

  @Override
  public void afterRecord(final AuctionHouseExportRecord record) {
    if (readerCallback != null) {
      readerCallback.afterRecord(record);
    }
    recordCount++;
  }

  @Override
  public void close() {
    if (readerCallback != null) {
      readerCallback.close();
    }
    log.debug("Closed");
  }

  private void logRecordStatus(final boolean onlyWhenSnapshotSizeReached) {
    if (onlyWhenSnapshotSizeReached && recordCount < snapshotCount + SNAPSHOT_SIZE) {
      return;
    }

    final long newSnapshotTime = System.currentTimeMillis();

    final int count = recordCount - snapshotCount;
    final long duration = newSnapshotTime - snapshotTime;

    if (count > 0) {
      log.info("Read {} records. {}ms per record.", count, MS_FORMAT.format(duration * 1d / count));
    }

    snapshotTime = newSnapshotTime;
    snapshotCount = recordCount;
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
    Arrays.sort(fileNames, new Comparator<String>() {

      @Override
      public int compare(final String o1, final String o2) {
        return o1.compareTo(o2) * -1;
      }

    });

    final File[] subDirectories = directory.listFiles(new FileFilter() {
      @Override
      public boolean accept(final File file) {
        return file.canRead() && file.isDirectory();
      }
    });

    Arrays.sort(subDirectories, new Comparator<File>() {

      @Override
      public int compare(final File o1, final File o2) {
        return o1.compareTo(o2) * -1;
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
                  final AuctionHouseExportRecord record = createRecord(recordData, faction, (String) realmData.get("slug"), snapshotTime, fileMd5Hash);
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

  private AuctionHouseExportRecord createRecord(final Map<String, Object> recordData, final Faction faction, final String realm, final Calendar snapshotTime, final String fileMd5Hash) {
    AuctionHouseExportRecord result = new AuctionHouseExportRecord(new AuctionHouseExportFile(fileMd5Hash).snapshotTime(snapshotTime.getTimeInMillis()));
    result.faction(faction).realm(realm);

    if (recordData.containsKey("auc")) {
      final Object value = recordData.get("auc");
      if (value instanceof Integer) {
        result.auctionId((Integer) value);
      }
    }

    if (recordData.containsKey("item")) {
      final Object value = recordData.get("item");
      if (value instanceof Integer) {
        result.itemId((Integer) value);
      }
    }

    if (recordData.containsKey("bid")) {
      final Object value = recordData.get("bid");
      if (value instanceof Integer) {
        result.bidAmount((Integer) value);
      }
    }

    if (recordData.containsKey("buyout")) {
      final Object value = recordData.get("buyout");
      if (value instanceof Integer) {
        result.buyoutAmount((Integer) value);
      }
    }

    if (recordData.containsKey("quantity")) {
      final Object value = recordData.get("quantity");
      if (value instanceof Integer) {
        result.quantity((Integer) value);
      }
    }

    if (recordData.containsKey("bid")) {
      final Object value = recordData.get("bid");
      if (value instanceof Integer) {
        result.bidAmount((Integer) value);
      }
    }

    if (recordData.containsKey("timeLeft")) {
      final Object value = recordData.get("timeLeft");
      if (value instanceof String) {
        result.timeLeft(AuctionDuration.lookup((String) value));
      }
    }

    if (recordData.containsKey("petSpeciesId")) {
      final Object value = recordData.get("petSpeciesId");
      if (value instanceof Integer) {
        result.petSpeciesId((Integer) value);
      }
    }

    if (recordData.containsKey("petBreedId")) {
      final Object value = recordData.get("petBreedId");
      if (value instanceof Integer) {
        result.petBreedId((Integer) value);
      }
    }

    if (recordData.containsKey("petLevel")) {
      final Object value = recordData.get("petLevel");
      if (value instanceof Integer) {
        result.petLevel((Integer) value);
      }
    }

    if (recordData.containsKey("petQualityId")) {
      final Object value = recordData.get("petQualityId");
      if (value instanceof Integer) {
        result.petQualityId((Integer) value);
      }
    }

    return result;
  }

  private String getMd5HashOfFile(final File file) {
    String result = null;

    /*
     * try (FileInputStream fileStream = new FileInputStream(file)) {
     * result = DigestUtils.md2Hex(fileStream);
     * } catch (IOException e) {
     * log.error(e.getMessage(), e);
     * }
     */

    String filePath = file.getAbsolutePath() + "/" + file.getName();
    filePath = filePath.replace(directoryPath, "");

    result = DigestUtils.md2Hex(filePath);
    return result;
  }

}
