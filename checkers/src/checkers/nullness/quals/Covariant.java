package checkers.nullness.quals;

import java.lang.annotation.*;


/**
 * TODO: doc.
 * A marker annotation to signify that the type argument corresponding to the annotated
 * type variable can safely ignore KeyFor annotations.
 * The prime example is Map.Entry<K>.
 * It is not checked whether the annotated class is immutable.
 * The value is the set of zero-based indices of the type parameters that should be covariant.
 * 
 * TODO: move to a different package?
 *
 */

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Covariant {
	int[] value();
}