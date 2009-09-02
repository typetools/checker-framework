package checkers.igj.quals;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 *
 * Indicates that the annotated method could assigns (but not mutate) the fields
 * of {@code this} object.
 *
 * A method with an AssignsFields receiver may not use the receiver to
 * invoke other methods with mutable or immutable reciever.
 *
 * @checker.framework.manual #igj-checker IGJ Checker
 */
// TODO: Document this

@Documented
@Retention(RetentionPolicy.RUNTIME)
//@Target( { CONSTRUCTOR, METHOD })
@TypeQualifier // (for now)
@SubtypeOf( ReadOnly.class )
public @interface AssignsFields {

}
