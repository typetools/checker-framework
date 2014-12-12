package org.checkerframework.framework.type;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.type.TypeKind;

/**
 * A utility class to convert trees into corresponding AnnotatedTypeMirrors.
 * This class should be used ONLY from AnnotatedTypeFactory.
 *
 * For each method in TypeFromTree there is a corresponding TypeFromTreeVisitor
 * that handles the input tree.  The list of methods implemented by these visitors
 * outline which trees each method will support.  If a tree kind is not handled
 * by the given visitor, then execution is halted and an RuntimeException is thrown
 * which includes a list of supported tree types.
 */
class TypeFromTree {

    private static final TypeFromTypeTreeVisitor typeTreeVisitor = new TypeFromTypeTreeVisitor();
    private static final TypeFromMemberVisitor memberVisitor = new TypeFromMemberVisitor();
    private static final TypeFromClassVisitor classVisitor = new TypeFromClassVisitor();
    private static final TypeFromExpressionVisitor expressionVisitor = new TypeFromExpressionVisitor();

    /**
     * @param tree Must be an ExpressionTree
     * @return An AnnotatedTypeMirror representing the input expression tree.
     */
    public static AnnotatedTypeMirror fromExpression(final AnnotatedTypeFactory typeFactory, final ExpressionTree tree) {
        abortIfTreeIsNull(typeFactory, tree);

        final AnnotatedTypeMirror type = expressionVisitor.visit(tree, typeFactory);
        ifExecutableCheckElement(typeFactory, tree, type);

        return type;
    }

    /**
     * @param tree Must represent a class member
     * @return An AnnotatedTypeMirror representing the input tree
     */
    public static AnnotatedTypeMirror fromMember(final AnnotatedTypeFactory typeFactory, final Tree tree) {
        abortIfTreeIsNull(typeFactory, tree);

        final AnnotatedTypeMirror type = memberVisitor.visit(tree, typeFactory);
        ifExecutableCheckElement(typeFactory, tree, type);
        return type;
    }

    /**
     * @param tree Must be a type tree
     * @return An AnnotatedTypeMirror representing the input type tree
     */
    public static AnnotatedTypeMirror fromTypeTree(final AnnotatedTypeFactory typeFactory, final Tree tree) {
        abortIfTreeIsNull(typeFactory, tree);

        final AnnotatedTypeMirror type = typeTreeVisitor.visit(tree, typeFactory);
        abortIfTypeIsExecutable(typeFactory, tree, type);
        return type;
    }

    /**
     * @return An AnnotatedDeclaredType representing the input ClassTree
     */
    public static AnnotatedDeclaredType fromClassTree(final AnnotatedTypeFactory typeFactory, final ClassTree tree) {
        abortIfTreeIsNull(typeFactory, tree);

        final AnnotatedDeclaredType type = (AnnotatedDeclaredType) classVisitor.visit(tree, typeFactory);
        abortIfTypeIsExecutable(typeFactory, tree, type);
        return type;
    }

    protected static void abortIfTreeIsNull(final AnnotatedTypeFactory typeFactory, final Tree tree) {
        if (tree == null) {
            ErrorReporter.errorAbort("Encountered null tree" + summarize(typeFactory, tree));
        }
    }

    protected static void ifExecutableCheckElement(final AnnotatedTypeFactory typeFactory, final Tree tree,
                                                   final AnnotatedTypeMirror type) {
        if (type.getKind() == TypeKind.EXECUTABLE) {
            if (((AnnotatedExecutableType) type).getElement() == null) {
                ErrorReporter.errorAbort(
                    "Executable has no element:\n" + summarize(typeFactory, tree, type)
                );
            }
        }
    }

    protected static void abortIfTypeIsExecutable(final AnnotatedTypeFactory typeFactory, final Tree tree,
                                                  final AnnotatedTypeMirror type) {
        if (type.getKind() == TypeKind.EXECUTABLE) {
            ErrorReporter.errorAbort(
                "Unexpected Executable typekind:\n" + summarize(typeFactory, tree, type)
            );
        }
    }

    protected static String summarize(final AnnotatedTypeFactory typeFactory, final Tree tree) {
        return "tree=" + tree + "\n"
             + "typeFactory=" + typeFactory.getClass().getSimpleName();

    }

    protected static String summarize(final AnnotatedTypeFactory typeFactory, final Tree tree,
                                      final AnnotatedTypeMirror type) {
        return "type=" + type + "\n" + summarize(typeFactory, tree);
    }
}
