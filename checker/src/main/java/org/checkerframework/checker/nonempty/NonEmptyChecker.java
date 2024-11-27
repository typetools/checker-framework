package org.checkerframework.checker.nonempty;

import com.sun.source.tree.MethodTree;
import java.util.Set;
import org.checkerframework.checker.optional.OptionalChecker;
import org.checkerframework.checker.optional.OptionalImplChecker;
import org.checkerframework.checker.optional.OptionalImplVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * A type-checker that prevents {@link java.util.NoSuchElementException} in the use of container
 * classes.
 *
 * <p>Runs the {@link org.checkerframework.checker.optional.OptionalImplChecker}(as a subchecker),
 * using explicitly-written (i.e., programmer-written) annotations from the Non-Empty type system to
 * refine the Optional analysis. This improves analysis of operations on containers (e.g., Streams,
 * Collections) that result in values of type Optional.
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
@SupportedOptions("checkAllDefs")
public class NonEmptyChecker extends BaseTypeChecker {

  /** Creates a NonEmptyChecker. */
  public NonEmptyChecker() {
    super();
  }

  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends SourceChecker>> checkers = super.getImmediateSubcheckerClasses();
    checkers.add(OptionalImplChecker.class);
    return checkers;
  }

  @Override
  public boolean shouldSkipDefs(MethodTree tree) {
    if (!(this.getParentChecker() instanceof OptionalChecker)) {
      return false;
    }
    return !getMethodsToVerify().contains(tree);
  }

  /**
   * Obtains the methods to verify w.r.t. the Non-Empty type system from the Optional Checker.
   *
   * @return the set of methods to be verified by the Non-Empty Checker
   * @throws AssertionError if invoked when the {@link OptionalImplChecker} is not set as a
   *     subchecker of this checker
   */
  private Set<MethodTree> getMethodsToVerify() {
    OptionalImplChecker optionalCheckerImpl = getSubchecker(OptionalImplChecker.class);
    assert optionalCheckerImpl != null;
    OptionalImplVisitor optionalVisitor = (OptionalImplVisitor) optionalCheckerImpl.getVisitor();
    return optionalVisitor.getMethodsToVerifyWithNonEmptyChecker();
  }
}
