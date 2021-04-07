package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is an alias for {@link MustCall} that applies to the type on which it is written
 * and all of its subtypes. It is useful to avoid the need to annotate each subtype with an {@link
 * MustCall} annotation. This annotation may only be written on a class declaration.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface InheritableMustCall {
  /**
   * Methods that must be called, on any expression whose type is annotated.
   *
   * @return methods that must be called
   */
  public String[] value() default {};
}
