// A test that checks that parameterized classes in the JDK don't cause false positives
// when they are used with an @MustCall-annotated class.

import java.util.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a")
class ListOfMustCall {
  static void test(ListOfMustCall lm) {
    List<ListOfMustCall> l = new ArrayList<>();
    // add(E e) takes an object of the type argument's type
    l.add(lm);
    // remove(Object e) takes an object
    l.remove(lm);
  }

  static void test2(ListOfMustCall lm) {
    List<@MustCall("a") ListOfMustCall> l = new ArrayList<>();
    l.add(lm);
    l.remove(lm);
  }

  static void test3(ListOfMustCall lm) {
    List<? extends ListOfMustCall> l = new ArrayList<>();
    l.remove(lm);
  }

  static void test4(ListOfMustCall lm) {
    List<? extends @MustCall("a") ListOfMustCall> l = new ArrayList<>();
    l.remove(lm);
  }
}
