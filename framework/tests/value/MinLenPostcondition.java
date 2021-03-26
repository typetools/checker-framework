import org.checkerframework.common.value.qual.*;

public class MinLenPostcondition {
  public void m(String a) {
    if (!a.isEmpty()) {
      char c = a.charAt(0);
    }
  }
}
