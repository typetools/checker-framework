import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.*;

public class KeyForValidation {

  // :: error: (expression.unparsable)
  // :: error: (initialization.static.field.uninitialized)
  static @KeyFor("this") Object f;

  // :: error: (initialization.field.uninitialized)
  @KeyFor("this") Object g;

  // :: error: (expression.unparsable)
  void m(@KeyFor("#0") Object p) {}

  // :: error: (expression.unparsable)
  void m2(@KeyFor("#4") Object p) {}

  // OK
  void m3(@KeyFor("#2") Object p, Map m) {}

  // TODO: index for a non-map
  void m4(@KeyFor("#1") Object p, Map m) {}

  // TODO: index with wrong type
  void m4(@KeyFor("#2") String p, Map<Integer, Integer> m) {}

  // :: error: (expression.unparsable)
  // :: error: (initialization.field.uninitialized)
  @KeyFor("INVALID") Object h;

  // :: error: (initialization.field.uninitialized)
  @KeyFor("f") Object i;

  void foo(Object p) {
    // :: error: (expression.unparsable)
    @KeyFor("ALSOBAD") Object j;

    @KeyFor("j") Object k;
    @KeyFor("f") Object l;

    @KeyFor("p") Object o;
  }

  // :: error: (expression.unparsable)
  void foo2(@KeyFor("ALSOBAD") Object o) {}
  // :: error: (expression.unparsable)
  void foo3(@KeyFor("ALSOBAD") Object[] o) {}
  // :: error: (expression.unparsable)
  void foo4(Map<@KeyFor("ALSOBAD") Object, Object> o) {}
  // :: error: (expression.unparsable)
  @KeyFor("ALSOBAD") Object[] foo5() {
    throw new RuntimeException();
  }
  // :: error: (expression.unparsable)
  @KeyFor("ALSOBAD") Object foo6() {
    throw new RuntimeException();
  }
  // :: error: (expression.unparsable)
  Map<@KeyFor("ALSOBAD") Object, Object> foo7() {
    throw new RuntimeException();
  }
  // :: error: (expression.unparsable)
  <@KeyFor("ALSOBAD") T> void foo8() {
    throw new RuntimeException();
  }
  // :: error: (expression.unparsable)
  <@KeyForBottom T extends @KeyFor("ALSOBAD") Object> void foo9() {}
  // :: error: (expression.unparsable)
  void foo10(@KeyFor("ALSOBAD") KeyForValidation this) {}

  // :: error: (expression.unparsable)
  public void test(Set<@KeyFor("BAD") String> keySet) {
    // :: error: (expression.unparsable)
    new ArrayList<@KeyFor("BAD") String>(keySet);
    // :: error: (expression.unparsable)
    List<@KeyFor("BAD") String> list = new ArrayList<>();

    // :: error: (expression.unparsable)
    for (@KeyFor("BAD") String s : list) {}
  }

  // Test static context.

  Object instanceField = new Object();

  // :: error: (expression.unparsable)
  static void bar2(@KeyFor("this.instanceField") Object o) {}
  // :: error: (expression.unparsable)
  static void bar3(@KeyFor("this.instanceField") Object[] o) {}
  // :: error: (expression.unparsable)
  static void bar4(Map<@KeyFor("this.instanceField") Object, Object> o) {}
  // :: error: (expression.unparsable)
  static @KeyFor("this.instanceField") Object[] bar5() {
    throw new RuntimeException();
  }
  // :: error: (expression.unparsable)
  static @KeyFor("this.instanceField") Object bar6() {
    throw new RuntimeException();
  }
  // :: error: (expression.unparsable)
  static Map<@KeyFor("this.instanceField") Object, Object> bar7() {
    throw new RuntimeException();
  }
  // :: error: (expression.unparsable)
  static <@KeyFor("this.instanceField") T> void bar8() {
    throw new RuntimeException();
  }
  // :: error: (expression.unparsable)
  static <@KeyForBottom T extends @KeyFor("this.instanceField") Object> void bar9() {}

  // :: error: (expression.unparsable)
  public static void test2(Set<@KeyFor("this.instanceField") String> keySet) {
    // :: error: (expression.unparsable)
    new ArrayList<@KeyFor("this.instanceField") String>(keySet);
    // :: error: (expression.unparsable)
    new ArrayList<@KeyFor("this.instanceField") String>();

    List<String> list = new ArrayList<>();
    // :: error: (enhancedfor) :: error: (expression.unparsable)
    for (@KeyFor("this.instanceField") String s : list) {}
  }
}
