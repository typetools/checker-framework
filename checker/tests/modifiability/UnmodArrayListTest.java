import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;

public class UnmodArrayListTest {

  void testUnmodifiableCastEscape() {
    ArrayList<String> myList = new ArrayList<>();
    myList.add("hello");

    // Checker correctly infers @Unmodifiable here.
    @Unmodifiable List<String> view = Collections.unmodifiableList(myList);

    ArrayList<String> backToMod = (ArrayList<String>) view;

    // :: error: [method.invocation]
    backToMod.add("Boom");
  }

  void test(@Modifiable ArrayList<String> tests) {
    tests.add("a");
  }

  void test2(ArrayList<String> tests) {
    // :: error: [method.invocation]
    tests.add("a");
  }
}
