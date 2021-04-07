package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.JavaExpression;

/**
 * Indicates that the method resets the must-call obligation on the target (the value argument) to
 * its declaration when it is invoked. Calling a method with this annotation also clears the
 * called-methods store for the target (in the Object Construction Checker).
 *
 * <p>It is an error to call a method annotated by this annotation if the target object is declared
 * as having a non-empty CalledMethods type (i.e. its type is not top in the CM hierarchy).
 *
 * <p>It is an error to fail to write this annotation on any method that (re-)assigns a non-final,
 * owning field with a must-call obligation. It is an error if the target of that annotation is not
 * exactly the value "this".
 *
 * <p>This annotation is trusted, not checked (though it can only add obligations, so the analysis
 * remains conservative).
 *
 * <p>This annotation is repeatable: a programmer may write more than one {@code CreatesObligation}
 * annotation on a single method. If so, the annotations should have different targets.
 *
 * @checker_framework.manual #must-call-checker Must Call Checker
 */
@Target({ElementType.METHOD})
@InheritedAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CreatesObligation.List.class)
public @interface CreatesObligation {

  /**
   * The target of this annotation is stored in this field.
   *
   * @return the object to which must-call obligations are added when the annotated method is
   *     invoked
   */
  @JavaExpression
  String value() default "this";

  /**
   * A wrapper annotation that makes the {@link CreatesObligation} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link CreatesObligation} annotation at the same location.
   *
   * @checker_framework.manual #must-call-checker Must Call Checker
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  @InheritedAnnotation
  @interface List {
    /**
     * Return the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    CreatesObligation[] value();
  }
}
