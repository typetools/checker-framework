import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.common.value.qual.MinLen;

public class LessThanLenBug {
  public static void m1(int[] shorter) {
    int[] longer = new int[4 * shorter.length];
    // :: error: (assignment.type.incompatible)
    @LTLengthOf("longer") int x = shorter.length;
    int i = longer[x];
  }

  public static void m2(int @MinLen(1) [] shorter) {
    int[] longer = new int[4 * shorter.length];
    @LTLengthOf("longer") int x = shorter.length;
    int i = longer[x];
  }

  public static void main(String[] args) {
    m1(new int[0]);
  }
}
