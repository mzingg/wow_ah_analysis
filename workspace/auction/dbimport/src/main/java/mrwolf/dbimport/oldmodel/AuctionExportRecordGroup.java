package mrwolf.dbimport.oldmodel;

import lombok.Getter;
import mrwolf.dbimport.export.AuctionHouseExportRecord;

import java.time.ZoneOffset;
import java.util.*;

public class AuctionExportRecordGroup {

  @Getter
  private AuctionHouseExportRecord latestRecord;

  private final Stack<String> recordHistory;

  private final Set<Long> bidHistory;

  public AuctionExportRecordGroup() {
    this.latestRecord = null;
    recordHistory = new Stack<>();
    bidHistory = new LinkedHashSet<>();
  }

  public void collect(final AuctionHouseExportRecord record) {
    if (record == null) {
      return;
    }

    if (latestRecord != null && latestRecord.auctionId() != record.auctionId()) {
      return;
    }

    recordHistory.push(record.id());
    bidHistory.add(record.bidAmount());

    latestRecord = record;
  }

  public boolean isExpired(final long timePoint) {
    long maxTime = latestRecord.originFile().snapshotTime() + latestRecord.timeLeft().getOffsetTime();
    return timePoint > maxTime;
  }

  public List<String> getHistory() {
    return Collections.unmodifiableList(recordHistory);
  }

}
