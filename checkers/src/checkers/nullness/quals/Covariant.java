package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.quals.MarkerQualifier;

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
@MarkerQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Covariant {}