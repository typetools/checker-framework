package checkers.oigj.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 *
 * Template annotation over IGJ Immutability annotations. It acts
 * somewhat similar to Type Variables in Generics. The annotation
 * value is used to distinguish between multiple instances of
 * {@code @I}.<br />
 *
 * <b>Usage On classes</b><br />
 * A class annotated with {@code I} could be declared with any IGJ
 * Immutability annotation. The actual immutability that {@code @I} is
 * resolved dectates the immutability type for all the non-static
 * appearances of {@code @I} with the same value as the class
 * declaration.<br />
 *
 * Example: <br />
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
 * <b>Usage On Methods</b><br />
 * For example, it could be used for method parameters, return values,
 * and the actual IGJ immutability value would be resolved based on
 * the method invocation.<br />
 *
 * Example: <br />
 * <pre>
 *      static &#064;I Point getMidPoint(@I Point p1, @I Point p2)
 *          { ...}
 * </pre>
 *
 * The method would return a {@code Point} object that returns a Point with
 * the same immutability type as the passed parameters if p1 and p2 match in
 * immutability, otherwise {@code @I} is resolved to {@code @ReadOnly}.
 *
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
//@PolymorphicQualifier // TODO: uncomment later
@SubtypeOf(ReadOnly.class)
public @interface I {
    String value() default "I";
}
