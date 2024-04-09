package org.checkerframework.checker.mustcallonelements;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElements;
import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElementsUnknown;
import org.checkerframework.checker.mustcallonelements.qual.OwningArray;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.SubtypeIsSubsetQualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.*;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;

/**
 * The annotated type factory for the Must Call Checker. Primarily responsible for the subtyping
 * rules between @MustCallOnElements annotations. Additionally holds some static datastructures used
 * for pattern-matching loops that create/fulfill MustCallOnElements obligations. These are in the
 * MustCall checker, since it runs before the MustCallOnElements checker and the pattern- match must
 * be finished by the time the MustCallOnElements checker runs.
 */
public class MustCallOnElementsAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link MustCallOnElementsUnknown} annotation. */
  public final AnnotationMirror TOP;

  /** The {@code @}{@link MustCallOnElements} annotation. It is the default in unannotated code. */
  public final AnnotationMirror BOTTOM;

  /**
   * Map from trees representing expressions to the temporary variables that represent them in the
   * store.
   *
   * <p>Consider the following code, adapted from Apache Zookeeper:
   *
   * <pre>
   *   sock = SocketChannel.open();
   *   sock.socket().setSoLinger(false, -1);
   * </pre>
   *
   * This code is safe from resource leaks: sock is an unconnected socket and therefore has no
   * must-call obligation. The expression sock.socket() similarly has no must-call obligation
   * because it is a resource alias, but without a temporary variable that represents that
   * expression in the store, the resource leak checker wouldn't be able to determine that.
   *
   * <p>These temporary variables are only created once---here---but are used by all three parts of
   * the resource leak checker by calling {@link #getTempVar(Node)}. The temporary variables are
   * shared in the same way that subcheckers share CFG structure; see {@link
   * #getSharedCFGForTree(Tree)}.
   */
  /*package-private*/ final IdentityHashMap<Tree, LocalVariableNode> tempVars =
      new IdentityHashMap<>(100);

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

  /**
   * Fetches the store from the results of dataflow for {@code first}. If {@code afterFirstStore} is
   * true, then the store after {@code first} is returned; if {@code afterFirstStore} is false, the
   * store before {@code succ} is returned.
   *
   * @param afterFirstStore whether to use the store after the first block or the store before its
   *     successor, succ
   * @param first a block
   * @param succ first's successor
   * @return the appropriate CFStore, populated with MustCall annotations, from the results of
   *     running dataflow
   */
  public CFStore getStoreForBlock(boolean afterFirstStore, Block first, Block succ) {
    return afterFirstStore ? flowResult.getStoreAfter(first) : flowResult.getStoreBefore(succ);
  }

  /**
   * Returns the store immediately before the specified tree.
   *
   * @param tree an AST node
   * @return the mcoe store immediately before the tree
   */
  public CFStore getStoreForTree(Tree tree) {
    return flowResult.getStoreBefore(tree);
  }

  /**
   * Returns the store immediately after the specified tree.
   *
   * @param tree an AST node
   * @return the mcoe store immediately after the tree
   */
  public CFStore getStoreAfterTree(Tree tree) {
    return flowResult.getStoreAfter(tree);
  }

  /** True if -AnoLightweightOwnership was passed on the command line. */
  // private final boolean noLightweightOwnership;

  /**
   * True if -AenableWpiForRlc (see {@link ResourceLeakChecker#ENABLE_WPI_FOR_RLC}) was passed on
   * the command line.
   */
  private final boolean enableWpiForRlc;

  /**
   * Creates a MustCallOnElementsAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  public MustCallOnElementsAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    TOP = AnnotationBuilder.fromClass(elements, MustCallOnElementsUnknown.class);
    BOTTOM = createMustCallOnElements(Collections.emptyList());
    // noLightweightOwnership =
    // checker.hasOption(MustCallOnElementsChecker.NO_LIGHTWEIGHT_OWNERSHIP);
    enableWpiForRlc = checker.hasOption(ResourceLeakChecker.ENABLE_WPI_FOR_RLC);
    if (checker instanceof MustCallOnElementsChecker) {
      this.postInit();
    }
  }

  /**
   * Cache of the MustCallOnElements annotations that have actually been created. Most programs
   * require few distinct MustCallOnElements annotations (e.g. MustCallOnElements() and
   * MustCallOnElements("close")).
   */
  private final Map<List<String>, AnnotationMirror> mustCallOnElementsAnnotations =
      new HashMap<>(10);

  /**
   * Checks if WPI is enabled for the Resource Leak Checker inference. See {@link
   * ResourceLeakChecker#ENABLE_WPI_FOR_RLC}.
   *
   * @return returns true if WPI is enabled for the Resource Leak Checker
   */
  protected boolean isWpiEnabledForRLC() {
    return enableWpiForRlc;
  }

  /**
   * Return the temporary variable for node, if it exists. See {@code #tempVars}.
   *
   * @param node a CFG node
   * @return the corresponding temporary variable, or null if there is not one
   */
  public @Nullable LocalVariableNode getTempVar(Node node) {
    return tempVars.get(node.getTree());
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(), new MustCallOnElementsTreeAnnotator(this));
  }

  @Override
  protected TypeAnnotator createTypeAnnotator() {
    return new ListTypeAnnotator(
        super.createTypeAnnotator(), new MustCallOnElementsTypeAnnotator(this));
  }

  /**
   * returns whether the specified member-select AST node is in a pattern-matched loop that fulfills
   * an {@code @OwningArray} obligation.
   *
   * @param memSelect the member-select AST node
   * @return whether the node is in a pattern-matched loop fulfilling an mcoe obligation
   */
  public static boolean doesMethodAccessCloseArrayObligation(MemberSelectTree memSelect) {
    return obligationFulfillingMethodAccess.contains(memSelect);
  }

  /**
   * Marks the specified member-select AST node as one that fulfills a mcoe obligation for an
   * {@code @OwningArray} array, i.e. marks the node as being in a pattern-matched loop. Only call
   * when the corrresponding loop has been successfully pattern-matched.
   *
   * @param memSelect the member-select AST node
   */
  public static void fulfillArrayObligationForMethodAccess(MemberSelectTree memSelect) {
    obligationFulfillingMethodAccess.add(memSelect);
  }

  /**
   * returns whether the specified assignment AST node is in a pattern-matched allocating for-loop.
   *
   * @param assgn the assignment AST node
   * @return whether the specified node is in an allocating for-loop for an {@code @OwningArray}.
   */
  public static boolean doesAssignmentCreateArrayObligation(AssignmentTree assgn) {
    return obligationCreatingAssignments.contains(assgn);
  }

  /**
   * Marks the specified assignment AST node as one that's in a loop that creates a mcoe obligation
   * for an {@code @OwningArray} array, i.e. marks the node as being in a pattern-matched loop. Only
   * call when the corrresponding loop has been successfully pattern-matched.
   *
   * @param assgn the assignment node
   */
  public static void createArrayObligationForAssignment(AssignmentTree assgn) {
    obligationCreatingAssignments.add(assgn);
  }

  /**
   * Marks the specified less-than AST node as one that's the condition of a loop that creates a
   * mcoe obligation for an {@code @OwningArray} array. The obligations created are passed in the
   * second argument.
   *
   * @param tree the less-than node
   * @param methods a list of the methods in the obligation
   */
  public static void createArrayObligationForLessThan(Tree tree, List<String> methods) {
    assert (tree.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of a method calling for-loop, but is not a LESS_THAN tree";
    whichObligationsDoesLoopWithThisConditionCreateMap.put(tree, methods);
  }

  /**
   * Marks the specified less-than AST node as one that's the condition of a loop that fulfills a
   * mcoe obligation for an {@code @OwningArray} array. The fulfilled mcoe method is passed in the
   * second argument.
   *
   * @param tree the less-than AST node
   * @param method the method that is called on the array elements in the loop
   */
  public static void closeArrayObligationForLessThan(Tree tree, String method) {
    assert (tree.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of a method calling for-loop, but is not a LESS_THAN tree";
    whichMethodDoesLoopWithThisConditionCallMap.put(tree, method);
  }

  /**
   * Associates the given less-than AST-node (which is a loop condition for a successfully
   * pattern-matched loop for opening/closing a MustCallOnElements obligation) with the arrayTree
   * AST-node that corresponds to the array in question.
   *
   * @param condition the less-than AST-node
   * @param arrayTree the array AST-node
   */
  public static void putArrayAffectedByLoopWithThisCondition(
      Tree condition, ExpressionTree arrayTree) {
    assert (condition.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of an obligation changing for-loop, but is not a LESS_THAN tree";
    arrayTreeForLoopWithThisCondition.put(condition, arrayTree);
  }

  /**
   * Returns the list of MustCallOnElements obligations created in this loop, specified through the
   * less-than AST-node, which is the loop condition.
   *
   * @param condition the condition of a pattern-matched loop that creates a MustCallOnElements
   *     obligation
   * @return list of the methods that are the MustCallOnElements obligations created in this loop
   */
  public static List<String> whichObligationsDoesLoopWithThisConditionCreate(Tree condition) {
    assert (condition.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of a method calling for-loop, but is not a LESS_THAN tree";
    return whichObligationsDoesLoopWithThisConditionCreateMap.get(condition);
  }

  /**
   * Returns the name of the method that is called in the pattern-matched,
   * MustCallOnElements-fulfilling loop specified by the given Tree, which is the condition of said
   * loop.
   *
   * @param condition the condition of a pattern-matched loop that closes a MustCallOnElements
   *     obligation
   * @return name of the method that is called on the elements of the array in the loop
   */
  public static String whichMethodDoesLoopWithThisConditionCall(Tree condition) {
    assert (condition.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of a method calling for-loop, but is not a LESS_THAN tree";
    return whichMethodDoesLoopWithThisConditionCallMap.get(condition);
  }

  /**
   * Fetches the array AST-node, for which a MustCallOnElements obligation is opened/closed in the
   * loop, for which the given lessThan AST-node is the condition.
   *
   * @param condition the less-than AST-node
   * @return the array AST-node in the loop body
   */
  public static ExpressionTree getArrayTreeForLoopWithThisCondition(Tree condition) {
    assert (condition.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of an obligation changing for-loop, but is not a LESS_THAN tree";
    return arrayTreeForLoopWithThisCondition.get(condition);
  }

  /*
   * Change the default @MustCallOnElements type value of @OwningArray fields to contain the @MustCall
   * methods of the component, if no manual annotation is present.
   * For example the type of: final @OwningArray Socket[] s is changed to @MustCallOnElements("close").
   */
  // @Override
  // public void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean useFlow) {
  //   super.addComputedTypeAnnotations(tree, type, useFlow);
  //   if (tree instanceof VariableTree) {
  //     VariableTree varTree = (VariableTree) tree;
  //     Element elt = TreeUtils.elementFromDeclaration(varTree);
  //     boolean noMcoeAnno = true;
  //     for (AnnotationMirror paramAnno : elt.asType().getAnnotationMirrors()) {
  //       if (AnnotationUtils.areSameByName(paramAnno,
  // MustCallOnElements.class.getCanonicalName())) {
  //         // is @MustCallOnElements annotation
  //         noMcoeAnno = false;
  //         break;
  //       }
  //     }
  //     if (noMcoeAnno) { // don't override an existing manual annotation
  //       if ((elt.getKind() == ElementKind.FIELD || elt.getKind() == ElementKind.PARAMETER)
  //           && getDeclAnnotation(elt, OwningArray.class) != null) {
  //         TypeMirror componentType = ((ArrayType) elt.asType()).getComponentType();
  //         List<String> mcoeObligationsOfOwningField = getMustCallValuesForType(componentType);
  //         AnnotationMirror newType = getMustCallOnElementsType(mcoeObligationsOfOwningField);
  //         type.replaceAnnotation(newType);
  //       }
  //     }
  //   }
  // }

  /*
   * Change the default @MustCallOnElements type value of @OwningArray fields and @OwningArray method parameters
   * to contain the @MustCall methods of the component, if no manual annotation is present.
   * For example the type of: final @OwningArray Socket[] s is changed to @MustCallOnElements("close").
   */
  /* TODO could add: if (elt instanceof VariableElement) {} to ensure it's a declaration? */
  @Override
  public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
    super.addComputedTypeAnnotations(elt, type);
    if (elt.getKind() == ElementKind.METHOD) {
      // is a param @OwningArray?
      // change the type of that param
      ExecutableElement method = (ExecutableElement) elt;
      AnnotatedExecutableType methodType = (AnnotatedExecutableType) type;
      Iterator<? extends VariableElement> paramIterator = method.getParameters().iterator();
      Iterator<AnnotatedTypeMirror> paramTypeIterator = methodType.getParameterTypes().iterator();
      while (paramIterator.hasNext() && paramTypeIterator.hasNext()) {
        VariableElement param = paramIterator.next();
        AnnotatedTypeMirror paramType = paramTypeIterator.next();
        // if (getDeclAnnotation(param, OwningArray.class) != null) {
        // @OwningArray parameter
        boolean noMcoeAnno = true;
        for (AnnotationMirror paramAnno : param.asType().getAnnotationMirrors()) {
          if (AnnotationUtils.areSameByName(
              paramAnno, MustCallOnElements.class.getCanonicalName())) {
            // is @MustCallOnElements annotation
            noMcoeAnno = false;
            break;
          }
          if (AnnotationUtils.areSameByName(
              paramAnno, MustCallOnElementsUnknown.class.getCanonicalName())) {
            noMcoeAnno = false;
            break;
          }
        }
        if (noMcoeAnno) { // don't override an existing manual annotation
          TypeMirror componentType = ((ArrayType) param.asType()).getComponentType();
          List<String> mcoeObligationsOfOwningField = getMustCallValuesForType(componentType);
          AnnotationMirror newType = getMustCallOnElementsType(mcoeObligationsOfOwningField);
          paramType.replaceAnnotation(newType);
        }
      }
    } else if (elt.asType() instanceof ArrayType) {
      if (!(elt instanceof VariableElement)) {
        // not a declaration
        return;
      }
      boolean noMcoeAnno = true;
      for (AnnotationMirror paramAnno : elt.asType().getAnnotationMirrors()) {
        if (AnnotationUtils.areSameByName(paramAnno, MustCallOnElements.class.getCanonicalName())) {
          // is @MustCallOnElements annotation
          noMcoeAnno = false;
          break;
        }
        if (AnnotationUtils.areSameByName(
            paramAnno, MustCallOnElementsUnknown.class.getCanonicalName())) {
          noMcoeAnno = false;
          break;
        }
      }
      if (noMcoeAnno) { // don't override an existing manual annotation
        if ((elt.getKind() == ElementKind.FIELD
                && getDeclAnnotation(elt, OwningArray.class) != null)
            || elt.getKind() == ElementKind.PARAMETER) {
          TypeMirror componentType = ((ArrayType) elt.asType()).getComponentType();
          List<String> mcoeObligationsOfOwningField = getMustCallValuesForType(componentType);
          AnnotationMirror newType = getMustCallOnElementsType(mcoeObligationsOfOwningField);
          type.replaceAnnotation(newType);
        }
      }
    }
  }

  /**
   * Generate an annotation from a list of method names.
   *
   * @param methodNames the names of the methods to add to the type
   * @return the annotation with the given methods as value
   */
  private @Nullable AnnotationMirror getMustCallOnElementsType(List<String> methodNames) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, BOTTOM);
    builder.setValue("value", CollectionsPlume.withoutDuplicatesSorted(methodNames));
    return builder.build();
  }

  /**
   * Returns the list of mustcall obligations for a type.
   *
   * @param type the type
   * @return the list of mustcall obligations for the type
   */
  private List<String> getMustCallValuesForType(TypeMirror type) {
    InheritableMustCall imcAnnotation =
        TypesUtils.getClassFromType(type).getAnnotation(InheritableMustCall.class);
    MustCall mcAnnotation = TypesUtils.getClassFromType(type).getAnnotation(MustCall.class);
    Set<String> mcValues = new HashSet<>();
    if (mcAnnotation != null) {
      mcValues.addAll(Arrays.asList(mcAnnotation.value()));
    }
    if (imcAnnotation != null) {
      mcValues.addAll(Arrays.asList(imcAnnotation.value()));
    }
    return new ArrayList<>(mcValues);
  }

  @Override
  public void setRoot(@Nullable CompilationUnitTree root) {
    super.setRoot(root);
    // TODO: This should probably be guarded by isSafeToClearSharedCFG from
    // GenericAnnotatedTypeFactory, but this works here because we know the Must Call Checker is
    // always the first subchecker that's sharing tempvars.
    tempVars.clear();
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
  public AnnotationMirror createMustCallOnElements(List<String> val) {
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

  /**
   * Returns true if the given type should never have a must-call-on-elements obligation.
   *
   * @param type the type to check
   * @return true if the given type should never have a must-call-on-elements obligation
   */
  public boolean shouldHaveNoMustCallOnElementsObligation(TypeMirror type) {
    return type.getKind().isPrimitive() || TypesUtils.isClass(type) || TypesUtils.isString(type);
  }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new MustCallOnElementsQualifierHierarchy(
        this.getSupportedTypeQualifiers(), this.getProcessingEnv(), this);
  }

  /** Qualifier hierarchy for the MustCallOnElements Checker. */
  class MustCallOnElementsQualifierHierarchy extends SubtypeIsSubsetQualifierHierarchy {

    /**
     * Creates a SubtypeIsSubsetQualifierHierarchy from the given classes.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
     * @param processingEnv processing environment
     * @param atypeFactory the associated type factory
     */
    public MustCallOnElementsQualifierHierarchy(
        Collection<Class<? extends Annotation>> qualifierClasses,
        ProcessingEnvironment processingEnv,
        GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
      super(qualifierClasses, processingEnv, atypeFactory);
    }

    @Override
    public boolean isSubtypeShallow(
        AnnotationMirror subQualifier,
        TypeMirror subType,
        AnnotationMirror superQualifier,
        TypeMirror superType) {
      if (shouldHaveNoMustCallOnElementsObligation(subType)
          || shouldHaveNoMustCallOnElementsObligation(superType)) {
        return true;
      }
      return super.isSubtypeShallow(subQualifier, subType, superQualifier, superType);
    }

    @Override
    public @Nullable AnnotationMirror leastUpperBoundShallow(
        AnnotationMirror qualifier1, TypeMirror tm1, AnnotationMirror qualifier2, TypeMirror tm2) {
      boolean tm1NoMustCallOnElements = shouldHaveNoMustCallOnElementsObligation(tm1);
      boolean tm2NoMustCallOnElements = shouldHaveNoMustCallOnElementsObligation(tm2);
      if (tm1NoMustCallOnElements == tm2NoMustCallOnElements) {
        return super.leastUpperBoundShallow(qualifier1, tm1, qualifier2, tm2);
      } else if (tm1NoMustCallOnElements) {
        return qualifier1;
      } else { // if (tm2NoMustCallOnElements) {
        return qualifier2;
      }
    }
  }

  /**
   * The TreeAnnotator for the MustCallOnElements type system.
   *
   * <p>This tree annotator treats non-owning method parameters as bottom, regardless of their
   * declared type, when they appear in the body of the method. Doing so is safe because being
   * non-owning means, by definition, that their must-call obligations are only relevant in the
   * callee. (This behavior is disabled if the {@code -AnoLightweightOwnership} option is passed to
   *
   * <p>The tree annotator also changes the type of resource variables to remove "close" from their
   * must-call types, because the try-with-resources statement guarantees that close() is called on
   * all such variables.
   */
  private class MustCallOnElementsTreeAnnotator extends TreeAnnotator {
    /**
     * Create a MustCallOnElementsTreeAnnotator.
     *
     * @param mustCallOnElementsAnnotatedTypeFactory the type factory
     */
    public MustCallOnElementsTreeAnnotator(
        MustCallOnElementsAnnotatedTypeFactory mustCallOnElementsAnnotatedTypeFactory) {
      super(mustCallOnElementsAnnotatedTypeFactory);
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, AnnotatedTypeMirror type) {
      Element elt = TreeUtils.elementFromUse(tree);
      // The following changes are not desired for RLC _inference_ in unannotated programs,
      // where a goal is to infer and add @Owning annotations to formal parameters.
      // Therefore, if WPI is enabled, they should not be executed.
      if (getWholeProgramInference() == null
          && elt.getKind() == ElementKind.PARAMETER
          && getDeclAnnotation(elt, OwningArray.class) == null) {
        // Parameters that are not annotated with @Owning should be treated as bottom
        // (to suppress warnings about them). An exception is polymorphic parameters,
        // which might be @MustCallOnElementsAlias (and so wouldn't be annotated with @Owning):
        // these are not modified, to support verification of @MustCallOnElementsAlias
        // annotations.
        type.replaceAnnotation(BOTTOM);
      }
      return super.visitIdentifier(tree, type);
    }
  }
}
