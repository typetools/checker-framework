package org.checkerframework.checker.nonempty;

import com.sun.source.tree.MethodTree;
import java.util.Collections;
import java.util.Set;
import org.checkerframework.checker.optional.OptionalChecker;
import org.checkerframework.checker.optional.OptionalVisitor;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A type-checker that prevents {@link java.util.NoSuchElementException} in the use of container
 * classes.
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
public class NonEmptyChecker extends DelegationChecker {

  @Override
  protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();
    checkers.add(OptionalChecker.class);
    checkers.add(AliasingChecker.class);
    return checkers;
  }

  @Override
  public boolean shouldSkipDefs(MethodTree tree) {
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
