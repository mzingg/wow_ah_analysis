package mrwolf.dbimport.export;

import lombok.*;
import lombok.experimental.Accessors;
import mrwolf.dbimport.common.AuctionDuration;
import mrwolf.dbimport.common.Faction;
import org.apache.commons.lang3.StringUtils;
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
@Getter
@Setter
@Accessors(fluent = true)
@EqualsAndHashCode
public class AuctionHouseExportRecord {

  @Id
  @NonNull
  private final String id;

  @NonNull
  private final AuctionHouseExportFile originFile;

  private String realm;

  @Indexed
  private Faction faction;

  @Indexed
  private int auctionId;

  private int itemId;

  private long bidAmount;

  private long buyoutAmount;

  private int quantity;

  private AuctionDuration timeLeft;

  private int petSpeciesId;

  private int petBreedId;

  private int petLevel;

  private int petQualityId;

  public AuctionHouseExportRecord(String id, AuctionHouseExportFile originFile) {
    this.id = id;
    this.originFile = originFile;
    this.realm = StringUtils.EMPTY;
    this.faction = Faction.NEUTRAL;
    this.timeLeft = AuctionDuration.VERY_LONG;
  }
}
