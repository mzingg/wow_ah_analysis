package mrwolf.dbimport.model;

import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public enum AuctionDuration {

  VERY_LONG(172800000), // 48h
  LONG(43200000), // 12h
  MEDIUM(7200000), // 2h
  SHORT(1800000); // 30m

  @Getter
  private long offsetTime;

  private final static Map<String, AuctionDuration> lookupTable = new HashMap<>();

  static {
    for (AuctionDuration duration : values()) {
      lookupTable.put(duration.name().toLowerCase(), duration);
    }
  }

  private AuctionDuration(final long offsetTime) {
    this.offsetTime = offsetTime;
  }

  public static AuctionDuration lookup(final String key) {
    if (StringUtils.isEmpty(key)) {
      return null;
    }

    final String unifiedKey = key.trim().toLowerCase();
    if (!lookupTable.containsKey(unifiedKey)) {
      return null;
    }

    return lookupTable.get(unifiedKey);
  }

}
