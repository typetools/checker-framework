package android.support.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.CLASS)
public @interface IntRange {
  long from() default Long.MIN_VALUE;

  long to() default Long.MAX_VALUE;
}
