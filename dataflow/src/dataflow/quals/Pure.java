package dataflow.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Pure} is a method annotation that indicates some form of
 * <em>purity</em> of that method. The dataflow framework supports three forms
 * of purity: side-effect freedom; determinism; and both together.
 *
 * <ul>
 * <li><tt>@Pure(Kind.SIDE_EFFECT_FREE)</tt>: A method is called
 * <em>side-effect free</em> if it has no visible side-effects. That is, if some
 * dataflow fact is known before a call to such a method, then it is still known
 * afterwards, even if the fact is about some non-final field.
 * <p>
 * Only the visible side-effects are important. The method is allowed to cache
 * the answer to a computationally expensive query, for instance.
 * <p>
 * If a method is annotated as side-effect-free, the Checker Framework warns if
 * the method uses any of the following Java constructs:
 * <ol>
 * <li>Assignment to any expression, except for local variables (and method
 * parameters).
 * <li>A method invocation of a method that is not side-effect free (as
 * indicated by a {@link Pure} annotation of type {@code Kind.SIDE_EFFECT_FREE}
 * on that methods declaration).
 * <li>Construction of a new object where the constructor is not side-effect
 * free.
 * </ol>
 * This is a conservative analysis. That is, these checks are sufficient for
 * soundness (no false negatives), but the checks are unnecessarily strict
 * (there are false positives). In particular, a method that caches its result
 * will be rejected.
 *
 * <li><tt>@Pure(Kind.DETERMINISTIC)</em>: A method is called
 * <em>deterministic</em> or <em>idempotent</em> if it returns the same
 * value every time called with the same parameters and in the same
 * environment. The parameters include the receiver, and the environment
 * includes all of the Java heap (that is, all fields of all objects and
 * all static variables).
 * <p>
 * If a method is annotated as deterministic, the Checker Framework warns
 * if the method uses any of the following Java constructs:
 * <ol>
 * <li>Assignment to any expression, except for local variables (and method
 * parameters).
 * <li>A method invocation of a method that is not deterministic (as indicated
 * by a {@link Pure}() annotation of type {@code Kind.DETERMINISTIC} on that
 * method's declaration).
 * <li>Construction of a new object.
 * <li>Catching any exceptions.
 * </ol>
 * This is a conservative analysis.  That is, these checks are sufficient
 * for soundness (no false negatives), but the checks are unnecessarily
 * strict (there are false positives).
 *
 * <li><tt>@Pure</tt>: indicates that the method is both side-effect-free
 * and deterministic.
 * </ul>
 *
 * <p>
 * A constructor can be <tt>@Pure</tt>, but a constructor <em>invocation</em> is
 * not deterministic since it returns a different new object each time. TODO:
 * Side-effect free constructors could be allowed to set their own fields.
 * </p>
 *
 * <p>
 * Note that the rules for checking currently imply that every deterministic
 * method is also side-effect free. This might change in the future; in general,
 * a deterministic method does not need to be side-effect free.
 *
 * @author Stefan Heule
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface Pure {

    Kind[] value() default { Kind.DETERMINISTIC, Kind.SIDE_EFFECT_FREE };

    /**
     * The type of purity.
     */
    public static enum Kind {
        /** The method has no visible side-effects. */
        SIDE_EFFECT_FREE,

        /**
         * The method returns exactly the same value when called in the same
         * environment.
         */
        DETERMINISTIC
    }
}
