// Test case for Issue 132:
// http://code.google.com/p/checker-framework/issues/detail?id=132
// Method type argument inference test case.
public class GenericTest2 {
  public interface Data<S> {
  }

  public interface DataUtils {
    <T> Data<T> makeData(T value);
  }

  public <U> void test(U value, DataUtils utils) {
    Data<? extends U> data = utils.makeData(value);
  }
}
