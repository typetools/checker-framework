// Test case for https://github.com/typetools/checker-framework/issues/5402
// @skip-test until the bug is fixed

import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.calledmethods.qual.RequiresCalledMethods;
import org.checkerframework.common.returnsreceiver.qual.This;

public class Issue5402 {}

class Issue5402_Ok1 {

  public @This Issue5402_Ok1 bar() {
    return this;
  }

  public void baz(@CalledMethods("bar") Issue5402_Ok1 this) {}

  public static void test() {
    final Issue5402_Ok1 foo = new Issue5402_Ok1();
    foo.bar().baz(); // No error
  }
}

class Issue5402_Ok2 {

  public @This Issue5402_Ok2 bar() {
    return this;
  }

  @RequiresCalledMethods(value = "this", methods = "bar")
  public void baz() {}

  public static void test() {
    final Issue5402_Ok2 foo = new Issue5402_Ok2();
    final Issue5402_Ok2 foo1 = foo.bar();
    foo1.baz(); // No error
  }
}

class Issue5402_Bad {

  public @This Issue5402_Bad bar() {
    return this;
  }

  @RequiresCalledMethods(value = "this", methods = "bar")
  public void baz() {}

  public static void test() {
    final Issue5402_Bad foo = new Issue5402_Bad();
    foo.bar().baz(); // Error
  }
}
