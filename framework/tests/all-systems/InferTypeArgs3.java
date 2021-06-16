import java.util.Arrays;
import java.util.HashSet;

public class InferTypeArgs3 {
  @SuppressWarnings({"deprecation", "removal", "cast.unsafe.constructor.invocation"})
  void test() {
    java.util.Arrays.asList(new Integer(1), "");
  }

  void foo() {
    new HashSet<>(Arrays.asList(new Object()));
    new HashSet<Object>(Arrays.asList(new Object())) {};
  }
}
