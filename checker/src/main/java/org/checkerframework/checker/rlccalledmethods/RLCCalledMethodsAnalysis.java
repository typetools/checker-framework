package org.checkerframework.checker.rlccalledmethods;

import org.checkerframework.checker.calledmethods.CalledMethodsAnalysis;

/** The RLCCalledMethodsAnalysis, extends the set of ignored exception types. */
public class RLCCalledMethodsAnalysis extends CalledMethodsAnalysis {

  // /**
  //  * The set of exceptions to ignore, cached from {@link
  //  * ResourceLeakChecker#getIgnoredExceptions()}.
  //  */
  // private final SetOfTypes ignoredExceptions;

  /**
   * Creates a new {@code RLCCalledMethodsAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  protected RLCCalledMethodsAnalysis(
      RLCCalledMethodsChecker checker, RLCCalledMethodsAnnotatedTypeFactory factory) {
    super(checker, factory);
    // this.ignoredExceptions = checker.getIgnoredExceptions();
  }

  // @Override
  // public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
  //   return ignoredExceptions.contains(getTypes(), exceptionType);
  // }
}
