package org.checkerframework.framework.util.typeinference8.util;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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

  /** Path to the top level expression whose type arguments are inferred. */
  public TreePath pathToExpression;

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

  /** There's no way to tell if an element is a parameter of a lambda, so keep track of them. */
  public final Set<VariableElement> lambdaParms = new HashSet<>();

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
   * information needed to compute its type once inference has progressed far enough.
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

  /**
   * Adds the parameters to the list of trees that are lambda parameters.
   *
   * <p>There's no way to tell if a tree is a parameter of a lambda, so keep track of them.
   *
   * @param parameters list of lambda parameters
   */
  public void addLambdaParms(List<? extends VariableTree> parameters) {
    for (VariableTree tree : parameters) {
      lambdaParms.add(TreeUtils.elementFromDeclaration(tree));
    }
  }

  /**
   * Returns true if the {@code expression} is a lambda parameter.
   *
   * @param expression an expression
   * @return true if the {@code expression} is a lambda parameter
   */
  public boolean isLambdaParam(ExpressionTree expression) {
    Element element = TreeUtils.elementFromTree(expression);
    if (element == null || element.getKind() != ElementKind.PARAMETER) {
      return false;
    }
    return lambdaParms.contains((VariableElement) element);
  }
}
