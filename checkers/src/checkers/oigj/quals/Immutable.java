package checkers.oigj.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Indicates that the annotated reference is an immutable reference to an
 * immutable object.
 *
 * An Immutable object cannot be modified. Its fields may be reassigned or
 * mutated only if they are explicitly marked as Mutable or Assignable.
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(ReadOnly.class)
public @interface Immutable {}
