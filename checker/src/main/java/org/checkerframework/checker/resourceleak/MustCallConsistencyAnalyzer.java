package org.checkerframework.checker.resourceleak;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.mustcall.CreatesMustCallForElementSupplier;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.Kind;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.Block.BlockType;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.cfg.node.ThisNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.StringsPlume;

/**
 * An analyzer that checks consistency of {@link MustCall} and {@link CalledMethods} types, thereby
 * detecting resource leaks. For any expression <em>e</em> the analyzer ensures that when <em>e</em>
 * goes out of scope, there exists a resource alias <em>r</em> of <em>e</em> (which might be
 * <em>e</em> itself) such that the must-call methods of <em>r</em> (i.e. the values of <em>r</em>'s
 * MustCall type) are contained in the value of <em>r</em>'s CalledMethods type. For any <em>e</em>
 * for which this property does not hold, the analyzer reports a {@code
 * "required.method.not.called"} error, indicating a possible resource leak.
 *
 * <p>Mechanically, the analysis tracks dataflow facts about the obligations of sets of
 * resource-aliases that refer to the same resource, and checks their must-call and called-methods
 * types when the last reference to those sets goes out of scope. That is, this class implements a
 * lightweight alias analysis that tracks must-alias sets for resources.
 *
 * <p>Class {@link Obligation} represents a single such dataflow fact. Abstractly, each dataflow
 * fact is a pair: a set of resource aliases to some resource, and the list of must-call methods
 * that need to be called on one of the resource aliases. Concretely, the Must Call Checker is
 * responsible for tracking the latter - an expression's must-call type indicates which methods must
 * be called - so this dataflow analysis only actually tracks the sets of resource aliases. When the
 * last resource alias in an Obligation goes out-of-scope, the analysis queries the Must Call
 * Checker to get the list of must-call methods to check against.
 *
 * <p>The algorithm here adds, modifies, or removes obligations from those it is tracking when
 * certain code patterns are encountered, to account for ownership transfer. Here are non-exhaustive
 * examples:
 *
 * <ul>
 *   <li>A new obligation is added to the tracked set when a constructor or a method with an owning
 *       return is invoked.
 *   <li>An obligation is modified when an expression with a tracked obligation is assigned to a
 *       local variable or a resource-alias method or constructor is called (the new local or the
 *       result of the resource-alias method/constructor is added to the existing resource alias
 *       set).
 *   <li>An obligation can be removed when a member of a resource-alias set is assigned to an owning
 *       field or passed to a method in a parameter location that is annotated as {@code @Owning}.
 * </ul>
 *
 * <p>Throughout, this class uses the temporary-variable facilities provided by the Must Call and
 * Resource Leak type factories to both emulate a three-address-form IR, simplifying some analysis
 * logic, and to permit expressions to have their types refined in their respective checkers'
 * stores. These temporary variables can be members of resource-alias sets. Without temporary
 * variables, the checker wouldn't be able to verify code such as {@code new Socket(host,
 * port).close()}, which would cause false positives. Temporaries are created for {@code new}
 * expressions, method calls (for the return value), and ternary expressions. Other types of
 * expressions may also be supported in the future.
 */
/* package-private */
class MustCallConsistencyAnalyzer {

  /**
   * Aliases through which the checker has already reported about a resource leak, to avoid
   * duplicate reports.
   */
  private final Set<ResourceAlias> reportedErrorAliases = new HashSet<>();

  /**
   * The type factory for the Resource Leak Checker, which is used to get called methods types and
   * to access the Must Call Checker.
   */
  private final ResourceLeakAnnotatedTypeFactory typeFactory;

  /** The Resource Leak Checker, used to issue errors. */
  private final ResourceLeakChecker checker;

  /** The analysis from the Resource Leak Checker, used to get input stores based on CFG blocks. */
  private final CFAnalysis analysis;

  /**
   * An Obligation is the abstraction used by this dataflow analysis for a must-call obligation. A
   * must-call obligation has two parts: the list of one or more methods that must be called, and
   * the set of "resource aliases" on which calling the required method(s) are equivalent. This
   * dataflow analysis (and therefore this class) tracks only the latter; the former are tracked by
   * the {@link MustCallChecker} and are accessed by looking up the type(s) in its type system of
   * the resource aliases contained in each obligation.
   */
  private static class Obligation {

    /**
     * The set of resource aliases through which this must-call obligation can be satisfied. {@code
     * Obligation} is deeply immutable. If some code were to accidentally mutate a {@code
     * resourceAliases} set it could be really nasty to debug, so this set is always immutable.
     */
    public final ImmutableSet<ResourceAlias> resourceAliases;

    /**
     * Create an obligation from a set of resource aliases.
     *
     * @param resourceAliases a set of resource aliases
     */
    public Obligation(Set<ResourceAlias> resourceAliases) {
      this.resourceAliases = ImmutableSet.copyOf(resourceAliases);
    }

    /**
     * Returns the resource alias in this obligation's resource alias set corresponding to {@code
     * localVariableNode} if one is present. Otherwise, returns null.
     *
     * @param localVariableNode some local variable
     * @return the resource alias corresponding to {@code localVariableNode} if one is present;
     *     otherwise, null
     */
    private @Nullable ResourceAlias getResourceAlias(LocalVariableNode localVariableNode) {
      Element element = localVariableNode.getElement();
      for (ResourceAlias alias : resourceAliases) {
        if (alias.reference.getElement().equals(element)) {
          return alias;
        }
      }
      return null;
    }

    /**
     * Returns true if this contains a resource alias corresponding to {@code localVariableNode},
     * meaning that calling the required methods on {@code localVariableNode} is sufficient to
     * satisfy the must-call obligation this object represents.
     *
     * @param localVariableNode some local variable node
     * @return true if a resource alias corresponding to {@code localVariableNode} is present
     */
    private boolean canBeSatisfiedThrough(LocalVariableNode localVariableNode) {
      return getResourceAlias(localVariableNode) != null;
    }

    @Override
    public String toString() {
      return "Obligation: resourceAliases: " + Iterables.toString(resourceAliases);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Obligation that = (Obligation) obj;
      return this.resourceAliases.equals(that.resourceAliases);
    }

    @Override
    public int hashCode() {
      return Objects.hash(resourceAliases);
    }
  }

  /**
   * This class represents a single resource alias in a resource alias set. A resource alias is a
   * reference through which a must-call obligation can be satisfied; every must-call obligation has
   * one or more resource aliases, and calling the required method(s) through any of them satisfies
   * the obligation.
   *
   * <p>Internally, a resource alias is represented by a pair of a local or temporary variable (the
   * "reference" through which the must-call obligations for the alias set to which it belongs can
   * be satisfied) along with a tree that "assigns" the reference. The tree's primary purpose is
   * error reporting, but it is also used to determine if this resource alias is one of the
   * expressions in a ternary, which are treated specially by the algorithm (see {@link
   * #handleTernarySuccIfNeeded(Block, Block, Set)}).
   */
  /* package-private */ static class ResourceAlias {

    /**
     * The JavaExpression that represents the reference through which this resource can have its
     * must-call obligations satisfied. This is either a local variable actually defined in the
     * source code, or a temporary variable for an expression.
     */
    public final LocalVariable reference;

    /** The tree at which it was assigned, for error reporting. */
    public final Tree tree;

    /**
     * Create a new resource alias.
     *
     * @param reference the local variable
     * @param tree the tree
     */
    public ResourceAlias(LocalVariable reference, Tree tree) {
      this.reference = reference;
      this.tree = tree;
    }

    @Override
    public String toString() {
      return "(ResourceAlias: reference: " + reference + " |||| tree: " + tree + ")";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ResourceAlias that = (ResourceAlias) o;
      return reference.equals(that.reference) && tree.equals(that.tree);
    }

    @Override
    public int hashCode() {
      return Objects.hash(reference, tree);
    }
  }

  /**
   * Creates a consistency analyzer. Typically, the type factory's postAnalyze method would
   * instantiate a new consistency analyzer using this constructor and then call {@link
   * #analyze(ControlFlowGraph)}.
   *
   * @param typeFactory the type factory
   * @param analysis the analysis from the type factory. Usually this would have protected access,
   *     so this constructor cannot get it directly.
   */
  /* package-private */
  MustCallConsistencyAnalyzer(ResourceLeakAnnotatedTypeFactory typeFactory, CFAnalysis analysis) {
    this.typeFactory = typeFactory;
    this.checker = (ResourceLeakChecker) typeFactory.getChecker();
    this.analysis = analysis;
  }

  /**
   * The main function of the consistency dataflow analysis. The analysis tracks dataflow facts
   * ("obligations") of type {@link Obligation}, each representing a set of resource aliases for
   * some value with a non-empty {@code @MustCall} obligation. (It is not necessary to track
   * expressions with empty {@code @MustCall} obligations, because they are trivially fulfilled.)
   *
   * @param cfg the control flow graph of the method to check
   */
  // TODO: This analysis is currently implemented directly using a worklist; in the future, it
  // should be rewritten to use the dataflow framework of the Checker Framework.
  /* package-private */
  void analyze(ControlFlowGraph cfg) {
    // The `visited` set contains everything that has been added to the worklist, even if it has not
    // yet been removed and analyzed.
    Set<BlockWithObligations> visited = new LinkedHashSet<>();
    Deque<BlockWithObligations> worklist = new ArrayDeque<>();

    // Add any owning parameters to the initial set of variables to track.
    BlockWithObligations entry =
        new BlockWithObligations(cfg.getEntryBlock(), computeOwningParameters(cfg));
    worklist.add(entry);
    visited.add(entry);

    while (!worklist.isEmpty()) {
      BlockWithObligations current = worklist.remove();
      // A *mutable* set that eventually holds the set of obligations to be propagated to successor
      // blocks. The set is initialized to the current obligations and updated by the methods
      // invoked in the for loop below.
      Set<Obligation> obligations = new LinkedHashSet<>(current.obligations);

      for (Node node : current.block.getNodes()) {
        if (node instanceof AssignmentNode) {
          updateObligationsForAssignment((AssignmentNode) node, obligations);
        } else if (node instanceof ReturnNode) {
          updateObligationsForOwningReturn((ReturnNode) node, cfg, obligations);
        } else if (node instanceof MethodInvocationNode || node instanceof ObjectCreationNode) {
          updateObligationsForInvocation(obligations, node);
        }
        // All other types of nodes are ignored. This is safe, because other kinds of
        // nodes cannot create or modify the resource-alias sets that the algorithm is tracking.
      }

      propagateObligationsToSuccessorBlocks(visited, worklist, obligations, current.block);
    }
  }

  /**
   * Update a set of obligations to account for a method or constructor invocation.
   *
   * @param obligations the obligations to update
   * @param node the method or constructor invocation
   */
  private void updateObligationsForInvocation(Set<Obligation> obligations, Node node) {
    removeObligationsAtOwnershipTransferToParameters(obligations, node);
    if (node instanceof MethodInvocationNode
        && typeFactory.canCreateObligations()
        && typeFactory.hasCreatesMustCallFor((MethodInvocationNode) node)) {
      checkCreatesMustCallForInvocation(obligations, (MethodInvocationNode) node);
      // Count calls to @CreatesMustCallFor methods as creating new resources. Doing so could
      // result in slightly over-counting, because @CreatesMustCallFor doesn't guarantee that a
      // new resource is created: it just means that a new resource might have been created.
      incrementNumMustCall(node);
    }

    if (!shouldTrackInvocationResult(obligations, node)) {
      return;
    }

    if (typeFactory.hasDeclaredMustCall(node.getTree())) {
      // The incrementNumMustCall call above increments the count for the target of the
      // @CreatesMustCallFor annotation.  By contrast, this call increments the count for the return
      // value of the method (which can't be the target of the annotation, because our syntax
      // doesn't support that).
      incrementNumMustCall(node);
    }
    updateObligationsWithInvocationResult(obligations, node);
  }

  /**
   * Checks that an invocation of a CreatesMustCallFor method is valid.
   *
   * <p>Such an invocation is valid if for all expressions in the CreatesMustCallFor annotation one
   * of the following conditions is true: 1) the expression is an owning pointer, 2) the expression
   * already has a tracked obligation, or 3) the method in which the invocation occurs also has
   * an @CreatesMustCallFor annotation, with the same expression. {@link
   * #isValidCreatesMustCallForExpression(Set, JavaExpression, TreePath)} contains the logic for
   * checking an individual expression against these rules.
   *
   * <p>If none of the above are true, this method issues a reset.not.owning error.
   *
   * <p>For soundness, this method also guarantees that if any of the expressions has a tracked
   * obligation, any tracked resource aliases of it will be removed (lest the analysis conclude that
   * it is already closed because one of these aliases was closed before the method was invoked).
   * Aliases created after the CreatesMustCallFor method is invoked are still permitted.
   *
   * @param obligations the currently-tracked obligations; this value is side-effected if there is
   *     an obligation in it which tracks any expression from the CreatesMustCallFor annotation as
   *     one of its resource aliases
   * @param node a method invocation node, invoking a method with a CreatesMustCallFor annotation
   */
  private void checkCreatesMustCallForInvocation(
      Set<Obligation> obligations, MethodInvocationNode node) {

    TreePath currentPath = typeFactory.getPath(node.getTree());
    List<JavaExpression> expressions =
        CreatesMustCallForElementSupplier.getCreatesMustCallForExpressions(
            node, typeFactory, typeFactory);
    Set<JavaExpression> missing = new HashSet<>();
    for (JavaExpression expression : expressions) {
      if (!isValidCreatesMustCallForExpression(obligations, expression, currentPath)) {
        missing.add(expression);
      }
    }

    if (missing.isEmpty()) {
      // All expressions matched one of the rules, so the invocation is valid.
      return;
    }

    String missingStrs = StringsPlume.join(", ", missing);
    checker.reportError(node.getTree(), "reset.not.owning", missingStrs);
  }

  /**
   * Checks the validity of the given expression from an invoked method's {@link
   * org.checkerframework.checker.mustcall.qual.CreatesMustCallFor} annotation. See {@link
   * #checkCreatesMustCallForInvocation(Set, MethodInvocationNode)}.
   *
   * <p>An expression is valid if one of the following conditions is true: 1) the expression is an
   * owning pointer, 2) the expression already has a tracked obligation, or 3) the method in which
   * the invocation occurs also has an @CreatesMustCallFor annotation, with the same expression.
   *
   * @param obligations the currently-tracked obligations; this value is side-effected if there is
   *     an obligation in it which tracks {@code expression} as one of its resource aliases
   * @param expression an element of a method's @CreatesMustCallFor annotation
   * @param path the path on which the method from whose @CreateMustCallFor annotation {@code
   *     expression} came was invoked
   * @return true iff the expression is valid, as defined above
   */
  private boolean isValidCreatesMustCallForExpression(
      Set<Obligation> obligations, JavaExpression expression, TreePath path) {
    if (expression instanceof FieldAccess) {
      Element elt = ((FieldAccess) expression).getField();
      if (!checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
          && typeFactory.getDeclAnnotation(elt, Owning.class) != null) {
        // The expression is an Owning field.  This satisfies case 1.
        return true;
      }
    } else if (expression instanceof LocalVariable) {
      Element elt = ((LocalVariable) expression).getElement();
      if (!checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
          && typeFactory.getDeclAnnotation(elt, Owning.class) != null) {
        // The expression is an Owning param.  This satisfies case 1.
        return true;
      } else {
        Obligation toRemove = null;
        Obligation toAdd = null;
        for (Obligation obligation : obligations) {
          for (ResourceAlias alias : obligation.resourceAliases) {
            if (expression.equals(alias.reference)) {
              // This satisfies case 2 above. Remove all its aliases, then return below.
              if (toRemove != null) {
                throw new BugInCF(
                    "tried to remove multiple sets containing a reset expression at once");
              }
              toRemove = obligation;
              toAdd = new Obligation(ImmutableSet.of(alias));
            }
          }
        }

        if (toRemove != null) {
          obligations.remove(toRemove);
          obligations.add(toAdd);
          // This satisfies case 2.
          return true;
        }
      }
    }

    // TODO: Getting this every time is inefficient if a method has many @CreatesMustCallFor
    // annotations, but that should be a rare path.
    MethodTree enclosingMethodTree = TreePathUtil.enclosingMethod(path);
    if (enclosingMethodTree == null) {
      return false;
    }
    ExecutableElement enclosingMethodElt = TreeUtils.elementFromDeclaration(enclosingMethodTree);
    MustCallAnnotatedTypeFactory mcAtf =
        typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
    List<String> enclosingCmcfValues =
        ResourceLeakVisitor.getCreatesMustCallForValues(enclosingMethodElt, mcAtf, typeFactory);
    if (enclosingCmcfValues.isEmpty()) {
      return false;
    }
    for (String enclosingCmcfValue : enclosingCmcfValues) {
      JavaExpression enclosingTarget;
      try {
        enclosingTarget =
            StringToJavaExpression.atMethodBody(enclosingCmcfValue, enclosingMethodTree, checker);
      } catch (JavaExpressionParseException e) {
        // Do not issue an error here, because it would be a duplicate.
        // The error will be issued by the Transfer class of the checker,
        // via the CreatesMustCallForElementSupplier interface.
        enclosingTarget = null;
      }

      if (areSame(expression, enclosingTarget)) {
        // This satisfies case 3.
        return true;
      }
    }
    return false;
  }

  /**
   * Checks whether the two JavaExpressions are the same. This is identical to calling equals() on
   * one of them, with two exceptions: the second expression can be null, and "this" references are
   * compared using their underlying type. (ThisReference#equals always returns true, which is
   * probably a bug and isn't accurate in the case of nested classes.)
   *
   * @param target a JavaExpression
   * @param enclosingTarget another, possibly null, JavaExpression
   * @return true iff they represent the same program element
   */
  private boolean areSame(JavaExpression target, @Nullable JavaExpression enclosingTarget) {
    if (enclosingTarget == null) {
      return false;
    }
    if (enclosingTarget instanceof ThisReference && target instanceof ThisReference) {
      return enclosingTarget.getType().toString().equals(target.getType().toString());
    } else {
      return enclosingTarget.equals(target);
    }
  }

  /**
   * Given a node representing a method or constructor call, updates the set of obligations to
   * account for the result, which is treated as a new resource alias. Adds the new resource alias
   * to the set of an obligation in {@code obligations}: either an existing obligation if the result
   * is definitely resource-aliased with it, or a new obligation if not.
   *
   * @param obligations the currently-tracked obligations. This is always side-effected: either a
   *     new resource alias is added to the resource alias set of an existing obligation, or a new
   *     obligation with a single-element resource alias set is created and added.
   * @param node the node whose result is to be tracked; must be {@link MethodInvocationNode} or
   *     {@link ObjectCreationNode}
   */
  private void updateObligationsWithInvocationResult(Set<Obligation> obligations, Node node) {
    Tree tree = node.getTree();
    // Only track the result of the call if there is a temporary variable for the call node
    // (because if there is no temporary, then the invocation must produce an untrackable value,
    // such as a primitive type).
    LocalVariableNode tmpVar = typeFactory.getTempVarForNode(node);
    if (tmpVar == null) {
      return;
    }

    // `mustCallAlias` is the argument passed in the MustCallAlias position, if any exists,
    // otherwise null.
    Node mustCallAlias = getMustCallAliasArgumentNode(node);
    // If mustCallAlias is null and call returns @This, set mustCallAlias to the receiver.
    if (mustCallAlias == null
        && node instanceof MethodInvocationNode
        && typeFactory.returnsThis((MethodInvocationTree) tree)) {
      mustCallAlias =
          removeCastsAndGetTmpVarIfPresent(((MethodInvocationNode) node).getTarget().getReceiver());
    }

    ResourceAlias tmpVarAsResourceAlias = new ResourceAlias(new LocalVariable(tmpVar), tree);
    if (mustCallAlias instanceof FieldAccessNode) {
      // Do not track the call result if the MustCallAlias argument is a field.  Handling of
      // @Owning fields is a completely separate check, and there is never a need to track an alias
      // of a non-@Owning field, as by definition such a field does not have obligations!
    } else if (mustCallAlias instanceof LocalVariableNode) {
      // If mustCallAlias is a local variable already being tracked, add tmpVarAsResourceAlias
      // to the set containing mustCallAlias.
      Obligation obligationContainingMustCallAlias =
          getObligationForVar(obligations, (LocalVariableNode) mustCallAlias);
      if (obligationContainingMustCallAlias != null) {
        Set<ResourceAlias> newResourceAliasSet =
            FluentIterable.from(obligationContainingMustCallAlias.resourceAliases)
                .append(tmpVarAsResourceAlias)
                .toSet();
        obligations.remove(obligationContainingMustCallAlias);
        obligations.add(new Obligation(newResourceAliasSet));
      }
      // It is not an error if there is no obligation containing the must-call alias. In that
      // case, what has usually happened is that no obligation was created in the first place.
      // For example, when checking the invocation of a "wrapper stream" constructor, if the
      // argument in the must-call alias position is some stream with no obligations like a
      // ByteArrayInputStream, then no Obligation object will have been created for it and therefore
      // obligationContainingMustCallAlias will be null.
    } else {
      // If there is no MustCallAlias, create a new obligation for the resouce alias.
      obligations.add(new Obligation(ImmutableSet.of(tmpVarAsResourceAlias)));
    }
  }

  /**
   * Determines if the result of the given method or constructor invocation node should be tracked
   * in {@code obligations}. In some cases, there is no need to track the result because the
   * obligations are already satisfied in some other way or there cannot possibly be obligations
   * because of the structure of the code.
   *
   * <p>Specifically, an invocation result does NOT need to be tracked if any of the following is
   * true:
   *
   * <ul>
   *   <li>The invocation is a call to a {@code this()} or {@code super()} constructor.
   *   <li>The method's return type is annotated with MustCallAlias and the argument passed in this
   *       invocation in the corresponding position is an owning field.
   *   <li>The method's return type is non-owning, which can either be because the method has no
   *       return type or because the return type is annotated with {@link NotOwning}.
   * </ul>
   *
   * <p>This method can also side-effect {@code obligations}, if node is a super or this constructor
   * call with MustCallAlias annotations, by removing that obligation.
   *
   * @param obligations the current set of obligations
   * @param node the invocation node to check; must be {@link MethodInvocationNode} or {@link
   *     ObjectCreationNode}
   * @return true iff the result of node should be tracked in obligations
   */
  private boolean shouldTrackInvocationResult(Set<Obligation> obligations, Node node) {
    Tree callTree = node.getTree();
    if (callTree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      MethodInvocationTree methodInvokeTree = (MethodInvocationTree) callTree;

      if (TreeUtils.isSuperConstructorCall(methodInvokeTree)
          || TreeUtils.isThisConstructorCall(methodInvokeTree)) {
        Node mustCallAliasArgument = getMustCallAliasArgumentNode(node);
        // If the MustCallAlias argument is also in the set of obligations, then remove it -- its
        // obligation has been fulfilled by being passed on to the MustCallAlias constructor
        // (because a
        // this/super constructor call can only occur in the body of another constructor).
        if (mustCallAliasArgument instanceof LocalVariableNode) {
          removeObligationsContainingVar(obligations, (LocalVariableNode) mustCallAliasArgument);
        }
        return false;
      }
      return !returnTypeIsMustCallAliasWithUntrackable((MethodInvocationNode) node)
          && !hasNotOwningReturnType((MethodInvocationNode) node);
    }
    // Constructor results from new expressions are always owning.
    return true;
  }

  /**
   * Returns true if this node represents a method invocation of a must-call-alias method, where the
   * argument in the must-call-alias position is untrackable: an owning field or a pointer that is
   * guaranteed to be non-owning, such as {@code "this"} or a non-owning field. Owning fields are
   * handled by the rest of the checker, not by this algorithm, so they are "untrackable".
   * Non-owning fields and this nodes are guaranteed to be non-owning, and are therefore also
   * "untrackable". Because both owning and non-owning fields are untrackable (and there are no
   * other kinds of fields), this method returns true for all field accesses.
   *
   * @param node a method invocation node
   * @return true if this is the invocation of a method whose return type is MCA with an owning
   *     field or a definitely non-owning pointer
   */
  private boolean returnTypeIsMustCallAliasWithUntrackable(MethodInvocationNode node) {
    Node mustCallAliasArgument = getMustCallAliasArgumentNode(node);
    return mustCallAliasArgument instanceof FieldAccessNode
        || mustCallAliasArgument instanceof ThisNode;
  }

  /**
   * Checks if {@code node} is either directly enclosed by a {@link TypeCastNode} or is the then or
   * else operand of a {@link TernaryExpressionNode}, by looking at the successor block in the CFG.
   * These are all the cases where the enclosing operator is a "no-op" that evaluates to the same
   * value as {@code node} (in the appropriate case for a ternary expression). This method is only
   * used within {@link #propagateObligationsToSuccessorBlocks(Set, Deque, Set, Block)} to ensure
   * obligations are propagated to cast / ternary nodes properly. It relies on the assumption that a
   * {@link TypeCastNode} or {@link TernaryExpressionNode} will only appear in a CFG as the first
   * node in a block.
   *
   * @param node the CFG node
   * @return {@code true} if {@code node} is in a {@link SingleSuccessorBlock} {@code b}, the first
   *     {@link Node} in {@code b}'s successor block is a {@link TypeCastNode} or a {@link
   *     TernaryExpressionNode}, and {@code node} is an operand of the successor node; {@code false}
   *     otherwise
   */
  private boolean inCastOrTernary(Node node) {
    if (!(node.getBlock() instanceof SingleSuccessorBlock)) {
      return false;
    }
    Block successorBlock = ((SingleSuccessorBlock) node.getBlock()).getSuccessor();
    if (successorBlock != null) {
      List<Node> succNodes = successorBlock.getNodes();
      if (succNodes.size() > 0) {
        Node succNode = succNodes.get(0);
        if (succNode instanceof TypeCastNode) {
          return ((TypeCastNode) succNode).getOperand().equals(node);
        } else if (succNode instanceof TernaryExpressionNode) {
          TernaryExpressionNode ternaryExpressionNode = (TernaryExpressionNode) succNode;
          return ternaryExpressionNode.getThenOperand().equals(node)
              || ternaryExpressionNode.getElseOperand().equals(node);
        }
      }
    }
    return false;
  }

  /**
   * Transfer ownership of any locals passed as arguments to {@code @Owning} parameters at a method
   * or constructor call by removing the obligations corresponding to those locals.
   *
   * @param obligations the current set of obligations, which is side-effected to remove obligations
   *     for locals that are passed as owning parameters to the method or constructor
   * @param node a method or constructor invocation node
   */
  private void removeObligationsAtOwnershipTransferToParameters(
      Set<Obligation> obligations, Node node) {

    if (checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)) {
      // Never transfer ownership to parameters, matching the default in the analysis built into
      // Eclipse.
      return;
    }

    List<Node> arguments = getArgumentsOfInvocation(node);
    List<? extends VariableElement> parameters = getParametersOfInvocation(node);

    if (arguments.size() != parameters.size()) {
      // This could happen, e.g., with varargs, or with strange cases like generated Enum
      // constructors. In the varargs case (i.e. if the varargs parameter is owning),
      // only the first of the varargs arguments will actually get transferred: the second
      // and later varargs arguments will continue to be tracked at the call-site.
      // For now, just skip this case - the worst that will happen is a false positive in
      // cases like the varargs one described above.
      // TODO allow for ownership transfer here if needed in future
      return;
    }
    for (int i = 0; i < arguments.size(); i++) {
      Node n = removeCastsAndGetTmpVarIfPresent(arguments.get(i));
      if (n instanceof LocalVariableNode) {
        LocalVariableNode local = (LocalVariableNode) n;
        if (varTrackedInObligations(local, obligations)) {

          // check if parameter has an @Owning annotation
          VariableElement parameter = parameters.get(i);
          Set<AnnotationMirror> annotationMirrors = typeFactory.getDeclAnnotations(parameter);

          for (AnnotationMirror anno : annotationMirrors) {
            if (AnnotationUtils.areSameByName(
                anno, "org.checkerframework.checker.mustcall.qual.Owning")) {
              // transfer ownership!
              obligations.remove(getObligationForVar(obligations, local));
              break;
            }
          }
        }
      }
    }
  }

  /**
   * If the return type of the enclosing method is {@code @Owning}, treat its obligations as
   * satisfied by removing it from {@code obligations}.
   *
   * @param node a return node
   * @param cfg the CFG of the enclosing method
   * @param obligations the current set of tracked obligations. If ownership is transferred, it is
   *     side-effected to remove the obligations of the returned value.
   */
  private void updateObligationsForOwningReturn(
      ReturnNode node, ControlFlowGraph cfg, Set<Obligation> obligations) {
    if (isTransferOwnershipAtReturn(cfg)) {
      Node returnExpr = node.getResult();
      returnExpr = getTempVarOrNode(returnExpr);
      if (returnExpr instanceof LocalVariableNode) {
        removeObligationsContainingVar(obligations, (LocalVariableNode) returnExpr);
      }
    }
  }

  /**
   * Helper method that gets the temporary node corresponding to {@code node}, if one exists. If
   * not, this method returns its input.
   *
   * @param node a node
   * @return the temporary for node, or node if no temporary exists
   */
  private Node getTempVarOrNode(final Node node) {
    Node temp = typeFactory.getTempVarForNode(node);
    if (temp != null) {
      return temp;
    }
    return node;
  }

  /**
   * Should ownership be transferred to the return type of the method corresponding to a CFG?
   * Returns true when there is no {@link NotOwning} annotation on the return type.
   *
   * @param cfg the CFG of the method
   * @return true iff ownership should be transferred to the return type of the method corresponding
   *     to a CFG
   */
  private boolean isTransferOwnershipAtReturn(ControlFlowGraph cfg) {
    if (checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)) {
      // If not using LO, default to always transfer at return, just like Eclipse does.
      return true;
    }

    UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
    if (underlyingAST instanceof UnderlyingAST.CFGMethod) {
      // TODO: lambdas? In that case false is returned below, which means that ownership will
      //  not be transferred.
      MethodTree method = ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
      ExecutableElement executableElement = TreeUtils.elementFromDeclaration(method);
      return typeFactory.getDeclAnnotation(executableElement, NotOwning.class) == null;
    }
    return false;
  }

  /**
   * Updates a set of obligations to account for an assignment. Assigning to an owning field might
   * remove obligations, assigning to a new local variable might modify an obligation (by increasing
   * the size of its resource alias set), etc.
   *
   * @param assignmentNode the assignment
   * @param obligations the set of obligations to update
   */
  private void updateObligationsForAssignment(
      AssignmentNode assignmentNode, Set<Obligation> obligations) {
    Node lhs = assignmentNode.getTarget();
    Element lhsElement = TreeUtils.elementFromTree(lhs.getTree());
    // Use the temporary variable for the rhs if it exists.
    Node rhs = NodeUtils.removeCasts(assignmentNode.getExpression());
    rhs = getTempVarOrNode(rhs);

    // Ownership transfer to @Owning field.
    if (lhsElement.getKind() == ElementKind.FIELD) {
      boolean isOwningField =
          !checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
              && typeFactory.getDeclAnnotation(lhsElement, Owning.class) != null;
      // Check that there is no obligation on the lhs, if the field is non-final and owning.
      if (isOwningField
          && typeFactory.canCreateObligations()
          && !ElementUtils.isFinal(lhsElement)) {
        checkReassignmentToField(assignmentNode, obligations);
      }
      // Remove obligations from local variables, now that the owning field is responsible.
      // (When obligation creation is turned off, non-final fields cannot take ownership.)
      if (isOwningField
          && rhs instanceof LocalVariableNode
          && (typeFactory.canCreateObligations() || ElementUtils.isFinal(lhsElement))) {
        removeObligationsContainingVar(obligations, (LocalVariableNode) rhs);
      }
    } else if (lhs instanceof LocalVariableNode) {
      LocalVariableNode lhsVar = (LocalVariableNode) lhs;
      updateObligationsForPseudoAssignment(assignmentNode, obligations, lhsVar, rhs);
    }
  }

  /**
   * Remove any obligations that contain {@code var} in their resource-alias set.
   *
   * @param obligations the set of obligations
   * @param var a variable
   */
  private void removeObligationsContainingVar(Set<Obligation> obligations, LocalVariableNode var) {
    Obligation obligationForVar = getObligationForVar(obligations, var);
    while (obligationForVar != null) {
      obligations.remove(obligationForVar);
      obligationForVar = getObligationForVar(obligations, var);
    }
  }

  /**
   * Update a set of tracked obligations to account for a (pseudo-)assignment to some variable, as
   * in a gen-kill dataflow analysis problem. That is, add ("gen") and remove ("kill") resource
   * aliases from obligations in the {@code obligations} set as appropriate based on the
   * (pseudo-)assignment performed by {@code node}. This method may also remove an obligation
   * entirely if the analysis concludes that its resource alias set is empty because the last
   * tracked alias to it has been overwritten (including checking that the must-call obligations
   * were satisfied before the assignment).
   *
   * <p>Pseudo-assignments may include operations that "assign" to a temporary variable, exposing
   * the possible value flow into the variable. E.g., for a ternary expression {@code b ? x : y}
   * whose temporary variable is {@code t}, this method may process "assignments" {@code t = x} and
   * {@code t = y}, thereby capturing the two possible values of {@code t}.
   *
   * @param node the node performing the pseudo-assignment; it is not necessarily an assignment node
   * @param obligations the tracked obligations, which will be side-effected
   * @param lhsVar the left-hand side variable for the pseudo-assignment
   * @param rhs the right-hand side for the pseudo-assignment, which must have been converted to a
   *     temporary variable (via a call to {@link
   *     ResourceLeakAnnotatedTypeFactory#getTempVarForNode(Node)})
   */
  private void updateObligationsForPseudoAssignment(
      Node node, Set<Obligation> obligations, LocalVariableNode lhsVar, Node rhs) {
    // Replacements to eventually perform in obligations.  This map is kept to avoid a
    // ConcurrentModificationException in the loop below.
    Map<Obligation, Obligation> replacements = new LinkedHashMap<>();
    // Construct aliasForAssignment once outside the loop for efficiency.
    ResourceAlias aliasForAssignment = new ResourceAlias(new LocalVariable(lhsVar), node.getTree());
    for (Obligation obligation : obligations) {
      // This is a non-null value iff the resource alias set for obligation needs to
      // change because of the pseudo-assignment. The value of this variable is the new
      // alias set for obligation if it is non-null.
      Set<ResourceAlias> newResourceAliasesForObligation = null;

      // Always kill the lhs var if it is present in the resource alias set for this obligation
      // by removing it from the resource alias set.
      ResourceAlias aliasForLhs = obligation.getResourceAlias(lhsVar);
      if (aliasForLhs != null) {
        newResourceAliasesForObligation = new LinkedHashSet<>(obligation.resourceAliases);
        newResourceAliasesForObligation.remove(aliasForLhs);
      }
      // If rhs is a variable tracked in the obligation's resource alias set, gen the lhs
      // by adding it to the resource alias set.
      if (rhs instanceof LocalVariableNode
          && obligation.canBeSatisfiedThrough((LocalVariableNode) rhs)) {
        LocalVariableNode rhsVar = (LocalVariableNode) rhs;
        if (newResourceAliasesForObligation == null) {
          newResourceAliasesForObligation = new LinkedHashSet<>(obligation.resourceAliases);
        }
        newResourceAliasesForObligation.add(aliasForAssignment);
        // Remove temp vars from tracking once they are assigned to another location.
        if (typeFactory.isTempVar(rhsVar)) {
          ResourceAlias aliasForRhs = obligation.getResourceAlias(rhsVar);
          if (aliasForRhs != null) {
            newResourceAliasesForObligation.remove(aliasForRhs);
          }
        }
      }

      // If no changes were made to the resource alias set, there is no need to update the
      // obligation.
      if (newResourceAliasesForObligation == null) {
        continue;
      }

      if (newResourceAliasesForObligation.isEmpty()) {
        // Because the last reference to the resource has been overwritten, check the must-call
        // obligation.
        MustCallAnnotatedTypeFactory mcAtf =
            typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
        checkMustCall(
            obligation,
            typeFactory.getStoreBefore(node),
            mcAtf.getStoreBefore(node),
            "variable overwritten by assignment " + node.getTree());
      }
      replacements.put(obligation, new Obligation(newResourceAliasesForObligation));
    }

    // Finally, update obligations according to the replacements.
    for (Map.Entry<Obligation, Obligation> entry : replacements.entrySet()) {
      obligations.remove(entry.getKey());
      if (!entry.getValue().resourceAliases.isEmpty()) {
        obligations.add(entry.getValue());
      }
    }
  }

  /**
   * Issues an error if the given re-assignment to a non-final, owning field is not valid. A
   * re-assignment is valid if the called methods type of the lhs before the assignment satisfies
   * the must-call obligations of the field.
   *
   * @param node an assignment to a non-final, owning field
   * @param obligations current tracked obligations
   */
  private void checkReassignmentToField(AssignmentNode node, Set<Obligation> obligations) {

    Node lhsNode = node.getTarget();

    if (!(lhsNode instanceof FieldAccessNode)) {
      throw new BugInCF(
          "checkReassignmentToField: non-field node " + node + " of type " + node.getClass());
    }

    FieldAccessNode lhs = (FieldAccessNode) lhsNode;
    Node receiver = lhs.getReceiver();

    // TODO: it would be better to defer getting the path until after checking
    // for a CreatesMustCallFor annotation, because getting the path can be expensive.
    // It might be possible to exploit the CFG structure to find the containing
    // method (rather than using the path, as below), because if a method is being
    // analyzed then it should be the root of the CFG (I think).
    TreePath currentPath = typeFactory.getPath(node.getTree());
    MethodTree enclosingMethodTree = TreePathUtil.enclosingMethod(currentPath);

    if (enclosingMethodTree == null) {
      // Assignments outside of methods must be field initializers, which
      // are always safe. (Note that the Resource Leak Checker doesn't support static owning fields,
      // so static initializers don't need to be considered here.)
      return;
    }

    // Check that there is a corresponding CreatesMustCallFor annotation, unless this is
    // 1) an assignment to a field of a newly-declared local variable whose scope does not
    // extend beyond the method's body (and which therefore could not be targeted by an annotation
    // on the method declaration), or 2) the rhs is a null literal (so there's nothing to reset).
    if (!(receiver instanceof LocalVariableNode
            && varTrackedInObligations((LocalVariableNode) receiver, obligations))
        && !(node.getExpression() instanceof NullLiteralNode)) {
      checkEnclosingMethodIsCreatesMustCallFor(node, enclosingMethodTree);
    }

    MustCallAnnotatedTypeFactory mcTypeFactory =
        typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);

    // Get the Must Call type for the field. If there's info about this field in the store, use
    // that. Otherwise, use the declared type of the field
    CFStore mcStore = mcTypeFactory.getStoreBefore(lhs);
    CFValue mcValue = mcStore.getValue(lhs);
    AnnotationMirror mcAnno;
    if (mcValue == null) {
      // No store value, so use the declared type.
      mcAnno = mcTypeFactory.getAnnotatedType(lhs.getElement()).getAnnotation(MustCall.class);
    } else {
      mcAnno = AnnotationUtils.getAnnotationByClass(mcValue.getAnnotations(), MustCall.class);
    }
    List<String> mcValues =
        AnnotationUtils.getElementValueArray(
            mcAnno, mcTypeFactory.getMustCallValueElement(), String.class);

    if (mcValues.isEmpty()) {
      return;
    }

    // Get the store before the RHS rather than the assignment node, because the CFG always has
    // the RHS first. If the RHS has side-effects, then the assignment node's store will have
    // had its inferred types erased.
    Node rhs = node.getExpression();
    CFStore cmStoreBefore = typeFactory.getStoreBefore(rhs);
    CFValue cmValue = cmStoreBefore == null ? null : cmStoreBefore.getValue(lhs);
    AnnotationMirror cmAnno = null;
    if (cmValue != null) {
      for (AnnotationMirror anno : cmValue.getAnnotations()) {
        if (AnnotationUtils.areSameByName(
            anno, "org.checkerframework.checker.calledmethods.qual.CalledMethods")) {
          cmAnno = anno;
          break;
        }
      }
    }
    if (cmAnno == null) {
      cmAnno = typeFactory.top;
    }
    if (!calledMethodsSatisfyMustCall(mcValues, cmAnno)) {
      Element lhsElement = TreeUtils.elementFromTree(lhs.getTree());
      if (!checker.shouldSkipUses(lhsElement)) {
        checker.reportError(
            node.getTree(),
            "required.method.not.called",
            formatMissingMustCallMethods(mcValues),
            lhsElement.asType().toString(),
            " Non-final owning field might be overwritten");
      }
    }
  }

  /**
   * Checks that the method that encloses an assignment is marked with @CreatesMustCallFor
   * annotation whose target is the object whose field is being re-assigned.
   *
   * @param node an assignment node whose lhs is a non-final, owning field
   * @param enclosingMethod the MethodTree in which the re-assignment takes place
   */
  private void checkEnclosingMethodIsCreatesMustCallFor(
      AssignmentNode node, MethodTree enclosingMethod) {
    Node lhs = node.getTarget();
    if (!(lhs instanceof FieldAccessNode)) {
      return;
    }

    String receiverString = receiverAsString((FieldAccessNode) lhs);
    if ("this".equals(receiverString) && TreeUtils.isConstructor(enclosingMethod)) {
      // Constructors always create must-call obligations, so there is no need for them to
      // be annotated.
      return;
    }
    ExecutableElement enclosingMethodElt = TreeUtils.elementFromDeclaration(enclosingMethod);
    MustCallAnnotatedTypeFactory mcAtf =
        typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);

    List<String> cmcfValues =
        ResourceLeakVisitor.getCreatesMustCallForValues(enclosingMethodElt, mcAtf, typeFactory);

    if (cmcfValues.isEmpty()) {
      checker.reportError(
          enclosingMethod,
          "missing.creates.mustcall.for",
          receiverString,
          ((FieldAccessNode) lhs).getFieldName());
      return;
    }

    List<String> checked = new ArrayList<>();
    for (String targetStrWithoutAdaptation : cmcfValues) {
      String targetStr;
      try {
        targetStr =
            StringToJavaExpression.atMethodBody(
                    targetStrWithoutAdaptation, enclosingMethod, checker)
                .toString();
      } catch (JavaExpressionParseException e) {
        targetStr = targetStrWithoutAdaptation;
      }
      if (targetStr.equals(receiverString)) {
        // This @CreatesMustCallFor annotation matches.
        return;
      }
      checked.add(targetStr);
    }
    checker.reportError(
        enclosingMethod,
        "incompatible.creates.mustcall.for",
        receiverString,
        ((FieldAccessNode) lhs).getFieldName(),
        String.join(", ", checked));
  }

  /**
   * Gets a standardized name for an object whose field is being re-assigned.
   *
   * @param fieldAccessNode a field access node
   * @return the name of the object whose field is being accessed (the receiver), as a string
   */
  private String receiverAsString(FieldAccessNode fieldAccessNode) {
    Node receiver = fieldAccessNode.getReceiver();
    if (receiver instanceof ThisNode) {
      return "this";
    }
    if (receiver instanceof LocalVariableNode) {

      return ((LocalVariableNode) receiver).getName();
    }
    throw new BugInCF(
        "unexpected receiver of field assignment: " + receiver + " of type " + receiver.getClass());
  }

  /**
   * Finds the argument passed in the {@code @MustCallAlias} position for a call.
   *
   * @param callNode callNode representing the call; must be {@link MethodInvocationNode} or {@link
   *     ObjectCreationNode}
   * @return if {@code callNode} invokes a method with a {@code @MustCallAlias} annotation on some
   *     formal parameter (or the receiver), returns the result of calling {@link
   *     #removeCastsAndGetTmpVarIfPresent(Node)} on the argument passed in that position.
   *     Otherwise, returns {@code null}.
   */
  private @Nullable Node getMustCallAliasArgumentNode(Node callNode) {
    Preconditions.checkArgument(
        callNode instanceof MethodInvocationNode || callNode instanceof ObjectCreationNode);
    if (!typeFactory.hasMustCallAlias(callNode.getTree())) {
      return null;
    }

    Node result = null;
    List<Node> args = getArgumentsOfInvocation(callNode);
    List<? extends VariableElement> parameters = getParametersOfInvocation(callNode);
    for (int i = 0; i < args.size(); i++) {
      if (typeFactory.hasMustCallAlias(parameters.get(i))) {
        result = args.get(i);
        break;
      }
    }

    // If none of the parameters were @MustCallAlias, it must be the receiver
    if (result == null && callNode instanceof MethodInvocationNode) {
      result = ((MethodInvocationNode) callNode).getTarget().getReceiver();
    }

    result = removeCastsAndGetTmpVarIfPresent(result);
    return result;
  }

  /**
   * If a temporary variable exists for node after typecasts have been removed, return it.
   * Otherwise, return node.
   *
   * @param node a node
   * @return either a tempvar for node's content sans typecasts, or node
   */
  private Node removeCastsAndGetTmpVarIfPresent(Node node) {
    // TODO: Create temp vars for TypeCastNodes as well, so there is no need to explicitly remove
    // casts here.
    node = NodeUtils.removeCasts(node);
    return getTempVarOrNode(node);
  }

  /**
   * Get the nodes representing the arguments of a method or constructor invocation from the
   * invocation node.
   *
   * @param node a MethodInvocation or ObjectCreation node
   * @return the arguments, in order
   */
  private List<Node> getArgumentsOfInvocation(Node node) {
    if (node instanceof MethodInvocationNode) {
      MethodInvocationNode invocationNode = (MethodInvocationNode) node;
      return invocationNode.getArguments();
    } else if (node instanceof ObjectCreationNode) {
      return ((ObjectCreationNode) node).getArguments();
    } else {
      throw new BugInCF("unexpected node type " + node.getClass());
    }
  }

  /**
   * Get the elements representing the formal parameters of a method or constructor, from an
   * invocation of that method or constructor.
   *
   * @param node a method invocation or object creation node
   * @return a list of the declarations of the formal parameters of the method or constructor being
   *     invoked
   */
  private List<? extends VariableElement> getParametersOfInvocation(Node node) {
    ExecutableElement executableElement;
    if (node instanceof MethodInvocationNode) {
      MethodInvocationNode invocationNode = (MethodInvocationNode) node;
      executableElement = TreeUtils.elementFromUse(invocationNode.getTree());
    } else if (node instanceof ObjectCreationNode) {
      executableElement = TreeUtils.elementFromUse(((ObjectCreationNode) node).getTree());
    } else {
      throw new BugInCF("unexpected node type " + node.getClass());
    }

    return executableElement.getParameters();
  }

  /**
   * Does the method being invoked have a not-owning return type?
   *
   * @param node a method invocation
   * @return true iff the checker is not in no-lightweight-ownership mode and (1) the method has a
   *     void return type, or (2) a NotOwning annotation is present on the method declaration
   */
  private boolean hasNotOwningReturnType(MethodInvocationNode node) {
    if (checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)) {
      // Default to always transferring at return if not using LO, just like Eclipse does.
      return false;
    }
    MethodInvocationTree methodInvocationTree = node.getTree();
    ExecutableElement executableElement = TreeUtils.elementFromUse(methodInvocationTree);
    // void methods are "not owning" by construction
    return (ElementUtils.getType(executableElement).getKind() == TypeKind.VOID)
        || (typeFactory.getDeclAnnotation(executableElement, NotOwning.class) != null);
  }

  /**
   * Get all successor blocks for some block, except for those corresponding to ignored exception
   * types. See {@link #ignoredExceptionTypes}. Each exceptional successor is paired with the type
   * of exception that leads to it, for use in error messages.
   *
   * @param block input block
   * @return set of pairs (b, t), where b is a successor block, and t is the type of exception for
   *     the CFG edge from block to b, or {@code null} if b is a non-exceptional successor
   */
  private Set<Pair<Block, @Nullable TypeMirror>> getSuccessorsExceptIgnoredExceptions(Block block) {
    if (block.getType() == Block.BlockType.EXCEPTION_BLOCK) {
      ExceptionBlock excBlock = (ExceptionBlock) block;
      Set<Pair<Block, @Nullable TypeMirror>> result = new LinkedHashSet<>();
      // regular successor
      Block regularSucc = excBlock.getSuccessor();
      if (regularSucc != null) {
        result.add(Pair.of(regularSucc, null));
      }
      // non-ignored exception successors
      Map<TypeMirror, Set<Block>> exceptionalSuccessors = excBlock.getExceptionalSuccessors();
      for (Map.Entry<TypeMirror, Set<Block>> entry : exceptionalSuccessors.entrySet()) {
        TypeMirror exceptionType = entry.getKey();
        if (!isIgnoredExceptionType(((Type) exceptionType).tsym.getQualifiedName())) {
          for (Block exSucc : entry.getValue()) {
            result.add(Pair.of(exSucc, exceptionType));
          }
        }
      }
      return result;
    } else {
      Set<Pair<Block, @Nullable TypeMirror>> result = new LinkedHashSet<>();
      for (Block b : block.getSuccessors()) {
        result.add(Pair.of(b, null));
      }
      return result;
    }
  }

  /**
   * Propagates a set of obligations to successors, and performs consistency checks when variables
   * are going out of scope.
   *
   * <p>The basic algorithm loops over the successor blocks of the current block. For each
   * successor, it checks every obligation in obligations. If the successor is an exit block or all
   * of an obligation's resource aliases might be going out of scope, then a consistency check
   * occurs (with two exceptions, both related to temporary variables that don't actually get
   * assigned; see code comments for details) and an error is issued if it fails. If the successor
   * is any other kind of block and there is information about at least one of the obligation's
   * aliases in the successor store (i.e. the resource itself definitely does not go out of scope),
   * then the obligation is passed forward to the successor ("propagated") with any definitely
   * out-of-scope aliases removed from its resource alias set.
   *
   * @param visited block-obligations pairs already analyzed or already on the worklist
   * @param worklist current worklist
   * @param obligations obligations of the current block
   * @param currentBlock the current block
   */
  private void propagateObligationsToSuccessorBlocks(
      Set<BlockWithObligations> visited,
      Deque<BlockWithObligations> worklist,
      Set<Obligation> obligations,
      Block currentBlock) {
    List<Node> currentBlockNodes = currentBlock.getNodes();
    // For each successor block that isn't caused by an ignored exception type, this loop computes
    // the set of obligations that should be propagated to it and then adds it to the worklist if
    // any of its resource aliases are still in scope in the successor block. If none are, then the
    // loop performs a consistency check for that obligation.
    for (Pair<Block, @Nullable TypeMirror> successorAndExceptionType :
        getSuccessorsExceptIgnoredExceptions(currentBlock)) {
      Block successor = successorAndExceptionType.first;
      // If nonnull, currentBlock is an ExceptionBlock.
      TypeMirror exceptionType = successorAndExceptionType.second;
      Set<Obligation> curObligations =
          handleTernarySuccIfNeeded(currentBlock, successor, obligations);
      // successorObligations eventually contains the obligations to propagate to successor. The
      // loop below mutates it.
      Set<Obligation> successorObligations = new LinkedHashSet<>();
      // A detailed reason to give in the case that the last resource alias of an obligation
      // goes out of scope without a called-methods type that satisfies the obligation along the
      // current control-flow edge. Computed here for efficiency; used in the loop over the
      // obligations, below.
      String exitReasonForErrorMessage =
          exceptionType == null
              ?
              // Technically the variable may be going out of scope before the method exit, but that
              // doesn't seem to provide additional helpful information.
              "regular method exit"
              : "possible exceptional exit due to "
                  + ((ExceptionBlock) currentBlock).getNode().getTree()
                  + " with exception type "
                  + exceptionType;
      // Computed outside the obligation loop for efficiency.
      CFStore regularStoreOfSuccessor = analysis.getInput(successor).getRegularStore();
      for (Obligation obligation : curObligations) {
        // This boolean is true if there is no evidence that the obligation does not go out of
        // scope - that is, if there is definitely a resource alias that is in scope in the
        // successor.
        boolean obligationGoesOutOfScopeBeforeSuccessor = true;
        for (ResourceAlias resourceAlias : obligation.resourceAliases) {
          if (aliasInScopeInSuccessor(regularStoreOfSuccessor, resourceAlias)) {
            obligationGoesOutOfScopeBeforeSuccessor = false;
            break;
          }
        }
        // This check is to determine if this obligation's resource aliases are definitely going
        // out of scope: if this is an exit block or there is no information about any of them in
        // the successor store, all aliases must be going out of scope and a consistency check
        // should occur.
        if (successor.getType() == BlockType.SPECIAL_BLOCK /* special blocks are exit blocks */
            || obligationGoesOutOfScopeBeforeSuccessor) {
          MustCallAnnotatedTypeFactory mcAtf =
              typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);

          // If successor is an exceptional successor, and obligation represents the temporary
          // variable for  currentBlock's node, do not propagate or do a consistency check, as in
          // the exceptional case the "assignment" to the temporary variable does not succeed.
          //
          // Note that this test cannot be "successor.getType() == BlockType.EXCEPTIONAL_BLOCK",
          // because not every exceptional successor is an exceptional block. For example,
          // the successor might be a regular block (containing a catch clause, for example),
          // or a special block indicating an exceptional exit. Nor can this test be
          // "currentBlock.getType() == BlockType.EXCEPTIONAL_BLOCK", because some exception types
          // are ignored. Whether exceptionType is null captures the logic of both of these cases.
          if (exceptionType != null) {
            Node exceptionalNode = NodeUtils.removeCasts(((ExceptionBlock) currentBlock).getNode());
            LocalVariableNode tmpVarForExcNode = typeFactory.getTempVarForNode(exceptionalNode);
            if (tmpVarForExcNode != null
                && obligation.resourceAliases.size() == 1
                && obligation.canBeSatisfiedThrough(tmpVarForExcNode)) {
              continue;
            }
          }

          // Always propagate obligation to successor if current block represents code nested
          // in a cast or ternary expression.  Without this logic, the analysis may report a false
          // positive when the obligation represents a temporary variable for a nested
          // expression, as the temporary may not appear in the successor store and hence seems to
          // be going out of scope.  The temporary will be handled with special logic; casts are
          // unwrapped at various points in the analysis, and ternary expressions are handled by
          // handleTernarySuccIfNeeded.
          if (currentBlockNodes.size() == 1 && inCastOrTernary(currentBlockNodes.get(0))) {
            successorObligations.add(obligation);
            continue;
          }

          // At this point, a consistency check will definitely occur.
          // Which stores from the called-methods and must-call checkers are used in
          // the consistency check varies depending on the context. The rules are:
          // 1. if the current block has no nodes (and therefore the store must come from a block
          //    rather than a node):
          //    1a. if there is information about any alias in the resource alias set
          //        in the successor store, use the successor's CM and MC stores, which
          //        contain whatever information is true after this block finishes.
          //    1b. if there is not any information about any alias in the resource alias
          //        set in the successor store, use the current blocks' CM and MC stores,
          //        which contain whatever information is true before this (empty) block.
          // 2. if the current block has one or more nodes, always use the CM store after
          //    the last node. To decide which MC store to use:
          //    2a. if the last node in the block is the invocation of an @CreatesMustCallFor
          //        method that might throw an exception, and the consistency check is for
          //        an exceptional path, use the MC store immediately before the method invocation,
          //        because the method threw an exception rather than finishing and therefore did
          //        not actually create an obligation, so the MC store after might contain
          //        obligations that do not need to be fulfilled along this path.
          //        2b. in all other cases, use the MC store from after the last node in the block.
          CFStore mcStore, cmStore;
          if (currentBlockNodes.size() == 0 /* currentBlock is special or conditional */) {
            cmStore =
                obligationGoesOutOfScopeBeforeSuccessor
                    ? analysis.getInput(currentBlock).getRegularStore() // 1a. (CM)
                    : regularStoreOfSuccessor; // 1b. (CM)
            mcStore =
                mcAtf.getStoreForBlock(
                    obligationGoesOutOfScopeBeforeSuccessor,
                    currentBlock, // 1a. (MC)
                    successor); // 1b. (MC)
          } else { // In this case, current block has at least one node.
            // Use the called-methods store immediately after the last node in currentBlock.
            Node last = currentBlockNodes.get(currentBlockNodes.size() - 1); // 2. (CM)
            cmStore = typeFactory.getStoreAfter(last);
            // If this is an exceptional block, check the MC store beforehand to avoid
            // issuing an error about a call to a CreatesMustCallFor method that might throw
            // an exception. Otherwise, use the store after.
            if (exceptionType != null && isInvocationOfCreatesMustCallForMethod(last)) {
              mcStore = mcAtf.getStoreBefore(last); // 2a. (MC)
            } else {
              mcStore = mcAtf.getStoreAfter(last); // 2b. (MC)
            }
          }
          checkMustCall(obligation, cmStore, mcStore, exitReasonForErrorMessage);

        } else { // In this case, there is info in the successor store about some alias in
          // obligation. Handles the possibility that some resource in the obligation may go out of
          // scope.
          Set<ResourceAlias> copyOfResourceAliases =
              new LinkedHashSet<>(obligation.resourceAliases);
          copyOfResourceAliases.removeIf(
              alias -> !aliasInScopeInSuccessor(regularStoreOfSuccessor, alias));
          successorObligations.add(new Obligation(copyOfResourceAliases));
        }
      }

      propagate(new BlockWithObligations(successor, successorObligations), visited, worklist);
    }
  }

  /**
   * Returns true if {@code alias.reference} is definitely in-scope in the successor store: that is,
   * there is a value for it in {@code successorStore}. Also returns true if {@code alias.tree} is a
   * {@link ConditionalExpressionTree}. This special rule for a {@link ConditionalExpressionTree} is
   * to accommodate the handling of ternary expressions, because the analysis tracks the temporary
   * variable for the expression at the program point before that expression; see {@link
   * #handleTernarySuccIfNeeded(Block, Block, Set)}.
   *
   * @param successorStore the regular store of the successor block
   * @param alias the resource alias to check
   * @return true if the variable is definitely in scope for the purposes of the consistency
   *     checking algorithm in the successor block from which the store came
   */
  private boolean aliasInScopeInSuccessor(CFStore successorStore, ResourceAlias alias) {
    return successorStore.getValue(alias.reference) != null
        || alias.tree instanceof ConditionalExpressionTree;
  }

  /**
   * Handles control-flow to a block starting with a {@link TernaryExpressionNode}.
   *
   * <p>In the Checker Framework's CFG, a ternary expression is represented as a {@link
   * org.checkerframework.dataflow.cfg.block.ConditionalBlock}, whose successor blocks are the two
   * cases of the ternary expression. The {@link TernaryExpressionNode} is the first node in the
   * successor block of the two cases.
   *
   * <p>To handle this representation, the control-flow transition from a node for a ternary
   * expression case <em>c</em> to the successor {@link TernaryExpressionNode} <em>t</em> is treated
   * as a pseudo-assignment from <em>c</em> to the temporary variable for <em>t</em>. With this
   * handling, the obligations reaching the successor node of <em>t</em> will properly account for
   * the execution of case <em>c</em>.
   *
   * <p>If the successor block does not begin with a {@link TernaryExpressionNode} that needs to be
   * handled, this method simply returns {@code obligations}.
   *
   * @param pred the predecessor block, potentially corresponding to the ternary expression case
   * @param succ the successor block, potentially starting with a {@link TernaryExpressionNode}
   * @param obligations the obligations before the control-flow transition
   * @return a new set of obligations to account for the {@link TernaryExpressionNode}, or just
   *     {@code obligations} if no handling is required.
   */
  private Set<Obligation> handleTernarySuccIfNeeded(
      Block pred, Block succ, Set<Obligation> obligations) {
    List<Node> succNodes = succ.getNodes();
    if (succNodes.isEmpty() || !(succNodes.get(0) instanceof TernaryExpressionNode)) {
      return obligations;
    }
    TernaryExpressionNode ternaryNode = (TernaryExpressionNode) succNodes.get(0);
    LocalVariableNode ternaryTempVar = typeFactory.getTempVarForNode(ternaryNode);
    if (ternaryTempVar == null) {
      return obligations;
    }
    List<Node> predNodes = pred.getNodes();
    // Right-hand side of the pseudo-assignment to the ternary expression temporary variable.
    Node rhs = NodeUtils.removeCasts(predNodes.get(predNodes.size() - 1));
    if (!(rhs instanceof LocalVariableNode)) {
      rhs = typeFactory.getTempVarForNode(rhs);
      if (rhs == null) {
        return obligations;
      }
    }
    Set<Obligation> newDefs = new LinkedHashSet<>(obligations);
    updateObligationsForPseudoAssignment(ternaryNode, newDefs, ternaryTempVar, rhs);
    return newDefs;
  }

  /**
   * Returns true if node is a MethodInvocationNode of a method with a CreatesMustCallFor
   * annotation.
   *
   * @param node a node
   * @return true if node is a MethodInvocationNode of a method with a CreatesMustCallFor annotation
   */
  private boolean isInvocationOfCreatesMustCallForMethod(Node node) {
    if (!(node instanceof MethodInvocationNode)) {
      return false;
    }
    MethodInvocationNode miNode = (MethodInvocationNode) node;
    return typeFactory.hasCreatesMustCallFor(miNode);
  }

  /**
   * Finds {@link Owning} formal parameters for the method corresponding to a CFG.
   *
   * @param cfg the CFG
   * @return the owning formal parameters of the method that corresponds to the given cfg, or an
   *     empty set if the given CFG doesn't correspond to a method body
   */
  private Set<Obligation> computeOwningParameters(ControlFlowGraph cfg) {
    // TODO what about lambdas?
    if (cfg.getUnderlyingAST().getKind() == Kind.METHOD) {
      MethodTree method = ((UnderlyingAST.CFGMethod) cfg.getUnderlyingAST()).getMethod();
      Set<Obligation> result = new LinkedHashSet<>(1);
      for (VariableTree param : method.getParameters()) {
        Element paramElement = TreeUtils.elementFromDeclaration(param);
        if (typeFactory.hasMustCallAlias(paramElement)
            || (typeFactory.hasDeclaredMustCall(param)
                && !checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
                && paramElement.getAnnotation(Owning.class) != null)) {
          result.add(
              new Obligation(
                  ImmutableSet.of(new ResourceAlias(new LocalVariable(paramElement), param))));
          // Increment numMustCall for each @Owning parameter tracked by the enclosing method.
          incrementNumMustCall(paramElement);
        }
      }
      return result;
    }
    return Collections.emptySet();
  }

  /**
   * Checks whether there is some resource alias set <em>R</em> in {@code obligations} such that
   * <em>R</em> contains a {@link ResourceAlias} whose local variable is {@code node}.
   *
   * @param var the local variable to look for
   * @param obligations the set of obligations to search
   * @return true iff there is a resource alias set in obligations that contains node
   */
  private static boolean varTrackedInObligations(
      LocalVariableNode var, Set<Obligation> obligations) {
    for (Obligation obligation : obligations) {
      if (obligation.canBeSatisfiedThrough(var)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the obligation whose resource aliase set contains the given local variable, if one exists
   * in obligations.
   *
   * @param obligations set of obligations
   * @param node variable of interest
   * @return the obligation in {@code obligations} whose resource alias set contains {@code node},
   *     or {@code null} if there is no such obligation
   */
  private static @Nullable Obligation getObligationForVar(
      Set<Obligation> obligations, LocalVariableNode node) {
    for (Obligation obligation : obligations) {
      if (obligation.canBeSatisfiedThrough(node)) {
        return obligation;
      }
    }
    return null;
  }

  /**
   * For the given obligation, checks that at least one of its variables has its {@code @MustCall}
   * obligation satisfied, based on {@code @CalledMethods} and {@code @MustCall} types in the given
   * stores.
   *
   * @param obligation the obligation
   * @param cmStore the called-methods store
   * @param mcStore the must-call store
   * @param outOfScopeReason if the {@code @MustCall} obligation is not satisfied, a useful
   *     explanation to include in the error message
   */
  private void checkMustCall(
      Obligation obligation, CFStore cmStore, CFStore mcStore, String outOfScopeReason) {

    List<String> mustCallValue = typeFactory.getMustCallValue(obligation.resourceAliases, mcStore);
    // optimization: if there are no must-call methods, do not need to perform the check
    if (mustCallValue == null || mustCallValue.isEmpty()) {
      return;
    }

    boolean mustCallSatisfied = false;
    for (ResourceAlias alias : obligation.resourceAliases) {

      // sometimes the store is null!  this looks like a bug in checker dataflow.
      // TODO track down and report the root-cause bug
      CFValue lhsCFValue = cmStore != null ? cmStore.getValue(alias.reference) : null;
      AnnotationMirror cmAnno = null;

      if (lhsCFValue != null) { // When store contains the lhs
        for (AnnotationMirror anno : lhsCFValue.getAnnotations()) {
          if (AnnotationUtils.areSameByName(
              anno, "org.checkerframework.checker.calledmethods.qual.CalledMethods")) {
            cmAnno = anno;
          }
        }
      }
      if (cmAnno == null) {
        cmAnno =
            typeFactory
                .getAnnotatedType(alias.reference.getElement())
                .getAnnotationInHierarchy(typeFactory.top);
      }

      if (calledMethodsSatisfyMustCall(mustCallValue, cmAnno)) {
        mustCallSatisfied = true;
        break;
      }
    }

    if (!mustCallSatisfied) {
      // Report the error at the first alias' definition. This choice is arbitrary but consistent.
      ResourceAlias firstAlias = obligation.resourceAliases.iterator().next();
      if (!reportedErrorAliases.contains(firstAlias)) {
        if (!checker.shouldSkipUses(TreeUtils.elementFromTree(firstAlias.tree))) {
          reportedErrorAliases.add(firstAlias);
          checker.reportError(
              firstAlias.tree,
              "required.method.not.called",
              formatMissingMustCallMethods(mustCallValue),
              firstAlias.reference.getType().toString(),
              outOfScopeReason);
        }
      }
    }
  }

  /**
   * Increment the -AcountMustCall counter.
   *
   * @param node the node being counted, to extract the type
   */
  private void incrementNumMustCall(Node node) {
    if (checker.hasOption(ResourceLeakChecker.COUNT_MUST_CALL)) {
      TypeMirror type = node.getType();
      incrementMustCallImpl(type);
    }
  }

  /**
   * Increment the -AcountMustCall counter.
   *
   * @param elt the elt being counted, to extract the type
   */
  private void incrementNumMustCall(Element elt) {
    if (checker.hasOption(ResourceLeakChecker.COUNT_MUST_CALL)) {
      TypeMirror type = elt.asType();
      incrementMustCallImpl(type);
    }
  }

  /**
   * Shared implementation for the two version of countMustCall. Don't call this directly.
   *
   * @param type the type of the object that has a must call obligation
   */
  private void incrementMustCallImpl(TypeMirror type) {
    // only count uses of JDK classes, since that's what the paper reported
    if (!isJdkClass(TypesUtils.getTypeElement(type).getQualifiedName().toString())) {
      return;
    }
    checker.numMustCall++;
  }

  /**
   * Is the given class a java* class? This is a heuristic for whether the class was defined in the
   * JDK.
   *
   * @param qualifiedName a fully qualified name of a class
   * @return true iff the type's fully-qualified name starts with "java", indicating that it is from
   *     a java.* or javax.* package (probably)
   */
  /* package-private */ static boolean isJdkClass(String qualifiedName) {
    return qualifiedName.startsWith("java");
  }

  /**
   * Do the called methods represented by the {@link CalledMethods} type {@code cmAnno} include all
   * the methods in {@code mustCallValues}?
   *
   * @param mustCallValues the strings representing the must-call obligations
   * @param cmAnno an annotation from the called-methods type hierarchy
   * @return true iff cmAnno is a subtype of a called-methods annotation with the same values as
   *     mustCallValues
   */
  private boolean calledMethodsSatisfyMustCall(
      List<String> mustCallValues, AnnotationMirror cmAnno) {
    // Create this annotation and use a subtype test because there's no guarantee that
    // cmAnno is actually an instance of CalledMethods: it could be CMBottom or CMPredicate.
    AnnotationMirror cmAnnoForMustCallMethods =
        typeFactory.createCalledMethods(mustCallValues.toArray(new String[0]));
    return typeFactory.getQualifierHierarchy().isSubtype(cmAnno, cmAnnoForMustCallMethods);
  }

  /**
   * The exception types in this set are ignored in the CFG when determining if a resource leaks
   * along an exceptional path. These kinds of errors fall into a few categories: runtime errors,
   * errors that the JVM can issue on any statement, and errors that can be prevented by running
   * some other CF checker.
   */
  private static Set<String> ignoredExceptionTypes =
      new HashSet<>(
          ImmutableSet.of(
              // Any method call has a CFG edge for Throwable/RuntimeException/Error to represent
              // run-time
              // misbehavior. Ignore it.
              Throwable.class.getCanonicalName(),
              Error.class.getCanonicalName(),
              RuntimeException.class.getCanonicalName(),
              // Use the Nullness Checker to prove this won't happen.
              NullPointerException.class.getCanonicalName(),
              // These errors can't be predicted statically, so ignore them and assume they
              // won't happen.
              ClassCircularityError.class.getCanonicalName(),
              ClassFormatError.class.getCanonicalName(),
              NoClassDefFoundError.class.getCanonicalName(),
              OutOfMemoryError.class.getCanonicalName(),
              // It's not our problem if the Java type system is wrong.
              ClassCastException.class.getCanonicalName(),
              // It's not our problem if the code is going to divide by zero.
              ArithmeticException.class.getCanonicalName(),
              // Use the Index Checker to prevent these errors.
              ArrayIndexOutOfBoundsException.class.getCanonicalName(),
              NegativeArraySizeException.class.getCanonicalName(),
              // Most of the time, this exception is infeasible, as the charset used
              // is guaranteed to be present by the Java spec (e.g., "UTF-8"). Eventually,
              // this exclusion could be refined by looking at the charset being requested.
              UnsupportedEncodingException.class.getCanonicalName()));

  /**
   * Is {@code exceptionClassName} an exception type the checker ignores, to avoid excessive false
   * positives? For now the checker ignores most runtime exceptions (especially the runtime
   * exceptions that can occur at any point during the program due to something going wrong in the
   * JVM, like OutOfMemoryError and ClassCircularityError) and exceptions that can be proved to
   * never occur by another Checker Framework built-in checker, such as null-pointer dereferences
   * (the Nullness Checker) and out-of-bounds array indexing (the Index Checker).
   *
   * @param exceptionClassName the fully-qualified name of the exception
   * @return true if the given exception class should be ignored
   */
  private static boolean isIgnoredExceptionType(@FullyQualifiedName Name exceptionClassName) {
    return ignoredExceptionTypes.contains(exceptionClassName.toString());
  }

  /**
   * If the input {@code state} has not been visited yet, add it to {@code visited} and {@code
   * worklist}.
   *
   * @param state the current state
   * @param visited the states that have been analyzed or are already on the worklist
   * @param worklist the states that will be analyzed
   */
  private static void propagate(
      BlockWithObligations state,
      Set<BlockWithObligations> visited,
      Deque<BlockWithObligations> worklist) {

    if (visited.add(state)) {
      worklist.add(state);
    }
  }

  /**
   * Formats a list of must-call method names to be printed in an error message.
   *
   * @param mustCallVal the list of must-call strings
   * @return a formatted string
   */
  /* package-private */
  static String formatMissingMustCallMethods(List<String> mustCallVal) {
    int size = mustCallVal.size();
    if (size == 0) {
      throw new BugInCF("empty mustCallVal " + mustCallVal);
    } else if (size == 1) {
      return "method " + mustCallVal.get(0);
    } else {
      return "methods " + String.join(", ", mustCallVal);
    }
  }

  /**
   * A pair of a {@link Block} and a set of obligations (i.e. dataflow facts) on entry to the block.
   * Each obligation is an {@link Obligation}, representing a set of resource aliases for some
   * tracked resource. The analyzer's worklist consists of BlockWithObligations objects, each
   * representing the need to handle the set of obligations reaching the block during analysis.
   */
  private static class BlockWithObligations {

    /** The block. */
    public final Block block;

    /** The facts. */
    public final ImmutableSet<Obligation> obligations;

    /**
     * Create a new BlockWithObligations from a block and a set of obligations.
     *
     * @param b the block
     * @param obligations the set of incoming obligations at the start of the block (may be the
     *     empty set)
     */
    public BlockWithObligations(Block b, Set<Obligation> obligations) {
      this.block = b;
      this.obligations = ImmutableSet.copyOf(obligations);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BlockWithObligations that = (BlockWithObligations) o;
      return block.equals(that.block) && obligations.equals(that.obligations);
    }

    @Override
    public int hashCode() {
      return Objects.hash(block, obligations);
    }
  }
}
