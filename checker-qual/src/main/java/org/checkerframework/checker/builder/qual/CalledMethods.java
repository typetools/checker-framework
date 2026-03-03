package org.checkerframework.checker.builder.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A deprecated variant of {@link org.checkerframework.checker.calledmethods.qual.CalledMethods}.
 *
 * <p>Lombok outputs this annotation. This annotation could be marked as deprecated, but that causes
 * extra warnings when processing delombok'd code.
 *
 * @checker_framework.manual #called-methods-checker Called Methods Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface CalledMethods {
  /**
   * The names of methods that have definitely been called.
   *
   * @return the names of methods that have definetely been called
   */
  String[] value();
}
