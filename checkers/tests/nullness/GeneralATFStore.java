// Test case for a mysterious compilation failure
// when compiling plume-lib.
// The underlying reason was that the GeneralATF
// tried storing defaulted declaration annotations.

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface FailAnno {
  String value();
  boolean flag() default false;
}

class Fail {
  @FailAnno(value="Fail", flag=true)
  String f;

  Object x = Fail.class;
}
