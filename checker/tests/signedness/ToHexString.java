import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class ToHexString {
  void toHexString(int x) {
    Integer.toHexString(x);
  }

  void toHexStringU(@Unsigned int x) {
    Integer.toHexString(x);
  }

  void toHexStringS(@Signed int x) {
    Integer.toHexString(x);
  }
}
