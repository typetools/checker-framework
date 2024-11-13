package org.checkerframework.checker.nonempty;

import com.sun.source.tree.MethodTree;
import java.util.Set;
import org.checkerframework.checker.optional.OptionalImplChecker;
import org.checkerframework.checker.optional.OptionalImplVisitor;
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
public class NonEmptyChecker extends BaseTypeChecker {

  /** Creates a NonEmptyChecker. */
  public NonEmptyChecker() {
    super();
  }

  @Override
  public void initChecker() {
    super.initChecker();
  }

  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends SourceChecker>> checkers = super.getImmediateSubcheckerClasses();
    checkers.add(OptionalImplChecker.class);
    return checkers;
  }

  @Override
  public boolean shouldSkipDefs(MethodTree tree) {
    return !getMethodsToVerify().contains(tree);
  }

  /**
   * Obtains the methods to verify w.r.t. the Non-Empty type system from the Optional Checker.
   *
   * @return the set of methods to be verified by the Non-Empty Checker
   * @throws AssertionError if invoked when {@link shouldRunAsOptionalChecker} is false
   */
  private Set<MethodTree> getMethodsToVerify() {
    OptionalImplChecker optionalCheckerImpl = getSubchecker(OptionalImplChecker.class);
    assert optionalCheckerImpl != null : "@AssumeAssertion(nullness): runAsOptionalChecker is true";
    OptionalImplVisitor optionalVisitor = (OptionalImplVisitor) optionalCheckerImpl.getVisitor();
    return optionalVisitor.getMethodsToVerifyWithNonEmptyChecker();
  }
}
