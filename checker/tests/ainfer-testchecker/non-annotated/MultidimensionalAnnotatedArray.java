// test case for https://github.com/typetools/checker-framework/issues/3422

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

public class MultidimensionalAnnotatedArray {
  boolean[][] field = getArray();

  public boolean[] @AinferSibling1 [] getArray() {
    return null;
  }
}
