package mrwolf.dbimport.model;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Getter
@Accessors(fluent = true)
public class BidHistoryEntry {

  private final String key;

  private long amount;

  private long timestamp;

  private AuctionDuration duration;

  public BidHistoryEntry(long amount, long timestamp, AuctionDuration duration) {
    this.amount = amount;
    this.timestamp = timestamp;
    this.duration = duration;
    this.key = DigestUtils.sha1Hex(amount + timestamp + duration.name());
  }


}
