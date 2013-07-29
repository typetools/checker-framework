/*
 * Copyright (C) 2007 Júlio Vilmar Gesser.
 * 
 * This file is part of Java 1.5 parser and Abstract Syntax Tree.
 *
 * Java 1.5 parser and Abstract Syntax Tree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java 1.5 parser and Abstract Syntax Tree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java 1.5 parser and Abstract Syntax Tree.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Created on 05/10/2006
 */
package cfjapa.parser.ast.visitor;

import cfjapa.parser.ast.BlockComment;
import cfjapa.parser.ast.CompilationUnit;
import cfjapa.parser.ast.ImportDeclaration;
import cfjapa.parser.ast.IndexUnit;
import cfjapa.parser.ast.LineComment;
import cfjapa.parser.ast.PackageDeclaration;
import cfjapa.parser.ast.TypeParameter;
import cfjapa.parser.ast.body.AnnotationDeclaration;
import cfjapa.parser.ast.body.AnnotationMemberDeclaration;
import cfjapa.parser.ast.body.ClassOrInterfaceDeclaration;
import cfjapa.parser.ast.body.ConstructorDeclaration;
import cfjapa.parser.ast.body.EmptyMemberDeclaration;
import cfjapa.parser.ast.body.EmptyTypeDeclaration;
import cfjapa.parser.ast.body.EnumConstantDeclaration;
import cfjapa.parser.ast.body.EnumDeclaration;
import cfjapa.parser.ast.body.FieldDeclaration;
import cfjapa.parser.ast.body.InitializerDeclaration;
import cfjapa.parser.ast.body.JavadocComment;
import cfjapa.parser.ast.body.MethodDeclaration;
import cfjapa.parser.ast.body.Parameter;
import cfjapa.parser.ast.body.VariableDeclarator;
import cfjapa.parser.ast.body.VariableDeclaratorId;
import cfjapa.parser.ast.expr.ArrayAccessExpr;
import cfjapa.parser.ast.expr.ArrayCreationExpr;
import cfjapa.parser.ast.expr.ArrayInitializerExpr;
import cfjapa.parser.ast.expr.AssignExpr;
import cfjapa.parser.ast.expr.BinaryExpr;
import cfjapa.parser.ast.expr.BooleanLiteralExpr;
import cfjapa.parser.ast.expr.CastExpr;
import cfjapa.parser.ast.expr.CharLiteralExpr;
import cfjapa.parser.ast.expr.ClassExpr;
import cfjapa.parser.ast.expr.ConditionalExpr;
import cfjapa.parser.ast.expr.DoubleLiteralExpr;
import cfjapa.parser.ast.expr.EnclosedExpr;
import cfjapa.parser.ast.expr.FieldAccessExpr;
import cfjapa.parser.ast.expr.InstanceOfExpr;
import cfjapa.parser.ast.expr.IntegerLiteralExpr;
import cfjapa.parser.ast.expr.IntegerLiteralMinValueExpr;
import cfjapa.parser.ast.expr.LongLiteralExpr;
import cfjapa.parser.ast.expr.LongLiteralMinValueExpr;
import cfjapa.parser.ast.expr.MarkerAnnotationExpr;
import cfjapa.parser.ast.expr.MemberValuePair;
import cfjapa.parser.ast.expr.MethodCallExpr;
import cfjapa.parser.ast.expr.NameExpr;
import cfjapa.parser.ast.expr.NormalAnnotationExpr;
import cfjapa.parser.ast.expr.NullLiteralExpr;
import cfjapa.parser.ast.expr.ObjectCreationExpr;
import cfjapa.parser.ast.expr.QualifiedNameExpr;
import cfjapa.parser.ast.expr.SingleMemberAnnotationExpr;
import cfjapa.parser.ast.expr.StringLiteralExpr;
import cfjapa.parser.ast.expr.SuperExpr;
import cfjapa.parser.ast.expr.ThisExpr;
import cfjapa.parser.ast.expr.UnaryExpr;
import cfjapa.parser.ast.expr.VariableDeclarationExpr;
import cfjapa.parser.ast.stmt.AssertStmt;
import cfjapa.parser.ast.stmt.BlockStmt;
import cfjapa.parser.ast.stmt.BreakStmt;
import cfjapa.parser.ast.stmt.CatchClause;
import cfjapa.parser.ast.stmt.ContinueStmt;
import cfjapa.parser.ast.stmt.DoStmt;
import cfjapa.parser.ast.stmt.EmptyStmt;
import cfjapa.parser.ast.stmt.ExplicitConstructorInvocationStmt;
import cfjapa.parser.ast.stmt.ExpressionStmt;
import cfjapa.parser.ast.stmt.ForStmt;
import cfjapa.parser.ast.stmt.ForeachStmt;
import cfjapa.parser.ast.stmt.IfStmt;
import cfjapa.parser.ast.stmt.LabeledStmt;
import cfjapa.parser.ast.stmt.ReturnStmt;
import cfjapa.parser.ast.stmt.SwitchEntryStmt;
import cfjapa.parser.ast.stmt.SwitchStmt;
import cfjapa.parser.ast.stmt.SynchronizedStmt;
import cfjapa.parser.ast.stmt.ThrowStmt;
import cfjapa.parser.ast.stmt.TryStmt;
import cfjapa.parser.ast.stmt.TypeDeclarationStmt;
import cfjapa.parser.ast.stmt.WhileStmt;
import cfjapa.parser.ast.type.ClassOrInterfaceType;
import cfjapa.parser.ast.type.PrimitiveType;
import cfjapa.parser.ast.type.ReferenceType;
import cfjapa.parser.ast.type.VoidType;
import cfjapa.parser.ast.type.WildcardType;

/**
 * @author Julio Vilmar Gesser
 */
public interface GenericVisitor<R, A> {

    //- Compilation Unit ----------------------------------

    public R visit(IndexUnit n, A arg);

    public R visit(CompilationUnit n, A arg);

    public R visit(PackageDeclaration n, A arg);

    public R visit(ImportDeclaration n, A arg);

    public R visit(TypeParameter n, A arg);

    public R visit(LineComment n, A arg);

    public R visit(BlockComment n, A arg);

    //- Body ----------------------------------------------

    public R visit(ClassOrInterfaceDeclaration n, A arg);

    public R visit(EnumDeclaration n, A arg);

    public R visit(EmptyTypeDeclaration n, A arg);

    public R visit(EnumConstantDeclaration n, A arg);

    public R visit(AnnotationDeclaration n, A arg);

    public R visit(AnnotationMemberDeclaration n, A arg);

    public R visit(FieldDeclaration n, A arg);

    public R visit(VariableDeclarator n, A arg);

    public R visit(VariableDeclaratorId n, A arg);

    public R visit(ConstructorDeclaration n, A arg);

    public R visit(MethodDeclaration n, A arg);

    public R visit(Parameter n, A arg);

    public R visit(EmptyMemberDeclaration n, A arg);

    public R visit(InitializerDeclaration n, A arg);

    public R visit(JavadocComment n, A arg);

    //- Type ----------------------------------------------

    public R visit(ClassOrInterfaceType n, A arg);

    public R visit(PrimitiveType n, A arg);

    public R visit(ReferenceType n, A arg);

    public R visit(VoidType n, A arg);

    public R visit(WildcardType n, A arg);

    //- Expression ----------------------------------------

    public R visit(ArrayAccessExpr n, A arg);

    public R visit(ArrayCreationExpr n, A arg);

    public R visit(ArrayInitializerExpr n, A arg);

    public R visit(AssignExpr n, A arg);

    public R visit(BinaryExpr n, A arg);

    public R visit(CastExpr n, A arg);

    public R visit(ClassExpr n, A arg);

    public R visit(ConditionalExpr n, A arg);

    public R visit(EnclosedExpr n, A arg);

    public R visit(FieldAccessExpr n, A arg);

    public R visit(InstanceOfExpr n, A arg);

    public R visit(StringLiteralExpr n, A arg);

    public R visit(IntegerLiteralExpr n, A arg);

    public R visit(LongLiteralExpr n, A arg);

    public R visit(IntegerLiteralMinValueExpr n, A arg);

    public R visit(LongLiteralMinValueExpr n, A arg);

    public R visit(CharLiteralExpr n, A arg);

    public R visit(DoubleLiteralExpr n, A arg);

    public R visit(BooleanLiteralExpr n, A arg);

    public R visit(NullLiteralExpr n, A arg);

    public R visit(MethodCallExpr n, A arg);

    public R visit(NameExpr n, A arg);

    public R visit(ObjectCreationExpr n, A arg);

    public R visit(QualifiedNameExpr n, A arg);

    public R visit(ThisExpr n, A arg);

    public R visit(SuperExpr n, A arg);

    public R visit(UnaryExpr n, A arg);

    public R visit(VariableDeclarationExpr n, A arg);

    public R visit(MarkerAnnotationExpr n, A arg);

    public R visit(SingleMemberAnnotationExpr n, A arg);

    public R visit(NormalAnnotationExpr n, A arg);

    public R visit(MemberValuePair n, A arg);

    //- Statements ----------------------------------------

    public R visit(ExplicitConstructorInvocationStmt n, A arg);

    public R visit(TypeDeclarationStmt n, A arg);

    public R visit(AssertStmt n, A arg);

    public R visit(BlockStmt n, A arg);

    public R visit(LabeledStmt n, A arg);

    public R visit(EmptyStmt n, A arg);

    public R visit(ExpressionStmt n, A arg);

    public R visit(SwitchStmt n, A arg);

    public R visit(SwitchEntryStmt n, A arg);

    public R visit(BreakStmt n, A arg);

    public R visit(ReturnStmt n, A arg);

    public R visit(IfStmt n, A arg);

    public R visit(WhileStmt n, A arg);

    public R visit(ContinueStmt n, A arg);

    public R visit(DoStmt n, A arg);

    public R visit(ForeachStmt n, A arg);

    public R visit(ForStmt n, A arg);

    public R visit(ThrowStmt n, A arg);

    public R visit(SynchronizedStmt n, A arg);

    public R visit(TryStmt n, A arg);

    public R visit(CatchClause n, A arg);

}
