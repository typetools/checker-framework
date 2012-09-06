package checkers.nullness.quals;

import java.lang.annotation.*;

/**
 * <p>
 * Indicates that if the method is a pure method, so calling it
 * multiple times with the same arguments yields the same results.
 * </p><p>
 * The method should not have any visible side-effect.
 * Non-visible benevolent side effects (e.g., caching) are possible.
 * </p>
 *
 * <!-- This text is from the Checker Framework manual and should be updated
 * whenever the Checker Framework manual is changed. -->
 * <p>For example, consider the following declaration and uses:</p>
 * <pre>         @Nullable Object getField(Object arg) { ... }
 * 
 *         ...
 *         if (x.getField(y) != null) {
 *           x.getField(y).toString();
 *         }</pre>
 * <p>Ordinarily, the Nullness Checker would issue a warning regarding the
 * <tt>toString()</tt> call, because the receiver <tt>x.getField(y)</tt> might
 * be <tt>null</tt>, according to the <tt>@Nullable</tt> annotation in the
 * declaration of <tt>getField</tt>. If you change the declaration of
 * <tt>getField</tt> to</p>
 * <pre>        @Pure @Nullable Object getField(Object arg) { ... }</pre>
 * <p>then the Nullness Checker issues no warnings, because it can reason that
 * the two invocations <tt>x.getField(y)</tt> have the same value, and
 * therefore that <tt>x.getField(y)</tt> is non-null within the then branch
 * of the if statement.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Pure {
}
