package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds.BoundKind;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;

/** An inference variable */
public class Variable extends AbstractTypeMirror {

    public final VariableBounds variableBounds;
    /** Identification number. Used only to make debugging easier. */
    protected final int id;

    /**
     * The expression for which this variable is being solved. Used to differentiate inference
     * variables for two different invocations to the same method.
     */
    protected final ExpressionTree invocation;

    /** Type variable for which the instantiation of this variable is a type argument, */
    public final TypeVariable typeVariable;

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
        this.variableBounds = new VariableBounds(context);
        this.typeVariable = typeVariable;
        this.invocation = invocation;
        this.id = id;
    }

    @Override
    public Variable create(TypeMirror type) {
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
    public Variable capture() {
        return this;
    }

    @Override
    public Variable getErased() {
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
        return context.modelTypes.isSameType(typeVariable, variable.typeVariable)
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
                return inst.variableBounds.getInstantiation();
            }
        }
        return this;
    }

    @Override
    public String toString() {
        if (variableBounds.hasInstantiation()) {
            return "a" + id + " := " + variableBounds.getInstantiation();
        }
        return "a" + id;
    }

    /** @return true if this is a variable created for a capture bound. */
    public boolean isCaptureVariable() {
        return this instanceof CaptureVariable;
    }

    /** Save the current bounds. */
    public void save() {
        variableBounds.save();
    }

    /** Restore the bounds to the state previously saved. */
    public void restore() {
        variableBounds.restore();
    }

    /**
     * Adds the initial bounds to this variable. These are the bounds implied by the upper bounds of
     * the type variable. See end of JLS 18.1.3.
     *
     * @param map used to determine if the bounds refer to another variable
     */
    public void initialBounds(Theta map) {
        variableBounds.initialBounds(typeVariable, map);
    }

    /** @return true if this has a throws bound */
    public boolean hasThrowsBound() {
        return variableBounds.hasThrowsBound();
    }

    /** Sets the value of hasThrowsBound to {@code hasThrowsBound} */
    public void setHasThrowsBound(boolean hasThrowsBound) {
        this.variableBounds.hasThrowsBound = hasThrowsBound;
    }

    /** Adds {@code otherType} as bound against this variable. */
    public boolean addBound(BoundKind kind, AbstractType otherType) {
        return variableBounds.addBound(kind, otherType);
    }

    public LinkedHashSet<ProperType> findProperLowerBounds() {
        return variableBounds.findProperLowerBounds();
    }

    public LinkedHashSet<ProperType> findProperUpperBounds() {
        return variableBounds.findProperUpperBounds();
    }

    public LinkedHashSet<AbstractType> upperBounds() {
        return variableBounds.upperBounds();
    }

    /** Apply instantiations to all bounds and constraints of this variable. */
    public boolean applyInstantiationsToBounds(List<Variable> instantiations) {
        return variableBounds.applyInstantiationsToBounds(instantiations);
    }

    /** @return all variables mentioned in a bound against this variable. */
    public Collection<? extends Variable> getVariablesMentionedInBounds() {
        return variableBounds.getVariablesMentionedInBounds();
    }

    /** @return the instantiation of this variable */
    public ProperType getInstantiation() {
        return variableBounds.getInstantiation();
    }

    /** @return true if this has an instantiation */
    public boolean hasInstantiation() {
        return variableBounds.hasInstantiation();
    }

    /** @return true if any bound mentions a primitive wrapper type. */
    public boolean hasPrimitiveWrapperBound() {
        return variableBounds.hasPrimitiveWrapperBound();
    }

    /**
     * @return true if any lower or equal bound is a parameterized type with at least one wildcard
     *     for a type argument
     */
    public boolean hasWildcardParameterizedLowerOrEqualBound() {
        return variableBounds.hasWildcardParameterizedLowerOrEqualBound();
    }

    /**
     * Does this bound set contain two bounds of the forms {@code S1 <: var} and {@code S2 <: var},
     * where S1 and S2 have supertypes that are two different parameterizations of the same generic
     * class or interface?
     */
    public boolean hasLowerBoundDifferentParam() {
        return variableBounds.hasLowerBoundDifferentParam();
    }

    /**
     * Does this bound set contain a bound of one of the forms {@code var = S} or {@code S <: var},
     * where there exists no type of the form {@code G<...>} that is a supertype of S, but the raw
     * type {@code |G<...>|} is a supertype of S?
     */
    public boolean hasRawTypeLowerOrEqualBound(AbstractType g) {
        return variableBounds.hasRawTypeLowerOrEqualBound(g);
    }
}
