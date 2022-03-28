package org.checkerframework.framework.util.typeinference8.constraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.bound.FalseBound;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.types.Dependencies;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.FalseBoundException;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;

/** A set of constraints and the operations that can be performed on them. */
public class ConstraintSet implements ReductionResult {
  public static final ConstraintSet TRUE =
      new ConstraintSet() {
        @Override
        public String toString() {
          return "TRUE";
        }
      };

  public static final ConstraintSet TRUE_ANNO_FAIL =
      new ConstraintSet(true) {
        @Override
        public String toString() {
          return "TRUE_ANNO_FAIL";
        }
      };

  public static final ReductionResult FALSE =
      new ReductionResult() {
        @Override
        public String toString() {
          return "FALSE";
        }
      };

  /**
   * A list of constraints in this set. It does not contain constraints that are equal. This needs
   * to be kept in the order created, which should be lexically left to right. This is so the {@link
   * #getClosedSubset(Dependencies)} is computed correctly.
   */
  private final List<Constraint> list;

  private boolean annotationFailure = false;

  private ConstraintSet(boolean annotationFailure) {
    this();
    this.annotationFailure = annotationFailure;
  }

  public ConstraintSet(Constraint... constraints) {
    if (constraints != null) {
      list = new ArrayList<>(constraints.length);
      list.addAll(Arrays.asList(constraints));
    } else {
      list = new ArrayList<>();
    }
  }

  /** Adds {@code c} to this set, if c isn't already in the list. */
  public void add(Constraint c) {
    if (c != null && !list.contains(c)) {
      list.add(c);
    }
  }

  /** Adds all constraints in {@code constraintSet} to this constraint set. */
  public void addAll(ConstraintSet constraintSet) {
    if (constraintSet == TRUE_ANNO_FAIL) {
      constraintSet.annotationFailure = true;
    }
    list.addAll(constraintSet.list);
  }

  /** Adds all constraints in {@code constraintSet} to this constraint set. */
  public void addAll(Collection<? extends Constraint> constraintSet) {
    list.addAll(constraintSet);
  }

  /**
   * Return whether or not this constraint set is empty.
   *
   * @return whether or not this constraint set is empty
   */
  public boolean isEmpty() {
    return list.isEmpty();
  }

  /**
   * Removes and returns the first constraint that was added to this set.
   *
   * @return first constraint that was added to this set
   */
  public Constraint pop() {
    assert !isEmpty();
    return list.remove(0);
  }

  /** Remove all constraints in {@code subset} from this constraint set. */
  public void remove(ConstraintSet subset) {
    if (this == subset) {
      list.clear();
    }
    list.removeAll(subset.list);
  }

  /**
   * A subset of constraints is selected in C, satisfying the property that, for each constraint, no
   * input variable can influence an output variable of another constraint in C. (See JLS 18.5.2.2)
   */
  public ConstraintSet getClosedSubset(Dependencies dependencies) {
    ConstraintSet subset = new ConstraintSet();
    Set<Variable> inputDependencies = new LinkedHashSet<>();
    Set<Variable> outDependencies = new LinkedHashSet<>();
    for (Constraint c : list) {
      if (c.getKind() == Kind.EXPRESSION
          || c.getKind() == Kind.LAMBDA_EXCEPTION
          || c.getKind() == Kind.METHOD_REF_EXCEPTION) {
        Set<Variable> newInputs = dependencies.get(c.getInputVariables());
        Set<Variable> newOutputs = dependencies.get(c.getOutputVariables());
        if (Collections.disjoint(newInputs, outDependencies)
            && Collections.disjoint(newOutputs, inputDependencies)) {
          inputDependencies.addAll(newInputs);
          outDependencies.addAll(newOutputs);
          subset.add(c);
        } else {
          // A cycle (or cycles) in the graph of dependencies between constraints exists.
          subset = new ConstraintSet();
          break;
        }
      } else {
        subset.add(c);
      }
    }

    if (!subset.isEmpty()) {
      return subset;
    }

    outDependencies.clear();
    inputDependencies.clear();
    // If this subset is empty, then there is a cycle (or cycles) in the graph of dependencies
    // between constraints.
    List<Constraint> consideredConstraints = new ArrayList<>();
    for (Constraint c : list) {
      Set<Variable> newInputs = dependencies.get(c.getInputVariables());
      Set<Variable> newOutputs = dependencies.get(c.getOutputVariables());
      if (inputDependencies.isEmpty()
          || !Collections.disjoint(newInputs, outDependencies)
          || !Collections.disjoint(newOutputs, inputDependencies)) {
        inputDependencies.addAll(newInputs);
        outDependencies.addAll(newOutputs);
        consideredConstraints.add(c);
      }
    }

    // A single constraint is selected from the considered constraints, as follows:

    // If any of the considered constraints have the form <Expression -> T>, then the selected
    // constraint is the considered constraint of this form that contains the expression to the
    // left (3.5) of the expression of every other considered constraint of this form.

    // If no considered constraint has the form <Expression -> T>, then the selected constraint
    // is the considered constraint that contains the expression to the left of the expression
    // of every other considered constraint.

    for (Constraint c : consideredConstraints) {
      if (c.getKind() == Kind.EXPRESSION) {
        return new ConstraintSet(c);
      }
    }

    return new ConstraintSet(consideredConstraints.get(0));
  }

  /**
   * Return all variables mentioned by any constraint in this set.
   *
   * @return all variables mentioned by any constraint in this set
   */
  public Set<Variable> getAllInferenceVariables() {
    Set<Variable> vars = new LinkedHashSet<>();
    for (Constraint constraint : list) {
      vars.addAll(constraint.getInferenceVariables());
    }
    return vars;
  }

  /**
   * Return all input variables for all constraints in this set.
   *
   * @return all input variables for all constraints in this set
   */
  public Set<Variable> getAllInputVariables() {
    Set<Variable> vars = new LinkedHashSet<>();
    for (Constraint constraint : list) {
      vars.addAll(constraint.getInputVariables());
    }
    return vars;
  }

  /** Applies the instantiations to all the constraints in this set. */
  public void applyInstantiations(List<Variable> instantiations) {
    for (Constraint constraint : list) {
      constraint.applyInstantiations(instantiations);
    }
  }

  @Override
  public String toString() {
    return "Size: " + list.size();
  }

  /**
   * Reduces all the constraints in this set. (See JLS 18.2)
   *
   * @return the bound set produced by reducing this constraint set
   */
  public BoundSet reduce(Java8InferenceContext context) {
    BoundSet boundSet = new BoundSet(context);
    while (!this.isEmpty()) {
      Constraint constraint = this.pop();
      ReductionResult result = constraint.reduce(context);
      if (result instanceof ReductionResultPair) {
        boundSet.merge(((ReductionResultPair) result).boundSet);
        if (boundSet.containsFalse()) {
          throw new FalseBoundException(constraint, result);
        }
        this.addAll(((ReductionResultPair) result).constraintSet);
      } else if (result instanceof Constraint) {
        this.add((Constraint) result);
      } else if (result instanceof ConstraintSet) {
        this.addAll((ConstraintSet) result);
      } else if (result instanceof BoundSet) {
        boundSet.merge((BoundSet) result);
        if (boundSet.containsFalse()) {
          throw new FalseBoundException(constraint, result);
        }
      } else if (result == null || result == ConstraintSet.FALSE || result instanceof FalseBound) {
        throw new FalseBoundException(constraint, result);
      } else if (result == UNCHECKED_CONVERSION) {
        boundSet.setUncheckedConversion(true);
      } else {
        throw new RuntimeException("Not found " + result);
      }
    }
    if (this.annotationFailure) {
      boundSet.annoFail = true;
    }
    return boundSet;
  }
}
