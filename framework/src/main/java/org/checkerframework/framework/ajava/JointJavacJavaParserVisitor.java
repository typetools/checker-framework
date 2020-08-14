package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
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
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
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
import java.util.Iterator;
import java.util.List;
import org.checkerframework.javacutil.BugInCF;

public class JointJavacJavaParserVisitor implements TreeVisitor<Void, Node> {
    @Override
    public Void visitAnnotation(AnnotationTree javacTree, Node javaParserNode) {
        // It seems javac stores annotation arguments assignments, so @MyAnno("myArg") might be
        // stored the same as @MyAnno(value="myArg") which has a single element argument list with
        // an assignment.
        if (javaParserNode instanceof MarkerAnnotationExpr) {
            processAnnotation(javacTree, (MarkerAnnotationExpr) javaParserNode);
        } else if (javaParserNode instanceof SingleMemberAnnotationExpr) {
            SingleMemberAnnotationExpr node = (SingleMemberAnnotationExpr) javaParserNode;
            processAnnotation(javacTree, node);
            assert javacTree.getArguments().size() == 1;
            ExpressionTree value = javacTree.getArguments().get(0);
            assert value instanceof AssignmentTree;
            AssignmentTree assignment = (AssignmentTree) value;
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
        // In javac, a type like @Tainted String would be an IdentifierTree for String stored in an
        // AnnotatedTypeTree, whereas in JavaParser it would be a single ClassOrIntefaceType that
        // stores the annotations. For types with annotations we must unwrap the inner type in
        // javac. As a result, the JavaParserNode may be visited twice, once for the outer type and
        // once for the inner type.
        if (!(javaParserNode instanceof NodeWithAnnotations)) {
            throwUnexpectedNodeType(javaParserNode, NodeWithAnnotations.class);
        }

        processAnnotatedType(javacTree, javaParserNode);
        NodeWithAnnotations<?> node = (NodeWithAnnotations<?>) javaParserNode;
        visitLists(javacTree.getAnnotations(), node.getAnnotations());
        javacTree.getUnderlyingType().accept(this, javaParserNode);
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
        Iterator<? extends StatementTree> javacIter = javacTree.getStatements().iterator();
        boolean hasNextJavac = javacIter.hasNext();
        StatementTree javacStatement = hasNextJavac ? javacIter.next() : null;

        Iterator<Statement> javaParserIter = node.getStatements().iterator();
        boolean hasNextJavaParser = javaParserIter.hasNext();
        Statement javaParserStatement = hasNextJavaParser ? javaParserIter.next() : null;

        while (hasNextJavac || hasNextJavaParser) {
            // Skip synthetic javac super() calls by checking if the JavaParser statement matches.
            if (hasNextJavac
                    && isDefaultSuperConstructorCall(javacStatement)
                    && (!hasNextJavaParser
                            || !isDefaultSuperConstructorCall(javaParserStatement))) {
                continue;
            }

            // In javac, a line like int i = 0, j = 0 is expanded as two sibling VariableTree
            // instances. In javaParser this is one VariableDeclarationExpr with two nested
            // VariableDeclarators. Match the declarators with the VariableTrees.
            if (hasNextJavaParser
                    && javaParserStatement.isExpressionStmt()
                    && javaParserStatement
                            .asExpressionStmt()
                            .getExpression()
                            .isVariableDeclarationExpr()) {
                for (VariableDeclarator decl :
                        javaParserStatement
                                .asExpressionStmt()
                                .getExpression()
                                .asVariableDeclarationExpr()
                                .getVariables()) {
                    System.out.println("Processing decl: " + decl);
                    System.out.println("javacStatement currently: " + javacStatement);
                    assert hasNextJavac;
                    assert javacStatement.getKind() == Kind.VARIABLE;
                    javacStatement.accept(this, decl);
                    hasNextJavac = javacIter.hasNext();
                    javacStatement = hasNextJavac ? javacIter.next() : null;
                }

                hasNextJavaParser = javaParserIter.hasNext();
                javaParserStatement = hasNextJavaParser ? javaParserIter.next() : null;
                continue;
            }

            assert hasNextJavac;
            assert hasNextJavaParser;
            javacStatement.accept(this, javaParserStatement);
            hasNextJavac = javacIter.hasNext();
            javacStatement = hasNextJavac ? javacIter.next() : null;

            hasNextJavaParser = javaParserIter.hasNext();
            javaParserStatement = hasNextJavaParser ? javaParserIter.next() : null;
        }

        assert !hasNextJavac;
        assert !hasNextJavaParser;
        return null;
    }

    private boolean isDefaultSuperConstructorCall(StatementTree statement) {
        if (statement.getKind() != Kind.EXPRESSION_STATEMENT) {
            return false;
        }

        ExpressionStatementTree expressionStatement = (ExpressionStatementTree) statement;
        if (expressionStatement.getExpression().getKind() != Kind.METHOD_INVOCATION) {
            return false;
        }

        MethodInvocationTree invocation =
                (MethodInvocationTree) expressionStatement.getExpression();
        if (invocation.getMethodSelect().getKind() != Kind.IDENTIFIER) {
            return false;
        }

        if (!((IdentifierTree) invocation.getMethodSelect()).getName().contentEquals("super")) {
            return false;
        }

        return invocation.getArguments().isEmpty();
    }

    private boolean isDefaultSuperConstructorCall(Statement statement) {
        if (!statement.isExplicitConstructorInvocationStmt()) {
            return false;
        }

        ExplicitConstructorInvocationStmt invocation =
                statement.asExplicitConstructorInvocationStmt();
        return !invocation.isThis() && invocation.getArguments().isEmpty();
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
        visitClassMembers(javacTree.getMembers(), node.getMembers());
        return null;
    }

    private void visitClassMembers(
            List<? extends Tree> javacMembers, List<BodyDeclaration<?>> javaParserMembers) {
        // The javac members might have an artificially generated default constructor, don't process
        // it if present.
        Iterator<BodyDeclaration<?>> javaParserIter = javaParserMembers.iterator();
        boolean hasNextJavaParser = javaParserIter.hasNext();
        BodyDeclaration<?> javaParserStatement = hasNextJavaParser ? javaParserIter.next() : null;
        for (Tree javacMember : javacMembers) {
            if (isNoArgumentConstructor(javacMember)
                    && (!hasNextJavaParser || !isNoArgumentConstructor(javaParserStatement))) {
                continue;
            }

            assert hasNextJavaParser;
            javacMember.accept(this, javaParserStatement);
            hasNextJavaParser = javaParserIter.hasNext();
            javaParserStatement = hasNextJavaParser ? javaParserIter.next() : null;
        }

        assert !hasNextJavaParser;
    }

    private boolean isNoArgumentConstructor(Tree member) {
        if (member.getKind() != Kind.METHOD) {
            return false;
        }

        MethodTree methodTree = (MethodTree) member;
        return methodTree.getName().contentEquals("<init>") && methodTree.getParameters().isEmpty();
    }

    private boolean isNoArgumentConstructor(BodyDeclaration<?> member) {
        return member.isConstructorDeclaration()
                && member.asConstructorDeclaration().getParameters().isEmpty();
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

        processContinue(javacTree, (ContinueStmt) javaParserNode);
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

        // Javac stores a list of expression statements and JavaParser stores a list of statements,
        // the javac statements must be unwrapped.
        assert javacTree.getUpdate().size() == node.getUpdate().size();
        Iterator<Expression> javaParserIter = node.getUpdate().iterator();
        for (ExpressionStatementTree update : javacTree.getUpdate()) {
            update.getExpression().accept(this, javaParserIter.next());
        }

        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree arg0, Node arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitIf(IfTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof IfStmt)) {
            throwUnexpectedNodeType(javaParserNode, IfStmt.class);
        }

        IfStmt node = (IfStmt) javaParserNode;
        processIf(javacTree, node);
        javacTree.getCondition().accept(this, node.getCondition());
        javacTree.getThenStatement().accept(this, node.getThenStmt());
        assert (javacTree.getElseStatement() != null) == node.getElseStmt().isPresent();
        if (javacTree.getElseStatement() != null) {
            javacTree.getElseStatement().accept(this, node.getElseStmt().get());
        }

        return null;
    }

    @Override
    public Void visitImport(ImportTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ImportDeclaration)) {
            throwUnexpectedNodeType(javaParserNode, ImportDeclaration.class);
        }

        ImportDeclaration node = (ImportDeclaration) javaParserNode;
        processImport(javacTree, node);
        javacTree.getQualifiedIdentifier().accept(this, node.getName());
        return null;
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof InstanceOfExpr)) {
            throwUnexpectedNodeType(javaParserNode, InstanceOfExpr.class);
        }

        InstanceOfExpr node = (InstanceOfExpr) javaParserNode;
        processInstanceOf(javacTree, node);
        javacTree.getExpression().accept(this, node.getExpression());
        javacTree.getType().accept(this, node.getType());
        return null;
    }

    @Override
    public Void visitIntersectionType(IntersectionTypeTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof IntersectionType)) {
            throwUnexpectedNodeType(javaParserNode, IntersectionType.class);
        }

        IntersectionType node = (IntersectionType) javaParserNode;
        processIntersectionType(javacTree, node);
        visitLists(javacTree.getBounds(), node.getElements());
        return null;
    }

    @Override
    public Void visitLabeledStatement(LabeledStatementTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof LabeledStmt)) {
            throwUnexpectedNodeType(javaParserNode, LabeledStmt.class);
        }

        LabeledStmt node = (LabeledStmt) javaParserNode;
        processLabeledStatement(javacTree, node);
        javacTree.getStatement().accept(this, node.getStatement());
        return null;
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof LambdaExpr)) {
            throwUnexpectedNodeType(javaParserNode, LambdaExpr.class);
        }

        LambdaExpr node = (LambdaExpr) javaParserNode;
        processLambdaExpression(javacTree, node);
        visitLists(javacTree.getParameters(), node.getParameters());
        javacTree.getBody().accept(this, node.getBody());
        return null;
    }

    @Override
    public Void visitLiteral(LiteralTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof LiteralExpr)) {
            throwUnexpectedNodeType(javaParserNode, LiteralExpr.class);
        }

        processLiteral(javacTree, (LiteralExpr) javaParserNode);
        return null;
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof MethodReferenceExpr)) {
            throwUnexpectedNodeType(javaParserNode, MethodReferenceExpr.class);
        }

        MethodReferenceExpr node = (MethodReferenceExpr) javaParserNode;
        processMemberReference(javacTree, node);
        // TODO: Is getScope the correct method here?
        javacTree.getQualifierExpression().accept(this, node.getScope());
        assert (javacTree.getTypeArguments() != null) == node.getTypeArguments().isPresent();
        if (javacTree.getTypeArguments() != null) {
            visitLists(javacTree.getTypeArguments(), node.getTypeArguments().get());
        }

        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree arg0, Node arg1) {
        // TODO: a member select tree can be many things, see the javadoc which references JLS
        // sections. For example, it could be a field access, method invocations, each of which
        // javaparser has its own types for. The hardest may be something like java.lang.String,
        // which javac stores as member references but javaparser uses nested ClassOrInterfaceTypes
        // for.
        return null;
    }

    @Override
    public Void visitMethod(MethodTree javacTree, Node javaParserNode) {
        if (javaParserNode instanceof CallableDeclaration) {}

        // TODO: Use methods like isMethodDeclaration and asMethodDeclaration here.
        if (javaParserNode instanceof MethodDeclaration) {
            return visitMethodForMethodDeclaration(javacTree, (MethodDeclaration) javaParserNode);
        }

        if (javaParserNode instanceof ConstructorDeclaration) {
            return visitMethodForConstructorDeclaration(
                    javacTree, (ConstructorDeclaration) javaParserNode);
        }

        if (javaParserNode instanceof AnnotationMemberDeclaration) {
            return visitMethodForAnnotationMemberDeclaration(
                    javacTree, (AnnotationMemberDeclaration) javaParserNode);
        }

        throwUnexpectedNodeType(javaParserNode);
        return null;
    }

    private Void visitMethodForMethodDeclaration(
            MethodTree javacTree, MethodDeclaration javaParserNode) {
        processMethod(javacTree, javaParserNode);
        // TODO: Handle modifiers. In javac this is a ModifiersTree but in JavaParser it's a list of
        // modifiers. This is a problem because a ModifiersTree has separate accessors to
        // annotations and other modifiers, so the order doesn't match. It might be that for
        // JavaParser, the annotations and other modifiers are also accessed separately.
        // TODO: Is getType the correct method here?
        javacTree.getReturnType().accept(this, javaParserNode.getType());
        // Unlike other constructs, the list is non-null even if no type parameters are present.
        visitLists(javacTree.getTypeParameters(), javaParserNode.getTypeParameters());
        assert (javacTree.getReceiverParameter() != null)
                == javaParserNode.getReceiverParameter().isPresent();
        if (javacTree.getReceiverParameter() != null) {
            javacTree
                    .getReceiverParameter()
                    .accept(this, javaParserNode.getReceiverParameter().get());
        }

        // TODO: Do both lists exclude the receiver?
        visitLists(javacTree.getParameters(), javaParserNode.getParameters());

        visitLists(javacTree.getThrows(), javaParserNode.getThrownExceptions());
        assert (javacTree.getBody() != null) == javaParserNode.getBody().isPresent();
        if (javacTree.getBody() != null) {
            javacTree.getBody().accept(this, javaParserNode.getBody().get());
        }

        return null;
    }

    private Void visitMethodForConstructorDeclaration(
            MethodTree javacTree, ConstructorDeclaration javaParserNode) {
        processMethod(javacTree, javaParserNode);
        // TODO: Handle modifiers.
        // Unlike other constructs, the list is non-null even if no type parameters are present.
        visitLists(javacTree.getTypeParameters(), javaParserNode.getTypeParameters());
        // TODO: For constructors, when is the receiver present? Always? Never?
        assert (javacTree.getReceiverParameter() != null)
                == javaParserNode.getReceiverParameter().isPresent();
        if (javacTree.getReceiverParameter() != null) {
            javacTree
                    .getReceiverParameter()
                    .accept(this, javaParserNode.getReceiverParameter().get());
        }

        visitLists(javacTree.getParameters(), javaParserNode.getParameters());
        visitLists(javacTree.getThrows(), javaParserNode.getThrownExceptions());
        javacTree.getBody().accept(this, javaParserNode.getBody());
        return null;
    }

    private Void visitMethodForAnnotationMemberDeclaration(
            MethodTree javacTree, AnnotationMemberDeclaration javaParserNode) {
        processMethod(javacTree, javaParserNode);
        // TODO: Handle modifiers. See corresponding comment in above method.
        javacTree.getReturnType().accept(this, javaParserNode.getType());
        assert (javacTree.getDefaultValue() != null)
                == javaParserNode.getDefaultValue().isPresent();
        if (javacTree.getDefaultValue() != null) {
            javacTree.getDefaultValue().accept(this, javaParserNode.getDefaultValue().get());
        }

        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof MethodCallExpr)) {
            throwUnexpectedNodeType(javaParserNode, MethodCallExpr.class);
        }

        MethodCallExpr node = (MethodCallExpr) javaParserNode;
        processMethodInvocation(javacTree, node);
        // In javac, the type arguments will be empty even if no type arguments are specified, but
        // in JavaParser the type arguments will have the none Optional value.
        if (javacTree.getTypeArguments().isEmpty()) {
            assert node.getTypeArguments().isEmpty();
        } else {
            assert node.getTypeArguments().isPresent();
            visitLists(javacTree.getTypeArguments(), node.getTypeArguments().get());
        }

        // TODO: Handle method select. In javac both the receiver and method name are stored in
        // getMemberSelect(). If there's no explicit receiver, this may return a single
        // IdentifierTree with the method name. In JavaParser, the method name is always stored in
        // the MethodCallExpr itself and the receiver is an optional that may or may not be present.
        visitLists(javacTree.getArguments(), node.getArguments());
        return null;
    }

    @Override
    public Void visitModifiers(ModifiersTree arg0, Node arg1) {
        // TODO How to handle this? I don't think there's a corresponding JavaParser class, maybe
        // the NodeWithModifiers interface?
        return null;
    }

    @Override
    public Void visitModule(ModuleTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ModuleDeclaration)) {
            throwUnexpectedNodeType(javaParserNode, ModuleDeclaration.class);
        }

        ModuleDeclaration node = (ModuleDeclaration) javaParserNode;
        processModule(javacTree, node);
        visitLists(javacTree.getAnnotations(), node.getAnnotations());
        javacTree.getName().accept(this, node.getName());
        return null;
    }

    @Override
    public Void visitNewArray(NewArrayTree javacTree, Node javaParserNode) {
        // TODO: Implement this. Some notes:
        // - javacTree.getAnnotations() seems to always return empty, any annotations on the base
        // type seem to go on the type itself in javacTree.getType(). The JavaParser version doesn't
        // even have a corresponding getAnnotations method.
        // - When there are no initializers, both systems use similar representations. The
        // dimensions line up.
        // - When there is an initializer, they differ greatly for multi-dimensional arrays. Javac
        // turns an expression like new int[][]{{1, 2}, {3, 4}} into a single NewArray tree with
        // type int[] and two initializer elements {1, 2} and {3, 4}. However, for each of the
        // sub-initializers, it creates an implicit NewArray tree with a null component type.
        // JavaParser keeps the whole expression as one ArrayCreationExpr with multiple dimensions
        // and the initializer stored in special ArrayInitializerExpr type.
        return null;
    }

    @Override
    public Void visitNewClass(NewClassTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ObjectCreationExpr)) {
            throwUnexpectedNodeType(javaParserNode, ObjectCreationExpr.class);
        }

        ObjectCreationExpr node = (ObjectCreationExpr) javaParserNode;
        processNewClass(javacTree, node);
        assert (javacTree.getEnclosingExpression() != null) == node.getScope().isPresent();
        if (javacTree.getEnclosingExpression() != null) {
            javacTree.getEnclosingExpression().accept(this, node.getScope().get());
        }

        javacTree.getIdentifier().accept(this, node.getType());
        if (javacTree.getTypeArguments().isEmpty()) {
            assert node.getTypeArguments().isEmpty();
        } else {
            assert node.getTypeArguments().isPresent();
            visitLists(javacTree.getTypeArguments(), node.getTypeArguments().get());
        }

        visitLists(javacTree.getArguments(), node.getArguments());
        assert (javacTree.getClassBody() != null) == node.getBegin().isPresent();
        if (javacTree.getClassBody() != null) {
            visitClassMembers(
                    javacTree.getClassBody().getMembers(), node.getAnonymousClassBody().get());
        }

        return null;
    }

    @Override
    public Void visitOpens(OpensTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ModuleOpensDirective)) {
            throwUnexpectedNodeType(javaParserNode, ModuleOpensDirective.class);
        }

        ModuleOpensDirective node = (ModuleOpensDirective) javaParserNode;
        processOpens(javacTree, node);
        javacTree.getPackageName().accept(this, node.getName());
        visitLists(javacTree.getModuleNames(), node.getModuleNames());
        return null;
    }

    @Override
    public Void visitOther(Tree javacTree, Node javaParserNode) {
        processOther(javacTree, javaParserNode);
        return null;
    }

    @Override
    public Void visitPackage(PackageTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof PackageDeclaration)) {
            throwUnexpectedNodeType(javaParserNode, PackageDeclaration.class);
        }

        PackageDeclaration node = (PackageDeclaration) javaParserNode;
        processPackage(javacTree, node);
        visitLists(javacTree.getAnnotations(), node.getAnnotations());
        javacTree.getPackageName().accept(this, node.getName());
        return null;
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree javacTree, Node javaParserNode) {
        // TODO: Implement this, what's the relationship to JavaParser's NodeWithTypeArguments?
        return null;
    }

    @Override
    public Void visitParenthesized(ParenthesizedTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof EnclosedExpr)) {
            throwUnexpectedNodeType(javaParserNode, EnclosedExpr.class);
        }

        EnclosedExpr node = (EnclosedExpr) javaParserNode;
        processParenthesized(javacTree, node);
        javacTree.getExpression().accept(this, node.getInner());
        return null;
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree javacTree, Node javaParserNode) {
        if (javaParserNode instanceof PrimitiveType) {
            processPrimitiveType(javacTree, (PrimitiveType) javaParserNode);
        } else if (javaParserNode instanceof VoidType) {
            processPrimitiveType(javacTree, (VoidType) javaParserNode);
        } else {
            throwUnexpectedNodeType(javaParserNode);
        }

        return null;
    }

    @Override
    public Void visitProvides(ProvidesTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ModuleProvidesDirective)) {
            throwUnexpectedNodeType(javaParserNode, ModuleProvidesDirective.class);
        }

        ModuleProvidesDirective node = (ModuleProvidesDirective) javaParserNode;
        processProvides(javacTree, node);
        javacTree.getServiceName().accept(this, node.getName());
        visitLists(javacTree.getImplementationNames(), node.getWith());
        return null;
    }

    @Override
    public Void visitRequires(RequiresTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ModuleRequiresDirective)) {
            throwUnexpectedNodeType(javaParserNode, ModuleRequiresDirective.class);
        }

        ModuleRequiresDirective node = (ModuleRequiresDirective) javaParserNode;
        processRequires(javacTree, node);
        javacTree.getModuleName().accept(this, node.getName());
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ReturnStmt)) {
            throwUnexpectedNodeType(javaParserNode, ReturnStmt.class);
        }

        ReturnStmt node = (ReturnStmt) javaParserNode;
        processReturn(javacTree, node);
        assert (javacTree.getExpression() != null) == node.getExpression().isPresent();
        if (javacTree.getExpression() != null) {
            javacTree.getExpression().accept(this, node.getExpression().get());
        }

        return null;
    }

    // TODO: Take SwitchNode here instead?
    @Override
    public Void visitSwitch(SwitchTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof SwitchStmt)) {
            throwUnexpectedNodeType(javaParserNode, SwitchStmt.class);
        }

        SwitchStmt node = (SwitchStmt) javaParserNode;
        processSwitch(javacTree, node);
        javacTree.getExpression().accept(this, node.getSelector());
        visitLists(javacTree.getCases(), node.getEntries());
        return null;
    }

    @Override
    public Void visitSynchronized(SynchronizedTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof SynchronizedStmt)) {
            throwUnexpectedNodeType(javaParserNode, SynchronizedStmt.class);
        }

        SynchronizedStmt node = (SynchronizedStmt) javaParserNode;
        processSynchronized(javacTree, node);
        javacTree.getExpression().accept(this, node.getExpression());
        javacTree.getBlock().accept(this, node.getBody());
        return null;
    }

    @Override
    public Void visitThrow(ThrowTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ThrowStmt)) {
            throwUnexpectedNodeType(javaParserNode, ThrowStmt.class);
        }

        ThrowStmt node = (ThrowStmt) javaParserNode;
        processThrow(javacTree, node);
        javacTree.getExpression().accept(this, node.getExpression());
        return null;
    }

    @Override
    public Void visitTry(TryTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof TryStmt)) {
            throwUnexpectedNodeType(javaParserNode, TryStmt.class);
        }

        TryStmt node = (TryStmt) javaParserNode;
        processTry(javacTree, node);
        visitLists(javacTree.getResources(), node.getResources());
        javacTree.getBlock().accept(this, node.getTryBlock());
        visitLists(javacTree.getCatches(), node.getCatchClauses());
        assert (javacTree.getFinallyBlock() != null) == node.getFinallyBlock().isPresent();
        if (javacTree.getFinallyBlock() != null) {
            javacTree.getFinallyBlock().accept(this, node.getFinallyBlock().get());
        }

        return null;
    }

    @Override
    public Void visitTypeCast(TypeCastTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof CastExpr)) {
            throwUnexpectedNodeType(javaParserNode, CastExpr.class);
        }

        CastExpr node = (CastExpr) javaParserNode;
        processTypeCast(javacTree, node);
        javacTree.getType().accept(this, node.getType());
        javacTree.getExpression().accept(this, node.getExpression());
        return null;
    }

    @Override
    public Void visitTypeParameter(TypeParameterTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof TypeParameter)) {
            throwUnexpectedNodeType(javaParserNode, TypeParameter.class);
        }

        TypeParameter node = (TypeParameter) javaParserNode;
        processTypeParameter(javacTree, node);
        visitLists(javacTree.getAnnotations(), node.getAnnotations());
        visitLists(javacTree.getBounds(), node.getTypeBound());
        return null;
    }

    @Override
    public Void visitUnary(UnaryTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof UnaryExpr)) {
            throwUnexpectedNodeType(javaParserNode, UnaryExpr.class);
        }

        UnaryExpr node = (UnaryExpr) javaParserNode;
        processUnary(javacTree, node);
        javacTree.getExpression().accept(this, node.getExpression());
        return null;
    }

    @Override
    public Void visitUnionType(UnionTypeTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof UnionType)) {
            throwUnexpectedNodeType(javaParserNode, UnionType.class);
        }

        UnionType node = (UnionType) javaParserNode;
        processUnionType(javacTree, node);
        visitLists(javacTree.getTypeAlternatives(), node.getElements());
        return null;
    }

    @Override
    public Void visitUses(UsesTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ModuleUsesDirective)) {
            throwUnexpectedNodeType(javaParserNode, ModuleUsesDirective.class);
        }

        ModuleUsesDirective node = (ModuleUsesDirective) javaParserNode;
        processUses(javacTree, node);
        javacTree.getServiceName().accept(this, node.getName());
        return null;
    }

    @Override
    public Void visitVariable(VariableTree javacTree, Node javaParserNode) {
        // TODO: Implement this. In int i = 0, j = 0, javac desugars this as two
        // VariableDeclarations
        // but JavaParser keeps it as a single VariableDeclarationExpr with two nested
        // VariableDeclarator. Also, in JavaParser the VariableDeclartionExpr is nested in an
        // ExpressionStmt which doesn't exist in javac.
        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof WhileStmt)) {
            throwUnexpectedNodeType(javaParserNode, WhileStmt.class);
        }

        WhileStmt node = (WhileStmt) javaParserNode;
        processWhileLoop(javacTree, node);
        javacTree.getCondition().accept(this, node.getCondition());
        javacTree.getStatement().accept(this, node.getBody());
        return null;
    }

    @Override
    public Void visitWildcard(WildcardTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof WildcardType)) {
            throwUnexpectedNodeType(javaParserNode, WildcardType.class);
        }

        WildcardType node = (WildcardType) javaParserNode;
        processWildcard(javacTree, node);
        // In javac, whether the bound is an extends or super clause depends on the kind of the
        // tree.
        assert (javacTree.getKind() == Kind.EXTENDS_WILDCARD) == node.getExtendedType().isPresent();
        assert (javacTree.getKind() == Kind.SUPER_WILDCARD) == node.getSuperType().isPresent();
        switch (javacTree.getKind()) {
            case UNBOUNDED_WILDCARD:
                break;
            case EXTENDS_WILDCARD:
                javacTree.getBound().accept(this, node.getExtendedType().get());
                break;
            case SUPER_WILDCARD:
                javacTree.getBound().accept(this, node.getSuperType().get());
                break;
            default:
                throw new BugInCF("Unexpected wildcard kind: %s", javacTree);
        }

        return null;
    }

    public void processAnnotation(AnnotationTree javacTree, NormalAnnotationExpr javaParserNode) {}

    public void processAnnotation(AnnotationTree javacTree, MarkerAnnotationExpr javaParserNode) {}

    public void processAnnotation(
            AnnotationTree javacTree, SingleMemberAnnotationExpr javaParserNode) {}

    /** {@code javaParserNode} is guaranteed to implement {@code NodeWithAnnotations<?>}. */
    public void processAnnotatedType(AnnotatedTypeTree javacTree, Node javaParserNode) {}

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

    public void processIf(IfTree javacTree, IfStmt javaParserNode) {}

    // TODO: Document that the javaparser name may not include "*".
    public void processImport(ImportTree javacTree, ImportDeclaration javaParserNode) {}

    public void processInstanceOf(InstanceOfTree javacTree, InstanceOfExpr javaParserNode) {}

    public void processIntersectionType(
            IntersectionTypeTree javacTree, IntersectionType javaParserNode) {}

    public void processLabeledStatement(
            LabeledStatementTree javacTree, LabeledStmt javaParserNode) {}

    public void processLambdaExpression(
            LambdaExpressionTree javacTree, LambdaExpr javaParserNode) {}

    public void processLiteral(LiteralTree javacTree, LiteralExpr javaParserNode) {}

    public void processMemberReference(
            MemberReferenceTree javacTree, MethodReferenceExpr javaParserNode) {}

    public void processMethod(MethodTree javacTree, MethodDeclaration javaParserNode) {}

    public void processMethod(MethodTree javacTree, ConstructorDeclaration javaParserNode) {}

    public void processMethod(MethodTree javacTree, AnnotationMemberDeclaration javaParserNode) {}

    public void processMethodInvocation(
            MethodInvocationTree javacTree, MethodCallExpr javaParserNode) {}

    public void processModule(ModuleTree javacTree, ModuleDeclaration javaParserNode) {}

    public void processNewClass(NewClassTree javacTree, ObjectCreationExpr javaParserNode) {}

    public void processOpens(OpensTree javacTree, ModuleOpensDirective javaParserNode) {}

    public void processOther(Tree javacTree, Node javaParserNode) {}

    public void processPackage(PackageTree javacTree, PackageDeclaration javaParserNode) {}

    public void processParenthesized(ParenthesizedTree javacTree, EnclosedExpr javaParserNode) {}

    public void processPrimitiveType(PrimitiveTypeTree javacTree, PrimitiveType javaParserNode) {}

    public void processPrimitiveType(PrimitiveTypeTree javacTree, VoidType javaParserNode) {}

    public void processProvides(ProvidesTree javacTree, ModuleProvidesDirective javaParserNode) {}

    public void processRequires(RequiresTree javacTree, ModuleRequiresDirective javaParserNode) {}

    public void processReturn(ReturnTree javacTree, ReturnStmt javaParserNode) {}

    public void processSwitch(SwitchTree javacTree, SwitchStmt javaParserNode) {}

    public void processSynchronized(SynchronizedTree javacTree, SynchronizedStmt javaParserNode) {}

    public void processThrow(ThrowTree javacTree, ThrowStmt javaParserNode) {}

    public void processTry(TryTree javacTree, TryStmt javaParserNode) {}

    public void processTypeCast(TypeCastTree javacTree, CastExpr javaParserNode) {}

    public void processTypeParameter(TypeParameterTree javacTree, TypeParameter javaParserNode) {}

    public void processUnary(UnaryTree javacTree, UnaryExpr javaParserNode) {}

    public void processUnionType(UnionTypeTree javacTree, UnionType javaParserNode) {}

    public void processUses(UsesTree javacTree, ModuleUsesDirective javaParserNode) {}

    public void processWhileLoop(WhileLoopTree javacTree, WhileStmt javaParserNode) {}

    public void processWildcard(WildcardTree javacTree, WildcardType javaParserNode) {}

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
