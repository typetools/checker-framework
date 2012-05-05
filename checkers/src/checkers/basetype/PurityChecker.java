package checkers.basetype;

import java.util.List;

import javax.lang.model.element.Element;

import checkers.quals.Pure;
import checkers.types.AnnotatedTypeFactory;
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
import com.sun.tools.javac.tree.TreeScanner;

/**
 * A visitor that checks the purity (as defined by {@link checkers.quals.Pure})
 * of a statement or expression.
 * 
 * @see {@link Pure}
 * 
 * @author Stefan Heule
 * 
 */
public class PurityChecker {

    public static Result checkPurity(MethodTree method,
            AnnotatedTypeFactory atypeFactory) {
        PurityCheckerHelper helper = new PurityCheckerHelper(method,
                atypeFactory);
        Result res = helper.scan(method.getBody(), new PureResult());
        return res;
    }

    /**
     * Result of the {@link PurityChecker}.
     */
    public static abstract class Result {

        /** Is the method pure? */
        public abstract boolean isPure();

        /**
         * @return The reason why the method might not be pure (only applicable
         *         if {@code isPure()} returns {@code false}).
         */
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

    /**
     * Helper class to keep {@link PurityChecker}s interface clean. The
     * implementation is heavily based on {@link TreeScanner}, but some parts of
     * the AST are skipped (such as types or modifiers). Furthermore, scanning
     * works differently in that the input parameter (usually named {@code p})
     * gets "threaded through", instead of using {@code reduce}.
     */
    protected static class PurityCheckerHelper implements
            TreeVisitor<Result, Result> {

        protected AnnotatedTypeFactory atypeFactory;
        protected MethodTree method;
        protected/* @Nullable */List<Element> methodParameter;

        public PurityCheckerHelper(MethodTree method,
                AnnotatedTypeFactory atypeFactory) {
            this.atypeFactory = atypeFactory;
            this.method = method;
        }

        /**
         * Scan a single node.
         */
        public Result scan(Tree node, Result p) {
            return node == null ? p : node.accept(this, p);
        }

        /**
         * Scan a list of nodes.
         */
        public Result scan(Iterable<? extends Tree> nodes, Result p) {
            Result r = p;
            if (nodes != null) {
                for (Tree node : nodes) {
                    r = scan(node, r);
                }
            }
            return r;
        }

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
            r = scan(node.getCondition(), r);
            return r;
        }

        public Result visitWhileLoop(WhileLoopTree node, Result p) {
            Result r = scan(node.getCondition(), p);
            r = scan(node.getStatement(), r);
            return r;
        }

        public Result visitForLoop(ForLoopTree node, Result p) {
            Result r = scan(node.getInitializer(), p);
            r = scan(node.getCondition(), r);
            r = scan(node.getUpdate(), r);
            r = scan(node.getStatement(), r);
            return r;
        }

        public Result visitEnhancedForLoop(EnhancedForLoopTree node, Result p) {
            Result r = scan(node.getVariable(), p);
            r = scan(node.getExpression(), r);
            r = scan(node.getStatement(), r);
            return r;
        }

        public Result visitLabeledStatement(LabeledStatementTree node, Result p) {
            return scan(node.getStatement(), p);
        }

        public Result visitSwitch(SwitchTree node, Result p) {
            Result r = scan(node.getExpression(), p);
            r = scan(node.getCases(), r);
            return r;
        }

        public Result visitCase(CaseTree node, Result p) {
            Result r = scan(node.getExpression(), p);
            r = scan(node.getStatements(), r);
            return r;
        }

        public Result visitSynchronized(SynchronizedTree node, Result p) {
            Result r = scan(node.getExpression(), p);
            r = scan(node.getBlock(), r);
            return r;
        }

        public Result visitTry(TryTree node, Result p) {
            Result r = scan(node.getResources(), p);
            r = scan(node.getBlock(), r);
            r = scan(node.getCatches(), r);
            r = scan(node.getFinallyBlock(), r);
            return r;
        }

        public Result visitCatch(CatchTree node, Result p) {
            Result r = scan(node.getParameter(), p);
            r = scan(node.getBlock(), r);
            return r;
        }

        public Result visitConditionalExpression(
                ConditionalExpressionTree node, Result p) {
            Result r = scan(node.getCondition(), p);
            r = scan(node.getTrueExpression(), r);
            r = scan(node.getFalseExpression(), r);
            return r;
        }

        public Result visitIf(IfTree node, Result p) {
            Result r = scan(node.getCondition(), p);
            r = scan(node.getThenStatement(), r);
            r = scan(node.getElseStatement(), r);
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
            r = scan(node.getDetail(), r);
            return r;
        }

        public Result visitMethodInvocation(MethodInvocationTree node, Result p) {
            Element elt = TreeUtils.elementFromUse(node);
            boolean isPureCall = atypeFactory
                    .getDeclAnnotation(elt, Pure.class) != null;
            if (!isPureCall) {
                p = new NonPureResult("non-pure method call", p);
            }
            Result r = scan(node.getMethodSelect(), p);
            r = scan(node.getArguments(), r);
            return r;
        }

        public Result visitNewClass(NewClassTree node, Result p) {
            Result r = scan(node.getEnclosingExpression(), new NonPureResult(
                    "creation of new object", p));
            r = scan(node.getIdentifier(), r);
            r = scan(node.getTypeArguments(), r);
            r = scan(node.getArguments(), r);
            r = scan(node.getClassBody(), r);
            return r;
        }

        public Result visitNewArray(NewArrayTree node, Result p) {
            Result r = scan(node.getType(), p);
            r = scan(node.getDimensions(), r);
            r = scan(node.getInitializers(), r);
            return r;
        }

        public Result visitLambdaExpression(LambdaExpressionTree node, Result p) {
            Result r = scan(node.getParameters(), p);
            r = scan(node.getBody(), r);
            return r;
        }

        public Result visitParenthesized(ParenthesizedTree node, Result p) {
            return scan(node.getExpression(), p);
        }

        public Result visitAssignment(AssignmentTree node, Result p) {
            ExpressionTree variable = node.getVariable();
            p = assignmentCheck(p, variable);
            Result r = scan(variable, p);
            r = scan(node.getExpression(), r);
            return r;
        }

        protected Result assignmentCheck(Result p, ExpressionTree variable) {
            if (TreeUtils.isFieldAccess(variable)) {
                // rhs is a field access
                p = new NonPureResult("assignment to field '"
                        + TreeUtils.getFieldName(variable) + "'", p);
            } else if (variable instanceof ArrayAccessTree) {
                // rhs is array access
                ArrayAccessTree a = (ArrayAccessTree) variable;
                p = new NonPureResult("assignment to array '"
                        + a.getExpression() + "'", p);
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
            r = scan(node.getExpression(), r);
            return r;
        }

        public Result visitUnary(UnaryTree node, Result p) {
            return scan(node.getExpression(), p);
        }

        public Result visitBinary(BinaryTree node, Result p) {
            Result r = scan(node.getLeftOperand(), p);
            r = scan(node.getRightOperand(), r);
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
            r = scan(node.getIndex(), r);
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
