package checkers.types;

import checkers.quals.*;

import javax.lang.model.element.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;

/**
 * Static utility methods used by annotation abstractions in this package. Some
 * methods in this class depend on the use of Sun javac internals; any procedure
 * in the checkers framework that uses a non-public API should be placed here.
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public class InternalUtils {
    
    /** Cannot be instantiated. */
    private InternalUtils() {
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
    public static @Nullable Tree enclosingMethod(final @Nullable TreePath path) {

        @Nullable TreePath p = path; 
        
        // Ascend the path, stopping when a method node is found.
        while (p != null && p.getParentPath() != null) {
            @Nullable Tree leaf = p.getLeaf();
            assert leaf != null; /*nninvariant*/
            if (leaf.getKind() == Tree.Kind.METHOD)
                return leaf;
            p = p.getParentPath();
        }

        return null;
    }
     
    /**
     * Gets the enclosing symbol (as an {@link Element}) for the tree node
     * defined by the given {@link TreePath}. The enclosing symbol is 
     * significant because annotations on types are stored in their enclosing
     * symbols. For variables, fields, methods, and classes, the enclosing
     * symbol is simply the symbol for that program element and annotations are 
     * contained there. For other annotation locations (like typecasts or
     * generics), however, annotations are stored higher up the tree at the
     * class, method, or variable/field that encloses the location.
     *
     * @param path the path defining the tree node
     * @return the symbol of the enclosing program element as given by the path,
     *         or null if one does not exist
     */
    public static @Nullable Element enclosingSymbol(final @Nullable TreePath path) {
         
        // If an enclosing method is readily attainable return its symbol.
        @Nullable Tree enclosingMethod = enclosingMethod(path);
        if (enclosingMethod != null) {
            @Nullable Element elt = symbol(enclosingMethod);
            return elt;
        }
        
        // Ascend the path, stopping at a variable/field or class node.
        @Nullable TreePath p = path;
        @Nullable Tree leaf = null;
        while (p != null && p.getParentPath() != null) {
            leaf = p.getLeaf();
            assert leaf != null; /*nninvariant*/
            if (leaf.getKind() == Tree.Kind.VARIABLE || leaf.getKind() == Tree.Kind.CLASS)
                break;
            p = p.getParentPath();
        }
                
        // Return null if we didn't find anything. Otherwise, return the
        // symbol for the class, variable, or field.
        if (leaf == null) 
            return null;
        else if (leaf.getKind() == Tree.Kind.CLASS)
            return TreeInfo.symbolFor((JCTree)leaf);
        else       
            return symbol(leaf);
    }
    
    /**
     * Gets the enclosing "annotation source", which is identical to {@link 
     * InternalUtils#enclosingSymbol} but will correctly return the symbol that
     * contains annotations for code in a static initializer.
     *
     * @param path the path defining the tree node
     * @return the symbol that contains the annotations for the tail of the
     *         given tree path (its "annotation source")
     *
     * @deprecated No longer necessary following changes to the compiler.
     */
    @Deprecated
    public static @Nullable Element enclosingAnnotationSource(final @Nullable TreePath path) {
        @Nullable Tree tree;
        @Nullable TreePath fpath = path;
        while (fpath != null && fpath.getLeaf() != null) {
            tree = fpath.getLeaf();
            assert tree != null; /*nninvariant*/
            if (tree.getKind() == Tree.Kind.BLOCK) {
                BlockTree bt = (BlockTree)tree;
                if (bt.isStatic()) {
                    @Nullable TreePath pp = fpath.getParentPath();
                    assert pp != null; /*nninvariant*/
                    return symbol(pp.getLeaf());
                }
            }
            fpath = fpath.getParentPath();
        }
        return enclosingSymbol(path);
    }
    
    /**
     * Determines whether or not the two trees refer to the same node. 
     * {@link Tree}s do not have an overriden {@code .equals} method and can't
     * normally be compared with {@code ==} after copying (with 
     * {@link TreeCopier}), so it sometimes the case that two trees refer to the
     * same source position but both {@code ==} and {@code .equals} return false.
     *
     * @param source the tree to compare against
     * @param dest the tree to compare to the source
     * @return true if the two trees refer to the same node, false if they don't
     */
    public static boolean refersTo(@Nullable Tree source, @Nullable Tree dest) {
        
        if (!(source instanceof JCTree) || !(dest instanceof JCTree))
            return false;
        
        return (source == dest || ((@NonNull JCTree)source).pos == ((@NonNull JCTree)dest).pos);
    }
    
    /**
     * Gets the {@link Element} ("symbol") for the given Tree API node.
     *
     * @param tree
     *            the {@link Tree} node to get the symbol for
     * @throws IllegalArgumentException
     *         if {@code tree} is null or is not a valid javac-internal tree 
     *         (JCTree)
     * @return the {@code {@link Symbol}} for the given tree, or null if one
     *         could not be found
     */
    public static @Nullable Element symbol(@Nullable Tree tree) {
        
        if (tree == null)
            throw new IllegalArgumentException("tree is null");
        
        if (!(tree instanceof JCTree))
            throw new IllegalArgumentException("tree is not a valid Javac tree");
        
        switch (tree.getKind()) {
            case VARIABLE:
            case METHOD:
            case CLASS:
                return TreeInfo.symbolFor((JCTree) tree);
        
            // symbol() only works on MethodSelects, so we need to get it manually
            // for method invocations.
            case METHOD_INVOCATION:
                return TreeInfo.symbol(((JCMethodInvocation) tree)
                .getMethodSelect());

            case PARENTHESIZED:
                ParenthesizedTree pTree = (ParenthesizedTree)tree;
                return symbol(skipParens(pTree.getExpression()));

            case ASSIGNMENT:
                return TreeInfo.symbol((JCTree)((AssignmentTree)tree).getVariable()); 

            case ARRAY_ACCESS:
                return TreeInfo.symbol((JCTree)((ArrayAccessTree)tree).getExpression());

            default:
                return TreeInfo.symbol((JCTree) tree);
        }
    }
    
    /**
     * Determines whether or not the node referred to by the given 
     * {@link TreePath} is an anonymous constructor (the constructor for an
     * anonymous class.
     *
     * @param path the {@link TreePath} for a node that may be an anonymous
     *        constructor
     * @return true if the given path points to an anonymous constructor, false
     *         if it does not
     */
    public static boolean isAnonymousConstructor(final TreePath path) {
        
        @Nullable Element e = InternalUtils.enclosingSymbol(path);
        if (e == null || !(e instanceof Symbol))
            return false;
        
        if ((((@NonNull Symbol)e).flags() & Flags.ANONCONSTR) != 0)
            return true;
        
        return false;
    }

    /**
     * Determines the fully qualified name of the class to which the
     * given element belongs. 
     *
     * @param elt the element to determine the name of
     * @return the fully qualified name of the element's enclosing
     * class (if it is a class member) or the element's name (if it is
     * a class)
     */
    public static String getQualifiedName(Element elt) {

        Symbol s = (Symbol)elt;
        @Nullable Symbol encl = s.enclClass();
        if (encl == null) {
            @Nullable Name n = s.getQualifiedName();
            assert n != null; /*nninvariant*/
            return (@NonNull String)n.toString();
        }
        @Nullable Name n = encl.getQualifiedName();
        assert n != null; /*nninvariant*/
        return (@NonNull String)encl.toString();
    }

    public static @Nullable ExpressionTree skipParens(@Nullable ExpressionTree tree) {
        return TreeInfo.skipParens((JCTree.JCExpression)tree); 
    }

    /**
     * Determines the symbol for a constructor given an invocation via {@code
     * new}.
     *
     * @param tree the constructor invocation
     * @return the {@link ExecutableElement} corresponding to the constructor
     *         call in {@code tree}
     */
    public static ExecutableElement constructor(NewClassTree tree) {

        if (!(tree instanceof JCTree.JCNewClass))
            throw new IllegalArgumentException("not a javac internal tree");

        Element e = ((JCTree.JCNewClass)tree).constructor;

        assert e instanceof ExecutableElement;

        return (ExecutableElement)e;
    }
}
