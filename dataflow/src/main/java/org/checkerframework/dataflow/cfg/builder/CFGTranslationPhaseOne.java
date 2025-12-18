package org.checkerframework.dataflow.cfg.builder;

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
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store.FlowRule;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AnyPatternNode;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.ArrayTypeNode;
import org.checkerframework.dataflow.cfg.node.AssertionErrorNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.BitwiseAndNode;
import org.checkerframework.dataflow.cfg.node.BitwiseComplementNode;
import org.checkerframework.dataflow.cfg.node.BitwiseOrNode;
import org.checkerframework.dataflow.cfg.node.BitwiseXorNode;
import org.checkerframework.dataflow.cfg.node.BooleanLiteralNode;
import org.checkerframework.dataflow.cfg.node.CaseNode;
import org.checkerframework.dataflow.cfg.node.CatchMarkerNode;
import org.checkerframework.dataflow.cfg.node.CharacterLiteralNode;
import org.checkerframework.dataflow.cfg.node.ClassDeclarationNode;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.ConditionalAndNode;
import org.checkerframework.dataflow.cfg.node.ConditionalNotNode;
import org.checkerframework.dataflow.cfg.node.ConditionalOrNode;
import org.checkerframework.dataflow.cfg.node.DeconstructorPatternNode;
import org.checkerframework.dataflow.cfg.node.DoubleLiteralNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.ExplicitThisNode;
import org.checkerframework.dataflow.cfg.node.ExpressionStatementNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.FloatLiteralNode;
import org.checkerframework.dataflow.cfg.node.FloatingDivisionNode;
import org.checkerframework.dataflow.cfg.node.FloatingRemainderNode;
import org.checkerframework.dataflow.cfg.node.FunctionalInterfaceNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisNode;
import org.checkerframework.dataflow.cfg.node.InstanceOfNode;
import org.checkerframework.dataflow.cfg.node.IntegerDivisionNode;
import org.checkerframework.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.dataflow.cfg.node.IntegerRemainderNode;
import org.checkerframework.dataflow.cfg.node.LambdaResultExpressionNode;
import org.checkerframework.dataflow.cfg.node.LeftShiftNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.LongLiteralNode;
import org.checkerframework.dataflow.cfg.node.MarkerNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.NarrowingConversionNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.dataflow.cfg.node.NullChkNode;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.dataflow.cfg.node.NumericalMinusNode;
import org.checkerframework.dataflow.cfg.node.NumericalMultiplicationNode;
import org.checkerframework.dataflow.cfg.node.NumericalPlusNode;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.PackageNameNode;
import org.checkerframework.dataflow.cfg.node.ParameterizedTypeNode;
import org.checkerframework.dataflow.cfg.node.PrimitiveTypeNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.SignedRightShiftNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.dataflow.cfg.node.StringConversionNode;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;
import org.checkerframework.dataflow.cfg.node.SuperNode;
import org.checkerframework.dataflow.cfg.node.SwitchExpressionNode;
import org.checkerframework.dataflow.cfg.node.SynchronizedNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.cfg.node.ThisNode;
import org.checkerframework.dataflow.cfg.node.ThrowNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.cfg.node.UnsignedRightShiftNode;
import org.checkerframework.dataflow.cfg.node.ValueLiteralNode;
import org.checkerframework.dataflow.cfg.node.VariableDeclarationNode;
import org.checkerframework.dataflow.cfg.node.WideningConversionNode;
import org.checkerframework.dataflow.qual.AssertMethod;
import org.checkerframework.dataflow.qual.TerminatesExecution;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava11.BindingPatternUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava11.CaseUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava11.DeconstructionPatternUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava11.InstanceOfUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava11.SwitchExpressionUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava11.YieldUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypeKindUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.trees.TreeBuilder;
import org.plumelib.util.ArrayMap;
import org.plumelib.util.ArraySet;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.IPair;
import org.plumelib.util.IdentityArraySet;

/**
 * Class that performs phase one of the translation process. It generates the following information:
 *
 * <ul>
 *   <li>A sequence of extended nodes.
 *   <li>A set of bindings from {@link Label}s to positions in the node sequence.
 *   <li>A set of leader nodes that give rise to basic blocks in phase two.
 *   <li>A mapping from AST tree nodes to {@link Node}s.
 * </ul>
 *
 * <p>The return type of this scanner is {@link Node}. For expressions, the corresponding node is
 * returned to allow linking between different nodes.
 *
 * <p>However, for statements there is usually no single {@link Node} that is created, and thus null
 * is returned.
 *
 * <p>Every {@code visit*} method is assumed to add at least one extended node to the list of nodes
 * (which might only be a jump).
 *
 * <p>The entry point to process a single body (e.g., method, lambda, top-level block) is {@link
 * #process(TreePath, UnderlyingAST)}.
 */
@SuppressWarnings("nullness") // TODO
public class CFGTranslationPhaseOne extends TreeScanner<Node, Void> {

  /** Path to the tree currently being scanned. */
  private TreePath path;

  /** Annotation processing environment and its associated type and tree utilities. */
  protected final ProcessingEnvironment env;

  /** The javac element utilities. */
  protected final Elements elements;

  /** The javac type utilities. */
  protected final Types types;

  /** The javac tree utilities. */
  protected final Trees trees;

  /** The tree builder. */
  protected final TreeBuilder treeBuilder;

  /** The annotation provider, e.g., a type factory. */
  protected final AnnotationProvider annotationProvider;

  /** Can assertions be assumed to be disabled? */
  protected final boolean assumeAssertionsDisabled;

  /** Can assertions be assumed to be enabled? */
  protected final boolean assumeAssertionsEnabled;

  /* --------------------------------------------------------- */
  /* Extended Node Types and Labels */
  /* --------------------------------------------------------- */

  /** Special label to identify the regular exit. */
  private final Label regularExitLabel;

  /** Special label to identify the exceptional exit. */
  private final Label exceptionalExitLabel;

  /**
   * Current {@link LabelCell} to which a return statement should jump, or null if there is no valid
   * destination.
   */
  private @Nullable LabelCell returnTargetLC;

  /**
   * Current {@link LabelCell} to which a break statement with no label should jump, or null if
   * there is no valid destination.
   */
  private @Nullable LabelCell breakTargetLC;

  /**
   * Map from AST label Names to CFG {@link Label}s for breaks. Each labeled statement creates two
   * CFG {@link Label}s, one for break and one for continue.
   */
  private Map<Name, Label> breakLabels;

  /**
   * Current {@link LabelCell} to which a continue statement with no label should jump, or null if
   * there is no valid destination.
   */
  private @Nullable LabelCell continueTargetLC;

  /**
   * Map from AST label Names to CFG {@link Label}s for continues. Each labeled statement creates
   * two CFG {@link Label}s, one for break and one for continue.
   */
  private Map<Name, Label> continueLabels;

  /** Nested scopes of try-catch blocks in force at the current program point. */
  private final TryStack tryStack;

  /** SwitchBuilder for the current switch. Used to match yield statements to enclosing switches. */
  private SwitchBuilder switchBuilder;

  /**
   * Maps from AST {@link Tree}s to sets of {@link Node}s. Every Tree that produces a value will
   * have at least one corresponding Node. Trees that undergo conversions, such as boxing or
   * unboxing, can map to two distinct Nodes. The Node for the pre-conversion value is stored in the
   * treeToCfgNodes, while the Node for the post-conversion value is stored in the
   * treeToConvertedCfgNodes.
   */
  private final IdentityHashMap<Tree, Set<Node>> treeToCfgNodes;

  /** Map from AST {@link Tree}s to post-conversion sets of {@link Node}s. */
  private final IdentityHashMap<Tree, Set<Node>> treeToConvertedCfgNodes;

  /**
   * Map from postfix increment or decrement trees that are AST {@link UnaryTree}s to the synthetic
   * tree that is {@code v + 1} or {@code v - 1}.
   */
  private final IdentityHashMap<UnaryTree, BinaryTree> postfixTreeToCfgNodes;

  /** The list of extended nodes. */
  private final ArrayList<ExtendedNode> nodeList;

  /** The bindings of labels to positions (i.e., indices) in the {@code nodeList}. */
  private final Map<Label, Integer> bindings;

  /** The set of leaders (represented as indices into {@code nodeList}). */
  private final Set<Integer> leaders;

  /**
   * All return nodes (if any) encountered. Only includes return statements that actually return
   * something.
   */
  private final List<ReturnNode> returnNodes;

  /**
   * Class declarations that have been encountered when building the control-flow graph for a
   * method.
   */
  private final List<ClassTree> declaredClasses;

  /**
   * Lambdas encountered when building the control-flow graph for a method, variable initializer, or
   * initializer.
   */
  private final List<LambdaExpressionTree> declaredLambdas;

  /** The ArithmeticException type. */
  protected final TypeMirror arithmeticExceptionType;

  /** The ArrayIndexOutOfBoundsException type. */
  protected final TypeMirror arrayIndexOutOfBoundsExceptionType;

  /** The AssertionError type. */
  protected final TypeMirror assertionErrorType;

  /** The ClassCastException type . */
  protected final TypeMirror classCastExceptionType;

  /** The Iterable type (erased). */
  protected final TypeMirror iterableType;

  /** The NegativeArraySizeException type. */
  protected final TypeMirror negativeArraySizeExceptionType;

  /** The NullPointerException type . */
  protected final TypeMirror nullPointerExceptionType;

  /** The OutOfMemoryError type. */
  protected final @Nullable TypeMirror outOfMemoryErrorType;

  /** The ClassCircularityError type. */
  protected final @Nullable TypeMirror classCircularityErrorType;

  /** The ClassFormatErrorType type. */
  protected final @Nullable TypeMirror classFormatErrorType;

  /** The NoClassDefFoundError type. */
  protected final @Nullable TypeMirror noClassDefFoundErrorType;

  /** The String type. */
  protected final TypeMirror stringType;

  /** The Throwable type. */
  protected final TypeMirror throwableType;

  /**
   * Supertypes of all unchecked exceptions. The size is 2 and the contents are {@code
   * RuntimeException} and {@code Error}.
   */
  protected final Set<TypeMirror> uncheckedExceptionTypes;

  /**
   * Exceptions that can be thrown by array creation "new SomeType[]". The size is 2 and the
   * contents are {@code NegativeArraySizeException} and {@code OutOfMemoryError}. This list comes
   * from JLS 15.10.1 "Run-Time Evaluation of Array Creation Expressions".
   */
  protected final Set<TypeMirror> newArrayExceptionTypes;

  /**
   * Exceptions that can be thrown by array access "a[i]". The size is 2 and the contents are {@code
   * ArrayIndexOutOfBoundsException} and {@code NullPointerException}.
   */
  protected final Set<TypeMirror> arrayAccessExceptionTypes;

  /**
   * Creates {@link CFGTranslationPhaseOne}.
   *
   * @param treeBuilder builder for new AST nodes
   * @param annotationProvider extracts annotations from AST nodes
   * @param assumeAssertionsDisabled can assertions be assumed to be disabled?
   * @param assumeAssertionsEnabled can assertions be assumed to be enabled?
   * @param env annotation processing environment containing type utilities
   */
  @SuppressWarnings("this-escape")
  public CFGTranslationPhaseOne(
      TreeBuilder treeBuilder,
      AnnotationProvider annotationProvider,
      boolean assumeAssertionsEnabled,
      boolean assumeAssertionsDisabled,
      ProcessingEnvironment env) {
    this.env = env;
    this.treeBuilder = treeBuilder;
    this.annotationProvider = annotationProvider;

    assert !(assumeAssertionsDisabled && assumeAssertionsEnabled);
    this.assumeAssertionsEnabled = assumeAssertionsEnabled;
    this.assumeAssertionsDisabled = assumeAssertionsDisabled;

    elements = env.getElementUtils();
    types = env.getTypeUtils();
    trees = Trees.instance(env);

    // initialize lists and maps
    treeToCfgNodes = new IdentityHashMap<>();
    treeToConvertedCfgNodes = new IdentityHashMap<>();
    postfixTreeToCfgNodes = new IdentityHashMap<>();
    nodeList = new ArrayList<>();
    bindings = new HashMap<>();
    leaders = new HashSet<>();

    regularExitLabel = new Label();
    exceptionalExitLabel = new Label();
    tryStack = new TryStack(exceptionalExitLabel);
    returnTargetLC = new LabelCell(regularExitLabel);
    breakLabels = new HashMap<>(4);
    continueLabels = new HashMap<>(4);
    returnNodes = new ArrayList<>();
    declaredClasses = new ArrayList<>();
    declaredLambdas = new ArrayList<>();

    arithmeticExceptionType = getTypeMirror(ArithmeticException.class);
    arrayIndexOutOfBoundsExceptionType = getTypeMirror(ArrayIndexOutOfBoundsException.class);
    assertionErrorType = getTypeMirror(AssertionError.class);
    classCastExceptionType = getTypeMirror(ClassCastException.class);
    iterableType = types.erasure(getTypeMirror(Iterable.class));
    negativeArraySizeExceptionType = getTypeMirror(NegativeArraySizeException.class);
    nullPointerExceptionType = getTypeMirror(NullPointerException.class);
    outOfMemoryErrorType = maybeGetTypeMirror(OutOfMemoryError.class);
    classCircularityErrorType = maybeGetTypeMirror(ClassCircularityError.class);
    classFormatErrorType = maybeGetTypeMirror(ClassFormatError.class);
    noClassDefFoundErrorType = maybeGetTypeMirror(NoClassDefFoundError.class);
    stringType = getTypeMirror(String.class);
    throwableType = getTypeMirror(Throwable.class);

    uncheckedExceptionTypes = new ArraySet<>(2);
    uncheckedExceptionTypes.add(getTypeMirror(RuntimeException.class));
    uncheckedExceptionTypes.add(getTypeMirror(Error.class));
    newArrayExceptionTypes = new ArraySet<>(2);
    newArrayExceptionTypes.add(negativeArraySizeExceptionType);
    if (outOfMemoryErrorType != null) {
      newArrayExceptionTypes.add(outOfMemoryErrorType);
    }
    arrayAccessExceptionTypes = new ArraySet<>(2);
    arrayAccessExceptionTypes.add(arrayIndexOutOfBoundsExceptionType);
    arrayAccessExceptionTypes.add(nullPointerExceptionType);
  }

  /**
   * Performs the actual work of phase one: processing a single body (of a method, lambda, top-level
   * block, etc.).
   *
   * @param bodyPath path to the body of the underlying AST's method
   * @param underlyingAST the AST for which the CFG is to be built
   * @return the result of phase one
   */
  public PhaseOneResult process(TreePath bodyPath, UnderlyingAST underlyingAST) {

    // Set class variables
    this.path = bodyPath;

    // Traverse AST of the method body.
    try { // "finally" clause is "this.path = null"
      Node finalNode = scan(path.getLeaf(), null);

      // If we are building the CFG for a lambda with a single expression as the body, then
      // add an extra node for the result of that lambda.
      if (underlyingAST.getKind() == UnderlyingAST.Kind.LAMBDA) {
        LambdaExpressionTree lambdaTree = ((UnderlyingAST.CFGLambda) underlyingAST).getLambdaTree();
        if (lambdaTree.getBodyKind() == LambdaExpressionTree.BodyKind.EXPRESSION) {
          Node resultNode =
              new LambdaResultExpressionNode((ExpressionTree) lambdaTree.getBody(), finalNode);
          extendWithNode(resultNode);
        }
      }

      // Add marker to indicate that the next block will be the exit block.
      // Note: if there is a return statement earlier in the method (which is always the case
      // for non-void methods), then this is not strictly necessary. However, it is also not a
      // problem, as it will just generate a degenerate control graph case that will be
      // removed in a later phase.
      nodeList.add(new UnconditionalJump(regularExitLabel));

      return new PhaseOneResult(
          underlyingAST,
          treeToCfgNodes,
          treeToConvertedCfgNodes,
          postfixTreeToCfgNodes,
          nodeList,
          bindings,
          leaders,
          returnNodes,
          regularExitLabel,
          exceptionalExitLabel,
          declaredClasses,
          declaredLambdas,
          types);
    } finally {
      this.path = null;
    }
  }

  /**
   * Process a single body within {@code root}. This method does not process the entire given
   * CompilationUnitTree. Rather, it processes one body (of a method/lambda/etc.) within it, which
   * corresponds to {@code underlyingAST}.
   *
   * @param root the compilation unit
   * @param underlyingAST the AST corresponding to the body to process
   * @return a PhaseOneResult
   */
  public PhaseOneResult process(CompilationUnitTree root, UnderlyingAST underlyingAST) {
    // TODO: Isn't this costly? Is there no cache we can reuse?
    TreePath bodyPath = trees.getPath(root, underlyingAST.getCode());
    assert bodyPath != null;
    return process(bodyPath, underlyingAST);
  }

  /**
   * Perform any actions required when CFG translation creates a new Tree that is not part of the
   * original AST.
   *
   * @param tree the newly created Tree
   */
  public void handleArtificialTree(Tree tree) {}

  /**
   * Returns the current path for the tree currently being scanned.
   *
   * @return the current path
   */
  public TreePath getCurrentPath() {
    return path;
  }

  @Override
  public Node scan(Tree tree, Void p) {
    if (tree == null) {
      return null;
    }

    TreePath prev = path;
    @SuppressWarnings("interning:not.interned") // Looking for exact match.
    boolean treeIsLeaf = path.getLeaf() != tree;
    if (treeIsLeaf) {
      path = new TreePath(path, tree);
    }
    try {
      // TODO: use JCP to add version-specific behavior
      if (SystemUtil.jreVersion >= 14) {
        // Must use String comparison to support compiling on JDK 11 and earlier.
        // Features added between JDK 12 and JDK 17 inclusive.
        switch (tree.getKind().name()) {
          case "BINDING_PATTERN":
            return visitBindingPattern17(path.getLeaf(), p);
          case "SWITCH_EXPRESSION":
            return visitSwitchExpression17(tree, p);
          case "YIELD":
            return visitYield17(tree, p);
          case "DECONSTRUCTION_PATTERN":
            return visitDeconstructionPattern21(tree, p);
          case "ANY_PATTERN":
            return visitAnyPattern22(tree, p);
          default:
            // fall through to generic behavior
        }
      }

      return tree.accept(this, p);
    } finally {
      path = prev;
    }
  }

  /**
   * Visit a AnyPatternTree.
   *
   * @param anyPatternTree an AnyPatternTree, typed as Tree so the Checker Framework compiles under
   *     JDK 21 and earlier
   * @param unused an unused parameter
   * @return the result of visiting the tree
   */
  private Node visitAnyPattern22(Tree anyPatternTree, Void unused) {
    AnyPatternNode anyPatternNode =
        new AnyPatternNode(TreeUtils.typeOf(anyPatternTree), anyPatternTree);
    extendWithNode(anyPatternNode);
    return anyPatternNode;
  }

  /**
   * Visit a SwitchExpressionTree.
   *
   * @param yieldTree a YieldTree, typed as Tree to be backward-compatible
   * @param p parameter
   * @return the result of visiting the switch expression tree
   */
  public Node visitYield17(Tree yieldTree, Void p) {
    ExpressionTree resultExpression = YieldUtils.getValue(yieldTree);
    switchBuilder.buildSwitchExpressionResult(resultExpression);
    return null;
  }

  /**
   * Visit a SwitchExpressionTree.
   *
   * @param switchExpressionTree a SwitchExpressionTree, typed as Tree to be backward-compatible
   * @param p parameter
   * @return the result of visiting the switch expression tree
   */
  public Node visitSwitchExpression17(Tree switchExpressionTree, Void p) {
    SwitchBuilder oldSwitchBuilder = switchBuilder;
    switchBuilder = new SwitchBuilder(switchExpressionTree);
    Node result = switchBuilder.build();
    switchBuilder = oldSwitchBuilder;
    return result;
  }

  /**
   * Visit a BindingPatternTree.
   *
   * @param bindingPatternTree a BindingPatternTree, typed as Tree to be backward-compatible
   * @param p parameter
   * @return the result of visiting the binding pattern tree
   */
  public Node visitBindingPattern17(Tree bindingPatternTree, Void p) {
    ClassTree enclosingClass = TreePathUtil.enclosingClass(getCurrentPath());
    TypeElement classElem = TreeUtils.elementFromDeclaration(enclosingClass);
    Node receiver = new ImplicitThisNode(classElem.asType());
    VariableTree varTree = BindingPatternUtils.getVariable(bindingPatternTree);
    VariableDeclarationNode variableDeclarationNode = new VariableDeclarationNode(varTree);
    extendWithNode(variableDeclarationNode);
    LocalVariableNode varNode = new LocalVariableNode(varTree, receiver);
    extendWithNode(varNode);
    return varNode;
  }

  /**
   * Visit a DeconstructionPatternTree.
   *
   * @param deconstructionPatternTree a DeconstructionPatternTree, typed as Tree so the Checker
   *     Framework compiles under JDK 20 and earlier
   * @param p an unused parameter
   * @return the result of visiting the tree
   */
  public Node visitDeconstructionPattern21(Tree deconstructionPatternTree, Void p) {
    List<? extends Tree> nestedPatternTrees =
        DeconstructionPatternUtils.getNestedPatterns(deconstructionPatternTree);
    List<Node> nestedPatterns = new ArrayList<>(nestedPatternTrees.size());
    for (Tree pattern : nestedPatternTrees) {
      nestedPatterns.add(scan(pattern, p));
    }
    DeconstructorPatternNode dcpN =
        new DeconstructorPatternNode(
            TreeUtils.typeOf(deconstructionPatternTree), deconstructionPatternTree, nestedPatterns);
    extendWithNode(dcpN);
    return dcpN;
  }

  /* --------------------------------------------------------- */
  /* Nodes and Labels Management */
  /* --------------------------------------------------------- */

  /**
   * Add a node to the lookup map if it not already present.
   *
   * @param node the node to add to the lookup map
   */
  protected void addToLookupMap(Node node) {
    Tree tree = node.getTree();
    if (tree == null) {
      return;
    }
    Set<Node> existing = treeToCfgNodes.get(tree);
    if (existing == null) {
      Set<Node> newSet = new IdentityArraySet<Node>(1);
      newSet.add(node);
      treeToCfgNodes.put(tree, newSet);
    } else {
      existing.add(node);
    }

    Tree enclosingParens = parenMapping.get(tree);
    while (enclosingParens != null) {
      Set<Node> exp =
          treeToCfgNodes.computeIfAbsent(enclosingParens, k -> new IdentityArraySet<>(1));
      exp.add(node);
      enclosingParens = parenMapping.get(enclosingParens);
    }
  }

  /**
   * Add a node in the post-conversion lookup map. The node should refer to a Tree and that Tree
   * should already be in the pre-conversion lookup map. This method is used to update the Tree-Node
   * mapping with conversion nodes.
   *
   * @param node the node to add to the lookup map
   */
  protected void addToConvertedLookupMap(Node node) {
    Tree tree = node.getTree();
    addToConvertedLookupMap(tree, node);
  }

  /**
   * Add a node in the post-conversion lookup map. The tree argument should already be in the
   * pre-conversion lookup map. This method is used to update the Tree-Node mapping with conversion
   * nodes.
   *
   * @param tree the tree used as a key in the map
   * @param node the node to add to the lookup map
   */
  protected void addToConvertedLookupMap(Tree tree, Node node) {
    assert tree != null;
    assert treeToCfgNodes.containsKey(tree);
    Set<Node> existing = treeToConvertedCfgNodes.get(tree);
    if (existing == null) {
      Set<Node> newSet = new IdentityArraySet<>(1);
      newSet.add(node);
      treeToConvertedCfgNodes.put(tree, newSet);
    } else {
      existing.add(node);
    }
  }

  /**
   * Extend the list of extended nodes with a node.
   *
   * @param node the node to add
   */
  protected void extendWithNode(Node node) {
    addToLookupMap(node);
    extendWithExtendedNode(new NodeHolder(node));
  }

  /**
   * Extend the list of extended nodes with a node, where {@code node} might throw the exception
   * {@code cause}.
   *
   * @param node the node to add
   * @param cause an exception that the node might throw
   * @return the node holder
   */
  protected NodeWithExceptionsHolder extendWithNodeWithException(Node node, TypeMirror cause) {
    addToLookupMap(node);
    return extendWithNodeWithExceptions(node, Collections.singleton(cause));
  }

  /**
   * Extend the list of extended nodes with a node, where {@code node} might throw any of the
   * exceptions in {@code causes}.
   *
   * @param node the node to add
   * @param causes the set of exceptions that the node might throw
   * @return the node holder
   */
  protected NodeWithExceptionsHolder extendWithNodeWithExceptions(
      Node node, Set<TypeMirror> causes) {
    addToLookupMap(node);
    Map<TypeMirror, Set<Label>> exceptions = new ArrayMap<>(causes.size());
    for (TypeMirror cause : causes) {
      exceptions.put(cause, tryStack.possibleLabels(cause));
    }
    NodeWithExceptionsHolder exNode = new NodeWithExceptionsHolder(node, exceptions);
    extendWithExtendedNode(exNode);
    return exNode;
  }

  /**
   * Extend a list of extended nodes with a ClassName node.
   *
   * <p>Evaluating a class literal kicks off class loading (JLS 15.8.2) which can fail and throw one
   * of the specified subclasses of a LinkageError or an OutOfMemoryError (JLS 12.2.1).
   *
   * @param node the ClassName node to add
   * @return the node holder
   */
  protected NodeWithExceptionsHolder extendWithClassNameNode(ClassNameNode node) {
    Set<TypeMirror> thrownSet = new ArraySet<>(4);
    if (classCircularityErrorType != null) {
      thrownSet.add(classCircularityErrorType);
    }
    if (classFormatErrorType != null) {
      thrownSet.add(classFormatErrorType);
    }
    if (noClassDefFoundErrorType != null) {
      thrownSet.add(noClassDefFoundErrorType);
    }
    if (outOfMemoryErrorType != null) {
      thrownSet.add(outOfMemoryErrorType);
    }

    return extendWithNodeWithExceptions(node, thrownSet);
  }

  /**
   * Insert {@code node} after {@code pred} in the list of extended nodes, or append to the list if
   * {@code pred} is not present.
   *
   * @param node the node to add
   * @param pred the desired predecessor of node
   * @return the node holder
   */
  protected <T extends Node> T insertNodeAfter(T node, Node pred) {
    addToLookupMap(node);
    insertExtendedNodeAfter(new NodeHolder(node), pred);
    return node;
  }

  /**
   * Insert a {@code node} that might throw the exceptions in {@code causes} after {@code pred} in
   * the list of extended nodes, or append to the list if {@code pred} is not present.
   *
   * @param node the node to add
   * @param causes the set of exceptions that the node might throw
   * @param pred the desired predecessor of node
   * @return the node holder
   */
  protected NodeWithExceptionsHolder insertNodeWithExceptionsAfter(
      Node node, Set<TypeMirror> causes, Node pred) {
    addToLookupMap(node);
    Map<TypeMirror, Set<Label>> exceptions = new ArrayMap<>(causes.size());
    for (TypeMirror cause : causes) {
      exceptions.put(cause, tryStack.possibleLabels(cause));
    }
    NodeWithExceptionsHolder exNode = new NodeWithExceptionsHolder(node, exceptions);
    insertExtendedNodeAfter(exNode, pred);
    return exNode;
  }

  /**
   * Extend the list of extended nodes with an extended node.
   *
   * @param n the extended node
   */
  protected void extendWithExtendedNode(ExtendedNode n) {
    nodeList.add(n);
  }

  /**
   * Insert {@code n} after the node {@code pred} in the list of extended nodes, or append {@code n}
   * if {@code pred} is not present.
   *
   * @param n the extended node
   * @param pred the desired predecessor
   */
  @SuppressWarnings("ModifyCollectionInEnhancedForLoop")
  protected void insertExtendedNodeAfter(ExtendedNode n, @FindDistinct Node pred) {
    int index = -1;
    for (int i = 0; i < nodeList.size(); i++) {
      ExtendedNode inList = nodeList.get(i);
      if (inList instanceof NodeHolder || inList instanceof NodeWithExceptionsHolder) {
        if (inList.getNode() == pred) {
          index = i;
          break;
        }
      }
    }
    if (index != -1) {
      nodeList.add(index + 1, n);
      // update bindings
      for (Map.Entry<Label, Integer> e : bindings.entrySet()) {
        if (e.getValue() >= index + 1) {
          bindings.put(e.getKey(), e.getValue() + 1);
        }
      }
      // update leaders
      Set<Integer> oldLeaders = new HashSet<>(leaders);
      leaders.clear();
      for (Integer l : oldLeaders) {
        if (l >= index + 1) {
          leaders.add(l + 1);
        } else {
          leaders.add(l);
        }
      }
    } else {
      nodeList.add(n);
    }
  }

  /**
   * Add the label {@code l} to the extended node that will be placed next in the sequence.
   *
   * @param l the node to add to the forthcoming extended node
   */
  protected void addLabelForNextNode(Label l) {
    if (bindings.containsKey(l)) {
      throw new BugInCF("bindings already contains key %s: %s", l, bindings);
    }
    leaders.add(nodeList.size());
    bindings.put(l, nodeList.size());
  }

  /* --------------------------------------------------------- */
  /* Utility Methods */
  /* --------------------------------------------------------- */

  /** The UID for the next unique name. */
  protected long uid = 0;

  /**
   * Returns a unique name starting with {@code prefix}.
   *
   * @param prefix the prefix of the unique name
   * @return a unique name starting with {@code prefix}
   */
  protected String uniqueName(String prefix) {
    return prefix + "#num" + uid++;
  }

  /**
   * If the input node is an unboxed primitive type, insert a call to the appropriate valueOf
   * method, otherwise leave it alone.
   *
   * @param node in input node
   * @return a Node representing the boxed version of the input, which may simply be the input node
   */
  protected Node box(Node node) {
    // For boxing conversion, see JLS 5.1.7
    if (TypesUtils.isPrimitive(node.getType())) {
      PrimitiveType primitive = types.getPrimitiveType(node.getType().getKind());
      TypeMirror boxedType = types.getDeclaredType(types.boxedClass(primitive));

      TypeElement boxedElement = (TypeElement) ((DeclaredType) boxedType).asElement();
      IdentifierTree classTree = treeBuilder.buildClassUse(boxedElement);
      handleArtificialTree(classTree);
      // No need to handle possible errors from evaluating a class literal here
      // since this is synthetic code that can't fail.
      ClassNameNode className = new ClassNameNode(classTree);
      className.setInSource(false);
      insertNodeAfter(className, node);

      MemberSelectTree valueOfSelect = treeBuilder.buildValueOfMethodAccess(classTree);
      handleArtificialTree(valueOfSelect);
      MethodAccessNode valueOfAccess = new MethodAccessNode(valueOfSelect, className);
      valueOfAccess.setInSource(false);
      insertNodeAfter(valueOfAccess, className);

      MethodInvocationTree valueOfCall =
          treeBuilder.buildMethodInvocation(valueOfSelect, (ExpressionTree) node.getTree());
      handleArtificialTree(valueOfCall);
      Node boxed =
          new MethodInvocationNode(
              valueOfCall, valueOfAccess, Collections.singletonList(node), getCurrentPath());
      boxed.setInSource(false);
      // Add Throwable to account for unchecked exceptions
      addToConvertedLookupMap(node.getTree(), boxed);
      insertNodeWithExceptionsAfter(boxed, uncheckedExceptionTypes, valueOfAccess);
      return boxed;
    } else {
      return node;
    }
  }

  /**
   * If the input node is a boxed type, unbox it, otherwise leave it alone.
   *
   * @param node in input node
   * @return a Node representing the unboxed version of the input, which may simply be the input
   *     node
   */
  protected Node unbox(Node node) {
    if (TypesUtils.isBoxedPrimitive(node.getType())) {

      MemberSelectTree primValueSelect = treeBuilder.buildPrimValueMethodAccess(node.getTree());
      handleArtificialTree(primValueSelect);
      MethodAccessNode primValueAccess = new MethodAccessNode(primValueSelect, node);
      primValueAccess.setInSource(false);
      // Method access may throw NullPointerException
      insertNodeWithExceptionsAfter(
          primValueAccess, Collections.singleton(nullPointerExceptionType), node);

      MethodInvocationTree primValueCall = treeBuilder.buildMethodInvocation(primValueSelect);
      handleArtificialTree(primValueCall);
      Node unboxed =
          new MethodInvocationNode(
              primValueCall, primValueAccess, Collections.emptyList(), getCurrentPath());
      unboxed.setInSource(false);

      // Add Throwable to account for unchecked exceptions
      addToConvertedLookupMap(node.getTree(), unboxed);
      insertNodeWithExceptionsAfter(unboxed, uncheckedExceptionTypes, primValueAccess);
      return unboxed;
    } else {
      return node;
    }
  }

  private TreeInfo getTreeInfo(Tree tree) {
    TypeMirror type = TreeUtils.typeOf(tree);
    boolean boxed = TypesUtils.isBoxedPrimitive(type);
    TypeMirror unboxedType = boxed ? types.unboxedType(type) : type;

    boolean bool = TypesUtils.isBooleanType(type);
    boolean numeric = TypesUtils.isNumeric(unboxedType);

    return new TreeInfo() {
      @Override
      public boolean isNumeric() {
        return numeric;
      }

      @Override
      public boolean isBoxed() {
        return boxed;
      }

      @Override
      public boolean isBoolean() {
        return bool;
      }

      @Override
      public TypeMirror unboxedType() {
        return unboxedType;
      }
    };
  }

  /**
   * Returns the unboxed tree if necessary, as described in JLS 5.1.8.
   *
   * @return the unboxed tree if necessary, as described in JLS 5.1.8
   */
  private Node unboxAsNeeded(Node node, boolean boxed) {
    return boxed ? unbox(node) : node;
  }

  /**
   * Convert the input node to String type, if it isn't already.
   *
   * @param node an input node
   * @return a Node with the value promoted to String, which may be the input node
   */
  protected Node stringConversion(Node node) {
    // For string conversion, see JLS 5.1.11
    if (!TypesUtils.isString(node.getType())) {
      Node converted = new StringConversionNode(node.getTree(), node, stringType);
      addToConvertedLookupMap(converted);
      insertNodeAfter(converted, node);
      return converted;
    } else {
      return node;
    }
  }

  /**
   * Perform unary numeric promotion on the input node.
   *
   * @param node a node producing a value of numeric primitive or boxed type
   * @return a Node with the value promoted to the int, long, float, or double; may return be the
   *     input node
   */
  protected Node unaryNumericPromotion(Node node) {
    // For unary numeric promotion, see JLS 5.6.1
    node = unbox(node);

    switch (node.getType().getKind()) {
      case BYTE:
      case CHAR:
      case SHORT:
        {
          TypeMirror intType = types.getPrimitiveType(TypeKind.INT);
          Node widened = new WideningConversionNode(node.getTree(), node, intType);
          addToConvertedLookupMap(widened);
          insertNodeAfter(widened, node);
          return widened;
        }
      default:
        // Nothing to do.
        break;
    }

    return node;
  }

  /**
   * Returns true if the argument type is a numeric primitive or a boxed numeric primitive and false
   * otherwise.
   */
  protected boolean isNumericOrBoxed(TypeMirror type) {
    if (TypesUtils.isBoxedPrimitive(type)) {
      type = types.unboxedType(type);
    }
    return TypesUtils.isNumeric(type);
  }

  /**
   * Compute the type to which two numeric types must be promoted before performing a binary numeric
   * operation on them. The input types must both be numeric and the output type is primitive.
   *
   * @param left the type of the left operand
   * @param right the type of the right operand
   * @return a TypeMirror representing the binary numeric promoted type
   */
  protected TypeMirror binaryPromotedType(TypeMirror left, TypeMirror right) {
    if (!left.getKind().isPrimitive()) {
      if (TypesUtils.isCapturedTypeVariable(left)) {
        // This doesn't seem legal according to the JLS, but javac accepts it.
        left = types.unboxedType(TypesUtils.upperBound(left));
      } else {
        left = types.unboxedType(left);
      }
    }

    if (!right.getKind().isPrimitive()) {
      if (TypesUtils.isCapturedTypeVariable(right)) {
        // This doesn't seem legal according to the JLS, but javac accepts it.
        right = types.unboxedType(TypesUtils.upperBound(right));
      } else {
        right = types.unboxedType(right);
      }
    }

    TypeKind promotedTypeKind = TypeKindUtils.widenedNumericType(left, right);
    return types.getPrimitiveType(promotedTypeKind);
  }

  /**
   * Perform binary numeric promotion on the input node to make it match the expression type.
   *
   * @param node a node producing a value of numeric primitive or boxed type
   * @param exprType the type to promote the value to
   * @return a Node with the value promoted to the exprType, which may be the input node
   */
  protected Node binaryNumericPromotion(Node node, TypeMirror exprType) {
    // For binary numeric promotion, see JLS 5.6.2
    node = unbox(node);

    if (!types.isSameType(node.getType(), exprType)) {
      Node widened = new WideningConversionNode(node.getTree(), node, exprType);
      addToConvertedLookupMap(widened);
      insertNodeAfter(widened, node);
      return widened;
    } else {
      return node;
    }
  }

  /**
   * Perform widening primitive conversion on the input node to make it match the destination type.
   *
   * @param node a node producing a value of numeric primitive type
   * @param destType the type to widen the value to
   * @return a Node with the value widened to the exprType, which may be the input node
   */
  protected Node widen(Node node, TypeMirror destType) {
    // For widening conversion, see JLS 5.1.2
    assert TypesUtils.isPrimitive(node.getType()) && TypesUtils.isPrimitive(destType)
        : "widening must be applied to primitive types";
    if (types.isSubtype(node.getType(), destType) && !types.isSameType(node.getType(), destType)) {
      Node widened = new WideningConversionNode(node.getTree(), node, destType);
      addToConvertedLookupMap(widened);
      insertNodeAfter(widened, node);
      return widened;
    } else {
      return node;
    }
  }

  /**
   * Perform narrowing conversion on the input node to make it match the destination type.
   *
   * @param node a node producing a value of numeric primitive type
   * @param destType the type to narrow the value to
   * @return a Node with the value narrowed to the exprType, which may be the input node
   */
  protected Node narrow(Node node, TypeMirror destType) {
    // For narrowing conversion, see JLS 5.1.3
    assert TypesUtils.isPrimitive(node.getType()) && TypesUtils.isPrimitive(destType)
        : "narrowing must be applied to primitive types";
    if (types.isSubtype(destType, node.getType()) && !types.isSameType(destType, node.getType())) {
      Node narrowed = new NarrowingConversionNode(node.getTree(), node, destType);
      addToConvertedLookupMap(narrowed);
      insertNodeAfter(narrowed, node);
      return narrowed;
    } else {
      return node;
    }
  }

  /**
   * Perform narrowing conversion and optionally boxing conversion on the input node to make it
   * match the destination type.
   *
   * @param node a node producing a value of numeric primitive type
   * @param destType the type to narrow the value to (possibly boxed)
   * @return a Node with the value narrowed and boxed to the destType, which may be the input node
   */
  protected Node narrowAndBox(Node node, TypeMirror destType) {
    if (TypesUtils.isBoxedPrimitive(destType)) {
      return box(narrow(node, types.unboxedType(destType)));
    } else {
      return narrow(node, destType);
    }
  }

  /**
   * Returns true if a conversion from the type of the node to varType requires narrowing.
   *
   * @param varType the type of a variable (or general LHS) to be converted to
   * @param node a node whose value is being converted
   * @return true if this conversion requires narrowing to succeed
   */
  protected boolean conversionRequiresNarrowing(TypeMirror varType, Node node) {
    // Narrowing is restricted to cases where the left hand side is byte, char, short or Byte,
    // Char, Short and the right hand side is a constant.
    TypeMirror unboxedVarType =
        TypesUtils.isBoxedPrimitive(varType) ? types.unboxedType(varType) : varType;
    TypeKind unboxedVarKind = unboxedVarType.getKind();
    boolean isLeftNarrowableTo =
        unboxedVarKind == TypeKind.BYTE
            || unboxedVarKind == TypeKind.SHORT
            || unboxedVarKind == TypeKind.CHAR;
    boolean isRightConstant = node instanceof ValueLiteralNode;
    return isLeftNarrowableTo && isRightConstant;
  }

  /**
   * Assignment conversion and method invocation conversion are almost identical, except that
   * assignment conversion allows narrowing. We factor out the common logic here.
   *
   * @param node a Node producing a value
   * @param varType the type of a variable
   * @param contextAllowsNarrowing if true, allow narrowing (for assignment conversion) (for method
   *     invocation conversion)
   * @return a Node with the value converted to the type of the variable, which may be the input
   *     node itself
   */
  protected Node commonConvert(Node node, TypeMirror varType, boolean contextAllowsNarrowing) {
    // For assignment conversion, see JLS 5.2
    // For method invocation conversion, see JLS 5.3

    // Check for identical types or "identity conversion"
    TypeMirror nodeType = node.getType();
    boolean isSameType = types.isSameType(nodeType, varType);
    if (isSameType) {
      return node;
    }

    boolean isRightNumeric = TypesUtils.isNumeric(nodeType);
    boolean isRightPrimitive = TypesUtils.isPrimitive(nodeType);
    boolean isRightBoxed = TypesUtils.isBoxedPrimitive(nodeType);
    boolean isRightReference = nodeType instanceof ReferenceType;
    boolean isLeftNumeric = TypesUtils.isNumeric(varType);
    boolean isLeftPrimitive = TypesUtils.isPrimitive(varType);
    // boolean isLeftBoxed = TypesUtils.isBoxedPrimitive(varType);
    boolean isLeftReference = varType instanceof ReferenceType;
    boolean isSubtype = types.isSubtype(nodeType, varType);

    if (isRightNumeric && isLeftNumeric && isSubtype) {
      node = widen(node, varType);
    } else if (isRightReference && isLeftReference && isSubtype) {
      // widening reference conversion is a no-op, but if it
      // applies, then later conversions do not.
    } else if (isRightPrimitive && isLeftReference) {
      if (contextAllowsNarrowing && conversionRequiresNarrowing(varType, node)) {
        node = narrowAndBox(node, varType);
      } else {
        node = box(node);
      }
    } else if (isRightBoxed && isLeftPrimitive) {
      node = unbox(node);
      nodeType = node.getType();

      if (types.isSubtype(nodeType, varType) && !types.isSameType(nodeType, varType)) {
        node = widen(node, varType);
      }
    } else if (isRightPrimitive && isLeftPrimitive) {
      if (contextAllowsNarrowing && conversionRequiresNarrowing(varType, node)) {
        node = narrow(node, varType);
      }
    }
    // `node` might have been re-assigned; if `nodeType` is needed, set it again.
    // nodeType = node.getType();

    // TODO: if checkers need to know about null references of
    // a particular type, add logic for them here.

    return node;
  }

  /**
   * Perform assignment conversion so that it can be assigned to a variable of the given type.
   *
   * @param node a Node producing a value
   * @param varType the type of a variable
   * @return a Node with the value converted to the type of the variable, which may be the input
   *     node itself
   */
  protected Node assignConvert(Node node, TypeMirror varType) {
    return commonConvert(node, varType, true);
  }

  /**
   * Perform method invocation conversion so that the node can be passed as a formal parameter of
   * the given type.
   *
   * @param node a Node producing a value
   * @param formalType the type of a formal parameter
   * @return a Node with the value converted to the type of the formal, which may be the input node
   *     itself
   */
  protected Node methodInvocationConvert(Node node, TypeMirror formalType) {
    return commonConvert(node, formalType, false);
  }

  /**
   * Given a method element, its type at the call site, and a list of argument expressions, return a
   * list of {@link Node}s representing the arguments converted for a call of the method. This
   * method applies to both method invocations and constructor calls.
   *
   * @param tree the invocation tree for the call
   * @param method an ExecutableElement representing a method to be called
   * @param methodType an ExecutableType representing the type of the method call; the type must be
   *     viewpoint-adapted to the call
   * @param actualExprs a List of argument expressions to a call
   * @return a List of {@link Node}s representing arguments after conversions required by a call to
   *     this method
   */
  protected List<Node> convertCallArguments(
      ExpressionTree tree,
      ExecutableElement method,
      ExecutableType methodType,
      List<? extends ExpressionTree> actualExprs) {
    // NOTE: It is important to convert one method argument before generating CFG nodes for the
    // next argument, since label binding expects nodes to be generated in execution order.
    // Therefore, this method first determines which conversions need to be applied and then
    // iterates over the actual arguments.
    List<? extends TypeMirror> formals = methodType.getParameterTypes();
    int numFormals = formals.size();

    ArrayList<Node> convertedNodes = new ArrayList<>(numFormals);
    AssertMethodTuple assertMethodTuple = getAssertMethodTuple(method);

    int numActuals = actualExprs.size();
    if (method.isVarArgs()) {
      // Create a new array argument if the actuals outnumber the formals, or if the last
      // actual is not assignable to the last formal.
      int lastArgIndex = numFormals - 1;
      TypeMirror lastParamType = formals.get(lastArgIndex);
      if (numActuals == numFormals
          && types.isAssignable(TreeUtils.typeOf(actualExprs.get(numActuals - 1)), lastParamType)) {
        // Normal call with no array creation, apply method
        // invocation conversion to all arguments.
        for (int i = 0; i < numActuals; i++) {
          Node actualVal = scan(actualExprs.get(i), null);
          if (i == assertMethodTuple.booleanParam) {
            treatMethodAsAssert((MethodInvocationTree) tree, assertMethodTuple, actualVal);
          }
          if (actualVal == null) {
            throw new BugInCF(
                "CFGBuilder: scan returned null for %s [%s]",
                actualExprs.get(i), actualExprs.get(i).getClass());
          }
          convertedNodes.add(methodInvocationConvert(actualVal, formals.get(i)));
        }
      } else {
        assert lastParamType instanceof ArrayType : "variable argument formal must be an array";
        // Apply method invocation conversion to lastArgIndex arguments and use the
        // remaining ones to initialize an array.
        for (int i = 0; i < lastArgIndex; i++) {
          Node actualVal = scan(actualExprs.get(i), null);
          if (i == assertMethodTuple.booleanParam) {
            treatMethodAsAssert((MethodInvocationTree) tree, assertMethodTuple, actualVal);
          }
          convertedNodes.add(methodInvocationConvert(actualVal, formals.get(i)));
        }

        TypeMirror elemType = ((ArrayType) lastParamType).getComponentType();

        List<ExpressionTree> inits = new ArrayList<>(numActuals - lastArgIndex);
        List<Node> initializers = new ArrayList<>(numActuals - lastArgIndex);
        for (int i = lastArgIndex; i < numActuals; i++) {
          inits.add(actualExprs.get(i));
          Node actualVal = scan(actualExprs.get(i), null);
          initializers.add(assignConvert(actualVal, elemType));
        }

        NewArrayTree wrappedVarargs = treeBuilder.buildNewArray(elemType, inits);
        handleArtificialTree(wrappedVarargs);

        Node lastArgument =
            new ArrayCreationNode(
                wrappedVarargs,
                lastParamType,
                /* dimensions= */ Collections.emptyList(),
                initializers);
        extendWithNode(lastArgument);

        convertedNodes.add(lastArgument);
      }
    } else {
      for (int i = 0; i < numActuals; i++) {
        Node actualVal = scan(actualExprs.get(i), null);
        if (i == assertMethodTuple.booleanParam) {
          treatMethodAsAssert((MethodInvocationTree) tree, assertMethodTuple, actualVal);
        }
        convertedNodes.add(methodInvocationConvert(actualVal, formals.get(i)));
      }
    }

    return convertedNodes;
  }

  /**
   * Returns the AssertMethodTuple for {@code method}. If {@code method} is not an assert method,
   * then {@link AssertMethodTuple#NONE} is returned.
   *
   * @param method a method element that might be an assert method
   * @return the AssertMethodTuple for {@code method}
   */
  protected AssertMethodTuple getAssertMethodTuple(ExecutableElement method) {
    AnnotationMirror assertMethodAnno =
        annotationProvider.getDeclAnnotation(method, AssertMethod.class);
    if (assertMethodAnno == null) {
      return AssertMethodTuple.NONE;
    }

    // Dataflow does not require checker-qual.jar to be on the users classpath, so
    // AnnotationUtils.getElementValue(...) cannot be used.

    int booleanParam =
        AnnotationUtils.getElementValueNotOnClasspath(
                assertMethodAnno, "parameter", Integer.class, 1)
            - 1;

    TypeMirror exceptionType =
        AnnotationUtils.getElementValueNotOnClasspath(
            assertMethodAnno, "value", Type.ClassType.class, (Type.ClassType) assertionErrorType);
    boolean isAssertFalse =
        AnnotationUtils.getElementValueNotOnClasspath(
            assertMethodAnno, "isAssertFalse", Boolean.class, false);
    return new AssertMethodTuple(booleanParam, exceptionType, isAssertFalse);
  }

  /** Holds the elements of an {@link AssertMethod} annotation. */
  protected static class AssertMethodTuple {

    /** A tuple representing the lack of an {@link AssertMethodTuple}. */
    protected static final AssertMethodTuple NONE = new AssertMethodTuple(-1, null, false);

    /**
     * 0-based index of the parameter of the expression that is tested by the assert method. (Or -1
     * if this isn't an assert method.)
     */
    public final int booleanParam;

    /** The type of the exception thrown by the assert method. */
    public final TypeMirror exceptionType;

    /** Is this an assert false method? */
    public final boolean isAssertFalse;

    /**
     * Creates an AssertMethodTuple.
     *
     * @param booleanParam 0-based index of the parameter of the expression that is tested by the
     *     assert method
     * @param exceptionType the type of the exception thrown by the assert method
     * @param isAssertFalse is this an assert false method
     */
    public AssertMethodTuple(int booleanParam, TypeMirror exceptionType, boolean isAssertFalse) {
      this.booleanParam = booleanParam;
      this.exceptionType = exceptionType;
      this.isAssertFalse = isAssertFalse;
    }
  }

  /**
   * Convert an operand of a conditional expression to the type of the whole expression.
   *
   * @param node a node occurring as the second or third operand of a conditional expression
   * @param destType the type to promote the value to
   * @return a Node with the value promoted to the destType, which may be the input node
   */
  protected Node conditionalExprPromotion(Node node, TypeMirror destType) {
    // For rules on converting operands of conditional expressions,
    // JLS 15.25
    TypeMirror nodeType = node.getType();

    // If the operand is already the same type as the whole
    // expression, then do nothing.
    if (types.isSameType(nodeType, destType)) {
      return node;
    }

    // If the operand is a primitive and the whole expression is
    // boxed, then apply boxing.
    if (TypesUtils.isPrimitive(nodeType) && TypesUtils.isBoxedPrimitive(destType)) {
      return box(node);
    }

    // If the operand is byte or Byte and the whole expression is
    // short, then convert to short.
    boolean isBoxedPrimitive = TypesUtils.isBoxedPrimitive(nodeType);
    TypeMirror unboxedNodeType = isBoxedPrimitive ? types.unboxedType(nodeType) : nodeType;
    TypeMirror unboxedDestType =
        TypesUtils.isBoxedPrimitive(destType) ? types.unboxedType(destType) : destType;
    if (TypesUtils.isNumeric(unboxedNodeType) && TypesUtils.isNumeric(unboxedDestType)) {
      if (unboxedNodeType.getKind() == TypeKind.BYTE && destType.getKind() == TypeKind.SHORT) {
        if (isBoxedPrimitive) {
          node = unbox(node);
        }
        return widen(node, destType);
      }

      // If the operand is Byte, Short or Character and the whole expression
      // is the unboxed version of it, then apply unboxing.
      TypeKind destKind = destType.getKind();
      if (destKind == TypeKind.BYTE || destKind == TypeKind.CHAR || destKind == TypeKind.SHORT) {
        if (isBoxedPrimitive) {
          return unbox(node);
        } else if (nodeType.getKind() == TypeKind.INT) {
          return narrow(node, destType);
        }
      }

      return binaryNumericPromotion(node, destType);
    }

    // For the final case in JLS 15.25, apply boxing but not lub.
    if (TypesUtils.isPrimitive(nodeType)
        && (destType.getKind() == TypeKind.DECLARED
            || destType.getKind() == TypeKind.UNION
            || destType.getKind() == TypeKind.INTERSECTION)) {
      return box(node);
    }

    return node;
  }

  /**
   * Returns the label {@link Name} of the leaf in the argument path, or null if the leaf is not a
   * labeled statement.
   */
  protected @Nullable Name getLabel(TreePath path) {
    if (path.getParentPath() != null) {
      Tree parent = path.getParentPath().getLeaf();
      if (parent instanceof LabeledStatementTree) {
        return ((LabeledStatementTree) parent).getLabel();
      }
    }
    return null;
  }

  /* --------------------------------------------------------- */
  /* Visitor Methods */
  /* --------------------------------------------------------- */

  @Override
  public Node visitAnnotatedType(AnnotatedTypeTree tree, Void p) {
    return scan(tree.getUnderlyingType(), p);
  }

  @Override
  public Node visitAnnotation(AnnotationTree tree, Void p) {
    throw new BugInCF("AnnotationTree is unexpected in AST to CFG translation");
  }

  @Override
  public MethodInvocationNode visitMethodInvocation(MethodInvocationTree tree, Void p) {

    // see JLS 15.12.4

    // First, compute the receiver, if any (15.12.4.1).
    // Second, evaluate the actual arguments, left to right and possibly some arguments are
    // stored into an array for varargs calls (15.12.4.2).
    // Third, test the receiver, if any, for nullness (15.12.4.4).
    // Fourth, convert the arguments to the type of the formal parameters (15.12.4.5).
    // Fifth, if the method is synchronized, lock the receiving object or class (15.12.4.5).
    ExecutableElement method = TreeUtils.elementFromUse(tree);
    if (method == null) {
      // The method wasn't found, e.g. because of a compilation error.
      return null;
    }

    ExpressionTree methodSelect = tree.getMethodSelect();
    assert TreeUtils.isMethodAccess(methodSelect)
        : "Expected a method access, but got: " + methodSelect;

    List<? extends ExpressionTree> actualExprs = tree.getArguments();

    // Look up method to invoke and possibly throw NullPointerException
    Node receiver = getReceiver(methodSelect);

    MethodAccessNode target = new MethodAccessNode(methodSelect, method, receiver);

    if (ElementUtils.isStatic(method) || receiver instanceof ThisNode) {
      // No NullPointerException can be thrown, use normal node
      extendWithNode(target);
    } else {
      extendWithNodeWithException(target, nullPointerExceptionType);
    }

    List<Node> arguments;
    if (TreeUtils.isEnumSuperCall(tree)) {
      // Don't convert arguments for enum super calls.  The AST contains no actual arguments,
      // while the method element expects two arguments, leading to an exception in
      // convertCallArguments.
      // Since no actual arguments are present in the AST that is being checked, it shouldn't
      // cause any harm to omit the conversions.
      // See also BaseTypeVisitor.visitMethodInvocation and QualifierPolymorphism.annotate.
      arguments = Collections.emptyList();
    } else {
      arguments = convertCallArguments(tree, method, TreeUtils.typeFromUse(tree), actualExprs);
    }

    // TODO: lock the receiver for synchronized methods

    MethodInvocationNode node = new MethodInvocationNode(tree, target, arguments, getCurrentPath());

    ExtendedNode extendedNode = extendWithMethodInvocationNode(method, node);

    /* Check for the TerminatesExecution annotation. */
    boolean terminatesExecution =
        annotationProvider.getDeclAnnotation(method, TerminatesExecution.class) != null;
    if (terminatesExecution) {
      extendedNode.setTerminatesExecution(true);
    }
    return node;
  }

  /**
   * Extends the CFG with a MethodInvocationNode, accounting for potential exceptions thrown by the
   * invocation.
   *
   * @param method the invoked method
   * @param node the invocation
   * @return an ExtendedNode representing the invocation and its possible thrown exceptions
   */
  private ExtendedNode extendWithMethodInvocationNode(
      ExecutableElement method, MethodInvocationNode node) {
    List<? extends TypeMirror> thrownTypes = method.getThrownTypes();
    Set<TypeMirror> thrownSet =
        new LinkedHashSet<>(thrownTypes.size() + uncheckedExceptionTypes.size());
    // Add exceptions explicitly mentioned in the throws clause.
    thrownSet.addAll(thrownTypes);
    // Add types to account for unchecked exceptions
    thrownSet.addAll(uncheckedExceptionTypes);

    return extendWithNodeWithExceptions(node, thrownSet);
  }

  @Override
  public Node visitAssert(AssertTree tree, Void p) {

    // see JLS 14.10

    // If assertions are enabled, then we can just translate the assertion.
    if (assumeAssertionsEnabled || assumeAssertionsEnabledFor(tree)) {
      translateAssertWithAssertionsEnabled(tree);
      return null;
    }

    // If assertions are disabled, then nothing is executed.
    if (assumeAssertionsDisabled) {
      return null;
    }

    // Otherwise, we don't know if assertions are enabled, so we use a
    // variable "ea" and case-split on it. One branch does execute the
    // assertion, while the other assumes assertions are disabled.
    VariableTree ea = getAssertionsEnabledVariable();

    // all necessary labels
    Label assertionEnabled = new Label();
    Label assertionDisabled = new Label();

    extendWithNode(new LocalVariableNode(ea));
    extendWithExtendedNode(new ConditionalJump(assertionEnabled, assertionDisabled));

    // 'then' branch (i.e. check the assertion)
    addLabelForNextNode(assertionEnabled);

    translateAssertWithAssertionsEnabled(tree);

    // 'else' branch
    addLabelForNextNode(assertionDisabled);

    return null;
  }

  /**
   * Should assertions be assumed to be executed for a given {@link AssertTree}? False by default.
   */
  protected boolean assumeAssertionsEnabledFor(AssertTree tree) {
    return false;
  }

  /** The {@link VariableTree} that indicates whether assertions are enabled or not. */
  protected VariableTree ea = null;

  /** Get a synthetic {@link VariableTree} that indicates whether assertions are enabled or not. */
  protected VariableTree getAssertionsEnabledVariable() {
    if (ea == null) {
      String name = uniqueName("assertionsEnabled");
      Element owner = TreePathUtil.findNearestEnclosingElement(getCurrentPath());
      ExpressionTree initializer = null;
      ea =
          treeBuilder.buildVariableDecl(
              types.getPrimitiveType(TypeKind.BOOLEAN), name, owner, initializer);
      handleArtificialTree(ea);
    }
    return ea;
  }

  /**
   * Translates an assertion statement to the correct CFG nodes. The translation assumes that
   * assertions are enabled.
   */
  protected void translateAssertWithAssertionsEnabled(AssertTree tree) {

    // all necessary labels
    Label assertEnd = new Label();
    Label elseEntry = new Label();

    // basic block for the condition
    Node condition = unbox(scan(tree.getCondition(), null));
    ConditionalJump cjump = new ConditionalJump(assertEnd, elseEntry);
    extendWithExtendedNode(cjump);

    // else branch
    Node detail = null;
    addLabelForNextNode(elseEntry);
    if (tree.getDetail() != null) {
      detail = scan(tree.getDetail(), null);
    }
    AssertionErrorNode assertNode =
        new AssertionErrorNode(tree, condition, detail, assertionErrorType);
    extendWithNode(assertNode);
    NodeWithExceptionsHolder exNode =
        extendWithNodeWithException(
            new ThrowNode(null, assertNode, env.getTypeUtils()), assertionErrorType);
    exNode.setTerminatesExecution(true);

    // then branch (nothing happens)
    addLabelForNextNode(assertEnd);
  }

  /**
   * Translates a method marked as {@link AssertMethod} into CFG nodes corresponding to an {@code
   * assert} statement.
   *
   * @param tree the method invocation tree for a method marked as {@link AssertMethod}
   * @param assertMethodTuple the assert method tuple for the method
   * @param condition the boolean expression node for the argument that the method tests
   */
  protected void treatMethodAsAssert(
      MethodInvocationTree tree, AssertMethodTuple assertMethodTuple, Node condition) {

    // all necessary labels
    Label thenLabel = new Label();
    Label elseLabel = new Label();
    ConditionalJump cjump = new ConditionalJump(thenLabel, elseLabel);
    extendWithExtendedNode(cjump);

    addLabelForNextNode(assertMethodTuple.isAssertFalse ? thenLabel : elseLabel);
    AssertionErrorNode assertNode =
        new AssertionErrorNode(tree, condition, null, assertMethodTuple.exceptionType);
    extendWithNode(assertNode);
    NodeWithExceptionsHolder exNode =
        extendWithNodeWithException(
            new ThrowNode(null, assertNode, env.getTypeUtils()), assertMethodTuple.exceptionType);
    exNode.setTerminatesExecution(true);

    addLabelForNextNode(assertMethodTuple.isAssertFalse ? elseLabel : thenLabel);
  }

  @Override
  public Node visitAssignment(AssignmentTree tree, Void p) {

    // see JLS 15.26.1

    AssignmentNode assignmentNode;
    ExpressionTree variable = tree.getVariable();
    TypeMirror varType = TreeUtils.typeOf(variable);

    // case 1: lhs is field access
    if (TreeUtils.isFieldAccess(variable)) {
      // visit receiver
      Node receiver = getReceiver(variable);

      // visit expression
      Node expression = scan(tree.getExpression(), p);
      expression = assignConvert(expression, varType);

      // visit field access (throws null-pointer exception)
      FieldAccessNode target = new FieldAccessNode(variable, receiver);
      target.setLValue();

      Element element = TreeUtils.elementFromUse(variable);
      if (ElementUtils.isStatic(element) || receiver instanceof ThisNode) {
        // No NullPointerException can be thrown, use normal node
        extendWithNode(target);
      } else {
        extendWithNodeWithException(target, nullPointerExceptionType);
      }

      // add assignment node
      assignmentNode = new AssignmentNode(tree, target, expression);
      extendWithNode(assignmentNode);
    }

    // case 2: lhs is not a field access
    else {
      Node target = scan(variable, p);
      target.setLValue();

      assignmentNode = translateAssignment(tree, target, tree.getExpression());
    }

    return assignmentNode;
  }

  /** Translate an assignment. */
  protected AssignmentNode translateAssignment(Tree tree, Node target, ExpressionTree rhs) {
    Node expression = scan(rhs, null);
    return translateAssignment(tree, target, expression);
  }

  /** Translate an assignment where the RHS has already been scanned. */
  protected AssignmentNode translateAssignment(Tree tree, Node target, Node expression) {
    assert tree instanceof AssignmentTree || tree instanceof VariableTree;
    target.setLValue();
    expression = assignConvert(expression, target.getType());
    AssignmentNode assignmentNode = new AssignmentNode(tree, target, expression);
    extendWithNode(assignmentNode);
    return assignmentNode;
  }

  /**
   * Note 1: Requires {@code tree} to be a field or method access tree.
   *
   * <p>Note 2: Visits the receiver and adds all necessary blocks to the CFG.
   *
   * @param tree the field or method access tree containing the receiver: one of
   *     MethodInvocationTree, AssignmentTree, or IdentifierTree
   * @return the receiver of the field or method access
   */
  private Node getReceiver(ExpressionTree tree) {
    assert TreeUtils.isFieldAccess(tree) || TreeUtils.isMethodAccess(tree);
    if (tree instanceof MemberSelectTree) {
      // `tree` has an explicit receiver.
      MemberSelectTree mtree = (MemberSelectTree) tree;
      return scan(mtree.getExpression(), null);
    } else {
      // `tree` lacks an explicit reciever.
      Element ele = TreeUtils.elementFromUse(tree);
      TypeElement declaringClass = ElementUtils.enclosingTypeElement(ele);
      TypeMirror typeOfDeclaringClass = ElementUtils.getType(declaringClass);
      if (ElementUtils.isStatic(ele)) {
        ClassNameNode node = new ClassNameNode(typeOfDeclaringClass, declaringClass);
        extendWithClassNameNode(node);
        return node;
      } else {
        ClassTree classTree = TreePathUtil.enclosingClass(getCurrentPath());
        TypeElement classEle = TreeUtils.elementFromDeclaration(classTree);

        // An implicit receiver is the first enclosing type that is a subtype of the type where the
        // element is declared.
        while (!TypesUtils.isErasedSubtype(classEle.asType(), typeOfDeclaringClass, types)) {
          Element enclosing = classEle.getEnclosingElement();
          while (!(enclosing instanceof TypeElement)) {
            enclosing = enclosing.getEnclosingElement();
          }
          classEle = (TypeElement) enclosing;
        }
        Node node = new ImplicitThisNode(classEle.asType());
        extendWithNode(node);
        return node;
      }
    }
  }

  /**
   * Map an operation with assignment to the corresponding operation without assignment.
   *
   * @param kind a Tree.Kind representing an operation with assignment
   * @return the Tree.Kind for the same operation without assignment
   */
  protected Tree.Kind withoutAssignment(Tree.Kind kind) {
    switch (kind) {
      case DIVIDE_ASSIGNMENT:
        return Tree.Kind.DIVIDE;
      case MULTIPLY_ASSIGNMENT:
        return Tree.Kind.MULTIPLY;
      case REMAINDER_ASSIGNMENT:
        return Tree.Kind.REMAINDER;
      case MINUS_ASSIGNMENT:
        return Tree.Kind.MINUS;
      case PLUS_ASSIGNMENT:
        return Tree.Kind.PLUS;
      case LEFT_SHIFT_ASSIGNMENT:
        return Tree.Kind.LEFT_SHIFT;
      case RIGHT_SHIFT_ASSIGNMENT:
        return Tree.Kind.RIGHT_SHIFT;
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
        return Tree.Kind.UNSIGNED_RIGHT_SHIFT;
      case AND_ASSIGNMENT:
        return Tree.Kind.AND;
      case OR_ASSIGNMENT:
        return Tree.Kind.OR;
      case XOR_ASSIGNMENT:
        return Tree.Kind.XOR;
      default:
        return Tree.Kind.ERRONEOUS;
    }
  }

  @Override
  public Node visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
    // According the JLS 15.26.2, E1 op= E2 is equivalent to
    // E1 = (T) ((E1) op (E2)), where T is the type of E1,
    // except that E1 is evaluated only once.
    //

    Tree.Kind kind = tree.getKind();
    switch (kind) {
      case DIVIDE_ASSIGNMENT:
      case MULTIPLY_ASSIGNMENT:
      case REMAINDER_ASSIGNMENT:
        {
          // see JLS 15.17 and 15.26.2
          Node targetLHS = scan(tree.getVariable(), p);
          Node value = scan(tree.getExpression(), p);

          TypeMirror exprType = TreeUtils.typeOf(tree);
          TypeMirror leftType = TreeUtils.typeOf(tree.getVariable());
          TypeMirror rightType = TreeUtils.typeOf(tree.getExpression());
          TypeMirror promotedType = binaryPromotedType(leftType, rightType);
          Node targetRHS = binaryNumericPromotion(targetLHS, promotedType);
          value = binaryNumericPromotion(value, promotedType);

          BinaryTree operTree =
              treeBuilder.buildBinary(
                  promotedType, withoutAssignment(kind), tree.getVariable(), tree.getExpression());
          handleArtificialTree(operTree);
          Node operNode;
          if (kind == Tree.Kind.MULTIPLY_ASSIGNMENT) {
            operNode = new NumericalMultiplicationNode(operTree, targetRHS, value);
          } else if (kind == Tree.Kind.DIVIDE_ASSIGNMENT) {
            if (TypesUtils.isIntegralPrimitive(exprType)) {
              operNode = new IntegerDivisionNode(operTree, targetRHS, value);
              extendWithNodeWithException(operNode, arithmeticExceptionType);
            } else {
              operNode = new FloatingDivisionNode(operTree, targetRHS, value);
            }
          } else {
            assert kind == Tree.Kind.REMAINDER_ASSIGNMENT;
            if (TypesUtils.isIntegralPrimitive(exprType)) {
              operNode = new IntegerRemainderNode(operTree, targetRHS, value);
              extendWithNodeWithException(operNode, arithmeticExceptionType);
            } else {
              operNode = new FloatingRemainderNode(operTree, targetRHS, value);
            }
          }
          extendWithNode(operNode);

          TypeMirror castType = TypeAnnotationUtils.unannotatedType(leftType);
          TypeCastTree castTree = treeBuilder.buildTypeCast(castType, operTree);
          handleArtificialTree(castTree);
          TypeCastNode castNode = new TypeCastNode(castTree, operNode, castType, types);
          castNode.setInSource(false);
          extendWithNode(castNode);

          AssignmentNode assignNode = new AssignmentNode(tree, targetLHS, castNode);
          extendWithNode(assignNode);
          return assignNode;
        }

      case MINUS_ASSIGNMENT:
      case PLUS_ASSIGNMENT:
        {
          // see JLS 15.18 and 15.26.2

          Node targetLHS = scan(tree.getVariable(), p);
          Node value = scan(tree.getExpression(), p);

          TypeMirror leftType = TreeUtils.typeOf(tree.getVariable());
          TypeMirror rightType = TreeUtils.typeOf(tree.getExpression());

          if (TypesUtils.isString(leftType) || TypesUtils.isString(rightType)) {
            assert (kind == Tree.Kind.PLUS_ASSIGNMENT);
            Node targetRHS = stringConversion(targetLHS);
            value = stringConversion(value);
            BinaryTree operTree =
                treeBuilder.buildBinary(
                    leftType, withoutAssignment(kind), tree.getVariable(), tree.getExpression());
            handleArtificialTree(operTree);
            Node operNode = new StringConcatenateNode(operTree, targetRHS, value);
            extendWithNode(operNode);
            AssignmentNode assignNode = new AssignmentNode(tree, targetLHS, operNode);
            extendWithNode(assignNode);
            return assignNode;
          } else {
            TypeMirror promotedType = binaryPromotedType(leftType, rightType);
            Node targetRHS = binaryNumericPromotion(targetLHS, promotedType);
            value = binaryNumericPromotion(value, promotedType);

            BinaryTree operTree =
                treeBuilder.buildBinary(
                    promotedType,
                    withoutAssignment(kind),
                    tree.getVariable(),
                    tree.getExpression());
            handleArtificialTree(operTree);
            Node operNode;
            if (kind == Tree.Kind.PLUS_ASSIGNMENT) {
              operNode = new NumericalAdditionNode(operTree, targetRHS, value);
            } else {
              assert kind == Tree.Kind.MINUS_ASSIGNMENT;
              operNode = new NumericalSubtractionNode(operTree, targetRHS, value);
            }
            extendWithNode(operNode);

            TypeMirror castType = TypeAnnotationUtils.unannotatedType(leftType);
            TypeCastTree castTree = treeBuilder.buildTypeCast(castType, operTree);
            handleArtificialTree(castTree);
            TypeCastNode castNode = new TypeCastNode(castTree, operNode, castType, types);
            castNode.setInSource(false);
            extendWithNode(castNode);

            // Map the compound assignment tree to an assignment node, which
            // will have the correct type.
            AssignmentNode assignNode = new AssignmentNode(tree, targetLHS, castNode);
            extendWithNode(assignNode);
            return assignNode;
          }
        }

      case LEFT_SHIFT_ASSIGNMENT:
      case RIGHT_SHIFT_ASSIGNMENT:
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
        {
          // see JLS 15.19 and 15.26.2
          Node targetLHS = scan(tree.getVariable(), p);
          Node value = scan(tree.getExpression(), p);

          TypeMirror leftType = TreeUtils.typeOf(tree.getVariable());

          Node targetRHS = unaryNumericPromotion(targetLHS);
          value = unaryNumericPromotion(value);

          BinaryTree operTree =
              treeBuilder.buildBinary(
                  leftType, withoutAssignment(kind), tree.getVariable(), tree.getExpression());
          handleArtificialTree(operTree);
          Node operNode;
          if (kind == Tree.Kind.LEFT_SHIFT_ASSIGNMENT) {
            operNode = new LeftShiftNode(operTree, targetRHS, value);
          } else if (kind == Tree.Kind.RIGHT_SHIFT_ASSIGNMENT) {
            operNode = new SignedRightShiftNode(operTree, targetRHS, value);
          } else {
            assert kind == Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT;
            operNode = new UnsignedRightShiftNode(operTree, targetRHS, value);
          }
          extendWithNode(operNode);

          TypeMirror castType = TypeAnnotationUtils.unannotatedType(leftType);
          TypeCastTree castTree = treeBuilder.buildTypeCast(castType, operTree);
          handleArtificialTree(castTree);
          TypeCastNode castNode = new TypeCastNode(castTree, operNode, castType, types);
          castNode.setInSource(false);
          extendWithNode(castNode);

          AssignmentNode assignNode = new AssignmentNode(tree, targetLHS, castNode);
          extendWithNode(assignNode);
          return assignNode;
        }

      case AND_ASSIGNMENT:
      case OR_ASSIGNMENT:
      case XOR_ASSIGNMENT:
        // see JLS 15.22
        Node targetLHS = scan(tree.getVariable(), p);
        Node value = scan(tree.getExpression(), p);

        TypeMirror leftType = TreeUtils.typeOf(tree.getVariable());
        TypeMirror rightType = TreeUtils.typeOf(tree.getExpression());

        Node targetRHS = null;
        if (isNumericOrBoxed(leftType) && isNumericOrBoxed(rightType)) {
          TypeMirror promotedType = binaryPromotedType(leftType, rightType);
          targetRHS = binaryNumericPromotion(targetLHS, promotedType);
          value = binaryNumericPromotion(value, promotedType);
        } else if (TypesUtils.isBooleanType(leftType) && TypesUtils.isBooleanType(rightType)) {
          targetRHS = unbox(targetLHS);
          value = unbox(value);
        } else {
          throw new BugInCF("Both arguments to logical operation must be numeric or boolean");
        }

        BinaryTree operTree =
            treeBuilder.buildBinary(
                leftType, withoutAssignment(kind), tree.getVariable(), tree.getExpression());
        handleArtificialTree(operTree);
        Node operNode;
        if (kind == Tree.Kind.AND_ASSIGNMENT) {
          operNode = new BitwiseAndNode(operTree, targetRHS, value);
        } else if (kind == Tree.Kind.OR_ASSIGNMENT) {
          operNode = new BitwiseOrNode(operTree, targetRHS, value);
        } else {
          assert kind == Tree.Kind.XOR_ASSIGNMENT;
          operNode = new BitwiseXorNode(operTree, targetRHS, value);
        }
        extendWithNode(operNode);

        TypeMirror castType = TypeAnnotationUtils.unannotatedType(leftType);
        TypeCastTree castTree = treeBuilder.buildTypeCast(castType, operTree);
        handleArtificialTree(castTree);
        TypeCastNode castNode = new TypeCastNode(castTree, operNode, castType, types);
        castNode.setInSource(false);
        extendWithNode(castNode);

        AssignmentNode assignNode = new AssignmentNode(tree, targetLHS, castNode);
        extendWithNode(assignNode);
        return assignNode;
      default:
        throw new BugInCF("unexpected compound assignment type");
    }
  }

  @Override
  public Node visitBinary(BinaryTree tree, Void p) {
    // Note that for binary operations it is important to perform any required promotion on the
    // left operand before generating any Nodes for the right operand, because labels must be
    // inserted AFTER ALL preceding Nodes and BEFORE ALL following Nodes.
    Node r;
    Tree leftTree = tree.getLeftOperand();
    Tree rightTree = tree.getRightOperand();

    Tree.Kind kind = tree.getKind();
    switch (kind) {
      case DIVIDE:
      case MULTIPLY:
      case REMAINDER:
        {
          // see JLS 15.17

          TypeMirror exprType = TreeUtils.typeOf(tree);
          TypeMirror leftType = TreeUtils.typeOf(leftTree);
          TypeMirror rightType = TreeUtils.typeOf(rightTree);
          TypeMirror promotedType = binaryPromotedType(leftType, rightType);

          Node left = binaryNumericPromotion(scan(leftTree, p), promotedType);
          Node right = binaryNumericPromotion(scan(rightTree, p), promotedType);

          if (kind == Tree.Kind.MULTIPLY) {
            r = new NumericalMultiplicationNode(tree, left, right);
          } else if (kind == Tree.Kind.DIVIDE) {
            if (TypesUtils.isIntegralPrimitive(exprType)) {
              r = new IntegerDivisionNode(tree, left, right);
              extendWithNodeWithException(r, arithmeticExceptionType);
            } else {
              r = new FloatingDivisionNode(tree, left, right);
            }
          } else {
            assert kind == Tree.Kind.REMAINDER;
            if (TypesUtils.isIntegralPrimitive(exprType)) {
              r = new IntegerRemainderNode(tree, left, right);
              extendWithNodeWithException(r, arithmeticExceptionType);
            } else {
              r = new FloatingRemainderNode(tree, left, right);
            }
          }
          break;
        }

      case MINUS:
      case PLUS:
        {
          // see JLS 15.18

          // TypeMirror exprType = InternalUtils.typeOf(tree);
          TypeMirror leftType = TreeUtils.typeOf(leftTree);
          TypeMirror rightType = TreeUtils.typeOf(rightTree);

          if (TypesUtils.isString(leftType) || TypesUtils.isString(rightType)) {
            assert (kind == Tree.Kind.PLUS);
            Node left = stringConversion(scan(leftTree, p));
            Node right = stringConversion(scan(rightTree, p));
            r = new StringConcatenateNode(tree, left, right);
          } else {
            TypeMirror promotedType = binaryPromotedType(leftType, rightType);
            Node left = binaryNumericPromotion(scan(leftTree, p), promotedType);
            Node right = binaryNumericPromotion(scan(rightTree, p), promotedType);

            // TODO: Decide whether to deal with floating-point value
            // set conversion.
            if (kind == Tree.Kind.PLUS) {
              r = new NumericalAdditionNode(tree, left, right);
            } else {
              assert kind == Tree.Kind.MINUS;
              r = new NumericalSubtractionNode(tree, left, right);
            }
          }
          break;
        }

      case LEFT_SHIFT:
      case RIGHT_SHIFT:
      case UNSIGNED_RIGHT_SHIFT:
        {
          // see JLS 15.19

          Node left = unaryNumericPromotion(scan(leftTree, p));
          Node right = unaryNumericPromotion(scan(rightTree, p));

          if (kind == Tree.Kind.LEFT_SHIFT) {
            r = new LeftShiftNode(tree, left, right);
          } else if (kind == Tree.Kind.RIGHT_SHIFT) {
            r = new SignedRightShiftNode(tree, left, right);
          } else {
            assert kind == Tree.Kind.UNSIGNED_RIGHT_SHIFT;
            r = new UnsignedRightShiftNode(tree, left, right);
          }
          break;
        }

      case GREATER_THAN:
      case GREATER_THAN_EQUAL:
      case LESS_THAN:
      case LESS_THAN_EQUAL:
        {
          // see JLS 15.20.1
          TypeMirror leftType = TreeUtils.typeOf(leftTree);
          if (TypesUtils.isBoxedPrimitive(leftType)) {
            leftType = types.unboxedType(leftType);
          }

          TypeMirror rightType = TreeUtils.typeOf(rightTree);
          if (TypesUtils.isBoxedPrimitive(rightType)) {
            rightType = types.unboxedType(rightType);
          }

          TypeMirror promotedType = binaryPromotedType(leftType, rightType);
          Node left = binaryNumericPromotion(scan(leftTree, p), promotedType);
          Node right = binaryNumericPromotion(scan(rightTree, p), promotedType);

          if (kind == Tree.Kind.GREATER_THAN) {
            r = new GreaterThanNode(tree, left, right);
          } else if (kind == Tree.Kind.GREATER_THAN_EQUAL) {
            r = new GreaterThanOrEqualNode(tree, left, right);
          } else if (kind == Tree.Kind.LESS_THAN) {
            r = new LessThanNode(tree, left, right);
          } else {
            assert kind == Tree.Kind.LESS_THAN_EQUAL;
            r = new LessThanOrEqualNode(tree, left, right);
          }
          break;
        }

      case EQUAL_TO:
      case NOT_EQUAL_TO:
        {
          // see JLS 15.21
          TreeInfo leftInfo = getTreeInfo(leftTree);
          TreeInfo rightInfo = getTreeInfo(rightTree);
          Node left = scan(leftTree, p);
          Node right = scan(rightTree, p);

          if (leftInfo.isNumeric()
              && rightInfo.isNumeric()
              && !(leftInfo.isBoxed() && rightInfo.isBoxed())) {
            // JLS 15.21.1 numerical equality
            TypeMirror promotedType =
                binaryPromotedType(leftInfo.unboxedType(), rightInfo.unboxedType());
            left = binaryNumericPromotion(left, promotedType);
            right = binaryNumericPromotion(right, promotedType);
          } else if (leftInfo.isBoolean()
              && rightInfo.isBoolean()
              && !(leftInfo.isBoxed() && rightInfo.isBoxed())) {
            // JSL 15.21.2 boolean equality
            left = unboxAsNeeded(left, leftInfo.isBoxed());
            right = unboxAsNeeded(right, rightInfo.isBoxed());
          }

          if (kind == Tree.Kind.EQUAL_TO) {
            r = new EqualToNode(tree, left, right);
          } else {
            assert kind == Tree.Kind.NOT_EQUAL_TO;
            r = new NotEqualNode(tree, left, right);
          }
          break;
        }

      case AND:
      case OR:
      case XOR:
        {
          // see JLS 15.22
          TypeMirror leftType = TreeUtils.typeOf(leftTree);
          TypeMirror rightType = TreeUtils.typeOf(rightTree);
          boolean isBooleanOp =
              TypesUtils.isBooleanType(leftType) && TypesUtils.isBooleanType(rightType);

          Node left;
          Node right;

          if (isBooleanOp) {
            left = unbox(scan(leftTree, p));
            right = unbox(scan(rightTree, p));
          } else if (isNumericOrBoxed(leftType) && isNumericOrBoxed(rightType)) {
            TypeMirror promotedType = binaryPromotedType(leftType, rightType);
            left = binaryNumericPromotion(scan(leftTree, p), promotedType);
            right = binaryNumericPromotion(scan(rightTree, p), promotedType);
          } else {
            left = unbox(scan(leftTree, p));
            right = unbox(scan(rightTree, p));
          }

          if (kind == Tree.Kind.AND) {
            r = new BitwiseAndNode(tree, left, right);
          } else if (kind == Tree.Kind.OR) {
            r = new BitwiseOrNode(tree, left, right);
          } else {
            assert kind == Tree.Kind.XOR;
            r = new BitwiseXorNode(tree, left, right);
          }
          break;
        }

      case CONDITIONAL_AND:
      case CONDITIONAL_OR:
        {
          // see JLS 15.23 and 15.24

          // all necessary labels
          Label rightStartLabel = new Label();
          Label shortCircuitLabel = new Label();

          // left-hand side
          Node left = scan(leftTree, p);

          ConditionalJump cjump;
          if (kind == Tree.Kind.CONDITIONAL_AND) {
            cjump = new ConditionalJump(rightStartLabel, shortCircuitLabel);
            cjump.setFalseFlowRule(FlowRule.ELSE_TO_ELSE);
          } else {
            cjump = new ConditionalJump(shortCircuitLabel, rightStartLabel);
            cjump.setTrueFlowRule(FlowRule.THEN_TO_THEN);
          }
          extendWithExtendedNode(cjump);

          // right-hand side
          addLabelForNextNode(rightStartLabel);
          Node right = scan(rightTree, p);

          // conditional expression itself
          addLabelForNextNode(shortCircuitLabel);
          if (kind == Tree.Kind.CONDITIONAL_AND) {
            r = new ConditionalAndNode(tree, left, right);
          } else {
            r = new ConditionalOrNode(tree, left, right);
          }
          break;
        }
      default:
        throw new BugInCF("unexpected binary tree: " + kind);
    }
    assert r != null : "unexpected binary tree";
    extendWithNode(r);
    return r;
  }

  @Override
  public Node visitBlock(BlockTree tree, Void p) {
    for (StatementTree n : tree.getStatements()) {
      scan(n, null);
    }
    return null;
  }

  @Override
  public Node visitBreak(BreakTree tree, Void p) {
    Name label = tree.getLabel();
    if (label == null) {
      assert breakTargetLC != null : "no target for break statement";

      extendWithExtendedNode(new UnconditionalJump(breakTargetLC.accessLabel()));
    } else {
      assert breakLabels.containsKey(label);

      extendWithExtendedNode(new UnconditionalJump(breakLabels.get(label)));
    }

    return null;
  }

  // This visits a switch statement.
  // Switch expressions are visited by visitSwitchExpression17.
  @Override
  public Node visitSwitch(SwitchTree tree, Void p) {
    SwitchBuilder builder = new SwitchBuilder(tree);
    builder.build();
    return null;
  }

  /**
   * Helper class for handling switch statements and switch expressions, including all their
   * substatements such as case labels.
   */
  private class SwitchBuilder {

    /**
     * The tree for the switch statement or switch expression. Its type may be {@link SwitchTree}
     * (for a switch statement) or {@code SwitchExpressionTree}.
     */
    private final Tree switchTree;

    /** The case trees of {@code switchTree} */
    private final List<? extends CaseTree> caseTrees;

    /**
     * The Tree for the selector expression.
     *
     * <pre>
     *   switch ( <em>selector expression</em> ) { ... }
     * </pre>
     */
    private final ExpressionTree selectorExprTree;

    /** The labels for the case bodies. */
    private final Label[] caseBodyLabels;

    /**
     * The Node for the assignment of the switch selector expression to a synthetic local variable.
     */
    private AssignmentNode selectorExprAssignment;

    /**
     * If {@link #switchTree} is a switch expression, then this is a result variable: the synthetic
     * variable that all results of {@code #switchTree} are assigned to. Otherwise, this is null.
     */
    private @Nullable VariableTree switchExprVarTree;

    /**
     * Construct a SwitchBuilder.
     *
     * @param switchTree a {@link SwitchTree} or a {@code SwitchExpressionTree}
     */
    private SwitchBuilder(Tree switchTree) {
      this.switchTree = switchTree;
      if (TreeUtils.isSwitchStatement(switchTree)) {
        SwitchTree switchStatementTree = (SwitchTree) switchTree;
        this.caseTrees = switchStatementTree.getCases();
        this.selectorExprTree = switchStatementTree.getExpression();
      } else {
        this.caseTrees = SwitchExpressionUtils.getCases(switchTree);
        this.selectorExprTree = SwitchExpressionUtils.getExpression(switchTree);
      }
      // "+ 1" for the default case.  If the switch has an explicit default case, then
      // the last element of the array is never used.
      this.caseBodyLabels = new Label[caseTrees.size() + 1];
    }

    /**
     * Build up the CFG for the switchTree.
     *
     * @return if the switch is a switch expression, then a {@link SwitchExpressionNode}; otherwise,
     *     null
     */
    public @Nullable SwitchExpressionNode build() {
      LabelCell oldBreakTargetLC = breakTargetLC;
      breakTargetLC = new LabelCell(new Label());
      int numCases = caseTrees.size();

      for (int i = 0; i < numCases; ++i) {
        caseBodyLabels[i] = new Label();
      }
      caseBodyLabels[numCases] = breakTargetLC.peekLabel();

      buildSelector();

      buildSwitchExpressionVar();

      if (TreeUtils.isSwitchStatement(switchTree)) {
        // It's a switch statement, not a switch expression.
        extendWithNode(
            new MarkerNode(
                switchTree,
                "start of switch statement #" + TreeUtils.treeUids.get(switchTree),
                env.getTypeUtils()));
      }

      // JLS 14.11.2
      // https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.2
      // states "For compatibility reasons, switch statements that are not enhanced switch
      // statements are not required to be exhaustive".
      // Switch expressions and enhanced switch statements are exhaustive.
      boolean switchExprOrEnhanced =
          !TreeUtils.isSwitchStatement(switchTree)
              || TreeUtils.isEnhancedSwitchStatement((SwitchTree) switchTree);
      // Build CFG for the cases.
      int defaultIndex = -1;
      for (int i = 0; i < numCases; ++i) {
        CaseTree caseTree = caseTrees.get(i);
        if (CaseUtils.isDefaultCaseTree(caseTree)) {
          // Per the Java Language Specification, the checks of all cases must happen
          // before the default case, no matter where `default:` is written.  Therefore,
          // build the default case last.
          defaultIndex = i;
        } else if (i == numCases - 1 && defaultIndex == -1) {
          // This is the last case, and there is no default case.
          // Switch expressions and enhanced switch statements are exhaustive.
          buildCase(caseTree, i, switchExprOrEnhanced);
        } else {
          buildCase(caseTree, i, false);
        }
      }

      if (defaultIndex != -1) {
        // The checks of all cases must happen before the default case, therefore we build
        // the default case last.
        // Fallthrough is still handled correctly with the caseBodyLabels.
        buildCase(caseTrees.get(defaultIndex), defaultIndex, false);
      }

      addLabelForNextNode(breakTargetLC.peekLabel());
      breakTargetLC = oldBreakTargetLC;
      if (TreeUtils.isSwitchStatement(switchTree)) {
        // It's a switch statement, not a switch expression.
        extendWithNode(
            new MarkerNode(
                switchTree,
                "end of switch statement #" + TreeUtils.treeUids.get(switchTree),
                env.getTypeUtils()));
      }

      if (!TreeUtils.isSwitchStatement(switchTree)) {
        // It's a switch expression, not a switch statement.
        IdentifierTree switchExprVarUseTree = treeBuilder.buildVariableUse(switchExprVarTree);
        handleArtificialTree(switchExprVarUseTree);

        LocalVariableNode switchExprVarUseNode = new LocalVariableNode(switchExprVarUseTree);
        switchExprVarUseNode.setInSource(false);
        extendWithNode(switchExprVarUseNode);
        SwitchExpressionNode switchExpressionNode =
            new SwitchExpressionNode(
                TreeUtils.typeOf(switchTree), switchTree, switchExprVarUseNode);
        extendWithNode(switchExpressionNode);
        return switchExpressionNode;
      } else {
        return null;
      }
    }

    /**
     * Builds the CFG for the selector expression. It also creates a synthetic variable and assigns
     * the selector expression to the variable. This assignment node is stored in {@link
     * #selectorExprAssignment}. It can later be used to refine the selector expression in case
     * bodies.
     */
    private void buildSelector() {
      // Create a synthetic variable to which the switch selector expression will be assigned
      TypeMirror selectorExprType = TreeUtils.typeOf(selectorExprTree);
      VariableTree selectorVarTree =
          treeBuilder.buildVariableDecl(
              selectorExprType,
              uniqueName("switch"),
              TreePathUtil.findNearestEnclosingElement(getCurrentPath()),
              null);
      handleArtificialTree(selectorVarTree);

      VariableDeclarationNode selectorVarNode = new VariableDeclarationNode(selectorVarTree);
      selectorVarNode.setInSource(false);
      extendWithNode(selectorVarNode);

      IdentifierTree selectorVarUseTree = treeBuilder.buildVariableUse(selectorVarTree);
      handleArtificialTree(selectorVarUseTree);

      LocalVariableNode selectorVarUseNode = new LocalVariableNode(selectorVarUseTree);
      selectorVarUseNode.setInSource(false);
      extendWithNode(selectorVarUseNode);

      Node selectorExprNode = unbox(scan(selectorExprTree, null));

      AssignmentTree assign = treeBuilder.buildAssignment(selectorVarUseTree, selectorExprTree);
      handleArtificialTree(assign);

      selectorExprAssignment = new AssignmentNode(assign, selectorVarUseNode, selectorExprNode);
      selectorExprAssignment.setInSource(false);
      extendWithNode(selectorExprAssignment);
    }

    /**
     * If {@link #switchTree} is a switch expression tree, this method creates a synthetic variable
     * whose value is the value of the switch expression.
     */
    private void buildSwitchExpressionVar() {
      if (TreeUtils.isSwitchStatement(switchTree)) {
        // A switch statement does not have a value, so do nothing.
        return;
      }
      TypeMirror switchExprType = TreeUtils.typeOf(switchTree);
      switchExprVarTree =
          treeBuilder.buildVariableDecl(
              switchExprType,
              uniqueName("switchExpr"),
              TreePathUtil.findNearestEnclosingElement(getCurrentPath()),
              null);
      handleArtificialTree(switchExprVarTree);

      VariableDeclarationNode switchExprVarNode = new VariableDeclarationNode(switchExprVarTree);
      switchExprVarNode.setInSource(false);
      extendWithNode(switchExprVarNode);
    }

    /**
     * Build the CFG for the given case tree.
     *
     * @param caseTree a case tree whose CFG to build
     * @param index the index of the case tree in {@link #caseBodyLabels}
     * @param isLastCaseOfExhaustive true if this is the last case of an exhaustive switch
     *     statement, with no fallthrough to it. In other words, no test of the labels is necessary.
     */
    private void buildCase(CaseTree caseTree, int index, boolean isLastCaseOfExhaustive) {
      boolean isDefaultCase = CaseUtils.isDefaultCaseTree(caseTree);
      // If true, no test of labels is necessary.
      // Unfortunately, if isLastCaseOfExhaustive==TRUE, no flow-sensitive refinement occurs
      // within the body of the CaseNode.  In the future, that can be performed, but it
      // requires addition of InfeasibleExitBlock, a new SpecialBlock in the CFG.
      boolean isTerminalCase = isDefaultCase || isLastCaseOfExhaustive;

      Label thisBodyLabel = caseBodyLabels[index];
      Label nextBodyLabel = caseBodyLabels[index + 1];
      // `nextCaseLabel` is not used if isTerminalCase==FALSE.
      Label nextCaseLabel = new Label();

      // Handle the case expressions
      if (!isTerminalCase) {
        // A case expression exists, and it needs to be tested.
        ArrayList<Node> exprs = new ArrayList<>();
        for (Tree exprTree : CaseUtils.getLabels(caseTree)) {
          exprs.add(scan(exprTree, null));
        }

        ExpressionTree guardTree = CaseUtils.getGuard(caseTree);
        Node guard = (guardTree == null) ? null : scan(guardTree, null);

        CaseNode test =
            new CaseNode(caseTree, selectorExprAssignment, exprs, guard, env.getTypeUtils());
        extendWithNode(test);
        extendWithExtendedNode(new ConditionalJump(thisBodyLabel, nextCaseLabel));
      }

      // Handle the case body
      addLabelForNextNode(thisBodyLabel);
      if (caseTree.getStatements() != null) {
        // This is a switch labeled statement group.
        // A "switch labeled statement group" is a "case L:" label along with its code.
        // The code either ends with a "yield" statement, or it falls through.
        for (StatementTree stmt : caseTree.getStatements()) {
          scan(stmt, null);
        }
        // Handle possible fallthrough by adding jump to next body.
        if (!isTerminalCase) {
          extendWithExtendedNode(new UnconditionalJump(nextBodyLabel));
        }
      } else {
        // This is either the default case or a switch labeled rule (which appears in a
        // switch expression).
        // A "switch labeled rule" is a "case L ->" label along with its code.
        Tree bodyTree = CaseUtils.getBody(caseTree);
        if (!TreeUtils.isSwitchStatement(switchTree) && bodyTree instanceof ExpressionTree) {
          buildSwitchExpressionResult((ExpressionTree) bodyTree);
        } else {
          scan(bodyTree, null);
          // Switch rules never fall through so add jump to the break target.
          assert breakTargetLC != null : "no target for case statement";
          extendWithExtendedNode(new UnconditionalJump(breakTargetLC.accessLabel()));
        }
      }

      if (!isTerminalCase) {
        addLabelForNextNode(nextCaseLabel);
      }
    }

    /**
     * Does the following for the result expression of a switch expression, {@code
     * resultExpression}:
     *
     * <ol>
     *   <li>Builds the CFG for the switch expression result.
     *   <li>Creates an assignment node for the assignment of {@code resultExpression} to {@code
     *       switchExprVarTree}.
     *   <li>Adds an unconditional jump to {@link #breakTargetLC} (the end of the switch
     *       expression).
     * </ol>
     *
     * @param resultExpression the result of a switch expression; either from a yield or an
     *     expression in a case rule
     */
    /*package-private*/ void buildSwitchExpressionResult(ExpressionTree resultExpression) {
      IdentifierTree switchExprVarUseTree = treeBuilder.buildVariableUse(switchExprVarTree);
      handleArtificialTree(switchExprVarUseTree);

      LocalVariableNode switchExprVarUseNode = new LocalVariableNode(switchExprVarUseTree);
      switchExprVarUseNode.setInSource(false);
      extendWithNode(switchExprVarUseNode);

      Node resultExprNode = scan(resultExpression, null);

      AssignmentTree assign = treeBuilder.buildAssignment(switchExprVarUseTree, resultExpression);
      handleArtificialTree(assign);

      AssignmentNode assignmentNode =
          new AssignmentNode(assign, switchExprVarUseNode, resultExprNode);
      assignmentNode.setInSource(false);
      extendWithNode(assignmentNode);
      // Switch rules never fall through so add jump to the break target.
      assert breakTargetLC != null : "no target for case statement";
      extendWithExtendedNode(new UnconditionalJump(breakTargetLC.accessLabel()));
    }
  }

  @Override
  public Node visitCase(CaseTree tree, Void p) {
    // This assertion assumes that `case` appears only within a switch statement,
    throw new AssertionError("case visitor is implemented in SwitchBuilder");
  }

  @Override
  public Node visitCatch(CatchTree tree, Void p) {
    scan(tree.getParameter(), p);
    scan(tree.getBlock(), p);
    return null;
  }

  // This is not invoked for top-level classes.  Maybe it is, for classes defined within method
  // bodies.
  @Override
  public Node visitClass(ClassTree tree, Void p) {
    declaredClasses.add(tree);
    Node classbody = new ClassDeclarationNode(tree);
    extendWithNode(classbody);
    return classbody;
  }

  @Override
  public Node visitConditionalExpression(ConditionalExpressionTree tree, Void p) {
    // see JLS 15.25
    TypeMirror exprType = TreeUtils.typeOf(tree);
    if (exprType.getKind() == TypeKind.NULL) {
      // Happens when the 2nd and 3rd operands are both null, e.g.: b ? null : null
      Tree parent = TreePathUtil.getContextForPolyExpression(getCurrentPath());
      if (parent != null) {
        exprType = TreeUtils.typeOf(parent);
        // exprType is null when the condition is non-atomic, e.g.: x.isEmpty() ? null :
        // null
      }
      if (parent == null || exprType == null) {
        exprType = TypesUtils.getObjectTypeMirror(env);
      }
    }
    Label trueStart = new Label();
    Label falseStart = new Label();
    Label merge = new Label();

    // create a synthetic variable for the value of the conditional expression
    VariableTree condExprVarTree =
        treeBuilder.buildVariableDecl(
            exprType,
            uniqueName("condExpr"),
            TreePathUtil.findNearestEnclosingElement(getCurrentPath()),
            null);
    handleArtificialTree(condExprVarTree);
    VariableDeclarationNode condExprVarNode = new VariableDeclarationNode(condExprVarTree);
    condExprVarNode.setInSource(false);
    extendWithNode(condExprVarNode);

    Node condition = unbox(scan(tree.getCondition(), p));
    ConditionalJump cjump = new ConditionalJump(trueStart, falseStart);
    extendWithExtendedNode(cjump);

    addLabelForNextNode(trueStart);
    ExpressionTree trueExprTree = tree.getTrueExpression();
    Node trueExprNode = scan(trueExprTree, p);
    trueExprNode = conditionalExprPromotion(trueExprNode, exprType);
    extendWithAssignmentForConditionalExpr(condExprVarTree, trueExprTree, trueExprNode);
    extendWithExtendedNode(new UnconditionalJump(merge));

    addLabelForNextNode(falseStart);
    ExpressionTree falseExprTree = tree.getFalseExpression();
    Node falseExprNode = scan(falseExprTree, p);
    falseExprNode = conditionalExprPromotion(falseExprNode, exprType);
    extendWithAssignmentForConditionalExpr(condExprVarTree, falseExprTree, falseExprNode);
    extendWithExtendedNode(new UnconditionalJump(merge));

    addLabelForNextNode(merge);
    IPair<IdentifierTree, LocalVariableNode> treeAndLocalVarNode = buildVarUseNode(condExprVarTree);
    Node node =
        new TernaryExpressionNode(
            tree, condition, trueExprNode, falseExprNode, treeAndLocalVarNode.second);
    extendWithNode(node);

    return node;
  }

  /**
   * Extend the CFG with an assignment for either the true or false case of a conditional
   * expression, assigning the value of the expression for the case to the synthetic variable for
   * the conditional expression
   *
   * @param condExprVarTree tree for synthetic variable for conditional expression
   * @param caseExprTree expression tree for the case
   * @param caseExprNode node for the case
   */
  private void extendWithAssignmentForConditionalExpr(
      VariableTree condExprVarTree, ExpressionTree caseExprTree, Node caseExprNode) {
    IPair<IdentifierTree, LocalVariableNode> treeAndLocalVarNode = buildVarUseNode(condExprVarTree);

    AssignmentTree assign = treeBuilder.buildAssignment(treeAndLocalVarNode.first, caseExprTree);
    handleArtificialTree(assign);

    // Build a "synthetic" assignment node, allowing special handling in transfer functions
    AssignmentNode assignmentNode =
        new AssignmentNode(assign, treeAndLocalVarNode.second, caseExprNode, true);
    assignmentNode.setInSource(false);
    extendWithNode(assignmentNode);
  }

  /**
   * Build a pair of {@link IdentifierTree} and {@link LocalVariableNode} to represent a use of some
   * variable. Does not add the node to the CFG.
   *
   * @param varTree tree for the variable
   * @return a pair whose first element is the synthetic {@link IdentifierTree} for the use, and
   *     whose second element is the {@link LocalVariableNode} representing the use
   */
  private IPair<IdentifierTree, LocalVariableNode> buildVarUseNode(VariableTree varTree) {
    IdentifierTree condExprVarUseTree = treeBuilder.buildVariableUse(varTree);
    handleArtificialTree(condExprVarUseTree);
    LocalVariableNode condExprVarUseNode = new LocalVariableNode(condExprVarUseTree);
    condExprVarUseNode.setInSource(false);
    // Do not actually add the node to the CFG.
    return IPair.of(condExprVarUseTree, condExprVarUseNode);
  }

  @Override
  public Node visitContinue(ContinueTree tree, Void p) {
    Name label = tree.getLabel();
    UnconditionalJump uj;
    if (label == null) {
      assert continueTargetLC != null : "no target for continue statement";
      uj = new UnconditionalJump(continueTargetLC.accessLabel());
    } else {
      assert continueLabels.containsKey(label);
      uj = new UnconditionalJump(continueLabels.get(label));
    }

    extendWithExtendedNode(uj);
    return null;
  }

  @Override
  public Node visitDoWhileLoop(DoWhileLoopTree tree, Void p) {
    Name parentLabel = getLabel(getCurrentPath());

    Label loopEntry = new Label();
    Label loopExit = new Label();

    // If the loop is a labeled statement, then its continue target is identical for continues
    // with no label and continues with the loop's label.
    Label conditionStart;
    if (parentLabel != null) {
      conditionStart = continueLabels.get(parentLabel);
    } else {
      conditionStart = new Label();
    }

    LabelCell oldBreakTargetLC = breakTargetLC;
    breakTargetLC = new LabelCell(loopExit);

    LabelCell oldContinueTargetLC = continueTargetLC;
    continueTargetLC = new LabelCell(conditionStart);

    // Loop body
    addLabelForNextNode(loopEntry);
    assert tree.getStatement() != null;
    scan(tree.getStatement(), p);

    // Condition
    addLabelForNextNode(conditionStart);
    assert tree.getCondition() != null;
    unbox(scan(tree.getCondition(), p));
    ConditionalJump cjump = new ConditionalJump(loopEntry, loopExit);
    extendWithExtendedNode(cjump);

    // Loop exit
    addLabelForNextNode(loopExit);

    breakTargetLC = oldBreakTargetLC;
    continueTargetLC = oldContinueTargetLC;

    return null;
  }

  @Override
  public Node visitErroneous(ErroneousTree tree, Void p) {
    throw new BugInCF("ErroneousTree is unexpected in AST to CFG translation: " + tree);
  }

  @Override
  public Node visitExpressionStatement(ExpressionStatementTree tree, Void p) {
    ExpressionTree exprTree = tree.getExpression();
    scan(exprTree, p);
    extendWithNode(new ExpressionStatementNode(exprTree));
    return null;
  }

  @Override
  public Node visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
    // see JLS 14.14.2
    Name parentLabel = getLabel(getCurrentPath());

    Label conditionStart = new Label();
    Label loopEntry = new Label();
    Label loopExit = new Label();

    // If the loop is a labeled statement, then its continue target is identical for continues
    // with no label and continues with the loop's label.
    Label updateStart;
    if (parentLabel != null) {
      updateStart = continueLabels.get(parentLabel);
    } else {
      updateStart = new Label();
    }

    LabelCell oldBreakTargetLC = breakTargetLC;
    breakTargetLC = new LabelCell(loopExit);

    LabelCell oldContinueTargetLC = continueTargetLC;
    continueTargetLC = new LabelCell(updateStart);

    // Distinguish loops over Iterables from loops over arrays.

    VariableTree variable = tree.getVariable();
    VariableElement variableElement = TreeUtils.elementFromDeclaration(variable);
    ExpressionTree expression = tree.getExpression();
    StatementTree statement = tree.getStatement();

    TypeMirror exprType = TreeUtils.typeOf(expression);

    if (types.isSubtype(exprType, iterableType)) {
      // Take the upper bound of a type variable or wildcard
      exprType = TypesUtils.upperBound(exprType);

      assert (exprType instanceof DeclaredType) : "an Iterable must be a DeclaredType";
      DeclaredType declaredExprType = (DeclaredType) exprType;
      declaredExprType.getTypeArguments();

      MemberSelectTree iteratorSelect = treeBuilder.buildIteratorMethodAccess(expression);
      handleArtificialTree(iteratorSelect);

      MethodInvocationTree iteratorCall = treeBuilder.buildMethodInvocation(iteratorSelect);
      handleArtificialTree(iteratorCall);

      VariableTree iteratorVariable =
          createEnhancedForLoopIteratorVariable(iteratorCall, variableElement);
      handleArtificialTree(iteratorVariable);

      VariableDeclarationNode iteratorVariableDecl = new VariableDeclarationNode(iteratorVariable);
      iteratorVariableDecl.setInSource(false);

      extendWithNode(iteratorVariableDecl);

      Node expressionNode = scan(expression, p);

      MethodAccessNode iteratorAccessNode = new MethodAccessNode(iteratorSelect, expressionNode);
      iteratorAccessNode.setInSource(false);
      extendWithNode(iteratorAccessNode);
      MethodInvocationNode iteratorCallNode =
          new MethodInvocationNode(
              iteratorCall, iteratorAccessNode, Collections.emptyList(), getCurrentPath());
      iteratorCallNode.setInSource(false);
      extendWithNode(iteratorCallNode);

      translateAssignment(
          iteratorVariable, new LocalVariableNode(iteratorVariable), iteratorCallNode);

      // Test the loop ending condition
      addLabelForNextNode(conditionStart);
      IdentifierTree iteratorUse1 = treeBuilder.buildVariableUse(iteratorVariable);
      handleArtificialTree(iteratorUse1);

      LocalVariableNode iteratorReceiverNode = new LocalVariableNode(iteratorUse1);
      iteratorReceiverNode.setInSource(false);
      extendWithNode(iteratorReceiverNode);

      MemberSelectTree hasNextSelect = treeBuilder.buildHasNextMethodAccess(iteratorUse1);
      handleArtificialTree(hasNextSelect);

      MethodAccessNode hasNextAccessNode =
          new MethodAccessNode(hasNextSelect, iteratorReceiverNode);
      hasNextAccessNode.setInSource(false);
      extendWithNode(hasNextAccessNode);

      MethodInvocationTree hasNextCall = treeBuilder.buildMethodInvocation(hasNextSelect);
      handleArtificialTree(hasNextCall);

      MethodInvocationNode hasNextCallNode =
          new MethodInvocationNode(
              hasNextCall, hasNextAccessNode, Collections.emptyList(), getCurrentPath());
      hasNextCallNode.setInSource(false);
      extendWithNode(hasNextCallNode);
      extendWithExtendedNode(new ConditionalJump(loopEntry, loopExit));

      // Loop body, starting with declaration of the loop iteration variable
      addLabelForNextNode(loopEntry);
      extendWithNode(new VariableDeclarationNode(variable));

      IdentifierTree iteratorUse2 = treeBuilder.buildVariableUse(iteratorVariable);
      handleArtificialTree(iteratorUse2);

      LocalVariableNode iteratorReceiverNode2 = new LocalVariableNode(iteratorUse2);
      iteratorReceiverNode2.setInSource(false);
      extendWithNode(iteratorReceiverNode2);

      MemberSelectTree nextSelect = treeBuilder.buildNextMethodAccess(iteratorUse2);
      handleArtificialTree(nextSelect);

      MethodAccessNode nextAccessNode = new MethodAccessNode(nextSelect, iteratorReceiverNode2);
      nextAccessNode.setInSource(false);
      extendWithNode(nextAccessNode);

      MethodInvocationTree nextCall = treeBuilder.buildMethodInvocation(nextSelect);
      handleArtificialTree(nextCall);

      MethodInvocationNode nextCallNode =
          new MethodInvocationNode(
              nextCall, nextAccessNode, Collections.emptyList(), getCurrentPath());
      // If the type of iteratorVariable is a capture, its type tree may be missing
      // annotations, so save the expression in the node so that the full type can be
      // found later.
      nextCallNode.setIterableExpression(expression);
      nextCallNode.setEnhancedForLoop(tree);
      nextCallNode.setInSource(false);
      extendWithNode(nextCallNode);

      AssignmentNode assignNode =
          translateAssignment(variable, new LocalVariableNode(variable), nextCall);
      // translateAssignment() scans variable and creates new nodes, so set the expression
      // there, too.
      ((MethodInvocationNode) assignNode.getExpression()).setIterableExpression(expression);
      ((MethodInvocationNode) assignNode.getExpression()).setEnhancedForLoop(tree);

      assert statement != null;
      scan(statement, p);

      // Loop back edge
      addLabelForNextNode(updateStart);
      extendWithExtendedNode(new UnconditionalJump(conditionStart));

    } else {
      // TODO: Shift any labels after the initialization of the
      // temporary array variable.

      VariableTree arrayVariable = createEnhancedForLoopArrayVariable(expression, variableElement);
      handleArtificialTree(arrayVariable);

      VariableDeclarationNode arrayVariableNode = new VariableDeclarationNode(arrayVariable);
      arrayVariableNode.setInSource(false);
      extendWithNode(arrayVariableNode);
      Node expressionNode = scan(expression, p);

      translateAssignment(arrayVariable, new LocalVariableNode(arrayVariable), expressionNode)
          .setDesugaredFromEnhancedArrayForLoop();

      // Declare and initialize the loop index variable
      TypeMirror intType = types.getPrimitiveType(TypeKind.INT);

      LiteralTree zero = treeBuilder.buildLiteral(Integer.valueOf(0));
      handleArtificialTree(zero);

      VariableTree indexVariable =
          treeBuilder.buildVariableDecl(
              intType, uniqueName("index"), variableElement.getEnclosingElement(), zero);
      handleArtificialTree(indexVariable);
      VariableDeclarationNode indexVariableNode = new VariableDeclarationNode(indexVariable);
      indexVariableNode.setInSource(false);
      extendWithNode(indexVariableNode);
      IntegerLiteralNode zeroNode = new IntegerLiteralNode(zero);
      extendWithNode(zeroNode);

      translateAssignment(indexVariable, new LocalVariableNode(indexVariable), zeroNode);

      // Compare index to array length
      addLabelForNextNode(conditionStart);
      IdentifierTree indexUse1 = treeBuilder.buildVariableUse(indexVariable);
      handleArtificialTree(indexUse1);
      LocalVariableNode indexNode1 = new LocalVariableNode(indexUse1);
      indexNode1.setInSource(false);
      extendWithNode(indexNode1);

      IdentifierTree arrayUse1 = treeBuilder.buildVariableUse(arrayVariable);
      handleArtificialTree(arrayUse1);
      LocalVariableNode arrayNode1 = new LocalVariableNode(arrayUse1);
      extendWithNode(arrayNode1);

      MemberSelectTree lengthSelect = treeBuilder.buildArrayLengthAccess(arrayUse1);
      handleArtificialTree(lengthSelect);
      FieldAccessNode lengthAccessNode = new FieldAccessNode(lengthSelect, arrayNode1);
      lengthAccessNode.setInSource(false);
      extendWithNodeWithException(lengthAccessNode, nullPointerExceptionType);

      BinaryTree lessThan = treeBuilder.buildLessThan(indexUse1, lengthSelect);
      handleArtificialTree(lessThan);

      LessThanNode lessThanNode = new LessThanNode(lessThan, indexNode1, lengthAccessNode);
      lessThanNode.setInSource(false);
      extendWithNode(lessThanNode);
      extendWithExtendedNode(new ConditionalJump(loopEntry, loopExit));

      // Loop body, starting with declaration of the loop iteration variable
      addLabelForNextNode(loopEntry);
      extendWithNode(new VariableDeclarationNode(variable));

      IdentifierTree arrayUse2 = treeBuilder.buildVariableUse(arrayVariable);
      handleArtificialTree(arrayUse2);
      LocalVariableNode arrayNode2 = new LocalVariableNode(arrayUse2);
      arrayNode2.setInSource(false);
      extendWithNode(arrayNode2);

      IdentifierTree indexUse2 = treeBuilder.buildVariableUse(indexVariable);
      handleArtificialTree(indexUse2);
      LocalVariableNode indexNode2 = new LocalVariableNode(indexUse2);
      indexNode2.setInSource(false);
      extendWithNode(indexNode2);

      ArrayAccessTree arrayAccess = treeBuilder.buildArrayAccess(arrayUse2, indexUse2);
      handleArtificialTree(arrayAccess);
      ArrayAccessNode arrayAccessNode = new ArrayAccessNode(arrayAccess, arrayNode2, indexNode2);
      arrayAccessNode.setArrayExpression(expression);
      arrayAccessNode.setEnhancedForLoop(tree);
      arrayAccessNode.setInSource(false);
      extendWithNode(arrayAccessNode);
      AssignmentNode arrayAccessAssignNode =
          translateAssignment(variable, new LocalVariableNode(variable), arrayAccessNode);
      // translateAssignment() scans variable and creates new nodes, so set the expression
      // there, too.
      Node arrayAccessAssignNodeExpr = arrayAccessAssignNode.getExpression();
      if (arrayAccessAssignNodeExpr instanceof ArrayAccessNode) {
        ((ArrayAccessNode) arrayAccessAssignNodeExpr).setArrayExpression(expression);
        ((ArrayAccessNode) arrayAccessAssignNodeExpr).setEnhancedForLoop(tree);
      } else if (arrayAccessAssignNodeExpr instanceof MethodInvocationNode) {
        // If the array component type is a primitive, there may be a boxing or unboxing
        // conversion. Treat that as an iterator.
        MethodInvocationNode boxingNode = (MethodInvocationNode) arrayAccessAssignNodeExpr;
        boxingNode.setIterableExpression(expression);
        boxingNode.setEnhancedForLoop(tree);
      }

      assert statement != null;
      scan(statement, p);

      // Loop back edge
      addLabelForNextNode(updateStart);

      IdentifierTree indexUse3 = treeBuilder.buildVariableUse(indexVariable);
      handleArtificialTree(indexUse3);
      LocalVariableNode indexNode3 = new LocalVariableNode(indexUse3);
      indexNode3.setInSource(false);
      extendWithNode(indexNode3);

      LiteralTree oneTree = treeBuilder.buildLiteral(Integer.valueOf(1));
      handleArtificialTree(oneTree);
      Node one = new IntegerLiteralNode(oneTree);
      one.setInSource(false);
      extendWithNode(one);

      BinaryTree addOneTree = treeBuilder.buildBinary(intType, Tree.Kind.PLUS, indexUse3, oneTree);
      handleArtificialTree(addOneTree);
      Node addOneNode = new NumericalAdditionNode(addOneTree, indexNode3, one);
      addOneNode.setInSource(false);
      extendWithNode(addOneNode);

      AssignmentTree assignTree = treeBuilder.buildAssignment(indexUse3, addOneTree);
      handleArtificialTree(assignTree);
      Node assignNode = new AssignmentNode(assignTree, indexNode3, addOneNode);
      assignNode.setInSource(false);
      extendWithNode(assignNode);

      extendWithExtendedNode(new UnconditionalJump(conditionStart));
    }

    // Loop exit
    addLabelForNextNode(loopExit);

    breakTargetLC = oldBreakTargetLC;
    continueTargetLC = oldContinueTargetLC;

    return null;
  }

  protected VariableTree createEnhancedForLoopIteratorVariable(
      MethodInvocationTree iteratorCall, VariableElement variableElement) {
    TypeMirror iteratorType = TreeUtils.typeOf(iteratorCall);

    // Declare and initialize a new, unique iterator variable
    VariableTree iteratorVariable =
        treeBuilder.buildVariableDecl(
            iteratorType, // annotatedIteratorTypeTree,
            uniqueName("iter"),
            variableElement.getEnclosingElement(),
            iteratorCall);
    return iteratorVariable;
  }

  protected VariableTree createEnhancedForLoopArrayVariable(
      ExpressionTree expression, VariableElement variableElement) {
    TypeMirror arrayType = TreeUtils.typeOf(expression);

    // Declare and initialize a temporary array variable
    VariableTree arrayVariable =
        treeBuilder.buildVariableDecl(
            arrayType, uniqueName("array"), variableElement.getEnclosingElement(), expression);
    return arrayVariable;
  }

  @Override
  public Node visitForLoop(ForLoopTree tree, Void p) {
    Name parentLabel = getLabel(getCurrentPath());

    Label conditionStart = new Label();
    Label loopEntry = new Label();
    Label loopExit = new Label();

    // If the loop is a labeled statement, then its continue target is identical for continues
    // with no label and continues with the loop's label.
    Label updateStart;
    if (parentLabel != null) {
      updateStart = continueLabels.get(parentLabel);
    } else {
      updateStart = new Label();
    }

    LabelCell oldBreakTargetLC = breakTargetLC;
    breakTargetLC = new LabelCell(loopExit);

    LabelCell oldContinueTargetLC = continueTargetLC;
    continueTargetLC = new LabelCell(updateStart);

    // Initializer
    for (StatementTree init : tree.getInitializer()) {
      scan(init, p);
    }

    // Condition
    addLabelForNextNode(conditionStart);
    if (tree.getCondition() != null) {
      unbox(scan(tree.getCondition(), p));
      ConditionalJump cjump = new ConditionalJump(loopEntry, loopExit);
      extendWithExtendedNode(cjump);
    }

    // Loop body
    addLabelForNextNode(loopEntry);
    assert tree.getStatement() != null;
    scan(tree.getStatement(), p);

    // Update
    addLabelForNextNode(updateStart);
    for (ExpressionStatementTree update : tree.getUpdate()) {
      scan(update, p);
    }

    extendWithExtendedNode(new UnconditionalJump(conditionStart));

    // Loop exit
    addLabelForNextNode(loopExit);

    breakTargetLC = oldBreakTargetLC;
    continueTargetLC = oldContinueTargetLC;

    return null;
  }

  @Override
  public Node visitIdentifier(IdentifierTree tree, Void p) {
    Node node;
    if (TreeUtils.isFieldAccess(tree)) {
      Node receiver = getReceiver(tree);
      node = new FieldAccessNode(tree, receiver);
    } else {
      Element element = TreeUtils.elementFromUse(tree);
      switch (element.getKind()) {
        case FIELD:
          // Note that "this"/"super" is a field, but not a field access.
          if (element.getSimpleName().contentEquals("this")) {
            node = new ExplicitThisNode(tree);
          } else {
            node = new SuperNode(tree);
          }
          break;
        case EXCEPTION_PARAMETER:
        case LOCAL_VARIABLE:
        case RESOURCE_VARIABLE:
        case PARAMETER:
          node = new LocalVariableNode(tree);
          break;
        case PACKAGE:
          node = new PackageNameNode(tree);
          break;
        default:
          if (ElementUtils.isTypeDeclaration(element)) {
            node = new ClassNameNode(tree);
            break;
          } else if (ElementUtils.isBindingVariable(element)) {
            // Note: BINDING_VARIABLE should be added as a direct case above when
            // instanceof pattern matching and Java15 are supported.
            node = new LocalVariableNode(tree);
            break;
          }
          throw new BugInCF("bad element kind " + element.getKind());
      }
    }
    if (node instanceof ClassNameNode) {
      extendWithClassNameNode((ClassNameNode) node);
    } else {
      extendWithNode(node);
    }
    return node;
  }

  @Override
  public Node visitIf(IfTree tree, Void p) {
    // all necessary labels
    Label thenEntry = new Label();
    Label elseEntry = new Label();
    Label endIf = new Label();

    // basic block for the condition
    unbox(scan(tree.getCondition(), p));

    ConditionalJump cjump = new ConditionalJump(thenEntry, elseEntry);
    extendWithExtendedNode(cjump);

    // then branch
    addLabelForNextNode(thenEntry);
    StatementTree thenStatement = tree.getThenStatement();
    scan(thenStatement, p);
    extendWithExtendedNode(new UnconditionalJump(endIf));

    // else branch
    addLabelForNextNode(elseEntry);
    StatementTree elseStatement = tree.getElseStatement();
    if (elseStatement != null) {
      scan(elseStatement, p);
    }

    // label the end of the if statement
    addLabelForNextNode(endIf);

    return null;
  }

  @Override
  public Node visitImport(ImportTree tree, Void p) {
    throw new BugInCF("ImportTree is unexpected in AST to CFG translation: " + tree);
  }

  @Override
  public Node visitArrayAccess(ArrayAccessTree tree, Void p) {
    Node array = scan(tree.getExpression(), p);
    Node index = unaryNumericPromotion(scan(tree.getIndex(), p));
    Node arrayAccess = new ArrayAccessNode(tree, array, index);
    extendWithNodeWithExceptions(arrayAccess, arrayAccessExceptionTypes);
    return arrayAccess;
  }

  @Override
  public Node visitLabeledStatement(LabeledStatementTree tree, Void p) {
    // This method can set the break target after generating all Nodes in the contained
    // statement, but it can't set the continue target, which may be in the middle of a
    // sequence of nodes. Labeled loops must look up and use the continue Labels.
    Name labelName = tree.getLabel();

    Label breakLabel = new Label(labelName + "_break");
    Label continueLabel = new Label(labelName + "_continue");

    breakLabels.put(labelName, breakLabel);
    continueLabels.put(labelName, continueLabel);

    scan(tree.getStatement(), p);

    addLabelForNextNode(breakLabel);

    breakLabels.remove(labelName);
    continueLabels.remove(labelName);

    return null;
  }

  @Override
  public Node visitLiteral(LiteralTree tree, Void p) {
    Node r;
    switch (tree.getKind()) {
      case BOOLEAN_LITERAL:
        r = new BooleanLiteralNode(tree);
        break;
      case CHAR_LITERAL:
        r = new CharacterLiteralNode(tree);
        break;
      case DOUBLE_LITERAL:
        r = new DoubleLiteralNode(tree);
        break;
      case FLOAT_LITERAL:
        r = new FloatLiteralNode(tree);
        break;
      case INT_LITERAL:
        r = new IntegerLiteralNode(tree);
        break;
      case LONG_LITERAL:
        r = new LongLiteralNode(tree);
        break;
      case NULL_LITERAL:
        r = new NullLiteralNode(tree);
        break;
      case STRING_LITERAL:
        r = new StringLiteralNode(tree);
        break;
      default:
        throw new BugInCF("unexpected literal tree: " + tree);
    }
    extendWithNode(r);
    return r;
  }

  @Override
  public Node visitMethod(MethodTree tree, Void p) {
    throw new BugInCF("MethodTree is unexpected in AST to CFG translation");
  }

  @Override
  public Node visitModifiers(ModifiersTree tree, Void p) {
    throw new BugInCF("ModifiersTree is unexpected in AST to CFG translation");
  }

  @Override
  public Node visitNewArray(NewArrayTree tree, Void p) {
    // see JLS 15.10

    ArrayType type = (ArrayType) TreeUtils.typeOf(tree);
    TypeMirror elemType = type.getComponentType();

    List<? extends ExpressionTree> dimensions = tree.getDimensions();
    List<? extends ExpressionTree> initializers = tree.getInitializers();
    assert dimensions != null;

    List<Node> dimensionNodes =
        CollectionsPlume.mapList(dim -> unaryNumericPromotion(scan(dim, p)), dimensions);

    List<Node> initializerNodes;
    if (initializers == null) {
      initializerNodes = Collections.emptyList();
    } else {
      initializerNodes =
          CollectionsPlume.mapList(init -> assignConvert(scan(init, p), elemType), initializers);
    }

    Node node = new ArrayCreationNode(tree, type, dimensionNodes, initializerNodes);

    extendWithNodeWithExceptions(node, newArrayExceptionTypes);
    return node;
  }

  @Override
  public Node visitNewClass(NewClassTree tree, Void p) {
    // see JLS 15.9

    DeclaredType classType = (DeclaredType) TreeUtils.typeOf(tree);
    TypeMirror enclosingType = classType.getEnclosingType();
    Tree enclosingExpr = tree.getEnclosingExpression();
    Node enclosingExprNode;
    if (enclosingExpr != null) {
      enclosingExprNode = scan(enclosingExpr, p);
    } else if (enclosingType.getKind() == TypeKind.DECLARED) {
      // This is an inner class (instance nested class).
      // As there is no explicit enclosing expression, create a node for the implicit this
      // argument.
      enclosingExprNode = new ImplicitThisNode(enclosingType);
      extendWithNode(enclosingExprNode);
    } else {
      // For static nested classes, the kind would be Typekind.None.

      enclosingExprNode = null;
    }

    // Convert constructor arguments
    ExecutableElement constructor = TreeUtils.elementFromUse(tree);

    List<? extends ExpressionTree> actualExprs = tree.getArguments();

    List<Node> arguments =
        convertCallArguments(tree, constructor, TreeUtils.typeFromUse(tree), actualExprs);

    // TODO: for anonymous classes, don't use the identifier alone.
    // See https://github.com/typetools/checker-framework/issues/890 .
    Node constructorNode = scan(tree.getIdentifier(), p);

    // Handle anonymous classes in visitClass.
    // Note that getClassBody() and therefore classbody can be null.
    ClassDeclarationNode classbody = (ClassDeclarationNode) scan(tree.getClassBody(), p);

    Node node =
        new ObjectCreationNode(tree, enclosingExprNode, constructorNode, arguments, classbody);
    List<? extends TypeMirror> thrownTypes = constructor.getThrownTypes();
    Set<TypeMirror> thrownSet =
        ArraySet.newArraySetOrLinkedHashSet(thrownTypes.size() + uncheckedExceptionTypes.size());
    // Add exceptions explicitly mentioned in the throws clause.
    thrownSet.addAll(thrownTypes);
    // Add types to account for unchecked exceptions
    thrownSet.addAll(uncheckedExceptionTypes);

    extendWithNodeWithExceptions(node, thrownSet);

    return node;
  }

  /**
   * Maps a {@code Tree} to its directly enclosing {@code ParenthesizedTree} if one exists.
   *
   * <p>This map is used by {@link CFGTranslationPhaseOne#addToLookupMap(Node)} to associate a
   * {@code ParenthesizedTree} with the dataflow {@code Node} that was used during inference. This
   * map is necessary because dataflow does not create a {@code Node} for a {@code
   * ParenthesizedTree}.
   */
  private final Map<Tree, ParenthesizedTree> parenMapping = new HashMap<>();

  @Override
  public Node visitParenthesized(ParenthesizedTree tree, Void p) {
    parenMapping.put(tree.getExpression(), tree);
    return scan(tree.getExpression(), p);
  }

  @Override
  public Node visitReturn(ReturnTree tree, Void p) {
    ExpressionTree ret = tree.getExpression();
    // TODO: also have a return-node if nothing is returned
    ReturnNode result = null;
    if (ret != null) {
      Node node = scan(ret, p);
      result = new ReturnNode(tree, node, env.getTypeUtils());
      returnNodes.add(result);
      extendWithNode(result);
    }

    extendWithExtendedNode(new UnconditionalJump(this.returnTargetLC.accessLabel()));

    return result;
  }

  @Override
  public Node visitMemberSelect(MemberSelectTree tree, Void p) {
    Node expr = scan(tree.getExpression(), p);
    if (!TreeUtils.isFieldAccess(tree)) {
      // Could be a selector of a class or package
      Element element = TreeUtils.elementFromUse(tree);
      if (ElementUtils.isTypeElement(element)) {
        ClassNameNode result = new ClassNameNode(tree, expr);
        extendWithClassNameNode(result);
        return result;
      } else if (element.getKind() == ElementKind.PACKAGE) {
        Node result = new PackageNameNode(tree, (PackageNameNode) expr);
        extendWithNode(result);
        return result;
      } else {
        throw new BugInCF("Unexpected element kind: " + element.getKind());
      }
    }

    Node node = new FieldAccessNode(tree, expr);

    Element element = TreeUtils.elementFromUse(tree);
    if (ElementUtils.isStatic(element)
        || expr instanceof ImplicitThisNode
        || expr instanceof ExplicitThisNode) {
      // No NullPointerException can be thrown, use normal node
      extendWithNode(node);
    } else {
      extendWithNodeWithException(node, nullPointerExceptionType);
    }

    return node;
  }

  @Override
  public Node visitEmptyStatement(EmptyStatementTree tree, Void p) {
    return null;
  }

  @Override
  public Node visitSynchronized(SynchronizedTree tree, Void p) {
    // see JLS 14.19

    Node synchronizedExpr = scan(tree.getExpression(), p);
    SynchronizedNode synchronizedStartNode =
        new SynchronizedNode(tree, synchronizedExpr, true, env.getTypeUtils());
    extendWithNode(synchronizedStartNode);
    scan(tree.getBlock(), p);
    SynchronizedNode synchronizedEndNode =
        new SynchronizedNode(tree, synchronizedExpr, false, env.getTypeUtils());
    extendWithNode(synchronizedEndNode);

    return null;
  }

  @Override
  public Node visitThrow(ThrowTree tree, Void p) {
    Node expression = scan(tree.getExpression(), p);
    TypeMirror exception = expression.getType();
    ThrowNode throwsNode = new ThrowNode(tree, expression, env.getTypeUtils());
    NodeWithExceptionsHolder exNode = extendWithNodeWithException(throwsNode, exception);
    exNode.setTerminatesExecution(true);
    return throwsNode;
  }

  @Override
  public Node visitCompilationUnit(CompilationUnitTree tree, Void p) {
    throw new BugInCF("CompilationUnitTree is unexpected in AST to CFG translation");
  }

  /**
   * Returns the first argument if it is non-null, otherwise return the second argument. Throws an
   * exception if both arguments are null.
   *
   * @param <A> the type of the arguments
   * @param first a reference
   * @param second a reference
   * @return the first argument that is non-null
   */
  private static <A> A firstNonNull(A first, A second) {
    if (first != null) {
      return first;
    } else if (second != null) {
      return second;
    } else {
      throw new NullPointerException();
    }
  }

  @Override
  public Node visitTry(TryTree tree, Void p) {
    List<? extends CatchTree> catches = tree.getCatches();
    BlockTree finallyBlock = tree.getFinallyBlock();

    extendWithNode(
        new MarkerNode(
            tree, "start of try statement #" + TreeUtils.treeUids.get(tree), env.getTypeUtils()));

    List<IPair<TypeMirror, Label>> catchLabels =
        CollectionsPlume.mapList(
            (CatchTree c) -> IPair.of(TreeUtils.typeOf(c.getParameter().getType()), new Label()),
            catches);

    // Store return/break/continue labels, just in case we need them for a finally block.
    LabelCell oldReturnTargetLC = returnTargetLC;
    LabelCell oldBreakTargetLC = breakTargetLC;
    Map<Name, Label> oldBreakLabels = breakLabels;
    LabelCell oldContinueTargetLC = continueTargetLC;
    Map<Name, Label> oldContinueLabels = continueLabels;

    Label finallyLabel = null;
    Label exceptionalFinallyLabel = null;

    if (finallyBlock != null) {
      finallyLabel = new Label();

      exceptionalFinallyLabel = new Label();
      tryStack.pushFrame(new TryFinallyFrame(exceptionalFinallyLabel));

      returnTargetLC = new LabelCell();

      breakTargetLC = new LabelCell();
      breakLabels = new TryFinallyScopeMap();

      continueTargetLC = new LabelCell();
      continueLabels = new TryFinallyScopeMap();
    }

    Label doneLabel = new Label();

    tryStack.pushFrame(new TryCatchFrame(types, catchLabels));

    extendWithNode(
        new MarkerNode(
            tree, "start of try block #" + TreeUtils.treeUids.get(tree), env.getTypeUtils()));

    handleTryResourcesAndBlock(tree, p, tree.getResources());

    extendWithNode(
        new MarkerNode(
            tree, "end of try block #" + TreeUtils.treeUids.get(tree), env.getTypeUtils()));

    extendWithExtendedNode(new UnconditionalJump(firstNonNull(finallyLabel, doneLabel)));

    // This pops the try-catch frame
    tryStack.popFrame();

    int catchIndex = 0;
    for (CatchTree c : catches) {
      addLabelForNextNode(catchLabels.get(catchIndex).second);
      TypeMirror catchType = TreeUtils.typeOf(c.getParameter().getType());
      extendWithNode(new CatchMarkerNode(tree, "start", catchType, env.getTypeUtils()));
      scan(c, p);
      extendWithNode(new CatchMarkerNode(tree, "end", catchType, env.getTypeUtils()));

      catchIndex++;
      extendWithExtendedNode(new UnconditionalJump(firstNonNull(finallyLabel, doneLabel)));
    }

    if (finallyLabel != null) {
      handleFinally(
          tree,
          doneLabel,
          finallyLabel,
          exceptionalFinallyLabel,
          () -> scan(finallyBlock, p),
          oldReturnTargetLC,
          oldBreakTargetLC,
          oldBreakLabels,
          oldContinueTargetLC,
          oldContinueLabels);
    }

    addLabelForNextNode(doneLabel);

    return null;
  }

  /**
   * A recursive helper method to handle the resource declarations (if any) in a {@link TryTree} and
   * its main block. If the {@code resources} list is empty, the method scans the main block of the
   * try statement and returns. Otherwise, the first resource declaration in {@code resources} is
   * desugared, following the logic in JLS 14.20.3.1. A resource declaration <i>r</i> is desugared
   * by adding the nodes for <i>r</i> itself to the CFG, followed by a synthetic nested {@code try}
   * block and {@code finally} block. The synthetic {@code try} block contains any remaining
   * resource declarations and the original try block (handled via recursion). The synthetic {@code
   * finally} block contains a call to {@code close} for <i>r</i>, guaranteeing that on every path
   * through the CFG, <i>r</i> is closed.
   *
   * @param tryTree the original try tree (with 0 or more resources) from the AST
   * @param p the value to pass to calls to {@code scan}
   * @param resources the remaining resource declarations to handle
   */
  private void handleTryResourcesAndBlock(TryTree tryTree, Void p, List<? extends Tree> resources) {
    if (resources.isEmpty()) {
      // Either `tryTree` was not a try-with-resources, or this method was called
      // recursively and all the resources have been handled.  Just scan the main try block.
      scan(tryTree.getBlock(), p);
      return;
    }

    // Handle the first resource declaration in the list.  The rest will be handled by a
    // recursive call.
    Tree resourceDeclarationTree = resources.get(0);

    extendWithNode(
        new MarkerNode(
            resourceDeclarationTree,
            "start of try for resource #" + TreeUtils.treeUids.get(resourceDeclarationTree),
            env.getTypeUtils()));

    // Store return/break/continue labels.  Generating a synthetic finally block for closing the
    // resource requires creating fresh return/break/continue labels and then restoring the old
    // labels afterward.
    LabelCell oldReturnTargetLC = returnTargetLC;
    LabelCell oldBreakTargetLC = breakTargetLC;
    Map<Name, Label> oldBreakLabels = breakLabels;
    LabelCell oldContinueTargetLC = continueTargetLC;
    Map<Name, Label> oldContinueLabels = continueLabels;

    // Add nodes for the resource declaration to the CFG.  NOTE: it is critical to add these
    // nodes *before* pushing a TryFinallyFrame for the finally block that will close the
    // resource.  If any exception occurs due to code within the resource declaration, the
    // corresponding variable or field is *not* automatically closed (as it was never
    // assigned a value).
    Node resourceCloseNode = scan(resourceDeclarationTree, p);

    // Now, set things up for our synthetic finally block that closes the resource.
    Label doneLabel = new Label();
    Label finallyLabel = new Label();

    Label exceptionalFinallyLabel = new Label();
    tryStack.pushFrame(new TryFinallyFrame(exceptionalFinallyLabel));

    returnTargetLC = new LabelCell();

    breakTargetLC = new LabelCell();
    breakLabels = new TryFinallyScopeMap();

    continueTargetLC = new LabelCell();
    continueLabels = new TryFinallyScopeMap();

    extendWithNode(
        new MarkerNode(
            resourceDeclarationTree,
            "start of try block for resource #" + TreeUtils.treeUids.get(resourceDeclarationTree),
            env.getTypeUtils()));
    // Recursively handle any remaining resource declarations and the main block of the try
    handleTryResourcesAndBlock(tryTree, p, resources.subList(1, resources.size()));
    extendWithNode(
        new MarkerNode(
            resourceDeclarationTree,
            "end of try block for resource #" + TreeUtils.treeUids.get(resourceDeclarationTree),
            env.getTypeUtils()));

    extendWithExtendedNode(new UnconditionalJump(finallyLabel));

    // Generate the finally block that closes the resource
    handleFinally(
        resourceDeclarationTree,
        doneLabel,
        finallyLabel,
        exceptionalFinallyLabel,
        () -> addCloseCallForResource(resourceDeclarationTree, resourceCloseNode),
        oldReturnTargetLC,
        oldBreakTargetLC,
        oldBreakLabels,
        oldContinueTargetLC,
        oldContinueLabels);

    addLabelForNextNode(doneLabel);
  }

  /**
   * Adds a synthetic {@code close} call to the CFG to close some resource variable declared or used
   * in a try-with-resources.
   *
   * @param resourceDeclarationTree the resource declaration
   * @param resourceToCloseNode node represented the variable or field on which {@code close} should
   *     be invoked
   */
  private void addCloseCallForResource(Tree resourceDeclarationTree, Node resourceToCloseNode) {
    Tree receiverTree = resourceDeclarationTree;
    if (receiverTree instanceof VariableTree) {
      receiverTree = treeBuilder.buildVariableUse((VariableTree) receiverTree);
      handleArtificialTree(receiverTree);
    }

    MemberSelectTree closeSelect =
        treeBuilder.buildCloseMethodAccess((ExpressionTree) receiverTree);
    handleArtificialTree(closeSelect);

    MethodInvocationTree closeCall = treeBuilder.buildMethodInvocation(closeSelect);
    handleArtificialTree(closeCall);

    Node receiverNode = resourceToCloseNode;
    if (receiverNode instanceof AssignmentNode) {
      // variable declaration; use the LHS
      receiverNode = ((AssignmentNode) resourceToCloseNode).getTarget();
    }
    // TODO do we need to insert some kind of node representing a use of receiverNode
    // (which can be either a LocalVariableNode or a FieldAccessNode)?
    MethodAccessNode closeAccessNode = new MethodAccessNode(closeSelect, receiverNode);
    closeAccessNode.setInSource(false);
    extendWithNode(closeAccessNode);
    MethodInvocationNode closeCallNode =
        new MethodInvocationNode(
            closeCall, closeAccessNode, Collections.emptyList(), getCurrentPath());
    closeCallNode.setInSource(false);
    extendWithMethodInvocationNode(TreeUtils.elementFromUse(closeCall), closeCallNode);
  }

  /**
   * Shared logic for CFG generation for a finally block. The block may correspond to a {@link
   * TryTree} originally in the source code, or it may be a synthetic finally block used to model
   * closing of a resource due to try-with-resources.
   *
   * @param markerTree tree to reference when creating {@link MarkerNode}s for the finally block
   * @param doneLabel label for the normal successor of the try block (no exceptions, returns,
   *     breaks, or continues)
   * @param finallyLabel label for the entry of the finally block for the normal case
   * @param exceptionalFinallyLabel label for entry of the finally block for when the try block
   *     throws an exception
   * @param finallyBlockCFGGenerator generates CFG nodes and edges for the finally block
   * @param oldReturnTargetLC old return target label cell, which gets restored to {@link
   *     #returnTargetLC} while handling the finally block
   * @param oldBreakTargetLC old break target label cell, which gets restored to {@link
   *     #breakTargetLC} while handling the finally block
   * @param oldBreakLabels old break labels, which get restored to {@link #breakLabels} while
   *     handling the finally block
   * @param oldContinueTargetLC old continue target label cell, which gets restored to {@link
   *     #continueTargetLC} while handling the finally block
   * @param oldContinueLabels old continue labels, which get restored to {@link #continueLabels}
   *     while handling the finally block
   */
  private void handleFinally(
      Tree markerTree,
      Label doneLabel,
      Label finallyLabel,
      Label exceptionalFinallyLabel,
      Runnable finallyBlockCFGGenerator,
      LabelCell oldReturnTargetLC,
      LabelCell oldBreakTargetLC,
      Map<Name, Label> oldBreakLabels,
      LabelCell oldContinueTargetLC,
      Map<Name, Label> oldContinueLabels) {
    // Reset values before analyzing the finally block!

    tryStack.popFrame();

    { // Scan 'finallyBlock' for only 'finallyLabel' (a successful path)
      addLabelForNextNode(finallyLabel);
      extendWithNode(
          new MarkerNode(
              markerTree,
              "start of finally block #" + TreeUtils.treeUids.get(markerTree),
              env.getTypeUtils()));
      finallyBlockCFGGenerator.run();
      extendWithNode(
          new MarkerNode(
              markerTree,
              "end of finally block #" + TreeUtils.treeUids.get(markerTree),
              env.getTypeUtils()));
      extendWithExtendedNode(new UnconditionalJump(doneLabel));
    }

    if (hasExceptionalPath(exceptionalFinallyLabel)) {
      // If an exceptional path exists, scan 'finallyBlock' for 'exceptionalFinallyLabel',
      // and scan copied 'finallyBlock' for 'finallyLabel' (a successful path). If there
      // is no successful path, it will be removed in later phase.
      // TODO: Don't we need a separate finally block for each kind of exception?
      addLabelForNextNode(exceptionalFinallyLabel);
      extendWithNode(
          new MarkerNode(
              markerTree,
              "start of finally block for Throwable #" + TreeUtils.treeUids.get(markerTree),
              env.getTypeUtils()));

      finallyBlockCFGGenerator.run();

      NodeWithExceptionsHolder throwing =
          extendWithNodeWithException(
              new MarkerNode(
                  markerTree,
                  "end of finally block for Throwable #" + TreeUtils.treeUids.get(markerTree),
                  env.getTypeUtils()),
              throwableType);

      throwing.setTerminatesExecution(true);
    }

    if (returnTargetLC.wasAccessed()) {
      addLabelForNextNode(returnTargetLC.peekLabel());
      returnTargetLC = oldReturnTargetLC;

      extendWithNode(
          new MarkerNode(
              markerTree,
              "start of finally block for return #" + TreeUtils.treeUids.get(markerTree),
              env.getTypeUtils()));
      finallyBlockCFGGenerator.run();
      extendWithNode(
          new MarkerNode(
              markerTree,
              "end of finally block for return #" + TreeUtils.treeUids.get(markerTree),
              env.getTypeUtils()));
      extendWithExtendedNode(new UnconditionalJump(returnTargetLC.accessLabel()));
    } else {
      returnTargetLC = oldReturnTargetLC;
    }

    if (breakTargetLC.wasAccessed()) {
      addLabelForNextNode(breakTargetLC.peekLabel());
      breakTargetLC = oldBreakTargetLC;

      extendWithNode(
          new MarkerNode(
              markerTree,
              "start of finally block for break #" + TreeUtils.treeUids.get(markerTree),
              env.getTypeUtils()));
      finallyBlockCFGGenerator.run();
      extendWithNode(
          new MarkerNode(
              markerTree,
              "end of finally block for break #" + TreeUtils.treeUids.get(markerTree),
              env.getTypeUtils()));
      extendWithExtendedNode(new UnconditionalJump(breakTargetLC.accessLabel()));
    } else {
      breakTargetLC = oldBreakTargetLC;
    }

    Map<Name, Label> accessedBreakLabels = ((TryFinallyScopeMap) breakLabels).getAccessedNames();
    if (!accessedBreakLabels.isEmpty()) {
      breakLabels = oldBreakLabels;

      for (Map.Entry<Name, Label> access : accessedBreakLabels.entrySet()) {
        addLabelForNextNode(access.getValue());
        extendWithNode(
            new MarkerNode(
                markerTree,
                "start of finally block for break label "
                    + access.getKey()
                    + " #"
                    + TreeUtils.treeUids.get(markerTree),
                env.getTypeUtils()));
        finallyBlockCFGGenerator.run();
        extendWithNode(
            new MarkerNode(
                markerTree,
                "end of finally block for break label "
                    + access.getKey()
                    + " #"
                    + TreeUtils.treeUids.get(markerTree),
                env.getTypeUtils()));
        extendWithExtendedNode(new UnconditionalJump(breakLabels.get(access.getKey())));
      }
    } else {
      breakLabels = oldBreakLabels;
    }

    if (continueTargetLC.wasAccessed()) {
      addLabelForNextNode(continueTargetLC.peekLabel());
      continueTargetLC = oldContinueTargetLC;

      extendWithNode(
          new MarkerNode(
              markerTree,
              "start of finally block for continue #" + TreeUtils.treeUids.get(markerTree),
              env.getTypeUtils()));
      finallyBlockCFGGenerator.run();
      extendWithNode(
          new MarkerNode(
              markerTree,
              "end of finally block for continue #" + TreeUtils.treeUids.get(markerTree),
              env.getTypeUtils()));
      extendWithExtendedNode(new UnconditionalJump(continueTargetLC.accessLabel()));
    } else {
      continueTargetLC = oldContinueTargetLC;
    }

    Map<Name, Label> accessedContinueLabels =
        ((TryFinallyScopeMap) continueLabels).getAccessedNames();
    if (!accessedContinueLabels.isEmpty()) {
      continueLabels = oldContinueLabels;

      for (Map.Entry<Name, Label> access : accessedContinueLabels.entrySet()) {
        addLabelForNextNode(access.getValue());
        extendWithNode(
            new MarkerNode(
                markerTree,
                "start of finally block for continue label "
                    + access.getKey()
                    + " #"
                    + TreeUtils.treeUids.get(markerTree),
                env.getTypeUtils()));
        finallyBlockCFGGenerator.run();
        extendWithNode(
            new MarkerNode(
                markerTree,
                "end of finally block for continue label "
                    + access.getKey()
                    + " #"
                    + TreeUtils.treeUids.get(markerTree),
                env.getTypeUtils()));
        extendWithExtendedNode(new UnconditionalJump(continueLabels.get(access.getKey())));
      }
    } else {
      continueLabels = oldContinueLabels;
    }
  }

  /**
   * Returns true if an exceptional node for {@code target} exists in {@link #nodeList}.
   *
   * @param target label for exception
   * @return true when an exceptional node for {@code target} exists in {@link #nodeList}
   */
  private boolean hasExceptionalPath(Label target) {
    for (ExtendedNode node : nodeList) {
      if (node instanceof NodeWithExceptionsHolder) {
        NodeWithExceptionsHolder exceptionalNode = (NodeWithExceptionsHolder) node;
        for (Set<Label> labels : exceptionalNode.getExceptions().values()) {
          if (labels.contains(target)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public Node visitParameterizedType(ParameterizedTypeTree tree, Void p) {
    Node result = new ParameterizedTypeNode(tree);
    extendWithNode(result);
    return result;
  }

  @Override
  public Node visitUnionType(UnionTypeTree tree, Void p) {
    throw new BugInCF("UnionTypeTree is unexpected in AST to CFG translation: " + tree);
  }

  @Override
  public Node visitArrayType(ArrayTypeTree tree, Void p) {
    Node result = new ArrayTypeNode(tree, types);
    extendWithNode(result);
    return result;
  }

  @Override
  public Node visitTypeCast(TypeCastTree tree, Void p) {
    Node operand = scan(tree.getExpression(), p);
    TypeMirror type = TreeUtils.typeOf(tree.getType());
    Node node = new TypeCastNode(tree, operand, type, types);

    extendWithNodeWithException(node, classCastExceptionType);
    return node;
  }

  @Override
  public Node visitPrimitiveType(PrimitiveTypeTree tree, Void p) {
    Node result = new PrimitiveTypeNode(tree, types);
    extendWithNode(result);
    return result;
  }

  @Override
  public Node visitTypeParameter(TypeParameterTree tree, Void p) {
    throw new BugInCF("TypeParameterTree is unexpected in AST to CFG translation");
  }

  @Override
  public Node visitInstanceOf(InstanceOfTree tree, Void p) {
    InstanceOfNode instanceOfNode;
    Node operand = scan(tree.getExpression(), p);
    Tree patternTree = InstanceOfUtils.getPattern(tree);
    if (patternTree != null) {
      Node pattern = scan(patternTree, p);
      instanceOfNode = new InstanceOfNode(tree, operand, pattern, pattern.getType(), types);
    } else {
      TypeMirror refType = TreeUtils.typeOf(tree.getType());
      instanceOfNode = new InstanceOfNode(tree, operand, refType, types);
    }
    extendWithNode(instanceOfNode);
    return instanceOfNode;
  }

  @Override
  public Node visitUnary(UnaryTree tree, Void p) {
    Node result;
    Tree.Kind kind = tree.getKind();
    switch (kind) {
      case BITWISE_COMPLEMENT:
      case UNARY_MINUS:
      case UNARY_PLUS:
        {
          // see JLS 15.14 and 15.15
          Node expr = scan(tree.getExpression(), p);
          expr = unaryNumericPromotion(expr);

          // TypeMirror exprType = InternalUtils.typeOf(tree);

          switch (kind) {
            case BITWISE_COMPLEMENT:
              result = new BitwiseComplementNode(tree, expr);
              break;
            case UNARY_MINUS:
              result = new NumericalMinusNode(tree, expr);
              break;
            case UNARY_PLUS:
              result = new NumericalPlusNode(tree, expr);
              break;
            default:
              throw new BugInCF("Unexpected unary tree kind: " + kind);
          }
          extendWithNode(result);
          return result;
        }

      case LOGICAL_COMPLEMENT:
        {
          // see JLS 15.15.6
          Node expr = scan(tree.getExpression(), p);
          result = new ConditionalNotNode(tree, unbox(expr));
          extendWithNode(result);
          return result;
        }

      case POSTFIX_DECREMENT:
      case POSTFIX_INCREMENT:
      case PREFIX_DECREMENT:
      case PREFIX_INCREMENT:
        {
          ExpressionTree exprTree = tree.getExpression();
          Node expr = scan(exprTree, p);

          boolean isIncrement =
              kind == Tree.Kind.POSTFIX_INCREMENT || kind == Tree.Kind.PREFIX_INCREMENT;
          boolean isPostfix =
              kind == Tree.Kind.POSTFIX_INCREMENT || kind == Tree.Kind.POSTFIX_DECREMENT;

          result = null; // for definite assignment; it is assigned in isPostfix and in !isPostfix
          if (isPostfix) {
            TypeMirror exprType = TreeUtils.typeOf(exprTree);
            VariableTree tempVarDecl =
                treeBuilder.buildVariableDecl(
                    exprType,
                    uniqueName("tempPostfix"),
                    TreePathUtil.findNearestEnclosingElement(getCurrentPath()),
                    tree.getExpression());
            handleArtificialTree(tempVarDecl);
            VariableDeclarationNode tempVarDeclNode = new VariableDeclarationNode(tempVarDecl);
            tempVarDeclNode.setInSource(false);
            extendWithNode(tempVarDeclNode);

            Tree tempVar = treeBuilder.buildVariableUse(tempVarDecl);
            handleArtificialTree(tempVar);
            Node tempVarNode = new LocalVariableNode(tempVar);
            tempVarNode.setInSource(false);
            extendWithNode(tempVarNode);

            AssignmentNode tempAssignNode = new AssignmentNode(tree, tempVarNode, expr);
            tempAssignNode.setInSource(false);
            extendWithNode(tempAssignNode);

            Tree resultExpr = treeBuilder.buildVariableUse(tempVarDecl);
            handleArtificialTree(resultExpr);
            result = new LocalVariableNode(resultExpr);
            result.setInSource(false);
            extendWithNode(result);
          }
          AssignmentNode unaryAssign =
              createIncrementOrDecrementAssign(tree, expr, isIncrement, isPostfix);
          if (!isPostfix) {
            result = unaryAssign;
          }
          return result;
        }

      case OTHER:
      default:
        // special node NLLCHK
        if (tree.toString().startsWith("<*nullchk*>")) {
          Node expr = scan(tree.getExpression(), p);
          result = new NullChkNode(tree, expr);
          extendWithNode(result);
          return result;
        }

        throw new BugInCF("Unknown kind (" + kind + ") of unary expression: " + tree);
    }
  }

  /**
   * Create assignment node which represent increment or decrement.
   *
   * @param unaryTree increment or decrement tree
   * @param expr expression node to be incremented or decremented
   * @param isIncrement true when it's increment
   * @param isPostfix true if {@code expr} is a postfix increment or decrement
   * @return assignment node for corresponding increment or decrement
   */
  private AssignmentNode createIncrementOrDecrementAssign(
      UnaryTree unaryTree, Node expr, boolean isIncrement, boolean isPostfix) {
    ExpressionTree exprTree = (ExpressionTree) expr.getTree();
    TypeMirror exprType = expr.getType();
    TypeMirror oneType = types.getPrimitiveType(TypeKind.INT);
    TypeMirror promotedType = binaryPromotedType(exprType, oneType);

    LiteralTree oneTree = treeBuilder.buildLiteral(1);
    handleArtificialTree(oneTree);

    Node exprRHS = binaryNumericPromotion(expr, promotedType);
    Node one = new IntegerLiteralNode(oneTree);
    one.setInSource(false);
    extendWithNode(one);
    one = binaryNumericPromotion(one, promotedType);

    BinaryTree operTree =
        treeBuilder.buildBinary(
            promotedType, isIncrement ? Tree.Kind.PLUS : Tree.Kind.MINUS, exprTree, oneTree);
    if (isPostfix) {
      postfixTreeToCfgNodes.put(unaryTree, operTree);
    }
    handleArtificialTree(operTree);

    Node operNode;
    if (isIncrement) {
      operNode = new NumericalAdditionNode(operTree, exprRHS, one);
    } else {
      operNode = new NumericalSubtractionNode(operTree, exprRHS, one);
    }
    operNode.setInSource(false);
    extendWithNode(operNode);

    Node narrowed = narrowAndBox(operNode, exprType);

    Tree target;
    if (isPostfix) {
      target = treeBuilder.buildAssignment(exprTree, (ExpressionTree) narrowed.getTree());
      handleArtificialTree(target);
    } else {
      target = unaryTree;
    }

    AssignmentNode assignNode = new AssignmentNode(target, expr, narrowed);
    assignNode.setInSource(false);
    extendWithNode(assignNode);
    return assignNode;
  }

  @Override
  public Node visitVariable(VariableTree tree, Void p) {

    // see JLS 14.4

    boolean isField = false;
    if (getCurrentPath().getParentPath() != null) {
      Tree.Kind kind = TreeUtils.getKindRecordAsClass(getCurrentPath().getParentPath().getLeaf());
      // CLASS includes records.
      if (kind == Tree.Kind.CLASS || kind == Tree.Kind.INTERFACE || kind == Tree.Kind.ENUM) {
        isField = true;
      }
    }
    Node node = null;

    ClassTree enclosingClass = TreePathUtil.enclosingClass(getCurrentPath());
    TypeElement classElem = TreeUtils.elementFromDeclaration(enclosingClass);
    Node receiver = new ImplicitThisNode(classElem.asType());

    if (isField) {
      ExpressionTree initializer = tree.getInitializer();
      assert initializer != null;
      node =
          translateAssignment(
              tree,
              new FieldAccessNode(tree, TreeUtils.elementFromDeclaration(tree), receiver),
              initializer);
    } else {
      // local variable definition
      VariableDeclarationNode decl = new VariableDeclarationNode(tree);
      extendWithNode(decl);

      // initializer

      ExpressionTree initializer = tree.getInitializer();
      if (initializer != null) {
        node = translateAssignment(tree, new LocalVariableNode(tree, receiver), initializer);
      }
    }

    return node;
  }

  @Override
  public Node visitWhileLoop(WhileLoopTree tree, Void p) {
    Name parentLabel = getLabel(getCurrentPath());

    Label loopEntry = new Label();
    Label loopExit = new Label();

    // If the loop is a labeled statement, then its continue target is identical for continues
    // with no label and continues with the loop's label.
    Label conditionStart;
    if (parentLabel != null) {
      conditionStart = continueLabels.get(parentLabel);
    } else {
      conditionStart = new Label();
    }

    LabelCell oldBreakTargetLC = breakTargetLC;
    breakTargetLC = new LabelCell(loopExit);

    LabelCell oldContinueTargetLC = continueTargetLC;
    continueTargetLC = new LabelCell(conditionStart);

    // Condition
    addLabelForNextNode(conditionStart);
    assert tree.getCondition() != null;
    // Determine whether the loop condition has the constant value true, according to the
    // compiler logic.
    boolean isCondConstTrue = TreeUtils.isExprConstTrue(tree.getCondition());

    unbox(scan(tree.getCondition(), p));

    if (!isCondConstTrue) {
      // If the loop condition does not have the constant value true, the control flow is
      // split into two branches.
      ConditionalJump cjump = new ConditionalJump(loopEntry, loopExit);
      extendWithExtendedNode(cjump);
    }

    // Loop body
    addLabelForNextNode(loopEntry);
    assert tree.getStatement() != null;
    scan(tree.getStatement(), p);

    if (isCondConstTrue) {
      // The condition has the constant value true, so we can directly jump back to the loop
      // entry.
      extendWithExtendedNode(new UnconditionalJump(loopEntry));
    } else {
      // Otherwise, jump back to evaluate the condition.
      extendWithExtendedNode(new UnconditionalJump(conditionStart));
    }

    // Loop exit
    addLabelForNextNode(loopExit);

    breakTargetLC = oldBreakTargetLC;
    continueTargetLC = oldContinueTargetLC;

    return null;
  }

  @Override
  public Node visitLambdaExpression(LambdaExpressionTree tree, Void p) {
    declaredLambdas.add(tree);
    Node node = new FunctionalInterfaceNode(tree);
    extendWithNode(node);
    return node;
  }

  @Override
  public Node visitMemberReference(MemberReferenceTree tree, Void p) {
    Tree enclosingExpr = tree.getQualifierExpression();
    if (enclosingExpr != null) {
      scan(enclosingExpr, p);
    }

    Node node = new FunctionalInterfaceNode(tree);
    extendWithNode(node);

    return node;
  }

  @Override
  public Node visitWildcard(WildcardTree tree, Void p) {
    throw new BugInCF("WildcardTree is unexpected in AST to CFG translation");
  }

  @Override
  public Node visitOther(Tree tree, Void p) {
    throw new BugInCF("Unknown AST element encountered in AST to CFG translation.");
  }

  /**
   * Returns the TypeMirror for the given class.
   *
   * @param clazz a class
   * @return the TypeMirror for the class
   */
  protected TypeMirror getTypeMirror(Class<?> clazz) {
    return TypesUtils.typeFromClass(clazz, types, elements);
  }

  /**
   * Returns the TypeMirror for the given class, or {@code null} if the type is not present.
   *
   * <p>This can be used to handle system types that are not present. For example, in Java code that
   * is translated to JavaScript using j2cl, the custom bootclasspath contains APIs that are
   * emulated in JavaScript, so some types such as OutOfMemoryError are deliberately not present.
   *
   * @param clazz a class, which must have a canonical name
   * @return the TypeMirror for the class, or {@code null} if the type is not present
   */
  protected @Nullable TypeMirror maybeGetTypeMirror(Class<?> clazz) {
    String name = clazz.getCanonicalName();
    assert name != null : clazz + " does not have a canonical name";
    TypeElement element = elements.getTypeElement(name);
    if (element == null) {
      return null;
    }
    return element.asType();
  }
}
