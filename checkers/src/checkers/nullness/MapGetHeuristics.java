package checkers.nullness;

import static checkers.util.Heuristics.Matchers.*;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;

import checkers.nullness.quals.KeyFor;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeFactory;
import checkers.util.AnnotationUtils;
import checkers.util.Heuristics.Matcher;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;
import checkers.util.Resolver;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;

/**
 * Utilities class for handling {@code Map.get()} invocations.
 *
 * The heuristics cover the following cases:
 *
 * <ol>
 * <li value="1">Within the true condition of a map.containsKey() if statement:
 * <pre><code>if (map.containsKey(key)) { Object v = map.get(key); }</code></pre>
 * </li>
 *
 * <li value="2">Within an enhanced-for loop of the map.keySet():
 * <pre><code>for (Object key: map.keySet()) { Object v = map.get(key); }</code></pre>
 * </li>
 *
 * <li value="3">Preceded by an assertion of contains or nullness get check:
 * <pre><code>assert map.containsKey(key);
 * Object v = map.get(key);</code></pre>
 *
 * Or
 *
 * <pre><code>assert map.get(key) != null;
 * Object v = map.get(key);</code></pre>
 *
 * <li value="4">Preceded by an check of contains or nullness if
 * test that throws an exception, in the first line:
 *
 * <pre><code>if (!map.contains(key)) throw new Exception();
 * Object v = map.get(key);
 * </code></pre>
 *
 * <li value="5">Preceded by a put-if-absent pattern convention:
 *
 * <pre><code>if (!map.contains(key)) map.put(key, DEFAULT_VALUE);
 * Object v = map.get(key);</code></pre>
 *
 * </ol>
 */
/*package-scope*/ class MapGetHeuristics {

    private final ProcessingEnvironment env;
    private final NullnessAnnotatedTypeFactory factory;
    private final AnnotatedTypeFactory keyForFactory;
    private final Resolver resolver;

    private final ExecutableElement mapGet;
    private final ExecutableElement mapPut;
    private final ExecutableElement mapKeySet;
    private final ExecutableElement mapContains;

    public MapGetHeuristics(ProcessingEnvironment env,
            NullnessAnnotatedTypeFactory factory,
            AnnotatedTypeFactory keyForFactory) {
        this.env = env;
        this.factory = factory;
        this.keyForFactory = keyForFactory;
        this.resolver = new Resolver(env);

        mapGet = TreeUtils.getMethod("java.util.Map", "get", 1, env);
        mapPut = TreeUtils.getMethod("java.util.Map", "put", 2, env);
        mapKeySet = TreeUtils.getMethod("java.util.Map", "keySet", 0, env);
        mapContains = TreeUtils.getMethod("java.util.Map", "containsKey", 1, env);
    }

    public void handle(TreePath path, AnnotatedExecutableType method) {
        MethodInvocationTree tree = (MethodInvocationTree)path.getLeaf();
        if (TreeUtils.isMethodInvocation(tree, mapGet, env)) {
            AnnotatedTypeMirror type = method.getReturnType();
            if (!isSuppressable(path)) {
                type.replaceAnnotation(factory.NULLABLE);
            } else {
                type.replaceAnnotation(factory.NONNULL);
            }
        }
    }

    /**
     * Checks whether the key passed to {@code Map.get(K key)} is known
     * to be in the map.
     *
     * TODO: Document when this method returns true
     */
    private boolean isSuppressable(TreePath path) {
        MethodInvocationTree tree = (MethodInvocationTree)path.getLeaf();
        Element elt = getSite(tree);

        if (elt instanceof VariableElement
            && tree.getArguments().get(0) instanceof IdentifierTree
            && isKeyInMap((IdentifierTree)tree.getArguments().get(0),
                          (VariableElement)elt)) {
            return true;
        }

        if (elt instanceof VariableElement) {
            ExpressionTree arg = tree.getArguments().get(0);
            return keyForInMap(arg, elt, path)
                || keyForInMap(arg, ((VariableElement)elt).getSimpleName().toString())
                || keyForInMap(arg, String.valueOf(TreeUtils.getReceiverTree(tree)));
        }

        return false;
    }

    /**
     * Returns true if the key is a member of the specified map
     */
    private boolean keyForInMap(ExpressionTree key,
            String mapName) {
        AnnotatedTypeMirror keyForType = keyForFactory.getAnnotatedType(key);

        AnnotationMirror anno = keyForType.getAnnotation(KeyFor.class);
        if (anno == null)
            return false;

        List<String> maps = AnnotationUtils.parseStringArrayValue(anno, "value");

        return maps.contains(mapName);
    }

    private boolean keyForInMap(ExpressionTree key,
            Element mapElement, TreePath path) {
        AnnotatedTypeMirror keyForType = keyForFactory.getAnnotatedType(key);

        AnnotationMirror anno = keyForType.getAnnotation(KeyFor.class);
        if (anno == null)
            return false;

        List<String> maps = AnnotationUtils.parseStringArrayValue(anno, "value");
        for (String map: maps) {
            Element elt = resolver.findVariable(map, path);
            if (elt.equals(mapElement) &&
                    !isSiteRequired(TreeUtils.getReceiverTree((ExpressionTree)path.getLeaf()), elt)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper function to determine if the passed element is sufficient
     * to resolve a reference at compile time, without needing to
     * represent the call/dereference site.
     */
    private boolean isSiteRequired(ExpressionTree node, Element elt) {
        boolean r = ElementUtils.isStatic(elt) ||
            !elt.getKind().isField() ||
            factory.isMostEnclosingThisDeref(node);
        return !r;
    }

    /**
     * Case 1: get() is within true clause of map.containsKey()
     */
    public Matcher inContains(final Element key, final VariableElement map) {
        return or(whenTrue(new Matcher() {
            @Override public Boolean visitMethodInvocation(MethodInvocationTree node, Void p) {
                return isInvocationOfContains(key, map, node);
            }
        }), withIn(ofKind(Tree.Kind.CONDITIONAL_EXPRESSION, new Matcher() {
            @Override public Boolean visitConditionalExpression(ConditionalExpressionTree tree, Void p) {
                return isInvocationOfContains(key, map, tree.getCondition());
            }
        })));
    }

    /**
     * Case 2: get() is within enhanced for-loop over the keys
     */
    private Matcher inForEnhanced(final Element key,
            final VariableElement map) {
        return withIn(ofKind(Tree.Kind.ENHANCED_FOR_LOOP, new Matcher() {
            @Override public Boolean visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
                if (key.equals(TreeUtils.elementFromDeclaration(tree.getVariable())))
                    return visit(tree.getExpression(), p);
                return false;
            }

            @Override public Boolean visitMethodInvocation(MethodInvocationTree tree, Void p) {
                return (TreeUtils.isMethodInvocation(tree, mapKeySet, env) && map.equals(getSite(tree)));
            }
        }));
    }

    /**
     * Case 3: get() is preceded with an assert
     */
    private Matcher preceededByAssert(final Element key, final VariableElement map) {
        return preceededBy(ofKind(Tree.Kind.ASSERT, new Matcher() {
            @Override public Boolean visitAssert(AssertTree tree, Void p) {
                return isInvocationOfContains(key, map, tree.getCondition())
                    || isCheckOfGet(key, map, tree.getCondition());
            }
        }));
    }

    private boolean isTerminating(StatementTree tree) {
        StatementTree first = firstStatement(tree);
        if (first instanceof ThrowTree)
            return true;
        if (first instanceof ReturnTree)
            return true;

        if (first instanceof IfTree) {
            IfTree ifTree = (IfTree)first;
            if (ifTree.getElseStatement() != null
                && isTerminating(ifTree.getThenStatement())
                && isTerminating(ifTree.getElseStatement()))
                return true;
        }

        return false;
    }

    /**
     * Case 4: get() is preceded with explicit assertion
     */
    private Matcher preceededByExplicitAssert(final Element key,
            final VariableElement map) {
        return preceededBy(ofKind(Tree.Kind.IF, new Matcher() {
            @Override public Boolean visitIf(IfTree tree, Void p) {
                return (isNotContained(key, map, tree.getCondition())
                    && isTerminating(tree.getThenStatement()));
            }
        }));
    }

    /**
     * Case 5: get() is preceded by put-if-absent pattern
     */
    private Matcher preceededByIfThenPut(final Element key, final VariableElement map) {
        return preceededBy(ofKind(Tree.Kind.IF, new Matcher() {
            @Override public Boolean visitIf(IfTree tree, Void p) {
                if (isNotContained(key, map, tree.getCondition())) {
                    StatementTree first = firstStatement(tree.getThenStatement());
                    if (first != null
                        && first.getKind() == Tree.Kind.EXPRESSION_STATEMENT
                        && isInvocationOfPut(key, map, ((ExpressionStatementTree)first).getExpression())) {
                        return true;
                    }
                }
                return false;
            }
        }));
    }

    private Matcher keyInMatcher(Element key, VariableElement map) {
        return or(inContains(key, map),
                inForEnhanced(key, map),
                preceededByAssert(key, map),
                preceededByExplicitAssert(key, map),
                preceededByIfThenPut(key, map)
                );
    }
    /**
     * Checks for the supported patterns, and determines if we can
     * infer that the queried key exists in the map
     *
     * @param keyTree  the argument passed to {@code Map.get()}
     * @param map   the symbol of map
     * @return  true if key is in the map
     */
    private boolean isKeyInMap(IdentifierTree keyTree, VariableElement map) {
        TreePath path = factory.getPath(keyTree);
        Element key = TreeUtils.elementFromUse(keyTree);

        return keyInMatcher(key, map).match(path);
    }

    private Element getSite(MethodInvocationTree tree) {
        AnnotatedDeclaredType type =
            (AnnotatedDeclaredType)factory.getReceiverType(tree);
        return type.getElement();
    }

    private boolean isInvocationOfContains(Element key, VariableElement map, ExpressionTree tree) {
        if (TreeUtils.skipParens(tree) instanceof MethodInvocationTree) {
            MethodInvocationTree invok = (MethodInvocationTree)TreeUtils.skipParens(tree);
            if (TreeUtils.isMethodInvocation(invok, mapContains, env)) {
                Element containsArgument = InternalUtils.symbol(invok.getArguments().get(0));
                if (key.equals(containsArgument) && map.equals(getSite(invok)))
                    return true;
            }
        }
        return false;
    }

    private boolean isInvocationOfPut(Element key, VariableElement map, ExpressionTree tree) {
        if (TreeUtils.skipParens(tree) instanceof MethodInvocationTree) {
            MethodInvocationTree invok = (MethodInvocationTree)TreeUtils.skipParens(tree);
            if (TreeUtils.isMethodInvocation(invok, mapPut, env)) {
                Element containsArgument = InternalUtils.symbol(invok.getArguments().get(0));
                if (key.equals(containsArgument) && map.equals(getSite(invok)))
                    return true;
            }
        }
        return false;
    }

    private boolean isNotContained(Element key, VariableElement map, ExpressionTree tree) {
        tree = TreeUtils.skipParens(tree);
        return (tree.getKind() == Tree.Kind.LOGICAL_COMPLEMENT
                && isInvocationOfContains(key, map, ((UnaryTree)tree).getExpression()));
    }

    private StatementTree firstStatement(StatementTree tree) {
        StatementTree first = tree;
        while (first.getKind() == Tree.Kind.BLOCK) {
            List<? extends StatementTree> trees = ((BlockTree)first).getStatements();
            if (trees.isEmpty())
                return null;
            else
                first = trees.iterator().next();
        }
        return first;
    }

    private boolean isCheckOfGet(Element key, VariableElement map, ExpressionTree tree) {
        tree = TreeUtils.skipParens(tree);
        if (tree.getKind() != Tree.Kind.NOT_EQUAL_TO
            || ((BinaryTree)tree).getRightOperand().getKind() != Tree.Kind.NULL_LITERAL)
            return false;

        Tree right = TreeUtils.skipParens(((BinaryTree)tree).getLeftOperand());
        if (right instanceof MethodInvocationTree) {
            MethodInvocationTree invok = (MethodInvocationTree)right;
            if (TreeUtils.isMethodInvocation(invok, mapGet, env)) {
                Element containsArgument = InternalUtils.symbol(invok.getArguments().get(0));
                if (key.equals(containsArgument) && map.equals(getSite(invok)))
                    return true;
            }
        }
        return false;
    }
}
