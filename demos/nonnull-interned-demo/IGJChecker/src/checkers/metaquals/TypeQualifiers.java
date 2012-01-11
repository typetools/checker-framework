package checkers.metaquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to indicate the type qualifiers supported by the annotated
 * {\code Checker}.
 * 
 * This may be used reflectively by the framework to construct the qualifiers
 * hierarchy.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target ( { ElementType.TYPE } )
public @interface TypeQualifiers {
    Class<?>[] value();
}
