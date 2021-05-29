import java.util.Arrays;
import org.checkerframework.common.value.qual.MinLen;

public class ArraysSort {

  void sortInt(int @MinLen(10) [] nums) {
    // Checks the correct handling of the toIndex parameter
    Arrays.sort(nums, 0, 10);
    // :: error: (argument)
    Arrays.sort(nums, 0, 11);
  }
}
