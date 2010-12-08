package checkers.types;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;

import checkers.types.visitors.AnnotatedTypeScanner;
import checkers.types.visitors.AnnotatedTypeVisitor;
import checkers.types.visitors.SimpleAnnotatedTypeVisitor;
import checkers.util.AnnotationUtils;
import checkers.util.ElementUtils;
import checkers.util.TreeUtils;
import checkers.util.TypesUtils;

import checkers.nullness.quals.NonNull;
import checkers.quals.TypeQualifier;

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
 * @since 1.6
 */
public abstract class AnnotatedTypeMirror {

    /**
     * Creates the appropriate AnnotatedTypeMirror specific wrapper for the
     * provided type
     *
     * @param type
     * @param env
     * @param typeFactory
     * @return [to document]
     */
    public static AnnotatedTypeMirror createType(TypeMirror type,
            ProcessingEnvironment env, AnnotatedTypeFactory typeFactory) {
        if (type == null)
            return null;
        if (replacer == null)
            replacer = new Replacer(typeFactory.types);

        switch (type.getKind()) {
            case ARRAY:
                return new AnnotatedArrayType((ArrayType)type, env, typeFactory);
            case DECLARED:
                return new AnnotatedDeclaredType((DeclaredType)type, env, typeFactory);
            case ERROR:
                throw new AssertionError("Input should type-checked already...");
            case EXECUTABLE:
                return new AnnotatedExecutableType((ExecutableType)type, env, typeFactory);
            case VOID:
            case PACKAGE:
            case NONE:
                return new AnnotatedNoType((NoType)type, env, typeFactory);
            case NULL:
                return new AnnotatedNullType((NullType)type, env, typeFactory);
            case TYPEVAR: {
                AnnotatedTypeVariable atype = new AnnotatedTypeVariable((TypeVariable)type, env, typeFactory);
                return atype;
            }
            case WILDCARD: {
                AnnotatedWildcardType atype = new AnnotatedWildcardType((WildcardType)type, env, typeFactory);
                return atype;
            }
            default:
                if (type.getKind().isPrimitive())
                    return new AnnotatedPrimitiveType((PrimitiveType)type, env, typeFactory);
            throw new AssertionError("Unidentified type " + type);
        }
    }

    public List<? extends AnnotatedTypeMirror> directSuperTypes() {
        return directSuperTypes(this);
    }

    /** Processing Environment of the current round **/
    protected final ProcessingEnvironment env;

    /** The factory to use for lazily creating annotations. */
    protected final AnnotationUtils annotationFactory;

    /** The factory to use for lazily creating annotated types. */
    protected final AnnotatedTypeFactory typeFactory;

    /** Actual type wrapped with this AnnotatedTypeMirror **/
    protected final TypeMirror actualType;

    /** the Element associated with this instance value, if one exists **/
    // TODO: Clarify, with value not the element of the type.
    // I.e. For 'Integer i;' the element would be for 'i' not 'Integer'
    protected Element element;

    /** The enclosing Type **/
    protected AnnotatedTypeMirror enclosingType;

    /** The annotations on this type. */
    // AnnotationMirror doesn't override Object.hashCode, .equals, so we use
    // the class name of Annotation instead
    // Caution: Assumes that a type can have at most one AnnotationMirror for
    // any Annotation type. JSR308 is pushing to have this change.
    protected final Set<AnnotationMirror> annotations = AnnotationUtils.createAnnotationSet();

    private static int uidCounter = 0;
    public int uid;

    /**
     * Constructor for AnnotatedTypeMirror.
     *
     * @param type  the underlying type
     * @param env   Processing Environment
     * @param typeFactory TODO
     */
    // TODO: Have static factory methods
    private AnnotatedTypeMirror(TypeMirror type, ProcessingEnvironment env,
            AnnotatedTypeFactory typeFactory) {
        this.actualType = type;
        this.env = env;
        this.annotationFactory = AnnotationUtils.getInstance(env);
        assert typeFactory != null;
        this.typeFactory = typeFactory;
        uid = ++uidCounter;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AnnotatedTypeMirror))
            return false;
        AnnotatedTypeMirror t = (AnnotatedTypeMirror)o;
        if (this.env.getTypeUtils().isSameType(this.actualType, t.actualType)
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
     * Returns the underlying unannotated Java type wrapped with this
     *
     * @return  the underlying type
     */
    public TypeMirror getUnderlyingType() {
        return actualType;
    }

    /**
     * Sets the enclosing type
     *
     * @param enclosingType
     */
    void setEnclosingType(AnnotatedTypeMirror enclosingType) {
        this.enclosingType = enclosingType;
    }

    /**
     * Returns the enclosing type, as in the type of {@code A} in the type
     * {@code A.B}.
     *
     * @return enclosingType the enclosing type
     */
    public AnnotatedTypeMirror getEnclosingType() {
        return enclosingType;
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
     * Returns the actual annotation mirror used to annotate this type,
     * whose name equals the passed annotationName if one exist, null otherwise.
     *
     * @param annotationName
     * @return the annotation mirror for annotationName
     */
    public AnnotationMirror getAnnotation(String annotationName) {
        assert annotationName != null : annotationName + " cannot be null";
        Name name = env.getElementUtils().getName(annotationName);
        return getAnnotation(name);
    }

    /**
     * Returns the actual annotation mirror used to annotate this type,
     * whose name equals the passed annotationName if one exist, null otherwise.
     *
     * @param annotationName
     * @return the annotation mirror for annotationName
     */
    public AnnotationMirror getAnnotation(Name annotationName) {
        assert annotationName != null : annotationName + " cannot be null";
        for (AnnotationMirror anno : getAnnotations())
            if (AnnotationUtils.annotationName(anno).equals(annotationName))
                return anno;
        return null;
    }

    /**
     * Returns the actual annotation mirror used to annotate this type,
     * whose name equals the passed annotationName if one exist, null otherwise.
     *
     * @param anno annotation class
     * @return the annotation mirror for anno
     */
    public AnnotationMirror getAnnotation(Class<? extends Annotation> anno) {
        return getAnnotation(anno.getCanonicalName());
    }

    /**
     * Determines whether this type contains an annotation with the same
     * annotation type as a particular annotation. This method does not
     * consider an annotation's values.
     *
     * @param a the annotation to check for
     * @return true iff the type contains an annotation with the same type as
     * the annotation given by {@code a}
     */
    public boolean hasAnnotation(AnnotationMirror a) {
        return getAnnotations().contains(a);
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
     * Adds an annotation to this type. If the annotation does not have the
     * {@link TypeQualifier} meta-annotation, this method has no effect.
     *
     * @param a the annotation to add
     */
    public void addAnnotation(AnnotationMirror a) {
        if (a == null) return;
        if (typeFactory.isSupportedQualifier(a))
            this.annotations.add(a);
        else {
            AnnotationMirror aliased = typeFactory.aliasedAnnotation(a);
            addAnnotation(aliased);
        }
    }

    /**
     * Adds an annotation to this type. If the annotation does not have the
     * {@link TypeQualifier} meta-annotation, this method has no effect.
     *
     * @param a the class of the annotation to add
     */
    public void addAnnotation(Class<? extends Annotation> a) {
        AnnotationMirror anno = annotationFactory.fromClass(a);
        addAnnotation(anno);
    }

    /**
     * Adds multiple annotations to this type.
     *
     * @param annotations the annotations to add
     */
    public void addAnnotations(Iterable<? extends AnnotationMirror> annotations) {
        for (AnnotationMirror a : annotations)
            this.addAnnotation(a);
    }

    /**
     * Removes an annotation from the type.
     *
     * @param a the annotation to remove
     * @return true if the annotation was removed, false if the type's
     * annotations were unchanged
     */
    public boolean removeAnnotation(AnnotationMirror a) {
        return annotations.remove(getAnnotation(AnnotationUtils.annotationName(a)));
    }

    public boolean removeAnnotation(Class<? extends Annotation> a) {
        return removeAnnotation(annotationFactory.fromClass(a));
    }

    /**
     * Removes multiple annotations from the type.
     *
     * @param annotations the annotations to remove
     * @return true if at least one annotation was removed, false if the type's
     * annotations were unchanged
     */
    public boolean removeAnnotations(Iterable<? extends AnnotationMirror> annotations) {
        boolean changed = false;
        for (AnnotationMirror a : annotations)
            changed |= this.removeAnnotation(a);
        return changed;
    }

    /**
     * Removes all annotations on this type
     */
    public void clearAnnotations() {
        annotations.clear();
    }

    // A Helper method to print annotations separated with a space
    protected final static String formatAnnotationString(Collection<? extends AnnotationMirror> lst) {
        StringBuilder sb = new StringBuilder();
        for (AnnotationMirror obj : lst) {
            if (!obj.getElementValues().isEmpty()) {
                sb.append(obj.toString());
                sb.append(" ");
                continue;
            }
            sb.append("@");
            sb.append(obj.getAnnotationType().asElement().getSimpleName());
            sb.append(" ");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return formatAnnotationString(getAnnotations())
                + this.actualType;
    }

    public String toStringDebug() {
        return toString() + " " + getClass().getSimpleName() + "#" + uid;
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
     * {@code AnnotatedTypeMirror}
     *
     * @param type  an empty type where fields of this are copied to
     * @param annotation whether annotations are copied or not
     */
    protected AnnotatedTypeMirror copyFields(AnnotatedTypeMirror type, boolean annotation) {
        type.setElement(getElement());
        type.setEnclosingType(getEnclosingType());
        if (annotation)
            type.addAnnotations(annotations);
        return type;
    }

    /**
     * Returns a shallow copy of this type.
     *
     * @param annotation
     *            whether copy should have annotations
     */
    public abstract AnnotatedTypeMirror getCopy(boolean annotation);

    protected static AnnotatedDeclaredType createTypeOfObject(AnnotatedTypeFactory typeFactory) {
        AnnotatedDeclaredType objectType =
        typeFactory.fromElement(
                typeFactory.elements.getTypeElement(
                        Object.class.getCanonicalName()));
        return objectType;
    }

    /**
     * Sub
     *
     * @param mappings
     */
    public AnnotatedTypeMirror substitute(
            Map<? extends AnnotatedTypeMirror,
                    ? extends AnnotatedTypeMirror> mappings) {
        if (mappings.containsKey(this))
            return mappings.get(this).getCopy(true);

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

        /** Supertype of this type **/
        @Deprecated
        protected AnnotatedDeclaredType superclass;

        /** The interfaces that this type implements **/
        @Deprecated
        protected List<AnnotatedDeclaredType> interfaces;

        boolean isGeneric = false;

        protected final DeclaredType actualType;

        protected List<AnnotatedDeclaredType> supertypes;

        /**
         * Constructor for this type
         *
         * @param type  underlying kind of this type
         * @param env   the processing environment
         * @param typeFactory TODO
         */
        AnnotatedDeclaredType(DeclaredType type,
                ProcessingEnvironment env, AnnotatedTypeFactory typeFactory) {
            super(type, env, typeFactory);
            this.actualType = type;
            DeclaredType elem = (DeclaredType)((TypeElement)type.asElement()).asType();
            isGeneric = !elem.getTypeArguments().isEmpty();
            this.interfaces = new LinkedList<AnnotatedDeclaredType>();
            this.supertypes = null;
        }


        @Override
        public String toString() {
            final Element typeElt = this.getUnderlyingType().asElement();
            StringBuilder sb = new StringBuilder();
            sb.append(formatAnnotationString(getAnnotations()));
            sb.append(typeElt.getSimpleName());
            if (!this.getTypeArguments().isEmpty()) {
                sb.append("<");

                boolean isFirst = true;
                for (AnnotatedTypeMirror typeArg : getTypeArguments()) {
                    if (!isFirst) sb.append(", ");
                    sb.append(typeArg);
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
         * @param t the type arguments
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
                        typeArgs.add(createType(t, env, typeFactory));
                }
                typeArgs = Collections.unmodifiableList(typeArgs);
            }
            return typeArgs;
        }

        /**
         * @return the super type of this
         */
        @Deprecated
        public AnnotatedTypeMirror getSuperclass() {

            TypeElement elt = (TypeElement)this.actualType.asElement();
            assert elt.getSuperclass() != null;

            AnnotatedTypeMirror type = createType(elt.asType(), env, typeFactory);

            // Some types don't have a supertype (e.g., java.lang.Cloneable),
            // so we return a copy of their NoType.
            if (type instanceof AnnotatedNoType) return type.getCopy(true);

            assert type instanceof AnnotatedDeclaredType : "not a declared type: "
                    + type;

            AnnotatedDeclaredType at = (AnnotatedDeclaredType)type;
            at.addAnnotations(this.getAnnotations());

            return at;
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

        /**
         * @return the interfaces of this type
         */
        @Deprecated
        public List<AnnotatedDeclaredType> getInterfaces() {
            TypeElement elt = (TypeElement)this.actualType.asElement();
            List<AnnotatedDeclaredType> myInterfaces = new LinkedList<AnnotatedDeclaredType>();
            for (TypeMirror dt : elt.getInterfaces()) {
                AnnotatedDeclaredType at =
                    (AnnotatedDeclaredType)createType(dt, env, typeFactory);
                at.addAnnotations(this.getAnnotations());
                myInterfaces.add(at);
            }
            return Collections.unmodifiableList(myInterfaces);
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

        @Override
        public AnnotatedDeclaredType getCopy(boolean annotation) {
            AnnotatedDeclaredType type =
                new AnnotatedDeclaredType(this.getUnderlyingType(), this.env, this.typeFactory);
            copyFields(type, annotation);

            type.setTypeArguments(getTypeArguments());

            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                ? extends AnnotatedTypeMirror> mapping) {
            if (mapping.containsKey(this))
                return mapping.get(this);

            AnnotatedDeclaredType type = getCopy(true);

            // No need to substitute type params for now!
            //
            // Why is this the case? The following was necessary to get the
            // Interning checker's Generics test case to work. -MP

            List<AnnotatedTypeMirror> typeArgs = new ArrayList<AnnotatedTypeMirror>();
            for (AnnotatedTypeMirror t : getTypeArguments())
                typeArgs.add(t.substitute(mapping));
            type.setTypeArguments(typeArgs);

            if (TypesUtils.isAnonymousType(actualType)
                && this.supertypes != null) {
                // watch need to copy upper bound as well
                List<AnnotatedDeclaredType> supertypes = new ArrayList<AnnotatedDeclaredType>();
                for (AnnotatedDeclaredType t : directSuperTypes())
                    supertypes.add((AnnotatedDeclaredType)t.substitute(mapping));
                type.setDirectSuperTypes(supertypes);
            }
            return type;
        }


        @Override
        public AnnotatedDeclaredType getErased() {
            // 1. |G<T_1, ..., T_n>| = |G|
            // 2. |T.C| = |T|.C
            if (!getTypeArguments().isEmpty()) {
                // Handle case 1.
                AnnotatedDeclaredType rType =
                    (AnnotatedDeclaredType)AnnotatedTypeMirror.createType(
                            env.getTypeUtils().erasure(actualType),
                            env, typeFactory);
                rType.addAnnotations(getAnnotations());
                rType.setElement(element);
                rType.setTypeArguments(Collections.<AnnotatedTypeMirror> emptyList());
                return rType.getErased();
            } else if ((getEnclosingType() != null) &&
                       (getEnclosingType().getKind() != TypeKind.NONE)) {
                // Handle case 2
                // TODO: Test this
                AnnotatedDeclaredType rType = getCopy(true);
                AnnotatedTypeMirror et = getEnclosingType();
                rType.setEnclosingType(et.getErased());
                return rType;
            } else {
                return this;
            }
        }

    }

    /**
     * Represents a type of an executable. An executable is a method, constructor, or initializer.
     */
    public static class AnnotatedExecutableType extends AnnotatedTypeMirror {

        private final ExecutableType actualType;

        AnnotatedExecutableType(ExecutableType type,
                ProcessingEnvironment env, AnnotatedTypeFactory factory) {
            super(type, env, factory);
            this.actualType = type;
        }

        final private List<AnnotatedTypeMirror> paramTypes =
            new LinkedList<AnnotatedTypeMirror>();
        private AnnotatedDeclaredType receiverType;
        private AnnotatedTypeMirror returnType;
        final private List<AnnotatedTypeMirror> throwsTypes =
            new LinkedList<AnnotatedTypeMirror>();
        final private List<AnnotatedTypeVariable> typeVarTypes =
            new LinkedList<AnnotatedTypeVariable>();

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
                    paramTypes.add(createType(t, env, typeFactory));
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
                        actualType.getReturnType(), env, typeFactory);
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
                AnnotatedTypeMirror type = createType(encl.asType(), env, typeFactory);
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
                    throwsTypes.add(createType(t, env, typeFactory));
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
                for (TypeMirror t : actualType.getTypeVariables())
                    typeVarTypes.add((AnnotatedTypeVariable)createType(
                            t, env, typeFactory));
            }
            return Collections.unmodifiableList(typeVarTypes);
        }

        @Override
        public AnnotatedExecutableType getCopy(boolean annotation) {
            AnnotatedExecutableType type =
                new AnnotatedExecutableType(getUnderlyingType(), env, typeFactory);
            copyFields(type, annotation);
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
            AnnotatedExecutableType type =
                new AnnotatedExecutableType(
                        (ExecutableType) env.getTypeUtils().erasure(getUnderlyingType()),
                        env, typeFactory);
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

            // Worry about type variable
            return type;
        }

        @Override
        public String toString() {
            return getReturnType()
                + (getTypeVariables().isEmpty() ? "" : " <" + getTypeVariables() + ">")
                + (getParameterTypes().isEmpty() ? " ()" : " (" + getParameterTypes() + ")")
                + " " + getReceiverType()
                + (getThrownTypes().isEmpty() ? "" : " throws " + getThrownTypes());
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

        AnnotatedArrayType(ArrayType type,
                ProcessingEnvironment env, AnnotatedTypeFactory factory) {
            super(type, env, factory);
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
                        actualType.getComponentType(), env, typeFactory));
            return componentType;
        }


        @Override
        public AnnotatedArrayType getCopy(boolean annotation) {
            AnnotatedArrayType type = new AnnotatedArrayType(actualType, env, typeFactory);
            copyFields(type, annotation);
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

        public String toStringAsCanonical() {
            StringBuilder sb = new StringBuilder();

            AnnotatedArrayType array = this;
            AnnotatedTypeMirror component;
            while (true) {
                component = array.getComponentType();
				if (array.getAnnotations().size() > 0) {
					sb.append(' ');
					sb.append(formatAnnotationString(array.getAnnotations()).trim());
					sb.append(' ');
				}
                sb.append("[]");
                if (!(component instanceof AnnotatedArrayType)) {
                    sb.insert(0, component.getUnderlyingType().toString());
                    break;
                }
                array = (AnnotatedArrayType) component;
            }
            sb.insert(0, formatAnnotationString(component.getAnnotations()));
            return sb.toString();
        }

        @Override
        public String toString() {
            return toStringAsCanonical();
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

        @Deprecated
        AnnotatedTypeVariable(TypeVariable type,
                ProcessingEnvironment env, AnnotatedTypeFactory factory) {
            super(type, env, factory);
            this.actualType = type;
        }

        /** The element of the type variable **/
        private Element typeVarElement;
        /** The lower bound of the type variable **/
        private AnnotatedTypeMirror lowerBound;
        /** The upper bound of the type variable **/
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
         * Set the element of the type variable
         *
         * @param element   the type variable element
         */
        void setTypeVariableElement(Element element) {
            this.typeVarElement = element;
        }

        /**
         * @return the element of the type variable in this
         */
        public Element getTypeVariableElement() {
            return typeVarElement;
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
         * @return the lower bound type of this type variable
         */
        public AnnotatedTypeMirror getLowerBound() {
            if (lowerBound == null && actualType.getLowerBound() != null) // lazy init
                setLowerBound(createType(actualType.getLowerBound(), env, typeFactory));
            return lowerBound;
        }

        /**
         * Set the upper bound of this variable type
         * @param type the upper bound type
         */
        void setUpperBound(AnnotatedTypeMirror type) {
            this.upperBound = type;
            // What is this supposed to do???
            // Substituting "this" for "this" seems like a no-op, however, removing it breaks stuff.
            AnnotatedTypeMirror upperBound = type.substitute(Collections.singletonMap(this, this));
            this.upperBound = upperBound;
        }

        /**
         * @return the upper bound type of this type variable
         */
        public AnnotatedTypeMirror getUpperBound() {
            if (upperBound == null
                    && actualType.getUpperBound() != null) // lazy init
                setUpperBound(createType(
                        actualType.getUpperBound(), env, typeFactory));
            return upperBound;
        }
        
        private boolean inUpperBounds = false;
        
        @Override
        public AnnotatedTypeVariable getCopy(boolean annotation) {
        	if (inUpperBounds) return this;
        	
            AnnotatedTypeVariable type =
                new AnnotatedTypeVariable(actualType, env, typeFactory);
            copyFields(type, annotation);
            type.setTypeVariableElement(getTypeVariableElement());
            if (getUpperBound().isAnnotated() &&
            		!inUpperBounds) {
            	inUpperBounds = true;
            	type.inUpperBounds = true;
            	type.setUpperBound(getUpperBound());
            	inUpperBounds = false;
            }
            return type;
        }

        @Override
        public AnnotatedTypeMirror getErased() {
            // |T extends A&B| = |A|
            return this.getUpperBound().getErased();
        }

        private static <K extends AnnotatedTypeMirror, V extends AnnotatedTypeMirror>
        V mapGetHelper(
                Map<K, V> mappings, AnnotatedTypeVariable key) {
            for (Map.Entry<K, V> entry : mappings.entrySet()) {
                K possible = entry.getKey();
                if (possible == key) return entry.getValue();
                if (possible instanceof AnnotatedTypeVariable) {
                    AnnotatedTypeVariable other = (AnnotatedTypeVariable)possible;
                    Element oElt = other.getUnderlyingType().asElement();
                    if (key.getUnderlyingType().asElement().equals(oElt)) {
                        if (!key.annotations.isEmpty()
                                && !AnnotationUtils.areSame(key.annotations, other.annotations)) {
                            @SuppressWarnings("unchecked")
                            V found = (V)entry.getValue().getCopy(false);
                            found.addAnnotations(key.annotations);
                            return found;
                        } else
                            return entry.getValue();
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
            if (found != null) return found;

            AnnotatedTypeVariable type = getCopy(true);
            Map<AnnotatedTypeMirror, AnnotatedTypeMirror> newMappings =
                new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>(mappings);
            newMappings.put(this, type);
            type.setLowerBound(getLowerBound().substitute(newMappings));
            if (getUpperBound() != null)
                type.setUpperBound(getUpperBound().substitute(newMappings));
            return type;
        }

        @Override
        public boolean isAnnotated() {
            return (super.isAnnotated()
                    || (getUpperBound() != null && getUpperBound().isAnnotated()));
        }

        public Set<AnnotationMirror> getAnnotationsOnTypeVar() {
            return super.getAnnotations();
        }

        @Override
        public Set<AnnotationMirror> getAnnotations() {
            if (!super.isAnnotated() && getUpperBound() != null)
                return getUpperBound().getAnnotations();
            return super.getAnnotations();
        }

        @Override
        public void addAnnotation(AnnotationMirror anno) {
            super.addAnnotation(anno);
            this.getUpperBound().addAnnotation(anno);
        }

        // Style taken from Type
        boolean isPrintingBound = false;
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(formatAnnotationString(getAnnotations()));
            sb.append(actualType);
            if (!isPrintingBound) {
                try {
                    isPrintingBound = true;
                    if (getLowerBound() != null && getLowerBound().getKind() != TypeKind.NULL) {
                        sb.append(" super ");
                        sb.append(getLowerBound());
                    }
                    if (getUpperBound() != null && getUpperBound().getKind() != TypeKind.NULL) {
                        sb.append(" extends ");
                        sb.append(getUpperBound());
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

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof AnnotatedTypeVariable))
                return false;
            AnnotatedTypeVariable other = (AnnotatedTypeVariable) o;
            boolean isSame =
                this.getUnderlyingType() == other.getUnderlyingType()
                && AnnotationUtils.areSame(this.annotations, other.annotations);
            return isSame;
        }
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

        AnnotatedNoType(NoType type, ProcessingEnvironment env, AnnotatedTypeFactory factory) {
            super(type, env, factory);
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
        public AnnotatedNoType getCopy(boolean annotation) {
            AnnotatedNoType type = new AnnotatedNoType(actualType, env, typeFactory);
            copyFields(type, annotation);
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

        AnnotatedNullType(NullType type, ProcessingEnvironment env, AnnotatedTypeFactory factory) {
            super(type, env, factory);
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
        public AnnotatedNullType getCopy(boolean annotation) {

            AnnotatedNullType type = new AnnotatedNullType(actualType, env, typeFactory);
            copyFields(type, annotation);
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
        public String toString() {
            return "null";
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

        AnnotatedPrimitiveType(PrimitiveType type,
                ProcessingEnvironment env, AnnotatedTypeFactory factory) {
            super(type, env, factory);
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
        public AnnotatedPrimitiveType getCopy(boolean annotation) {
            AnnotatedPrimitiveType type =
                new AnnotatedPrimitiveType(actualType, env, typeFactory);
            copyFields(type, annotation);
            return type;
        }

        @Override
        public AnnotatedTypeMirror substitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            // cannot substitute
            // return getCopy(true);
        	// WMD wants to substitute primitive types!
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

        @Deprecated
        AnnotatedWildcardType(WildcardType type, ProcessingEnvironment env, AnnotatedTypeFactory factory) {
            super(type, env, factory);
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

        /**
         * @return the lower bound of this wildcard. If no lower bound is
         * explicitly declared, {@code null} is returned.
         */
        public AnnotatedTypeMirror getSuperBound() {
            if (superBound == null
                    && actualType.getSuperBound() != null) // lazy init
                setSuperBound(createType(
                        actualType.getSuperBound(), env, typeFactory));
            return this.superBound;
        }

        /**
         * Sets the upper bound of this wild card
         *
         * @param type  the type of the upper bound
         */
        void setExtendsBound(AnnotatedTypeMirror type) {
            this.extendsBound = type;
        }

        /**
         * @return the lower bound of this wildcard. If no lower bound is
         * explicitly declared, {@code null} is returned.
         */
        public AnnotatedTypeMirror getExtendsBound() {
            if (extendsBound == null) { // lazy init
                TypeMirror superType = actualType.getExtendsBound();
                if (superType == null)
                    superType = env.getElementUtils().getTypeElement("java.lang.Object").asType();
                setExtendsBound(createType(superType, env, typeFactory));
            }
            return this.extendsBound;
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
        public AnnotatedWildcardType getCopy(boolean annotation) {
            AnnotatedWildcardType type = new AnnotatedWildcardType(actualType, env, typeFactory);
            copyFields(type, annotation);

            type.setExtendsBound(getExtendsBound());
            type.setSuperBound(getSuperBound());

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
            if (getExtendsBound() != null)
                type.setExtendsBound(getExtendsBound().substitute(newMapping));
            if (getSuperBound() != null)
                type.setSuperBound(getSuperBound().substitute(newMapping));

            return type;
        }

        @Override
        public AnnotatedTypeMirror getErased() {
            // |T extends A&B| = |A|
            if (getExtendsBound() != null)
                return getExtendsBound().getErased();
            return createTypeOfObject(typeFactory);
        }

        boolean isPrintingBound = false;
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("?");
            if (!isPrintingBound) {
                try {
                    isPrintingBound = true;
                    if (getExtendsBound() != null && getExtendsBound().getKind() != TypeKind.NONE) {
                        sb.append(" extends ");
                        sb.append(getExtendsBound());
                    }
                    if (getSuperBound() != null && getSuperBound().getKind() != TypeKind.NULL) {
                        sb.append(" super ");
                        sb.append(getSuperBound());
                    }
                } finally {
                    isPrintingBound = false;
                }
            }
            return sb.toString();
        }

        @Override
        public boolean isAnnotated() {
            return (super.isAnnotated()
                    || (getExtendsBound() != null && getExtendsBound().isAnnotated()));
        }

        @Override
        public Set<AnnotationMirror> getAnnotations() {
            if (!super.isAnnotated() && getExtendsBound() != null)
                return getExtendsBound().getAnnotations();
            return super.getAnnotations();
        }
    }

    protected final List<AnnotatedDeclaredType>
    directSuperTypes(AnnotatedDeclaredType type) {
        setSuperTypeFinder(type.typeFactory);
        List<AnnotatedDeclaredType> supertypes =
            superTypeFinder.visitDeclared(type, null);
        typeFactory.postDirectSuperTypes(type, supertypes);
        return supertypes;
    }

    private final List<? extends AnnotatedTypeMirror> directSuperTypes(
            AnnotatedTypeMirror type) {
        setSuperTypeFinder(type.typeFactory);
        List<? extends AnnotatedTypeMirror> supertypes =
            superTypeFinder.visit(type, null);
        typeFactory.postDirectSuperTypes(type, supertypes);
        return supertypes;
    }

    private static void setSuperTypeFinder(AnnotatedTypeFactory factory) {
        if (superTypeFinder == null || superTypeFinder.typeFactory != factory)
            superTypeFinder = new SuperTypeFinder(factory);
    }

    private static SuperTypeFinder superTypeFinder;

    private static class SuperTypeFinder extends
    SimpleAnnotatedTypeVisitor<List<? extends AnnotatedTypeMirror>, Void> {
        private Types types;
        private final AnnotatedTypeFactory typeFactory;

        SuperTypeFinder(AnnotatedTypeFactory typeFactory) {
            this.typeFactory = typeFactory;
            this.types = typeFactory.env.getTypeUtils();
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
            AnnotatedDeclaredType boxedType = typeFactory.getAnnotatedType(boxed);
            if (!annotations.isEmpty())
                boxedType.clearAnnotations();
            boxedType.addAnnotations(annotations);
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
                    typeFactory.toAnnotatedType(types.getPrimitiveType(superPrimitiveType));
                superPrimitive.addAnnotations(annotations);
                superTypes.add(superPrimitive);
            }

            return superTypes;
        }

        @Override
        public List<AnnotatedDeclaredType> visitDeclared(AnnotatedDeclaredType type, Void p) {
            List<AnnotatedDeclaredType> supertypes =
                new ArrayList<AnnotatedDeclaredType>();
            Set<AnnotationMirror> annotations = type.getAnnotations();

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

            ClassTree classTree = typeFactory.trees.getTree(typeElement);
            // Testing against enum and annotation. idealy we can simply use element!
            if (classTree != null) {
                supertypes.addAll(supertypesFromTree(classTree));
            } else {
                supertypes.addAll(supertypesFromElement(typeElement));
                final Element elem = type.getElement() == null ? typeElement : type.getElement();
            }

            for (AnnotatedDeclaredType dt : supertypes) {
                if (isRaw)
                    dt.setTypeArguments(Collections.<AnnotatedTypeMirror>emptyList());
                else if (!mapping.isEmpty())
                    replacer.visit(dt, mapping);
            }

            return supertypes;
        }

        private List<AnnotatedDeclaredType> supertypesFromElement(TypeElement typeElement) {
            List<AnnotatedDeclaredType> supertypes = new ArrayList<AnnotatedDeclaredType>();
            // Find the super types: Start with superclass
            if (typeElement.getSuperclass().getKind() != TypeKind.NONE) {
                DeclaredType superClass = (DeclaredType) typeElement.getSuperclass();
                AnnotatedDeclaredType dt =
                    (AnnotatedDeclaredType)typeFactory.toAnnotatedType(superClass);
                supertypes.add(dt);
            } else if (!ElementUtils.isObject(typeElement)) {
                supertypes.add(createTypeOfObject(typeFactory));
            }
            for (TypeMirror st : typeElement.getInterfaces()) {
                AnnotatedDeclaredType ast =
                    (AnnotatedDeclaredType)typeFactory.toAnnotatedType(st);
                supertypes.add(ast);
            }
            TypeFromElement.annotateSupers(supertypes, typeElement);
            return supertypes;
        }

        private List<AnnotatedDeclaredType> supertypesFromTree(ClassTree classTree) {
            List<AnnotatedDeclaredType> supertypes = new ArrayList<AnnotatedDeclaredType>();
            if (classTree.getExtendsClause() != null) {
                AnnotatedDeclaredType adt = (AnnotatedDeclaredType)
                    typeFactory.fromTypeTree(classTree.getExtendsClause());
                supertypes.add(adt);
            } else if (!ElementUtils.isObject(TreeUtils.elementFromDeclaration(classTree))) {
                supertypes.add(createTypeOfObject(typeFactory));
            }

            for (Tree implemented : classTree.getImplementsClause()) {
                AnnotatedDeclaredType adt = (AnnotatedDeclaredType)
                    typeFactory.getAnnotatedTypeFromTypeTree(implemented);
                supertypes.add(adt);
            }

            TypeElement elem = TreeUtils.elementFromDeclaration(classTree);
            if (elem.getKind() == ElementKind.ENUM) {
                DeclaredType dt = (DeclaredType) elem.getSuperclass();
                AnnotatedDeclaredType adt = (AnnotatedDeclaredType)typeFactory.toAnnotatedType(dt);
                supertypes.add(adt);
            } else if (elem.getKind() == ElementKind.ANNOTATION_TYPE) {
                DeclaredType dt = (DeclaredType) typeFactory.elements.getTypeElement("java.lang.annotation.Annotation").asType();
                AnnotatedDeclaredType adt = (AnnotatedDeclaredType)typeFactory.toAnnotatedType(dt);
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
            Elements elements = typeFactory.elements;
            final AnnotatedTypeMirror objectType =
                typeFactory.getAnnotatedType(elements.getTypeElement("java.lang.Object"));
            objectType.addAnnotations(annotations);
            superTypes.add(objectType);

            final AnnotatedTypeMirror cloneableType =
                typeFactory.getAnnotatedType(elements.getTypeElement("java.lang.Cloneable"));
            cloneableType.addAnnotations(annotations);
            superTypes.add(cloneableType);

            final AnnotatedTypeMirror serializableType =
                typeFactory.getAnnotatedType(elements.getTypeElement("java.io.Serializable"));
            serializableType.addAnnotations(annotations);
            superTypes.add(serializableType);

            if (type.getComponentType() instanceof AnnotatedReferenceType) {
                for (AnnotatedTypeMirror sup : type.getComponentType().directSuperTypes()) {
                    ArrayType arrType = typeFactory.types.getArrayType(sup.getUnderlyingType());
                    AnnotatedArrayType aarrType = (AnnotatedArrayType)
                        typeFactory.toAnnotatedType(arrType);
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
            if (type.getUpperBound() != null)
                superTypes.add(type.getUpperBound());
            return superTypes;
        }

        @Override
        public List<AnnotatedTypeMirror> visitWildcard(AnnotatedWildcardType type, Void p) {
            List<AnnotatedTypeMirror> superTypes = new ArrayList<AnnotatedTypeMirror>();
            if (type.getExtendsBound() != null)
                superTypes.add(type.getExtendsBound());
            return superTypes;
        }
    };

    private static AnnotatedTypeScanner<Void, Map<TypeParameterElement, AnnotatedTypeMirror>>
    replacer;

    static class Replacer extends AnnotatedTypeScanner<Void, Map<TypeParameterElement, AnnotatedTypeMirror>> {
        final Types types;
        public Replacer(Types types) {
            this.types = types;
        }

        public Void visitDeclared(AnnotatedDeclaredType type, Map<TypeParameterElement, AnnotatedTypeMirror> mapping) {
            List<AnnotatedTypeMirror> args = new ArrayList<AnnotatedTypeMirror>();
            for (AnnotatedTypeMirror arg : type.getTypeArguments()) {
                Element elem = types.asElement(arg.getUnderlyingType());
                if ((elem != null) &&
                        (elem.getKind() == ElementKind.TYPE_PARAMETER) &&
                        (mapping.containsKey(elem))) {
                    AnnotatedTypeMirror other = mapping.get(elem);
                    if (!arg.annotations.isEmpty()) {
                        other.clearAnnotations();
                        other.addAnnotations(arg.annotations);
                    }
                    args.add(other);
                } else
                    args.add(arg);
            }
            type.setTypeArguments(args);
            return super.visitDeclared(type, mapping);
        }
    };

}
