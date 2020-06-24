package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a declaration annotation that applies to type declarations and packages. On a type, it
 * means that the class conceptually takes a type qualifier parameter, though there is nowhere to
 * write it because the class hard-codes a Java basetype rather than taking a type parameter.
 * Writing {@code HasQualifierParameter} on a package is the same as writing it on each class in
 * that package.
 *
 * <p>Writing {@code @HasQualifierParameter} on a type declaration has two effects.
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
 * <p>This annotation may not be written on the same class as {@code NoQualifierParameter} for the
 * same hierarchy.
 *
 * <p>When {@code @HasQualifierParameter} is written on a package, it is equivalent to writing it on
 * each class in that package with the same arguments, including classes in sub-packages. It can be
 * disabled on a specific class by writing {@code @NoQualifierParameter} on that class.
 *
 * @see NoQualifierParameter
 */
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
