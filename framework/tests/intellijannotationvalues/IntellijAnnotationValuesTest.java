package intellijannotationvalues;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.testchecker.lib.UncheckedByteCode;
import org.checkerframework.framework.testchecker.testaccumulation.qual.TestAccumulation;
import org.checkerframework.framework.testchecker.testaccumulation.qual.TestAccumulationPredicate;

/** An annotation with class-literal element values. */
@Target(ElementType.METHOD)
@interface ClassLiteralValues {
  Class<?> primitive();

  Class<?> noType();

  Class<?> array();
}

/** Tests that annotation element values are read from an {@code annotations.xml} file. */
public class IntellijAnnotationValuesTest {

  void testArrayValue(String s) {
    @TestAccumulation({"alpha", "beta"}) String s1 = s.trim();
    @TestAccumulation({"alpha"}) String s2 = s.trim();
    // :: error: [assignment]
    @TestAccumulation({"gamma"}) String s3 = s.trim();
  }

  void testStringValue(String s) {
    @TestAccumulationPredicate("alpha") String s1 = s.strip();
    @TestAccumulation({"alpha"}) String s2 = s.strip();
    // :: error: [assignment]
    @TestAccumulation({"beta"}) String s3 = s.strip();
  }

  void testBoundedTypeVariable() {
    UncheckedByteCode<Object> lib = new UncheckedByteCode<>();
    @TestAccumulation({"bounded"}) CharSequence s1 = lib.<CharSequence>getI("");
    // :: error: [assignment]
    @TestAccumulation({"other"}) CharSequence s2 = lib.<CharSequence>getI("");
  }
}
