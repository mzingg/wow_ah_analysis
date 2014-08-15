package mrwolf.dbimport.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@CompoundIndexes({
  @CompoundIndex(name = "unique_record_index", def = "{'auctionId': 1, 'snapshotHash': 1}", unique = true, dropDups = true),
  @CompoundIndex(name = "sort_records_index", def = "{'auctionId': 1, 'snapshotTime': 1}")
})
public class AuctionExportRecord {

  @Id
  @Getter
  private String id;

  @Getter
  @Setter
  private String realm;

  @Getter
  @Setter
  @Indexed
  private Faction faction;

  @Getter
  @Setter
  private int auctionId;

  @Getter
  @Setter
  private int itemId;

  @Getter
  @Setter
  private String owner;

  @Getter
  @Setter
  private long snapshotTime;

  @Getter
  @Setter
  private String snapshotHash;

  @Getter
  @Setter
  private long bidAmount;

  @Getter
  @Setter
  private long buyoutAmount;

  @Getter
  @Setter
  private int quantity;

  @Getter
  @Setter
  private AuctionDuration timeLeft;

  @Getter
  @Setter
  private int petSpeciesId;

  @Getter
  @Setter
  private int petBreedId;

  @Getter
  @Setter
  private int petLevel;

  @Getter
  @Setter
  private int petQualityId;

  @Getter
  @Setter
  private int rand;

  @Getter
  @Setter
  private int seed;
}
