package checkers.basetype;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.TreePath;

/**
 * A visitor that checks the purity (as defined by {@link checkers.quals.Pure})
 * of a statement or expression.
 * 
 * @author Stefan Heule
 * 
 */
public class PurityChecker {

    public static boolean isPure(TreePath methodBodyPath) {
        PurityCheckerHelper helper = new PurityCheckerHelper();
        Boolean res = helper.scan(methodBodyPath, null);
        return res != null && res;
    }

    private static class PurityCheckerHelper implements
            TreeVisitor<Boolean, Void> {

        /**
         * Scan a single node.
         */
        public Boolean scan(Tree node, Void p) {
            return (node == null) ? null : node.accept(this, p);
        }

        private Boolean scanAndReduce(Tree node, Void p, Boolean r) {
            return reduce(scan(node, p), r);
        }

        /**
         * Scan a list of nodes.
         */
        public Boolean scan(Iterable<? extends Tree> nodes, Void p) {
            Boolean r = null;
            if (nodes != null) {
                boolean first = true;
                for (Tree node : nodes) {
                    r = (first ? scan(node, p) : scanAndReduce(node, p, r));
                    first = false;
                }
            }
            return r;
        }

        private Boolean scanAndReduce(Iterable<? extends Tree> nodes, Void p,
                Boolean r) {
            return reduce(scan(nodes, p), r);
        }

        /**
         * Reduces two results into a combined result. The default
         * implementation is to return the first parameter. The general contract
         * of the method is that it may take any action whatsoever.
         */
        public Boolean reduce(Boolean r1, Boolean r2) {
            return r1 != null && r2 != null && r1 && r2;
        }

        /* ***************************************************************************
         * Visitor methods
         * *******************************************************
         */

        public Boolean visitCompilationUnit(CompilationUnitTree node, Void p) {
            Boolean r = scan(node.getPackageAnnotations(), p);
            r = scanAndReduce(node.getPackageName(), p, r);
            r = scanAndReduce(node.getImports(), p, r);
            r = scanAndReduce(node.getTypeDecls(), p, r);
            return r;
        }

        public Boolean visitImport(ImportTree node, Void p) {
            return scan(node.getQualifiedIdentifier(), p);
        }

        public Boolean visitClass(ClassTree node, Void p) {
            Boolean r = scan(node.getModifiers(), p);
            r = scanAndReduce(node.getTypeParameters(), p, r);
            r = scanAndReduce(node.getExtendsClause(), p, r);
            r = scanAndReduce(node.getImplementsClause(), p, r);
            r = scanAndReduce(node.getMembers(), p, r);
            return r;
        }

        public Boolean visitMethod(MethodTree node, Void p) {
            Boolean r = scan(node.getModifiers(), p);
            r = scanAndReduce(node.getReturnType(), p, r);
            r = scanAndReduce(node.getTypeParameters(), p, r);
            r = scanAndReduce(node.getParameters(), p, r);
            r = scanAndReduce(node.getReceiverAnnotations(), p, r);
            r = scanAndReduce(node.getThrows(), p, r);
            r = scanAndReduce(node.getBody(), p, r);
            r = scanAndReduce(node.getDefaultValue(), p, r);
            return r;
        }

        public Boolean visitVariable(VariableTree node, Void p) {
            Boolean r = scan(node.getModifiers(), p);
            r = scanAndReduce(node.getType(), p, r);
            r = scanAndReduce(node.getInitializer(), p, r);
            return r;
        }

        public Boolean visitEmptyStatement(EmptyStatementTree node, Void p) {
            return null;
        }

        public Boolean visitBlock(BlockTree node, Void p) {
            return scan(node.getStatements(), p);
        }

        public Boolean visitDoWhileLoop(DoWhileLoopTree node, Void p) {
            Boolean r = scan(node.getStatement(), p);
            r = scanAndReduce(node.getCondition(), p, r);
            return r;
        }

        public Boolean visitWhileLoop(WhileLoopTree node, Void p) {
            Boolean r = scan(node.getCondition(), p);
            r = scanAndReduce(node.getStatement(), p, r);
            return r;
        }

        public Boolean visitForLoop(ForLoopTree node, Void p) {
            Boolean r = scan(node.getInitializer(), p);
            r = scanAndReduce(node.getCondition(), p, r);
            r = scanAndReduce(node.getUpdate(), p, r);
            r = scanAndReduce(node.getStatement(), p, r);
            return r;
        }

        public Boolean visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
            Boolean r = scan(node.getVariable(), p);
            r = scanAndReduce(node.getExpression(), p, r);
            r = scanAndReduce(node.getStatement(), p, r);
            return r;
        }

        public Boolean visitLabeledStatement(LabeledStatementTree node, Void p) {
            return scan(node.getStatement(), p);
        }

        public Boolean visitSwitch(SwitchTree node, Void p) {
            Boolean r = scan(node.getExpression(), p);
            r = scanAndReduce(node.getCases(), p, r);
            return r;
        }

        public Boolean visitCase(CaseTree node, Void p) {
            Boolean r = scan(node.getExpression(), p);
            r = scanAndReduce(node.getStatements(), p, r);
            return r;
        }

        public Boolean visitSynchronized(SynchronizedTree node, Void p) {
            Boolean r = scan(node.getExpression(), p);
            r = scanAndReduce(node.getBlock(), p, r);
            return r;
        }

        public Boolean visitTry(TryTree node, Void p) {
            Boolean r = scan(node.getResources(), p);
            r = scanAndReduce(node.getBlock(), p, r);
            r = scanAndReduce(node.getCatches(), p, r);
            r = scanAndReduce(node.getFinallyBlock(), p, r);
            return r;
        }

        public Boolean visitCatch(CatchTree node, Void p) {
            Boolean r = scan(node.getParameter(), p);
            r = scanAndReduce(node.getBlock(), p, r);
            return r;
        }

        public Boolean visitConditionalExpression(
                ConditionalExpressionTree node, Void p) {
            Boolean r = scan(node.getCondition(), p);
            r = scanAndReduce(node.getTrueExpression(), p, r);
            r = scanAndReduce(node.getFalseExpression(), p, r);
            return r;
        }

        public Boolean visitIf(IfTree node, Void p) {
            Boolean r = scan(node.getCondition(), p);
            r = scanAndReduce(node.getThenStatement(), p, r);
            r = scanAndReduce(node.getElseStatement(), p, r);
            return r;
        }

        public Boolean visitExpressionStatement(ExpressionStatementTree node,
                Void p) {
            return scan(node.getExpression(), p);
        }

        public Boolean visitBreak(BreakTree node, Void p) {
            return null;
        }

        public Boolean visitContinue(ContinueTree node, Void p) {
            return null;
        }

        public Boolean visitReturn(ReturnTree node, Void p) {
            return scan(node.getExpression(), p);
        }

        public Boolean visitThrow(ThrowTree node, Void p) {
            return scan(node.getExpression(), p);
        }

        public Boolean visitAssert(AssertTree node, Void p) {
            Boolean r = scan(node.getCondition(), p);
            r = scanAndReduce(node.getDetail(), p, r);
            return r;
        }

        public Boolean visitMethodInvocation(MethodInvocationTree node, Void p) {
            Boolean r = scan(node.getTypeArguments(), p);
            r = scanAndReduce(node.getMethodSelect(), p, r);
            r = scanAndReduce(node.getArguments(), p, r);
            return r;
        }

        public Boolean visitNewClass(NewClassTree node, Void p) {
            Boolean r = scan(node.getEnclosingExpression(), p);
            r = scanAndReduce(node.getIdentifier(), p, r);
            r = scanAndReduce(node.getTypeArguments(), p, r);
            r = scanAndReduce(node.getArguments(), p, r);
            r = scanAndReduce(node.getClassBody(), p, r);
            return r;
        }

        public Boolean visitNewArray(NewArrayTree node, Void p) {
            Boolean r = scan(node.getType(), p);
            r = scanAndReduce(node.getDimensions(), p, r);
            r = scanAndReduce(node.getInitializers(), p, r);
            return r;
        }

        public Boolean visitLambdaExpression(LambdaExpressionTree node, Void p) {
            Boolean r = scan(node.getParameters(), p);
            r = scanAndReduce(node.getBody(), p, r);
            return r;
        }

        public Boolean visitParenthesized(ParenthesizedTree node, Void p) {
            return scan(node.getExpression(), p);
        }

        public Boolean visitAssignment(AssignmentTree node, Void p) {
            Boolean r = scan(node.getVariable(), p);
            r = scanAndReduce(node.getExpression(), p, r);
            return r;
        }

        public Boolean visitCompoundAssignment(CompoundAssignmentTree node,
                Void p) {
            Boolean r = scan(node.getVariable(), p);
            r = scanAndReduce(node.getExpression(), p, r);
            return r;
        }

        public Boolean visitUnary(UnaryTree node, Void p) {
            return scan(node.getExpression(), p);
        }

        public Boolean visitBinary(BinaryTree node, Void p) {
            Boolean r = scan(node.getLeftOperand(), p);
            r = scanAndReduce(node.getRightOperand(), p, r);
            return r;
        }

        public Boolean visitTypeCast(TypeCastTree node, Void p) {
            Boolean r = scan(node.getType(), p);
            r = scanAndReduce(node.getExpression(), p, r);
            return r;
        }

        public Boolean visitInstanceOf(InstanceOfTree node, Void p) {
            Boolean r = scan(node.getExpression(), p);
            r = scanAndReduce(node.getType(), p, r);
            return r;
        }

        public Boolean visitArrayAccess(ArrayAccessTree node, Void p) {
            Boolean r = scan(node.getExpression(), p);
            r = scanAndReduce(node.getIndex(), p, r);
            return r;
        }

        public Boolean visitMemberSelect(MemberSelectTree node, Void p) {
            return scan(node.getExpression(), p);
        }

        public Boolean visitMemberReference(MemberReferenceTree node, Void p) {
            Boolean r = scan(node.getQualifierExpression(), p);
            r = scanAndReduce(node.getTypeArguments(), p, r);
            return r;
        }

        public Boolean visitIdentifier(IdentifierTree node, Void p) {
            return null;
        }

        public Boolean visitLiteral(LiteralTree node, Void p) {
            return null;
        }

        public Boolean visitPrimitiveType(PrimitiveTypeTree node, Void p) {
            return null;
        }

        public Boolean visitArrayType(ArrayTypeTree node, Void p) {
            return scan(node.getType(), p);
        }

        public Boolean visitParameterizedType(ParameterizedTypeTree node, Void p) {
            Boolean r = scan(node.getType(), p);
            r = scanAndReduce(node.getTypeArguments(), p, r);
            return r;
        }

        public Boolean visitUnionType(UnionTypeTree node, Void p) {
            return scan(node.getTypeAlternatives(), p);
        }

        public Boolean visitTypeParameter(TypeParameterTree node, Void p) {
            Boolean r = scan(node.getAnnotations(), p);
            r = scanAndReduce(node.getBounds(), p, r);
            return r;
        }

        public Boolean visitWildcard(WildcardTree node, Void p) {
            return scan(node.getBound(), p);
        }

        public Boolean visitModifiers(ModifiersTree node, Void p) {
            return scan(node.getAnnotations(), p);
        }

        public Boolean visitAnnotation(AnnotationTree node, Void p) {
            Boolean r = scan(node.getAnnotationType(), p);
            r = scanAndReduce(node.getArguments(), p, r);
            return r;
        }

        public Boolean visitAnnotatedType(AnnotatedTypeTree node, Void p) {
            Boolean r = scan(node.getAnnotations(), p);
            r = scanAndReduce(node.getUnderlyingType(), p, r);
            return r;
        }

        public Boolean visitOther(Tree node, Void p) {
            return null;
        }

        public Boolean visitErroneous(ErroneousTree node, Void p) {
            return null;
        }
    }

}
