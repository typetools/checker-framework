package org.checkerframework.javacutil.trees;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;

/**
 * A Utility class for coping AST {@link JCTree} fully, including type and symbol information. This
 * class is a helper class and used only in {@link TreeBuilder#copy(Tree)}.
 *
 * @see TreeBuilder#copy(Tree)
 */
class FullyTreeCopier extends TreeCopier<Void> {

    /* package private */ FullyTreeCopier(TreeMaker treeMaker) {
        super(treeMaker);
    }

    @Override
    public <T extends JCTree> T copy(T tree, Void p) {
        T t = super.copy(tree, p);
        if (t == null) {
            return null;
        }

        t.type = tree.type;
        return t;
    }

    @Override
    public JCTree visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        JCAssignOp t = (JCAssignOp) super.visitCompoundAssignment(node, p);
        t.operator = ((JCAssignOp) node).operator;
        return t;
    }

    @Override
    public JCTree visitBinary(BinaryTree node, Void p) {
        JCBinary t = (JCBinary) super.visitBinary(node, p);
        t.operator = ((JCBinary) node).operator;
        return t;
    }

    @Override
    public JCTree visitClass(ClassTree node, Void p) {
        JCClassDecl t = (JCClassDecl) super.visitClass(node, p);
        t.sym = ((JCClassDecl) node).sym;
        return t;
    }

    @Override
    public JCTree visitIdentifier(IdentifierTree node, Void p) {
        JCIdent t = (JCIdent) super.visitIdentifier(node, p);
        t.sym = ((JCIdent) node).sym;
        return t;
    }

    @Override
    public JCTree visitMethod(MethodTree node, Void p) {
        JCMethodDecl t = (JCMethodDecl) super.visitMethod(node, p);
        t.sym = ((JCMethodDecl) node).sym;
        return t;
    }

    @Override
    public JCTree visitMethodInvocation(MethodInvocationTree node, Void p) {
        JCMethodInvocation t = (JCMethodInvocation) super.visitMethodInvocation(node, p);
        t.varargsElement = ((JCMethodInvocation) node).varargsElement;
        return t;
    }

    @Override
    public JCTree visitNewClass(NewClassTree node, Void p) {
        JCNewClass t = (JCNewClass) super.visitNewClass(node, p);
        t.constructor = ((JCNewClass) node).constructor;
        t.constructorType = ((JCNewClass) node).constructorType;
        t.varargsElement = ((JCNewClass) node).varargsElement;
        return t;
    }

    @Override
    public JCTree visitLambdaExpression(LambdaExpressionTree node, Void p) {
        JCLambda t = (JCLambda) super.visitLambdaExpression(node, p);
        t.canCompleteNormally = ((JCLambda) node).canCompleteNormally;
        return t;
    }

    @Override
    public JCTree visitMemberSelect(MemberSelectTree node, Void p) {
        JCFieldAccess t = (JCFieldAccess) super.visitMemberSelect(node, p);
        t.sym = ((JCFieldAccess) node).sym;
        return t;
    }

    @Override
    public JCTree visitMemberReference(MemberReferenceTree node, Void p) {
        JCMemberReference t = (JCMemberReference) super.visitMemberReference(node, p);
        t.kind = ((JCMemberReference) node).kind;
        t.sym = ((JCMemberReference) node).sym;
        t.varargsElement = ((JCMemberReference) node).varargsElement;
        t.refPolyKind = ((JCMemberReference) node).refPolyKind;
        t.ownerAccessible = ((JCMemberReference) node).ownerAccessible;
        t.overloadKind = ((JCMemberReference) node).overloadKind;
        return t;
    }

    @Override
    public JCTree visitTry(TryTree node, Void p) {
        JCTry t = (JCTry) super.visitTry(node, p);
        t.finallyCanCompleteNormally = ((JCTry) node).finallyCanCompleteNormally;
        return t;
    }

    @Override
    public JCTree visitUnary(UnaryTree node, Void p) {
        JCUnary t = (JCUnary) super.visitUnary(node, p);
        t.operator = ((JCUnary) node).operator;
        return t;
    }

    @Override
    public JCTree visitVariable(VariableTree node, Void p) {
        JCVariableDecl t = (JCVariableDecl) super.visitVariable(node, p);
        t.sym = ((JCVariableDecl) node).sym;
        return t;
    }
}
