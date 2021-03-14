package org.checkerframework.framework.util.typeinference.solver;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.framework.util.typeinference.constraint.TIsU;
import org.checkerframework.framework.util.typeinference.constraint.TSuperU;
import org.checkerframework.framework.util.typeinference.constraint.TUConstraint;
import org.checkerframework.javacutil.TypeAnnotationUtils;

/** Converts a set of TUConstraints into a ConstraintMap. */
public class ConstraintMapBuilder {

  /**
   * Let Ti be a the ith target being inferred Let ATV(i) be the annotated type variable that
   * represents as use of Ti which may or may not have primary annotations. Let ATM be an annotated
   * type mirror that may or may not be target Tx, or have a component target Tx Let Ai be the type
   * argument we are trying to infer for Ti
   *
   * <p>We have a set of constraints of the form: {@code ATV(i) <?> ATM}
   *
   * <p>Where {@code <?>} is either a subtype ({@code <:}), supertype ({@code :>}), or equality
   * relationship ({@code =}).
   *
   * <p>Regardless of what {@code <?>} is, a constraint will only imply constraints on Ai in a given
   * hierarchy if ATV(i) does NOT have a primary annotation in that hierarchy. That is:
   *
   * <p>E.g. Let ATV(i) be @NonNull Ti, the constraints @NonNull Ti = @NonNull @Initialized String
   * does not imply any primary annotation in the Nullness hierarchy for type argument Ai because
   * the Annotated type mirror has a primary annotation in the NUllness hierarchy.
   *
   * <p>However, it does imply that Ai has a primary annotation of @Initialized since ATV(i) has no
   * primary annotation in the initialization hierarchy.
   *
   * <p>Note, constraints come in 2 forms:
   *
   * <ul>
   *   <li>between a target and a concrete AnnotatedTypeMirror. E.g., As seen above {@code (@NonNull
   *       Ti = @NonNull @Initialized String)}
   *   <li>between two targets E.g., {@code (@NonNull Ti = Tj)}
   * </ul>
   */
  public ConstraintMap build(
      Set<TypeVariable> targets, Set<TUConstraint> constraints, AnnotatedTypeFactory typeFactory) {

    final QualifierHierarchy qualifierHierarchy = typeFactory.getQualifierHierarchy();
    final AnnotationMirrorSet tops =
        new AnnotationMirrorSet(qualifierHierarchy.getTopAnnotations());
    final ConstraintMap result = new ConstraintMap(targets);

    final AnnotationMirrorSet tAnnos = new AnnotationMirrorSet();
    final AnnotationMirrorSet uAnnos = new AnnotationMirrorSet();
    final AnnotationMirrorSet hierarchiesInRelation = new AnnotationMirrorSet();

    for (TUConstraint constraint : constraints) {
      tAnnos.clear();
      uAnnos.clear();
      hierarchiesInRelation.clear();

      final AnnotatedTypeVariable typeT = constraint.typeVariable;
      final AnnotatedTypeMirror typeU = constraint.relatedType;

      // If typeU is from an argument to the method, then treat typeU as an ordinary type even
      // if it is a target type variable.  This is for the case where the inferred type is the
      // declared type parameter.  For example,
      // public <T> T get(T t) {
      //   return this.get(t);
      // }
      // The inferred type of T should be T.
      if (!constraint.uIsArg
          && typeU.getKind() == TypeKind.TYPEVAR
          && targets.contains(
              (TypeVariable) TypeAnnotationUtils.unannotatedType(typeU.getUnderlyingType()))) {
        if (typeT.getAnnotations().isEmpty() && typeU.getAnnotations().isEmpty()) {
          hierarchiesInRelation.addAll(tops);

        } else {

          for (AnnotationMirror top : tops) {
            final AnnotationMirror tAnno = typeT.getAnnotationInHierarchy(top);
            final AnnotationMirror uAnno = typeU.getAnnotationInHierarchy(top);

            if (tAnno == null) {
              if (uAnno == null) {
                hierarchiesInRelation.add(top);

              } else {
                tAnnos.add(uAnno);
              }
            } else {
              if (uAnno == null) {
                uAnnos.add(tAnno);

              } else {
                // This tells us nothing, they both should be equal but either way
                // we gain no information if both type vars have annotations
              }
            }
          }

          // If we have a case where Ti = @NonNull Tj we know that for the @Initialization hierarchy
          // Ti = TJ and we know that for the @Nullable hierarchy Ti = @NonNull <some other type>.
          // This step saves @NonNull annotation.
          // This case also covers the case where i = j.
          if (!tAnnos.isEmpty()) {
            addToPrimaryRelationship(
                (TypeVariable) TypeAnnotationUtils.unannotatedType(typeT.getUnderlyingType()),
                constraint,
                result,
                tAnnos,
                qualifierHierarchy);
          }

          if (!uAnnos.isEmpty()) {
            addToPrimaryRelationship(
                (TypeVariable) TypeAnnotationUtils.unannotatedType(typeU.getUnderlyingType()),
                constraint,
                result,
                uAnnos,
                qualifierHierarchy);
          }
        }

        // This is the case where we have a relationship between two different targets (Ti
        // <?> Tj and i != j)
        if (!typeFactory.types.isSameType(
            TypeAnnotationUtils.unannotatedType(typeT.getUnderlyingType()),
            TypeAnnotationUtils.unannotatedType(typeU.getUnderlyingType()))) {
          addToTargetRelationship(
              (TypeVariable) TypeAnnotationUtils.unannotatedType(typeT.getUnderlyingType()),
              (TypeVariable) TypeAnnotationUtils.unannotatedType(typeU.getUnderlyingType()),
              result,
              constraint,
              hierarchiesInRelation);
        }
      } else {
        for (AnnotationMirror top : tops) {
          final AnnotationMirror tAnno = typeT.getAnnotationInHierarchy(top);

          if (tAnno == null) {
            hierarchiesInRelation.add(top);
          }
        }

        addToTypeRelationship(
            (TypeVariable) TypeAnnotationUtils.unannotatedType(typeT.getUnderlyingType()),
            typeU,
            result,
            constraint,
            hierarchiesInRelation);
      }
    }

    return result;
  }

  private void addToTargetRelationship(
      TypeVariable typeT,
      TypeVariable typeU,
      ConstraintMap result,
      TUConstraint constraint,
      AnnotationMirrorSet hierarchiesInRelation) {
    if (constraint instanceof TIsU) {
      result.addTargetEquality(typeT, typeU, hierarchiesInRelation);
    } else if (constraint instanceof TSuperU) {
      result.addTargetSupertype(typeT, typeU, hierarchiesInRelation);
    } else {
      result.addTargetSubtype(typeT, typeU, hierarchiesInRelation);
    }
  }

  public void addToPrimaryRelationship(
      TypeVariable typeVariable,
      TUConstraint constraint,
      ConstraintMap result,
      AnnotationMirrorSet annotationMirrors,
      QualifierHierarchy qualifierHierarchy) {
    if (constraint instanceof TIsU) {
      result.addPrimaryEqualities(typeVariable, qualifierHierarchy, annotationMirrors);
    } else if (constraint instanceof TSuperU) {
      result.addPrimarySupertype(typeVariable, qualifierHierarchy, annotationMirrors);
    } else {
      result.addPrimarySubtypes(typeVariable, qualifierHierarchy, annotationMirrors);
    }
  }

  public void addToTypeRelationship(
      TypeVariable target,
      AnnotatedTypeMirror type,
      ConstraintMap result,
      TUConstraint constraint,
      AnnotationMirrorSet hierarchies) {
    if (constraint instanceof TIsU) {
      result.addTypeEqualities(target, type, hierarchies);
    } else if (constraint instanceof TSuperU) {
      result.addTypeSupertype(target, type, hierarchies);
    } else {
      result.addTypeSubtype(target, type, hierarchies);
    }
  }
}
