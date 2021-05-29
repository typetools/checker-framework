import org.checkerframework.checker.tainting.qual.PolyTainted;

public class PolyReceivers {

  static class MyClass {
    public void start(@PolyTainted MyClass this) {}
  }

  PolyReceivers(int i, Runnable... runnables) {}

  PolyReceivers(Consumer<String> consumer) {
    consumer.consume("hello"); // Use lambda as a constructor argument
  }

  interface Top {
    public void consume(String s);
  }

  interface Sub extends Top {
    public default void otherMethod() {}
  }

  interface Consumer<T> {
    void consume(T t);
  }

  void varargs(Runnable... runnables) {}

  public static void consumeStr(String str) {}

  public static void consumeStr2(String str) {}

  <E extends Consumer<String>> void context(E e, Sub s) {
    new PolyReceivers(PolyReceivers::consumeStr);

    Consumer<String> cs1 = (false) ? PolyReceivers::consumeStr2 : PolyReceivers::consumeStr;
    Consumer<String> cs2 = (false) ? e : PolyReceivers::consumeStr;
    Top t = (false) ? s : PolyReceivers::consumeStr;

    new PolyReceivers(42, new MyClass()::start); // Use lambda as a constructor argument
    varargs(new MyClass()::start, new MyClass()::start); // Use lambda in a var arg list of method
  }
}
