package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.nullness.NullnessChecker;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Indicates that a field (or variable) is lazily initialized to a non-null
 * value.  Once the field becomes non-null, it never becomes null again.
 * There is no guarantee that the field ever becomes non-null, however.
 * <p>
 *
 * Lazily initialized fields have these two properties:
 * <ol>
 * <li>The field may be assigned only non-null values.</li>
 * <li>The field may be re-assigned as often as desired.</li>
 * </ol>
 * <p>
 *
 * When the field is first read within a method, the field cannot be
 * assumed to be non-null.  After a check that a {@code LazyNonNull} field
 * holds a non-null value, all subsequent accesses <em>within that
 * method</em> can be assumed to be non-null, even after arbitrary external
 * method calls that might access the field.
 * <p>
 * 
 * {@code LazyNonNull} gives stronger guarantees than {@link Nullable}.
 * After a check that a {@link Nullable} field holds a non-null value, only
 * accesses until the next non-{@link Pure} method is called can be assumed
 * to be non-null.
 * <p>
 *
 * To indicate that a {@code LazyNonNull} or {@code Nullable} field is
 * non-null whenever a particular method is called, use
 * {@link NonNullOnEntry}.
 * <p>
 *
 * Final fields are treated as LazyNonNull by default.
 * <p>
 *
 * This annotation is associated with the {@link NullnessChecker}.
 *
 * @see Nullable
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@TypeQualifier
@SubtypeOf(Nullable.class)
public @interface LazyNonNull {}
