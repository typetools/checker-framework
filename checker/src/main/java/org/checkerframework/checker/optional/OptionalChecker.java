package org.checkerframework.checker.optional;

import com.sun.source.tree.MethodTree;
import java.util.Set;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * A type-checker that prevents {@link java.util.NoSuchElementException} in the use of container
 * classes.
 *
 * <p>If "-ArunAsOptionalChecker" is passed, then the Non-Empty Checker acts like the Optional
 * Checker, but with better precision. More specifically, it:
 *
 * <ol>
 *   <li>Runs the Optional Checker (as a subchecker), using explicitly-written (i.e.,
 *       programmer-written) annotations from the Non-Empty type system to refine the Optional
 *       analysis. This improves analysis of operations on containers (e.g., Streams, Collections)
 *       that result in values of type Optional.
 *   <li>Checks only explicitly-written {@code @NonEmpty} annotations.
 * </ol>
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
@SupportedOptions("runAsOptionalChecker")
public class OptionalChecker extends BaseTypeChecker {

  /** True if the {@code shouldRunOptionalChecker} variable has been set. */
  private boolean isShouldRunOptionalCheckerSet = false;

  /**
   * True if the Non-Empty Checker should be run as the Optional Checker with increased precision.
   */
  private boolean shouldRunAsOptionalChecker = false;

  /** Creates a NonEmptyChecker. */
  public OptionalChecker() {
    super();
  }

  @Override
  public void initChecker() {
    super.initChecker();
    shouldRunAsOptionalChecker = this.hasOptionNoSubcheckers("runAsOptionalChecker");
    isShouldRunOptionalCheckerSet = true;
  }

  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends SourceChecker>> checkers = super.getImmediateSubcheckerClasses();
    if (shouldRunAsOptionalChecker()) {
      checkers.add(OptionalWithoutNonEmptyChecker.class);
    }
    return checkers;
  }

  @Override
  public boolean shouldSkipDefs(MethodTree tree) {
    if (shouldRunAsOptionalChecker()) {
      return !getMethodsToVerify().contains(tree);
    }
    return super.shouldSkipDefs(tree);
  }

  /**
   * Obtains the methods to verify w.r.t. the Non-Empty type system from the Optional Checker.
   *
   * @return the set of methods to be verified by the Non-Empty Checker
   * @throws AssertionError if invoked when {@link shouldRunAsOptionalChecker} is false
   */
  private Set<MethodTree> getMethodsToVerify() {
    assert shouldRunAsOptionalChecker; // Invariant: this method is invoked iff
    // shouldRunAsOptionalChecker is true
    OptionalWithoutNonEmptyChecker optionalChecker =
        getSubchecker(OptionalWithoutNonEmptyChecker.class);
    assert optionalChecker != null : "@AssumeAssertion(nullness): runAsOptionalChecker is true";
    OptionalWithoutNonEmptyVisitor optionalVisitor =
        (OptionalWithoutNonEmptyVisitor) optionalChecker.getVisitor();
    return optionalVisitor.getMethodsToVerifyWithNonEmptyChecker();
  }

  /**
   * Returns the value of {@link shouldRunAsOptionalChecker}.
   *
   * <p>This method behaves as a getter for {@link shouldRunAsOptionalChecker}, and avoids
   * re-computing its value each time (i.e., avoids repeated calls to {@link
   * SourceChecker#hasOptionNoSubcheckers}).
   *
   * @return the value of {@link shouldRunAsOptionalChecker}
   */
  private boolean shouldRunAsOptionalChecker() {
    if (!isShouldRunOptionalCheckerSet) {
      shouldRunAsOptionalChecker = this.hasOptionNoSubcheckers("runAsOptionalChecker");
      isShouldRunOptionalCheckerSet = true;
    }
    return shouldRunAsOptionalChecker;
  }
}
