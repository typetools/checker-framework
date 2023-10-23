package org.checkerframework.checker.optional.util;

import java.util.Optional;
import org.checkerframework.checker.optional.qual.EnsuresPresent;
import org.checkerframework.checker.optional.qual.MaybePresent;
import org.checkerframework.checker.optional.qual.Present;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Utility class for the Optional Checker.
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

  private OptionalUtil() {
    throw new AssertionError("shouldn't be instantiated");
  }

  @EnsuresPresent("#1")
  public static <T extends @MaybePresent Object> @Present Optional<T> castPresent(
      @MaybePresent Optional<T> ref) {
    assert ref.isPresent() : "Misuse of castPresent: called with an empty Optional";
    return (@Present Optional<T>) ref;
  }
}
