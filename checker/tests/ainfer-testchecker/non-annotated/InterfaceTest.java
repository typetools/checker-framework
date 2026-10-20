// checks that types can be inferred for constants defined in interfaces

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

@SuppressWarnings("cast.unsafe")
public interface InterfaceTest {
  public String toaster = getAinferSibling1();

  public static @AinferSibling1 String getAinferSibling1() {
    return (@AinferSibling1 String) "foo";
  }

  default void requireAinferSibling1(@AinferSibling1 String x) {}

  default void testX() {
    requireAinferSibling1(toaster);
  }
}
