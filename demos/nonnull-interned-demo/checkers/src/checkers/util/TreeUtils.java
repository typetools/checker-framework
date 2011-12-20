package checkers.util;

import checkers.quals.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;

import checkers.types.AnnotatedClassType;
import checkers.types.AnnotatedMethodType;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;

/**
 * A utility class made for helping to analyze a given {@code Tree}.
 */
// TODO: This class needs significant restructuring
@DefaultQualifier("checkers.nullness.quals.NonNull")
public final class TreeUtils {
    // Cannot be initilized
    private TreeUtils() {
    }

    /**
     * Checks if the provided method is a constructor method or no.
     * 
     * @param tree
     *            a tree defining the method
     * @return true iff tree describes a constructor
     */
    public static boolean isConstructor(MethodTree tree) {
        return (tree.getName().contentEquals("<init>"));
    }
    
    /**
     * Checks if the provided method invocation is a call to an instance-self
     * method
     * 
     * @param tree
     *            a tree defining a method invocation
     * 
     * @return true iff tree describes a call to an instance-self method
     */
    public static boolean isSelfCall(MethodInvocationTree tree) {
        @Nullable ExpressionTree mst = tree.getMethodSelect();
        assert mst != null; /*nninvariant*/
        return isSelfAccess(mst);
    }

    public static boolean isSelfAccess(Tree tree) {
        switch (tree.getKind()) {
        case IDENTIFIER:
            // FIXME: Access to local variables and import static
            return true;
        case MEMBER_SELECT:
            MemberSelectTree selectTree = (MemberSelectTree) tree;

            if (selectTree.getExpression().getKind() != Tree.Kind.IDENTIFIER)
                return false;
            IdentifierTree ident = (IdentifierTree) selectTree.getExpression();
            Name objName = ident.getName();
            return (objName.contentEquals("this") || objName
                    .contentEquals("super"));
        default:
            return false;
        }
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
        @Nullable ExpressionTree mst = tree.getMethodSelect();
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
     * Gets the enclosing method of the tree node defined by the given
     * {@code {@link TreePath}}. It returns a {@link Tree}, from which
     * {@link AnnotatedClassType} or {@link Element} can be obtained.
     * 
     * 
     * @param path
     *            the path defining the tree node
     * @return the enclosing class (or interface) as given by the path, or null
     *         if one does not exist.
     */
    public static @Nullable ClassTree enclosingClass(final @Nullable TreePath path) {
        @Nullable TreePath p = path;

        // Ascend the path, stopping when a class node is found.
        while (p != null && p.getParentPath() != null) {
            @Nullable Tree leaf = p.getLeaf();
            assert leaf != null; /*nninvariant*/
            if (leaf.getKind() == Tree.Kind.CLASS)
                return (ClassTree) leaf;
            p = p.getParentPath();
        }

        return null;
    }
    
    /**
     * Gets the enclosing method of the tree node defined by the given
     * {@code {@link TreePath}}. It returns a {@link Tree}, from which an
     * {@link AnnotatedMethodType} or {@link Element} can be obtained.
     * 
     * @param path
     *            the path defining the tree node
     * @return the enclosing method as given by the path, or null if one does
     *         not exist
     */
    public static @Nullable MethodTree enclosingMethod(final @Nullable TreePath path) {

        @Nullable TreePath p = path;

        // Ascend the path, stopping when a method node is found.
        while (p != null && p.getParentPath() != null) {
            @Nullable Tree leaf = p.getLeaf();
            assert leaf != null; /*nninvariant*/
            if (leaf.getKind() == Tree.Kind.METHOD)
                return (MethodTree)leaf;
            p = p.getParentPath();
        }

        return null;
    }
    
    /**
     * If the given tree is a parenthesized tree, it returns the enclosed
     * non-parenthesized tree. Otherwise, it returns the same tree.
     * 
     * @param tree  a tree
     * @return  the outermost non-parenthesized tree enclosed by the given tree
     */
    public Tree skipParens(final Tree tree) {
        @Nullable Tree t = tree;
        while (t.getKind() == Tree.Kind.PARENTHESIZED)
            t = ((ParenthesizedTree)t).getExpression();
        assert t != null; /*nninvariant*/
        return t;
    }
    

    /**
     * If the given tree is a parenthesized tree, it returns the enclosed
     * non-parenthesized tree. Otherwise, it returns the same tree.
     * 
     * @param tree  an expression tree
     * @return  the outermost non-parenthesized tree enclosed by the given tree
     */
    public ExpressionTree skipParens(final ExpressionTree tree) {
        @Nullable ExpressionTree t = tree;
        while (t.getKind() == Tree.Kind.PARENTHESIZED)
            t = ((ParenthesizedTree)t).getExpression();
        assert t != null; /*nninvariant*/
        return t;
    }
}
