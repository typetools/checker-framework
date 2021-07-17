// checks that types can be inferred for constants defined in interfaces

import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;

@SuppressWarnings("cast.unsafe")
interface InterfaceTest {
  public String toaster = getSibling1();

  public static @Sibling1 String getSibling1() {
    return (@Sibling1 String) "foo";
  }

  default void requireSibling1(@Sibling1 String x) {}

  default void testX() {
    requireSibling1(toaster);
  }
}
