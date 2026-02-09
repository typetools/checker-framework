package org.checkerframework.framework.ajava;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BugInCF;

/**
 * A visitor that traverses two javac ASTs simultaneously. The two trees must be structurally
 * identical (modulo differences, such as annotations and explicit receiver parameters, between a
 * Java file and its corresponding {@code .ajava} file). For example, modifiers must be in the same
 * order in the two files.
 *
 * <p>The entry point is {@link #scan(Tree, Tree)}, which eventually calls a {@code visitXyz}
 * method. The {@code visitXyz} methods in this base class call {@link #defaultAction}, then drive
 * paired recursion explicitly by calling {@link #scan(Tree, Tree)} and {@link #scanList(List,
 * List)} on corresponding child trees.
 *
 * <p>To use this class, extend it and override {@link #defaultAction(Tree, Tree)} to perform work
 * for each matched pair of trees. Subclasses may also override specific {@code visitXyz} methods to
 * customize behavior, but do not need to override root methods such as {@code visitCompilationUnit}
 * or {@code visitClass} unless they want to change traversal.
 *
 * <p><b>WARNING:</b> This class intentionally does <em>not</em> behave like {@link
 * com.sun.source.util.TreeScanner}. Although it subclasses {@link SimpleTreeVisitor}, recursion is
 * <em>not</em> automatic. To recurse, the {@code visitXyz} methods in this class and any subclass
 * must explicitly call {@link #scan} or {@link #scanList}. This makes recursion explicit and keeps
 * all paired-tree structural checks in {@link #scan(Tree, Tree)} rather than at every call site.
 *
 * <p>This base visitor does not compare or traverse annotation lists, since annotations may
 * legitimately differ between a Java file and its corresponding {@code .ajava} file.
 */
public abstract class DoubleJavacVisitor extends SimpleTreeVisitor<Void, Tree> {

  /** Create a DoubleJavacVisitor. */
  protected DoubleJavacVisitor() {}

  /**
   * Default action performed on each pair of nodes from matching ASTs.
   *
   * <p>This method is called by each {@code visitXyz} method in this class before scanning child
   * trees. It does not itself recurse; recursion is driven by the {@code visitXyz} methods.
   *
   * @param tree1 the first tree in the matched pair
   * @param tree2 the second tree in the matched pair
   * @return null
   */
  @Override
  protected abstract Void defaultAction(Tree tree1, Tree tree2);

  //
  // Scan methods
  //

  /**
   * Traverses two corresponding trees together by dispatching to the appropriate {@code visitXyz}
   * method.
   *
   * <p>Both trees must be null, or both trees must have the same {@link Tree.Kind}.
   *
   * @param tree1 the first tree to scan, or null
   * @param tree2 the second tree to scan, or null
   */
  public final void scan(@Nullable Tree tree1, @Nullable Tree tree2) {
    if (tree1 == null && tree2 == null) {
      return;
    }

    if (tree1 == null || tree2 == null) {
      throw new BugInCF(
          String.format(
              "%s.scan: one tree is null: tree1=%s [%s] tree2=%s [%s]",
              this.getClass().getCanonicalName(), tree1, kind1, tree2, kind2));
    }

    Tree.Kind kind1 = tree1 == null ? null : tree1.getKind();
    Tree.Kind kind2 = tree2 == null ? null : tree2.getKind();

    if (tree1.getKind() != tree2.getKind()) {
      throw new BugInCF(
          String.format(
              "%s.scan: mismatched kinds: tree1=%s [%s] tree2=%s [%s]",
              this.getClass().getCanonicalName(), tree1, kind1, tree2, kind2));
    }

    // `accept` will call the appropriate `visitXyz` method.
    tree1.accept(this, tree2);
  }

  /**
   * Traverses two corresponding expression trees together.
   *
   * <p>This method exists to document intent at call sites where the children being traversed are
   * known to be expressions. It performs no additional checks beyond those in {@link #scan} and
   * simply delegates to that method.
   *
   * @param expr1 the first expression tree, or null
   * @param expr2 the second expression tree, or null
   */
  public final void scanExpr(@Nullable ExpressionTree expr1, @Nullable ExpressionTree expr2) {
    scan(expr1, expr2);
  }

  /**
   * Traverses two lists of trees in lockstep by scanning corresponding elements. For each pair of
   * corresponding elements index, this method invokes {@link #scan(Tree, Tree)}.
   *
   * <p>The two list arguments must either both be null or both be non-null and have the same
   * length. Corresponding elements of {@code list1} and {@code list2} must have the same AST
   * structure.
   *
   * @param list1 the first list of trees, or null
   * @param list2 the second list of trees, or null
   */
  public final void scanList(
      @Nullable List<? extends Tree> list1, @Nullable List<? extends Tree> list2) {
    if (list1 == null && list2 == null) {
      return;
    }
    if (list1 == null || list2 == null) {
      throw new BugInCF(
          String.format(
              "%s.scanList: one list is null: list1=%s list2=%s",
              this.getClass().getCanonicalName(), list1, list2));
    }
    if (list1.size() != list2.size()) {
      throw new BugInCF(
          String.format(
              "%s.scanList(%s [size %d], %s [size %d])",
              this.getClass().getCanonicalName(), list1, list1.size(), list2, list2.size()));
    }
    for (int i = 0; i < list1.size(); i++) {
      scan(list1.get(i), list2.get(i));
    }
  }

  //
  // Visitor methods
  //

  /**
   * Visits a compilation unit (which represents a Java file) and scans its module, package,
   * imports, and top-level type declarations.
   *
   * @param ctree1 compilation unit tree from the first AST
   * @param tree2 compilation unit tree from the second AST
   * @return null
   */
  @Override
  public Void visitCompilationUnit(CompilationUnitTree ctree1, Tree tree2) {
    CompilationUnitTree ctree2 = (CompilationUnitTree) tree2;
    defaultAction(ctree1, ctree2);

    scan(ctree1.getModule(), ctree2.getModule());
    scan(ctree1.getPackage(), ctree2.getPackage());

    scanList(ctree1.getImports(), ctree2.getImports());
    scanList(ctree1.getTypeDecls(), ctree2.getTypeDecls());
    return null;
  }

  /**
   * Visits a package declaration and scans its name.
   *
   * @param ptree1 package tree from the first AST
   * @param tree2 package tree from the second AST
   * @return null
   */
  @Override
  public Void visitPackage(PackageTree ptree1, Tree tree2) {
    PackageTree ptree2 = (PackageTree) tree2;
    defaultAction(ptree1, ptree2);

    scan(ptree1.getPackageName(), ptree2.getPackageName());
    return null;
  }

  /**
   * Visits an import declaration and scans its qualified identifier.
   *
   * @param itree1 import tree from the first AST
   * @param tree2 import tree from the second AST
   * @return null
   */
  @Override
  public Void visitImport(ImportTree itree1, Tree tree2) {
    ImportTree itree2 = (ImportTree) tree2;
    defaultAction(itree1, itree2);

    scan(itree1.getQualifiedIdentifier(), itree2.getQualifiedIdentifier());
    return null;
  }

  /**
   * Visits a class-like declaration and scans its modifiers, type parameters, superclass,
   * interfaces, permits clause, and members.
   *
   * @param ctree1 class tree from the first AST
   * @param tree2 class tree from the second AST
   * @return null
   */
  @Override
  public Void visitClass(ClassTree ctree1, Tree tree2) {
    ClassTree ctree2 = (ClassTree) tree2;
    defaultAction(ctree1, ctree2);

    scan(ctree1.getModifiers(), ctree2.getModifiers());
    scanList(ctree1.getTypeParameters(), ctree2.getTypeParameters());
    scan(ctree1.getExtendsClause(), ctree2.getExtendsClause());
    scanList(ctree1.getImplementsClause(), ctree2.getImplementsClause());
    scanList(ctree1.getPermitsClause(), ctree2.getPermitsClause());

    scanList(ctree1.getMembers(), ctree2.getMembers());
    return null;
  }

  /**
   * Visits a method or constructor declaration and scans modifiers, type parameters, return type,
   * receiver parameter, formal parameters, throws clause, and body.
   *
   * @param mtree1 method tree from the first AST
   * @param tree2 method tree from the second AST
   * @return null
   */
  @Override
  public Void visitMethod(MethodTree mtree1, Tree tree2) {
    MethodTree mtree2 = (MethodTree) tree2;
    defaultAction(mtree1, mtree2);

    scan(mtree1.getModifiers(), mtree2.getModifiers());
    scanList(mtree1.getTypeParameters(), mtree2.getTypeParameters());

    scan(mtree1.getReturnType(), mtree2.getReturnType());
    // Receiver parameters may be absent in .java and present in .ajava.
    if (mtree1.getReceiverParameter() != null && mtree2.getReceiverParameter() != null) {
      scan(mtree1.getReceiverParameter(), mtree2.getReceiverParameter());
    }

    scanList(mtree1.getParameters(), mtree2.getParameters());
    scanList(mtree1.getThrows(), mtree2.getThrows());

    scan(mtree1.getDefaultValue(), mtree2.getDefaultValue());
    scan(mtree1.getBody(), mtree2.getBody());
    return null;
  }

  /**
   * Visits a variable declaration (field, local, parameter) and scans its modifiers, type,
   * initializer.
   *
   * @param vtree1 variable tree from the first AST
   * @param tree2 variable tree from the second AST
   * @return null
   */
  @Override
  public Void visitVariable(VariableTree vtree1, Tree tree2) {
    VariableTree vtree2 = (VariableTree) tree2;
    defaultAction(vtree1, vtree2);

    scan(vtree1.getModifiers(), vtree2.getModifiers());
    scan(vtree1.getType(), vtree2.getType());
    scan(vtree1.getInitializer(), vtree2.getInitializer());
    return null;
  }

  /**
   * Visits a modifiers node.
   *
   * @param mtree1 modifiers tree from the first AST
   * @param tree2 modifiers tree from the second AST
   * @return null
   */
  @Override
  public Void visitModifiers(ModifiersTree mtree1, Tree tree2) {
    ModifiersTree mtree2 = (ModifiersTree) tree2;
    defaultAction(mtree1, mtree2);
    return null;
  }

  /**
   * Visits an annotated type and scans both its type-use annotations and its underlying type.
   *
   * <p>Type-use annotations may legitimately differ between a Java file and its corresponding
   * {@code .ajava} file, so this visitor does not compare or traverse the annotation list.
   *
   * @param atree1 annotated type tree from the first AST
   * @param tree2 annotated type tree from the second AST
   * @return null
   */
  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree atree1, Tree tree2) {
    AnnotatedTypeTree atree2 = (AnnotatedTypeTree) tree2;
    defaultAction(atree1, atree2);

    scan(atree1.getUnderlyingType(), atree2.getUnderlyingType());
    return null;
  }

  /**
   * Visits a parameterized type and scans the base type and type arguments.
   *
   * @param ptree1 parameterized type tree from the first AST
   * @param tree2 parameterized type tree from the second AST
   * @return null
   */
  @Override
  public Void visitParameterizedType(ParameterizedTypeTree ptree1, Tree tree2) {
    ParameterizedTypeTree ptree2 = (ParameterizedTypeTree) tree2;
    defaultAction(ptree1, ptree2);

    scan(ptree1.getType(), ptree2.getType());
    scanList(ptree1.getTypeArguments(), ptree2.getTypeArguments());
    return null;
  }

  /**
   * Visits an array type and scans the component type.
   *
   * @param atree1 array type tree from the first AST
   * @param tree2 array type tree from the second AST
   * @return null
   */
  @Override
  public Void visitArrayType(ArrayTypeTree atree1, Tree tree2) {
    ArrayTypeTree atree2 = (ArrayTypeTree) tree2;
    defaultAction(atree1, atree2);

    scan(atree1.getType(), atree2.getType());
    return null;
  }

  /**
   * Visits a primitive type.
   *
   * @param ptree1 primitive type tree from the first AST
   * @param tree2 primitive type tree from the second AST
   * @return null
   */
  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree ptree1, Tree tree2) {
    PrimitiveTypeTree ptree2 = (PrimitiveTypeTree) tree2;
    defaultAction(ptree1, ptree2);
    return null;
  }

  /**
   * Visits a type parameter and scans its bounds.
   *
   * <p>Annotations on the type parameter may legitimately differ between a Java file and its
   * corresponding {@code .ajava} file, so this visitor does not compare or traverse the annotation
   * list.
   *
   * @param ttree1 type parameter tree from the first AST
   * @param tree2 type parameter tree from the second AST
   * @return null
   */
  @Override
  public Void visitTypeParameter(TypeParameterTree ttree1, Tree tree2) {
    TypeParameterTree ttree2 = (TypeParameterTree) tree2;
    defaultAction(ttree1, ttree2);

    scanList(ttree1.getBounds(), ttree2.getBounds());
    return null;
  }

  /**
   * Visits a wildcard type and scans its bound (extends or super), if present.
   *
   * <p>This method does not check whether the wildcard uses {@code extends} or {@code super}; only
   * the bound tree is compared.
   *
   * @param wtree1 wildcard tree from the first AST
   * @param tree2 wildcard tree from the second AST
   * @return null
   */
  @Override
  public Void visitWildcard(WildcardTree wtree1, Tree tree2) {
    WildcardTree wtree2 = (WildcardTree) tree2;
    defaultAction(wtree1, wtree2);

    scan(wtree1.getBound(), wtree2.getBound());
    return null;
  }

  /**
   * Visits an identifier.
   *
   * @param itree1 identifier tree from the first AST
   * @param tree2 identifier tree from the second AST
   * @return null
   */
  @Override
  public Void visitIdentifier(IdentifierTree itree1, Tree tree2) {
    IdentifierTree itree2 = (IdentifierTree) tree2;
    defaultAction(itree1, itree2);
    return null;
  }

  /**
   * Visits a member select expression and scans the receiver expression.
   *
   * @param mtree1 member select tree from the first AST
   * @param tree2 member select tree from the second AST
   * @return null
   */
  @Override
  public Void visitMemberSelect(MemberSelectTree mtree1, Tree tree2) {
    MemberSelectTree mtree2 = (MemberSelectTree) tree2;
    defaultAction(mtree1, mtree2);

    scanExpr(mtree1.getExpression(), mtree2.getExpression());
    return null;
  }

  /**
   * Visits a block and scans its statements.
   *
   * @param btree1 block tree from the first AST
   * @param tree2 block tree from the second AST
   * @return null
   */
  @Override
  public Void visitBlock(BlockTree btree1, Tree tree2) {
    BlockTree btree2 = (BlockTree) tree2;
    defaultAction(btree1, btree2);

    scanList(btree1.getStatements(), btree2.getStatements());
    return null;
  }

  /**
   * Visits an expression statement and scans its expression.
   *
   * @param etree1 expression statement from the first AST
   * @param tree2 expression statement from the second AST
   * @return null
   */
  @Override
  public Void visitExpressionStatement(ExpressionStatementTree etree1, Tree tree2) {
    ExpressionStatementTree etree2 = (ExpressionStatementTree) tree2;
    defaultAction(etree1, etree2);

    scanExpr(etree1.getExpression(), etree2.getExpression());
    return null;
  }

  /**
   * Visits a return statement and scans its expression, if present.
   *
   * @param rtree1 return tree from the first AST
   * @param tree2 return tree from the second AST
   * @return null
   */
  @Override
  public Void visitReturn(ReturnTree rtree1, Tree tree2) {
    ReturnTree rtree2 = (ReturnTree) tree2;
    defaultAction(rtree1, rtree2);

    scan(rtree1.getExpression(), rtree2.getExpression());
    return null;
  }

  /**
   * Visits a throw statement and scans its expression.
   *
   * @param ttree1 throw tree from the first AST
   * @param tree2 throw tree from the second AST
   * @return null
   */
  @Override
  public Void visitThrow(ThrowTree ttree1, Tree tree2) {
    ThrowTree ttree2 = (ThrowTree) tree2;
    defaultAction(ttree1, ttree2);

    scanExpr(ttree1.getExpression(), ttree2.getExpression());
    return null;
  }

  /**
   * Visits a try statement and scans resources, blocks, and catch/finally structures.
   *
   * @param ttree1 try tree from the first AST
   * @param tree2 try tree from the second AST
   * @return null
   */
  @Override
  public Void visitTry(TryTree ttree1, Tree tree2) {
    TryTree ttree2 = (TryTree) tree2;
    defaultAction(ttree1, ttree2);

    scanList(ttree1.getResources(), ttree2.getResources());
    scan(ttree1.getBlock(), ttree2.getBlock());
    scanList(ttree1.getCatches(), ttree2.getCatches());
    scan(ttree1.getFinallyBlock(), ttree2.getFinallyBlock());
    return null;
  }
}
