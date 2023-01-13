// Test case for https://github.com/typetools/checker-framework/issues/5526.

public abstract class LongDouble<T> {

  public abstract T getMaxValue();

  public class LongLongDouble extends LongDouble<Long> {
    @java.lang.Override
    public Long getMaxValue() {
      return Long.MAX_VALUE;
    }
  }

  public class DoubleLongDouble extends LongDouble<Double> {
    @java.lang.Override
    public Double getMaxValue() {
      return Double.MAX_VALUE;
    }
  }
}
