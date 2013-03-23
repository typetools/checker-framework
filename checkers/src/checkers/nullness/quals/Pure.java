package checkers.nullness.quals;

import java.lang.annotation.*;

/**
 * An annotation to indicate that a method is <em>pure</em>. This annotation has
 * been deprecated in favor of {@link dataflow.quals.Pure} due to lacking a
 * clear specification of its semantics, and due to the fact that it has not
 * been checked (but was a trusted annotation).
 * 
 * <p>
 * Until the annotation is removed, it is aliased to {@link dataflow.quals.Pure}
 * and indicates that a method is both side-effect free and deterministic.
 * 
 * @see dataflow.quals.Pure
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Deprecated()
public @interface Pure {
}
