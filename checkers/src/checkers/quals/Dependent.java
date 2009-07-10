package checkers.quals;

import java.lang.annotation.*;

/**
 * Refines the qualified type of the annotated field or variable based on the
 * receiver qualified type.  The annotation declares a relationship between
 * multiple type qualifier hierarchies.
 *
 * <p><b>Example:</b>
 * Consider a field, {@code lock}, that is only initialized if the
 * enclosing object, receiver, is marked as {@code ThreadSafe}.  Such field
 * can be declared as:
 *
 * <pre><code>
 *   private @Nullable @Dependent(result=NonNull.class, when=ThreadSafe.class)
 *     Lock lock;
 * </code></pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Dependent {

    /**
     * The class of the refined qualifier to be applied.
     */
    Class<? extends Annotation> result();

    /**
     * The qualifier class of the receiver that cause the {@code result}
     * qualifier to be applied.
     */
    Class<? extends Annotation> when();
}
