package checkers.nullness;

import static checkers.util.Heuristics.Matchers.ofKind;
import static checkers.util.Heuristics.Matchers.or;
import static checkers.util.Heuristics.Matchers.preceededBy;
import static checkers.util.Heuristics.Matchers.whenTrue;
import static checkers.util.Heuristics.Matchers.withIn;

import checkers.nullness.quals.KeyFor;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.Heuristics.Matcher;
import checkers.util.Resolver2;

import javacutils.AnnotationUtils;
import javacutils.ElementUtils;
import javacutils.InternalUtils;
import javacutils.TreeUtils;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import com.sun.source.tree.AssertTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;

/**
 * Utility class for handling {@code Map.get()} invocations, and setting the
 * result to @NonNull or @Nullable.  Does not handle @KeyFor.
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

    private final ProcessingEnvironment processingEnv;
    private final NullnessAnnotatedTypeFactory atypeFactory;
    private final AnnotatedTypeFactory keyForFactory;
    private final Resolver2 resolver;

    private final ExecutableElement mapGet;
    private final ExecutableElement mapPut;
    private final ExecutableElement mapKeySet;
    private final ExecutableElement mapContains;

    public MapGetHeuristics(ProcessingEnvironment env,
            NullnessAnnotatedTypeFactory factory,
            AnnotatedTypeFactory keyForFactory) {
        this.processingEnv = env;
        this.atypeFactory = factory;
        this.keyForFactory = keyForFactory;
        this.resolver = new Resolver2(env);

        mapGet = TreeUtils.getMethod("java.util.Map", "get", 1, env);
        mapPut = TreeUtils.getMethod("java.util.Map", "put", 2, env);
        mapKeySet = TreeUtils.getMethod("java.util.Map", "keySet", 0, env);
        mapContains = TreeUtils.getMethod("java.util.Map", "containsKey", 1, env);
    }

    /**
     * Main entry point:  set the annotation on the type of the expression
     * represented by path.
     * @param path a path to a method invocation
     */
    public void handle(TreePath path, AnnotatedExecutableType method) {
        try {
            MethodInvocationTree tree = (MethodInvocationTree) path.getLeaf();
            if (TreeUtils.isMethodInvocation(tree, mapGet, processingEnv)) {
                AnnotatedTypeMirror type = method.getReturnType();
                if (mapGetReturnsNonNull(path)) {
                    type.replaceAnnotation(atypeFactory.NONNULL);
                } else {
                    type.replaceAnnotation(atypeFactory.NULLABLE);
                }
            }
        } catch (Throwable t) {
            // TODO: this is an ugly hack to suppress some problems in Resolver2
            // that cause an exception. See tests/nullness/KeyFors.java for an
            // example that might be affected.
        }
    }

    /**
     * Checks whether the key passed to {@code Map.get(K key)} is known
     * to be in the map.
     *
     * TODO: Document when this method returns true
     * @param a path to an invocation of Map.get
     */
    private boolean mapGetReturnsNonNull(TreePath path) {
        MethodInvocationTree tree = (MethodInvocationTree)path.getLeaf();
        Element receiver = getReceiver(tree);

        if (receiver instanceof VariableElement) {
            VariableElement rvar = (VariableElement)receiver;

            ExpressionTree arg = tree.getArguments().get(0);

            if (arg instanceof IdentifierTree
                && isKeyInMap((IdentifierTree)arg, rvar)) {
                return true;
            }

            return keyForInMap(arg, receiver, path)
                || keyForInMap(arg, rvar.getSimpleName().toString())
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

        List<String> maps = AnnotationUtils.getElementValueArray(anno, "value", String.class, false);

        return maps.contains(mapName);
    }

    private boolean keyForInMap(ExpressionTree key,
            Element mapElement, TreePath path) {
        AnnotatedTypeMirror keyForType = keyForFactory.getAnnotatedType(key);

        AnnotationMirror anno = keyForType.getAnnotation(KeyFor.class);
        if (anno == null)
            return false;

        List<String> maps = AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
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
            atypeFactory.isMostEnclosingThisDeref(node);
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
                return (TreeUtils.isMethodInvocation(tree, mapKeySet, processingEnv) && map.equals(getReceiver(tree)));
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
        TreePath path = atypeFactory.getPath(keyTree);
        Element key = TreeUtils.elementFromUse(keyTree);

        return keyInMatcher(key, map).match(path);
    }

    /** Given a method invocation tree, return the Element for its receiver. */
    private Element getReceiver(MethodInvocationTree tree) {
        Element element = InternalUtils.symbol(tree);
        assert element != null : "Unexpected null element for tree: " + tree;
        // Return null if the element kind has no receiver.
        if (!ElementUtils.hasReceiver(element)) {
            return null;
        }
        ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
        Element rcvelem = InternalUtils.symbol(receiver);
        return rcvelem;
    }

    private boolean isInvocationOf(ExecutableElement method, Element key, VariableElement map, ExpressionTree tree) {
        if (TreeUtils.skipParens(tree) instanceof MethodInvocationTree) {
            MethodInvocationTree invok = (MethodInvocationTree)TreeUtils.skipParens(tree);
            if (TreeUtils.isMethodInvocation(invok, method, processingEnv)) {
                Element containsArgument = InternalUtils.symbol(invok.getArguments().get(0));
                if (key.equals(containsArgument) && map.equals(getReceiver(invok)))
                    return true;
            }
        }
        return false;
    }

    private boolean isInvocationOfContains(Element key, VariableElement map, ExpressionTree tree) {
        return isInvocationOf(mapContains, key, map, tree);
    }

    private boolean isInvocationOfPut(Element key, VariableElement map, ExpressionTree tree) {
        return isInvocationOf(mapPut, key, map, tree);
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
            if (TreeUtils.isMethodInvocation(invok, mapGet, processingEnv)) {
                Element containsArgument = InternalUtils.symbol(invok.getArguments().get(0));
                if (key.equals(containsArgument) && map.equals(getReceiver(invok)))
                    return true;
            }
        }
        return false;
    }
}
