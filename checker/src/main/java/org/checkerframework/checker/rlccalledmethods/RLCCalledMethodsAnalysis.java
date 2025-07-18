package org.checkerframework.checker.rlccalledmethods;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsAnalysis;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.checker.resourceleak.SetOfTypes;

/**
 * This variant of CFAnalysis extends the set of ignored exception types.
 *
 * @see RLCCalledMethodsChecker#getIgnoredExceptions()
 */
public class RLCCalledMethodsAnalysis extends CalledMethodsAnalysis {

  /**
   * The set of exceptions to ignore, cached from {@link
   * ResourceLeakChecker#getIgnoredExceptions()}.
   */
  private final SetOfTypes ignoredExceptions;

  /**
   * Creates a new {@code CalledMethodsAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  protected RLCCalledMethodsAnalysis(
      RLCCalledMethodsChecker checker, CalledMethodsAnnotatedTypeFactory factory) {
    super(checker, factory);
    this.ignoredExceptions = checker.getIgnoredExceptions();
  }

  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return ignoredExceptions.contains(getTypes(), exceptionType);
  }
}
