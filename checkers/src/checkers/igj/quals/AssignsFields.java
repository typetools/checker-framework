package checkers.igj.quals;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method could assign (but not mutate) the fields
 * of {@code this} object.
 *
 * A method with an AssignsFields receiver may not use the receiver to
 * invoke other methods with mutable or immutable receiver.
 *
 * @checker.framework.manual #igj-checker IGJ Checker
 */
// TODO: Document this

@TypeQualifier // (for now)
@SubtypeOf( ReadOnly.class )
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
public @interface AssignsFields {}
