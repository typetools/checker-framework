// Test case for Issue 132:
// https://github.com/typetools/checker-framework/issues/132
// Method type argument inference test case.
public class GenericTest2 {
  public interface Data<S> {}

  public interface DataUtils {
    <T> Data<T> makeData(T value);
  }

  public <U> void test(U value, DataUtils utils) {
    Data<? extends U> data = utils.makeData(value);
  }
}
