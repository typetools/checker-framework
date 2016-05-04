package org.checkerframework.framework.util.element;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.ElementAnnotationApplier;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;

import static org.checkerframework.framework.util.element.ElementAnnotationUtil.annotateViaTypeAnnoPosition;
import static org.checkerframework.framework.util.element.ElementAnnotationUtil.partitionByTargetType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;

import static com.sun.tools.javac.code.TargetType.CAST;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_REFERENCE;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.EXCEPTION_PARAMETER;
import static com.sun.tools.javac.code.TargetType.INSTANCEOF;
import static com.sun.tools.javac.code.TargetType.LOCAL_VARIABLE;
import static com.sun.tools.javac.code.TargetType.METHOD_FORMAL_PARAMETER;
import static com.sun.tools.javac.code.TargetType.METHOD_INVOCATION_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.METHOD_RECEIVER;
import static com.sun.tools.javac.code.TargetType.METHOD_REFERENCE;
import static com.sun.tools.javac.code.TargetType.METHOD_REFERENCE_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.METHOD_RETURN;
import static com.sun.tools.javac.code.TargetType.METHOD_TYPE_PARAMETER;
import static com.sun.tools.javac.code.TargetType.METHOD_TYPE_PARAMETER_BOUND;
import static com.sun.tools.javac.code.TargetType.NEW;
import static com.sun.tools.javac.code.TargetType.RESOURCE_VARIABLE;
import static com.sun.tools.javac.code.TargetType.THROWS;

/**
 * Adds annotations to one formal parameter of a method or lambda within a method.
 */
public class ParamApplier extends IndexedElementAnnotationApplier {

    public static void apply(AnnotatedTypeMirror type, Element element, AnnotatedTypeFactory typeFactory) {
        new ParamApplier(type, element, typeFactory).extractAndApply();
    }

    public static int RECEIVER_PARAM_INDEX = Integer.MIN_VALUE;

    public static boolean accepts(final AnnotatedTypeMirror type, final Element element) {
        return element.getKind() == ElementKind.PARAMETER;
    }

    private final Symbol.MethodSymbol enclosingMethod;
    private final boolean isLambdaParam;
    private final Integer lambdaParamIndex;
    private final LambdaExpressionTree lambdaTree;

    ParamApplier(AnnotatedTypeMirror type, Element element, AnnotatedTypeFactory typeFactory) {
        super(type, element);
        enclosingMethod = getParentMethod( element );

        if (enclosingMethod.getKind() != ElementKind.INSTANCE_INIT
         && enclosingMethod.getKind() != ElementKind.STATIC_INIT
         && enclosingMethod.getParameters().contains(element)) {
            lambdaTree = null;
            isLambdaParam = false;
            lambdaParamIndex = null;

        } else {
            Pair<VariableTree, LambdaExpressionTree> paramToEnclosingLambda =
                ElementAnnotationApplier.getParamAndLambdaTree((VariableElement) element, typeFactory);

            if (paramToEnclosingLambda != null) {
                VariableTree paramDecl = paramToEnclosingLambda.first;
                lambdaTree = paramToEnclosingLambda.second;
                isLambdaParam = true;
                lambdaParamIndex = lambdaTree.getParameters().indexOf(paramDecl);

            } else {
                lambdaTree = null;
                isLambdaParam = false;
                lambdaParamIndex = null;
            }
        }
    }

    /**
     * @return The index of element its parent method's parameter list.
     * Integer.MIN_VALUE if the element is the receiver parameter.
     */
    @Override
    public int getElementIndex() {
        if (isLambdaParam) {
            return lambdaParamIndex;
        }

        if (isReceiver(element)) {
            return RECEIVER_PARAM_INDEX;
        }

        final int paramIndex = enclosingMethod.getParameters().indexOf(element);
        if (paramIndex == -1) {
            ErrorReporter.errorAbort("Could not find parameter Element in parameter list! " +
                    "Parameter( " + element + " ) Parent ( " + enclosingMethod + " ) ");
        }

        return paramIndex;
    }

    /**
     * @return the parameter index of anno's TypeAnnotationPosition
     */
    @Override
    public int getTypeCompoundIndex(Attribute.TypeCompound anno) {
        return anno.getPosition().parameter_index;
    }

    /**
     * @return {TargetType.METHOD_FORMAL_PARAMETER, TargetType.METHOD_RECEIVER}
     */
    @Override
    protected TargetType[] annotatedTargets() {
        return new TargetType[]{ METHOD_FORMAL_PARAMETER, METHOD_RECEIVER };
    }

    /**
     * @return Any annotation TargetType that can be found on a method
     */
    @Override
    protected TargetType[] validTargets() {
        return new TargetType []{
             METHOD_FORMAL_PARAMETER, METHOD_RETURN, THROWS, METHOD_TYPE_PARAMETER, METHOD_TYPE_PARAMETER_BOUND,
             LOCAL_VARIABLE, RESOURCE_VARIABLE, EXCEPTION_PARAMETER, NEW, CAST, INSTANCEOF,
             METHOD_INVOCATION_TYPE_ARGUMENT, CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT, METHOD_REFERENCE,
             CONSTRUCTOR_REFERENCE, METHOD_REFERENCE_TYPE_ARGUMENT, CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
        };
    }

    /**
     * @return The TypeCompounds (annotations) of the enclosing method for this parameter
     */
    @Override
    protected Iterable<Attribute.TypeCompound> getRawTypeAttributes() {
        return enclosingMethod.getRawTypeAttributes();
    }

    @Override
    protected Map<TargetClass, List<TypeCompound>> sift(Iterable<Attribute.TypeCompound> typeCompounds) {
        //this will sift out the annotations that do not have the right position index
        final Map<TargetClass, List<Attribute.TypeCompound>> targetClassToAnnos = super.sift(typeCompounds);

        final List<Attribute.TypeCompound> targeted = targetClassToAnnos.get(TargetClass.TARGETED);
        final List<Attribute.TypeCompound> valid    = targetClassToAnnos.get(TargetClass.VALID);

        //if this is a lambdaParam, filter out from targeted those annos that apply to method formal parameters
        //if this is a method formal param, filter out from targeted those annos that apply to lambdas
        int i = 0;
        while (i < targeted.size()) {
            final Tree onLambda = targeted.get(i).position.onLambda;
            if (onLambda == null) {
                if (!isLambdaParam) {
                    ++i;
                } else {
                    valid.add(targeted.remove(i));
                }

            } else {
                if (onLambda.equals(this.lambdaTree)) {
                    ++i;
                } else {
                    valid.add(targeted.remove(i));
                }

            }
        }

        return targetClassToAnnos;
    }

    /**
     * @param targeted Type compounds with formal method parameter target types with parameter_index == getIndex
     */
    @Override
    protected void handleTargeted(final List<Attribute.TypeCompound> targeted) {

        final List<TypeCompound> formalParams = new ArrayList<>();
        Map<TargetType, List<TypeCompound>> targetToAnnos = partitionByTargetType(targeted, formalParams, METHOD_RECEIVER);

        if (isReceiver(element)) {
            annotateViaTypeAnnoPosition(type, targetToAnnos.get(METHOD_RECEIVER));

        } else {
            annotateViaTypeAnnoPosition(type, formalParams);

        }
    }

    /**
     * @return True if element represents the receiver parameter of a method
     */
    private boolean isReceiver(final Element element) {
        return element.getKind() == ElementKind.PARAMETER && element.getSimpleName().contentEquals("this");
    }

    @Override
    protected boolean isAccepted() {
        return accepts(type, element);
    }


    /**
     * Return the enclosing MethodSymbol of the given element, throwing an exception of the symbol's enclosing
     * element is not a MethodSymbol
     * @param methodChildElem Some element that is a child of a method typeDeclaration (e.g. a parameter or return type)
     * @return The MethodSymbol of the method containing methodChildElem
     */
    public static Symbol.MethodSymbol getParentMethod(final Element methodChildElem) {
        if (!( methodChildElem.getEnclosingElement() instanceof Symbol.MethodSymbol)) {
            throw new RuntimeException("Element is not a direct child of a MethodSymbol. Element ( " + methodChildElem +
                    " parent ( " + methodChildElem.getEnclosingElement() + " ) ");
        }
        return (Symbol.MethodSymbol) methodChildElem.getEnclosingElement();
    }
}
