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
import java.util.Objects;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.visitor.DoubleAnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.typeinference8.InvocationTypeInference;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.InferenceFactory;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.IPair;

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
   * Scans a type for a polymorphic primary annotation. One scanner serves every call, because
   * {@link
   * org.checkerframework.framework.type.visitor.AnnotatedTypeScanner#visit(org.checkerframework.framework.type.AnnotatedTypeMirror)}
   * resets it.
   */
  private final SimpleAnnotatedTypeScanner<Boolean, Void> polymorphicQualifierScanner;

  /**
   * Replaces each polymorphic primary annotation in one of two types by the annotation at the same
   * position and in the same qualifier hierarchy in the other type. One scanner serves every call,
   * because {@link
   * org.checkerframework.framework.type.visitor.AnnotatedTypeScanner#visit(org.checkerframework.framework.type.AnnotatedTypeMirror,
   * Object)} resets it.
   */
  private final DoubleAnnotatedTypeScanner<Void> polymorphicQualifierReplacer;

  /**
   * The value of {@link AbstractType#inferenceProblemHashCode()} for an {@link AbstractType} of
   * this inference problem whose {@link AbstractType#ignoreAnnotations} field is true.
   */
  private final int inferenceProblemHashIgnoringAnnotations;

  /**
   * The value of {@link AbstractType#inferenceProblemHashCode()} for an {@link AbstractType} of
   * this inference problem whose {@link AbstractType#ignoreAnnotations} field is false.
   */
  private final int inferenceProblemHashNotIgnoringAnnotations;

  /**
   * Returns a hash code for the fields that {@link AbstractType#sameInferenceProblem} compares.
   * Those fields are final, so an {@link AbstractType} of this inference problem has one of only
   * two values: this is computed once per context rather than once per {@link AbstractType}.
   *
   * @param ignoreAnnotations the value of {@link AbstractType#ignoreAnnotations}
   * @return a hash code for the fields that {@link AbstractType#sameInferenceProblem} compares
   */
  public int inferenceProblemHashCode(boolean ignoreAnnotations) {
    return ignoreAnnotations
        ? inferenceProblemHashIgnoringAnnotations
        : inferenceProblemHashNotIgnoringAnnotations;
  }

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
    this.inferenceProblemHashIgnoringAnnotations = Objects.hash(true, this, factory);
    this.inferenceProblemHashNotIgnoringAnnotations = Objects.hash(false, this, factory);
    this.inferenceTypeFactory = new InferenceFactory(this);
    this.object = inferenceTypeFactory.getObject();
    QualifierHierarchy qualifierHierarchy = factory.getQualifierHierarchy();
    this.polymorphicQualifierScanner =
        new SimpleAnnotatedTypeScanner<>(
            (type, p) -> {
              for (AnnotationMirror anno : type.getPrimaryAnnotations()) {
                if (qualifierHierarchy.isPolymorphicQualifier(anno)) {
                  return true;
                }
              }
              return false;
            },
            Boolean::logicalOr,
            false);
    this.polymorphicQualifierReplacer =
        new DoubleAnnotatedTypeScanner<Void>() {
          @Override
          protected Void defaultAction(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
            if (type1 == null || type2 == null) {
              return null;
            }
            for (AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
              AnnotationMirror anno1 = type1.getPrimaryAnnotationInHierarchy(top);
              AnnotationMirror anno2 = type2.getPrimaryAnnotationInHierarchy(top);
              if (anno1 == null || anno2 == null) {
                // A type without a primary annotation in this hierarchy, such as a use of a type
                // variable, has no qualifier here for the other type to conflict with.
                continue;
              }
              if (qualifierHierarchy.isPolymorphicQualifier(anno1)) {
                if (!qualifierHierarchy.isPolymorphicQualifier(anno2)) {
                  type1.replaceAnnotation(anno2);
                }
              } else if (qualifierHierarchy.isPolymorphicQualifier(anno2)) {
                type2.replaceAnnotation(anno1);
              }
            }
            return null;
          }
        };
  }

  /**
   * Returns true if {@code type}, or any type that it contains, has a polymorphic primary
   * annotation.
   *
   * @param type an annotated type
   * @return true if {@code type}, or any type that it contains, has a polymorphic primary
   *     annotation
   */
  public boolean hasPolymorphicQualifier(AnnotatedTypeMirror type) {
    return polymorphicQualifierScanner.visit(type);
  }

  /**
   * Returns copies of {@code type1} and {@code type2} in which each polymorphic primary annotation
   * has been replaced by the annotation at the same position and in the same qualifier hierarchy in
   * the other type. A comparison of the copies therefore succeeds wherever a polymorphic qualifier
   * is compared -- a polymorphic qualifier could be instantiated to whatever it is compared against
   * -- and is unchanged everywhere else.
   *
   * <p>The two types must have the same structure, which holds when their underlying Java types are
   * the same.
   *
   * @param type1 a type
   * @param type2 a type with the same structure as {@code type1}
   * @return copies of the two types, in the order the arguments were given
   */
  public IPair<AnnotatedTypeMirror, AnnotatedTypeMirror> replacePolymorphicQualifiers(
      AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
    AnnotatedTypeMirror copy1 = type1.deepCopy();
    AnnotatedTypeMirror copy2 = type2.deepCopy();
    polymorphicQualifierReplacer.visit(copy1, copy2);
    return IPair.of(copy1, copy2);
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
