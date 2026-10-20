// Test case for https://github.com/typetools/checker-framework/issues/2090

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.units.qual.*;
import org.checkerframework.framework.qual.SubtypeOf;

public class Issue2090 {
  /** Fraction. */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
  @SubtypeOf(UnknownUnits.class)
  public static @interface frac {
    Prefix value() default Prefix.one;
  }

  /** Percent. */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
  @SubtypeOf(UnknownUnits.class)
  @UnitsMultiple(quantity = frac.class, prefix = Prefix.centi)
  public static @interface pct {}

  public @pct Float test(final String param) {
    if (param.equals("foo")) {
      return null;
    }
    return toFrac(3f);
  }

  @pct
  Float toFrac(float number) {
    return number;
  }
}
