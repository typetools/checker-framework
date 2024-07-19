package org.checkerframework.checker.resourceleak;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.accumulation.AccumulationAnalysis;

/**
 * This variant of CFAnalysis extends the set of ignored exception types.
 *
 * @see ResourceLeakChecker#getIgnoredExceptions()
 */
public class ResourceLeakAnalysis extends AccumulationAnalysis {

  /**
   * The set of exceptions to ignore, cached from {@link
   * ResourceLeakChecker#getIgnoredExceptions()}.
   */
  private final SetOfTypes ignoredExceptions;

  /**
   * Creates a new {@code ResourceLeakAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  protected ResourceLeakAnalysis(
      ResourceLeakChecker checker, ResourceLeakAnnotatedTypeFactory factory) {
    super(checker, factory);
    this.ignoredExceptions = checker.getIgnoredExceptions();
  }

  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return ignoredExceptions.contains(getTypes(), exceptionType);
  }
}
