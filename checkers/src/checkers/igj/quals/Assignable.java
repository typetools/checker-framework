package checkers.igj.quals;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 *
 * Indicates the annotated {@code Field} may be re-assigned regardless of the
 * immutability of the enclosing class or object instance.
 *
 * @checker.framework.manual #igj-checker IGJ Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( { FIELD } )
public @interface Assignable {

}
