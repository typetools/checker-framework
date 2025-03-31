package org.checkerframework.framework.util.typeinference8.bound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;
import org.checkerframework.framework.util.typeinference8.types.CaptureVariable;
import org.checkerframework.framework.util.typeinference8.types.Dependencies;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Resolution;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.plumelib.util.StringsPlume;

/**
 * Manages a set of bounds. Bounds are stored in the variable to which they apply, except for
 * capture bounds which are stored in this class.
 */
public class BoundSet implements ReductionResult {
  /**
   * Max number of incorporation loops. Use same constant as {@link
   * com.sun.tools.javac.comp.Infer#MAX_INCORPORATION_STEPS}
   */
  // TODO: revert to com.sun.tools.javac.comp.Infer#MAX_INCORPORATION_STEPS
  public static final int MAX_INCORPORATION_STEPS = 1000;

  /** All inference variables in this bound set. */
  private final LinkedHashSet<Variable> variables;

  /** All capture bounds. */
  private final LinkedHashSet<CaptureBound> captures;

  /** The context. */
  private final Java8InferenceContext context;

  /**
   * If true, then type argument inference failed because an annotated type could not be inferred.
   */
  public boolean annoInferenceFailed = false;

  /** The error message to report to users. */
  public String errorMsg = "";

  /** Whether this bounds set contains the false bound. */
  private boolean containsFalse;

  /** Whether unchecked conversion was necessary to reduce and incorporate this bound set. */
  private boolean uncheckedConversion;

  /**
   * Creates a bound set.
   *
   * @param context the context
   */
  public BoundSet(Java8InferenceContext context) {
    assert context != null;
    this.variables = new LinkedHashSet<>();
    this.captures = new LinkedHashSet<>();
    this.context = context;
    this.containsFalse = false;
    this.uncheckedConversion = false;
  }

  /**
   * Copy constructor.
   *
   * @param toCopy bound set to copy
   */
  public BoundSet(BoundSet toCopy) {
    this.context = toCopy.context;
    this.containsFalse = toCopy.containsFalse;
    this.captures = new LinkedHashSet<>(toCopy.captures);
    this.variables = new LinkedHashSet<>(toCopy.variables);
    this.uncheckedConversion = toCopy.uncheckedConversion;
  }

  /**
   * Save the current state of the variables so they can be restored if the first attempt at
   * resolution fails.
   */
  public void saveBounds() {
    for (Variable v : variables) {
      v.save();
    }
  }

  /**
   * Restore the bounds to the last saved state. This method is called if the first attempt at
   * resolution fails.
   */
  public void restore() {
    for (Variable v : variables) {
      v.restore();
    }
  }

  /**
   * Creates a new bound set for the variables in theta. (The initial bounds for the variables were
   * added to the variables when theta was created.)
   *
   * @param theta a Map from type variable to inference variable
   * @param context inference context
   * @return initial bounds
   */
  public static BoundSet initialBounds(Theta theta, Java8InferenceContext context) {
    BoundSet boundSet = new BoundSet(context);
    boundSet.variables.addAll(theta.values());
    return boundSet;
  }

  /**
   * Merges {@code newSet} into this bound set.
   *
   * @param newSet bound set to merge
   * @return whether the merge changed this bound set
   */
  public boolean merge(BoundSet newSet) {
    boolean changed = captures.addAll(newSet.captures);
    changed |= variables.addAll(newSet.variables);
    containsFalse |= newSet.containsFalse;
    uncheckedConversion |= newSet.uncheckedConversion;
    annoInferenceFailed |= newSet.annoInferenceFailed;
    if (this.errorMsg.isEmpty()) {
      this.errorMsg = newSet.errorMsg;
    } else if (!newSet.errorMsg.isEmpty()) {
      this.errorMsg += System.lineSeparator() + newSet.errorMsg;
    }
    return changed;
  }

  /** Adds the false bound to this bound set. */
  public void addFalse() {
    containsFalse = true;
  }

  /**
   * Return whether this bound set contains false.
   *
   * @return whether this bound set contains false
   */
  public boolean containsFalse() {
    return containsFalse;
  }

  /**
   * Return whether unchecked conversion was necessary to reduce and incorporate this bound set
   *
   * @return whether unchecked conversion was necessary to reduce and incorporate this bound set
   */
  public boolean isUncheckedConversion() {
    return uncheckedConversion;
  }

  /**
   * Sets whether unchecked conversion was necessary to reduce and incorporate this bound set.
   *
   * @param uncheckedConversion whether unchecked conversion was necessary to reduce and incorporate
   *     this bound set
   */
  public void setUncheckedConversion(boolean uncheckedConversion) {
    this.uncheckedConversion = uncheckedConversion;
  }

  /**
   * Adds {@code capture} to this bound set.
   *
   * @param capture a capture bound
   */
  public void addCapture(CaptureBound capture) {
    captures.add(capture);
    variables.addAll(capture.getAllVariablesOnLHS());
  }

  /**
   * Does the bound set contain a bound of the form {@code G<..., ai, ...> = capture(G<...>)} for
   * any variable in {@code as}?
   *
   * @param as a collection of varialbes
   * @return whether the bound set contain a bound of the form {@code G<..., ai, ...> =
   *     capture(G<...>)} for any variable in {@code as}
   */
  public boolean containsCapture(Collection<Variable> as) {
    List<Variable> list = new ArrayList<>();
    for (CaptureBound c : captures) {
      list.addAll(c.getAllVariablesOnLHS());
    }
    for (Variable ai : as) {
      if (list.contains(ai)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a list of variables in {@code alphas} that are instantiated.
   *
   * @param alphas a list of variables
   * @return a list of variables in {@code alphas} that are instantiated
   */
  public List<Variable> getInstantiationsInAlphas(Collection<Variable> alphas) {
    List<Variable> list = new ArrayList<>();
    for (Variable var : alphas) {
      if (var.getBounds().hasInstantiation()) {
        list.add(var);
      }
    }
    return list;
  }

  /**
   * Returns a list of all variables in this bound set that are instantiated.
   *
   * @return a list of all variables in this bound set that are instantiated
   */
  public List<Variable> getInstantiatedVariables() {
    List<Variable> list = new ArrayList<>();
    for (Variable var : variables) {
      if (var.getBounds().hasInstantiation()) {
        list.add(var);
      }
    }
    return list;
  }

  /**
   * Resolve all inference variables mentioned in any bound.
   *
   * @return a list of resolved variables in this bounds set
   */
  public List<Variable> resolve() {
    BoundSet b = Resolution.resolve(new ArrayList<>(variables), this, context);
    return b.getInstantiationsInAlphas(variables);
  }

  /**
   * Returns the dependencies between variables.
   *
   * @return the dependencies between variables
   */
  public Dependencies getDependencies() {
    return getDependencies(new ArrayList<>());
  }

  /**
   * Adds the {@code additionalVars} to this bound set and returns the dependencies between all
   * variables in this bound set.
   *
   * @param additionalVars variables to add to this bound set
   * @return the dependencies between all variables in this bound set
   */
  public Dependencies getDependencies(Collection<Variable> additionalVars) {
    for (Theta t : context.maps.values()) {
      variables.addAll(t.values());
    }
    //    variables.addAll(additionalVars);
    Dependencies dependencies = new Dependencies();

    for (CaptureBound capture : captures) {
      List<? extends CaptureVariable> lhsVars = capture.getAllVariablesOnLHS();
      Set<Variable> rhsVars = capture.getAllVariablesOnRHS();
      for (Variable var : lhsVars) {
        // An inference variable alpha appearing on the left-hand side of a bound of the
        // form G<..., alpha, ...> = capture(G<...>) depends on the resolution of every
        // other inference variable mentioned in this bound (on both sides of the = sign).
        dependencies.putOrAddAll(var, rhsVars);
        dependencies.putOrAddAll(var, lhsVars);
      }
    }
    Set<Variable> allVariables = new LinkedHashSet<>(variables);
    allVariables.addAll(additionalVars);
    for (Variable alpha : allVariables) {
      LinkedHashSet<Variable> alphaDependencies =
          new LinkedHashSet<>(alpha.getBounds().getVariablesMentionedInBounds());

      if (alpha.isCaptureVariable()) {
        // If alpha appears on the left-hand side of another bound of the form
        // G<..., alpha, ...> = capture(G<...>), then beta depends on the resolution of
        // alpha.
        for (Variable beta : alphaDependencies) {
          dependencies.putOrAdd(beta, alpha);
        }
      } else {
        for (Variable beta : alphaDependencies) {
          if (!beta.isCaptureVariable()) {
            // Otherwise, alpha depends on the resolution of beta.
            dependencies.putOrAdd(alpha, beta);
          }
        }
      }
      // An inference variable alpha depends on the resolution of itself.
      dependencies.putOrAdd(alpha, alpha);
    }

    // Add transitive dependencies
    dependencies.calculateTransitiveDependencies();

    return dependencies;
  }

  /**
   * Incorporates {@code newBounds} into this bounds set.
   *
   * <p>Incorporation creates new constraints that are then reduced to a bound set which is further
   * incorporated into this bound set. Incorporation terminates when the bounds set has reached a
   * fixed point. <a
   * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.3">JLS 18 .1</a>
   * defines this fixed point and further explains incorporation.
   *
   * @param newBounds bounds to incorporate
   */
  public void incorporateToFixedPoint(final BoundSet newBounds) {
    this.containsFalse |= newBounds.containsFalse;
    if (this.containsFalse()) {
      return;
    }
    merge(newBounds);
    int count = 0;
    do {
      count++;
      List<Variable> instantiations = getInstantiatedVariables();
      boolean boundsChangeInst = false;
      if (!instantiations.isEmpty()) {
        for (Variable var : variables) {
          boundsChangeInst = var.getBounds().applyInstantiationsToBounds();
        }
      }
      boundsChangeInst |= captures.addAll(newBounds.captures);
      for (Variable alpha : variables) {
        boundsChangeInst = alpha.getBounds().applyInstantiationsToBounds();

        while (!alpha.getBounds().constraints.isEmpty()) {
          boundsChangeInst = true;
          merge(alpha.getBounds().constraints.reduceOneStep(context));
          alpha.getBounds().applyInstantiationsToBounds();
        }
      }
      if (newBounds.isUncheckedConversion()) {
        this.setUncheckedConversion(true);
      }

      if (!boundsChangeInst) {
        return;
      }

      containsFalse |= newBounds.containsFalse;
      assert count < MAX_INCORPORATION_STEPS : "Max incorporation steps reached.";
    } while (!containsFalse && count < MAX_INCORPORATION_STEPS);
  }

  /**
   * Remove any capture bound that mentions any variable in {@code as}.
   *
   * @param as a set of variables
   */
  public void removeCaptures(Set<Variable> as) {
    captures.removeIf((CaptureBound c) -> c.isCaptureMentionsAny(as));
  }

  @Override
  public String toString() {
    if (containsFalse) {
      return "FALSE";
    } else if (variables.isEmpty()) {
      return "EMPTY";
    }
    String vars = StringsPlume.join(", ", getInstantiatedVariables());
    if (vars.isEmpty()) {
      return "No instantiated variables";
    } else {
      return "Instantiated variables: " + vars;
    }
  }
}
