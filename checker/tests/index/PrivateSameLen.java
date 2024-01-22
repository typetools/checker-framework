import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.dataflow.qual.Pure;

public class PrivateSameLen {

  @Pure
  private @SameLen("#1") String getSameLenString(String in) {
    return in;
  }

  private void test() {
    String in = "foo";
    @SameLen("this.getSameLenString(in)") String myStr = getSameLenString(in);
  }
}
