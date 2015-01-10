package mrwolf.dbimport.export;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import mrwolf.dbimport.model.AuctionDuration;
import mrwolf.dbimport.model.Faction;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Accessors(fluent = true)
@EqualsAndHashCode
public class AuctionHouseExportRecord {

  private final long snapshotTime;

  private final long snapshotHash;

  private String id;

  private String realm;

  private Faction faction;

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

  public AuctionHouseExportRecord(long snapshotTime, long snapshotHash) {
    this.snapshotTime = snapshotTime;
    this.snapshotHash = snapshotHash;
    this.realm = StringUtils.EMPTY;
    this.faction = Faction.NEUTRAL;
    this.timeLeft = AuctionDuration.VERY_LONG;
  }

}
