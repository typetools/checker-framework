// Tests the feature that allows checkers to add annotations onto a class
// declaration, which is used for custom inference logic. There is custom
// logic in the AinferTestChecker specifically for classes with the name
// "IShouldBeSibling1" that infers an @Sibling1 annotation for them.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

@SuppressWarnings("super.invocation") // Intentional.
public class IShouldBeSibling1 {
  public static void test(IShouldBeSibling1 s1) {
    // :: warning: (assignment)
    @AinferSibling1 IShouldBeSibling1 s = s1;
  }
}
