package checkers.igj.quals;

import java.lang.annotation.*;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Indicates that the annotated reference is a ReadOnly reference.
 *
 * A {@code ReadOnly} reference could refer to a Mutable or an Immutable
 * object. An object may not be mutated through a read only reference,
 * except if the field is marked {@code Assignable}. Only a method with a
 * readonly receiver can be called using a readonly reference.
 *
 * @checker.framework.manual #igj-checker IGJ Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf({})
public @interface ReadOnly {}
