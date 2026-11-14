package org.checkerframework.checker.resourceleak;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.NavigableSet;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SupportedOptions;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The entry point for the Resource Leak Checker. This checker only counts the number of {@link
 * org.checkerframework.checker.mustcall.qual.MustCall} annotations. This checker calls the {@link
 * RLCCalledMethodsChecker} as a direct subchecker, which then in turn calls the {@link
 * MustCallChecker} as a subchecker, and afterwards this checker traverses the CFG to check whether
 * all MustCall obligations are fulfilled.
 *
 * <p>The checker hierarchy is: this "empty" RLC &rarr; RLCCalledMethodsChecker &rarr;
 * MustCallChecker.
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
   *
   * <p>A must-call obligation is counted if its type is a JDK class. It is created by an invocation
   * whose result has a must-call obligation, by an invocation of a {@code CreatesMustCallFor}
   * method, by an owning or must-call-alias formal parameter, or by an owning field. An obligation
   * that the checker fails to verify is not counted as verified, even if the checker does not
   * report the failure (for example, because a warning is suppressed).
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
   * The keys of the must-call obligations that were found. Populated only if the {@link
   * #COUNT_MUST_CALL} command-line option was supplied. See {@link #countFoundObligation}.
   */
  private final Set<Object> foundObligations = new HashSet<>();

  /**
   * The keys of the must-call obligations that the checker failed to verify. Populated only if the
   * {@link #COUNT_MUST_CALL} command-line option was supplied. A key in this set but not in {@link
   * #foundObligations} is ignored.
   */
  private final Set<Object> failedObligations = new HashSet<>();

  @Override
  protected Set<Class<? extends SourceChecker>> getSupportedCheckers() {
    Set<Class<? extends SourceChecker>> checkers = new LinkedHashSet<>(1);
    checkers.add(RLCCalledMethodsChecker.class);

    return checkers;
  }

  /**
   * Records that the checker found the must-call obligation that {@code key} identifies, if the
   * {@link #COUNT_MUST_CALL} command-line option was supplied and {@code type} is a JDK class.
   * Recording the same key again has no effect.
   *
   * @param key an object that identifies a must-call obligation, such as the element of an owning
   *     field or of an owning formal parameter. Keys are compared with {@code equals}.
   * @param type the type of the object that has the obligation
   * @return {@code key} if this method recorded it, or null otherwise
   */
  public @Nullable Object countFoundObligation(Object key, TypeMirror type) {
    if (!hasOption(COUNT_MUST_CALL)) {
      return null;
    }
    // Only count classes in the JDK, since that's what the paper reported.  The erasure of a type
    // variable is the erasure of its upper bound.
    TypeElement typeElement = TypesUtils.getTypeElement(getTypeUtils().erasure(type));
    if (typeElement == null || !isJdkClass(typeElement.getQualifiedName().toString())) {
      return null;
    }
    foundObligations.add(key);
    return key;
  }

  /**
   * Records that the checker failed to verify the must-call obligation that {@code key} identifies.
   * The failure is counted only if {@link #countFoundObligation} records the same key, before or
   * after this call. Recording the same key again has no effect.
   *
   * @param key an object that identifies a must-call obligation, as for {@link
   *     #countFoundObligation}, or null to do nothing
   */
  public void countFailedObligation(@Nullable Object key) {
    if (key != null && hasOption(COUNT_MUST_CALL)) {
      failedObligations.add(key);
    }
  }

  /**
   * Returns true if the given class is a java* class. This is a heuristic for whether the class was
   * defined in the JDK.
   *
   * @param qualifiedName a fully qualified name of a class
   * @return true iff the type's fully-qualified name starts with "java", indicating that it is from
   *     a java.* or javax.* package (probably)
   */
  private static boolean isJdkClass(String qualifiedName) {
    return qualifiedName.startsWith("java");
  }

  @Override
  public void typeProcessingOver() {
    if (hasOption(COUNT_MUST_CALL)) {
      int numFailed = 0;
      for (Object key : failedObligations) {
        if (foundObligations.contains(key)) {
          numFailed++;
        }
      }
      message(
          Diagnostic.Kind.WARNING, "Found %d must call obligation(s).%n", foundObligations.size());
      message(
          Diagnostic.Kind.WARNING,
          "Successfully verified %d must call obligation(s).%n",
          foundObligations.size() - numFailed);
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
