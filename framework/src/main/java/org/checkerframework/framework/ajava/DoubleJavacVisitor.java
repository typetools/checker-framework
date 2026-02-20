package org.checkerframework.framework.ajava;

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
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.UsesTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtilsAfterJava11;

/**
 * A visitor that traverses two javac ASTs simultaneously. The two trees must be structurally
 * identical (modulo differences, such as annotations and explicit receiver parameters, between a
 * Java file and its corresponding {@code .ajava} file).
 *
 * <p>The entry point is {@link #scan(Tree, Tree)}. Given two corresponding trees, {@code scan}
 * performs basic structural checks and then calls {@link Tree#accept} on the first tree to dispatch
 * to the appropriate {@code visitXyz} method. The {@code visitXyz} methods in this base class drive
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
 * all paired-tree structural checks in {@link #scan(Tree, Tree)} rather than duplicating them in
 * every {@code visitXyz} method.
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
              "%s.scan: mismatched kinds: tree1=%s [%s] tree2=%s [%s]",
              this.getClass().getCanonicalName(), tree1, kind1, tree2, kind2));
    }

    // For tree types added after JDK 11, the tree classes do not exist at compile time,
    // so we cannot override the visitXyz methods directly.  Handle them via reflection.
    if (visitReflective(tree1, tree2)) {
      return;
    }

    // `accept` will call the appropriate `visitXyz` method.
    tree1.accept(this, tree2);
  }

  /**
   * Handles tree types that were added after JDK 11 and therefore cannot be referenced directly.
   * Calls {@link #defaultAction} and scans child trees using {@link TreeUtilsAfterJava11} helpers.
   *
   * @param tree1 the first tree
   * @param tree2 the second tree
   * @return true if the tree kind was handled, false otherwise
   */
  private boolean visitReflective(Tree tree1, Tree tree2) {
    switch (tree1.getKind().name()) {
      case "YIELD":
        defaultAction(tree1, tree2);
        scanExpr(
            TreeUtilsAfterJava11.YieldUtils.getValue(tree1),
            TreeUtilsAfterJava11.YieldUtils.getValue(tree2));
        return true;

      case "SWITCH_EXPRESSION":
        defaultAction(tree1, tree2);
        scanExpr(
            TreeUtilsAfterJava11.SwitchExpressionUtils.getExpression(tree1),
            TreeUtilsAfterJava11.SwitchExpressionUtils.getExpression(tree2));
        scanList(
            TreeUtilsAfterJava11.SwitchExpressionUtils.getCases(tree1),
            TreeUtilsAfterJava11.SwitchExpressionUtils.getCases(tree2));
        return true;

      case "BINDING_PATTERN":
        defaultAction(tree1, tree2);
        scan(
            TreeUtilsAfterJava11.BindingPatternUtils.getVariable(tree1),
            TreeUtilsAfterJava11.BindingPatternUtils.getVariable(tree2));
        return true;

      case "DEFAULT_CASE_LABEL":
        defaultAction(tree1, tree2);
        return true;

      case "CONSTANT_CASE_LABEL":
        defaultAction(tree1, tree2);
        scanExpr(
            TreeUtilsAfterJava11.ConstantCaseLabelUtils.getConstantExpression(tree1),
            TreeUtilsAfterJava11.ConstantCaseLabelUtils.getConstantExpression(tree2));
        return true;

      case "PATTERN_CASE_LABEL":
        defaultAction(tree1, tree2);
        scan(
            TreeUtilsAfterJava11.PatternCaseLabelUtils.getPattern(tree1),
            TreeUtilsAfterJava11.PatternCaseLabelUtils.getPattern(tree2));
        return true;

      case "DECONSTRUCTION_PATTERN":
        defaultAction(tree1, tree2);
        scan(
            TreeUtilsAfterJava11.DeconstructionPatternUtils.getDeconstructor(tree1),
            TreeUtilsAfterJava11.DeconstructionPatternUtils.getDeconstructor(tree2));
        scanList(
            TreeUtilsAfterJava11.DeconstructionPatternUtils.getNestedPatterns(tree1),
            TreeUtilsAfterJava11.DeconstructionPatternUtils.getNestedPatterns(tree2));
        return true;

      default:
        return false;
    }
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
  // Visitor methods — in the same order as SimpleTreeVisitor.
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
   * interfaces, permits clause, record components (on JDK 16+, via reflection), and members.
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
    scanList(
        TreeUtilsAfterJava11.ClassTreeUtils.getPermitsClause(ctree1),
        TreeUtilsAfterJava11.ClassTreeUtils.getPermitsClause(ctree2));

    // Record components are only available on JDK 16+; access via reflection.
    scanList(
        TreeUtilsAfterJava11.ClassTreeUtils.getRecordComponents(ctree1),
        TreeUtilsAfterJava11.ClassTreeUtils.getRecordComponents(ctree2));

    scanList(ctree1.getMembers(), ctree2.getMembers());
    return null;
  }

  /**
   * Visits a method or constructor declaration and scans modifiers, type parameters, return type,
   * receiver parameter, formal parameters, throws clause, default value, and body.
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
   * Visits a variable declaration (field, local, parameter) and scans its modifiers, type, and
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
   * Visits an empty statement (a lone semicolon).
   *
   * @param etree1 empty statement tree from the first AST
   * @param tree2 empty statement tree from the second AST
   * @return null
   */
  @Override
  public Void visitEmptyStatement(EmptyStatementTree etree1, Tree tree2) {
    EmptyStatementTree etree2 = (EmptyStatementTree) tree2;
    defaultAction(etree1, etree2);
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
   * Visits a do-while loop and scans its statement and condition.
   *
   * @param dtree1 do-while loop tree from the first AST
   * @param tree2 do-while loop tree from the second AST
   * @return null
   */
  @Override
  public Void visitDoWhileLoop(DoWhileLoopTree dtree1, Tree tree2) {
    DoWhileLoopTree dtree2 = (DoWhileLoopTree) tree2;
    defaultAction(dtree1, dtree2);

    scan(dtree1.getStatement(), dtree2.getStatement());
    scanExpr(dtree1.getCondition(), dtree2.getCondition());
    return null;
  }

  /**
   * Visits a while loop and scans its condition and statement.
   *
   * @param wtree1 while loop tree from the first AST
   * @param tree2 while loop tree from the second AST
   * @return null
   */
  @Override
  public Void visitWhileLoop(WhileLoopTree wtree1, Tree tree2) {
    WhileLoopTree wtree2 = (WhileLoopTree) tree2;
    defaultAction(wtree1, wtree2);

    scanExpr(wtree1.getCondition(), wtree2.getCondition());
    scan(wtree1.getStatement(), wtree2.getStatement());
    return null;
  }

  /**
   * Visits a for loop and scans its initializers, condition, update expressions, and statement.
   *
   * @param ftree1 for loop tree from the first AST
   * @param tree2 for loop tree from the second AST
   * @return null
   */
  @Override
  public Void visitForLoop(ForLoopTree ftree1, Tree tree2) {
    ForLoopTree ftree2 = (ForLoopTree) tree2;
    defaultAction(ftree1, ftree2);

    scanList(ftree1.getInitializer(), ftree2.getInitializer());
    scanExpr(ftree1.getCondition(), ftree2.getCondition());
    scanList(ftree1.getUpdate(), ftree2.getUpdate());
    scan(ftree1.getStatement(), ftree2.getStatement());
    return null;
  }

  /**
   * Visits an enhanced for loop and scans its variable, expression, and statement.
   *
   * @param etree1 enhanced for loop tree from the first AST
   * @param tree2 enhanced for loop tree from the second AST
   * @return null
   */
  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree etree1, Tree tree2) {
    EnhancedForLoopTree etree2 = (EnhancedForLoopTree) tree2;
    defaultAction(etree1, etree2);

    scan(etree1.getVariable(), etree2.getVariable());
    scanExpr(etree1.getExpression(), etree2.getExpression());
    scan(etree1.getStatement(), etree2.getStatement());
    return null;
  }

  /**
   * Visits a labeled statement and scans its statement.
   *
   * @param ltree1 labeled statement tree from the first AST
   * @param tree2 labeled statement tree from the second AST
   * @return null
   */
  @Override
  public Void visitLabeledStatement(LabeledStatementTree ltree1, Tree tree2) {
    LabeledStatementTree ltree2 = (LabeledStatementTree) tree2;
    defaultAction(ltree1, ltree2);

    scan(ltree1.getStatement(), ltree2.getStatement());
    return null;
  }

  /**
   * Visits a switch statement and scans its expression and cases.
   *
   * @param stree1 switch tree from the first AST
   * @param tree2 switch tree from the second AST
   * @return null
   */
  @Override
  public Void visitSwitch(SwitchTree stree1, Tree tree2) {
    SwitchTree stree2 = (SwitchTree) tree2;
    defaultAction(stree1, stree2);

    scanExpr(stree1.getExpression(), stree2.getExpression());
    scanList(stree1.getCases(), stree2.getCases());
    return null;
  }

  /**
   * Visits a case clause and scans its labels, guard expression (JDK 21+), statements, and body.
   * Uses {@link TreeUtilsAfterJava11.CaseUtils} to handle JDK 12+ and 21+ API differences.
   *
   * @param ctree1 case tree from the first AST
   * @param tree2 case tree from the second AST
   * @return null
   */
  @Override
  public Void visitCase(CaseTree ctree1, Tree tree2) {
    CaseTree ctree2 = (CaseTree) tree2;
    defaultAction(ctree1, ctree2);

    scanList(
        TreeUtilsAfterJava11.CaseUtils.getLabels(ctree1),
        TreeUtilsAfterJava11.CaseUtils.getLabels(ctree2));

    scanExpr(
        TreeUtilsAfterJava11.CaseUtils.getGuard(ctree1),
        TreeUtilsAfterJava11.CaseUtils.getGuard(ctree2));

    if (TreeUtilsAfterJava11.CaseUtils.isCaseRule(ctree1)
        != TreeUtilsAfterJava11.CaseUtils.isCaseRule(ctree2)) {
      throw new BugInCF(
          String.format(
              "%s.visitCase: mismatched case forms: tree1 isCaseRule=%s tree2 isCaseRule=%s",
              this.getClass().getCanonicalName(),
              TreeUtilsAfterJava11.CaseUtils.isCaseRule(ctree1),
              TreeUtilsAfterJava11.CaseUtils.isCaseRule(ctree2)));
    }

    if (TreeUtilsAfterJava11.CaseUtils.isCaseRule(ctree1)) {
      scan(
          TreeUtilsAfterJava11.CaseUtils.getBody(ctree1),
          TreeUtilsAfterJava11.CaseUtils.getBody(ctree2));
    } else {
      @SuppressWarnings("deprecation")
      List<? extends Tree> stmts1 = ctree1.getStatements();
      @SuppressWarnings("deprecation")
      List<? extends Tree> stmts2 = ctree2.getStatements();
      scanList(stmts1, stmts2);
    }
    return null;
  }

  /**
   * Visits a synchronized statement and scans its expression and block.
   *
   * @param stree1 synchronized tree from the first AST
   * @param tree2 synchronized tree from the second AST
   * @return null
   */
  @Override
  public Void visitSynchronized(SynchronizedTree stree1, Tree tree2) {
    SynchronizedTree stree2 = (SynchronizedTree) tree2;
    defaultAction(stree1, stree2);

    scanExpr(stree1.getExpression(), stree2.getExpression());
    scan(stree1.getBlock(), stree2.getBlock());
    return null;
  }

  /**
   * Visits a try statement and scans its resources, try block, catch clauses, and finally block.
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

  /**
   * Visits a catch clause and scans its parameter and block.
   *
   * @param ctree1 catch tree from the first AST
   * @param tree2 catch tree from the second AST
   * @return null
   */
  @Override
  public Void visitCatch(CatchTree ctree1, Tree tree2) {
    CatchTree ctree2 = (CatchTree) tree2;
    defaultAction(ctree1, ctree2);

    scan(ctree1.getParameter(), ctree2.getParameter());
    scan(ctree1.getBlock(), ctree2.getBlock());
    return null;
  }

  /**
   * Visits a conditional (ternary) expression and scans its condition, true expression, and false
   * expression.
   *
   * @param ctree1 conditional expression tree from the first AST
   * @param tree2 conditional expression tree from the second AST
   * @return null
   */
  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree ctree1, Tree tree2) {
    ConditionalExpressionTree ctree2 = (ConditionalExpressionTree) tree2;
    defaultAction(ctree1, ctree2);

    scanExpr(ctree1.getCondition(), ctree2.getCondition());
    scanExpr(ctree1.getTrueExpression(), ctree2.getTrueExpression());
    scanExpr(ctree1.getFalseExpression(), ctree2.getFalseExpression());
    return null;
  }

  /**
   * Visits an if statement and scans its condition, then statement, and else statement.
   *
   * @param itree1 if tree from the first AST
   * @param tree2 if tree from the second AST
   * @return null
   */
  @Override
  public Void visitIf(IfTree itree1, Tree tree2) {
    IfTree itree2 = (IfTree) tree2;
    defaultAction(itree1, itree2);

    scanExpr(itree1.getCondition(), itree2.getCondition());
    scan(itree1.getThenStatement(), itree2.getThenStatement());
    scan(itree1.getElseStatement(), itree2.getElseStatement());
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
   * Visits a break statement.
   *
   * @param btree1 break tree from the first AST
   * @param tree2 break tree from the second AST
   * @return null
   */
  @Override
  public Void visitBreak(BreakTree btree1, Tree tree2) {
    BreakTree btree2 = (BreakTree) tree2;
    defaultAction(btree1, btree2);
    return null;
  }

  /**
   * Visits a continue statement.
   *
   * @param ctree1 continue tree from the first AST
   * @param tree2 continue tree from the second AST
   * @return null
   */
  @Override
  public Void visitContinue(ContinueTree ctree1, Tree tree2) {
    ContinueTree ctree2 = (ContinueTree) tree2;
    defaultAction(ctree1, ctree2);
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
   * Visits a throw statement and scans its thrown expression.
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
   * Visits an assert statement and scans its condition and detail expression.
   *
   * @param atree1 assert tree from the first AST
   * @param tree2 assert tree from the second AST
   * @return null
   */
  @Override
  public Void visitAssert(AssertTree atree1, Tree tree2) {
    AssertTree atree2 = (AssertTree) tree2;
    defaultAction(atree1, atree2);

    scanExpr(atree1.getCondition(), atree2.getCondition());
    scanExpr(atree1.getDetail(), atree2.getDetail());
    return null;
  }

  /**
   * Visits a method invocation and scans its type arguments, method select, and arguments.
   *
   * @param mtree1 method invocation tree from the first AST
   * @param tree2 method invocation tree from the second AST
   * @return null
   */
  @Override
  public Void visitMethodInvocation(MethodInvocationTree mtree1, Tree tree2) {
    MethodInvocationTree mtree2 = (MethodInvocationTree) tree2;
    defaultAction(mtree1, mtree2);

    scanList(mtree1.getTypeArguments(), mtree2.getTypeArguments());
    scanExpr(mtree1.getMethodSelect(), mtree2.getMethodSelect());
    scanList(mtree1.getArguments(), mtree2.getArguments());
    return null;
  }

  /**
   * Visits a new class expression and scans its enclosing expression, type arguments, identifier,
   * arguments, and class body.
   *
   * @param ntree1 new class tree from the first AST
   * @param tree2 new class tree from the second AST
   * @return null
   */
  @Override
  public Void visitNewClass(NewClassTree ntree1, Tree tree2) {
    NewClassTree ntree2 = (NewClassTree) tree2;
    defaultAction(ntree1, ntree2);

    scanExpr(ntree1.getEnclosingExpression(), ntree2.getEnclosingExpression());
    scanList(ntree1.getTypeArguments(), ntree2.getTypeArguments());
    scan(ntree1.getIdentifier(), ntree2.getIdentifier());
    scanList(ntree1.getArguments(), ntree2.getArguments());
    scan(ntree1.getClassBody(), ntree2.getClassBody());
    return null;
  }

  /**
   * Visits a new array expression and scans its type, dimensions, and initializers.
   *
   * <p>Array-level annotations may legitimately differ between a Java file and its corresponding
   * {@code .ajava} file, so this visitor does not compare or traverse the annotation lists.
   *
   * @param ntree1 new array tree from the first AST
   * @param tree2 new array tree from the second AST
   * @return null
   */
  @Override
  public Void visitNewArray(NewArrayTree ntree1, Tree tree2) {
    NewArrayTree ntree2 = (NewArrayTree) tree2;
    defaultAction(ntree1, ntree2);

    scan(ntree1.getType(), ntree2.getType());
    scanList(ntree1.getDimensions(), ntree2.getDimensions());
    scanList(ntree1.getInitializers(), ntree2.getInitializers());
    return null;
  }

  /**
   * Visits a lambda expression and scans its parameters and body.
   *
   * @param ltree1 lambda expression tree from the first AST
   * @param tree2 lambda expression tree from the second AST
   * @return null
   */
  @Override
  public Void visitLambdaExpression(LambdaExpressionTree ltree1, Tree tree2) {
    LambdaExpressionTree ltree2 = (LambdaExpressionTree) tree2;
    defaultAction(ltree1, ltree2);

    scanList(ltree1.getParameters(), ltree2.getParameters());
    scan(ltree1.getBody(), ltree2.getBody());
    return null;
  }

  /**
   * Visits a parenthesized expression and scans its inner expression.
   *
   * @param ptree1 parenthesized tree from the first AST
   * @param tree2 parenthesized tree from the second AST
   * @return null
   */
  @Override
  public Void visitParenthesized(ParenthesizedTree ptree1, Tree tree2) {
    ParenthesizedTree ptree2 = (ParenthesizedTree) tree2;
    defaultAction(ptree1, ptree2);

    scanExpr(ptree1.getExpression(), ptree2.getExpression());
    return null;
  }

  /**
   * Visits an assignment expression and scans its variable and expression.
   *
   * @param atree1 assignment tree from the first AST
   * @param tree2 assignment tree from the second AST
   * @return null
   */
  @Override
  public Void visitAssignment(AssignmentTree atree1, Tree tree2) {
    AssignmentTree atree2 = (AssignmentTree) tree2;
    defaultAction(atree1, atree2);

    scanExpr(atree1.getVariable(), atree2.getVariable());
    scanExpr(atree1.getExpression(), atree2.getExpression());
    return null;
  }

  /**
   * Visits a compound assignment expression and scans its variable and expression.
   *
   * @param ctree1 compound assignment tree from the first AST
   * @param tree2 compound assignment tree from the second AST
   * @return null
   */
  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree ctree1, Tree tree2) {
    CompoundAssignmentTree ctree2 = (CompoundAssignmentTree) tree2;
    defaultAction(ctree1, ctree2);

    scanExpr(ctree1.getVariable(), ctree2.getVariable());
    scanExpr(ctree1.getExpression(), ctree2.getExpression());
    return null;
  }

  /**
   * Visits a unary expression and scans its operand.
   *
   * @param utree1 unary tree from the first AST
   * @param tree2 unary tree from the second AST
   * @return null
   */
  @Override
  public Void visitUnary(UnaryTree utree1, Tree tree2) {
    UnaryTree utree2 = (UnaryTree) tree2;
    defaultAction(utree1, utree2);

    scanExpr(utree1.getExpression(), utree2.getExpression());
    return null;
  }

  /**
   * Visits a binary expression and scans its left and right operands.
   *
   * @param btree1 binary tree from the first AST
   * @param tree2 binary tree from the second AST
   * @return null
   */
  @Override
  public Void visitBinary(BinaryTree btree1, Tree tree2) {
    BinaryTree btree2 = (BinaryTree) tree2;
    defaultAction(btree1, btree2);

    scanExpr(btree1.getLeftOperand(), btree2.getLeftOperand());
    scanExpr(btree1.getRightOperand(), btree2.getRightOperand());
    return null;
  }

  /**
   * Visits a type cast expression and scans its target type and expression.
   *
   * @param ttree1 type cast tree from the first AST
   * @param tree2 type cast tree from the second AST
   * @return null
   */
  @Override
  public Void visitTypeCast(TypeCastTree ttree1, Tree tree2) {
    TypeCastTree ttree2 = (TypeCastTree) tree2;
    defaultAction(ttree1, ttree2);

    scan(ttree1.getType(), ttree2.getType());
    scanExpr(ttree1.getExpression(), ttree2.getExpression());
    return null;
  }

  /**
   * Visits an instanceof expression and scans its expression, type, and pattern (JDK 16+).
   *
   * @param itree1 instanceof tree from the first AST
   * @param tree2 instanceof tree from the second AST
   * @return null
   */
  @Override
  public Void visitInstanceOf(InstanceOfTree itree1, Tree tree2) {
    InstanceOfTree itree2 = (InstanceOfTree) tree2;
    defaultAction(itree1, itree2);

    scanExpr(itree1.getExpression(), itree2.getExpression());
    scan(itree1.getType(), itree2.getType());
    scan(
        TreeUtilsAfterJava11.InstanceOfUtils.getPattern(itree1),
        TreeUtilsAfterJava11.InstanceOfUtils.getPattern(itree2));
    return null;
  }

  /**
   * Visits an array access expression and scans its array expression and index.
   *
   * @param atree1 array access tree from the first AST
   * @param tree2 array access tree from the second AST
   * @return null
   */
  @Override
  public Void visitArrayAccess(ArrayAccessTree atree1, Tree tree2) {
    ArrayAccessTree atree2 = (ArrayAccessTree) tree2;
    defaultAction(atree1, atree2);

    scanExpr(atree1.getExpression(), atree2.getExpression());
    scanExpr(atree1.getIndex(), atree2.getIndex());
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
   * Visits a member reference (method reference) and scans its qualifier expression and type
   * arguments.
   *
   * @param mtree1 member reference tree from the first AST
   * @param tree2 member reference tree from the second AST
   * @return null
   */
  @Override
  public Void visitMemberReference(MemberReferenceTree mtree1, Tree tree2) {
    MemberReferenceTree mtree2 = (MemberReferenceTree) tree2;
    defaultAction(mtree1, mtree2);

    scanExpr(mtree1.getQualifierExpression(), mtree2.getQualifierExpression());
    scanList(mtree1.getTypeArguments(), mtree2.getTypeArguments());
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
   * Visits a literal.
   *
   * @param ltree1 literal tree from the first AST
   * @param tree2 literal tree from the second AST
   * @return null
   */
  @Override
  public Void visitLiteral(LiteralTree ltree1, Tree tree2) {
    LiteralTree ltree2 = (LiteralTree) tree2;
    defaultAction(ltree1, ltree2);
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
   * Visits a union type (multi-catch) and scans its type alternatives.
   *
   * @param utree1 union type tree from the first AST
   * @param tree2 union type tree from the second AST
   * @return null
   */
  @Override
  public Void visitUnionType(UnionTypeTree utree1, Tree tree2) {
    UnionTypeTree utree2 = (UnionTypeTree) tree2;
    defaultAction(utree1, utree2);

    scanList(utree1.getTypeAlternatives(), utree2.getTypeAlternatives());
    return null;
  }

  /**
   * Visits an intersection type and scans its bounds.
   *
   * @param itree1 intersection type tree from the first AST
   * @param tree2 intersection type tree from the second AST
   * @return null
   */
  @Override
  public Void visitIntersectionType(IntersectionTypeTree itree1, Tree tree2) {
    IntersectionTypeTree itree2 = (IntersectionTypeTree) tree2;
    defaultAction(itree1, itree2);

    scanList(itree1.getBounds(), itree2.getBounds());
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
   * <p>The wildcard direction ({@code extends} vs. {@code super}) is enforced by {@link #scan(Tree,
   * Tree)}, which checks that both trees have the same {@link Tree.Kind}.
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
   * Visits a modifiers node.
   *
   * <p>Declaration annotations may legitimately differ between a Java file and its corresponding
   * {@code .ajava} file, so this visitor does not compare or traverse the annotation list.
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
   * Visits an annotation and scans its annotation type and arguments.
   *
   * @param atree1 annotation tree from the first AST
   * @param tree2 annotation tree from the second AST
   * @return null
   */
  @Override
  public Void visitAnnotation(AnnotationTree atree1, Tree tree2) {
    AnnotationTree atree2 = (AnnotationTree) tree2;
    defaultAction(atree1, atree2);

    scan(atree1.getAnnotationType(), atree2.getAnnotationType());
    scanList(atree1.getArguments(), atree2.getArguments());
    return null;
  }

  /**
   * Visits an annotated type and scans its underlying type.
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
   * Visits a module declaration and scans its name and directives.
   *
   * <p>Module annotations may legitimately differ between a Java file and its corresponding {@code
   * .ajava} file, so this visitor does not compare or traverse the annotation list.
   *
   * @param mtree1 module tree from the first AST
   * @param tree2 module tree from the second AST
   * @return null
   */
  @Override
  public Void visitModule(ModuleTree mtree1, Tree tree2) {
    ModuleTree mtree2 = (ModuleTree) tree2;
    defaultAction(mtree1, mtree2);

    scan(mtree1.getName(), mtree2.getName());
    scanList(mtree1.getDirectives(), mtree2.getDirectives());
    return null;
  }

  /**
   * Visits an exports directive and scans its package name and module names.
   *
   * @param etree1 exports tree from the first AST
   * @param tree2 exports tree from the second AST
   * @return null
   */
  @Override
  public Void visitExports(ExportsTree etree1, Tree tree2) {
    ExportsTree etree2 = (ExportsTree) tree2;
    defaultAction(etree1, etree2);

    scan(etree1.getPackageName(), etree2.getPackageName());
    scanList(etree1.getModuleNames(), etree2.getModuleNames());
    return null;
  }

  /**
   * Visits an opens directive and scans its package name and module names.
   *
   * @param otree1 opens tree from the first AST
   * @param tree2 opens tree from the second AST
   * @return null
   */
  @Override
  public Void visitOpens(OpensTree otree1, Tree tree2) {
    OpensTree otree2 = (OpensTree) tree2;
    defaultAction(otree1, otree2);

    scan(otree1.getPackageName(), otree2.getPackageName());
    scanList(otree1.getModuleNames(), otree2.getModuleNames());
    return null;
  }

  /**
   * Visits a provides directive and scans its service name and implementation names.
   *
   * @param ptree1 provides tree from the first AST
   * @param tree2 provides tree from the second AST
   * @return null
   */
  @Override
  public Void visitProvides(ProvidesTree ptree1, Tree tree2) {
    ProvidesTree ptree2 = (ProvidesTree) tree2;
    defaultAction(ptree1, ptree2);

    scan(ptree1.getServiceName(), ptree2.getServiceName());
    scanList(ptree1.getImplementationNames(), ptree2.getImplementationNames());
    return null;
  }

  /**
   * Visits a requires directive and scans its module name.
   *
   * @param rtree1 requires tree from the first AST
   * @param tree2 requires tree from the second AST
   * @return null
   */
  @Override
  public Void visitRequires(RequiresTree rtree1, Tree tree2) {
    RequiresTree rtree2 = (RequiresTree) tree2;
    defaultAction(rtree1, rtree2);

    scan(rtree1.getModuleName(), rtree2.getModuleName());
    return null;
  }

  /**
   * Visits a uses directive and scans its service name.
   *
   * @param utree1 uses tree from the first AST
   * @param tree2 uses tree from the second AST
   * @return null
   */
  @Override
  public Void visitUses(UsesTree utree1, Tree tree2) {
    UsesTree utree2 = (UsesTree) tree2;
    defaultAction(utree1, utree2);

    scan(utree1.getServiceName(), utree2.getServiceName());
    return null;
  }

  /**
   * Visits an erroneous tree and scans its error trees.
   *
   * @param etree1 erroneous tree from the first AST
   * @param tree2 erroneous tree from the second AST
   * @return null
   */
  @Override
  public Void visitErroneous(ErroneousTree etree1, Tree tree2) {
    ErroneousTree etree2 = (ErroneousTree) tree2;
    defaultAction(etree1, etree2);

    scanList(etree1.getErrorTrees(), etree2.getErrorTrees());
    return null;
  }

  /**
   * Visits an unknown tree kind.
   *
   * @param tree1 tree from the first AST
   * @param tree2 tree from the second AST
   * @return null
   */
  @Override
  public Void visitOther(Tree tree1, Tree tree2) {
    defaultAction(tree1, tree2);
    return null;
  }
}
