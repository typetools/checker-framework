package org.checkerframework.checker.nonempty;

import com.sun.source.tree.MethodTree;
import java.util.Set;
import org.checkerframework.checker.optional.OptionalChecker;
import org.checkerframework.checker.optional.OptionalVisitor;
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

  /**
   * The compiler option used to invoke the Non-Empty Checker as the Optional Checker with increased
   * precision.
   */
  private final String RUN_AS_OPTIONAL_CHECKER_KEY = "runAsOptionalChecker";

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
    if (this.hasOptionNoSubcheckers(RUN_AS_OPTIONAL_CHECKER_KEY)) {
      checkers.add(OptionalChecker.class);
    }
    return checkers;
  }

  @Override
  public boolean shouldSkipDefs(MethodTree tree) {
    if (this.hasOptionNoSubcheckers("runAsOptionalChecker")) {
      return !getMethodsToVerify().contains(tree);
    }
    return super.shouldSkipDefs(tree);
  }

  /**
   * Obtains the methods to verify w.r.t. the Non-Empty type system from the Optional Checker. See
   * the class documentation for information about "-ArunAsOptionalChecker".
   *
   * @return the set of methods to be verified by the Non-Empty Checker
   */
  private Set<MethodTree> getMethodsToVerify() {
    assert this.hasOptionNoSubcheckers(RUN_AS_OPTIONAL_CHECKER_KEY);
    OptionalChecker optionalChecker = getSubchecker(OptionalChecker.class);
    assert optionalChecker != null : "@AssumeAssertion(nullness): runAsOptionalChecker is true";
    OptionalVisitor optionalVisitor = (OptionalVisitor) optionalChecker.getVisitor();
    return optionalVisitor.getMethodsToVerifyWithNonEmptyChecker();
  }
}
