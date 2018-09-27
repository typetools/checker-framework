package org.checkerframework.framework.type.poly;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.EquivalentAtmComboScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Implements framework support for qualifier polymorphism.
 *
 * <p>{@link DefaultQualifierPolymorphism} implements the abstract methods in this class. Subclasses
 * can alter the way instantiations of polymorphic qualifiers are combined.
 */
public abstract class AbstractQualifierPolymorphism implements QualifierPolymorphism {

    /** Annotated type factory */
    protected final AnnotatedTypeFactory atypeFactory;

    /**
     * The polymorphic qualifiers: mapping from a polymorphic qualifier of a qualifier hierarchy to
     * the top qualifier of that hierarchy. Field always non-null but might be an empty mapping.
     */
    protected final AnnotationMirrorMap<AnnotationMirror> polyQuals;

    /** The qualifiers at the top of the qualifier hierarchy. */
    protected final AnnotationMirrorSet topQuals;

    /** The qualifier hierarchy to use. */
    protected final QualifierHierarchy qualhierarchy;

    /** {@link PolyAll} annotation mirror. */
    protected final AnnotationMirror POLYALL;

    /** Determines the instantiations for each polymorphic qualifier. */
    private PolyCollector collector;

    /**
     * Completes a type by removing any unresolved polymorphic qualifiers, replacing them with the
     * top qualifiers.
     */
    private Completer completer;

    /** Replaces each polymorphic qualifier with its instantiation. */
    private AnnotatedTypeScanner<Void, AnnotationMirrorMap<AnnotationMirrorSet>> replacer;

    /**
     * Creates an {@link AbstractQualifierPolymorphism} instance that uses the given checker for
     * querying type qualifiers and the given factory for getting annotated types. Subclasses need
     * to add polymorphic qualifiers to {@code polyQuals}.
     *
     * @param env the processing environment
     * @param factory the factory for the current checker
     */
    public AbstractQualifierPolymorphism(ProcessingEnvironment env, AnnotatedTypeFactory factory) {
        this.atypeFactory = factory;
        this.qualhierarchy = factory.getQualifierHierarchy();
        this.topQuals = new AnnotationMirrorSet(qualhierarchy.getTopAnnotations());

        Elements elements = env.getElementUtils();
        this.POLYALL = AnnotationBuilder.fromClass(elements, PolyAll.class);

        this.polyQuals = new AnnotationMirrorMap<>();

        this.collector = new PolyCollector();
        this.completer = new Completer();
        this.replacer = new Replacer();
    }

    /**
     * Reset to allow reuse of the same instance. Subclasses should override this method to clear
     * their additional state; they must call the super implementation.
     */
    protected void reset() {
        completer.reset();
        replacer.reset();
        collector.reset();
    }

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param tree the tree associated with the type
     * @param type the type to annotate
     */
    @Override
    public void annotate(MethodInvocationTree tree, AnnotatedExecutableType type) {
        if (polyQuals.isEmpty()) {
            return;
        }

        // javac produces enum super calls with zero arguments even though the
        // method element requires two.
        // See also BaseTypeVisitor.visitMethodInvocation and
        // CFGBuilder.CFGTranslationPhaseOne.visitMethodInvocation
        if (TreeUtils.isEnumSuper(tree)) {
            return;
        }
        List<AnnotatedTypeMirror> parameters =
                AnnotatedTypes.expandVarArgs(atypeFactory, type, tree.getArguments());
        List<AnnotatedTypeMirror> arguments =
                AnnotatedTypes.getAnnotatedTypes(atypeFactory, parameters, tree.getArguments());

        AnnotationMirrorMap<AnnotationMirrorSet> matchingMapping =
                collector.visit(arguments, parameters);

        // for super() and this() method calls, getReceiverType(tree) does not return the correct
        // type. So, just skip those.  This is consistent with skipping receivers of constructors
        // below.
        if (type.getReceiverType() != null
                && !TreeUtils.isSuperCall(tree)
                && !TreeUtils.isThisCall(tree)) {
            matchingMapping =
                    collector.reduce(
                            matchingMapping,
                            collector.visit(
                                    atypeFactory.getReceiverType(tree), type.getReceiverType()));
        }

        if (matchingMapping != null && !matchingMapping.isEmpty()) {
            replacer.visit(type, matchingMapping);
        } else {
            completer.visit(type);
        }
        reset();
    }

    @Override
    public void annotate(NewClassTree tree, AnnotatedExecutableType type) {
        if (polyQuals.isEmpty()) {
            return;
        }
        List<AnnotatedTypeMirror> requiredArgs =
                AnnotatedTypes.expandVarArgs(atypeFactory, type, tree.getArguments());
        List<AnnotatedTypeMirror> arguments =
                AnnotatedTypes.getAnnotatedTypes(atypeFactory, requiredArgs, tree.getArguments());

        AnnotationMirrorMap<AnnotationMirrorSet> matchingMapping =
                collector.visit(arguments, requiredArgs);
        // TODO: poly on receiver for constructors?
        // matchingMapping = collector.reduce(matchingMapping,
        //        collector.visit(factory.getReceiverType(tree), type.getReceiverType()));

        if (matchingMapping != null && !matchingMapping.isEmpty()) {
            replacer.visit(type, matchingMapping);
        } else {
            completer.visit(type);
        }
        reset();
    }

    @Override
    public void annotate(
            AnnotatedExecutableType functionalInterface, AnnotatedExecutableType memberReference) {
        for (AnnotationMirror type : functionalInterface.getReturnType().getAnnotations()) {
            if (QualifierPolymorphism.isPolymorphicQualified(type)) {
                // functional interface has a polymorphic qualifier, so they should not be resolved
                // on memberReference.
                return;
            }
        }

        List<AnnotatedTypeMirror> args = functionalInterface.getParameterTypes();
        List<AnnotatedTypeMirror> requiredArgs = memberReference.getParameterTypes();
        if (args.size() == requiredArgs.size() + 1) {
            // If the member reference is a reference to an instance method of an arbitrary
            // object, then first parameter of the functional interface corresponds to the
            // receiver of the member reference.
            List<AnnotatedTypeMirror> newRequiredArgs = new ArrayList<>();
            newRequiredArgs.add(memberReference.getReceiverType());
            newRequiredArgs.addAll(requiredArgs);
            requiredArgs = newRequiredArgs;
        }
        // Deal with varargs
        if (memberReference.isVarArgs() && !functionalInterface.isVarArgs()) {
            requiredArgs = AnnotatedTypes.expandVarArgsFromTypes(memberReference, args);
        }

        AnnotationMirrorMap<AnnotationMirrorSet> matchingMapping =
                collector.visit(args, requiredArgs);

        if (matchingMapping != null && !matchingMapping.isEmpty()) {
            replacer.visit(memberReference, matchingMapping);
        } else {
            // TODO: Do we need this (return type?)
            completer.visit(memberReference);
        }
        reset();
    }

    /**
     * Returns an annotation set that is the merge of the two sets of annotations. The sets are
     * instantiations for {@code polyQual}.
     *
     * @param polyQual polymorphic qualifier for which {@code a1Annos} and {@code a2Annos} are
     *     instantiations
     * @param a1Annos a set that is an instantiation of {@code polyQual}
     * @param a2Annos a set that is an instantiation of {@code polyQual}
     * @return the merge of the two sets
     */
    protected abstract AnnotationMirrorSet combine(
            AnnotationMirror polyQual, AnnotationMirrorSet a1Annos, AnnotationMirrorSet a2Annos);

    /**
     * Replaces the top-level polymorphic annotations in {@code type} with the instantiations in
     * {@code matches}.
     *
     * <p>This method is called on all parts of a type.
     *
     * @param type AnnotationTypeMirror whose poly annotations are replaced
     * @param replacements mapping from polymorphic annotation to instantiation
     */
    protected abstract void replace(
            AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirrorSet> replacements);

    /** Replaces each polymorphic qualifier with its instantiation. */
    class Replacer extends AnnotatedTypeScanner<Void, AnnotationMirrorMap<AnnotationMirrorSet>> {
        @Override
        public Void scan(
                AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirrorSet> replacements) {
            replace(type, replacements);
            return super.scan(type, replacements);
        }
    }

    /**
     * Completes a type by removing any unresolved polymorphic qualifiers, replacing them with the
     * top qualifiers.
     */
    class Completer extends AnnotatedTypeScanner<Void, Void> {
        @Override
        protected Void scan(AnnotatedTypeMirror type, Void p) {
            for (Map.Entry<AnnotationMirror, AnnotationMirror> pqentry : polyQuals.entrySet()) {
                AnnotationMirror top = pqentry.getValue();
                AnnotationMirror poly = pqentry.getKey();

                if (type.hasAnnotation(poly)) {
                    type.removeAnnotation(poly);
                    if (top == null) {
                        // poly is PolyAll -> add all tops not explicitly given
                        type.addMissingAnnotations(topQuals);
                    } else if (type.getKind() != TypeKind.TYPEVAR
                            && type.getKind() != TypeKind.WILDCARD) {
                        // Do not add the top qualifiers to type variables and wildcards
                        type.addAnnotation(top);
                    }
                }
            }
            return super.scan(type, p);
        }
    }

    /**
     * A Helper class that tries to resolve the polymorphic qualifiers with the most restricted
     * qualifier. The mapping is from the polymorphic qualifier to the substitution for that
     * qualifier, which is a set of qualifiers. For most polymorphic qualifiers this will be a
     * singleton set. For the @PolyAll qualifier, this might be a set of qualifiers.
     */
    private class PolyCollector
            extends EquivalentAtmComboScanner<AnnotationMirrorMap<AnnotationMirrorSet>, Void> {

        /**
         * List of {@link AnnotatedTypeVariable} or {@link AnnotatedWildcardType} that have been
         * visited. Call {@link #visited(AnnotatedTypeMirror)} to check if the type have been
         * visited, so that reference equality is used rather than {@link #equals(Object)}.
         */
        private final List<AnnotatedTypeMirror> visitedType2 = new ArrayList<>();

        /**
         * Returns true if the {@link AnnotatedTypeMirror} has been visited. If it has not, then it
         * is added to the list of visited AnnotatedTypeMirrors. This prevents infinite recursion on
         * recursive types.
         */
        private boolean visited(AnnotatedTypeMirror atm) {
            for (AnnotatedTypeMirror atmVisit : visitedType2) {
                // Use reference equality rather than equals because the visitor may visit two types
                // that are structurally equal, but not actually the same.  For example, the
                // wildcards in Pair<?,?> may be equal, but they both should be visited.
                if (atmVisit == atm) {
                    return true;
                }
            }
            visitedType2.add(atm);
            return false;
        }

        @Override
        protected AnnotationMirrorMap<AnnotationMirrorSet> scanWithNull(
                AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void aVoid) {
            return new AnnotationMirrorMap<>();
        }

        @Override
        public AnnotationMirrorMap<AnnotationMirrorSet> reduce(
                AnnotationMirrorMap<AnnotationMirrorSet> r1,
                AnnotationMirrorMap<AnnotationMirrorSet> r2) {

            if (r1 == null || r1.isEmpty()) {
                return r2;
            }
            if (r2 == null || r2.isEmpty()) {
                return r1;
            }

            AnnotationMirrorMap<AnnotationMirrorSet> res = new AnnotationMirrorMap<>();
            // Ensure that all qualifiers from r1 and r2 are visited.
            AnnotationMirrorSet r2remain = new AnnotationMirrorSet();
            r2remain.addAll(r2.keySet());
            for (Map.Entry<AnnotationMirror, AnnotationMirrorSet> entry : r1.entrySet()) {
                AnnotationMirror polyQual = entry.getKey();
                AnnotationMirrorSet a1Annos = entry.getValue();
                AnnotationMirrorSet a2Annos = r2.get(polyQual);
                if (a2Annos != null && !a2Annos.isEmpty()) {
                    r2remain.remove(polyQual);
                }
                res.put(polyQual, combine(polyQual, a1Annos, a2Annos));
            }
            for (AnnotationMirror key2 : r2remain) {
                res.put(key2, r2.get(key2));
            }
            return res;
        }

        /**
         * Calls {@link #visit(AnnotatedTypeMirror, AnnotatedTypeMirror)} for each type in types.
         */
        private AnnotationMirrorMap<AnnotationMirrorSet> visit(
                Iterable<? extends AnnotatedTypeMirror> types,
                Iterable<? extends AnnotatedTypeMirror> polyTypes) {
            AnnotationMirrorMap<AnnotationMirrorSet> result = new AnnotationMirrorMap<>();

            Iterator<? extends AnnotatedTypeMirror> itert = types.iterator();
            Iterator<? extends AnnotatedTypeMirror> itera = polyTypes.iterator();

            while (itert.hasNext() && itera.hasNext()) {
                AnnotatedTypeMirror type = itert.next();
                AnnotatedTypeMirror actualType = itera.next();
                result = reduce(result, visit(type, actualType));
            }
            return result;
        }

        /**
         * Creates a mapping of polymorphic qualifiers to their instantiations by visiting each
         * composite type in type.
         *
         * @param type AnnotateTypeMirror used to find instantiations
         * @param polyType AnnotatedTypeMirror that may have polymorphich qualifiers
         * @return a mapping of polymorphic qualifiers to their instantiations
         */
        private AnnotationMirrorMap<AnnotationMirrorSet> visit(
                AnnotatedTypeMirror type, AnnotatedTypeMirror polyType) {
            if (type.getKind() == TypeKind.NULL) {
                return mapQualifierToPoly(type, polyType);
            }

            if (type.getKind() == TypeKind.WILDCARD) {
                AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) type;
                if (wildcardType.isUninferredTypeArgument()) {
                    return mapQualifierToPoly(wildcardType.getExtendsBound(), polyType);
                }

                switch (polyType.getKind()) {
                    case WILDCARD:
                        AnnotatedTypeMirror asSuper =
                                AnnotatedTypes.asSuper(atypeFactory, wildcardType, polyType);
                        return visit(asSuper, polyType, null);
                    case TYPEVAR:
                        return mapQualifierToPoly(wildcardType.getExtendsBound(), polyType);
                    default:
                        return mapQualifierToPoly(wildcardType.getExtendsBound(), polyType);
                }
            }

            AnnotatedTypeMirror asSuper = AnnotatedTypes.asSuper(atypeFactory, type, polyType);

            return visit(asSuper, polyType, null);
        }

        /**
         * If the primary annotation of {@code actualType} is a polymorphic qualifier, then it is
         * mapped to the primary annotation of {@code type} and the map is returned. Otherwise, an
         * empty map is returned.
         */
        private AnnotationMirrorMap<AnnotationMirrorSet> mapQualifierToPoly(
                AnnotatedTypeMirror type, AnnotatedTypeMirror actualType) {
            AnnotationMirrorMap<AnnotationMirrorSet> result = new AnnotationMirrorMap<>();

            for (Map.Entry<AnnotationMirror, AnnotationMirror> kv : polyQuals.entrySet()) {
                AnnotationMirror top = kv.getValue();
                AnnotationMirror poly = kv.getKey();

                if (top == null && actualType.hasAnnotation(POLYALL)) {
                    // PolyAll qualifier
                    result.put(poly, new AnnotationMirrorSet(type.getAnnotations()));
                } else if (actualType.hasAnnotation(poly)) {
                    AnnotationMirror typeQual = type.getAnnotationInHierarchy(top);
                    if (typeQual != null) {
                        result.put(poly, AnnotationMirrorSet.singleElementSet(typeQual));
                    }
                }
            }
            return result;
        }

        @Override
        protected String defaultErrorMessage(
                AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void aVoid) {
            return String.format(
                    "AbstractQualifierPolymorphism: Unexpected combination: type1: %s (%s) type2: %s (%s).",
                    type1, type1.getKind(), type2, type2.getKind());
        }

        @Override
        public AnnotationMirrorMap<AnnotationMirrorSet> visitArray_Array(
                AnnotatedArrayType type1, AnnotatedArrayType type2, Void aVoid) {
            AnnotationMirrorMap<AnnotationMirrorSet> result = mapQualifierToPoly(type1, type2);
            return reduce(result, super.visitArray_Array(type1, type2, aVoid));
        }

        @Override
        public AnnotationMirrorMap<AnnotationMirrorSet> visitDeclared_Declared(
                AnnotatedDeclaredType type1, AnnotatedDeclaredType type2, Void aVoid) {
            // Don't call super because asSuper has to be called on each type argument.
            if (visited(type2)) {
                return new AnnotationMirrorMap<>();
            }

            AnnotationMirrorMap<AnnotationMirrorSet> result = mapQualifierToPoly(type1, type2);

            Iterator<AnnotatedTypeMirror> type2Args = type2.getTypeArguments().iterator();
            for (AnnotatedTypeMirror type1Arg : type1.getTypeArguments()) {
                AnnotatedTypeMirror type2Arg = type2Args.next();
                result = reduce(result, visit(type1Arg, type2Arg));
            }

            return result;
        }

        @Override
        public AnnotationMirrorMap<AnnotationMirrorSet> visitIntersection_Intersection(
                AnnotatedIntersectionType type1, AnnotatedIntersectionType type2, Void aVoid) {
            AnnotationMirrorMap<AnnotationMirrorSet> result = mapQualifierToPoly(type1, type2);
            return reduce(result, super.visitIntersection_Intersection(type1, type2, aVoid));
        }

        @Override
        public AnnotationMirrorMap<AnnotationMirrorSet> visitNull_Null(
                AnnotatedNullType type1, AnnotatedNullType type2, Void aVoid) {
            return mapQualifierToPoly(type1, type2);
        }

        @Override
        public AnnotationMirrorMap<AnnotationMirrorSet> visitPrimitive_Primitive(
                AnnotatedPrimitiveType type1, AnnotatedPrimitiveType type2, Void aVoid) {
            return mapQualifierToPoly(type1, type2);
        }

        @Override
        public AnnotationMirrorMap<AnnotationMirrorSet> visitTypevar_Typevar(
                AnnotatedTypeVariable type1, AnnotatedTypeVariable type2, Void aVoid) {
            if (visited(type2)) {
                return new AnnotationMirrorMap<>();
            }
            AnnotationMirrorMap<AnnotationMirrorSet> result = mapQualifierToPoly(type1, type2);
            return reduce(result, super.visitTypevar_Typevar(type1, type2, aVoid));
        }

        @Override
        public AnnotationMirrorMap<AnnotationMirrorSet> visitUnion_Union(
                AnnotatedUnionType type1, AnnotatedUnionType type2, Void aVoid) {
            AnnotationMirrorMap<AnnotationMirrorSet> result = mapQualifierToPoly(type1, type2);
            return reduce(result, super.visitUnion_Union(type1, type2, aVoid));
        }

        @Override
        public AnnotationMirrorMap<AnnotationMirrorSet> visitWildcard_Wildcard(
                AnnotatedWildcardType type1, AnnotatedWildcardType type2, Void aVoid) {
            if (visited(type2)) {
                return new AnnotationMirrorMap<>();
            }
            AnnotationMirrorMap<AnnotationMirrorSet> result = mapQualifierToPoly(type1, type2);
            return reduce(result, super.visitWildcard_Wildcard(type1, type2, aVoid));
        }

        /** Resets the state. */
        public void reset() {
            this.visitedType2.clear();
            this.visited.clear();
        }
    }
}
