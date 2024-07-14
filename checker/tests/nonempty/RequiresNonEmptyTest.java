import java.util.LinkedList;
import java.util.List;
import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.checker.nonempty.qual.RequiresNonEmpty;
import org.checkerframework.dataflow.qual.Pure;

class MyClass {

  List<String> list1 = new LinkedList<>();
  List<String> list2;

  @RequiresNonEmpty("list1")
  @Pure
  void m1() {}

  @RequiresNonEmpty({"list1", "list2"})
  @Pure
  void m2() {}

  @RequiresNonEmpty({"list1", "list2"})
  void m3() {}

  void m4() {}

  void test(@NonEmpty List<String> l1, @NonEmpty List<String> l2) {
    MyClass testClass = new MyClass();

    // At this point, we should have an error since m1 requires that list1 is @NonEmpty, which
    // is not the case here.
    // :: error: (contracts.precondition)
    testClass.m1();

    testClass.list1 = l1;
    testClass.m1(); // OK

    // A call to m2 is still illegal here, since list2 is still @UnknownNonEmpty.
    // :: error: (contracts.precondition)
    testClass.m2();

    testClass.list2 = l2;
    testClass.m2(); // OK

    testClass.m4();

    // No longer OK to call m2, no guarantee that m4() was pure.
    // :: error: (contracts.precondition)
    testClass.m2();
  }
}
