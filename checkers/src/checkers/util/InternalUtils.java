package checkers.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import checkers.nullness.quals.*;
import checkers.source.SourceChecker;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;

/**
 * Static utility methods used by annotation abstractions in this package. Some
 * methods in this class depend on the use of Sun javac internals; any procedure
 * in the Checker Framework that uses a non-public API should be placed here.
 */
public class InternalUtils {

    private InternalUtils() {
        // Cannot be instantiated.
    }

    /**
     * Gets the {@link Element} ("symbol") for the given Tree API node.
     *
     * @param tree the {@link Tree} node to get the symbol for
     * @throws IllegalArgumentException
     *         if {@code tree} is null or is not a valid javac-internal tree
     *         (JCTree)
     * @return the {@code {@link Symbol}} for the given tree, or null if one
     *         could not be found
     */
    public static /*@Nullable*/ Element symbol(/*@Nullable*/ Tree tree) {
        if (tree == null) {
            SourceChecker.errorAbort("InternalUtils.symbol: tree is null");
            return null; // dead code
        }

        if (!(tree instanceof JCTree)) {
            SourceChecker.errorAbort("InternalUtils.symbol: tree is not a valid Javac tree");
            return null; // dead code
        }

        if (TreeUtils.isExpressionTree(tree)) {
            tree = TreeUtils.skipParens((ExpressionTree)tree);
        }

        switch (tree.getKind()) {
            case VARIABLE:
            case METHOD:
            case CLASS:
            case ENUM:
            case INTERFACE:
            case ANNOTATION_TYPE:
                return TreeInfo.symbolFor((JCTree) tree);

            // symbol() only works on MethodSelects, so we need to get it manually
            // for method invocations.
            case METHOD_INVOCATION:
                return TreeInfo.symbol(((JCMethodInvocation) tree).getMethodSelect());

            case ASSIGNMENT:
                return TreeInfo.symbol((JCTree)((AssignmentTree)tree).getVariable());

            case ARRAY_ACCESS:
                return symbol(((ArrayAccessTree)tree).getExpression());

            case NEW_CLASS:
                return ((JCNewClass)tree).constructor;

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
     * indicates whether it should return the constructor that gets invoked
     * in cases of anonymous classes
     */
    private static final boolean RETURN_INVOKE_CONSTRUCTOR = true;

    /**
     * Determines the symbol for a constructor given an invocation via
     * {@code new}.
     *
     * If the tree is a declaration of an anonymous class, then method returns
     * constructor that gets invoked in the extended class, rather than the
     * anonymous constructor implicitly added by the constructor (JLS 15.9.5.1)
     *
     * @param tree the constructor invocation
     * @return the {@link ExecutableElement} corresponding to the constructor
     *         call in {@code tree}
     */
    public static ExecutableElement constructor(NewClassTree tree) {

        if (!(tree instanceof JCTree.JCNewClass)) {
            SourceChecker.errorAbort("InternalUtils.constructor: not a javac internal tree");
            return null; // dead code
        }

        JCNewClass newClassTree = (JCNewClass)tree;

        if (RETURN_INVOKE_CONSTRUCTOR && tree.getClassBody() != null) {
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
            JCTree.JCMethodInvocation superInvok = (JCMethodInvocation)stmt.expr;
            return (ExecutableElement)TreeInfo.symbol(superInvok.meth);
        }

        Element e = newClassTree.constructor;

        assert e instanceof ExecutableElement;

        return (ExecutableElement)e;
    }

    public final static List<AnnotationMirror> annotationsFromTypeAnnotationTrees(List<? extends AnnotationTree> annos) {
        List<AnnotationMirror> annotations = new ArrayList<AnnotationMirror>(annos.size());
        for (AnnotationTree anno : annos)
            annotations.add(((JCTypeAnnotation)anno).attribute_field);
        return annotations;
    }
    public final static List<? extends AnnotationMirror> annotationsFromTree(AnnotatedTypeTree node) {
        return annotationsFromTypeAnnotationTrees(((JCAnnotatedType)node).annotations);
    }

    public final static List<? extends AnnotationMirror> annotationsFromTree(TypeParameterTree node) {
        return annotationsFromTypeAnnotationTrees(((JCTypeParameter)node).annotations);
    }

    public final static List<? extends AnnotationMirror> annotationsFromArrayCreation(NewArrayTree node, int level) {

        assert node instanceof JCNewArray;
        final JCNewArray newArray = ((JCNewArray)node);

        if (level == -1) {
            return annotationsFromTypeAnnotationTrees(newArray.annotations);
        }

        if (newArray.dimAnnotations.length() > 0
                && (level >= 0)
                && (level < newArray.dimAnnotations.size()))
            return annotationsFromTypeAnnotationTrees(newArray.dimAnnotations.get(level));

        return Collections.emptyList();
    }


    public static TypeMirror typeOf(Tree tree) {
        return ((JCTree)tree).type;
    }
}
