// Test case for crash that occurred during WPI when calling isSuper inappropriately.

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
