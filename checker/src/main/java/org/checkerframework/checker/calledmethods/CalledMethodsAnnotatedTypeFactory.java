package org.checkerframework.checker.calledmethods;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collection;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.builder.AutoValueSupport;
import org.checkerframework.checker.calledmethods.builder.BuilderFrameworkSupport;
import org.checkerframework.checker.calledmethods.builder.LombokSupport;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsBottom;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsPredicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.UserError;

/** The annotated type factory for the Called Methods Checker. */
public class CalledMethodsAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

  /**
   * The builder frameworks (such as Lombok and AutoValue) supported by this instance of the Called
   * Methods Checker.
   */
  private Collection<BuilderFrameworkSupport> builderFrameworkSupports;

  /**
   * Whether to use the Value Checker as a subchecker to reduce false positives when analyzing calls
   * to the AWS SDK. Defaults to false. Controlled by the command-line option {@code
   * -AuseValueChecker}.
   */
  private final boolean useValueChecker;

  /**
   * The {@link java.util.Collections#singletonList} method. It is treated specially by {@link
   * #adjustMethodNameUsingValueChecker}.
   */
  private final ExecutableElement collectionsSingletonList =
      TreeUtils.getMethod("java.util.Collections", "singletonList", 1, getProcessingEnv());

  /** The value argument to {@link CalledMethods}. */
  /* package-private */ final ExecutableElement calledMethodsValueElement =
      TreeUtils.getMethod(CalledMethods.class, "value", 0, processingEnv);

  /**
   * Create a new CalledMethodsAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  public CalledMethodsAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker, CalledMethods.class, CalledMethodsBottom.class, CalledMethodsPredicate.class);
    this.builderFrameworkSupports = new ArrayList<>(2);
    String[] disabledFrameworks;
    if (checker.hasOption(CalledMethodsChecker.DISABLE_BUILDER_FRAMEWORK_SUPPORTS)) {
      disabledFrameworks =
          checker.getOption(CalledMethodsChecker.DISABLE_BUILDER_FRAMEWORK_SUPPORTS).split(",");
    } else {
      disabledFrameworks = new String[0];
    }
    enableFrameworks(disabledFrameworks);
    this.useValueChecker = checker.hasOption(CalledMethodsChecker.USE_VALUE_CHECKER);
    // Lombok generates @CalledMethods annotations using an old package name,
    // so we maintain it as an alias.
    addAliasedTypeAnnotation(
        "org.checkerframework.checker.builder.qual.CalledMethods", CalledMethods.class, true);
    // Lombok also generates an @NotCalledMethods annotation, which we have no support for. We
    // therefore treat it as top.
    addAliasedTypeAnnotation(
        "org.checkerframework.checker.builder.qual.NotCalledMethods", this.top);
    this.postInit();
  }

  /**
   * Enables support for the default builder-generation frameworks, except those listed in the
   * disabled builder frameworks parsed from the -AdisableBuilderFrameworkSupport option's
   * arguments. Throws a UserError if the user included an unsupported framework in the list of
   * frameworks to be disabled.
   *
   * @param disabledFrameworks the disabled builder frameworks
   */
  private void enableFrameworks(String[] disabledFrameworks) {
    boolean enableAutoValueSupport = true;
    boolean enableLombokSupport = true;
    for (String framework : disabledFrameworks) {
      switch (framework) {
        case "autovalue":
          enableAutoValueSupport = false;
          break;
        case "lombok":
          enableLombokSupport = false;
          break;
        default:
          throw new UserError(
              "Unsupported builder framework in -AdisableBuilderFrameworkSupports: " + framework);
      }
    }
    if (enableAutoValueSupport) {
      builderFrameworkSupports.add(new AutoValueSupport(this));
    }
    if (enableLombokSupport) {
      builderFrameworkSupports.add(new LombokSupport(this));
    }
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(super.createTreeAnnotator(), new CalledMethodsTreeAnnotator(this));
  }

  @Override
  protected TypeAnnotator createTypeAnnotator() {
    return new ListTypeAnnotator(super.createTypeAnnotator(), new CalledMethodsTypeAnnotator(this));
  }

  @Override
  public boolean returnsThis(MethodInvocationTree tree) {
    return super.returnsThis(tree)
        // Continue to trust but not check the old {@link
        // org.checkerframework.checker.builder.qual.ReturnsReceiver} annotation, for
        // backwards compatibility.
        || this.getDeclAnnotation(
                TreeUtils.elementFromUse(tree),
                org.checkerframework.checker.builder.qual.ReturnsReceiver.class)
            != null;
  }

  /**
   * Given a tree, returns the name of the method that the tree should be considered as calling.
   * Returns "withOwners" if the call sets an "owner", "owner-alias", or "owner-id" filter. Returns
   * "withImageIds" if the call sets an "image-ids" filter.
   *
   * <p>Package-private to permit calls from {@link CalledMethodsTransfer}.
   *
   * @param methodName the name of the method being explicitly called
   * @param tree the invocation of the method
   * @return "withOwners" or "withImageIds" if the tree is an equivalent filter addition. Otherwise,
   *     return the first argument.
   */
  // This cannot return a Name because filterTreeToMethodName cannot.
  public String adjustMethodNameUsingValueChecker(
      final String methodName, final MethodInvocationTree tree) {
    if (!useValueChecker) {
      return methodName;
    }

    ExecutableElement invokedMethod = TreeUtils.elementFromUse(tree);
    if (!ElementUtils.enclosingTypeElement(invokedMethod)
        .getQualifiedName()
        .contentEquals("com.amazonaws.services.ec2.model.DescribeImagesRequest")) {
      return methodName;
    }

    if (methodName.equals("withFilters") || methodName.equals("setFilters")) {
      ValueAnnotatedTypeFactory valueATF = getTypeFactoryOfSubchecker(ValueChecker.class);
      for (Tree filterTree : tree.getArguments()) {
        if (TreeUtils.isMethodInvocation(
            filterTree, collectionsSingletonList, getProcessingEnv())) {
          // Descend into a call to Collections.singletonList()
          filterTree = ((MethodInvocationTree) filterTree).getArguments().get(0);
        }
        String adjustedMethodName = filterTreeToMethodName(filterTree, valueATF);
        if (adjustedMethodName != null) {
          return adjustedMethodName;
        }
      }
    }
    return methodName;
  }

  /**
   * Determine the name of the method in DescribeImagesRequest that is equivalent to the Filter in
   * the given tree.
   *
   * <p>Returns null unless the argument is one of the following:
   *
   * <ul>
   *   <li>a constructor invocation of the Filter constructor whose first argument is the name, such
   *       as {@code new Filter("owner").*}, or
   *   <li>a call to the withName method, such as {@code new Filter().*.withName("owner").*}.
   * </ul>
   *
   * In those cases, it returns either the argument to the constructor or the argument to the last
   * invocation of withName ("owner" in both of the above examples).
   *
   * @param filterTree the tree that represents the filter (an argument to the withFilters or
   *     setFilters method)
   * @param valueATF the type factory from the Value Checker
   * @return the adjusted method name, or null if the method name should not be adjusted
   */
  // This cannot return a Name because filterKindToMethodName cannot.
  private @Nullable String filterTreeToMethodName(
      Tree filterTree, ValueAnnotatedTypeFactory valueATF) {
    while (filterTree != null && filterTree.getKind() == Tree.Kind.METHOD_INVOCATION) {

      MethodInvocationTree filterTreeAsMethodInvocation = (MethodInvocationTree) filterTree;
      String filterMethodName = TreeUtils.methodName(filterTreeAsMethodInvocation).toString();
      if (filterMethodName.contentEquals("withName")
          && filterTreeAsMethodInvocation.getArguments().size() >= 1) {
        Tree withNameArgTree = filterTreeAsMethodInvocation.getArguments().get(0);
        String withNameArg = ValueCheckerUtils.getExactStringValue(withNameArgTree, valueATF);
        return filterKindToMethodName(withNameArg);
      }
      // Proceed leftward (toward the receiver) in a fluent call sequence.
      filterTree = TreeUtils.getReceiverTree(filterTreeAsMethodInvocation.getMethodSelect());
    }
    // The loop has reached the beginning of a fluent sequence of method calls.  If the ultimate
    // receiver at the beginning of that fluent sequence is a call to the Filter() constructor, then
    // use the first argument to the Filter constructor, which is the name of the filter.
    if (filterTree == null) {
      return null;
    }
    if (filterTree.getKind() == Tree.Kind.NEW_CLASS) {
      ExpressionTree constructorArg = ((NewClassTree) filterTree).getArguments().get(0);
      String filterKindName = ValueCheckerUtils.getExactStringValue(constructorArg, valueATF);
      if (filterKindName != null) {
        return filterKindToMethodName(filterKindName);
      }
    }
    return null;
  }

  /**
   * Converts from a kind of filter to the name of the corresponding method on a
   * DescribeImagesRequest object.
   *
   * @param filterKind the kind of filter
   * @return "withOwners" if filterKind is "owner", "owner-alias", or "owner-id"; "withImageIds" if
   *     filterKind is "image-id"; null otherwise
   */
  private static @Nullable String filterKindToMethodName(String filterKind) {
    switch (filterKind) {
      case "owner":
      case "owner-alias":
      case "owner-id":
        return "withOwners";
      case "image-id":
        return "withImageIds";
      default:
        return null;
    }
  }

  /**
   * At a fluent method call (which returns {@code this}), add the method to the type of the return
   * value.
   */
  private class CalledMethodsTreeAnnotator extends AccumulationTreeAnnotator {
    /**
     * Creates an instance of this tree annotator for the given type factory.
     *
     * @param factory the type factory
     */
    public CalledMethodsTreeAnnotator(AccumulationAnnotatedTypeFactory factory) {
      super(factory);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
      // Accumulate a method call, by adding the method being invoked to the return type.
      if (returnsThis(tree)) {
        String methodName = TreeUtils.getMethodName(tree.getMethodSelect());
        methodName = adjustMethodNameUsingValueChecker(methodName, tree);
        AnnotationMirror oldAnno = type.getAnnotationInHierarchy(top);
        AnnotationMirror newAnno =
            qualHierarchy.greatestLowerBound(oldAnno, createAccumulatorAnnotation(methodName));
        type.replaceAnnotation(newAnno);
      }

      // Also do the standard accumulation analysis behavior: copy any accumulation
      // annotations from the receiver to the return type.
      return super.visitMethodInvocation(tree, type);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
      for (BuilderFrameworkSupport builderFrameworkSupport : builderFrameworkSupports) {
        builderFrameworkSupport.handleConstructor(tree, type);
      }
      return super.visitNewClass(tree, type);
    }
  }

  /**
   * Adds @CalledMethod annotations for build() methods of AutoValue and Lombok Builders to ensure
   * required properties have been set.
   */
  private class CalledMethodsTypeAnnotator extends TypeAnnotator {

    /**
     * Constructor matching super.
     *
     * @param atypeFactory the type factory
     */
    public CalledMethodsTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitExecutable(AnnotatedTypeMirror.AnnotatedExecutableType t, Void p) {
      ExecutableElement element = t.getElement();

      TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

      for (BuilderFrameworkSupport builderFrameworkSupport : builderFrameworkSupports) {
        if (builderFrameworkSupport.isToBuilderMethod(element)) {
          builderFrameworkSupport.handleToBuilderMethod(t);
        }
      }

      Element nextEnclosingElement = enclosingElement.getEnclosingElement();
      if (nextEnclosingElement.getKind().isClass()) {
        for (BuilderFrameworkSupport builderFrameworkSupport : builderFrameworkSupports) {
          if (builderFrameworkSupport.isBuilderBuildMethod(element)) {
            builderFrameworkSupport.handleBuilderBuildMethod(t);
          }
        }
      }

      return super.visitExecutable(t, p);
    }
  }

  /**
   * Returns the annotation type mirror for the type of {@code expressionTree} with default
   * annotations applied. As types relevant to Called Methods checking are rarely used inside
   * generics, this is typically the best choice for type inference.
   */
  @Override
  public @Nullable AnnotatedTypeMirror getDummyAssignedTo(ExpressionTree expressionTree) {
    TypeMirror type = TreeUtils.typeOf(expressionTree);
    if (type.getKind() != TypeKind.VOID) {
      AnnotatedTypeMirror atm = type(expressionTree);
      addDefaultAnnotations(atm);
      return atm;
    }
    return null;
  }

  /**
   * Fetch the supported builder frameworks that are enabled.
   *
   * @return a collection of builder frameworks that are enabled in this run of the checker
   */
  /* package-private */ Collection<BuilderFrameworkSupport> getBuilderFrameworkSupports() {
    return builderFrameworkSupports;
  }
}
