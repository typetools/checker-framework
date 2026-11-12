package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.AnnotatedTypeProjection;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.Typing;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.InferenceType;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds.BoundKind;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Resolution;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava17.DeconstructionPatternUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Infers the annotated type of a record pattern, as specified by <a
 * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-18.html#jls-18.5.5">JLS 18.5.5</a>.
 *
 * <p>The type arguments of a record pattern are inferred even when they are written explicitly,
 * because the type of a record pattern cannot be annotated (JLS 14.30.1). The inferred type can
 * then be more specific than the written type: for example, it is {@code Box<String>} when {@code
 * Box<? extends CharSequence>(var x)} is matched against a {@code Supplier<String>}.
 */
public class RecordPatternTypeInference {

  /** The type factory. */
  private final AnnotatedTypeFactory typeFactory;

  /** The {@code DeconstructionPatternTree} whose type is inferred. */
  private final Tree pattern;

  /**
   * The deconstructor of {@link #pattern}. It identifies the inference variables that are created
   * for {@link #pattern}.
   */
  private final ExpressionTree deconstructor;

  /** The inference context. */
  private final Java8InferenceContext context;

  /**
   * Why inference failed.
   *
   * @param message a description of the failure
   * @param crashed true if inference failed because of a bug in the Checker Framework, rather than
   *     because of a qualifier
   */
  public record Failure(String message, boolean crashed) {}

  /**
   * The result of inferring the type of a record pattern.
   *
   * @param matchedType the type of the values that are matched against the record pattern
   * @param failure why inference failed, or null if it did not fail
   * @param componentTypes the types of the record components of the record pattern, in declaration
   *     order; if inference failed, these are the types for the record pattern's Java type with
   *     default annotations
   */
  public record Result(
      AnnotatedTypeMirror matchedType,
      @Nullable Failure failure,
      List<AnnotatedTypeMirror> componentTypes) {}

  /**
   * Creates a {@code RecordPatternTypeInference}.
   *
   * @param typeFactory the type factory
   * @param pathToPattern the path to the {@code DeconstructionPatternTree} whose type is inferred
   */
  public RecordPatternTypeInference(AnnotatedTypeFactory typeFactory, TreePath pathToPattern) {
    this.typeFactory = typeFactory;
    this.pattern = pathToPattern.getLeaf();
    this.deconstructor = DeconstructionPatternUtils.getDeconstructor(pattern);
    this.context = new InvocationTypeInference(typeFactory, pathToPattern).context;
  }

  /**
   * Infers the annotated type of the record pattern when values of {@code matchedType} are matched
   * against it.
   *
   * @param matchedType the type of the values that are matched against the record pattern
   * @return the result of inference
   */
  public Result infer(AnnotatedTypeMirror matchedType) {
    TypeElement record = TypesUtils.getTypeElement(TreeUtils.typeOf(pattern));
    assert record != null : "@AssumeAssertion(nullness): a record pattern's type is a record";
    if (record.getTypeParameters().isEmpty()) {
      return createResult(matchedType, null, typeFactory.getDefaultRecordPatternType(pattern));
    }
    try {
      return inferGeneric(record, matchedType);
    } catch (Exception ex) {
      // javac inferred the type arguments of the pattern, so the Checker Framework should too.
      if (!typeFactory
          .getChecker()
          .getBooleanOption("convertTypeArgInferenceCrashToWarning", true)) {
        throw BugInCF.addLocation(pattern, ex);
      }
      return createFailedResult(
          matchedType, new Failure("An exception occurred: " + ex.getLocalizedMessage(), true));
    }
  }

  /**
   * Returns the result of inference when the record pattern has type {@code patternType}. The
   * primary qualifiers of {@code patternType} are replaced by those of {@code matchedType}, limited
   * to the qualifier upper bound of the record type, as for a cast to the record type.
   *
   * @param matchedType the type of the values that are matched against the record pattern
   * @param failure why inference failed, or null if it did not fail
   * @param patternType the annotated type of the record pattern; side-effected by this method
   * @return the result of inference
   */
  private Result createResult(
      AnnotatedTypeMirror matchedType,
      @Nullable Failure failure,
      AnnotatedDeclaredType patternType) {
    // The primary qualifiers of the pattern type are those of the receiver of each accessor, which
    // determine the accessor's polymorphic qualifiers.
    patternType.replaceAnnotations(
        typeFactory
            .getQualifierUpperBounds()
            .limitToBound(patternType.getUnderlyingType(), matchedType.getAnnotations()));
    return new Result(
        matchedType.deepCopy(), failure, typeFactory.getRecordComponentTypes(patternType));
  }

  /**
   * Returns the result of inference when it fails. The record pattern then has its Java type with
   * default annotations.
   *
   * @param matchedType the type of the values that are matched against the record pattern
   * @param failure why inference failed
   * @return the result of inference
   */
  private Result createFailedResult(AnnotatedTypeMirror matchedType, Failure failure) {
    return createResult(matchedType, failure, typeFactory.getDefaultRecordPatternType(pattern));
  }

  /**
   * Infers the annotated type of the record pattern for the generic record class {@code record}.
   *
   * @param record the record class of the record pattern; it is generic
   * @param matchedType the type of the values that are matched against the record pattern
   * @return the result of inference
   */
  private Result inferGeneric(TypeElement record, AnnotatedTypeMirror matchedType) {
    // Step 2: the inference variables alpha1, ..., alphan, with bounds from the declared bounds.
    Theta map = context.inferenceTypeFactory.createThetaForTypeParameters(deconstructor, record);
    List<Variable> alphas = new ArrayList<>(map.values());

    // Step 3: capture each parameterized type T (or each one that bounds T), and replace the
    // captured type variables by the inference variables beta1, ..., betak.
    List<AnnotatedDeclaredType> capturedTypes = new ArrayList<>();
    addCapturedParameterizedTypes(matchedType, capturedTypes);
    context.inferenceTypeFactory.addVariablesForCapturedTypeArguments(
        deconstructor, capturedTypes, map);
    BoundSet boundSet = BoundSet.initialBounds(map, context);

    // Step 4: reduce <T' = R'>, where R' is the supertype of R<alpha1, ..., alphan> that is a
    // parameterization of the same generic class as T'.
    AbstractType recordType = context.inferenceTypeFactory.getTypeOfElement(record, map);
    for (AnnotatedDeclaredType capturedType : capturedTypes) {
      AbstractType rPrime = recordType.asSuper(capturedType.getUnderlyingType());
      if (rPrime == null) {
        continue;
      }
      AbstractType tPrime = InferenceType.create(capturedType, map, context);
      ConstraintSet constraints =
          new ConstraintSet(
              new Typing("Record pattern " + pattern, tPrime, rPrime, Kind.TYPE_EQUALITY));
      boundSet.incorporateToFixedPoint(constraints.reduce(context));
    }
    if (boundSet.containsFalse()) {
      throw new BugInCF("Record pattern inference produced the false bound for %s", pattern);
    }
    if (boundSet.annoInferenceFailed) {
      return createFailedResult(matchedType, new Failure(boundSet.errorMsg, false));
    }
    // Two proper bounds v = S and v = T reduce to <S = T>, which ignores qualifiers, so check that
    // the qualifiers of all of them agree.
    for (Variable v : map.values()) {
      ProperType instantiation = v.getInstantiation();
      if (instantiation == null) {
        continue;
      }
      for (AbstractType bound : v.getBounds().getBoundsOfKind(BoundKind.EQUAL)) {
        if (bound.isProper()
            && ((ProperType) bound).checkAnnotationEquality(instantiation) != ConstraintSet.TRUE) {
          return createFailedResult(
              matchedType,
              new Failure(
                  bound.getAnnotatedType() + " = " + instantiation.getAnnotatedType(), false));
        }
      }
    }

    // Step 5: resolve, skipping straight to fresh type variables.
    Resolution.resolveWithFreshTypeVariables(new ArrayList<>(alphas), boundSet, context);

    // Step 6: the upward projection of R<A1, ..., An>.
    List<AnnotatedTypeMirror> typeArgs = new ArrayList<>(alphas.size());
    TypeMirror[] javaTypeArgs = new TypeMirror[alphas.size()];
    for (int i = 0; i < alphas.size(); i++) {
      ProperType instantiation = alphas.get(i).getInstantiation();
      if (instantiation == null) {
        throw new BugInCF("Record pattern inference did not instantiate %s", alphas.get(i));
      }
      typeArgs.add(instantiation.getAnnotatedType());
      javaTypeArgs[i] = instantiation.getJavaType();
    }
    DeclaredType javaType = typeFactory.types.getDeclaredType(record, javaTypeArgs);
    AnnotatedDeclaredType patternType =
        (AnnotatedDeclaredType) AnnotatedTypeMirror.createType(javaType, typeFactory, false);
    patternType.addAnnotations(
        typeFactory.getDefaultRecordPatternType(pattern).getPrimaryAnnotations());
    patternType.setTypeArguments(typeArgs);
    // Project only the fresh type variables that resolution introduced, not the captured type
    // variables that the matched type already mentions.
    List<TypeVariable> matchedTypeVariables = new ArrayList<>();
    addCapturedTypeVariables(matchedType.getUnderlyingType(), matchedTypeVariables);
    return createResult(
        matchedType,
        null,
        (AnnotatedDeclaredType)
            AnnotatedTypeProjection.upward(typeFactory, patternType, matchedTypeVariables));
  }

  /**
   * Adds to {@code typeVariables} each captured type variable that {@code type} mentions.
   *
   * @param type a type
   * @param typeVariables the list to add to
   */
  private static void addCapturedTypeVariables(TypeMirror type, List<TypeVariable> typeVariables) {
    switch (type.getKind()) {
      case TYPEVAR -> {
        if (TypesUtils.isCapturedTypeVariable(type)) {
          typeVariables.add((TypeVariable) type);
        }
      }
      case DECLARED -> {
        DeclaredType declaredType = (DeclaredType) type;
        for (TypeMirror typeArg : declaredType.getTypeArguments()) {
          addCapturedTypeVariables(typeArg, typeVariables);
        }
        addCapturedTypeVariables(declaredType.getEnclosingType(), typeVariables);
      }
      case ARRAY -> addCapturedTypeVariables(((ArrayType) type).getComponentType(), typeVariables);
      case WILDCARD -> {
        WildcardType wildcard = (WildcardType) type;
        if (wildcard.getExtendsBound() != null) {
          addCapturedTypeVariables(wildcard.getExtendsBound(), typeVariables);
        }
        if (wildcard.getSuperBound() != null) {
          addCapturedTypeVariables(wildcard.getSuperBound(), typeVariables);
        }
      }
      case INTERSECTION -> {
        for (TypeMirror bound : ((IntersectionType) type).getBounds()) {
          addCapturedTypeVariables(bound, typeVariables);
        }
      }
      default -> {}
    }
  }

  /**
   * Adds to {@code capturedTypes} the capture of {@code type} if it is a parameterized type, or of
   * each of its bounds if it is a type variable or intersection type (JLS 18.5.5, step 3).
   *
   * @param type the type of values that are matched against the record pattern, or a bound of it
   * @param capturedTypes the list to add to
   */
  private void addCapturedParameterizedTypes(
      AnnotatedTypeMirror type, List<AnnotatedDeclaredType> capturedTypes) {
    switch (type.getKind()) {
      case TYPEVAR ->
          addCapturedParameterizedTypes(
              ((AnnotatedTypeVariable) type).getUpperBound(), capturedTypes);
      case INTERSECTION -> {
        for (AnnotatedTypeMirror bound : ((AnnotatedIntersectionType) type).getBounds()) {
          addCapturedParameterizedTypes(bound, capturedTypes);
        }
      }
      case DECLARED -> {
        AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) type;
        if (!declaredType.getTypeArguments().isEmpty() && !declaredType.isUnderlyingTypeRaw()) {
          capturedTypes.add(
              (AnnotatedDeclaredType) typeFactory.applyCaptureConversion(declaredType));
        }
      }
      default -> {}
    }
  }
}
