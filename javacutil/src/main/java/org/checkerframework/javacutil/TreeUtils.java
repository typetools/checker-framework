package org.checkerframework.javacutil;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotatedType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCLambda.ParameterKind;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A utility class made for helping to analyze a given {@code Tree}. */
// TODO: This class needs significant restructuring
public final class TreeUtils {

    // Class cannot be instantiated.
    private TreeUtils() {
        throw new AssertionError("Class TreeUtils cannot be instantiated.");
    }

    /**
     * Checks if the provided method is a constructor method or no.
     *
     * @param tree a tree defining the method
     * @return true iff tree describes a constructor
     */
    public static boolean isConstructor(final MethodTree tree) {
        return tree.getName().contentEquals("<init>");
    }

    /**
     * Checks if the method invocation is a call to super.
     *
     * @param tree a tree defining a method invocation
     * @return true iff tree describes a call to super
     */
    public static boolean isSuperConstructorCall(MethodInvocationTree tree) {
        return isNamedMethodCall("super", tree);
    }

    /**
     * Checks if the method invocation is a call to "this".
     *
     * @param tree a tree defining a method invocation
     * @return true iff tree describes a call to this
     */
    public static boolean isThisConstructorCall(MethodInvocationTree tree) {
        return isNamedMethodCall("this", tree);
    }

    /**
     * Checks if the method call is a call to the given method name.
     *
     * @param name a method name
     * @param tree a tree defining a method invocation
     * @return true iff tree is a call to the given method
     */
    private static boolean isNamedMethodCall(String name, MethodInvocationTree tree) {
        return getMethodName(tree.getMethodSelect()).equals(name);
    }

    /**
     * Returns true if the tree is a tree that 'looks like' either an access of a field or an
     * invocation of a method that are owned by the same accessing instance.
     *
     * <p>It would only return true if the access tree is of the form:
     *
     * <pre>
     *   field
     *   this.field
     *
     *   method()
     *   this.method()
     * </pre>
     *
     * It does not perform any semantical check to differentiate between fields and local variables;
     * local methods or imported static methods.
     *
     * @param tree expression tree representing an access to object member
     * @return {@code true} iff the member is a member of {@code this} instance
     */
    public static boolean isSelfAccess(final ExpressionTree tree) {
        ExpressionTree tr = TreeUtils.withoutParens(tree);
        // If method invocation check the method select
        if (tr.getKind() == Tree.Kind.ARRAY_ACCESS) {
            return false;
        }

        if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
            tr = ((MethodInvocationTree) tree).getMethodSelect();
        }
        tr = TreeUtils.withoutParens(tr);
        if (tr.getKind() == Tree.Kind.TYPE_CAST) {
            tr = ((TypeCastTree) tr).getExpression();
        }
        tr = TreeUtils.withoutParens(tr);

        if (tr.getKind() == Tree.Kind.IDENTIFIER) {
            return true;
        }

        if (tr.getKind() == Tree.Kind.MEMBER_SELECT) {
            tr = ((MemberSelectTree) tr).getExpression();
            if (tr.getKind() == Tree.Kind.IDENTIFIER) {
                Name ident = ((IdentifierTree) tr).getName();
                return ident.contentEquals("this") || ident.contentEquals("super");
            }
        }

        return false;
    }

    /**
     * Gets the first enclosing tree in path, of the specified kind.
     *
     * @param path the path defining the tree node
     * @param kind the kind of the desired tree
     * @return the enclosing tree of the given type as given by the path
     */
    public static Tree enclosingOfKind(final TreePath path, final Tree.Kind kind) {
        return enclosingOfKind(path, EnumSet.of(kind));
    }

    /**
     * Gets the first enclosing tree in path, with any one of the specified kinds.
     *
     * @param path the path defining the tree node
     * @param kinds the set of kinds of the desired tree
     * @return the enclosing tree of the given type as given by the path
     */
    public static Tree enclosingOfKind(final TreePath path, final Set<Tree.Kind> kinds) {
        TreePath p = path;

        while (p != null) {
            Tree leaf = p.getLeaf();
            assert leaf != null; /*nninvariant*/
            if (kinds.contains(leaf.getKind())) {
                return leaf;
            }
            p = p.getParentPath();
        }

        return null;
    }

    /**
     * Gets path to the first enclosing class tree, where class is defined by the classTreeKinds
     * method.
     *
     * @param path the path defining the tree node
     * @return the path to the enclosing class tree
     */
    public static TreePath pathTillClass(final TreePath path) {
        return pathTillOfKind(path, classTreeKinds());
    }

    /**
     * Gets path to the first enclosing tree of the specified kind.
     *
     * @param path the path defining the tree node
     * @param kind the kind of the desired tree
     * @return the path to the enclosing tree of the given type
     */
    public static TreePath pathTillOfKind(final TreePath path, final Tree.Kind kind) {
        return pathTillOfKind(path, EnumSet.of(kind));
    }

    /**
     * Gets path to the first enclosing tree with any one of the specified kinds.
     *
     * @param path the path defining the tree node
     * @param kinds the set of kinds of the desired tree
     * @return the path to the enclosing tree of the given type
     */
    public static TreePath pathTillOfKind(final TreePath path, final Set<Tree.Kind> kinds) {
        TreePath p = path;

        while (p != null) {
            Tree leaf = p.getLeaf();
            assert leaf != null; /*nninvariant*/
            if (kinds.contains(leaf.getKind())) {
                return p;
            }
            p = p.getParentPath();
        }

        return null;
    }

    /**
     * Gets the first enclosing tree in path, of the specified class.
     *
     * @param path the path defining the tree node
     * @param treeClass the class of the desired tree
     * @return the enclosing tree of the given type as given by the path
     */
    public static <T extends Tree> T enclosingOfClass(
            final TreePath path, final Class<T> treeClass) {
        TreePath p = path;

        while (p != null) {
            Tree leaf = p.getLeaf();
            if (treeClass.isInstance(leaf)) {
                return treeClass.cast(leaf);
            }
            p = p.getParentPath();
        }

        return null;
    }

    /**
     * Gets the enclosing class of the tree node defined by the given {@link TreePath}. It returns a
     * {@link Tree}, from which {@code checkers.types.AnnotatedTypeMirror} or {@link Element} can be
     * obtained.
     *
     * @param path the path defining the tree node
     * @return the enclosing class (or interface) as given by the path, or null if one does not
     *     exist
     */
    public static @Nullable ClassTree enclosingClass(final @Nullable TreePath path) {
        return (ClassTree) enclosingOfKind(path, classTreeKinds());
    }

    /**
     * Gets the enclosing variable of a tree node defined by the given {@link TreePath}.
     *
     * @param path the path defining the tree node
     * @return the enclosing variable as given by the path, or null if one does not exist
     */
    public static VariableTree enclosingVariable(final TreePath path) {
        return (VariableTree) enclosingOfKind(path, Tree.Kind.VARIABLE);
    }

    /**
     * Gets the enclosing method of the tree node defined by the given {@link TreePath}. It returns
     * a {@link Tree}, from which an {@code checkers.types.AnnotatedTypeMirror} or {@link Element}
     * can be obtained.
     *
     * @param path the path defining the tree node
     * @return the enclosing method as given by the path, or null if one does not exist
     */
    public static @Nullable MethodTree enclosingMethod(final @Nullable TreePath path) {
        return (MethodTree) enclosingOfKind(path, Tree.Kind.METHOD);
    }

    /**
     * Gets the enclosing method or lambda expression of the tree node defined by the given {@link
     * TreePath}. It returns a {@link Tree}, from which an {@code
     * checkers.types.AnnotatedTypeMirror} or {@link Element} can be obtained.
     *
     * @param path the path defining the tree node
     * @return the enclosing method or lambda as given by the path, or null if one does not exist
     */
    public static @Nullable Tree enclosingMethodOrLambda(final @Nullable TreePath path) {
        return enclosingOfKind(path, EnumSet.of(Tree.Kind.METHOD, Kind.LAMBDA_EXPRESSION));
    }

    public static @Nullable BlockTree enclosingTopLevelBlock(TreePath path) {
        TreePath parpath = path.getParentPath();
        while (parpath != null && !classTreeKinds.contains(parpath.getLeaf().getKind())) {
            path = parpath;
            parpath = parpath.getParentPath();
        }
        if (path.getLeaf().getKind() == Tree.Kind.BLOCK) {
            return (BlockTree) path.getLeaf();
        }
        return null;
    }

    /**
     * If the given tree is a parenthesized tree, return the enclosed non-parenthesized tree.
     * Otherwise, return the same tree.
     *
     * @param tree an expression tree
     * @return the outermost non-parenthesized tree enclosed by the given tree
     */
    public static ExpressionTree withoutParens(final ExpressionTree tree) {
        ExpressionTree t = tree;
        while (t.getKind() == Tree.Kind.PARENTHESIZED) {
            t = ((ParenthesizedTree) t).getExpression();
        }
        return t;
    }

    /**
     * Gets the first enclosing tree in path, that is not a parenthesis.
     *
     * @param path the path defining the tree node
     * @return a pair of a non-parenthesis tree that contains the argument, and its child that is
     *     the argument or is a parenthesized version of it
     */
    public static Pair<Tree, Tree> enclosingNonParen(final TreePath path) {
        TreePath parentPath = path.getParentPath();
        Tree enclosing = parentPath.getLeaf();
        Tree enclosingChild = path.getLeaf();
        while (enclosing.getKind() == Kind.PARENTHESIZED) {
            parentPath = parentPath.getParentPath();
            enclosingChild = enclosing;
            enclosing = parentPath.getLeaf();
        }
        return Pair.of(enclosing, enclosingChild);
    }

    /**
     * Returns the tree with the assignment context for the treePath leaf node. (Does not handle
     * pseudo-assignment of an argument to a parameter or a receiver expression to a receiver.)
     *
     * <p>The assignment context for the {@code treePath} is the leaf of its parent, if the parent
     * is one of the following trees:
     *
     * <ul>
     *   <li>AssignmentTree
     *   <li>CompoundAssignmentTree
     *   <li>MethodInvocationTree
     *   <li>NewArrayTree
     *   <li>NewClassTree
     *   <li>ReturnTree
     *   <li>VariableTree
     * </ul>
     *
     * If the parent is a ConditionalExpressionTree we need to distinguish two cases: If the leaf is
     * either the then or else branch of the ConditionalExpressionTree, then recurse on the parent.
     * If the leaf is the condition of the ConditionalExpressionTree, then return null to not
     * consider this assignment context.
     *
     * <p>If the leaf is a ParenthesizedTree, then recurse on the parent.
     *
     * <p>Otherwise, null is returned.
     *
     * @return the assignment context as described
     */
    public static Tree getAssignmentContext(final TreePath treePath) {
        TreePath parentPath = treePath.getParentPath();

        if (parentPath == null) {
            return null;
        }

        Tree parent = parentPath.getLeaf();
        switch (parent.getKind()) {
            case PARENTHESIZED:
                return getAssignmentContext(parentPath);
            case CONDITIONAL_EXPRESSION:
                ConditionalExpressionTree cet = (ConditionalExpressionTree) parent;
                if (cet.getCondition() == treePath.getLeaf()) {
                    // The assignment context for the condition is simply boolean.
                    // No point in going on.
                    return null;
                }
                // Otherwise use the context of the ConditionalExpressionTree.
                return getAssignmentContext(parentPath);
            case ASSIGNMENT:
            case METHOD_INVOCATION:
            case NEW_ARRAY:
            case NEW_CLASS:
            case RETURN:
            case VARIABLE:
                return parent;
            default:
                // 11 Tree.Kinds are CompoundAssignmentTrees,
                // so use instanceof rather than listing all 11.
                if (parent instanceof CompoundAssignmentTree) {
                    return parent;
                }
                return null;
        }
    }

    /**
     * Gets the {@link Element} for the given Tree API node. For an object instantiation returns the
     * value of the {@link JCNewClass#constructor} field. Note that this result might differ from
     * the result of {@link TreeUtils#constructor(NewClassTree)}.
     *
     * @param tree the {@link Tree} node to get the symbol for
     * @throws IllegalArgumentException if {@code tree} is null or is not a valid javac-internal
     *     tree (JCTree)
     * @return the {@link Symbol} for the given tree, or null if one could not be found
     */
    public static @Nullable Element elementFromTree(Tree tree) {
        if (tree == null) {
            throw new BugInCF("InternalUtils.symbol: tree is null");
        }

        if (!(tree instanceof JCTree)) {
            throw new BugInCF("InternalUtils.symbol: tree is not a valid Javac tree");
        }

        if (isExpressionTree(tree)) {
            tree = withoutParens((ExpressionTree) tree);
        }

        switch (tree.getKind()) {
            case VARIABLE:
            case METHOD:
            case CLASS:
            case ENUM:
            case INTERFACE:
            case ANNOTATION_TYPE:
            case TYPE_PARAMETER:
                return TreeInfo.symbolFor((JCTree) tree);

                // symbol() only works on MethodSelects, so we need to get it manually
                // for method invocations.
            case METHOD_INVOCATION:
                return TreeInfo.symbol(((JCMethodInvocation) tree).getMethodSelect());

            case ASSIGNMENT:
                return TreeInfo.symbol((JCTree) ((AssignmentTree) tree).getVariable());

            case ARRAY_ACCESS:
                return elementFromTree(((ArrayAccessTree) tree).getExpression());

            case NEW_CLASS:
                return ((JCNewClass) tree).constructor;

            case MEMBER_REFERENCE:
                // TreeInfo.symbol, which is used in the default case, didn't handle
                // member references until JDK8u20. So handle it here.
                return ((JCMemberReference) tree).sym;

            default:
                return TreeInfo.symbol((JCTree) tree);
        }
    }

    /**
     * Gets the element for a class corresponding to a declaration.
     *
     * @return the element for the given class
     */
    public static final TypeElement elementFromDeclaration(ClassTree node) {
        TypeElement elt = (TypeElement) TreeUtils.elementFromTree(node);
        return elt;
    }

    /**
     * Gets the element for a method corresponding to a declaration.
     *
     * @return the element for the given method
     */
    public static final ExecutableElement elementFromDeclaration(MethodTree node) {
        ExecutableElement elt = (ExecutableElement) TreeUtils.elementFromTree(node);
        return elt;
    }

    /**
     * Gets the element for a variable corresponding to its declaration.
     *
     * @return the element for the given variable
     */
    public static final VariableElement elementFromDeclaration(VariableTree node) {
        VariableElement elt = (VariableElement) TreeUtils.elementFromTree(node);
        return elt;
    }

    /**
     * Gets the element for the declaration corresponding to this use of an element. To get the
     * element for a declaration, use {@link #elementFromDeclaration(ClassTree)}, {@link
     * #elementFromDeclaration(MethodTree)}, or {@link #elementFromDeclaration(VariableTree)}
     * instead.
     *
     * <p>This method is just a wrapper around {@link TreeUtils#elementFromTree(Tree)}, but this
     * class might be the first place someone looks for this functionality.
     *
     * @param node the tree corresponding to a use of an element
     * @return the element for the corresponding declaration
     */
    public static final Element elementFromUse(ExpressionTree node) {
        return TreeUtils.elementFromTree(node);
    }

    // Specialization for return type.
    // Might return null if element wasn't found.
    public static final ExecutableElement elementFromUse(MethodInvocationTree node) {
        Element el = TreeUtils.elementFromTree(node);
        if (el instanceof ExecutableElement) {
            return (ExecutableElement) el;
        } else {
            return null;
        }
    }

    /**
     * Specialization for return type. Might return null if element wasn't found.
     *
     * @see #constructor(NewClassTree)
     */
    public static final ExecutableElement elementFromUse(NewClassTree node) {
        Element el = TreeUtils.elementFromTree(node);
        if (el instanceof ExecutableElement) {
            return (ExecutableElement) el;
        } else {
            return null;
        }
    }

    /**
     * Determines the symbol for a constructor given an invocation via {@code new}.
     *
     * <p>If the tree is a declaration of an anonymous class, then method returns constructor that
     * gets invoked in the extended class, rather than the anonymous constructor implicitly added by
     * the constructor (JLS 15.9.5.1)
     *
     * @see #elementFromUse(NewClassTree)
     * @param tree the constructor invocation
     * @return the {@link ExecutableElement} corresponding to the constructor call in {@code tree}
     */
    public static ExecutableElement constructor(NewClassTree tree) {

        if (!(tree instanceof JCTree.JCNewClass)) {
            throw new BugInCF("InternalUtils.constructor: not a javac internal tree");
        }

        JCNewClass newClassTree = (JCNewClass) tree;

        if (tree.getClassBody() != null) {
            // anonymous constructor bodies should contain exactly one statement
            // in the form:
            //    super(arg1, ...)
            // or
            //    o.super(arg1, ...)
            //
            // which is a method invocation (!) to the actual constructor

            // the method call is guaranteed to return nonnull
            JCMethodDecl anonConstructor =
                    (JCMethodDecl) TreeInfo.declarationFor(newClassTree.constructor, newClassTree);
            assert anonConstructor != null;
            assert anonConstructor.body.stats.size() == 1;
            JCExpressionStatement stmt = (JCExpressionStatement) anonConstructor.body.stats.head;
            JCTree.JCMethodInvocation superInvok = (JCMethodInvocation) stmt.expr;
            return (ExecutableElement) TreeInfo.symbol(superInvok.meth);
        } else {
            Element e = newClassTree.constructor;
            return (ExecutableElement) e;
        }
    }

    /**
     * Determine whether the given ExpressionTree has an underlying element.
     *
     * @param node the ExpressionTree to test
     * @return whether the tree refers to an identifier, member select, or method invocation
     */
    public static final boolean isUseOfElement(ExpressionTree node) {
        node = TreeUtils.withoutParens(node);
        switch (node.getKind()) {
            case IDENTIFIER:
            case MEMBER_SELECT:
            case METHOD_INVOCATION:
            case NEW_CLASS:
                return true;
            default:
                return false;
        }
    }

    /** @return the name of the invoked method */
    public static final Name methodName(MethodInvocationTree node) {
        ExpressionTree expr = node.getMethodSelect();
        if (expr.getKind() == Tree.Kind.IDENTIFIER) {
            return ((IdentifierTree) expr).getName();
        } else if (expr.getKind() == Tree.Kind.MEMBER_SELECT) {
            return ((MemberSelectTree) expr).getIdentifier();
        }
        throw new BugInCF("TreeUtils.methodName: cannot be here: " + node);
    }

    /**
     * @return true if the first statement in the body is a self constructor invocation within a
     *     constructor
     */
    public static final boolean containsThisConstructorInvocation(MethodTree node) {
        if (!TreeUtils.isConstructor(node) || node.getBody().getStatements().isEmpty()) {
            return false;
        }

        StatementTree st = node.getBody().getStatements().get(0);
        if (!(st instanceof ExpressionStatementTree)
                || !(((ExpressionStatementTree) st).getExpression()
                        instanceof MethodInvocationTree)) {
            return false;
        }

        MethodInvocationTree invocation =
                (MethodInvocationTree) ((ExpressionStatementTree) st).getExpression();

        return "this".contentEquals(TreeUtils.methodName(invocation));
    }

    public static final Tree firstStatement(Tree tree) {
        Tree first;
        if (tree.getKind() == Tree.Kind.BLOCK) {
            BlockTree block = (BlockTree) tree;
            if (block.getStatements().isEmpty()) {
                first = block;
            } else {
                first = block.getStatements().iterator().next();
            }
        } else {
            first = tree;
        }
        return first;
    }

    /**
     * Determine whether the given class contains an explicit constructor.
     *
     * @param node a class tree
     * @return true, iff there is an explicit constructor
     */
    public static boolean hasExplicitConstructor(ClassTree node) {
        TypeElement elem = TreeUtils.elementFromDeclaration(node);

        for (ExecutableElement ee : ElementFilter.constructorsIn(elem.getEnclosedElements())) {
            MethodSymbol ms = (MethodSymbol) ee;
            long mod = ms.flags();

            if ((mod & Flags.SYNTHETIC) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the tree is of a diamond type. In contrast to the implementation in TreeInfo,
     * this version works on Trees.
     *
     * @see com.sun.tools.javac.tree.TreeInfo#isDiamond(JCTree)
     */
    public static final boolean isDiamondTree(Tree tree) {
        switch (tree.getKind()) {
            case ANNOTATED_TYPE:
                return isDiamondTree(((AnnotatedTypeTree) tree).getUnderlyingType());
            case PARAMETERIZED_TYPE:
                return ((ParameterizedTypeTree) tree).getTypeArguments().isEmpty();
            case NEW_CLASS:
                return isDiamondTree(((NewClassTree) tree).getIdentifier());
            default:
                return false;
        }
    }

    /** Returns true if the tree represents a {@code String} concatenation operation. */
    public static final boolean isStringConcatenation(Tree tree) {
        return (tree.getKind() == Tree.Kind.PLUS && TypesUtils.isString(TreeUtils.typeOf(tree)));
    }

    /** Returns true if the compound assignment tree is a string concatenation. */
    public static final boolean isStringCompoundConcatenation(CompoundAssignmentTree tree) {
        return (tree.getKind() == Tree.Kind.PLUS_ASSIGNMENT
                && TypesUtils.isString(TreeUtils.typeOf(tree)));
    }

    /**
     * Returns true if the node is a constant-time expression.
     *
     * <p>A tree is a constant-time expression if it is:
     *
     * <ol>
     *   <li>a literal tree
     *   <li>a reference to a final variable initialized with a compile time constant
     *   <li>a String concatenation of two compile time constants
     * </ol>
     */
    public static boolean isCompileTimeString(ExpressionTree node) {
        ExpressionTree tree = TreeUtils.withoutParens(node);
        if (tree instanceof LiteralTree) {
            return true;
        }

        if (TreeUtils.isUseOfElement(tree)) {
            Element elt = TreeUtils.elementFromUse(tree);
            return ElementUtils.isCompileTimeConstant(elt);
        } else if (TreeUtils.isStringConcatenation(tree)) {
            BinaryTree binOp = (BinaryTree) tree;
            return isCompileTimeString(binOp.getLeftOperand())
                    && isCompileTimeString(binOp.getRightOperand());
        } else {
            return false;
        }
    }

    /** Returns the receiver tree of a field access or a method invocation. */
    public static ExpressionTree getReceiverTree(ExpressionTree expression) {
        ExpressionTree receiver;
        switch (expression.getKind()) {
            case METHOD_INVOCATION:
                // Trying to handle receiver calls to trees of the form
                //     ((m).getArray())
                // returns the type of 'm' in this case
                receiver = ((MethodInvocationTree) expression).getMethodSelect();

                if (receiver.getKind() == Tree.Kind.MEMBER_SELECT) {
                    receiver = ((MemberSelectTree) receiver).getExpression();
                } else {
                    // It's a method call "m(foo)" without an explicit receiver
                    return null;
                }
                break;
            case NEW_CLASS:
                receiver = ((NewClassTree) expression).getEnclosingExpression();
                break;
            case ARRAY_ACCESS:
                receiver = ((ArrayAccessTree) expression).getExpression();
                break;
            case MEMBER_SELECT:
                receiver = ((MemberSelectTree) expression).getExpression();
                // Avoid int.class
                if (receiver instanceof PrimitiveTypeTree) {
                    return null;
                }
                break;
            case IDENTIFIER:
                // It's a field access on implicit this or a local variable/parameter.
                return null;
            default:
                return null;
        }

        return TreeUtils.withoutParens(receiver);
    }

    // TODO: What about anonymous classes?
    // Adding Tree.Kind.NEW_CLASS here doesn't work, because then a
    // tree gets cast to ClassTree when it is actually a NewClassTree,
    // for example in enclosingClass above.
    private static final Set<Tree.Kind> classTreeKinds =
            EnumSet.of(
                    Tree.Kind.CLASS,
                    Tree.Kind.ENUM,
                    Tree.Kind.INTERFACE,
                    Tree.Kind.ANNOTATION_TYPE);

    public static Set<Tree.Kind> classTreeKinds() {
        return classTreeKinds;
    }

    /**
     * Is the given tree kind a class, i.e. a class, enum, interface, or annotation type.
     *
     * @param tree the tree to test
     * @return true, iff the given kind is a class kind
     */
    public static boolean isClassTree(Tree tree) {
        return classTreeKinds().contains(tree.getKind());
    }

    private static final Set<Tree.Kind> typeTreeKinds =
            EnumSet.of(
                    Tree.Kind.PRIMITIVE_TYPE,
                    Tree.Kind.PARAMETERIZED_TYPE,
                    Tree.Kind.TYPE_PARAMETER,
                    Tree.Kind.ARRAY_TYPE,
                    Tree.Kind.UNBOUNDED_WILDCARD,
                    Tree.Kind.EXTENDS_WILDCARD,
                    Tree.Kind.SUPER_WILDCARD,
                    Tree.Kind.ANNOTATED_TYPE);

    public static Set<Tree.Kind> typeTreeKinds() {
        return typeTreeKinds;
    }

    /**
     * Is the given tree a type instantiation?
     *
     * <p>TODO: this is an under-approximation: e.g. an identifier could be either a type use or an
     * expression. How can we distinguish.
     *
     * @param tree the tree to test
     * @return true, iff the given tree is a type
     */
    public static boolean isTypeTree(Tree tree) {
        return typeTreeKinds().contains(tree.getKind());
    }

    /**
     * Returns true if the given element is an invocation of the method, or of any method that
     * overrides that one.
     */
    public static boolean isMethodInvocation(
            Tree tree, ExecutableElement method, ProcessingEnvironment env) {
        if (!(tree instanceof MethodInvocationTree)) {
            return false;
        }
        MethodInvocationTree methInvok = (MethodInvocationTree) tree;
        ExecutableElement invoked = TreeUtils.elementFromUse(methInvok);
        return ElementUtils.isMethod(invoked, method, env);
    }

    /**
     * Returns true if the argument is an invocation of one of the given methods, or of any method
     * that overrides them.
     */
    public static boolean isMethodInvocation(
            Tree methodTree, List<ExecutableElement> methods, ProcessingEnvironment processingEnv) {
        if (!(methodTree instanceof MethodInvocationTree)) {
            return false;
        }
        for (ExecutableElement Method : methods) {
            if (isMethodInvocation(methodTree, Method, processingEnv)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the ExecutableElement for the method declaration of methodName, in class typeName,
     * with params formal parameters. Errs if there is not exactly one matching method. If more than
     * one method takes the same number of formal parameters, then use {@link #getMethod(String,
     * String, ProcessingEnvironment, String...)}.
     */
    public static ExecutableElement getMethod(
            String typeName, String methodName, int params, ProcessingEnvironment env) {
        List<ExecutableElement> methods = getMethods(typeName, methodName, params, env);
        if (methods.size() == 1) {
            return methods.get(0);
        }
        throw new BugInCF(
                "TreeUtils.getMethod(%s, %s, %d): expected 1 match, found %d",
                typeName, methodName, params, methods.size());
    }

    /**
     * Returns all ExecutableElements for method declarations of methodName, in class typeName, with
     * params formal parameters.
     */
    public static List<ExecutableElement> getMethods(
            String typeName, String methodName, int params, ProcessingEnvironment env) {
        List<ExecutableElement> methods = new ArrayList<>(1);
        TypeElement typeElt = env.getElementUtils().getTypeElement(typeName);
        if (typeElt == null) {
            throw new UserError("Configuration problem! Could not load type: " + typeName);
        }
        for (ExecutableElement exec : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
            if (exec.getSimpleName().contentEquals(methodName)
                    && exec.getParameters().size() == params) {
                methods.add(exec);
            }
        }
        return methods;
    }

    /**
     * Returns the ExecutableElement for a method declaration of methodName, in class typeName, with
     * formal parameters of the given types. Errs if there is no matching method.
     */
    public static ExecutableElement getMethod(
            String typeName, String methodName, ProcessingEnvironment env, String... paramTypes) {
        TypeElement typeElt = env.getElementUtils().getTypeElement(typeName);
        for (ExecutableElement exec : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
            if (exec.getSimpleName().contentEquals(methodName)
                    && exec.getParameters().size() == paramTypes.length) {
                boolean typesMatch = true;
                List<? extends VariableElement> params = exec.getParameters();
                for (int i = 0; i < paramTypes.length; i++) {
                    VariableElement ve = params.get(i);
                    TypeMirror tm = TypeAnnotationUtils.unannotatedType(ve.asType());
                    if (!tm.toString().equals(paramTypes[i])) {
                        typesMatch = false;
                        break;
                    }
                }
                if (typesMatch) {
                    return exec;
                }
            }
        }
        throw new BugInCF(
                "TreeUtils.getMethod: found no match for "
                        + typeName
                        + "."
                        + methodName
                        + "("
                        + Arrays.toString(paramTypes)
                        + ")");
    }

    /**
     * Determine whether the given expression is either "this" or an outer "C.this".
     *
     * <p>TODO: Should this also handle "super"?
     */
    public static final boolean isExplicitThisDereference(ExpressionTree tree) {
        if (tree.getKind() == Tree.Kind.IDENTIFIER
                && ((IdentifierTree) tree).getName().contentEquals("this")) {
            // Explicit this reference "this"
            return true;
        }

        if (tree.getKind() != Tree.Kind.MEMBER_SELECT) {
            return false;
        }

        MemberSelectTree memSelTree = (MemberSelectTree) tree;
        if (memSelTree.getIdentifier().contentEquals("this")) {
            // Outer this reference "C.this"
            return true;
        }
        return false;
    }

    /**
     * Determine whether {@code tree} is a class literal, such as.
     *
     * <pre>
     *   <em>Object</em> . <em>class</em>
     * </pre>
     *
     * @return true iff if tree is a class literal
     */
    public static boolean isClassLiteral(Tree tree) {
        if (tree.getKind() != Tree.Kind.MEMBER_SELECT) {
            return false;
        }
        return "class".equals(((MemberSelectTree) tree).getIdentifier().toString());
    }

    /**
     * Determine whether {@code tree} is a field access expressions, such as.
     *
     * <pre>
     *   <em>f</em>
     *   <em>obj</em> . <em>f</em>
     * </pre>
     *
     * @return true iff if tree is a field access expression (implicit or explicit)
     */
    public static boolean isFieldAccess(Tree tree) {
        if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
            // explicit field access
            MemberSelectTree memberSelect = (MemberSelectTree) tree;
            Element el = TreeUtils.elementFromUse(memberSelect);
            return el.getKind().isField();
        } else if (tree.getKind() == Tree.Kind.IDENTIFIER) {
            // implicit field access
            IdentifierTree ident = (IdentifierTree) tree;
            Element el = TreeUtils.elementFromUse(ident);
            return el.getKind().isField()
                    && !ident.getName().contentEquals("this")
                    && !ident.getName().contentEquals("super");
        }
        return false;
    }

    /**
     * Compute the name of the field that the field access {@code tree} accesses. Requires {@code
     * tree} to be a field access, as determined by {@code isFieldAccess}.
     *
     * @return the name of the field accessed by {@code tree}.
     */
    public static String getFieldName(Tree tree) {
        assert isFieldAccess(tree);
        if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
            MemberSelectTree mtree = (MemberSelectTree) tree;
            return mtree.getIdentifier().toString();
        } else {
            IdentifierTree itree = (IdentifierTree) tree;
            return itree.getName().toString();
        }
    }

    /**
     * Determine whether {@code tree} refers to a method element, such as.
     *
     * <pre>
     *   <em>m</em>(...)
     *   <em>obj</em> . <em>m</em>(...)
     * </pre>
     *
     * @return true iff if tree is a method access expression (implicit or explicit)
     */
    public static boolean isMethodAccess(Tree tree) {
        if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
            // explicit method access
            MemberSelectTree memberSelect = (MemberSelectTree) tree;
            Element el = TreeUtils.elementFromUse(memberSelect);
            return el.getKind() == ElementKind.METHOD || el.getKind() == ElementKind.CONSTRUCTOR;
        } else if (tree.getKind() == Tree.Kind.IDENTIFIER) {
            // implicit method access
            IdentifierTree ident = (IdentifierTree) tree;
            // The field "super" and "this" are also legal methods
            if (ident.getName().contentEquals("super") || ident.getName().contentEquals("this")) {
                return true;
            }
            Element el = TreeUtils.elementFromUse(ident);
            return el.getKind() == ElementKind.METHOD || el.getKind() == ElementKind.CONSTRUCTOR;
        }
        return false;
    }

    /**
     * Compute the name of the method that the method access {@code tree} accesses. Requires {@code
     * tree} to be a method access, as determined by {@code isMethodAccess}.
     *
     * @return the name of the method accessed by {@code tree}.
     */
    public static String getMethodName(Tree tree) {
        assert isMethodAccess(tree);
        if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
            MemberSelectTree mtree = (MemberSelectTree) tree;
            return mtree.getIdentifier().toString();
        } else {
            IdentifierTree itree = (IdentifierTree) tree;
            return itree.getName().toString();
        }
    }

    /**
     * @return {@code true} if and only if {@code tree} can have a type annotation.
     *     <p>TODO: is this implementation precise enough? E.g. does a .class literal work
     *     correctly?
     */
    public static boolean canHaveTypeAnnotation(Tree tree) {
        return ((JCTree) tree).type != null;
    }

    /**
     * Returns true if and only if the given {@code tree} represents a field access of the given
     * {@link VariableElement}.
     */
    public static boolean isSpecificFieldAccess(Tree tree, VariableElement var) {
        if (tree instanceof MemberSelectTree) {
            MemberSelectTree memSel = (MemberSelectTree) tree;
            Element field = TreeUtils.elementFromUse(memSel);
            return field.equals(var);
        } else if (tree instanceof IdentifierTree) {
            IdentifierTree idTree = (IdentifierTree) tree;
            Element field = TreeUtils.elementFromUse(idTree);
            return field.equals(var);
        } else {
            return false;
        }
    }

    /**
     * Returns the VariableElement for a field declaration.
     *
     * @param typeName the class where the field is declared
     * @param fieldName the name of the field
     * @param env the processing environment
     * @return the VariableElement for typeName.fieldName
     */
    public static VariableElement getField(
            String typeName, String fieldName, ProcessingEnvironment env) {
        TypeElement mapElt = env.getElementUtils().getTypeElement(typeName);
        for (VariableElement var : ElementFilter.fieldsIn(mapElt.getEnclosedElements())) {
            if (var.getSimpleName().contentEquals(fieldName)) {
                return var;
            }
        }
        throw new BugInCF("TreeUtils.getField: shouldn't be here");
    }

    /**
     * Determine whether the given tree represents an ExpressionTree.
     *
     * @param tree the Tree to test
     * @return whether the tree is an ExpressionTree
     */
    public static boolean isExpressionTree(Tree tree) {
        // TODO: is there a nicer way than an instanceof?
        return tree instanceof ExpressionTree;
    }

    /**
     * @param node the method invocation to check
     * @return true if this is a super call to the {@link Enum} constructor
     */
    public static boolean isEnumSuper(MethodInvocationTree node) {
        ExecutableElement ex = TreeUtils.elementFromUse(node);
        Name name = ElementUtils.getQualifiedClassName(ex);
        boolean correctClass = "java.lang.Enum".contentEquals(name);
        boolean correctMethod = "<init>".contentEquals(ex.getSimpleName());
        return correctClass && correctMethod;
    }

    /**
     * Determine whether the given tree represents a declaration of a type (including type
     * parameters).
     *
     * @param node the Tree to test
     * @return true if the tree is a type declaration
     */
    public static boolean isTypeDeclaration(Tree node) {
        switch (node.getKind()) {
                // These tree kinds are always declarations.  Uses of the declared
                // types have tree kind IDENTIFIER.
            case ANNOTATION_TYPE:
            case CLASS:
            case ENUM:
            case INTERFACE:
            case TYPE_PARAMETER:
                return true;

            default:
                return false;
        }
    }

    /**
     * Returns whether or not the leaf of the tree path is in a static scope.
     *
     * @param path TreePath whose leaf may or may not be in static scope
     * @return returns whether or not the leaf of the tree path is in a static scope
     */
    public static boolean isTreeInStaticScope(TreePath path) {
        MethodTree enclosingMethod = TreeUtils.enclosingMethod(path);

        if (enclosingMethod != null) {
            return enclosingMethod.getModifiers().getFlags().contains(Modifier.STATIC);
        }
        // no enclosing method, check for static or initializer block
        BlockTree block = enclosingTopLevelBlock(path);
        if (block != null) {
            return block.isStatic();
        }

        // check if its in a variable initializer
        Tree t = enclosingVariable(path);
        if (t != null) {
            return ((VariableTree) t).getModifiers().getFlags().contains(Modifier.STATIC);
        }
        ClassTree classTree = enclosingClass(path);
        if (classTree != null) {
            return classTree.getModifiers().getFlags().contains(Modifier.STATIC);
        }
        return false;
    }

    /**
     * Returns whether or not tree is an access of array length.
     *
     * @param tree tree to check
     * @return true if tree is an access of array length
     */
    public static boolean isArrayLengthAccess(Tree tree) {
        if (tree.getKind() == Kind.MEMBER_SELECT
                && isFieldAccess(tree)
                && getFieldName(tree).equals("length")) {
            ExpressionTree expressionTree = ((MemberSelectTree) tree).getExpression();
            if (TreeUtils.typeOf(expressionTree).getKind() == TypeKind.ARRAY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether or not the node referred to by the given {@link TreePath} is an anonymous
     * constructor (the constructor for an anonymous class.
     *
     * @param method the {@link TreePath} for a node that may be an anonymous constructor
     * @return true if the given path points to an anonymous constructor, false if it does not
     */
    public static boolean isAnonymousConstructor(final MethodTree method) {
        @Nullable Element e = elementFromTree(method);
        if (!(e instanceof Symbol)) {
            return false;
        }

        if ((((@NonNull Symbol) e).flags() & Flags.ANONCONSTR) != 0) {
            return true;
        }

        return false;
    }

    public static final List<AnnotationMirror> annotationsFromTypeAnnotationTrees(
            List<? extends AnnotationTree> annos) {
        List<AnnotationMirror> annotations = new ArrayList<>(annos.size());
        for (AnnotationTree anno : annos) {
            annotations.add(TreeUtils.annotationFromAnnotationTree(anno));
        }
        return annotations;
    }

    public static AnnotationMirror annotationFromAnnotationTree(AnnotationTree tree) {
        return ((JCAnnotation) tree).attribute;
    }

    public static final List<? extends AnnotationMirror> annotationsFromTree(
            AnnotatedTypeTree node) {
        return annotationsFromTypeAnnotationTrees(((JCAnnotatedType) node).annotations);
    }

    public static final List<? extends AnnotationMirror> annotationsFromTree(
            TypeParameterTree node) {
        return annotationsFromTypeAnnotationTrees(((JCTypeParameter) node).annotations);
    }

    public static final List<? extends AnnotationMirror> annotationsFromArrayCreation(
            NewArrayTree node, int level) {

        assert node instanceof JCNewArray;
        final JCNewArray newArray = ((JCNewArray) node);

        if (level == -1) {
            return annotationsFromTypeAnnotationTrees(newArray.annotations);
        }

        if (newArray.dimAnnotations.length() > 0
                && (level >= 0)
                && (level < newArray.dimAnnotations.size())) {
            return annotationsFromTypeAnnotationTrees(newArray.dimAnnotations.get(level));
        }

        return Collections.emptyList();
    }

    /** @return true if the tree is the declaration or use of a local variable */
    public static boolean isLocalVariable(Tree tree) {
        if (tree.getKind() == Kind.VARIABLE) {
            return elementFromDeclaration((VariableTree) tree).getKind()
                    == ElementKind.LOCAL_VARIABLE;
        } else if (tree.getKind() == Kind.IDENTIFIER) {
            return elementFromUse((ExpressionTree) tree).getKind() == ElementKind.LOCAL_VARIABLE;
        }
        return false;
    }

    /** @return the type as a TypeMirror of {@code tree} */
    public static TypeMirror typeOf(Tree tree) {
        return ((JCTree) tree).type;
    }

    /**
     * The type of the lambda or method reference tree is a functional interface type. This method
     * returns the single abstract method declared by that functional interface. (The type of this
     * method is referred to as the function type.)
     *
     * @param tree lambda or member reference tree
     * @param env ProcessingEnvironment
     * @return the single abstract method declared by the type of the tree
     */
    public static Symbol findFunction(Tree tree, ProcessingEnvironment env) {
        Context ctx = ((JavacProcessingEnvironment) env).getContext();
        Types javacTypes = Types.instance(ctx);
        return javacTypes.findDescriptorSymbol(((Type) typeOf(tree)).asElement());
    }

    /**
     * Returns true if {@code tree} is an implicitly typed lambda.
     *
     * <p>A lambda expression whose formal type parameters have inferred types is an implicitly
     * typed lambda. (See JLS 15.27.1)
     *
     * @param tree any kind of tree
     * @return true iff {@code tree} is an implicitly typed lambda.
     */
    public static boolean isImplicitlyTypedLambda(Tree tree) {
        return tree.getKind() == Kind.LAMBDA_EXPRESSION
                && ((JCLambda) tree).paramKind == ParameterKind.IMPLICIT;
    }
}
