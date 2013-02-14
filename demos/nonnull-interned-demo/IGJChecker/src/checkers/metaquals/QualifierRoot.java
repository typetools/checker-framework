package checkers.metaquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation to specify the root type qualifier in the qualifiers 
 * hierarchy, i.e. the qualifier that is a supertype of all other qualifiers
 * in the qualifiers hierarchy.
 * 
 * Examples of such qualifiers: {@code ReadOnly}, {@code Nullable}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target ( { ElementType.TYPE } )
public @interface QualifierRoot {

}
