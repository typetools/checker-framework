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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.mustcall.qual.PolyMustCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.QualifierUpperBounds;
import org.checkerframework.framework.type.SubtypeIsSubsetQualifierHierarchy;
import org.checkerframework.framework.type.poly.DefaultQualifierPolymorphism;
import org.checkerframework.framework.type.poly.QualifierPolymorphism;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.DefaultQualifierForUseTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;

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
  public final AnnotationMirror POLY;

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

  /** The MustCall.value field/element. */
  private final ExecutableElement mustCallValueElement =
      TreeUtils.getMethod(MustCall.class, "value", 0, processingEnv);

  /** The InheritableMustCall.value field/element. */
  /*package-private*/ final ExecutableElement inheritableMustCallValueElement =
      TreeUtils.getMethod(InheritableMustCall.class, "value", 0, processingEnv);

  /** The CreatesMustCallFor.List.value field/element. */
  private final ExecutableElement createsMustCallForListValueElement =
      TreeUtils.getMethod(CreatesMustCallFor.List.class, "value", 0, processingEnv);

  /** The CreatesMustCallFor.value field/element. */
  private final ExecutableElement createsMustCallForValueElement =
      TreeUtils.getMethod(CreatesMustCallFor.class, "value", 0, processingEnv);

  /** True if -AnoLightweightOwnership was passed on the command line. */
  private final boolean noLightweightOwnership;

  /**
   * True if -AenableWpiForRlc (see {@link ResourceLeakChecker#ENABLE_WPI_FOR_RLC}) was passed on
   * the command line.
   */
  private final boolean enableWpiForRlc;

  /**
   * Creates a MustCallAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  public MustCallAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    TOP = AnnotationBuilder.fromClass(elements, MustCallUnknown.class);
    BOTTOM = createMustCall(Collections.emptyList());
    POLY = AnnotationBuilder.fromClass(elements, PolyMustCall.class);
    addAliasedTypeAnnotation(InheritableMustCall.class, MustCall.class, true);
    if (!checker.hasOption(MustCallChecker.NO_RESOURCE_ALIASES)) {
      // In NO_RESOURCE_ALIASES mode, all @MustCallAlias annotations are ignored.
      addAliasedTypeAnnotation(MustCallAlias.class, POLY);
    }
    noLightweightOwnership = checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP);
    enableWpiForRlc = checker.hasOption(ResourceLeakChecker.ENABLE_WPI_FOR_RLC);
    this.postInit();
  }

  @Override
  public void setRoot(@Nullable CompilationUnitTree newRoot) {
    super.setRoot(newRoot);
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
  public AnnotationMirror withoutClose(@Nullable AnnotationMirror anno) {
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

  /** Treat non-owning method parameters as @MustCallUnknown (top) when the method is called. */
  @Override
  public void methodFromUsePreSubstitution(
      ExpressionTree tree, AnnotatedExecutableType type, boolean resolvePolyQuals) {
    ExecutableElement declaration;
    if (tree instanceof MethodInvocationTree) {
      declaration = TreeUtils.elementFromUse((MethodInvocationTree) tree);
    } else if (tree instanceof MemberReferenceTree) {
      declaration = (ExecutableElement) TreeUtils.elementFromUse(tree);
    } else {
      throw new TypeSystemError("unexpected type of method tree: " + tree.getKind());
    }
    changeNonOwningParameterTypesToTop(declaration, type);
    super.methodFromUsePreSubstitution(tree, type, resolvePolyQuals);
  }

  @Override
  protected void constructorFromUsePreSubstitution(
      NewClassTree tree, AnnotatedExecutableType type, boolean resolvePolyQuals) {
    ExecutableElement declaration = TreeUtils.elementFromUse(tree);
    changeNonOwningParameterTypesToTop(declaration, type);
    super.constructorFromUsePreSubstitution(tree, type, resolvePolyQuals);
  }

  /**
   * Class to implement the customized semantics of {@link MustCallAlias} (and {@link PolyMustCall})
   * annotations; see the {@link MustCallAlias} documentation for details.
   */
  private class MustCallQualifierPolymorphism extends DefaultQualifierPolymorphism {
    /**
     * Creates a {@link MustCallQualifierPolymorphism}.
     *
     * @param env the processing environment
     * @param factory the factory for the current checker
     */
    public MustCallQualifierPolymorphism(ProcessingEnvironment env, AnnotatedTypeFactory factory) {
      super(env, factory);
    }

    @Override
    protected void replace(
        AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirror> replacements) {
      AnnotationMirrorMap<AnnotationMirror> realReplacements = replacements;
      AnnotationMirror extantPolyAnnoReplacement = null;
      TypeElement typeElement = TypesUtils.getTypeElement(type.getUnderlyingType());
      // only customize replacement for type elements
      if (typeElement != null) {
        assert replacements.size() == 1 && replacements.containsKey(POLY);
        extantPolyAnnoReplacement = replacements.get(POLY);
        if (AnnotationUtils.areSameByName(
            extantPolyAnnoReplacement, MustCall.class.getCanonicalName())) {
          List<String> extentReplacementVals =
              AnnotationUtils.getElementValueArray(
                  extantPolyAnnoReplacement,
                  getMustCallValueElement(),
                  String.class,
                  Collections.emptyList());
          // Replacement is only customized when the parameter type has a non-empty
          // must-call obligation.
          if (!extentReplacementVals.isEmpty()) {
            AnnotationMirror inheritableMustCall =
                getDeclAnnotation(typeElement, InheritableMustCall.class);
            if (inheritableMustCall != null) {
              List<String> inheritableMustCallVals =
                  AnnotationUtils.getElementValueArray(
                      inheritableMustCall,
                      inheritableMustCallValueElement,
                      String.class,
                      Collections.emptyList());
              if (!inheritableMustCallVals.equals(extentReplacementVals)) {
                // Use the must call values from the @InheritableMustCall annotation
                // instead. This allows for wrapper types to have a must-call method
                // with a different name than the must-call method for the wrapped
                // type.
                AnnotationMirror mustCall = createMustCall(inheritableMustCallVals);
                realReplacements = new AnnotationMirrorMap<>();
                realReplacements.put(POLY, mustCall);
              }
            }
          }
        }
      }
      super.replace(type, realReplacements);
    }
  }

  @Override
  protected QualifierPolymorphism createQualifierPolymorphism() {
    return new MustCallQualifierPolymorphism(processingEnv, this);
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
    // Formal parameters without a declared owning annotation are disregarded by the RLC
    // _analysis_, as their @MustCall obligation is set to Top in this method. However,
    // this computation is not desirable for RLC _inference_ in unannotated programs,
    // where a goal is to infer and add @Owning annotations to formal parameters.
    if (getWholeProgramInference() != null && !isWpiEnabledForRLC()) {
      return;
    }
    List<AnnotatedTypeMirror> parameterTypes = type.getParameterTypes();
    for (int i = 0; i < parameterTypes.size(); i++) {
      Element paramDecl = declaration.getParameters().get(i);
      if (noLightweightOwnership || getDeclAnnotation(paramDecl, Owning.class) == null) {
        AnnotatedTypeMirror paramType = parameterTypes.get(i);
        if (!paramType.hasPrimaryAnnotation(POLY)) {
          paramType.replaceAnnotation(TOP);
        }
      }
    }
    if (declaration.isVarArgs()) {
      // also modify the component type of a varargs array
      AnnotatedTypeMirror varargsType =
          ((AnnotatedArrayType) parameterTypes.get(parameterTypes.size() - 1)).getComponentType();
      if (!varargsType.hasPrimaryAnnotation(POLY)) {
        varargsType.replaceAnnotation(TOP);
      }
    }
  }

  @Override
  protected DefaultQualifierForUseTypeAnnotator createDefaultForUseTypeAnnotator() {
    return new MustCallDefaultQualifierForUseTypeAnnotator();
  }

  /**
   * Returns the {@link MustCall#value} element. For use with {@link
   * AnnotationUtils#getElementValueArray}.
   *
   * @return the {@link MustCall#value} element
   */
  public ExecutableElement getMustCallValueElement() {
    return mustCallValueElement;
  }

  /**
   * Returns the {@link InheritableMustCall#value} element.
   *
   * @return the {@link InheritableMustCall#value} element
   */
  public ExecutableElement getInheritableMustCallValueElement() {
    return inheritableMustCallValueElement;
  }

  /** Support @InheritableMustCall meaning @MustCall on all subtype elements. */
  private class MustCallDefaultQualifierForUseTypeAnnotator
      extends DefaultQualifierForUseTypeAnnotator {

    /** Creates a {@code MustCallDefaultQualifierForUseTypeAnnotator}. */
    public MustCallDefaultQualifierForUseTypeAnnotator() {
      super(MustCallAnnotatedTypeFactory.this);
    }

    @Override
    protected AnnotationMirrorSet getExplicitAnnos(Element element) {
      AnnotationMirrorSet explict = super.getExplicitAnnos(element);
      if (explict.isEmpty() && ElementUtils.isTypeElement(element)) {
        AnnotationMirror inheritableMustCall =
            getDeclAnnotation(element, InheritableMustCall.class);
        if (inheritableMustCall != null) {
          List<String> mustCallVal =
              AnnotationUtils.getElementValueArray(
                  inheritableMustCall, inheritableMustCallValueElement, String.class);
          return AnnotationMirrorSet.singleton(createMustCall(mustCallVal));
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
  private class MustCallQualifierUpperBounds extends QualifierUpperBounds {

    /**
     * Creates a {@link QualifierUpperBounds} from the MustCall Checker the annotations that are in
     * the type hierarchy.
     */
    public MustCallQualifierUpperBounds() {
      super(MustCallAnnotatedTypeFactory.this);
    }

    @Override
    protected AnnotationMirrorSet getAnnotationFromElement(Element element) {
      AnnotationMirrorSet explict = super.getAnnotationFromElement(element);
      if (!explict.isEmpty()) {
        return explict;
      }
      AnnotationMirror inheritableMustCall = getDeclAnnotation(element, InheritableMustCall.class);
      if (inheritableMustCall != null) {
        List<String> mustCallVal =
            AnnotationUtils.getElementValueArray(
                inheritableMustCall, inheritableMustCallValueElement, String.class);
        return AnnotationMirrorSet.singleton(createMustCall(mustCallVal));
      }
      return AnnotationMirrorSet.emptySet();
    }
  }

  /**
   * Cache of the MustCall annotations that have actually been created. Most programs require few
   * distinct MustCall annotations (e.g. MustCall() and MustCall("close")).
   */
  private final Map<List<String>, AnnotationMirror> mustCallAnnotations = new HashMap<>(10);

  /**
   * Creates a {@link MustCall} annotation whose values are the given strings.
   *
   * @param val the methods that should be called
   * @return an annotation indicating that the given methods should be called
   */
  public AnnotationMirror createMustCall(List<String> val) {
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
    String[] methodArray = methodList.toArray(new String[0]);
    Arrays.sort(methodArray);
    builder.setValue("value", methodArray);
    return builder.build();
  }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new MustCallQualifierHierarchy(
        this.getSupportedTypeQualifiers(), this.getProcessingEnv(), this);
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
   * callee. (This behavior is disabled if the {@code -AnoLightweightOwnership} option is passed to
   * the checker.)
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
    public Void visitIdentifier(IdentifierTree tree, AnnotatedTypeMirror type) {
      Element elt = TreeUtils.elementFromUse(tree);
      // The following changes are not desired for RLC _inference_ in unannotated programs,
      // where a goal is to infer and add @Owning annotations to formal parameters.
      // Therefore, if WPI is enabled, they should not be executed.
      if (getWholeProgramInference() == null
          && elt.getKind() == ElementKind.PARAMETER
          && (noLightweightOwnership || getDeclAnnotation(elt, Owning.class) == null)) {
        if (!type.hasPrimaryAnnotation(POLY)) {
          // Parameters that are not annotated with @Owning should be treated as bottom
          // (to suppress warnings about them). An exception is polymorphic parameters,
          // which might be @MustCallAlias (and so wouldn't be annotated with @Owning):
          // these are not modified, to support verification of @MustCallAlias
          // annotations.
          type.replaceAnnotation(BOTTOM);
        }
      }
      return super.visitIdentifier(tree, type);
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
   * Returns true if the given type should never have a must-call obligation.
   *
   * @param type the type to check
   * @return true if the given type should never have a must-call obligation
   */
  public boolean shouldHaveNoMustCallObligation(TypeMirror type) {
    return type.getKind().isPrimitive() || TypesUtils.isClass(type) || TypesUtils.isString(type);
  }

  /** Qualifier hierarchy for the Must Call Checker. */
  private class MustCallQualifierHierarchy extends SubtypeIsSubsetQualifierHierarchy {

    /**
     * Creates a SubtypeIsSubsetQualifierHierarchy from the given classes.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
     * @param processingEnv processing environment
     * @param atypeFactory the associated type factory
     */
    public MustCallQualifierHierarchy(
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
      if (shouldHaveNoMustCallObligation(subType) || shouldHaveNoMustCallObligation(superType)) {
        return true;
      }
      return super.isSubtypeShallow(subQualifier, subType, superQualifier, superType);
    }

    @Override
    public @Nullable AnnotationMirror leastUpperBoundShallow(
        AnnotationMirror qualifier1, TypeMirror tm1, AnnotationMirror qualifier2, TypeMirror tm2) {
      boolean tm1NoMustCall = shouldHaveNoMustCallObligation(tm1);
      boolean tm2NoMustCall = shouldHaveNoMustCallObligation(tm2);
      if (tm1NoMustCall == tm2NoMustCall) {
        return super.leastUpperBoundShallow(qualifier1, tm1, qualifier2, tm2);
      } else if (tm1NoMustCall) {
        return qualifier1;
      } else { // if (tm2NoMustCall) {
        return qualifier2;
      }
    }
  }
}
