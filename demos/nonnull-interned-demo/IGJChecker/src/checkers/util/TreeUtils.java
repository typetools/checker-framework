package checkers.util;

import checkers.quals.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;

/**
 * A utility class made for helping to analyze a given {@code Tree}.
 */
// TODO: This class needs significant restructuring
@DefaultQualifier("checkers.nullness.quals.NonNull")
public final class TreeUtils {
    
    // Cannot be initialized
    private TreeUtils() { }

    /**
     * Checks if the provided method is a constructor method or no.
     * 
     * @param tree
     *            a tree defining the method
     * @return true iff tree describes a constructor
     */
    public static boolean isConstructor(final MethodTree tree) {
        return (tree.getName().contentEquals("<init>"));
    }
    
    /**
     * Returns {@code true} if the tree represents an access to a member of
     * the same accessing instance.
     * 
     * Performs a compile-time check and would not handle run-time references
     * to the receiver instance. Thus, in the case:
     * 
     * <code>
     *      Object selfReference = this;
     *      int hashCode = selfReference.hashCode()
     * </code>
     * 
     * the hashCode() invocation wouldn't be considered a self access.
     * 
     * @param tree  expression tree representing an access to object member
     * @return {@code true} iff the member is a member of {@code this} instance
     */
    public static boolean isSelfAccess(final ExpressionTree tree) {
        ExpressionTree tr = TreeUtils.skipParens(tree);
        // If method invocation check the method select
        while (tr.getKind() == Tree.Kind.ARRAY_ACCESS)
            tr = ((ArrayAccessTree)tr).getExpression();
        tr = TreeUtils.skipParens(tr);

        if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
            tr = ((MethodInvocationTree)tree).getMethodSelect();
        }
        tr = TreeUtils.skipParens(tr);
        if (tr.getKind() == Tree.Kind.TYPE_CAST)
            tr = ((TypeCastTree)tr).getExpression();
        tr = TreeUtils.skipParens(tr);

        if (tr.getKind() == Tree.Kind.IDENTIFIER)
            return true;
        
        if (tr.getKind() == Tree.Kind.MEMBER_SELECT) {
            tr = ((MemberSelectTree)tr).getExpression();
            if (tr.getKind() == Tree.Kind.IDENTIFIER) {
                Name ident = ((IdentifierTree)tr).getName();
                return ident.contentEquals("this") ||
                        ident.contentEquals("super");
            }
        }

        return false;
    }

    /**
     * Checks if the method invocation is a call to super
     * 
     * @param tree
     *            a tree defining a method invocation
     * 
     * @return true iff tree describes a call to super
     */
    public static boolean isSuperCall(MethodInvocationTree tree) {
        /*@Nullable*/ ExpressionTree mst = tree.getMethodSelect();
        assert mst != null; /*nninvariant*/
        if (mst.getKind() != Tree.Kind.MEMBER_SELECT)
            return false;

        MemberSelectTree selectTree = (MemberSelectTree)mst;

        if (selectTree.getExpression().getKind() != Tree.Kind.IDENTIFIER)
            return false;

        return ((IdentifierTree) selectTree.getExpression()).getName()
                .contentEquals("super");
    }


    /**
     * Gets the enclosing tree of the provided type of a tree node defined
     * by the given {@link TreePath}.
     * 
     * @param path  the path defining the tree node
     * @param kind  the kind of the desired tree
     * @return the enclosing tree of the given type as given by the path 
     */
    public static Tree enclosingOfKind(final TreePath path, final Tree.Kind kind) {
        TreePath p = path;

        while (p != null && p.getParentPath() != null) {
            Tree leaf = p.getLeaf();
            assert leaf != null; /*nninvariant*/
            if (leaf.getKind() == kind)
                return leaf;
            p = p.getParentPath();
        }

        return null;
    }

    /**
     * Gets the enclosing method of the tree node defined by the given
     * {@code {@link TreePath}}. It returns a {@link Tree}, from which
     * {@link checkers.types.AnnotatedTypeMirror} or {@link Element} can be
     * obtained.
     * 
     * @param path
     *            the path defining the tree node
     * @return the enclosing class (or interface) as given by the path, or null
     *         if one does not exist.
     */
    public static /*@Nullable*/ ClassTree enclosingClass(final /*@Nullable*/ TreePath path) {
        return (ClassTree) enclosingOfKind(path, Tree.Kind.CLASS);
    }

    /**
     * Gets the enclosing variable of a tree node defined by the given 
     * {@link TreePath}.
     * 
     * @param path the path defining the tree node
     * @return the enclosing variable as given by the path, or null if one does not exist
     */
    public static VariableTree enclosingVariable(final TreePath path) {
        return (VariableTree) enclosingOfKind(path, Tree.Kind.VARIABLE);
    }
    
    /**
     * Gets the enclosing method of the tree node defined by the given
     * {@code {@link TreePath}}. It returns a {@link Tree}, from which an
     * {@link checkers.types.AnnotatedTypeMirror} or {@link Element} can be
     * obtained.
     * 
     * @param path the path defining the tree node
     * @return the enclosing method as given by the path, or null if one does
     *         not exist
     */
    public static /*@Nullable*/ MethodTree enclosingMethod(final /*@Nullable*/ TreePath path) {
        return (MethodTree) enclosingOfKind(path, Tree.Kind.METHOD);
    }
    
    /**
     * If the given tree is a parenthesized tree, it returns the enclosed
     * non-parenthesized tree. Otherwise, it returns the same tree.
     * 
     * @param tree  a tree
     * @return  the outermost non-parenthesized tree enclosed by the given tree
     */
    public static Tree skipParens(final Tree tree) {
        Tree t = tree;
        while (t.getKind() == Tree.Kind.PARENTHESIZED)
            t = ((ParenthesizedTree)t).getExpression();
        return t;
    }
    

    /**
     * If the given tree is a parenthesized tree, it returns the enclosed
     * non-parenthesized tree. Otherwise, it returns the same tree.
     * 
     * @param tree  an expression tree
     * @return  the outermost non-parenthesized tree enclosed by the given tree
     */
    public static ExpressionTree skipParens(final ExpressionTree tree) {
        ExpressionTree t = tree;
        while (t.getKind() == Tree.Kind.PARENTHESIZED)
            t = ((ParenthesizedTree)t).getExpression();
        return t;
    }
    
    /**
     * Returns the tree with the assignment context for the treePath
     * leaf node.
     *
	 * The assignment context for the treepath is the most enclosing
     * tree of type:
     * <ul>
     *   <li>AssignmentTree </li>
     *   <li>CompoundAssignmentTree </li>
     *   <li>MethodInvocationTree</li>
     *   <li>NewArrayTree</li>
     *   <li>NewClassTree</li>
     *   <li>ReturnTree</li>
     *   <li>VariableTree</li>
     * </ul>
     *
     * @param treePath
     * @return	the assignment context as described.
     */
    public static Tree getAssignmentContext(final TreePath treePath) {
        TreePath path = treePath.getParentPath();
        
        while (path != null) {
            Tree node = path.getLeaf();
            if ((node instanceof AssignmentTree) ||
                    (node instanceof CompoundAssignmentTree) ||
                    (node instanceof MethodInvocationTree) ||
                    (node instanceof NewArrayTree) ||
                    (node instanceof NewClassTree) ||
                    (node instanceof ReturnTree) ||
                    (node instanceof VariableTree))
                return node;
            else
                path = path.getParentPath();
        }
        return null;
    }
}
