package org.checkerframework.common.initializedfields.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

/**
 * A method postcondition annotation indicates which fields the method definitely initializes.
 *
 * @checker_framework.manual #initialized-fields-checker Initialized Fields Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PostconditionAnnotation(qualifier = InitializedFields.class)
@InheritedAnnotation
@Repeatable(EnsuresInitializedFields.List.class)
public @interface EnsuresInitializedFields {
  /**
   * The object whose fields this method initializes.
   *
   * @return object whose fields are initialized
   */
  public String[] value() default {"this"};

  /**
   * Fields that this method initializes.
   *
   * @return fields that this method initializes
   */
  @QualifierArgument("value")
  public String[] fields();

  /**
   * A wrapper annotation that makes the {@link EnsuresInitializedFields} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link EnsuresInitializedFields} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @PostconditionAnnotation(qualifier = InitializedFields.class)
  @InheritedAnnotation
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresInitializedFields[] value();
  }
}
