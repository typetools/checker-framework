package org.checkerframework.framework.util.element;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.element.ElementAnnotationUtil.UnexpectedAnnotationLocationException;
import org.checkerframework.javacutil.BugInCF;

/** Applies the annotations present for a method type parameter onto an AnnotatedTypeVariable. */
public class MethodTypeParamApplier extends TypeParamElementAnnotationApplier {

    /** Apply annotations from {@code element} to {@code type}. */
    public static void apply(
            AnnotatedTypeVariable type, Element element, AnnotatedTypeFactory typeFactory)
            throws UnexpectedAnnotationLocationException {
        new MethodTypeParamApplier(type, element, typeFactory).extractAndApply();
    }

    /** @return true if element represents a type parameter for a method */
    public static boolean accepts(final AnnotatedTypeMirror type, final Element element) {
        return element.getKind() == ElementKind.TYPE_PARAMETER
                && element.getEnclosingElement() instanceof Symbol.MethodSymbol;
    }

    private final Symbol.MethodSymbol enclosingMethod;

    MethodTypeParamApplier(
            AnnotatedTypeVariable type, Element element, AnnotatedTypeFactory typeFactory) {
        super(type, element, typeFactory);

        if (!(element.getEnclosingElement() instanceof Symbol.MethodSymbol)) {
            throw new BugInCF(
                    "TypeParameter not enclosed by method?  Type( "
                            + type
                            + " ) "
                            + "Element ( "
                            + element
                            + " ) ");
        }

        enclosingMethod = (Symbol.MethodSymbol) element.getEnclosingElement();
    }

    /** @return TargetType.METHOD_TYPE_PARAMETER */
    @Override
    protected TargetType lowerBoundTarget() {
        return TargetType.METHOD_TYPE_PARAMETER;
    }

    /** @return TargetType.METHOD_TYPE_PARAMETER_BOUND */
    @Override
    protected TargetType upperBoundTarget() {
        return TargetType.METHOD_TYPE_PARAMETER_BOUND;
    }

    /** @return the index of element in the type parameter list of its enclosing method */
    @Override
    public int getElementIndex() {
        return enclosingMethod.getTypeParameters().indexOf(element);
    }

    @Override
    protected TargetType[] validTargets() {
        return new TargetType[] {
            TargetType.METHOD_RETURN,
            TargetType.METHOD_FORMAL_PARAMETER,
            TargetType.METHOD_RECEIVER,
            TargetType.THROWS,
            TargetType.LOCAL_VARIABLE,
            TargetType.RESOURCE_VARIABLE,
            TargetType.EXCEPTION_PARAMETER,
            TargetType.NEW,
            TargetType.CAST,
            TargetType.INSTANCEOF,
            TargetType.METHOD_INVOCATION_TYPE_ARGUMENT,
            TargetType.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT,
            TargetType.METHOD_REFERENCE,
            TargetType.CONSTRUCTOR_REFERENCE,
            TargetType.METHOD_REFERENCE_TYPE_ARGUMENT,
            TargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT,
        };
    }

    /** @return the TypeCompounds (annotations) of the declaring element */
    @Override
    protected Iterable<Attribute.TypeCompound> getRawTypeAttributes() {
        return enclosingMethod.getRawTypeAttributes();
    }

    @Override
    protected boolean isAccepted() {
        return accepts(type, element);
    }
}
