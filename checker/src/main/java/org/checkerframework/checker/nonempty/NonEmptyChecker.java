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
 * <p>Runs the {@link org.checkerframework.checker.optional.OptionalImplChecker} (as a subchecker),
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
    // Ideally, the OptionalImplChecker should only be added as a subchecker if this Non-Empty
    // Checker is being run in aggregate with the top-level Optional Checker
    // However, it appears that this information (via SourceChecker#getParentChecker()) is not
    // available at the time when this method is called.
    checkers.add(OptionalImplChecker.class);
    return checkers;
  }

  @Override
  public boolean shouldSkipDefs(MethodTree tree) {
    if (!(this.getParentChecker() instanceof OptionalChecker)) {
      // If the parent checker is null, or if it is NOT an instance of the top-level Optional
      // Checker, this indicates that this Non-Empty Checker is being run independently. In
      // this case, check all definitions, not just the ones related to the Optional Checker's
      // guarantee.
      return false;
    }
    return !getMethodsToVerify().contains(tree.getName().toString());
  }

  /**
   * Obtains the methods to verify w.r.t. the Non-Empty type system from the Optional Checker.
   *
   * @return the set of names of the methods to be verified by the Non-Empty Checker
   * @throws AssertionError if the {@link OptionalImplChecker} is not a subchecker of this checker
   */
  private Set<String> getMethodsToVerify() {
    OptionalImplChecker optionalCheckerImpl = getSubchecker(OptionalImplChecker.class);
    assert optionalCheckerImpl != null;
    OptionalImplVisitor optionalVisitor = (OptionalImplVisitor) optionalCheckerImpl.getVisitor();
    return optionalVisitor.getNamesOfMethodsToVerifyWithNonEmptyChecker();
  }
}
