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
 * <p>Runs the {@link org.checkerframework.checker.optional.OptionalImplChecker}(as a subchecker),
 * using explicitly-written (i.e., programmer-written) annotations from the Non-Empty type system to
 * refine the Optional analysis. This improves analysis of operations on containers (e.g., Streams,
 * Collections) that result in values of type Optional.
 *
 * <p>The Non-Empty Checker's default modality is to only verify (i.e., establish guarantees) for
 * methods that are passed to it by the Optional Checker. Running the Non-Empty Checker in this
 * default modality does <i>not</i> guarantee the absence of {@link
 * java.util.NoSuchElementException} across the <i>entire</i> program.
 *
 * <p>If "-AcheckAllDefs" is passed, then the Non-Empty Checker will attempt to verify <i>all</i>
 * method definitions to prevent {@link java.util.NoSuchElementException}. This modality introduces
 * a larger number of false positives and errors, and so it is not generally supported for users.
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
    if (hasOptionNoSubcheckers("checkAllDefs")) {
      // The "-AcheckAllDefs" flag was passed on the command-line, indicating that the user
      // wants to verify the entire program, not just the methods collected by the Optional Checker.
      // Consequently, do not skip any method definitions.
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
