package org.checkerframework.framework.type;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeVisitor;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.StringsPlume;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/**
 * BoundsInitializer creates AnnotatedTypeMirrors (without annotations) for the bounds of type
 * variables and wildcards. Its static helper methods are called from AnnotatedTypeMirror. When an
 * initializer method is called for a particular bound, the entirety of that bound, including
 * circular references, will be created.
 */
public class BoundsInitializer {
    // ============================================================================================
    // Static helper methods called from AnnotatedTypeMirror to initialize bounds of wildcards or
    // type variables
    // ============================================================================================

    /**
     * Initializes the type arguments of {@code declaredType}. The upper bound of unbound wildcards
     * is set to the upper bound of the type parameter for which it is an argument. If {@code
     * declaredType} is raw, then the type arguments are uninferred wildcards.
     *
     * @param declaredType type whose arguments are initialized
     */
    public static void initializeTypeArgs(AnnotatedDeclaredType declaredType) {
        final DeclaredType underlyingType = (DeclaredType) declaredType.underlyingType;
        if (underlyingType.getTypeArguments().isEmpty() && !declaredType.isUnderlyingTypeRaw()) {
            // No type arguments to initialize.
            return;
        }

        final TypeElement typeElement =
                (TypeElement) declaredType.atypeFactory.types.asElement(underlyingType);
        int numTypeParameters = typeElement.getTypeParameters().size();
        final List<AnnotatedTypeMirror> typeArgs = new ArrayList<>(numTypeParameters);

        // Create AnnotatedTypeMirror for each type argument and store them in the typeArgsMap.
        // Take un-annotated type variables as the key for this map.
        Map<TypeVariable, AnnotatedTypeMirror> typeArgMap = new HashMap<>(numTypeParameters);
        for (int i = 0; i < numTypeParameters; i++) {
            TypeMirror javaTypeArg;
            if (declaredType.isUnderlyingTypeRaw()) {
                TypeVariable typeVariable =
                        (TypeVariable) typeElement.getTypeParameters().get(i).asType();
                javaTypeArg = getUpperBoundAsWildcard(typeVariable, declaredType.atypeFactory);
            } else {
                javaTypeArg = declaredType.getUnderlyingType().getTypeArguments().get(i);
            }

            final AnnotatedTypeMirror typeArg =
                    AnnotatedTypeMirror.createType(
                            javaTypeArg, declaredType.atypeFactory, declaredType.isDeclaration());
            if (typeArg.getKind() == TypeKind.WILDCARD) {
                AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) typeArg;
                wildcardType.setTypeVariable(typeElement.getTypeParameters().get(i));
                if (declaredType.isUnderlyingTypeRaw()) {
                    wildcardType.setUninferredTypeArgument();
                }
            }
            typeArgs.add(typeArg);

            // Add mapping from type parameter to the annotated type argument.
            TypeVariable key =
                    (TypeVariable)
                            TypeAnnotationUtils.unannotatedType(
                                    typeElement.getTypeParameters().get(i).asType());
            typeArgMap.put(key, typeArg);

            if (javaTypeArg.getKind() == TypeKind.TYPEVAR) {
                // Add mapping from Java type argument to the annotated type argument.
                key = (TypeVariable) TypeAnnotationUtils.unannotatedType(javaTypeArg);
                typeArgMap.put(key, typeArg);
            }
        }

        // Initialize type argument bounds using the typeArgsMap.
        for (AnnotatedTypeMirror typeArg : typeArgs) {
            switch (typeArg.getKind()) {
                case WILDCARD:
                    AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) typeArg;
                    initializeExtendsBound(wildcardType, typeArgMap);
                    initializeSuperBound(wildcardType, typeArgMap);
                    break;
                case TYPEVAR:
                    initializeBounds((AnnotatedTypeVariable) typeArg, typeArgMap);
                    break;
                default:
                    // do nothing
            }
        }
        declaredType.typeArgs = Collections.unmodifiableList(typeArgs);
    }

    /**
     * Returns a wildcard whose upper bound is the same as {@code typeVariable}. If the upper bound
     * is an intersection, then this method returns an unbound wildcard.
     */
    private static WildcardType getUpperBoundAsWildcard(
            TypeVariable typeVariable, AnnotatedTypeFactory factory) {
        TypeMirror upperBound = typeVariable.getUpperBound();
        switch (upperBound.getKind()) {
            case ARRAY:
            case DECLARED:
            case TYPEVAR:
                return factory.types.getWildcardType(upperBound, null);
            case INTERSECTION:
                // Can't create a wildcard with an intersection as the upper bound, so use
                // an unbound wildcard instead.  The extends bound of the
                // AnnotatedWildcardType will be initialized properly by this class.
                return factory.types.getWildcardType(null, null);
            default:
                throw new BugInCF(
                        "Unexpected upper bound kind: %s type: %s",
                        upperBound.getKind(), upperBound);
        }
    }

    /**
     * Create the entire lower bound and upper bound, with no missing information, for typeVar. If a
     * typeVar is recursive the appropriate cycles will be introduced in the type
     *
     * @param typeVar the type variable whose lower bound is being initialized
     */
    public static void initializeBounds(final AnnotatedTypeVariable typeVar) {
        initializeBounds(typeVar, null);
    }

    /**
     * Create the entire lower bound and upper bound, with no missing information, for typeVar. If a
     * typeVar is recursive the appropriate cycles will be introduced in the type
     *
     * @param typeVar the type variable whose lower bound is being initialized
     * @param map a mapping of type parameters to type arguments. May be null.
     */
    private static void initializeBounds(
            final AnnotatedTypeVariable typeVar, Map<TypeVariable, AnnotatedTypeMirror> map) {
        final Set<AnnotationMirror> annos = saveAnnotations(typeVar);

        InitializerVisitor visitor =
                new InitializerVisitor(new TypeVariableStructure(typeVar), map);
        visitor.initializeLowerBound(typeVar);
        visitor.resolveTypeVarReferences(typeVar);

        InitializerVisitor visitor2 =
                new InitializerVisitor(new TypeVariableStructure(typeVar), map);
        visitor2.initializeUpperBound(typeVar);
        visitor2.resolveTypeVarReferences(typeVar);

        restoreAnnotations(typeVar, annos);
    }

    /**
     * If we are initializing a type variable with a primary annotation than we should first
     * initialize it as if it were a declaration (i.e. as if it had no primary annotations) and then
     * apply the primary annotations. We do this so that when we make copies of the original type to
     * represent recursive references the recursive references don't have the primary annotation.
     *
     * <pre>{@code
     * e.g.   given the declaration {@code <E extends List<E>>}
     *        if we do not do this, the NonNull on the use @NonNull E
     *        would be copied to the primary annotation on E in the bound {@code List<E>}
     *        i.e. the use would be {@code <@NonNull E extends @NonNull List<@NonNull E>>}
     *             rather than      {@code <@NonNull E extends @NonNull List<E>>}
     * }</pre>
     */
    private static Set<AnnotationMirror> saveAnnotations(final AnnotatedTypeMirror type) {
        if (!type.getAnnotationsField().isEmpty()) {
            final Set<AnnotationMirror> annos = new HashSet<>(type.getAnnotations());
            type.clearPrimaryAnnotations();
            return annos;
        }

        return null;
    }

    private static void restoreAnnotations(
            final AnnotatedTypeMirror type, final Set<AnnotationMirror> annos) {
        if (annos != null) {
            type.addAnnotations(annos);
        }
    }

    /**
     * Create the entire super bound, with no missing information, for wildcard. If a wildcard is
     * recursive the appropriate cycles will be introduced in the type
     *
     * @param wildcard the wildcard whose lower bound is being initialized
     */
    public static void initializeSuperBound(final AnnotatedWildcardType wildcard) {
        initializeSuperBound(wildcard, null);
    }

    /**
     * Create the entire super bound, with no missing information, for wildcard. If a wildcard is
     * recursive the appropriate cycles will be introduced in the type
     *
     * @param wildcard the wildcard whose lower bound is being initialized
     * @param map a mapping of type parameters to type arguments. May be null.
     */
    private static void initializeSuperBound(
            final AnnotatedWildcardType wildcard, Map<TypeVariable, AnnotatedTypeMirror> map) {
        final Set<AnnotationMirror> annos = saveAnnotations(wildcard);

        InitializerVisitor visitor = new InitializerVisitor(new RecursiveTypeStructure(), map);
        visitor.initializeSuperBound(wildcard);
        visitor.resolveTypeVarReferences(wildcard);

        restoreAnnotations(wildcard, annos);
    }

    /**
     * Create the entire extends bound, with no missing information, for wildcard. If a wildcard is
     * recursive the appropriate cycles will be introduced in the type
     *
     * @param wildcard the wildcard whose extends bound is being initialized
     */
    public static void initializeExtendsBound(final AnnotatedWildcardType wildcard) {
        initializeExtendsBound(wildcard, null);
    }

    /**
     * Create the entire extends bound, with no missing information, for wildcard. If a wildcard is
     * recursive the appropriate cycles will be introduced in the type
     *
     * @param wildcard the wildcard whose extends bound is being initialized
     * @param map a mapping of type parameters to type arguments. May be null.
     */
    private static void initializeExtendsBound(
            final AnnotatedWildcardType wildcard, Map<TypeVariable, AnnotatedTypeMirror> map) {
        final Set<AnnotationMirror> annos = saveAnnotations(wildcard);
        InitializerVisitor visitor = new InitializerVisitor(new RecursiveTypeStructure(), map);
        visitor.initializeExtendsBound(wildcard);
        visitor.resolveTypeVarReferences(wildcard);
        restoreAnnotations(wildcard, annos);
    }

    // ============================================================================================
    // Classes and methods used to make the above static helper methods work
    // ============================================================================================

    /**
     * Creates the AnnotatedTypeMirrors (without annotations) for the bounds of all type variables
     * and wildcards in a given type. If the type is recursive, {@code T extends Comparable<T>},
     * then all references to the same type variable are references to the same AnnotatedTypeMirror.
     */
    private static class InitializerVisitor implements AnnotatedTypeVisitor<Void, Void> {
        /**
         * The {@link RecursiveTypeStructure} corresponding to the first wildcard or type variable
         * bound initialization that kicked this visitation off.
         */
        private final RecursiveTypeStructure topLevelStructure;

        /**
         * The {@link RecursiveTypeStructure} corresponding to the wildcard or type variable that is
         * currently being visited.
         */
        private RecursiveTypeStructure currentStructure;

        /** A mapping from TypeVariable to its {@link TypeVariableStructure}. */
        private final Map<TypeVariable, TypeVariableStructure> typeVarToStructure = new HashMap<>();

        /**
         * A mapping from WildcardType to its AnnotatedWildcardType. The first time this visitor
         * encounters a wildcard it creates an annotated type and adds it to this map. The next time
         * the wilcard is encounter, the annotated type in this map is returned.
         */
        private final Map<WildcardType, AnnotatedWildcardType> wildcards = new HashMap<>();

        /**
         * A mapping from IntersectionType to its AnnotatedIntersectionType. The first time this
         * visitor encounters an intersection it creates an annotated type and adds it to this map.
         * The next time the intersection is encounter, the annotated type in this map is returned.
         */
        private final Map<IntersectionType, AnnotatedIntersectionType> intersections =
                new HashMap<>();

        /**
         * Mapping from TypeVariable to AnnotatedTypeMirror. The annotated type mirror should be
         * used for any use of the type variable rather than creating and initializing a new
         * annotated type. This is used for type arguments that have already been initialized
         * outside of this visitor.
         */
        private final Map<TypeVariable, AnnotatedTypeMirror> typevars;

        /**
         * Creates an InitializerVisitor.
         *
         * @param recursiveTypeStructure structure for the type being initialized
         * @param typevars a mapping from type variable to annotated types that have already been
         *     initialized
         */
        public InitializerVisitor(
                RecursiveTypeStructure recursiveTypeStructure,
                Map<TypeVariable, AnnotatedTypeMirror> typevars) {
            this.topLevelStructure = recursiveTypeStructure;
            this.currentStructure = recursiveTypeStructure;
            if (typevars != null) {
                this.typevars = typevars;
            } else {
                this.typevars = Collections.emptyMap();
            }
            if (recursiveTypeStructure instanceof TypeVariableStructure) {
                TypeVariableStructure typeVarStruct =
                        (TypeVariableStructure) recursiveTypeStructure;
                typeVarToStructure.put(typeVarStruct.typeVar, typeVarStruct);
            }
        }

        // ----------------------------------------------------------------------------------------
        // Visit methods that keep track of the path traversed through type variable bounds, and the
        // wildcards/intersections that have been encountered.
        // ----------------------------------------------------------------------------------------

        @Override
        public Void visit(AnnotatedTypeMirror type) {
            type.accept(this, null);
            return null;
        }

        @Override
        public Void visit(AnnotatedTypeMirror type, Void aVoid) {
            visit(type);
            return null;
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Void aVoid) {
            initializeTypeArgs(type);
            if (type.enclosingType != null) {
                TypePathNode node = currentStructure.addPathNode(new EnclosingTypeNode());
                visit(type.enclosingType);
                currentStructure.removePathNode(node);
            }
            return null;
        }

        @Override
        public Void visitIntersection(AnnotatedIntersectionType type, Void aVoid) {

            if (intersections.containsKey(type.getUnderlyingType())) {
                return null;
            }

            intersections.put(type.getUnderlyingType(), type);

            List<AnnotatedTypeMirror> bounds = type.getBounds();
            for (int i = 0; i < bounds.size(); i++) {
                AnnotatedTypeMirror supertype = bounds.get(i);
                TypePathNode node = currentStructure.addPathNode(new IntersectionBoundNode(i));
                visit(supertype);
                currentStructure.removePathNode(node);
            }
            return null;
        }

        @Override
        public Void visitUnion(AnnotatedUnionType type, Void aVoid) {

            List<AnnotatedDeclaredType> alts = type.getAlternatives();
            for (int i = 0; i < alts.size(); i++) {
                AnnotatedDeclaredType alt = alts.get(i);
                TypePathNode node = currentStructure.addPathNode(new AlternativeTypeNode(i));
                visit(alt);
                currentStructure.removePathNode(node);
            }
            return null;
        }

        @Override
        public Void visitArray(AnnotatedArrayType type, Void aVoid) {
            if (!TypesUtils.isPrimitive(type.getComponentType().getUnderlyingType())) {
                // Only recur on component type if it's not a primitive.
                // Array component types are the only place a primitive is allowed in bounds
                TypePathNode componentNode = currentStructure.addPathNode(new ArrayComponentNode());
                type.setComponentType(getOrVisit(type.getComponentType()));
                currentStructure.removePathNode(componentNode);
            }
            return null;
        }

        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, Void aVoid) {
            this.currentStructure.addTypeVar(type.getUnderlyingType());
            if (typeVarToStructure.containsKey(type.getUnderlyingType())) {
                return null;
            }
            TypeVariableStructure typeVarStruct = new TypeVariableStructure(type);
            typeVarToStructure.put(type.getUnderlyingType(), typeVarStruct);
            RecursiveTypeStructure parentStructure = this.currentStructure;

            // If type is a captured type variable, then its type variables should be created new,
            // rather than using one from the rest of the type.  So, clear the typevars map of all
            // but the mapping with key type.
            Map<TypeVariable, AnnotatedTypeMirror> hold = new HashMap<>();
            if (TypesUtils.isCapturedTypeVariable(type.getUnderlyingType())) {
                for (Map.Entry<TypeVariable, AnnotatedTypeMirror> entry :
                        new ArrayList<>(typevars.entrySet())) {
                    if (!type.atypeFactory.types.isSameType(
                            entry.getKey(), entry.getValue().underlyingType)) {
                        hold.put(entry.getKey(), entry.getValue());
                        typevars.remove(entry.getKey(), entry.getValue());
                    }
                }
            }
            this.currentStructure = typeVarStruct;
            initializeUpperBound(type);
            initializeLowerBound(type);
            this.currentStructure = parentStructure;
            typevars.putAll(hold);

            return null;
        }

        @Override
        public Void visitNull(AnnotatedNullType type, Void aVoid) {
            return null;
        }

        @Override
        public Void visitWildcard(AnnotatedWildcardType wildcard, Void aVoid) {
            if (wildcard.getSuperBoundField() == null) {
                initializeSuperBound(wildcard);

            } else {
                throw new BugInCF(
                        "Wildcard super field should not be initialized:%n"
                                + "wildcard=%s%n"
                                + "currentStructure=%s%n",
                        wildcard, currentStructure);
            }

            if (wildcard.getExtendsBoundField() == null) {
                initializeExtendsBound(wildcard);
            } else {
                throw new BugInCF(
                        "Wildcard extends field should not be initialized:%n"
                                + "wildcard=%s%n"
                                + "currentStructure=%s%n",
                        wildcard, currentStructure);
            }

            return null;
        }

        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType type, Void aVoid) {
            throw new BugInCF("Unexpected AnnotatedPrimitiveType " + type);
        }

        @Override
        public Void visitNoType(AnnotatedNoType type, Void aVoid) {
            throw new BugInCF("Unexpected AnnotatedNoType " + type);
        }

        @Override
        public Void visitExecutable(AnnotatedExecutableType type, Void aVoid) {
            throw new BugInCF("Unexpected AnnotatedExecutableType " + type);
        }

        /**
         * If the underlying type of {@code type} has been visited before, return the previous
         * AnnotatedTypeMirror. Otherwise, visit {@code type} and return it.
         *
         * @param type type to visit
         * @return {@code type} or an AnnotatedTypeMirror with the same underlying type that was
         *     previously visited.
         */
        public AnnotatedTypeMirror getOrVisit(AnnotatedTypeMirror type) {
            switch (type.getKind()) {
                case WILDCARD:
                    AnnotatedWildcardType wildcard = (AnnotatedWildcardType) type;
                    if (wildcards.containsKey(wildcard.getUnderlyingType())) {
                        return wildcards.get(wildcard.getUnderlyingType());
                    }
                    break;
                case INTERSECTION:
                    if (intersections.containsKey(type.getUnderlyingType())) {
                        return intersections.get(type.getUnderlyingType());
                    }
                    break;
                case TYPEVAR:
                    TypeVariable key =
                            (TypeVariable)
                                    TypeAnnotationUtils.unannotatedType(type.getUnderlyingType());
                    if (typevars.containsKey(key)) {
                        return typevars.get(key);
                    }
                    break;
                default:
                    // do nothing
            }
            visit(type);
            return type;
        }

        // ----------------------------------------------------------------------------------------
        //

        /**
         * Initialize {@code typeVar}'s upper bound.
         *
         * @param typeVar type variable whose upper bound is initialized
         */
        public void initializeUpperBound(AnnotatedTypeVariable typeVar) {
            AnnotatedTypeMirror upperBound = createAndSetUpperBound(typeVar);

            TypePathNode pathNode = new UpperBoundNode();
            currentStructure.addPathNode(pathNode);
            visit(upperBound);
            currentStructure.removePathNode(pathNode);
        }

        /**
         * Initialize {@code typeVar}'s lower bound.
         *
         * @param typeVar type variable whose lower bound is initialized
         */
        public void initializeLowerBound(AnnotatedTypeVariable typeVar) {
            AnnotatedTypeMirror lowerBound = createAndSetLowerBound(typeVar);

            TypePathNode pathNode = new LowerBoundNode();
            currentStructure.addPathNode(pathNode);
            visit(lowerBound);
            currentStructure.removePathNode(pathNode);
        }

        /**
         * Initialize {@code wildcard}'s super bound.
         *
         * @param wildcard wildcard whose super bound is initialized
         */
        public void initializeSuperBound(AnnotatedWildcardType wildcard) {
            AnnotatedTypeFactory typeFactory = wildcard.atypeFactory;

            WildcardType underlyingType = wildcard.getUnderlyingType();
            TypeMirror underlyingSuperBound = underlyingType.getSuperBound();
            if (underlyingSuperBound == null) {
                underlyingSuperBound =
                        TypesUtils.wildLowerBound(
                                underlyingType, wildcard.atypeFactory.processingEnv);
            }

            AnnotatedTypeMirror superBound =
                    AnnotatedTypeMirror.createType(underlyingSuperBound, typeFactory, false);
            wildcard.setSuperBound(superBound);

            this.wildcards.put(wildcard.getUnderlyingType(), wildcard);

            TypePathNode superNode = currentStructure.addPathNode(new SuperBoundNode());
            visit(superBound);
            currentStructure.removePathNode(superNode);
        }

        /**
         * Initialize {@code wildcard}'s extends bound.
         *
         * @param wildcard wildcard whose extends bound is initialized
         */
        public void initializeExtendsBound(AnnotatedWildcardType wildcard) {
            AnnotatedTypeFactory typeFactory = wildcard.atypeFactory;

            WildcardType javaWildcardType = wildcard.getUnderlyingType();
            TypeMirror javaExtendsBound;
            if (javaWildcardType.getExtendsBound() != null) {
                javaExtendsBound = javaWildcardType.getExtendsBound();
            } else {
                javaExtendsBound = TypesUtils.getObjectTypeMirror(typeFactory.processingEnv);
            }

            if (wildcard.isUninferredTypeArgument()) {
                rawTypeWildcards.put(wildcard.getTypeVariable(), wildcard.getUnderlyingType());
            }

            AnnotatedTypeMirror extendsBound =
                    AnnotatedTypeMirror.createType(javaExtendsBound, typeFactory, false);
            wildcard.setExtendsBound(extendsBound);

            this.wildcards.put(wildcard.getUnderlyingType(), wildcard);

            TypePathNode extendsNode = currentStructure.addPathNode(new ExtendsBoundNode());
            visit(extendsBound);
            currentStructure.removePathNode(extendsNode);
        }

        /**
         * Initialize {@code declaredType}'s type arguments.
         *
         * @param declaredType declared type whose type arguments are initialized
         */
        private void initializeTypeArgs(AnnotatedDeclaredType declaredType) {
            DeclaredType underlyingType = (DeclaredType) declaredType.underlyingType;
            if (underlyingType.getTypeArguments().isEmpty()
                    && !declaredType.isUnderlyingTypeRaw()) {
                return;
            }
            TypeElement typeElement =
                    (TypeElement) declaredType.atypeFactory.types.asElement(underlyingType);
            List<AnnotatedTypeMirror> typeArgs;
            if (declaredType.typeArgs == null) {
                int numTypeParameters = typeElement.getTypeParameters().size();
                typeArgs = new ArrayList<>(numTypeParameters);
                for (int i = 0; i < numTypeParameters; i++) {
                    TypeMirror javaTypeArg =
                            getJavaType(declaredType, typeElement.getTypeParameters(), i);
                    AnnotatedTypeMirror atmArg =
                            AnnotatedTypeMirror.createType(
                                    javaTypeArg, declaredType.atypeFactory, false);
                    typeArgs.add(atmArg);
                    if (atmArg.getKind() == TypeKind.WILDCARD
                            && declaredType.isUnderlyingTypeRaw()) {
                        ((AnnotatedWildcardType) atmArg).setUninferredTypeArgument();
                    }
                }
            } else {
                typeArgs = declaredType.typeArgs;
            }

            List<AnnotatedTypeMirror> typeArgReplacements = new ArrayList<>(typeArgs.size());
            for (int i = 0; i < typeArgs.size(); i++) {
                AnnotatedTypeMirror typeArg = typeArgs.get(i);
                TypePathNode node = currentStructure.addPathNode(new TypeArgNode(i));
                if (typeArg.getKind() == TypeKind.WILDCARD) {
                    ((AnnotatedWildcardType) typeArg)
                            .setTypeVariable(typeElement.getTypeParameters().get(i));
                }
                typeArgReplacements.add(getOrVisit(typeArg));
                currentStructure.removePathNode(node);
            }

            declaredType.setTypeArguments(typeArgReplacements);
        }

        /**
         * Store the wildcards created as type arguments to raw types.
         *
         * <p>{@code class Foo<T extends Foo> {}} The upper bound of the wildcard in {@code Foo<?>}
         * is {@code Foo}. The type argument of {@code Foo} is initialized to {@code ? extends Foo}.
         * The type argument of {@code Foo} in {@code ? extends Foo} needs to be initialized to the
         * same type argument as the first {@code Foo} so that
         * BoundsInitializer.InitializerVisitor#getOrVisit will return the cached
         * AnnotatedWildcardType.
         */
        private final Map<TypeVariable, WildcardType> rawTypeWildcards = new HashMap<>();

        /**
         * Returns the underlying Java type of the {@code i}-th type argument of {@code type}. If
         * {@code type} is raw, then a new wildcard is created or returned from {@code
         * rawTypeWildcards}.
         *
         * @param type declared type
         * @param parameters elements of the type parameters
         * @param i index of the type parameter
         * @return the underlying Java type of the {@code i}-th type argument of {@code type}
         */
        private TypeMirror getJavaType(
                AnnotatedDeclaredType type,
                List<? extends TypeParameterElement> parameters,
                int i) {
            if (type.isUnderlyingTypeRaw()) {
                TypeVariable typeVariable = (TypeVariable) parameters.get(i).asType();
                if (rawTypeWildcards.containsKey(typeVariable)) {
                    return rawTypeWildcards.get(typeVariable);
                }
                WildcardType wildcard = getUpperBoundAsWildcard(typeVariable, type.atypeFactory);
                rawTypeWildcards.put(typeVariable, wildcard);
                return wildcard;
            } else {
                return type.getUnderlyingType().getTypeArguments().get(i);
            }
        }

        /**
         * Replace all type variables in type with the AnnotatedTypeMirrors created when
         * initializing it.
         *
         * @param type all type variables are replaced
         */
        public void resolveTypeVarReferences(AnnotatedTypeMirror type) {
            List<AnnotatedTypeVariable> annotatedTypeVars = new ArrayList<>();
            if (type.getKind() == TypeKind.TYPEVAR) {
                annotatedTypeVars.add((AnnotatedTypeVariable) type);
            }

            // Gather a list of all AnnotatedTypeVariables and all the replacements to perform.
            for (TypeVariableStructure typeVarStruct : typeVarToStructure.values()) {
                typeVarStruct.findAllReplacements(typeVarToStructure);
                annotatedTypeVars.addAll(typeVarStruct.getAnnotatedTypeVars());
            }

            // Do the replacements.
            for (AnnotatedTypeVariable atv : annotatedTypeVars) {
                TypeVariableStructure list = typeVarToStructure.get(atv.getUnderlyingType());
                list.replaceTypeVariablesInType(atv);
            }

            if (type.getKind() == TypeKind.WILDCARD) {
                // Do the "top level" replacements.
                AnnotatedWildcardType wildcard = (AnnotatedWildcardType) type;
                topLevelStructure.findAllReplacements(typeVarToStructure);
                for (AnnotatedTypeVariable typeVar : topLevelStructure.getAnnotatedTypeVars()) {
                    TypeVariableStructure list =
                            typeVarToStructure.get(typeVar.getUnderlyingType());
                    list.replaceTypeVariablesInType(typeVar);
                }
                topLevelStructure.replaceTypeVariablesInType(wildcard);
            }
        }
    }

    /**
     * Creates the upper bound type for {@code typeVar} and sets it.
     *
     * @param typeVar type variable
     * @return the newly created upper bound
     */
    private static AnnotatedTypeMirror createAndSetUpperBound(AnnotatedTypeVariable typeVar) {
        AnnotatedTypeMirror upperBound =
                AnnotatedTypeMirror.createType(
                        typeVar.getUnderlyingType().getUpperBound(), typeVar.atypeFactory, false);
        typeVar.setUpperBound(upperBound);
        return upperBound;
    }

    /**
     * Creates the lower bound type for {@code typeVar} and sets it. If the type variable does not
     * have a lower bound, then a null type is created.
     *
     * @param typeVar type variable
     * @return the newly created lower bound
     */
    private static AnnotatedTypeMirror createAndSetLowerBound(AnnotatedTypeVariable typeVar) {
        TypeMirror lb = typeVar.getUnderlyingType().getLowerBound();
        if (lb == null) {
            // Use bottom type to ensure there is a lower bound.
            Context context =
                    ((JavacProcessingEnvironment) typeVar.atypeFactory.processingEnv).getContext();
            Symtab syms = Symtab.instance(context);
            lb = syms.botType;
        }
        AnnotatedTypeMirror lowerBound =
                AnnotatedTypeMirror.createType(lb, typeVar.atypeFactory, false);
        typeVar.setLowerBound(lowerBound);
        return lowerBound;
    }

    /**
     * Contains all the type variables and the type path to reach them found when scanning a
     * particular type variable or wildcard. Then uses this information to replace the type
     * variables with AnnotatedTypeVariables.
     */
    private static class RecursiveTypeStructure {

        /** List of TypePath and TypeVariables that were found will traversing this type. */
        private final List<Pair<TypePath, TypeVariable>> typeVarsInType = new ArrayList<>();

        /** Current path used to mark the locations of TypeVariables. */
        private final TypePath currentPath = new TypePath();

        /**
         * Add a type variable found at the current path while visiting the type variable or
         * wildcard associated with this structure.
         *
         * @param typeVariable TypeVariable
         */
        public void addTypeVar(TypeVariable typeVariable) {
            typeVarsInType.add(Pair.of(this.currentPath.copy(), typeVariable));
        }

        /**
         * Add a node in the path.
         *
         * @param node node to add
         * @return {@code node}
         */
        public TypePathNode addPathNode(TypePathNode node) {
            currentPath.add(node);
            return node;
        }

        /**
         * Remove the last node in the path if it is {@code node}; otherwise, throw an exception.
         *
         * @param node last node in the path
         */
        public void removePathNode(@FindDistinct TypePathNode node) {
            if (currentPath.getLeaf() != node) {
                throw new BugInCF(
                        "Cannot remove node: %s. It is not the last node. currentPath= %s",
                        node, currentPath);
            }
            currentPath.removeLeaf();
        }

        /**
         * For all type variables contained within the type variable or wildcard that this structure
         * represents, this a list of the replacement {@link AnnotatedTypeVariable} for the location
         * specified by the {@link TypePath}.
         */
        private List<Pair<TypePath, AnnotatedTypeVariable>> replacementList;

        /**
         * Find the AnnotatedTypeVariables that should replace the type variables found in this
         * type.
         *
         * @param typeVarToStructure a mapping from TypeVariable to TypeVariableStructure
         */
        public void findAllReplacements(
                Map<TypeVariable, TypeVariableStructure> typeVarToStructure) {
            this.annotatedTypeVariables = new ArrayList<>(typeVarsInType.size());
            this.replacementList = new ArrayList<>(typeVarsInType.size());
            for (Pair<TypePath, TypeVariable> pair : typeVarsInType) {
                TypeVariableStructure targetStructure = typeVarToStructure.get(pair.second);
                AnnotatedTypeVariable template =
                        targetStructure.annotatedTypeVar.deepCopy().asUse();
                annotatedTypeVariables.add(template);
                replacementList.add(Pair.of(pair.first, template));
            }
        }

        /** List of {@link AnnotatedTypeVariable}s found in this type. */
        private List<AnnotatedTypeVariable> annotatedTypeVariables;

        /**
         * A list of all AnnotatedTypeVariables found in this type. {@link
         * #findAllReplacements(Map)} must be called first.
         *
         * @return a list of all AnnotatedTypeVariables found in this type
         */
        public List<AnnotatedTypeVariable> getAnnotatedTypeVars() {
            if (annotatedTypeVariables == null) {
                throw new BugInCF("Call createReplacementList before calling this method.");
            }
            return annotatedTypeVariables;
        }

        /**
         * Replaces all type variables in {@code type} with their replacements. ({@link
         * #findAllReplacements(Map)} must be called first so that the replacements can be found.)
         *
         * @param type annotated type whose type variables are replaced
         */
        public void replaceTypeVariablesInType(AnnotatedTypeMirror type) {
            if (replacementList == null) {
                throw new BugInCF("Call createReplacementList before calling this method.");
            }
            for (Pair<TypePath, AnnotatedTypeVariable> entry : replacementList) {
                TypePath path = entry.first;
                AnnotatedTypeVariable replacement = entry.second;
                path.replaceTypeVariable(type, replacement);
            }
        }
    }

    /** A {@link RecursiveTypeStructure} for a type variable. */
    private static class TypeVariableStructure extends RecursiveTypeStructure {
        /** The type variable whose structure is being described. */
        public final TypeVariable typeVar;

        /**
         * The first annotated type variable that was encountered and traversed in order to describe
         * typeVar. It is expanded during visitation and it is later used as a template for other
         * uses of typeVar
         */
        public final AnnotatedTypeVariable annotatedTypeVar;

        /**
         * Creates an {@link TypeVariableStructure}
         *
         * @param annotatedTypeVar annotated type for the type variable whose structure is being
         *     described
         */
        public TypeVariableStructure(AnnotatedTypeVariable annotatedTypeVar) {
            this.typeVar = annotatedTypeVar.getUnderlyingType();
            this.annotatedTypeVar = annotatedTypeVar;
        }
    }

    /**
     * A list of {@link TypePathNode}s. Each node represents a "location" of a composite type. For
     * example, an {@link UpperBoundNode} represents the upper bound type of a type variable
     */
    @SuppressWarnings("serial")
    private static class TypePath extends ArrayList<TypePathNode> {

        @Override
        public String toString() {
            return StringsPlume.join(",", this);
        }

        /**
         * Create a copy of this path.
         *
         * @return a copy of this path
         */
        public TypePath copy() {
            TypePath copy = new TypePath();
            for (TypePathNode node : this) {
                copy.add(node.copy());
            }
            return copy;
        }

        /**
         * Return the leaf node of this path.
         *
         * @return the leaf node or null if the path is empty
         */
        public TypePathNode getLeaf() {
            if (this.isEmpty()) {
                return null;
            }
            return this.get(size() - 1);
        }

        /** Remove the leaf node if one exists. */
        public void removeLeaf() {
            if (this.isEmpty()) {
                return;
            }
            this.remove(size() - 1);
        }

        /**
         * In {@code type}, replace the type at the location specified by this path with {@code
         * replacement}.
         *
         * @param type annotated type that is side-effected
         * @param replacement annotated type to add to {@code type}
         */
        public void replaceTypeVariable(
                AnnotatedTypeMirror type, AnnotatedTypeVariable replacement) {
            AnnotatedTypeMirror current = type;
            for (int i = 0; i < size() - 1; i++) {
                current = get(i).getType(current);
            }
            this.getLeaf().replaceType(current, replacement);
        }
    }

    /**
     * A {@link TypePathNode} represents a "location" of a composite type. For example, an {@link
     * UpperBoundNode} represents the upper bound type of a type variable.
     */
    private abstract static class TypePathNode {

        /** The {@link TypeKind} of the parent of this node. */
        public final TypeKind parentTypeKind;

        /**
         * Creates a {@link TypePathNode}.
         *
         * @param parentTypeKind kind of parent of this node
         */
        TypePathNode(TypeKind parentTypeKind) {
            this.parentTypeKind = parentTypeKind;
        }

        /**
         * A copy constructor.
         *
         * @param template node to copy
         */
        TypePathNode(TypePathNode template) {
            this.parentTypeKind = template.parentTypeKind;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName();
        }

        /**
         * Returns the annotated type at the location represented by this node in {@code type}.
         *
         * @param type parent type
         * @return the annotated type at the location represented by this node in {@code type}
         * @throws BugInCF if {@code type} does not have a type at this location
         */
        public final AnnotatedTypeMirror getType(AnnotatedTypeMirror type) {
            abortIfNotKind(parentTypeKind, null, type);
            return getTypeInternal(type);
        }

        /**
         * Internal implementation of {@link #getType(AnnotatedTypeMirror)}.
         *
         * @param parent type that is sideffected by this method
         * @return the annotated type at the location represented by this node in {@code type}
         */
        protected abstract AnnotatedTypeMirror getTypeInternal(AnnotatedTypeMirror parent);

        /**
         * Replaces the type at the location represented by this node in {@code parent} with {@code
         * replacement}.
         *
         * @param parent type that is sideffected by this method
         * @param replacement the replacement
         * @throws BugInCF if {@code type} does not have a type at this location
         */
        public final void replaceType(
                AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            abortIfNotKind(parentTypeKind, replacement, parent);
            replaceTypeInternal(parent, replacement);
        }

        /**
         * Internal implementation of #replaceType.
         *
         * @param parent type that is sideffected by this method
         * @param replacement the replacement
         */
        protected abstract void replaceTypeInternal(
                AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement);

        /**
         * Returns a copy of the node.
         *
         * @return a copy of this node
         */
        public abstract TypePathNode copy();

        /**
         * Throws a {@link BugInCF} if {@code parent} is {@code typeKind}.
         *
         * @param typeKind TypeKind
         * @param replacement for debugging
         * @param parent possible parent type of this node
         * @throws BugInCF if {@code parent} is {@code typeKind}
         */
        private void abortIfNotKind(
                TypeKind typeKind, AnnotatedTypeVariable replacement, AnnotatedTypeMirror parent) {
            if (parent.getKind() == typeKind) {
                return;
            }

            throw new BugInCF(
                    "Unexpected parent kind:%nparent= %s%nreplacements= %s%n expected= %s",
                    parent, replacement, typeKind);
        }
    }

    /** Represents an enclosing type of a declared type. */
    private static class EnclosingTypeNode extends TypePathNode {

        /** Create an enclosing node. */
        EnclosingTypeNode() {
            super(TypeKind.DECLARED);
        }

        @Override
        protected void replaceTypeInternal(
                AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            // An enclosing type cannot be a type variable, so do nothing.
        }

        @Override
        public AnnotatedDeclaredType getTypeInternal(AnnotatedTypeMirror parent) {
            return ((AnnotatedDeclaredType) parent).getEnclosingType();
        }

        @Override
        public TypePathNode copy() {
            return new EnclosingTypeNode();
        }
    }

    /** Represents an extends bound of a wildcard. */
    private static class ExtendsBoundNode extends TypePathNode {
        /** Creates an ExtendsBoundNode. */
        ExtendsBoundNode() {
            super(TypeKind.WILDCARD);
        }

        @Override
        protected void replaceTypeInternal(
                AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            ((AnnotatedWildcardType) parent).setExtendsBound(replacement);
        }

        @Override
        protected AnnotatedTypeMirror getTypeInternal(AnnotatedTypeMirror parent) {
            return ((AnnotatedWildcardType) parent).getExtendsBound();
        }

        @Override
        public TypePathNode copy() {
            return new ExtendsBoundNode();
        }
    }

    /** Represents a super bound of a wildcard. */
    private static class SuperBoundNode extends TypePathNode {
        /** Creates a SuperBoundNode. */
        SuperBoundNode() {
            super(TypeKind.WILDCARD);
        }

        @Override
        protected void replaceTypeInternal(
                AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            ((AnnotatedWildcardType) parent).setSuperBound(replacement);
        }

        @Override
        protected AnnotatedTypeMirror getTypeInternal(AnnotatedTypeMirror parent) {
            return ((AnnotatedWildcardType) parent).getSuperBound();
        }

        @Override
        public TypePathNode copy() {
            return new SuperBoundNode();
        }
    }

    /** Represents an upper bound of a type variable. */
    private static class UpperBoundNode extends TypePathNode {

        /** Creates an UpperBoundNode. */
        UpperBoundNode() {
            super(TypeKind.TYPEVAR);
        }

        @Override
        protected void replaceTypeInternal(
                AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            ((AnnotatedTypeVariable) parent).setUpperBound(replacement);
        }

        @Override
        protected AnnotatedTypeMirror getTypeInternal(AnnotatedTypeMirror parent) {
            AnnotatedTypeVariable parentAtv = (AnnotatedTypeVariable) parent;
            if (parentAtv.getUpperBoundField() != null) {
                return parentAtv.getUpperBoundField();
            }
            return createAndSetUpperBound((AnnotatedTypeVariable) parent);
        }

        @Override
        public TypePathNode copy() {
            return new UpperBoundNode();
        }
    }

    /** Represents a lower bound of a type variable. */
    private static class LowerBoundNode extends TypePathNode {

        /** Creates a LowerBoundNode. */
        LowerBoundNode() {
            super(TypeKind.TYPEVAR);
        }

        @Override
        protected void replaceTypeInternal(
                AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            ((AnnotatedTypeVariable) parent).setLowerBound(replacement);
        }

        @Override
        protected AnnotatedTypeMirror getTypeInternal(AnnotatedTypeMirror parent) {

            AnnotatedTypeVariable parentAtv = (AnnotatedTypeVariable) parent;
            if (parentAtv.getLowerBoundField() != null) {
                return parentAtv.getLowerBoundField();
            }
            // else // TODO: I think this should never happen at this point, throw exception
            return createAndSetLowerBound((AnnotatedTypeVariable) parent);
        }

        @Override
        public TypePathNode copy() {
            return new LowerBoundNode();
        }
    }

    /** Represents a component type of an array type. */
    private static class ArrayComponentNode extends TypePathNode {

        /** Create ArrayComponentNode. */
        ArrayComponentNode() {
            super(TypeKind.ARRAY);
        }

        @Override
        protected void replaceTypeInternal(
                AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            ((AnnotatedArrayType) parent).setComponentType(replacement);
        }

        @Override
        protected AnnotatedTypeMirror getTypeInternal(AnnotatedTypeMirror parent) {
            return ((AnnotatedArrayType) parent).getComponentType();
        }

        @Override
        public TypePathNode copy() {
            return new ArrayComponentNode();
        }
    }

    /** A bound type of an intersection type. */
    private static class IntersectionBoundNode extends TypePathNode {

        /** The index of the particular bound type of an intersection type this node represents. */
        public final int boundIndex;

        /**
         * Creates an IntersectionBoundNode.
         *
         * @param boundIndex the index of the particular bound type of an intersection type this
         *     node represents
         */
        IntersectionBoundNode(int boundIndex) {
            super(TypeKind.INTERSECTION);
            this.boundIndex = boundIndex;
        }

        /**
         * Copy constructor.
         *
         * @param template node to copy
         */
        IntersectionBoundNode(IntersectionBoundNode template) {
            super(template);
            boundIndex = template.boundIndex;
        }

        @Override
        public String toString() {
            return super.toString() + "( superIndex=" + boundIndex + " )";
        }

        @Override
        protected void replaceTypeInternal(
                AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            AnnotatedIntersectionType intersection = (AnnotatedIntersectionType) parent;
            List<AnnotatedTypeMirror> bounds = new ArrayList<>(intersection.bounds);
            bounds.set(boundIndex, replacement);
            intersection.setBounds(bounds);
        }

        @Override
        protected AnnotatedTypeMirror getTypeInternal(AnnotatedTypeMirror parent) {
            AnnotatedIntersectionType isect = (AnnotatedIntersectionType) parent;
            if (isect.getBounds().size() <= boundIndex) {
                throw new BugInCF("Invalid superIndex %d: parent=%s", boundIndex, parent);
            }

            return isect.getBounds().get(boundIndex);
        }

        @Override
        public TypePathNode copy() {
            return new IntersectionBoundNode(this);
        }
    }

    /** Represents an alternative type of a union node. */
    private static class AlternativeTypeNode extends TypePathNode {

        /**
         * The index of the particular alternative type of the union node that this node represents.
         */
        public final int altIndex;

        /**
         * Creates a AlternativeTypeNode.
         *
         * @param altIndex the index of the particular alternative type of the union node that this
         *     node represents
         */
        AlternativeTypeNode(int altIndex) {
            super(TypeKind.UNION);
            this.altIndex = altIndex;
        }

        /**
         * Copy constructor.
         *
         * @param template node to copy
         */
        AlternativeTypeNode(AlternativeTypeNode template) {
            super(template);
            altIndex = template.altIndex;
        }

        @Override
        public String toString() {
            return super.toString() + "( altIndex=" + altIndex + " )";
        }

        @Override
        protected void replaceTypeInternal(
                AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            throw new BugInCF(
                    "Union types cannot be intersection bounds.%nparent=%s%nreplacement=%s",
                    parent, replacement);
        }

        @Override
        protected AnnotatedTypeMirror getTypeInternal(AnnotatedTypeMirror parent) {
            AnnotatedUnionType isect = (AnnotatedUnionType) parent;
            if (parent.directSupertypes().size() <= altIndex) {
                throw new BugInCF("Invalid altIndex( %s ):%nparent=%s", altIndex, parent);
            }

            return isect.directSupertypes().get(altIndex);
        }

        @Override
        public TypePathNode copy() {
            return new AlternativeTypeNode(this);
        }
    }

    /** Represents a type argument of a declared type. */
    private static class TypeArgNode extends TypePathNode {

        /** The index of the type argument that this node represents. */
        public final int argIndex;

        /**
         * Creates a TypeArgumentNode.
         *
         * @param argIndex index of the type argument that this node represents
         */
        TypeArgNode(int argIndex) {
            super(TypeKind.DECLARED);
            this.argIndex = argIndex;
        }

        /**
         * Copy constructor.
         *
         * @param template node to copy
         */
        TypeArgNode(TypeArgNode template) {
            super(template);
            this.argIndex = template.argIndex;
        }

        @Override
        public String toString() {
            return super.toString() + "( argIndex=" + argIndex + " )";
        }

        @Override
        protected void replaceTypeInternal(
                AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            AnnotatedDeclaredType parentAdt = (AnnotatedDeclaredType) parent;
            List<AnnotatedTypeMirror> typeArgs = new ArrayList<>(parentAdt.getTypeArguments());
            if (argIndex >= typeArgs.size()) {
                throw new BugInCF(
                        StringsPlume.joinLines(
                                "Invalid type arg index.",
                                "parent=" + parent,
                                "replacement=" + replacement,
                                "argIndex=" + argIndex));
            }
            typeArgs.add(argIndex, replacement);
            typeArgs.remove(argIndex + 1);
            parentAdt.setTypeArguments(typeArgs);
        }

        @Override
        protected AnnotatedTypeMirror getTypeInternal(AnnotatedTypeMirror parent) {
            AnnotatedDeclaredType parentAdt = (AnnotatedDeclaredType) parent;

            List<AnnotatedTypeMirror> typeArgs = parentAdt.getTypeArguments();
            if (argIndex >= typeArgs.size()) {
                throw new BugInCF(
                        StringsPlume.joinLines(
                                "Invalid type arg index.",
                                "parent=" + parent,
                                "argIndex=" + argIndex));
            }

            return typeArgs.get(argIndex);
        }

        @Override
        public TypePathNode copy() {
            return new TypeArgNode(this);
        }
    }
}
