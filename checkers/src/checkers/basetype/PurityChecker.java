package checkers.basetype;

import checkers.util.TreeUtils;

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
import com.sun.source.tree.ExpressionTree;
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

    public static Result checkPurity(TreePath methodBodyPath) {
        PurityCheckerHelper helper = new PurityCheckerHelper();
        Result res = helper.scan(methodBodyPath, new PureResult());
        return res;
    }

    public static abstract class Result {
        public abstract boolean isPure();

        public abstract String getReason();
    }

    protected static class PureResult extends Result {

        @Override
        public boolean isPure() {
            return true;
        }

        @Override
        public String getReason() {
            assert false : "only applicable for NonPureResult";
            return null;
        }
    }

    protected static class NonPureResult extends Result {
        protected final String reason;
        protected final/* @Nullable */NonPureResult nextReason;

        public NonPureResult(String reason) {
            this.reason = reason;
            this.nextReason = null;
        }

        public NonPureResult(String reason, Result nextReason) {
            this.reason = reason;
            if (nextReason instanceof NonPureResult) {
                this.nextReason = (NonPureResult) nextReason;
            } else {
                this.nextReason = null;
            }
        }

        @Override
        public boolean isPure() {
            return false;
        }

        @Override
        public String getReason() {
            return reason
                    + (nextReason != null ? ", " + nextReason.getReason() : "");
        }
    }

    private static class PurityCheckerHelper implements
            TreeVisitor<Result, Result> {

        /**
         * Scan a single node.
         */
        public Result scan(Tree node, Result p) {
            return node.accept(this, p);
        }

        private Result scanAndReduce(Tree node, Result p, Result r) {
            return reduce(scan(node, p), r);
        }

        private Result scanAndReduce(Iterable<? extends Tree> nodes, Result p,
                Result r) {
            return reduce(scan(nodes, p), r);
        }

        /**
         * Scan a list of nodes.
         */
        public Result scan(Iterable<? extends Tree> nodes, Result p) {
            Result r = null;
            if (nodes != null) {
                boolean first = true;
                for (Tree node : nodes) {
                    r = (first ? scan(node, p) : scanAndReduce(node, p, r));
                    first = false;
                }
            }
            return r;
        }

        /**
         * Reduces two results into a combined result. The default
         * implementation is to return the first parameter. The general contract
         * of the method is that it may take any action whatsoever.
         */
        public Result reduce(Result r1, Result r2) {
            if (r1 != null && r2 != null && r1.isPure() && r2.isPure()) {
                return new PureResult();
            } else {
                return new NonPureResult("TODO");
            }
        }

        /* ***************************************************************************
         * Visitor methods
         * *******************************************************
         */

        public Result visitCompilationUnit(CompilationUnitTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitImport(ImportTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitClass(ClassTree node, Result p) {
            return p;
        }

        public Result visitMethod(MethodTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitVariable(VariableTree node, Result p) {
            return scan(node.getInitializer(), p);
        }

        public Result visitEmptyStatement(EmptyStatementTree node, Result p) {
            return p;
        }

        public Result visitBlock(BlockTree node, Result p) {
            return scan(node.getStatements(), p);
        }

        public Result visitDoWhileLoop(DoWhileLoopTree node, Result p) {
            Result r = scan(node.getStatement(), p);
            r = scanAndReduce(node.getCondition(), p, r);
            return r;
        }

        public Result visitWhileLoop(WhileLoopTree node, Result p) {
            Result r = scan(node.getCondition(), p);
            r = scanAndReduce(node.getStatement(), p, r);
            return r;
        }

        public Result visitForLoop(ForLoopTree node, Result p) {
            Result r = scan(node.getInitializer(), p);
            r = scanAndReduce(node.getCondition(), p, r);
            r = scanAndReduce(node.getUpdate(), p, r);
            r = scanAndReduce(node.getStatement(), p, r);
            return r;
        }

        public Result visitEnhancedForLoop(EnhancedForLoopTree node, Result p) {
            Result r = scan(node.getVariable(), p);
            r = scanAndReduce(node.getExpression(), p, r);
            r = scanAndReduce(node.getStatement(), p, r);
            return r;
        }

        public Result visitLabeledStatement(LabeledStatementTree node, Result p) {
            return scan(node.getStatement(), p);
        }

        public Result visitSwitch(SwitchTree node, Result p) {
            Result r = scan(node.getExpression(), p);
            r = scanAndReduce(node.getCases(), p, r);
            return r;
        }

        public Result visitCase(CaseTree node, Result p) {
            Result r = scan(node.getExpression(), p);
            r = scanAndReduce(node.getStatements(), p, r);
            return r;
        }

        public Result visitSynchronized(SynchronizedTree node, Result p) {
            Result r = scan(node.getExpression(), p);
            r = scanAndReduce(node.getBlock(), p, r);
            return r;
        }

        public Result visitTry(TryTree node, Result p) {
            Result r = scan(node.getResources(), p);
            r = scanAndReduce(node.getBlock(), p, r);
            r = scanAndReduce(node.getCatches(), p, r);
            r = scanAndReduce(node.getFinallyBlock(), p, r);
            return r;
        }

        public Result visitCatch(CatchTree node, Result p) {
            Result r = scan(node.getParameter(), p);
            r = scanAndReduce(node.getBlock(), p, r);
            return r;
        }

        public Result visitConditionalExpression(
                ConditionalExpressionTree node, Result p) {
            Result r = scan(node.getCondition(), p);
            r = scanAndReduce(node.getTrueExpression(), p, r);
            r = scanAndReduce(node.getFalseExpression(), p, r);
            return r;
        }

        public Result visitIf(IfTree node, Result p) {
            Result r = scan(node.getCondition(), p);
            r = scanAndReduce(node.getThenStatement(), p, r);
            r = scanAndReduce(node.getElseStatement(), p, r);
            return r;
        }

        public Result visitExpressionStatement(ExpressionStatementTree node,
                Result p) {
            return scan(node.getExpression(), p);
        }

        public Result visitBreak(BreakTree node, Result p) {
            return p;
        }

        public Result visitContinue(ContinueTree node, Result p) {
            return p;
        }

        public Result visitReturn(ReturnTree node, Result p) {
            return scan(node.getExpression(), p);
        }

        public Result visitThrow(ThrowTree node, Result p) {
            return scan(node.getExpression(), p);
        }

        public Result visitAssert(AssertTree node, Result p) {
            Result r = scan(node.getCondition(), p);
            r = scanAndReduce(node.getDetail(), p, r);
            return r;
        }

        public Result visitMethodInvocation(MethodInvocationTree node, Result p) {
            Result r = scan(node.getMethodSelect(), p);
            r = scanAndReduce(node.getArguments(), p, r);
            // TODO: nonpure call
            return r;
        }

        public Result visitNewClass(NewClassTree node, Result p) {
            Result r = scan(node.getEnclosingExpression(), new NonPureResult(
                    "creation of new object", p));
            r = scanAndReduce(node.getIdentifier(), p, r);
            r = scanAndReduce(node.getTypeArguments(), p, r);
            r = scanAndReduce(node.getArguments(), p, r);
            r = scanAndReduce(node.getClassBody(), p, r);
            return r;
        }

        public Result visitNewArray(NewArrayTree node, Result p) {
            Result r = scan(node.getType(), p);
            r = scanAndReduce(node.getDimensions(), p, r);
            r = scanAndReduce(node.getInitializers(), p, r);
            return r;
        }

        public Result visitLambdaExpression(LambdaExpressionTree node, Result p) {
            Result r = scan(node.getParameters(), p);
            r = scanAndReduce(node.getBody(), p, r);
            return r;
        }

        public Result visitParenthesized(ParenthesizedTree node, Result p) {
            return scan(node.getExpression(), p);
        }

        public Result visitAssignment(AssignmentTree node, Result p) {
            ExpressionTree variable = node.getVariable();
            p = assignmentCheck(p, variable);
            Result r = scan(variable, p);
            r = scanAndReduce(node.getExpression(), p, r);
            return r;
        }

        protected Result assignmentCheck(Result p, ExpressionTree variable) {
            if (TreeUtils.isFieldAccess(variable)) {
                // rhs is a field access
                p = new NonPureResult("assignment to field", p);
            } else if (variable instanceof ArrayAccessTree) {
                // allow only local variables for the array
                ArrayAccessTree a = (ArrayAccessTree) variable;
                if (!isLocalVariable(a.getExpression())) {
                    p = new NonPureResult("assignment to array field", p);
                }
            } else {
                // rhs is a local variable
                assert isLocalVariable(variable);
            }
            return p;
        }

        protected boolean isLocalVariable(ExpressionTree variable) {
            return variable instanceof IdentifierTree
                    && !TreeUtils.isFieldAccess(variable);
        }

        public Result visitCompoundAssignment(CompoundAssignmentTree node,
                Result p) {
            ExpressionTree variable = node.getVariable();
            p = assignmentCheck(p, variable);
            Result r = scan(variable, p);
            r = scanAndReduce(node.getExpression(), p, r);
            return r;
        }

        public Result visitUnary(UnaryTree node, Result p) {
            return scan(node.getExpression(), p);
        }

        public Result visitBinary(BinaryTree node, Result p) {
            Result r = scan(node.getLeftOperand(), p);
            r = scanAndReduce(node.getRightOperand(), p, r);
            return r;
        }

        public Result visitTypeCast(TypeCastTree node, Result p) {
            Result r = scan(node.getExpression(), p);
            return r;
        }

        public Result visitInstanceOf(InstanceOfTree node, Result p) {
            Result r = scan(node.getExpression(), p);
            return r;
        }

        public Result visitArrayAccess(ArrayAccessTree node, Result p) {
            Result r = scan(node.getExpression(), p);
            r = scanAndReduce(node.getIndex(), p, r);
            return r;
        }

        public Result visitMemberSelect(MemberSelectTree node, Result p) {
            return scan(node.getExpression(), p);
        }

        public Result visitMemberReference(MemberReferenceTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitIdentifier(IdentifierTree node, Result p) {
            return p;
        }

        public Result visitLiteral(LiteralTree node, Result p) {
            return p;
        }

        public Result visitPrimitiveType(PrimitiveTypeTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitArrayType(ArrayTypeTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitParameterizedType(ParameterizedTypeTree node,
                Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitUnionType(UnionTypeTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitTypeParameter(TypeParameterTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitWildcard(WildcardTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitModifiers(ModifiersTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitAnnotation(AnnotationTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitAnnotatedType(AnnotatedTypeTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitOther(Tree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }

        public Result visitErroneous(ErroneousTree node, Result p) {
            assert false : "this type of tree is unexpected here";
            return null;
        }
    }

}
