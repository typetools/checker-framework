package org.checkerframework.framework.type.explicit;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import java.util.List;

import static org.checkerframework.framework.type.explicit.ElementAnnotationUtil.annotateViaTypeAnnoPosition;
import static org.checkerframework.framework.type.explicit.ElementAnnotationUtil.getParentMethod;
import static com.sun.tools.javac.code.TargetType.*;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.METHOD_REFERENCE_TYPE_ARGUMENT;

/**
 * Adds annotations to one formal parameter of a method.
 */
class ParamApplier extends IndexedElementAnnotationApplier {

    public static void apply(AnnotatedTypeMirror type, Element element, AnnotatedTypeFactory typeFactory) {
        new ParamApplier(type, element).extractAndApply();
    }

    public static int RECEIVER_PARAM_INDEX = Integer.MIN_VALUE;

    public static boolean accepts(final AnnotatedTypeMirror type, final Element element) {
        return element.getKind() == ElementKind.PARAMETER;
    }

    private final Symbol.MethodSymbol enclosingMethod;

    public ParamApplier(AnnotatedTypeMirror type, Element element) {
        super(type, element);
        enclosingMethod = getParentMethod( element );
    }

    /**
     * @return The index of element its parent method's parameter list.
     * Integer.MIN_VALUE if the element is the receiver parameter.
     */
    @Override
    public int getElementIndex() {
        if( isReceiver(element) ) {
            return RECEIVER_PARAM_INDEX;
        }

        final int paramIndex = enclosingMethod.getParameters().indexOf(element);
        if( paramIndex == -1 ) {
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
             METHOD_FORMAL_PARAMETER, METHOD_RETURN, THROWS,METHOD_TYPE_PARAMETER, METHOD_TYPE_PARAMETER_BOUND,
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

    /**
     * @param targeted Type compounds with formal method parameter target types with parameter_index == getIndex
     */
    @Override
    protected void handleTargeted(final List<Attribute.TypeCompound> targeted) {

        if( isReceiver( element ) ) {
            for( final Attribute.TypeCompound anno : targeted) {
                if( anno.position.type == METHOD_RECEIVER ) {
                    annotateViaTypeAnnoPosition(type, anno);
                }
            }

        } else {
            for( final Attribute.TypeCompound anno : targeted) {
                annotateViaTypeAnnoPosition(type, anno);
            }
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
}
