package org.checkerframework.checker.collectionownership;

import java.util.Set;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SourceChecker;

/**
 * This typechecker tracks at most one owning variable per resource collection using an ownership
 * type system. The Resource Leak Checker verifies that (at least) the determined owner fulfills the
 * calling obligations of its elements.
 */
public class CollectionOwnershipChecker extends BaseTypeChecker {

  /** Creates a CollectionOwnershipChecker. */
  public CollectionOwnershipChecker() {}

  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends SourceChecker>> checkers = super.getImmediateSubcheckerClasses();
    checkers.add(RLCCalledMethodsChecker.class);
    return checkers;
  }
}
