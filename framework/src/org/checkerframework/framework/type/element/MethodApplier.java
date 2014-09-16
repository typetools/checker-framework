package org.checkerframework.framework.util.element;


import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import static org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;

import static org.checkerframework.framework.util.element.ElementAnnotationUtil.*;
import static com.sun.tools.javac.code.TargetType.*;

import com.sun.tools.javac.code.TypeAnnotationPosition;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.Element;
import java.util.List;

/**
 * Adds annotations from element to the return type, formal parameter types, type parameters, and
 * throws clauses of the AnnotatedExecutableType type.
 */
public class MethodApplier extends TargetedElementAnnotationApplier {

    public static void apply(AnnotatedTypeMirror type, Element element, AnnotatedTypeFactory typeFactory) {
        new MethodApplier(type, element, typeFactory).extractAndApply();
    }

    public static boolean accepts( final AnnotatedTypeMirror typeMirror, final Element element ) {
        return element instanceof Symbol.MethodSymbol &&
               typeMirror instanceof AnnotatedExecutableType;
    }

    private final AnnotatedTypeFactory typeFactory;

    /**
     * Method being annotated, this symbol contains all relevant annotations
     */
    private final Symbol.MethodSymbol methodSymbol;
    private final AnnotatedExecutableType methodType;

    MethodApplier(AnnotatedTypeMirror type, Element element, AnnotatedTypeFactory typeFactory) {
        super(type, element);
        this.typeFactory = typeFactory;
        this.methodSymbol = (Symbol.MethodSymbol) element;
        this.methodType = (AnnotatedExecutableType) type;
    }

    /**
     * @return Receiver, returns, and throws.  See extract and apply as we also annotate type params.
     */
    @Override
    protected TargetType[] annotatedTargets() {
        return new TargetType[]{ METHOD_RECEIVER, METHOD_RETURN, THROWS };
    }

    /**
     * @return All possible annotation positions for a method except those in annotatedTargets
     */
    @Override
    protected TargetType[] validTargets() {
        return new TargetType[]{
            LOCAL_VARIABLE, RESOURCE_VARIABLE, EXCEPTION_PARAMETER, NEW, CAST, INSTANCEOF,
            METHOD_INVOCATION_TYPE_ARGUMENT, CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT, METHOD_REFERENCE,
            CONSTRUCTOR_REFERENCE, METHOD_REFERENCE_TYPE_ARGUMENT,CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT,
            METHOD_TYPE_PARAMETER, METHOD_TYPE_PARAMETER_BOUND, METHOD_FORMAL_PARAMETER
        };
    }

    /**
     * @return The annotations on the method symbol (element)
     */
    @Override
    protected Iterable<Attribute.TypeCompound> getRawTypeAttributes() {
        return methodSymbol.getRawTypeAttributes();
    }

    /**
     * @inherit
     */
    @Override
    protected boolean isAccepted() {
        return MethodApplier.accepts(type, element);
    }

    /**
     * Sets the method's element, annotates it's return type, parameters, type parameters, and throws
     * annotations.
     */
    @Override
    public void extractAndApply() {
        methodType.setElement(methodSymbol); //Preserves previous behavior

        // Add declaration annotations to the return type if
        if( methodType.getReturnType() instanceof AnnotatedTypeVariable) {
            applyTypeVarUseOnReturnType();
        }
        addAnnotationsFromElement(methodType.getReturnType(), methodSymbol.getAnnotationMirrors());

        final List<AnnotatedTypeMirror> params = methodType.getParameterTypes();
        for (int i = 0; i < params.size(); ++i) {
            // Add declaration annotations to the parameter type
            addAnnotationsFromElement(params.get(i), methodSymbol.getParameters().get(i).getAnnotationMirrors());
        }

        //ensures that we check that there are only valid target types on this class, there are no "invalid" locations
        super.extractAndApply();

        applyAllElementAnnotations( methodType.getParameterTypes(), methodSymbol.getParameters(),     typeFactory );
        applyAllElementAnnotations( methodType.getTypeVariables(),  methodSymbol.getTypeParameters(), typeFactory );
    }

    //NOTE that these are the only locations not handled elsewhere, otherwise we call apply
    @Override
    protected void handleTargeted(final List<Attribute.TypeCompound> targeted) {

        for( Attribute.TypeCompound anno : targeted) {
            switch( anno.position.type ) {

                case METHOD_RECEIVER:
                    annotateViaTypeAnnoPosition(methodType.getReceiverType(), anno);
                    break;

                case METHOD_RETURN:
                    annotateViaTypeAnnoPosition(methodType.getReturnType(), anno);
                    break;

                case THROWS:
                    applyThrowsAnnotation(anno);
                    break;

                default:
                    ErrorReporter.errorAbort("Undexpected annotation ( " + anno + " ) for" +
                            "type ( " + type +" ) and element ( " + element + " ) ");
            }
        }

    }

    private void applyThrowsAnnotation( final Attribute.TypeCompound anno ) {
        final TypeAnnotationPosition annoPos = anno.position;
        final List<AnnotatedTypeMirror> thrown = methodType.getThrownTypes();
        if (annoPos.type_index >= 0 && annoPos.type_index < thrown.size()) {
            annotateViaTypeAnnoPosition(thrown.get(annoPos.type_index), anno);
        } else {
            ErrorReporter.errorAbort("MethodApplier.applyThrowsAnnotation: " +
                    "invalid throws index " + annoPos.type_index +
                    " for annotation: " + anno+
                    " for element: " + ElementUtils.getVerboseName(element));
        }
    }

    /**
     * If the return type is a use of a type variable first apply the bound annotations from the
     * type variables declaration
     */
    private void applyTypeVarUseOnReturnType() {
        new TypeVarUseApplier(methodType.getReturnType(), methodSymbol, typeFactory).extractAndApply();
    }

}
