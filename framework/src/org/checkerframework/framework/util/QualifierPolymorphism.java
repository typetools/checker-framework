package org.checkerframework.framework.util;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeVisitor;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Implements framework support for type qualifier polymorphism. Checkers that wish to use it should
 * add calls to {@link #annotate(MethodInvocationTree, AnnotatedTypeMirror.AnnotatedExecutableType)}
 * to the {@link AnnotatedTypeFactory#addComputedTypeAnnotations(Tree, AnnotatedTypeMirror)} and
 * {@link AnnotatedTypeFactory#addComputedTypeAnnotations(Tree, AnnotatedTypeMirror)} methods.
 *
 * <p>This implementation currently only supports polymorphism for method invocations, for which the
 * return type depends on the unification of the parameter/receiver types.
 *
 * @see PolymorphicQualifier
 */
public class QualifierPolymorphism {

    private final Types types;

    private final AnnotatedTypeFactory atypeFactory;

    private final Completer completer;

    /**
     * The polymorphic qualifiers: mapping from the top of a qualifier hierarchy to the polymorphic
     * qualifier of that hierarchy. Field always non-null but might be an empty mapping. The "null"
     * key, if present, always maps to PolyAll.
     */
    protected final Map<AnnotationMirror, AnnotationMirror> polyQuals;

    /** The qualifiers at the top of the qualifier hierarchy. */
    protected final Set<? extends AnnotationMirror> topQuals;

    /** The qualifier hierarchy to use. */
    protected final QualifierHierarchy qualhierarchy;

    private final AnnotationMirror POLYALL;

    /**
     * Creates a {@link QualifierPolymorphism} instance that uses the given checker for querying
     * type qualifiers and the given factory for getting annotated types.
     *
     * @param env the processing environment
     * @param factory the factory for the current checker
     */
    public QualifierPolymorphism(ProcessingEnvironment env, AnnotatedTypeFactory factory) {
        this.atypeFactory = factory;

        this.types = env.getTypeUtils();

        Elements elements = env.getElementUtils();
        POLYALL = AnnotationBuilder.fromClass(elements, PolyAll.class);
        this.qualhierarchy = factory.getQualifierHierarchy();

        Map<AnnotationMirror, AnnotationMirror> polys =
                new HashMap<AnnotationMirror, AnnotationMirror>();
        for (AnnotationMirror aam : qualhierarchy.getTypeQualifiers()) {
            if (isPolyAll(aam)) {
                polys.put(null, aam);
                continue;
            }
            for (AnnotationMirror aa : aam.getAnnotationType().asElement().getAnnotationMirrors()) {
                if (aa.getAnnotationType()
                        .toString()
                        .equals(PolymorphicQualifier.class.getCanonicalName())) {
                    Name plval = AnnotationUtils.getElementValueClassName(aa, "value", true);
                    AnnotationMirror ttreetop;
                    if (PolymorphicQualifier.class.getCanonicalName().contentEquals(plval)) {
                        Set<? extends AnnotationMirror> tops = qualhierarchy.getTopAnnotations();
                        if (tops.size() != 1) {
                            ErrorReporter.errorAbort(
                                    "QualifierPolymorphism: PolymorphicQualifier has to specify type hierarchy, if more than one exist; top types: "
                                            + tops);
                        }
                        ttreetop = tops.iterator().next();
                    } else {
                        AnnotationMirror ttree = AnnotationBuilder.fromName(elements, plval);
                        ttreetop = qualhierarchy.getTopAnnotation(ttree);
                    }
                    if (polys.containsKey(ttreetop)) {
                        ErrorReporter.errorAbort(
                                "QualifierPolymorphism: checker has multiple polymorphic qualifiers: "
                                        + polys.get(ttreetop)
                                        + " and "
                                        + aam);
                    }
                    polys.put(ttreetop, aam);
                }
            }
        }

        this.polyQuals = polys;
        this.topQuals = qualhierarchy.getTopAnnotations();

        this.collector = new PolyCollector();
        this.completer = new Completer();
    }

    public static AnnotationMirror getPolymorphicQualifier(AnnotationMirror qual) {
        if (qual == null) {
            return null;
        }
        Element qualElt = qual.getAnnotationType().asElement();
        for (AnnotationMirror am : qualElt.getAnnotationMirrors()) {
            if (am.getAnnotationType()
                    .toString()
                    .equals(PolymorphicQualifier.class.getCanonicalName())) {
                return am;
            }
        }
        return null;
    }

    public static boolean isPolymorphicQualified(AnnotationMirror qual) {
        return getPolymorphicQualifier(qual) != null;
    }

    public static boolean isPolyAll(AnnotationMirror qual) {
        return AnnotationUtils.areSameByClass(qual, PolyAll.class);
    }

    /**
     * Returns null if the qualifier is not polymorphic. Returns the (given) top of the type
     * hierarchy, in which it is polymorphic, otherwise. The top qualifier is given by the
     * programmer, so must be normalized to ensure its the real top.
     */
    public static Class<? extends Annotation> getPolymorphicQualifierTop(
            Elements elements, AnnotationMirror qual) {
        AnnotationMirror poly = getPolymorphicQualifier(qual);

        // System.out.println("poly: " + poly + " pq: " +
        //     PolymorphicQualifier.class.getCanonicalName());
        if (poly == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> ret =
                (Class<? extends Annotation>)
                        AnnotationUtils.getElementValueClass(poly, "value", true);
        return ret;
    }

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param tree the tree associated with the type
     * @param type the type to annotate
     */
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

        Map<AnnotationMirror, Set<? extends AnnotationMirror>> matchingMapping =
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
    }

    public void annotate(NewClassTree tree, AnnotatedExecutableType type) {
        if (polyQuals.isEmpty()) {
            return;
        }
        List<AnnotatedTypeMirror> requiredArgs =
                AnnotatedTypes.expandVarArgs(atypeFactory, type, tree.getArguments());
        List<AnnotatedTypeMirror> arguments =
                AnnotatedTypes.getAnnotatedTypes(atypeFactory, requiredArgs, tree.getArguments());

        Map<AnnotationMirror, Set<? extends AnnotationMirror>> matchingMapping =
                collector.visit(arguments, requiredArgs);
        // TODO: poly on receiver for constructors?
        // matchingMapping = collector.reduce(matchingMapping,
        //        collector.visit(factory.getReceiverType(tree), type.getReceiverType()));

        if (matchingMapping != null && !matchingMapping.isEmpty()) {
            replacer.visit(type, matchingMapping);
        } else {
            completer.visit(type);
        }
    }

    public void annotate(
            AnnotatedExecutableType functionalInterface, AnnotatedExecutableType memberReference) {
        for (AnnotationMirror type : functionalInterface.getReturnType().getAnnotations()) {
            if (isPolymorphicQualified(type)) {
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

        Map<AnnotationMirror, Set<? extends AnnotationMirror>> matchingMapping =
                collector.visit(args, requiredArgs);

        if (matchingMapping != null && !matchingMapping.isEmpty()) {
            replacer.visit(memberReference, matchingMapping);
        } else {
            // TODO: Do we need this (return type?)
            completer.visit(memberReference);
        }
    }

    private final AnnotatedTypeScanner<Void, Map<AnnotationMirror, Set<? extends AnnotationMirror>>>
            replacer =
                    new AnnotatedTypeScanner<
                            Void, Map<AnnotationMirror, Set<? extends AnnotationMirror>>>() {
                        @Override
                        public Void scan(
                                AnnotatedTypeMirror type,
                                Map<AnnotationMirror, Set<? extends AnnotationMirror>> matches) {
                            for (Map.Entry<AnnotationMirror, Set<? extends AnnotationMirror>>
                                    pqentry : matches.entrySet()) {
                                AnnotationMirror poly = pqentry.getKey();
                                if (poly != null && type.hasAnnotation(poly)) {
                                    type.removeAnnotation(poly);
                                    Set<? extends AnnotationMirror> quals = pqentry.getValue();
                                    type.replaceAnnotations(quals);
                                }
                            }
                            return super.scan(type, matches);
                        }
                    };

    /**
     * Completes a type by removing any unresolved polymorphic qualifiers, replacing them with the
     * top qualifiers.
     */
    class Completer extends AnnotatedTypeScanner<Void, Void> {
        @Override
        protected Void scan(AnnotatedTypeMirror type, Void p) {
            for (Map.Entry<AnnotationMirror, AnnotationMirror> pqentry : polyQuals.entrySet()) {
                AnnotationMirror top = pqentry.getKey();
                AnnotationMirror poly = pqentry.getValue();

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

    private final PolyCollector collector;

    /**
     * A Helper class that tries to resolve the polymorhpic qualifiers with the most restricted
     * qualifier. The mapping is from the polymorhpic qualifier to the substitution for that
     * qualifier, which is a set of qualifiers. For most polymorphic qualifiers this will be a
     * singleton set. For the @PolyAll qualifier, this might be a set of qualifiers.
     */
    private class PolyCollector
            extends SimpleAnnotatedTypeVisitor<
                    Map<AnnotationMirror, Set<? extends AnnotationMirror>>, AnnotatedTypeMirror> {

        public Map<AnnotationMirror, Set<? extends AnnotationMirror>> reduce(
                Map<AnnotationMirror, Set<? extends AnnotationMirror>> r1,
                Map<AnnotationMirror, Set<? extends AnnotationMirror>> r2) {

            if (r1 == null || r1.isEmpty()) {
                return r2;
            }
            if (r2 == null || r2.isEmpty()) {
                return r1;
            }

            Map<AnnotationMirror, Set<? extends AnnotationMirror>> res =
                    new HashMap<AnnotationMirror, Set<? extends AnnotationMirror>>(r1.size());
            // Ensure that all qualifiers from r1 and r2 are visited.
            Set<AnnotationMirror> r2remain = AnnotationUtils.createAnnotationSet();
            r2remain.addAll(r2.keySet());
            for (Map.Entry<AnnotationMirror, Set<? extends AnnotationMirror>> kv1 : r1.entrySet()) {
                AnnotationMirror key1 = kv1.getKey();
                Set<? extends AnnotationMirror> a1Annos = kv1.getValue();
                Set<? extends AnnotationMirror> a2Annos = r2.get(key1);
                if (a2Annos != null && !a2Annos.isEmpty()) {
                    r2remain.remove(key1);
                    Set<? extends AnnotationMirror> lubs =
                            qualhierarchy.leastUpperBounds(a1Annos, a2Annos);
                    res.put(key1, lubs);
                } else {
                    res.put(key1, a1Annos);
                }
            }
            for (AnnotationMirror key2 : r2remain) {
                res.put(key2, r2.get(key2));
            }
            return res;
        }

        public Map<AnnotationMirror, Set<? extends AnnotationMirror>> visit(
                Iterable<? extends AnnotatedTypeMirror> types,
                Iterable<? extends AnnotatedTypeMirror> actualTypes) {
            Map<AnnotationMirror, Set<? extends AnnotationMirror>> result =
                    new HashMap<AnnotationMirror, Set<? extends AnnotationMirror>>();

            Iterator<? extends AnnotatedTypeMirror> itert = types.iterator();
            Iterator<? extends AnnotatedTypeMirror> itera = actualTypes.iterator();

            while (itert.hasNext() && itera.hasNext()) {
                AnnotatedTypeMirror type = itert.next();
                AnnotatedTypeMirror actualType = itera.next();
                result = reduce(result, visit(type, actualType));
            }
            return result;
        }

        @Override
        public Map<AnnotationMirror, Set<? extends AnnotationMirror>> visitDeclared(
                AnnotatedDeclaredType type, AnnotatedTypeMirror actualType) {
            if (actualType.getKind() == TypeKind.INTERSECTION) {
                // We don't support poly annotations on intersection types
                // See Issue 744
                // https://github.com/typetools/checker-framework/issues/744
                return Collections.emptyMap();
            }

            if (actualType.getKind() == TypeKind.TYPEVAR) {
                if (visited.contains(actualType.getUnderlyingType())) {
                    return Collections.emptyMap();
                }
                visited.add(actualType.getUnderlyingType());
                Map<AnnotationMirror, Set<? extends AnnotationMirror>> result =
                        visit(type, ((AnnotatedTypeVariable) actualType).getUpperBound());
                visited.remove(actualType.getUnderlyingType());
                return result;
            }

            if (actualType.getKind() == TypeKind.WILDCARD) {
                if (visited.contains(actualType.getUnderlyingType())) {
                    return Collections.emptyMap();
                }
                AnnotatedWildcardType wctype = (AnnotatedWildcardType) actualType;

                visited.add(actualType.getUnderlyingType());

                Map<AnnotationMirror, Set<? extends AnnotationMirror>> result;
                if (wctype.getUnderlyingType().getExtendsBound() != null) {
                    result = visit(type, wctype.getExtendsBound());
                } else if (wctype.getUnderlyingType().getSuperBound() != null) {
                    if (TypesUtils.isErasedSubtype(
                            type.getUnderlyingType(),
                            wctype.getSuperBound().getUnderlyingType(),
                            types)) {
                        result = visit(type, wctype.getSuperBound());
                    } else if (wctype.getSuperBound().getKind() == TypeKind.NULL) {
                        // TODO: poly annotation on wildcards need to be reviewed.  This prevents
                        // a crash in asSuper.
                        result = Collections.emptyMap();
                    } else {
                        AnnotatedTypeMirror superBoundAsSuper =
                                AnnotatedTypes.asSuper(atypeFactory, wctype.getSuperBound(), type);
                        result = visit(type, superBoundAsSuper);
                    }
                } else {
                    result = Collections.emptyMap();
                }
                visited.remove(actualType.getUnderlyingType());
                return result;
            }

            if (actualType.getKind() != type.getKind() || actualType == type) {
                return Collections.emptyMap();
            }

            assert actualType.getKind() == type.getKind();
            AnnotatedDeclaredType dcType = (AnnotatedDeclaredType) actualType;
            type = AnnotatedTypes.asSuper(atypeFactory, type, dcType);

            Map<AnnotationMirror, Set<? extends AnnotationMirror>> result =
                    mapQualifierToPoly(type, dcType);
            if (!type.wasRaw() && !dcType.wasRaw()) {
                result = reduce(result, visit(type.getTypeArguments(), dcType.getTypeArguments()));
            }

            return result;
        }

        @Override
        public Map<AnnotationMirror, Set<? extends AnnotationMirror>> visitPrimitive(
                AnnotatedPrimitiveType type, AnnotatedTypeMirror actualType) {
            return mapQualifierToPoly(type, actualType);
        }

        /**
         * If the primary annotation of {@code actualType} is a polymorphic qualifier, then it is
         * mapped to the primary annotation of {@code type} and the map is returned. Otherwise, an
         * empty map is returned.
         */
        private Map<AnnotationMirror, Set<? extends AnnotationMirror>> mapQualifierToPoly(
                AnnotatedTypeMirror type, AnnotatedTypeMirror actualType) {
            Map<AnnotationMirror, Set<? extends AnnotationMirror>> result = new HashMap<>();

            for (Map.Entry<AnnotationMirror, AnnotationMirror> kv : polyQuals.entrySet()) {
                AnnotationMirror top = kv.getKey();
                AnnotationMirror poly = kv.getValue();

                if (top == null && actualType.hasAnnotation(POLYALL)) {
                    // PolyAll qualifier
                    result.put(poly, type.getAnnotations());
                } else if (actualType.hasAnnotation(poly)) {
                    AnnotationMirror typeQual = type.getAnnotationInHierarchy(top);
                    result.put(poly, Collections.singleton(typeQual));
                }
            }
            return result;
        }

        @Override
        public Map<AnnotationMirror, Set<? extends AnnotationMirror>> visitNull(
                AnnotatedNullType type, AnnotatedTypeMirror actualType) {

            Map<AnnotationMirror, Set<? extends AnnotationMirror>> result =
                    mapQualifierToPoly(type, actualType);
            if (!result.isEmpty()) {
                return result;
            } else {
                return super.visitNull(type, actualType);
            }
        }

        @Override
        public Map<AnnotationMirror, Set<? extends AnnotationMirror>> visitArray(
                AnnotatedArrayType type, AnnotatedTypeMirror actualType) {
            if (actualType.getKind() == TypeKind.INTERSECTION) {
                // We don't support poly annotations on intersection types
                // See Issue 744
                // https://github.com/typetools/checker-framework/issues/744
                return Collections.emptyMap();
            }
            if (actualType.getKind() == TypeKind.DECLARED) {
                return visit(AnnotatedTypes.asSuper(atypeFactory, type, actualType), actualType);
            }
            if (actualType.getKind() == TypeKind.TYPEVAR) {
                if (visited.contains(actualType.getUnderlyingType())) {
                    return Collections.emptyMap();
                }
                visited.add(actualType.getUnderlyingType());
                Map<AnnotationMirror, Set<? extends AnnotationMirror>> result =
                        visit(type, ((AnnotatedTypeVariable) actualType).getUpperBound());
                visited.remove(actualType.getUnderlyingType());
                return result;
            }
            if (actualType.getKind() == TypeKind.WILDCARD) {
                if (visited.contains(actualType.getUnderlyingType())) {
                    return Collections.emptyMap();
                }
                visited.add(actualType.getUnderlyingType());
                Map<AnnotationMirror, Set<? extends AnnotationMirror>> result =
                        visit(type, ((AnnotatedWildcardType) actualType).getExtendsBound());
                visited.remove(actualType.getUnderlyingType());
                return result;
            }

            assert type.getKind() == actualType.getKind() : actualType;
            Map<AnnotationMirror, Set<? extends AnnotationMirror>> result =
                    mapQualifierToPoly(type, actualType);
            AnnotatedArrayType arType = (AnnotatedArrayType) actualType;
            result = reduce(result, visit(type.getComponentType(), arType.getComponentType()));
            return result;
        }

        private final Set<TypeMirror> visited = new HashSet<TypeMirror>();

        @Override
        public Map<AnnotationMirror, Set<? extends AnnotationMirror>> visitTypeVariable(
                AnnotatedTypeVariable type, AnnotatedTypeMirror actualType) {

            if (actualType.getKind() == TypeKind.WILDCARD) {
                // give up
                return Collections.emptyMap();
            }

            AnnotatedTypeMirror typeSuper = AnnotatedTypes.asSuper(atypeFactory, type, actualType);
            if (typeSuper.getKind() != TypeKind.TYPEVAR) {
                return visit(typeSuper, actualType);
            }

            if (typeSuper.getKind() == actualType.getKind()
                    && type.getKind() == actualType.getKind()) {
                // I've preserved the old logic here, I am not sure the actual reasoning
                // however, please see the else case as to where it fails

                AnnotatedTypeVariable tvType = (AnnotatedTypeVariable) typeSuper;
                if (visited.contains(actualType.getUnderlyingType())) {
                    return Collections.emptyMap();
                }
                visited.add(type.getUnderlyingType());
                // a type variable cannot be annotated
                Map<AnnotationMirror, Set<? extends AnnotationMirror>> result =
                        visit(type.getUpperBound(), tvType.getUpperBound());
                visited.remove(type.getUnderlyingType());
                return result;

            } else {
                // When using the polyCollector we compare the formal parameters to the actual
                // arguments but, when the formal parameters are uses of method type parameters then
                // the declared formal parameters may not actually be supertypes of their arguments
                // (though they should be if we substituted them for the method call's type
                // arguments).
                // For an example of this see framework/tests/all-system/PolyCollectorTypeVars.java
                return visit(type.getUpperBound(), actualType);
            }
        }

        @Override
        public Map<AnnotationMirror, Set<? extends AnnotationMirror>> visitWildcard(
                AnnotatedWildcardType type, AnnotatedTypeMirror actualType) {
            if (type.isUninferredTypeArgument()
                    || !TypesUtils.isErasedSubtype(
                            type.getUnderlyingType(), actualType.getUnderlyingType(), types)) {
                return mapQualifierToPoly(type.getExtendsBound(), actualType);
            }
            AnnotatedTypeMirror typeSuper = AnnotatedTypes.asSuper(atypeFactory, type, actualType);
            if (typeSuper.getKind() != TypeKind.WILDCARD) {
                return visit(typeSuper, actualType);
            }
            // TODO: hack against unbound wildcard introduced by
            // separate compilation. Test against Issue 257 test.
            if (((com.sun.tools.javac.code.Type.WildcardType) typeSuper.getUnderlyingType())
                    .isUnbound()) {
                return Collections.emptyMap();
            }

            if (actualType.getKind() != TypeKind.WILDCARD
                    && actualType.getKind() != TypeKind.TYPEVAR) {
                // currently because the default action of inferTypeArgs is to use a wildcard when
                // we fail to infer a type, the actualType might not be a wildcard
                return Collections.emptyMap();
            }

            AnnotatedWildcardType wcType = (AnnotatedWildcardType) typeSuper;

            if (visited.contains(actualType.getUnderlyingType())) {
                return Collections.emptyMap();
            }
            visited.add(type.getUnderlyingType());
            Map<AnnotationMirror, Set<? extends AnnotationMirror>> result;
            if (type.getExtendsBound() != null && wcType.getExtendsBound() != null) {
                result = visit(type.getExtendsBound(), wcType.getExtendsBound());
            } else if (type.getSuperBound() != null && wcType.getSuperBound() != null) {
                result = visit(type.getSuperBound(), wcType.getSuperBound());
            } else {
                result = new HashMap<AnnotationMirror, Set<? extends AnnotationMirror>>();
            }

            visited.remove(type.getUnderlyingType());
            return result;
        }
    }
}
