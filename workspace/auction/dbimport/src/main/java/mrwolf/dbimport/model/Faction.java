package mrwolf.dbimport.model;

import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

public enum Faction {

  NO_FACTION((byte) 0), ALLIANCE((byte) 1), HORDE((byte) 2), NEUTRAL((byte) 3), END_OF_FILE((byte) 99), END_OF_IMPORT((byte) 199);
  private final static Map<String, Faction> BY_NAME = new HashMap<>();
  private final static Map<Byte, Faction> BY_ID = new HashMap<>();

  static {
    for (Faction faction : values()) {
      BY_ID.put(faction.databaseId, faction);
      BY_NAME.put(faction.name().toLowerCase(), faction);
    }
  }

  @Getter
  private byte databaseId;

  private Faction(final byte databaseId) {
    this.databaseId = databaseId;
  }

  public static Faction lookup(@NonNull String key) {
    String unifiedKey = key.trim().toLowerCase();
    if (BY_NAME.containsKey(unifiedKey)) {
      return BY_NAME.get(unifiedKey);
    }

    return NO_FACTION;
  }

  public static Faction byId(byte databaseId) {
    if (BY_ID.containsKey(databaseId)) {
      return BY_ID.get(databaseId);
    }

    return NO_FACTION;
  }
}
