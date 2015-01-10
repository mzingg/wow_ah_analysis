package mrwolf.dbimport.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import mrwolf.dbimport.export.AuctionHouseExportException;
import mrwolf.dbimport.export.AuctionHouseExportRecord;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Getter
@Setter
@Accessors(fluent = true)
public class AuctionRecord {

  @Getter(AccessLevel.PRIVATE)
  private final Map<String, BidHistoryEntry> bidHistory;

  private int auctionId;
  private String realm;
  private Faction faction;
  private int itemId;
  private long buyoutAmount;
  private int quantity;
  private int petSpeciesId;
  private int petBreedId;
  private int petLevel;
  private int petQualityId;
  private long lastOccurence;
  private AuctionDuration lastDuration;
  private AuctionStatus status;

  private boolean initialized;

  public AuctionRecord() {
    this.realm = StringUtils.EMPTY;
    this.lastDuration = AuctionDuration.VERY_LONG;
    this.faction = Faction.NO_FACTION;
    this.bidHistory = new LinkedHashMap<>();
    this.initialized = false;
    this.status = AuctionStatus.ACTIVE;
  }

  public void update(@NonNull AuctionHouseExportRecord record) throws AuctionHouseExportException {
    validate(record, this.initialized);
    if (!this.initialized) {
      updateMetaData(record);
      this.initialized = true;
    }

    lastOccurence = record.snapshotTime();
    lastDuration = record.timeLeft();

    updateStatusFlag();
    update(new BidHistoryEntry(record.auctionId(), record.bidAmount(), lastOccurence, lastDuration));
  }

  public Collection<BidHistoryEntry> getBidHistoryList() {
    return Collections.unmodifiableCollection(bidHistory.values());
  }

  public void update(BidHistoryEntry historyEntry) {
    if (!bidHistory.containsKey(historyEntry.key())) {
      bidHistory.put(historyEntry.key(), historyEntry);
    }
  }

  public void update(List<BidHistoryEntry> historyEntries) {
    for (BidHistoryEntry entry : historyEntries) {
      update(entry);
    }
  }

  private void validate(AuctionHouseExportRecord record, boolean compareMetaData) throws AuctionHouseExportException {

    if (Faction.END_OF_FILE.equals(record.faction()) && record.auctionId() > 0) {
      // Special case end of import file marker record needs not to be validated
      return;
    }

    if (record.auctionId() <= 0) {
      throw new AuctionHouseExportException("Invalid auctionId.");
    }

    if (record.faction() == null) {
      throw new AuctionHouseExportException("Invalid faction.");
    }

    if (StringUtils.isBlank(record.realm())) {
      throw new AuctionHouseExportException("Invalid realm.");
    }

    if (record.itemId() <= 0) {
      throw new AuctionHouseExportException("Invalid itemId.");
    }

    if (record.quantity() <= 0) {
      throw new AuctionHouseExportException("Invalid quantity.");
    }

    if (record.petSpeciesId() < 0) {
      throw new AuctionHouseExportException("Invalid petSpeciesId.");
    }

    if (record.petBreedId() < 0) {
      throw new AuctionHouseExportException("Invalid petBreedId.");
    }

    if (record.petQualityId() < 0) {
      throw new AuctionHouseExportException("Invalid petQualityId.");
    }

    if (record.petLevel() < 0) {
      throw new AuctionHouseExportException("Invalid petLevel.");
    }

    if (record.buyoutAmount() < 0) {
      throw new AuctionHouseExportException("Invalid buyoutAmount.");
    }

    if (record.bidAmount() < 0) {
      throw new AuctionHouseExportException("Invalid bidAmount.");
    }

    if (record.snapshotHash() < 0) {
      throw new AuctionHouseExportException("Invalid snapshotHash.");
    }

    if (record.snapshotTime() <= 0) {
      throw new AuctionHouseExportException("Invalid snapshotTime.");
    }

    if (compareMetaData) {
      if (auctionId() != record.auctionId() || !faction().equals(record.faction()) || !realm().equals(record.realm()) ||
          itemId() != record.itemId() || quantity() != record.quantity() ||
          petSpeciesId() != record.petSpeciesId() || petBreedId() != record.petBreedId() || petQualityId() != record.petQualityId() || petLevel() != record.petLevel()) {
        throw new AuctionHouseExportException("Incompatible record.");
      }
    }
  }

  private void updateMetaData(AuctionHouseExportRecord record) throws AuctionHouseExportException {
    this.auctionId = record.auctionId();
    this.faction = record.faction();
    this.realm = record.realm();
    this.itemId = record.itemId();
    this.quantity = record.quantity();
    this.petSpeciesId = record.petSpeciesId();
    this.petBreedId = record.petBreedId();
    this.petQualityId = record.petQualityId();
    this.petLevel = record.petLevel();
    this.buyoutAmount = record.buyoutAmount();
  }

  private void updateStatusFlag() {
    if (isExpired(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))) {
      this.status = AuctionStatus.EXPIRED;
    } else if (!AuctionDuration.SHORT.equals(lastDuration)) {
      this.status = AuctionStatus.PROBABLY_SOLD;
    }
    // ACTIVE is the default in the Ctor
  }

  public boolean isExpired(long timePoint) {
    return timePoint > (lastOccurence() + lastDuration().getOffsetTime());
  }

}
