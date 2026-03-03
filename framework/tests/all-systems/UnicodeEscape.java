// Test case for https://github.com/typetools/checker-framework/issues/6631.

public class UnicodeEscape {
  void foo() {
    while (true) {
      System.out.print("Enter an expression like \u005c"1+(2+3)*4;\u005c" :");
    }
  }
}
