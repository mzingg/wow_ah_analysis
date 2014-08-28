package mrwolf.dbimport.model;

import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

public enum Faction {

  NO_FACTION((byte) 0), ALLIANCE((byte) 1), HORDE((byte) 2), NEUTRAL((byte) 3), END_OF_FILE((byte) 99), END_OF_IMPORT((byte) 199);
  private final static Map<String, Faction> lookupTable = new HashMap<>();

  static {
    for (Faction faction : values()) {
      lookupTable.put(faction.name().toLowerCase(), faction);
    }
  }

  @Getter
  private byte databaseId;

  private Faction(final byte databaseId) {
    this.databaseId = databaseId;
  }

  public static Faction lookup(@NonNull String key) {
    String unifiedKey = key.trim().toLowerCase();
    if (lookupTable.containsKey(unifiedKey)) {
      return lookupTable.get(unifiedKey);
    }

    return NO_FACTION;
  }
}
