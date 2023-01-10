import org.checkerframework.checker.calledmethods.qual.RequiresCalledMethods;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.dataflow.qual.Deterministic;

public class Issue5402 {

  @Deterministic
  public @This Issue5402 bar() {
    return this;
  }
  @Deterministic
  public @This Issue5402 bar(String s) {
    return this;
  }

  @RequiresCalledMethods( value = "this", methods = "bar" )
  public void baz() {}


  public static void test() {
    final Issue5402 foo = new Issue5402();
    foo.bar().baz();
    new Issue5402().bar().baz();
     final Issue5402 foo3 = new Issue5402();
        foo3.bar(String.format("")).baz();
  }

}
