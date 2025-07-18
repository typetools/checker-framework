import java.util.ArrayList;
import org.checkerframework.checker.nonempty.qual.EnsuresNonEmptyIf;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

class EnsuresNonEmptyIfTest {

  @EnsuresNonEmptyIf(result = true, expression = "#1")
  boolean m1(ArrayList<String> l1) {
    try {
      l1.add("foo");
      return true;
    } catch (Exception e) {
      // As per the JDK documentation for Collections, an exception is thrown when adding to a
      // collection fails
      return false;
    }
  }

  void m2(@NonEmpty ArrayList<String> l1) {}

  void test(ArrayList<String> l1) {
    // m2 requires a @NonEmpty collection, l1 has type @UnknownNonEmpty
    // :: error: (argument)
    m2(l1);

    if (!m1(l1)) {
      // :: error: (argument)
      m2(l1);
    } else {
      m2(l1); // OK
    }
  }
}
