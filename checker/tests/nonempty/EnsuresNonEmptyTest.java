import java.util.ArrayList;
import org.checkerframework.checker.optional.qual.EnsuresNonEmpty;
import org.checkerframework.checker.optional.qual.NonEmpty;

class EnsuresNonEmptyTest {

  @EnsuresNonEmpty("#1")
  void m1(ArrayList<String> l1) {
    l1.add("foo");
  }

  void m2(@NonEmpty ArrayList<String> l1) {}

  void test(ArrayList<String> l1) {
    // m2 requires a @NonEmpty collection, l1 has type @UnknownNonEmpty
    // :: error: (argument)
    m2(l1);

    m1(l1);
    m2(l1); // OK
  }
}
