package org.checkerframework.framework.util.typeinference8.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.QualifierTyping;
import org.checkerframework.framework.util.typeinference8.constraint.TypeConstraint;
import org.checkerframework.framework.util.typeinference8.constraint.Typing;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.IPair;

/** Data structure to stores the bounds of a variable. */
public class VariableBounds {

  /** Kind of bound. */
  public enum BoundKind {
    /** {@code other type <: this } */
    LOWER,
    /** {@code this <: other type } */
    UPPER,
    /** {@code this = other type } */
    EQUAL;
  }

  /** The variable whose bounds this class represents. */
  private final Variable variable;

  /** The context. */
  private final Java8InferenceContext context;

  /** The type to which this variable is instantiated. */
  private ProperType instantiation = null;

  /**
   * Bounds on this variable. Stored as a map from kind of bound (upper, lower, equal) to a set of
   * {@link AbstractType}s.
   */
  public final EnumMap<BoundKind, Set<AbstractType>> bounds = new EnumMap<>(BoundKind.class);

  /**
   * Qualifier bounds on this variable. Stored as a map from kind of bound (upper, lower, equal) to
   * a set of {@link AnnotationMirror}s. A qualifier bound is a bound on the primary annotation of
   * this variable.
   */
  public final EnumMap<BoundKind, Set<AbstractQualifier>> qualifierBounds =
      new EnumMap<>(BoundKind.class);

  /** Constraints implied by complementary pairs of bounds found during incorporation. */
  public final ConstraintSet constraints = new ConstraintSet();

  /** Whether this variable has a throws bounds. */
  public boolean hasThrowsBound = false;

  /** Saved bounds used in the event the first attempt at resolution fails. */
  public EnumMap<BoundKind, LinkedHashSet<AbstractType>> savedBounds = null;

  /** Saved qualifier bounds used in the event the first attempt at resolution fails. */
  public EnumMap<BoundKind, LinkedHashSet<AbstractQualifier>> savedQualifierBounds = null;

  /**
   * Creates bounds for {@code variable}.
   *
   * @param variable a variable
   * @param context the context
   */
  public VariableBounds(Variable variable, Java8InferenceContext context) {
    this.variable = variable;
    this.context = context;
    bounds.put(BoundKind.EQUAL, new LinkedHashSet<>());
    bounds.put(BoundKind.UPPER, new LinkedHashSet<>());
    bounds.put(BoundKind.LOWER, new LinkedHashSet<>());

    qualifierBounds.put(BoundKind.EQUAL, new LinkedHashSet<>());
    qualifierBounds.put(BoundKind.UPPER, new LinkedHashSet<>());
    qualifierBounds.put(BoundKind.LOWER, new LinkedHashSet<>());
  }

  /** Save the current bounds in case the first attempt at resolution fails. */
  public void save() {
    savedBounds = new EnumMap<>(BoundKind.class);
    savedBounds.put(BoundKind.EQUAL, new LinkedHashSet<>(bounds.get(BoundKind.EQUAL)));
    savedBounds.put(BoundKind.UPPER, new LinkedHashSet<>(bounds.get(BoundKind.UPPER)));
    savedBounds.put(BoundKind.LOWER, new LinkedHashSet<>(bounds.get(BoundKind.LOWER)));

    savedQualifierBounds = new EnumMap<>(BoundKind.class);
    savedQualifierBounds.put(
        BoundKind.EQUAL, new LinkedHashSet<>(qualifierBounds.get(BoundKind.EQUAL)));
    savedQualifierBounds.put(
        BoundKind.UPPER, new LinkedHashSet<>(qualifierBounds.get(BoundKind.UPPER)));
    savedQualifierBounds.put(
        BoundKind.LOWER, new LinkedHashSet<>(qualifierBounds.get(BoundKind.LOWER)));
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
    qualifierBounds.clear();
    qualifierBounds.put(
        BoundKind.EQUAL, new LinkedHashSet<>(savedQualifierBounds.get(BoundKind.EQUAL)));
    qualifierBounds.put(
        BoundKind.UPPER, new LinkedHashSet<>(savedQualifierBounds.get(BoundKind.UPPER)));
    qualifierBounds.put(
        BoundKind.LOWER, new LinkedHashSet<>(savedQualifierBounds.get(BoundKind.LOWER)));
  }

  /**
   * Return true if this has a throws bound.
   *
   * @return true if this has a throws bound
   */
  public boolean hasThrowsBound() {
    return hasThrowsBound;
  }

  /**
   * Set has throws bound
   *
   * @param b has thrown bound
   */
  public void setHasThrowsBound(boolean b) {
    hasThrowsBound = b;
  }

  /**
   * Adds {@code otherType} as bound against this variable.
   *
   * @param parent the constraint whose reduction created this bound
   * @param kind the kind of bound
   * @param otherType the bound type
   * @return if a new bound was added
   */
  public boolean addBound(Constraint parent, BoundKind kind, AbstractType otherType) {
    if (otherType.isUseOfVariable() && ((UseOfVariable) otherType).getVariable() == variable) {
      return false;
    }
    if (kind == BoundKind.EQUAL && otherType.isProper()) {
      instantiation = ((ProperType) otherType).boxType();
    }
    if (bounds.get(kind).add(otherType)) {
      addConstraintsFromComplementaryBounds(parent, kind, otherType);
      Set<AbstractQualifier> aQuals = otherType.getQualifiers();
      addConstraintsFromComplementaryQualifierBounds(kind, aQuals);
      return true;
    }
    return false;
  }

  /**
   * Adds {@code qualifiers} as a qualifier bound against this variable.
   *
   * @param kind the kind of bound
   * @param qualifiers the qualifiers
   */
  public void addQualifierBound(BoundKind kind, Set<? extends AbstractQualifier> qualifiers) {
    addConstraintsFromComplementaryQualifierBounds(kind, qualifiers);
    addConstraintsFromComplementaryBounds(kind, qualifiers);
    qualifierBounds.get(kind).addAll(qualifiers);
  }

  /**
   * Add constraints created via incorporation of the bound. See JLS 18.3.1.
   *
   * @param kind the kind of bound
   * @param qualifiers the qualifiers
   */
  public void addConstraintsFromComplementaryQualifierBounds(
      BoundKind kind, Set<? extends AbstractQualifier> qualifiers) {
    Set<AbstractQualifier> equalBounds = qualifierBounds.get(BoundKind.EQUAL);
    switch (kind) {
      case EQUAL:
        addQualifierConstraint(qualifiers, equalBounds, Kind.QUALIFIER_EQUALITY);
        addQualifierConstraint(
            qualifierBounds.get(BoundKind.LOWER), qualifiers, Kind.QUALIFIER_SUBTYPE);
        addQualifierConstraint(
            qualifiers, qualifierBounds.get(BoundKind.UPPER), Kind.QUALIFIER_SUBTYPE);
        break;
      case LOWER:
        addQualifierConstraint(qualifiers, equalBounds, Kind.QUALIFIER_SUBTYPE);
        addQualifierConstraint(
            qualifiers, qualifierBounds.get(BoundKind.UPPER), Kind.QUALIFIER_SUBTYPE);
        break;
      default: // UPPER
        addQualifierConstraint(equalBounds, qualifiers, Kind.QUALIFIER_SUBTYPE);
        addQualifierConstraint(
            qualifierBounds.get(BoundKind.LOWER), qualifiers, Kind.QUALIFIER_SUBTYPE);
        break;
    }
  }

  /**
   * Add a {@link QualifierTyping} constraint for a qualifier in {@code setT} and the qualifier in
   * {@code setS} in the same hierarchy.
   *
   * @param setT a set of abstract qualifiers on the left side of the constraint
   * @param setS a set of abstract qualifiers on the right side of the constraint
   * @param kind the kind of constraint
   */
  @SuppressWarnings("interning:not.interned") // Checking for exact object.
  private void addQualifierConstraint(
      Set<? extends AbstractQualifier> setT, Set<? extends AbstractQualifier> setS, Kind kind) {
    for (AbstractQualifier t : setT) {
      for (AbstractQualifier s : setS) {
        if (s != t && s.sameHierarchy(t)) {
          constraints.add(new QualifierTyping(t, s, kind));
        }
      }
    }
  }

  /**
   * Add constraints created via incorporation of the bound. See JLS 18.3.1.
   *
   * @param parent the constraint whose reduction created this bound
   * @param kind the kind of bound
   * @param boundType the type of the bound
   */
  @SuppressWarnings("interning:not.interned") // Checking for exact object.
  public void addConstraintsFromComplementaryBounds(
      Constraint parent, BoundKind kind, AbstractType boundType) {
    if (parent instanceof TypeConstraint) {
      ((TypeConstraint) parent).source = "From complementary bound.";
    }
    switch (kind) {
      case EQUAL:
        for (AbstractType t : bounds.get(BoundKind.EQUAL)) {
          if (boundType != t) {
            constraints.add(new Typing(parent, boundType, t, Kind.TYPE_EQUALITY));
          }
        }
        for (AbstractType t : bounds.get(BoundKind.LOWER)) {
          if (boundType != t) {
            constraints.add(new Typing(parent, t, boundType, Kind.SUBTYPE));
          }
        }
        for (AbstractType t : bounds.get(BoundKind.UPPER)) {
          if (boundType != t) {
            constraints.add(new Typing(parent, boundType, t, Kind.SUBTYPE));
          }
        }
        break;
      case LOWER:
        for (AbstractType t : bounds.get(BoundKind.EQUAL)) {
          if (boundType != t) {
            constraints.add(new Typing(parent, boundType, t, Kind.SUBTYPE));
          }
        }
        for (AbstractType t : bounds.get(BoundKind.UPPER)) {
          if (boundType != t) {
            constraints.add(new Typing(parent, boundType, t, Kind.SUBTYPE));
          }
        }
        break;
      case UPPER:
        for (AbstractType t : bounds.get(BoundKind.EQUAL)) {
          if (boundType != t) {
            constraints.add(new Typing(parent, t, boundType, Kind.SUBTYPE));
          }
        }
        for (AbstractType t : bounds.get(BoundKind.LOWER)) {
          if (boundType != t) {
            constraints.add(new Typing(parent, t, boundType, Kind.SUBTYPE));
          }
        }
        break;
    }

    if (kind == BoundKind.UPPER) {
      // When a bound set contains a pair of bounds var <: S and var <: T, and there exists
      // a supertype of S of the form G<S1, ..., Sn> and
      // a supertype of T of the form G<T1,..., Tn> (for some generic class or interface, G),
      // then for all i (1 <= i <= n), if Si and Ti are types (not wildcards),
      // the constraint formula <Si = Ti> is implied.
      if (boundType.isInferenceType() || boundType.isProper()) {
        for (AbstractType t : bounds.get(BoundKind.LOWER)) {
          if (t.isProper() || t.isInferenceType()) {
            constraints.addAll(getConstraintsFromParameterized(boundType, t));
          }
        }
      }
    }
    if (boundType.isUseOfVariable()) {
      UseOfVariable boundVar = (UseOfVariable) boundType;
      switch (kind) {
        case EQUAL:
          boundVar.addQualifierBound(BoundKind.EQUAL, qualifierBounds.get(BoundKind.EQUAL));
          boundVar.addQualifierBound(BoundKind.LOWER, qualifierBounds.get(BoundKind.LOWER));
          boundVar.addQualifierBound(BoundKind.UPPER, qualifierBounds.get(BoundKind.UPPER));
          break;
        case LOWER:
          boundVar.addQualifierBound(BoundKind.UPPER, qualifierBounds.get(BoundKind.EQUAL));
          boundVar.addQualifierBound(BoundKind.LOWER, qualifierBounds.get(BoundKind.LOWER));
          break;
        case UPPER:
          boundVar.addQualifierBound(BoundKind.LOWER, qualifierBounds.get(BoundKind.EQUAL));
          boundVar.addQualifierBound(BoundKind.UPPER, qualifierBounds.get(BoundKind.UPPER));
          break;
      }
    }
  }

  /**
   * Adds constraints from complementary bounds.
   *
   * @param kind kind of bound
   * @param s qualifiers
   */
  public void addConstraintsFromComplementaryBounds(
      BoundKind kind, Set<? extends AbstractQualifier> s) {
    // Copy bound to equal variables
    for (AbstractType t : bounds.get(BoundKind.EQUAL)) {
      if (t.isUseOfVariable()) {
        VariableBounds otherBounds = ((UseOfVariable) t).getVariable().getBounds();
        otherBounds.qualifierBounds.get(kind).addAll(s);
      }
    }

    if (kind == BoundKind.EQUAL || kind == BoundKind.UPPER) {
      for (AbstractType t : bounds.get(BoundKind.LOWER)) {
        if (t.isUseOfVariable()) {
          VariableBounds otherBounds = ((UseOfVariable) t).getVariable().getBounds();
          otherBounds.qualifierBounds.get(BoundKind.UPPER).addAll(s);
        }
      }
    }

    if (kind == BoundKind.EQUAL || kind == BoundKind.LOWER) {
      for (AbstractType t : bounds.get(BoundKind.UPPER)) {
        if (t.isUseOfVariable()) {
          VariableBounds otherBounds = ((UseOfVariable) t).getVariable().getBounds();
          otherBounds.qualifierBounds.get(BoundKind.LOWER).addAll(s);
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
   * @return the constraints between the type arguments to {@code s} and {@code t}
   */
  private List<Typing> getConstraintsFromParameterized(AbstractType s, AbstractType t) {
    String source = "Constraint from parameterized bound.";

    IPair<AbstractType, AbstractType> pair =
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
        constraints.add(new Typing(source, si, ti, Kind.TYPE_EQUALITY));
      }
    }
    return constraints;
  }

  /**
   * Returns whether this variable only has bounds against proper types.
   *
   * @return whether this variable only has bounds against proper types.
   */
  public boolean onlyProperBounds() {
    for (BoundKind k : BoundKind.values()) {
      for (AbstractType bound : bounds.get(k)) {
        if (!bound.isProper()) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Return all lower bounds that are proper types.
   *
   * @return all lower bounds that are proper types
   */
  public Set<ProperType> findProperLowerBounds() {
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
  public Set<ProperType> findProperUpperBounds() {
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
  public Set<AbstractType> upperBounds() {
    LinkedHashSet<AbstractType> set = new LinkedHashSet<>();
    for (AbstractType bound : bounds.get(BoundKind.UPPER)) {
      if (!bound.isUseOfVariable()) {
        set.add(bound);
      }
    }
    return set;
  }

  /**
   * Apply instantiations to all bounds and constraints of this variable.
   *
   * @return whether any of the bounds changed
   */
  @SuppressWarnings("interning:not.interned") // Checking for exact object.
  public boolean applyInstantiationsToBounds() {
    boolean changed = false;
    for (Set<AbstractType> boundList : bounds.values()) {
      LinkedHashSet<AbstractType> newBounds = new LinkedHashSet<>(boundList.size());
      for (AbstractType bound : boundList) {
        AbstractType newBound = bound.applyInstantiations();
        if (newBound != bound && !boundList.contains(newBound)) {
          changed = true;
        }
        newBounds.add(newBound);
      }
      boundList.clear();
      boundList.addAll(newBounds);
    }
    constraints.applyInstantiations();

    if (changed && instantiation == null) {
      for (AbstractType bound : bounds.get(BoundKind.EQUAL)) {
        if (bound.isProper()) {
          instantiation = ((ProperType) bound).boxType();
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
      if (!type.isUseOfVariable() && type.isWildcardParameterizedType()) {
        return true;
      }
    }
    for (AbstractType type : bounds.get(BoundKind.LOWER)) {
      if (!type.isUseOfVariable() && type.isWildcardParameterizedType()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Does this bound set contain two bounds of the forms {@code S1 <: var} and {@code S2 <: var},
   * where S1 and S2 have supertypes that are two different parameterizations of the same generic
   * class or interface?
   *
   * @return whether this bound set contain two bounds of the forms {@code S1 <: var} and {@code S2
   *     <: var}, where S1 and S2 have supertypes that are two different parameterizations of the
   *     same generic class or interface
   */
  public boolean hasLowerBoundDifferentParam() {
    List<AbstractType> parameteredTypes = new ArrayList<>();
    for (AbstractType type : bounds.get(BoundKind.LOWER)) {
      if (type.isProper() && type.isParameterizedType()) {
        parameteredTypes.add(type);
      }
    }
    for (int i = 0; i < parameteredTypes.size(); i++) {
      AbstractType s1 = parameteredTypes.get(i);
      for (int j = i + 1; j < parameteredTypes.size(); j++) {
        AbstractType s2 = parameteredTypes.get(j);
        IPair<AbstractType, AbstractType> supers =
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
   * Returns true if there exists an equal or lower bound against a type, S, such that S is not a
   * subtype of {@code G<...>}, but S is a subtype of the raw type {@code |G<...>|}, where {@code G}
   * a generic class or interface for which the parameter of this method, {@code t}, is a
   * parameterization.
   *
   * @param t a parameterization of a generic class or interface, {@code G}
   * @return true if there exists an equal or lower bound against a type, S, such that S is not a
   *     subtype of {@code G<...>}, but S is a subtype of the raw type {@code |G<...>|}, where
   *     {@code G} a generic class or interface for which the parameter of this method, {@code t},
   *     is a parameterization.
   */
  public boolean hasRawTypeLowerOrEqualBound(AbstractType t) {
    for (AbstractType type : bounds.get(BoundKind.LOWER)) {
      if (type.isUseOfVariable()) {
        continue;
      }
      AbstractType superTypeOfS = type.asSuper(t.getJavaType());
      if (superTypeOfS != null && superTypeOfS.isRaw()) {
        return true;
      }
    }

    for (AbstractType type : bounds.get(BoundKind.EQUAL)) {
      if (type.isUseOfVariable()) {
        continue;
      }
      AbstractType superTypeOfS = type.asSuper(t.getJavaType());
      if (superTypeOfS != null && superTypeOfS.isRaw()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the constraints generated when incorporating a capture bound. See JLS 18.3.2.
   *
   * @param Ai the captured type argument
   * @param Bi the bound of the type variable
   * @return constraints generated when incorporating a capture bound
   */
  public ConstraintSet getWildcardConstraints(AbstractType Ai, AbstractType Bi) {
    ConstraintSet constraintSet = new ConstraintSet();
    String source = "Constraint from wildcard bound.";

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
          constraintSet.add(new Typing(source, T, r, TypeConstraint.Kind.SUBTYPE));
        }
      } else if (T.isObject()) {
        // If T is Object, then var <: R implies the constraint formula <Bi theta <: R>
        for (AbstractType r : upperBoundsNonVar) {
          constraintSet.add(new Typing(source, Bi, r, TypeConstraint.Kind.SUBTYPE));
        }
      }
      // else no constraint
    } else {
      // Super bounded wildcard
      // var <: R implies the constraint formula <Bi theta <: R>
      for (AbstractType r : upperBoundsNonVar) {
        constraintSet.add(new Typing(source, Bi, r, TypeConstraint.Kind.SUBTYPE));
      }

      // R <: var implies the constraint formula <R <: T>
      AbstractType T = Ai.getWildcardLowerBound();
      for (AbstractType r : lowerBoundsNonVar) {
        constraintSet.add(new Typing(source, r, T, TypeConstraint.Kind.SUBTYPE));
      }
    }
    return constraintSet;
  }
}
