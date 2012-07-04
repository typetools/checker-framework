package checkers.types;

import checkers.quals.*;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.comp.*;

/**
 * Represents an annotation along with its annotation target (its
 * {@link AnnotationTarget}), which cannot (publicly) be obtained from an
 * {@link AnnotationMirror}. Some methods in this class depend on non-public
 * APIs.
 *
 * @see AnnotationTarget
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public final class InternalAnnotation implements AnnotationData {

    /** The annotation's Target. */
    private final @Nullable AnnotationTarget target;

    /** The annotation's location. */
    private final AnnotationLocation location;

    /** The {@link AnnotationMirror} for the annotation. */
    private final AnnotationMirror mirror;

    /** The annotation's class. */
    private final Class<? extends Annotation> annotation;

    /** Elements utilities, for getting annotation arguments with defaults. */
    private final Elements elements;

    /** Types utilities, for comparing types. */
    private final Types types;

    /**
     * Creates an annotation from the given {@link AnnotationMirror}[
     *
     * @param mirror the annotation, as represented by the compiler
     * @param env the processing environment
     */
    InternalAnnotation(AnnotationMirror mirror, ProcessingEnvironment env) {

        this.mirror = mirror;

        // Get the class from the mirror.
        try {
            this.annotation = (Class<@NonNull Annotation>) Class.forName(
                    mirror.getAnnotationType().toString());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("invalid annotation class: " + e.getMessage());
        }

        // Get the target from the mirror.
        if (mirror instanceof Attribute.Compound) {
            Attribute.Compound attr = (Attribute.Compound)mirror;
            this.target = null;
        } else
            this.target = null;

        // Get the location from the target.
        if (target != null && target.type.hasLocation()
                && target.location != null) { /*nninvariant*/
            assert target != null; // FIXME: flow workaround
            this.location = AnnotationLocation.fromArray(target.location);
        } else
            this.location = AnnotationLocation.RAW;

        this.elements = env.getElementUtils();
        this.types = env.getTypeUtils();
    }

    /**
     * Determines whether or not the annotation is an extended annotation.
     *
     * @return true if the represented annotation is an extended annotation,
     *         false otherwise
     */
    public boolean isExtended() {
        return this.target != null && this.target.type != TargetType.UNKNOWN;
    }

    /**
     * @return the target for this extended annotation
     */
    public @Nullable AnnotationTarget getTarget() {
        return this.target;
    }

    /**
     * Determines whether or not the annotation is on a raw type.
     *
     * @return true if the annotation is on a raw type, false if it's on a
     *         type argument (generic or array)
     */
    public boolean isRaw() {
        assert this.location == AnnotationLocation.RAW;
        if (this.target == null) /*nnbug*/
            return true;
        return !this.target.type.hasLocation();
    }

    /**
     * @return the location of the annotation
     */
    public AnnotationLocation getLocation() {
        return this.location;
    }

    /**
     * @return the extended annotation's class
     */
    public Class<? extends Annotation> getAnnotationClass() {
        return this.annotation;
    }

    /**
     * @return the annotation's type
     */
    public TypeMirror getType() {
        return this.mirror.getAnnotationType();
    }

    /**
     * @see AnnotationMirror#getElementValues()
     * @return the values of the annotation's arguments (with defaults)
     */
    public Map<? extends ExecutableElement, ? extends @Nullable AnnotationValue> getValues() {
        return elements.getElementValuesWithDefaults(mirror); // FIXME: checker bug (wildcards)
    }

    /**
     * Creates a list of the annotations from the given element; annotations
     * that were manually added are not included.
     *
     * @param element
     *            the {@link Element} to get annotations from
     * @param env the current processing environment
     *
     * @return the element's annotations as {@link InternalAnnotation}s
     */
    static List<InternalAnnotation> fromElement(Element element, ProcessingEnvironment env) {
        List<InternalAnnotation> annotations = new LinkedList<InternalAnnotation>();

        if (element == null)
            return annotations;

        if (!(element instanceof Symbol))
            throw new RuntimeException("element not a symbol");

        Symbol s = (Symbol) element;

        assert s.attributes_field != null;

        for (AnnotationMirror a : s.attributes_field) {
            InternalAnnotation ia = new InternalAnnotation(a, env);
            @Nullable AnnotationTarget target = ia.getTarget();
            // TODO: verify for other target types
            if (target != null && target.type == TargetType.FIELD_GENERIC_OR_ARRAY &&
                    element.getKind() == ElementKind.CLASS)
                continue;
            annotations.add(ia);
        }

        return annotations;
    }

    /**
     * Creates a list of the annotations in the
     * given {@link Element} that reference the given {@link Tree}. Useful for
     * annotations that are located in the body of the enclosing method.
     *
     * @param e
     *            the source of the annotations
     * @param tree
     *            the {@link Tree} that the annotations in {@code e} belong to
     * @param env the current processing environment
     *
     * @return the annotations on {@code e} that refer to {@code tree}
     */
    static List<InternalAnnotation> fromElement(Element e,
            Tree tree, ProcessingEnvironment env) {

        List<InternalAnnotation> methodAnnos = fromElement(e, env);
        List<InternalAnnotation> thisAnnos = new LinkedList<InternalAnnotation>();

        for (InternalAnnotation a : methodAnnos) {
            @Nullable AnnotationTarget target = a.getTarget();
            if (target == null || (target != null && target.ref == null)) // FIXME: flow workaround
                continue;
            assert target != null; // FIXME: flow workaround
            if (InternalUtils.refersTo(target.ref, tree))
                thisAnnos.add(a);
        }

        return thisAnnos;
    }

    /**
     * For each method parameter in the given element, gathers the annotations
     * on that parameter into a list and returns these lists.
     *
     * @param element
     *            the (symbol of the) method from which parameter annotations
     *            will be extracted
     * @param env the current processing environment
     * @return a list of the lists of the {@InternalAnnotation}s for each
     *         parameter in the method given by {@code element}
     */
    public static List<List<InternalAnnotation>> forMethodParameters(
            Element element, ProcessingEnvironment env) {

        List<List<InternalAnnotation>> annotations = new LinkedList<List<InternalAnnotation>>();

        if (element instanceof MethodSymbol)
            for (VarSymbol vs : ((MethodSymbol) element).params())
                annotations.add(fromElement(vs, env));

        return annotations;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof AnnotationData) {
            AnnotationData s = (AnnotationData)o;
            return types.isSameType(getType(), s.getType())
                    && getLocation().equals(s.getLocation())
                    && getValues().equals(s.getValues());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 17 * getType().toString().hashCode() + 19 * getLocation().hashCode() 
                + 31 * getValues().hashCode();
    }

    @Override
    public String toString() {
        return String.format("[%s@%s %s]",
                this.getType(),
                this.getLocation(),
                this.getValues());
    }
}
