// Test case for https://tinyurl.com/cfissue/3207

import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.common.value.qual.MinLen;

public class Issue3207 {

  void m(int @MinLen(1) [] arr) {
    @LTLengthOf("arr") int j = 0;
  }

  void m2(int @MinLen(1) [] @MinLen(1) [] arr) {
    @LTLengthOf("arr[0]") int j = 0;
  }

  void m3(int @MinLen(1) [] @MinLen(1) [] arr) {
    int @MinLen(1) [] arr0 = arr[0];
    @LTLengthOf("arr0") int j = 0;
  }
}
