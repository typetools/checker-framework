package org.checkerframework.framework.util.typeinference8.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.ContainsInferenceVariable;
import org.checkerframework.framework.util.typeinference8.types.Dependencies;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds;

/**
 * Resolution finds an instantiation for each variable in a given set of variables. It does this
 * usings all the bounds on a variable. Because a bound on a variable by be another unresolved
 * variable, the order in which the variables must be computed before resolution. If the set of
 * variables contains any captured variables, then a different resolution algorthim is used. If a
 * set of variables does not contain a captured variable, but the resolution fails, then the
 * resolution algorithm for captured variables is used.
 *
 * <p>Resolution is discussed in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.4">JLS Section
 * 18.4</a>.
 *
 * <p>Entry point is two static methods, {@link #resolveSmallestSet(LinkedHashSet, BoundSet)} and
 * {@link #resolve(Variable, BoundSet, Java8InferenceContext)}, which create {@link Resolution}
 * objects that actually preform the resolution.
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
    List<Variable> resolvedVars = boundSet.getInstantiatedVariables();
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

  private final Java8InferenceContext context;
  /** The set of dependencies between the variables. */
  private final Dependencies dependencies;

  private Resolution(Java8InferenceContext context, Dependencies dependencies) {
    this.context = context;
    this.dependencies = dependencies;
  }

  /**
   * Resolve all the variables in {@code unresolvedVars}.
   *
   * @param boundSet current bound set
   * @param unresolvedVars set of unresolved variables that includes all dependencies
   * @return the bounds set with the resolved bounds
   */
  private BoundSet resolve(BoundSet boundSet, Queue<Variable> unresolvedVars) {
    List<Variable> resolvedVars = boundSet.getInstantiatedVariables();

    while (!unresolvedVars.isEmpty()) {
      assert !boundSet.containsFalse();

      LinkedHashSet<Variable> smallestDependencySet =
          getSmallestDependecySet(resolvedVars, unresolvedVars);

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
  private LinkedHashSet<Variable> getSmallestDependecySet(
      List<Variable> resolvedVars, Queue<Variable> unresolvedVars) {
    LinkedHashSet<Variable> smallestDependencySet = null;
    // This loop is looking for the smallest set of dependencies that have not been resolved.
    for (Variable alpha : unresolvedVars) {
      LinkedHashSet<Variable> alphasDependencySet = dependencies.get(alpha);
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
  private BoundSet resolveSmallestSet(LinkedHashSet<Variable> as, BoundSet boundSet) {
    assert !boundSet.containsFalse();

    BoundSet resolvedBounds;
    if (boundSet.containsCapture(as)) {
      // First resolve the non-capture variables using the usual resolution algorithm.
      //            for (Variable ai : as) {
      //                if (!ai.isCaptureVariable()) {
      //                    resolveNoCapture(ai);
      //                }
      //            }
      fixes(new ArrayList<>(as), boundSet);
      as.removeAll(boundSet.getInstantiatedVariables());
      // Then resolve the capture variables
      resolvedBounds = resolveWithCapture(as, boundSet, context);
    } else {
      BoundSet copy = new BoundSet(boundSet);
      // Save the current bounds in case the first attempt at resolution fails.
      copy.saveBounds();
      try {
        resolvedBounds = resolveNoCapture(as, boundSet);
      } catch (FalseBoundException ex) {
        resolvedBounds = null;
      }
      if (resolvedBounds == null || resolvedBounds.containsFalse()) {
        boundSet = copy;
        // If resolveNoCapture fails, then undo an resolved variables from the failed
        // attempt.
        boundSet.restore();
        resolvedBounds = resolveWithCapture(as, boundSet, context);
      }
    }
    return resolvedBounds;
  }

  // TODO: I'm not sure what's going on with this method, I need to investigate it.
  private void fixes(List<Variable> variables, BoundSet boundSet) {
    Variable smallV;
    do {
      smallV = null;
      int smallest = Integer.MAX_VALUE;
      for (Variable v : variables) {
        v.getBounds().applyInstantiationsToBounds(boundSet.getInstantiatedVariables());
        if (v.getBounds().hasInstantiation()) {
          variables.remove(v);
          // TODO: Why break rather than searching all variables?
          break;
        }
        if (!v.isCaptureVariable()) {
          int size = v.getBounds().getVariablesMentionedInBounds().size();
          if (size < smallest) {
            smallest = size;
            smallV = v;
          }
        }
      }
      if (smallV != null) {
        resolveNoCapture(smallV);
        variables.remove(smallV);
      }
    } while (smallV != null);
  }

  /** https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.4-320-A */
  private BoundSet resolveNoCapture(LinkedHashSet<Variable> as, BoundSet boundSet) {
    BoundSet resolvedBoundSet = new BoundSet(context);
    for (Variable ai : as) {
      assert !ai.getBounds().hasInstantiation();
      resolveNoCapture(ai);
      if (!ai.getBounds().hasInstantiation()) {
        resolvedBoundSet.addFalse();
        break;
      }
    }
    boundSet.incorporateToFixedPoint(resolvedBoundSet);
    return boundSet;
  }

  /** https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.4-320-A */
  private void resolveNoCapture(Variable ai) {
    assert !ai.getBounds().hasInstantiation();
    LinkedHashSet<ProperType> lowerBounds = ai.getBounds().findProperLowerBounds();
    if (!lowerBounds.isEmpty()) {
      ProperType lub = context.inferenceTypeFactory.lub(lowerBounds);
      ai.getBounds().addBound(VariableBounds.BoundKind.EQUAL, lub);
      return;
    }

    LinkedHashSet<ProperType> upperBounds = ai.getBounds().findProperUpperBounds();
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
                VariableBounds.BoundKind.EQUAL, context.inferenceTypeFactory.getRuntimeException());
      } else {
        ai.getBounds().addBound(VariableBounds.BoundKind.EQUAL, ti);
      }
    }
  }

  /** https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.4-320-B */
  private static BoundSet resolveWithCapture(
      LinkedHashSet<Variable> as, BoundSet boundSet, Java8InferenceContext context) {
    assert !boundSet.containsFalse();
    boundSet.removeCaptures(as);
    BoundSet resolvedBoundSet = new BoundSet(context);
    List<Variable> asList = new ArrayList<>();
    List<TypeVariable> typeVar = new ArrayList<>();
    List<ProperType> typeArg = new ArrayList<>();

    for (Variable ai : as) {
      ai.getBounds().applyInstantiationsToBounds(boundSet.getInstantiatedVariables());
      if (ai.getBounds().hasInstantiation()) {
        // If ai is equal to a variable that was resolved previously,
        // ai would now have an instantiation.
        continue;
      }
      asList.add(ai);
      LinkedHashSet<ProperType> lowerBounds = ai.getBounds().findProperLowerBounds();
      ProperType lowerBound = context.inferenceTypeFactory.lub(lowerBounds);

      LinkedHashSet<AbstractType> upperBounds = ai.getBounds().upperBounds();
      AbstractType upperBound = context.inferenceTypeFactory.glb(upperBounds);

      typeVar.add(ai.getJavaType());
      ProperType freshTypeVar = context.inferenceTypeFactory.createWildcard(lowerBound, upperBound);
      typeArg.add(freshTypeVar);
    }

    List<ProperType> subsTypeArg =
        context.inferenceTypeFactory.getSubsTypeArgs(typeVar, typeArg, asList);

    // Create the new bounds.
    for (int i = 0; i < asList.size(); i++) {
      Variable ai = asList.get(i);
      ContainsInferenceVariable.getMentionedTypeVariables(
          Collections.singleton(ai.getJavaType()), subsTypeArg.get(i).getJavaType());
      ai.getBounds().addBound(VariableBounds.BoundKind.EQUAL, subsTypeArg.get(i).capture(context));
    }

    boundSet.incorporateToFixedPoint(resolvedBoundSet);
    return boundSet;
  }
}
