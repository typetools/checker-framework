package org.checkerframework.checker.resourceleak;

import java.util.LinkedHashSet;
import java.util.NavigableSet;
import java.util.Set;
import javax.tools.Diagnostic;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * The entry point for the Resource Leak Checker. This checker only counts the number of {@link
 * org.checkerframework.checker.mustcall.qual.MustCall} annotations. This checker calls the {@link
 * RLCCalledMethodsChecker} as a direct subchecker, which then in turn calls the {@link
 * MustCallChecker} as a subchecker, and afterwards this checker traverses the CFG to check whether
 * all MustCall obligations are fulfilled.
 *
 * <p>The checker hierarchy is: this "empty" RLC &rarr; RLCCalledMethodsChecker &rarr;
 * MustCallChecker
 *
 * <p>The MustCallChecker is a subchecker of the RLCCm checker (instead of a sibling), since we want
 * them to operate on the same CFG (so we can get both a CM and MC store for a given CFG block),
 * which only works if one of them is a subchecker of the other.
 */
@SupportedOptions({
  "permitStaticOwning",
  "permitInitializationLeak",
  ResourceLeakChecker.COUNT_MUST_CALL,
  MustCallChecker.IGNORED_EXCEPTIONS,
  MustCallChecker.NO_CREATES_MUSTCALLFOR,
  MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP,
  MustCallChecker.NO_RESOURCE_ALIASES,
  ResourceLeakChecker.ENABLE_WPI_FOR_RLC,
  ResourceLeakChecker.ENABLE_RETURNS_RECEIVER
})
public class ResourceLeakChecker extends AggregateChecker {

  /** Creates a ResourceLeakChecker. */
  public ResourceLeakChecker() {}

  /**
   * Command-line option for counting how many must-call obligations were checked by the Resource
   * Leak Checker, and emitting the number after processing all files. Used for generating tables
   * for a research paper. Not of interest to most users.
   */
  public static final String COUNT_MUST_CALL = "countMustCall";

  /**
   * Ordinarily, when the -Ainfer flag is used, whole-program inference is run for every checker and
   * sub-checker. However, the Resource Leak Checker is different. The -Ainfer flag enables the
   * RLC's own (non-WPI) inference mechanism ({@link MustCallInference}). To use WPI in addition to
   * this mechanism for its sub-checkers, use the -AenableWpiForRlc flag, which is intended only for
   * testing and experiments.
   */
  public static final String ENABLE_WPI_FOR_RLC = "enableWpiForRlc";

  /**
   * The Returns Receiver Checker is disabled by default for the Resource Leak Checker, as it adds
   * significant overhead and typically provides little benefit. To enable it, use the
   * -AenableReturnsReceiverForRlc flag.
   */
  public static final String ENABLE_RETURNS_RECEIVER = "enableReturnsReceiverForRlc";

  /**
   * The number of expressions with must-call obligations that were checked. Incremented only if the
   * {@link #COUNT_MUST_CALL} command-line option was supplied.
   */
  /*package-private*/ int numMustCall = 0;

  /**
   * The number of must-call-related errors issued. The count of verified must-call expressions is
   * the difference between this and {@link #numMustCall}.
   */
  private int numMustCallFailed = 0;

  @Override
  protected Set<Class<? extends SourceChecker>> getSupportedCheckers() {
    Set<Class<? extends SourceChecker>> checkers = new LinkedHashSet<>(1);
    checkers.add(RLCCalledMethodsChecker.class);

    return checkers;
  }

  @Override
  public void reportError(
      @Nullable Object source, @CompilerMessageKey String messageKey, Object... args) {
    if (messageKey.equals("required.method.not.called")) {
      // This is safe because of the message key.
      String qualifiedTypeName = (String) args[1];
      // Only count classes in the JDK, not user-defined classes.
      if (MustCallConsistencyAnalyzer.isJdkClass(qualifiedTypeName)) {
        numMustCallFailed++;
      }
    }
    super.reportError(source, messageKey, args);
  }

  @Override
  public void typeProcessingOver() {
    if (hasOption(COUNT_MUST_CALL)) {
      message(Diagnostic.Kind.WARNING, "Found %d must call obligation(s).%n", numMustCall);
      message(
          Diagnostic.Kind.WARNING,
          "Successfully verified %d must call obligation(s).%n",
          numMustCall - numMustCallFailed);
    }
    super.typeProcessingOver();
  }

  @Override
  public NavigableSet<String> getSuppressWarningsPrefixes() {
    NavigableSet<String> result = super.getSuppressWarningsPrefixes();
    result.add("builder");
    return result;
  }
}
