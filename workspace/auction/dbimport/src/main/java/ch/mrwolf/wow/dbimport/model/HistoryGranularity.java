package ch.mrwolf.wow.dbimport.model;

public enum HistoryGranularity {

  // TODO: Finde Zeiten
  LOWTIME,
  HIGHTIME,

  NIGHT(new TimeConstraint(22, 2), new TimeConstraint(0, 8)), // 22:00-23:59 AND 00:00-07:59
  MORNING(8, 4), // 08:00-11:59
  AFTERNOON(12, 4), // 12:00-15:59
  EVENING(16, 6), // 16:00-21:59

  NIGHT_EARLY(22, 2), // 22:00-23:59
  NIGHT_MID(0, 4), // 00:00-03:59
  NIGHT_LATE(4, 4), // 04:00-07:59
  MORNING_EARLY(8, 2), // 08:00-09:59
  MORNING_LATE(10, 2), // 10:00-11:59
  AFTERNOON_EARLY(12, 2), // 12:00-13:59
  AFTERNOON_LATE(14, 2), // 14:00-15:59
  EVENING_EARLY(16, 2), // 16:00-17:59
  EVENING_MID(18, 2), // 18.00-19:59
  EVENING_LATE(20, 2), // 20.00-21:59

  ZERO(0),
  ONE(1),
  TWO(2),
  THREE(3),
  FOUR(4),
  FIVE(5),
  SIX(6),
  SEVEN(7),
  EIGHT(8),
  NINE(9),
  TEN(10),
  ELEVEN(11),
  TWELF(12),
  THIRTEEN(13),
  FOURTEEN(14),
  FIFTEEN(15),
  SIXTEEN(16),
  SEVENTEEN(17),
  EIGHTTEEN(18),
  NINETEEN(19),
  TWENTY(20),
  TWENTYONE(21),
  TWENTYTWO(22),
  TWENTYTHREE(23);

  private HistoryGranularity(final TimeConstraint... constraints) {

  }

  private HistoryGranularity(final int startHour, final int lengthHours) {
    this(new TimeConstraint(startHour, lengthHours));
  }

  private HistoryGranularity(final int startHour) {
    this(startHour, 1);
  }

}
