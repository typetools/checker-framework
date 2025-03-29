package org.checkerframework.framework.util.typeinference8.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.types.AbstractQualifier;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.Dependencies;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds.BoundKind;

/**
 * Resolution finds an instantiation for each variable in a given set of variables. It does this
 * using all the bounds on a variable. Because a bound on a variable by be another unresolved
 * variable, the order in which the variables must be computed before resolution. If the set of
 * variables contains any captured variables, then a different resolution algorthim is used. If a
 * set of variables does not contain a captured variable, but the resolution fails, then the
 * resolution algorithm for captured variables is used.
 *
 * <p>Resolution is discussed in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.4">JLS Section
 * 18.4</a>.
 *
 * <p>Entry point is two static methods, {@link #resolveSmallestSet(Set, BoundSet)} and {@link
 * #resolve(Variable, BoundSet, Java8InferenceContext)}, which create {@link Resolution} objects
 * that actually preform the resolution.
 */
public class Resolution {

  /**
   * Instantiates a set of variables, {@code as}.
   *
   * @param as the set of variables to resolve
   * @param boundSet the bound set that includes {@code as}
   * @param context Java8InferenceContext
   * @return bound set where {@code as} have instantiations
   */
  public static BoundSet resolve(
      Collection<Variable> as, BoundSet boundSet, Java8InferenceContext context) {

    // Remove any variables that already have instantiations
    List<Variable> resolvedVars = boundSet.getInstantiatedVariables();
    as.removeAll(resolvedVars);
    if (as.isEmpty()) {
      return boundSet;
    }
    // Calculate the dependencies between variables. (A variable depends on another if it is
    // included in one of its bounds.)
    Dependencies dependencies = boundSet.getDependencies();
    Queue<Variable> unresolvedVars = new ArrayDeque<>(as);
    for (Variable var : as) {
      for (Variable dep : dependencies.get(var)) {
        if (!unresolvedVars.contains(dep)) {
          unresolvedVars.add(dep);
        }
      }
    }

    // Remove any variables that already have instantiations
    unresolvedVars.removeAll(resolvedVars);
    if (unresolvedVars.isEmpty()) {
      return boundSet;
    }

    // Resolve the variables
    Resolution resolution = new Resolution(context, dependencies);
    boundSet = resolution.resolve(boundSet, unresolvedVars);
    assert !boundSet.containsFalse();
    return boundSet;
  }

  /**
   * Instantiates the variable {@code a}.
   *
   * @param a the variable to resolve
   * @param boundSet the bound set that includes {@code a}
   * @param context Java8InferenceContext
   * @return bound set where {@code a} is instantiated
   */
  public static BoundSet resolve(Variable a, BoundSet boundSet, Java8InferenceContext context) {
    if (a.getBounds().hasInstantiation()) {
      return boundSet;
    }
    Dependencies dependencies = boundSet.getDependencies();

    LinkedHashSet<Variable> unresolvedVars = new LinkedHashSet<>();
    unresolvedVars.add(a);
    Resolution resolution = new Resolution(context, dependencies);
    boundSet = resolution.resolveSmallestSet(unresolvedVars, boundSet);
    assert !boundSet.containsFalse();
    return boundSet;
  }

  /** The context. */
  private final Java8InferenceContext context;

  /** The set of dependencies between the variables. */
  private final Dependencies dependencies;

  /**
   * Creates a resolution.
   *
   * @param context the context
   * @param dependencies the dependencies
   */
  private Resolution(Java8InferenceContext context, Dependencies dependencies) {
    this.context = context;
    this.dependencies = dependencies;
  }

  /**
   * Resolve all the variables in {@code unresolvedVars}.
   *
   * @param boundSet current bound set
   * @param unresolvedVars a set of unresolved variables that includes all dependencies
   * @return the bounds set with the resolved bounds
   */
  private BoundSet resolve(BoundSet boundSet, Queue<Variable> unresolvedVars) {
    List<Variable> resolvedVars = boundSet.getInstantiatedVariables();

    while (!unresolvedVars.isEmpty()) {
      assert !boundSet.containsFalse();

      Set<Variable> smallestDependencySet = getSmallestDependecySet(resolvedVars, unresolvedVars);

      // Resolve the smallest unresolved dependency set.
      boundSet = resolveSmallestSet(smallestDependencySet, boundSet);

      resolvedVars = boundSet.getInstantiatedVariables();
      unresolvedVars.removeAll(resolvedVars);
    }
    return boundSet;
  }

  /**
   * Returns the smallest set of unresolved variables that includes any variable on which a variable
   * in the set depends.
   *
   * @param resolvedVars variables that have been resolved
   * @param unresolvedVars variables that have not been resolved
   * @return the smallest set of unresolved variable
   */
  private Set<Variable> getSmallestDependecySet(
      List<Variable> resolvedVars, Queue<Variable> unresolvedVars) {
    Set<Variable> smallestDependencySet = null;
    // This loop is looking for the smallest set of dependencies that have not been resolved.
    for (Variable alpha : unresolvedVars) {
      Set<Variable> alphasDependencySet = dependencies.get(alpha);
      alphasDependencySet.removeAll(resolvedVars);

      if (smallestDependencySet == null
          || alphasDependencySet.size() < smallestDependencySet.size()) {
        smallestDependencySet = alphasDependencySet;
      }

      if (smallestDependencySet.size() == 1) {
        // If the size is 1, then alpha has the smallest possible set of unresolved
        // dependencies.
        // (A variable is always dependent on itself.) So, stop looking for smaller ones.
        break;
      }
    }
    return smallestDependencySet;
  }

  /**
   * Resolves {@code as}
   *
   * @param as the smallest set of unresolved variables that includes all any variable on which a
   *     variable in the set depends
   * @param boundSet current bounds set
   * @return current bound set
   */
  private BoundSet resolveSmallestSet(Set<Variable> as, BoundSet boundSet) {
    assert !boundSet.containsFalse();

    if (boundSet.containsCapture(as)) {
      resolveNonCapturesFirst(new ArrayList<>(as));
      boundSet.getInstantiatedVariables().forEach(as::remove);
      // Then resolve the capture variables
      return resolveWithCapture(as, boundSet, context);
    } else {
      BoundSet copy = new BoundSet(boundSet);
      // Save the current bounds in case the first attempt at resolution fails.
      copy.saveBounds();
      try {
        BoundSet resolvedBounds = resolveNoCapture(as, boundSet);
        if (!resolvedBounds.containsFalse()) {
          return resolvedBounds;
        }
      } catch (FalseBoundException ex) {
        // Try with capture.
      }
      boundSet = copy;
      // If resolveNoCapture fails, then undo all resolved variables from the failed attempt.
      boundSet.restore();
      return resolveWithCapture(as, boundSet, context);
    }
  }

  /**
   * Resolves all the non-capture variables in {@code vars}.
   *
   * @param vars the variables
   */
  private void resolveNonCapturesFirst(List<Variable> vars) {
    // Variables that are not captures and need to be resolved. (Avoid side-effecting vars)
    List<Variable> varsToResolve = new ArrayList<>(vars);
    varsToResolve.removeIf(Variable::isCaptureVariable);
    applyInstantiationsToBounds(varsToResolve);
    varsToResolve.removeIf(v -> v.getBounds().hasInstantiation());

    // Until varsToResolve is empty:
    // Find the variable, alpha, in `varsToResolve` that has the fewest varsToResolve mentioned in
    // alpha's bounds.
    // Resolve alpha using the "noncapture" resolution method. (That is find an instantiation of
    // alpha using the "noncapture" resolution method.)
    // Remove alpha from `varsToResolve`.
    while (!varsToResolve.isEmpty()) {
      Variable alpha = null;
      // Smallest number of varsToResolve mentioned in alpha's bounds so far.
      int fewestVarsInBounds = Integer.MAX_VALUE;
      for (Variable v : varsToResolve) {
        int size = v.getBounds().getVariablesMentionedInBounds().size();
        if (size < fewestVarsInBounds) {
          fewestVarsInBounds = size;
          alpha = v;
        }
      }
      if (alpha != null) {
        resolveNoCapture(alpha);
        varsToResolve.remove(alpha);
      }
      applyInstantiationsToBounds(varsToResolve);
      varsToResolve.removeIf(v -> v.getBounds().hasInstantiation());
    }
  }

  /**
   * Apply the instantiated variables to the bounds of the variables in {@code variables}. This may
   * result in an instantiation being found of a variable in {@code variables}.
   *
   * @param variables a list of variables
   */
  private static void applyInstantiationsToBounds(List<Variable> variables) {
    boolean changed;
    do {
      changed = false;
      for (Variable v : variables) {
        if (v.getBounds().hasInstantiation()) {
          continue;
        }
        v.getBounds().applyInstantiationsToBounds();
        if (v.getBounds().hasInstantiation()) {
          // If v now has an instantiation, then loop through all the variables again to apply it to
          // all the bounds of the other variables.
          changed = true;
        }
      }
    } while (changed);
  }

  /**
   * Resolves all variables in {@code as} by instantiating each to the greatest lower bound of its
   * proper upper bounds. This may fail and resolveWithCapture will need to be used instead.
   *
   * @param as variables to resolve
   * @param boundSet the bound set to use
   * @return the resolved bound st
   */
  private BoundSet resolveNoCapture(Set<Variable> as, BoundSet boundSet) {
    BoundSet resolvedBoundSet = new BoundSet(context);
    for (Variable ai : as) {
      ai.getBounds().applyInstantiationsToBounds();
      if (ai.getBounds().hasInstantiation()) {
        continue;
      }
      resolveNoCapture(ai);
      if (!ai.getBounds().hasInstantiation()) {
        resolvedBoundSet.addFalse();
        break;
      }
    }
    boundSet.incorporateToFixedPoint(resolvedBoundSet);
    return boundSet;
  }

  /**
   * Resolves {@code ai} by instantiating it to the greatest lower bound of its proper upper bounds.
   *
   * @param ai variable to resolve
   */
  private void resolveNoCapture(Variable ai) {
    assert !ai.getBounds().hasInstantiation();
    Set<ProperType> lowerBounds = ai.getBounds().findProperLowerBounds();

    if (!lowerBounds.isEmpty()) {
      ProperType lubProperType = context.inferenceTypeFactory.lub(lowerBounds);
      Set<AbstractQualifier> qualifierLowerBounds =
          ai.getBounds().qualifierBounds.get(BoundKind.LOWER);
      if (!qualifierLowerBounds.isEmpty()) {
        QualifierHierarchy qh = context.typeFactory.getQualifierHierarchy();
        Set<AnnotationMirror> lubAnnos = AbstractQualifier.lub(qualifierLowerBounds, context);
        if (lubProperType.getAnnotatedType().getKind() != TypeKind.TYPEVAR) {
          Set<? extends AnnotationMirror> newLubAnnos =
              qh.leastUpperBoundsQualifiersOnly(
                  lubAnnos, lubProperType.getAnnotatedType().getPrimaryAnnotations());
          lubProperType.getAnnotatedType().replaceAnnotations(newLubAnnos);
        } else {

          AnnotatedTypeVariable lubTV = (AnnotatedTypeVariable) lubProperType.getAnnotatedType();
          Set<? extends AnnotationMirror> newLubAnnos =
              qh.leastUpperBoundsQualifiersOnly(
                  lubAnnos, lubTV.getLowerBound().getPrimaryAnnotations());
          lubTV.getLowerBound().replaceAnnotations(newLubAnnos);
        }
      }
      ai.getBounds().addBound(null, VariableBounds.BoundKind.EQUAL, lubProperType);
      return;
    }

    Set<ProperType> upperBounds = ai.getBounds().findProperUpperBounds();
    if (!upperBounds.isEmpty()) {
      ProperType ti = null;
      boolean useRuntimeEx = false;
      for (ProperType liProperType : upperBounds) {
        TypeMirror li = liProperType.getJavaType();
        if (ai.getBounds().hasThrowsBound()
            && context.env.getTypeUtils().isSubtype(context.runtimeEx, li)) {
          useRuntimeEx = true;
        }
        if (ti == null) {
          ti = liProperType;
        } else {
          ti = (ProperType) context.inferenceTypeFactory.glb(ti, liProperType);
        }
      }
      if (useRuntimeEx) {
        ai.getBounds()
            .addBound(
                null,
                VariableBounds.BoundKind.EQUAL,
                context.inferenceTypeFactory.getRuntimeException());
      } else {
        ai.getBounds().addBound(null, VariableBounds.BoundKind.EQUAL, ti);
      }
    }
  }

  /**
   * Instantiates the variables in {@code as} by creating fresh type variables using the bounds of
   * the variables.
   *
   * @param as a set of variables to resolve
   * @param boundSet the bounds set to use
   * @param context the contest
   * @return the resolved bound set
   */
  private static BoundSet resolveWithCapture(
      Set<Variable> as, BoundSet boundSet, Java8InferenceContext context) {
    assert !boundSet.containsFalse();
    boundSet.removeCaptures(as);
    BoundSet resolvedBoundSet = new BoundSet(context);
    List<Variable> asList = new ArrayList<>();
    List<TypeVariable> typeVar = new ArrayList<>();
    List<AbstractType> typeArg = new ArrayList<>();

    for (Variable ai : as) {
      ai.getBounds().applyInstantiationsToBounds();
      if (ai.getBounds().hasInstantiation()) {
        // If ai is equal to a variable that was resolved previously,
        // ai would now have an instantiation.
        continue;
      }
      asList.add(ai);
      Set<ProperType> lowerBounds = ai.getBounds().findProperLowerBounds();
      ProperType lowerBound = context.inferenceTypeFactory.lub(lowerBounds);

      Set<? extends AnnotationMirror> lowerBoundAnnos;
      Set<AbstractQualifier> qualifierLowerBounds =
          ai.getBounds().qualifierBounds.get(BoundKind.LOWER);
      if (!qualifierLowerBounds.isEmpty()) {
        QualifierHierarchy qh = context.typeFactory.getQualifierHierarchy();
        lowerBoundAnnos = AbstractQualifier.lub(qualifierLowerBounds, context);
        if (lowerBound != null) {
          if (lowerBound.getAnnotatedType().getKind() != TypeKind.TYPEVAR) {
            Set<? extends AnnotationMirror> newLubAnnos =
                qh.leastUpperBoundsQualifiersOnly(
                    lowerBoundAnnos, lowerBound.getAnnotatedType().getPrimaryAnnotations());
            lowerBound.getAnnotatedType().replaceAnnotations(newLubAnnos);
            lowerBoundAnnos = newLubAnnos;
          } else {
            AnnotatedTypeVariable lubTV = (AnnotatedTypeVariable) lowerBound.getAnnotatedType();
            Set<? extends AnnotationMirror> newLubAnnos =
                qh.leastUpperBoundsQualifiersOnly(
                    lowerBoundAnnos, lubTV.getLowerBound().getPrimaryAnnotations());
            lubTV.getLowerBound().replaceAnnotations(newLubAnnos);
            lowerBoundAnnos = newLubAnnos;
          }
        }
      } else {
        lowerBoundAnnos = Collections.emptySet();
      }

      Set<AbstractType> upperBounds = ai.getBounds().upperBounds();
      AbstractType upperBound = context.inferenceTypeFactory.glb(upperBounds);
      Set<? extends AnnotationMirror> upperBoundAnnos;
      Set<AbstractQualifier> qualifierUpperBounds =
          ai.getBounds().qualifierBounds.get(BoundKind.UPPER);
      if (!qualifierUpperBounds.isEmpty()) {
        upperBoundAnnos = AbstractQualifier.glb(qualifierUpperBounds, context);
        if (upperBound != null) {
          upperBoundAnnos =
              context
                  .typeFactory
                  .getQualifierHierarchy()
                  .greatestLowerBoundsQualifiersOnly(
                      upperBoundAnnos, upperBound.getAnnotatedType().getPrimaryAnnotations());
          upperBound.getAnnotatedType().replaceAnnotations(upperBoundAnnos);
        }
      } else {
        upperBoundAnnos = Collections.emptySet();
      }

      typeVar.add(ai.getJavaType());
      AbstractType freshTypeVar =
          context.inferenceTypeFactory.createFreshTypeVariable(
              lowerBound, lowerBoundAnnos, upperBound, upperBoundAnnos);
      typeArg.add(freshTypeVar);
    }

    List<AbstractType> subsTypeArg =
        context.inferenceTypeFactory.getSubsTypeArgs(typeVar, typeArg, asList);

    // Create the new bounds.
    for (int i = 0; i < asList.size(); i++) {
      Variable ai = asList.get(i);
      ai.getBounds().addBound(null, VariableBounds.BoundKind.EQUAL, subsTypeArg.get(i));
    }

    boundSet.incorporateToFixedPoint(resolvedBoundSet);
    return boundSet;
  }
}
