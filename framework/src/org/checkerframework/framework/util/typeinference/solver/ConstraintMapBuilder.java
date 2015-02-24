package org.checkerframework.framework.util.typeinference.solver;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.typeinference.constraint.TIsU;
import org.checkerframework.framework.util.typeinference.constraint.TSuperU;
import org.checkerframework.framework.util.typeinference.constraint.TUConstraint;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Converts a set of TUConstraints into a ConstraintMap.
 */
public class ConstraintMapBuilder {


    public ConstraintMap build(Set<TypeVariable> targets,
                               Set<TUConstraint> constraints,
                               AnnotatedTypeFactory typeFactory) {

        final QualifierHierarchy qualifierHierarchy = typeFactory.getQualifierHierarchy();
        final Set<? extends AnnotationMirror> tops = qualifierHierarchy.getTopAnnotations();
        final ConstraintMap result = new ConstraintMap(targets);

        final Set<AnnotationMirror> tAnnos = new LinkedHashSet<>();
        final Set<AnnotationMirror> uAnnos = new LinkedHashSet<>();
        final Set<AnnotationMirror> hierarchiesInRelation = new LinkedHashSet<>();

        for (TUConstraint constraint : constraints) {
            tAnnos.clear();
            uAnnos.clear();
            hierarchiesInRelation.clear();

            final AnnotatedTypeVariable typeT = constraint.typeVariable;
            final AnnotatedTypeMirror typeU = constraint.relatedType;

            if (typeU.getKind() == TypeKind.TYPEVAR && targets.contains(typeU.getUnderlyingType())) {
                if (typeT.getAnnotations().isEmpty() && typeU.getAnnotations().isEmpty()) {
                    hierarchiesInRelation.addAll(tops);

                } else {

                    for (AnnotationMirror top : tops) {
                        final AnnotationMirror tAnno = typeT.getAnnotationInHierarchy(top);
                        final AnnotationMirror uAnno = typeU.getAnnotationInHierarchy(top);

                        if (tAnno == null ) {
                            if (uAnno == null) {
                                hierarchiesInRelation.add(top);

                            } else {
                                tAnnos.add(uAnno);

                            }
                        } else {
                            if (uAnno == null) {
                                uAnnos.add(tAnno);

                            } else {
                                //Tell's us nothing?  The two annotation should be equal?  Test that they are
                                //equal?
                                if (AnnotationUtils.areSame(uAnno, tAnno)) {
                                    ErrorReporter.errorAbort(
                                            "Annotations should be equivalent!\n"
                                                    + "tAnno = " + uAnno + "\n"
                                                    + "uAnno = " + tAnno + "\n"
                                                    + "constraint = " + constraint + "\n"
                                    );
                                }
                            }
                        }
                    }

                    if (!tAnnos.isEmpty()) {
                        addToPrimaryRelationship(typeT.getUnderlyingType(),
                                                 constraint, result, tAnnos, qualifierHierarchy);
                    }

                    if (!uAnnos.isEmpty()) {
                        addToPrimaryRelationship((TypeVariable) typeU.getUnderlyingType(),
                                                 constraint, result, uAnnos, qualifierHierarchy);
                    }
                }

                if (!typeT.getUnderlyingType().equals(typeU.getUnderlyingType())) {
                    addToTargetRelationship(typeT.getUnderlyingType(), (TypeVariable) typeU.getUnderlyingType(),
                                            result, constraint, hierarchiesInRelation);
                }
            } else {
                for (AnnotationMirror top : tops) {
                    final AnnotationMirror tAnno = typeT.getAnnotationInHierarchy(top);

                    if (tAnno == null) {
                        hierarchiesInRelation.add(top);
                    }
                }

                addToTypeRelationship(typeT.getUnderlyingType(), typeU, result, constraint, hierarchiesInRelation);
            }

        }

        return result;
    }

    private void addToTargetRelationship(TypeVariable typeT, TypeVariable typeU, ConstraintMap result,
                                         TUConstraint constraint, Set<AnnotationMirror> hierarchiesInRelation) {
        if (constraint instanceof TIsU) {
            result.addTargetEquality(typeT, typeU, hierarchiesInRelation);
        } else if (constraint instanceof TSuperU) {
            result.addTargetSupertype(typeT, typeU, hierarchiesInRelation);
        } else {
            result.addTargetSubtype(typeT, typeU, hierarchiesInRelation);
        }
    }

    public void addToPrimaryRelationship(TypeVariable typeVariable, TUConstraint constraint, ConstraintMap result,
                                         Set<AnnotationMirror> annotationMirrors, QualifierHierarchy qualifierHierarchy) {
        if (constraint instanceof TIsU) {
            result.addPrimaryEqualities(typeVariable, qualifierHierarchy, annotationMirrors);
        } else if (constraint instanceof TSuperU) {
            result.addPrimarySupertype(typeVariable, qualifierHierarchy, annotationMirrors);
        } else {
            result.addPrimarySubtypes(typeVariable, qualifierHierarchy, annotationMirrors);
        }
    }

    public void addToTypeRelationship(TypeVariable target, AnnotatedTypeMirror type,ConstraintMap result,
                                      TUConstraint constraint, Set<AnnotationMirror> hierarchies) {
        if (constraint instanceof TIsU) {
            result.addTypeEqualities(target, type, hierarchies);
        } else if (constraint instanceof TSuperU) {
            result.addTypeSupertype(target, type, hierarchies);
        } else {
            result.addTypeSubtype(target, type, hierarchies);
        }
    }
}
