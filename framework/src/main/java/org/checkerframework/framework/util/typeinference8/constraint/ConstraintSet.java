package org.checkerframework.framework.util.typeinference8.constraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.types.Dependencies;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.FalseBoundException;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.BugInCF;

/** A set of constraints and the operations that can be performed on them. */
public class ConstraintSet implements ReductionResult {

  /** The result given when a constraint set reduces to true. */
  @SuppressWarnings("interning:assignment")
  public static final @InternedDistinct ConstraintSet TRUE =
      new ConstraintSet() {
        @Override
        public String toString() {
          return "TRUE";
        }
      };

  /**
   * The Java types are correct, but the qualifiers are not in the correct relationship. Return this
   * rather than throwing an exception so that type arguments with the correct Java type are still
   * inferred.
   */
  @SuppressWarnings("interning:assignment")
  public static final @InternedDistinct ConstraintSet TRUE_ANNO_FAIL =
      new ConstraintSet(true) {
        @Override
        public String toString() {
          return "TRUE_ANNO_FAIL";
        }
      };

  /** The result given when a constraint set reduces to false. */
  @SuppressWarnings("interning:assignment")
  public static final @InternedDistinct ReductionResult FALSE =
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

  /** Whether inference failed because the qualifiers where not in the correct relationship. */
  private boolean annotationFailure = false;

  /**
   * Creates a new constraint set.
   *
   * @param annotationFailure inference failed because the qualifiers where not in the correct
   *     relationship
   */
  private ConstraintSet(boolean annotationFailure) {
    this();
    this.annotationFailure = annotationFailure;
  }

  /**
   * Creates a constraint set with {@code constraints}.
   *
   * @param constraints constraints to add to the newly created set
   */
  public ConstraintSet(Constraint... constraints) {
    if (constraints != null) {
      list = new ArrayList<>(constraints.length);
      list.addAll(Arrays.asList(constraints));
    } else {
      list = new ArrayList<>();
    }
  }

  /**
   * Adds {@code c} to this set, if c isn't already in the list.
   *
   * @param c a constraint to add to this set
   */
  public void add(Constraint c) {
    if (c != null && !list.contains(c)) {
      list.add(c);
    }
  }

  /**
   * Adds all constraints in {@code constraintSet} to this constraint set.
   *
   * @param constraintSet a set of constraints to add to this set
   */
  public void addAll(ConstraintSet constraintSet) {
    if (constraintSet.annotationFailure) {
      this.annotationFailure = true;
    }
    constraintSet.list.forEach(this::add);
  }

  /**
   * Adds all constraints in {@code constraintSet} to this constraint set.
   *
   * @param constraintSet a collection of constraints to add to this set
   */
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

  /**
   * Adds the constraint to the beginning of the set.
   *
   * @param constraint a constraint
   */
  public void push(Constraint constraint) {
    if (constraint != null && !list.contains(constraint)) {
      list.add(0, constraint);
    }
  }

  /**
   * Adds the constraints to the beginning of the set and matatines the order of the constraints.
   *
   * @param constraints constraints
   */
  public void push(ConstraintSet constraints) {
    for (int i = constraints.list.size() - 1; i > -1; i--) {
      push(constraints.list.get(i));
    }
  }

  /**
   * Remove all constraints in {@code subset} from this constraint set.
   *
   * @param subset the set of constraints to remove from this set
   */
  @SuppressWarnings("interning:not.interned")
  public void remove(ConstraintSet subset) {
    if (this == subset) {
      list.clear();
    }
    list.removeAll(subset.list);
  }

  /**
   * A subset of constraints is selected in this constraint set, satisfying the property that, for
   * each constraint, no input variable can influence an output variable of another constraint in
   * this constraint set. (See JLS 18.5.2.2)
   *
   * @param dependencies an object describing the dependencies of inference variables
   * @return s a subset of constraints is this constraint set
   */
  public ConstraintSet getClosedSubset(Dependencies dependencies) {
    ConstraintSet subset = new ConstraintSet();
    Set<Variable> inputDependencies = new LinkedHashSet<>();
    Set<Variable> outDependencies = new LinkedHashSet<>();
    for (Constraint constraint : list) {
      if (constraint.getKind() == Kind.EXPRESSION
          || constraint.getKind() == Kind.LAMBDA_EXCEPTION
          || constraint.getKind() == Kind.METHOD_REF_EXCEPTION) {
        TypeConstraint c = (TypeConstraint) constraint;
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
        subset.add(constraint);
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
    for (Constraint constraint : list) {
      if (!(constraint instanceof TypeConstraint)) {
        continue;
      }
      TypeConstraint c = (TypeConstraint) constraint;
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
    for (Constraint c : list) {
      if (c instanceof TypeConstraint) {
        vars.addAll(((TypeConstraint) c).getInferenceVariables());
      }
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
      if (constraint instanceof TypeConstraint) {
        vars.addAll(((TypeConstraint) constraint).getInputVariables());
      }
    }
    return vars;
  }

  /** Applies the instantiations to all the constraints in this set. */
  public void applyInstantiations() {
    for (Constraint constraint : list) {
      if (constraint instanceof TypeConstraint) {
        ((TypeConstraint) constraint).applyInstantiations();
      }
    }
  }

  @Override
  public String toString() {
    return "Size: " + list.size();
  }

  /**
   * Reduces all the constraints in this set. (See JLS 18.2)
   *
   * @param context the context
   * @return the bound set produced by reducing this constraint set
   */
  public BoundSet reduce(Java8InferenceContext context) {
    BoundSet boundSet = new BoundSet(context);
    while (!this.isEmpty()) {
      if (this.list.size() > BoundSet.MAX_INCORPORATION_STEPS) {
        throw new BugInCF("TO MANY CONSTRAINTS: %s", context.pathToExpression.getLeaf());
      }
      BoundSet result = reduceOneStep(context);
      boundSet.merge(result);
    }
    return boundSet;
  }

  /**
   * Reduces all the constraints in this set. (See JLS 18.2) If an {@link AdditionalArgument} is
   * found it is reduced one step and then this method returns.
   *
   * @param context the context
   * @return the bound set produced by reducing this constraint set
   */
  public BoundSet reduceAdditionalArgOnce(Java8InferenceContext context) {
    BoundSet boundSet = new BoundSet(context);
    while (!this.isEmpty()) {
      if (this.list.size() > BoundSet.MAX_INCORPORATION_STEPS) {
        throw new BugInCF("TOO MANY CONSTRAINTS: %s", context.pathToExpression.getLeaf());
      }
      boolean foundAA = this.list.get(0).getKind() == Kind.ADDITIONAL_ARG;
      BoundSet result = reduceOneStep(context);
      if (foundAA) {
        return boundSet;
      }
      boundSet.merge(result);
    }
    return boundSet;
  }

  /**
   * Reduce one constraint in this set.
   *
   * @param context the context
   * @return the result of reducing one constraint in this set
   */
  public BoundSet reduceOneStep(Java8InferenceContext context) {
    boolean alreadyFailed = this.annotationFailure;
    BoundSet boundSet = new BoundSet(context);

    Constraint constraint = this.pop();
    ReductionResult result = constraint.reduce(context);
    if (result instanceof ReductionResultPair) {
      boundSet.merge(((ReductionResultPair) result).boundSet);
      if (boundSet.containsFalse()) {
        throw new FalseBoundException(constraint, result);
      }
      this.addAll(((ReductionResultPair) result).constraintSet);
    } else if (result instanceof TypeConstraint) {
      // Add the new constraints to the beginning of the list so they are reduced first. This is
      // because
      // each constraint is supposed to be fully resolved before moving onto another one.
      this.push((Constraint) result);
    } else if (result instanceof ConstraintSet) {
      // Add the new constraints to the beginning of the list so they are reduced first. This is
      // because
      // each constraint is supposed to be fully resolved before moving onto another one.
      this.push((ConstraintSet) result);
    } else if (result instanceof BoundSet) {
      boundSet.merge((BoundSet) result);
      if (boundSet.containsFalse()) {
        throw new FalseBoundException(constraint, result);
      }
    } else if (result == null || result == ConstraintSet.FALSE) {
      throw new FalseBoundException(constraint, result);
    } else if (result == UNCHECKED_CONVERSION) {
      boundSet.setUncheckedConversion(true);
    } else {
      throw new RuntimeException("Not found " + result);
    }
    if (this.annotationFailure) {
      boundSet.annoInferenceFailed = true;
      if (!alreadyFailed && boundSet.errorMsg.isEmpty()) {
        boundSet.errorMsg = constraint.toString();
      }
    }
    return boundSet;
  }
}
