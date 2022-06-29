// @below-java17-jdk-skip-test
public class NullnessSwitchExpressions {
  public enum Day {
    SUNDAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY;
  }

  void method1() {
    Day day = Day.WEDNESDAY;
    Object o =
        switch (day) {
          case MONDAY, FRIDAY, SUNDAY -> "hello";
          case TUESDAY -> null;
          case THURSDAY, SATURDAY -> "hello";
          case WEDNESDAY -> "hello";
          default -> throw new IllegalStateException("Invalid day: " + day);
        };

    // :: error: (dereference.of.nullable)
    o.toString();
  }

  void method2() {
    Day day = Day.WEDNESDAY;
    Object o =
        switch (day) {
          case MONDAY, FRIDAY, SUNDAY -> "hello";
          case TUESDAY -> "hello";
          case THURSDAY, SATURDAY -> "hello";
          case WEDNESDAY -> "hello";
          default -> throw new IllegalStateException("Invalid day: " + day);
        };

    o.toString();
  }

  void method3() {
    Day day = Day.WEDNESDAY;
    Object o =
        switch (day) {
          case MONDAY, FRIDAY, SUNDAY -> "hello";
          case TUESDAY -> "hello";
          case THURSDAY, SATURDAY -> {
            String s = null;
            if (day == Day.THURSDAY) {
              s = "hello";
              s.toString();
            }
            yield s;
          }
          case WEDNESDAY -> "hello";
          default -> throw new IllegalStateException("Invalid day: " + day);
        };

    // :: error: (dereference.of.nullable)
    o.toString();
  }
}
