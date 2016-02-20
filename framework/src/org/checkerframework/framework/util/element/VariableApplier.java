package org.checkerframework.framework.util.element;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.List;

import static org.checkerframework.framework.util.element.ElementAnnotationUtil.addAnnotationsFromElement;
import static org.checkerframework.framework.util.element.ElementAnnotationUtil.annotateViaTypeAnnoPosition;
import static org.checkerframework.framework.util.element.ElementAnnotationUtil.contains;
import static com.sun.tools.javac.code.TargetType.*;

/**
 *  Applies annotations to variable declaration (providing they are not the use of a TYPE_PARAMETER).
 */
public class VariableApplier extends TargetedElementAnnotationApplier {

    public static void apply(final AnnotatedTypeMirror type, final Element element) {
        new VariableApplier(type, element).extractAndApply();
    }

    private static final ElementKind[] acceptedKinds = {
            ElementKind.LOCAL_VARIABLE, ElementKind.RESOURCE_VARIABLE, ElementKind.EXCEPTION_PARAMETER
    };

    /**
     * @return True if this is a variable declaration including fields an enum constants
     */
    public static boolean accepts(final AnnotatedTypeMirror typeMirror, final Element element) {
        return contains(element.getKind(), acceptedKinds) || element.getKind().isField();
    }

    private final Symbol.VarSymbol varSymbol;

    VariableApplier(final AnnotatedTypeMirror type, final Element element) {
        super(type, element);
        varSymbol = (Symbol.VarSymbol) element;
    }

    @Override
    protected TargetType[] annotatedTargets() {
        return new TargetType[]{ LOCAL_VARIABLE, RESOURCE_VARIABLE, EXCEPTION_PARAMETER, FIELD };
    }

    @Override
    protected TargetType[] validTargets() {
        return new TargetType []{
                NEW, CAST, INSTANCEOF, METHOD_INVOCATION_TYPE_ARGUMENT, CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT,
                METHOD_REFERENCE, CONSTRUCTOR_REFERENCE, METHOD_REFERENCE_TYPE_ARGUMENT,
                CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
        };
    }

    @Override
    protected Iterable<Attribute.TypeCompound> getRawTypeAttributes() {
        return varSymbol.getRawTypeAttributes();
    }

    @Override
    protected boolean isAccepted() {
        return accepts(type, element);
    }

    @Override
    protected void handleTargeted(final List<Attribute.TypeCompound> targeted) {
        annotateViaTypeAnnoPosition(type, targeted);
    }

    public void extractAndApply() {
        // Add declaration annotations to the local variable type
        addAnnotationsFromElement(type, varSymbol.getAnnotationMirrors());
        super.extractAndApply();
    }
}
