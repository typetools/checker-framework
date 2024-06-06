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
 * <p>Note: the {@literal disableOptionalChecker} command-line flag is required if users (or tests,
 * for that matter) ever want to run the Non-Empty Checker by itself (i.e., without the Optional
 * Checker as a subchecker). Otherwise, tests that contain code that rely on JDK annotations (or any
 * Non-Empty annotations that aren't explicitly programmer-written in source code) will fail.
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
@SupportedOptions("disableOptionalChecker")
public class NonEmptyChecker extends BaseTypeChecker {

  /** Creates a NonEmptyChecker. */
  public NonEmptyChecker() {
    super();
  }

  @Override
  protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();
    if (!this.hasOptionNoSubcheckers("disableOptionalChecker")) {
      checkers.add(OptionalChecker.class);
    }
    return checkers;
  }

  @Override
  public boolean shouldSkipDefs(MethodTree tree) {
    if (this.hasOptionNoSubcheckers("disableOptionalChecker")) {
      return false;
    }
    return !getMethodsToCheck().contains(tree);
  }

  /**
   * Obtains the methods to check w.r.t. the Non-Empty type system from the Optional Checker.
   *
   * <p>The Optional Checker uses explicitly-written (i.e., programmer-written) annotations from the
   * Non-Empty type system to refine its analysis with respect to operations on containers (e.g.,
   * Streams, Collections) that result in values of type Optional.
   *
   * <p>This method provides access to the Non-Empty Checker for methods that should be verified
   *
   * @return a set of methods to be checked by the Non-Empty Checker
   */
  private Set<MethodTree> getMethodsToCheck() {
    OptionalChecker optionalChecker = getSubchecker(OptionalChecker.class);
    if (optionalChecker != null && optionalChecker.getVisitor() instanceof OptionalVisitor) {
      OptionalVisitor optionalVisitor = (OptionalVisitor) optionalChecker.getVisitor();
      return optionalVisitor.getMethodsForNonEmptyChecker();
    }
    return Collections.emptySet();
  }
}
