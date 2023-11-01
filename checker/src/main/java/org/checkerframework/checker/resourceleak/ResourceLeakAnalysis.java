package org.checkerframework.checker.resourceleak;

import com.sun.tools.javac.code.Type;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsAnalysis;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;

/**
 * This variant of CFAnalysis extends the set of ignored exception types.
 *
 * @see ResourceLeakChecker#getIgnoredExceptions()
 */
public class ResourceLeakAnalysis extends CalledMethodsAnalysis {

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
  protected ResourceLeakAnalysis(
      ResourceLeakChecker checker, CalledMethodsAnnotatedTypeFactory factory) {
    super(checker, factory);
    this.ignoredExceptions = checker.getIgnoredExceptions();
  }

  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    if (exceptionType.getKind() == TypeKind.DECLARED) {
      return ignoredExceptionTypes.contains(
          ((Type) exceptionType).tsym.getQualifiedName().toString());
    }
    return false;
  }
}
