package org.checkerframework.framework.util.typeinference8.util;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.typeinference8.InvocationTypeInference;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.InferenceFactory;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * An object to pass around for use during invocation type inference. One context is created per
 * top-level invocation expression.
 */
public class Java8InferenceContext {

  /**
   * Path to the top level expression whose type arguments are inferred. This is the only mutable
   * field in this class; see {@link #setPathToExpression}.
   */
  private TreePath pathToExpression;

  /** javax.annotation.processing.ProcessingEnvironment */
  public final ProcessingEnvironment env;

  /** ProperType for java.lang.Object. */
  public final ProperType object;

  /** Invocation type inference object. */
  public final InvocationTypeInference inference;

  /** com.sun.tools.javac.code.Types */
  public final Types types;

  /** javax.lang.model.util.Types */
  public final javax.lang.model.util.Types modelTypes;

  /** The type of class that encloses the top level expression whose type arguments are inferred. */
  public final DeclaredType enclosingType;

  /**
   * Store previously created type variable to inference variable maps as a map from invocation
   * expression to Theta.
   */
  public final Map<ExpressionTree, Theta> maps;

  /** Number of non-capture variables in this inference problem. */
  private int variableCount = 1;

  /** Number of capture variables in this inference problem. */
  private int captureVariableCount = 1;

  /** Number of qualifier variables in this inference problem. */
  private int qualifierVarCount = 1;

  /** TypeMirror for java.lang.Error. */
  public final TypeMirror error;

  /** TypeMirror for java.lang.RuntimeException. */
  public final TypeMirror runtimeException;

  /** The inference factory. */
  public final InferenceFactory inferenceTypeFactory;

  /** The annotated type factory. */
  public final AnnotatedTypeFactory typeFactory;

  /**
   * Where an implicitly typed lambda parameter's type comes from: the target type of the lambda
   * that declares it, and the parameter's index in the lambda's parameter list.
   *
   * <p>The lambda's target type is stored rather than the parameter's own type because, when this
   * is recorded, the target type may still mention inference variables.
   *
   * @param lambdaTargetType the target type of the lambda that declares the parameter
   * @param index the index of the parameter in the lambda's parameter list
   */
  public record LambdaParamTarget(AbstractType lambdaTargetType, int index) {}

  /**
   * Maps each implicitly typed lambda parameter encountered by this inference problem to the
   * information needed to compute its type.
   *
   * @see InvocationTypeInference#getLambdaParameterType(VariableElement)
   */
  public final Map<VariableElement, LambdaParamTarget> lambdaParamTargets = new HashMap<>();

  /**
   * Records where each parameter of an implicitly typed lambda gets its type from.
   *
   * @param parameters the formal parameters of an implicitly typed lambda
   * @param lambdaTargetType the target type of that lambda
   */
  public void addLambdaParamTargets(
      List<? extends VariableTree> parameters, AbstractType lambdaTargetType) {
    for (int i = 0; i < parameters.size(); i++) {
      lambdaParamTargets.put(
          TreeUtils.elementFromDeclaration(parameters.get(i)),
          new LambdaParamTarget(lambdaTargetType, i));
    }
  }

  /**
   * Creates a context.
   *
   * @param factory type factory
   * @param pathToExpression path to the expression whose type arguments are inferred
   * @param inference inference object
   */
  @SuppressWarnings("this-escape")
  public Java8InferenceContext(
      AnnotatedTypeFactory factory, TreePath pathToExpression, InvocationTypeInference inference) {
    this.typeFactory = factory;
    this.pathToExpression = pathToExpression;
    this.env = factory.getProcessingEnv();
    this.inference = inference;
    JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) env;
    this.types = Types.instance(javacEnv.getContext());
    this.modelTypes = factory.getProcessingEnv().getTypeUtils();
    ClassTree clazz = TreePathUtil.enclosingClass(pathToExpression);
    this.enclosingType = (DeclaredType) TreeUtils.typeOf(clazz);
    this.maps = new HashMap<>();
    this.error = TypesUtils.typeFromClass(Error.class, env.getTypeUtils(), env.getElementUtils());
    this.runtimeException =
        TypesUtils.typeFromClass(RuntimeException.class, env.getTypeUtils(), env.getElementUtils());
    this.inferenceTypeFactory = new InferenceFactory(this);
    this.object = inferenceTypeFactory.getObject();
  }

  /**
   * Returns the path to the expression whose type arguments are inferred.
   *
   * @return the path to the expression whose type arguments are inferred
   */
  public TreePath getPathToExpression() {
    return pathToExpression;
  }

  /**
   * Sets the path to the expression whose type arguments are inferred.
   *
   * <p>This method exists because inference for an outer invocation does not always instantiate the
   * type variables of a method reference that appears within one of its arguments. (The method
   * reference need not be the argument itself; it might be nested, as {@code A::m} is in {@code
   * foo(flag ? A::m : B::m)}.) In that case, inference is run a second time, on the method
   * reference itself, in this same context: the variables and maps that the first run created are
   * still needed, but the target type (see {@link
   * org.checkerframework.framework.util.typeinference8.types.InferenceFactory#getTargetType}) must
   * now be computed with respect to the method reference rather than the outer invocation. Calling
   * this method is what makes that happen, so its effect depends on when it is called relative to
   * the two inference runs.
   *
   * @param pathToExpression the path to the expression whose type arguments are inferred
   */
  public void setPathToExpression(TreePath pathToExpression) {
    this.pathToExpression = pathToExpression;
  }

  /**
   * Returns the next number to use as the id for a non-capture variable. This id is only unique for
   * this inference problem.
   *
   * @return the next number to use as the id for a non-capture variable
   */
  public int getNextVariableId() {
    return variableCount++;
  }

  /**
   * Returns the next number to use as the id for a capture variable. This id is only unique for
   * this inference problem.
   *
   * @return the next number to use as the id for a capture variable
   */
  public int getNextCaptureVariableId() {
    return captureVariableCount++;
  }

  /**
   * Returns the next number to use as the id for a qualifier variable. This id is only unique for
   * this inference problem.
   *
   * @return the next number to use as the id for a qualifier variable
   */
  public int getNextQualifierVariableId() {
    return qualifierVarCount++;
  }
}
