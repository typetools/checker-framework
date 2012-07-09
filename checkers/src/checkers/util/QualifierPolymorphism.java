package checkers.util;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.PolymorphicQualifier;
import checkers.source.SourceChecker;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.types.visitors.AnnotatedTypeScanner;
import checkers.types.visitors.SimpleAnnotatedTypeVisitor;

import com.sun.source.tree.*;

/**
 * Implements framework support for type qualifier polymorphism. Checkers that
 * wish to use it should add calls to
 * {@link #annotate(MethodInvocationTree, AnnotatedTypeMirror.AnnotatedExecutableType) annotate(MethodInvocationTree, AnnotatedExecutableType)}
 * {@link #annotate(Element, AnnotatedTypeMirror)} to the
 * {@link AnnotatedTypeFactory#annotateImplicit(Tree, AnnotatedTypeMirror)} and
 * {@link AnnotatedTypeFactory#annotateImplicit(Tree, AnnotatedTypeMirror)}
 * methods.
 *
 * <p>
 *
 * This implementation currently only supports polymorphism for method
 * invocations, for which the return type depends on the unification of the
 * parameter/receiver types.
 *
 * @see PolymorphicQualifier
 */
public class QualifierPolymorphism {

    private final AnnotatedTypeFactory factory;
    private final AnnotationUtils annoFactory;
    private final AnnotatedTypes atypes;

    private final Completer completer;

    /** The polymorphic qualifiers: mapping from the root of a qualifier
     * hierarchy to the polymorphic qualifier of that hierarchy.
     * Always non-null but might be an empty mapping.
     */
    protected final Map<AnnotationMirror, AnnotationMirror> polyQuals;

    /** The qualifier at the root of the hierarchy. */
    protected final Set<AnnotationMirror> rootQuals;

    /** The qualifier hierarchy to use. */
    protected final QualifierHierarchy qualhierarchy;

    /**
     * Creates a {@link QualifierPolymorphism} instance that uses the given
     * checker for querying type qualifiers and the given factory for getting
     * annotated types.
     *
     * @param checker the current checker
     * @param factory the factory for the current checker
     */
    public QualifierPolymorphism(BaseTypeChecker checker, AnnotatedTypeFactory factory) {
        this.factory = factory;

        final ProcessingEnvironment env = checker.getProcessingEnvironment();
        this.atypes = new AnnotatedTypes(env, factory);
        this.annoFactory = AnnotationUtils.getInstance(env);
        this.qualhierarchy = checker.getQualifierHierarchy();

        Map<AnnotationMirror, AnnotationMirror> polys = new HashMap<AnnotationMirror, AnnotationMirror>();
        for (Class<? extends Annotation> a : checker.getSupportedTypeQualifiers()) {
            AnnotationMirror aam = annoFactory.fromClass(a);
            for (AnnotationMirror aa : aam.getAnnotationType().asElement().getAnnotationMirrors() ) {
                if (aa.getAnnotationType().toString().equals(PolymorphicQualifier.class.getCanonicalName())) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Annotation> plval = (Class<? extends Annotation>)
                                                        AnnotationUtils.parseTypeValue(aa, "value");
                    AnnotationMirror ttreeroot;
                    if (PolymorphicQualifier.class.equals(plval)) {
                        Set<AnnotationMirror> roots = qualhierarchy.getRootAnnotations();
                        if (roots.size() != 1) {
                            throw new SourceChecker.CheckerError(
                                    "PolymorphicQualifier has to specify type hierarchy, if more than one exist; top types: " +
                                    roots);
                        }
                        ttreeroot = roots.iterator().next();
                    } else {
                        AnnotationMirror ttree = annoFactory.fromClass(plval);
                        ttreeroot = qualhierarchy.getRootAnnotation(ttree);
                    }
                    if (polys.containsKey(ttreeroot)) {
                        throw new SourceChecker.CheckerError(
                                "Checker has multiple polymorphic qualifiers: " +
                                polys.get(ttreeroot) + " and " + a);
                    }
                    polys.put(ttreeroot, annoFactory.fromClass(a));
                }
            }
        }

        this.polyQuals = polys;
        this.rootQuals = qualhierarchy.getRootAnnotations();

        this.collector = new PolyCollector();
        this.completer = new Completer();
    }

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param tree the tree associated with the type
     * @param type the type to annotate
     */
    public void annotate(MethodInvocationTree tree, AnnotatedExecutableType type) {
        if (polyQuals.isEmpty()) return;
        List<AnnotatedTypeMirror> arguments = atypes.getAnnotatedTypes(tree.getArguments());
        List<AnnotatedTypeMirror> requiredArgs = atypes.expandVarArgs(type, tree.getArguments());
        Map<AnnotationMirror, AnnotationMirror> matchingMapping = collector.visit(arguments, requiredArgs);
        matchingMapping = collector.reduce(matchingMapping,
                collector.visit(factory.getReceiverType(tree), type.getReceiverType()));

        if (matchingMapping != null && !matchingMapping.isEmpty()) {
            replacer.visit(type, matchingMapping);
        } else {
            completer.visit(type);
        }
    }

    private final AnnotatedTypeScanner<Void, Map<AnnotationMirror, AnnotationMirror>> replacer
    = new AnnotatedTypeScanner<Void, Map<AnnotationMirror, AnnotationMirror>>() {
        @Override
        public Void scan(AnnotatedTypeMirror type, Map<AnnotationMirror, AnnotationMirror> matches) {
            if (type != null) {
                for (Map.Entry<AnnotationMirror, AnnotationMirror> pqentry : matches.entrySet()) {
                    AnnotationMirror poly = pqentry.getKey();
                    if (type.hasAnnotation(poly)) {
                        AnnotationMirror qual = pqentry.getValue();
                        type.replaceAnnotation(qual);
                    }
                }
            }
            return super.scan(type, matches);
        }
    };

    /**
     * Completes a type by removing any unresolved polymorphic qualifiers,
     * replacing them with the root qualifiers.
     */
    class Completer extends AnnotatedTypeScanner<Void, Void> {
        @Override
        protected Void scan(AnnotatedTypeMirror type, Void p) {
            if (type != null) {
                for (Map.Entry<AnnotationMirror, AnnotationMirror> pqentry : polyQuals.entrySet()) {
                    // AnnotationMirror top = pqentry.getKey();
                    AnnotationMirror poly = pqentry.getValue();

                    if (type.hasAnnotation(poly)) {
                        type.removeAnnotation(poly);
                    }
                    if (!type.isAnnotated() &&
                            !(type.getKind()==TypeKind.TYPEVAR || type.getKind()==TypeKind.WILDCARD)) {
                        // Do not add the root qualifier to type variables and wildcards
                        type.addAnnotations(rootQuals);
                    }
                }
            }
            return super.scan(type, p);
        }
    }


    private final PolyCollector collector;

    /**
     * A Helper class that tries to resolve the polymorhpic qualifiers with
     * the most restricted qualifier.
     * The mapping is from the polymorhpic qualifier to the substitution for that qualifier.
     */
    private class PolyCollector
    extends SimpleAnnotatedTypeVisitor<Map<AnnotationMirror, AnnotationMirror>, AnnotatedTypeMirror> {

        public Map<AnnotationMirror, AnnotationMirror> reduce(Map<AnnotationMirror, AnnotationMirror> r1,
                Map<AnnotationMirror, AnnotationMirror> r2) {

            if (r1 == null || r1.isEmpty())
                return r2;
            if (r2 == null || r2.isEmpty())
                return r1;

            Map<AnnotationMirror, AnnotationMirror> res = new HashMap<AnnotationMirror, AnnotationMirror>(r1.size());
            Set<AnnotationMirror> r2remain = AnnotationUtils.createAnnotationSet();
            r2remain.addAll(r2.keySet());
            for (Map.Entry<AnnotationMirror, AnnotationMirror> kv1 : r1.entrySet()) {
                AnnotationMirror key1 = kv1.getKey();
                AnnotationMirror a1Anno = kv1.getValue();
                AnnotationMirror a2Anno = r2.get(key1);
                if (a2Anno!=null) {
                    r2remain.remove(key1);
                    AnnotationMirror lub = qualhierarchy.leastUpperBound(a1Anno, a2Anno);
                    res.put(key1, lub);
                } else {
                    res.put(key1, a1Anno);
                }
            }
            for (AnnotationMirror key2 : r2remain) {
                res.put(key2, r2.get(key2));
            }
            return res;
        }

        public Map<AnnotationMirror, AnnotationMirror> visit(Iterable<? extends AnnotatedTypeMirror> types,
                Iterable<? extends AnnotatedTypeMirror> actualTypes) {
            Map<AnnotationMirror, AnnotationMirror> result = new HashMap<AnnotationMirror, AnnotationMirror>();

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
        public Map<AnnotationMirror, AnnotationMirror> visitDeclared(
                AnnotatedDeclaredType type, AnnotatedTypeMirror actualType) {

            if (actualType.getKind() == TypeKind.TYPEVAR) {
                if (visited.contains(actualType.getUnderlyingType()))
                    return Collections.emptyMap();
                visited.add((TypeVariable)actualType.getUnderlyingType());
                Map<AnnotationMirror, AnnotationMirror> result =
                        visit(type, ((AnnotatedTypeVariable)actualType).getUpperBound());
                visited.remove(actualType.getUnderlyingType());
                return result;
            }

            if (actualType.getKind() == TypeKind.WILDCARD) {
                if (visited.contains(actualType.getUnderlyingType()))
                    return Collections.emptyMap();
                AnnotatedWildcardType wctype = (AnnotatedWildcardType)actualType;

                visited.add(actualType.getUnderlyingType());

                Map<AnnotationMirror, AnnotationMirror> result;
                if (wctype.getUnderlyingType().getExtendsBound()!=null) {
                    result = visit(type, wctype.getExtendsBound());
                } else if (wctype.getUnderlyingType().getSuperBound()!=null) {
                    // TODO: is the logic different for super bounds?
                    result = visit(type, wctype.getSuperBound());
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
            type = (AnnotatedDeclaredType)atypes.asSuper(type, actualType);
            // TODO: type is null if type is an intersection type
            // assert type != null;
            if (type == null)
                return Collections.emptyMap();

            AnnotatedDeclaredType dcType = (AnnotatedDeclaredType)actualType;

            Map<AnnotationMirror, AnnotationMirror> result =
                new HashMap<AnnotationMirror, AnnotationMirror>();

            for (Map.Entry<AnnotationMirror, AnnotationMirror> kv : polyQuals.entrySet()) {
                AnnotationMirror root = kv.getKey();
                AnnotationMirror poly = kv.getValue();

                if (dcType.hasAnnotation(poly)) {
                    AnnotationMirror typeQual = type.getAnnotationInHierarchy(root);
                    result.put(poly, typeQual);
                }
            }

            if (type.isParameterized() && dcType.isParameterized()) {
                result = reduce(result, visit(type.getTypeArguments(), dcType.getTypeArguments()));
            }

            return result;
        }

        @Override
        public Map<AnnotationMirror, AnnotationMirror> visitNull(
                AnnotatedNullType type, AnnotatedTypeMirror actualType) {

            Map<AnnotationMirror, AnnotationMirror> result =
                    new HashMap<AnnotationMirror, AnnotationMirror>();

            for (Map.Entry<AnnotationMirror, AnnotationMirror> kv : polyQuals.entrySet()) {
                AnnotationMirror root = kv.getKey();
                AnnotationMirror poly = kv.getValue();

                if (actualType.hasAnnotation(poly)) {
                    AnnotationMirror typeQual = type.getAnnotationInHierarchy(root);
                    result.put(poly, typeQual);
                }
            }
            if (!result.isEmpty()) {
                return result;
            } else {
                return super.visitNull(type, actualType);
            }
        }

        @Override
        public Map<AnnotationMirror, AnnotationMirror> visitArray(
                AnnotatedArrayType type, AnnotatedTypeMirror actualType) {

            if (actualType.getKind() == TypeKind.DECLARED)
                return visit(atypes.asSuper(type, actualType), actualType);
            if (actualType.getKind() == TypeKind.TYPEVAR) {
                if (visited.contains(actualType.getUnderlyingType()))
                    return Collections.emptyMap();
                visited.add(actualType.getUnderlyingType());
                Map<AnnotationMirror, AnnotationMirror> result = visit(type, ((AnnotatedTypeVariable)actualType).getUpperBound());
                visited.remove(actualType.getUnderlyingType());
                return result;
            }
            if (actualType.getKind() == TypeKind.WILDCARD) { 
                if (visited.contains(actualType.getUnderlyingType()))
                    return Collections.emptyMap();
                visited.add(actualType.getUnderlyingType());
                Map<AnnotationMirror, AnnotationMirror> result = visit(type, ((AnnotatedWildcardType)actualType).getExtendsBound());
                visited.remove(actualType.getUnderlyingType());
                return result;
            }

            assert type.getKind() == actualType.getKind() : actualType;
            AnnotatedArrayType arType = (AnnotatedArrayType)actualType;

            Map<AnnotationMirror, AnnotationMirror> result =
                new HashMap<AnnotationMirror, AnnotationMirror>();

            for (Map.Entry<AnnotationMirror, AnnotationMirror> kv : polyQuals.entrySet()) {
                AnnotationMirror root = kv.getKey();
                AnnotationMirror poly = kv.getValue();

                if (arType.hasAnnotation(poly)) {
                    AnnotationMirror typeQual = type.getAnnotationInHierarchy(root);
                    result.put(poly, typeQual);
                }
            }

            result = reduce(result, visit(type.getComponentType(), arType.getComponentType()));
            return result;
        }

        private final Set<TypeMirror> visited = new HashSet<TypeMirror>();

        @Override
        public Map<AnnotationMirror, AnnotationMirror> visitTypeVariable(
                AnnotatedTypeVariable type, AnnotatedTypeMirror actualType) {

            if (actualType.getKind() == TypeKind.WILDCARD)
                // give up
                return Collections.emptyMap();

            AnnotatedTypeMirror typeSuper = findType(type, actualType);
            if (typeSuper.getKind() != TypeKind.TYPEVAR)
                return visit(typeSuper, actualType);

            assert typeSuper.getKind() == actualType.getKind() : actualType;
            assert type.getKind() == actualType.getKind() : actualType;
            AnnotatedTypeVariable tvType = (AnnotatedTypeVariable)typeSuper;

            if (visited.contains(actualType.getUnderlyingType()))
                return Collections.emptyMap();
            visited.add(type.getUnderlyingType());
            // a type variable cannot be annotated
            Map<AnnotationMirror, AnnotationMirror> result = visit(type.getUpperBound(), tvType.getUpperBound());
            visited.remove(type.getUnderlyingType());
            return result;
        }

        @Override
        public Map<AnnotationMirror, AnnotationMirror> visitWildcard(
                AnnotatedWildcardType type, AnnotatedTypeMirror actualType) {
            AnnotatedTypeMirror typeSuper = findType(type, actualType);
            if (typeSuper.getKind() != TypeKind.WILDCARD)
                return visit(typeSuper, actualType);
            assert typeSuper.getKind() == actualType.getKind() : actualType;
            AnnotatedWildcardType wcType = (AnnotatedWildcardType)typeSuper;

            if (visited.contains(actualType.getUnderlyingType()))
                return Collections.emptyMap();
            visited.add(type.getUnderlyingType());
            Map<AnnotationMirror, AnnotationMirror> result;
            if (type.getExtendsBound() != null && wcType.getExtendsBound() != null)
                result = visit(type.getExtendsBound(), wcType.getExtendsBound());
            else if (type.getSuperBound() != null && wcType.getSuperBound() != null)
                result = visit(type.getSuperBound(), wcType.getSuperBound());
            else
                result = new HashMap<AnnotationMirror, AnnotationMirror>();

            visited.remove(type.getUnderlyingType());
            return result;
        }

        private AnnotatedTypeMirror findType(AnnotatedTypeMirror type, AnnotatedTypeMirror actualType) {
            AnnotatedTypeMirror result = atypes.asSuper(type, actualType);
            // result shouldn't be null, will test this hypothesis later
            // assert result != null;
            return (result != null ? result : type);
        }
    }

}
