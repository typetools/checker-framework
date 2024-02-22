package org.checkerframework.checker.initialization;

import java.util.NavigableSet;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * Tracks whether a value is initialized (all its fields are set), and checks that values are
 * initialized before being used. Implements the freedom-before-commitment scheme for
 * initialization, augmented by type frames.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 */
public abstract class InitializationChecker extends BaseTypeChecker {

  /** Create a new InitializationChecker. */
  protected InitializationChecker() {}

  @Override
  public NavigableSet<String> getSuppressWarningsPrefixes() {
    NavigableSet<String> result = super.getSuppressWarningsPrefixes();
    // "fbc" is for backward compatibility only.
    // Notes:
    //   * "fbc" suppresses *all* warnings, not just those related to initialization.  See
    //     https://checkerframework.org/manual/#initialization-checking-suppressing-warnings .
    //   * "initialization" is not a checkername/prefix.
    result.add("fbc");
    return result;
  }
}
