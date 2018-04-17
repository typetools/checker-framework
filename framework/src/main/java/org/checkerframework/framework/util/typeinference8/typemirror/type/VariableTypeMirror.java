package org.checkerframework.framework.util.typeinference8.typemirror.type;

import com.sun.source.tree.ExpressionTree;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Theta;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds.BoundKind;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;

/** An inference variable */
public class VariableTypeMirror extends AbstractTypeMirror implements Variable {

    protected final VariableBounds variableBounds;
    /** Identification number. Used only to make debugging easier. */
    protected final int id;

    /**
     * The expression for which this variable is being solved. Used to differentiate inference
     * variables for two different invocations to the same method.
     */
    protected final ExpressionTree invocation;

    /** Type variable for which the instantiation of this variable is a type argument, */
    protected final TypeVariable typeVariable;

    VariableTypeMirror(
            TypeVariable typeVariable, ExpressionTree invocation, Java8InferenceContext context) {
        this(typeVariable, invocation, context, context.getNextVariableId());
    }

    VariableTypeMirror(
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
    public VariableBounds getBounds() {
        return variableBounds;
    }
    /**
     * Adds the initial bounds to this variable. These are the bounds implied by the upper bounds of
     * the type variable. See end of JLS 18.1.3.
     *
     * @param map used to determine if the bounds refer to another variable
     */
    @Override
    public void initialBounds(Theta map) {
        TypeMirror upperBound = typeVariable.getUpperBound();
        // If Pl has no TypeBound, the bound {@literal al <: Object} appears in the set. Otherwise, for
        // each type T delimited by & in the TypeBound, the bound {@literal al <: T[P1:=a1,..., Pp:=ap]}
        // appears in the set; if this results in no proper upper bounds for al (only dependencies),
        // then the bound {@literal al <: Object} also appears in the set.
        switch (upperBound.getKind()) {
            case INTERSECTION:
                for (TypeMirror bound : ((IntersectionType) upperBound).getBounds()) {
                    AbstractType t1 = InferenceTypeMirror.create(bound, map, context);
                    variableBounds.addBound(BoundKind.UPPER, t1);
                }
                break;
            default:
                AbstractType t1 = InferenceTypeMirror.create(upperBound, map, context);
                variableBounds.addBound(BoundKind.UPPER, t1);
                break;
        }
    }

    @Override
    public VariableTypeMirror create(TypeMirror type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isObject() {
        return false;
    }

    @Override
    public List<ProperType> getTypeParameterProperTypeBounds() {
        return null;
    }

    @Override
    public VariableTypeMirror capture() {
        return this;
    }

    @Override
    public VariableTypeMirror getErased() {
        return this;
    }

    @Override
    public TypeVariable getJavaType() {
        return typeVariable;
    }

    @Override
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

        VariableTypeMirror variable = (VariableTypeMirror) o;
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
                return inst.getBounds().getInstantiation();
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

    /** Save the current bounds. */
    @Override
    public void save() {
        variableBounds.save();
    }

    /** Restore the bounds to the state previously saved. */
    @Override
    public void restore() {
        variableBounds.restore();
    }

    @Override
    public boolean isCaptureVariable() {
        return false;
    }
}
