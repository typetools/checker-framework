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
package org.checkerframework.stubparser.ast.visitor;

import org.checkerframework.stubparser.ast.BlockComment;
import org.checkerframework.stubparser.ast.CompilationUnit;
import org.checkerframework.stubparser.ast.ImportDeclaration;
import org.checkerframework.stubparser.ast.IndexUnit;
import org.checkerframework.stubparser.ast.LineComment;
import org.checkerframework.stubparser.ast.PackageDeclaration;
import org.checkerframework.stubparser.ast.TypeParameter;
import org.checkerframework.stubparser.ast.body.AnnotationDeclaration;
import org.checkerframework.stubparser.ast.body.AnnotationMemberDeclaration;
import org.checkerframework.stubparser.ast.body.ClassOrInterfaceDeclaration;
import org.checkerframework.stubparser.ast.body.ConstructorDeclaration;
import org.checkerframework.stubparser.ast.body.EmptyMemberDeclaration;
import org.checkerframework.stubparser.ast.body.EmptyTypeDeclaration;
import org.checkerframework.stubparser.ast.body.EnumConstantDeclaration;
import org.checkerframework.stubparser.ast.body.EnumDeclaration;
import org.checkerframework.stubparser.ast.body.FieldDeclaration;
import org.checkerframework.stubparser.ast.body.InitializerDeclaration;
import org.checkerframework.stubparser.ast.body.JavadocComment;
import org.checkerframework.stubparser.ast.body.MethodDeclaration;
import org.checkerframework.stubparser.ast.body.Parameter;
import org.checkerframework.stubparser.ast.body.VariableDeclarator;
import org.checkerframework.stubparser.ast.body.VariableDeclaratorId;
import org.checkerframework.stubparser.ast.expr.ArrayAccessExpr;
import org.checkerframework.stubparser.ast.expr.ArrayCreationExpr;
import org.checkerframework.stubparser.ast.expr.ArrayInitializerExpr;
import org.checkerframework.stubparser.ast.expr.AssignExpr;
import org.checkerframework.stubparser.ast.expr.BinaryExpr;
import org.checkerframework.stubparser.ast.expr.BooleanLiteralExpr;
import org.checkerframework.stubparser.ast.expr.CastExpr;
import org.checkerframework.stubparser.ast.expr.CharLiteralExpr;
import org.checkerframework.stubparser.ast.expr.ClassExpr;
import org.checkerframework.stubparser.ast.expr.ConditionalExpr;
import org.checkerframework.stubparser.ast.expr.DoubleLiteralExpr;
import org.checkerframework.stubparser.ast.expr.EnclosedExpr;
import org.checkerframework.stubparser.ast.expr.FieldAccessExpr;
import org.checkerframework.stubparser.ast.expr.InstanceOfExpr;
import org.checkerframework.stubparser.ast.expr.IntegerLiteralExpr;
import org.checkerframework.stubparser.ast.expr.IntegerLiteralMinValueExpr;
import org.checkerframework.stubparser.ast.expr.LongLiteralExpr;
import org.checkerframework.stubparser.ast.expr.LongLiteralMinValueExpr;
import org.checkerframework.stubparser.ast.expr.MarkerAnnotationExpr;
import org.checkerframework.stubparser.ast.expr.MemberValuePair;
import org.checkerframework.stubparser.ast.expr.MethodCallExpr;
import org.checkerframework.stubparser.ast.expr.NameExpr;
import org.checkerframework.stubparser.ast.expr.NormalAnnotationExpr;
import org.checkerframework.stubparser.ast.expr.NullLiteralExpr;
import org.checkerframework.stubparser.ast.expr.ObjectCreationExpr;
import org.checkerframework.stubparser.ast.expr.QualifiedNameExpr;
import org.checkerframework.stubparser.ast.expr.SingleMemberAnnotationExpr;
import org.checkerframework.stubparser.ast.expr.StringLiteralExpr;
import org.checkerframework.stubparser.ast.expr.SuperExpr;
import org.checkerframework.stubparser.ast.expr.ThisExpr;
import org.checkerframework.stubparser.ast.expr.UnaryExpr;
import org.checkerframework.stubparser.ast.expr.VariableDeclarationExpr;
import org.checkerframework.stubparser.ast.stmt.AssertStmt;
import org.checkerframework.stubparser.ast.stmt.BlockStmt;
import org.checkerframework.stubparser.ast.stmt.BreakStmt;
import org.checkerframework.stubparser.ast.stmt.CatchClause;
import org.checkerframework.stubparser.ast.stmt.ContinueStmt;
import org.checkerframework.stubparser.ast.stmt.DoStmt;
import org.checkerframework.stubparser.ast.stmt.EmptyStmt;
import org.checkerframework.stubparser.ast.stmt.ExplicitConstructorInvocationStmt;
import org.checkerframework.stubparser.ast.stmt.ExpressionStmt;
import org.checkerframework.stubparser.ast.stmt.ForStmt;
import org.checkerframework.stubparser.ast.stmt.ForeachStmt;
import org.checkerframework.stubparser.ast.stmt.IfStmt;
import org.checkerframework.stubparser.ast.stmt.LabeledStmt;
import org.checkerframework.stubparser.ast.stmt.ReturnStmt;
import org.checkerframework.stubparser.ast.stmt.SwitchEntryStmt;
import org.checkerframework.stubparser.ast.stmt.SwitchStmt;
import org.checkerframework.stubparser.ast.stmt.SynchronizedStmt;
import org.checkerframework.stubparser.ast.stmt.ThrowStmt;
import org.checkerframework.stubparser.ast.stmt.TryStmt;
import org.checkerframework.stubparser.ast.stmt.TypeDeclarationStmt;
import org.checkerframework.stubparser.ast.stmt.WhileStmt;
import org.checkerframework.stubparser.ast.type.ClassOrInterfaceType;
import org.checkerframework.stubparser.ast.type.PrimitiveType;
import org.checkerframework.stubparser.ast.type.ReferenceType;
import org.checkerframework.stubparser.ast.type.VoidType;
import org.checkerframework.stubparser.ast.type.WildcardType;

/**
 * @author Julio Vilmar Gesser
 */
public interface VoidVisitor<A> {

    //- Compilation Unit ----------------------------------

    public void visit(IndexUnit n, A arg);

    public void visit(CompilationUnit n, A arg);

    public void visit(PackageDeclaration n, A arg);

    public void visit(ImportDeclaration n, A arg);

    public void visit(TypeParameter n, A arg);

    public void visit(LineComment n, A arg);

    public void visit(BlockComment n, A arg);

    //- Body ----------------------------------------------

    public void visit(ClassOrInterfaceDeclaration n, A arg);

    public void visit(EnumDeclaration n, A arg);

    public void visit(EmptyTypeDeclaration n, A arg);

    public void visit(EnumConstantDeclaration n, A arg);

    public void visit(AnnotationDeclaration n, A arg);

    public void visit(AnnotationMemberDeclaration n, A arg);

    public void visit(FieldDeclaration n, A arg);

    public void visit(VariableDeclarator n, A arg);

    public void visit(VariableDeclaratorId n, A arg);

    public void visit(ConstructorDeclaration n, A arg);

    public void visit(MethodDeclaration n, A arg);

    public void visit(Parameter n, A arg);

    public void visit(EmptyMemberDeclaration n, A arg);

    public void visit(InitializerDeclaration n, A arg);

    public void visit(JavadocComment n, A arg);

    //- Type ----------------------------------------------

    public void visit(ClassOrInterfaceType n, A arg);

    public void visit(PrimitiveType n, A arg);

    public void visit(ReferenceType n, A arg);

    public void visit(VoidType n, A arg);

    public void visit(WildcardType n, A arg);

    //- Expression ----------------------------------------

    public void visit(ArrayAccessExpr n, A arg);

    public void visit(ArrayCreationExpr n, A arg);

    public void visit(ArrayInitializerExpr n, A arg);

    public void visit(AssignExpr n, A arg);

    public void visit(BinaryExpr n, A arg);

    public void visit(CastExpr n, A arg);

    public void visit(ClassExpr n, A arg);

    public void visit(ConditionalExpr n, A arg);

    public void visit(EnclosedExpr n, A arg);

    public void visit(FieldAccessExpr n, A arg);

    public void visit(InstanceOfExpr n, A arg);

    public void visit(StringLiteralExpr n, A arg);

    public void visit(IntegerLiteralExpr n, A arg);

    public void visit(LongLiteralExpr n, A arg);

    public void visit(IntegerLiteralMinValueExpr n, A arg);

    public void visit(LongLiteralMinValueExpr n, A arg);

    public void visit(CharLiteralExpr n, A arg);

    public void visit(DoubleLiteralExpr n, A arg);

    public void visit(BooleanLiteralExpr n, A arg);

    public void visit(NullLiteralExpr n, A arg);

    public void visit(MethodCallExpr n, A arg);

    public void visit(NameExpr n, A arg);

    public void visit(ObjectCreationExpr n, A arg);

    public void visit(QualifiedNameExpr n, A arg);

    public void visit(ThisExpr n, A arg);

    public void visit(SuperExpr n, A arg);

    public void visit(UnaryExpr n, A arg);

    public void visit(VariableDeclarationExpr n, A arg);

    public void visit(MarkerAnnotationExpr n, A arg);

    public void visit(SingleMemberAnnotationExpr n, A arg);

    public void visit(NormalAnnotationExpr n, A arg);

    public void visit(MemberValuePair n, A arg);

    //- Statements ----------------------------------------

    public void visit(ExplicitConstructorInvocationStmt n, A arg);

    public void visit(TypeDeclarationStmt n, A arg);

    public void visit(AssertStmt n, A arg);

    public void visit(BlockStmt n, A arg);

    public void visit(LabeledStmt n, A arg);

    public void visit(EmptyStmt n, A arg);

    public void visit(ExpressionStmt n, A arg);

    public void visit(SwitchStmt n, A arg);

    public void visit(SwitchEntryStmt n, A arg);

    public void visit(BreakStmt n, A arg);

    public void visit(ReturnStmt n, A arg);

    public void visit(IfStmt n, A arg);

    public void visit(WhileStmt n, A arg);

    public void visit(ContinueStmt n, A arg);

    public void visit(DoStmt n, A arg);

    public void visit(ForeachStmt n, A arg);

    public void visit(ForStmt n, A arg);

    public void visit(ThrowStmt n, A arg);

    public void visit(SynchronizedStmt n, A arg);

    public void visit(TryStmt n, A arg);

    public void visit(CatchClause n, A arg);

}
