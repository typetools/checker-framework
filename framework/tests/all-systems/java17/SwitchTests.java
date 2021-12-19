// A "case rule" uses the "->" syntax.
// A "case label" uses the ":" syntax.
// The two cannot be mixed.

// @below-java14-jdk-skip-test

import java.time.DayOfWeek;

@SuppressWarnings("all") // Just check for crashes.
class SwitchTests {

    String statementRule(DayOfWeek day) {
        var today = "";
        switch (day) {
            case SATURDAY, SUNDAY -> today = "weekend";
            case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> today = "workday";
            default -> throw new IllegalArgumentException("Invalid day: " + day.name());
        }
        return today;
    }

    String expressionRule1(DayOfWeek day) {
        var today =
                switch (day) {
                    case SATURDAY, SUNDAY -> "weekend";
                    case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "workday";
                    default -> throw new IllegalArgumentException("Invalid day: " + day.name());
                };

        return today;
    }

    int expressionRule2(DayOfWeek day) {
        int numLetters =
                switch (day) {
                    case MONDAY, FRIDAY, SUNDAY -> 6;
                    case TUESDAY -> 7;
                    case THURSDAY, SATURDAY -> 8;
                    case WEDNESDAY -> 9;
                };
        return numLetters;
    }

    String expressionLabel(DayOfWeek day) {
        var today =
                switch (day) {
                    case SATURDAY, SUNDAY:
                        yield "weekend";
                    case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY:
                        yield "workday";
                    default:
                        throw new IllegalArgumentException("Invalid day: " + day.name());
                };

        return today;
    }

    String expressionLabelBlock(DayOfWeek day) {
        var today =
                switch (day) {
                    case SATURDAY, SUNDAY:
                        yield "weekend";
                    case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY:
                        {
                            var kind = "workday";
                            yield kind;
                        }
                    default:
                        {
                            var kind = day.name();
                            System.out.println(kind);
                            throw new IllegalArgumentException("Invalid day: " + kind);
                        }
                };

        return today;
    }

    String expressionRuleYield(DayOfWeek day) {
        var today =
                switch (day) {
                    case SATURDAY, SUNDAY -> "weekend";
                    case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "workday";
                    default -> {
                        var len = day.name().length();
                        yield len;
                    }
                };
        return today.toString();
    }

    void fallthrough1(int myInt) {
        switch (myInt) {
            case 1 -> System.out.println("One");
            case 2 -> {
                System.out.println("Two");
                System.out.println("No fall through");
            }
            default -> System.out.println("Something else");
        }
    }

    void fallthrough2(int myInt) {
        switch (myInt) {
            case 1:
                System.out.println("One");
                break;
            case 2:
                System.out.println("Two");
                System.out.println("Falling through");
            default:
                System.out.println("Something else");
        }
    }

    void fallthrough3(int myInt) {
        switch (myInt) {
            case 1, 2, 3:
                System.out.println("One, two or three");
                break;
            default:
                System.out.println("Something else");
        }
    }

    void fallthrough4(int myInt) {
        switch (myInt) {
            case 1:
                System.out.print("Hello ");
                // Fall through to next case
            case 2:
                System.out.println("World");
                break;
            default:
                System.out.println("Not executed");
        }
    }

    int multipleLabels(int month, int year) {
        int numDays = 0;

        switch (month) {
            case 1, 3, 5, 7, 8, 10, 12 -> {
                numDays = 31;
            }
            case 4, 6, 9, 11 -> {
                numDays = 30;
            }
            case 2 -> {
                if (((year % 4 == 0) && !(year % 100 == 0)) || (year % 400 == 0)) {
                    numDays = 29;
                } else {
                    numDays = 28;
                }
            }
            default -> {
                System.out.println("Invalid month.");
            }
        }
        return numDays;
    }
}
