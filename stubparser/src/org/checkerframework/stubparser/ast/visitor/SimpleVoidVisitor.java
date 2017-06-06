package org.checkerframework.stubparser.ast.visitor;

import org.checkerframework.stubparser.ast.*;
import org.checkerframework.stubparser.ast.body.*;
import org.checkerframework.stubparser.ast.expr.*;
import org.checkerframework.stubparser.ast.stmt.*;
import org.checkerframework.stubparser.ast.type.*;

public class SimpleVoidVisitor<A> implements VoidVisitor<A> {
    public void defaultAction(Node n, A arg) { }

    public void visit(Node n, A arg) {
        throw new IllegalStateException(n.getClass().getName());
    }

    @Override
    public void visit(IndexUnit n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(CompilationUnit n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(PackageDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ImportDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(TypeParameter n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(LineComment n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(BlockComment n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(EnumDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(EmptyTypeDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(EnumConstantDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(AnnotationDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(AnnotationMemberDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(FieldDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(VariableDeclarator n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(VariableDeclaratorId n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ConstructorDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(MethodDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(Parameter n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(EmptyMemberDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(InitializerDeclaration n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(JavadocComment n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ClassOrInterfaceType n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(PrimitiveType n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ReferenceType n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(VoidType n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(WildcardType n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ArrayAccessExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ArrayCreationExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ArrayInitializerExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(AssignExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(BinaryExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(CastExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ClassExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ConditionalExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(EnclosedExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(FieldAccessExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(InstanceOfExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(StringLiteralExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(IntegerLiteralExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(LongLiteralExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(IntegerLiteralMinValueExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(LongLiteralMinValueExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(CharLiteralExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(DoubleLiteralExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(BooleanLiteralExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(NullLiteralExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(MethodCallExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(NameExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ObjectCreationExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(QualifiedNameExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ThisExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(SuperExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(UnaryExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(VariableDeclarationExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(MarkerAnnotationExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(SingleMemberAnnotationExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(NormalAnnotationExpr n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(MemberValuePair n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(TypeDeclarationStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(AssertStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(BlockStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(LabeledStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(EmptyStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ExpressionStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(SwitchStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(SwitchEntryStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(BreakStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ReturnStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(IfStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(WhileStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ContinueStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(DoStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ForeachStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ForStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(ThrowStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(SynchronizedStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(TryStmt n, A arg) { defaultAction(n, arg); }

    @Override
    public void visit(CatchClause n, A arg) { defaultAction(n, arg); }

}
