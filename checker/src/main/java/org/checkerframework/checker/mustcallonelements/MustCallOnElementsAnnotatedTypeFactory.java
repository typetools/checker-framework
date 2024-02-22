package org.checkerframework.checker.mustcallonelements;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
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
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElements;
import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElementsUnknown;
import org.checkerframework.checker.mustcallonelements.qual.OwningArray;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
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

  /** True if -AnoLightweightOwnership was passed on the command line. */
  private final boolean noLightweightOwnership;

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
    noLightweightOwnership = checker.hasOption(MustCallOnElementsChecker.NO_LIGHTWEIGHT_OWNERSHIP);
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
   * Treat non-owning-array method parameters as @MustCallOnElementsUnknown (top) when the method is
   * called.
   */
  // @Override
  // public void methodFromUsePreSubstitution(
  //     ExpressionTree tree, AnnotatedExecutableType type, boolean resolvePolyQuals) {
  //   ExecutableElement declaration;
  //   if (tree instanceof MethodInvocationTree) {
  //     declaration = TreeUtils.elementFromUse((MethodInvocationTree) tree);
  //   } else if (tree instanceof MemberReferenceTree) {
  //     declaration = (ExecutableElement) TreeUtils.elementFromUse(tree);
  //   } else {
  //     throw new TypeSystemError("unexpected type of method tree: " + tree.getKind());
  //   }
  //   changeNonOwningParameterTypesToTop(declaration, type);
  //   super.methodFromUsePreSubstitution(tree, type, resolvePolyQuals);
  // }

  /**
   * Changes the type of each parameter not annotated as @OwningArray to @MustCallOnElementsUnknown
   * (top). Also replaces the component type of the varargs array, if applicable.
   *
   * <p>This method is not responsible for handling receivers, which can never be owning.
   *
   * @param declaration a method or constructor declaration
   * @param type the method or constructor's type
   */
  // private void changeNonOwningParameterTypesToTop(
  //     ExecutableElement declaration, AnnotatedExecutableType type) {
  //   // Formal parameters without a declared owning annotation are disregarded by the RLC
  //   // _analysis_, as their @MustCallOnElements obligation is set to Top in this method. However,
  //   // this computation is not desirable for RLC _inference_ in unannotated programs,
  //   // where a goal is to infer and add @Owning annotations to formal parameters.
  //   if (getWholeProgramInference() != null && !isWpiEnabledForRLC()) {
  //     return;
  //   }
  //   List<AnnotatedTypeMirror> parameterTypes = type.getParameterTypes();
  //   for (int i = 0; i < parameterTypes.size(); i++) {
  //     Element paramDecl = declaration.getParameters().get(i);
  //     if (noLightweightOwnership || getDeclAnnotation(paramDecl, OwningArray.class) == null) {
  //       AnnotatedTypeMirror paramType = parameterTypes.get(i);
  //       paramType.replaceAnnotation(TOP);
  //     }
  //   }
  //   if (declaration.isVarArgs()) {
  //     // also modify the component type of a varargs array
  //     AnnotatedTypeMirror varargsType =
  //         ((AnnotatedArrayType) parameterTypes.get(parameterTypes.size() -
  // 1)).getComponentType();
  //     varargsType.replaceAnnotation(TOP);
  //   }
  // }

  // @Override
  // protected void constructorFromUsePreSubstitution(
  //     NewClassTree tree, AnnotatedExecutableType type, boolean resolvePolyQuals) {
  //   ExecutableElement declaration = TreeUtils.elementFromUse(tree);
  //   changeNonOwningParameterTypesToTop(declaration, type);
  //   super.constructorFromUsePreSubstitution(tree, type, resolvePolyQuals);
  // }

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
        : "Trying to associate Tree as condition of a method calling for-loop, but is not a LESS_THAN tree";
    whichObligationsDoesLoopWithThisConditionCreateMap.put(tree, methods);
  }

  public static void closeArrayObligationForLessThan(Tree tree, String method) {
    assert (tree.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of a method calling for-loop, but is not a LESS_THAN tree";
    whichMethodDoesLoopWithThisConditionCallMap.put(tree, method);
  }

  public static void putArrayAffectedByLoopWithThisCondition(
      Tree condition, ExpressionTree arrayTree) {
    assert (condition.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of an obligation changing for-loop, but is not a LESS_THAN tree";
    arrayTreeForLoopWithThisCondition.put(condition, arrayTree);
  }

  public static List<String> whichObligationsDoesLoopWithThisConditionCreate(Tree condition) {
    assert (condition.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of a method calling for-loop, but is not a LESS_THAN tree";
    return whichObligationsDoesLoopWithThisConditionCreateMap.get(condition);
  }

  public static String whichMethodDoesLoopWithThisConditionCall(Tree condition) {
    assert (condition.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of a method calling for-loop, but is not a LESS_THAN tree";
    return whichMethodDoesLoopWithThisConditionCallMap.get(condition);
  }

  public static ExpressionTree getArrayTreeForLoopWithThisCondition(Tree condition) {
    assert (condition.getKind() == Tree.Kind.LESS_THAN)
        : "Trying to associate Tree as condition of an obligation changing for-loop, but is not a LESS_THAN tree";
    return arrayTreeForLoopWithThisCondition.get(condition);
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

  /** Qualifier hierarchy for the Must Call Checker. */
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
          && (noLightweightOwnership || getDeclAnnotation(elt, OwningArray.class) == null)) {
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

  // @Override
  // protected QualifierUpperBounds createQualifierUpperBounds() {
  //   return new MustCallOnElementsQualifierUpperBounds();
  // }

  // /** Support @InheritableMustCallOnElements meaning @MustCallOnElements on all subtypes. */
  // class MustCallOnElementsQualifierUpperBounds extends QualifierUpperBounds {

  //   /**
  //    * Creates a {@link QualifierUpperBounds} from the MustCallOnElements Checker the annotations
  // that are in
  //    * the type hierarchy.
  //    */
  //   public MustCallOnElementsQualifierUpperBounds() {
  //     super(MustCallOnElementsAnnotatedTypeFactory.this);
  //   }

  //   @Override
  //   protected AnnotationMirrorSet getAnnotationFromElement(Element element) {
  //     AnnotationMirrorSet explict = super.getAnnotationFromElement(element);
  //     if (!explict.isEmpty()) {
  //       return explict;
  //     }
  //     AnnotationMirror inheritableMustCallOnElements = getDeclAnnotation(element,
  // InheritableMustCallOnElements.class);
  //     if (inheritableMustCallOnElements != null) {
  //       List<String> mustCallOnElementsVal =
  //           AnnotationUtils.getElementValueArray(
  //               inheritableMustCallOnElements, inheritableMustCallOnElementsValueElement,
  // String.class);
  //       return AnnotationMirrorSet.singleton(createMustCallOnElements(mustCallOnElementsVal));
  //     }
  //     return AnnotationMirrorSet.emptySet();
  //   }
  // }
}
