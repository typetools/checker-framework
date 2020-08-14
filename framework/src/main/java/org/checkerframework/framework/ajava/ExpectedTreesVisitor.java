package org.checkerframework.framework.ajava;

import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
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
    public Void visitModifiers(ModifiersTree tree, Void p) {
        // Don't add ModifierTrees or children because they have no corresponding JavaParser node.
        return null;
    }
}
