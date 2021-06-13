package org.checkerframework.checker.mustcall;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.mustcall.qual.PolyMustCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.QualifierUpperBounds;
import org.checkerframework.framework.type.SubtypeIsSubsetQualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.DefaultQualifierForUseTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The annotated type factory for the Must Call Checker. Primarily responsible for the subtyping
 * rules between @MustCall annotations.
 */
public class MustCallAnnotatedTypeFactory extends BaseAnnotatedTypeFactory
    implements CreatesMustCallForElementSupplier {

  /** The {@code @}{@link MustCallUnknown} annotation. */
  public final AnnotationMirror TOP;

  /** The {@code @}{@link MustCall}{@code ()} annotation. It is the default in unannotated code. */
  public final AnnotationMirror BOTTOM;

  /** The {@code @}{@link PolyMustCall} annotation. */
  final AnnotationMirror POLY;

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
  /* package-private */ final HashMap<Tree, LocalVariableNode> tempVars = new HashMap<>();

  /** The MustCall.value field/element. */
  final ExecutableElement mustCallValueElement =
      TreeUtils.getMethod(MustCall.class, "value", 0, processingEnv);

  /** The InheritableMustCall.value field/element. */
  final ExecutableElement inheritableMustCallValueElement =
      TreeUtils.getMethod(InheritableMustCall.class, "value", 0, processingEnv);

  /** The CreatesMustCallFor.List.value field/element. */
  private final ExecutableElement createsMustCallForListValueElement =
      TreeUtils.getMethod(CreatesMustCallFor.List.class, "value", 0, processingEnv);

  /** The CreatesMustCallFor.value field/element. */
  private final ExecutableElement createsMustCallForValueElement =
      TreeUtils.getMethod(CreatesMustCallFor.class, "value", 0, processingEnv);

  /**
   * Creates a MustCallAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  public MustCallAnnotatedTypeFactory(final BaseTypeChecker checker) {
    super(checker);
    TOP = AnnotationBuilder.fromClass(elements, MustCallUnknown.class);
    BOTTOM = createMustCall(Collections.emptyList());
    POLY = AnnotationBuilder.fromClass(elements, PolyMustCall.class);
    addAliasedTypeAnnotation(InheritableMustCall.class, MustCall.class, true);
    if (!checker.hasOption(MustCallChecker.NO_RESOURCE_ALIASES)) {
      // In NO_RESOURCE_ALIASES mode, all @MustCallAlias annotations are ignored.
      addAliasedTypeAnnotation(MustCallAlias.class, POLY);
    }
    this.postInit();
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
    // Explicitly name the qualifiers, in order to exclude @MustCallAlias.
    return new LinkedHashSet<>(
        Arrays.asList(MustCall.class, MustCallUnknown.class, PolyMustCall.class));
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(super.createTreeAnnotator(), new MustCallTreeAnnotator(this));
  }

  @Override
  protected TypeAnnotator createTypeAnnotator() {
    return new ListTypeAnnotator(super.createTypeAnnotator(), new MustCallTypeAnnotator(this));
  }

  /**
   * Returns a {@literal @}MustCall annotation that is like the input, but it does not have "close".
   * Returns the argument annotation mirror (not a new one) if the argument doesn't have "close" as
   * one of its elements.
   *
   * <p>If the argument is null, returns bottom.
   *
   * @param anno a MustCall annotation
   * @return a MustCall annotation that does not have "close" as one of its values, but is otherwise
   *     identical to anno
   */
  // Package private to permit usage from the visitor in the common assignment check.
  /* package-private */ AnnotationMirror withoutClose(@Nullable AnnotationMirror anno) {
    if (anno == null || AnnotationUtils.areSame(anno, BOTTOM)) {
      return BOTTOM;
    } else if (!AnnotationUtils.areSameByName(
        anno, "org.checkerframework.checker.mustcall.qual.MustCall")) {
      return anno;
    }
    List<String> values =
        AnnotationUtils.getElementValueArray(anno, mustCallValueElement, String.class);
    // Use `removeAll` because `remove` only removes the first occurrence.
    if (values.removeAll(Collections.singletonList("close"))) {
      return createMustCall(values);
    } else {
      return anno;
    }
  }

  /**
   * Returns true iff the given element is a resource variable.
   *
   * @param elt an element; may be null, in which case this method always returns false
   * @return true iff the given element represents a resource variable
   */
  private boolean isResourceVariable(@Nullable Element elt) {
    return elt != null && elt.getKind() == ElementKind.RESOURCE_VARIABLE;
  }

  /** Treat non-owning method parameters as @MustCallUnknown (top) when the method is called. */
  @Override
  public void methodFromUsePreSubstitution(ExpressionTree tree, AnnotatedExecutableType type) {
    ExecutableElement declaration;
    if (tree instanceof MethodInvocationTree) {
      declaration = TreeUtils.elementFromUse((MethodInvocationTree) tree);
    } else if (tree instanceof MemberReferenceTree) {
      declaration = (ExecutableElement) TreeUtils.elementFromTree(tree);
    } else {
      throw new BugInCF("unexpected type of method tree: " + tree.getKind());
    }
    changeNonOwningParameterTypesToTop(declaration, type);
    super.methodFromUsePreSubstitution(tree, type);
  }

  @Override
  protected void constructorFromUsePreSubstitution(
      NewClassTree tree, AnnotatedExecutableType type) {
    ExecutableElement declaration = TreeUtils.elementFromUse(tree);
    changeNonOwningParameterTypesToTop(declaration, type);
    super.constructorFromUsePreSubstitution(tree, type);
  }

  /**
   * Changes the type of each parameter not annotated as @Owning to @MustCallUnknown (top). Also
   * replaces the component type of the varargs array, if applicable.
   *
   * <p>This method is not responsible for handling receivers, which can never be owning.
   *
   * @param declaration a method or constructor declaration
   * @param type the method or constructor's type
   */
  private void changeNonOwningParameterTypesToTop(
      ExecutableElement declaration, AnnotatedExecutableType type) {
    List<AnnotatedTypeMirror> parameterTypes = type.getParameterTypes();
    for (int i = 0; i < parameterTypes.size(); i++) {
      Element paramDecl = declaration.getParameters().get(i);
      if (checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
          || getDeclAnnotation(paramDecl, Owning.class) == null) {
        AnnotatedTypeMirror paramType = parameterTypes.get(i);
        if (!paramType.hasAnnotation(POLY)) {
          paramType.replaceAnnotation(TOP);
        }
      }
    }
    if (declaration.isVarArgs()) {
      // also modify the component type of a varargs array
      AnnotatedTypeMirror varargsType =
          ((AnnotatedArrayType) parameterTypes.get(parameterTypes.size() - 1)).getComponentType();
      if (!varargsType.hasAnnotation(POLY)) {
        varargsType.replaceAnnotation(TOP);
      }
    }
  }

  @Override
  protected DefaultQualifierForUseTypeAnnotator createDefaultForUseTypeAnnotator() {
    return new MustCallDefaultQualifierForUseTypeAnnotator();
  }

  /** Support @InheritableMustCall meaning @MustCall on all subtype elements. */
  class MustCallDefaultQualifierForUseTypeAnnotator extends DefaultQualifierForUseTypeAnnotator {

    /** Creates a {@code MustCallDefaultQualifierForUseTypeAnnotator}. */
    public MustCallDefaultQualifierForUseTypeAnnotator() {
      super(MustCallAnnotatedTypeFactory.this);
    }

    @Override
    protected Set<AnnotationMirror> getExplicitAnnos(Element element) {
      Set<AnnotationMirror> explict = super.getExplicitAnnos(element);
      if (explict.isEmpty() && ElementUtils.isTypeElement(element)) {
        AnnotationMirror inheritableMustCall =
            getDeclAnnotation(element, InheritableMustCall.class);
        if (inheritableMustCall != null) {
          List<String> mustCallVal =
              AnnotationUtils.getElementValueArray(
                  inheritableMustCall, inheritableMustCallValueElement, String.class);
          return Collections.singleton(createMustCall(mustCallVal));
        }
      }
      return explict;
    }
  }

  @Override
  protected QualifierUpperBounds createQualifierUpperBounds() {
    return new MustCallQualifierUpperBounds();
  }

  /** Support @InheritableMustCall meaning @MustCall on all subtypes. */
  class MustCallQualifierUpperBounds extends QualifierUpperBounds {

    /**
     * Creates a {@link QualifierUpperBounds} from the MustCall Checker the annotations that are in
     * the type hierarchy.
     */
    public MustCallQualifierUpperBounds() {
      super(MustCallAnnotatedTypeFactory.this);
    }

    @Override
    protected Set<AnnotationMirror> getAnnotationFromElement(Element element) {
      Set<AnnotationMirror> explict = super.getAnnotationFromElement(element);
      if (!explict.isEmpty()) {
        return explict;
      }
      AnnotationMirror inheritableMustCall = getDeclAnnotation(element, InheritableMustCall.class);
      if (inheritableMustCall != null) {
        List<String> mustCallVal =
            AnnotationUtils.getElementValueArray(
                inheritableMustCall, inheritableMustCallValueElement, String.class);
        return Collections.singleton(createMustCall(mustCallVal));
      }
      return Collections.emptySet();
    }
  }

  /**
   * Cache of the MustCall annotations that have actually been created. Most programs require few
   * distinct MustCall annotations (e.g. MustCall() and MustCall("close")).
   */
  private Map<List<String>, AnnotationMirror> mustCallAnnotations = new HashMap<>(10);

  /**
   * Creates a {@link MustCall} annotation whose values are the given strings.
   *
   * @param val the methods that should be called
   * @return an annotation indicating that the given methods should be called
   */
  public AnnotationMirror createMustCall(final List<String> val) {
    return mustCallAnnotations.computeIfAbsent(val, this::createMustCallImpl);
  }

  /**
   * Creates a {@link MustCall} annotation whose values are the given strings.
   *
   * <p>This internal version bypasses the cache, and is only used for new annotations.
   *
   * @param methodList the methods that should be called
   * @return an annotation indicating that the given methods should be called
   */
  private AnnotationMirror createMustCallImpl(List<String> methodList) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, MustCall.class);
    String[] methodArray = methodList.toArray(new String[methodList.size()]);
    Arrays.sort(methodArray);
    builder.setValue("value", methodArray);
    return builder.build();
  }

  @Override
  public QualifierHierarchy createQualifierHierarchy() {
    return new SubtypeIsSubsetQualifierHierarchy(
        this.getSupportedTypeQualifiers(), this.getProcessingEnv());
  }

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
   * Returns the CreatesMustCallFor.value field/element.
   *
   * @return the CreatesMustCallFor.value field/element
   */
  @Override
  public ExecutableElement getCreatesMustCallForValueElement() {
    return createsMustCallForValueElement;
  }

  /**
   * Returns the CreatesMustCallFor.List.value field/element.
   *
   * @return the CreatesMustCallFor.List.value field/element
   */
  @Override
  public ExecutableElement getCreatesMustCallForListValueElement() {
    return createsMustCallForListValueElement;
  }

  /**
   * The TreeAnnotator for the MustCall type system.
   *
   * <p>This tree annotator treats non-owning method parameters as bottom, regardless of their
   * declared type, when they appear in the body of the method. Doing so is safe because being
   * non-owning means, by definition, that their must-call obligations are only relevant in the
   * callee. (This behavior is disabled if the -AnoLightweightOwnership option is passed to the
   * checker.)
   *
   * <p>The tree annotator also changes the type of resource variables to remove "close" from their
   * must-call types, because the try-with-resources statement guarantees that close() is called on
   * all such variables.
   */
  private class MustCallTreeAnnotator extends TreeAnnotator {
    /**
     * Create a MustCallTreeAnnotator.
     *
     * @param mustCallAnnotatedTypeFactory the type factory
     */
    public MustCallTreeAnnotator(MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory) {
      super(mustCallAnnotatedTypeFactory);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, AnnotatedTypeMirror type) {
      Element elt = TreeUtils.elementFromTree(node);
      if (elt.getKind() == ElementKind.PARAMETER
          && (checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
              || getDeclAnnotation(elt, Owning.class) == null)) {
        type.replaceAnnotation(BOTTOM);
      }
      if (isResourceVariable(TreeUtils.elementFromTree(node))) {
        type.replaceAnnotation(withoutClose(type.getAnnotationInHierarchy(TOP)));
      }
      return super.visitIdentifier(node, type);
    }
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
}
