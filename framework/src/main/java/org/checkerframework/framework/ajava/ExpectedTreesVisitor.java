package org.checkerframework.framework.ajava;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.HashSet;
import java.util.Set;

/**
 * Stores each visited tree that should match with some JavaParser node if the same Java file was
 * parsed with both.
 */
public class ExpectedTreesVisitor extends TreeScannerWithDefaults {
    private Set<Tree> trees;

    public ExpectedTreesVisitor() {
        trees = new HashSet<>();
    }

    public Set<Tree> getTrees() {
        return trees;
    }

    @Override
    public void defaultAction(Tree tree) {
        trees.add(tree);
    }

    @Override
    public Void visitAnnotation(AnnotationTree tree, Void p) {
        // Skip annotations because ajava files are not required to have the same annotations as
        // their corresponding java files.
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree tree, Void p) {
        // Javac inserts calls to super() at the start of constructors with no this or super call.
        // These don't have matching JavaParser nodes.
        if (JointJavacJavaParserVisitor.isDefaultSuperConstructorCall(tree)) {
            return null;
        }

        return super.visitExpressionStatement(tree, p);
    }

    @Override
    public Void visitMethod(MethodTree tree, Void p) {
        // Synthetic default constructors don't have matching JavaParser nodes.
        if (JointJavacJavaParserVisitor.isNoArgumentConstructor(tree)) {
            return null;
        }

        return super.visitMethod(tree, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
        Void result = super.visitMethodInvocation(tree, p);
        // In a method invocation like myObject.myMethod(), the method invocation stores
        // myObject.myMethod as its own MemberSelectTree which has no corresponding JavaParserNode.
        // This node should not be checked.
        if (tree.getMethodSelect().getKind() == Kind.MEMBER_SELECT) {
            trees.remove(tree.getMethodSelect());
        }

        return result;
    }

    @Override
    public Void visitModifiers(ModifiersTree tree, Void p) {
        // Don't add ModifierTrees or children because they have no corresponding JavaParser node.
        return null;
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, Void p) {
        // Skip array initialization because it's not implemented yet.
        return null;
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void p) {
        Void result = super.visitNewClass(tree, p);
        // Javac stores anonymous class bodies as a whole ClassTree but JavaParser just stores a
        // list of members, so remove the class body itself.
        if (tree.getClassBody() != null) {
            trees.remove(tree.getClassBody());
        }

        return result;
    }
}
