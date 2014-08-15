package mrwolf.dbimport.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

@Document
public class AuctionRecord {

  @Id
  @Getter
  @Setter
  private int auctionId;

  @Getter
  @Setter
  private String realm;

  @Getter
  @Setter
  @Indexed
  private Faction faction;

  @Getter
  @Setter
  private int itemId;

  @Getter
  private long lastOccurence;

  public void setLastOccurence(final long lastOccurence) {
    this.lastOccurence = lastOccurence;
    updateDateDependentFields();
  }

  @Getter
  private AuctionDuration lastDuration;

  public void setLastDuration(final AuctionDuration lastDuration) {
    this.lastDuration = lastDuration;
    updateDateDependentFields();
    updateSoldFlag();
  }

  @Getter
  private boolean probablySold;

  private void updateSoldFlag() {
    this.probablySold = !AuctionDuration.SHORT.equals(lastDuration);
  }

  @Getter
  private long maxAuctionEnd;

  @Getter
  private int year;
  @Getter
  private int month;
  @Getter
  private int day;
  @Getter
  private int hour;

  private void updateDateDependentFields() {
    this.maxAuctionEnd = getLastOccurence() + getLastDuration().getOffsetTime();

    final Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(this.maxAuctionEnd);

    this.year = cal.get(Calendar.YEAR);
    this.month = cal.get(Calendar.MONTH);
    this.day = cal.get(Calendar.DAY_OF_MONTH);
    this.hour = cal.get(Calendar.HOUR_OF_DAY);
  }

  @Getter
  @Setter
  private String owner;

  @Getter
  private Set<Long> bidHistory;

  public void setBidHistory(final Set<Long> bidHistory) {
    this.bidHistory = bidHistory;
    this.bidCount = bidHistory != null ? bidHistory.size() : 0;
  }

  @Getter
  private int bidCount;

  @Getter
  @Setter
  private long buyoutAmount;

  @Getter
  @Setter
  private int quantity;

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

  public AuctionRecord() {
    this.lastDuration = AuctionDuration.VERY_LONG;
    this.faction = Faction.SPECIAL;
    this.bidHistory = new LinkedHashSet<>();
  }

}
