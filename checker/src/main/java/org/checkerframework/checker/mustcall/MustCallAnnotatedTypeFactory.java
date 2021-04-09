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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import com.sun.source.tree.VariableTree;
import org.checkerframework.checker.mustcall.qual.CreatesObligation;
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
import org.checkerframework.framework.type.SubtypeIsSubsetQualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The annotated type factory for the must call checker. Primarily responsible for the subtyping
 * rules between @MustCall annotations.
 */
public class MustCallAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link MustCallUnknown} annotation. */
  public final AnnotationMirror TOP;

  /** The {@code @}{@link MustCall}{@code ()} annotation. It is the default in unannotated code. */
  public final AnnotationMirror BOTTOM;

  /** The {@code @}{@link PolyMustCall} annoattion. */
  final AnnotationMirror POLY;

  /**
   * A cache of locations at which an inconsistent.mustcall.subtype error has already been issued,
   * to avoid issuing duplicate errors. Reset with each compilation unit.
   */
  private final Set<Element> elementsIssuedInconsistentMustCallSubtypeErrors =
      new HashSet<>(this.getCacheSize());

  /**
   * Map from trees representing expressions to the temporary variables that represent them in the
   * store.
   */
  /* package-private */ HashMap<Tree, LocalVariableNode> tempVars =
      new HashMap<>(this.getCacheSize());

  /** The MustCall.value field/element. */
  final ExecutableElement mustCallValueElement =
      TreeUtils.getMethod(MustCall.class, "value", 0, processingEnv);

  /** The InheritableMustCall.value field/element. */
  final ExecutableElement inheritableMustCallValueElement =
      TreeUtils.getMethod(InheritableMustCall.class, "value", 0, processingEnv);

  /** The CreatesObligation.List.value field/element. */
  final ExecutableElement createsObligationListValueElement =
      TreeUtils.getMethod(CreatesObligation.List.class, "value", 0, processingEnv);

  /** The CreatesObligation.value field/element. */
  final ExecutableElement createsObligationValueElement =
      TreeUtils.getMethod(CreatesObligation.class, "value", 0, processingEnv);

  /**
   * Creates a MustCallAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  public MustCallAnnotatedTypeFactory(final BaseTypeChecker checker) {
    super(checker);
    TOP = AnnotationBuilder.fromClass(elements, MustCallUnknown.class);
    BOTTOM = createMustCall();
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
    elementsIssuedInconsistentMustCallSubtypeErrors.clear();
    // TODO: this should probably be guarded by isSafeToClearSharedCFG from
    // GenericAnnotatedTypeFactory, but this works here because we know the MCC is always the first
    // subchecker that's sharing tempvars.
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
    // shortcut for easy paths
    if (anno == null || AnnotationUtils.areSame(anno, BOTTOM)) {
      return BOTTOM;
    } else if (!AnnotationUtils.areSameByName(
        anno, "org.checkerframework.checker.mustcall.qual.MustCall")) {
      return anno;
    }
    List<String> values =
        AnnotationUtils.getElementValueArray(anno, mustCallValueElement, String.class);
    if (!values.contains("close")) {
      return anno;
    }
    return createMustCall(values.stream().filter(s -> !"close".equals(s)).toArray(String[]::new));
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

  /** Treat non-owning method parameters as @MustCallUnknown when the method is called. */
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
   * Changes the type of each parameter not annotated as @Owning to top. Also replaces the component
   * type of the varargs array, if applicable.
   *
   * Note that this method is not responsible for handling receivers, which can never be owning.
   *
   * @param declaration a method or constructor declaration
   * @param type the method or constructor's type
   */
  private void changeNonOwningParameterTypesToTop(
      ExecutableElement declaration, AnnotatedExecutableType type) {
    for (int i = 0; i < type.getParameterTypes().size(); i++) {
      Element paramDecl = declaration.getParameters().get(i);
      if (checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
          || getDeclAnnotation(paramDecl, Owning.class) == null) {
        AnnotatedTypeMirror paramType = type.getParameterTypes().get(i);
        if (!paramType.hasAnnotation(POLY)) {
          paramType.replaceAnnotation(TOP);
        }
        if (declaration.isVarArgs() && i == type.getParameterTypes().size() - 1) {
          // also modify the component type of a varargs array
            AnnotatedTypeMirror varargsType = ((AnnotatedArrayType) paramType).getComponentType();
            if (!varargsType.hasAnnotation(POLY)) {
              varargsType.replaceAnnotation(TOP);
            }
        }
      }
    }
  }

  @Override
  public AnnotatedTypeMirror fromElement(Element elt) {
    AnnotatedTypeMirror type = super.fromElement(elt);
    // Support @InheritableMustCall meaning @MustCall on all class declaration elements.
    if (ElementUtils.isTypeElement(elt)) {
      AnnotationMirror inheritableMustCall = getDeclAnnotation(elt, InheritableMustCall.class);
      if (inheritableMustCall != null) {
        List<String> mustCallVal =
            AnnotationUtils.getElementValueArray(
                inheritableMustCall, inheritableMustCallValueElement, String.class);
        AnnotationMirror inheritedMCAnno = createMustCall(mustCallVal.toArray(new String[0]));
        // Ensure that there isn't an inconsistent, user-written @MustCall annotation and
        // issue an error if there is. Otherwise, replace the implicit @MustCall({}) with
        // the inherited must-call annotation.
        AnnotationMirror writtenMCAnno = type.getAnnotationInHierarchy(this.TOP);
        if (writtenMCAnno != null
            && !this.getQualifierHierarchy().isSubtype(inheritedMCAnno, writtenMCAnno)) {
          if (!elementsIssuedInconsistentMustCallSubtypeErrors.contains(elt)
              && !this.checker.shouldSkipUses(elt)) {
            checker.reportError(
                elt,
                "inconsistent.mustcall.subtype",
                elt.getSimpleName(),
                writtenMCAnno,
                inheritableMustCall);
            elementsIssuedInconsistentMustCallSubtypeErrors.add(elt);
          }
        } else {
          type.replaceAnnotation(inheritedMCAnno);
        }
      }
    }
    return type;
  }

  /**
   * Creates a {@link MustCall} annotation whose values are the given strings.
   *
   * @param val the methods that should be called
   * @return an annotation indicating that the given methods should be called
   */
  public AnnotationMirror createMustCall(final String... val) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, MustCall.class);
    Arrays.sort(val);
    builder.setValue("value", val);
    return builder.build();
  }

  @Override
  public QualifierHierarchy createQualifierHierarchy() {
    return new SubtypeIsSubsetQualifierHierarchy(
        this.getSupportedTypeQualifiers(), this.getProcessingEnv());
  }

  /**
   * Fetches the store from the results of dataflow for block. If useBlock is true,
   * then the store after block is returned; if useBlock is false, the store before succ
   * is returned.
   *
   * @param useBlock whether to use the store after the block itself or the store before its successor, succ
   * @param block a block
   * @param succ block's successor
   * @return the appropriate CFStore, populated with MustCall annotations, from the results of
   *     running dataflow
   */
  public CFStore getStoreForBlock(boolean useBlock, Block block, Block succ) {
    return useBlock ? flowResult.getStoreAfter(block) : flowResult.getStoreBefore(succ);
  }

  /**
   * The TreeAnnotator for the MustCall type system. This tree annotator treats non-owning method
   * parameters as bottom, regardless of their declared type, when they appear in the body of the
   * method. Doing so is safe because being non-owning means, by definition, that their must-call
   * obligations are only relevant at the call site.
   */
  private class MustCallTreeAnnotator extends TreeAnnotator {
    /**
     * Create a MustCallTreeAnnotator
     *
     * @param mustCallAnnotatedTypeFactory the type factory
     */
    public MustCallTreeAnnotator(MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory) {
      super(mustCallAnnotatedTypeFactory);
    }

    // When they appear in the body of a method or constructor, treat non-owning parameters
    // as bottom regardless of their declared type.
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
   * Return the temporary variable for node, if it exists.
   *
   * @param node a CFG node
   * @return the corresponding temporary variable, or null if there is not one
   */
  public @Nullable LocalVariableNode getTempVar(Node node) {
    return tempVars.get(node.getTree());
  }
}
