package mrwolf.dbimport.oldmodel;

import lombok.Getter;

public class TimeConstraint {

  @Getter
  private final int startHour; // NOPMD

  @Getter
  private final int lengthHours; // NOPMD

  public TimeConstraint(final int startHour, final int lengthHours) {
    this.startHour = startHour;
    this.lengthHours = lengthHours;
  }

}
