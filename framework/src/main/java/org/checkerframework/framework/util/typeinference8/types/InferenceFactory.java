package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.VariableTree;
import java.util.LinkedHashSet;
import java.util.List;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.javacutil.Pair;

public interface InferenceFactory {

    InvocationType getTypeOfMethodAdaptedToUse(ExpressionTree invocation);

    ProperType getTargetType();

    InvocationType compileTimeDeclarationType(MemberReferenceTree memRef);

    InvocationType findFunctionType(MemberReferenceTree memRef);

    /**
     * @return a supertype of S of the form {@code G<S1, ..., Sn>} and a supertype of T of the form
     *     {@code G<T1,..., Tn>} for some generic class or interface, G. If such types exist;
     *     otherwise, null is returned.
     */
    Pair<AbstractType, AbstractType> getParameterizedSupers(AbstractType a, AbstractType b);

    ProperType getTypeOfExpression(ExpressionTree tree);

    ProperType getTypeOfVariable(VariableTree tree);

    ProperType getObject();

    ProperType lub(LinkedHashSet<ProperType> lowerBounds);

    AbstractType glb(LinkedHashSet<AbstractType> lowerBounds);

    AbstractType glb(AbstractType a, AbstractType b);

    ProperType getRuntimeException();

    ConstraintSet getCheckedExceptionConstraints(ExpressionTree expression, Theta map);

    ProperType createWildcard(ProperType lowerBound, AbstractType upperBound);

    List<ProperType> getSubsTypeArgs(
            List<TypeVariable> typeVar, List<ProperType> typeArg, List<Variable> asList);
}
