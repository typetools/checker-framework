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
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava17.DeconstructionPatternUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Infers the annotated type of a record pattern, the way that <a
 * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-18.html#jls-18.5.5">JLS 18.5.5</a>
 * infers its Java type.
 *
 * <p>This class differs from JLS 18.5.5 in these ways:
 *
 * <ul>
 *   <li>This class infers the type arguments of a record pattern even when the programmer writes
 *       them, because the type of a record pattern cannot be annotated (JLS 14.30.1). The inferred
 *       type can then be more specific than the written type: for example, it is {@code
 *       Box<String>} when a value of type {@code Supplier<String>} is matched against {@code Box<?
 *       extends CharSequence>(var x)}.
 *   <li>This class omits step 1, because javac has already checked that the matched type is checked
 *       cast convertible to the raw record class.
 *   <li>If the bound set contains the bound false, this class throws {@link BugInCF} rather than
 *       failing, because javac accepted the record pattern. An exception is a nested record
 *       pattern that no value of its matched type can match; see {@link
 *       #createImpossibleMatchResult}.
 *   <li>Inference fails if the qualifier bounds cannot be satisfied, or if two proper equal bounds
 *       of an inference variable have different qualifiers. JLS 18.5.5 has no qualifiers.
 * </ul>
 */
public class RecordPatternTypeInference {

  /** The type factory. */
  private final AnnotatedTypeFactory typeFactory;

  /** The {@code DeconstructionPatternTree} whose type this object infers. */
  private final Tree pattern;

  /**
   * The deconstructor of {@link #pattern}: the tree for which this object creates inference
   * variables.
   */
  private final ExpressionTree deconstructor;

  /** The path to {@link #pattern}. */
  private final TreePath pathToPattern;

  /**
   * Why inference failed.
   *
   * @param message a description of the failure
   * @param crashed if true, inference failed because of a bug in the Checker Framework; if false,
   *     inference failed because the qualifier bounds cannot be satisfied or conflict
   */
  public record Failure(String message, boolean crashed) {}

  /**
   * The result of inferring the type of a record pattern: the matched type, why inference failed
   * (if it did), and the types of the record components as members of the inferred type.
   *
   * @param matchedType the type of the values that are matched against the record pattern
   * @param failure why inference failed, or null if it did not fail
   * @param componentTypes the types of the record components as members of the inferred
   *     parameterization of the record class, before upward projection, in declaration order; if
   *     inference failed, they are members of the Java type of the record pattern with default
   *     annotations, except that its primary qualifiers are those of {@code matchedType}, limited
   *     to the qualifier upper bound of the record class
   */
  public record Result(
      AnnotatedTypeMirror matchedType,
      @Nullable Failure failure,
      List<AnnotatedTypeMirror> componentTypes) {}

  /**
   * Creates a {@code RecordPatternTypeInference}.
   *
   * @param typeFactory the type factory
   * @param pathToPattern the path to the {@code DeconstructionPatternTree} whose type this object
   *     infers
   */
  public RecordPatternTypeInference(AnnotatedTypeFactory typeFactory, TreePath pathToPattern) {
    this.typeFactory = typeFactory;
    this.pattern = pathToPattern.getLeaf();
    this.deconstructor = DeconstructionPatternUtils.getDeconstructor(pattern);
    this.pathToPattern = pathToPattern;
  }

  /**
   * Infers the annotated type of the record pattern when values of {@code matchedType} are matched
   * against it.
   *
   * <p>If the record class is not generic, the type of the record pattern is its Java type with
   * default annotations, except that its primary qualifiers are those of {@code matchedType},
   * limited to the qualifier upper bound of the record class.
   *
   * <p>If inference throws an exception, this method throws {@link BugInCF} if the option {@code
   * -AconvertTypeArgInferenceCrashToWarning} is false, and otherwise returns a failed result whose
   * {@code crashed} is true.
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
      return inferForGenericRecord(record, matchedType);
    } catch (Exception ex) {
      // javac accepted the record pattern, so an exception here is a bug in the Checker Framework.
      if (!typeFactory
          .getChecker()
          .getBooleanOption("convertTypeArgInferenceCrashToWarning", true)) {
        throw BugInCF.addLocation(pattern, ex);
      }
      return createFailedResult(matchedType, new Failure("An exception occurred: " + ex, true));
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
    // The primary qualifiers of the pattern type are the receiver qualifiers of each accessor call,
    // so they determine how each accessor's polymorphic qualifiers are resolved.
    patternType.replaceAnnotations(
        typeFactory
            .getQualifierUpperBounds()
            .limitToBound(patternType.getUnderlyingType(), matchedType.getAnnotations()));
    return new Result(
        matchedType.deepCopy(), failure, typeFactory.getRecordComponentTypes(patternType));
  }

  /**
   * Returns the result of inference when it fails. The type of the record pattern is its Java type
   * with default annotations, except that its primary qualifiers are those of {@code matchedType},
   * limited to the qualifier upper bound of the record class.
   *
   * @param matchedType the type of the values that are matched against the record pattern
   * @param failure why inference failed
   * @return the result of inference
   */
  private Result createFailedResult(AnnotatedTypeMirror matchedType, Failure failure) {
    return createResult(matchedType, failure, typeFactory.getDefaultRecordPatternType(pattern));
  }

  /**
   * Returns true if the record pattern is nested in another record pattern.
   *
   * @return true if the record pattern is nested in another record pattern
   */
  private boolean isNested() {
    return TreeUtils.isDeconstructionPatternTree(pathToPattern.getParentPath().getLeaf());
  }

  /**
   * Returns the result of inference for a nested record pattern that no value of {@code
   * matchedType} can match.
   *
   * <p>javac checks a nested record pattern against the type of the record component in the Java
   * type of the enclosing record pattern. The matched type of a nested record pattern is the type of
   * the record component in the inferred type of the enclosing record pattern, which can be more
   * specific. For example, if {@code record Outer<T>(T value) implements Supplier<T>} and {@code
   * record Inner<U>(U value)}, and a {@code Supplier<ArrayList<String>>} is matched against {@code
   * Outer<?>(Inner(var x))}, then javac checks {@code Inner} against the capture of {@code ?}, but
   * the matched type of {@code Inner(var x)} is {@code ArrayList<String>}. No {@code ArrayList} is
   * an {@code Inner}, so the nested record pattern never matches, and any type for it is sound. Like
   * javac, this method uses the Java type of the record pattern.
   *
   * @param matchedType the type of the values that are matched against the record pattern
   * @return the result of inference
   */
  private Result createImpossibleMatchResult(AnnotatedTypeMirror matchedType) {
    return createResult(matchedType, null, typeFactory.getDefaultRecordPatternType(pattern));
  }

  /**
   * Infers the annotated type of the record pattern, whose record class is {@code record}.
   *
   * @param record the record class of the record pattern; must be generic
   * @param matchedType the type of the values that are matched against the record pattern
   * @return the result of inference
   */
  private Result inferForGenericRecord(TypeElement record, AnnotatedTypeMirror matchedType) {
    Java8InferenceContext context = new InvocationTypeInference(typeFactory, pathToPattern).context;
    // Step 2: create inference variables alpha1, ..., alphan (`alphas`) for the type parameters
    // P1, ..., Pn of the record class R (`record`), with bounds from the declared bounds of P1,
    // ..., Pn.
    Theta alphaMap =
        context.inferenceTypeFactory.createThetaForTypeParameters(deconstructor, record);
    List<Variable> alphas = new ArrayList<>(alphaMap.values());

    // Step 3: capture the matched type T (`matchedType`) if it is a parameterized type, or else
    // each parameterized type among its upper bounds, recursively.  Create inference variables
    // beta1, ..., betak to replace the captured type variables that are type arguments of the
    // captures.  The betas are in a separate Theta from the alphas, because only captured type
    // variables are replaced in the matched type.  For example, within `record Box<T>`, the
    // matched type `Box<T>` mentions the record's type parameter T, which must not be replaced
    // by its alpha.
    List<AnnotatedDeclaredType> capturedTypes = new ArrayList<>();
    addCapturedParameterizedTypes(matchedType, capturedTypes);
    Theta betaMap = new Theta();
    context.inferenceTypeFactory.addVariablesForCapturedTypeArguments(
        deconstructor, capturedTypes, betaMap);
    Theta map = new Theta();
    map.putAll(alphaMap);
    map.putAll(betaMap);
    BoundSet boundSet = BoundSet.initialBounds(map, context);

    // Step 4: for each capture T' (`tPrime`, with each captured type variable replaced by its
    // beta), reduce <T' = R'>, where R' (`rPrime`) is the supertype of R<alpha1, ..., alphan>
    // (`recordType`) that is a parameterization of the same generic class as T'.
    AbstractType recordType = context.inferenceTypeFactory.getTypeOfElement(record, alphaMap);
    for (AnnotatedDeclaredType capturedType : capturedTypes) {
      AbstractType rPrime = recordType.asSuper(capturedType.getUnderlyingType());
      if (rPrime == null) {
        if (isNested()) {
          return createImpossibleMatchResult(matchedType);
        }
        // javac checked that the matched type is castable to the record class, which is final, so
        // the record class is a subtype of every parameterized type that bounds the matched type.
        throw new BugInCF(
            "Record pattern %s: %s is not a supertype of %s",
            TreeUtils.toStringTruncated(pattern, 65), capturedType, recordType);
      }
      AbstractType tPrime = InferenceType.create(capturedType, betaMap, context);
      ConstraintSet constraints =
          new ConstraintSet(
              new Typing("Record pattern " + pattern, tPrime, rPrime, Kind.TYPE_EQUALITY));
      try {
        boundSet.incorporateToFixedPoint(constraints.reduce(context));
      } catch (FalseBoundException ex) {
        if (isNested()) {
          return createImpossibleMatchResult(matchedType);
        }
        throw ex;
      }
    }
    if (boundSet.containsFalse()) {
      if (isNested()) {
        return createImpossibleMatchResult(matchedType);
      }
      // javac accepted the record pattern, so a false bound is a bug in the Checker Framework.
      throw new BugInCF(
          "Record pattern inference produced the false bound for %s",
          TreeUtils.toStringTruncated(pattern, 65));
    }
    if (boundSet.annoInferenceFailed) {
      return createFailedResult(matchedType, new Failure(boundSet.errorMsg, false));
    }
    // Incorporation of two proper bounds v = S1 and v = S2 implies <S1 = S2>, whose reduction
    // ignores qualifiers, so check that each proper equal bound of v has the same qualifiers as
    // v's instantiation.
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

    // Step 5: resolve alpha1, ..., alphan, skipping directly to the resolution step that introduces
    // fresh type variables.
    Resolution.resolveWithFreshTypeVariablesOnly(new ArrayList<>(alphas), boundSet, context);

    // Step 6: let A1, ..., An be the instantiations of alpha1, ..., alphan.  The type of the record
    // pattern is the upward projection of R<A1, ..., An> (`patternType`) with respect to the fresh
    // type variables.  The component types are computed from R<A1, ..., An> instead, and the type
    // of
    // a `var` binding variable is the upward projection of its component type.  Projecting
    // R<A1, ..., An> first would lose bounds: `AnnotatedTypeProjection` gives `?` for
    // `? extends A & B`, and the capture of `?` is bounded only by the type parameter's bound.
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
    patternType.setTypeArguments(typeArgs);
    return createResult(matchedType, null, patternType);
  }

  /**
   * Adds to {@code capturedTypes} the capture of {@code type} if {@code type} is a parameterized
   * type. If {@code type} is a type variable, recurs on its upper bound; if {@code type} is an
   * intersection type, recurs on each of its bounds (JLS 18.5.5, step 3).
   *
   * @param type the type of values that are matched against the record pattern, or, recursively, an
   *     upper bound of it
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
