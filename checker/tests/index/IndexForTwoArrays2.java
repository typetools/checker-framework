// Test case for issue #34: https://github.com/kelloggm/checker-framework/issues/34

public class IndexForTwoArrays2 {

  public boolean equals(int[] da1, int[] da2) {
    if (da1.length != da2.length) {
      return false;
    }
    int k = 0;

    for (int i = 0; i < da1.length; i++) {
      if (da1[i] != da2[i]) {
        return false;
      }
    }
    return true;
  }
}
