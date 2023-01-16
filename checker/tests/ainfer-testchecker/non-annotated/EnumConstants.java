// @skip-test

// Check that types on enum constants can be inferred.  This test doesn't succeed for either kind of
// WPI, because WPI doesn't learn anything about enum constants from how they're used. They also
// cannot be assigned to, so there's no way for WPI to learn their types.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

public class EnumConstants {
  enum MyEnum {
    ONE,
    TWO;
  }

  void requiresS1(@AinferSibling1 MyEnum e) {}

  void test() {
    // :: warning: (argument)
    requiresS1(MyEnum.ONE);
  }
}
