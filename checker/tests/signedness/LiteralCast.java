import java.util.Arrays;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class LiteralCast {

  public static void main(String[] args) {
    testCompile(2);
    // manifest literals are treated as @SignednessGlb
    testCompile(-2);
    // :: error: (argument)
    testCompile((@Signed int) 2);
  }

  public static void testCompile(@Unsigned int x) {
    @Unsigned int[] arr = {1, 2, 3, 4, 5, 56};

    Arrays.fill(arr, x);
    Arrays.fill(arr, (@Unsigned int) Integer.valueOf(-2));
    Arrays.fill(arr, Integer.valueOf(-2));
    Arrays.fill(arr, (@Unsigned int) Integer.valueOf(2));
  }
}
