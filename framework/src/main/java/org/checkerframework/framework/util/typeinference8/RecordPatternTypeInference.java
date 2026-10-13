package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
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
import org.checkerframework.framework.util.typeinference8.util.FalseBoundException;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Resolution;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava17.DeconstructionPatternUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Infers the annotated type of a record pattern, as specified by <a
 * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-18.html#jls-18.5.5">JLS 18.5.5</a>.
 *
 * <p>The type arguments of a record pattern are inferred even when they are written explicitly,
 * because the type of a record pattern cannot be annotated (JLS 14.30.1).
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
   * A description of why inference failed, or null if it has not failed or failed without a
   * description.
   */
  private @Nullable String errorMessage = null;

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
   * Returns the annotated type of the record pattern when values of {@code matchedType} are matched
   * against it, or null if inference fails. If inference fails because of a qualifier, {@link
   * #getErrorMessage} then describes the failure.
   *
   * @param matchedType the type of the values that are matched against the record pattern
   * @return the annotated type of the record pattern, or null if inference fails
   */
  public @Nullable AnnotatedDeclaredType infer(AnnotatedTypeMirror matchedType) {
    TypeElement record = TypesUtils.getTypeElement(TreeUtils.typeOf(pattern));
    assert record != null : "@AssumeAssertion(nullness): a record pattern's type is a record";
    if (record.getTypeParameters().isEmpty()) {
      return typeFactory.getDefaultRecordPatternType(pattern);
    }
    try {
      return inferGeneric(record, matchedType);
    } catch (FalseBoundException e) {
      return null;
    }
  }

  /**
   * Returns a description of why the most recent call to {@link #infer} failed, or null if it did
   * not fail because of a qualifier.
   *
   * @return a description of why inference failed, or null
   */
  public @Nullable String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Returns the annotated type of the record pattern for the generic record class {@code record},
   * or null if inference fails.
   *
   * @param record the record class of the record pattern; it is generic
   * @param matchedType the type of the values that are matched against the record pattern
   * @return the annotated type of the record pattern, or null if inference fails
   */
  private @Nullable AnnotatedDeclaredType inferGeneric(
      TypeElement record, AnnotatedTypeMirror matchedType) {
    // Step 2: the inference variables alpha1, ..., alphan, with bounds from the declared bounds.
    Theta map = context.inferenceTypeFactory.createThetaForRecordPattern(deconstructor, record);
    List<Variable> alphas = new ArrayList<>(map.values());

    // Step 3: capture each parameterized type T (or each one that bounds T), and replace the
    // captured type variables by the inference variables beta1, ..., betak.
    List<AnnotatedDeclaredType> capturedTypes = new ArrayList<>();
    addCapturedParameterizedTypes(matchedType, capturedTypes);
    for (AnnotatedDeclaredType capturedType : capturedTypes) {
      context.inferenceTypeFactory.addVariablesForCapturedTypeArguments(
          deconstructor, capturedType, map);
    }
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
      return null;
    }
    // Two proper bounds alpha = S and alpha = T reduce to <S = T>, which ignores qualifiers, so
    // check that the qualifiers of all of them agree.
    for (Variable alpha : alphas) {
      ProperType instantiation = alpha.getInstantiation();
      if (instantiation == null) {
        continue;
      }
      for (AbstractType bound : alpha.getBounds().getBoundsOfKind(BoundKind.EQUAL)) {
        if (bound.isProper()
            && ((ProperType) bound).checkAnnotationEquality(instantiation) != ConstraintSet.TRUE) {
          errorMessage = bound.getAnnotatedType() + " = " + instantiation.getAnnotatedType();
          return null;
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
        return null;
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
    // The JLS projects only the fresh type variables that resolution introduced, but this projects
    // all captured type variables.  The component types of the pattern are the same either way,
    // because a pattern variable's type is projected with respect to all of them (JLS 14.30.1).
    return (AnnotatedDeclaredType) AnnotatedTypeProjection.upward(typeFactory, patternType);
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
