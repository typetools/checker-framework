package org.checkerframework.framework.type;

/*>>>
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.interning.qual.*;
*/

import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
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
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.visitor.AnnotatedTypeVisitor;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;

/**
 * Represents an annotated type in the Java programming language. Types include primitive types,
 * declared types (class and interface types), array types, type variables, and the null type. Also
 * represented are wildcard type arguments, the signature and return types of executables, and
 * pseudo-types corresponding to packages and to the keyword {@code void}.
 *
 * <p>Types should be compared using the utility methods in {@link AnnotatedTypes}. There is no
 * guarantee that any particular type will always be represented by the same object.
 *
 * <p>To implement operations based on the class of an {@code AnnotatedTypeMirror} object, either
 * use a visitor or use the result of the {@link #getKind()} method.
 *
 * @see TypeMirror
 */
public abstract class AnnotatedTypeMirror {

    /**
     * Creates the appropriate AnnotatedTypeMirror specific wrapper for the provided type
     *
     * @param isDeclaration true if the result should is a type declaration
     */
    public static AnnotatedTypeMirror createType(
            TypeMirror type, AnnotatedTypeFactory atypeFactory, boolean isDeclaration) {
        if (type == null) {
            ErrorReporter.errorAbort(
                    "AnnotatedTypeMirror.createType: input type must not be null!");
            return null;
        }

        AnnotatedTypeMirror result;
        switch (type.getKind()) {
            case ARRAY:
                result = new AnnotatedArrayType((ArrayType) type, atypeFactory);
                break;
            case DECLARED:
                result =
                        new AnnotatedDeclaredType((DeclaredType) type, atypeFactory, isDeclaration);
                break;
            case ERROR:
                ErrorReporter.errorAbort(
                        "AnnotatedTypeMirror.createType: input should type-check already! Found error type: "
                                + type);
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
                result =
                        new AnnotatedTypeVariable((TypeVariable) type, atypeFactory, isDeclaration);
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
                ErrorReporter.errorAbort(
                        "AnnotatedTypeMirror.createType: unidentified type "
                                + type
                                + " ("
                                + type.getKind()
                                + ")");
                return null; // dead code
        }
        /*if (jctype.isAnnotated()) {
            result.addAnnotations(jctype.getAnnotationMirrors());
        }*/
        return result;
    }

    protected static final EqualityAtmComparer equalityComparer = new EqualityAtmComparer();
    protected static final HashcodeAtmVisitor hashcodeVisitor = new HashcodeAtmVisitor();

    /** The factory to use for lazily creating annotated types. */
    protected final AnnotatedTypeFactory atypeFactory;

    /** Actual type wrapped with this AnnotatedTypeMirror */
    protected final TypeMirror actualType;

    /** The annotations on this type. */
    // AnnotationMirror doesn't override Object.hashCode, .equals, so we use
    // the class name of Annotation instead.
    // Caution: Assumes that a type can have at most one AnnotationMirror for
    // any Annotation type. JSR308 is pushing to have this change.
    protected final Set<AnnotationMirror> annotations = AnnotationUtils.createAnnotationSet();

    /** The explicitly written annotations on this type. */
    // TODO: use this to cache the result once computed? For generic types?
    // protected final Set<AnnotationMirror> explicitannotations =
    // AnnotationUtils.createAnnotationSet();

    /**
     * Constructor for AnnotatedTypeMirror.
     *
     * @param type the underlying type
     * @param atypeFactory used to create further types and to access global information (Types,
     *     Elements, ...)
     */
    private AnnotatedTypeMirror(TypeMirror type, AnnotatedTypeFactory atypeFactory) {
        this.actualType = type;
        assert atypeFactory != null;
        this.atypeFactory = atypeFactory;
    }

    @Override
    public final boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null || !(o instanceof AnnotatedTypeMirror)) {
            return false;
        }

        return equalityComparer.visit(this, (AnnotatedTypeMirror) o, null);
    }

    @Pure
    @Override
    public final int hashCode() {
        return hashcodeVisitor.visit(this);
    }

    /**
     * Applies a visitor to this type.
     *
     * @param <R> the return type of the visitor's methods
     * @param <P> the type of the additional parameter to the visitor's methods
     * @param v the visitor operating on this type
     * @param p additional parameter to the visitor
     * @return a visitor-specified result
     */
    public abstract <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p);

    /**
     * Returns the {@code kind} of this type
     *
     * @return the kind of this type
     */
    public TypeKind getKind() {
        return actualType.getKind();
    }

    /**
     * Returns the underlying unannotated Java type, which this wraps
     *
     * @return the underlying type
     */
    public TypeMirror getUnderlyingType() {
        return actualType;
    }

    /**
     * Returns true if this type mirror represents a declaration, rather than a use, of a type.
     *
     * <p>For example, {@code class List<T> { ... }} declares a new type {@code List<T>}, while
     * {@code List<Integer>} is a use of the type.
     *
     * @return true if this represents a declaration
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
     * <p>It doesn't account for annotations in deep types (type arguments, array components, etc).
     *
     * @param p the qualifier hierarchy to check for
     * @return true iff an annotation from the same hierarchy as p is present
     */
    public boolean isAnnotatedInHierarchy(AnnotationMirror p) {
        return getAnnotationInHierarchy(p) != null;
    }

    /**
     * Returns an annotation from the given sub-hierarchy, if such an annotation targets this type;
     * otherwise returns null.
     *
     * <p>It doesn't account for annotations in deep types (type arguments, array components, etc).
     *
     * @param p the qualifier hierarchy to check for
     * @return an annotation from the same hierarchy as p if present
     */
    public AnnotationMirror getAnnotationInHierarchy(AnnotationMirror p) {
        AnnotationMirror aliased = p;
        if (!atypeFactory.isSupportedQualifier(aliased)) {
            aliased = atypeFactory.aliasedAnnotation(p);
        }
        if (atypeFactory.isSupportedQualifier(aliased)) {
            QualifierHierarchy qualHier = this.atypeFactory.getQualifierHierarchy();
            AnnotationMirror anno = qualHier.findAnnotationInSameHierarchy(annotations, aliased);
            if (anno != null) {
                return anno;
            }
        }
        return null;
    }

    /**
     * Returns an annotation from the given sub-hierarchy, if such an annotation is present on this
     * type or on its extends bounds; otherwise returns null.
     *
     * <p>It doesn't account for annotations in deep types (type arguments, array components, etc).
     *
     * @param p the qualifier hierarchy to check for
     * @return an annotation from the same hierarchy as p if present
     */
    public AnnotationMirror getEffectiveAnnotationInHierarchy(AnnotationMirror p) {
        AnnotationMirror aliased = p;
        if (!atypeFactory.isSupportedQualifier(aliased)) {
            aliased = atypeFactory.aliasedAnnotation(p);
        }
        if (atypeFactory.isSupportedQualifier(aliased)) {
            QualifierHierarchy qualHier = this.atypeFactory.getQualifierHierarchy();
            AnnotationMirror anno =
                    qualHier.findAnnotationInSameHierarchy(getEffectiveAnnotations(), aliased);
            if (anno != null) {
                return anno;
            }
        }
        return null;
    }

    /**
     * Returns the annotations on this type.
     *
     * <p>It does not include annotations in deep types (type arguments, array components, etc).
     *
     * @return a unmodifiable set of the annotations on this
     */
    public final Set<AnnotationMirror> getAnnotations() {
        return Collections.unmodifiableSet(annotations);
    }

    /**
     * Returns the annotations on this type.
     *
     * <p>It does not include annotations in deep types (type arguments, array components, etc).
     *
     * <p>The returned set should not be modified, but for efficiency reasons modification is not
     * prevented. Modifications might break invariants.
     *
     * @return the set of the annotations on this, directly
     */
    protected final Set<AnnotationMirror> getAnnotationsField() {
        return annotations;
    }

    /**
     * Returns the "effective" annotations on this type, i.e. the annotations on the type itself, or
     * on the upper/extends bound of a type variable/wildcard (recursively, until a class type is
     * reached).
     *
     * @return a set of the annotations on this
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
     * Returns the actual annotation mirror used to annotate this type, whose Class equals the
     * passed annoClass if one exists, null otherwise.
     *
     * @param annoClass annotation class
     * @return the annotation mirror for anno
     */
    public AnnotationMirror getAnnotation(Class<? extends Annotation> annoClass) {
        for (AnnotationMirror annoMirror : annotations) {
            if (AnnotationUtils.areSameByClass(annoMirror, annoClass)) {
                return annoMirror;
            }
        }
        return null;
    }

    /**
     * Returns the set of explicitly written annotations on this type that are supported by this
     * checker. This is useful to check the validity of annotations explicitly present on a type, as
     * flow inference might add annotations that were not previously present. Note that since
     * AnnotatedTypeMirror instances are created for type uses, this method will return explicit
     * annotations in type use locations but will not return explicit annotations that had an impact
     * on defaulting, such as an explicit annotation on a class declaration. For example, given:
     *
     * <p>{@code @MyExplicitAnno class MyClass { }; MyClass myClassInstance; }
     *
     * <p>the result of calling {@code
     * atypeFactory.getAnnotatedType(variableTreeForMyClassInstance).getExplicitAnnotations()}
     *
     * <p>will not contain {@code @MyExplicitAnno}.
     *
     * @return the set of explicitly written annotations on this type that are supported by this
     *     checker
     */
    public Set<AnnotationMirror> getExplicitAnnotations() {
        // TODO JSR 308: The explicit type annotations should be always present
        Set<AnnotationMirror> explicitAnnotations = AnnotationUtils.createAnnotationSet();
        List<? extends AnnotationMirror> typeAnnotations =
                this.getUnderlyingType().getAnnotationMirrors();

        Set<? extends AnnotationMirror> validAnnotations =
                atypeFactory.getQualifierHierarchy().getTypeQualifiers();
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
     * Determines whether this type contains the given annotation. This method considers the
     * annotation's values, that is, if the type is "@A("s") @B(3) Object" a call with "@A("t") or
     * "@A" will return false, whereas a call with "@B(3)" will return true.
     *
     * <p>In contrast to {@link #hasAnnotationRelaxed(AnnotationMirror)} this method also compares
     * annotation values.
     *
     * @param a the annotation to check for
     * @return true iff the type contains the annotation {@code a}
     * @see #hasAnnotationRelaxed(AnnotationMirror)
     */
    public boolean hasAnnotation(AnnotationMirror a) {
        return AnnotationUtils.containsSame(annotations, a);
    }

    /**
     * Determines whether this type contains an annotation with the same annotation type as a
     * particular annotation. This method does not consider an annotation's values.
     *
     * @param a the class of annotation to check for
     * @return true iff the type contains an annotation with the same type as the annotation given
     *     by {@code a}
     */
    public boolean hasAnnotation(Class<? extends Annotation> a) {
        return getAnnotation(a) != null;
    }

    /**
     * Returns the actual effective annotation mirror used to annotate this type, whose Class equals
     * the passed annoClass if one exists, null otherwise.
     *
     * @param annoClass annotation class
     * @return the annotation mirror for anno
     */
    public AnnotationMirror getEffectiveAnnotation(Class<? extends Annotation> annoClass) {
        for (AnnotationMirror annoMirror : getEffectiveAnnotations()) {
            if (AnnotationUtils.areSameByClass(annoMirror, annoClass)) {
                return annoMirror;
            }
        }
        return null;
    }

    /**
     * A version of hasAnnotation that considers annotations on the upper bound of wildcards and
     * type variables.
     *
     * @see #hasAnnotation(Class)
     */
    public boolean hasEffectiveAnnotation(Class<? extends Annotation> a) {
        return getEffectiveAnnotation(a) != null;
    }

    /**
     * A version of hasAnnotation that considers annotations on the upper bound of wildcards and
     * type variables.
     *
     * @see #hasAnnotation(AnnotationMirror)
     */
    public boolean hasEffectiveAnnotation(AnnotationMirror a) {
        return AnnotationUtils.containsSame(getEffectiveAnnotations(), a);
    }

    /**
     * Determines whether this type contains the given annotation explicitly written at declaration.
     * This method considers the annotation's values, that is, if the type is "@A("s") @B(3) Object"
     * a call with "@A("t") or "@A" will return false, whereas a call with "@B(3)" will return true.
     *
     * <p>In contrast to {@link #hasExplicitAnnotationRelaxed(AnnotationMirror)} this method also
     * compares annotation values.
     *
     * <p>See the documentation for {@link #getExplicitAnnotations()} for details on which explicit
     * annotations are not included.
     *
     * @param a the annotation to check for
     * @return true iff the annotation {@code a} is explicitly written on the type
     * @see #hasExplicitAnnotationRelaxed(AnnotationMirror)
     * @see #getExplicitAnnotations()
     */
    public boolean hasExplicitAnnotation(AnnotationMirror a) {
        return AnnotationUtils.containsSame(getExplicitAnnotations(), a);
    }

    /**
     * Determines whether this type contains an annotation with the same annotation type as a
     * particular annotation. This method does not consider an annotation's values, that is, if the
     * type is "@A("s") @B(3) Object" a call with "@A("t"), "@A", or "@B" will return true.
     *
     * @param a the annotation to check for
     * @return true iff the type contains an annotation with the same type as the annotation given
     *     by {@code a}
     * @see #hasAnnotation(AnnotationMirror)
     */
    public boolean hasAnnotationRelaxed(AnnotationMirror a) {
        return AnnotationUtils.containsSameIgnoringValues(annotations, a);
    }

    /**
     * A version of hasAnnotationRelaxed that considers annotations on the upper bound of wildcards
     * and type variables.
     *
     * @see #hasAnnotationRelaxed(AnnotationMirror)
     */
    public boolean hasEffectiveAnnotationRelaxed(AnnotationMirror a) {
        return AnnotationUtils.containsSameIgnoringValues(getEffectiveAnnotations(), a);
    }

    /**
     * A version of hasAnnotationRelaxed that only considers annotations that are explicitly written
     * on the type.
     *
     * <p>See the documentation for {@link #getExplicitAnnotations()} for details on which explicit
     * annotations are not included.
     *
     * @see #hasAnnotationRelaxed(AnnotationMirror)
     * @see #getExplicitAnnotations()
     */
    public boolean hasExplicitAnnotationRelaxed(AnnotationMirror a) {
        return AnnotationUtils.containsSameIgnoringValues(getExplicitAnnotations(), a);
    }

    /**
     * Determines whether this type contains an explicitly written annotation with the same
     * annotation type as a particular annotation. This method does not consider an annotation's
     * values.
     *
     * <p>See the documentation for {@link #getExplicitAnnotations()} for details on which explicit
     * annotations are not included.
     *
     * @param a the class of annotation to check for
     * @return true iff the type contains an explicitly written annotation with the same type as the
     *     annotation given by {@code a}
     * @see #getExplicitAnnotations()
     */
    public boolean hasExplicitAnnotation(Class<? extends Annotation> a) {
        return AnnotationUtils.containsSameIgnoringValues(
                getExplicitAnnotations(), getAnnotation(a));
    }

    /**
     * Adds an annotation to this type. Only annotations supported by the type factory are added.
     *
     * @param a the annotation to add
     */
    public void addAnnotation(AnnotationMirror a) {
        if (a == null) {
            ErrorReporter.errorAbort(
                    "AnnotatedTypeMirror.addAnnotation: null is not a valid annotation.");
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
     * Adds an annotation to this type, removing any existing annotation from the same qualifier
     * hierarchy first.
     *
     * @param a the annotation to add
     */
    public void replaceAnnotation(AnnotationMirror a) {
        if (!AnnotationUtils.areSameByClass(a, PolyAll.class)) {
            this.removeAnnotationInHierarchy(a);
        }
        this.addAnnotation(a);
    }

    /**
     * Adds an annotation to this type.
     *
     * @param a the class of the annotation to add
     */
    public void addAnnotation(Class<? extends Annotation> a) {
        AnnotationMirror anno = AnnotationBuilder.fromClass(atypeFactory.elements, a);
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
     * Adds those annotations to the current type, for which no annotation from the same qualifier
     * hierarchy is present.
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
     * Adds multiple annotations to this type, removing any existing annotations from the same
     * qualifier hierarchy first.
     *
     * @param replAnnos the annotations to replace
     */
    public void replaceAnnotations(Iterable<? extends AnnotationMirror> replAnnos) {
        for (AnnotationMirror a : replAnnos) {
            this.replaceAnnotation(a);
        }
    }

    /**
     * Removes an annotation from the type.
     *
     * @param a the annotation to remove
     * @return true if the annotation was removed, false if the type's annotations were unchanged
     */
    public boolean removeAnnotation(AnnotationMirror a) {
        // Going from the AnnotationMirror to its name and then calling
        // getAnnotation ensures that we get the canonical AnnotationMirror that can be
        // removed.
        // TODO: however, this also means that if we are annotated with "@I(1)" and
        // remove "@I(2)" it will be removed. Is this what we want?
        // It's currently necessary for the Lock Checker.
        AnnotationMirror anno =
                AnnotationUtils.getAnnotationByName(annotations, AnnotationUtils.annotationName(a));
        if (anno != null) {
            return annotations.remove(anno);
        } else {
            return false;
        }
    }

    public boolean removeAnnotation(Class<? extends Annotation> a) {
        AnnotationMirror anno = AnnotationBuilder.fromClass(atypeFactory.elements, a);
        if (anno == null || !atypeFactory.isSupportedQualifier(anno)) {
            ErrorReporter.errorAbort(
                    "AnnotatedTypeMirror.removeAnnotation called with un-supported class: " + a);
        }
        return removeAnnotation(anno);
    }

    /**
     * Remove any annotation that is in the same qualifier hierarchy as the parameter.
     *
     * @param a an annotation from the same qualifier hierarchy
     * @return if an annotation was removed
     */
    public boolean removeAnnotationInHierarchy(AnnotationMirror a) {
        AnnotationMirror prev = this.getAnnotationInHierarchy(a);
        if (prev != null) {
            return this.removeAnnotation(prev);
        }
        return false;
    }

    /**
     * Remove an annotation that is in the same qualifier hierarchy as the parameter, unless it's
     * the top annotation.
     *
     * @param a an annotation from the same qualifier hierarchy
     * @return if an annotation was removed
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
     * @param annotations the annotations to remove
     * @return true if at least one annotation was removed, false if the type's annotations were
     *     unchanged
     */
    public boolean removeAnnotations(Iterable<? extends AnnotationMirror> annotations) {
        boolean changed = false;
        for (AnnotationMirror a : annotations) {
            changed |= this.removeAnnotation(a);
        }
        return changed;
    }

    /**
     * Removes all primary annotations on this type. Make sure to add an annotation again, e.g.
     * Unqualified.
     *
     * <p>This method should only be used in very specific situations. For individual type systems,
     * it is generally better to use {@link #removeAnnotation(AnnotationMirror)} and similar
     * methods.
     */
    public void clearAnnotations() {
        annotations.clear();
    }

    @SideEffectFree
    @Override
    public final String toString() {
        return atypeFactory.getAnnotatedTypeFormatter().format(this);
    }

    @SideEffectFree
    public final String toString(boolean verbose) {
        return atypeFactory.getAnnotatedTypeFormatter().format(this, verbose);
    }

    /**
     * Returns the erasure type of the this type, according to JLS specifications.
     *
     * @see <a
     *     href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.6">https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.6</a>
     * @return the erasure of this AnnotatedTypeMirror, this is always a copy even if the erasure
     *     and the original type are equivalent
     */
    public AnnotatedTypeMirror getErased() {
        return deepCopy();
    }

    /**
     * Returns a deep copy of this type. A deep copy implies that each component type is copied
     * recursively and the returned type refers to those copies in its component locations.
     *
     * <p>Note: deepCopy provides two important properties in the returned copy:
     *
     * <ol>
     *   <li>Structure preservation -- The exact structure of the original AnnotatedTypeMirror is
     *       preserved in the copy including all component types.
     *   <li>Annotation preservation -- All of the annotations from the original AnnotatedTypeMirror
     *       and its components have been copied to the new type.
     * </ol>
     *
     * If copyAnnotations is set to false, the second property, Annotation preservation, is removed.
     * This is useful for cases in which the user may want to copy the structure of a type exactly
     * but NOT its annotations.
     *
     * @return a deep copy
     */
    public abstract AnnotatedTypeMirror deepCopy(final boolean copyAnnotations);

    /**
     * @return a deep copy of this type with annotations
     * @see #deepCopy(boolean)
     */
    public abstract AnnotatedTypeMirror deepCopy();

    /**
     * Returns a shallow copy of this type. A shallow copy implies that each component type in the
     * output copy refers to the same object as the object being copie.
     *
     * @param copyAnnotations whether copy should have annotations, i.e. whether field {@code
     *     annotations} should be copied.
     */
    public abstract AnnotatedTypeMirror shallowCopy(boolean copyAnnotations);

    /**
     * Returns a shallow copy of this type with annotations.
     *
     * @see #shallowCopy(boolean)
     */
    public abstract AnnotatedTypeMirror shallowCopy();

    protected static AnnotatedDeclaredType createTypeOfObject(AnnotatedTypeFactory atypeFactory) {
        AnnotatedDeclaredType objectType =
                atypeFactory.fromElement(
                        atypeFactory.elements.getTypeElement(Object.class.getCanonicalName()));
        objectType.declaration = false;
        return objectType;
    }

    /** Represents a declared type (whether class or interface). */
    public static class AnnotatedDeclaredType extends AnnotatedTypeMirror {

        /** Parametrized Type Arguments */
        protected List<AnnotatedTypeMirror> typeArgs;

        /**
         * Whether the type was initially raw, i.e. the user did not provide the type arguments.
         * typeArgs will contain inferred type arguments, which might be too conservative at the
         * moment. TODO: improve inference.
         *
         * <p>Ideally, the field would be final. However, when we determine the supertype of a raw
         * type, we need to set wasRaw for the supertype.
         */
        private boolean wasRaw;

        /** The enclosing Type */
        protected AnnotatedDeclaredType enclosingType;

        private boolean declaration;

        /**
         * Constructor for this type
         *
         * @param type underlying kind of this type
         * @param atypeFactory the AnnotatedTypeFactory used to create this type
         */
        private AnnotatedDeclaredType(
                DeclaredType type, AnnotatedTypeFactory atypeFactory, boolean declaration) {
            super(type, atypeFactory);
            TypeElement typeelem = (TypeElement) type.asElement();
            DeclaredType declty = (DeclaredType) typeelem.asType();
            wasRaw = !declty.getTypeArguments().isEmpty() && type.getTypeArguments().isEmpty();

            TypeMirror encl = type.getEnclosingType();
            if (encl.getKind() == TypeKind.DECLARED) {
                this.enclosingType =
                        (AnnotatedDeclaredType) createType(encl, atypeFactory, declaration);
                // Force instantiation of type arguments of enclosing type.
                this.enclosingType.getTypeArguments();
            } else if (encl.getKind() != TypeKind.NONE) {
                ErrorReporter.errorAbort(
                        "AnnotatedDeclaredType: unsupported enclosing type: "
                                + type.getEnclosingType()
                                + " ("
                                + encl.getKind()
                                + ")");
            }

            this.declaration = declaration;
        }

        @Override
        public boolean isDeclaration() {
            return declaration;
        }

        @Override
        public AnnotatedDeclaredType deepCopy(boolean copyAnnotations) {
            return (AnnotatedDeclaredType) new AnnotatedTypeCopier(copyAnnotations).visit(this);
        }

        @Override
        public AnnotatedDeclaredType deepCopy() {
            return deepCopy(true);
        }

        @Override
        public AnnotatedDeclaredType asUse() {
            if (!this.isDeclaration()) {
                return this;
            }
            AnnotatedDeclaredType result = this.shallowCopy(true);
            result.declaration = false;
            // setTypeArguments calls asUse on all the new type arguments.
            result.setTypeArguments(typeArgs);

            return result;
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitDeclared(this, p);
        }

        /**
         * Sets the type arguments on this type
         *
         * @param ts the type arguments
         */
        // WMD
        public void setTypeArguments(List<? extends AnnotatedTypeMirror> ts) {
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

        /** @return the type argument for this type */
        public List<AnnotatedTypeMirror> getTypeArguments() {
            if (typeArgs != null) {
                return typeArgs;
            } else if (wasRaw()) {
                // Initialize the type arguments with uninferred wildcards.
                BoundsInitializer.initializeTypeArgs(this);

                // Copy annotations from the declaration to the wildcards.
                AnnotatedDeclaredType declaration =
                        atypeFactory.fromElement((TypeElement) getUnderlyingType().asElement());
                for (int i = 0; i < typeArgs.size(); i++) {
                    AnnotatedTypeVariable typeParam =
                            (AnnotatedTypeVariable) declaration.getTypeArguments().get(i);
                    AnnotatedWildcardType wct = (AnnotatedWildcardType) typeArgs.get(i);
                    wct.getExtendsBound().replaceAnnotations(typeParam.getUpperBound().annotations);
                    wct.getSuperBound().replaceAnnotations(typeParam.getLowerBound().annotations);
                    wct.replaceAnnotations(typeParam.annotations);
                }
                return typeArgs;
            } else if (getUnderlyingType().getTypeArguments().isEmpty()) {
                typeArgs = Collections.emptyList();
                return typeArgs;
            } else {
                // Initialize type argument for a non-raw declared type that has type arguments/
                BoundsInitializer.initializeTypeArgs(this);
                return typeArgs;
            }
        }

        /**
         * Returns true if the type was raw, that is, type arguments were not provided but instead
         * inferred.
         *
         * @return true iff the type was raw
         */
        public boolean wasRaw() {
            return wasRaw;
        }

        /**
         * Set the wasRaw flag to true. This should only be necessary when determining the
         * supertypes of a raw type.
         */
        protected void setWasRaw() {
            this.wasRaw = true;
        }

        @Override
        public DeclaredType getUnderlyingType() {
            return (DeclaredType) actualType;
        }

        @Override
        public List<AnnotatedDeclaredType> directSuperTypes() {
            return Collections.unmodifiableList(SupertypeFinder.directSuperTypes(this));
        }

        @Override
        public AnnotatedDeclaredType shallowCopy() {
            return shallowCopy(true);
        }

        @Override
        public AnnotatedDeclaredType shallowCopy(boolean copyAnnotations) {
            AnnotatedDeclaredType type =
                    new AnnotatedDeclaredType(getUnderlyingType(), atypeFactory, declaration);
            if (copyAnnotations) {
                type.addAnnotations(this.getAnnotationsField());
            }
            type.setEnclosingType(getEnclosingType());
            type.setTypeArguments(getTypeArguments());
            return type;
        }

        /**
         * Return the declared type with its type arguments removed. This also replaces the
         * underlying type with its erasure.
         *
         * @return a fresh copy of the declared type with no type arguments
         */
        @Override
        public AnnotatedDeclaredType getErased() {
            // 1. |G<T_1, ..., T_n>| = |G|
            // 2. |T.C| = |T|.C
            if (!getTypeArguments().isEmpty()) {
                // Handle case 1.
                AnnotatedDeclaredType rType =
                        (AnnotatedDeclaredType)
                                AnnotatedTypeMirror.createType(
                                        atypeFactory.types.erasure(actualType),
                                        atypeFactory,
                                        declaration);
                rType.addAnnotations(annotations);
                rType.setTypeArguments(Collections.emptyList());
                return rType.getErased();

            } else if ((getEnclosingType() != null)
                    && (getEnclosingType().getKind() != TypeKind.NONE)) {
                // Handle case 2
                // Shallow copy provides a fresh type when there are no type arguments
                // and we set the enclosing type
                // Therefore, we do not need to use deepCopy
                AnnotatedDeclaredType rType = shallowCopy();
                AnnotatedDeclaredType et = getEnclosingType();
                rType.setEnclosingType(et.getErased());
                return rType;

            } else {

                return this.deepCopy();
            }
        }

        /** Sets the enclosing type */
        /*default-visibility*/ void setEnclosingType(AnnotatedDeclaredType enclosingType) {
            this.enclosingType = enclosingType;
        }

        /**
         * Returns the enclosing type, as in the type of {@code A} in the type {@code A.B}.
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

        private ExecutableElement element;

        private AnnotatedExecutableType(ExecutableType type, AnnotatedTypeFactory factory) {
            super(type, factory);
        }

        protected final List<AnnotatedTypeMirror> paramTypes = new ArrayList<AnnotatedTypeMirror>();
        protected AnnotatedDeclaredType receiverType;
        protected AnnotatedTypeMirror returnType;
        protected final List<AnnotatedTypeMirror> throwsTypes =
                new ArrayList<AnnotatedTypeMirror>();
        protected final List<AnnotatedTypeVariable> typeVarTypes =
                new ArrayList<AnnotatedTypeVariable>();

        /** @return true if this type represents a varargs method */
        public boolean isVarArgs() {
            return this.element.isVarArgs();
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitExecutable(this, p);
        }

        @Override
        public ExecutableType getUnderlyingType() {
            return (ExecutableType) this.actualType;
        }

        /**
         * It never makes sense to add annotations to an executable type - instead, they should be
         * added to the right component.
         */
        @Override
        public void addAnnotation(AnnotationMirror a) {
            assert false : "AnnotatedExecutableType.addAnnotation should never be called";
        }

        /**
         * Sets the parameter types of this executable type
         *
         * @param params the parameter types
         */
        void setParameterTypes(List<? extends AnnotatedTypeMirror> params) {
            paramTypes.clear();
            paramTypes.addAll(params);
        }

        /** @return the parameter types of this executable type */
        public List<AnnotatedTypeMirror> getParameterTypes() {
            if (paramTypes.isEmpty()
                    && !((ExecutableType) actualType).getParameterTypes().isEmpty()) { // lazy init
                for (TypeMirror t : ((ExecutableType) actualType).getParameterTypes()) {
                    paramTypes.add(createType(t, atypeFactory, false));
                }
            }
            return Collections.unmodifiableList(paramTypes);
        }

        /**
         * Sets the return type of this executable type
         *
         * @param returnType the return type
         */
        void setReturnType(AnnotatedTypeMirror returnType) {
            this.returnType = returnType;
        }

        /**
         * The return type of a method or constructor. For constructors, the return type is not
         * VOID, but the type of the enclosing class.
         *
         * @return the return type of this executable type
         */
        public AnnotatedTypeMirror getReturnType() {
            if (returnType == null
                    && element != null
                    && ((ExecutableType) actualType).getReturnType() != null) { // lazy init
                TypeMirror aret = ((ExecutableType) actualType).getReturnType();
                if (((MethodSymbol) element).isConstructor()) {
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
         *
         * @param receiverType the receiver type
         */
        void setReceiverType(AnnotatedDeclaredType receiverType) {
            this.receiverType = receiverType;
        }

        /**
         * @return the receiver type of this executable type; null for static methods and
         *     constructors of top-level classes
         */
        public /*@Nullable*/ AnnotatedDeclaredType getReceiverType() {
            if (receiverType == null
                    // Static methods don't have a receiver
                    && !ElementUtils.isStatic(getElement())
                    // Array constructors should also not have a receiver. Array members have a
                    // getEnclosingElement().getEnclosingElement() of NONE
                    && (!(getElement().getKind() == ElementKind.CONSTRUCTOR
                            && getElement()
                                    .getEnclosingElement()
                                    .getSimpleName()
                                    .toString()
                                    .equals("Array")
                            && getElement()
                                            .getEnclosingElement()
                                            .getEnclosingElement()
                                            .asType()
                                            .getKind()
                                    == TypeKind.NONE))
                    // Top-level constructors don't have a receiver
                    && (getElement().getKind() != ElementKind.CONSTRUCTOR
                            || getElement().getEnclosingElement().getEnclosingElement().getKind()
                                    != ElementKind.PACKAGE)) {

                TypeElement encl = ElementUtils.enclosingClass(getElement());
                if (getElement().getKind() == ElementKind.CONSTRUCTOR) {
                    // Can only reach this branch if we're the constructor of a nested class
                    encl = ElementUtils.enclosingClass(encl.getEnclosingElement());
                }
                AnnotatedTypeMirror type = createType(encl.asType(), atypeFactory, false);
                assert type instanceof AnnotatedDeclaredType;
                receiverType = (AnnotatedDeclaredType) type;
            }
            return receiverType;
        }

        /**
         * Sets the thrown types of this executable type
         *
         * @param thrownTypes the thrown types
         */
        void setThrownTypes(List<? extends AnnotatedTypeMirror> thrownTypes) {
            this.throwsTypes.clear();
            this.throwsTypes.addAll(thrownTypes);
        }

        /** @return the thrown types of this executable type */
        public List<AnnotatedTypeMirror> getThrownTypes() {
            if (throwsTypes.isEmpty()
                    && !((ExecutableType) actualType).getThrownTypes().isEmpty()) { // lazy init
                for (TypeMirror t : ((ExecutableType) actualType).getThrownTypes()) {
                    throwsTypes.add(createType(t, atypeFactory, false));
                }
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

        /** @return the type variables of this executable type, if any */
        public List<AnnotatedTypeVariable> getTypeVariables() {
            if (typeVarTypes.isEmpty()
                    && !((ExecutableType) actualType).getTypeVariables().isEmpty()) { // lazy init
                for (TypeMirror t : ((ExecutableType) actualType).getTypeVariables()) {
                    typeVarTypes.add((AnnotatedTypeVariable) createType(t, atypeFactory, true));
                }
            }
            return Collections.unmodifiableList(typeVarTypes);
        }

        @Override
        public AnnotatedExecutableType deepCopy(boolean copyAnnotations) {
            return (AnnotatedExecutableType) new AnnotatedTypeCopier(copyAnnotations).visit(this);
        }

        @Override
        public AnnotatedExecutableType deepCopy() {
            return deepCopy(true);
        }

        @Override
        public AnnotatedExecutableType shallowCopy(boolean copyAnnotations) {
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

        @Override
        public AnnotatedExecutableType shallowCopy() {
            return shallowCopy(true);
        }

        public /*@NonNull*/ ExecutableElement getElement() {
            return element;
        }

        public void setElement(/*@NonNull*/ ExecutableElement elem) {
            this.element = elem;
        }

        @Override
        public AnnotatedExecutableType getErased() {
            AnnotatedExecutableType type =
                    new AnnotatedExecutableType(
                            (ExecutableType) atypeFactory.types.erasure(getUnderlyingType()),
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

        private List<AnnotatedTypeMirror> erasureList(Iterable<? extends AnnotatedTypeMirror> lst) {
            List<AnnotatedTypeMirror> erased = new ArrayList<AnnotatedTypeMirror>();
            for (AnnotatedTypeMirror t : lst) {
                erased.add(t.getErased());
            }
            return erased;
        }
    }

    /**
     * Represents Array types in java. A multidimensional array type is represented as an array type
     * whose component type is also an array type.
     */
    public static class AnnotatedArrayType extends AnnotatedTypeMirror {

        private AnnotatedArrayType(ArrayType type, AnnotatedTypeFactory factory) {
            super(type, factory);
        }

        /** The component type of this array type */
        private AnnotatedTypeMirror componentType;

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitArray(this, p);
        }

        @Override
        public ArrayType getUnderlyingType() {
            return (ArrayType) this.actualType;
        }

        /**
         * Sets the component type of this array
         *
         * @param type the component type
         */
        // WMD
        public void setComponentType(AnnotatedTypeMirror type) {
            this.componentType = type;
        }

        /** @return the component type of this array */
        public AnnotatedTypeMirror getComponentType() {
            if (componentType == null) // lazy init
            setComponentType(
                        createType(
                                ((ArrayType) actualType).getComponentType(), atypeFactory, false));
            return componentType;
        }

        @Override
        public AnnotatedArrayType deepCopy(boolean copyAnnotations) {
            return (AnnotatedArrayType) new AnnotatedTypeCopier(copyAnnotations).visit(this);
        }

        @Override
        public AnnotatedArrayType deepCopy() {
            return deepCopy(true);
        }

        @Override
        public AnnotatedArrayType shallowCopy(boolean copyAnnotations) {
            AnnotatedArrayType type = new AnnotatedArrayType((ArrayType) actualType, atypeFactory);
            if (copyAnnotations) {
                type.addAnnotations(this.getAnnotationsField());
            }
            type.setComponentType(getComponentType());
            return type;
        }

        @Override
        public AnnotatedArrayType shallowCopy() {
            return shallowCopy(true);
        }

        @Override
        public AnnotatedArrayType getErased() {
            // IMPORTANT NOTE: The returned type is a fresh Object because
            // the componentType is the only component of arrays and the
            // call to getErased will return a fresh object.
            // | T[ ] | = |T| [ ]
            AnnotatedArrayType at = shallowCopy();
            AnnotatedTypeMirror ct = at.getComponentType().getErased();
            at.setComponentType(ct);
            return at;
        }
    }

    /**
     * Represents a type variable. A type variable may be explicitly declared by a type parameter of
     * a type, method, or constructor. A type variable may also be declared implicitly, as by the
     * capture conversion of a wildcard type argument (see chapter 5 of The Java Language
     * Specification, Third Edition).
     */
    public static class AnnotatedTypeVariable extends AnnotatedTypeMirror {

        private AnnotatedTypeVariable(
                TypeVariable type, AnnotatedTypeFactory atypeFactory, boolean declaration) {
            super(type, atypeFactory);
            this.declaration = declaration;
        }

        /** The lower bound of the type variable. */
        private AnnotatedTypeMirror lowerBound;

        /** The upper bound of the type variable. */
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
         * @param declaration true if this type variable should be considered a declaration
         */
        public void setDeclaration(boolean declaration) {
            this.declaration = declaration;
        }

        @Override
        public AnnotatedTypeVariable asUse() {
            if (!this.isDeclaration()) {
                return this;
            }

            AnnotatedTypeVariable result = this.shallowCopy();
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
         * <p>Returns the lower bound of this type variable. While a type parameter cannot include
         * an explicit lower bound declaration, capture conversion can produce a type variable with
         * a non-trivial lower bound. Type variables otherwise have a lower bound of NullType.
         *
         * @param type the lower bound type
         */
        void setLowerBound(AnnotatedTypeMirror type) {
            if (type == null || type.isDeclaration()) {
                ErrorReporter.errorAbort(
                        "Lower bounds should never be null or a declaration.\n"
                                + "  new bound = "
                                + type
                                + "\n  type = "
                                + this);
            }
            this.lowerBound = type;
            fixupBoundAnnotations();
        }

        /**
         * Get the lower bound field directly, bypassing any lazy initialization. This method is
         * necessary to prevent infinite recursions in initialization. In general, prefer
         * getLowerBound.
         *
         * @return the lower bound field
         */
        public AnnotatedTypeMirror getLowerBoundField() {
            return lowerBound;
        }

        /** @return the lower bound type of this type variable */
        public AnnotatedTypeMirror getLowerBound() {
            if (lowerBound == null) { // lazy init
                BoundsInitializer.initializeBounds(this);
                fixupBoundAnnotations();
            }
            return lowerBound;
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

            // We allow the above replacement first because primary annotations might not have
            // annotations for all hierarchies, so we don't want to avoid placing bottom on the
            // lower bound for those hierarchies that don't have a qualifier in primaryAnnotations.
            if (!this.getAnnotationsField().isEmpty()) {
                if (upperBound != null) {
                    replaceUpperBoundAnnotations();
                }

                // Note:
                // if the lower bound is a type variable
                // then when we place annotations on the primary annotation
                //   this will actually cause the type variable to be exact and
                //   propagate the primary annotation to the type variable because
                //   primary annotations overwrite the upper and lower bounds of type variables
                //   when getUpperBound/getLowerBound is called
                if (lowerBound != null) {
                    lowerBound.replaceAnnotations(this.getAnnotationsField());
                }
            }
        }

        /**
         * Replaces (or adds if none exist) the primary annotation of all upper bounds of typeVar,
         * the AnnotatedTypeVariable with the annotations provided. The AnnotatedTypeVariable will
         * only have multiple upper bounds if the upper bound is an intersection.
         */
        private void replaceUpperBoundAnnotations() {
            if (upperBound.getKind() == TypeKind.INTERSECTION) {
                final List<AnnotatedDeclaredType> bounds =
                        ((AnnotatedIntersectionType) upperBound).directSuperTypes();
                for (final AnnotatedDeclaredType bound : bounds) {
                    bound.replaceAnnotations(this.getAnnotationsField());
                }
            } else {
                upperBound.replaceAnnotations(this.getAnnotationsField());
            }
        }

        /**
         * Set the upper bound of this variable type
         *
         * @param type the upper bound type
         */
        void setUpperBound(AnnotatedTypeMirror type) {
            if (type == null || type.isDeclaration()) {
                ErrorReporter.errorAbort(
                        "Upper bounds should never be null or a declaration.\n"
                                + "  new bound = "
                                + type
                                + "\n  type = "
                                + this);
            }
            this.upperBound = type;
            fixupBoundAnnotations();
        }

        /**
         * Get the upper bound field directly, bypassing any lazy initialization. This method is
         * necessary to prevent infinite recursions in initialization. In general, prefer
         * getUpperBound.
         *
         * @return the upper bound field
         */
        public AnnotatedTypeMirror getUpperBoundField() {
            return upperBound;
        }

        /**
         * Get the upper bound of the type variable, possibly lazily initializing it. Attention: If
         * the upper bound is lazily initialized, it will not contain any annotations! Callers of
         * the method have to make sure that an AnnotatedTypeFactory first processed the bound.
         *
         * @return the upper bound type of this type variable
         */
        public AnnotatedTypeMirror getUpperBound() {
            if (upperBound == null) { // lazy init
                BoundsInitializer.initializeBounds(this);
                fixupBoundAnnotations();
            }
            return upperBound;
        }

        public AnnotatedTypeParameterBounds getBounds() {
            return new AnnotatedTypeParameterBounds(getUpperBound(), getLowerBound());
        }

        public AnnotatedTypeParameterBounds getBoundFields() {
            return new AnnotatedTypeParameterBounds(getUpperBoundField(), getLowerBoundField());
        }

        /** Used to terminate recursion into upper bounds. */
        private boolean inUpperBounds = false;

        @Override
        public AnnotatedTypeVariable deepCopy(boolean copyAnnotations) {
            return (AnnotatedTypeVariable) new AnnotatedTypeCopier(copyAnnotations).visit(this);
        }

        @Override
        public AnnotatedTypeVariable deepCopy() {
            return deepCopy(true);
        }

        @Override
        public AnnotatedTypeVariable shallowCopy(boolean copyAnnotations) {
            AnnotatedTypeVariable type =
                    new AnnotatedTypeVariable(
                            ((TypeVariable) actualType), atypeFactory, declaration);

            if (copyAnnotations) {
                type.addAnnotations(this.getAnnotationsField());
            }

            if (!inUpperBounds) {
                inUpperBounds = true;
                type.inUpperBounds = true;
                type.setUpperBound(getUpperBound().shallowCopy());
                inUpperBounds = false;
                type.inUpperBounds = false;
            }

            type.setLowerBound(getLowerBound().shallowCopy());

            return type;
        }

        @Override
        public AnnotatedTypeVariable shallowCopy() {
            return shallowCopy(true);
        }

        /**
         * This method will traverse the upper bound of this type variable calling getErased until
         * it finds the concrete upper bound. e.g.
         *
         * <pre>{@code  <E extends T>, T extends S, S extends List<String>>}</pre>
         *
         * A call to getErased will return the type List
         *
         * @return the erasure of the upper bound of this type
         *     <p>IMPORTANT NOTE: getErased should always return a FRESH object. This will occur for
         *     type variables if all other getErased methods are implemented appropriately.
         *     Therefore, to avoid extra copy calls, this method will not call deepCopy on
         *     getUpperBound
         */
        @Override
        public AnnotatedTypeMirror getErased() {
            // |T extends A&B| = |A|
            return this.getUpperBound().getErased();
        }
    }

    /**
     * A pseudo-type used where no actual type is appropriate. The kinds of NoType are:
     *
     * <ul>
     *   <li>VOID -- corresponds to the keyword void.
     *   <li>PACKAGE -- the pseudo-type of a package element.
     *   <li>NONE -- used in other cases where no actual type is appropriate; for example, the
     *       superclass of java.lang.Object.
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
        public AnnotatedNoType deepCopy(boolean copyAnnotations) {
            return (AnnotatedNoType) new AnnotatedTypeCopier(copyAnnotations).visit(this);
        }

        @Override
        public AnnotatedNoType deepCopy() {
            return deepCopy(true);
        }

        @Override
        public AnnotatedNoType shallowCopy(boolean copyAnnotations) {
            AnnotatedNoType type = new AnnotatedNoType((NoType) actualType, atypeFactory);
            if (copyAnnotations) {
                type.addAnnotations(this.getAnnotationsField());
            }
            return type;
        }

        @Override
        public AnnotatedNoType shallowCopy() {
            return shallowCopy(true);
        }
    }

    /** Represents the null type. This is the type of the expression {@code null}. */
    public static class AnnotatedNullType extends AnnotatedTypeMirror {

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
        public AnnotatedNullType deepCopy(boolean copyAnnotations) {
            return (AnnotatedNullType) new AnnotatedTypeCopier(copyAnnotations).visit(this);
        }

        @Override
        public AnnotatedNullType deepCopy() {
            return deepCopy(true);
        }

        @Override
        public AnnotatedNullType shallowCopy(boolean copyAnnotations) {
            AnnotatedNullType type = new AnnotatedNullType((NullType) actualType, atypeFactory);
            if (copyAnnotations) {
                type.addAnnotations(this.getAnnotationsField());
            }
            return type;
        }

        @Override
        public AnnotatedNullType shallowCopy() {
            return shallowCopy(true);
        }
    }

    /**
     * Represents a primitive type. These include {@code boolean}, {@code byte}, {@code short},
     * {@code int}, {@code long}, {@code char}, {@code float}, and {@code double}.
     */
    public static class AnnotatedPrimitiveType extends AnnotatedTypeMirror {

        private AnnotatedPrimitiveType(PrimitiveType type, AnnotatedTypeFactory factory) {
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
        public AnnotatedPrimitiveType deepCopy(boolean copyAnnotations) {
            return (AnnotatedPrimitiveType) new AnnotatedTypeCopier(copyAnnotations).visit(this);
        }

        @Override
        public AnnotatedPrimitiveType deepCopy() {
            return deepCopy(true);
        }

        @Override
        public AnnotatedPrimitiveType shallowCopy(boolean copyAnnotations) {
            AnnotatedPrimitiveType type =
                    new AnnotatedPrimitiveType((PrimitiveType) actualType, atypeFactory);
            if (copyAnnotations) {
                type.addAnnotations(this.getAnnotationsField());
            }
            return type;
        }

        @Override
        public AnnotatedPrimitiveType shallowCopy() {
            return shallowCopy(true);
        }
    }

    /**
     * Represents a wildcard type argument. Examples include:
     *
     * <p>? ? extends Number ? super T
     *
     * <p>A wildcard may have its upper bound explicitly set by an extends clause, its lower bound
     * explicitly set by a super clause, or neither (but not both).
     */
    public static class AnnotatedWildcardType extends AnnotatedTypeMirror {
        /** SuperBound */
        private AnnotatedTypeMirror superBound;

        /** ExtendBound */
        private AnnotatedTypeMirror extendsBound;

        /**
         * The type variable to which this wildcard is an argument. Used to initialize the upper
         * bound of unbounded wildcards and wildcards in raw types.
         */
        private TypeVariable typeVariable = null;

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
         * @param type the type of the lower bound
         */
        void setSuperBound(AnnotatedTypeMirror type) {
            if (type == null || type.isDeclaration()) {
                ErrorReporter.errorAbort(
                        "Super bounds should never be null or a declaration.\n"
                                + "  new bound = "
                                + type
                                + "\n  type = "
                                + this);
            }
            this.superBound = type;
            fixupBoundAnnotations();
        }

        public AnnotatedTypeMirror getSuperBoundField() {
            return superBound;
        }

        /**
         * @return the lower bound of this wildcard. If no lower bound is explicitly declared,
         *     {@code null} is returned.
         */
        public AnnotatedTypeMirror getSuperBound() {
            if (superBound == null) {
                BoundsInitializer.initializeSuperBound(this);
                fixupBoundAnnotations();
            }
            return this.superBound;
        }

        /**
         * Sets the upper bound of this wildcard
         *
         * @param type the type of the upper bound
         */
        void setExtendsBound(AnnotatedTypeMirror type) {
            if (type == null || type.isDeclaration()) {
                ErrorReporter.errorAbort(
                        "Extends bounds should never be null or a declaration.\n"
                                + "  new bound = "
                                + type
                                + "\n  type = "
                                + this);
            }
            this.extendsBound = type;
            fixupBoundAnnotations();
        }

        public AnnotatedTypeMirror getExtendsBoundField() {
            return extendsBound;
        }

        /**
         * @return the upper bound of this wildcard. If no upper bound is explicitly declared, the
         *     upper bound of the type variable to which the wildcard is bound is used.
         */
        public AnnotatedTypeMirror getExtendsBound() {
            if (extendsBound == null) {
                BoundsInitializer.initializeExtendsBound(this);
                fixupBoundAnnotations();
            }
            return this.extendsBound;
        }

        private void fixupBoundAnnotations() {
            if (!this.getAnnotationsField().isEmpty()) {
                if (superBound != null) {
                    superBound.replaceAnnotations(this.getAnnotationsField());
                }
                if (extendsBound != null) {
                    extendsBound.replaceAnnotations(this.getAnnotationsField());
                }
            }
        }

        /**
         * Sets type variable to which this wildcard is an argument. This method should only be
         * called during initialization of the type.
         */
        void setTypeVariable(TypeParameterElement typeParameterElement) {
            this.typeVariable = (TypeVariable) typeParameterElement.asType();
        }

        /**
         * Sets type variable to which this wildcard is an argument. This method should only be
         * called during initialization of the type.
         */
        void setTypeVariable(TypeVariable typeVariable) {
            this.typeVariable = typeVariable;
        }

        /**
         * Returns the type variable to which this wildcard is an argument. Used to initialize the
         * upper bound of unbounded wildcards and wildcards in raw types.
         *
         * @return the type variable to which this wildcard is an argument
         */
        public TypeVariable getTypeVariable() {
            return typeVariable;
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
        public AnnotatedWildcardType deepCopy(boolean copyAnnotations) {
            return (AnnotatedWildcardType) new AnnotatedTypeCopier(copyAnnotations).visit(this);
        }

        @Override
        public AnnotatedWildcardType deepCopy() {
            return deepCopy(true);
        }

        @Override
        public AnnotatedWildcardType shallowCopy(boolean copyAnnotations) {
            AnnotatedWildcardType type =
                    new AnnotatedWildcardType((WildcardType) actualType, atypeFactory);
            type.setExtendsBound(getExtendsBound().shallowCopy());
            type.setSuperBound(getSuperBound().shallowCopy());
            if (copyAnnotations) {
                type.addAnnotations(this.getAnnotationsField());
            }

            type.uninferredTypeArgument = uninferredTypeArgument;
            type.typeVariable = typeVariable;

            return type;
        }

        @Override
        public AnnotatedWildcardType shallowCopy() {
            return shallowCopy(true);
        }

        /**
         * @see
         *     org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable#getErased()
         */
        @Override
        public AnnotatedTypeMirror getErased() {
            // |? extends A&B| = |A|
            return getExtendsBound().getErased();
        }

        // Remove the uninferredTypeArgument once method type
        // argument inference and raw type handling is improved.
        private boolean uninferredTypeArgument = false;

        /**
         * Set that this wildcard is from an uninferred type argument. This method should only be
         * used within the framework. Once issues that depend on this hack, in particular Issue 979,
         * are fixed, this must be removed.
         */
        public void setUninferredTypeArgument() {
            uninferredTypeArgument = true;
        }

        /**
         * Returns whether or not this wildcard is a type argument for which inference failed to
         * infer a type.
         *
         * @return returns whether or not this wildcard is a type argument for which inference
         *     failed
         */
        public boolean isUninferredTypeArgument() {
            return uninferredTypeArgument;
        }
    }

    public static class AnnotatedIntersectionType extends AnnotatedTypeMirror {

        /**
         * AnnotatedIntersectionTypes are created by type parameters whose bounds include an &amp;.
         * For example: {@code <T extends MyObject & Serializable & Comparable<MyObject>>}
         *
         * <p>The bound {@code MyObject &amp; Serializable &amp; Comparable} is an intersection type
         * with direct supertypes [MyObject, Serializable, Comparable]
         *
         * @param type underlying kind of this type
         * @param atypeFactory the factory used to construct this intersection type
         */
        private AnnotatedIntersectionType(
                IntersectionType type, AnnotatedTypeFactory atypeFactory) {
            super(type, atypeFactory);
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitIntersection(this, p);
        }

        @Override
        public AnnotatedIntersectionType deepCopy(boolean copyAnnotations) {
            return (AnnotatedIntersectionType) new AnnotatedTypeCopier(copyAnnotations).visit(this);
        }

        @Override
        public AnnotatedIntersectionType deepCopy() {
            return deepCopy(true);
        }

        @Override
        public AnnotatedIntersectionType shallowCopy(boolean copyAnnotations) {
            AnnotatedIntersectionType type =
                    new AnnotatedIntersectionType((IntersectionType) actualType, atypeFactory);
            if (copyAnnotations) {
                type.addAnnotations(this.getAnnotationsField());
            }
            type.supertypes = this.supertypes;
            return type;
        }

        @Override
        public AnnotatedIntersectionType shallowCopy() {
            return shallowCopy(true);
        }

        protected List<AnnotatedDeclaredType> supertypes;

        @Override
        public List<AnnotatedDeclaredType> directSuperTypes() {
            if (supertypes == null) {
                List<? extends TypeMirror> ubounds = ((IntersectionType) actualType).getBounds();
                List<AnnotatedDeclaredType> res =
                        new ArrayList<AnnotatedDeclaredType>(ubounds.size());
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
    }

    // TODO: Ensure union types are handled everywhere.
    // TODO: Should field "annotations" contain anything?
    public static class AnnotatedUnionType extends AnnotatedTypeMirror {

        /**
         * Constructor for this type
         *
         * @param type underlying kind of this type
         * @param atypeFactory TODO
         */
        private AnnotatedUnionType(UnionType type, AnnotatedTypeFactory atypeFactory) {
            super(type, atypeFactory);
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitUnion(this, p);
        }

        @Override
        public AnnotatedUnionType deepCopy(boolean copyAnnotations) {
            return (AnnotatedUnionType) new AnnotatedTypeCopier(copyAnnotations).visit(this);
        }

        @Override
        public AnnotatedUnionType deepCopy() {
            return deepCopy(true);
        }

        @Override
        public AnnotatedUnionType shallowCopy(boolean copyAnnotations) {
            AnnotatedUnionType type = new AnnotatedUnionType((UnionType) actualType, atypeFactory);
            if (copyAnnotations) {
                type.addAnnotations(this.getAnnotationsField());
            }
            type.alternatives = this.alternatives;
            return type;
        }

        @Override
        public AnnotatedUnionType shallowCopy() {
            return shallowCopy(true);
        }

        protected List<AnnotatedDeclaredType> alternatives;

        public List<AnnotatedDeclaredType> getAlternatives() {
            if (alternatives == null) {
                List<? extends TypeMirror> ualts = ((UnionType) actualType).getAlternatives();
                List<AnnotatedDeclaredType> res =
                        new ArrayList<AnnotatedDeclaredType>(ualts.size());
                for (TypeMirror alt : ualts) {
                    res.add((AnnotatedDeclaredType) createType(alt, atypeFactory, false));
                }
                alternatives = Collections.unmodifiableList(res);
            }
            return alternatives;
        }
    }

    /**
     * This method returns a list of AnnotatedTypeMirrors where the Java type of each ATM is an
     * immediate supertype (class or interface) of the Java type of this. If the directSuperType has
     * type arguments, then the annotations on those type arguments are taken with proper
     * translation from the declaration of the Java type of this.
     *
     * <p>For example,
     *
     * <pre>
     * {@code class B<T> { ... } }
     * {@code class A extends B<@NonNull String> { ... } }
     * {@code @Nullable A a;}
     * </pre>
     *
     * The direct supertype of the ATM {@code @Nullable A} is {@code @Nullable B<@NonNull String>}.
     *
     * <p>An example with more complex type arguments:
     *
     * <pre>
     * {@code class D<Q,R> { ... } }
     * {@code class A<T,S> extends D<S,T> { ... } }
     * {@code @Nullable A<@NonNull String, @NonNull Object> a;}
     * </pre>
     *
     * The direct supertype of the ATM {@code @Nullable A<@NonNull String, @NonNull Object>} is
     * {@code @Nullable B<@NonNull Object, @NonNull String>}.
     *
     * <p>An example with more than one direct supertype:
     *
     * <pre>
     * {@code class B<T> implements List<Integer> { ... } }
     * {@code class A extends B<@NonNull String> implements List<Integer> { ... } }
     * {@code @Nullable A a;}
     * </pre>
     *
     * The direct supertypes of the ATM {@code @Nullable A} are {@code @Nullable B <@NonNull
     * String>} and {@code @Nullable List<@NonNull Integer>}.
     *
     * @see Types#directSupertypes(TypeMirror)
     */
    public List<? extends AnnotatedTypeMirror> directSuperTypes() {
        return SupertypeFinder.directSuperTypes(this);
    }
}
