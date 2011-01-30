package checkers.metaquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An meta-annotation indicating that the annotated annotation is a type
 * qualifier.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target ( { ElementType.TYPE } )
public @interface TypeQualifier {

}
