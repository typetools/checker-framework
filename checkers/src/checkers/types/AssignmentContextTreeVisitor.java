package checkers.types;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreeScanner;

/**
 * A TreeVisitor that maintains an assignment context.
 *
 * TODO: documentation and generalization
 */
public class AssignmentContextTreeVisitor extends SimpleTreeVisitor<Void, AnnotatedTypeMirror> {
    protected Tree context;

    public Void visitAssignment(AssignmentTree node, AnnotatedTypeMirror type) {
        Tree precontext = context;
        context = node.getVariable();
        try {
            this.visit(node.getExpression(), type);
        } finally {
            context = precontext;
        }
        return null;
    }

    public Void visitVariable(VariableTree node, AnnotatedTypeMirror type) {
        Tree precontext = context;
        context = node;
        type = AnnotatedTypes.deepCopy(type);
        try {
            this.visit(node.getModifiers(), type);
            this.visit(node.getType(), type);
            this.visit(node.getInitializer(), type);
        } finally {
            context = precontext;
        }
        return null;
    }
}
