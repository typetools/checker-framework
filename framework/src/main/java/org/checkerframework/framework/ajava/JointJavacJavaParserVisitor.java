package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
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
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.checkerframework.javacutil.BugInCF;

public abstract class JointJavacJavaParserVisitor implements TreeVisitor<Void, Node> {
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
            throwUnexpectedNodeType(javacTree, javaParserNode);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, NodeWithAnnotations.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, ArrayAccessExpr.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, ArrayType.class);
        }

        ArrayType node = (ArrayType) javaParserNode;
        processArrayType(javacTree, node);
        javacTree.getType().accept(this, node.getComponentType());
        return null;
    }

    @Override
    public Void visitAssert(AssertTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof AssertStmt)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, AssertStmt.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, AssignExpr.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, BinaryExpr.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, BlockStmt.class);
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
                hasNextJavac = javacIter.hasNext();
                javacStatement = hasNextJavac ? javacIter.next() : null;
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

    public static boolean isDefaultSuperConstructorCall(StatementTree statement) {
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
            throwUnexpectedNodeType(javacTree, javaParserNode, BreakStmt.class);
        }

        processBreak(javacTree, (BreakStmt) javaParserNode);
        return null;
    }

    @Override
    public Void visitCase(CaseTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof SwitchEntry)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, SwitchEntry.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, CatchClause.class);
        }

        CatchClause node = (CatchClause) javaParserNode;
        processCatch(javacTree, node);
        javacTree.getParameter().accept(this, node.getParameter());
        javacTree.getBlock().accept(this, node.getBody());
        return null;
    }

    @Override
    public Void visitClass(ClassTree javacTree, Node javaParserNode) {
        if (javaParserNode instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration node = (ClassOrInterfaceDeclaration) javaParserNode;
            processClass(javacTree, node);
            visitLists(javacTree.getTypeParameters(), node.getTypeParameters());
            if (javacTree.getKind() == Kind.CLASS) {
                if (javacTree.getExtendsClause() == null) {
                    assert node.getExtendedTypes().isEmpty();
                } else {
                    assert node.getExtendedTypes().size() == 1;
                    javacTree.getExtendsClause().accept(this, node.getExtendedTypes().get(0));
                }

                visitLists(javacTree.getImplementsClause(), node.getImplementedTypes());
            } else if (javacTree.getKind() == Kind.INTERFACE) {
                visitLists(javacTree.getImplementsClause(), node.getExtendedTypes());
            }

            visitClassMembers(javacTree.getMembers(), node.getMembers());
        } else if (javaParserNode instanceof AnnotationDeclaration) {
            AnnotationDeclaration node = (AnnotationDeclaration) javaParserNode;
            processClass(javacTree, node);
            visitClassMembers(javacTree.getMembers(), node.getMembers());
        } else if (javaParserNode instanceof LocalClassDeclarationStmt) {
            javacTree.accept(
                    this, ((LocalClassDeclarationStmt) javaParserNode).getClassDeclaration());
        } else if (javaParserNode instanceof EnumDeclaration) {
            EnumDeclaration node = (EnumDeclaration) javaParserNode;
            processClass(javacTree, node);
            visitLists(javacTree.getImplementsClause(), node.getImplementedTypes());
            // In an enum declaration, the enum constants are expanded as constant variable members
            // whereas in JavaParser they're stored as one object, need to match them.
            assert javacTree.getKind() == Kind.ENUM;
            List<Tree> javacMembers = new ArrayList<>(javacTree.getMembers());
            // If there are any constants in this enum, then they will show up as the first members
            // of the javac tree, except for possibly a synthetic constructor. Thus, in this case
            // any member before the first variable instance should be discarded.
            if (!node.getEntries().isEmpty()) {
                while (!javacMembers.isEmpty() && javacMembers.get(0).getKind() != Kind.VARIABLE) {
                    javacMembers.remove(0);
                }
            }

            for (EnumConstantDeclaration entry : node.getEntries()) {
                assert !javacMembers.isEmpty();
                javacMembers.get(0).accept(this, entry);
                javacMembers.remove(0);
            }

            visitClassMembers(javacMembers, node.getMembers());
        } else {
            throwUnexpectedNodeType(javacTree, javaParserNode);
        }

        return null;
    }

    private void visitClassMembers(
            List<? extends Tree> javacMembers, List<BodyDeclaration<?>> javaParserMembers) {
        // The javac members might have an artificially generated default constructor, don't process
        // it if present.
        Iterator<? extends Tree> javacIter = javacMembers.iterator();
        boolean hasNextJavac = javacIter.hasNext();
        Tree javacMember = hasNextJavac ? javacIter.next() : null;
        Iterator<BodyDeclaration<?>> javaParserIter = javaParserMembers.iterator();
        boolean hasNextJavaParser = javaParserIter.hasNext();
        BodyDeclaration<?> javaParserMember = hasNextJavaParser ? javaParserIter.next() : null;

        while (hasNextJavac || hasNextJavaParser) {
            // Skip javac's synthetic no-argument constructors.
            if (hasNextJavac
                    && isNoArgumentConstructor(javacMember)
                    && (!hasNextJavaParser || !isNoArgumentConstructor(javaParserMember))) {
                hasNextJavac = javacIter.hasNext();
                javacMember = hasNextJavac ? javacIter.next() : null;
                continue;
            }

            // In javac, a line like int i = 0, j = 0 is expanded as two sibling VariableTree
            // instances. In javaParser this is one FieldDeclaration with two nested
            // VariableDeclarators. Match the declarators with the VariableTrees.
            if (hasNextJavaParser && javaParserMember.isFieldDeclaration()) {
                for (VariableDeclarator decl :
                        javaParserMember.asFieldDeclaration().getVariables()) {
                    assert hasNextJavac;
                    assert javacMember.getKind() == Kind.VARIABLE;
                    javacMember.accept(this, decl);
                    hasNextJavac = javacIter.hasNext();
                    javacMember = hasNextJavac ? javacIter.next() : null;
                }

                hasNextJavaParser = javaParserIter.hasNext();
                javaParserMember = hasNextJavaParser ? javaParserIter.next() : null;
                continue;
            }

            assert hasNextJavac;
            assert hasNextJavaParser;
            javacMember.accept(this, javaParserMember);

            hasNextJavac = javacIter.hasNext();
            javacMember = hasNextJavac ? javacIter.next() : null;

            hasNextJavaParser = javaParserIter.hasNext();
            javaParserMember = hasNextJavaParser ? javaParserIter.next() : null;
        }

        assert !hasNextJavac;
        assert !hasNextJavaParser;
    }

    private void visitAnonymouClassBody(
            ClassTree javacBody, List<BodyDeclaration<?>> javaParserMembers) {
        // Like normal class bodies, javac will insert synthetic constructors. In an anonymous
        // class, the generated constructor will have the same parameter types as the type it
        // extends and will pass them to the superconstructor. This means that just checking for a
        // no-argument constructor doesn't work in this case. However, it's impossible to declare
        // constructors in an anonymous class, so any constructor is synthetic. We skip those
        // members before processing the rest like normal.
        List<Tree> members = new ArrayList<>(javacBody.getMembers());
        while (!members.isEmpty()) {
            Tree member = members.get(0);
            if (member.getKind() == Kind.METHOD) {
                MethodTree methodTree = (MethodTree) member;
                if (methodTree.getName().contentEquals("<init>")) {
                    members.remove(0);
                    continue;
                }
            }

            break;
        }

        visitClassMembers(members, javaParserMembers);
    }

    public static boolean isNoArgumentConstructor(Tree member) {
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
            throwUnexpectedNodeType(javacTree, javaParserNode, CompilationUnit.class);
        }

        CompilationUnit node = (CompilationUnit) javaParserNode;
        processCompilationUnit(javacTree, node);

        // TODO: A CompilationUnitTree could also be a package-info.java file. Currently skipping
        // descending into these specific constructs such as getPackageAnnotations, because they
        // probably won't be useful and TreeScanner also skips them. Should we process them?
        assert (javacTree.getPackage() != null) == node.getPackageDeclaration().isPresent();
        if (javacTree.getPackage() != null) {
            javacTree.getPackage().accept(this, node.getPackageDeclaration().get());
        }

        visitLists(javacTree.getImports(), node.getImports());
        visitLists(javacTree.getTypeDecls(), node.getTypes());
        return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof AssignExpr)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, AssignExpr.class);
        }

        AssignExpr node = (AssignExpr) javaParserNode;
        processCompoundAssignment(javacTree, node);
        javacTree.getVariable().accept(this, node.getTarget());
        javacTree.getExpression().accept(this, node.getValue());
        return null;
    }

    @Override
    public Void visitConditionalExpression(
            ConditionalExpressionTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ConditionalExpr)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, ConditionalExpr.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, ContinueStmt.class);
        }

        processContinue(javacTree, (ContinueStmt) javaParserNode);
        return null;
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof DoStmt)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, DoStmt.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, EmptyStmt.class);
        }

        processEmptyStatement(javacTree, (EmptyStmt) javaParserNode);
        return null;
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ForEachStmt)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, ForEachStmt.class);
        }

        ForEachStmt node = (ForEachStmt) javaParserNode;
        processEnhancedForLoop(javacTree, node);
        // TODO: Fix the fact that the variable might be a JavaParser VariableDeclarationExpr.
        javacTree.getVariable().accept(this, node.getVariableDeclarator());
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
            throwUnexpectedNodeType(javacTree, javaParserNode, ModuleExportsDirective.class);
        }

        ModuleExportsDirective node = (ModuleExportsDirective) javaParserNode;
        processExports(javacTree, node);
        visitLists(javacTree.getModuleNames(), node.getModuleNames());
        javacTree.getPackageName().accept(this, node.getName());
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree javacTree, Node javaParserNode) {
        if (javaParserNode instanceof ExpressionStmt) {
            ExpressionStmt node = (ExpressionStmt) javaParserNode;
            processExpressionStatemen(javacTree, node);
            javacTree.getExpression().accept(this, node.getExpression());
        } else if (javaParserNode instanceof ExplicitConstructorInvocationStmt) {
            // In this case the expression will be a MethodTree, which would be better to match with
            // the statement than the expression statement itself.
            javacTree.getExpression().accept(this, javaParserNode);
        } else {
            throwUnexpectedNodeType(javacTree, javaParserNode);
        }

        return null;
    }

    @Override
    public Void visitForLoop(ForLoopTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ForStmt)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, ForStmt.class);
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
    public Void visitIdentifier(IdentifierTree javacTree, Node javaParserNode) {
        if (javaParserNode instanceof ClassOrInterfaceType) {
            processIdentifier(javacTree, (ClassOrInterfaceType) javaParserNode);
        } else if (javaParserNode instanceof Name) {
            processIdentifier(javacTree, (Name) javaParserNode);
        } else if (javaParserNode instanceof NameExpr) {
            processIdentifier(javacTree, (NameExpr) javaParserNode);
        } else if (javaParserNode instanceof SimpleName) {
            processIdentifier(javacTree, (SimpleName) javaParserNode);
        } else if (javaParserNode instanceof ThisExpr) {
            processIdentifier(javacTree, (ThisExpr) javaParserNode);
        } else if (javaParserNode instanceof SuperExpr) {
            processIdentifier(javacTree, (SuperExpr) javaParserNode);
        } else if (javaParserNode instanceof TypeExpr) {
            // This occurs in a member reference like MyClass::myMember. The MyClass is wrapped in a
            // TypeExpr.
            javacTree.accept(this, ((TypeExpr) javaParserNode).getType());
        } else {
            throwUnexpectedNodeType(javacTree, javaParserNode);
        }

        return null;
    }

    @Override
    public Void visitIf(IfTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof IfStmt)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, IfStmt.class);
        }

        IfStmt node = (IfStmt) javaParserNode;
        processIf(javacTree, node);
        assert javacTree.getCondition().getKind() == Kind.PARENTHESIZED;
        ParenthesizedTree condition = (ParenthesizedTree) javacTree.getCondition();
        condition.getExpression().accept(this, node.getCondition());
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
            throwUnexpectedNodeType(javacTree, javaParserNode, ImportDeclaration.class);
        }

        ImportDeclaration node = (ImportDeclaration) javaParserNode;
        processImport(javacTree, node);
        // In javac trees, a name like a.* is stored as a member select, but JavaParser just stores
        // a and records that the name ends in an asterisk.
        if (node.isAsterisk()) {
            assert javacTree.getQualifiedIdentifier().getKind() == Kind.MEMBER_SELECT;
            MemberSelectTree identifier = (MemberSelectTree) javacTree.getQualifiedIdentifier();
            identifier.getExpression().accept(this, node.getName());
        } else {
            javacTree.getQualifiedIdentifier().accept(this, node.getName());
        }
        return null;
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof InstanceOfExpr)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, InstanceOfExpr.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, IntersectionType.class);
        }

        IntersectionType node = (IntersectionType) javaParserNode;
        processIntersectionType(javacTree, node);
        visitLists(javacTree.getBounds(), node.getElements());
        return null;
    }

    @Override
    public Void visitLabeledStatement(LabeledStatementTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof LabeledStmt)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, LabeledStmt.class);
        }

        LabeledStmt node = (LabeledStmt) javaParserNode;
        processLabeledStatement(javacTree, node);
        javacTree.getStatement().accept(this, node.getStatement());
        return null;
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof LambdaExpr)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, LambdaExpr.class);
        }

        LambdaExpr node = (LambdaExpr) javaParserNode;
        processLambdaExpression(javacTree, node);
        visitLists(javacTree.getParameters(), node.getParameters());
        javacTree.getBody().accept(this, node.getBody());
        return null;
    }

    @Override
    public Void visitLiteral(LiteralTree javacTree, Node javaParserNode) {
        if (javaParserNode instanceof LiteralExpr) {
            processLiteral(javacTree, (LiteralExpr) javaParserNode);
        } else if (javaParserNode instanceof UnaryExpr) {
            processLiteral(javacTree, (UnaryExpr) javaParserNode);
        } else {
            throwUnexpectedNodeType(javacTree, javaParserNode);
        }

        return null;
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof MethodReferenceExpr)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, MethodReferenceExpr.class);
        }

        MethodReferenceExpr node = (MethodReferenceExpr) javaParserNode;
        processMemberReference(javacTree, node);
        javacTree.getQualifierExpression().accept(this, node.getScope());
        assert (javacTree.getTypeArguments() != null) == node.getTypeArguments().isPresent();
        if (javacTree.getTypeArguments() != null) {
            visitLists(javacTree.getTypeArguments(), node.getTypeArguments().get());
        }

        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree javacTree, Node javaParserNode) {
        if (javaParserNode instanceof FieldAccessExpr) {
            FieldAccessExpr node = (FieldAccessExpr) javaParserNode;
            processMemberSelect(javacTree, node);
            javacTree.getExpression().accept(this, node.getScope());
        } else if (javaParserNode instanceof Name) {
            Name node = (Name) javaParserNode;
            processMemberSelect(javacTree, node);
            assert node.getQualifier().isPresent();
            javacTree.getExpression().accept(this, node.getQualifier().get());
        } else if (javaParserNode instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType node = (ClassOrInterfaceType) javaParserNode;
            processMemberSelect(javacTree, node);
            assert node.getScope().isPresent();
            javacTree.getExpression().accept(this, node.getScope().get());
        } else if (javaParserNode instanceof ClassExpr) {
            ClassExpr node = (ClassExpr) javaParserNode;
            processMemberSelect(javacTree, node);
            javacTree.getExpression().accept(this, node.getType());
        } else {
            throwUnexpectedNodeType(javacTree, javaParserNode);
        }

        return null;
    }

    @Override
    public Void visitMethod(MethodTree javacTree, Node javaParserNode) {
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

        throwUnexpectedNodeType(javacTree, javaParserNode);
        return null;
    }

    private Void visitMethodForMethodDeclaration(
            MethodTree javacTree, MethodDeclaration javaParserNode) {
        processMethod(javacTree, javaParserNode);
        // TODO: Handle modifiers. In javac this is a ModifiersTree but in JavaParser it's a list of
        // modifiers. This is a problem because a ModifiersTree has separate accessors to
        // annotations and other modifiers, so the order doesn't match. It might be that for
        // JavaParser, the annotations and other modifiers are also accessed separately.
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
        if (javaParserNode instanceof MethodCallExpr) {
            MethodCallExpr node = (MethodCallExpr) javaParserNode;
            processMethodInvocation(javacTree, node);
            // In javac, the type arguments will be empty even if no type arguments are specified,
            // but
            // in JavaParser the type arguments will have the none Optional value.
            // The left side of this assert is checking if the list is empty and the right side is a
            // check on the prescence of an optional.
            assert javacTree.getTypeArguments().isEmpty() == node.getTypeArguments().isEmpty();
            if (!javacTree.getTypeArguments().isEmpty()) {
                visitLists(javacTree.getTypeArguments(), node.getTypeArguments().get());
            }

            // In JavaParser, the method name itself and receiver are stored as fields of the
            // invocation
            // itself, but in javac they might be combined into one MemberSelectTree. That member
            // select
            // may also be a single IdentifierTree if no receiver was written. This requires one
            // layer
            // of unnesting.
            ExpressionTree methodSelect = javacTree.getMethodSelect();
            if (methodSelect.getKind() == Kind.IDENTIFIER) {
                methodSelect.accept(this, node.getName());
            } else if (methodSelect.getKind() == Kind.MEMBER_SELECT) {
                MemberSelectTree selection = (MemberSelectTree) methodSelect;
                assert node.getScope().isPresent();
                selection.getExpression().accept(this, node.getScope().get());
            } else {
                throw new BugInCF("Unexpected method selection type: %s", methodSelect);
            }

            visitLists(javacTree.getArguments(), node.getArguments());
        } else if (javaParserNode instanceof ExplicitConstructorInvocationStmt) {
            ExplicitConstructorInvocationStmt node =
                    (ExplicitConstructorInvocationStmt) javaParserNode;
            processMethodInvocation(javacTree, node);
            // The left side of this assert is checking if the list is empty and the right side is a
            // check on the prescence of an optional.
            assert javacTree.getTypeArguments().isEmpty() == node.getTypeArguments().isEmpty();
            if (!javacTree.getTypeArguments().isEmpty()) {
                visitLists(javacTree.getTypeArguments(), node.getTypeArguments().get());
            }

            visitLists(javacTree.getArguments(), node.getArguments());
        }

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
            throwUnexpectedNodeType(javacTree, javaParserNode, ModuleDeclaration.class);
        }

        ModuleDeclaration node = (ModuleDeclaration) javaParserNode;
        processModule(javacTree, node);
        visitLists(javacTree.getAnnotations(), node.getAnnotations());
        javacTree.getName().accept(this, node.getName());
        return null;
    }

    @Override
    public Void visitNewArray(NewArrayTree javacTree, Node javaParserNode) {
        // TODO: Implement this, it's too much work to do now.
        //
        // Some notes:
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
            throwUnexpectedNodeType(javacTree, javaParserNode, ObjectCreationExpr.class);
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
        assert (javacTree.getClassBody() != null) == node.getAnonymousClassBody().isPresent();
        if (javacTree.getClassBody() != null) {
            visitAnonymouClassBody(javacTree.getClassBody(), node.getAnonymousClassBody().get());
        }

        return null;
    }

    @Override
    public Void visitOpens(OpensTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ModuleOpensDirective)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, ModuleOpensDirective.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, PackageDeclaration.class);
        }

        PackageDeclaration node = (PackageDeclaration) javaParserNode;
        processPackage(javacTree, node);
        visitLists(javacTree.getAnnotations(), node.getAnnotations());
        javacTree.getPackageName().accept(this, node.getName());
        return null;
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ClassOrInterfaceType)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, ClassOrInterfaceType.class);
        }

        ClassOrInterfaceType node = (ClassOrInterfaceType) javaParserNode;
        processParameterizedType(javacTree, node);
        javacTree.getType().accept(this, node);
        // TODO: In a parameterized type, will the first branch ever run?
        if (javacTree.getTypeArguments().isEmpty()) {
            assert node.getTypeArguments().isEmpty() || node.getTypeArguments().get().isEmpty();
        } else {
            assert node.getTypeArguments().isPresent();
            visitLists(javacTree.getTypeArguments(), node.getTypeArguments().get());
        }
        return null;
    }

    @Override
    public Void visitParenthesized(ParenthesizedTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof EnclosedExpr)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, EnclosedExpr.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode);
        }

        return null;
    }

    @Override
    public Void visitProvides(ProvidesTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ModuleProvidesDirective)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, ModuleProvidesDirective.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, ModuleRequiresDirective.class);
        }

        ModuleRequiresDirective node = (ModuleRequiresDirective) javaParserNode;
        processRequires(javacTree, node);
        javacTree.getModuleName().accept(this, node.getName());
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ReturnStmt)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, ReturnStmt.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, SwitchStmt.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, SynchronizedStmt.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, ThrowStmt.class);
        }

        ThrowStmt node = (ThrowStmt) javaParserNode;
        processThrow(javacTree, node);
        javacTree.getExpression().accept(this, node.getExpression());
        return null;
    }

    @Override
    public Void visitTry(TryTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof TryStmt)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, TryStmt.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, CastExpr.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, TypeParameter.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, UnaryExpr.class);
        }

        UnaryExpr node = (UnaryExpr) javaParserNode;
        processUnary(javacTree, node);
        javacTree.getExpression().accept(this, node.getExpression());
        return null;
    }

    @Override
    public Void visitUnionType(UnionTypeTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof UnionType)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, UnionType.class);
        }

        UnionType node = (UnionType) javaParserNode;
        processUnionType(javacTree, node);
        visitLists(javacTree.getTypeAlternatives(), node.getElements());
        return null;
    }

    @Override
    public Void visitUses(UsesTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof ModuleUsesDirective)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, ModuleUsesDirective.class);
        }

        ModuleUsesDirective node = (ModuleUsesDirective) javaParserNode;
        processUses(javacTree, node);
        javacTree.getServiceName().accept(this, node.getName());
        return null;
    }

    @Override
    public Void visitVariable(VariableTree javacTree, Node javaParserNode) {
        if (javaParserNode instanceof VariableDeclarator) {
            VariableDeclarator node = (VariableDeclarator) javaParserNode;
            processVariable(javacTree, node);
            javacTree.getType().accept(this, node.getType());
            // The name expression can be null, even when a name exists.
            if (javacTree.getNameExpression() != null) {
                javacTree.getNameExpression().accept(this, node.getName());
            }

            assert (javacTree.getInitializer() != null) == node.getInitializer().isPresent();
            if (javacTree.getInitializer() != null) {
                javacTree.getInitializer().accept(this, node.getInitializer().get());
            }
        } else if (javaParserNode instanceof Parameter) {
            Parameter node = (Parameter) javaParserNode;
            processVariable(javacTree, node);
            if (node.isVarArgs()) {
                assert javacTree.getType().getKind() == Kind.ARRAY_TYPE;
                ArrayTypeTree arrayType = (ArrayTypeTree) javacTree.getType();
                arrayType.getType().accept(this, node.getType());
            } else {
                javacTree.getType().accept(this, node.getType());
            }

            // The name expression can be null, even when a name exists.
            if (javacTree.getNameExpression() != null) {
                javacTree.getNameExpression().accept(this, node.getName());
            }

            assert javacTree.getInitializer() == null;
        } else if (javaParserNode instanceof ReceiverParameter) {
            ReceiverParameter node = (ReceiverParameter) javaParserNode;
            processVariable(javacTree, node);
            javacTree.getType().accept(this, node.getType());
            // The name expression can be null, even when a name exists.
            if (javacTree.getNameExpression() != null) {
                javacTree.getNameExpression().accept(this, node.getName());
            }

            assert javacTree.getInitializer() == null;
        } else if (javaParserNode instanceof EnumConstantDeclaration) {
            // An enum constant is expanded as a variable declaration initialized to a constuctor
            // call.
            EnumConstantDeclaration node = (EnumConstantDeclaration) javaParserNode;
            processVariable(javacTree, node);
            if (javacTree.getNameExpression() != null) {
                javacTree.getNameExpression().accept(this, node.getName());
            }

            assert javacTree.getInitializer().getKind() == Kind.NEW_CLASS;
            NewClassTree constructor = (NewClassTree) javacTree.getInitializer();
            visitLists(constructor.getArguments(), node.getArguments());
            if (constructor.getClassBody() != null) {
                visitAnonymouClassBody(constructor.getClassBody(), node.getClassBody());
            } else {
                assert node.getClassBody().isEmpty();
            }
        } else {
            throwUnexpectedNodeType(javacTree, javaParserNode);
        }

        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree javacTree, Node javaParserNode) {
        if (!(javaParserNode instanceof WhileStmt)) {
            throwUnexpectedNodeType(javacTree, javaParserNode, WhileStmt.class);
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
            throwUnexpectedNodeType(javacTree, javaParserNode, WildcardType.class);
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

    public abstract void processAnnotation(
            AnnotationTree javacTree, NormalAnnotationExpr javaParserNode);

    public abstract void processAnnotation(
            AnnotationTree javacTree, MarkerAnnotationExpr javaParserNode);

    public abstract void processAnnotation(
            AnnotationTree javacTree, SingleMemberAnnotationExpr javaParserNode);

    /** {@code javaParserNode} is guaranteed to implement {@code NodeWithAnnotations<?>}. */
    public abstract void processAnnotatedType(AnnotatedTypeTree javacTree, Node javaParserNode);

    public abstract void processArrayAccess(
            ArrayAccessTree javacTree, ArrayAccessExpr javaParserNode);

    public abstract void processArrayType(ArrayTypeTree javacTree, ArrayType javaParserNode);

    public abstract void processAssert(AssertTree javacTree, AssertStmt javaParserNode);

    public abstract void processAssignment(AssignmentTree javacTree, AssignExpr javaParserNode);

    public abstract void processBinary(BinaryTree javacTree, BinaryExpr javaParserNode);

    public abstract void processBlock(BlockTree javacTree, BlockStmt javaParserNode);

    public abstract void processBreak(BreakTree javacTree, BreakStmt javaParserNode);

    public abstract void processCase(CaseTree javacTree, SwitchEntry javaParserNode);

    public abstract void processCatch(CatchTree javacTree, CatchClause javaParserNode);

    public abstract void processClass(ClassTree javacTree, AnnotationDeclaration javaParserNode);

    public abstract void processClass(
            ClassTree javacTree, ClassOrInterfaceDeclaration javaParserNode);

    public abstract void processClass(ClassTree javacTree, EnumDeclaration javaParserNode);

    public abstract void processCompilationUnit(
            CompilationUnitTree javacTree, CompilationUnit javaParserNode);

    public abstract void processConditionalExpression(
            ConditionalExpressionTree javacTree, ConditionalExpr javaParserNode);

    public abstract void processContinue(ContinueTree javacTree, ContinueStmt javaParserNode);

    public abstract void processDoWhileLoop(DoWhileLoopTree javacTree, DoStmt javaParserNode);

    public abstract void processEmptyStatement(
            EmptyStatementTree javacTree, EmptyStmt javaParserNode);

    public abstract void processEnhancedForLoop(
            EnhancedForLoopTree javacTree, ForEachStmt javaParserNode);

    public abstract void processExports(
            ExportsTree javacTree, ModuleExportsDirective javaParserNode);

    public abstract void processExpressionStatemen(
            ExpressionStatementTree javacTree, ExpressionStmt javaParserNode);

    public abstract void processForLoop(ForLoopTree javacTree, ForStmt javaParserNode);

    public abstract void processIdentifier(
            IdentifierTree javacTree, ClassOrInterfaceType javaParserNode);

    public abstract void processIdentifier(IdentifierTree javacTree, Name javaParserNode);

    public abstract void processIdentifier(IdentifierTree javacTree, NameExpr javaParserNode);

    public abstract void processIdentifier(IdentifierTree javacTree, SimpleName javaParserNode);

    public abstract void processIdentifier(IdentifierTree javacTree, SuperExpr javaParserNode);

    public abstract void processIdentifier(IdentifierTree javacTree, ThisExpr javaParserNode);

    public abstract void processIf(IfTree javacTree, IfStmt javaParserNode);

    // TODO: Document that the javaparser name may not include "*".
    public abstract void processImport(ImportTree javacTree, ImportDeclaration javaParserNode);

    public abstract void processInstanceOf(InstanceOfTree javacTree, InstanceOfExpr javaParserNode);

    public abstract void processIntersectionType(
            IntersectionTypeTree javacTree, IntersectionType javaParserNode);

    public abstract void processLabeledStatement(
            LabeledStatementTree javacTree, LabeledStmt javaParserNode);

    public abstract void processLambdaExpression(
            LambdaExpressionTree javacTree, LambdaExpr javaParserNode);

    public abstract void processLiteral(LiteralTree javacTree, UnaryExpr javaParserNode);

    public abstract void processLiteral(LiteralTree javacTree, LiteralExpr javaParserNode);

    public abstract void processMemberReference(
            MemberReferenceTree javacTree, MethodReferenceExpr javaParserNode);

    public abstract void processMemberSelect(MemberSelectTree javacTree, ClassExpr javaParserNode);

    public abstract void processMemberSelect(
            MemberSelectTree javacTree, ClassOrInterfaceType javaParserNode);

    public abstract void processMemberSelect(
            MemberSelectTree javacTree, FieldAccessExpr javaParserNode);

    public abstract void processMemberSelect(MemberSelectTree javacTree, Name javaParserNode);

    public abstract void processMethod(MethodTree javacTree, MethodDeclaration javaParserNode);

    public abstract void processMethod(MethodTree javacTree, ConstructorDeclaration javaParserNode);

    public abstract void processMethod(
            MethodTree javacTree, AnnotationMemberDeclaration javaParserNode);

    public abstract void processMethodInvocation(
            MethodInvocationTree javacTree, ExplicitConstructorInvocationStmt javaParserNode);

    public abstract void processMethodInvocation(
            MethodInvocationTree javacTree, MethodCallExpr javaParserNode);

    public abstract void processModule(ModuleTree javacTree, ModuleDeclaration javaParserNode);

    public abstract void processNewClass(NewClassTree javacTree, ObjectCreationExpr javaParserNode);

    public abstract void processOpens(OpensTree javacTree, ModuleOpensDirective javaParserNode);

    public abstract void processOther(Tree javacTree, Node javaParserNode);

    public abstract void processPackage(PackageTree javacTree, PackageDeclaration javaParserNode);

    // TODO: Document that the JavaParser node may be processed twice.
    public abstract void processParameterizedType(
            ParameterizedTypeTree javacTree, ClassOrInterfaceType javaParserNode);

    public abstract void processParenthesized(
            ParenthesizedTree javacTree, EnclosedExpr javaParserNode);

    public abstract void processPrimitiveType(
            PrimitiveTypeTree javacTree, PrimitiveType javaParserNode);

    public abstract void processPrimitiveType(PrimitiveTypeTree javacTree, VoidType javaParserNode);

    public abstract void processProvides(
            ProvidesTree javacTree, ModuleProvidesDirective javaParserNode);

    public abstract void processRequires(
            RequiresTree javacTree, ModuleRequiresDirective javaParserNode);

    public abstract void processReturn(ReturnTree javacTree, ReturnStmt javaParserNode);

    public abstract void processSwitch(SwitchTree javacTree, SwitchStmt javaParserNode);

    public abstract void processSynchronized(
            SynchronizedTree javacTree, SynchronizedStmt javaParserNode);

    public abstract void processThrow(ThrowTree javacTree, ThrowStmt javaParserNode);

    public abstract void processTry(TryTree javacTree, TryStmt javaParserNode);

    public abstract void processTypeCast(TypeCastTree javacTree, CastExpr javaParserNode);

    public abstract void processTypeParameter(
            TypeParameterTree javacTree, TypeParameter javaParserNode);

    public abstract void processUnary(UnaryTree javacTree, UnaryExpr javaParserNode);

    public abstract void processUnionType(UnionTypeTree javacTree, UnionType javaParserNode);

    public abstract void processUses(UsesTree javacTree, ModuleUsesDirective javaParserNode);

    public abstract void processVariable(
            VariableTree javacTree, EnumConstantDeclaration javaParserNode);

    public abstract void processVariable(VariableTree javacTree, Parameter javaParserNode);

    public abstract void processVariable(VariableTree javacTree, ReceiverParameter javaParserNode);

    public abstract void processVariable(VariableTree javacTree, VariableDeclarator javaParserNode);

    public abstract void processWhileLoop(WhileLoopTree javacTree, WhileStmt javaParserNode);

    public abstract void processWildcard(WildcardTree javacTree, WildcardType javaParserNode);

    // TODO: Documentation on how to use getKind to determine the type of compound assignment like
    // in the javadoc for CompoundAssignmentTree. You could also get it from the javaparser node.
    public abstract void processCompoundAssignment(
            CompoundAssignmentTree javacTree, AssignExpr javaParserNode);

    private void visitLists(List<? extends Tree> javacTrees, List<? extends Node> javaParserNodes) {
        assert javacTrees.size() == javaParserNodes.size();
        Iterator<? extends Node> nodeIter = javaParserNodes.iterator();
        for (Tree tree : javacTrees) {
            tree.accept(this, nodeIter.next());
        }
    }

    private void throwUnexpectedNodeType(Tree javacTree, Node javaParserNode) {
        throw new BugInCF(
                "Javac and JavaParser trees desynced while processing tree %s, unexpected node type: %s",
                javacTree, javaParserNode.getClass());
    }

    private void throwUnexpectedNodeType(
            Tree javacTree, Node javaParserNode, Class<?> expectedType) {
        throw new BugInCF(
                "Javac and JavaParser trees desynced while processing tree %s, expected: %s, actual: %s",
                javacTree, expectedType, javaParserNode.getClass());
    }
}
