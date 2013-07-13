package ch.mrwolf.wow.dbimport.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

public enum AuctionDuration {

  SHORT, MEDIUM, LONG, VERY_LONG;

  private final static Map<String, AuctionDuration> lookup = new HashMap<String, AuctionDuration>();
  static {
    for (AuctionDuration value : values()) {
      lookup.put(value.name().toLowerCase(), value);
    }
  }

  public static AuctionDuration lookUp(final String name) {
    if (!StringUtils.isEmpty(name) && lookup.containsKey(name.toLowerCase())) {
      return lookup.get(name.toLowerCase());
    }

    return null;
  }

}
