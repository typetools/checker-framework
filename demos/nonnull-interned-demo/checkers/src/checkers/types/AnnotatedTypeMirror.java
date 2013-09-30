package checkers.types;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;

/**
 * Represents a annotated type in the JSR308 Java programming
 * language. Types include primitive types, declared types (class and
 * interface types), array types, type variables, and the null type.
 * Also represented are wildcard type arguments, the signature and
 * return types of executables, and pseudo-types corresponding to
 * packages and to the keyword void.
 * 
 * Types should be compared using the utility methods in
 * {@link AnnotatedTypes}. There is no guarantee that any particular
 * type will always be represented by the same object.
 * 
 * To implement operations based on the class of an TypeMirror object,
 * either use a visitor or use the result of the {@link getKind()}
 * method.
 * 
 */
public abstract class AnnotatedTypeMirror {
    /** Processing Environment of the current round **/
    protected final ProcessingEnvironment env;

    protected final AnnotationFactory annotationFactory;
    
    /** Actual type wrapped with this AnnotatedTypeMirror **/
    protected final TypeMirror actualType;
    
    /** the Element associated with this instance value, if one exists **/
    // TODO: Clarify, with value not the element of the type.
    // I.e. For 'Integer i;' the element would be for 'i' not 'Integer'
    protected Element element;
    
    /** The enclosing Type **/
    protected AnnotatedTypeMirror enclosingType;
    
    protected final Set<AnnotationData> annotations =
        new HashSet<AnnotationData>();
    protected final Set<AnnotationData> includes =
        new HashSet<AnnotationData>();
    protected final Set<AnnotationData> excludes =
        new HashSet<AnnotationData>();

    /**
     * Constructor for AnnotatedTypeMirror.
     * 
     * @param type  the underlying type
     * @param env   Processing Environment
     */
    // TODO: Have static factory methods
    AnnotatedTypeMirror(TypeMirror type, ProcessingEnvironment env) {
        this.actualType = type;
        this.env = env;
        this.annotationFactory = new AnnotationFactory(env);
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
     * Returns the annotations on this type. 
     * 
     * It does not include annotations in deep types (type arguments, array 
     * components, etc).
     * 
     * @param implicit  whether included annotations are to be considered
     * @return  a set of the annotations on this
     */
    public Set<AnnotationData> getAnnotations(boolean implicit) {
        Set<AnnotationData> annotations = new HashSet<AnnotationData>();
        annotations.addAll(annotations);
        if (implicit) {
            annotations.addAll(includes);
            annotations.removeAll(excludes);
        }
        return Collections.unmodifiableSet(annotations);
    }

    /**
     * Determines whether the annotated type has an annotation with a certain name
     * 
     * @param annotationName the name of the annotation to check for
     * @param implicit  whether or not to check included/excluded annotations
     * @return true iff the annotation with the given name is pesent
     */
    public boolean hasAnnotation(CharSequence annotationName, boolean implicit) {
        AnnotationData annotation = 
            annotationFactory.createAnnotation(annotationName, 
                    AnnotationLocation.RAW);
        
        if (implicit) {
            if (excludes.contains(annotation))
                return false;
            
            if (includes.contains(annotation))
                return true;
        }
        
        return annotations.contains(annotation);
    }

    /**
     * Determines whether the annotated type has an annotation with a certain class
     * 
     * @param Annotation the class of the annotation to check for
     * @param implicit  whether or not to check included/excluded annotations
     * @return true iff the annotation with the given name is pesent
     */
    public boolean hasAnnotation(Class<? extends Annotation> annotation,
            boolean implicit) {
        return hasAnnotation(annotation.getCanonicalName(), implicit);
    }

    /**
     * Adds the given annotation, represented by an {@link AnnotationData},
     * to this class type at the location provided by the
     * {@link AnnotationData}.
     *
     * @param annotation the annotation to be added
     */
    public void annotate(AnnotationData annotation) {
        annotations.add(annotation);
        excludes.remove(annotation);
    }

    /**
     * Adds the given annotation, represented by an {@link AnnotationData},
     * to this class type at the location provided by the class
     *
     * @param annotation the annotation class to be added
     */
    public void annotate(Class<? extends Annotation> annotation) {
        annotate(annotation.getCanonicalName());
    }

    /**
     * Adds the given annotation, represented by an {@link AnnotationData},
     * to this class type at the location provided by the
     * annotation name
     *
     * @param annotation the annotation to add
     */
    public void annotate(CharSequence annotation) {
        AnnotationData annon = annotationFactory.createAnnotation(annotation, AnnotationLocation.RAW);
        annotations.add(annon);
    }

    /**
     * Forces the given annotation to be included among the annotations checked
     * by {@link AnnotatedTypeMirror#hasAnnotation}. This is useful for
     * treating class types as having an annotation even though they don't
     * (e.g., a primitive type that is implicitly {@code @NonNull}).
     *
     * @param annotation the annotation to include among this type's annotations
     */
    public void include(AnnotationData annotation) {
        includes.add(annotation);
        excludes.remove(annotation);
    }

    /**
     * Forces the given annotation to be included among the annotations 
     * checked
     * by {@link AnnotatedTypeMirror#hasAnnotation}. This is useful for
     * treating class types as having an annotation even though they don't
     * (e.g., a primitive type that is implicitly {@code @NonNull}).
     *
     * @param annotation the annotation class to include among this type's 
     * annotations
     */
    public void include(Class<? extends Annotation> annotation) {
        include(annotation.getCanonicalName());
    }

    /**
     * Forces the given annotation to be included among the annotations checked
     * by {@link AnnotatedTypeMirror#hasAnnotation}. This is useful for
     * treating class types as having an annotation even though they don't
     * (e.g., a primitive type that is implicitly {@code @NonNull}).
     *
     * @param annotation the annotation to include among this type's annotations
     */
    public void include(CharSequence annotation) {
        AnnotationData annon = annotationFactory.createAnnotation(annotation, AnnotationLocation.RAW);
        include(annon);
    }

    /**
     * Forces the given annotation to be skipped by {@code hasAnnotationAt}.
     * Useful in conjunction with annotation defaults (e.g., if variables are
     * {@code @NonNull} by default and the checker finds a {@code @Nullable}
     * variable, it can simply exclude {@code @NonNull} rather than handle
     * {@code @Nullable} as a special case).
     *
     * @param annotation the annotation to exclude from this type's annotations
     */
    public void exclude(AnnotationData annotation) {
        excludes.add(annotation);
        includes.remove(annotation);
    }

    /**
     * Forces the given annotation to be skipped by {@code hasAnnotationAt}.
     * Useful in conjunction with annotation defaults (e.g., if variables are
     * {@code @NonNull} by default and the checker finds a {@code @Nullable}
     * variable, it can simply exclude {@code @NonNull} rather than handle
     * {@code @Nullable} as a special case).
     *
     * @param annotation the annotation class to exclude from this type's annotations
     */
    public void exclude(Class<? extends Annotation> annotation) {
        exclude(annotation.getCanonicalName());
    }

    /**
     * Forces the given annotation to be skipped by {@code hasAnnotationAt}.
     * Useful in conjunction with annotation defaults (e.g., if variables are
     * {@code @NonNull} by default and the checker finds a {@code @Nullable}
     * variable, it can simply exclude {@code @NonNull} rather than handle
     * {@code @Nullable} as a special case).
     *
     * @param annotation the annotation name to exclude from this type's annotations
     */
    public void exclude(CharSequence annotation) {
        AnnotationData annon = annotationFactory.createAnnotation(annotation, AnnotationLocation.RAW);
        exclude(annon);
    }

    /* To ease transitions */
    public AnnotationLocation getPosition() {
        throw new RuntimeException("Not implemented yet!");
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
     * @return  the {@code Element} of the value of this type
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

    public AnnotatedTypeMirror getErasured() {
        return this;
    }
    
    /**
     * Copy the fields on this type onto the passed type.
     * This method needs to be overriden by any subclass of 
     * {@code AnnotatedTypeMirror}
     * 
     * @param type  an empty type where fields of this are copied to
     * @param annotation whether annotations are copied or not
     */
    protected AnnotatedTypeMirror copyFields(AnnotatedTypeMirror type, boolean annotation) {
        type.setElement(getElement());
        type.setEnclosingType(getEnclosingType());
        if (annotation) {
            for (AnnotationData annon : annotations)
                type.annotate(annon);
            for (AnnotationData annon : includes)
                type.include(annon);
            for (AnnotationData annon : excludes)
                type.exclude(annon);
        }

        return type;
    }
    
    /**
     * Returns a shallow copy of this type.
     * 
     * @param annotation
     *            whether copy should have annotations
     */
    public abstract AnnotatedTypeMirror getCopy(boolean annotation);

    /**
     * Sub
     * 
     * @param mappings
     */
    public AnnotatedTypeMirror subsitute(
            Map<? extends AnnotatedTypeMirror, 
                    ? extends AnnotatedTypeMirror> mappings) {
        if (mappings.containsKey(this))
            return mappings.get(this).getCopy(true);
        else
            return this;
    }
    
    public static interface AnnotatedReferenceType { }
    
    /**
     * Represents a declared type (whether class or interface).
     */
    public static class AnnotatedDeclaredType extends AnnotatedTypeMirror 
    implements AnnotatedReferenceType {

        /** Parametrized Type Arguments **/
        final protected List<AnnotatedTypeMirror> typeArgs =
            new LinkedList<AnnotatedTypeMirror>();

        /** Supertype of this type **/
        protected AnnotatedTypeMirror superType;
        
        /** The interfaces that this type implements **/
        protected List<AnnotatedTypeMirror> interfaces;

        boolean isGeneric = false;
        
        /**
         * Constructor for this type
         * 
         * @param type  underlying kind of this type
         * @param env   the processing environment
         */
        AnnotatedDeclaredType(TypeMirror type,
                ProcessingEnvironment env) {
            super(type, env);
            TypeElement elem =(TypeElement)env.getTypeUtils().asElement(type);
            isGeneric = !elem.getTypeParameters().isEmpty();
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitDeclared(this, p);
        }

        /**
         * Sets the type arguments on this type
         * @param t the type arguments
         */
        void setTypeArguments(List<? extends AnnotatedTypeMirror> ts) {
            typeArgs.clear();
            typeArgs.addAll(ts);
        }

        /**
         * @return the type argument for this type
         */
        public List<? extends AnnotatedTypeMirror> getTypeArguments() {
            return Collections.unmodifiableList(typeArgs);
        }

        /**
         * Sets the supertype associated with this type
         * 
         * @param superType the super type of this type
         */
        void setSupertype(AnnotatedTypeMirror superType) {
            this.superType = superType;
        }

        /**
         * @return the super type of this
         */
        public AnnotatedTypeMirror getSupertype() {
            return superType;
        }

        /**
         * Returns true if the type is generic, even if the type is erased
         * or used as a RAW type.
         * 
         * @returns true iff the type is generic
         */
        public boolean isGeneric() {
            return isGeneric;
        }
        /**
         * Sets the interfaces of this type
         * 
         * @param interfaces the interface types of this
         */
        void setInterfaces(
                List<? extends AnnotatedTypeMirror> interfaces) {
            this.interfaces.clear();
            this.interfaces.addAll(interfaces);
        }
        
        /**
         * @return the interfaces of this type
         */
        public List<? extends AnnotatedTypeMirror> getInterfaces() {
            return Collections.unmodifiableList(interfaces);
        }

        @Override
        public AnnotatedDeclaredType getCopy(boolean annotation) {
            AnnotatedDeclaredType type =
                new AnnotatedDeclaredType(this.getUnderlyingType(), this.env);
            copyFields(type, annotation);

            type.setInterfaces(getInterfaces());
            type.setSupertype(getSupertype());
            type.setTypeArguments(getTypeArguments());

            return type;
        }
        
        @Override
        public AnnotatedTypeMirror subsitute(
                Map<? extends AnnotatedTypeMirror,
                ? extends AnnotatedTypeMirror> mapping) {
            if (mapping.containsKey(this))
                return mapping.get(this);
            
            AnnotatedDeclaredType type = getCopy(true);
            
            // Set interfaces
            {
                List<AnnotatedTypeMirror> interfaces = 
                    new ArrayList<AnnotatedTypeMirror>();
                for (AnnotatedTypeMirror t : getInterfaces())
                    interfaces.add(t.subsitute(mapping));
                type.setInterfaces(interfaces);
            }
            
            // Set Supertype
            type.setSupertype(getSupertype().subsitute(mapping));
            
            // No need to subsitute type params for now!
            
            return type;
        }
 
        
        @Override
        public AnnotatedDeclaredType getErasured() {
            // 1. |G<T_1, ..., T_n>| = |G|
            // 2. |T.C| = |T|.C
            if (!getTypeArguments().isEmpty()) {
                // Handle case 1.
                AnnotatedDeclaredType rType = getCopy(true);
                rType.setTypeArguments(Collections.<AnnotatedTypeMirror> emptyList());
                return rType.getErasured();
            } else if (getEnclosingType().getKind() != TypeKind.NONE) {
                // Handle case 2
                // TODO: Test this
                AnnotatedDeclaredType rType = getCopy(true);
                AnnotatedTypeMirror et = getEnclosingType();
                rType.setEnclosingType(et.getErasured());
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

        AnnotatedExecutableType(TypeMirror type,
                ProcessingEnvironment env) {
            super(type, env);
        }

        final private List<AnnotatedTypeMirror> paramTypes =
            new LinkedList<AnnotatedTypeMirror>();
        private AnnotatedTypeMirror receiverType;
        private AnnotatedTypeMirror returnType;
        final private List<AnnotatedTypeMirror> throwsTypes =
            new LinkedList<AnnotatedTypeMirror>();
        final private List<AnnotatedTypeVariable> typeVarTypes =
            new LinkedList<AnnotatedTypeVariable>();

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitExecutable(this, p);
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
        public List<? extends AnnotatedTypeMirror> getParameterTypes() {
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
            return returnType;
        }

        /**
         * Sets the receiver type on this executable type
         * @param receiverType the receiver type
         */
        void setReceiverType(AnnotatedTypeMirror receiverType) {
            this.receiverType = receiverType;
        }

        /**
         * @return the receiver type of this executable type
         */
        public AnnotatedTypeMirror getReceiverType() {
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
        public List<? extends AnnotatedTypeMirror> getThrownTypes() {
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
            return Collections.unmodifiableList(typeVarTypes);
        }

        @Override
        public AnnotatedExecutableType getCopy(boolean annotation) {
            AnnotatedExecutableType type =
                new AnnotatedExecutableType(getUnderlyingType(), env);
            copyFields(type, annotation);
            type.setParameterTypes(getParameterTypes());
            type.setReceiverType(getReceiverType());
            type.setReturnType(getReturnType());
            type.setThrownTypes(getThrownTypes());
            type.setTypeVariables(getTypeVariables());

            
            return type;
        }
        
        @Override
        public AnnotatedExecutableType subsitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            // Shouldn't subsitute for methods!
            AnnotatedExecutableType type = getCopy(true);
            
            // Params
            {
                List<AnnotatedTypeMirror> params = new ArrayList<AnnotatedTypeMirror>();
                for (AnnotatedTypeMirror t : getParameterTypes()) {
                    params.add(t.subsitute(mappings));
                }
                type.setParameterTypes(params);
            }
            
            type.setReceiverType(getReceiverType().subsitute(mappings));
            type.setReturnType(getReturnType().subsitute(mappings));
            
            // Throws
            {
                List<AnnotatedTypeMirror> throwns = new ArrayList<AnnotatedTypeMirror>();
                for (AnnotatedTypeMirror t : getThrownTypes()) {
                    throwns.add(t.subsitute(mappings));
                }
                type.setThrownTypes(throwns);
            }
            
            // Worry about type variable
            return type;
        }
    }

    /**
     * Represents Array types in java. A multidimensional array type is 
     * represented as an array type whose component type is also an 
     * array type.
     */
    public static class AnnotatedArrayType extends AnnotatedTypeMirror 
    implements AnnotatedReferenceType {

        AnnotatedArrayType(TypeMirror type,
                ProcessingEnvironment env) {
            super(type, env);
        }

        /** The component type of this array type */
        private AnnotatedTypeMirror componentType;

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitArray(this, p);
        }

        /**
         * Sets the component type of this array
         * 
         * @param type the component type
         */
        void setComponentType(AnnotatedTypeMirror type) {
            this.componentType = type;
        }

        /**
         * @return the component type of this array
         */
        public AnnotatedTypeMirror getComponentType() {
            return componentType;
        }


        @Override
        public AnnotatedArrayType getCopy(boolean annotation) {
            AnnotatedArrayType type = new AnnotatedArrayType(actualType, env);
            copyFields(type, annotation);
            type.setComponentType(getComponentType());
            return type;
        }

        @Override
        public AnnotatedTypeMirror subsitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            if (mappings.containsKey(this))
                return mappings.get(this);
            
            AnnotatedArrayType type = getCopy(true);
            type.setComponentType(getComponentType().subsitute(mappings));
            return type;
        }
        
        @Override
        public AnnotatedArrayType getErasured() {
            // | T[ ] | = |T| [ ]
            AnnotatedArrayType at = getCopy(true);
            AnnotatedTypeMirror ct = at.getComponentType().getErasured();
            at.setComponentType(ct);
            return at;

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

        AnnotatedTypeVariable(TypeMirror type,
                ProcessingEnvironment env) {
            super(type, env);
        }

        /** The element of the type variable **/
        private Element typeVarElement;
        /** The lower bound of the type variable **/
        private AnnotatedTypeMirror lowerBound;
        /** The upper bound of the type variable **/
        private AnnotatedTypeMirror upperBound;

        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitTypeVariable(this, p);
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
            return lowerBound;
        }

        /**
         * Set the upper bound of this variable type
         * @param type the upper bound type
         */
        void setUpperBound(AnnotatedTypeMirror type) {
            this.upperBound = type;
        }

        /**
         * @return the upper bound type of this type variable
         */
        public AnnotatedTypeMirror getUpperBound() {
            return upperBound;
        }

        @Override
        public AnnotatedTypeVariable getCopy(boolean annotation) {
            AnnotatedTypeVariable type =
                new AnnotatedTypeVariable(actualType, env);
            copyFields(type, annotation);
            type.setTypeVariableElement(getTypeVariableElement());
            type.setUpperBound(getUpperBound());
            return type;
        }
        
        @Override 
        public AnnotatedTypeMirror getErasured() {
            // |T extends A&B| = |A|
            return this.getUpperBound().getErasured();
        }
        
        @Override
        public AnnotatedTypeMirror subsitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            if (mappings.containsKey(this))
                return mappings.get(this);
            
            AnnotatedTypeVariable type = getCopy(true);
            type.setLowerBound(getLowerBound().subsitute(mappings));
            type.setUpperBound(getUpperBound().subsitute(mappings));
            return type;
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

        AnnotatedNoType(TypeMirror type, ProcessingEnvironment env) {
            super(type, env);
        }
        // No need for methods
        // Might like to override annotate(), include(), execlude() 
        // AS NoType does not accept any annotations

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitNoType(this, p);
        }
        
        public AnnotatedNoType getCopy(boolean annotation) {
            AnnotatedNoType type = new AnnotatedNoType(actualType, env);
            copyFields(type, annotation);
            return type;
        }
        
        @Override
        public AnnotatedTypeMirror subsitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            // Cannot subsitute
            return getCopy(true);
        }
    }
    
    /**
     * Represents the null type. This is the type of the expression {@code null}.
     */
    public static class AnnotatedNullType extends AnnotatedTypeMirror 
    implements AnnotatedReferenceType {

        AnnotatedNullType(TypeMirror type, ProcessingEnvironment env) {
            super(type, env);
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitNull(this, p);
        }
        
        @Override
        public AnnotatedNullType getCopy(boolean annotation) {
            
            AnnotatedNullType type = new AnnotatedNullType(actualType, env);
            copyFields(type, annotation);
            return type;
        }
        
        @Override
        public AnnotatedTypeMirror subsitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            // cannot subsitute
            return getCopy(true);            
        }

    }
    
    /**
     * Represents a primitive type. These include {@code boolean}, 
     * {@code byte}, {@code short}, {@code int}, {@code long}, {@code char}, 
     * {@code float}, and {@code double}.
     */
    public static class AnnotatedPrimitiveType extends AnnotatedTypeMirror 
    implements AnnotatedReferenceType {

        AnnotatedPrimitiveType(TypeMirror type,
                ProcessingEnvironment env) {
            super(type, env);
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitPrimitive(this, p);
        }
        
        @Override
        public AnnotatedPrimitiveType getCopy(boolean annotation) {
            AnnotatedPrimitiveType type = 
                new AnnotatedPrimitiveType(actualType, env);
            copyFields(type, annotation);
            return type;
        }
        
        @Override
        public AnnotatedTypeMirror subsitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            // canno subsitute
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
        
        AnnotatedWildcardType(TypeMirror type, ProcessingEnvironment env) {
            super(type, env);
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
            return this.extendsBound;
        }

        @Override
        public <R, P> R accept(AnnotatedTypeVisitor<R, P> v, P p) {
            return v.visitWildcard(this, p);
        }
        
        @Override
        public AnnotatedWildcardType getCopy(boolean annotation) {
            AnnotatedWildcardType type = new AnnotatedWildcardType(actualType, env);
            copyFields(type, annotation);
            
            type.setExtendsBound(getExtendsBound());
            type.setSuperBound(getSuperBound());
            
            return type;
        }
        
        @Override
        public AnnotatedTypeMirror subsitute(
                Map<? extends AnnotatedTypeMirror,
                        ? extends AnnotatedTypeMirror> mappings) {
            if (mappings.containsKey(this))
                return mappings.get(this);
            
            AnnotatedWildcardType type = getCopy(true);
            type.setExtendsBound(getExtendsBound().subsitute(mappings));
            type.setSuperBound(getExtendsBound().subsitute(mappings));
            return type;
        }

        @Override
        public AnnotatedTypeMirror getErasured() {
            // |T extends A&B| = |A|
            return getExtendsBound().getErasured();
        }
    }
}
