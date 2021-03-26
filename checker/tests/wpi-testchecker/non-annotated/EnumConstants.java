// @skip-test

// Check that types on enum constants can be inferred.
// This test doesn't succeed for either kind of WPI,
// because WPI doesn't learn anything about enum constants
// from how they're used. They also cannot be assigned to,
// so there's no way for WPI to learn their types.

import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1;

public class EnumConstants {
  enum MyEnum {
    ONE,
    TWO;
  }

  void requiresS1(@Sibling1 MyEnum e) {}

  void test() {
    // :: warning: argument.type.incompatible
    requiresS1(MyEnum.ONE);
  }
}
