// Test case for https://github.com/typetools/checker-framework/issues/5412

// @below-java17-jdk-skip-test

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
}
