package org.checkerframework.checker.resourceleak;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsAnalysis;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesMustCallForChecker;
import org.checkerframework.checker.mustcall.SetOfTypes;

/**
 * This variant of CFAnalysis extends the set of ignored exception types.
 *
 * @see MustCallChecker#getIgnoredExceptions()
 */
public class ResourceLeakAnalysis extends CalledMethodsAnalysis {

  /**
   * The set of exceptions to ignore, cached from {@link MustCallChecker#getIgnoredExceptions()}.
   */
  private final SetOfTypes ignoredExceptions;

  /**
   * Creates a new {@code CalledMethodsAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  protected ResourceLeakAnalysis(
      ResourceLeakChecker checker, CalledMethodsAnnotatedTypeFactory factory) {
    super(checker, factory);
    MustCallChecker mustCallChecker = checker.getSubchecker(MustCallChecker.class);
    if (mustCallChecker == null) {
      mustCallChecker =
          (MustCallChecker)
              checker.getSubchecker(MustCallNoCreatesMustCallForChecker.class).getParentChecker();
    }
    this.ignoredExceptions = mustCallChecker.getIgnoredExceptions();
  }

  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return ignoredExceptions.contains(getTypes(), exceptionType);
  }
}
