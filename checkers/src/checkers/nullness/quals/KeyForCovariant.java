package checkers.nullness.quals;

import java.lang.annotation.*;

/**
 * TODO
 * A marker annotation to signify that type arguments of the annotated class
 * can safely ignore KeyFor annotations.
 * The prime example is Map.Entry.
 * It is not checked whether the annotated class is immutable.
 * 
 * TODO: is the class level fine-grained enough? It would be a bit more
 * cumbersome to support this per type variable.
 *
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface KeyForCovariant {}