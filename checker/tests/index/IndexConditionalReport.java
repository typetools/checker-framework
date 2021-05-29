// test case for https://github.com/typetools/checker-framework/issues/2345

public class IndexConditionalReport {

  public int getI(int len) {
    for (int i = 0; i < len; i++) {
      if (false) {
        return i == 0 ? -1 : i; // unexpected error issued here
      }
    }
    return -1;
  }
}
