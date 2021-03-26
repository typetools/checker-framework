import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;

public class EmptyAnnotationArgument {

  // :: warning: (no.values.given)
  void mArray(int @ArrayLen({}) [] a) {}
  // :: warning: (no.values.given)
  void mBool(@BoolVal({}) boolean arg) {}
  // :: warning: (no.values.given)
  void mDouble(@DoubleVal({}) double arg) {}
  // :: warning: (no.values.given)
  void mInt(@IntVal({}) int arg) {}
  // :: warning: (no.values.given)
  void mString(@StringVal({}) String arg) {}
}
