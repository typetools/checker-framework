package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.type.ArrayType;
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
import com.sun.source.tree.ExportsTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.OpensTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ProvidesTree;
import com.sun.source.tree.RequiresTree;
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
import com.sun.source.tree.UsesTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.checkerframework.javacutil.BugInCF;

public class JointJavacJavaParserVisitor implements TreeVisitor<Void, Node> {
    @Override
    public Void visitAnnotation(AnnotationTree javacTree, Node javaParserNode) {
        // It seems javac stores annotation arguments assignments, so @MyAnno("myArg") might be
        // stored the same as @MyAnno(value="myArg") which has a single element argument list with
        // an assignment.
        // TODO: Visit name trees for name=value assignments in annotations?
        if (javaParserNode instanceof MarkerAnnotationExpr) {
            processAnnotation(javacTree, (MarkerAnnotationExpr) javaParserNode);
        } else if (javaParserNode instanceof SingleMemberAnnotationExpr) {
            SingleMemberAnnotationExpr node = (SingleMemberAnnotationExpr) javaParserNode;
            processAnnotation(javacTree, node);
            assert javacTree.getArguments().size() == 1;
            ExpressionTree value = javacTree.getArguments().get(0);
            assert value instanceof AssignmentTree;
            AssignmentTree assignment = (AssignmentTree) value;
            // TODO: Is getMemberValue actually the expression value?
            assignment.getExpression().accept(this, node.getMemberValue());
        } else if (javaParserNode instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr node = (NormalAnnotationExpr) javaParserNode;
            processAnnotation(javacTree, node);
            assert javacTree.getArguments().size() == node.getPairs().size();
            Iterator<MemberValuePair> argIter = node.getPairs().iterator();
            for (ExpressionTree arg : javacTree.getArguments()) {
                assert arg instanceof AssignmentTree;
                AssignmentTree assignment = (AssignmentTree) arg;
                assignment.getExpression().accept(this, argIter.next().getValue());
            }
        } else {
            throwUnexpectedNodeType(javaParserNode);
        }

        return null;
    }

    @Override
    public Void visitAnnotatedType(AnnotatedTypeTree javacTree, Node javaParserNode) {
        // TODO: can't generate a tree of this type in tests.
        // TODO: There doesn't seem to be a JavaParser equivalent, I think it instead adds the
        // NodeWithAnnotations interface to types that may have annotations. How to deal with that?
        return null;
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ArrayAccessExpr)) {
            throwUnexpectedNodeType(javaParserNode, ArrayAccessExpr.class);
        }

        ArrayAccessExpr node = (ArrayAccessExpr) javaParserNode;
        processArrayAccess(javacTree, node);
        javacTree.getExpression().accept(this, node.getName());
        javacTree.getIndex().accept(this, node.getIndex());
        return null;
    }

    @Override
    public Void visitArrayType(ArrayTypeTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ArrayType)) {
            throwUnexpectedNodeType(javaParserNode, ArrayType.class);
        }

        ArrayType node = (ArrayType) javaParserNode;
        processArrayType(javacTree, node);
        javacTree.getType().accept(this, node.getComponentType());
        return null;
    }

    @Override
    public Void visitAssert(AssertTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof AssertStmt)) {
            throwUnexpectedNodeType(javaParserNode, AssertStmt.class);
        }

        AssertStmt node = (AssertStmt) javaParserNode;
        processAssert(javacTree, node);
        javacTree.getCondition().accept(this, node.getCheck());
        ExpressionTree detail = javacTree.getDetail();
        assert (detail != null) == node.getMessage().isPresent();
        if (detail != null) {
            detail.accept(this, node.getMessage().get());
        }

        return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof AssignExpr)) {
            throwUnexpectedNodeType(javaParserNode, AssignExpr.class);
        }

        AssignExpr node = (AssignExpr) javaParserNode;
        processAssignment(javacTree, node);
        javacTree.getVariable().accept(this, node.getTarget());
        javacTree.getExpression().accept(this, node.getValue());
        return null;
    }

    @Override
    public Void visitBinary(BinaryTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof BinaryExpr)) {
            throwUnexpectedNodeType(javaParserNode, BinaryExpr.class);
        }

        // TODO: Check that the operator types match?
        BinaryExpr node = (BinaryExpr) javaParserNode;
        processBinary(javacTree, node);
        javacTree.getLeftOperand().accept(this, node.getLeft());
        javacTree.getRightOperand().accept(this, node.getRight());
        return null;
    }

    @Override
    public Void visitBlock(BlockTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof BlockStmt)) {
            throwUnexpectedNodeType(javaParserNode, BlockStmt.class);
        }

        BlockStmt node = (BlockStmt) javaParserNode;
        processBlock(javacTree, node);
        visitLists(javacTree.getStatements(), node.getStatements());
        return null;
    }

    @Override
    public Void visitBreak(BreakTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof BreakStmt)) {
            throwUnexpectedNodeType(javaParserNode, BreakStmt.class);
        }

        processBreak(javacTree, (BreakStmt) javaParserNode);
        return null;
    }

    @Override
    public Void visitCase(CaseTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof SwitchEntry)) {
            throwUnexpectedNodeType(javaParserNode, SwitchEntry.class);
        }

        SwitchEntry node = (SwitchEntry) javaParserNode;
        processCase(javacTree, node);
        // The expression is null if and only if the case is the default case.
        // Java 12 introduced multiple label cases, but expressions should contain at most one
        // element for Java 11 and below.
        List<Expression> expressions = node.getLabels();
        if (javacTree.getExpression() == null) {
            assert expressions.isEmpty();
        } else {
            assert expressions.size() == 1;
            javacTree.getExpression().accept(this, expressions.get(0));
        }

        visitLists(javacTree.getStatements(), node.getStatements());
        return null;
    }

    @Override
    public Void visitCatch(CatchTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof CatchClause)) {
            throwUnexpectedNodeType(javaParserNode, CatchClause.class);
        }

        CatchClause node = (CatchClause) javaParserNode;
        processCatch(javacTree, node);
        javacTree.getParameter().accept(this, node.getParameter());
        javacTree.getBlock().accept(this, node.getBody());
        return null;
    }

    @Override
    public Void visitClass(ClassTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ClassOrInterfaceDeclaration)) {
            throwUnexpectedNodeType(javaParserNode, ClassOrInterfaceDeclaration.class);
        }

        ClassOrInterfaceDeclaration node = (ClassOrInterfaceDeclaration) javaParserNode;
        processClass(javacTree, node);
        visitLists(javacTree.getTypeParameters(), node.getTypeParameters());
        if (javacTree.getExtendsClause() == null) {
            assert node.getExtendedTypes().isEmpty();
        } else {
            assert node.getExtendedTypes().size() == 1;
            javacTree.getExtendsClause().accept(this, node.getExtendedTypes().get(0));
        }

        visitLists(javacTree.getImplementsClause(), node.getImplementedTypes());
        List<? extends Tree> javacMembers = new ArrayList<>(javacTree.getMembers());
        // TODO: Fix this, find a better way.
        // The javac members might have an artificially generated default constructor, remove it
        // from the list if present.
        if (javacMembers.size() == node.getMembers().size() + 1) {
            javacMembers.remove(0);
        }
        assert javacMembers.size() == node.getMembers().size();
        visitLists(javacMembers, node.getMembers());
        return null;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof CompilationUnit)) {
            throwUnexpectedNodeType(javaParserNode, CompilationUnit.class);
        }

        CompilationUnit node = (CompilationUnit) javaParserNode;
        processCompilationUnit(javacTree, node);

        // TODO: A CompilationUnitTree could also be a package-info.java file. Currently skipping
        // descending into these specific constructs such as getPackageAnnotations, because they
        // probably won't be useful and TreeScanner also skips them. Should we process them?
        // TODO: Why is this called getPackageName? Does it always return a PackageTree?
        assert (javacTree.getPackageName() != null) == node.getPackageDeclaration().isPresent();
        if (javacTree.getPackageName() != null) {
            javacTree.getPackageName().accept(this, node.getPackageDeclaration().get());
        }

        visitLists(javacTree.getImports(), node.getImports());
        visitLists(javacTree.getTypeDecls(), node.getTypes());
        return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof AssignExpr)) {
            throwUnexpectedNodeType(javaParserNode, AssignExpr.class);
        }

        AssignExpr node = (AssignExpr) javaParserNode;
        javacTree.getVariable().accept(this, node.getTarget());
        javacTree.getExpression().accept(this, node.getValue());
        return null;
    }

    @Override
    public Void visitConditionalExpression(
            ConditionalExpressionTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ConditionalExpr)) {
            throwUnexpectedNodeType(javaParserNode, ConditionalExpr.class);
        }

        ConditionalExpr node = (ConditionalExpr) javaParserNode;
        processConditionalExpression(javacTree, node);
        javacTree.getCondition().accept(this, node.getCondition());
        javacTree.getTrueExpression().accept(this, node.getThenExpr());
        javacTree.getFalseExpression().accept(this, node.getElseExpr());
        return null;
    }

    @Override
    public Void visitContinue(ContinueTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ContinueStmt)) {
            throwUnexpectedNodeType(javaParserNode, ContinueStmt.class);
        }

        ContinueStmt node = (ContinueStmt) javaParserNode;
        processContinue(javacTree, node);
        return null;
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof DoStmt)) {
            throwUnexpectedNodeType(javaParserNode, DoStmt.class);
        }

        DoStmt node = (DoStmt) javaParserNode;
        processDoWhileLoop(javacTree, node);
        javacTree.getCondition().accept(this, node.getCondition());
        javacTree.getStatement().accept(this, node.getBody());
        return null;
    }

    @Override
    public Void visitEmptyStatement(EmptyStatementTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof EmptyStmt)) {
            throwUnexpectedNodeType(javaParserNode, EmptyStmt.class);
        }

        processEmptyStatement(javacTree, (EmptyStmt) javaParserNode);
        return null;
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ForEachStmt)) {
            throwUnexpectedNodeType(javaParserNode, ForEachStmt.class);
        }

        ForEachStmt node = (ForEachStmt) javaParserNode;
        processEnhancedForLoop(javacTree, node);
        javacTree.getVariable().accept(this, node.getVariable());
        javacTree.getExpression().accept(this, node.getIterable());
        javacTree.getStatement().accept(this, node.getBody());
        return null;
    }

    @Override
    public Void visitErroneous(ErroneousTree javacTree, Node javaParserNode) {
        // An erroneous tree is a malformed expression, so skip.
        return null;
    }

    @Override
    public Void visitExports(ExportsTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ModuleExportsDirective)) {
            throwUnexpectedNodeType(javaParserNode, ModuleExportsDirective.class);
        }

        ModuleExportsDirective node = (ModuleExportsDirective) javaParserNode;
        processExports(javacTree, node);
        visitLists(javacTree.getModuleNames(), node.getModuleNames());
        // TODO: I'm not sure if getName is the correct method to use here.
        javacTree.getPackageName().accept(this, node.getName());
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ExpressionStmt)) {
            throwUnexpectedNodeType(javaParserNode, ExpressionStmt.class);
        }

        ExpressionStmt node = (ExpressionStmt) javaParserNode;
        processExpressionStatemen(javacTree, node);
        javacTree.getExpression().accept(this, node.getExpression());
        return null;
    }

    @Override
    public Void visitForLoop(ForLoopTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ForStmt)) {
            throwUnexpectedNodeType(javaParserNode, ForStmt.class);
        }

        ForStmt node = (ForStmt) javaParserNode;
        processForLoop(javacTree, node);
        visitLists(javacTree.getInitializer(), node.getInitialization());
        assert (javacTree.getCondition() != null) == node.getCompare().isPresent();
        if (javacTree.getCondition() != null) {
            javacTree.getCondition().accept(this, node.getCompare().get());
        }

        visitLists(javacTree.getUpdate(), node.getUpdate());
        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitIf(IfTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitImport(ImportTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitIntersectionType(IntersectionTypeTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitLabeledStatement(LabeledStatementTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitLiteral(LiteralTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitMethod(MethodTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitModifiers(ModifiersTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitModule(ModuleTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitNewArray(NewArrayTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitNewClass(NewClassTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitOpens(OpensTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitOther(Tree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitPackage(PackageTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitParenthesized(ParenthesizedTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitProvides(ProvidesTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitRequires(RequiresTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitSwitch(SwitchTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitSynchronized(SynchronizedTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitThrow(ThrowTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitTry(TryTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitTypeCast(TypeCastTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitTypeParameter(TypeParameterTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitUnary(UnaryTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitUnionType(UnionTypeTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitUses(UsesTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitVariable(VariableTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitWildcard(WildcardTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    public void processAnnotation(AnnotationTree javacTree, NormalAnnotationExpr javaParserNode) {}

    public void processAnnotation(AnnotationTree javacTree, MarkerAnnotationExpr javaParserNode) {}

    public void processAnnotation(
            AnnotationTree javacTree, SingleMemberAnnotationExpr javaParserNode) {}

    public void processArrayAccess(ArrayAccessTree javacTree, ArrayAccessExpr javaParserNode) {}

    public void processArrayType(ArrayTypeTree javacTree, ArrayType javaParserNode) {}

    public void processAssert(AssertTree javacTree, AssertStmt javaParserNode) {}

    public void processAssignment(AssignmentTree javacTree, AssignExpr javaParserNode) {}

    public void processBinary(BinaryTree javacTree, BinaryExpr javaParserNode) {}

    public void processBlock(BlockTree javacTree, BlockStmt javaParserNode) {}

    public void processBreak(BreakTree javacTree, BreakStmt javaParserNode) {}

    public void processCase(CaseTree javacTree, SwitchEntry javaParserNode) {}

    public void processCatch(CatchTree javacTree, CatchClause javaParserNode) {}

    public void processClass(ClassTree javacTree, ClassOrInterfaceDeclaration javaParserNode) {}

    public void processCompilationUnit(
            CompilationUnitTree javacTree, CompilationUnit javaParserNode) {}

    public void processConditionalExpression(
            ConditionalExpressionTree javacTree, ConditionalExpr javaParserNode) {}

    public void processContinue(ContinueTree javacTree, ContinueStmt javaParserNode) {}

    public void processDoWhileLoop(DoWhileLoopTree javacTree, DoStmt javaParserNode) {}

    public void processEmptyStatement(EmptyStatementTree javacTree, EmptyStmt javaParserNode) {}

    public void processEnhancedForLoop(EnhancedForLoopTree javacTree, ForEachStmt javaParserNode) {}

    public void processExports(ExportsTree javacTree, ModuleExportsDirective javaParserNode) {}

    public void processExpressionStatemen(
            ExpressionStatementTree javacTree, ExpressionStmt javaParserNode) {}

    public void processForLoop(ForLoopTree javacTree, ForStmt javaParserNode) {}

    // TODO: Documentation on how to use getKind to determine the type of compound assignment like
    // in the javadoc for CompoundAssignmentTree. You could also get it from the javaparser node.
    public void processCompoundAssignment(
            CompoundAssignmentTree javacTree, AssignExpr javaParserNode) {}

    private void visitLists(List<? extends Tree> javacTrees, List<? extends Node> javaParserNodes) {
        assert javacTrees.size() == javaParserNodes.size();
        Iterator<? extends Node> nodeIter = javaParserNodes.iterator();
        for (Tree tree : javacTrees) {
            tree.accept(this, nodeIter.next());
        }
    }

    private void throwUnexpectedNodeType(Node javaParserNode) {
        throw new BugInCF(
                "Javac and JavaParser trees desynced, unexpected node type: %s",
                javaParserNode.getClass());
    }

    private void throwUnexpectedNodeType(Node javaParserNode, Class<?> expectedType) {
        throw new BugInCF(
                "Javac and JavaParser trees desynced, expected: %s, actual: %s",
                expectedType, javaParserNode.getClass());
    }
}
