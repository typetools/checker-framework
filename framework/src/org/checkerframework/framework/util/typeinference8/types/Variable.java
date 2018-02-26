package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.Typing;
import org.checkerframework.framework.util.typeinference8.util.InternalInferenceUtils;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TypesUtils;

/** An inference variable */
public class Variable extends AbstractType {

    /** Identification number. Used only to make debugging easier. */
    protected final int id;

    /** Type variable for which the instantiation of this variable is a type argument, */
    protected final TypeVariable typeVariable;

    /**
     * The expression for which this variable is being solved. Used to differentiate inference
     * variables for two different invocations to the same method.
     */
    protected final ExpressionTree invocation;

    protected ProperType instantiation = null;

    /**
     * Bounds on this variable. Stored as a map from kind of bound (upper, lower, equal) to a set of
     * {@link AbstractType}s.
     */
    protected final EnumMap<BoundKind, Set<AbstractType>> bounds = new EnumMap<>(BoundKind.class);

    /** Constraints implied by complementary pairs of bounds found during incorporation. */
    public final ConstraintSet constraints = new ConstraintSet();

    /** Whether or not this variable has a throws bounds. */
    private boolean hasThrowsBound = false;

    /** Saved bounds. */
    private EnumMap<BoundKind, LinkedHashSet<AbstractType>> savedBounds = null;

    public Variable(
            TypeVariable typeVariable, ExpressionTree invocation, Java8InferenceContext context) {
        this(typeVariable, invocation, context, context.getNextVariableId());
    }

    protected Variable(
            TypeVariable typeVariable,
            ExpressionTree invocation,
            Java8InferenceContext context,
            int id) {
        super(context);
        assert typeVariable != null;
        this.typeVariable = typeVariable;
        this.invocation = invocation;
        this.id = id;
        bounds.put(BoundKind.EQUAL, new LinkedHashSet<>());
        bounds.put(BoundKind.UPPER, new LinkedHashSet<>());
        bounds.put(BoundKind.LOWER, new LinkedHashSet<>());
    }

    @Override
    public AbstractType create(TypeMirror type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isObject() {
        return false;
    }

    @Override
    public List<ProperType> getTypeParameterBounds() {
        return null;
    }

    @Override
    public AbstractType capture() {
        return this;
    }

    @Override
    public AbstractType getErased() {
        return this;
    }

    @Override
    public TypeVariable getJavaType() {
        return typeVariable;
    }

    public ExpressionTree getInvocation() {
        return invocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Variable variable = (Variable) o;
        return context.factory
                        .getContext()
                        .getTypeUtils()
                        .isSameType(typeVariable, variable.typeVariable)
                && invocation == variable.invocation;
    }

    @Override
    public int hashCode() {
        int result = typeVariable.toString().hashCode();
        result = 31 * result + Kind.VARIABLE.hashCode();
        result = 31 * result + invocation.hashCode();
        return result;
    }

    @Override
    public Kind getKind() {
        return Kind.VARIABLE;
    }

    @Override
    public Collection<Variable> getInferenceVariables() {
        return Collections.singleton(this);
    }

    @Override
    public AbstractType applyInstantiations(List<Variable> instantiations) {
        for (Variable inst : instantiations) {
            if (inst.equals(this)) {
                return inst.instantiation;
            }
        }
        return this;
    }

    @Override
    public String toString() {
        if (hasInstantiation()) {
            return "a" + id + " := " + getInstantiation();
        }
        return "a" + id;
    }

    /** @return true if this is a variable created for a capture bound. */
    public boolean isCaptureVariable() {
        return this instanceof CaptureVariable;
    }

    // <editor-fold defaultstate="collapsed" desc="Bound opps">
    public enum BoundKind {
        /** {@code other type <: this } */
        LOWER,
        /** {@code this <: other type } */
        UPPER,
        /** {@code this = other type } */
        EQUAL;
    }

    /** Save the current bounds. */
    public void save() {
        savedBounds = new EnumMap<>(BoundKind.class);
        savedBounds.put(BoundKind.EQUAL, new LinkedHashSet<>(bounds.get(BoundKind.EQUAL)));
        savedBounds.put(BoundKind.UPPER, new LinkedHashSet<>(bounds.get(BoundKind.UPPER)));
        savedBounds.put(BoundKind.LOWER, new LinkedHashSet<>(bounds.get(BoundKind.LOWER)));
    }

    /** Restore the bounds to the state previously saved. */
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
     * Adds the initial bounds to this variable. These are the bounds implied by the upper bounds of
     * the type variable. See end of JLS 18.1.3.
     *
     * @param map used to determine if the bounds refer to another variable
     */
    public void initialBounds(Theta map) {
        TypeMirror upperBound = typeVariable.getUpperBound();
        // If Pl has no TypeBound, the bound {@literal al <: Object} appears in the set. Otherwise, for
        // each type T delimited by & in the TypeBound, the bound {@literal al <: T[P1:=a1,..., Pp:=ap]}
        // appears in the set; if this results in no proper upper bounds for al (only dependencies),
        // then the bound {@literal al <: Object} also appears in the set.
        switch (upperBound.getKind()) {
            case INTERSECTION:
                for (TypeMirror bound : ((IntersectionType) upperBound).getBounds()) {
                    AbstractType t1 = InferenceType.create(bound, map, context);
                    addBound(BoundKind.UPPER, t1);
                }
                break;
            default:
                AbstractType t1 = InferenceType.create(upperBound, map, context);
                addBound(BoundKind.UPPER, t1);
                break;
        }
    }

    /** @return true if this has a throws bound */
    public boolean hasThrowsBound() {
        return hasThrowsBound;
    }

    /** Sets the value of hasThrowsBound to {@code hasThrowsBound} */
    public void setHasThrowsBound(boolean hasThrowsBound) {
        this.hasThrowsBound = hasThrowsBound;
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
    private void addConstraintsFromComplementaryBounds(BoundKind kind, AbstractType s) {
        if (kind == BoundKind.EQUAL) {
            for (AbstractType t : bounds.get(BoundKind.EQUAL)) {
                if (s != t) {
                    constraints.add(new Typing(s, t, Typing.Kind.TYPE_EQUALITY));
                }
            }
        } else if (kind == BoundKind.LOWER) {
            for (AbstractType t : bounds.get(BoundKind.EQUAL)) {
                if (s != t) {
                    constraints.add(new Typing(s, t, Typing.Kind.SUBTYPE));
                }
            }
        } else { // UPPER
            for (AbstractType t : bounds.get(BoundKind.EQUAL)) {
                if (s != t) {
                    constraints.add(new Typing(t, s, Typing.Kind.SUBTYPE));
                }
            }
        }

        if (kind == BoundKind.EQUAL || kind == BoundKind.UPPER) {
            for (AbstractType t : bounds.get(BoundKind.LOWER)) {
                if (s != t) {
                    constraints.add(new Typing(t, s, Typing.Kind.SUBTYPE));
                }
            }
        }

        if (kind == BoundKind.EQUAL || kind == BoundKind.LOWER) {
            for (AbstractType t : bounds.get(BoundKind.UPPER)) {
                if (s != t) {
                    constraints.add(new Typing(s, t, Typing.Kind.SUBTYPE));
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

    private List<Typing> getConstraintsFromParameterized(AbstractType s, AbstractType t) {
        Pair<TypeMirror, TypeMirror> pair =
                InternalInferenceUtils.getParameterizedSupers(
                        s.getJavaType(), t.getJavaType(), context);
        if (pair == null) {
            return new ArrayList<>();
        }
        List<Typing> constraints = new ArrayList<>();

        List<AbstractType> ss = s.asSuper(pair.first).getTypeArguments();
        List<AbstractType> ts = t.asSuper(pair.second).getTypeArguments();
        assert ss.size() == ts.size();

        for (int i = 0; i < ss.size(); i++) {
            AbstractType si = ss.get(i);
            AbstractType ti = ts.get(i);
            if (si.getTypeKind() != TypeKind.WILDCARD && ti.getTypeKind() != TypeKind.WILDCARD) {
                constraints.add(new Typing(si, ti, Constraint.Kind.TYPE_EQUALITY));
            }
        }
        return constraints;
    }

    public LinkedHashSet<ProperType> findProperLowerBounds() {
        LinkedHashSet<ProperType> set = new LinkedHashSet<>();
        for (AbstractType bound : bounds.get(BoundKind.LOWER)) {
            if (bound.isProper()) {
                set.add((ProperType) bound);
            }
        }
        return set;
    }

    public LinkedHashSet<ProperType> findProperUpperBounds() {
        LinkedHashSet<ProperType> set = new LinkedHashSet<>();
        for (AbstractType bound : bounds.get(BoundKind.UPPER)) {
            if (bound.isProper()) {
                set.add((ProperType) bound);
            }
        }
        return set;
    }

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

    /** @return all variables mentioned in a bound against this variable. */
    public Collection<? extends Variable> getVariablesMentionedInBounds() {
        List<Variable> mentioned = new ArrayList<>();
        for (Set<AbstractType> boundList : bounds.values()) {
            for (AbstractType bound : boundList) {
                mentioned.addAll(bound.getInferenceVariables());
            }
        }
        return mentioned;
    }

    /** @return the instantiation of this variable */
    public ProperType getInstantiation() {
        return instantiation;
    }

    /** @return true if this has an instantiation */
    public boolean hasInstantiation() {
        return instantiation != null;
    }

    /** @return true if any bound mentions a primitive wrapper type. */
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
     * @return true if any lower or equal bound is a parameterized type with at least one wildcard
     *     for a type argument
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
            TypeMirror s1Java = s1.getJavaType();
            for (int j = i + 1; j < parameteredTypes.size(); j++) {
                AbstractType s2 = parameteredTypes.get(j);
                TypeMirror s2Java = s2.getJavaType();
                Pair<TypeMirror, TypeMirror> supers =
                        InternalInferenceUtils.getParameterizedSupers(s1Java, s2Java, context);
                if (supers == null) {
                    continue;
                }
                List<AbstractType> s1TypeArgs = s1.asSuper(supers.first).getTypeArguments();
                List<AbstractType> s2TypeArgs = s2.asSuper(supers.second).getTypeArguments();
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
        TypeMirror gTypeMirror = g.getJavaType();
        for (AbstractType type : bounds.get(BoundKind.LOWER)) {
            if (type.isVariable()) {
                continue;
            }
            AbstractType superTypeOfS = type.asSuper(gTypeMirror);
            if (superTypeOfS != null && superTypeOfS.isRaw()) {
                return true;
            }
        }

        for (AbstractType type : bounds.get(BoundKind.EQUAL)) {
            if (type.isVariable()) {
                continue;
            }
            AbstractType superTypeOfS = type.asSuper(gTypeMirror);
            if (superTypeOfS != null && superTypeOfS.isRaw()) {
                return true;
            }
        }
        return false;
    }

    // </editor-fold>

}
