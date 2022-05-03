package org.checkerframework.framework.util.typeinference8.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.Typing;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TypesUtils;

/** Data structure to stores the bounds of a variable. */
public class VariableBounds {

  public enum BoundKind {
    /** {@code other type <: this } */
    LOWER,
    /** {@code this <: other type } */
    UPPER,
    /** {@code this = other type } */
    EQUAL;
  }

  private final Java8InferenceContext context;

  /** The type to which this variable is instantiated. */
  private ProperType instantiation = null;
  /**
   * Bounds on this variable. Stored as a map from kind of bound (upper, lower, equal) to a set of
   * {@link AbstractType}s.
   */
  public final EnumMap<BoundKind, Set<AbstractType>> bounds = new EnumMap<>(BoundKind.class);
  /** Constraints implied by complementary pairs of bounds found during incorporation. */
  public final ConstraintSet constraints = new ConstraintSet();
  /** Whether or not this variable has a throws bounds. */
  public boolean hasThrowsBound = false;
  /** Saved bounds used in the event the first attempt at resolution fails. */
  public EnumMap<BoundKind, LinkedHashSet<AbstractType>> savedBounds = null;

  public VariableBounds(Java8InferenceContext context) {
    this.context = context;
    bounds.put(BoundKind.EQUAL, new LinkedHashSet<>());
    bounds.put(BoundKind.UPPER, new LinkedHashSet<>());
    bounds.put(BoundKind.LOWER, new LinkedHashSet<>());
  }

  /** Save the current bounds in case the first attempt at resolution fails. */
  public void save() {
    savedBounds = new EnumMap<>(BoundKind.class);
    savedBounds.put(BoundKind.EQUAL, new LinkedHashSet<>(bounds.get(BoundKind.EQUAL)));
    savedBounds.put(BoundKind.UPPER, new LinkedHashSet<>(bounds.get(BoundKind.UPPER)));
    savedBounds.put(BoundKind.LOWER, new LinkedHashSet<>(bounds.get(BoundKind.LOWER)));
  }

  /**
   * Restore the bounds to the state previously saved. This method is called if the first attempt at
   * resolution fails.
   */
  public void restore() {
    assert savedBounds != null;
    instantiation = null;
    bounds.clear();
    bounds.put(BoundKind.EQUAL, new LinkedHashSet<>(savedBounds.get(BoundKind.EQUAL)));
    bounds.put(BoundKind.UPPER, new LinkedHashSet<>(savedBounds.get(BoundKind.UPPER)));
    bounds.put(BoundKind.LOWER, new LinkedHashSet<>(savedBounds.get(BoundKind.LOWER)));
    for (AbstractType t : bounds.get(BoundKind.EQUAL)) {
      if (t.isProper()) {
        instantiation = (ProperType) t;
      }
    }
  }

  /**
   * Return true if this has a throws bound.
   *
   * @return true if this has a throws bound
   */
  public boolean hasThrowsBound() {
    return hasThrowsBound;
  }

  public void setHasThrowsBound(boolean b) {
    hasThrowsBound = b;
  }

  /** Adds {@code otherType} as bound against this variable. */
  public boolean addBound(BoundKind kind, AbstractType otherType) {
    if (kind == BoundKind.EQUAL && otherType.isProper()) {
      instantiation = (ProperType) otherType;
    }
    if (bounds.get(kind).add(otherType)) {
      addConstraintsFromComplementaryBounds(kind, otherType);
      return true;
    }
    return false;
  }

  /** Add constraints created via incorporation of the bound. See JLS 18.3.1. */
  public void addConstraintsFromComplementaryBounds(BoundKind kind, AbstractType s) {
    if (kind == BoundKind.EQUAL) {
      for (AbstractType t : bounds.get(BoundKind.EQUAL)) {
        if (s != t) {
          constraints.add(new Typing(s, t, Kind.TYPE_EQUALITY));
        }
      }
    } else if (kind == BoundKind.LOWER) {
      for (AbstractType t : bounds.get(BoundKind.EQUAL)) {
        if (s != t) {
          constraints.add(new Typing(s, t, Kind.SUBTYPE));
        }
      }
    } else { // UPPER
      for (AbstractType t : bounds.get(BoundKind.EQUAL)) {
        if (s != t) {
          constraints.add(new Typing(t, s, Kind.SUBTYPE));
        }
      }
    }

    if (kind == BoundKind.EQUAL || kind == BoundKind.UPPER) {
      for (AbstractType t : bounds.get(BoundKind.LOWER)) {
        if (s != t) {
          constraints.add(new Typing(t, s, Kind.SUBTYPE));
        }
      }
    }

    if (kind == BoundKind.EQUAL || kind == BoundKind.LOWER) {
      for (AbstractType t : bounds.get(BoundKind.UPPER)) {
        if (s != t) {
          constraints.add(new Typing(s, t, Kind.SUBTYPE));
        }
      }
    }

    if (kind == BoundKind.UPPER) {
      // When a bound set contains a pair of bounds var <: S and var <: T, and there exists
      // a supertype of S of the form G<S1, ..., Sn> and
      // a supertype of T of the form G<T1,..., Tn> (for some generic class or interface, G),
      // then for all i (1 <= i <= n), if Si and Ti are types (not wildcards),
      // the constraint formula <Si = Ti> is implied.
      if (s.isInferenceType() || s.isProper()) {
        for (AbstractType t : bounds.get(BoundKind.LOWER)) {
          if (t.isProper() || t.isInferenceType()) {
            constraints.addAll(getConstraintsFromParameterized(s, t));
          }
        }
      }
    }
  }

  /**
   * Returns the constraints between the type arguments to {@code s} and {@code t}.
   *
   * <p>If the there exists a supertype of S of the form {@code G<S1, ..., Sn>} and a supertype of T
   * of the form {@code G<T1,..., Tn>} (for some generic class or interface, G), then for all i
   * ({@code 1 <= i <= n}), if Si and Ti are types (not wildcards), the constraint formula {@code
   * <Si = Ti>} is implied.
   *
   * @param s a type argument
   * @param t a type argument
   * @return the constraints between the type arguments to {@code s} and {@code t}.
   */
  private List<Typing> getConstraintsFromParameterized(AbstractType s, AbstractType t) {
    Pair<AbstractType, AbstractType> pair =
        context.inferenceTypeFactory.getParameterizedSupers(s, t);

    if (pair == null) {
      return new ArrayList<>();
    }

    List<AbstractType> ss = pair.first.getTypeArguments();
    List<AbstractType> ts = pair.second.getTypeArguments();
    assert ss.size() == ts.size();

    List<Typing> constraints = new ArrayList<>();
    for (int i = 0; i < ss.size(); i++) {
      AbstractType si = ss.get(i);
      AbstractType ti = ts.get(i);
      if (si.getTypeKind() != TypeKind.WILDCARD && ti.getTypeKind() != TypeKind.WILDCARD) {
        constraints.add(new Typing(si, ti, Kind.TYPE_EQUALITY));
      }
    }
    return constraints;
  }

  /**
   * Return all lower bounds that are proper types.
   *
   * @return all lower bounds that are proper types
   */
  public LinkedHashSet<ProperType> findProperLowerBounds() {
    LinkedHashSet<ProperType> set = new LinkedHashSet<>();
    for (AbstractType bound : bounds.get(BoundKind.LOWER)) {
      if (bound.isProper()) {
        set.add((ProperType) bound);
      }
    }
    return set;
  }

  /**
   * Returns all upper bounds that proper types.
   *
   * @return all upper bounds that are proper types
   */
  public LinkedHashSet<ProperType> findProperUpperBounds() {
    LinkedHashSet<ProperType> set = new LinkedHashSet<>();
    for (AbstractType bound : bounds.get(BoundKind.UPPER)) {
      if (bound.isProper()) {
        set.add((ProperType) bound);
      }
    }
    return set;
  }

  /**
   * Returns all upper bounds.
   *
   * @return all upper bounds
   */
  public LinkedHashSet<AbstractType> upperBounds() {
    LinkedHashSet<AbstractType> set = new LinkedHashSet<>();
    for (AbstractType bound : bounds.get(BoundKind.UPPER)) {
      if (!bound.isVariable()) {
        set.add(bound);
      }
    }
    return set;
  }

  /** Apply instantiations to all bounds and constraints of this variable. */
  public boolean applyInstantiationsToBounds(List<Variable> instantiations) {
    boolean changed = false;
    for (Set<AbstractType> boundList : bounds.values()) {
      LinkedHashSet<AbstractType> newBounds = new LinkedHashSet<>(boundList.size());
      for (AbstractType bound : boundList) {
        AbstractType newBound = bound.applyInstantiations(instantiations);
        if (newBound != bound && !boundList.contains(newBound)) {
          changed = true;
        }
        newBounds.add(newBound);
      }
      boundList.clear();
      boundList.addAll(newBounds);
    }
    constraints.applyInstantiations(instantiations);

    if (changed && instantiation == null) {
      for (AbstractType bound : bounds.get(BoundKind.EQUAL)) {
        if (bound.isProper()) {
          instantiation = (ProperType) bound;
        }
      }
    }
    return changed;
  }

  /**
   * Return all variables mentioned in a bound against this variable.
   *
   * @return all variables mentioned in a bound against this variable
   */
  public Collection<? extends Variable> getVariablesMentionedInBounds() {
    List<Variable> mentioned = new ArrayList<>();
    for (Set<AbstractType> boundList : bounds.values()) {
      for (AbstractType bound : boundList) {
        mentioned.addAll(bound.getInferenceVariables());
      }
    }
    return mentioned;
  }

  /**
   * Returns the instantiation of this variable.
   *
   * @return the instantiation of this variable
   */
  public ProperType getInstantiation() {
    return instantiation;
  }

  /**
   * Return true if this has an instantiation.
   *
   * @return true if this has an instantiation
   */
  public boolean hasInstantiation() {
    return instantiation != null;
  }

  /**
   * Returns true if any bound mentions a primitive wrapper type.
   *
   * @return true if any bound mentions a primitive wrapper type
   */
  public boolean hasPrimitiveWrapperBound() {
    for (Set<AbstractType> boundList : bounds.values()) {
      for (AbstractType bound : boundList) {
        if (bound.isProper() && TypesUtils.isBoxedPrimitive(bound.getJavaType())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if any lower or equal bound is a parameterized type with at least one wildcard as
   * a type argument.
   *
   * @return true if any lower or equal bound is a parameterized type with at least one wildcard for
   *     a type argument
   */
  public boolean hasWildcardParameterizedLowerOrEqualBound() {
    for (AbstractType type : bounds.get(BoundKind.EQUAL)) {
      if (!type.isVariable() && type.isWildcardParameterizedType()) {
        return true;
      }
    }
    for (AbstractType type : bounds.get(BoundKind.LOWER)) {
      if (!type.isVariable() && type.isWildcardParameterizedType()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Does this bound set contain two bounds of the forms {@code S1 <: var} and {@code S2 <: var},
   * where S1 and S2 have supertypes that are two different parameterizations of the same generic
   * class or interface?
   */
  public boolean hasLowerBoundDifferentParam() {
    List<AbstractType> parameteredTypes = new ArrayList<>();
    for (AbstractType type : bounds.get(BoundKind.LOWER)) {
      if (!type.isVariable() && type.isParameterizedType()) {
        parameteredTypes.add(type);
      }
    }
    for (int i = 0; i < parameteredTypes.size(); i++) {
      AbstractType s1 = parameteredTypes.get(i);
      for (int j = i + 1; j < parameteredTypes.size(); j++) {
        AbstractType s2 = parameteredTypes.get(j);
        Pair<AbstractType, AbstractType> supers =
            context.inferenceTypeFactory.getParameterizedSupers(s1, s2);
        if (supers == null) {
          continue;
        }
        List<AbstractType> s1TypeArgs = supers.first.getTypeArguments();
        List<AbstractType> s2TypeArgs = supers.second.getTypeArguments();
        if (!s1TypeArgs.equals(s2TypeArgs)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Does this bound set contain a bound of one of the forms {@code var = S} or {@code S <: var},
   * where there exists no type of the form {@code G<...>} that is a supertype of S, but the raw
   * type {@code |G<...>|} is a supertype of S?
   */
  public boolean hasRawTypeLowerOrEqualBound(AbstractType g) {
    for (AbstractType type : bounds.get(BoundKind.LOWER)) {
      if (type.isVariable()) {
        continue;
      }
      AbstractType superTypeOfS = type.asSuper(g.getJavaType());
      if (superTypeOfS != null && superTypeOfS.isRaw()) {
        return true;
      }
    }

    for (AbstractType type : bounds.get(BoundKind.EQUAL)) {
      if (type.isVariable()) {
        continue;
      }
      AbstractType superTypeOfS = type.asSuper(g.getJavaType());
      if (superTypeOfS != null && superTypeOfS.isRaw()) {
        return true;
      }
    }
    return false;
  }

  /** These are constraints generated when incorporating a capture bound. See JLS 18.3.2. */
  public ConstraintSet getWildcardConstraints(AbstractType Ai, AbstractType Bi) {
    ConstraintSet constraintSet = new ConstraintSet();

    // Only concerned with bounds against proper types or inference types.
    List<AbstractType> upperBoundsNonVar = new ArrayList<>();
    for (AbstractType bound : bounds.get(VariableBounds.BoundKind.UPPER)) {
      if (bound.isProper() || bound.isInferenceType()) {
        upperBoundsNonVar.add(bound);
      }
    }
    List<AbstractType> lowerBoundsNonVar = new ArrayList<>();
    for (AbstractType bound : bounds.get(VariableBounds.BoundKind.LOWER)) {
      if (bound.isProper() || bound.isInferenceType()) {
        lowerBoundsNonVar.add(bound);
      }
    }

    for (AbstractType bound : bounds.get(VariableBounds.BoundKind.EQUAL)) {
      if (bound.isProper() || bound.isInferenceType()) {
        // var = R implies the bound false
        return null;
      }
    }

    if (Ai.isUnboundWildcard()) {
      // R <: var implies the bound false
      if (!lowerBoundsNonVar.isEmpty()) {
        return null;
      }
      // var <: R implies the constraint formula <Bi theta <: R>
    } else if (Ai.isUpperBoundedWildcard()) {
      // R <: var implies the bound false
      if (!lowerBoundsNonVar.isEmpty()) {
        return null;
      }
      AbstractType T = Ai.getWildcardUpperBound();
      if (Bi.isObject()) {
        // If Bi is Object, then var <: R implies the constraint formula <T <: R>
        for (AbstractType r : upperBoundsNonVar) {
          constraintSet.add(new Typing(T, r, Constraint.Kind.SUBTYPE));
        }
      } else if (T.isObject()) {
        // If T is Object, then var <: R implies the constraint formula <Bi theta <: R>
        for (AbstractType r : upperBoundsNonVar) {
          constraintSet.add(new Typing(Bi, r, Constraint.Kind.SUBTYPE));
        }
      }
      // else no constraint
    } else {
      // Super bounded wildcard
      // var <: R implies the constraint formula <Bi theta <: R>
      for (AbstractType r : upperBoundsNonVar) {
        constraintSet.add(new Typing(Bi, r, Constraint.Kind.SUBTYPE));
      }

      // R <: var implies the constraint formula <R <: T>
      AbstractType T = Ai.getWildcardLowerBound();
      for (AbstractType r : lowerBoundsNonVar) {
        constraintSet.add(new Typing(r, T, Constraint.Kind.SUBTYPE));
      }
    }
    return constraintSet;
  }
}
