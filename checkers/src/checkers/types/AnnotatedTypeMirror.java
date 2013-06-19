package checkers.types;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import checkers.quals.InvisibleQualifier;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;
import checkers.source.SourceChecker;
import checkers.types.visitors.AnnotatedTypeScanner;
import checkers.types.visitors.AnnotatedTypeVisitor;
import checkers.types.visitors.SimpleAnnotatedTypeVisitor;
import checkers.util.AnnotatedTypes;
import checkers.util.AnnotationUtils;
import checkers.util.ElementUtils;
import checkers.util.TreeUtils;
/*>>>
import checkers.nullness.quals.NonNull;
*/

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;

/**
 * Represents an annotated type in the Java programming language.
 * Types include primitive types, declared types (class and interface types),
 * array types, type variables, and the null type.
 * Also represented are wildcard type arguments,
 * the signature and return types of executables,
 * and pseudo-types corresponding to packages and to the keyword {@code void}.
 *
 * <p> Types should be compared using the utility methods in {@link
 * AnnotatedTypes}.  There is no guarantee that any particular type will always
 * be represented by the same object.
 *
 * <p> To implement operations based on the class of an {@code
 * AnnotatedTypeMirror} object, either
 * use a visitor or use the result of the {@link #getKind()} method.
 *
 * @see TypeMirror
 */
public abstract class AnnotatedTypeMirror {

    /**
     * Creates the appropriate AnnotatedTypeMirror specific wrapper for the
     * provided type
     *
     * @param type
     * @param env
     * @param atypeFactory
     * @return [to document]
     */
    public static AnnotatedTypeMirror createType(TypeMirror type,
        AnnotatedTypeFactory atypeFactory) {
        if (type == null)
            return null;
        if (replacer == null)
            replacer = new Replacer(atypeFactory.types);

        type = ((com.sun.tools.javac.code.Type)type).unannotatedType();

        switch (type.getKind()) {
            case ARRAY:
                return new AnnotatedArrayType((ArrayType) type, atypeFactory);
            case DECLARED:
                return new AnnotatedDeclaredType((DeclaredType) type, atypeFactory);
            case ERROR:
                SourceChecker.errorAbort("AnnotatedTypeMirror.createType: input should type-check already! Found error type: " + type);
                return null; // dead code
            case EXECUTABLE:
                return new AnnotatedExecutableType((ExecutableType) type, atypeFactory);
            case VOID:
            case PACKAGE:
            case NONE:
                return new AnnotatedNoType((NoType) type, atypeFactory);
            case NULL:
                return new AnnotatedNullType((NullType) type, atypeFactory);
            case TYPEVAR:
                return new AnnotatedTypeVariable((TypeVariable) type, atypeFactory);
            case WILDCARD:
                return new AnnotatedWildcardType((WildcardType) type, atypeFactory);
            case INTERSECTION:
                return new AnnotatedIntersectionType((IntersectionType) type, atypeFactory);
            default:
                if (type.getKind().isPrimitive()) {
                    return new AnnotatedPrimitiveType((PrimitiveType) type, atypeFactory);
                }
                SourceChecker.errorAbort("AnnotatedTypeMirror.createType: unidentified type " +
                        type + " (" + type.getKind() + ")");
                return null; // dead code
        }
    }

    /** The factory to use for lazily creating annotated types. */
    protected final AnnotatedTypeFactory atypeFactory;

    /** Actual type wrapped with this AnnotatedTypeMirror **/
    // TODO: instead of shadowing this field in subclasses, add a type parameter?
    protected final TypeMirror actualType;

    /** the Element associated with this instance value, if one exists **/
    // TODO: Clarify, with value not the element of the type.
    // I.e. For 'Integer i;' the element would be for 'i' not 'Integer'
    protected Element element;

    /** The annotations on this type. */
    // AnnotationMirror doesn't override Object.hashCode, .equals, so we use
    // the class name of Annotation instead.
    // Caution: Assumes that a type can have at most one AnnotationMirror for
    // any Annotation type. JSR308 is pushing to have this change.
    protected final Set<AnnotationMirror> annotations = AnnotationUtils.createAnnotationSet();

    /** The explicitly written annotations on this type. */
    // TODO: use this to cache the result once computed? For generic types?
    // protected final Set<AnnotationMirror> explicitannotations = AnnotationUtils.createAnnotationSet();

    // If unique IDs are helpful, add these and the commented lines that use them.
    // private static int uidCounter = 0;
    // public int uid;

    /**
     * Constructor for AnnotatedTypeMirror.
     *
     * @param type  the underlying type
     * @param typeFactory used to create further types and to access
     *     global information (Types, Elements, ...)
     */
    private AnnotatedTypeMirror(TypeMirror type,
            AnnotatedTypeFactory atypeFactory) {
        this.actualType = type;
        assert atypeFactory != null;
        this.atypeFactory = atypeFactory;
        // uid = ++uidCounter;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AnnotatedTypeMirror))
            return false;
        AnnotatedTypeMirror t = (AnnotatedTypeMirror) o;
        if (atypeFactory.types.isSameType(this.actualType, t.actualType)
                && AnnotationUtils.areSame(getAnnotations(), t.getAnnotations()))
            return true;
        return false;
    }

    @Override
    public int hashCode() {
        return this.annotations.toString().hashCode() * 17
            + this.actualType.toString().hashCode() * 13;
    }

    /**
     * Applies a visitor to this type.
     *
     * @param <R>   the return type of the visitor's methods
     * @param <P>   the type of the additional parameter to the visitor's methods
     * @param v the visitor operating on this type
     * @param p additional parameter to the visitor
     * @return  a visitor-specified result
     */
    public abstract <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p);

    /**
     * Returns the {@code kind} of this type
     * @return the kind of this type
     */
    public TypeKind getKind() {
        return actualType.getKind();
    }

    /**
     * Returns the underlying unannotated Java type, which this wraps
     *
     * @return  the underlying type
     */
    public TypeMirror getUnderlyingType() {
        return actualType;
    }

    /**
     * Returns true if an annotation targets this type location.
     *
     * It doesn't account for annotations in deep types (type arguments,
     * array components, etc).
     *
     */
    public boolean isAnnotated() {
        return !annotations.isEmpty();
    }

    /**
     * Returns true if an annotation from the given sub-hierarchy targets this type.
     *
     * It doesn't account for annotations in deep types (type arguments,
     * array components, etc).
     *
     * @param p The qualifier hierarchy to check for.
     * @return True iff an annotation from the same hierarchy as p is present.
     */
    public boolean isAnnotatedInHierarchy(AnnotationMirror p) {
        return getAnnotationInHierarchy(p) != null;
    }

    /**
     * Returns an annotation from the given sub-hierarchy, if such
     * an annotation targets this type; otherwise returns null.
     *
     * It doesn't account for annotations in deep types (type arguments,
     * array components, etc).
     *
     * @param p The qualifier hierarchy to check for.
     * @return An annotation from the same hierarchy as p if present.
     */
    public AnnotationMirror getAnnotationInHierarchy(AnnotationMirror p) {
        AnnotationMirror aliased = p;
        if (!atypeFactory.isSupportedQualifier(aliased)) {
            aliased = atypeFactory.aliasedAnnotation(p);
        }
        if (atypeFactory.isSupportedQualifier(aliased)) {
            QualifierHierarchy qualHier = this.atypeFactory.getQualifierHierarchy();
            AnnotationMirror anno = qualHier.findCorrespondingAnnotation(aliased, annotations);
            if (anno != null) {
                return anno;
            }
        }
        return null;
    }

    /**
     * Returns the annotations on this type.
     *
     * It does not include annotations in deep types (type arguments, array
     * components, etc).
     *
     * @return  a set of the annotations on this
     */
    public Set<AnnotationMirror> getAnnotations() {
        return Collections.unmodifiableSet(annotations);
    }

    /**
     * Returns the "effective" annotations on this type, i.e. the annotations on
     * the type itself, or on the upper/extends bound of a type variable/wildcard.
     *
     * @return  a set of the annotations on this
     */
    public Set<AnnotationMirror> getEffectiveAnnotations() {
        return getAnnotations();
    }

    /**
     * Returns the actual annotation mirror used to annotate this type,
     * whose name equals the passed annotationName if one exists, null otherwise.
     *
     * @param annotationName
     * @return the annotation mirror for annotationName
     */
    public AnnotationMirror getAnnotation(Name annotationName) {
        assert annotationName != null : "Null annotationName in getAnnotation";
        for (AnnotationMirror anno : getAnnotations())
            if (annotationName.equals(AnnotationUtils.annotationName(anno)))
                return anno;
        return null;
    }

    /**
     * Returns the actual annotation mirror used to annotate this type,
     * whose name equals the passed annotationName if one exists, null otherwise.
     *
     * @param anno annotation class
     * @return the annotation mirror for anno
     */
    public AnnotationMirror getAnnotation(Class<? extends Annotation> anno) {
        return getAnnotation(atypeFactory.elements.getName(anno.getCanonicalName()));
    }

    /**
     * Returns the set of explicitly written annotations supported by this checker.
     * This is useful to check the validity of annotations explicitly present on a type,
     * as flow inference might add annotations that were not previously present.
     *
     * @return The set of explicitly written annotations supported by this checker.
     */
    public Set<AnnotationMirror> getExplicitAnnotations() {
        // If the element is null then it's an array. (Or a type argument, etc.?)
        if (this.element == null) {
            // TODO: Add support in the framework to read explicit annotations
            // from arrays (and type arguments, etc.?).
            return AnnotationUtils.createAnnotationSet();
        } else {
            Set<AnnotationMirror> explicitAnnotations = AnnotationUtils.createAnnotationSet();
            List<com.sun.tools.javac.code.Attribute.TypeCompound> typeAnnotations = ((com.sun.tools.javac.code.Symbol) this.element).getRawTypeAttributes();
            // TODO: should we instead try to go to the Checker and use getSupportedTypeQualifiers()?
            Set<Name> validAnnotations = atypeFactory.getQualifierHierarchy().getTypeQualifiers();
            for (com.sun.tools.javac.code.Attribute.TypeCompound explicitAnno : typeAnnotations) {
                for (Name validAnno : validAnnotations) {
                    if (explicitAnno.getAnnotationType().toString().equals(validAnno.toString())) {
                        explicitAnnotations.add(explicitAnno);
                    }
                }
            }
            return explicitAnnotations;
        }
    }

    /**
     * Determines whether this type contains the given annotation.
     * This method considers the annotation's values, that is,
     * if the type is "@A("s") @B(3) Object" a call with
     * "@A("t") or "@A" will return false, whereas a call with
     * "@B(3)" will return true.
     *
     * In contrast to {@link #hasAnnotationRelaxed(AnnotationMirror)}
     * this method also compares annotation values.
     *
     * @param a the annotation to check for
     * @return true iff the type contains the annotation {@code a}
     *
     * @see #hasAnnotationRelaxed(AnnotationMirror)
     */
    public boolean hasAnnotation(AnnotationMirror a) {
        return AnnotationUtils.containsSame(getAnnotations(), a);
    }

    /**
     * Determines whether this type contains the given annotation.
     *
     * @param a the annotation name to check for
     * @return true iff the type contains the annotation {@code a}
     *
     * @see #hasAnnotationRelaxed(AnnotationMirror)
     */
    public boolean hasAnnotation(Name a) {
        return getAnnotation(a) != null;
    }

    /**
     * Determines whether this type contains an annotation with the same
     * annotation type as a particular annotation. This method does not
     * consider an annotation's values.
     *
     * @param a the class of annotation to check for
     * @return true iff the type contains an annotation with the same type as
     * the annotation given by {@code a}
     */
    public boolean hasAnnotation(Class<? extends Annotation> a) {
        return getAnnotation(a) != null;
    }
    // TODO: do we need an "effective" version of the above hasAnnotation?

    /**
     * A version of hasAnnotation that considers annotations on the
     * upper bound of wildcards and type variables.
     *
     * @see #hasAnnotation(AnnotationMirror)
     */
    public boolean hasEffectiveAnnotation(AnnotationMirror a) {
        return AnnotationUtils.containsSame(getEffectiveAnnotations(), a);
    }

    /**
     * Determines whether this type contains the given annotation
     * explicitly written at declaration. This method considers the
     * annotation's values, that is, if the type is
     * "@A("s") @B(3) Object" a call with "@A("t") or "@A" will
     * return false, whereas a call with "@B(3)" will return true.
     *
     * In contrast to {@link #hasExplicitAnnotationRelaxed(AnnotationMirror)}
     * this method also compares annotation values.
     *
     * @param a the annotation to check for
     * @return true iff the annotation {@code a} is explicitly written
     * on the type
     *
     * @see #hasExplicitAnnotationRelaxed(AnnotationMirror)
     */
    public boolean hasExplicitAnnotation(AnnotationMirror a) {
        return AnnotationUtils.containsSame(getExplicitAnnotations(), a);
    }

    /**
     * Determines whether this type contains an annotation with the same
     * annotation type as a particular annotation. This method does not
     * consider an annotation's values, that is,
     * if the type is "@A("s") @B(3) Object" a call with
     * "@A("t"), "@A", or "@B" will return true.
     *
     * @param a the annotation to check for
     * @return true iff the type contains an annotation with the same type as
     * the annotation given by {@code a}
     *
     * @see #hasAnnotation(AnnotationMirror)
     */
    public boolean hasAnnotationRelaxed(AnnotationMirror a) {
        return AnnotationUtils.containsSameIgnoringValues(getAnnotations(), a);
    }

    /**
     * A version of hasAnnotationRelaxed that considers annotations on the
     * upper bound of wildcards and type variables.
     *
     * @see #hasAnnotationRelaxed(AnnotationMirror)
     */
    public boolean hasEffectiveAnnotationRelaxed(AnnotationMirror a) {
        return AnnotationUtils.containsSameIgnoringValues(getEffectiveAnnotations(), a);
    }

    /**
     * A version of hasAnnotationRelaxed that only considers annotations that
     * are explicitly written on the type.
     *
     * @see #hasAnnotationRelaxed(AnnotationMirror)
     */
    public boolean hasExplicitAnnotationRelaxed(AnnotationMirror a) {
        return AnnotationUtils.containsSameIgnoringValues(getExplicitAnnotations(), a);
    }

    /**
     * Determines whether this type contains an explictly written annotation
     * with the same annotation type as a particular annotation. This method
     * does not consider an annotation's values.
     *
     * @param a the class of annotation to check for
     * @return true iff the type contains an explicitly written annotation
     * with the same type as the annotation given by {@code a}
     */
    public boolean hasExplicitAnnotation(Class<? extends Annotation> a) {
        return AnnotationUtils.containsSameIgnoringValues(getExplicitAnnotations(), getAnnotation(a));
    }

    /**
     * Adds an annotation to this type. If the annotation does not have the
     * {@link TypeQualifier} meta-annotation, this method has no effect.
     *
     * @param a the annotation to add
     */
    public void addAnnotation(AnnotationMirror a) {
        if (a == null) {
            SourceChecker.errorAbort("AnnotatedTypeMirror.addAnnotation: null is not a valid annotation.");
        }
        if (atypeFactory.isSupportedQualifier(a)) {
            this.annotations.add(a);
        } else {
            AnnotationMirror aliased = atypeFactory.aliasedAnnotation(a);
            if (atypeFactory.isSupportedQualifier(aliased)) {
                addAnnotation(aliased);
            }
        }
    }

    /**
     * Adds an annotation to this type, removing any existing annotation from the
     * same qualifier hierarchy first.
     *
     * @param a the annotation to add
     */
    public void replaceAnnotation(AnnotationMirror a) {
        this.removeAnnotationInHierarchy(a);
        this.addAnnotation(a);
    }

    /**
     * Adds an annotation to this type. If the annotation does not have the
     * {@link TypeQualifier} meta-annotation, this method has no effect.
     *
     * @param a the class of the annotation to add
     */
    public void addAnnotation(Class<? extends Annotation> a) {
        AnnotationMirror anno = AnnotationUtils.fromClass(atypeFactory.elements, a);
        addAnnotation(anno);
    }

    /**
     * Adds multiple annotations to this type.
     *
     * @param annotations the annotations to add
     */
    public void addAnnotations(Iterable<? extends AnnotationMirror> annotations) {
        for (AnnotationMirror a : annotations) {
            this.addAnnotation(a);
        }
    }

    /**
     * Adds multiple annotations to this type, removing any existing annotations from the
     * same qualifier hierarchy first.
     *
     * @param replAnnos the annotations to replace
     */
    public void replaceAnnotations(Iterable<? extends AnnotationMirror> replAnnos) {
        for (AnnotationMirror a : replAnnos) {
            this.removeAnnotationInHierarchy(a);
            this.addAnnotation(a);
        }
    }

    /**
     * Removes an annotation from the type.
     *
     * @param a the annotation to remove
     * @return true if the annotation was removed, false if the type's
     * annotations were unchanged
     */
    public boolean removeAnnotation(AnnotationMirror a) {
        // Going from the AnnotationMirror to its name and then calling
        // getAnnotation ensures that we get the canonical AnnotationMirror that can be
        // removed.
        // TODO: however, this also means that if we are annotated with "@I(1)" and
        // remove "@I(2)" it will be removed. Is this what we want?
        // It's currently necessary for the IGJ and Lock Checkers.
        AnnotationMirror anno = getAnnotation(AnnotationUtils.annotationName(a));
        if (anno != null) {
            return annotations.remove(anno);
        } else {
            return false;
        }
    }

    public boolean removeAnnotation(Class<? extends Annotation> a) {
        AnnotationMirror anno = AnnotationUtils.fromClass(atypeFactory.elements, a);
        if (anno == null || !atypeFactory.isSupportedQualifier(anno)) {
            SourceChecker.errorAbort("AnnotatedTypeMirror.removeAnnotation called with un-supported class: " + a);
        }
        return removeAnnotation(anno);
    }

    /**
     * Remove any annotation that is in the same qualifier hierarchy as the parameter.
     *
     * @param a An annotation from the same qualifier hierarchy
     * @return If an annotation was removed
     */
    public boolean removeAnnotationInHierarchy(AnnotationMirror a) {
        AnnotationMirror prev = this.getAnnotationInHierarchy(a);
        if (prev != null) {
            return this.removeAnnotation(prev);
        }
        return false;
    }

    /**
     * Removes multiple annotations from the type.
     *
     * @param annotations
     *            the annotations to remove
     * @return true if at least one annotation was removed, false if the type's
     *         annotations were unchanged
     */
    public boolean removeAnnotations(Iterable<? extends AnnotationMirror> annotations) {
        boolean changed = false;
        for (AnnotationMirror a : annotations)
            changed |= this.removeAnnotation(a);
        return changed;
    }

    /**
     * Removes all annotations on this type.
     * Make sure to add an annotation again, e.g. Unqualified.
     */
    public void clearAnnotations() {
        annotations.clear();
    }

    public static boolean isUnqualified(AnnotationMirror anno) {
        String aname = Unqualified.class.getCanonicalName();
        return ((TypeElement)anno.getAnnotationType().asElement()).getQualifiedName().contentEquals(aname);
    }

    private static boolean isInvisibleQualified(AnnotationMirror anno) {
        return ((TypeElement)anno.getAnnotationType().asElement()).getAnnotation(InvisibleQualifier.class) != null;
    }

    // A helper method to print annotations separated by a space.
    // Note a final space after a list of annotations to separate from the underlying type.
    protected final static String formatAnnotationString(
            Collection<? extends AnnotationMirror> lst,
            boolean printInvisible) {
        StringBuilder sb = new StringBuilder();
        for (AnnotationMirror obj : lst) {
            if (obj == null) {
                SourceChecker.errorAbort("AnnotatedTypeMirror.formatAnnotationString: found null AnnotationMirror!");
            }
            if (isInvisibleQualified(obj) &&
                    !printInvisible) {
                continue;
            }
            formatAnnotationMirror(obj, sb);
            sb.append(" ");
        }
        return sb.toString();
    }

    // A helper method to output a single AnnotationMirror, without showing full package names.
    protected final static void formatAnnotationMirror(AnnotationMirror am, StringBuilder sb) {
        sb.append("@");
        sb.append(am.getAnnotationType().asElement().getSimpleName());
        Map<? extends ExecutableElement, ? extends AnnotationValue> args = am.getElementValues();
        if (!args.isEmpty()) {
            sb.append("(");
            boolean oneValue = false;
            if (args.size() == 1) {
                Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> first = args.entrySet().iterator().next();
                if (first.getKey().getSimpleName().contentEquals("value")) {
                    formatAnnotationMirrorArg(first.getValue(), sb);
                    oneValue = true;
                }
            }
            if (!oneValue) {
                boolean notfirst = false;
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> arg : args.entrySet()) {
                    if (notfirst) {
                        sb.append(", ");
                    }
                    notfirst = true;
                    sb.append(arg.getKey().getSimpleName() + "=");
                    formatAnnotationMirrorArg(arg.getValue(), sb);
                }
            }
            sb.append(")");
        }
    }

    // A helper method to output a single AnnotationValue, without showing full package names.
    @SuppressWarnings("unchecked")
    protected final static void formatAnnotationMirrorArg(AnnotationValue av, StringBuilder sb) {
        Object val = av.getValue();
        if (List.class.isAssignableFrom(val.getClass())) {
            List<AnnotationValue> vallist = (List<AnnotationValue>) val;
            if (vallist.size() == 1) {
                formatAnnotationMirrorArg(vallist.get(0), sb);
            } else {
                sb.append('{');
                boolean notfirst = false;
                for (AnnotationValue nav : vallist) {
                    if (notfirst) {
                        sb.append(", ");
                    }
                    notfirst = true;
                    formatAnnotationMirrorArg(nav, sb);
                }
                sb.append('}');
            }
        } else if (VariableElement.class.isAssignableFrom(val.getClass())) {
            VariableElement ve = (VariableElement) val;
            sb.append(ve.getEnclosingElement().getSimpleName() + "." + ve.getSimpleName());
        } else {
            sb.append(av.toString());
        }
    }

    @Override
    public final String toString() {
        // Also see
        // checkers.basetype.BaseTypeVisitor.commonAssignmentCheck(AnnotatedTypeMirror, AnnotatedTypeMirror, Tree, String)
        return toString(atypeFactory.processingEnv.getOptions().containsKey("printAllQualifiers"));
    }

    /**
     * A version of toString() that optionally outputs all type qualifiers,
     * including @InvisibleQualifier's.
     *
     * @param invisible Whether to always output invisible qualifiers.
     * @return A string representation of the current type containing all qualifiers.
     */
    public String toString(boolean invisible) {
        return formatAnnotationString(getAnnotations(), invisible)
                + this.actualType;
    }

    public String toStringDebug() {
        return toString(true) + " " + getClass().getSimpleName(); // + "#" + uid;
    }

    /**
     * Sets the Element associated with the value of this type, if one exists
     *
     * @param elem the element of this type value
     */
    void setElement(Element elem) {
        this.element = elem;
    }

    /**
     * Returns the element associated with the value the type represent, if any.
     * I.e. For 'Integer i;' the element would be for 'i' not 'Integer'
     *
     * @return  the {@code Element} of the value of this type, if one exists
     */
    public Element getElement() {
        return element;
    }

    /**
     * Returns the erasure type of the this type, according to JLS
     * specifications.
     *
     * @return  the erasure of this
     */
    public AnnotatedTypeMirror getErased() {
        return this;
    }

    /**
     * Copy the fields on this type onto the passed type.
     * This method needs to be overridden by any subclass of
     * {@code AnnotatedTypeMirror}.
     *
     * TODO: None of the subtypes in this compilation unit override this method, however.
     * Is this documentation inconsistent? Or should we add these implementations?
     * The separation between copyFields and getCopy is unclear.
     *
     * @param type  an empty type where fields of this are copied to
     * @param annotation whether annotations are copied or not
     */
    protected AnnotatedTypeMirror copyFields(AnnotatedTypeMirror type, boolean annotation) {
        type.setElement(getElement());
        if (annotation)
            type.addAnnotations(annotations);
        return type;
    }

    /**
     * Returns a shallow copy of this type.
     *
     * @param copyAnnotations
     *            whether copy should have annotations
     */
    public abstract AnnotatedTypeMirror getCopy(boolean copyAnnotations);

    protected static AnnotatedDeclaredType createTypeOfObject(AnnotatedTypeFactory atypeFactory) {
        AnnotatedDeclaredType objectType =
        atypeFactory.fromElement(
                atypeFactory.elements.getTypeElement(
                        Object.class.getCanonicalName()));
        return objectType;
    }

    /**
     * Return a copy of this, with the given substitutions performed.
     *
     * @param mappings
     */
    public AnnotatedTypeMirror substitute(
            Map<? extends AnnotatedTypeMirror,
                    ? extends AnnotatedTypeMirror> mappings) {
        if (mappings.containsKey(this)) {
            return mappings.get(this).getCopy(true);
        }
        return this.getCopy(true);
    }

    public static interface AnnotatedReferenceType {
        // No members.
    }

    /**
     * Represents a declared type (whether class or interface).
     */
    public static class AnnotatedDeclaredType extends AnnotatedTypeMirror
    implements AnnotatedReferenceType {

        /** Parametrized Type Arguments **/
        protected List<AnnotatedTypeMirror> typeArgs;

        protected boolean isGeneric = false;

        /** The enclosing Type **/
        protected AnnotatedDeclaredType enclosingType;

        protected final DeclaredType actualType;

        protected List<AnnotatedDeclaredType> supertypes;

        /**
         * Constructor for this type
         *
         * @param type  underlying kind of this type
         * @param atypeFactory TODO
         */
        private AnnotatedDeclaredType(DeclaredType type,
                AnnotatedTypeFactory atypeFactory) {
            super(type, atypeFactory);
            this.actualType = type;
            DeclaredType elem = (DeclaredType)((TypeElement)type.asElement()).asType();
            isGeneric = !elem.getTypeArguments().isEmpty();
            this.supertypes = null;
            TypeKind enclKind = type.getEnclosingType().getKind();
            if (enclKind == TypeKind.DECLARED) {
                this.enclosingType = (AnnotatedDeclaredType) createType(type.getEnclosingType(), atypeFactory);
            } else if (enclKind != TypeKind.NONE) {
                SourceChecker.errorAbort("AnnotatedDeclaredType: unsupported enclosing type: " +
                        type.getEnclosingType() + " (" + enclKind + ")");
            }
        }

        @Override
        public String toString(boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            final Element typeElt = this.getUnderlyingType().asElement();
            String smpl = typeElt.getSimpleName().toString();
            if (smpl.isEmpty()) {
                // For anonymous classes smpl is empty - toString
                // of the element is more useful.
                smpl = typeElt.toString();
            }
            sb.append(formatAnnotationString(getAnnotations(), printInvisible));
            sb.append(smpl);
            if (!this.getTypeArguments().isEmpty()) {
                sb.append("<");

                boolean isFirst = true;
                for (AnnotatedTypeMirror typeArg : getTypeArguments()) {
                    if (!isFirst) sb.append(", ");
                    sb.append(typeArg.toString(printInvisible));
                    isFirst = false;
                }
                sb.append(">");
            }
            return sb.toString();
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitDeclared(this, p);
        }

        /**
         * Sets the type arguments on this type
         * @param ts the type arguments
         */
        // WMD
        public
        void setTypeArguments(List<? extends AnnotatedTypeMirror> ts) {
            typeArgs = Collections.unmodifiableList(new ArrayList<AnnotatedTypeMirror>(ts));
        }

        /**
         * @return the type argument for this type
         */
        public List<AnnotatedTypeMirror> getTypeArguments() {
            if (typeArgs == null) {
                typeArgs = new ArrayList<AnnotatedTypeMirror>();
                if (!actualType.getTypeArguments().isEmpty()) { // lazy init
                    for (TypeMirror t : actualType.getTypeArguments())
                        typeArgs.add(createType(t, atypeFactory));
                }
                typeArgs = Collections.unmodifiableList(typeArgs);
            }
            return typeArgs;
        }

        /**
         * Returns true if the type is generic, even if the type is erased
         * or used as a RAW type.
         *
         * @return true iff the type is generic
         */
        public boolean isGeneric() {
            return isGeneric;
        }

        /**
         * Returns true if the type is a generic type and the type declaration
         * is parameterized.  Otherwise, it returns false, in cases where a
         * type is a raw type or a non-generic type.
         *
         * Note: Even if the type is a raw type, the framework actually adds
         * type arguments to the type.
         */
        public boolean isParameterized() {
            return !getTypeArguments().isEmpty()
                && !getUnderlyingType().getTypeArguments().isEmpty();
        }

        @Override
        public DeclaredType getUnderlyingType() {
            return actualType;
        }

        void setDirectSuperTypes(List<AnnotatedDeclaredType> supertypes) {
            this.supertypes = new ArrayList<AnnotatedDeclaredType>(supertypes);
        }

        @Override
        public List<AnnotatedDeclaredType> directSuperTypes() {
            if (supertypes == null) {
                supertypes = directSuperTypes(this);
            }
            return Collections.unmodifiableList(supertypes);
        }

        /*
         * Return the direct super types field without lazy initialization,
         * to prevent infinite recursion in IGJATF.postDirectSuperTypes.
         * TODO: find a nicer way, see the single caller in QualifierDefaults
         * for comment.
         */
        public List<AnnotatedDeclaredType> directSuperTypesField() {
            return supertypes;
        }

        @Override
        public AnnotatedDeclaredType getCopy(boolean copyAnnotations) {
            AnnotatedDeclaredType type =
                new AnnotatedDeclaredType(getUnderlyingType(), atypeFactory);
            copyFields(type, copyAnnotations);
            type.setEnclosingType(getEnclosingType());
            type.setTypeArguments(getTypeArguments());
            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                    ? extends AnnotatedTypeMirror> mappings) {
            if (mappings.containsKey(this))
                return mappings.get(this);

            AnnotatedDeclaredType type = getCopy(true);

            Map<AnnotatedTypeMirror, AnnotatedTypeMirror> newMappings =
                    new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>(mappings);
            newMappings.put(this, type);

            List<AnnotatedTypeMirror> typeArgs = new ArrayList<AnnotatedTypeMirror>();
            for (AnnotatedTypeMirror t : getTypeArguments())
                typeArgs.add(t.substitute(newMappings));
            type.setTypeArguments(typeArgs);

            return type;
        }

        @Override
        public AnnotatedDeclaredType getErased() {
            // 1. |G<T_1, ..., T_n>| = |G|
            // 2. |T.C| = |T|.C
            if (!getTypeArguments().isEmpty()) {
                Types types = atypeFactory.types;
                // Handle case 1.
                AnnotatedDeclaredType rType =
                    (AnnotatedDeclaredType)AnnotatedTypeMirror.createType(
                            types.erasure(actualType),
                            atypeFactory);
                rType.addAnnotations(getAnnotations());
                rType.setElement(element);
                rType.setTypeArguments(Collections.<AnnotatedTypeMirror> emptyList());
                return rType.getErased();
            } else if ((getEnclosingType() != null) &&
                       (getEnclosingType().getKind() != TypeKind.NONE)) {
                // Handle case 2
                // TODO: Test this
                AnnotatedDeclaredType rType = getCopy(true);
                AnnotatedDeclaredType et = getEnclosingType();
                rType.setEnclosingType(et.getErased());
                return rType;
            } else {
                return this;
            }
        }

        /* Using this equals method resulted in an infinite recursion
         * with type variables. TODO: Keep track of visited type variables?
        @Override
        public boolean equals(Object o) {
            boolean res = super.equals(o);

            if (res && (o instanceof AnnotatedDeclaredType)) {
                AnnotatedDeclaredType dt = (AnnotatedDeclaredType) o;

                List<AnnotatedTypeMirror> mytas = this.getTypeArguments();
                List<AnnotatedTypeMirror> othertas = dt.getTypeArguments();
                for (int i = 0; i < mytas.size(); ++i) {
                    if (!mytas.get(i).equals(othertas.get(i))) {
                        System.out.println("in AnnotatedDeclaredType; this: " + this + " and " + o);
                        res = false;
                        break;
                    }
                }
            }
            return res;
        }
        */

        /**
         * Sets the enclosing type
         *
         * @param enclosingType
         */
        void setEnclosingType(AnnotatedDeclaredType enclosingType) {
            this.enclosingType = enclosingType;
        }

        /**
         * Returns the enclosing type, as in the type of {@code A} in the type
         * {@code A.B}.
         *
         * @return enclosingType the enclosing type
         */
        public AnnotatedDeclaredType getEnclosingType() {
            return enclosingType;
        }
    }

    /**
     * Represents a type of an executable. An executable is a method, constructor, or initializer.
     */
    public static class AnnotatedExecutableType extends AnnotatedTypeMirror {

        private final ExecutableType actualType;

        private AnnotatedExecutableType(ExecutableType type,
                AnnotatedTypeFactory factory) {
            super(type, factory);
            this.actualType = type;
        }

        final private List<AnnotatedTypeMirror> paramTypes =
            new ArrayList<AnnotatedTypeMirror>();
        private AnnotatedDeclaredType receiverType;
        private AnnotatedTypeMirror returnType;
        final private List<AnnotatedTypeMirror> throwsTypes =
            new ArrayList<AnnotatedTypeMirror>();
        final private List<AnnotatedTypeVariable> typeVarTypes =
            new ArrayList<AnnotatedTypeVariable>();

        /**
         * @return true if this type represents a varargs method
         */
        public boolean isVarArgs() {
            if (this.element instanceof ExecutableElement)
                return ((ExecutableElement)this.element).isVarArgs();
            return false;
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitExecutable(this, p);
        }

        @Override
        public ExecutableType getUnderlyingType() {
            return this.actualType;
        }

        /**
         * Sets the parameter types of this executable type
         * @param params the parameter types
         */
        void setParameterTypes(
                List<? extends AnnotatedTypeMirror> params) {
            paramTypes.clear();
            paramTypes.addAll(params);
        }

        /**
         * @return the parameter types of this executable type
         */
        public List<AnnotatedTypeMirror> getParameterTypes() {
            if (paramTypes.isEmpty()
                    && !actualType.getParameterTypes().isEmpty()) { // lazy init
                for (TypeMirror t : actualType.getParameterTypes())
                    paramTypes.add(createType(t, atypeFactory));
            }
            return Collections.unmodifiableList(paramTypes);
        }

        /**
         * Sets the return type of this executable type
         * @param returnType    the return type
         */
        void setReturnType(AnnotatedTypeMirror returnType) {
            this.returnType = returnType;
        }

        /**
         * @return the return type of this executable type
         */
        public AnnotatedTypeMirror getReturnType() {
            if (returnType == null
                    && actualType.getReturnType() != null) // lazy init
                returnType = createType(
                        actualType.getReturnType(), atypeFactory);
            return returnType;
        }

        /**
         * Sets the receiver type on this executable type
         * @param receiverType the receiver type
         */
        void setReceiverType(AnnotatedDeclaredType receiverType) {
            this.receiverType = receiverType;
        }

        /**
         * @return the receiver type of this executable type
         */
        public AnnotatedDeclaredType getReceiverType() {
            if (receiverType == null) {
                TypeElement encl = ElementUtils.enclosingClass(getElement());
                AnnotatedTypeMirror type = createType(encl.asType(), atypeFactory);
                assert type instanceof AnnotatedDeclaredType;
                receiverType = (AnnotatedDeclaredType)type;
            }
            return receiverType;
        }

        /**
         * Sets the thrown types of this executable type
         *
         * @param thrownTypes the thrown types
         */
        void setThrownTypes(
                List<? extends AnnotatedTypeMirror> thrownTypes) {
            this.throwsTypes.clear();
            this.throwsTypes.addAll(thrownTypes);
        }

        /**
         * @return the thrown types of this executable type
         */
        public List<AnnotatedTypeMirror> getThrownTypes() {
            if (throwsTypes.isEmpty()
                    && !actualType.getThrownTypes().isEmpty()) { // lazy init
                for (TypeMirror t : actualType.getThrownTypes())
                    throwsTypes.add(createType(t, atypeFactory));
            }
            return Collections.unmodifiableList(throwsTypes);
        }

        /**
         * Sets the type variables associated with this executable type
         *
         * @param types the type variables of this executable type
         */
        void setTypeVariables(List<AnnotatedTypeVariable> types) {
            typeVarTypes.clear();
            typeVarTypes.addAll(types);
        }

        /**
         * @return the type variables of this executable type, if any
         */
        public List<AnnotatedTypeVariable> getTypeVariables() {
            if (typeVarTypes.isEmpty()
                    && !actualType.getTypeVariables().isEmpty()) { // lazy init
                for (TypeMirror t : actualType.getTypeVariables()) {
                    typeVarTypes.add((AnnotatedTypeVariable)createType(
                            t, atypeFactory));
                }
            }
            return Collections.unmodifiableList(typeVarTypes);
        }

        @Override
        public AnnotatedExecutableType getCopy(boolean copyAnnotations) {
            AnnotatedExecutableType type =
                new AnnotatedExecutableType(getUnderlyingType(), atypeFactory);
            copyFields(type, copyAnnotations);
            type.setParameterTypes(getParameterTypes());
            type.setReceiverType(getReceiverType());
            type.setReturnType(getReturnType());
            type.setThrownTypes(getThrownTypes());
            type.setTypeVariables(getTypeVariables());

            return type;
        }

        @Override
        public /*@NonNull*/ ExecutableElement getElement() {
            return (ExecutableElement) super.getElement();
        }

        @Override
        public AnnotatedExecutableType getErased() {
            Types types = atypeFactory.types;
            AnnotatedExecutableType type =
                new AnnotatedExecutableType(
                        (ExecutableType) types.erasure(getUnderlyingType()),
                        atypeFactory);
            copyFields(type, true);
            type.setParameterTypes(erasureList(getParameterTypes()));
            type.setReceiverType(getReceiverType().getErased());
            type.setReturnType(getReturnType().getErased());
            type.setThrownTypes(erasureList(getThrownTypes()));

            return type;
        }

        private List<AnnotatedTypeMirror> erasureList(List<? extends AnnotatedTypeMirror> lst) {
            List<AnnotatedTypeMirror> erased = new ArrayList<AnnotatedTypeMirror>();
            for (AnnotatedTypeMirror t : lst)
                erased.add(t.getErased());
            return erased;
        }

        @Override
        public AnnotatedExecutableType substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            // Shouldn't substitute for methods!
            AnnotatedExecutableType type = getCopy(true);

            // Params
            {
                List<AnnotatedTypeMirror> params = new ArrayList<AnnotatedTypeMirror>();
                for (AnnotatedTypeMirror t : getParameterTypes()) {
                    params.add(t.substitute(mappings));
                }
                type.setParameterTypes(params);
            }

            if (getReceiverType() != null)
                type.setReceiverType((AnnotatedDeclaredType)getReceiverType().substitute(mappings));

            type.setReturnType(getReturnType().substitute(mappings));

            // Throws
            {
                List<AnnotatedTypeMirror> throwns = new ArrayList<AnnotatedTypeMirror>();
                for (AnnotatedTypeMirror t : getThrownTypes()) {
                    throwns.add(t.substitute(mappings));
                }
                type.setThrownTypes(throwns);
            }

            // Method type variables
            {
                List<AnnotatedTypeVariable> mtvs = new ArrayList<AnnotatedTypeVariable>();
                for (AnnotatedTypeVariable t : getTypeVariables()) {
                    // Substitute upper and lower bound of the type variable.
                    AnnotatedTypeVariable newtv = AnnotatedTypes.deepCopy(t);
                    AnnotatedTypeMirror bnd = newtv.getUpperBoundField();
                    if (bnd != null) {
                        bnd = bnd.substitute(mappings);
                        newtv.setUpperBound(bnd);
                    }
                    bnd = newtv.getLowerBoundField();
                    if (bnd != null) {
                        bnd = bnd.substitute(mappings);
                        newtv.setLowerBound(bnd);
                    }
                    mtvs.add(newtv);
                }
                type.setTypeVariables(mtvs);
            }

            return type;
        }

        @Override
        public String toString(boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            if (!getTypeVariables().isEmpty()) {
                sb.append('<');
                for (AnnotatedTypeVariable atv : getTypeVariables()) {
                    sb.append(atv.toString(printInvisible));
                }
                sb.append("> ");
            }
            sb.append(getReturnType().toString(printInvisible));
            sb.append(' ');
            sb.append(this.getElement().getSimpleName());
            sb.append('(');
            sb.append(getReceiverType().toString(printInvisible));
            sb.append(" this");
            if (!getParameterTypes().isEmpty()) {
                int p = 0;
                for (AnnotatedTypeMirror atm : getParameterTypes()) {
                    sb.append(", ");
                    sb.append(atm.toString(printInvisible));
                    // Output some parameter names to make it look more like a method.
                    // TODO: go to the element and look up real parameter names, maybe.
                    sb.append(" p");
                    sb.append(p++);
                }
            }
            sb.append(')');
            if (!getThrownTypes().isEmpty()) {
                sb.append(" throws ");
                for (AnnotatedTypeMirror atm : getThrownTypes()) {
                    sb.append(atm.toString(printInvisible));
                }
            }
            return sb.toString();
        }
    }

    /**
     * Represents Array types in java. A multidimensional array type is
     * represented as an array type whose component type is also an
     * array type.
     */
    public static class AnnotatedArrayType extends AnnotatedTypeMirror
    implements AnnotatedReferenceType {

        private final ArrayType actualType;

        private AnnotatedArrayType(ArrayType type,
                AnnotatedTypeFactory factory) {
            super(type, factory);
            this.actualType = type;
        }

        /** The component type of this array type */
        private AnnotatedTypeMirror componentType;

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitArray(this, p);
        }

        @Override
        public ArrayType getUnderlyingType() {
            return this.actualType;
        }

        /**
         * Sets the component type of this array
         *
         * @param type the component type
         */
        // WMD
        public
        void setComponentType(AnnotatedTypeMirror type) {
            this.componentType = type;
        }

        /**
         * @return the component type of this array
         */
        public AnnotatedTypeMirror getComponentType() {
            if (componentType == null) // lazy init
                setComponentType(createType(
                        actualType.getComponentType(), atypeFactory));
            return componentType;
        }


        @Override
        public AnnotatedArrayType getCopy(boolean copyAnnotations) {
            AnnotatedArrayType type = new AnnotatedArrayType(actualType, atypeFactory);
            copyFields(type, copyAnnotations);
            type.setComponentType(getComponentType());
            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            if (mappings.containsKey(this))
                return mappings.get(this);

            AnnotatedArrayType type = getCopy(true);
            type.setComponentType(getComponentType().substitute(mappings));
            return type;
        }

        @Override
        public AnnotatedArrayType getErased() {
            // | T[ ] | = |T| [ ]
            AnnotatedArrayType at = getCopy(true);
            AnnotatedTypeMirror ct = at.getComponentType().getErased();
            at.setComponentType(ct);
            return at;

        }

        public String toStringAsCanonical(boolean printInvisible) {
            StringBuilder sb = new StringBuilder();

            AnnotatedArrayType array = this;
            AnnotatedTypeMirror component;
            while (true) {
                component = array.getComponentType();
                if (array.getAnnotations().size() > 0) {
                    sb.append(' ');
                    sb.append(formatAnnotationString(array.getAnnotations(), printInvisible));
                }
                sb.append("[]");
                if (!(component instanceof AnnotatedArrayType)) {
                    sb.insert(0, component.toString(printInvisible));
                    break;
                }
                array = (AnnotatedArrayType) component;
            }
            return sb.toString();
        }

        @Override
        public String toString(boolean printInvisible) {
            return toStringAsCanonical(printInvisible);
        }
    }

    /**
     * Represents a type variable. A type variable may be explicitly declared by
     * a type parameter of a type, method, or constructor. A type variable may
     * also be declared implicitly, as by the capture conversion of a wildcard
     * type argument (see chapter 5 of The Java Language Specification, Third
     * Edition).
     *
     */
    public static class AnnotatedTypeVariable extends AnnotatedTypeMirror
    implements AnnotatedReferenceType {

        private final TypeVariable actualType;

        private AnnotatedTypeVariable(TypeVariable type,
                AnnotatedTypeFactory factory) {
            super(type, factory);
            this.actualType = type;
        }

        /** The lower bound of the type variable. **/
        private AnnotatedTypeMirror lowerBound;

        /** The upper bound of the type variable. **/
        private AnnotatedTypeMirror upperBound;

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitTypeVariable(this, p);
        }

        @Override
        public TypeVariable getUnderlyingType() {
            return this.actualType;
        }

        /**
         * Set the lower bound of this variable type
         *
         * Returns the lower bound of this type variable. While a type
         * parameter cannot include an explicit lower bound declaration,
         * capture conversion can produce a type variable with a non-trivial
         * lower bound. Type variables otherwise have a lower bound of
         * NullType.
         *
         * @param type the lower bound type
         */
        void setLowerBound(AnnotatedTypeMirror type) {
            this.lowerBound = type;
        }

        /**
         * Get the lower bound field directly, bypassing any lazy initialization.
         * This method is necessary to prevent infinite recursions in initialization.
         * In general, prefer getLowerBound.
         *
         * @return the lower bound field.
         */
        public AnnotatedTypeMirror getLowerBoundField() {
            return lowerBound;
        }

        /**
         * @return the lower bound type of this type variable
         * @see #getEffectiveLowerBound
         */
        public AnnotatedTypeMirror getLowerBound() {
            if (lowerBound == null && actualType.getLowerBound() != null) { // lazy init
                setLowerBound(createType(actualType.getLowerBound(), atypeFactory));
            }
            if (lowerBound != null) {
                fixupBoundAnnotations();
            }
            return lowerBound;
        }

        /**
         * @return the effective lower bound:  the lower bound,
         * with annotations on the type variable considered.
        */
        public AnnotatedTypeMirror getEffectiveLowerBound() {
            AnnotatedTypeMirror effbnd = AnnotatedTypes.deepCopy(getLowerBound());
            effbnd.replaceAnnotations(annotations);
            return effbnd;
        }


        // If the lower bound was not present in actualType, then its
        // annotation was defaulted from the AnnotatedTypeFactory.  If the
        // lower bound annotation is a supertype of the upper bound
        // annotation, then the type is ill-formed.  In that case, change
        // the defaulted lower bound to be consistent with the
        // explicitly-written upper bound.
        //
        // As a concrete example, if the default annotation is @Nullable,
        // then the type "X extends @NonNull Y" should not be converted
        // into "X extends @NonNull Y super @Nullable bottomtype" but be
        // converted into "X extends @NonNull Y super @NonNull bottomtype".
        //
        // In addition, ensure consistency of annotations on type variables
        // and the upper bound. Assume class C<X extends @Nullable Object>.
        // The type of "@Nullable X" has to be "@Nullable X extends @Nullable Object",
        // because otherwise the annotations are inconsistent.
        private void fixupBoundAnnotations() {
            if (!annotations.isEmpty() && upperBound != null) {
                // TODO: there seems to be some (for me) unexpected sharing
                // between upper bounds. Without the copying in the next line, test
                // case KeyForChecked fails, because the annotation on the return type
                // type variable changes the upper bound of the parameter type variable.
                // Should such a copy be made somewhere else and for more?
                upperBound = upperBound.getCopy(true);
                // TODO: this direct replacement forbids us to check well-formedness,
                // which is done in
                // checkers.basetype.BaseTypeVisitor.TypeValidator.visitTypeVariable(AnnotatedTypeVariable, Tree)
                // and assumed in nullness test Wellformed.
                // Which behavior do we want?
                upperBound.replaceAnnotations(annotations);
            }
            if (upperBound != null && upperBound.getAnnotations().isEmpty()) {
                // new Throwable().printStackTrace();
                // upperBound.addAnnotations(typeFactory.qualHierarchy.getRootAnnotations());
                // TODO: this should never happen.
            }
            if (actualType.getLowerBound() instanceof NullType &&
                    lowerBound != null && upperBound != null) {
                Set<AnnotationMirror> lAnnos = lowerBound.getEffectiveAnnotations();
                Set<AnnotationMirror> uAnnos = upperBound.getEffectiveAnnotations();
                // System.out.printf("fixup: %s; low: %s; up: %s%n", this, lAnnos, uAnnos);

                if (lAnnos.isEmpty()) {
                    if (!annotations.isEmpty()) {
                        lowerBound.replaceAnnotations(annotations);
                    } else {
                        lowerBound.addAnnotations(atypeFactory.getQualifierHierarchy().getBottomAnnotations());
                        // TODO: the qualifier hierarchy is null in the NullnessATF.mapGetHeuristics
                        // How should this be handled? What is that factory doing?
                    }
                } else if (uAnnos.isEmpty()) {
                    // TODO: The subtype tests below fail with empty annotations.
                    // Is there anything better to do here?
                } else if (atypeFactory.getQualifierHierarchy().isSubtype(lAnnos, uAnnos)) {
                    // Nothing to do if lAnnos is a subtype of uAnnos.
                } else if (atypeFactory.getQualifierHierarchy().isSubtype(uAnnos, lAnnos)) {
                    lowerBound.replaceAnnotations(uAnnos);
                } else {
                    SourceChecker.errorAbort("AnnotatedTypeMirror.fixupBoundAnnotations: default annotation on lower bound ( " + lAnnos + ") is inconsistent with explicit upper bound: " + this);
                }
            }
        }

        /**
         * Set the upper bound of this variable type
         * @param type the upper bound type
         */
        void setUpperBound(AnnotatedTypeMirror type) {
            // TODO: create a deepCopy?
            this.upperBound = type;
        }

        /**
         * Get the upper bound field directly, bypassing any lazy initialization.
         * This method is necessary to prevent infinite recursions in initialization.
         * In general, prefer getUpperBound.
         *
         * @return the upper bound field.
         */
        public AnnotatedTypeMirror getUpperBoundField() {
            return upperBound;
        }

        /**
         * Get the upper bound of the type variable, possibly lazily initializing it.
         * Attention: If the upper bound is lazily initialized, it will not contain
         * any annotations! Callers of the method have to make sure that an
         * AnnotatedTypeFactory first processed the bound.
         *
         * @return the upper bound type of this type variable
         * @see #getEffectiveUpperBound
         */
        public AnnotatedTypeMirror getUpperBound() {
            if (upperBound == null && actualType.getUpperBound() != null) { // lazy init
                setUpperBound(createType(actualType.getUpperBound(), atypeFactory));
            }
            if (upperBound != null) {
                fixupBoundAnnotations();
            }
            return upperBound;
        }

        /**
         * @return the effective upper bound:  the upper bound,
         * with annotations on the type variable considered.
        */
        public AnnotatedTypeMirror getEffectiveUpperBound() {
            AnnotatedTypeMirror effbnd = AnnotatedTypes.deepCopy(getUpperBound());
            effbnd.replaceAnnotations(annotations);
            return effbnd;
        }

        @Override
        public Set<AnnotationMirror> getEffectiveAnnotations() {
            // Do we want to re-introduce more efficient versions that don't
            // compute the whole type?
            return getEffectiveUpperBound().getAnnotations();
        }

        /**
         *  Used to terminate recursion into upper bounds.
         */
        private boolean inUpperBounds = false;

        @Override
        public AnnotatedTypeVariable getCopy(boolean copyAnnotations) {
            AnnotatedTypeVariable type =
                new AnnotatedTypeVariable(actualType, atypeFactory);
            copyFields(type, copyAnnotations);
            if (!inUpperBounds && getUpperBound().isAnnotated()) {
                inUpperBounds = true;
                type.inUpperBounds = true;
                type.setUpperBound(getUpperBound());
                inUpperBounds = false;
                type.inUpperBounds = false;
            }
            return type;
        }

        @Override
        public AnnotatedTypeMirror getErased() {
            // |T extends A&B| = |A|
            return this.getEffectiveUpperBound().getErased();
        }

        /* TODO: If we use the stronger equals method below, we also
         * need this "canonical" version of the type variable.
         * This type variable will be used for hashmaps that keep track
         * of type arguments.
        private AnnotatedTypeVariable canonical;

        public AnnotatedTypeVariable getCanonical() {
            if (canonical == null) {
                canonical = new AnnotatedTypeVariable(this.actualType, env, atypeFactory);
            }
            return canonical;
        }
         */

        private static <K extends AnnotatedTypeMirror, V extends AnnotatedTypeMirror>
        V mapGetHelper(Map<K, V> mappings, AnnotatedTypeVariable key) {
            for (Map.Entry<K, V> entry : mappings.entrySet()) {
                K possible = entry.getKey();
                V possValue = entry.getValue();
                if (possible == key) return possValue;
                if (possible instanceof AnnotatedTypeVariable) {
                    AnnotatedTypeVariable other = (AnnotatedTypeVariable)possible;
                    Element oElt = other.getUnderlyingType().asElement();
                    if (key.getUnderlyingType().asElement().equals(oElt)) {
                        // Not identical AnnotatedTypeMirrors, but they wrap the same TypeMirror.
                        if (!key.annotations.isEmpty()
                                && !AnnotationUtils.areSame(key.annotations, other.annotations)) {
                            // An annotated type variable use means to override
                            // any annotations on the actual type argument.
                            @SuppressWarnings("unchecked")
                            V found = (V)possValue.getCopy(false);
                            found.addAnnotations(key.annotations);
                            return found;
                        } else {
                            return possValue;
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            AnnotatedTypeMirror found = mapGetHelper(mappings, this);
            if (found != null) {
                return found;
            }

            AnnotatedTypeVariable type = getCopy(true);
            /* TODO: the above call of getCopy results in calls of
             * getUpperBound, which lazily initializes the field.
             * This causes a modification of the data structure, when
             * all we want to do is copy it.
             * However, if we only do the first part of getCopy,
             * test cases fail. I spent a huge amount of time debugging
             * this and added the annotateImplicitHack above.
            AnnotatedTypeVariable type =
                    new AnnotatedTypeVariable(actualType, env, atypeFactory);
            copyFields(type, true);*/

            Map<AnnotatedTypeMirror, AnnotatedTypeMirror> newMappings =
                new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>(mappings);
            newMappings.put(this, type);
            if (lowerBound != null) {
                type.setLowerBound(lowerBound.substitute(newMappings));
            }
            if (upperBound != null) {
                type.setUpperBound(upperBound.substitute(newMappings));
            }
            return type;
        }

        // Style taken from Type
        boolean isPrintingBound = false;
        @Override
        public String toString(boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            sb.append(formatAnnotationString(annotations, printInvisible));
            sb.append(actualType);
            if (!isPrintingBound) {
                try {
                    isPrintingBound = true;
                    if (getLowerBoundField() != null && getLowerBoundField().getKind() != TypeKind.NULL) {
                        sb.append(" super ");
                        sb.append(getLowerBoundField().toString(printInvisible));
                    }
                    // If the upper bound annotation is not the default, perhaps
                    // print the upper bound even if its kind is TypeKind.NULL.
                    if (getUpperBoundField() != null && getUpperBoundField().getKind() != TypeKind.NULL) {
                        sb.append(" extends ");
                        sb.append(getUpperBoundField().toString(printInvisible));
                    }
                } finally {
                    isPrintingBound = false;
                }
            }
            return sb.toString();
        }

        @Override
        public int hashCode() {
            return this.getUnderlyingType().hashCode();
        }

        /* TODO: provide strict equality comparison.
        @Override
        public boolean equals(Object o) {
            boolean isSame = super.equals(o);
            if (!isSame || !(o instanceof AnnotatedTypeVariable))
                return false;
            AnnotatedTypeVariable other = (AnnotatedTypeVariable) o;
            isSame = this.getUpperBound().equals(other.getUpperBound()) &&
                    this.getLowerBound().equals(other.getLowerBound());
            return isSame;
        }
        */
    }

    /**
     * A pseudo-type used where no actual type is appropriate. The kinds of
     * NoType are:
     *
     * <ul>
     *   <li>VOID - corresponds to the keyword void.</li>
     *   <li> PACKAGE - the pseudo-type of a package element.</li>
     *   <li> NONE - used in other cases where no actual type is appropriate;
     *        for example, the superclass of java.lang.Object. </li>
     * </ul>
     */
    public static class AnnotatedNoType extends AnnotatedTypeMirror {

        private final NoType actualType;

        private AnnotatedNoType(NoType type, AnnotatedTypeFactory factory) {
            super(type, factory);
            this.actualType = type;
        }

        // No need for methods
        // Might like to override annotate(), include(), execlude()
        // AS NoType does not accept any annotations

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitNoType(this, p);
        }

        @Override
        public NoType getUnderlyingType() {
            return this.actualType;
        }

        @Override
        public AnnotatedNoType getCopy(boolean copyAnnotations) {
            AnnotatedNoType type = new AnnotatedNoType(actualType, atypeFactory);
            copyFields(type, copyAnnotations);
            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            // Cannot substitute
            return getCopy(true);
        }
    }

    /**
     * Represents the null type. This is the type of the expression {@code null}.
     */
    public static class AnnotatedNullType extends AnnotatedTypeMirror
    implements AnnotatedReferenceType {

        private final NullType actualType;

        private AnnotatedNullType(NullType type, AnnotatedTypeFactory factory) {
            super(type, factory);
            this.actualType = type;
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitNull(this, p);
        }

        @Override
        public NullType getUnderlyingType() {
            return this.actualType;
        }

        @Override
        public AnnotatedNullType getCopy(boolean copyAnnotations) {

            AnnotatedNullType type = new AnnotatedNullType(actualType, atypeFactory);
            copyFields(type, copyAnnotations);
            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            // cannot substitute
            return getCopy(true);
        }

        @Override
        public String toString(boolean printInvisible) {
            if (printInvisible) {
                return formatAnnotationString(getAnnotations(), printInvisible) + "null";
            } else {
                return "null";
            }
        }
    }

    /**
     * Represents a primitive type. These include {@code boolean},
     * {@code byte}, {@code short}, {@code int}, {@code long}, {@code char},
     * {@code float}, and {@code double}.
     */
    public static class AnnotatedPrimitiveType extends AnnotatedTypeMirror
    implements AnnotatedReferenceType {

        private final PrimitiveType actualType;

        private AnnotatedPrimitiveType(PrimitiveType type,
                AnnotatedTypeFactory factory) {
            super(type, factory);
            this.actualType = type;
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitPrimitive(this, p);
        }

        @Override
        public PrimitiveType getUnderlyingType() {
            return this.actualType;
        }

        @Override
        public AnnotatedPrimitiveType getCopy(boolean copyAnnotations) {
            AnnotatedPrimitiveType type =
                new AnnotatedPrimitiveType(actualType, atypeFactory);
            copyFields(type, copyAnnotations);
            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            if (mappings.containsKey(this))
                return mappings.get(this);
            return getCopy(true);
        }
    }

    /**
     * Represents a wildcard type argument. Examples include:
     *
     *    ?
     *    ? extends Number
     *    ? super T
     *
     * A wildcard may have its upper bound explicitly set by an extends
     * clause, its lower bound explicitly set by a super clause, or neither
     * (but not both).
     */
    public static class AnnotatedWildcardType extends AnnotatedTypeMirror {
        /** SuperBound **/
        private AnnotatedTypeMirror superBound;

        /** ExtendBound **/
        private AnnotatedTypeMirror extendsBound;

        private final WildcardType actualType;

        private AnnotatedWildcardType(WildcardType type, AnnotatedTypeFactory factory) {
            super(type, factory);
            this.actualType = type;
        }

        /**
         * Sets the super bound of this wild card
         *
         * @param type  the type of the lower bound
         */
        void setSuperBound(AnnotatedTypeMirror type) {
            this.superBound = type;
        }

        public AnnotatedTypeMirror getSuperBoundField() {
            return superBound;
        }

        /**
         * @return the lower bound of this wildcard. If no lower bound is
         * explicitly declared, {@code null} is returned.
         */
        public AnnotatedTypeMirror getSuperBound() {
            if (superBound == null
                    && actualType.getSuperBound() != null) {
                // lazy init
                setSuperBound(createType(actualType.getSuperBound(), atypeFactory));
            }
            return this.superBound;
        }

        public AnnotatedTypeMirror getEffectiveSuperBound() {
            AnnotatedTypeMirror spb = getSuperBound();
            if (spb == null) {
                return null;
            }
            AnnotatedTypeMirror effbnd = AnnotatedTypes.deepCopy(spb);
            effbnd.replaceAnnotations(annotations);
            return effbnd;
        }

        /**
         * Sets the upper bound of this wild card
         *
         * @param type  the type of the upper bound
         */
        void setExtendsBound(AnnotatedTypeMirror type) {
            this.extendsBound = type;
        }

        public AnnotatedTypeMirror getExtendsBoundField() {
            return extendsBound;
        }

        /**
         * @return the upper bound of this wildcard. If no upper bound is
         * explicitly declared, the upper bound of the type variable to which
         * the wildcard is bound is used.
         */
        public AnnotatedTypeMirror getExtendsBound() {
            if (extendsBound == null) {
                // lazy init
                TypeMirror superType = actualType.getExtendsBound();
                if (superType == null) {
                    // Take the upper bound of the type variable the wildcard is bound to.
                    com.sun.tools.javac.code.Type.WildcardType wct = (com.sun.tools.javac.code.Type.WildcardType) actualType;
                    com.sun.tools.javac.util.Context ctx = ((com.sun.tools.javac.processing.JavacProcessingEnvironment) atypeFactory.processingEnv).getContext();
                    superType = com.sun.tools.javac.code.Types.instance(ctx).upperBound(wct);
                }
                setExtendsBound(createType(superType, atypeFactory));
            }
            return this.extendsBound;
        }

        /**
         * @return the effective extends bound: the extends bound, with
         *         annotations on the type variable considered.
         */
        public AnnotatedTypeMirror getEffectiveExtendsBound() {
            AnnotatedTypeMirror effbnd = AnnotatedTypes.deepCopy(getExtendsBound());
            effbnd.replaceAnnotations(annotations);
            return effbnd;
        }

        /**
         * @return the effective upper bound annotations: the annotations on
         *         this, or if none, those on the upper bound.
         */
        public Set<AnnotationMirror> getEffectiveExtendsBoundAnnotations() {
            Set<AnnotationMirror> result = annotations;
            if (result.isEmpty()) {
                AnnotatedTypeMirror ub = getExtendsBound();
                if (ub != null) {
                    result = ub.getEffectiveAnnotations();
                }
            }
            return Collections.unmodifiableSet(result);
        }

        @Override
        public Set<AnnotationMirror> getEffectiveAnnotations() {
            return getEffectiveExtendsBoundAnnotations();
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitWildcard(this, p);
        }

        @Override
        public WildcardType getUnderlyingType() {
            return this.actualType;
        }

        @Override
        public AnnotatedWildcardType getCopy(boolean copyAnnotations) {
            AnnotatedWildcardType type = new AnnotatedWildcardType(actualType, atypeFactory);
            copyFields(type, copyAnnotations);

            type.setExtendsBound(getExtendsBound());
            type.setSuperBound(getSuperBound());

            type.methodTypeArgHack = methodTypeArgHack;

            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            if (mappings.containsKey(this))
                return mappings.get(this);

            AnnotatedWildcardType type = getCopy(true);
            // Prevent looping
            Map<AnnotatedTypeMirror, AnnotatedTypeMirror> newMapping =
                new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>(mappings);
            newMapping.put(this, type);

            // The extends and super bounds can be null because the underlying
            // type's extends and super bounds can be null.
            if (extendsBound != null)
                type.setExtendsBound(extendsBound.substitute(newMapping));
            if (superBound != null)
                type.setSuperBound(superBound.substitute(newMapping));

            return type;
        }

        @Override
        public AnnotatedTypeMirror getErased() {
            // |? extends A&B| = |A|
            return getEffectiveExtendsBound().getErased();
        }

        boolean isPrintingBound = false;
        @Override
        public String toString(boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            sb.append(formatAnnotationString(annotations, printInvisible));
            sb.append("?");
            if (!isPrintingBound) {
                try {
                    isPrintingBound = true;
                    if (getSuperBoundField() != null && getSuperBoundField().getKind() != TypeKind.NULL) {
                        sb.append(" super ");
                        sb.append(getSuperBoundField().toString(printInvisible));
                    }
                    if (getExtendsBoundField() != null && getExtendsBoundField().getKind() != TypeKind.NONE) {
                        sb.append(" extends ");
                        sb.append(getExtendsBoundField().toString(printInvisible));
                    }
                } finally {
                    isPrintingBound = false;
                }
            }
            return sb.toString();
        }

        // Remove the methodTypeArgHack once method type
        // argument inference (in AnnotatedTypes) is done
        // correctly.
        private boolean methodTypeArgHack = false;
        /* package-scope */ void setMethodTypeArgHack() {
            methodTypeArgHack = true;
        }
        /* package-scope */ boolean isMethodTypeArgHack() {
            return methodTypeArgHack;
        }
    }

    public static class AnnotatedIntersectionType extends AnnotatedTypeMirror {

        protected final IntersectionType actualType;

        /**
         * Constructor for this type
         *
         * @param type  underlying kind of this type
         * @param atypeFactory TODO
         */
        private AnnotatedIntersectionType(IntersectionType type,
                AnnotatedTypeFactory atypeFactory) {
            super(type, atypeFactory);
            this.actualType = type;
        }

        @Override
        public String toString(boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            // Prevent an infinite recursion that might happen when calling toString
            // within deepCopy, caused by postAsSuper in (at least) the IGJ checker.
            // if (this.supertypes == null) { return; }

            boolean isFirst = true;
            for(AnnotatedDeclaredType adt : this.directSuperTypes()) {
                if (!isFirst) sb.append(" & ");
                sb.append(adt.toString(printInvisible));
                isFirst = false;
            }
            return sb.toString();
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitIntersection(this, p);
        }

        @Override
        public AnnotatedIntersectionType getCopy(boolean copyAnnotations) {
            AnnotatedIntersectionType type =
                    new AnnotatedIntersectionType(actualType, atypeFactory);
            copyFields(type, copyAnnotations);
            return type;
        }

        protected List<AnnotatedDeclaredType> supertypes;

        @Override
        public List<AnnotatedDeclaredType> directSuperTypes() {
            if (supertypes == null) {
                List<? extends TypeMirror> ubounds = actualType.getBounds();
                List<AnnotatedDeclaredType> res = new ArrayList<AnnotatedDeclaredType>(ubounds.size());
                for (TypeMirror bnd : ubounds) {
                    res.add((AnnotatedDeclaredType) createType(bnd, atypeFactory));
                }
                supertypes = res;
            }
            return Collections.unmodifiableList(supertypes);
        }

        public List<AnnotatedDeclaredType> directSuperTypesField() {
            return supertypes;
        }

        void setDirectSuperTypes(List<AnnotatedDeclaredType> supertypes) {
            this.supertypes = new ArrayList<AnnotatedDeclaredType>(supertypes);
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                    ? extends AnnotatedTypeMirror> mappings) {
            if (mappings.containsKey(this))
                return mappings.get(this);

            AnnotatedIntersectionType type = getCopy(true);

            Map<AnnotatedTypeMirror, AnnotatedTypeMirror> newMappings =
                    new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>(mappings);
            newMappings.put(this, type);

            if (this.supertypes != null) {
                // watch need to copy upper bound as well
                List<AnnotatedDeclaredType> supertypes = new ArrayList<AnnotatedDeclaredType>();
                for (AnnotatedDeclaredType t : directSuperTypes())
                    supertypes.add((AnnotatedDeclaredType)t.substitute(newMappings));
                type.supertypes = supertypes;
            }
            return type;
        }
    }


    public List<? extends AnnotatedTypeMirror> directSuperTypes() {
        return directSuperTypes(this);
    }

    // Version of method below for declared types
    protected final List<AnnotatedDeclaredType> directSuperTypes(
            AnnotatedDeclaredType type) {
        setSuperTypeFinder(type.atypeFactory);
        List<AnnotatedDeclaredType> supertypes =
            superTypeFinder.visitDeclared(type, null);
        atypeFactory.postDirectSuperTypes(type, supertypes);
        return supertypes;
    }

    // Version of method above for all types
    private final List<? extends AnnotatedTypeMirror> directSuperTypes(
            AnnotatedTypeMirror type) {
        setSuperTypeFinder(type.atypeFactory);
        List<? extends AnnotatedTypeMirror> supertypes =
            superTypeFinder.visit(type, null);
        atypeFactory.postDirectSuperTypes(type, supertypes);
        return supertypes;
    }

    private static void setSuperTypeFinder(AnnotatedTypeFactory factory) {
        if (superTypeFinder == null || superTypeFinder.atypeFactory != factory)
            superTypeFinder = new SuperTypeFinder(factory);
    }

    private static SuperTypeFinder superTypeFinder;

    private static class SuperTypeFinder extends
    SimpleAnnotatedTypeVisitor<List<? extends AnnotatedTypeMirror>, Void> {
        private final Types types;
        private final AnnotatedTypeFactory atypeFactory;

        SuperTypeFinder(AnnotatedTypeFactory atypeFactory) {
            this.atypeFactory = atypeFactory;
            this.types = atypeFactory.types;
        }

        @Override
        public List<AnnotatedTypeMirror> defaultAction(AnnotatedTypeMirror t, Void p) {
            return new ArrayList<AnnotatedTypeMirror>();
        }


        /**
         * Primitive Rules:
         *
         * double >1 float
         * float >1 long
         * long >1 int
         * int >1 char
         * int >1 short
         * short >1 byte
         *
         * For easiness:
         * boxed(primitiveType) >: primitiveType
         */
        @Override
        public List<AnnotatedTypeMirror> visitPrimitive(AnnotatedPrimitiveType type, Void p) {
            List<AnnotatedTypeMirror> superTypes =
                new ArrayList<AnnotatedTypeMirror>();
            Set<AnnotationMirror> annotations = type.getAnnotations();

            // Find Boxed type
            TypeElement boxed = types.boxedClass(type.getUnderlyingType());
            AnnotatedDeclaredType boxedType = atypeFactory.getAnnotatedType(boxed);
            boxedType.replaceAnnotations(annotations);
            superTypes.add(boxedType);

            TypeKind superPrimitiveType = null;

            if (type.getKind() == TypeKind.BOOLEAN) {
                // Nothing
            } else if (type.getKind() == TypeKind.BYTE) {
                superPrimitiveType = TypeKind.SHORT;
            } else if (type.getKind() == TypeKind.CHAR) {
                superPrimitiveType = TypeKind.INT;
            } else if (type.getKind() == TypeKind.DOUBLE) {
                // Nothing
            } else if (type.getKind() == TypeKind.FLOAT) {
                superPrimitiveType = TypeKind.DOUBLE;
            } else if (type.getKind() == TypeKind.INT) {
                superPrimitiveType = TypeKind.LONG;
            } else if (type.getKind() == TypeKind.LONG) {
                superPrimitiveType = TypeKind.FLOAT;
            } else if (type.getKind() == TypeKind.SHORT) {
                superPrimitiveType = TypeKind.INT;
            } else
                assert false: "Forgot the primitive " + type;

            if (superPrimitiveType != null) {
                AnnotatedPrimitiveType superPrimitive = (AnnotatedPrimitiveType)
                    atypeFactory.toAnnotatedType(types.getPrimitiveType(superPrimitiveType));
                superPrimitive.addAnnotations(annotations);
                superTypes.add(superPrimitive);
            }

            return superTypes;
        }

        @Override
        public List<AnnotatedDeclaredType> visitDeclared(AnnotatedDeclaredType type, Void p) {
            List<AnnotatedDeclaredType> supertypes =
                new ArrayList<AnnotatedDeclaredType>();
            // Set<AnnotationMirror> annotations = type.getAnnotations();

            TypeElement typeElement =
                (TypeElement) type.getUnderlyingType().asElement();
            // Mapping of type variable to actual types
            Map<TypeParameterElement, AnnotatedTypeMirror> mapping =
                new HashMap<TypeParameterElement, AnnotatedTypeMirror>();

            final boolean isRaw = !type.isParameterized() && type.isGeneric();

            for (int i = 0; i < typeElement.getTypeParameters().size() &&
                            i < type.getTypeArguments().size(); ++i) {
                mapping.put(typeElement.getTypeParameters().get(i),
                        type.getTypeArguments().get(i));
            }

            ClassTree classTree = atypeFactory.trees.getTree(typeElement);
            // Testing against enum and annotation. Ideally we can simply use element!
            if (classTree != null) {
                supertypes.addAll(supertypesFromTree(type, classTree));
            } else {
                supertypes.addAll(supertypesFromElement(type, typeElement));
                // final Element elem = type.getElement() == null ? typeElement : type.getElement();
            }

            for (AnnotatedDeclaredType dt : supertypes) {
                if (isRaw) {
                    dt.setTypeArguments(Collections.<AnnotatedTypeMirror>emptyList());
                } else if (!mapping.isEmpty()) {
                    replacer.visit(dt, mapping);
                }
            }

            return supertypes;
        }

        private List<AnnotatedDeclaredType> supertypesFromElement(AnnotatedDeclaredType type, TypeElement typeElement) {
            List<AnnotatedDeclaredType> supertypes = new ArrayList<AnnotatedDeclaredType>();
            // Find the super types: Start with enums and superclass
            if (typeElement.getKind() == ElementKind.ENUM) {
                DeclaredType dt = (DeclaredType) typeElement.getSuperclass();
                AnnotatedDeclaredType adt = (AnnotatedDeclaredType) atypeFactory.toAnnotatedType(dt);
                List<AnnotatedTypeMirror> tas = adt.getTypeArguments();
                List<AnnotatedTypeMirror> newtas = new ArrayList<AnnotatedTypeMirror>();
                for (AnnotatedTypeMirror t : tas) {
                    // If the type argument of super is the same as the input type
                    if (atypeFactory.types.isSameType(t.getUnderlyingType(), type.getUnderlyingType())) {
                        t.addAnnotations(type.getAnnotations());
                        newtas.add(t);
                    }
                }
                adt.setTypeArguments(newtas);
                supertypes.add(adt);
            } else if (typeElement.getSuperclass().getKind() != TypeKind.NONE) {
                DeclaredType superClass = (DeclaredType) typeElement.getSuperclass();
                AnnotatedDeclaredType dt =
                    (AnnotatedDeclaredType) atypeFactory.toAnnotatedType(superClass);
                supertypes.add(dt);
            } else if (!ElementUtils.isObject(typeElement)) {
                supertypes.add(createTypeOfObject(atypeFactory));
            }
            for (TypeMirror st : typeElement.getInterfaces()) {
                AnnotatedDeclaredType ast =
                    (AnnotatedDeclaredType) atypeFactory.toAnnotatedType(st);
                supertypes.add(ast);
            }
            TypeFromElement.annotateSupers(supertypes, typeElement);

            return supertypes;
        }

        private List<AnnotatedDeclaredType> supertypesFromTree(AnnotatedDeclaredType type, ClassTree classTree) {
            List<AnnotatedDeclaredType> supertypes = new ArrayList<AnnotatedDeclaredType>();
            if (classTree.getExtendsClause() != null) {
                AnnotatedDeclaredType adt = (AnnotatedDeclaredType)
                    atypeFactory.fromTypeTree(classTree.getExtendsClause());
                supertypes.add(adt);
            } else if (!ElementUtils.isObject(TreeUtils.elementFromDeclaration(classTree))) {
                supertypes.add(createTypeOfObject(atypeFactory));
            }

            for (Tree implemented : classTree.getImplementsClause()) {
                AnnotatedDeclaredType adt = (AnnotatedDeclaredType)
                    atypeFactory.getAnnotatedTypeFromTypeTree(implemented);
                supertypes.add(adt);
            }

            TypeElement elem = TreeUtils.elementFromDeclaration(classTree);
            if (elem.getKind() == ElementKind.ENUM) {
                DeclaredType dt = (DeclaredType) elem.getSuperclass();
                AnnotatedDeclaredType adt = (AnnotatedDeclaredType) atypeFactory.toAnnotatedType(dt);
                List<AnnotatedTypeMirror> tas = adt.getTypeArguments();
                List<AnnotatedTypeMirror> newtas = new ArrayList<AnnotatedTypeMirror>();
                for (AnnotatedTypeMirror t : tas) {
                    // If the type argument of super is the same as the input type
                    if (atypeFactory.types.isSameType(t.getUnderlyingType(), type.getUnderlyingType())) {
                        t.addAnnotations(type.getAnnotations());
                        newtas.add(t);
                    }
                }
                adt.setTypeArguments(newtas);
                supertypes.add(adt);
            }
            return supertypes;
        }

        /**
         * For type = A[ ] ==>
         *  Object >: A[ ]
         *  Clonable >: A[ ]
         *  java.io.Serializable >: A[ ]
         *
         * if A is reference type, then also
         *  B[ ] >: A[ ] for any B[ ] >: A[ ]
         */
        @Override
        public List<AnnotatedTypeMirror> visitArray(AnnotatedArrayType type, Void p) {
            List<AnnotatedTypeMirror> superTypes = new ArrayList<AnnotatedTypeMirror>();
            Set<AnnotationMirror> annotations = type.getAnnotations();
            Elements elements = atypeFactory.elements;
            final AnnotatedTypeMirror objectType =
                atypeFactory.getAnnotatedType(elements.getTypeElement("java.lang.Object"));
            objectType.addAnnotations(annotations);
            superTypes.add(objectType);

            final AnnotatedTypeMirror cloneableType =
                atypeFactory.getAnnotatedType(elements.getTypeElement("java.lang.Cloneable"));
            cloneableType.addAnnotations(annotations);
            superTypes.add(cloneableType);

            final AnnotatedTypeMirror serializableType =
                atypeFactory.getAnnotatedType(elements.getTypeElement("java.io.Serializable"));
            serializableType.addAnnotations(annotations);
            superTypes.add(serializableType);

            if (type.getComponentType() instanceof AnnotatedReferenceType) {
                for (AnnotatedTypeMirror sup : type.getComponentType().directSuperTypes()) {
                    ArrayType arrType = atypeFactory.types.getArrayType(sup.getUnderlyingType());
                    AnnotatedArrayType aarrType = (AnnotatedArrayType)
                        atypeFactory.toAnnotatedType(arrType);
                    aarrType.setComponentType(sup);
                    aarrType.addAnnotations(annotations);
                    superTypes.add(aarrType);
                }
            }

            return superTypes;
        }

        @Override
        public List<AnnotatedTypeMirror> visitTypeVariable(AnnotatedTypeVariable type, Void p) {
            List<AnnotatedTypeMirror> superTypes = new ArrayList<AnnotatedTypeMirror>();
            if (type.getEffectiveUpperBound() != null)
                superTypes.add(AnnotatedTypes.deepCopy(type.getEffectiveUpperBound()));
            return superTypes;
        }

        @Override
        public List<AnnotatedTypeMirror> visitWildcard(AnnotatedWildcardType type, Void p) {
            List<AnnotatedTypeMirror> superTypes = new ArrayList<AnnotatedTypeMirror>();
            if (type.getEffectiveExtendsBound() != null)
                superTypes.add(AnnotatedTypes.deepCopy(type.getEffectiveExtendsBound()));
            return superTypes;
        }
    };

    private static Replacer replacer;

    private static class Replacer extends AnnotatedTypeScanner<Void, Map<TypeParameterElement, AnnotatedTypeMirror>> {
        final Types types;

        public Replacer(Types types) {
            this.types = types;
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Map<TypeParameterElement, AnnotatedTypeMirror> mapping) {
            List<AnnotatedTypeMirror> args = new ArrayList<AnnotatedTypeMirror>();
            for (AnnotatedTypeMirror arg : type.getTypeArguments()) {
                Element elem = types.asElement(arg.getUnderlyingType());
                if ((elem != null) &&
                        (elem.getKind() == ElementKind.TYPE_PARAMETER) &&
                        (mapping.containsKey(elem))) {
                    AnnotatedTypeMirror other = mapping.get(elem);
                    other.replaceAnnotations(arg.annotations);
                    args.add(other);
                } else {
                    args.add(arg);
                }
            }
            type.setTypeArguments(args);
            return super.visitDeclared(type, mapping);
        }

        @Override
        public Void visitArray(AnnotatedArrayType type, Map<TypeParameterElement, AnnotatedTypeMirror> mapping) {
            AnnotatedTypeMirror comptype = type.getComponentType();
            Element elem = types.asElement(comptype.getUnderlyingType());
            AnnotatedTypeMirror other;
            if ((elem != null) &&
                    (elem.getKind() == ElementKind.TYPE_PARAMETER) &&
                    (mapping.containsKey(elem))) {
                other = mapping.get(elem);
                other.replaceAnnotations(comptype.annotations);
                type.setComponentType(other);
            }
            return super.visitArray(type, mapping);
        }
    };

}
