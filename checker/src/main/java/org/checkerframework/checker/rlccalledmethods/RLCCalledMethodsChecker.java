package org.checkerframework.checker.rlccalledmethods;

import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.checker.resourceleak.SetOfTypes;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * The CalledMethodsChecker used as a subchecker in the ResourceLeakChecker and never independently.
 */
public class RLCCalledMethodsChecker extends CalledMethodsChecker {

  /** Creates a RLCCalledMethodsChecker. */
  public RLCCalledMethodsChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new RLCCalledMethodsVisitor(this);
  }

  /**
   * Get the set of exceptions that should be ignored. This set comes from the {@link
   * ResourceLeakChecker#IGNORED_EXCEPTIONS} option if it was provided, or {@link
   * ResourceLeakChecker#DEFAULT_IGNORED_EXCEPTIONS} if not.
   *
   * @return the set of exceptions to ignore
   */
  public SetOfTypes getIgnoredExceptions() {
    return ((RLCCalledMethodsAnnotatedTypeFactory) getTypeFactory())
        .getResourceLeakChecker()
        .getIgnoredExceptions();
  }
}
