package org.checkerframework.checker.rlccalledmethods;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsAnalysis;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.SetOfTypes;
import org.checkerframework.common.accumulation.AccumulationStore;

/**
 * This variant of CFAnalysis extends the set of ignored exception types.
 *
 * @see RLCCalledMethodsChecker#getIgnoredExceptions()
 */
public class RLCCalledMethodsAnalysis extends CalledMethodsAnalysis {

  /**
   * The set of exceptions to ignore, cached from {@link
   * RLCCalledMethodsChecker#getIgnoredExceptions()}.
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

  @Override
  public AccumulationStore createEmptyStore(boolean sequentialSemantics) {
    return new RLCCalledMethodsStore(this, sequentialSemantics);
  }

  @Override
  public AccumulationStore createCopiedStore(AccumulationStore store) {
    return new RLCCalledMethodsStore(store);
  }
}
