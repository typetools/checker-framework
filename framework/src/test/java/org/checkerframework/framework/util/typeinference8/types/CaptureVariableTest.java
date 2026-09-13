package org.checkerframework.framework.util.typeinference8.types;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that {@link CaptureVariable} uses reference equality, unlike {@link Variable}, which
 * compares the type variable and the invocation. Every capture bound introduces fresh capture
 * variables, and two capture bounds for one invocation create two capture variables for the same
 * type variable; those variables stand for different types.
 */
public class CaptureVariableTest {

  /** Creates a new CaptureVariableTest. */
  public CaptureVariableTest() {}

  /**
   * Two capture variables that were created from the same type variable and the same invocation are
   * not equal, and a set retains both of them.
   */
  @Test
  public void captureVariablesUseReferenceEquality() {
    AnnotatedTypeVariable annotatedTypeVariable = typeVariableOfGenericMethod("singletonList");
    TypeVariable typeVariable = annotatedTypeVariable.getUnderlyingType();

    CaptureVariable first =
        new CaptureVariable(annotatedTypeVariable, typeVariable, null, null, null, 1);
    CaptureVariable second =
        new CaptureVariable(annotatedTypeVariable, typeVariable, null, null, null, 2);

    Assert.assertEquals("a capture variable is not equal to itself", first, first);
    Assert.assertNotEquals(
        "two capture variables from one type variable and one invocation are equal", first, second);

    // The consequence of the two being equal would be that a set of inference variables, such as
    // BoundSet.variables, would hold only one of them.
    Set<Variable> variables = new LinkedHashSet<>();
    variables.add(first);
    variables.add(second);
    Assert.assertEquals(2, variables.size());
  }

  /**
   * Non-capture variables, by contrast, are equal if they have the same type variable and the same
   * invocation. This is the behavior that {@link CaptureVariable} must not inherit.
   */
  @Test
  public void nonCaptureVariablesUseValueEquality() {
    AnnotatedTypeVariable annotatedTypeVariable = typeVariableOfGenericMethod("singletonList");
    TypeVariable typeVariable = annotatedTypeVariable.getUnderlyingType();

    Variable first = new Variable(annotatedTypeVariable, typeVariable, null, null, null, 1);
    Variable second = new Variable(annotatedTypeVariable, typeVariable, null, null, null, 2);

    Assert.assertEquals(first, second);
    Assert.assertEquals(first.hashCode(), second.hashCode());
  }

  /**
   * The processing environment for {@link #typeFactory}. All the tests share one, because setting
   * one up is slow and the tests only read from it.
   */
  private static final ProcessingEnvironment env;

  /** Creates the annotated types that the tests use. */
  private static final AnnotatedTypeFactory typeFactory;

  static {
    Context context = new Context();
    env = JavacProcessingEnvironment.instance(context);
    JavaCompiler javac = JavaCompiler.instance(context);
    // The list of modules must be initialized before entering symbols.
    javac.initModules(com.sun.tools.javac.util.List.nil());
    javac.enterDone();

    // Any concrete checker would do; ValueChecker is one that the framework tests already depend
    // on.
    ValueChecker checker = new ValueChecker();
    checker.init(env);
    typeFactory = new AnnotatedTypeFactory(checker);
  }

  /**
   * Returns the first annotated type variable of the given method of {@code java.util.Collections}.
   *
   * @param methodName the simple name of a generic method of {@code java.util.Collections}, that
   *     is, one whose {@code getTypeVariables()} is non-empty
   * @return the first annotated type variable of the given method
   */
  private static AnnotatedTypeVariable typeVariableOfGenericMethod(String methodName) {
    TypeElement collections = env.getElementUtils().getTypeElement("java.util.Collections");
    for (ExecutableElement method : ElementFilter.methodsIn(collections.getEnclosedElements())) {
      if (method.getSimpleName().contentEquals(methodName)) {
        AnnotatedExecutableType methodType =
            (AnnotatedExecutableType)
                AnnotatedTypeMirror.createType(method.asType(), typeFactory, true);
        return methodType.getTypeVariables().get(0);
      }
    }
    throw new AssertionError("java.util.Collections." + methodName + " not found");
  }
}
