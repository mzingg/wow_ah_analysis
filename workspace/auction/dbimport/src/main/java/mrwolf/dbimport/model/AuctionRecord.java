package mrwolf.dbimport.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import mrwolf.dbimport.common.AuctionDuration;
import mrwolf.dbimport.common.Faction;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

@Document
@Getter
@Accessors(fluent = true)
public class AuctionRecord {

  @Id
  @Setter
  private int auctionId;

  @Setter
  private String realm;

  @Indexed
  @Setter
  private Faction faction;

  @Indexed
  @Setter
  private int itemId;

  @Setter
  private long buyoutAmount;

  @Setter
  private int quantity;

  @Setter
  private int petSpeciesId;

  @Setter
  private int petBreedId;

  @Setter
  private int petLevel;

  @Setter
  private int petQualityId;

  private long lastOccurence;

  private AuctionDuration lastDuration;

  private int year;
  private int month;
  private int day;
  private int hour;

  private int bidCount;
  private long maxAuctionEnd;
  private boolean probablySold;

  private final Set<Long> bidHistory;

  public AuctionRecord() {
    this.lastDuration = AuctionDuration.VERY_LONG;
    this.faction = Faction.SPECIAL;
    this.bidHistory = new LinkedHashSet<>();
  }

  public AuctionRecord bidHistory(@NonNull final Set<Long> bidHistory) {
    this.bidHistory.clear();
    this.bidHistory.addAll(bidHistory);
    this.bidCount = bidHistory.size();
    return this;
  }

  public AuctionRecord lastOccurence(final long lastOccurence) {
    this.lastOccurence = lastOccurence;
    updateDateDependentFields();
    return this;
  }


  public AuctionRecord lastDuration(final AuctionDuration lastDuration) {
    this.lastDuration = lastDuration;
    updateDateDependentFields();
    updateSoldFlag();
    return this;
  }

  private void updateDateDependentFields() {
    this.maxAuctionEnd = lastOccurence() + lastDuration().getOffsetTime();

    final Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(this.maxAuctionEnd);

    this.year = cal.get(Calendar.YEAR);
    this.month = cal.get(Calendar.MONTH);
    this.day = cal.get(Calendar.DAY_OF_MONTH);
    this.hour = cal.get(Calendar.HOUR_OF_DAY);
  }

  private void updateSoldFlag() {
    this.probablySold = !AuctionDuration.SHORT.equals(lastDuration);
  }

}
