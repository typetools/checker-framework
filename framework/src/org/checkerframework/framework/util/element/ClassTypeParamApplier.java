package org.checkerframework.framework.util.element;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;

/**
 * Applies the annotations present for a class type parameter onto an AnnotatedTypeVariable.  See
 * {@link TypeParamElementAnnotationApplier} for details.
 */
public class ClassTypeParamApplier extends TypeParamElementAnnotationApplier {

    public static void apply(AnnotatedTypeVariable type, Element element, AnnotatedTypeFactory typeFactory) {
        new ClassTypeParamApplier(type, element, typeFactory).extractAndApply();
    }

    /**
     * @return True if element represents a type parameter for a class.
     */
    public static boolean accepts(final AnnotatedTypeMirror type, final Element element) {
        return element.getKind() == ElementKind.TYPE_PARAMETER &&
               element.getEnclosingElement() instanceof Symbol.ClassSymbol;
    }

    /**
     * The class that holds the type parameter element
     */
    private final Symbol.ClassSymbol enclosingClass;

    ClassTypeParamApplier(AnnotatedTypeVariable type, Element element, AnnotatedTypeFactory typeFactory) {
        super(type, element, typeFactory);

        if (!( element.getEnclosingElement() instanceof Symbol.ClassSymbol )) {
            ErrorReporter.errorAbort("TypeParameter not enclosed by class?  Type( " + type + " ) " +
                    "Element ( " + element + " ) ");
        }

        enclosingClass = (Symbol.ClassSymbol) element.getEnclosingElement();
    }

    /**
     * @return TargetType.CLASS_TYPE_PARAMETER
     */
    @Override
    protected TargetType lowerBoundTarget() {
        return TargetType.CLASS_TYPE_PARAMETER;
    }

    /**
     * @return TargetType.CLASS_TYPE_PARAMETER_BOUND
     */
    @Override
    protected TargetType upperBoundTarget() {
        return TargetType.CLASS_TYPE_PARAMETER_BOUND;
    }

    /**
     * @return The index of element in the type parameter list of its enclosing class
     */
    @Override
    public int getElementIndex() {
        return enclosingClass.getTypeParameters().indexOf(element);
    }

    @Override
    protected TargetType[] validTargets() {
        return new TargetType[]{ TargetType.CLASS_EXTENDS };
    }

    /**
     * @return The raw type attributes of the enclosing class
     */
    @Override
    protected Iterable<Attribute.TypeCompound> getRawTypeAttributes() {
        return enclosingClass.getRawTypeAttributes();
    }

    @Override
    protected boolean isAccepted() {
        return accepts(type, element);
    }
}
