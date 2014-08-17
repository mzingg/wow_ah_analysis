package mrwolf.dbimport.export;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.experimental.Accessors;
import mrwolf.dbimport.model.AuctionDuration;
import mrwolf.dbimport.model.AuctionRecord;
import mrwolf.dbimport.model.Faction;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Document
@Getter
@Setter
@Accessors(fluent = true)
@EqualsAndHashCode(of = "snapshotHash")
@ToString
public class AuctionHouseExportFile {
  @Id
  @NonNull
  private final String snapshotHash;
  @Transient
  private final JsonFactory jsonFactory;
  @Transient
  private final List<AuctionHouseExportRecord> records;
  @Transient
  private final Map<Integer, AuctionRecord> auctions;

  private LocalDateTime snapshotTime;
  @Transient
  private File file;

  public AuctionHouseExportFile(String snapshotHash) {
    this.snapshotHash = snapshotHash;
    this.jsonFactory = new JsonFactory(new ObjectMapper());
    snapshotTime = LocalDateTime.now();
    records = new LinkedList<>();
    auctions = new HashMap<>();
  }

  public synchronized AuctionHouseExportFile read() throws AuctionHouseExportException {
    if (file != null && file.exists() && file.canRead() && file.isFile()) {
      try {
        readBzipFile();
        return this;
      } catch (IOException e) {
        throw new AuctionHouseExportException(e);
      }
    }
    throw new AuctionHouseExportException("Could not read export file [" + (file != null ? file.getAbsolutePath() : "no file") + "]");
  }

  private void readBzipFile() throws IOException, AuctionHouseExportException {
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
              Map<String, Object> recordData = jp.readValueAs(new TypeReference<Map<String, Object>>() {
              });
              AuctionHouseExportRecord record = new AuctionHouseExportRecord(this).faction(faction).realm((String) realmData.get("slug"));
              fillRecord(record, recordData);
              records.add(record);
              if (!auctions.containsKey(record.auctionId())) {
                auctions.put(record.auctionId(), new AuctionRecord());
              }
              auctions.get(record.auctionId()).update(record);
            }
          }

          // Advance end of 'auctions' object
          jp.nextToken();

        }
      }
    }
  }

  private void fillRecord(AuctionHouseExportRecord record, Map<String, Object> recordData) {
    if (recordData.containsKey("auc")) {
      final Object value = recordData.get("auc");
      if (value instanceof Integer) {
        record.auctionId((Integer) value);
      }
    }

    if (recordData.containsKey("item")) {
      final Object value = recordData.get("item");
      if (value instanceof Integer) {
        record.itemId((Integer) value);
      }
    }

    if (recordData.containsKey("bid")) {
      final Object value = recordData.get("bid");
      if (value instanceof Integer) {
        record.bidAmount((Integer) value);
      }
    }

    if (recordData.containsKey("buyout")) {
      final Object value = recordData.get("buyout");
      if (value instanceof Integer) {
        record.buyoutAmount((Integer) value);
      }
    }

    if (recordData.containsKey("quantity")) {
      final Object value = recordData.get("quantity");
      if (value instanceof Integer) {
        record.quantity((Integer) value);
      }
    }

    if (recordData.containsKey("bid")) {
      final Object value = recordData.get("bid");
      if (value instanceof Integer) {
        record.bidAmount((Integer) value);
      }
    }

    if (recordData.containsKey("timeLeft")) {
      final Object value = recordData.get("timeLeft");
      if (value instanceof String) {
        record.timeLeft(AuctionDuration.lookup((String) value));
      }
    }

    if (recordData.containsKey("petSpeciesId")) {
      final Object value = recordData.get("petSpeciesId");
      if (value instanceof Integer) {
        record.petSpeciesId((Integer) value);
      }
    }

    if (recordData.containsKey("petBreedId")) {
      final Object value = recordData.get("petBreedId");
      if (value instanceof Integer) {
        record.petBreedId((Integer) value);
      }
    }

    if (recordData.containsKey("petLevel")) {
      final Object value = recordData.get("petLevel");
      if (value instanceof Integer) {
        record.petLevel((Integer) value);
      }
    }

    if (recordData.containsKey("petQualityId")) {
      final Object value = recordData.get("petQualityId");
      if (value instanceof Integer) {
        record.petQualityId((Integer) value);
      }
    }
  }

}
