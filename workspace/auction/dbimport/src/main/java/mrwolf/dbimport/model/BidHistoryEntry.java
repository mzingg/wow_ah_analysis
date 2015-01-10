package mrwolf.dbimport.model;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.codec.digest.DigestUtils;

@Getter
@Accessors(fluent = true)
public class BidHistoryEntry {

  private int auctionId;

  private long amount;

  private long timestamp;

  private AuctionDuration duration;

  private final String key;

  public BidHistoryEntry(int auctionId, long amount, long timestamp, AuctionDuration duration) {
    this.auctionId = auctionId;
    this.amount = amount;
    this.timestamp = timestamp;
    this.duration = duration;
    this.key = DigestUtils.sha1Hex(auctionId + amount + timestamp + duration.name());
  }


}
