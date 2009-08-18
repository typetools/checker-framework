package checkers.nullness;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;

/**
 * Utilities class for handling {@code Map.get()} invocations.
 */
/*package-scope*/ class MapGetHeauristics {

    private final ProcessingEnvironment env;
    private final Elements elements;
    private final NullnessAnnotatedTypeFactory factory;

    private final ExecutableElement mapGet;
    private final ExecutableElement mapPut;
    private final ExecutableElement mapKeySet;
    private final ExecutableElement mapContains;

    public MapGetHeauristics(ProcessingEnvironment env,
            NullnessAnnotatedTypeFactory factory) {
        this.env = env;
        this.elements = env.getElementUtils();
        this.factory = factory;

        mapGet = getMethod("java.util.Map", "get", 1);
        mapPut = getMethod("java.util.Map", "put", 2);
        mapKeySet = getMethod("java.util.Map", "keySet", 0);
        mapContains = getMethod("java.util.Map", "containsKey", 1);
    }

    /**
     * @return true if the tree is a method invocation to a method overriding
     *        {@code Map.get(K)}
     */
    private boolean isMapGetInvocation(Tree tree) {
        return isMethod(tree, mapGet);
    }

    /**
     * Checks whether the key passed to {@code Map.get(K key)} is known
     * to be in the map.
     *
     * TODO: Document when this method returns true
     */
    private boolean isSuppressable(MethodInvocationTree tree) {
        Element elt = getSite(tree);
        return (elt instanceof VariableElement
                && tree.getArguments().get(0) instanceof IdentifierTree
                && isKeyInMap((IdentifierTree)tree.getArguments().get(0), (VariableElement)elt));
    }

    private boolean checkForContains(Element key, VariableElement map, TreePath path) {
        for (TreePath tp = path; tp != null; tp = tp.getParentPath()) {
            Tree.Kind kind = tp.getLeaf().getKind();
            if (kind != Tree.Kind.IF && kind != Tree.Kind.CONDITIONAL_EXPRESSION)
                continue;
            ExpressionTree condition =
                (kind == Tree.Kind.IF) ? ((IfTree)tp.getLeaf()).getCondition()
                        : ((ConditionalExpressionTree)tp.getLeaf()).getCondition();
            if (isInvocationOfContains(key, map, condition))
                return true;
        }
        return false;
    }

    private boolean isInvocationOfContains(Element key, VariableElement map, Tree tree) {
        if (TreeUtils.skipParens(tree) instanceof MethodInvocationTree) {
            MethodInvocationTree invok = (MethodInvocationTree)TreeUtils.skipParens(tree);
            if (isMethod(invok, mapContains)) {
                Element containsArgument = InternalUtils.symbol(invok.getArguments().get(0));
                if (key.equals(containsArgument) && map.equals(getSite(invok)))
                    return true;
            }
        }
        return false;
    }

    private boolean isInvocationOfPut(Element key, VariableElement map, Tree tree) {
        if (TreeUtils.skipParens(tree) instanceof MethodInvocationTree) {
            MethodInvocationTree invok = (MethodInvocationTree)TreeUtils.skipParens(tree);
            if (isMethod(invok, mapPut)) {
                Element containsArgument = InternalUtils.symbol(invok.getArguments().get(0));
                if (key.equals(containsArgument) && map.equals(getSite(invok)))
                    return true;
            }
        }
        return false;
    }

    private boolean isCheckOfGet(Element key, VariableElement map, Tree tree) {
        tree = TreeUtils.skipParens(tree);
        if (tree.getKind() != Tree.Kind.NOT_EQUAL_TO
            || ((BinaryTree)tree).getRightOperand().getKind() != Tree.Kind.NULL_LITERAL)
            return false;

        Tree right = TreeUtils.skipParens(((BinaryTree)tree).getLeftOperand());
        if (right instanceof MethodInvocationTree) {
            MethodInvocationTree invok = (MethodInvocationTree)right;
            if (isMethod(invok, mapGet)) {
                Element containsArgument = InternalUtils.symbol(invok.getArguments().get(0));
                if (key.equals(containsArgument) && map.equals(getSite(invok)))
                    return true;
            }
        }
        return false;
    }

    private boolean checkForEnhanced(Element key, VariableElement map, TreePath path) {
        for (TreePath tp = path; tp != null; tp = tp.getParentPath()) {
            if (tp.getLeaf().getKind() != Tree.Kind.ENHANCED_FOR_LOOP)
                continue;
            EnhancedForLoopTree forLoop = (EnhancedForLoopTree) tp.getLeaf();
            if (forLoop.getExpression() instanceof MethodInvocationTree) {
                MethodInvocationTree iterable = (MethodInvocationTree)forLoop.getExpression();
                if (key.equals(TreeUtils.elementFromDeclaration(forLoop.getVariable()))
                        && isMethod(iterable, mapKeySet)
                        && map.equals(getSite(iterable)))
                    return true;
            }
        }
        return false;
    }

    private boolean checkForAsserts(Element key, VariableElement map, TreePath path) {
        StatementTree stmt = TreeUtils.enclosingOfClass(path, StatementTree.class);
        if (stmt == null)
            return false;
        TreePath p = path;
        while (p.getLeaf() != stmt) p = p.getParentPath();
        assert p.getLeaf() == stmt;

        while (p != null && p.getLeaf() instanceof StatementTree) {
            if (p.getParentPath().getLeaf() instanceof BlockTree) {
                BlockTree block = (BlockTree)p.getParentPath().getLeaf();
                for (StatementTree st : block.getStatements()) {
                    if (st == p.getLeaf())
                        break;
                    if (st instanceof AssertTree
                            && (isInvocationOfContains(key, map, ((AssertTree)st).getCondition())
                                || isCheckOfGet(key, map, ((AssertTree)st).getCondition())))
                        return true;
                }
            }
            p = p.getParentPath();
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

    private boolean checkForIfExceptions(Element key, VariableElement map, TreePath path) {
        StatementTree stmt = TreeUtils.enclosingOfClass(path, StatementTree.class);
        if (stmt == null)
            return false;
        TreePath p = path;
        while (p.getLeaf() != stmt) p = p.getParentPath();
        assert p.getLeaf() == stmt;

        while (p != null && p.getLeaf() instanceof StatementTree) {
            if (p.getParentPath().getLeaf() instanceof BlockTree) {
                BlockTree block = (BlockTree)p.getParentPath().getLeaf();
                for (StatementTree st : block.getStatements()) {
                    if (st == p.getLeaf())
                        break;
                    if (st instanceof IfTree) {
                        IfTree ifTree = (IfTree)st;
                        if (isNotContained(key, map, ifTree.getCondition())) {
                            StatementTree first = firstStatement(ifTree.getThenStatement());
                            if (first instanceof ThrowTree)
                                return true;
                        }
                    }
                }
            }
            p = p.getParentPath();
        }
        return false;
    }

    private boolean checkForIfThenPut(Element key, VariableElement map, TreePath path) {
        StatementTree stmt = TreeUtils.enclosingOfClass(path, StatementTree.class);
        if (stmt == null)
            return false;
        TreePath p = path;
        while (p.getLeaf() != stmt) p = p.getParentPath();
        assert p.getLeaf() == stmt;

        while (p != null && p.getLeaf() instanceof StatementTree) {
            if (p.getParentPath().getLeaf() instanceof BlockTree) {
                BlockTree block = (BlockTree)p.getParentPath().getLeaf();
                for (StatementTree st : block.getStatements()) {
                    if (st == p.getLeaf())
                        break;
                    if (st instanceof IfTree) {
                        IfTree ifTree = (IfTree)st;
                        if (isNotContained(key, map, ifTree.getCondition())) {
                            StatementTree first = firstStatement(ifTree.getThenStatement());
                            if (first != null
                                && first.getKind() == Tree.Kind.EXPRESSION_STATEMENT
                                && isInvocationOfPut(key, map, ((ExpressionStatementTree)first).getExpression()))
                                return true;
                        }
                    }
                }
            }
            p = p.getParentPath();
        }
        return false;
    }

    private boolean isKeyInMap(IdentifierTree tree, VariableElement map) {
        TreePath path = factory.getPath(tree);
        Element key = TreeUtils.elementFromUse(tree);

        // Check for contains
        if (checkForContains(key, map, path)) {
            return true;
        }

        // Check for enhanced for loop
        if (checkForEnhanced(key, map, path)) {
            return true;
        }

        if (checkForAsserts(key, map, path)) {
            return true;
        }

        if (checkForIfExceptions(key, map, path)) {
            return true;
        }

        if (checkForIfThenPut(key, map, path)) {
            return true;
        }

        return false;
    }

    private Element getSite(MethodInvocationTree tree) {
        AnnotatedDeclaredType type =
            (AnnotatedDeclaredType)factory.getReceiver(tree);
        return type.getElement();
    }

    private boolean isMethod(Tree tree, ExecutableElement method) {
        if (!(tree instanceof MethodInvocationTree))
            return false;
        MethodInvocationTree methInvok = (MethodInvocationTree)tree;
        ExecutableElement invoked = TreeUtils.elementFromUse(methInvok);
        return isMethod(invoked, method);
    }

    private boolean isMethod(ExecutableElement questioned, ExecutableElement method) {
        return (questioned.equals(method)
                || env.getElementUtils().overrides(questioned, method,
                        (TypeElement)questioned.getEnclosingElement()));
    }

    private ExecutableElement getMethod(String typeName, String methodName, int params) {
        TypeElement mapElt = env.getElementUtils().getTypeElement(typeName);
        for (ExecutableElement exec : ElementFilter.methodsIn(mapElt.getEnclosedElements())) {
            if (exec.getSimpleName().contentEquals(methodName)
                    && exec.getParameters().size() == params)
                return exec;
        }
        throw new RuntimeException("Shouldn't be here!");
    }

    public void handle(MethodInvocationTree tree, AnnotatedExecutableType method) {
        if (isMapGetInvocation(tree)) {
            AnnotatedTypeMirror type = method.getReturnType();
            type.clearAnnotations();
            if (!isSuppressable((MethodInvocationTree)tree)) {
                type.addAnnotation(factory.NULLABLE);
            } else {
                type.addAnnotation(factory.NONNULL);
            }
        }
    }
}
