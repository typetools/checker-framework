package org.checkerframework.checker.nonempty;

import com.sun.source.tree.MethodTree;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.optional.OptionalChecker;
import org.checkerframework.checker.optional.OptionalVisitor;
import org.checkerframework.checker.regex.qual.Regex;
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
    return checkers;
  }

  @Override
  public Map<String, String> getOptions() {
    Map<String, String> options = new HashMap<>(super.getOptions());
    OptionalChecker optionalChecker = this.getSubchecker(OptionalChecker.class);
    if (optionalChecker != null && optionalChecker.getVisitor() instanceof OptionalVisitor) {
      OptionalVisitor optionalVisitor = (OptionalVisitor) optionalChecker.getVisitor();
      Set<MethodTree> methodsToCheck = optionalVisitor.getMethodsForNonEmptyChecker();
      String namesOfMethodsToCheck = getNamesOfMethodsToCheck(methodsToCheck);
      options.put("onlyDefs", namesOfMethodsToCheck);
    }
    return options;
  }

  /**
   * Create a regex that matches the names of all methods in the given set of methods.
   *
   * @param methodsToCheck the set of methods that should be checked by the Non-Empty Checker
   * @return a regex that matches the names of all methods in the given set of methods
   */
  private @Regex String getNamesOfMethodsToCheck(Set<MethodTree> methodsToCheck) {
    return methodsToCheck.stream().map(MethodTree::getName).collect(Collectors.joining("|"));
  }
}
