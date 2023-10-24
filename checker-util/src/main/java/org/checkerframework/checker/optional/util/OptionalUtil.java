package org.checkerframework.checker.optional.util;

import java.util.Optional;
import org.checkerframework.checker.optional.qual.EnsuresPresent;
import org.checkerframework.checker.optional.qual.MaybePresent;
import org.checkerframework.checker.optional.qual.Present;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * This is a utility class for the Optional Checker.
 *
 * <p>To avoid the need to write the OptionalUtil class name, do:
 *
 * <pre>import static org.checkerframework.checker.optional.util.OptionalUtil.castPresent;</pre>
 *
 * or
 *
 * <pre>import static org.checkerframework.checker.optional.util.OptionalUtil.*;</pre>
 *
 * <p><b>Runtime Dependency</b>: If you use this class, you must distribute (or link to) {@code
 * checker-qual.jar}, along with your binaries. Or, you can copy this class into your own project.
 */
@SuppressWarnings({
  "optional", // Optional utilities are trusted regarding the Optional type.
  "cast" // Casts look redundant if Optional Checker is not run.
})
@AnnotatedFor("optional")
public final class OptionalUtil {

  /** The OptionalUtil class should not be instantiated. */
  private OptionalUtil() {
    throw new AssertionError("shouldn't be instantiated");
  }

  /**
   * A method that suppresses warnings from the Optional Checker.
   *
   * <p>The method takes a possibly-empty Optional reference, unsafely casts it to have the @Present
   * type qualifier, and returns it. The Optional Checker considers both the return value, and also
   * the argument, to be present after the method call. Therefore, the {@code castPresent} method
   * can be used either as a cast expression or as a statement.
   *
   * <pre><code>
   *   // one way to use as a cast:
   *  {@literal @}Present String s = castPresent(possiblyEmpty1);
   *
   *   // another way to use as a cast:
   *   castPresent(possiblyEmpty2).toString();
   *
   *   // one way to use as a statement:
   *   castPresent(possiblyEmpty3);
   *   possiblyEmpty3.toString();
   * </code></pre>
   *
   * The {@code castPresent} method is intended to be used in situations where the programmer
   * definitively knows that a given Optional reference is present, but the type system is unable to
   * make this deduction. It is not intended for defensive programming, in which a programmer cannot
   * prove that the value is not empty but wishes to have an earlier indication if it is. See the
   * Checker Framework Manual for further discussion.
   *
   * <p>The method throws {@link AssertionError} if Java assertions are enabled and the argument is
   * empty. If the exception is ever thrown, then that indicates that the programmer misused the
   * method by using it in a circumstance where its argument can be empty.
   *
   * @param <T> the type of content of the Optional
   * @param ref an Optional reference of @MaybePresent type, that is present at run time
   * @return the argument, casted to have the type qualifier @Present
   */
  @EnsuresPresent("#1")
  public static <T extends @MaybePresent Object> @Present Optional<T> castPresent(
      @MaybePresent Optional<T> ref) {
    assert ref.isPresent() : "Misuse of castPresent: called with an empty Optional";
    return (@Present Optional<T>) ref;
  }
}
