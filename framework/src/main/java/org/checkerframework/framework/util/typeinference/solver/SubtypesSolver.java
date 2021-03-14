package org.checkerframework.framework.util.typeinference.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.framework.util.typeinference.GlbUtil;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredType;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Subtypes;

/**
 * Infers type arguments by using the Greatest Lower Bound computation on the subtype relationships
 * in a constraint map.
 */
public class SubtypesSolver {

  /**
   * Infers type arguments using subtype constraints.
   *
   * @param remainingTargets targets for which we still need to infer a value
   * @param constraints the set of constraints for all targets
   * @return a mapping from target to inferred type. Note this class always infers concrete types
   *     and will not infer that the target is equivalent to another target.
   */
  public InferenceResult solveFromSubtypes(
      final Set<TypeVariable> remainingTargets,
      final ConstraintMap constraints,
      final AnnotatedTypeFactory typeFactory) {
    return glbSubtypes(remainingTargets, constraints, typeFactory);
  }

  public InferenceResult glbSubtypes(
      final Set<TypeVariable> remainingTargets,
      final ConstraintMap constraints,
      final AnnotatedTypeFactory typeFactory) {
    final InferenceResult inferenceResult = new InferenceResult();
    final QualifierHierarchy qualifierHierarchy = typeFactory.getQualifierHierarchy();

    final Types types = typeFactory.getProcessingEnv().getTypeUtils();

    List<TypeVariable> targetsSubtypesLast = new ArrayList<>(remainingTargets);

    // If we have two type variables <A, A extends B> order them A then B
    // this is required because we will use the fact that B must be below A
    // when determining the glb of B
    Collections.sort(
        targetsSubtypesLast,
        new Comparator<TypeVariable>() {
          @Override
          public int compare(TypeVariable o1, TypeVariable o2) {
            if (types.isSubtype(o1, o2)) {
              return 1;
            } else if (types.isSubtype(o2, o1)) {
              return -1;
            }

            return 0;
          }
        });

    for (final TypeVariable target : targetsSubtypesLast) {
      Subtypes subtypes = constraints.getConstraints(target).subtypes;

      if (subtypes.types.isEmpty()) {
        continue;
      }

      propagatePreviousGlbs(subtypes, inferenceResult, subtypes.types);

      // if the subtypes size is only 1 then we need not do any GLBing on the underlying types
      // but we may have primary annotations that need to be GLBed
      AnnotationMirrorMap<AnnotationMirrorSet> primaries = subtypes.primaries;
      if (subtypes.types.size() == 1) {
        final Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet> entry =
            subtypes.types.entrySet().iterator().next();
        AnnotatedTypeMirror supertype = entry.getKey().deepCopy();

        for (AnnotationMirror top : entry.getValue()) {
          final AnnotationMirrorSet superAnnos = primaries.get(top);
          // if it is null we're just going to use the anno already on supertype
          if (superAnnos != null) {
            final AnnotationMirror supertypeAnno = supertype.getAnnotationInHierarchy(top);
            superAnnos.add(supertypeAnno);
          }
        }

        if (!primaries.isEmpty()) {
          for (AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
            final AnnotationMirror glb =
                greatestLowerBound(subtypes.primaries.get(top), qualifierHierarchy);
            supertype.replaceAnnotation(glb);
          }
        }

        inferenceResult.put(target, new InferredType(supertype));

      } else {

        // GLB all of the types than combine this with the GLB of primary annotation constraints
        final AnnotatedTypeMirror glbType = GlbUtil.glbAll(subtypes.types, typeFactory);
        if (glbType != null) {
          if (!primaries.isEmpty()) {
            for (AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
              final AnnotationMirror glb =
                  greatestLowerBound(subtypes.primaries.get(top), qualifierHierarchy);
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

  /**
   * /** If the target corresponding to targetRecord must be a subtype of another target for which
   * we have already determined a GLB, add that target's GLB to the list of subtypes to be GLBed for
   * this target.
   */
  protected static void propagatePreviousGlbs(
      final Subtypes targetSubtypes,
      InferenceResult solution,
      final Map<AnnotatedTypeMirror, AnnotationMirrorSet> subtypesOfTarget) {

    for (final Map.Entry<TypeVariable, AnnotationMirrorSet> subtypeTarget :
        targetSubtypes.targets.entrySet()) {
      final InferredValue subtargetInferredGlb = solution.get(subtypeTarget.getKey());

      if (subtargetInferredGlb != null) {
        final AnnotatedTypeMirror subtargetGlbType = ((InferredType) subtargetInferredGlb).type;
        AnnotationMirrorSet subtargetAnnos = subtypesOfTarget.get(subtargetGlbType);
        if (subtargetAnnos != null) {
          // there is already an equivalent type in the list of subtypes, just add
          // any hierarchies that are not in its list but are in the supertarget's list
          subtargetAnnos.addAll(subtypeTarget.getValue());
        } else {
          subtypesOfTarget.put(subtargetGlbType, subtypeTarget.getValue());
        }
      }
    }
  }

  /**
   * Returns the GLB of annos.
   *
   * @param annos a set of annotations in the same annotation hierarchy
   * @param qualifierHierarchy the qualifier of the annotation hierarchy
   * @return the GLB of annos
   */
  private final AnnotationMirror greatestLowerBound(
      final Iterable<? extends AnnotationMirror> annos, QualifierHierarchy qualifierHierarchy) {
    Iterator<? extends AnnotationMirror> annoIter = annos.iterator();
    AnnotationMirror glb = annoIter.next();

    while (annoIter.hasNext()) {
      glb = qualifierHierarchy.greatestLowerBound(glb, annoIter.next());
    }

    return glb;
  }
}
