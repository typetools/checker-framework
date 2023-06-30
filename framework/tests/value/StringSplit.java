import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.MinLen;

public class StringSplit {

  void needsALR1(String @ArrayLenRange(from = 1) [] arg) {}

  void g(String compiler) {
    needsALR1(compiler.trim().split(" +"));
  }

  void g2(String compiler) {
    needsALR1(mySplit(compiler.trim(), " +"));
  }

  String @MinLen(1) [] mySplit(String receiver, String regex) {
    return null;
  }
}
