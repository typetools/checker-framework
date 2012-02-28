package checkers.metaquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta annotation to specify all the qualifiers that the given qualifier
 * is a subtype of.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target ( { ElementType.TYPE } )
public @interface SubtypeOf {
    /** An array of the  supertype qualifiers of the annotated qualifier **/
    Class<?>[] value();
}
