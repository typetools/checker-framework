package org.checkerframework.framework.util;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.StubUnit;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;
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
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.UnparsableStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * A visitor that visits every node in an AST by default and performs a default action on each node
 * after visiting its children.
 *
 * <p>To use this class, override {@code defaultAction}. Unlike JavaParser's {@code
 * VoidVisitorWithDefaults}, visiting a node also visits all its children. This allows easily
 * performing an action on each node of an AST.
 */
public abstract class VoidVisitorWithDefaultAction extends VoidVisitorAdapter<Void> {
  /**
   * Action performed on each visited node.
   *
   * @param node node to perform action on
   */
  public abstract void defaultAction(Node node);

  @Override
  public void visit(AnnotationDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(AnnotationMemberDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ArrayAccessExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ArrayCreationExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ArrayCreationLevel n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ArrayInitializerExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ArrayType n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(AssertStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(AssignExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(BinaryExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(BlockComment n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(BlockStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(BooleanLiteralExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(BreakStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(CastExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(CatchClause n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(CharLiteralExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ClassExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ClassOrInterfaceDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ClassOrInterfaceType n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(CompilationUnit n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(StubUnit n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ConditionalExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ConstructorDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ContinueStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(DoStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(DoubleLiteralExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(EmptyStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(EnclosedExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(EnumConstantDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(EnumDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ExplicitConstructorInvocationStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ExpressionStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(FieldAccessExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(FieldDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ForStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ForEachStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(IfStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ImportDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(InitializerDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(InstanceOfExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(IntegerLiteralExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(IntersectionType n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(JavadocComment n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(LabeledStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(LambdaExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(LineComment n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(LocalClassDeclarationStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(LongLiteralExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(MarkerAnnotationExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(MemberValuePair n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(MethodCallExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(MethodDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(MethodReferenceExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(NameExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(Name n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(NormalAnnotationExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(NullLiteralExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ObjectCreationExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(PackageDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(Parameter n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(PrimitiveType n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ReturnStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(SimpleName n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(SingleMemberAnnotationExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(StringLiteralExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(SuperExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(SwitchEntry n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(SwitchStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(SynchronizedStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ThisExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ThrowStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(TryStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(TypeExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(TypeParameter n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(UnaryExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(UnionType n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(UnknownType n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(VariableDeclarationExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(VariableDeclarator n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(VoidType n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(WhileStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(WildcardType n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ModuleDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ModuleRequiresDirective n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ModuleExportsDirective n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ModuleProvidesDirective n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ModuleUsesDirective n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ModuleOpensDirective n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(UnparsableStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(ReceiverParameter n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(VarType n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(Modifier n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(SwitchExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(TextBlockLiteralExpr n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(YieldStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(RecordDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(LocalRecordDeclarationStmt n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }

  @Override
  public void visit(CompactConstructorDeclaration n, Void p) {
    super.visit(n, p);
    defaultAction(n);
  }
}
