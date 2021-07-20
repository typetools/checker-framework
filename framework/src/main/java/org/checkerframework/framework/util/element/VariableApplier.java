package org.checkerframework.framework.util.element;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.element.ElementAnnotationUtil.UnexpectedAnnotationLocationException;
import org.checkerframework.javacutil.BugInCF;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

/**
 * Applies annotations to variable declaration (providing they are not the use of a TYPE_PARAMETER).
 */
public class VariableApplier extends TargetedElementAnnotationApplier {

    /** Apply annotations from {@code element} to {@code type}. */
    public static void apply(final AnnotatedTypeMirror type, final Element element)
            throws UnexpectedAnnotationLocationException {
        new VariableApplier(type, element).extractAndApply();
    }

    private static final ElementKind[] acceptedKinds = {
        ElementKind.LOCAL_VARIABLE, ElementKind.RESOURCE_VARIABLE, ElementKind.EXCEPTION_PARAMETER
    };

    /**
     * Returns true if this is a variable declaration including fields an enum constants.
     *
     * @param typeMirror ignored
     * @return true if this is a variable declaration including fields an enum constants
     */
    public static boolean accepts(final AnnotatedTypeMirror typeMirror, final Element element) {
        return ElementAnnotationUtil.contains(element.getKind(), acceptedKinds)
                || element.getKind().isField();
    }

    private final Symbol.VarSymbol varSymbol;

    VariableApplier(final AnnotatedTypeMirror type, final Element element) {
        super(type, element);
        varSymbol = (Symbol.VarSymbol) element;

        if (type.getKind() == TypeKind.UNION
                && element.getKind() != ElementKind.EXCEPTION_PARAMETER) {
            throw new BugInCF(
                    "Union types only allowed for exception parameters. "
                            + "Type: "
                            + type
                            + " for element: "
                            + element);
        }
        // TODO: need a way to split the union types into the right alternative
        // to use for the annotation. The exception_index is probably what we
        // need to look at, but it might not be set at this point.
    }

    @Override
    protected TargetType[] annotatedTargets() {
        return new TargetType[] {
            TargetType.LOCAL_VARIABLE,
            TargetType.RESOURCE_VARIABLE,
            TargetType.EXCEPTION_PARAMETER,
            TargetType.FIELD
        };
    }

    @Override
    protected TargetType[] validTargets() {
        return new TargetType[] {
            TargetType.NEW,
            TargetType.CAST,
            TargetType.INSTANCEOF,
            TargetType.METHOD_INVOCATION_TYPE_ARGUMENT,
            TargetType.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT,
            TargetType.METHOD_REFERENCE,
            TargetType.CONSTRUCTOR_REFERENCE,
            TargetType.METHOD_REFERENCE_TYPE_ARGUMENT,
            TargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
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
    protected void handleTargeted(final List<TypeCompound> targeted)
            throws UnexpectedAnnotationLocationException {
        ElementAnnotationUtil.annotateViaTypeAnnoPosition(type, targeted);
    }

    @Override
    public void extractAndApply() throws UnexpectedAnnotationLocationException {
        // Add declaration annotations to the local variable type
        ElementAnnotationUtil.addDeclarationAnnotationsFromElement(
                type, varSymbol.getAnnotationMirrors());
        super.extractAndApply();
    }
}
