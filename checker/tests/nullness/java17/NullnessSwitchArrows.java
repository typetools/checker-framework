// @below-java14-jdk-skip-test
public class NullnessSwitchArrows {
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
        Object o;
        Day day = Day.WEDNESDAY;
        switch (day) {
            case MONDAY, FRIDAY, SUNDAY -> o = "hello";
            case TUESDAY -> o = null;
            case THURSDAY, SATURDAY -> o = "hello";
            case WEDNESDAY -> o = "hello";
            default -> throw new IllegalStateException("Invalid day: " + day);
        }

        // :: error: (dereference.of.nullable)
        o.toString();
    }

    void method2() {
        Object o;
        Day day = Day.WEDNESDAY;
        switch (day) {
            case MONDAY, FRIDAY, SUNDAY -> o = "hello";
            case TUESDAY -> o = "hello";
            case THURSDAY, SATURDAY -> o = "hello";
            case WEDNESDAY -> o = "hello";
            default -> throw new IllegalStateException("Invalid day: " + day);
        }

        o.toString();
    }

    void method2b() {
        Object o;
        Day day = Day.WEDNESDAY;
        switch (day) {
            case MONDAY, FRIDAY, SUNDAY:
                o = "hello";
                break;
            case TUESDAY:
                o = "hello";
                break;
            case THURSDAY, SATURDAY:
                o = "hello";
                break;
            case WEDNESDAY:
                o = "hello";
                break;
            default:
                throw new IllegalStateException("Invalid day: " + day);
        }

        o.toString();
    }

    void method3() {
        Object o;
        Day day = Day.WEDNESDAY;
        switch (day) {
            case MONDAY, FRIDAY, SUNDAY -> o = "hello";
            case TUESDAY -> o = "hello";
            case THURSDAY, SATURDAY -> o = "hello";
            case WEDNESDAY -> o = "hello";
            default -> o = "hello";
        }

        o.toString();
    }
}
