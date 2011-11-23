package checkers.util;

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
    
    private InternalUtils() {
        // Cannot be instantiated.
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
    public static /*@Nullable*/ Element symbol(/*@Nullable*/ Tree tree) {
        
        if (tree == null)
            throw new IllegalArgumentException("tree is null");
        
        if (!(tree instanceof JCTree))
            throw new IllegalArgumentException("tree is not a valid Javac tree");
        
        tree = TreeUtils.skipParens(tree);
        
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

            case ASSIGNMENT:
                return TreeInfo.symbol((JCTree)((AssignmentTree)tree).getVariable()); 

            case ARRAY_ACCESS:
                return symbol(((ArrayAccessTree)tree).getExpression());

            default:
                return TreeInfo.symbol((JCTree) tree);
        }
    }
    
    /**
     * Determines whether or not the node referred to by the given 
     * {@link TreePath} is an anonymous constructor (the constructor for an
     * anonymous class.
     *
     * @param method the {@link TreePath} for a node that may be an anonymous
     *        constructor
     * @return true if the given path points to an anonymous constructor, false
     *         if it does not
     */
    public static boolean isAnonymousConstructor(final MethodTree method) {
        /*@Nullable*/ Element e = InternalUtils.symbol(method);
        if (e == null || !(e instanceof Symbol))
            return false;
        
        if ((((/*@NonNull*/ Symbol)e).flags() & Flags.ANONCONSTR) != 0)
            return true;
        
        return false;
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
