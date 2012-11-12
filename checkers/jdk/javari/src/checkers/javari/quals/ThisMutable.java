package checkers.javari.quals;

import java.lang.annotation.Target;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * An annotation that indicates that the type of a field is the same as the
 * type of the reference via which the field was accessed.
 * <p>
 *
 * For example, consider a Bicycle class that that contains a Wheel object:
 * <pre>
 * class Bicycle {
 *   Wheel frontWheel;
 *   ...
 * }
 * </pre>
 *
 * In a mutable bicycle, it is reasonable to mutate the wheel.  In a
 * read-only bicycle, the wheel should not be mutated -- it should be
 * read-only just like the bicyle that contains it.  "This-mutability"
 * means thath the field has the same mutability as the <tt>this</tt>
 * reference.
 * <pre>
 * @Readonly Bicycle b1;
 * @Mutable  Bicycle b2;
 * ... b1.frontWheel ...    // type is: @Readonly Wheel
 * ... b2.frontWheel ...    // type is: @Mutable Wheel
 * </pre>
 * 
 * <tt>@ThisMutable</tt> is the default on fields, and does not make sense
 * to write elsewhere.  Therefore, <tt>@ThisMutable</tt> should never
 * appear in a program.
 */
@TypeQualifier
@Target({}) // A programmer cannot write @ThisMutable in a program
@SubtypeOf(ReadOnly.class)
public @interface ThisMutable {

}
