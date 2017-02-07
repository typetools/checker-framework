package org.checkerframework.framework.util.element;

import static com.sun.tools.javac.code.TargetType.CAST;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_REFERENCE;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.EXCEPTION_PARAMETER;
import static com.sun.tools.javac.code.TargetType.FIELD;
import static com.sun.tools.javac.code.TargetType.INSTANCEOF;
import static com.sun.tools.javac.code.TargetType.LOCAL_VARIABLE;
import static com.sun.tools.javac.code.TargetType.METHOD_INVOCATION_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.METHOD_REFERENCE;
import static com.sun.tools.javac.code.TargetType.METHOD_REFERENCE_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.NEW;
import static com.sun.tools.javac.code.TargetType.RESOURCE_VARIABLE;
import static org.checkerframework.framework.util.element.ElementAnnotationUtil.addAnnotationsFromElement;
import static org.checkerframework.framework.util.element.ElementAnnotationUtil.annotateViaTypeAnnoPosition;
import static org.checkerframework.framework.util.element.ElementAnnotationUtil.contains;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ErrorReporter;

/**
 * Applies annotations to variable declaration (providing they are not the use of a TYPE_PARAMETER).
 */
public class VariableApplier extends TargetedElementAnnotationApplier {

    public static void apply(final AnnotatedTypeMirror type, final Element element) {
        new VariableApplier(type, element).extractAndApply();
    }

    private static final ElementKind[] acceptedKinds = {
        ElementKind.LOCAL_VARIABLE, ElementKind.RESOURCE_VARIABLE, ElementKind.EXCEPTION_PARAMETER
    };

    /** @return true if this is a variable declaration including fields an enum constants */
    public static boolean accepts(final AnnotatedTypeMirror typeMirror, final Element element) {
        return contains(element.getKind(), acceptedKinds) || element.getKind().isField();
    }

    private final Symbol.VarSymbol varSymbol;

    VariableApplier(final AnnotatedTypeMirror type, final Element element) {
        super(type, element);
        varSymbol = (Symbol.VarSymbol) element;

        if (type.getKind() == TypeKind.UNION
                && element.getKind() != ElementKind.EXCEPTION_PARAMETER) {
            ErrorReporter.errorAbort(
                    "Union types only allowed for exception parameters! "
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
        return new TargetType[] {LOCAL_VARIABLE, RESOURCE_VARIABLE, EXCEPTION_PARAMETER, FIELD};
    }

    @Override
    protected TargetType[] validTargets() {
        return new TargetType[] {
            NEW,
            CAST,
            INSTANCEOF,
            METHOD_INVOCATION_TYPE_ARGUMENT,
            CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT,
            METHOD_REFERENCE,
            CONSTRUCTOR_REFERENCE,
            METHOD_REFERENCE_TYPE_ARGUMENT,
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

    @Override
    public void extractAndApply() {
        // Add declaration annotations to the local variable type
        addAnnotationsFromElement(type, varSymbol.getAnnotationMirrors());
        super.extractAndApply();
    }
}
