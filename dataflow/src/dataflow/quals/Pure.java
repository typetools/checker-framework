package dataflow.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Pure} is a method annotation that indicates some form of
 * <em>purity</em> of that method. There are several forms of purity:
 * <ul>
 * <li><em>Kind.SIDE_EFFECT_FREE</em>: A method is called
 * <em>side-effect free</em>, if it does not have any visible side-effects. That
 * is, if some dataflow fact is known before a call to such a method, then it is
 * still known afterwards, even if the fact is about some non-final field.
 * 
 * <p>
 * Only the visible side-effects are important; caching the answer to a
 * computationally expensive query for instance is allowed.
 * 
 * <p>
 * The default check made by the Checker Framework to determine if a method is
 * side-effect free consists of checking that a method does not use any of the
 * following Java constructs:
 * <ol>
 * <li>Assignment to any expression, except for local variables (and method
 * parameters).
 * <li>A method invocation of a method that is not side-effect free (as
 * indicated by a {@link Pure} annotation of type {@code Kind.SIDE_EFFECT_FREE}
 * on that methods declaration).
 * <li>Construction of a new object where the constructor is not side-effect
 * free.
 * </ol>
 * Note that these checks are meant to be sufficient (no false negatives), but
 * are not necessary (there are false positives). In particular, a method that
 * caches its result will be rejected.
 * <li><em>Kind.DETERMINISTIC</em>: A method is called <em>deterministic</em>,
 * if it returns the same value every time called with the same parameters and
 * in the same environment. The parameters include the receiver, and the
 * environment includes all of the Java heap (that is, all fields of all objects
 * and all static variables).
 * 
 * <p>
 * The default check made by the Checker Framework to determine if a method is
 * deterministic consists of checking that a method does not use any of the
 * following Java constructs:
 * <ol>
 * <li>Assignment to any expression, except for local variables (and method
 * parameters).
 * <li>A method invocation of a method that is not deterministic (as indicated
 * by a {@link Pure} annotation of type {@code Kind.DETERMINISTIC} on that
 * methods declaration).
 * <li>Construction of a new object
 * <li>Catching any exceptions.
 * </ol>
 * Note that these checks are meant to be sufficient (no false negatives), but
 * are not necessary (there are false positives).
 * </ul>
 * 
 * <p>
 * The default is that the method is both deterministic as well as side-effect
 * free.
 * 
 * <p>
 * Note that constructors can only be side-effect free, as the invocation of a
 * constructor creates a new object and is therefore by definition not
 * deterministic.
 * 
 * TODO: Side-effect free constructors could be allowed to set their own fields.
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
        /** The method does not have any visible side-effects */
        SIDE_EFFECT_FREE,

        /**
         * The method returns exactly the same value when called in the same
         * environment
         */
        DETERMINISTIC
    }
}
