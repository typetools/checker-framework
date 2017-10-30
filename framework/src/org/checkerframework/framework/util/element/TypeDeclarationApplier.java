package org.checkerframework.framework.util.element;

import static com.sun.tools.javac.code.TargetType.CAST;
import static com.sun.tools.javac.code.TargetType.CLASS_EXTENDS;
import static com.sun.tools.javac.code.TargetType.CLASS_TYPE_PARAMETER;
import static com.sun.tools.javac.code.TargetType.CLASS_TYPE_PARAMETER_BOUND;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_REFERENCE;
import static com.sun.tools.javac.code.TargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.EXCEPTION_PARAMETER;
import static com.sun.tools.javac.code.TargetType.INSTANCEOF;
import static com.sun.tools.javac.code.TargetType.METHOD_INVOCATION_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.METHOD_REFERENCE;
import static com.sun.tools.javac.code.TargetType.METHOD_REFERENCE_TYPE_ARGUMENT;
import static com.sun.tools.javac.code.TargetType.NEW;
import static com.sun.tools.javac.code.TargetType.RESOURCE_VARIABLE;
import static org.checkerframework.framework.util.element.ElementAnnotationUtil.applyAllElementAnnotations;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;
import java.util.List;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;

/** Apply annotations to a declared type based on its declaration. */
public class TypeDeclarationApplier extends TargetedElementAnnotationApplier {

    public static void apply(
            final AnnotatedTypeMirror type,
            final Element element,
            final AnnotatedTypeFactory typeFactory) {
        new TypeDeclarationApplier(type, element, typeFactory).extractAndApply();
    }

    /**
     * If a type_index == -1 it means that the index refers to the immediate supertype class of the
     * declaration. There is only ever one of these since java has no multiple inheritance
     */
    public static final int SUPERCLASS_INDEX = -1;

    /** @return true if type is an annotated declared type and element is a ClassSymbol */
    public static boolean accepts(final AnnotatedTypeMirror type, final Element element) {
        return type instanceof AnnotatedDeclaredType && element instanceof Symbol.ClassSymbol;
    }

    private final AnnotatedTypeFactory typeFactory;
    private final Symbol.ClassSymbol typeSymbol;
    private final AnnotatedDeclaredType declaredType;

    TypeDeclarationApplier(
            final AnnotatedTypeMirror type,
            final Element element,
            final AnnotatedTypeFactory typeFactory) {
        super(type, element);
        this.typeFactory = typeFactory;
        this.typeSymbol = (Symbol.ClassSymbol) element;
        this.declaredType = (AnnotatedDeclaredType) type;
    }

    @Override
    protected TargetType[] validTargets() {
        return new TargetType[] {
            RESOURCE_VARIABLE,
            EXCEPTION_PARAMETER,
            NEW,
            CAST,
            INSTANCEOF,
            METHOD_INVOCATION_TYPE_ARGUMENT,
            CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT,
            METHOD_REFERENCE,
            CONSTRUCTOR_REFERENCE,
            METHOD_REFERENCE_TYPE_ARGUMENT,
            CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT,
            CLASS_TYPE_PARAMETER,
            CLASS_TYPE_PARAMETER_BOUND
        };
    }

    @Override
    protected TargetType[] annotatedTargets() {
        return new TargetType[] {CLASS_EXTENDS};
    }

    /** All TypeCompounds (annotations) on the ClassSymbol */
    @Override
    protected Iterable<Attribute.TypeCompound> getRawTypeAttributes() {
        return typeSymbol.getRawTypeAttributes();
    }

    /**
     * While more than just annotations on extends or implements clause are annotated by this class,
     * only these annotations are passed to handleTargeted (as they are the only in the
     * annotatedTargets list). See extractAndApply for type parameters
     *
     * @param extendsAndImplementsAnnos annotations with a TargetType of CLASS_EXTENDS
     */
    @Override
    protected void handleTargeted(List<Attribute.TypeCompound> extendsAndImplementsAnnos) {
        for (final Attribute.TypeCompound anno : extendsAndImplementsAnnos) {

            if (anno.position.type_index >= SUPERCLASS_INDEX && anno.position.location.isEmpty()) {
                type.addAnnotation(anno);
            }
        }
    }

    /** Adds extends/implements and class annotations to type. Annotates type parameters. */
    @Override
    public void extractAndApply() {
        // ensures that we check that there only valid target types on this class, there are no
        // "targeted" locations
        super.extractAndApply();

        // Annotate raw types //TODO: ASK WERNER WHAT THIS MIGHT MEAN?  WHAT ACTUALLY GOES HERE?
        type.addAnnotations(typeSymbol.getAnnotationMirrors());

        applyAllElementAnnotations(
                declaredType.getTypeArguments(), typeSymbol.getTypeParameters(), typeFactory);
    }

    @Override
    protected boolean isAccepted() {
        return accepts(type, element);
    }
}
