package org.checkerframework.common.reflection.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This represents a {@link java.lang.Class Class&lt;T&gt;} object where the set of possible values
 * of T is known at compile time. If only one argument is given, then the exact value of T is known.
 * If more than one argument is given, then the value of T is one of those classes.
 *
 * @checker_framework.manual #methodval-and-classval-checkers ClassVal Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnknownClass.class})
public @interface ClassVal {
  /**
   * The name of the type that this Class object represents. The name is a "fully-qualified binary
   * name" ({@link org.checkerframework.checker.signature.qual.FqBinaryName}): a primitive or <a
   * href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-13.html#jls-13.1">binary name</a>,
   * possibly followed by some number of array brackets.
   *
   * @return the name of the type that this Class object represents
   */
  String[] value();
}
