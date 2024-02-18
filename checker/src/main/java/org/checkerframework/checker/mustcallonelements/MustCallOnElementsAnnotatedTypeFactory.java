package org.checkerframework.checker.mustcallonelements;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElements;
import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElementsUnknown;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The annotated type factory for the Must Call Checker. Primarily responsible for the subtyping
 * rules between @MustCallOnElements annotations.
 */
public class MustCallOnElementsAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link MustCallOnElementsUnknown} annotation. */
  public final AnnotationMirror TOP;

  /**
   * The {@code @}{@link MustCallOnElements}{@code ()} annotation. It is the default in unannotated
   * code.
   */
  public final AnnotationMirror BOTTOM;

  // /**
  //  * Map from trees representing expressions to the temporary variables that represent them in
  // the
  //  * store.
  //  *
  //  * <p>Consider the following code, adapted from Apache Zookeeper:
  //  *
  //  * <pre>
  //  *   sock = SocketChannel.open();
  //  *   sock.socket().setSoLinger(false, -1);
  //  * </pre>
  //  *
  //  * This code is safe from resource leaks: sock is an unconnected socket and therefore has no
  //  * must-call obligation. The expression sock.socket() similarly has no must-call obligation
  //  * because it is a resource alias, but without a temporary variable that represents that
  //  * expression in the store, the resource leak checker wouldn't be able to determine that.
  //  *
  //  * <p>These temporary variables are only created once---here---but are used by all three parts
  // of
  //  * the resource leak checker by calling {@link #getTempVar(Node)}. The temporary variables are
  //  * shared in the same way that subcheckers share CFG structure; see {@link
  //  * #getSharedCFGForTree(Tree)}.
  //  */
  // /*package-private*/ final IdentityHashMap<Tree, LocalVariableNode> tempVars =
  //     new IdentityHashMap<>(100);

  /** The MustCallOnElements.value field/element. */
  private final ExecutableElement mustCallOnElementsValueElement =
      TreeUtils.getMethod(MustCallOnElements.class, "value", 0, processingEnv);

  /** Set of assignments that open an obligation for an {@code @OwningArray} array. */
  private static Set<AssignmentTree> obligationCreatingAssignments = new HashSet<>();

  /** Set of method accesses that fulfill an obligation for an {@code @OwningArray} array. */
  private static Set<MemberSelectTree> obligationFulfillingMethodAccess = new HashSet<>();

  /**
   * Maps the AST-node corresponding to the loop condition of a loop calling a method on an
   * {@code @OwningArray} to the name of the method called.
   */
  private static Map<Tree, String> whichMethodDoesLoopWithThisConditionCallMap = new HashMap<>();

  /**
   * Maps the AST-node corresponding to the loop condition of a loop assigning a resource to an
   * {@code @OwningArray} to the methods that are part of the obligation.
   */
  private static Map<Tree, List<String>> whichObligationsDoesLoopWithThisConditionCreateMap =
      new HashMap<>();

  /**
   * Maps the AST-node corresponding to the loop condition of a loop assigning/closing a resource
   * to/from an {@code @OwningArray} to the array node.
   */
  private static Map<Tree, ExpressionTree> arrayTreeForLoopWithThisCondition = new HashMap<>();

  // /** True if -AnoLightweightOwnership was passed on the command line. */
  // private final boolean noLightweightOwnership;

  // /**
  //  * True if -AenableWpiForRlc (see {@link ResourceLeakChecker#ENABLE_WPI_FOR_RLC}) was passed on
  //  * the command line.
  //  */
  // private final boolean enableWpiForRlc;

  /**
   * Creates a MustCallOnElementsAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  public MustCallOnElementsAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    TOP = AnnotationBuilder.fromClass(elements, MustCallOnElementsUnknown.class);
    BOTTOM = createMustCall(Collections.emptyList());
    // noLightweightOwnership =
    // checker.hasOption(MustCallOnElementsChecker.NO_LIGHTWEIGHT_OWNERSHIP);
    // enableWpiForRlc = checker.hasOption(ResourceLeakChecker.ENABLE_WPI_FOR_RLC);
    this.postInit();
  }

  /**
   * Cache of the MustCallOnElements annotations that have actually been created. Most programs
   * require few distinct MustCallOnElements annotations (e.g. MustCallOnElements() and
   * MustCallOnElements("close")).
   */
  private final Map<List<String>, AnnotationMirror> mustCallOnElementsAnnotations =
      new HashMap<>(10);

  public static boolean doesMethodAccessCloseArrayObligation(MemberSelectTree memSelect) {
    return obligationFulfillingMethodAccess.contains(memSelect);
  }

  public static void fulfillArrayObligationForMethodAccess(MemberSelectTree memSelect) {
    obligationFulfillingMethodAccess.add(memSelect);
  }

  public static boolean doesAssignmentCreateArrayObligation(AssignmentTree assgn) {
    return obligationCreatingAssignments.contains(assgn);
  }

  public static void createArrayObligationForAssignment(AssignmentTree assgn) {
    obligationCreatingAssignments.add(assgn);
  }

  public static void createArrayObligationForLessThan(Tree tree, List<String> methods) {
    assert (tree.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of a method calling for-loop, but is not a"
            + " LESS_THAN tree";
    whichObligationsDoesLoopWithThisConditionCreateMap.put(tree, methods);
  }

  public static void closeArrayObligationForLessThan(Tree tree, String method) {
    assert (tree.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of a method calling for-loop, but is not a"
            + " LESS_THAN tree";
    whichMethodDoesLoopWithThisConditionCallMap.put(tree, method);
  }

  public static void putArrayAffectedByLoopWithThisCondition(
      Tree condition, ExpressionTree arrayTree) {
    assert (condition.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of an obligation changing for-loop, but is not a"
            + " LESS_THAN tree";
    arrayTreeForLoopWithThisCondition.put(condition, arrayTree);
  }

  public static List<String> whichObligationsDoesLoopWithThisConditionCreate(Tree condition) {
    assert (condition.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of a method calling for-loop, but is not a"
            + " LESS_THAN tree";
    return whichObligationsDoesLoopWithThisConditionCreateMap.get(condition);
  }

  public static String whichMethodDoesLoopWithThisConditionCall(Tree condition) {
    assert (condition.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of a method calling for-loop, but is not a"
            + " LESS_THAN tree";
    return whichMethodDoesLoopWithThisConditionCallMap.get(condition);
  }

  public static ExpressionTree getArrayTreeForLoopWithThisCondition(Tree condition) {
    assert (condition.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of an obligation changing for-loop, but is not a"
            + " LESS_THAN tree";
    return arrayTreeForLoopWithThisCondition.get(condition);
  }

  @Override
  public void setRoot(@Nullable CompilationUnitTree root) {
    super.setRoot(root);
    // TODO: This should probably be guarded by isSafeToClearSharedCFG from
    // GenericAnnotatedTypeFactory, but this works here because we know the Must Call Checker is
    // always the first subchecker that's sharing tempvars.
    // tempVars.clear();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(MustCallOnElements.class, MustCallOnElementsUnknown.class));
  }

  /**
   * Creates a {@link MustCallOnElements} annotation whose values are the given strings.
   *
   * @param val the methods that should be called
   * @return an annotation indicating that the given methods should be called
   */
  public AnnotationMirror createMustCall(List<String> val) {
    return mustCallOnElementsAnnotations.computeIfAbsent(val, this::createMustCallOnElementsImpl);
  }

  /**
   * Creates a {@link MustCallOnElements} annotation whose values are the given strings.
   *
   * <p>This internal version bypasses the cache, and is only used for new annotations.
   *
   * @param methodList the methods that should be called
   * @return an annotation indicating that the given methods should be called
   */
  private AnnotationMirror createMustCallOnElementsImpl(List<String> methodList) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, MustCallOnElements.class);
    String[] methodArray = methodList.toArray(new String[methodList.size()]);
    Arrays.sort(methodArray);
    builder.setValue("value", methodArray);
    return builder.build();
  }

  public ExecutableElement getMustCallOnElementsValueElement() {
    return mustCallOnElementsValueElement;
  }
}
