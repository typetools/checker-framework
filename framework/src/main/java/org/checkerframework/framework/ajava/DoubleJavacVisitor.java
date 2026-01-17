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
import org.checkerframework.javacutil.BugInCF;

/**
 * A visitor that traverses two javac ASTs simultaneously. The two trees are expected to be
 * structurally identical (modulo differences such as annotations between a Java file and its
 * corresponding {@code .ajava} file).
 *
 * <p>In some use cases (for example, comparing a Java file and its corresponding {@code .ajava}
 * file), annotations may legitimately differ between the two ASTs. This visitor does not attempt to
 * infer equivalence in the presence of such differences. Instead, it enforces structural alignment
 * during traversal, and annotation differences are tolerated only at call sites that use {@link
 * #scanAnnotations(List, List)}; all other lists are compared strictly via {@link #scanList}.
 *
 * <p>The main entry point is {@link #scan}. Given two corresponding trees, {@code scan} checks
 * basic structural invariants and then invokes {@link Tree#accept}, which dispatches to the
 * appropriate {@code visitXyz} method based on the runtime kind of the first tree.
 *
 * <p>To use this class, extend it and override {@link #defaultAction(Tree, Tree)}. Subclasses may
 * also override {@code visitXyz} methods for the tree kinds they care about. Each {@code visitXyz}
 * method is responsible for explicitly continuing traversal by calling {@link #scan}, {@link
 * #scanList}, or (when annotation mismatches are permitted) {@link #scanAnnotations} on
 * corresponding child trees.
 *
 * <p><b>WARNING:</b> This class intentionally does <em>not</em> behave like {@link
 * com.sun.source.util.TreeScanner}. Although it subclasses {@link SimpleTreeVisitor}, recursion is
 * <em>not</em> automatic. The {@code visitXyz} methods in this class recurse only when they
 * explicitly call {@link #scan}, {@link #scanList}, or {@link #scanAnnotations}. This design is
 * necessary to ensure that traversal of the two trees remains synchronized and that mismatches are
 * detected immediately.
 *
 * <p><b>Structural limitations:</b> This visitor enforces that the two trees have the same overall
 * shape and corresponding child structure, but it does not guarantee that they represent identical
 * source code in all respects. For example, when visiting wildcard types, this visitor compares
 * only the bound (if present) and does not check whether the wildcard is {@code extends} or {@code
 * super}. It also does not reason about semantic equivalence of expressions, and it treats the
 * order of structurally significant lists (such as members or imports) as significant. Such
 * differences are currently permitted and must be handled by subclasses if stricter equivalence is
 * required.
 *
 * <p>Additionally, record components are not traversed explicitly. The javac tree API used by this
 * class does not expose record components in all supported JDK versions, so record-specific
 * structure and annotations may be skipped.
 *
 * <p>The standard {@code accept} methods cannot be used directly to traverse both trees, because
 * javac visitors are designed to traverse a single tree. This class therefore uses {@code accept}
 * only for dispatch, and drives paired traversal explicitly via {@link #scan}.
 */
public abstract class DoubleJavacVisitor extends SimpleTreeVisitor<Void, Tree> {

  /** Create a DoubleJavacVisitor. */
  protected DoubleJavacVisitor() {}

  /**
   * Action hook that subclasses can override to process a matched pair of trees. Also, the fallback
   * action invoked when no {@code visitXyz} method is overridden for the current tree kind.
   *
   * <p>This method is invoked during traversal for a matched pair of trees when the corresponding
   * {@code visitXyz} method is not overridden by a subclass, or when a {@code visitXyz} method
   * explicitly delegates to {@code defaultAction}.
   *
   * <p><b>Important:</b> This method does <em>not</em> recurse into child nodes. Traversal proceeds
   * only through {@code visitXyz} methods that explicitly call {@link #scan} or {@link #scanList}.
   * For any tree kind that is not handled by a {@code visitXyz} method in this class or in a
   * subclass, only {@code defaultAction} is executed and the subtree rooted at that node is not
   * visited.
   *
   * <p>This design ensures that paired traversal of the two ASTs remains explicit and synchronized,
   * and avoids accidental descent into mismatched or unsupported tree structures.
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
   * Traverses two corresponding trees together.
   *
   * <p>This method checks that both trees are either null or have the same {@link Tree.Kind}, then
   * dispatches to the appropriate {@code visitXyz} method to continue paired traversal.
   *
   * @param tree1 the first tree to scan, or null
   * @param tree2 the second tree to scan, or null
   */
  public final void scan(@Nullable Tree tree1, @Nullable Tree tree2) {
    if (tree1 == null && tree2 == null) {
      return;
    }

    Tree.Kind kind1 = tree1 == null ? null : tree1.getKind();
    Tree.Kind kind2 = tree2 == null ? null : tree2.getKind();

    if (tree1 == null || tree2 == null) {
      throw new BugInCF(
          String.format(
              "%s.scan: one tree is null: tree1=%s [%s] tree2=%s [%s]",
              this.getClass().getCanonicalName(), tree1, kind1, tree2, kind2));
    }

    if (tree1.getKind() != tree2.getKind()) {
      throw new BugInCF(
          String.format(
              "%s.scan: mismatched kinds: %s [%s] vs %s [%s]",
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
   * Traverses two lists of trees in lockstep by scanning corresponding elements.
   *
   * <p>This method assumes that {@code list1} and {@code list2} represent parallel AST structures:
   * they must have the same size, and each element at index {@code i} in {@code list1} must
   * correspond to the element at index {@code i} in {@code list2}. For each index, this method
   * invokes {@link #scan(Tree, Tree)} on the paired elements.
   *
   * <p>A size mismatch indicates that paired traversal has become desynchronized and is treated as
   * a bug in the caller.
   *
   * <p>The two list arguments must either both be null or both be non-null. (Empty lists are
   * permitted.)
   *
   * @param list1 the first list of trees, or null
   * @param list2 the second list of trees, or null
   * @throws BugInCF if exactly one list is null or if the two lists have different sizes
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

  /**
   * Scans corresponding annotation trees while permitting annotation-list mismatches.
   *
   * <p>This helper exists to support comparisons between a Java source file and its corresponding
   * {@code .ajava} file, where additional annotations may legitimately appear. The two annotation
   * lists are traversed in lockstep up to the minimum of their sizes; any extra annotations in
   * either list are intentionally ignored.
   *
   * <p>If both lists are {@code null}, this method does nothing. If exactly one list is {@code
   * null}, paired traversal has become desynchronized and this is treated as a bug in the caller.
   *
   * @param anns1 the annotation list from the first AST, or {@code null}
   * @param anns2 the annotation list from the second AST, or {@code null}
   * @throws BugInCF if exactly one of the two lists is {@code null}
   */
  public final void scanAnnotations(
      @Nullable List<? extends AnnotationTree> anns1,
      @Nullable List<? extends AnnotationTree> anns2) {

    if (anns1 == null && anns2 == null) {
      return;
    }
    if (anns1 == null || anns2 == null) {
      throw new BugInCF(
          String.format(
              "%s.scanAnnotations: one list is null: anns1=%s anns2=%s",
              this.getClass().getCanonicalName(), anns1, anns2));
    }

    int n = Math.min(anns1.size(), anns2.size());
    for (int i = 0; i < n; i++) {
      scan(anns1.get(i), anns2.get(i));
    }
  }

  //
  // Visitor methods
  //

  /**
   * Visits a compilation unit (top-level file node) and scans its main children.
   *
   * @param tree1 compilation unit tree from the first AST
   * @param tree2 compilation unit tree from the second AST
   * @return null
   */
  @Override
  public Void visitCompilationUnit(CompilationUnitTree tree1, Tree tree2) {
    CompilationUnitTree tree2cu = (CompilationUnitTree) tree2;
    defaultAction(tree1, tree2cu);

    scan(tree1.getModule(), tree2cu.getModule());
    scan(tree1.getPackage(), tree2cu.getPackage());

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
  public Void visitPackage(PackageTree tree1, Tree tree2) {
    PackageTree tree2pkg = (PackageTree) tree2;
    defaultAction(tree1, tree2pkg);

    scanAnnotations(tree1.getAnnotations(), tree2pkg.getAnnotations());
    scan(tree1.getPackageName(), tree2pkg.getPackageName());
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
  public Void visitImport(ImportTree tree1, Tree tree2) {
    ImportTree tree2imp = (ImportTree) tree2;
    defaultAction(tree1, tree2imp);

    scan(tree1.getQualifiedIdentifier(), tree2imp.getQualifiedIdentifier());
    return null;
  }

  /**
   * Visits a class-like declaration and scans its modifiers, type parameters, superclass,
   * interfaces, permits clause, and members.
   *
   * <p><b>Note:</b> Record components are not traversed explicitly by this method. On some JDK
   * versions, the javac tree API does not expose record components via {@code ClassTree}, so
   * record-specific structure and annotations may be skipped.
   *
   * @param tree1 class tree from the first AST
   * @param tree2 class tree from the second AST
   * @return null
   */
  @Override
  public Void visitClass(ClassTree tree1, Tree tree2) {
    ClassTree tree2cls = (ClassTree) tree2;
    defaultAction(tree1, tree2cls);

    scan(tree1.getModifiers(), tree2cls.getModifiers());
    scanList(tree1.getTypeParameters(), tree2cls.getTypeParameters());
    scan(tree1.getExtendsClause(), tree2cls.getExtendsClause());
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
    defaultAction(tree1, tree2m);

    scan(tree1.getModifiers(), tree2m.getModifiers());
    scanList(tree1.getTypeParameters(), tree2m.getTypeParameters());

    scan(tree1.getReturnType(), tree2m.getReturnType());
    scan(tree1.getReceiverParameter(), tree2m.getReceiverParameter());

    scanList(tree1.getParameters(), tree2m.getParameters());
    scanList(tree1.getThrows(), tree2m.getThrows());

    scan(tree1.getDefaultValue(), tree2m.getDefaultValue());
    scan(tree1.getBody(), tree2m.getBody());
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
    defaultAction(tree1, tree2v);

    scan(tree1.getModifiers(), tree2v.getModifiers());
    scan(tree1.getType(), tree2v.getType());
    scan(tree1.getInitializer(), tree2v.getInitializer());
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
    defaultAction(tree1, tree2m);

    scanAnnotations(tree1.getAnnotations(), tree2m.getAnnotations());
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
    defaultAction(tree1, tree2a);

    scan(tree1.getAnnotationType(), tree2a.getAnnotationType());
    scanList(tree1.getArguments(), tree2a.getArguments());
    return null;
  }

  /**
   * Visits an annotated type and scans both its type-use annotations and its underlying type.
   *
   * <p>In javac's AST, annotations on a type (e.g., {@code @A String}) are represented as {@link
   * AnnotationTree} nodes returned by {@link AnnotatedTypeTree#getAnnotations()}. This method scans
   * those annotations and then scans the underlying type via {@link
   * AnnotatedTypeTree#getUnderlyingType()}.
   *
   * @param tree1 annotated type tree from the first AST
   * @param tree2 annotated type tree from the second AST
   * @return null
   */
  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree tree1, Tree tree2) {
    AnnotatedTypeTree tree2t = (AnnotatedTypeTree) tree2;
    defaultAction(tree1, tree2t);

    scanAnnotations(tree1.getAnnotations(), tree2t.getAnnotations());
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
    defaultAction(tree1, tree2p);

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
    defaultAction(tree1, tree2a);

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
    defaultAction(tree1, tree2p);
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
    defaultAction(tree1, tree2tp);

    scanAnnotations(tree1.getAnnotations(), tree2tp.getAnnotations());
    scanList(tree1.getBounds(), tree2tp.getBounds());
    return null;
  }

  /**
   * Visits a wildcard type and scans its bound (extends or super), if present.
   *
   * <p>This method does not check whether the wildcard uses {@code extends} or {@code super}; only
   * the bound tree is compared.
   *
   * @param tree1 wildcard tree from the first AST
   * @param tree2 wildcard tree from the second AST
   * @return null
   */
  @Override
  public Void visitWildcard(WildcardTree tree1, Tree tree2) {
    WildcardTree tree2w = (WildcardTree) tree2;
    defaultAction(tree1, tree2w);

    scan(tree1.getBound(), tree2w.getBound());
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
    defaultAction(tree1, tree2i);
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
    defaultAction(tree1, tree2ms);

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
    defaultAction(tree1, tree2b);

    scanList(tree1.getStatements(), tree2b.getStatements());
    return null;
  }

  /**
   * Visits an expression statement and scans its expression.
   *
   * @param tree1 expression statement from the first AST
   * @param tree2 expression statement from the second AST
   * @return null
   */
  @Override
  public Void visitExpressionStatement(ExpressionStatementTree tree1, Tree tree2) {
    ExpressionStatementTree tree2es = (ExpressionStatementTree) tree2;
    defaultAction(tree1, tree2es);

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
    defaultAction(tree1, tree2r);

    scan(tree1.getExpression(), tree2r.getExpression());
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
    defaultAction(tree1, tree2t);

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
    defaultAction(tree1, tree2t);

    scanList(tree1.getResources(), tree2t.getResources());
    scan(tree1.getBlock(), tree2t.getBlock());
    scanList(tree1.getCatches(), tree2t.getCatches());
    scan(tree1.getFinallyBlock(), tree2t.getFinallyBlock());
    return null;
  }
}
