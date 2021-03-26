package org.checkerframework.framework.util.typeinference.solver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Equalities;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Subtypes;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Supertypes;

/**
 * ConstraintMap holds simplified versions of the TUConstraints for ALL type variable for which we
 * are inferring an argument. The ConstraintMap is edited on the fly as the various solvers work
 * (unlike the AF/TU Constraints which are immutable).
 *
 * <p>This really consists of these things:
 *
 * <ol>
 *   <li>a Map({@code target => constraints for target})
 *   <li>Methods to easily build up the constraints in the map
 *   <li>A getter for the constraints of individual targets.
 * </ol>
 *
 * Note: This class, along with TargetConstraints, uses a lot of mutable state and few
 * setters/getters be careful. This choice was made as it makes the resulting code more readable.
 */
public class ConstraintMap {

  private final Map<TypeVariable, TargetConstraints> targetToRecords = new LinkedHashMap<>();

  public ConstraintMap(Set<TypeVariable> targets) {
    for (final TypeVariable target : targets) {
      targetToRecords.put(target, new TargetConstraints(target));
    }
  }

  public ConstraintMap(final ConstraintMap toCopy) {
    this.targetToRecords.putAll(toCopy.targetToRecords);
  }

  /** Gets the equality, subtypes, and supertypes constraints for a particular target. */
  public TargetConstraints getConstraints(final TypeVariable target) {
    return targetToRecords.get(target);
  }

  /**
   * Returns the set of all targets passed to the constructor of this constraint map (a target will
   * appear in this list whether or not it has any constraints added).
   *
   * @return the set of all targets passed to the constructor of this constraint map (a target will
   *     appear in this list whether or not it has any constraints added)
   */
  public Set<TypeVariable> getTargets() {
    return targetToRecords.keySet();
  }

  /**
   * Add a constraint indicating that the equivalent is equal to target in the given qualifier
   * hierarchies.
   */
  public void addTargetEquality(
      final TypeVariable target, final TypeVariable equivalent, AnnotationMirrorSet hierarchies) {
    final Equalities equalities = targetToRecords.get(target).equalities;
    final AnnotationMirrorSet equivalentTops = equalities.targets.get(equivalent);
    if (equivalentTops == null) {
      equalities.targets.put(equivalent, new AnnotationMirrorSet(hierarchies));
    } else {
      equivalentTops.addAll(hierarchies);
    }
  }

  /**
   * Add a constraint indicating that target has primary annotations equal to the given annotations.
   */
  public void addPrimaryEqualities(
      final TypeVariable target,
      QualifierHierarchy qualHierarchy,
      final AnnotationMirrorSet annos) {
    final Equalities equalities = targetToRecords.get(target).equalities;

    for (final AnnotationMirror anno : annos) {
      final AnnotationMirror top = qualHierarchy.getTopAnnotation(anno);
      if (!equalities.primaries.containsKey(top)) {
        equalities.primaries.put(top, anno);
      }
    }
  }

  /**
   * Add a constraint indicating that target is a supertype of subtype in the given qualifier
   * hierarchies.
   *
   * @param hierarchies a set of TOP annotations
   */
  public void addTargetSupertype(
      final TypeVariable target, final TypeVariable subtype, AnnotationMirrorSet hierarchies) {
    final Supertypes supertypes = targetToRecords.get(target).supertypes;
    final AnnotationMirrorSet supertypeTops = supertypes.targets.get(subtype);
    if (supertypeTops == null) {
      supertypes.targets.put(subtype, new AnnotationMirrorSet(hierarchies));
    } else {
      supertypeTops.addAll(hierarchies);
    }
  }

  /**
   * Add a constraint indicating that target is a supertype of subtype in the given qualifier
   * hierarchies.
   *
   * @param hierarchies a set of TOP annotations
   */
  public void addTypeSupertype(
      final TypeVariable target,
      final AnnotatedTypeMirror subtype,
      AnnotationMirrorSet hierarchies) {
    final Supertypes supertypes = targetToRecords.get(target).supertypes;
    final AnnotationMirrorSet supertypeTops = supertypes.types.get(subtype);
    if (supertypeTops == null) {
      supertypes.types.put(subtype, new AnnotationMirrorSet(hierarchies));
    } else {
      supertypeTops.addAll(hierarchies);
    }
  }

  /**
   * Add a constraint indicating that target's primary annotations are subtypes of the given
   * annotations.
   */
  public void addPrimarySupertype(
      final TypeVariable target,
      QualifierHierarchy qualifierHierarchy,
      final AnnotationMirrorSet annos) {
    final Supertypes supertypes = targetToRecords.get(target).supertypes;
    for (final AnnotationMirror anno : annos) {
      final AnnotationMirror top = qualifierHierarchy.getTopAnnotation(anno);
      AnnotationMirrorSet entries = supertypes.primaries.get(top);
      if (entries == null) {
        entries = new AnnotationMirrorSet();
        supertypes.primaries.put(top, entries);
      }
      entries.add(anno);
    }
  }

  /**
   * Add a constraint indicating that target is a subtype of supertype in the given qualifier
   * hierarchies.
   *
   * @param hierarchies a set of TOP annotations
   */
  public void addTargetSubtype(
      final TypeVariable target, final TypeVariable supertype, AnnotationMirrorSet hierarchies) {
    final Subtypes subtypes = targetToRecords.get(target).subtypes;
    final AnnotationMirrorSet subtypesTops = subtypes.targets.get(supertype);
    if (subtypesTops == null) {
      subtypes.targets.put(supertype, new AnnotationMirrorSet(hierarchies));
    } else {
      subtypesTops.addAll(hierarchies);
    }
  }

  /**
   * Add a constraint indicating that target is a subtype of supertype in the given qualifier
   * hierarchies.
   *
   * @param hierarchies a set of TOP annotations
   */
  public void addTypeSubtype(
      final TypeVariable target,
      final AnnotatedTypeMirror supertype,
      AnnotationMirrorSet hierarchies) {
    final Subtypes subtypes = targetToRecords.get(target).subtypes;
    final AnnotationMirrorSet subtypesTops = subtypes.types.get(supertype);
    if (subtypesTops == null) {
      subtypes.types.put(supertype, new AnnotationMirrorSet(hierarchies));
    } else {
      subtypesTops.addAll(hierarchies);
    }
  }

  /**
   * Add a constraint indicating that target's primary annotations are subtypes of the given
   * annotations.
   */
  public void addPrimarySubtypes(
      final TypeVariable target,
      QualifierHierarchy qualifierHierarchy,
      final AnnotationMirrorSet annos) {
    final Subtypes subtypes = targetToRecords.get(target).subtypes;
    for (final AnnotationMirror anno : annos) {
      final AnnotationMirror top = qualifierHierarchy.getTopAnnotation(anno);
      AnnotationMirrorSet entries = subtypes.primaries.get(top);
      if (entries == null) {
        entries = new AnnotationMirrorSet();
        subtypes.primaries.put(top, entries);
      }
      entries.add(anno);
    }
  }

  /**
   * Add a constraint indicating that target is equal to type in the given hierarchies.
   *
   * @param hierarchies a set of TOP annotations
   */
  public void addTypeEqualities(
      TypeVariable target, AnnotatedTypeMirror type, AnnotationMirrorSet hierarchies) {
    final Equalities equalities = targetToRecords.get(target).equalities;
    final AnnotationMirrorSet equalityTops = equalities.types.get(type);
    if (equalityTops == null) {
      equalities.types.put(type, new AnnotationMirrorSet(hierarchies));
    } else {
      equalityTops.addAll(hierarchies);
    }
  }
}
