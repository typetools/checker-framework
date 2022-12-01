// Test case for https://github.com/typetools/checker-framework/issues/5412

// @below-java17-jdk-skip-test

import org.checkerframework.checker.nullness.qual.NonNull;

enum MyEnum {
  VAL1,
  VAL2,
  VAL3
}

class SwitchTestExhaustive {
  public String foo1(MyEnum b) {
    final var s =
        switch (b) {
          case VAL1 -> "1";
          case VAL2 -> "2";
          case VAL3 -> "3";
        };
    return s;
  }

  public String foo2(MyEnum b) {
    final var s =
        switch (b) {
          case VAL1 -> "1";
          case VAL2 -> "2";
          case VAL3 -> "3";
          default -> throw new RuntimeException();
        };
    return s;
  }

  public String foo3(MyEnum b) {
    return switch (b) {
      case VAL1 -> "1";
      case VAL2 -> "2";
      case VAL3 -> "3";
    };
  }

  public String foo4(MyEnum b) {
    String aString = "foo";
    switch (b) {
      case VAL1:
        return "a";
      case VAL2:
        return "b";
      case VAL3:
        return "c";
      default:
        System.out.println(aString.hashCode());
        throw new Error();
    }
  }

  // TODO: test fallthrough to the default: case.
  public @NonNull String foo5(MyEnum b) {
    String aString = "foo";
    switch (b) {
      case VAL1:
        return aString;
      case VAL2:
        return aString;
      case VAL3:
      default:
        return aString;
    }
  }
}
