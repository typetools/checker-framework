package org.checkerframework.checker.nonempty;

import com.sun.source.tree.MethodTree;
import java.util.Set;
import org.checkerframework.checker.optional.OptionalChecker;
import org.checkerframework.checker.optional.OptionalImplChecker;
import org.checkerframework.checker.optional.OptionalImplVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SourceChecker;

/**
 * A type-checker that prevents {@link java.util.NoSuchElementException} in the use of container
 * classes.
 *
 * <p>Runs the {@link org.checkerframework.checker.optional.OptionalImplChecker} (as a subchecker)
 * by default. This checker should not yet be run as a standalone checker. The Non-Empty Checker
 * uses explicitly-written (i.e., programmer-written) annotations from the Non-Empty type system to
 * refine the analysis of operations on containers (e.g., Streams, Collections) that result in
 * values of type {@link java.util.Optional}.
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
public class NonEmptyChecker extends BaseTypeChecker {

  /** A cached instance of {@link OptionalImplVisitor} */
  private OptionalImplVisitor optionalImplVisitor;

  /** Creates a NonEmptyChecker. */
  public NonEmptyChecker() {
    super();
  }

  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends SourceChecker>> checkers = super.getImmediateSubcheckerClasses();
    if (this.parentChecker instanceof OptionalChecker) {
      // Add the OptionalImplChecker as a subchecker if this Non-Empty
      // Checker is being run in aggregate with the top-level Optional Checker
      checkers.add(OptionalImplChecker.class);
    }
    return checkers;
  }

  @Override
  public boolean shouldSkipDefs(MethodTree tree) {
    if (!(this.parentChecker instanceof OptionalChecker)) {
      // The parent checker being null, or not being an instance of the top-level Optional Checker
      // indicates that this Non-Empty Checker is being run independently.
      // Check all definitions in this case, not just the ones relevant to the Optional Checker's
      // guarantee.
      return super.shouldSkipDefs(tree);
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
    if (optionalImplVisitor == null) {
      OptionalImplChecker optionalCheckerImpl = getSubchecker(OptionalImplChecker.class);
      assert optionalCheckerImpl != null;
      optionalImplVisitor = (OptionalImplVisitor) optionalCheckerImpl.getVisitor();
    }
    return optionalImplVisitor.getNamesOfMethodsToVerifyWithNonEmptyChecker();
  }
}
