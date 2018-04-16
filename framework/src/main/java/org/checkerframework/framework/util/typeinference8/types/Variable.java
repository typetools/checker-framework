package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.types.typemirror.AbstractTypeMirror;
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

    public VariableBounds getBounds() {
        return variableBounds;
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
}
