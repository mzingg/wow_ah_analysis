package ch.mrwolf.wow.dbimport.model;

public class TimeConstraint {

  private final int startHour;

  private final int lengthHours;

  public TimeConstraint(final int startHour, final int lengthHours) {
    this.startHour = startHour;
    this.lengthHours = lengthHours;
  }

}
