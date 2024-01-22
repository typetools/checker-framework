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
@SuppressWarnings("optional:method.invocation") // parallel structure of two data structures
public abstract class DoubleJavaParserVisitor extends VoidVisitorAdapter<Node> {

  /** Create a DoubleJavaParserVisitor. */
  public DoubleJavaParserVisitor() {}

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
   * @param list2 second list of nodes, which has the same size as the first list
   */
  private void visitLists(List<? extends Node> list1, List<? extends Node> list2) {
    if (list1.size() != list2.size()) {
      throw new Error(
          String.format(
              "%s.visitLists(%s [size %d], %s [size %d])",
              this.getClass().getCanonicalName(), list1, list1.size(), list2, list2.size()));
    }
    for (int i = 0; i < list1.size(); i++) {
      list1.get(i).accept(this, list2.get(i));
    }
  }

  @Override
  public void visit(AnnotationDeclaration node1, Node other) {
    AnnotationDeclaration node2 = (AnnotationDeclaration) other;
    defaultAction(node1, node2);
    visitLists(node1.getMembers(), node2.getMembers());
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(AnnotationMemberDeclaration node1, Node other) {
    AnnotationMemberDeclaration node2 = (AnnotationMemberDeclaration) other;
    defaultAction(node1, node2);
    node1.getDefaultValue().ifPresent(dv -> dv.accept(this, node2.getDefaultValue().get()));
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(ArrayAccessExpr node1, Node other) {
    ArrayAccessExpr node2 = (ArrayAccessExpr) other;
    defaultAction(node1, node2);
    node1.getIndex().accept(this, node2.getIndex());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(ArrayCreationExpr node1, Node other) {
    ArrayCreationExpr node2 = (ArrayCreationExpr) other;
    defaultAction(node1, node2);
    node1.getElementType().accept(this, node2.getElementType());
    node1.getInitializer().ifPresent(init -> init.accept(this, node2.getInitializer().get()));
    visitLists(node1.getLevels(), node2.getLevels());
  }

  @Override
  public void visit(ArrayInitializerExpr node1, Node other) {
    ArrayInitializerExpr node2 = (ArrayInitializerExpr) other;
    defaultAction(node1, node2);
    visitLists(node1.getValues(), node2.getValues());
  }

  @Override
  public void visit(AssertStmt node1, Node other) {
    AssertStmt node2 = (AssertStmt) other;
    defaultAction(node1, node2);
    node1.getCheck().accept(this, node2.getCheck());
    node1.getMessage().ifPresent(m -> m.accept(this, node2.getMessage().get()));
  }

  @Override
  public void visit(AssignExpr node1, Node other) {
    AssignExpr node2 = (AssignExpr) other;
    defaultAction(node1, node2);
    node1.getTarget().accept(this, node2.getTarget());
    node1.getValue().accept(this, node2.getValue());
  }

  @Override
  public void visit(BinaryExpr node1, Node other) {
    BinaryExpr node2 = (BinaryExpr) other;
    defaultAction(node1, node2);
    node1.getLeft().accept(this, node2.getLeft());
    node1.getRight().accept(this, node2.getRight());
  }

  @Override
  public void visit(BlockComment node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(BlockStmt node1, Node other) {
    BlockStmt node2 = (BlockStmt) other;
    defaultAction(node1, node2);
    visitLists(node1.getStatements(), node2.getStatements());
  }

  @Override
  public void visit(BooleanLiteralExpr node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(BreakStmt node1, Node other) {
    BreakStmt node2 = (BreakStmt) other;
    defaultAction(node1, node2);
    node1.getLabel().ifPresent(l -> l.accept(this, node2.getLabel().get()));
  }

  @Override
  public void visit(CastExpr node1, Node other) {
    CastExpr node2 = (CastExpr) other;
    defaultAction(node1, node2);
    node1.getExpression().accept(this, node2.getExpression());
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(CatchClause node1, Node other) {
    CatchClause node2 = (CatchClause) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    node1.getParameter().accept(this, node2.getParameter());
  }

  @Override
  public void visit(CharLiteralExpr node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(ClassExpr node1, Node other) {
    ClassExpr node2 = (ClassExpr) other;
    defaultAction(node1, node2);
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(ClassOrInterfaceDeclaration node1, Node other) {
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
  public void visit(ClassOrInterfaceType node1, Node other) {
    ClassOrInterfaceType node2 = (ClassOrInterfaceType) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
    node1.getScope().ifPresent(s -> s.accept(this, node2.getScope().get()));
    node1.getTypeArguments().ifPresent(targs -> visitLists(targs, node2.getTypeArguments().get()));
  }

  @Override
  public void visit(CompilationUnit node1, Node other) {
    CompilationUnit node2 = (CompilationUnit) other;
    defaultAction(node1, node2);
    node1.getModule().ifPresent(m -> m.accept(this, node2.getModule().get()));
    node1
        .getPackageDeclaration()
        .ifPresent(pd -> pd.accept(this, node2.getPackageDeclaration().get()));
    visitLists(node1.getTypes(), node2.getTypes());
  }

  @Override
  public void visit(ConditionalExpr node1, Node other) {
    ConditionalExpr node2 = (ConditionalExpr) other;
    defaultAction(node1, node2);
    node1.getCondition().accept(this, node2.getCondition());
    node1.getElseExpr().accept(this, node2.getElseExpr());
    node1.getThenExpr().accept(this, node2.getThenExpr());
  }

  @Override
  public void visit(ConstructorDeclaration node1, Node other) {
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
  public void visit(ContinueStmt node1, Node other) {
    ContinueStmt node2 = (ContinueStmt) other;
    defaultAction(node1, node2);
    node1.getLabel().ifPresent(l -> l.accept(this, node2.getLabel().get()));
  }

  @Override
  public void visit(DoStmt node1, Node other) {
    DoStmt node2 = (DoStmt) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    node1.getCondition().accept(this, node2.getCondition());
  }

  @Override
  public void visit(DoubleLiteralExpr node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(EmptyStmt node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(EnclosedExpr node1, Node other) {
    EnclosedExpr node2 = (EnclosedExpr) other;
    defaultAction(node1, node2);
    node1.getInner().accept(this, node2.getInner());
  }

  @Override
  public void visit(EnumConstantDeclaration node1, Node other) {
    EnumConstantDeclaration node2 = (EnumConstantDeclaration) other;
    defaultAction(node1, node2);
    visitLists(node1.getArguments(), node2.getArguments());
    visitLists(node1.getClassBody(), node2.getClassBody());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(EnumDeclaration node1, Node other) {
    EnumDeclaration node2 = (EnumDeclaration) other;
    defaultAction(node1, node2);
    visitLists(node1.getEntries(), node2.getEntries());
    visitLists(node1.getImplementedTypes(), node2.getImplementedTypes());
    visitLists(node1.getMembers(), node2.getMembers());
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(ExplicitConstructorInvocationStmt node1, Node other) {
    ExplicitConstructorInvocationStmt node2 = (ExplicitConstructorInvocationStmt) other;
    defaultAction(node1, node2);
    visitLists(node1.getArguments(), node2.getArguments());
    node1.getExpression().ifPresent(l -> l.accept(this, node2.getExpression().get()));
    node1.getTypeArguments().ifPresent(l -> visitLists(l, node2.getTypeArguments().get()));
  }

  @Override
  public void visit(ExpressionStmt node1, Node other) {
    ExpressionStmt node2 = (ExpressionStmt) other;
    defaultAction(node1, node2);
    node1.getExpression().accept(this, node2.getExpression());
  }

  @Override
  public void visit(FieldAccessExpr node1, Node other) {
    FieldAccessExpr node2 = (FieldAccessExpr) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
    node1.getScope().accept(this, node2.getScope());
    node1.getTypeArguments().ifPresent(l -> visitLists(l, node2.getTypeArguments().get()));
  }

  @Override
  public void visit(FieldDeclaration node1, Node other) {
    FieldDeclaration node2 = (FieldDeclaration) other;
    defaultAction(node1, node2);
    visitLists(node1.getModifiers(), node2.getModifiers());
    visitLists(node1.getVariables(), node2.getVariables());
  }

  @Override
  public void visit(ForEachStmt node1, Node other) {
    ForEachStmt node2 = (ForEachStmt) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    node1.getIterable().accept(this, node2.getIterable());
    node1.getVariable().accept(this, node2.getVariable());
  }

  @Override
  public void visit(ForStmt node1, Node other) {
    ForStmt node2 = (ForStmt) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    node1.getCompare().ifPresent(l -> l.accept(this, node2.getCompare().get()));
    visitLists(node1.getInitialization(), node2.getInitialization());
    visitLists(node1.getUpdate(), node2.getUpdate());
  }

  @Override
  public void visit(IfStmt node1, Node other) {
    IfStmt node2 = (IfStmt) other;
    defaultAction(node1, node2);
    node1.getCondition().accept(this, node2.getCondition());
    node1.getElseStmt().ifPresent(l -> l.accept(this, node2.getElseStmt().get()));
    node1.getThenStmt().accept(this, node2.getThenStmt());
  }

  @Override
  public void visit(InitializerDeclaration node1, Node other) {
    InitializerDeclaration node2 = (InitializerDeclaration) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
  }

  @Override
  public void visit(InstanceOfExpr node1, Node other) {
    InstanceOfExpr node2 = (InstanceOfExpr) other;
    defaultAction(node1, node2);
    node1.getExpression().accept(this, node2.getExpression());
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(IntegerLiteralExpr node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(JavadocComment node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(LabeledStmt node1, Node other) {
    LabeledStmt node2 = (LabeledStmt) other;
    defaultAction(node1, node2);
    node1.getLabel().accept(this, node2.getLabel());
    node1.getStatement().accept(this, node2.getStatement());
  }

  @Override
  public void visit(LineComment node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(LongLiteralExpr node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(MarkerAnnotationExpr node1, Node other) {
    MarkerAnnotationExpr node2 = (MarkerAnnotationExpr) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(MemberValuePair node1, Node other) {
    MemberValuePair node2 = (MemberValuePair) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
    node1.getValue().accept(this, node2.getName());
  }

  @Override
  public void visit(MethodCallExpr node1, Node other) {
    MethodCallExpr node2 = (MethodCallExpr) other;
    defaultAction(node1, node2);
    visitLists(node1.getArguments(), node2.getArguments());
    node1.getName().accept(this, node2.getName());
    node1.getScope().ifPresent(l -> l.accept(this, node2.getScope().get()));
    node1.getTypeArguments().ifPresent(l -> visitLists(l, node2.getTypeArguments().get()));
  }

  @Override
  public void visit(MethodDeclaration node1, Node other) {
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
  public void visit(NameExpr node1, Node other) {
    NameExpr node2 = (NameExpr) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(NormalAnnotationExpr node1, Node other) {
    NormalAnnotationExpr node2 = (NormalAnnotationExpr) other;
    defaultAction(node1, node2);
    visitLists(node1.getPairs(), node2.getPairs());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(NullLiteralExpr node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(ObjectCreationExpr node1, Node other) {
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
  public void visit(PackageDeclaration node1, Node other) {
    PackageDeclaration node2 = (PackageDeclaration) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(Parameter node1, Node other) {
    Parameter node2 = (Parameter) other;
    defaultAction(node1, node2);
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(PrimitiveType node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(Name node1, Node other) {
    Name node2 = (Name) other;
    defaultAction(node1, node2);
    node1.getQualifier().ifPresent(l -> l.accept(this, node2.getQualifier().get()));
  }

  @Override
  public void visit(SimpleName node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(ArrayType node1, Node other) {
    ArrayType node2 = (ArrayType) other;
    defaultAction(node1, node2);
    node1.getComponentType().accept(this, node2.getComponentType());
  }

  @Override
  public void visit(ArrayCreationLevel node1, Node other) {
    ArrayCreationLevel node2 = (ArrayCreationLevel) other;
    defaultAction(node1, node2);
    node1.getDimension().ifPresent(l -> l.accept(this, node2.getDimension().get()));
  }

  @Override
  public void visit(IntersectionType node1, Node other) {
    IntersectionType node2 = (IntersectionType) other;
    defaultAction(node1, node2);
    visitLists(node1.getElements(), node2.getElements());
  }

  @Override
  public void visit(UnionType node1, Node other) {
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
  public void visit(ReturnStmt node1, Node other) {
    ReturnStmt node2 = (ReturnStmt) other;
    defaultAction(node1, node2);
    node1.getExpression().ifPresent(l -> l.accept(this, node2.getExpression().get()));
  }

  @Override
  public void visit(SingleMemberAnnotationExpr node1, Node other) {
    SingleMemberAnnotationExpr node2 = (SingleMemberAnnotationExpr) other;
    defaultAction(node1, node2);
    node1.getMemberValue().accept(this, node2.getMemberValue());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(StringLiteralExpr node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(SuperExpr node1, Node other) {
    SuperExpr node2 = (SuperExpr) other;
    defaultAction(node1, node2);
    node1.getTypeName().ifPresent(l -> l.accept(this, node2.getTypeName().get()));
  }

  @Override
  public void visit(SwitchEntry node1, Node other) {
    SwitchEntry node2 = (SwitchEntry) other;
    defaultAction(node1, node2);
    visitLists(node1.getLabels(), node2.getLabels());
    visitLists(node1.getStatements(), node2.getStatements());
  }

  @Override
  public void visit(SwitchStmt node1, Node other) {
    SwitchStmt node2 = (SwitchStmt) other;
    defaultAction(node1, node2);
    visitLists(node1.getEntries(), node2.getEntries());
    node1.getSelector().accept(this, node2.getSelector());
  }

  @Override
  public void visit(SynchronizedStmt node1, Node other) {
    SynchronizedStmt node2 = (SynchronizedStmt) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    node1.getExpression().accept(this, node2.getExpression());
  }

  @Override
  public void visit(ThisExpr node1, Node other) {
    ThisExpr node2 = (ThisExpr) other;
    defaultAction(node1, node2);
    node1.getTypeName().ifPresent(l -> l.accept(this, node2.getTypeName().get()));
  }

  @Override
  public void visit(ThrowStmt node1, Node other) {
    ThrowStmt node2 = (ThrowStmt) other;
    defaultAction(node1, node2);
    node1.getExpression().accept(this, node2.getExpression());
  }

  @Override
  public void visit(TryStmt node1, Node other) {
    TryStmt node2 = (TryStmt) other;
    defaultAction(node1, node2);
    visitLists(node1.getCatchClauses(), node2.getCatchClauses());
    node1.getFinallyBlock().ifPresent(l -> l.accept(this, node2.getFinallyBlock().get()));
    visitLists(node1.getResources(), node2.getResources());
    node1.getTryBlock().accept(this, node2.getTryBlock());
  }

  @Override
  public void visit(LocalClassDeclarationStmt node1, Node other) {
    LocalClassDeclarationStmt node2 = (LocalClassDeclarationStmt) other;
    defaultAction(node1, node2);
    node1.getClassDeclaration().accept(this, node2.getClassDeclaration());
  }

  @Override
  public void visit(LocalRecordDeclarationStmt node1, Node other) {
    LocalRecordDeclarationStmt node2 = (LocalRecordDeclarationStmt) other;
    defaultAction(node1, node2);
    node1.getRecordDeclaration().accept(this, node2.getRecordDeclaration());
  }

  @Override
  public void visit(TypeParameter node1, Node other) {
    TypeParameter node2 = (TypeParameter) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
    // Since ajava files and its corresponding Java file may differ in whether they contain a
    // type bound, only visit type bounds if they're present in both nodes.
    if (node1.getTypeBound().isEmpty() == node2.getTypeBound().isEmpty()) {
      visitLists(node1.getTypeBound(), node2.getTypeBound());
    }
  }

  @Override
  public void visit(UnaryExpr node1, Node other) {
    UnaryExpr node2 = (UnaryExpr) other;
    defaultAction(node1, node2);
    node1.getExpression().accept(this, node2.getExpression());
  }

  @Override
  public void visit(UnknownType node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(VariableDeclarationExpr node1, Node other) {
    VariableDeclarationExpr node2 = (VariableDeclarationExpr) other;
    defaultAction(node1, node2);
    visitLists(node1.getModifiers(), node2.getModifiers());
    visitLists(node1.getVariables(), node2.getVariables());
  }

  @Override
  public void visit(VariableDeclarator node1, Node other) {
    VariableDeclarator node2 = (VariableDeclarator) other;
    defaultAction(node1, node2);
    node1.getInitializer().ifPresent(l -> l.accept(this, node2.getInitializer().get()));
    node1.getName().accept(this, node2.getName());
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(VoidType node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(WhileStmt node1, Node other) {
    WhileStmt node2 = (WhileStmt) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    node1.getCondition().accept(this, node2.getCondition());
  }

  @Override
  public void visit(WildcardType node1, Node other) {
    WildcardType node2 = (WildcardType) other;
    defaultAction(node1, node2);
    node1.getExtendedType().ifPresent(l -> l.accept(this, node2.getExtendedType().get()));
    node1.getSuperType().ifPresent(l -> l.accept(this, node2.getSuperType().get()));
  }

  @Override
  public void visit(LambdaExpr node1, Node other) {
    LambdaExpr node2 = (LambdaExpr) other;
    defaultAction(node1, node2);
    node1.getBody().accept(this, node2.getBody());
    visitLists(node1.getParameters(), node2.getParameters());
  }

  @Override
  public void visit(MethodReferenceExpr node1, Node other) {
    MethodReferenceExpr node2 = (MethodReferenceExpr) other;
    defaultAction(node1, node2);
    node1.getScope().accept(this, node2.getScope());
    node1.getTypeArguments().ifPresent(l -> visitLists(l, node2.getTypeArguments().get()));
  }

  @Override
  public void visit(TypeExpr node1, Node other) {
    TypeExpr node2 = (TypeExpr) other;
    defaultAction(node1, node2);
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(ImportDeclaration node1, Node other) {
    ImportDeclaration node2 = (ImportDeclaration) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(ModuleDeclaration node1, Node other) {
    ModuleDeclaration node2 = (ModuleDeclaration) other;
    defaultAction(node1, node2);
    visitLists(node1.getDirectives(), node2.getDirectives());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(ModuleRequiresDirective node1, Node other) {
    ModuleRequiresDirective node2 = (ModuleRequiresDirective) other;
    defaultAction(node1, node2);
    visitLists(node1.getModifiers(), node2.getModifiers());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(ModuleExportsDirective node1, Node other) {
    ModuleExportsDirective node2 = (ModuleExportsDirective) other;
    defaultAction(node1, node2);
    visitLists(node1.getModuleNames(), node2.getModuleNames());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(ModuleProvidesDirective node1, Node other) {
    ModuleProvidesDirective node2 = (ModuleProvidesDirective) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
    visitLists(node1.getWith(), node2.getWith());
  }

  @Override
  public void visit(ModuleUsesDirective node1, Node other) {
    ModuleUsesDirective node2 = (ModuleUsesDirective) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(ModuleOpensDirective node1, Node other) {
    ModuleOpensDirective node2 = (ModuleOpensDirective) other;
    defaultAction(node1, node2);
    visitLists(node1.getModuleNames(), node2.getModuleNames());
    node1.getName().accept(this, node2.getName());
  }

  @Override
  public void visit(UnparsableStmt node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(ReceiverParameter node1, Node other) {
    ReceiverParameter node2 = (ReceiverParameter) other;
    defaultAction(node1, node2);
    node1.getName().accept(this, node2.getName());
    node1.getType().accept(this, node2.getType());
  }

  @Override
  public void visit(VarType node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(Modifier node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(SwitchExpr node1, Node other) {
    SwitchExpr node2 = (SwitchExpr) other;
    defaultAction(node1, node2);
    visitLists(node1.getEntries(), node2.getEntries());
    node1.getSelector().accept(this, node2.getSelector());
  }

  @Override
  public void visit(TextBlockLiteralExpr node1, Node other) {
    defaultAction(node1, other);
  }

  @Override
  public void visit(YieldStmt node1, Node other) {
    YieldStmt node2 = (YieldStmt) other;
    defaultAction(node1, node2);
    node1.getExpression().accept(this, node2.getExpression());
  }
}
