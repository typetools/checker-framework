import org.checkerframework.common.aliasing.qual.Unique;

public class ThrowTest {

  void foo() throws Exception {
    @Unique Exception e = new Exception();
    // :: error: (unique.leaked)
    throw e;
  }

  void bar() throws Exception {
    throw new Exception();
  }
}
