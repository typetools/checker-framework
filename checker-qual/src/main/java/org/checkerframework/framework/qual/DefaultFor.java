package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation applied to the declaration of a type qualifier. It specifies that the given
 * annotation should be the default for:
 *
 * <ul>
 *   <li>all uses at a particular location,
 *   <li>all uses of a particular type, and
 *   <li>all uses of a particular kind of type.
 * </ul>
 *
 * <p>The default applies to every match for any of this annotation's conditions.
 *
 * @see TypeUseLocation
 * @see DefaultQualifier
 * @see DefaultQualifierInHierarchy
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DefaultFor {
  /**
   * Returns the locations to which the annotation should be applied.
   *
   * @return the locations to which the annotation should be applied
   */
  TypeUseLocation[] value() default {};

  /**
   * Returns {@link TypeKind}s of types for which an annotation should be implicitly added.
   *
   * @return {@link TypeKind}s of types for which an annotation should be implicitly added
   */
  TypeKind[] typeKinds() default {};

  /**
   * Returns {@link Class}es for which an annotation should be applied. For example, if
   * {@code @MyAnno} is meta-annotated with {@code @DefaultFor(classes=String.class)}, then every
   * occurrence of {@code String} is actually {@code @MyAnno String}.
   *
   * <p>Only the given types, not their subtypes, receive the default. For instance, if the {@code
   * types} element contains only {@code Iterable}, then the default does not apply to a variable or
   * expression of type {@code Collection} which is a subtype of {@code Iterable}.
   *
   * @return {@link Class}es for which an annotation should be applied
   */
  Class<?>[] types() default {};

  /**
   * Returns regular expressions matching names of variables, to whose types the annotation should
   * be applied as a default. If a regular expression matches the name of a method, the annotation
   * is applied as a default to the return type.
   *
   * <p>The regular expression must match the entire variable or method name. For example, to match
   * any name that contains "foo", use ".*foo.*".
   *
   * <p>The default does not apply if the name matches any of the regexes in {@link
   * #namesExceptions}.
   *
   * <p>This affects formal parameter types only if one of the following is true:
   *
   * <ul>
   *   <li>The method's source code is available; that is, the method is type-checked along with
   *       client calls.
   *   <li>When the method was compiled in a previous run of javac, either the processor was run or
   *       the {@code -g} command-line option was provided.
   * </ul>
   *
   * @return regular expressions matching variables to whose type an annotation should be applied
   */
  String[] names() default {};

  /**
   * Returns exceptions to regular expression rules.
   *
   * @return exceptions to regular expression rules
   * @see #names
   */
  String[] namesExceptions() default {};
}
