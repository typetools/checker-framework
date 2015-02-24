package org.checkerframework.framework.util.typeinference.solver;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.typeinference.GlbUtil;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredType;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Subtypes;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeVariable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Created by jburke on 2/18/15.
 */
public class SubtypesSolver {
    public SubtypesSolver() {

    }

    public InferenceResult solveFromAssignment(final Set<TypeVariable> remainingTargets, final ConstraintMap constraints,
                                               final AnnotatedTypeFactory typeFactory) {
        return updateHackGlub(remainingTargets, constraints, typeFactory);
    }

    public InferenceResult updateHackGlub(final Set<TypeVariable> remainingTargets, final ConstraintMap constraints,
                                          final AnnotatedTypeFactory typeFactory) {
        final InferenceResult inferenceResult = new InferenceResult();
        final QualifierHierarchy qualifierHierarchy = typeFactory.getQualifierHierarchy();
        for(final TypeVariable target : remainingTargets) {
            Subtypes subtypes = constraints.getConstraints(target).subtypes;

            if (subtypes.types.isEmpty()) {
                continue;
            }

            Map<AnnotationMirror, Set<AnnotationMirror>> primaries = subtypes.primaries;
            //TODO: ADD GLB PROPAGATION
            if (subtypes.types.size() == 1) {
                final Entry<AnnotatedTypeMirror, Set<AnnotationMirror>> entry = subtypes.types.entrySet().iterator().next();
                AnnotatedTypeMirror supertype = entry.getKey().deepCopy();

                for (AnnotationMirror top : entry.getValue()) {
                    final Set<AnnotationMirror> superAnnos = primaries.get(top);
                    if (superAnnos != null) { //if it is null we're just going to use the anno already on supertype
                        final AnnotationMirror supertypeAnno = supertype.getAnnotationInHierarchy(top);
                        superAnnos.add(supertypeAnno);
                    }
                }

                if (!primaries.isEmpty()) {
                    for (AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
                        final AnnotationMirror glb = greatestLowerBound(subtypes.primaries.get(top), qualifierHierarchy);
                        supertype.replaceAnnotation(glb);
                    }
                }

                inferenceResult.put(target, new InferredType(supertype));

            }  else {

                final AnnotatedTypeMirror glbType = GlbUtil.glbAll(subtypes.types, typeFactory);
                if (glbType != null) {
                    if (!primaries.isEmpty()) {
                        for (AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
                            final AnnotationMirror glb = greatestLowerBound(subtypes.primaries.get(top), qualifierHierarchy);
                            final AnnotationMirror currentAnno = glbType.getAnnotationInHierarchy(top);

                            if (currentAnno == null) {
                                glbType.addAnnotation(glb);
                            } else if (glb != null) {
                                glbType.replaceAnnotation(qualifierHierarchy.greatestLowerBound(glb, currentAnno));
                            }
                        }
                    }

                    inferenceResult.put(target, new InferredType(glbType));
                }
            }
        }

        return inferenceResult;
    }


    private final AnnotationMirror greatestLowerBound(final Iterable<? extends AnnotationMirror> annos,
                                                      QualifierHierarchy qualifierHierarchy) {
        Iterator<? extends AnnotationMirror> annoIter = annos.iterator();
        AnnotationMirror lub = annoIter.next();

        while(annoIter.hasNext()) {
            lub = qualifierHierarchy.greatestLowerBound(lub, annoIter.next());
        }

        return lub;
    }
}
