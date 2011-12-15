package checkers.oigj.quals;

import java.lang.annotation.*;

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
 */
// TODO: Document this

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier // (for now)
@SubtypeOf(ReadOnly.class)
public @interface AssignsFields {}
