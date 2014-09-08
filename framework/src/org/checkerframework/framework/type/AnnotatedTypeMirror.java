package org.checkerframework.framework.type;

/*>>>
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.interning.qual.*;
*/

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
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
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
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.TypeQualifier;
import org.checkerframework.framework.type.explicit.ElementAnnotationUtil;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.AnnotatedTypeVisitor;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeVisitor;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
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
     * @param atypeFactory
     * @param isDeclaration true if the result should is a type declaration
     * @return [to document]
     */
    public static AnnotatedTypeMirror createType(TypeMirror type,
        AnnotatedTypeFactory atypeFactory, boolean isDeclaration) {
        if (type == null) {
            ErrorReporter.errorAbort("AnnotatedTypeMirror.createType: input type must not be null!");
            return null;
        }

        AnnotatedTypeMirror result;
        switch (type.getKind()) {
            case ARRAY:
                result = new AnnotatedArrayType((ArrayType) type, atypeFactory);
                break;
            case DECLARED:
                result = new AnnotatedDeclaredType((DeclaredType) type, atypeFactory, isDeclaration);
                break;
            case ERROR:
                ErrorReporter.errorAbort("AnnotatedTypeMirror.createType: input should type-check already! Found error type: " + type);
                return null; // dead code
            case EXECUTABLE:
                result = new AnnotatedExecutableType((ExecutableType) type, atypeFactory);
                break;
            case VOID:
            case PACKAGE:
            case NONE:
                result = new AnnotatedNoType((NoType) type, atypeFactory);
                break;
            case NULL:
                result = new AnnotatedNullType((NullType) type, atypeFactory);
                break;
            case TYPEVAR:
                result = new AnnotatedTypeVariable((TypeVariable) type, atypeFactory, isDeclaration);
                break;
            case WILDCARD:
                result = new AnnotatedWildcardType((WildcardType) type, atypeFactory);
                break;
            case INTERSECTION:
                result = new AnnotatedIntersectionType((IntersectionType) type, atypeFactory);
                break;
            case UNION:
                result = new AnnotatedUnionType((UnionType) type, atypeFactory);
                break;
            default:
                if (type.getKind().isPrimitive()) {
                    result = new AnnotatedPrimitiveType((PrimitiveType) type, atypeFactory);
                    break;
                }
                ErrorReporter.errorAbort("AnnotatedTypeMirror.createType: unidentified type " +
                        type + " (" + type.getKind() + ")");
                return null; // dead code
        }
        /*if (jctype.isAnnotated()) {
            result.addAnnotations(jctype.getAnnotationMirrors());
        }*/
        return result;
    }

    /** The factory to use for lazily creating annotated types. */
    protected final AnnotatedTypeFactory atypeFactory;

    /** Actual type wrapped with this AnnotatedTypeMirror **/
    protected final TypeMirror actualType;

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
     * @param atypeFactory used to create further types and to access
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
       if(this == o) {
            return true;
       }
       if (!(o instanceof AnnotatedTypeMirror))
           return false;
       AnnotatedTypeMirror t = (AnnotatedTypeMirror) o;

       //Note: isSameType never returns true for wildcards.  That is isSameType(myWildcard, myWildcard)
       //will return false.  This means, only referentially equal wildcards will return true in this method
       //because of the == test on the first line.  Two wildcards that are structurally equivalent
       //will NOT equal each other
       if (atypeFactory.types.isSameType(this.actualType, t.actualType)
               && AnnotationUtils.areSame(getAnnotations(), t.getAnnotations()))
           return true;
       return false;
    }

    @Pure
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
     * Returns true if this type mirror represents a declaration, rather than a
     * use, of a type.
     *
     * For example, <code>class List&lt;T&gt; { ... }</code> declares a new type
     * {@code List<T>}, while {@code List<Integer>} is a use of the type.
     *
     * @return  true if this represents a declaration
     */
    public boolean isDeclaration() {
        return false;
    }

    public AnnotatedTypeMirror asUse() {
        return this;
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
     * Returns an annotation from the given sub-hierarchy, if such
     * an annotation is present on this type or on its extends bounds;
     * otherwise returns null.
     *
     * It doesn't account for annotations in deep types (type arguments,
     * array components, etc).
     *
     * @param p The qualifier hierarchy to check for.
     * @return An annotation from the same hierarchy as p if present.
     */
    public AnnotationMirror getEffectiveAnnotationInHierarchy(AnnotationMirror p) {
        AnnotationMirror aliased = p;
        if (!atypeFactory.isSupportedQualifier(aliased)) {
            aliased = atypeFactory.aliasedAnnotation(p);
        }
        if (atypeFactory.isSupportedQualifier(aliased)) {
            QualifierHierarchy qualHier = this.atypeFactory.getQualifierHierarchy();
            AnnotationMirror anno = qualHier.findCorrespondingAnnotation(aliased,
                    getEffectiveAnnotations());
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
     * the type itself, or on the upper/extends bound of a type variable/wildcard
     * (recursively, until a class type is reached).
     *
     * @return  a set of the annotations on this
     */
    public Set<AnnotationMirror> getEffectiveAnnotations() {
        Set<AnnotationMirror> effectiveAnnotations = getErased().getAnnotations();
//        assert atypeFactory.qualHierarchy.getWidth() == effectiveAnnotations
//                .size() : "Invalid number of effective annotations ("
//                + effectiveAnnotations + "). Should be "
//                + atypeFactory.qualHierarchy.getWidth() + " but is "
//                + effectiveAnnotations.size() + ". Type: " + this.toString();
        return effectiveAnnotations;
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
        return getAnnotation(annotationName.toString().intern());
    }

    /**
     * Returns the actual annotation mirror used to annotate this type,
     * whose name equals the string argument if one exists, null otherwise.
     *
     * @param annotationStr
     * @return the annotation mirror for annotationStr
     */
    public AnnotationMirror getAnnotation(/*@Interned*/ String annotationStr) {
        assert annotationStr != null : "Null annotationName in getAnnotation";
        for (AnnotationMirror anno : getAnnotations())
            if (AnnotationUtils.areSameByName(anno, annotationStr))
                return anno;
        return null;
    }

    /**
     * Returns the actual annotation mirror used to annotate this type,
     * whose name equals the passed annotationName if one exists, null otherwise.
     *
     * @param annoClass annotation class
     * @return the annotation mirror for anno
     */
    public AnnotationMirror getAnnotation(Class<? extends Annotation> annoClass) {
        for (AnnotationMirror annoMirror : getAnnotations()) {
            if (AnnotationUtils.areSameByClass(annoMirror, annoClass)) {
                return annoMirror;
            }
        }
        return null;
    }

    /**
     * Returns the set of explicitly written annotations supported by this checker.
     * This is useful to check the validity of annotations explicitly present on a type,
     * as flow inference might add annotations that were not previously present.
     *
     * @return The set of explicitly written annotations supported by this checker.
     */
    public Set<AnnotationMirror> getExplicitAnnotations() {
        // TODO JSR 308: The explicit type annotations should be always present
        Set<AnnotationMirror> explicitAnnotations = AnnotationUtils.createAnnotationSet();
        List<? extends AnnotationMirror> typeAnnotations = this.getUnderlyingType().getAnnotationMirrors();

        Set<? extends AnnotationMirror> validAnnotations = atypeFactory.getQualifierHierarchy().getTypeQualifiers();
        for (AnnotationMirror explicitAnno : typeAnnotations) {
            for (AnnotationMirror validAnno : validAnnotations) {
                if (AnnotationUtils.areSameIgnoringValues(explicitAnno, validAnno)) {
                    explicitAnnotations.add(explicitAnno);
                }
            }
        }

        return explicitAnnotations;
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

    /**
     * A version of hasAnnotation that considers annotations on the
     * upper bound of wildcards and type variables.
     *
     * @see #hasAnnotation(Class)
     */
    public boolean hasEffectiveAnnotation(Class<? extends Annotation> a) {
        return AnnotationUtils.containsSameIgnoringValues(
                getEffectiveAnnotations(),
                AnnotationUtils.fromClass(atypeFactory.elements, a));
    }

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
            ErrorReporter.errorAbort("AnnotatedTypeMirror.addAnnotation: null is not a valid annotation.");
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
     * Adds those annotations to the current type, for which no annotation
     * from the same qualifier hierarchy is present.
     *
     * @param annotations the annotations to add
     */
    public void addMissingAnnotations(Iterable<? extends AnnotationMirror> annotations) {
        for (AnnotationMirror a : annotations) {
            if (!this.isAnnotatedInHierarchy(a)) {
                this.addAnnotation(a);
            }
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
        // It's currently necessary for the IGJ Checker and Lock Checker.
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
            ErrorReporter.errorAbort("AnnotatedTypeMirror.removeAnnotation called with un-supported class: " + a);
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
     * Remove an annotation that is in the same qualifier hierarchy as the parameter,
     * unless it's the top annotation.
     *
     * @param a An annotation from the same qualifier hierarchy
     * @return If an annotation was removed
     */
    public boolean removeNonTopAnnotationInHierarchy(AnnotationMirror a) {
        AnnotationMirror prev = this.getAnnotationInHierarchy(a);
        QualifierHierarchy qualHier = this.atypeFactory.getQualifierHierarchy();
        if (prev != null && !prev.equals(qualHier.getTopAnnotation(a))) {
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
     *
     * This method should only be used in very specific situations.
     * For individual type systems, it is generally better to use
     * {@link #removeAnnotation(AnnotationMirror)}
     * and similar methods.
     */
    public void clearAnnotations() {
        annotations.clear();
    }

    private static boolean isInvisibleQualified(AnnotationMirror anno) {
        return ((TypeElement)anno.getAnnotationType().asElement()).getAnnotation(InvisibleQualifier.class) != null;
    }

    // A helper method to print annotations separated by a space.
    // Note a final space after a list of annotations to separate from the underlying type.
    @SideEffectFree
    protected final static String formatAnnotationString(
            Collection<? extends AnnotationMirror> lst,
            boolean printInvisible) {
        StringBuilder sb = new StringBuilder();
        for (AnnotationMirror obj : lst) {
            if (obj == null) {
                ErrorReporter.errorAbort("AnnotatedTypeMirror.formatAnnotationString: found null AnnotationMirror!");
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

    /**
     * Returns the string representation of a single AnnotationMirror, without showing full package names.
     */
    public final static String formatAnnotationMirror(AnnotationMirror am) {
        StringBuilder sb = new StringBuilder();
        formatAnnotationMirror(am, sb);
        return sb.toString();
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

    @SideEffectFree
    @Override
    public final String toString() {
        // Also see
        // org.checkerframework.common.basetype.BaseTypeVisitor.commonAssignmentCheck(AnnotatedTypeMirror, AnnotatedTypeMirror, Tree, String)
        // TODO the direct access to the 'checker' field is not clean
        return toString(atypeFactory.checker.hasOption("printAllQualifiers"));
    }

    /**
     * A version of toString() that optionally outputs all type qualifiers,
     * including @InvisibleQualifier's.
     *
     * @param invisible Whether to always output invisible qualifiers.
     * @return A string representation of the current type containing all qualifiers.
     */
    @SideEffectFree
    public String toString(boolean invisible) {
        return formatAnnotationString(getAnnotations(), invisible)
                + this.actualType;
    }

    @SideEffectFree
    public String toStringDebug() {
        return toString(true) + " " + getClass().getSimpleName(); // + "#" + uid;
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
     * Returns a shallow copy of this type.
     *
     * @param copyAnnotations
     *            whether copy should have annotations, i.e. whether
     *            field {@code annotations} should be copied.
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
        return this.substitute(mappings, false);
    }

    public AnnotatedTypeMirror substitute(
            Map<? extends AnnotatedTypeMirror,
                    ? extends AnnotatedTypeMirror> mappings, boolean forDeepCopy) {
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

        /**
         * Whether the type was initially raw, i.e. the user
         * did not provide the type arguments.
         * typeArgs will contain inferred type arguments, which
         * might be too conservative at the moment.
         * TODO: improve inference.
         *
         * Ideally, the field would be final. However, when
         * we determine the supertype of a raw type, we need
         * to set wasRaw for the supertype.
         */
        private boolean wasRaw;

        /** The enclosing Type **/
        protected AnnotatedDeclaredType enclosingType;

        protected List<AnnotatedDeclaredType> supertypes = null;

        private boolean declaration;

        /**
         * Constructor for this type
         *
         * @param type  underlying kind of this type
         * @param atypeFactory TODO
         */
        private AnnotatedDeclaredType(DeclaredType type,
                AnnotatedTypeFactory atypeFactory, boolean declaration) {
            super(type, atypeFactory);
            TypeElement typeelem = (TypeElement) type.asElement();
            DeclaredType declty = (DeclaredType) typeelem.asType();
            wasRaw = !declty.getTypeArguments().isEmpty() &&
                      type.getTypeArguments().isEmpty();

            TypeMirror encl = type.getEnclosingType();
            if (encl.getKind() == TypeKind.DECLARED) {
                this.enclosingType = (AnnotatedDeclaredType) createType(encl, atypeFactory, true);
            } else if (encl.getKind() != TypeKind.NONE) {
                ErrorReporter.errorAbort("AnnotatedDeclaredType: unsupported enclosing type: " +
                        type.getEnclosingType() + " (" + encl.getKind() + ")");
            }

            this.declaration = declaration;
        }

        @Override
        public boolean isDeclaration() {
            return declaration;
        }

        @Override
        public AnnotatedDeclaredType asUse() {
            if (!this.isDeclaration()) {
                return this;
            }

            AnnotatedDeclaredType result = this.getCopy(true);
            result.declaration = false;

            List<AnnotatedTypeMirror> newArgs = new ArrayList<>();
            for (AnnotatedTypeMirror arg : result.getTypeArguments()) {
                AnnotatedTypeVariable paramDecl = (AnnotatedTypeVariable)arg;
                assert paramDecl.isDeclaration();
                newArgs.add(paramDecl.asUse());
            }
            result.setTypeArguments(newArgs);

            return result;
        }

        @SideEffectFree
        @Override
        public String toString(boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            if (declaration) {
                sb.append("/*DECL*/ ");
            }
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
            if (ts == null || ts.isEmpty()) {
                typeArgs = Collections.emptyList();
            } else {
                if (isDeclaration()) {
                    // TODO: check that all args are really declarations
                    typeArgs = Collections.unmodifiableList(ts);
                } else {
                    List<AnnotatedTypeMirror> uses = new ArrayList<>();
                    for (AnnotatedTypeMirror t : ts) {
                        uses.add(t.asUse());
                    }
                    typeArgs = Collections.unmodifiableList(uses);
                }
            }
        }

        /**
         * @return the type argument for this type
         */
        public List<AnnotatedTypeMirror> getTypeArguments() {
            if (typeArgs == null) {
                typeArgs = new ArrayList<AnnotatedTypeMirror>();
                if (!((DeclaredType)actualType).getTypeArguments().isEmpty()) { // lazy init
                    for (TypeMirror t : ((DeclaredType)actualType).getTypeArguments()) {
                        typeArgs.add(createType(t, atypeFactory, declaration));
                    }
                }
                typeArgs = Collections.unmodifiableList(typeArgs);
            }
            return typeArgs;
        }

        /**
         * Returns true if the type was raw, that is, type arguments were not
         * provided but instead inferred.
         *
         * @return true iff the type was raw
         */
        public boolean wasRaw() {
            return wasRaw;
        }

        /**
         * Set the wasRaw flag to true.
         * This should only be necessary when determining
         * the supertypes of a raw type.
         */
        protected void setWasRaw() {
            this.wasRaw = true;
        }

        @Override
        public DeclaredType getUnderlyingType() {
            return (DeclaredType) actualType;
        }

        void setDirectSuperTypes(List<AnnotatedDeclaredType> supertypes) {
            this.supertypes = new ArrayList<AnnotatedDeclaredType>(supertypes);
        }

        @Override
        public List<AnnotatedDeclaredType> directSuperTypes() {
            if (supertypes == null) {
                supertypes = Collections.unmodifiableList(directSuperTypes(this));
            }
            return supertypes;
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
                new AnnotatedDeclaredType(getUnderlyingType(), atypeFactory, declaration);
            if (copyAnnotations)
                type.addAnnotations(annotations);
            type.setEnclosingType(getEnclosingType());
            type.setTypeArguments(getTypeArguments());
            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                    ? extends AnnotatedTypeMirror> mappings, boolean forDeepCopy) {
            if (mappings.containsKey(this))
                return mappings.get(this);

            AnnotatedDeclaredType type = getCopy(true);

            Map<AnnotatedTypeMirror, AnnotatedTypeMirror> newMappings =
                    new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>(mappings);
            newMappings.put(this, type);

            List<AnnotatedTypeMirror> typeArgs = new ArrayList<AnnotatedTypeMirror>();
            for (AnnotatedTypeMirror t : getTypeArguments())
                typeArgs.add(t.substitute(newMappings, forDeepCopy));
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
                            atypeFactory, declaration);
                rType.addAnnotations(getAnnotations());
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
        /*default-visibility*/ void setEnclosingType(AnnotatedDeclaredType enclosingType) {
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

        private ExecutableElement element;

        private AnnotatedExecutableType(ExecutableType type,
                AnnotatedTypeFactory factory) {
            super(type, factory);
            this.actualType = type;
        }

        protected final List<AnnotatedTypeMirror> paramTypes =
                new ArrayList<AnnotatedTypeMirror>();
        protected AnnotatedDeclaredType receiverType;
        protected AnnotatedTypeMirror returnType;
        protected final List<AnnotatedTypeMirror> throwsTypes =
                new ArrayList<AnnotatedTypeMirror>();
        protected final List<AnnotatedTypeVariable> typeVarTypes =
                new ArrayList<AnnotatedTypeVariable>();

        /**
         * @return true if this type represents a varargs method
         */
        public boolean isVarArgs() {
            return this.element.isVarArgs();
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitExecutable(this, p);
        }

        @Override
        public ExecutableType getUnderlyingType() {
            return this.actualType;
        }

        /* TODO: it never makes sense to add annotations to an executable type -
         * instead, they should be added to the right component.
         * For simpler, more regular use, we might want to allow querying for annotations.
         *
        @Override
        public void addAnnotations(Iterable<? extends AnnotationMirror> annotations) {
            //Thread.dumpStack();
            super.addAnnotations(annotations);
        }
        @Override
        public void addAnnotation(AnnotationMirror a) {
            //Thread.dumpStack();
            super.addAnnotation(a);
        }
        @Override
        public void addAnnotation(Class<? extends Annotation> a) {
            //Thread.dumpStack();
            super.addAnnotation(a);
        }

        @Override
        public Set<AnnotationMirror> getAnnotations() {
            Thread.dumpStack();
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof AnnotatedExecutableType))
                return false;
            // TODO compare components
            return true;
        }
        */

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
                    paramTypes.add(createType(t, atypeFactory, false));
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
         * The return type of a method or constructor.
         * For constructors, the return type is not VOID, but the type of
         * the enclosing class.
         *
         * @return the return type of this executable type
         */
        public AnnotatedTypeMirror getReturnType() {
            if (returnType == null
                    && element != null
                    && actualType.getReturnType() != null) {// lazy init
                TypeMirror aret = actualType.getReturnType();
                if (((MethodSymbol)element).isConstructor()) {
                    // For constructors, the underlying return type is void.
                    // Take the type of the enclosing class instead.
                    aret = element.getEnclosingElement().asType();
                }
                returnType = createType(aret, atypeFactory, false);
            }
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
         * @return the receiver type of this executable type;
         *   null for static methods and constructors of top-level classes
         */
        public /*@Nullable*/ AnnotatedDeclaredType getReceiverType() {
            if (receiverType == null &&
                    // Static methods don't have a receiver
                    !ElementUtils.isStatic(getElement()) &&
                    // Top-level constructors don't have a receiver
                    (getElement().getKind() != ElementKind.CONSTRUCTOR ||
                    getElement().getEnclosingElement().getEnclosingElement().getKind() != ElementKind.PACKAGE)) {
                TypeElement encl = ElementUtils.enclosingClass(getElement());
                if (getElement().getKind() == ElementKind.CONSTRUCTOR) {
                    // Can only reach this branch if we're the constructor of a nested class
                    encl =  ElementUtils.enclosingClass(encl.getEnclosingElement());
                }
                AnnotatedTypeMirror type = createType(encl.asType(), atypeFactory, false);
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
                    throwsTypes.add(createType(t, atypeFactory, false));
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
                            t, atypeFactory, true));
                }
            }
            return Collections.unmodifiableList(typeVarTypes);
        }

        @Override
        public AnnotatedExecutableType getCopy(boolean copyAnnotations) {
            AnnotatedExecutableType type =
                new AnnotatedExecutableType(getUnderlyingType(), atypeFactory);

            type.setElement(getElement());
            type.setParameterTypes(getParameterTypes());
            type.setReceiverType(getReceiverType());
            type.setReturnType(getReturnType());
            type.setThrownTypes(getThrownTypes());
            type.setTypeVariables(getTypeVariables());

            return type;
        }

        public /*@NonNull*/ ExecutableElement getElement() {
            return element;
        }

        public void setElement(/*@NonNull*/ ExecutableElement elem) {
            this.element = elem;
        }

        @Override
        public AnnotatedExecutableType getErased() {
            Types types = atypeFactory.types;
            AnnotatedExecutableType type =
                new AnnotatedExecutableType(
                        (ExecutableType) types.erasure(getUnderlyingType()),
                        atypeFactory);
            type.setElement(getElement());
            type.setParameterTypes(erasureList(getParameterTypes()));
            if (getReceiverType() != null) {
                type.setReceiverType(getReceiverType().getErased());
            } else {
                type.setReceiverType(null);
            }
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
                        ? extends AnnotatedTypeMirror> mappings, boolean forDeepCopy) {
            // Shouldn't substitute for methods!
            AnnotatedExecutableType type = getCopy(true);

            // Params
            {
                List<AnnotatedTypeMirror> params = new ArrayList<AnnotatedTypeMirror>();
                for (AnnotatedTypeMirror t : getParameterTypes()) {
                    params.add(t.substitute(mappings, forDeepCopy));
                }
                type.setParameterTypes(params);
            }

            if (getReceiverType() != null)
                type.setReceiverType((AnnotatedDeclaredType)getReceiverType().substitute(mappings, forDeepCopy));

            type.setReturnType(getReturnType().substitute(mappings, forDeepCopy));

            // Throws
            {
                List<AnnotatedTypeMirror> throwns = new ArrayList<AnnotatedTypeMirror>();
                for (AnnotatedTypeMirror t : getThrownTypes()) {
                    throwns.add(t.substitute(mappings, forDeepCopy));
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
                        bnd = bnd.substitute(mappings, forDeepCopy);
                        newtv.setUpperBound(bnd);
                    }
                    bnd = newtv.getLowerBoundField();
                    if (bnd != null) {
                        bnd = bnd.substitute(mappings, forDeepCopy);
                        newtv.setLowerBound(bnd);
                    }
                    mtvs.add(newtv);
                }
                type.setTypeVariables(mtvs);
            }

            return type;
        }

        @SideEffectFree
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
            if (getReturnType() != null) {
                sb.append(getReturnType().toString(printInvisible));
            } else {
                sb.append("<UNKNOWNRETURN>");
            }
            sb.append(' ');
            if (element != null) {
                sb.append(element.getSimpleName());
            } else {
                sb.append("METHOD");
            }
            sb.append('(');
            AnnotatedDeclaredType rcv = getReceiverType();
            if (rcv != null) {
                sb.append(rcv.toString(printInvisible));
                sb.append(" this");
            }
            if (!getParameterTypes().isEmpty()) {
                int p = 0;
                for (AnnotatedTypeMirror atm : getParameterTypes()) {
                    if (rcv != null ||
                            p > 0) {
                        sb.append(", ");
                    }
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
                        actualType.getComponentType(), atypeFactory, false));
            return componentType;
        }


        @Override
        public AnnotatedArrayType getCopy(boolean copyAnnotations) {
            AnnotatedArrayType type = new AnnotatedArrayType(actualType, atypeFactory);
            if (copyAnnotations)
                type.addAnnotations(annotations);
            type.setComponentType(getComponentType());
            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings, boolean forDeepCopy) {
            if (mappings.containsKey(this))
                return mappings.get(this);

            AnnotatedArrayType type = getCopy(true);
            AnnotatedTypeMirror c = getComponentType();
            AnnotatedTypeMirror cs = c.substitute(mappings, forDeepCopy);
            type.setComponentType(cs);
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

        @SideEffectFree
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

        private AnnotatedTypeVariable(TypeVariable type,
                AnnotatedTypeFactory factory, boolean declaration) {
            super(type, factory);
            this.declaration = declaration;
        }

        /** The lower bound of the type variable. **/
        private AnnotatedTypeMirror lowerBound;

        /** The upper bound of the type variable. **/
        private AnnotatedTypeMirror upperBound;

        private boolean declaration;

        @Override
        public boolean isDeclaration() {
            return declaration;
        }

        @Override
        public void addAnnotation(AnnotationMirror a) {
            super.addAnnotation(a);
            fixupBoundAnnotations();
        }

        /**
         * Change whether this {@code AnnotatedTypeVariable} is considered a use or a declaration
         * (use this method with caution).
         *
         * @param declaration  true if this type variable should be considered a declaration
         */
        public void setDeclaration(boolean declaration) {
            this.declaration = declaration;
        }

        @Override
        public AnnotatedTypeVariable asUse() {
            if (!this.isDeclaration()) {
                return this;
            }

            AnnotatedTypeVariable result = this.getCopy(true);
            result.declaration = false;

            return result;
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitTypeVariable(this, p);
        }

        @Override
        public TypeVariable getUnderlyingType() {
            return (TypeVariable) this.actualType;
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
            if (type != null)
                type = type.asUse();
            this.lowerBound = type;
        }

        /**
         * Sets the lower bound of this type variable without calling asUse (and therefore making a copy)
         * @param type
         */
        void setLowerBoundField(AnnotatedTypeMirror type) {
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
            if (lowerBound == null) { // lazy init
                BoundsInitializer.initializeLowerBound(this);
            }
            fixupBoundAnnotations();
            return lowerBound;
        }

        /**
         * @return the effective lower bound:  the lower bound,
         * with annotations on the type variable considered.
        */
        public AnnotatedTypeMirror getEffectiveLowerBound() {
            return getLowerBound();
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

            //We allow the above replacement first because primary annotations might not have annotations for
            //all hierarchies, so we don't want to avoid placing bottom on the lower bound for those hierarchies that
            //don't have a qualifier in primaryAnnotations
            if(!annotations.isEmpty()) {
                if(upperBound!=null) {
                    replaceUpperBoundAnnotations();
                }

                //Note:
                // if the lower bound is a type variable
                // then when we place annotations on the primary annotation
                //   this will actually cause the type variable to be exact and
                //   propagate the primary annotation to the type variable because
                //   primary annotations overwrite the upper and lower bounds of type variables
                //   when getUpperBound/getLowerBound is called
                if(lowerBound != null) {
                    lowerBound.replaceAnnotations(annotations);
                }
            }
        }


        /**
         * Replaces (or adds if none exist) the primary annotation of all upper bounds of typeVar,
         * the AnnotatedTypeVariable with the annotations provided.  The AnnotatedTypeVariable will only
         * have multiple upper bounds if the upper bound is an intersection.
         */
        private void replaceUpperBoundAnnotations() {
            if (upperBound.getKind() == TypeKind.INTERSECTION) {
                final List<AnnotatedDeclaredType> bounds = ((AnnotatedIntersectionType) upperBound).directSuperTypes();
                for (final AnnotatedDeclaredType bound : bounds) {
                    bound.replaceAnnotations(annotations);
                }
            } else {
                upperBound.replaceAnnotations(annotations);
            }
        }

        /**
         * Set the upper bound of this variable type
         * @param type the upper bound type
         */
        void setUpperBound(AnnotatedTypeMirror type) {
            if(type.isDeclaration()) {
                ErrorReporter.errorAbort("Upper bounds should never contain declarations.\n"
                                       + "type=" + type);
            }
            this.upperBound = type;
        }

        /**
         * Set the upper bound of this variable type without making a copy using asUse
         * @param type the upper bound type
         */
        void setUpperBoundField(final AnnotatedTypeMirror type) {
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
            if (upperBound == null) { // lazy init
                BoundsInitializer.initializeUpperBound(this);
            }
            fixupBoundAnnotations();
            return upperBound;
        }

        /**
         * @return the effective upper bound:  the upper bound,
         * with annotations on the type variable considered.
        */
        public AnnotatedTypeMirror getEffectiveUpperBound() {
            return getUpperBound(); //TODO: REMOVE getEffectiveUpperBound
        }

        public AnnotatedTypeParameterBounds getBounds() {
            return new AnnotatedTypeParameterBounds(getUpperBound(), getLowerBound());
        }

        public AnnotatedTypeParameterBounds getBoundFields() {
            return new AnnotatedTypeParameterBounds(getUpperBoundField(), getLowerBoundField());
        }

        public AnnotatedTypeParameterBounds getEffectiveBounds() {
            return new AnnotatedTypeParameterBounds(getEffectiveUpperBound(), getEffectiveLowerBound());
        }

        /**
         *  Used to terminate recursion into upper bounds.
         */
        private boolean inUpperBounds = false;

        @Override
        public AnnotatedTypeVariable getCopy(boolean copyAnnotations) {
            AnnotatedTypeVariable type =
                new AnnotatedTypeVariable(((TypeVariable)actualType), atypeFactory, declaration);

            if (copyAnnotations) {
                type.addAnnotations(annotations);
            }

            if (!inUpperBounds) {
                inUpperBounds = true;
                type.inUpperBounds = true;
                type.setUpperBound(getUpperBound().getCopy(true));
                inUpperBounds = false;
                type.inUpperBounds = false;
            }

            type.setLowerBound(getLowerBound().getCopy(true));

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
        V mapGetHelper(Map<K, V> mappings, AnnotatedTypeVariable key, boolean forDeepCopy) {
            // Search through `mappings` for an ATV which represents the declaration of the type
            // variable `key`.
            for (Map.Entry<K, V> entry : mappings.entrySet()) {
                K possible = entry.getKey();
                V possValue = entry.getValue();
                if (possible instanceof AnnotatedTypeVariable) {

                    AnnotatedTypeVariable other = (AnnotatedTypeVariable)possible;
                    Element oElt = other.getUnderlyingType().asElement();

                    if (key.getUnderlyingType().asElement().equals(oElt)) {
                        // The underlying `Element` is the same for `key` and `other`, so `other` is
                        // the declaration of `key`.  Replace type variable `other` with type
                        // `possValue` at type variable use `key`.
                        @SuppressWarnings("unchecked")
                        V found = (V)possValue.getCopy(false);
                        found.addAnnotations(possValue.getAnnotations());
                        if (!forDeepCopy) {
                            key.atypeFactory.postTypeVarSubstitution((AnnotatedTypeVariable)possible, key, found);
                        }
                        return found;
                    }
                }
            }
            return null;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings, boolean forDeepCopy) {
            AnnotatedTypeMirror found = mapGetHelper(mappings, this, forDeepCopy);
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
                type.setLowerBound(lowerBound.substitute(newMappings, forDeepCopy));
            }
            if (upperBound != null) {
                type.setUpperBound(upperBound.substitute(newMappings, forDeepCopy));
            }
            return type;
        }

        // Style taken from Type
        boolean isPrintingBound = false;

        @SideEffectFree
        @Override
        public String toString(boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            if (declaration) {
                sb.append("/*DECL*/ ");
            }

            sb.append(actualType);
            if (!isPrintingBound) {
                try {
                    isPrintingBound = true;
                    sb.append("[");
                    printBound("super",   getLowerBoundField(), printInvisible, sb);
                    printBound("extends", getUpperBoundField(), printInvisible, sb);
                    sb.append("]");
                } finally {
                    isPrintingBound = false;
                }
            }
            return sb.toString();
        }

        @Pure
        @Override
        public int hashCode() {
            return this.getUnderlyingType().hashCode();
        }

        /**
         * This method returns the type parameter declaration corresponding
         * to this type variable.
         * TODO: this should be a separate class, something like AnnotatedTypeParameter,
         * which is not a subtype of AnnotatedTypeMirror.
         * At the moment, it is a ATV without qualifiers, suitable for use in
         * class/method type argument mappings.
         *
         * @return The type parameter declaration.
         */
        public AnnotatedTypeVariable getTypeParameterDeclaration() {
            AnnotatedTypeVariable res = this.getCopy(false);
            res.declaration = true;
            return res;
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

        private AnnotatedNoType(NoType type, AnnotatedTypeFactory factory) {
            super(type, factory);
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
            return (NoType) this.actualType;
        }

        @Override
        public AnnotatedNoType getCopy(boolean copyAnnotations) {
            AnnotatedNoType type = new AnnotatedNoType((NoType) actualType, atypeFactory);
            if (copyAnnotations)
                type.addAnnotations(annotations);
            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings, boolean forDeepCopy) {
            // Cannot substitute
            return getCopy(true);
        }
    }

    /**
     * Represents the null type. This is the type of the expression {@code null}.
     */
    public static class AnnotatedNullType extends AnnotatedTypeMirror
    implements AnnotatedReferenceType {

        private AnnotatedNullType(NullType type, AnnotatedTypeFactory factory) {
            super(type, factory);
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitNull(this, p);
        }

        @Override
        public NullType getUnderlyingType() {
            return (NullType) this.actualType;
        }

        @Override
        public AnnotatedNullType getCopy(boolean copyAnnotations) {
            AnnotatedNullType type = new AnnotatedNullType((NullType) actualType, atypeFactory);
            if (copyAnnotations)
                type.addAnnotations(annotations);
            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings, boolean forDeepCopy) {
            // cannot substitute
            return getCopy(true);
        }

        @SideEffectFree
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

        private AnnotatedPrimitiveType(PrimitiveType type,
                AnnotatedTypeFactory factory) {
            super(type, factory);
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitPrimitive(this, p);
        }

        @Override
        public PrimitiveType getUnderlyingType() {
            return (PrimitiveType) this.actualType;
        }

        @Override
        public AnnotatedPrimitiveType getCopy(boolean copyAnnotations) {
            AnnotatedPrimitiveType type =
                new AnnotatedPrimitiveType((PrimitiveType) actualType, atypeFactory);
            if (copyAnnotations)
                type.addAnnotations(annotations);
            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings, boolean forDeepCopy) {
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

        private AnnotatedWildcardType(WildcardType type, AnnotatedTypeFactory factory) {
            super(type, factory);
        }

        @Override
        public void addAnnotation(AnnotationMirror a) {
            super.addAnnotation(a);
            fixupBoundAnnotations();
        }

        /**
         * Sets the super bound of this wild card
         *
         * @param type  the type of the lower bound
         */
        void setSuperBound(AnnotatedTypeMirror type) {
            if (type != null)
                type = type.asUse();
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
            if ( superBound == null ) {
                BoundsInitializer.initializeSuperBound(this);
            }
            fixupBoundAnnotations();
            return this.superBound;
        }

        /**
         * @return the lower bound of this wildcard. If no lower bound is
         * explicitly declared, {@code null} is returned.
         */
        public AnnotatedTypeMirror getEffectiveSuperBound() {
            return AnnotatedTypes.deepCopy(getSuperBound());
        }

        /**
         * Sets the upper bound of this wild card
         *
         * @param type  the type of the upper bound
         */
        void setExtendsBound(AnnotatedTypeMirror type) {
            if (type != null)
                type = type.asUse();

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
                BoundsInitializer.initializeExtendsBound(this);
            }

            fixupBoundAnnotations();
            return this.extendsBound;
        }

        public AnnotatedTypeMirror getEffectiveExtendsBound() {
            return AnnotatedTypes.deepCopy(getExtendsBound());
        }

        private void fixupBoundAnnotations() {
            if(!this.annotations.isEmpty()) {
                if (superBound != null) {
                    superBound.replaceAnnotations(this.annotations);
                }
                if (extendsBound != null) {
                    extendsBound.replaceAnnotations(this.annotations);
                }
            }
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitWildcard(this, p);
        }

        @Override
        public WildcardType getUnderlyingType() {
            return (WildcardType) this.actualType;
        }

        @Override
        public AnnotatedWildcardType getCopy(boolean copyAnnotations) {
            AnnotatedWildcardType type = new AnnotatedWildcardType((WildcardType) actualType, atypeFactory);
            type.setExtendsBound(getExtendsBound().getCopy(true));
            type.setSuperBound(getSuperBound().getCopy(true));
            if (copyAnnotations)
                type.addAnnotations(annotations);

            type.typeArgHack = typeArgHack;

            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings, boolean forDeepCopy) {

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
                type.setExtendsBound(extendsBound.substitute(newMapping, forDeepCopy));
            if (superBound != null)
                type.setSuperBound(superBound.substitute(newMapping, forDeepCopy));

            if (type.getExtendsBound() != null &&
                    type.getSuperBound() != null &&
                    AnnotatedTypes.areSame(type.getExtendsBound(), type.getSuperBound())) {
                return type.getExtendsBound();
            } else {
                return type;
            }
        }

        @Override
        public AnnotatedTypeMirror getErased() {
            // |? extends A&B| = |A|
            return getEffectiveExtendsBound().getErased();
        }

        boolean isPrintingBound = false;

        @SideEffectFree
        @Override
        public String toString(boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            sb.append(formatAnnotationString(annotations, printInvisible));
            sb.append("?");
            if (!isPrintingBound) {
                try {
                    isPrintingBound = true;
                    sb.append("[");
                    printBound("super",   getSuperBoundField(),   printInvisible, sb);
                    printBound("extends", getExtendsBoundField(), printInvisible, sb);
                    sb.append("]");
                } finally {
                    isPrintingBound = false;
                }
            }
            return sb.toString();
        }

        // Remove the typeArgHack once method type
        // argument inference and raw type handling is improved.
        private boolean typeArgHack = false;

        /* package-scope */ void setTypeArgHack() {
            typeArgHack = true;
        }

        /* package-scope */ boolean isTypeArgHack() {
            return typeArgHack;
        }
    }

    public static class AnnotatedIntersectionType extends AnnotatedTypeMirror {

        /**
         * Constructor for this type
         *
         * @param type  underlying kind of this type
         * @param atypeFactory TODO
         */
        private AnnotatedIntersectionType(IntersectionType type,
                AnnotatedTypeFactory atypeFactory) {
            super(type, atypeFactory);
        }

        @SideEffectFree
        @Override
        public String toString(boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            // Prevent an infinite recursion that might happen when calling toString
            // within deepCopy, caused by postAsSuper in (at least) the IGJ Checker.
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
                    new AnnotatedIntersectionType((IntersectionType) actualType, atypeFactory);
            if (copyAnnotations)
                type.addAnnotations(annotations);
            type.supertypes = this.supertypes;
            return type;
        }

        protected List<AnnotatedDeclaredType> supertypes;

        @Override
        public List<AnnotatedDeclaredType> directSuperTypes() {
            if (supertypes == null) {
                List<? extends TypeMirror> ubounds = ((IntersectionType)actualType).getBounds();
                List<AnnotatedDeclaredType> res = new ArrayList<AnnotatedDeclaredType>(ubounds.size());
                for (TypeMirror bnd : ubounds) {
                    res.add((AnnotatedDeclaredType) createType(bnd, atypeFactory, false));
                }
                supertypes = Collections.unmodifiableList(res);
            }
            return supertypes;
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
                    ? extends AnnotatedTypeMirror> mappings, boolean forDeepCopy) {
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
                    supertypes.add((AnnotatedDeclaredType)t.substitute(newMappings, forDeepCopy));
                type.supertypes = supertypes;
            }
            return type;
        }
    }


    // TODO: Ensure union types are handled everywhere.
    // TODO: Should field "annotations" contain anything?
    public static class AnnotatedUnionType extends AnnotatedTypeMirror {

        /**
         * Constructor for this type
         *
         * @param type  underlying kind of this type
         * @param atypeFactory TODO
         */
        private AnnotatedUnionType(UnionType type,
                AnnotatedTypeFactory atypeFactory) {
            super(type, atypeFactory);
        }

        @SideEffectFree
        @Override
        public String toString(boolean printInvisible) {
            StringBuilder sb = new StringBuilder();

            boolean isFirst = true;
            for(AnnotatedDeclaredType adt : this.getAlternatives()) {
                if (!isFirst) sb.append(" | ");
                sb.append(adt.toString(printInvisible));
                isFirst = false;
            }
            return sb.toString();
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitUnion(this, p);
        }

        @Override
        public AnnotatedUnionType getCopy(boolean copyAnnotations) {
            AnnotatedUnionType type =
                    new AnnotatedUnionType((UnionType) actualType, atypeFactory);
            if (copyAnnotations)
                type.addAnnotations(annotations);
            type.alternatives = this.alternatives;
            return type;
        }

        protected List<AnnotatedDeclaredType> alternatives;

        public List<AnnotatedDeclaredType> getAlternatives() {
            if (alternatives == null) {
                List<? extends TypeMirror> ualts = ((UnionType)actualType).getAlternatives();
                List<AnnotatedDeclaredType> res = new ArrayList<AnnotatedDeclaredType>(ualts.size());
                for (TypeMirror alt : ualts) {
                    res.add((AnnotatedDeclaredType) createType(alt, atypeFactory, false));
                }
                alternatives = Collections.unmodifiableList(res);
            }
            return alternatives;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                    ? extends AnnotatedTypeMirror> mappings, boolean forDeepCopy) {
            if (mappings.containsKey(this))
                return mappings.get(this);

            AnnotatedUnionType type = getCopy(true);

            Map<AnnotatedTypeMirror, AnnotatedTypeMirror> newMappings =
                    new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>(mappings);
            newMappings.put(this, type);

            if (this.alternatives != null) {
                // watch need to copy alternatives as well
                List<AnnotatedDeclaredType> alternatives = new ArrayList<AnnotatedDeclaredType>();
                for (AnnotatedDeclaredType t : getAlternatives())
                    alternatives.add((AnnotatedDeclaredType)t.substitute(newMappings, forDeepCopy));
                type.alternatives = alternatives;
            }
            return type;
        }
    }



    public List<? extends AnnotatedTypeMirror> directSuperTypes() {
        return directSuperTypes(this);
    }

    // Version of method below for declared types
    protected final List<AnnotatedDeclaredType> directSuperTypes(AnnotatedDeclaredType type) {
        SupertypeFinder superTypeFinder = new SupertypeFinder(type.atypeFactory);
        List<AnnotatedDeclaredType> supertypes = superTypeFinder.visitDeclared(type, null);
        atypeFactory.postDirectSuperTypes(type, supertypes);
        return supertypes;
    }

    // Version of method above for all types
    private final List<? extends AnnotatedTypeMirror> directSuperTypes(AnnotatedTypeMirror type) {
        SupertypeFinder superTypeFinder = new SupertypeFinder(type.atypeFactory);
        List<? extends AnnotatedTypeMirror> supertypes = superTypeFinder.visit(type, null);
        atypeFactory.postDirectSuperTypes(type, supertypes);
        return supertypes;
    }

    /** print to sb keyWord followed by field.  NULL types are substituted with
     * their annotations followed by " Void"
     */
    private static void printBound(final String keyWord, final AnnotatedTypeMirror field,
                            final boolean printInvisible, final StringBuilder sb) {
        sb.append(" ");
        sb.append(keyWord);
        sb.append(" ");

        if(field == null) {
            sb.append("<null>");
        } else if(field.getKind() != TypeKind.NULL) {
            sb.append(field.toString(printInvisible));
        } else {
            sb.append(formatAnnotationString(field.getAnnotations(), printInvisible));
            sb.append("Void");
        }
    }

}
