package checkers.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Pure} is a method annotation that indicates the <em>purity</em> of
 * that method. A method is said to be pure if it does not contain any of the
 * following Java constructs:
 * <ol>
 * <li>Assignment to any expression, except for local variables (including
 * method parameters).
 * <li>A method invocation of a method that is not pure itself (as indicated by
 * a {@link Pure} annotation on that methods declaration).
 * <li>Construction of a new object.
 * </ol>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Pure {
}
