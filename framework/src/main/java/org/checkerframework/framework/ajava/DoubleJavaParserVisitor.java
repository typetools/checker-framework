package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
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
import java.util.List;

/**
 * A visitor that visits two JavaParser ASTs simultaneously that almost match. When calling a visit
 * method, the secondary argument must be another JavaParser {@code Node} that represents an AST
 * identical to the first argument except for the exceptions allowed between an ajava file and its
 * corresponding Java file, see the linked section of the manual.
 *
 * <p>To use this class, extend it and override {@link #defaultAction(Node, Node)}. This method will
 * be called on every pair of nodes in the two ASTs. This class does not visit annotations, since
 * those may differ between the two ASTs.
 *
 * @checker_framework.manual #ajava-contents ways in which the two visited ASTs may differ
 */
public abstract class DoubleJavaParserVisitor extends VoidVisitorAdapter<Node> {
  /**
   * Default action performed on all pairs of nodes from matching ASTs.
   *
   * @param node1 first node in pair
   * @param node2 second node in pair
   * @param <T> the Node type of {@code node1} and {@code node2}
   */
  public abstract <T extends Node> void defaultAction(T node1, T node2);

  /**
   * Given two lists with the same size where corresponding elements represent nodes with
   * almost-identical ASTs as specified in this class's description, visits each pair of
   * corresponding elements in order.
   *
   * @param list1 first list of nodes
   * @param list2 second list of nodes
   */
  private void visitLists(List<? extends Node> list1, List<? extends Node> list2) {
    assert list1.size() == list2.size();
    for (int i = 0; i < list1.size(); i++) {
      list1.get(i).accept(this, list2.get(i));
    }
  }

  @Override
  public void visit(final AnnotationDeclaration node1, final Node other) {
    AnnotationDeclaration node2 = (AnnotationDeclaration) other;
    defaultAction(node1, node2);
    visitLists(node1.getMembers(), node2.getMembers());
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final AnnotationMemberDeclaration node1, final Node other) {
    AnnotationMemberDeclaration node2 = (AnnotationMemberDeclaration) other;
    defaultAction(node1, node2);
    node1.getDefaultValue().ifPresent(l -> l.accept(this, node2.getDefaultValue().get()));
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(final ArrayAccessExpr node1, final Node other) {
    ArrayAccessExpr node2 = (ArrayAccessExpr) other;
    defaultAction(node1, node2);
    node1.getIndex().accept(this, node2.getIndex());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final ArrayCreationExpr node1, final Node other) {
    ArrayCreationExpr node2 = (ArrayCreationExpr) other;
    defaultAction(node1, node2);
    node1.getElementType().accept(this, node2.getElementType());
    node1.getInitializer().ifPresent(l -> l.accept(this, node2.getInitializer().get()));
    visitLists(node1.getLevels(), node2.getLevels());
  }

  @Override
  public void visit(final ArrayInitializerExpr node1, final Node other) {
    ArrayInitializerExpr node2 = (ArrayInitializerExpr) other;
    defaultAction(node1, node2);
    visitLists(node1.getValues(), node2.getValues());
  }

  @Override
  public void visit(final AssertStmt node1, final Node other) {
    AssertStmt node2 = (AssertStmt) other;
    defaultAction(node1, node2);
    node1.getCheck().accept(this, node2.getCheck());
    node1.getMessage().ifPresent(l -> l.accept(this, node2.getMessage().get()));
  }

  @Override
  public void visit(final AssignExpr node1, final Node other) {
    AssignExpr node2 = (AssignExpr) other;
    defaultAction(node1, node2);
    node1.getTarget().accept(this, node2.getTarget());
    node1.getValue().accept(this, node2.getValue());
  }

  @Override
  public void visit(final BinaryExpr node1, final Node other) {
    BinaryExpr node2 = (BinaryExpr) other;
    defaultAction(node1, node2);
    node1.getLeft().accept(this, node2.getLeft());
    node1.getRight().accept(this, node2.getRight());
  }

  @Override
  public void visit(final BlockComment node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final BlockStmt node1, final Node other) {
    BlockStmt node2 = (BlockStmt) other;
    defaultAction(node1, node2);
    visitLists(node1.getStatements(), node2.getStatements());
  }

  @Override
  public void visit(final BooleanLiteralExpr node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final BreakStmt node1, final Node other) {
    BreakStmt node2 = (BreakStmt) other;
    defaultAction(node1, node2);
    node1.getLabel().ifPresent(l -> l.accept(this, node2.getLabel().get()));
  }

  @Override
  public void visit(final CastExpr node1, final Node other) {
    CastExpr node2 = (CastExpr) other;
    defaultAction(node1, node2);
    node1.getExpression().accept(this, node2.getExpression());
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(final CatchClause node1, final Node other) {
    CatchClause node2 = (CatchClause) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    node1.getParameter().accept(this, node2.getParameter());
  }

  @Override
  public void visit(final CharLiteralExpr node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final ClassExpr node1, final Node other) {
    ClassExpr node2 = (ClassExpr) other;
    defaultAction(node1, node2);
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(final ClassOrInterfaceDeclaration node1, final Node other) {
    ClassOrInterfaceDeclaration node2 = (ClassOrInterfaceDeclaration) other;
    defaultAction(node1, node2);
    visitLists(node1.getExtendedTypes(), node2.getExtendedTypes());
    visitLists(node1.getImplementedTypes(), node2.getImplementedTypes());
    visitLists(node1.getTypeParameters(), node2.getTypeParameters());
    visitLists(node1.getMembers(), node2.getMembers());
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final ClassOrInterfaceType node1, final Node other) {
    ClassOrInterfaceType node2 = (ClassOrInterfaceType) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
    node1.getScope().ifPresent(l -> l.accept(this, node2.getScope().get()));
    node1.getTypeArguments().ifPresent(l -> visitLists(l, node2.getTypeArguments().get()));
  }

  @Override
  public void visit(final CompilationUnit node1, final Node other) {
    CompilationUnit node2 = (CompilationUnit) other;
    defaultAction(node1, node2);
    node1.getModule().ifPresent(l -> l.accept(this, node2.getModule().get()));
    node1
        .getPackageDeclaration()
        .ifPresent(l -> l.accept(this, node2.getPackageDeclaration().get()));
    visitLists(node1.getTypes(), node2.getTypes());
  }

  @Override
  public void visit(final ConditionalExpr node1, final Node other) {
    ConditionalExpr node2 = (ConditionalExpr) other;
    defaultAction(node1, node2);
    node1.getCondition().accept(this, node2.getCondition());
    node1.getElseExpr().accept(this, node2.getElseExpr());
    node1.getThenExpr().accept(this, node2.getThenExpr());
  }

  @Override
  public void visit(final ConstructorDeclaration node1, final Node other) {
    ConstructorDeclaration node2 = (ConstructorDeclaration) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
    visitLists(node1.getParameters(), node2.getParameters());
    if (node1.getReceiverParameter().isPresent() && node2.getReceiverParameter().isPresent()) {
      node1.getReceiverParameter().get().accept(this, node2.getReceiverParameter().get());
    }

    visitLists(node1.getThrownExceptions(), node2.getThrownExceptions());
    visitLists(node1.getTypeParameters(), node2.getTypeParameters());
  }

  @Override
  public void visit(CompactConstructorDeclaration node1, Node other) {
    CompactConstructorDeclaration node2 = (CompactConstructorDeclaration) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());

    visitLists(node1.getThrownExceptions(), node2.getThrownExceptions());
    visitLists(node1.getTypeParameters(), node2.getTypeParameters());
  }

  @Override
  public void visit(final ContinueStmt node1, final Node other) {
    ContinueStmt node2 = (ContinueStmt) other;
    defaultAction(node1, node2);
    node1.getLabel().ifPresent(l -> l.accept(this, node2.getLabel().get()));
  }

  @Override
  public void visit(final DoStmt node1, final Node other) {
    DoStmt node2 = (DoStmt) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    node1.getCondition().accept(this, node2.getCondition());
  }

  @Override
  public void visit(final DoubleLiteralExpr node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final EmptyStmt node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final EnclosedExpr node1, final Node other) {
    EnclosedExpr node2 = (EnclosedExpr) other;
    defaultAction(node1, node2);
    node1.getInner().accept(this, node2.getInner());
  }

  @Override
  public void visit(final EnumConstantDeclaration node1, final Node other) {
    EnumConstantDeclaration node2 = (EnumConstantDeclaration) other;
    defaultAction(node1, node2);
    visitLists(node1.getArguments(), node2.getArguments());
    visitLists(node1.getClassBody(), node2.getClassBody());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final EnumDeclaration node1, final Node other) {
    EnumDeclaration node2 = (EnumDeclaration) other;
    defaultAction(node1, node2);
    visitLists(node1.getEntries(), node2.getEntries());
    visitLists(node1.getImplementedTypes(), node2.getImplementedTypes());
    visitLists(node1.getMembers(), node2.getMembers());
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final ExplicitConstructorInvocationStmt node1, final Node other) {
    ExplicitConstructorInvocationStmt node2 = (ExplicitConstructorInvocationStmt) other;
    defaultAction(node1, node2);
    visitLists(node1.getArguments(), node2.getArguments());
    node1.getExpression().ifPresent(l -> l.accept(this, node2.getExpression().get()));
    node1.getTypeArguments().ifPresent(l -> visitLists(l, node2.getTypeArguments().get()));
  }

  @Override
  public void visit(final ExpressionStmt node1, final Node other) {
    ExpressionStmt node2 = (ExpressionStmt) other;
    defaultAction(node1, node2);
    node1.getExpression().accept(this, node2.getExpression());
  }

  @Override
  public void visit(final FieldAccessExpr node1, final Node other) {
    FieldAccessExpr node2 = (FieldAccessExpr) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
    node1.getScope().accept(this, node2.getScope());
    node1.getTypeArguments().ifPresent(l -> visitLists(l, node2.getTypeArguments().get()));
  }

  @Override
  public void visit(final FieldDeclaration node1, final Node other) {
    FieldDeclaration node2 = (FieldDeclaration) other;
    defaultAction(node1, node2);
    visitLists(node1.getModifiers(), node2.getModifiers());
    visitLists(node1.getVariables(), node2.getVariables());
  }

  @Override
  public void visit(final ForEachStmt node1, final Node other) {
    ForEachStmt node2 = (ForEachStmt) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    node1.getIterable().accept(this, node2.getIterable());
    node1.getVariable().accept(this, node2.getVariable());
  }

  @Override
  public void visit(final ForStmt node1, final Node other) {
    ForStmt node2 = (ForStmt) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    node1.getCompare().ifPresent(l -> l.accept(this, node2.getCompare().get()));
    visitLists(node1.getInitialization(), node2.getInitialization());
    visitLists(node1.getUpdate(), node2.getUpdate());
  }

  @Override
  public void visit(final IfStmt node1, final Node other) {
    IfStmt node2 = (IfStmt) other;
    defaultAction(node1, node2);
    node1.getCondition().accept(this, node2.getCondition());
    node1.getElseStmt().ifPresent(l -> l.accept(this, node2.getElseStmt().get()));
    node1.getThenStmt().accept(this, node2.getThenStmt());
  }

  @Override
  public void visit(final InitializerDeclaration node1, final Node other) {
    InitializerDeclaration node2 = (InitializerDeclaration) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
  }

  @Override
  public void visit(final InstanceOfExpr node1, final Node other) {
    InstanceOfExpr node2 = (InstanceOfExpr) other;
    defaultAction(node1, node2);
    node1.getExpression().accept(this, node2.getExpression());
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(final IntegerLiteralExpr node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final JavadocComment node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final LabeledStmt node1, final Node other) {
    LabeledStmt node2 = (LabeledStmt) other;
    defaultAction(node1, node2);
    node1.getLabel().accept(this, node2.getLabel());
    node1.getStatement().accept(this, node2.getStatement());
  }

  @Override
  public void visit(final LineComment node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final LongLiteralExpr node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final MarkerAnnotationExpr node1, final Node other) {
    MarkerAnnotationExpr node2 = (MarkerAnnotationExpr) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final MemberValuePair node1, final Node other) {
    MemberValuePair node2 = (MemberValuePair) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
    node1.getValue().accept(this, node2.getName());
  }

  @Override
  public void visit(final MethodCallExpr node1, final Node other) {
    MethodCallExpr node2 = (MethodCallExpr) other;
    defaultAction(node1, node2);
    visitLists(node1.getArguments(), node2.getArguments());
    node1.getName().accept(this, node2.getName());
    node1.getScope().ifPresent(l -> l.accept(this, node2.getScope().get()));
    node1.getTypeArguments().ifPresent(l -> visitLists(l, node2.getTypeArguments().get()));
  }

  @Override
  public void visit(final MethodDeclaration node1, final Node other) {
    MethodDeclaration node2 = (MethodDeclaration) other;
    defaultAction(node1, node2);
    node1.getBody().ifPresent(l -> l.accept(this, node2.getBody().get()));
    node1.getType().accept(this, node2.getType());
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
    visitLists(node1.getParameters(), node2.getParameters());
    if (node1.getReceiverParameter().isPresent() && node2.getReceiverParameter().isPresent()) {
      node1.getReceiverParameter().get().accept(this, node2.getReceiverParameter().get());
    }

    visitLists(node1.getThrownExceptions(), node2.getThrownExceptions());
    visitLists(node1.getTypeParameters(), node2.getTypeParameters());
  }

  @Override
  public void visit(final NameExpr node1, final Node other) {
    NameExpr node2 = (NameExpr) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final NormalAnnotationExpr node1, final Node other) {
    NormalAnnotationExpr node2 = (NormalAnnotationExpr) other;
    defaultAction(node1, node2);
    visitLists(node1.getPairs(), node2.getPairs());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final NullLiteralExpr node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final ObjectCreationExpr node1, final Node other) {
    ObjectCreationExpr node2 = (ObjectCreationExpr) other;
    defaultAction(node1, node2);
    node1
        .getAnonymousClassBody()
        .ifPresent(l -> visitLists(l, node2.getAnonymousClassBody().get()));
    visitLists(node1.getArguments(), node2.getArguments());
    node1.getScope().ifPresent(l -> l.accept(this, node2.getScope().get()));
    node1.getType().accept(this, node2.getType());
    node1.getTypeArguments().ifPresent(l -> visitLists(l, node2.getTypeArguments().get()));
  }

  @Override
  public void visit(final PackageDeclaration node1, final Node other) {
    PackageDeclaration node2 = (PackageDeclaration) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final Parameter node1, final Node other) {
    Parameter node2 = (Parameter) other;
    defaultAction(node1, node2);
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(final PrimitiveType node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final Name node1, final Node other) {
    Name node2 = (Name) other;
    defaultAction(node1, node2);
    node1.getQualifier().ifPresent(l -> l.accept(this, node2.getQualifier().get()));
  }

  @Override
  public void visit(final SimpleName node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final ArrayType node1, final Node other) {
    ArrayType node2 = (ArrayType) other;
    defaultAction(node1, node2);
    node1.getComponentType().accept(this, node2.getComponentType());
  }

  @Override
  public void visit(final ArrayCreationLevel node1, final Node other) {
    ArrayCreationLevel node2 = (ArrayCreationLevel) other;
    defaultAction(node1, node2);
    node1.getDimension().ifPresent(l -> l.accept(this, node2.getDimension().get()));
  }

  @Override
  public void visit(final IntersectionType node1, final Node other) {
    IntersectionType node2 = (IntersectionType) other;
    defaultAction(node1, node2);
    visitLists(node1.getElements(), node2.getElements());
  }

  @Override
  public void visit(final UnionType node1, final Node other) {
    UnionType node2 = (UnionType) other;
    defaultAction(node1, node2);
    visitLists(node1.getElements(), node2.getElements());
  }

  @Override
  public void visit(RecordDeclaration node1, Node other) {
    RecordDeclaration node2 = (RecordDeclaration) other;
    defaultAction(node1, node2);
    visitLists(node1.getImplementedTypes(), node2.getImplementedTypes());
    visitLists(node1.getTypeParameters(), node2.getTypeParameters());
    visitLists(node1.getParameters(), node2.getParameters());
    visitLists(node1.getMembers(), node2.getMembers());
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
    if (node1.getReceiverParameter().isPresent() && node2.getReceiverParameter().isPresent()) {
      node1.getReceiverParameter().get().accept(this, node2.getReceiverParameter().get());
    }
  }

  @Override
  public void visit(final ReturnStmt node1, final Node other) {
    ReturnStmt node2 = (ReturnStmt) other;
    defaultAction(node1, node2);
    node1.getExpression().ifPresent(l -> l.accept(this, node2.getExpression().get()));
  }

  @Override
  public void visit(final SingleMemberAnnotationExpr node1, final Node other) {
    SingleMemberAnnotationExpr node2 = (SingleMemberAnnotationExpr) other;
    defaultAction(node1, node2);
    node1.getMemberValue().accept(this, node2.getMemberValue());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final StringLiteralExpr node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final SuperExpr node1, final Node other) {
    SuperExpr node2 = (SuperExpr) other;
    defaultAction(node1, node2);
    node1.getTypeName().ifPresent(l -> l.accept(this, node2.getTypeName().get()));
  }

  @Override
  public void visit(final SwitchEntry node1, final Node other) {
    SwitchEntry node2 = (SwitchEntry) other;
    defaultAction(node1, node2);
    visitLists(node1.getLabels(), node2.getLabels());
    visitLists(node1.getStatements(), node2.getStatements());
  }

  @Override
  public void visit(final SwitchStmt node1, final Node other) {
    SwitchStmt node2 = (SwitchStmt) other;
    defaultAction(node1, node2);
    visitLists(node1.getEntries(), node2.getEntries());
    node1.getSelector().accept(this, node2.getSelector());
  }

  @Override
  public void visit(final SynchronizedStmt node1, final Node other) {
    SynchronizedStmt node2 = (SynchronizedStmt) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    node1.getExpression().accept(this, node2.getExpression());
  }

  @Override
  public void visit(final ThisExpr node1, final Node other) {
    ThisExpr node2 = (ThisExpr) other;
    defaultAction(node1, node2);
    node1.getTypeName().ifPresent(l -> l.accept(this, node2.getTypeName().get()));
  }

  @Override
  public void visit(final ThrowStmt node1, final Node other) {
    ThrowStmt node2 = (ThrowStmt) other;
    defaultAction(node1, node2);
    node1.getExpression().accept(this, node2.getExpression());
  }

  @Override
  public void visit(final TryStmt node1, final Node other) {
    TryStmt node2 = (TryStmt) other;
    defaultAction(node1, node2);
    visitLists(node1.getCatchClauses(), node2.getCatchClauses());
    node1.getFinallyBlock().ifPresent(l -> l.accept(this, node2.getFinallyBlock().get()));
    visitLists(node1.getResources(), node2.getResources());
    node1.getTryBlock().accept(this, node2.getTryBlock());
  }

  @Override
  public void visit(final LocalClassDeclarationStmt node1, final Node other) {
    LocalClassDeclarationStmt node2 = (LocalClassDeclarationStmt) other;
    defaultAction(node1, node2);
    node1.getClassDeclaration().accept(this, node2.getClassDeclaration());
  }

  @Override
  public void visit(LocalRecordDeclarationStmt node1, final Node other) {
    LocalRecordDeclarationStmt node2 = (LocalRecordDeclarationStmt) other;
    defaultAction(node1, node2);
    node1.getRecordDeclaration().accept(this, node2.getRecordDeclaration());
  }

  @Override
  public void visit(final TypeParameter node1, final Node other) {
    TypeParameter node2 = (TypeParameter) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
    // Since ajava files and its corresponding Java file may differ in whether they contain a type
    // bound, only visit type bounds if they're present in both nodes.
    if (node1.getTypeBound().isEmpty() == node2.getTypeBound().isEmpty()) {
      visitLists(node1.getTypeBound(), node2.getTypeBound());
    }
  }

  @Override
  public void visit(final UnaryExpr node1, final Node other) {
    UnaryExpr node2 = (UnaryExpr) other;
    defaultAction(node1, node2);
    node1.getExpression().accept(this, node2.getExpression());
  }

  @Override
  public void visit(final UnknownType node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final VariableDeclarationExpr node1, final Node other) {
    VariableDeclarationExpr node2 = (VariableDeclarationExpr) other;
    defaultAction(node1, node2);
    visitLists(node1.getModifiers(), node2.getModifiers());
    visitLists(node1.getVariables(), node2.getVariables());
  }

  @Override
  public void visit(final VariableDeclarator node1, final Node other) {
    VariableDeclarator node2 = (VariableDeclarator) other;
    defaultAction(node1, node2);
    node1.getInitializer().ifPresent(l -> l.accept(this, node2.getInitializer().get()));
    node1.getName().accept(this, node2.getName());
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(final VoidType node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final WhileStmt node1, final Node other) {
    WhileStmt node2 = (WhileStmt) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    node1.getCondition().accept(this, node2.getCondition());
  }

  @Override
  public void visit(final WildcardType node1, final Node other) {
    WildcardType node2 = (WildcardType) other;
    defaultAction(node1, node2);
    node1.getExtendedType().ifPresent(l -> l.accept(this, node2.getExtendedType().get()));
    node1.getSuperType().ifPresent(l -> l.accept(this, node2.getSuperType().get()));
  }

  @Override
  public void visit(final LambdaExpr node1, final Node other) {
    LambdaExpr node2 = (LambdaExpr) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    visitLists(node1.getParameters(), node2.getParameters());
  }

  @Override
  public void visit(final MethodReferenceExpr node1, final Node other) {
    MethodReferenceExpr node2 = (MethodReferenceExpr) other;
    defaultAction(node1, node2);
    node1.getScope().accept(this, node2.getScope());
    node1.getTypeArguments().ifPresent(l -> visitLists(l, node2.getTypeArguments().get()));
  }

  @Override
  public void visit(final TypeExpr node1, final Node other) {
    TypeExpr node2 = (TypeExpr) other;
    defaultAction(node1, node2);
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(final ImportDeclaration node1, final Node other) {
    ImportDeclaration node2 = (ImportDeclaration) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final ModuleDeclaration node1, final Node other) {
    ModuleDeclaration node2 = (ModuleDeclaration) other;
    defaultAction(node1, node2);
    visitLists(node1.getDirectives(), node2.getDirectives());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final ModuleRequiresDirective node1, final Node other) {
    ModuleRequiresDirective node2 = (ModuleRequiresDirective) other;
    defaultAction(node1, node2);
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final ModuleExportsDirective node1, final Node other) {
    ModuleExportsDirective node2 = (ModuleExportsDirective) other;
    defaultAction(node1, node2);
    visitLists(node1.getModuleNames(), node2.getModuleNames());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final ModuleProvidesDirective node1, final Node other) {
    ModuleProvidesDirective node2 = (ModuleProvidesDirective) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
    visitLists(node1.getWith(), node2.getWith());
  }

  @Override
  public void visit(final ModuleUsesDirective node1, final Node other) {
    ModuleUsesDirective node2 = (ModuleUsesDirective) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final ModuleOpensDirective node1, final Node other) {
    ModuleOpensDirective node2 = (ModuleOpensDirective) other;
    defaultAction(node1, node2);
    visitLists(node1.getModuleNames(), node2.getModuleNames());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(final UnparsableStmt node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final ReceiverParameter node1, final Node other) {
    ReceiverParameter node2 = (ReceiverParameter) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(final VarType node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final Modifier node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final SwitchExpr node1, final Node other) {
    SwitchExpr node2 = (SwitchExpr) other;
    defaultAction(node1, node2);
    visitLists(node1.getEntries(), node2.getEntries());
    node1.getSelector().accept(this, node2.getSelector());
  }

  @Override
  public void visit(final TextBlockLiteralExpr node1, final Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(final YieldStmt node1, final Node other) {
    YieldStmt node2 = (YieldStmt) other;
    defaultAction(node1, node2);
    node1.getExpression().accept(this, node2.getExpression());
  }
}
