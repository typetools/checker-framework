package checkers.igj.quals;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * Indicates that the annotated method could assigns (but not mutate) the fields
 * of {@code this} object.
 * 
 * An AssignsFields method may not invoke other mutable or immutable methods on 
 * the receiver.
 * 
 */
// TODO: Document this

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( { CONSTRUCTOR, METHOD })
public @interface AssignsFields {

}
