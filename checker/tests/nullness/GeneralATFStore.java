// Test case for a mysterious compilation failure.
// The underlying reason was that the GeneralATF
// tried storing defaulted declaration annotations.

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface FailAnno {
  String value();

  boolean flag() default false;
}

class Fail {
  @FailAnno(value = "Fail", flag = true)
  String f = "fail";

  Object x = Fail.class;
}
