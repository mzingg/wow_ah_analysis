package mrwolf.dbimport.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum AuctionDuration {

  VERY_LONG((byte)4, 1728000), // 48h
  LONG((byte)3, 432000), // 12h
  MEDIUM((byte)2, 72000), // 2h
  SHORT((byte)1, 18000); // 30m

  @Getter
  private long offsetTime;

  private final static Map<Byte, AuctionDuration> BY_ID = new HashMap<>();
  private final static Map<String, AuctionDuration> BY_NAME = new HashMap<>();

  static {
    for (AuctionDuration duration : values()) {
      BY_ID.put(duration.databaseId, duration);
      BY_NAME.put(duration.name().toLowerCase(), duration);
    }
  }

  @Getter
  private byte databaseId;

  private AuctionDuration(final byte databaseId, final long offsetTime) {
    this.databaseId = databaseId;
    this.offsetTime = offsetTime;
  }

  public static AuctionDuration lookup(final String key) {
    final String unifiedKey = key.trim().toLowerCase();
    if (BY_NAME.containsKey(unifiedKey)) {
      return BY_NAME.get(unifiedKey);
    }

    return null;
  }

  public static AuctionDuration byId(byte databaseId) {
    if (BY_ID.containsKey(databaseId)) {
      return BY_ID.get(databaseId);
    }

    return VERY_LONG;
  }
}
