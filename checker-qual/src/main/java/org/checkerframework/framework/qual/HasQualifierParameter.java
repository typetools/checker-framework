package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a declaration annotation that applies to type declarations and packages. On a
 * <b>type</b>, it means that the class conceptually takes a type qualifier parameter, though there
 * is nowhere to write it because the class hard-codes a Java basetype rather than taking a type
 * parameter. Writing {@code HasQualifierParameter} on a <b>package</b> is the same as writing it on
 * each class in that package.
 *
 * <h2>Written on a type declaration</h2>
 *
 * <p>Writing {@code @HasQualifierParameter} on a <b>type declaration</b> has two effects.
 *
 * <ol>
 *   <li>Invariant subtyping is used for occurrences of the type: no two occurrences of the type
 *       with different qualifiers have a subtyping relationship.
 *   <li>The polymorphic qualifier is the default for all occurrences of that type in its own
 *       compilation unit, including as the receiver, as another formal parameter, or as a return
 *       type.
 * </ol>
 *
 * Here is an example of the effect of invariant subtyping. Suppose we have the following
 * declaration:
 *
 * <pre>
 *  {@code @HasQualifierParameter}
 *   class StringBuffer { ... }
 * </pre>
 *
 * Then {@code @Tainted StringBuffer} is unrelated to {@code @Untainted StringBuffer}.
 *
 * <p>The type hierarchy looks like this:
 *
 * <pre>
 *
 *                       {@code @Tainted} Object
 *                      /       |       \
 *                     /        |       {@code @Tainted} Date
 *                   /          |               |
 *                  /           |               |
 *                 /   {@code @Untainted} Object       |
 *                /             |       \       |
 *  {@code @Tainted} StringBuffer      |      {@code @Untainted} Date
 *             |                |
 *             |      {@code @Untainted} StringBuffer
 *             |                |
 *  {@code @Tainted} MyStringBuffer    |
 *                              |
 *                    {@code @Untainted} MyStringBuffer
 * </pre>
 *
 * <p>When a type is {@code @HasQualifierParameter}, all its subtypes are as well. That is, the
 * {@code @HasQualifierParameter} annotation is inherited by subtypes.
 *
 * <h2>Written on a package</h2>
 *
 * <p>When {@code @HasQualifierParameter} is written on a package, it is equivalent to writing that
 * annotation on each class in the package or in a sub-package. It can be disabled on a specific
 * class and its subclasses by writing {@code @NoQualifierParameter} on that class. This annotation
 * may not be written on the same class as {@code NoQualifierParameter} for the same hierarchy.
 *
 * @see NoQualifierParameter
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface HasQualifierParameter {

  /**
   * Class of the top qualifier for the hierarchy for which this class has a qualifier parameter.
   *
   * @return the value
   */
  Class<? extends Annotation>[] value();
}
