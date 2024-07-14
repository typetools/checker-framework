package org.checkerframework.checker.nonempty;

import com.sun.source.tree.MethodTree;
import java.util.Collections;
import java.util.Set;
import org.checkerframework.checker.optional.OptionalChecker;
import org.checkerframework.checker.optional.OptionalVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * A type-checker that prevents {@link java.util.NoSuchElementException} in the use of container
 * classes.
 *
 * <p>Note: if "-ArunAsOptionalChecker" is passed, then the Non-Empty Checker first runs the
 * Optional Checker (as a subchecker), then checks only explicitly-written @NonEmpty annotations.
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
@SupportedOptions("runAsOptionalChecker") // See field `runAsOptionalChecker` for documentation.
public class NonEmptyChecker extends BaseTypeChecker {

  /** True if "-ArunAsOptionalChecker" was passed. */
  private boolean runAsOptionalChecker;

  /** Creates a NonEmptyChecker. */
  public NonEmptyChecker() {
    super();
    runAsOptionalChecker = this.hasOptionNoSubcheckers("runAsOptionalChecker");
  }

  @Override
  protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();
    if (runAsOptionalChecker) {
      checkers.add(OptionalChecker.class);
    }
    return checkers;
  }

  @Override
  public boolean shouldSkipDefs(MethodTree tree) {
    if (runAsOptionalChecker) {
      return !getMethodsToVerify().contains(tree);
    }
    return super.shouldSkipDefs(tree);
  }

  /**
   * Obtains the methods to verify w.r.t. the Non-Empty type system from the Optional Checker.
   *
   * <p>The Optional Checker uses explicitly-written (i.e., programmer-written) annotations from the
   * Non-Empty type system to refine its analysis with respect to operations on containers (e.g.,
   * Streams, Collections) that result in values of type Optional.
   *
   * <p>This method provides access to the Non-Empty Checker for methods that should be verified.
   *
   * @return the set of methods to be verified by the Non-Empty Checker
   */
  private Set<MethodTree> getMethodsToVerify() {
    OptionalChecker optionalChecker = getSubchecker(OptionalChecker.class);
    if (optionalChecker != null) {
      OptionalVisitor optionalVisitor = (OptionalVisitor) optionalChecker.getVisitor();
      return optionalVisitor.getMethodsToVerifyWithNonEmptyChecker();
    }
    return Collections.emptySet();
  }
}
