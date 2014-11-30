package org.checkerframework.checker.oigj.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 *
 * Template annotation over IGJ Immutability annotations. It acts
 * somewhat similar to Type Variables in Generics. The annotation
 * value is used to distinguish between multiple instances of
 * {@code @I}.<p>
 *
 * <b>Usage On classes</b><p>
 * A class annotated with {@code I} could be declared with any IGJ
 * Immutability annotation. The actual immutability that {@code @I} is
 * resolved dectates the immutability type for all the non-static
 * appearances of {@code @I} with the same value as the class
 * declaration.<p>
 *
 * Example:
 *
 * <pre>
 *     &#064;I
 *     public class FileDiscriptor {
 *        private &#064;Immutable Date creationData;
 *        private &#064;I Date lastModData;
 *
 *        public &#064;I getLastModDate() &#064;ReadOnly { }
 *     }
 *
 *     ...
 *     void useFileDiscriptor() {
 *        &#064;Mutable FileDiscriptor file =
 *                          new &#064;Mutable FileDiscriptor(...);
 *        ...
 *        &#064;Mutable Data date = file.getLastModDate();
 *
 *     }
 * </pre>
 *
 * In the last example, {@code @I} was resolved to {@code @Mutable} for
 * the instance file.
 *
 * <b>Usage On Methods</b><p>
 * For example, it could be used for method parameters, return values,
 * and the actual IGJ immutability value would be resolved based on
 * the method invocation.<p>
 *
 * Example:
 * <pre>
 *      static &#064;I Point getMidPoint(@I Point p1, @I Point p2)
 *          { ...}
 * </pre>
 *
 * The method would return a {@code Point} object that returns a Point with
 * the same immutability type as the passed parameters if p1 and p2 match in
 * immutability, otherwise {@code @I} is resolved to {@code @ReadOnly}.
 *
 * @checker_framework.manual #oigj-checker OIGJ Checker
 */
@TypeQualifier
//@PolymorphicQualifier // TODO: uncomment later
@SubtypeOf(ReadOnly.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface I {
    String value() default "I";
}
