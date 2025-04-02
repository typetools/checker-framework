import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

// Examples taken from
// https://docs.oracle.com/en/java/javase/24/language/unnamed-variables-and-patterns.html
// @below-java22-jdk-skip-test

// None of the WPI formats supports the new Java 22 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
@SuppressWarnings("all") // Just check for crashes.
public class UnnamedVars {

  void example1() {
    int[] orderIDs = {34, 45, 23, 27, 15};
    int total = 0;
    for (int _ : orderIDs) {
      total++;
    }
    System.out.println("Total: " + total);
  }

  record Caller(String phoneNumber) {}

  static List<Object> everyFifthCaller(Queue<Caller> q, int prizes) {
    var winners = new ArrayList<>();
    try {
      while (prizes > 0) {
        Caller _ = q.remove();
        Caller _ = q.remove();
        Caller _ = q.remove();
        Caller _ = q.remove();
        winners.add(q.remove());
        prizes--;
      }
    } catch (NoSuchElementException _) {
      // Do nothing
    }
    return winners;
  }

  static void doesFileExist(String path) {
    try (var _ = new FileReader(path)) {
      // Do nothing
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void example2() {
    Function<String, Integer> sideEffect =
        s -> {
          System.out.println(s);
          return 0;
        };

    for (int i = 0, _ = sideEffect.apply("Starting for-loop"); i < 10; i++) {
      System.out.println(i);
    }
  }

  static void stringLength(String s) {
    int len = 0;
    for (char _ : s.toCharArray()) {
      len++;
    }
    System.out.println("Length of " + s + ": " + len);
  }

  static void validateNumber(String s) {
    try {
      int i = Integer.parseInt(s);
      System.out.println(i + " is valid");
    } catch (NumberFormatException _) {
      System.out.println(s + " isn't valid");
    }
  }

  record Point(double x, double y) {}

  record UniqueRectangle(String id, Point upperLeft, Point lowerRight) {}

  static Map<String, String> getIDs(List<UniqueRectangle> r) {
    return r.stream().collect(Collectors.toMap(UniqueRectangle::id, _ -> "NODATA"));
  }

  enum Color {
    RED,
    GREEN,
    BLUE
  }

  record ColoredPoint(Point p, Color c) {}

  double getDistance(Object obj1, Object obj2) {
    if (obj1 instanceof ColoredPoint(Point p1, _) && obj2 instanceof ColoredPoint(Point p2, _)) {
      return java.lang.Math.sqrt(
          java.lang.Math.pow(p2.x - p1.x, 2) + java.lang.Math.pow(p2.y - p1.y, 2));
    } else {
      return -1;
    }
  }

  double getDistance2(Object obj1, Object obj2) {
    if (obj1 instanceof ColoredPoint(Point p1, Color _)
        && obj2 instanceof ColoredPoint(Point p2, Color _)) {
      return java.lang.Math.sqrt(
          java.lang.Math.pow(p2.x - p1.x, 2) + java.lang.Math.pow(p2.y - p1.y, 2));
    } else {
      return -1;
    }
  }

  sealed interface Employee permits Salaried, Freelancer, Intern {}

  record Salaried(String name, long salary) implements Employee {}

  record Freelancer(String name) implements Employee {}

  record Intern(String name) implements Employee {}

  void printSalary(Employee b) {
    switch (b) {
      case Salaried r -> System.out.println("Salary: " + r.salary());
      case Freelancer _ -> System.out.println("Other");
      case Intern _ -> System.out.println("Other");
    }
  }

  void printSalary2(Employee b) {
    switch (b) {
      case Salaried r -> System.out.println("Salary: " + r.salary());
      case Freelancer _, Intern _ -> System.out.println("Other");
    }
  }
}
