// test case for https://github.com/typetools/checker-framework/issues/3422

import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1;

public class MultidimensionalAnnotatedArray {
  boolean[][] field = getArray();

  public boolean[] @Sibling1 [] getArray() {
    return null;
  }
}
