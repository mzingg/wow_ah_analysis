package mrwolf.dbimport.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum AuctionStatus {
  ACTIVE((byte)1), PROBABLY_SOLD((byte)9), EXPIRED((byte)0);

  private final static Map<Byte, AuctionStatus> BY_ID = new HashMap<>();

  static {
    for (AuctionStatus status : values()) {
      BY_ID.put(status.databaseId, status);
    }
  }

  @Getter
  private byte databaseId;

  private AuctionStatus(final byte databaseId) {
    this.databaseId = databaseId;
  }

  public static AuctionStatus byId(byte databaseId) {
    if (BY_ID.containsKey(databaseId)) {
      return BY_ID.get(databaseId);
    }

    return ACTIVE;
  }

}
