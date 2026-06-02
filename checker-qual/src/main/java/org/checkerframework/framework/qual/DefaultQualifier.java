package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to a declaration of a package, type, method, variable, etc., specifies that the given
 * annotation should be the default. The default is applied to type uses within the declaration for
 * which no other annotation is explicitly written. (The default is not applied to the "parametric
 * locations": class declarations, type parameter declarations, and type parameter uses.) If
 * multiple {@code DefaultQualifier} annotations are in scope, the innermost one takes precedence.
 * DefaultQualifier takes precedence over {@link DefaultQualifierInHierarchy}.
 *
 * <p>You may write multiple {@code @DefaultQualifier} annotations (for unrelated type systems, or
 * with different {@code locations} fields) at the same location. For example:
 *
 * <pre>
 * &nbsp; @DefaultQualifier(NonNull.class)
 * &nbsp; @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.IMPLICIT_UPPER_BOUND)
 * &nbsp; @DefaultQualifier(Tainted.class)
 * &nbsp; class MyClass { ... }
 * </pre>
 *
 * <p>This annotation currently has no effect in stub files.
 *
 * @see org.checkerframework.framework.qual.TypeUseLocation
 * @see DefaultQualifierInHierarchy
 * @see DefaultFor
 * @checker_framework.manual #defaults Default qualifier for unannotated types
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
  ElementType.PACKAGE,
  ElementType.TYPE,
  ElementType.CONSTRUCTOR,
  ElementType.METHOD,
  ElementType.FIELD,
  ElementType.LOCAL_VARIABLE,
  ElementType.PARAMETER
})
@Repeatable(DefaultQualifier.List.class)
public @interface DefaultQualifier {

  /**
   * The Class for the default annotation.
   *
   * <p>To prevent affecting other type systems, always specify an annotation in your own type
   * hierarchy. (For example, do not set {@link
   * org.checkerframework.common.subtyping.qual.Unqualified} as the default.)
   */
  Class<? extends Annotation> value();

  /**
   * Returns the locations to which the annotation should be applied.
   *
   * @return the locations to which the annotation should be applied
   */
  TypeUseLocation[] locations() default {TypeUseLocation.ALL};

  /**
   * A wrapper annotation that makes the {@link DefaultQualifier} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link DefaultQualifier} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({
    ElementType.PACKAGE,
    ElementType.TYPE,
    ElementType.CONSTRUCTOR,
    ElementType.METHOD,
    ElementType.FIELD,
    ElementType.LOCAL_VARIABLE,
    ElementType.PARAMETER
  })
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    DefaultQualifier[] value();
  }
}
