package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.VariableTree;
import java.util.LinkedHashSet;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
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

    AbstractType getTypeOfElement(Element element, Theta map);

    ProperType getObject();

    ProperType lub(LinkedHashSet<ProperType> lowerBounds);

    AbstractType glb(LinkedHashSet<AbstractType> lowerBounds);

    AbstractType glb(AbstractType a, AbstractType b);

    ProperType getRuntimeException();

    ConstraintSet getCheckedExceptionConstraints(ExpressionTree expression, Theta map);

    ProperType createWildcard(ProperType lowerBound, AbstractType upperBound);

    List<ProperType> getSubsTypeArgs(
            List<TypeVariable> typeVar, List<ProperType> typeArg, List<Variable> asList);

    /**
     * If a mapping for {@code invocation} doesn't exist create it by:
     *
     * <p>Creates inference variables for the type parameters to {@code methodType} for a particular
     * {@code invocation}. Initializes the bounds of the variables. Returns a mapping from type
     * variables to newly created variables.
     *
     * <p>Otherwise, returns the previously created mapping.
     *
     * @param invocation method or constructor invocation
     * @param methodType type of generic method
     * @param context Java8InferenceContext
     * @return a mapping of the type variables of {@code methodType} to inference variables
     */
    Theta createTheta(
            ExpressionTree invocation, InvocationType methodType, Java8InferenceContext context);

    Theta createTheta(LambdaExpressionTree lambda, AbstractType t);

    Theta createThetaForCapture(ExpressionTree tree, AbstractType capturedType);

    List<AbstractType> findParametersOfFunctionType(AbstractType t, Theta map);

    AbstractType getTypeOfBound(TypeParameterElement pEle, Theta map);
}
