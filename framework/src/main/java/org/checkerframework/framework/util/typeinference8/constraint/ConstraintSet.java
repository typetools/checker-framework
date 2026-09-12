package org.checkerframework.framework.util.typeinference8.constraint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
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

/**
 * A set of constraints and the operations that can be performed on them.
 *
 * <p>If you add a method that modifies a constraint set, override that method in {@code
 * ImmutableConstraintSet}, so that the constant constraint sets {@link #TRUE} and {@link
 * #TRUE_ANNO_FAIL} remain immutable.
 */
public class ConstraintSet implements ReductionResult {

  /**
   * Max number of constraints in a constraint set. Reducing a constraint can create new
   * constraints, so a constraint set can grow during reduction; if it grows this large, then
   * reduction is most likely not terminating.
   */
  public static final int MAX_CONSTRAINTS = 10000;

  /** The result given when a constraint set reduces to true. It is empty and immutable. */
  @SuppressWarnings("interning:assignment")
  public static final @InternedDistinct ConstraintSet TRUE =
      new ImmutableConstraintSet("TRUE", false);

  /**
   * The Java types are correct, but the qualifiers are not in the correct relationship. Return this
   * rather than throwing an exception so that type arguments with the correct Java type are still
   * inferred.
   *
   * <p>It is empty and immutable.
   */
  @SuppressWarnings("interning:assignment")
  public static final @InternedDistinct ConstraintSet TRUE_ANNO_FAIL =
      new ImmutableConstraintSet("TRUE_ANNO_FAIL", true);

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
   * The constraints in this set. It does not contain constraints that are equal. This needs to be
   * kept in the order created, which should be lexically left to right. This is so the {@link
   * #getClosedSubset(ConstraintSet, Dependencies)} is computed correctly.
   */
  private final Deque<Constraint> queue;

  /**
   * The same constraints as {@link #queue}, for constant-time membership tests. {@link
   * #applyInstantiations} rebuilds it, because applying instantiations changes the hash code of a
   * constraint.
   *
   * <p>A constraint can be mutated through some other constraint set that contains it, which leaves
   * a stale entry here: one filed under the constraint's former hash code. A lookup never finds a
   * stale entry, because a constraint equal to the mutated constraint has the mutated constraint's
   * current hash code. So the only consequence is that a constraint equal to a mutated constraint
   * might be added to this set a second time, which costs an extra reduction but does not change
   * the result of reduction.
   */
  private final Set<Constraint> members;

  /** True if inference failed because the qualifiers were not in the correct relationship. */
  private boolean annotationFailure = false;

  /**
   * Creates an empty constraint set whose constraints cannot be modified. Only {@code
   * ImmutableConstraintSet} calls this constructor.
   *
   * @param annotationFailure inference failed because the qualifiers were not in the correct
   *     relationship
   */
  private ConstraintSet(boolean annotationFailure) {
    this.queue = new UnmodifiableEmptyDeque();
    this.members = Collections.emptySet();
    this.annotationFailure = annotationFailure;
  }

  /**
   * Creates a constraint set with {@code constraints}.
   *
   * @param constraints constraints to add to the newly created set
   */
  public ConstraintSet(Constraint... constraints) {
    if (constraints != null) {
      queue = new ArrayDeque<>(constraints.length);
      members = new LinkedHashSet<>(constraints.length);
      for (Constraint constraint : constraints) {
        addIfAbsent(constraint);
      }
    } else {
      queue = new ArrayDeque<>();
      members = new LinkedHashSet<>();
    }
  }

  /**
   * Adds {@code c} to the end of {@link #queue}, if {@code c} is non-null and no constraint equal
   * to it is already in {@link #queue}. This method is private so that it can be called from the
   * constructor.
   *
   * <p>This method is final because constructors call it.
   *
   * @param c a constraint to add to this set, or null
   */
  private void addIfAbsent(Constraint c) {
    if (c != null && members.add(c)) {
      queue.addLast(c);
    }
  }

  /**
   * Adds {@code c} to this set, if c isn't already in this set.
   *
   * @param c a constraint to add to this set
   */
  public void add(Constraint c) {
    addIfAbsent(c);
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
    constraintSet.queue.forEach(this::add);
  }

  /**
   * Adds all constraints in {@code constraints} to this constraint set.
   *
   * @param constraints a collection of constraints to add to this set
   */
  public void addAll(Collection<? extends Constraint> constraints) {
    constraints.forEach(this::add);
  }

  /**
   * Returns true if this constraint set is empty.
   *
   * @return true if this constraint set is empty
   */
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  /**
   * Removes and returns the first constraint that was added to this set.
   *
   * @return first constraint that was added to this set
   */
  public Constraint pop() {
    assert !isEmpty();
    Constraint result = queue.removeFirst();
    members.remove(result);
    return result;
  }

  /**
   * Adds the constraint to the beginning of this set.
   *
   * @param constraint a constraint
   */
  public void push(Constraint constraint) {
    if (constraint != null && members.add(constraint)) {
      queue.addFirst(constraint);
    }
  }

  /**
   * Adds the constraints to the beginning of this set and maintains the order of the constraints.
   *
   * @param constraints the constraints to add to the beginning of this set
   */
  public void pushAll(ConstraintSet constraints) {
    Iterator<Constraint> itor = constraints.queue.descendingIterator();
    while (itor.hasNext()) {
      this.push(itor.next());
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
      queue.clear();
      members.clear();
      return;
    }
    // Testing membership in a hash-based set, rather than calling `Deque.removeAll`, makes this
    // method take time linear (rather than quadratic) in the sizes of the two constraint sets.
    // `Constraint.equals` is expensive, because it walks the structure of the constrained types.
    Set<Constraint> toRemove = new LinkedHashSet<>(subset.queue);
    queue.removeIf(toRemove::contains);
    members.removeAll(toRemove);
  }

  /**
   * Returns a subset of {@code c}; for each constraint in the subset, no input variable can
   * influence an output variable of another constraint in C. If that subset is empty, returns a set
   * containing a single constraint that participates in a constraint cycle. (See JLS 18.5.2.2)
   *
   * @param c a nonempty constraint set
   * @param dependencies an object describing the dependencies of inference variables
   * @return a subset of constraints in {@code c} whose inputs do not affect {@code c}'s outputs, or
   *     a singleton constraint from a constraint cycle
   */
  public static ConstraintSet getClosedSubset(ConstraintSet c, Dependencies dependencies) {
    if (c.isEmpty()) {
      throw new BugInCF("ConstraintSet.getClosedSubset was passed an empty constraint set.");
    }
    ConstraintSet subset = new ConstraintSet();
    // Collection of all outputs of c.
    Set<Variable> allOutputsOfC = new LinkedHashSet<>();
    for (Constraint constraint : c.queue) {
      if (constraint instanceof TypeConstraint tc) {
        allOutputsOfC.addAll(tc.getOutputVariables());
      }
      // No other constraints have output variables
    }

    // From JLS 18.5.2.2:
    // A subset of constraints is selected in C, satisfying the property that, for each
    // constraint, no input variable can influence an output variable of another
    // constraint in C. The terms input variable and output variable are defined
    // below. An inference variable alpha can influence an inference variable beta if alpha
    // depends on the resolution of beta (§18.4), or vice versa; or if there exists a third
    // inference variable gamma such that alpha can influence gamma and gamma can influence beta.

    // Put another way:
    // Find a subset of the set c where the following is true for all the constraints in the subset:
    // no input variable of a constraint can influence an output variable of any constraint in c.
    // (Influence means that neither variable can depend on the other.)
    // The JLS does not specify whether this subset should be as large as possible, but this
    // implementation returns only one constraint. This seems to match the javac implementation.
    // Issue7019.java shows an example where returning the largest set fails.
    for (Constraint constraint : c.queue) {
      if (constraint.getKind() == Kind.EXPRESSION
          || constraint.getKind() == Kind.LAMBDA_EXCEPTION
          || constraint.getKind() == Kind.METHOD_REF_EXCEPTION
          || constraint.getKind() == Kind.LAMBDA_BODY) {
        List<Variable> inputsOfSingleConstraint = ((TypeConstraint) constraint).getInputVariables();
        boolean foundInfluence = false;
        inputLoop:
        for (Variable in : inputsOfSingleConstraint) {
          Set<Variable> inDependencies = dependencies.get(in);
          for (Variable out : allOutputsOfC) {
            if (inDependencies.contains(out) || dependencies.get(out).contains(in)) {
              foundInfluence = true;
              break inputLoop;
            }
          }
        }
        if (!foundInfluence) {
          // None of the inputs of constraint influence any output of any constraint in C.
          subset.add(constraint);
        }
      } else {
        // Other kinds of constraints do not have input variables.
        subset.add(constraint);
      }
    }

    if (!subset.isEmpty()) {
      // Return the first expression constraint; if there are none, return the first constraint.
      for (Constraint constraint : subset.queue) {
        if (constraint.getKind() == Kind.EXPRESSION) {
          return new ConstraintSet(constraint);
        }
      }

      return new ConstraintSet(subset.queue.getFirst());
    }

    // TODO: double check that this code is correct.
    // checker/tests/all-systems/java8inference/MapEntryGetFails.java is a test that uses this code.

    Set<Variable> inputDependencies = new LinkedHashSet<>();
    Set<Variable> outputDependencies = new LinkedHashSet<>();
    // If this subset is empty then no closed subset was found and there is a cycle (or cycles) in
    // the graph of dependencies between constraints.

    // From JLS 18.5.2.2:
    // In this case, the constraints in C that participate in a dependency cycle (or cycles) and do
    // not depend on any constraints outside of the cycle (or cycles) are considered.
    // A single constraint is selected from the considered constraints, as follows:

    // If any of the considered constraints have the form <Expression -> T>, then the selected
    // constraint is the considered constraint of this form that contains the expression to the
    // left (3.5) of the expression of every other considered constraint of this form.

    // If no considered constraint has the form <Expression -> T>, then the selected constraint
    // is the considered constraint that contains the expression to the left of the expression
    // of every other considered constraint.
    List<Constraint> consideredConstraints = new ArrayList<>();
    for (Constraint constraint : c.queue) {
      if (!(constraint instanceof TypeConstraint typeConstraint)) {
        continue;
      }

      Set<Variable> newInputs = dependencies.get(typeConstraint.getInputVariables());
      Set<Variable> newOutputs = dependencies.get(typeConstraint.getOutputVariables());
      if (inputDependencies.isEmpty()
          || !Collections.disjoint(newInputs, outputDependencies)
          || !Collections.disjoint(newOutputs, inputDependencies)) {
        inputDependencies.addAll(newInputs);
        outputDependencies.addAll(newOutputs);
        consideredConstraints.add(typeConstraint);
      }
    }

    for (Constraint constraint : consideredConstraints) {
      if (constraint.getKind() == Kind.EXPRESSION) {
        return new ConstraintSet(constraint);
      }
    }

    // `consideredConstraints` is nonempty:  `c` is nonempty (checked at the top of this method),
    // and `subset` is empty only if every constraint in `c` is a TypeConstraint (the first loop in
    // this method adds every other kind of constraint to `subset`).  The loop that populates
    // `consideredConstraints` adds the first TypeConstraint in `c`, because `inputDependencies`
    // starts out empty.
    return new ConstraintSet(consideredConstraints.get(0));
  }

  /**
   * Returns all variables mentioned by any constraint in this set.
   *
   * @return all variables mentioned by any constraint in this set
   */
  public Set<Variable> getAllInferenceVariables() {
    Set<Variable> vars = new LinkedHashSet<>();
    for (Constraint c : queue) {
      if (c instanceof TypeConstraint tc) {
        vars.addAll(tc.getInferenceVariables());
      }
    }
    return vars;
  }

  /**
   * Returns all input variables for all constraints in this set.
   *
   * @return all input variables for all constraints in this set
   */
  public Set<Variable> getAllInputVariables() {
    Set<Variable> vars = new LinkedHashSet<>();
    for (Constraint constraint : queue) {
      if (constraint instanceof TypeConstraint tc) {
        vars.addAll(tc.getInputVariables());
      }
    }
    return vars;
  }

  /** Applies the instantiations to all the constraints in this set. */
  public void applyInstantiations() {
    for (Constraint constraint : queue) {
      if (constraint instanceof TypeConstraint tc) {
        tc.applyInstantiations();
      }
    }
    // Applying instantiations changes the hash code of a constraint, and can make two constraints
    // in this set equal to one another.  Rebuild both the queue and the index, discarding every
    // constraint that is now equal to an earlier one.
    List<Constraint> constraints = new ArrayList<>(queue);
    queue.clear();
    members.clear();
    for (Constraint constraint : constraints) {
      addIfAbsent(constraint);
    }
  }

  @Override
  public String toString() {
    return "Size: " + queue.size();
  }

  /**
   * Throws an exception if this set contains more than {@link #MAX_CONSTRAINTS} constraints.
   *
   * @param context the context
   * @throws BugInCF if this set contains more than {@link #MAX_CONSTRAINTS} constraints
   */
  private void checkMaxConstraints(Java8InferenceContext context) {
    if (this.queue.size() > MAX_CONSTRAINTS) {
      // Throw rather than assert, so that this is reported as a
      // "type.argument.inference.crashed" error for this one expression, rather than as an
      // AssertionError that aborts the entire compilation.
      throw new BugInCF(
          "Max constraints (%d) exceeded while reducing: %s",
          MAX_CONSTRAINTS, context.getPathToExpression().getLeaf());
    }
  }

  /**
   * Reduces all the constraints in this set. (See JLS 18.2)
   *
   * @param context the context
   * @return the bound set produced by reducing this constraint set
   * @throws BugInCF if reduction creates more than {@link #MAX_CONSTRAINTS} constraints
   */
  public BoundSet reduce(Java8InferenceContext context) {
    BoundSet boundSet = new BoundSet(context);
    while (!this.isEmpty()) {
      checkMaxConstraints(context);
      BoundSet result = reduceOneStep(context);
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
    if (result instanceof ReductionResultPair rrp) {
      boundSet.merge(rrp.boundSet());
      if (boundSet.containsFalse()) {
        throw new FalseBoundException(constraint, result);
      }
      this.addAll(rrp.constraintSet());
    } else if (result instanceof TypeConstraint tc2) {
      // Add the new constraints to the beginning of this set so they are reduced first. This is
      // because each constraint is supposed to be reduced until no other constraints are produced
      // before moving onto another constraint.
      this.push(tc2);
    } else if (result instanceof ConstraintSet cs) {
      if (result == TRUE_ANNO_FAIL) {
        this.annotationFailure = true;
      } else {
        // Add the new constraints to the beginning of this set so they are reduced first. This is
        // because each constraint is supposed to be reduced until no other constraints are produced
        // before moving onto another constraint.
        this.pushAll(cs);
      }
    } else if (result instanceof BoundSet bs) {
      boundSet.merge(bs);
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
        if (constraint instanceof TypeConstraint tc) {
          boundSet.errorMsg = tc.constraintHistory();
        } else {
          boundSet.errorMsg = constraint.toString();
        }
      }
    }
    return boundSet;
  }

  /** An empty {@code ConstraintSet} that cannot be modified. */
  private static final class ImmutableConstraintSet extends ConstraintSet {

    /** The name of this constraint set; it is the value returned by {@link #toString}. */
    private final String name;

    /**
     * Creates an immutable constraint set.
     *
     * @param name the name of this constraint set; it is the value returned by {@link #toString}
     * @param annotationFailure inference failed because the qualifiers were not in the correct
     *     relationship
     */
    ImmutableConstraintSet(String name, boolean annotationFailure) {
      super(annotationFailure);
      this.name = name;
    }

    /**
     * Returns an exception to throw because this constraint set cannot be modified.
     *
     * @return an exception to throw because this constraint set cannot be modified
     */
    private BugInCF cannotModify() {
      return new BugInCF("Attempted to modify an immutable constraint set: %s", name);
    }

    @Override
    public void add(Constraint c) {
      throw cannotModify();
    }

    @Override
    public void addAll(ConstraintSet constraintSet) {
      throw cannotModify();
    }

    @Override
    public void addAll(Collection<? extends Constraint> constraintSet) {
      throw cannotModify();
    }

    @Override
    public Constraint pop() {
      throw cannotModify();
    }

    @Override
    public void push(Constraint constraint) {
      throw cannotModify();
    }

    @Override
    public void pushAll(ConstraintSet constraints) {
      throw cannotModify();
    }

    @Override
    public void remove(ConstraintSet subset) {
      throw cannotModify();
    }

    @Override
    public void applyInstantiations() {
      throw cannotModify();
    }

    // This method is overridden so that the error message describes reduction, rather than the
    // modification that the inherited implementation's call to pop() would report.
    @Override
    public BoundSet reduceOneStep(Java8InferenceContext context) {
      throw new BugInCF("Attempt to reduce the empty constraint set %s.", name);
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /**
   * An empty deque that cannot be modified; every method that would add an element throws {@link
   * BugInCF}. The immutable constraint sets {@link #TRUE} and {@link #TRUE_ANNO_FAIL} use it, so
   * that a modifying method that is not overridden in {@link ImmutableConstraintSet} fails fast
   * rather than corrupting a constraint set that every inference problem shares.
   *
   * <p>Only {@link #addFirst} and {@link #addLast} are overridden, because {@code ArrayDeque}
   * specifies that every other method that adds an element is equivalent to one of them. The
   * methods that remove an element need no override: on an empty deque, each of them either throws
   * an exception or does nothing.
   */
  private static final class UnmodifiableEmptyDeque extends ArrayDeque<Constraint> {

    /** Unique identifier for serialization. */
    private static final long serialVersionUID = 20260815L;

    /** Creates an UnmodifiableEmptyDeque. */
    UnmodifiableEmptyDeque() {
      super(0);
    }

    @Override
    public void addFirst(Constraint constraint) {
      throw new BugInCF("Attempted to modify an immutable constraint set.");
    }

    @Override
    public void addLast(Constraint constraint) {
      throw new BugInCF("Attempted to modify an immutable constraint set.");
    }
  }
}
