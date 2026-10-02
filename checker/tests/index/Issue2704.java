// Test case for https://github.com/typetools/checker-framework/issues/2704

public class Issue2704 {
  double covariance(final double[] xArray) {
    int length = xArray.length;
    for (int i = 0; i < length; i++) {}
    return ((double) length);
  }
}
