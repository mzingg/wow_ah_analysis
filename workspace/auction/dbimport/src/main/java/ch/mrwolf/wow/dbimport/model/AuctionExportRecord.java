package ch.mrwolf.wow.dbimport.model;

import java.util.Calendar;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.Id;

public class AuctionExportRecord {

  @Id
  private String id;

  @Getter
  @Setter
  private String realm;

  @Getter
  @Setter
  private Faction faction;

  @Getter
  @Setter
  private int auctionId;

  @Getter
  @Setter
  private int itemId;

  @Getter
  @Setter
  private String owner;

  @Getter
  @Setter
  private Calendar snapshotTime;

  @Getter
  @Setter
  private String snapshotHash;

  @Getter
  @Setter
  private long bidAmount;

  @Getter
  @Setter
  private long buyoutAmount;

  @Getter
  @Setter
  private int quantity;

  @Getter
  @Setter
  private AuctionDuration timeLeft;

  @Getter
  @Setter
  private int petSpeciesId;

  @Getter
  @Setter
  private int petBreedId;

  @Getter
  @Setter
  private int petLevel;

  @Getter
  @Setter
  private int petQualityId;

  @Getter
  @Setter
  private int rand;

  @Getter
  @Setter
  private int seed;
}
