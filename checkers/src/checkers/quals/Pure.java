package checkers.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Pure} is a method annotation that indicates the <em>purity</em> of
 * that method. A method is said to be pure if..
 * <p>
 * TODO: formalize notation of purity and
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Pure {
}
