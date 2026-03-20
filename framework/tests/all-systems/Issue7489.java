import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("all") // Just check for crashes.
// @below-java11-jdk-skip-test
public class Issue7489 {
  static List<SubClass> filter(final Collection<ClassA> collection) {
    return collection.stream()
        .map(x -> (SubClass) x)
        .sorted(Comparator.comparing(SubClass::getX).thenComparing(SubClass::getY))
        .toList();
  }

  public class ClassA {
    public Long getX() {
      return 0L;
    }
  }

  public class SubClass extends ClassA {
    public String getY() {
      throw new RuntimeException();
    }
  }
}
