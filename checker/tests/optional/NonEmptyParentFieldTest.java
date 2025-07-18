import java.util.List;
import org.checkerframework.checker.nonempty.qual.*;

class NonEmptyParentFieldTest {

  class GrandParent {

    List<Integer> grandParentEmptyList = List.of();
    @NonEmpty List<Integer> grandParentNEList = List.of(1, 2, 3);
  }

  class ParentOne extends GrandParent {

    List<Integer> parentEmptyList = List.of();
    @NonEmpty List<Integer> parentNEList = List.of(1, 2, 3);
  }

  class ParentTwo extends GrandParent {
    // Empty, no fields
  }

  class ChildOne extends ParentOne {

    // Check for fields one level up (direct superclass)
    void m1() {
      // :: error: (method.invocation)
      parentEmptyList.stream().max(Integer::compareTo).get();

      parentNEList.stream().max(Integer::compareTo).get(); // OK
    }

    // Check for field more than one level up (non-direct superclass)
    void m2() {
      // :: error: (method.invocation)
      grandParentEmptyList.stream().max(Integer::compareTo).get();

      grandParentNEList.stream().max(Integer::compareTo).get(); // OK
    }
  }

  class ChildTwo extends ParentTwo {

    // Check for field more than one level up (non-direct superclass)
    void m1() {
      // :: error: (method.invocation)
      grandParentEmptyList.stream().max(Integer::compareTo).get();

      grandParentNEList.stream().max(Integer::compareTo).get(); // OK
    }
  }
}
