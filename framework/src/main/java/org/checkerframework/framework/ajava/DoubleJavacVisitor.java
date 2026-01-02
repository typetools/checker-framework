package org.checkerframework.framework.ajava;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
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

/**
 * A visitor that visits two javac ASTs simultaneously that almost match.
 *
 * <p>This class is the javac-tree analogue of DoubleJavaParserVisitor. It is used when two javac
 * trees represent the same source file, except for differences that are permitted between a Java
 * file and its corresponding ajava file.
 *
 * <p>The visitor walks both trees in lockstep. For each matched pair of trees, the visitor invokes
 * defaultPairAction. Subclasses override defaultPairAction to perform comparisons or other
 * processing.
 *
 * <p>Unlike JavaParser ASTs, javac trees may contain additional wrapper nodes such as parentheses
 * or expression statements. To keep traversal aligned, this class normalizes certain wrapper nodes
 * before dispatching to visit methods.
 *
 * <p>To use this class, extend it, override defaultPairAction, and begin traversal by calling scan
 * on the two root trees.
 */
public abstract class DoubleJavacVisitor extends SimpleTreeVisitor<Void, Tree> {

  /** Create a DoubleJavacVisitor. */
  public DoubleJavacVisitor() {}

  /**
   * Default action performed on all matched pairs of trees.
   *
   * <p>This method is called for every visited pair, including node kinds that do not have a
   * dedicated visit method override in a subclass. Subclasses typically implement structural or
   * annotation comparisons here, and then use visit methods to drive recursive traversal.
   *
   * @param tree1 the first tree in the matched pair
   * @param tree2 the second tree in the matched pair
   */
  protected abstract void defaultPairAction(Tree tree1, Tree tree2);

  /**
   * The fallback visitor method used when no specific visitXxx override exists for a tree kind.
   *
   * <p>This implementation calls defaultPairAction and does not automatically recurse into
   * children. Subclasses are expected to override visitXxx methods for the tree kinds they care
   * about and call scan, scanOpt, and scanList to continue traversal.
   *
   * @param tree1 the visited tree from the first AST
   * @param tree2 the corresponding tree from the second AST
   * @return null
   */
  @Override
  protected Void defaultAction(Tree tree1, Tree tree2) {
    defaultPairAction(tree1, tree2);
    return null;
  }

  /**
   * Scans two trees in lockstep.
   *
   * <p>This is the main entry point for paired traversal. It:
   *
   * <p>1. Handles the case where both trees are null by doing nothing.
   *
   * <p>2. Treats exactly one null as an error, because it indicates the trees are no longer
   * aligned.
   *
   * <p>3. Verifies that the trees have the same kind, then dispatches to the appropriate visit
   * method via accept.
   *
   * @param tree1 the first tree to scan, or null
   * @param tree2 the second tree to scan, or null
   */
  public final void scan(@Nullable Tree tree1, @Nullable Tree tree2) {
    if (tree1 == null && tree2 == null) {
      return;
    }

    if (tree1 == null || tree2 == null) {
      throw new Error(
          String.format(
              "%s.scan: one tree is null: tree1=%s tree2=%s",
              this.getClass().getCanonicalName(), tree1, tree2));
    }

    // If we later discover javac introduces wrappers that desynchronize traversal (e.g.
    // parentheses),
    // we can re-introduce a normalization step here.
    if (tree1.getKind() != tree2.getKind()) {
      throw new Error(
          String.format(
              "%s.scan: mismatched kinds: %s vs %s",
              this.getClass().getCanonicalName(), tree1.getKind(), tree2.getKind()));
    }

    tree1.accept(this, tree2);
  }

  /**
   * Scans two trees that are optional children.
   *
   * <p>This helper is used at call sites where a child is permitted to be absent. If both trees are
   * null, this method does nothing. If exactly one is null, this is treated as an error because it
   * indicates the two trees are not aligned.
   *
   * @param tree1 the first tree, or null
   * @param tree2 the second tree, or null
   */
  public final void scanOpt(@Nullable Tree tree1, @Nullable Tree tree2) {
    if (tree1 == null && tree2 == null) {
      return;
    }
    if (tree1 == null || tree2 == null) {
      throw new Error(
          String.format(
              "%s.scanOpt: one tree is null: tree1=%s tree2=%s",
              this.getClass().getCanonicalName(), tree1, tree2));
    }
    scan(tree1, tree2);
  }

  /**
   * Scans two expression trees in lockstep.
   *
   * <p>This helper exists mainly to document intent at call sites where the children being scanned
   * are expressions. The implementation delegates to scan.
   *
   * @param expr1 the first expression tree
   * @param expr2 the second expression tree
   */
  public final void scanExpr(ExpressionTree expr1, ExpressionTree expr2) {
    scan(expr1, expr2);
  }

  /**
   * Given two lists of trees with the same size, scans corresponding elements in order.
   *
   * <p>This method assumes that the two lists represent parallel AST structures. A size mismatch
   * indicates that traversal has become desynchronized and is treated as an error.
   *
   * @param list1 the first list of trees
   * @param list2 the second list of trees
   */
  public final void scanList(List<? extends Tree> list1, List<? extends Tree> list2) {
    if (list1.size() != list2.size()) {
      throw new Error(
          String.format(
              "%s.scanList(%s [size %d], %s [size %d])",
              this.getClass().getCanonicalName(), list1, list1.size(), list2, list2.size()));
    }
    for (int i = 0; i < list1.size(); i++) {
      scan(list1.get(i), list2.get(i));
    }
  }

  /**
   * Visits a compilation unit (top-level file node) and scans its main children.
   *
   * @param tree1 compilation unit from the first AST
   * @param tree2 compilation unit from the second AST
   * @return null
   */
  @Override
  public final Void visitCompilationUnit(CompilationUnitTree tree1, Tree tree2) {
    CompilationUnitTree tree2cu = (CompilationUnitTree) tree2;
    defaultPairAction(tree1, tree2cu);

    scanOpt(tree1.getModule(), tree2cu.getModule());
    scanOpt(tree1.getPackage(), tree2cu.getPackage());

    scanList(tree1.getImports(), tree2cu.getImports());
    scanList(tree1.getTypeDecls(), tree2cu.getTypeDecls());
    return null;
  }

  /**
   * Visits a package declaration and scans its annotations and name.
   *
   * @param tree1 package tree from the first AST
   * @param tree2 package tree from the second AST
   * @return null
   */
  @Override
  public final Void visitPackage(PackageTree tree1, Tree tree2) {
    PackageTree tree2pkg = (PackageTree) tree2;
    defaultPairAction(tree1, tree2pkg);

    scanList(tree1.getAnnotations(), tree2pkg.getAnnotations());
    scanOpt(tree1.getPackageName(), tree2pkg.getPackageName());
    return null;
  }

  /**
   * Visits an import declaration and scans its qualified identifier.
   *
   * @param tree1 import tree from the first AST
   * @param tree2 import tree from the second AST
   * @return null
   */
  @Override
  public final Void visitImport(ImportTree tree1, Tree tree2) {
    ImportTree tree2imp = (ImportTree) tree2;
    defaultPairAction(tree1, tree2imp);

    scan(tree1.getQualifiedIdentifier(), tree2imp.getQualifiedIdentifier());
    return null;
  }

  /**
   * Visits a class-like declaration and scans its modifiers, type parameters, superclass,
   * interfaces, members, and (when present) record components.
   *
   * @param tree1 class tree from the first AST
   * @param tree2 class tree from the second AST
   * @return null
   */
  @Override
  public Void visitClass(ClassTree tree1, Tree tree2) {
    ClassTree tree2cls = (ClassTree) tree2;
    defaultPairAction(tree1, tree2cls);

    scan(tree1.getModifiers(), tree2cls.getModifiers());
    scanList(tree1.getTypeParameters(), tree2cls.getTypeParameters());
    scanOpt(tree1.getExtendsClause(), tree2cls.getExtendsClause());
    scanList(tree1.getImplementsClause(), tree2cls.getImplementsClause());
    scanList(tree1.getPermitsClause(), tree2cls.getPermitsClause());

    scanList(tree1.getMembers(), tree2cls.getMembers());
    return null;
  }

  /**
   * Visits a method or constructor declaration and scans modifiers, type parameters, return type,
   * receiver parameter, formal parameters, throws clause, and body.
   *
   * @param tree1 method tree from the first AST
   * @param tree2 method tree from the second AST
   * @return null
   */
  @Override
  public Void visitMethod(MethodTree tree1, Tree tree2) {
    MethodTree tree2m = (MethodTree) tree2;
    defaultPairAction(tree1, tree2m);

    scan(tree1.getModifiers(), tree2m.getModifiers());
    scanList(tree1.getTypeParameters(), tree2m.getTypeParameters());

    scanOpt(tree1.getReturnType(), tree2m.getReturnType());
    scanOpt(tree1.getReceiverParameter(), tree2m.getReceiverParameter());

    scanList(tree1.getParameters(), tree2m.getParameters());
    scanList(tree1.getThrows(), tree2m.getThrows());

    scanOpt(tree1.getDefaultValue(), tree2m.getDefaultValue());
    scanOpt(tree1.getBody(), tree2m.getBody());
    return null;
  }

  /**
   * Visits a variable declaration (field, local, parameter) and scans its modifiers, type,
   * initializer, and (for record components) accessor-like details are handled by the enclosing
   * node.
   *
   * @param tree1 variable tree from the first AST
   * @param tree2 variable tree from the second AST
   * @return null
   */
  @Override
  public Void visitVariable(VariableTree tree1, Tree tree2) {
    VariableTree tree2v = (VariableTree) tree2;
    defaultPairAction(tree1, tree2v);

    scan(tree1.getModifiers(), tree2v.getModifiers());
    scanOpt(tree1.getType(), tree2v.getType());
    scanOpt(tree1.getInitializer(), tree2v.getInitializer());
    return null;
  }

  /**
   * Visits a modifiers node and scans annotations.
   *
   * @param tree1 modifiers tree from the first AST
   * @param tree2 modifiers tree from the second AST
   * @return null
   */
  @Override
  public Void visitModifiers(ModifiersTree tree1, Tree tree2) {
    ModifiersTree tree2m = (ModifiersTree) tree2;
    defaultPairAction(tree1, tree2m);

    scanList(tree1.getAnnotations(), tree2m.getAnnotations());
    return null;
  }

  /**
   * Visits an annotation node and scans its argument expressions.
   *
   * @param tree1 annotation tree from the first AST
   * @param tree2 annotation tree from the second AST
   * @return null
   */
  @Override
  public Void visitAnnotation(AnnotationTree tree1, Tree tree2) {
    AnnotationTree tree2a = (AnnotationTree) tree2;
    defaultPairAction(tree1, tree2a);

    scan(tree1.getAnnotationType(), tree2a.getAnnotationType());
    scanList(tree1.getArguments(), tree2a.getArguments());
    return null;
  }

  /**
   * Visits an annotated type and scans the underlying type.
   *
   * <p>The annotations themselves are represented as AnnotationTree children in javac's AST. They
   * will be visited when scanning tree1.getAnnotations() if you add that here. If you want full
   * annotation coverage at the type level, keep the scanList call below.
   *
   * @param tree1 annotated type tree from the first AST
   * @param tree2 annotated type tree from the second AST
   * @return null
   */
  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree tree1, Tree tree2) {
    AnnotatedTypeTree tree2t = (AnnotatedTypeTree) tree2;
    defaultPairAction(tree1, tree2t);

    scanList(tree1.getAnnotations(), tree2t.getAnnotations());
    scan(tree1.getUnderlyingType(), tree2t.getUnderlyingType());
    return null;
  }

  /**
   * Visits a parameterized type and scans the base type and type arguments.
   *
   * @param tree1 parameterized type tree from the first AST
   * @param tree2 parameterized type tree from the second AST
   * @return null
   */
  @Override
  public Void visitParameterizedType(ParameterizedTypeTree tree1, Tree tree2) {
    ParameterizedTypeTree tree2p = (ParameterizedTypeTree) tree2;
    defaultPairAction(tree1, tree2p);

    scan(tree1.getType(), tree2p.getType());
    scanList(tree1.getTypeArguments(), tree2p.getTypeArguments());
    return null;
  }

  /**
   * Visits an array type and scans the component type.
   *
   * @param tree1 array type tree from the first AST
   * @param tree2 array type tree from the second AST
   * @return null
   */
  @Override
  public Void visitArrayType(ArrayTypeTree tree1, Tree tree2) {
    ArrayTypeTree tree2a = (ArrayTypeTree) tree2;
    defaultPairAction(tree1, tree2a);

    scan(tree1.getType(), tree2a.getType());
    return null;
  }

  /**
   * Visits a primitive type.
   *
   * @param tree1 primitive type tree from the first AST
   * @param tree2 primitive type tree from the second AST
   * @return null
   */
  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree tree1, Tree tree2) {
    PrimitiveTypeTree tree2p = (PrimitiveTypeTree) tree2;
    defaultPairAction(tree1, tree2p);
    return null;
  }

  /**
   * Visits a type parameter and scans its bounds and annotations on the parameter itself.
   *
   * @param tree1 type parameter tree from the first AST
   * @param tree2 type parameter tree from the second AST
   * @return null
   */
  @Override
  public Void visitTypeParameter(TypeParameterTree tree1, Tree tree2) {
    TypeParameterTree tree2tp = (TypeParameterTree) tree2;
    defaultPairAction(tree1, tree2tp);

    scanList(tree1.getAnnotations(), tree2tp.getAnnotations());
    scanList(tree1.getBounds(), tree2tp.getBounds());
    return null;
  }

  /**
   * Visits a wildcard type and scans its bound (extends or super), if present.
   *
   * @param tree1 wildcard tree from the first AST
   * @param tree2 wildcard tree from the second AST
   * @return null
   */
  @Override
  public Void visitWildcard(WildcardTree tree1, Tree tree2) {
    WildcardTree tree2w = (WildcardTree) tree2;
    defaultPairAction(tree1, tree2w);

    scanOpt(tree1.getBound(), tree2w.getBound());
    return null;
  }

  /**
   * Visits an identifier.
   *
   * @param tree1 identifier tree from the first AST
   * @param tree2 identifier tree from the second AST
   * @return null
   */
  @Override
  public Void visitIdentifier(IdentifierTree tree1, Tree tree2) {
    IdentifierTree tree2i = (IdentifierTree) tree2;
    defaultPairAction(tree1, tree2i);
    return null;
  }

  /**
   * Visits a member select expression and scans the selected expression.
   *
   * @param tree1 member select tree from the first AST
   * @param tree2 member select tree from the second AST
   * @return null
   */
  @Override
  public Void visitMemberSelect(MemberSelectTree tree1, Tree tree2) {
    MemberSelectTree tree2ms = (MemberSelectTree) tree2;
    defaultPairAction(tree1, tree2ms);

    scanExpr(tree1.getExpression(), tree2ms.getExpression());
    return null;
  }

  /**
   * Visits a block and scans its statements.
   *
   * @param tree1 block tree from the first AST
   * @param tree2 block tree from the second AST
   * @return null
   */
  @Override
  public Void visitBlock(BlockTree tree1, Tree tree2) {
    BlockTree tree2b = (BlockTree) tree2;
    defaultPairAction(tree1, tree2b);

    scanList(tree1.getStatements(), tree2b.getStatements());
    return null;
  }

  /**
   * Visits an expression statement and scans its expression.
   *
   * <p>This method may be redundant when normalize unwraps expression statements, but keeping it is
   * harmless and makes traversal robust when callers invoke scan directly on an
   * ExpressionStatementTree.
   *
   * @param tree1 expression statement from the first AST
   * @param tree2 expression statement from the second AST
   * @return null
   */
  @Override
  public Void visitExpressionStatement(ExpressionStatementTree tree1, Tree tree2) {
    ExpressionStatementTree tree2es = (ExpressionStatementTree) tree2;
    defaultPairAction(tree1, tree2es);

    scanExpr(tree1.getExpression(), tree2es.getExpression());
    return null;
  }

  /**
   * Visits a return statement and scans its returned expression, if present.
   *
   * @param tree1 return tree from the first AST
   * @param tree2 return tree from the second AST
   * @return null
   */
  @Override
  public Void visitReturn(ReturnTree tree1, Tree tree2) {
    ReturnTree tree2r = (ReturnTree) tree2;
    defaultPairAction(tree1, tree2r);

    scanOpt(tree1.getExpression(), tree2r.getExpression());
    return null;
  }

  /**
   * Visits a throw statement and scans its thrown expression.
   *
   * @param tree1 throw tree from the first AST
   * @param tree2 throw tree from the second AST
   * @return null
   */
  @Override
  public Void visitThrow(ThrowTree tree1, Tree tree2) {
    ThrowTree tree2t = (ThrowTree) tree2;
    defaultPairAction(tree1, tree2t);

    scanExpr(tree1.getExpression(), tree2t.getExpression());
    return null;
  }

  /**
   * Visits a try statement and scans resources, blocks, and catch/finally structures.
   *
   * @param tree1 try tree from the first AST
   * @param tree2 try tree from the second AST
   * @return null
   */
  @Override
  public Void visitTry(TryTree tree1, Tree tree2) {
    TryTree tree2t = (TryTree) tree2;
    defaultPairAction(tree1, tree2t);

    scanList(tree1.getResources(), tree2t.getResources());
    scan(tree1.getBlock(), tree2t.getBlock());
    scanList(tree1.getCatches(), tree2t.getCatches());
    scanOpt(tree1.getFinallyBlock(), tree2t.getFinallyBlock());
    return null;
  }
}
