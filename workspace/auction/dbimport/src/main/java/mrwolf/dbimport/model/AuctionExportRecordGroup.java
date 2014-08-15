package mrwolf.dbimport.model;

import lombok.Getter;

import java.util.*;

public class AuctionExportRecordGroup {

  @Getter
  private AuctionExportRecord latestRecord;

  private final Stack<String> recordHistory;

  private final Set<Long> bidHistory;

  public AuctionExportRecordGroup() {
    this.latestRecord = null;
    recordHistory = new Stack<>();
    bidHistory = new LinkedHashSet<>();
  }

  public void collect(final AuctionExportRecord record) {
    if (record == null) {
      return;
    }

    if (latestRecord != null && latestRecord.getAuctionId() != record.getAuctionId()) {
      return;
    }

    recordHistory.push(record.getId());
    bidHistory.add(record.getBidAmount());

    latestRecord = record;
  }

  public boolean isExpired(final long timePoint) {
    long maxTime = latestRecord.getSnapshotTime() + latestRecord.getTimeLeft().getOffsetTime();
    return timePoint > maxTime;
  }

  public List<String> getHistory() {
    return Collections.unmodifiableList(recordHistory);
  }

  public AuctionRecord getAuctionRecord() {
    final AuctionRecord result = new AuctionRecord();

    result.setAuctionId(latestRecord.getAuctionId());
    result.setLastOccurence(latestRecord.getSnapshotTime());
    result.setLastDuration(latestRecord.getTimeLeft());
    result.setFaction(latestRecord.getFaction());
    result.setRealm(latestRecord.getRealm());
    result.setOwner(latestRecord.getOwner());

    result.setItemId(latestRecord.getItemId());
    result.setQuantity(latestRecord.getQuantity());
    result.setPetSpeciesId(latestRecord.getPetSpeciesId());
    result.setPetBreedId(latestRecord.getPetBreedId());
    result.setPetQualityId(latestRecord.getPetQualityId());
    result.setPetLevel(latestRecord.getPetLevel());

    result.setBuyoutAmount(latestRecord.getBuyoutAmount());
    result.setBidHistory(bidHistory);

    return result;
  }
}
