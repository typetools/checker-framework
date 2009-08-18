package checkers.util;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.PolymorphicQualifier;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.types.visitors.AnnotatedTypeScanner;
import checkers.types.visitors.SimpleAnnotatedTypeScanner;
import checkers.types.visitors.SimpleAnnotatedTypeVisitor;

import com.sun.source.tree.*;
import com.sun.source.util.SimpleTreeVisitor;

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

    private final Resolver resolver;
    private final Completer completer;

    /** The polymorphic qualifier. */
    protected final AnnotationMirror polyQual;

    /** The qualifier at the root of the hierarchy. */
    protected final AnnotationMirror rootQual;

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

        AnnotationMirror poly = null;
        AnnotationMirror root = null;
        for (Class<? extends Annotation> a : checker.getSupportedTypeQualifiers()) {
            if (a.getAnnotation(PolymorphicQualifier.class) != null) {
                if (poly != null)
                    throw new IllegalArgumentException(
                            "checker has multiple polymorphic qualifiers");
                poly = annoFactory.fromClass(a);
            }
        }
        root = checker.getQualifierHierarchy().getRootAnnotation();

        this.polyQual = poly;
        this.rootQual = root;

        this.collector = new PolyCollector();
        this.resolver = new Resolver();
        this.completer = new Completer();
    }

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param elt the element associated with the type
     * @param type the type to annotate
     */
    public void annotate(Element elt, AnnotatedTypeMirror type) {
        if (polyQual == null) return;
        completer.visit(type);
    }

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param tree the tree associated with the type
     * @param type the type to annotate
     */
    public void annotate(MethodInvocationTree tree, AnnotatedExecutableType type) {
        if (polyQual == null)
            return;
        List<AnnotatedTypeMirror> arguments = atypes.getAnnotatedTypes(tree.getArguments());
        List<AnnotatedTypeMirror> requiredArgs = atypes.expandVarArgs(type, tree.getArguments());
        Map<String, AnnotationMirror> matchingMapping = collector.visit(arguments, requiredArgs);
        matchingMapping = collector.reduce(matchingMapping,
                collector.visit(factory.getReceiver(tree), type.getReceiverType()));
        if (matchingMapping != null && !matchingMapping.isEmpty())
            replacer.visit(type, matchingMapping.values().iterator().next());
        else
            completer.visit(type);
    }

    private AnnotatedTypeScanner<Void, AnnotationMirror> replacer
    = new AnnotatedTypeScanner<Void, AnnotationMirror>() {
        @Override
        public Void scan(AnnotatedTypeMirror type, AnnotationMirror qual) {
            if (type != null && type.hasAnnotation(polyQual)) {
                type.removeAnnotation(polyQual);
                type.addAnnotation(qual);
            }
            return super.scan(type, qual);
        }
    };

    /**
     * Completes a type by removing any unresolved polymorphic qualifiers,
     * replacing them with the root qualifier if one is defined.
     */
    class Completer extends AnnotatedTypeScanner<Void, Void> {
        @Override
        protected Void scan(AnnotatedTypeMirror type, Void p) {
            if (type != null && type.hasAnnotation(polyQual))
                type.removeAnnotation(polyQual);
            if (rootQual != null && type != null && type.getAnnotations().isEmpty())
                type.addAnnotation(rootQual);
            return super.scan(type, p);
        }
    }

    /**
     * Resolves the actual qualifiers to replace polymorphism qualifiers based
     * on the arguments of a method invocation.
     */
    class Resolver extends SimpleTreeVisitor<Void, AnnotatedTypeMirror> {

        /**
         * Given a method invocation along with the return, parameter, and
         * receiver types of the invoked method, performs substitution of
         * polymorphism annotations based on the method invocation arguments.
         *
         * @param node the method invocation
         * @param type the type on which annotations should be resolved
         * @param ret the return type of the invoked method
         * @param params the parameter types of the invoked method
         * @param rcv the receiver type of the invoked method
         */
        private void resolve(MethodInvocationTree node,
                AnnotatedTypeMirror type, AnnotatedTypeMirror ret,
                List<AnnotatedTypeMirror> params, AnnotatedTypeMirror rcv) {

            if (ret.hasAnnotation(polyQual)) {

                int i = 0;
                boolean flag = false;
                for (AnnotatedTypeMirror param : params) {
                    ExpressionTree arg = node.getArguments().get(i);
                    AnnotatedTypeMirror argType = factory.getAnnotatedType(arg);
                    flag = resolveForType(param, argType, type, flag);
                    if (argType instanceof AnnotatedDeclaredType
                            && param instanceof AnnotatedDeclaredType) {
                        AnnotatedDeclaredType argDecl = (AnnotatedDeclaredType) argType;
                        AnnotatedDeclaredType paramDecl = (AnnotatedDeclaredType) param;
                        for (int j = 0; j < argDecl.getTypeArguments().size(); j++)
                            flag = resolveForType(paramDecl.getTypeArguments()
                                    .get(j), argDecl.getTypeArguments().get(j), type,
                                    flag);
                    } // TODO else for arrays
                    i++;
                }

                if (flag)
                    return;

                if (rcv.hasAnnotation(polyQual)) {
                    Set<AnnotationMirror> r = factory.getReceiver(node).getAnnotations();
                    type.clearAnnotations();
                    type.addAnnotations(r);
                }
            }
            return;
        }

        /**
         * Resolves annotations for a particular type, accounting for wildcards.
         *
         * @param decl the type as declared (e.g. as a method parameter)
         * @param actual the actual type (e.g. as an argument to a method call)
         * @param type the type on which to perform substitutions
         * @param merge if false, replace annotations on {@code type}, else
         *        merge them
         * @return true if annotations were replaced
         */
        private boolean resolveForType(AnnotatedTypeMirror decl,
                AnnotatedTypeMirror actual, AnnotatedTypeMirror type, boolean merge) {
            if (decl instanceof AnnotatedWildcardType)
                decl = ((AnnotatedWildcardType) decl).getExtendsBound();
            if (actual instanceof AnnotatedWildcardType)
                actual = ((AnnotatedWildcardType) actual).getExtendsBound();
            if (decl.hasAnnotation(polyQual)) {
                if (!merge) {
                    type.clearAnnotations();
                    type.addAnnotations(actual.getAnnotations());
                    return true;
                }
                for (AnnotationMirror a : type.getAnnotations())
                    if (!actual.hasAnnotation(a))
                        type.removeAnnotation(a);
            }
            return false;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node,
                AnnotatedTypeMirror p) {

            if (p == null)
                return null;

            // Get the element & type of the invoked method.
            ExecutableElement elt = TreeUtils.elementFromUse(node);
            AnnotatedExecutableType ex = factory.fromElement(elt);

            // Get the return, parameter, and receiver types.
            AnnotatedTypeMirror ret = ex.getReturnType();
            List<AnnotatedTypeMirror> params = atypes.expandVarArgs(ex, node.getArguments());
            AnnotatedTypeMirror rcv = ex.getReceiverType();

            resolve(node, p, ret, params, rcv);

            if (p instanceof AnnotatedDeclaredType
                    && ret instanceof AnnotatedDeclaredType) {

                AnnotatedDeclaredType pDecl = (AnnotatedDeclaredType) p;
                AnnotatedDeclaredType rDecl = (AnnotatedDeclaredType) ex.getReturnType();

                for (int i = 0; i < pDecl.getTypeArguments().size(); i++)
                    resolve(node,
                            pDecl.getTypeArguments().get(i),
                            rDecl.getTypeArguments().get(i),
                            params, rcv);

            } else if (p instanceof AnnotatedArrayType
                    && ret instanceof AnnotatedArrayType) {
                AnnotatedArrayType pArray = (AnnotatedArrayType) p;
                AnnotatedArrayType rArray = (AnnotatedArrayType) ret;

                resolve(node,
                        pArray.getComponentType(), rArray.getComponentType(),
                        params, rcv);
            }

            return null;
        }
    }

    private final PolyCollector collector;

    /**
     * A Helper class that tries to resolve the immutability type variable,
     * as the type variable is assigned to the most restricted immutability
     */
    private class PolyCollector
    extends SimpleAnnotatedTypeVisitor<Map<String, AnnotationMirror>, AnnotatedTypeMirror> {
        private static final String KEY = "key";
        private final QualifierHierarchy hierarchy = factory.getQualifierHierarchy();

        public Map<String, AnnotationMirror> reduce(Map<String, AnnotationMirror> r1,
                Map<String, AnnotationMirror> r2) {

            if (r1 == null || r1.isEmpty())
                return r2;
            if (r2 == null || r2.isEmpty())
                return r1;

            AnnotationMirror a1Anno = r1.get(KEY);
            AnnotationMirror a2Anno = r2.get(KEY);
            AnnotationMirror lub = hierarchy.leastUpperBound(a1Anno, a2Anno);
            return Collections.singletonMap(KEY, lub);
        }

        public Map<String, AnnotationMirror> visit(Iterable<? extends AnnotatedTypeMirror> types,
                Iterable<? extends AnnotatedTypeMirror> actualTypes) {
            Map<String, AnnotationMirror> result = new HashMap<String, AnnotationMirror>();

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
        public Map<String, AnnotationMirror> visitDeclared(
                AnnotatedDeclaredType type, AnnotatedTypeMirror actualType) {

            if (actualType.getKind() == TypeKind.TYPEVAR) {
                if (typeVar.contains(actualType.getUnderlyingType()))
                    return Collections.emptyMap();
                typeVar.add((TypeVariable)actualType.getUnderlyingType());
                Map<String, AnnotationMirror> result = visit(type, ((AnnotatedTypeVariable)actualType).getUpperBound());
                typeVar.remove(actualType.getUnderlyingType());
                return result;
            }
            if (actualType.getKind() == TypeKind.WILDCARD)
                return visit(type, ((AnnotatedWildcardType)actualType).getExtendsBound());

            if (actualType.getKind() != type.getKind() || actualType == type)
                return Collections.emptyMap();

            assert actualType.getKind() == type.getKind();
            type = (AnnotatedDeclaredType)atypes.asSuper(type, actualType);
            assert type != null;
            AnnotatedDeclaredType dcType = (AnnotatedDeclaredType)actualType;

            Map<String, AnnotationMirror> result =
                new HashMap<String, AnnotationMirror>();

            if (dcType.hasAnnotation(polyQual)) {
                AnnotationMirror typeQual =
                    type.getAnnotations().isEmpty() ? null : type.getAnnotations().iterator().next();
                result.put(KEY, typeQual);
            }

            if (type.isParameterized() && dcType.isParameterized())
                result = reduce(result, visit(type.getTypeArguments(), dcType.getTypeArguments()));

            return result;
        }

        @Override
        public Map<String, AnnotationMirror> visitArray(
                AnnotatedArrayType type, AnnotatedTypeMirror actualType) {

            if (actualType.getKind() == TypeKind.DECLARED)
                return visit(atypes.asSuper(type, actualType), actualType);
            if (actualType.getKind() == TypeKind.TYPEVAR) {
                if (typeVar.contains(actualType.getUnderlyingType()))
                    return Collections.emptyMap();
                typeVar.add((TypeVariable)actualType.getUnderlyingType());
                Map<String, AnnotationMirror> result = visit(type, ((AnnotatedTypeVariable)actualType).getUpperBound());
                typeVar.remove(actualType.getUnderlyingType());
                return result;
            }
            if (actualType.getKind() == TypeKind.WILDCARD)
                return visit(type, ((AnnotatedWildcardType)actualType).getExtendsBound());

            assert type.getKind() == actualType.getKind() : actualType;
            AnnotatedArrayType arType = (AnnotatedArrayType)actualType;

            Map<String, AnnotationMirror> result =
                new HashMap<String, AnnotationMirror>();

            if (arType.hasAnnotation(polyQual)) {
                AnnotationMirror typeQual =
                    type.getAnnotations().isEmpty() ? null : type.getAnnotations().iterator().next();
                result.put(KEY, typeQual);
            }

            result = reduce(result, visit(type.getComponentType(), arType.getComponentType()));
            return result;
        }

        private Set<TypeVariable> typeVar = new HashSet<TypeVariable>();

        @Override
        public Map<String, AnnotationMirror> visitTypeVariable(
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

            typeVar.add(type.getUnderlyingType());
            // a type variable cannot be annotated
            Map<String, AnnotationMirror> result = visit(type.getUpperBound(), tvType.getUpperBound());
            typeVar.remove(type.getUnderlyingType());
            return result;
        }

        @Override
        public Map<String, AnnotationMirror> visitWildcard(
                AnnotatedWildcardType type, AnnotatedTypeMirror actualType) {
            AnnotatedTypeMirror typeSuper = findType(type, actualType);
            if (typeSuper.getKind() != TypeKind.WILDCARD)
                return visit(typeSuper, actualType);
            assert typeSuper.getKind() == actualType.getKind() : actualType;
            AnnotatedWildcardType wcType = (AnnotatedWildcardType)typeSuper;

            if (type.getExtendsBound() != null && wcType.getExtendsBound() != null)
                return visit(type.getExtendsBound(), wcType.getExtendsBound());
            else if (type.getSuperBound() != null && wcType.getSuperBound() != null)
                return visit(type.getSuperBound(), wcType.getSuperBound());
            else
                return new HashMap<String, AnnotationMirror>();
        }

        private AnnotatedTypeMirror findType(AnnotatedTypeMirror type, AnnotatedTypeMirror actualType) {
            AnnotatedTypeMirror result = atypes.asSuper(type, actualType);
            // result shouldn't be null, will test this hypothesis later
            // assert result != null;
            return (result != null ? result : type);
        }
    }

}
