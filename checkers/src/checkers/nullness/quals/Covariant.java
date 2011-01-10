package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.quals.MarkerQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

/**
 * TODO: doc.
 * A marker annotation to signify that the type argument corresponding to the annotated
 * type variable can safely ignore KeyFor annotations.
 * The prime example is Map.Entry<K>.
 * It is not checked whether the annotated class is immutable.
 * 
 * TODO: move to a different package?
 *
 */

@Documented
// TODO: I had a special case in BaseTypeChecker, similar to PolymorphicQualifier, but that didn't work :-(
@MarkerQualifier
// TODO: Who checks this annotation??? I don't find it's uses :-(
@TypeQualifier
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
// TODO: Without this, the checks fail, without any helpful output :-((
@SubtypeOf( Unqualified.class )
@Retention(RetentionPolicy.RUNTIME)
public @interface Covariant {}