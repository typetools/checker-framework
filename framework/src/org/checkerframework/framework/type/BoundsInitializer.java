package org.checkerframework.framework.type;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
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
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.TypesUtils;

/**
 * BoundsInitializer creates AnnotatedTypeMirrors (without annotations) for the bounds of type
 * variables and wildcards. Its static helper methods are called from AnnotatedTypeMirror. When an
 * initializer method is called for a particular bound, the entirety of that bound, including
 * circular references, will be created.
 */
public class BoundsInitializer {
    //==================================================================================================================
    // Static helper methods called from AnnotatedTypeMirror to initialize bounds of wildcards or type variables
    //==================================================================================================================

    /**
     * Create the entire lower bound and upper bound, with no missing information, for typeVar. If a
     * typeVar is recursive the appropriate cycles will be introduced in the type
     *
     * @param typeVar the type variable whose lower bound is being initialized
     */
    public static void initializeBounds(final AnnotatedTypeVariable typeVar) {
        final Set<AnnotationMirror> annos = saveAnnotations(typeVar);

        InitializerVisitor visitor =
                new InitializerVisitor(new TypeVariableStructure(null, typeVar));
        visitor.initializeLowerBound(typeVar);
        visitor.resolveTypeVarReferences(typeVar);

        InitializerVisitor visitor2 =
                new InitializerVisitor(new TypeVariableStructure(null, typeVar));
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
            type.clearAnnotations();
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
        final Set<AnnotationMirror> annos = saveAnnotations(wildcard);

        InitializerVisitor visitor = new InitializerVisitor(new WildcardStructure());
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
        final Set<AnnotationMirror> annos = saveAnnotations(wildcard);

        InitializerVisitor visitor = new InitializerVisitor(new WildcardStructure());
        visitor.initializeExtendsBound(wildcard);
        visitor.resolveTypeVarReferences(wildcard);
        restoreAnnotations(wildcard, annos);
    }

    //==================================================================================================================
    // Classes and methods used to make the above static helper methods work
    //==================================================================================================================

    /**
     * Creates the AnnotatedTypeMirrors (without annotations) for the bounds of all type variables
     * and wildcards in a given type. If the type is recursive, {@code T extends Comparable<T>},
     * then all references to the same type variable are references to the same AnnotatedTypeMirror.
     */
    private static class InitializerVisitor implements AnnotatedTypeVisitor<Void, Void> {
        /**
         * The BoundStructure starting from the first wildcard or type variable bound initialization
         * that kicked this visitation off
         */
        private final BoundStructure topLevelStructure;

        private BoundStructure currentStructure = null;

        private final Map<TypeVariable, TypeVariableStructure> typeVarToStructure = new HashMap<>();
        // private final Map<TypeVariable, TypeVariableRecord> typeVarToRecord = new HashMap<>();
        private final Map<WildcardType, AnnotatedWildcardType> wildcards = new HashMap<>();
        private final Map<IntersectionType, AnnotatedIntersectionType> intersections =
                new HashMap<>();
        // need current bound path

        public InitializerVisitor(final BoundStructure boundStructure) {
            this.topLevelStructure = boundStructure;
            this.currentStructure = boundStructure;
        }

        public InitializerVisitor(final TypeVariableStructure typeVarStruct) {
            this((BoundStructure) typeVarStruct);
            typeVarToStructure.put(typeVarStruct.typeVar, typeVarStruct);
        }

        //--------------------------------------------------------------------------------------------------------------
        // Visit methods that keep track of the path traversed through type variable bounds, and the
        // wildcards/intersections that have been encountered.
        //--------------------------------------------------------------------------------------------------------------

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
            return null;
        }

        @Override
        public Void visitIntersection(AnnotatedIntersectionType type, Void aVoid) {

            if (intersections.containsKey(type.getUnderlyingType())) {
                return null;
            }

            intersections.put((IntersectionType) type.getUnderlyingType(), type);

            final List<AnnotatedDeclaredType> supertypes = type.directSuperTypes();
            for (int i = 0; i < supertypes.size(); i++) {
                final AnnotatedDeclaredType supertype = supertypes.get(i);
                final BoundPathNode node = addPathNode(new IntersectionNode(i));
                visit(supertype);
                removePathNode(node);
            }
            return null;
        }

        @Override
        public Void visitUnion(AnnotatedUnionType type, Void aVoid) {

            final List<AnnotatedDeclaredType> alts = type.getAlternatives();
            for (int i = 0; i < alts.size(); i++) {
                final AnnotatedDeclaredType alt = alts.get(i);
                final BoundPathNode node = addPathNode(new UnionNode(i));
                visit(alt);
                removePathNode(node);
            }
            return null;
        }

        @Override
        public Void visitArray(AnnotatedArrayType type, Void aVoid) {
            if (!TypesUtils.isPrimitive(type.getComponentType().getUnderlyingType())) {
                // Only recur on component type if it's not a primitive.
                // Array component types are the only place a primitive is allowed in bounds
                final BoundPathNode componentNode = addPathNode(new ArrayComponentNode());
                type.setComponentType(replaceOrVisit(type.getComponentType()));
                removePathNode(componentNode);
            }
            return null;
        }

        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, Void aVoid) {
            this.currentStructure.addTypeVar(type.getUnderlyingType());

            if (!haveSeenTypeVar(type)) {
                pushNewTypeVarStruct(type);
                initializeUpperBound(type);
                initializeLowerBound(type);
                popCurrentTypeVarStruct(type);
            }

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
                ErrorReporter.errorAbort(
                        "Wildcard super field should not be initialized:\n"
                                + "wildcard="
                                + wildcard.toString()
                                + "currentPath="
                                + currentStructure.currentPath);
            }

            if (wildcard.getExtendsBoundField() == null) {
                initializeExtendsBound(wildcard);
            } else {
                ErrorReporter.errorAbort(
                        "Wildcard extends field should not be initialized:\n"
                                + "wildcard="
                                + wildcard.toString()
                                + "currentPath="
                                + currentStructure.currentPath);
            }

            return null;
        }

        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType type, Void aVoid) {
            return invalidType(type);
        }

        @Override
        public Void visitNoType(AnnotatedNoType type, Void aVoid) {
            return invalidType(type);
        }

        @Override
        public Void visitExecutable(AnnotatedExecutableType type, Void aVoid) {
            return invalidType(type);
        }

        public AnnotatedTypeMirror replaceOrVisit(final AnnotatedTypeMirror type) {
            if (type.getKind() == TypeKind.WILDCARD) {
                final AnnotatedWildcardType wildcard = (AnnotatedWildcardType) type;
                if (wildcards.containsKey(wildcard.getUnderlyingType())) {
                    return wildcards.get(wildcard.getUnderlyingType());

                } else {

                    visit(wildcard);
                }

                return wildcard;

            } else if (type.getKind() == TypeKind.INTERSECTION) {
                if (intersections.containsKey(type.getUnderlyingType())) {
                    return intersections.get(type.getUnderlyingType());
                }

                visit(type);
                return type;

            } else {
                visit(type);
                return type;
            }
        }

        //--------------------------------------------------------------------------------------------------------------
        //
        public void initializeUpperBound(final AnnotatedTypeVariable typeVar) {
            final AnnotatedTypeMirror upperBound = createAndSetUpperBound(typeVar);

            final BoundPathNode pathNode = new UpperBoundNode();
            addPathNode(pathNode);
            visit(upperBound);
            removePathNode(pathNode);
        }

        public void initializeLowerBound(final AnnotatedTypeVariable typeVar) {
            final AnnotatedTypeMirror lowerBound = createAndSetLowerBound(typeVar);

            final BoundPathNode pathNode = new LowerBoundNode();
            addPathNode(pathNode);
            visit(lowerBound);
            removePathNode(pathNode);
        }

        public void initializeSuperBound(final AnnotatedWildcardType wildcard) {
            final AnnotatedTypeFactory typeFactory = wildcard.atypeFactory;

            final WildcardType underlyingType = wildcard.getUnderlyingType();
            TypeMirror underlyingSuperBound = underlyingType.getSuperBound();
            if (underlyingSuperBound == null) {
                underlyingSuperBound =
                        TypesUtils.wildLowerBound(
                                wildcard.atypeFactory.processingEnv, underlyingType);
            }

            final AnnotatedTypeMirror superBound =
                    AnnotatedTypeMirror.createType(underlyingSuperBound, typeFactory, false);
            wildcard.setSuperBound(superBound);

            this.wildcards.put(wildcard.getUnderlyingType(), wildcard);

            final BoundPathNode superNode = addPathNode(new SuperNode());
            visit(superBound);
            removePathNode(superNode);
        }

        public void initializeExtendsBound(final AnnotatedWildcardType wildcard) {
            final AnnotatedTypeFactory typeFactory = wildcard.atypeFactory;

            final WildcardType underlyingType = wildcard.getUnderlyingType();
            TypeMirror underlyingExtendsBound = underlyingType.getExtendsBound();
            if (underlyingExtendsBound == null) {
                //TODO: AnnotatedTypeFactory#widenToUpperBound) and
                // SupertypeFinder#fixWildcardBound have similar logic for handling unbounded wildcards.
                // Merging those methods and this into AnnotatedWildcardType would improve the code greatly and
                // still be easier than implementing all of capture conversion
                // Take the upper bound of the type variable the wildcard is bound to.
                underlyingExtendsBound =
                        TypesUtils.wildUpperBound(
                                wildcard.atypeFactory.processingEnv, underlyingType);
            }

            final AnnotatedTypeMirror extendsBound =
                    AnnotatedTypeMirror.createType(underlyingExtendsBound, typeFactory, false);
            wildcard.setExtendsBound(extendsBound);

            this.wildcards.put(wildcard.getUnderlyingType(), wildcard);

            final BoundPathNode extendsNode = addPathNode(new ExtendsNode());
            visit(extendsBound);
            removePathNode(extendsNode);
        }

        private void initializeTypeArgs(final AnnotatedDeclaredType declaredType) {
            if (declaredType.typeArgs == null) {
                final DeclaredType actualType = (DeclaredType) declaredType.actualType;
                final List<AnnotatedTypeMirror> typeArgs = new ArrayList<>();
                if (!actualType.getTypeArguments().isEmpty()) { // lazy init
                    final List<? extends TypeMirror> actualTypeArgs = actualType.getTypeArguments();
                    for (int i = 0; i < actualTypeArgs.size(); i++) {
                        final AnnotatedTypeMirror annoTypeArg =
                                AnnotatedTypeMirror.createType(
                                        actualTypeArgs.get(i), declaredType.atypeFactory, false);

                        final BoundPathNode node = addPathNode(new TypeArgNode(i));
                        typeArgs.add(replaceOrVisit(annoTypeArg));
                        removePathNode(node);
                    }
                }
                declaredType.setTypeArguments(typeArgs);
            } else {

                final List<AnnotatedTypeMirror> typeArgs =
                        new ArrayList<>(declaredType.getTypeArguments());
                final List<AnnotatedTypeMirror> typeArgReplacements =
                        new ArrayList<>(typeArgs.size());
                for (int i = 0; i < typeArgs.size(); i++) {
                    final AnnotatedTypeMirror typeArg = typeArgs.get(i);
                    final BoundPathNode node = addPathNode(new TypeArgNode(i));
                    typeArgReplacements.add(replaceOrVisit(typeArg));
                    removePathNode(node);
                }

                declaredType.setTypeArguments(typeArgReplacements);
            }
        }

        public static Void invalidType(final AnnotatedTypeMirror atm) {
            ErrorReporter.errorAbort(
                    "Unexpected type in Wildcard bound:\n"
                            + "kind="
                            + atm.getKind()
                            + "\n"
                            + "atm="
                            + atm);
            return null; // dead code
        }

        public BoundPathNode addPathNode(final BoundPathNode node) {
            currentStructure.currentPath.add(node);
            return node;
        }

        public BoundPathNode removePathNode(final BoundPathNode node) {
            if (currentStructure.currentPath.getLast() != node) {
                ErrorReporter.errorAbort(
                        "Cannot remove node: "
                                + node
                                + " It is not the last item.\n"
                                + "node="
                                + node
                                + "\n"
                                + "currentPath="
                                + currentStructure.currentPath);
            } // else

            currentStructure.currentPath.removeLast();
            return node;
        }

        public void pushNewTypeVarStruct(final AnnotatedTypeVariable typeVar) {
            if (typeVarToStructure.containsKey(typeVar.getUnderlyingType())) {
                ErrorReporter.errorAbort(
                        "Starting a TypeVarStructure that already exists!\n"
                                + "typeVar="
                                + typeVar
                                + "\n"
                                + "currentStructure="
                                + currentStructure);
            }

            final TypeVariableStructure typeVarStruct =
                    new TypeVariableStructure(currentStructure, typeVar);
            typeVarToStructure.put(typeVar.getUnderlyingType(), typeVarStruct);

            this.currentStructure = typeVarStruct;
        }

        public boolean haveSeenTypeVar(final AnnotatedTypeVariable typeVariable) {
            return typeVarToStructure.containsKey(typeVariable.getUnderlyingType());
        }

        public void popCurrentTypeVarStruct(final AnnotatedTypeVariable typeVar) {
            if (!(this.currentStructure instanceof TypeVariableStructure)) {
                ErrorReporter.errorAbort(
                        "Trying to pop WildcardStructure!\n"
                                + "typeVar="
                                + typeVar
                                + "\n"
                                + "currentStucture="
                                + currentStructure
                                + "\n");
            } // else

            final TypeVariableStructure toPop = (TypeVariableStructure) this.currentStructure;
            if (toPop.typeVar != typeVar) {
                this.currentStructure = toPop.parent;
            }
        }

        public ReferenceMap createReferenceMap(final BoundStructure boundStruct) {
            final ReferenceMap refMap = new ReferenceMap();

            for (Entry<BoundPath, TypeVariable> entry : boundStruct.pathToTypeVar.entrySet()) {
                TypeVariableStructure targetStructure = typeVarToStructure.get(entry.getValue());

                AnnotatedTypeVariable template = targetStructure.annotatedTypeVar;
                refMap.put(entry.getKey(), template.deepCopy().asUse());

                addImmediateTypeVarPaths(refMap, entry.getKey(), targetStructure);
            }

            return refMap;
        }

        public void addImmediateTypeVarPaths(
                ReferenceMap refMap, BoundPath basePath, TypeVariableStructure targetStruct) {

            // explain typevar sleds
            for (BoundPath path : targetStruct.immediateBoundTypeVars) {
                final BoundPath newPath = basePath.copy();
                newPath.add(path.getFirst());

                TypeVariable immTypeVar = targetStruct.pathToTypeVar.get(path);
                TypeVariableStructure immTvStructure = typeVarToStructure.get(immTypeVar);

                AnnotatedTypeVariable template = immTvStructure.annotatedTypeVar;
                refMap.put(newPath, template.deepCopy());
            }
        }

        /**
         * A mapping of paths to the type that should be placed at the end of that path for all atvs
         * that of sourceType
         */
        @SuppressWarnings("serial")
        private static class ReferenceMap extends LinkedHashMap<BoundPath, AnnotatedTypeVariable> {
            //TODO: EXPLAINED LINK DUE TO TYPEVAR SLED
        }

        public void resolveTypeVarReferences(final AnnotatedTypeMirror boundedType) {
            final List<AnnotatedTypeVariable> annotatedTypeVars = new ArrayList<>();
            final Map<TypeVariable, ReferenceMap> typeVarToRefMap = new HashMap<>();

            for (final TypeVariableStructure typeVarStruct : typeVarToStructure.values()) {
                ReferenceMap refMap = createReferenceMap(typeVarStruct);
                typeVarToRefMap.put(typeVarStruct.typeVar, refMap);
                annotatedTypeVars.addAll(refMap.values());
            }

            for (final AnnotatedTypeVariable atv : annotatedTypeVars) {
                fixTypeVarPathReference(atv, typeVarToRefMap);
            }

            if (topLevelStructure instanceof WildcardStructure) {
                fixWildcardPathReference((AnnotatedWildcardType) boundedType, typeVarToRefMap);

            } else {
                final AnnotatedTypeVariable typeVar = (AnnotatedTypeVariable) boundedType;
                fixTypeVarPathReference(typeVar, typeVarToRefMap);
            }
        }

        public void fixWildcardPathReference(
                final AnnotatedWildcardType wildcard,
                final Map<TypeVariable, ReferenceMap> typeVarToRefMap) {

            final ReferenceMap topLevelMap = createReferenceMap(topLevelStructure);
            for (AnnotatedTypeVariable typeVar : topLevelMap.values()) {
                fixTypeVarPathReference(typeVar, typeVarToRefMap);
            }

            for (Entry<BoundPath, AnnotatedTypeVariable> pathToRef : topLevelMap.entrySet()) {
                final AnnotatedTypeMirror parent = traverseToParent(wildcard, pathToRef.getKey());
                final BoundPathNode terminus = pathToRef.getKey().getLast();
                terminus.setType(parent, pathToRef.getValue());
            }
        }

        public void fixTypeVarPathReference(
                final AnnotatedTypeVariable type, Map<TypeVariable, ReferenceMap> typeVarToRefMap) {
            final ReferenceMap refMap = typeVarToRefMap.get(type.getUnderlyingType());

            for (final Entry<BoundPath, AnnotatedTypeVariable> pathToRef : refMap.entrySet()) {
                final BoundPath path = pathToRef.getKey();
                final AnnotatedTypeVariable replacement = pathToRef.getValue().asUse();

                AnnotatedTypeMirror parent = traverseToParent(type, path);
                BoundPathNode terminus = path.getLast();

                terminus.replaceType(parent, replacement);
            }
        }

        public AnnotatedTypeMirror traverseToParent(
                final AnnotatedTypeMirror start, final List<BoundPathNode> path) {
            AnnotatedTypeMirror current = start;
            for (int i = 0; i < path.size() - 1; i++) {
                current = path.get(i).next(current);
            }

            return current;
        }
    }

    private static AnnotatedTypeMirror createAndSetUpperBound(final AnnotatedTypeVariable typeVar) {

        final AnnotatedTypeMirror upperBound =
                AnnotatedTypeMirror.createType(
                        typeVar.getUnderlyingType().getUpperBound(), typeVar.atypeFactory, false);
        typeVar.setUpperBound(upperBound);
        return upperBound;
    }

    private static AnnotatedTypeMirror createAndSetLowerBound(final AnnotatedTypeVariable typeVar) {
        TypeMirror lb = typeVar.getUnderlyingType().getLowerBound();
        if (lb == null) {
            // Use bottom type to ensure there is a lower bound.
            Context context =
                    ((JavacProcessingEnvironment) typeVar.atypeFactory.processingEnv).getContext();
            Symtab syms = Symtab.instance(context);
            lb = syms.botType;
        }
        final AnnotatedTypeMirror lowerBound =
                AnnotatedTypeMirror.createType(lb, typeVar.atypeFactory, false);
        typeVar.setLowerBound(lowerBound);
        return lowerBound;
    }

    private static boolean isImmediateBoundPath(final BoundPath path) {
        if (path.size() == 1) {
            switch (path.getFirst().kind) {
                case UpperBound:
                case LowerBound:
                    return true;

                default:
                    // do nothing
            }
        }

        return false;
    }

    private abstract static class BoundStructure {

        /**
         * A mapping of all BoundPaths to TypeVariables for all type variables contained within
         * annotatedTypeVar
         */
        public final Map<BoundPath, TypeVariable> pathToTypeVar = new LinkedHashMap<>();

        public final BoundPath currentPath = new BoundPath();

        public BoundStructure() {}

        public void addTypeVar(final TypeVariable typeVariable) {
            pathToTypeVar.put(this.currentPath.copy(), typeVariable);
        }
    }

    private static class WildcardStructure extends BoundStructure {}

    private static class TypeVariableStructure extends BoundStructure {
        /** The type variable whose structure is being described */
        public final TypeVariable typeVar;

        /**
         * The first annotated type variable that was encountered and traversed in order to describe
         * typeVar. It is expanded during visitation and it is later used as a template for other
         * uses of typeVar
         */
        public final AnnotatedTypeVariable annotatedTypeVar;

        /** The boundStructure that was active before this one */
        private final BoundStructure parent;

        /**
         * If this type variable is upper or lower bounded by another type variable (not a declared
         * type or intersection) then this variable will contain the path to that type variable
         * //TODO: Add link to explanation
         *
         * <p>e.g. {@code T extends E} &rArr; The structure for T will have an
         * immediateBoundTypeVars = List(UpperBound) The BoundPaths here must exist in pathToTypeVar
         */
        public Set<BoundPath> immediateBoundTypeVars = new LinkedHashSet<>();

        public TypeVariableStructure(
                final BoundStructure parent, final AnnotatedTypeVariable annotatedTypeVar) {
            this.parent = parent;
            this.typeVar = annotatedTypeVar.getUnderlyingType();
            this.annotatedTypeVar = annotatedTypeVar;
        }

        @Override
        public void addTypeVar(TypeVariable typeVariable) {
            final BoundPath copy = currentPath.copy();
            pathToTypeVar.put(copy, typeVariable);

            if (isImmediateBoundPath(copy)) {
                immediateBoundTypeVars.add(copy);
            }
        }
    }

    /** An array list of BoundPathNodes whose equals method is a referential equality check */
    @SuppressWarnings("serial")
    private static class BoundPath extends LinkedList<BoundPathNode> {

        @Override
        public boolean equals(final Object obj) {
            return this == obj;
        }

        @Override
        public String toString() {
            return PluginUtil.join(",", this);
        }

        public BoundPath copy() {
            final BoundPath copy = new BoundPath();
            for (final BoundPathNode node : this) {
                copy.add(node.copy());
            }

            return copy;
        }
    }

    // BoundPathNode's are a step in a "type path" that are used to
    private abstract static class BoundPathNode {
        enum Kind {
            Extends,
            Super,
            UpperBound,
            LowerBound,
            ArrayComponent,
            Intersection,
            Union,
            TypeArg
        }

        public Kind kind;
        public TypeKind typeKind;

        BoundPathNode() {}

        BoundPathNode(final BoundPathNode template) {
            this.kind = template.kind;
            this.typeKind = template.typeKind;
        }

        @Override
        public String toString() {
            return kind.toString();
        }

        public AnnotatedTypeMirror next(final AnnotatedTypeMirror parent) {
            abortIfParentNotKind(typeKind, null, parent);
            return getType(parent);
        }

        public void replaceType(
                final AnnotatedTypeMirror parent, final AnnotatedTypeVariable replacement) {
            abortIfParentNotKind(typeKind, replacement, parent);
            setType(parent, replacement);
        }

        public abstract void setType(
                final AnnotatedTypeMirror parent, final AnnotatedTypeVariable replacement);

        public abstract AnnotatedTypeMirror getType(final AnnotatedTypeMirror parent);

        public abstract BoundPathNode copy();
    }

    private static class ExtendsNode extends BoundPathNode {

        ExtendsNode() {
            kind = Kind.Extends;
            typeKind = TypeKind.WILDCARD;
        }

        ExtendsNode(ExtendsNode template) {
            super(template);
        }

        @Override
        public void setType(AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            ((AnnotatedWildcardType) parent).setExtendsBound(replacement);
        }

        @Override
        public AnnotatedTypeMirror getType(final AnnotatedTypeMirror parent) {
            return ((AnnotatedWildcardType) parent).getExtendsBound();
        }

        @Override
        public BoundPathNode copy() {
            return new ExtendsNode(this);
        }
    }

    private static class SuperNode extends BoundPathNode {

        SuperNode() {
            kind = Kind.Super;
            typeKind = TypeKind.WILDCARD;
        }

        SuperNode(SuperNode template) {
            super(template);
        }

        @Override
        public void setType(AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            ((AnnotatedWildcardType) parent).setSuperBound(replacement);
        }

        @Override
        public AnnotatedTypeMirror getType(final AnnotatedTypeMirror parent) {
            return ((AnnotatedWildcardType) parent).getSuperBound();
        }

        @Override
        public BoundPathNode copy() {
            return new SuperNode(this);
        }
    }

    private static class UpperBoundNode extends BoundPathNode {

        UpperBoundNode() {
            kind = Kind.UpperBound;
            typeKind = TypeKind.TYPEVAR;
        }

        UpperBoundNode(UpperBoundNode template) {
            super(template);
        }

        @Override
        public void setType(AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            ((AnnotatedTypeVariable) parent).setUpperBound(replacement);
        }

        @Override
        public AnnotatedTypeMirror getType(final AnnotatedTypeMirror parent) {
            final AnnotatedTypeVariable parentAtv = (AnnotatedTypeVariable) parent;
            if (parentAtv.getUpperBoundField() != null) {
                return parentAtv.getUpperBoundField();
            }
            return createAndSetUpperBound((AnnotatedTypeVariable) parent);
        }

        @Override
        public BoundPathNode copy() {
            return new UpperBoundNode(this);
        }
    }

    private static class LowerBoundNode extends BoundPathNode {
        LowerBoundNode() {
            kind = Kind.LowerBound;
            typeKind = TypeKind.TYPEVAR;
        }

        LowerBoundNode(LowerBoundNode template) {
            super(template);
        }

        @Override
        public void setType(AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            ((AnnotatedTypeVariable) parent).setLowerBound(replacement);
        }

        @Override
        public AnnotatedTypeMirror getType(final AnnotatedTypeMirror parent) {

            final AnnotatedTypeVariable parentAtv = (AnnotatedTypeVariable) parent;
            if (parentAtv.getLowerBoundField() != null) {
                return parentAtv.getLowerBoundField();
            }
            //else //TODO: I think this should never happen at this point, throw exception
            return createAndSetLowerBound((AnnotatedTypeVariable) parent);
        }

        @Override
        public BoundPathNode copy() {
            return new LowerBoundNode(this);
        }
    }

    private static class ArrayComponentNode extends BoundPathNode {

        ArrayComponentNode() {
            kind = Kind.ArrayComponent;
            typeKind = TypeKind.ARRAY;
        }

        ArrayComponentNode(ArrayComponentNode template) {
            super(template);
        }

        @Override
        public void setType(AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            ((AnnotatedArrayType) parent).setComponentType(replacement);
        }

        @Override
        public AnnotatedTypeMirror getType(final AnnotatedTypeMirror parent) {
            return ((AnnotatedArrayType) parent).getComponentType();
        }

        @Override
        public BoundPathNode copy() {
            return new ArrayComponentNode(this);
        }
    }

    private static class IntersectionNode extends BoundPathNode {
        public final int superIndex;

        IntersectionNode(int superIndex) {
            this.superIndex = superIndex;
            kind = Kind.Intersection;
            typeKind = TypeKind.INTERSECTION;
        }

        IntersectionNode(IntersectionNode template) {
            super(template);
            superIndex = template.superIndex;
        }

        @Override
        public String toString() {
            return super.toString() + "( superIndex=" + superIndex + " )";
        }

        @Override
        public void setType(AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            ErrorReporter.errorAbort(
                    "Type variables cannot be intersection bounds!\n"
                            + "parent="
                            + parent
                            + "\n"
                            + "replacement="
                            + replacement);
        }

        @Override
        public AnnotatedTypeMirror getType(final AnnotatedTypeMirror parent) {
            final AnnotatedIntersectionType isect = (AnnotatedIntersectionType) parent;
            if (parent.directSuperTypes().size() <= superIndex) {
                ErrorReporter.errorAbort(
                        "Invalid superIndex( " + superIndex + " ):\n" + "parent=" + parent);
            }

            return isect.directSuperTypes().get(superIndex);
        }

        @Override
        public BoundPathNode copy() {
            return new IntersectionNode(this);
        }
    }

    private static class UnionNode extends BoundPathNode {
        public final int altIndex;

        UnionNode(int altIndex) {
            this.altIndex = altIndex;
            kind = Kind.Union;
            typeKind = TypeKind.UNION;
        }

        UnionNode(UnionNode template) {
            super(template);
            altIndex = template.altIndex;
        }

        @Override
        public String toString() {
            return super.toString() + "( altIndex=" + altIndex + " )";
        }

        @Override
        public void setType(AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            ErrorReporter.errorAbort(
                    "Union types cannot be intersection bounds!\n"
                            + "parent="
                            + parent
                            + "\n"
                            + "replacement="
                            + replacement);
        }

        @Override
        public AnnotatedTypeMirror getType(final AnnotatedTypeMirror parent) {
            final AnnotatedUnionType isect = (AnnotatedUnionType) parent;
            if (parent.directSuperTypes().size() <= altIndex) {
                ErrorReporter.errorAbort(
                        "Invalid altIndex( " + altIndex + " ):\n" + "parent=" + parent);
            }

            return isect.directSuperTypes().get(altIndex);
        }

        @Override
        public BoundPathNode copy() {
            return new UnionNode(this);
        }
    }

    private static class TypeArgNode extends BoundPathNode {
        public final int argIndex;

        TypeArgNode(int argIndex) {
            this.argIndex = argIndex;
            kind = Kind.TypeArg;
            typeKind = TypeKind.DECLARED;
        }

        TypeArgNode(TypeArgNode template) {
            super(template);
            argIndex = template.argIndex;
        }

        @Override
        public String toString() {
            return super.toString() + "( argIndex=" + argIndex + " )";
        }

        @Override
        public void setType(AnnotatedTypeMirror parent, AnnotatedTypeVariable replacement) {
            abortIfParentNotKind(TypeKind.DECLARED, replacement, parent);
            final AnnotatedDeclaredType parentAdt = (AnnotatedDeclaredType) parent;

            List<AnnotatedTypeMirror> typeArgs = new ArrayList<>(parentAdt.getTypeArguments());
            if (argIndex >= typeArgs.size()) {
                ErrorReporter.errorAbort(
                        "Invalid type arg index!\n"
                                + "parent="
                                + parent
                                + "\n"
                                + "replacement="
                                + replacement
                                + "\n"
                                + "argIndex="
                                + argIndex);
            }
            typeArgs.add(argIndex, replacement);
            typeArgs.remove(argIndex + 1);
            parentAdt.setTypeArguments(typeArgs);
        }

        @Override
        public AnnotatedTypeMirror getType(final AnnotatedTypeMirror parent) {
            final AnnotatedDeclaredType parentAdt = (AnnotatedDeclaredType) parent;

            List<AnnotatedTypeMirror> typeArgs = parentAdt.getTypeArguments();
            if (argIndex >= typeArgs.size()) {
                ErrorReporter.errorAbort(
                        "Invalid type arg index!\n"
                                + "parent="
                                + parent
                                + "\n"
                                + "argIndex="
                                + argIndex);
            }

            return typeArgs.get(argIndex);
        }

        @Override
        public BoundPathNode copy() {
            return new TypeArgNode(this);
        }
    }

    public static void abortIfParentNotKind(
            final TypeKind typeKind,
            final AnnotatedTypeVariable type,
            final AnnotatedTypeMirror parent) {
        if (parent.getKind().equals(typeKind)) {
            return;
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("Unexpected parent kind:\n");
        builder.append("parent=");
        builder.append(parent);
        builder.append("\n");

        builder.append("replacement=");
        builder.append(type);
        builder.append("\n");

        builder.append("expected=");
        builder.append(typeKind);
        builder.append("\n");

        ErrorReporter.errorAbort(builder.toString());
    }
}
