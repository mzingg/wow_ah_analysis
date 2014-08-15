package mrwolf.dbimport.model;

import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public enum Faction {

  ALLIANCE((byte) 1), HORDE((byte) 2), NEUTRAL((byte) 3), SPECIAL((byte) 99);

  @Getter
  private byte databaseId;

  private final static Map<String, Faction> lookupTable = new HashMap<>();
  private final static Map<Byte, Faction> lookupTableById = new HashMap<>();

  static {
    for (Faction faction : values()) {
      lookupTable.put(faction.name().toLowerCase(), faction);
      lookupTableById.put(faction.getDatabaseId(), faction);
    }
  }

  private Faction(final byte databaseId) {
    this.databaseId = databaseId;
  }

  public static Faction lookup(final String key) {
    if (StringUtils.isEmpty(key)) {
      return null;
    }

    final String unifiedKey = key.trim().toLowerCase();
    if (!lookupTable.containsKey(unifiedKey)) {
      return null;
    }

    return lookupTable.get(unifiedKey);
  }

  public static Faction lookupById(final byte id) {
    if (id <= 0) {
      return null;
    }

    if (!lookupTableById.containsKey(id)) {
      return null;
    }

    return lookupTableById.get(id);
  }
}
