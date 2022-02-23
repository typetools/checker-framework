// Test case for issue #3711: https://github.com/typetools/checker-framework/issues/3710

public class Issue3710 {
  int returnIntWithLocalVariable(char c) {
    int i = c;
    return i;
  }

  long returnLongWithLocalVariable(char c) {
    long l = c;
    return l;
  }

  int returnIntWithoutLocalVariable(char c) {
    return c;
  }

  long returnLongWithoutLocalVariable(char c) {
    return c;
  }
}
