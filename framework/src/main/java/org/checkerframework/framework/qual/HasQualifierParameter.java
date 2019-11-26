package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This is a declaration annotation that applies to type declarations. It means that the class
 * conceptually takes a type qualifier parameter, though there is nowhere to write it because the
 * class hard-codes a Java basetype rather than taking a type parameter.
 *
 * <p>Writing {@code @HasQualifierParameter} on a type declaration has two effects.
 *
 * <ol>
 *   <li>Invariant subtyping is used for occurrences of the type: no two occurrences of the type
 *       with different qualifiers have a subtyping relationship.
 *   <li>The polymorphic qualifier is the default for all occurrences of that type in a method
 *       signature in its own compilation unit, including as the receiver, as another formal
 *       parameter, or as a return type.
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
 */
@Target(ElementType.TYPE)
@Documented
public @interface HasQualifierParameter {

    /**
     * Class of the top qualifier for the hierarchy for which this class has a qualifier parameter.
     *
     * @return the value
     */
    Class<? extends Annotation>[] value();
}
