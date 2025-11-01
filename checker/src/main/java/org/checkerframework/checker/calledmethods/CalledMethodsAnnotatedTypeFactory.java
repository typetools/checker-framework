package org.checkerframework.checker.calledmethods;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.builder.AutoValueSupport;
import org.checkerframework.checker.calledmethods.builder.BuilderFrameworkSupport;
import org.checkerframework.checker.calledmethods.builder.LombokSupport;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsBottom;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsPredicate;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsOnException;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsVarargs;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.Analysis.BeforeOrAfter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.UserError;

/** The annotated type factory for the Called Methods Checker. */
public class CalledMethodsAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

  /**
   * The builder frameworks (such as Lombok and AutoValue) supported by this instance of the Called
   * Methods Checker.
   */
  private final Collection<BuilderFrameworkSupport> builderFrameworkSupports;

  /**
   * If true, use the Value Checker as a subchecker to reduce false positives when analyzing calls
   * to the AWS SDK. Defaults to false. Controlled by the command-line option {@code
   * -AuseValueChecker}.
   */
  private final boolean useValueChecker;

  /**
   * The {@link java.util.Collections#singletonList} method. It is treated specially by {@link
   * #adjustMethodNameUsingValueChecker}.
   */
  @SuppressWarnings("this-escape")
  private final ExecutableElement collectionsSingletonList =
      TreeUtils.getMethod("java.util.Collections", "singletonList", 1, getProcessingEnv());

  /** The {@link CalledMethods#value} element/argument. */
  /*package-private*/ final ExecutableElement calledMethodsValueElement =
      TreeUtils.getMethod(CalledMethods.class, "value", 0, processingEnv);

  /** The {@link EnsuresCalledMethodsVarargs#value} element/argument. */
  /*package-private*/ final ExecutableElement ensuresCalledMethodsVarargsValueElement =
      TreeUtils.getMethod(EnsuresCalledMethodsVarargs.class, "value", 0, processingEnv);

  /** The {@link EnsuresCalledMethodsOnException#value} element/argument. */
  /*package-private*/ final ExecutableElement ensuresCalledMethodsOnExceptionValueElement =
      TreeUtils.getMethod(EnsuresCalledMethodsOnException.class, "value", 0, processingEnv);

  /** The {@link EnsuresCalledMethodsOnException#methods} element/argument. */
  /*package-private*/ final ExecutableElement ensuresCalledMethodsOnExceptionMethodsElement =
      TreeUtils.getMethod(EnsuresCalledMethodsOnException.class, "methods", 0, processingEnv);

  /** The {@link EnsuresCalledMethodsOnException.List#value} element/argument. */
  /*package-private*/ final ExecutableElement ensuresCalledMethodsOnExceptionListValueElement =
      TreeUtils.getMethod(EnsuresCalledMethodsOnException.List.class, "value", 0, processingEnv);

  /**
   * Create a new CalledMethodsAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public CalledMethodsAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker, CalledMethods.class, CalledMethodsBottom.class, CalledMethodsPredicate.class);

    this.builderFrameworkSupports = new ArrayList<>(2);
    List<String> disabledFrameworks =
        checker.getStringsOption(CalledMethodsChecker.DISABLE_BUILDER_FRAMEWORK_SUPPORTS, ',');
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

    // Don't call postInit() for subclasses.
    if (this.getClass() == CalledMethodsAnnotatedTypeFactory.class) {
      this.postInit();
    }
  }

  /**
   * Enables support for the default builder-generation frameworks, except those listed in the
   * disabled builder frameworks parsed from the -AdisableBuilderFrameworkSupport option's
   * arguments. Throws a UserError if the user included an unsupported framework in the list of
   * frameworks to be disabled.
   *
   * @param disabledFrameworks the disabled builder frameworks
   */
  private void enableFrameworks(List<String> disabledFrameworks) {
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
  public String adjustMethodNameUsingValueChecker(String methodName, MethodInvocationTree tree) {
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
    while (filterTree != null && filterTree instanceof MethodInvocationTree) {

      MethodInvocationTree filterTreeAsMethodInvocation = (MethodInvocationTree) filterTree;
      String filterMethodName = TreeUtils.methodName(filterTreeAsMethodInvocation).toString();
      if (filterMethodName.contentEquals("withName")
          && !filterTreeAsMethodInvocation.getArguments().isEmpty()) {
        Tree withNameArgTree = filterTreeAsMethodInvocation.getArguments().get(0);
        String withNameArg = ValueCheckerUtils.getExactStringValue(withNameArgTree, valueATF);
        return filterKindToMethodName(withNameArg);
      }
      // Proceed leftward (toward the receiver) in a fluent call sequence.
      filterTree = TreeUtils.getReceiverTree(filterTreeAsMethodInvocation.getMethodSelect());
    }
    // The loop has reached the beginning of a fluent sequence of method calls.  If the ultimate
    // receiver at the beginning of that fluent sequence is a call to the Filter() constructor,
    // then use the first argument to the Filter constructor, which is the name of the filter.
    if (filterTree == null) {
      return null;
    }
    if (filterTree instanceof NewClassTree) {
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
  private class CalledMethodsTreeAnnotator extends TreeAnnotator {
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
        TypeMirror typeMirror = type.getUnderlyingType();
        String methodName = TreeUtils.getMethodName(tree.getMethodSelect());
        methodName = adjustMethodNameUsingValueChecker(methodName, tree);
        AnnotationMirror oldAnno = type.getPrimaryAnnotationInHierarchy(top);
        AnnotationMirror newAnno =
            qualHierarchy.greatestLowerBoundShallow(
                oldAnno, typeMirror, createAccumulatorAnnotation(methodName), typeMirror);
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
     * Creates a CalledMethodsTypeAnnotator.
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

  @Override
  protected CalledMethodsAnalysis createFlowAnalysis() {
    return new CalledMethodsAnalysis(checker, this);
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
  /*package-private*/ Collection<BuilderFrameworkSupport> getBuilderFrameworkSupports() {
    return builderFrameworkSupports;
  }

  /**
   * Returns the called methods specified by the given {@link CalledMethods} annotation.
   *
   * @param calledMethodsAnnotation the annotation
   * @return the called methods
   */
  public List<String> getCalledMethods(AnnotationMirror calledMethodsAnnotation) {
    return AnnotationUtils.getElementValueArray(
        calledMethodsAnnotation, calledMethodsValueElement, String.class, Collections.emptyList());
  }

  @Override
  protected @Nullable AnnotationMirror createRequiresOrEnsuresQualifier(
      String expression,
      AnnotationMirror qualifier,
      AnnotatedTypeMirror declaredType,
      Analysis.BeforeOrAfter preOrPost,
      @Nullable List<AnnotationMirror> preconds) {
    if (preOrPost == BeforeOrAfter.AFTER && isAccumulatorAnnotation(qualifier)) {
      List<String> calledMethods = getCalledMethods(qualifier);
      if (!calledMethods.isEmpty()) {
        return ensuresCMAnno(expression, calledMethods);
      }
    }
    return super.createRequiresOrEnsuresQualifier(
        expression, qualifier, declaredType, preOrPost, preconds);
  }

  /**
   * Returns a {@code @EnsuresCalledMethods("...")} annotation for the given expression.
   *
   * @param expression the expression to put in the value field of the EnsuresCalledMethods
   *     annotation
   * @param calledMethods the methods that were definitely called on the expression
   * @return a {@code @EnsuresCalledMethods("...")} annotation for the given expression
   */
  private AnnotationMirror ensuresCMAnno(String expression, List<String> calledMethods) {
    return ensuresCMAnno(new String[] {expression}, calledMethods);
  }

  /**
   * Returns a {@code @EnsuresCalledMethods("...")} annotation for the given expressions.
   *
   * @param expressions the expressions to put in the value field of the EnsuresCalledMethods
   *     annotation
   * @param calledMethods the methods that were definitely called on the expression
   * @return a {@code @EnsuresCalledMethods("...")} annotation for the given expression
   */
  private AnnotationMirror ensuresCMAnno(String[] expressions, List<String> calledMethods) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, EnsuresCalledMethods.class);
    builder.setValue("value", expressions);
    builder.setValue("methods", calledMethods.toArray(new String[0]));
    AnnotationMirror am = builder.build();
    return am;
  }

  /**
   * Returns true if the checker should ignore exceptional control flow due to the given exception
   * type.
   *
   * @param exceptionType exception type
   * @return {@code true} if {@code exceptionType} is a member of {@link
   *     CalledMethodsAnalysis#ignoredExceptionTypes}, {@code false} otherwise
   */
  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    if (exceptionType.getKind() == TypeKind.DECLARED) {
      return CalledMethodsAnalysis.ignoredExceptionTypes.contains(
          TypesUtils.getQualifiedName((DeclaredType) exceptionType));
    }
    return false;
  }

  /**
   * Returns the exceptional postconditions for the given method from the {@link
   * EnsuresCalledMethodsOnException} annotations on it.
   *
   * @param methodOrConstructor the method to examine
   * @return the exceptional postconditions on the given method; the return value is newly-allocated
   *     and can be freely modified by callers
   */
  public Set<EnsuresCalledMethodOnExceptionContract> getExceptionalPostconditions(
      ExecutableElement methodOrConstructor) {
    Set<EnsuresCalledMethodOnExceptionContract> result = new LinkedHashSet<>();

    parseEnsuresCalledMethodOnExceptionListAnnotation(
        getDeclAnnotation(methodOrConstructor, EnsuresCalledMethodsOnException.List.class), result);

    parseEnsuresCalledMethodOnExceptionAnnotation(
        getDeclAnnotation(methodOrConstructor, EnsuresCalledMethodsOnException.class), result);

    return result;
  }

  /**
   * Helper for {@link #getExceptionalPostconditions(ExecutableElement)} that parses a {@link
   * EnsuresCalledMethodsOnException.List} annotation and stores the results in {@code out}.
   *
   * @param annotation the annotation
   * @param out the output collection
   */
  private void parseEnsuresCalledMethodOnExceptionListAnnotation(
      @Nullable AnnotationMirror annotation, Set<EnsuresCalledMethodOnExceptionContract> out) {
    if (annotation == null) {
      return;
    }

    List<AnnotationMirror> annotations =
        AnnotationUtils.getElementValueArray(
            annotation,
            ensuresCalledMethodsOnExceptionListValueElement,
            AnnotationMirror.class,
            Collections.emptyList());

    for (AnnotationMirror a : annotations) {
      parseEnsuresCalledMethodOnExceptionAnnotation(a, out);
    }
  }

  /**
   * Helper for {@link #getExceptionalPostconditions(ExecutableElement)} that parses a {@link
   * EnsuresCalledMethodsOnException} annotation and stores the results in {@code out}.
   *
   * @param annotation the annotation
   * @param out the output collection
   */
  private void parseEnsuresCalledMethodOnExceptionAnnotation(
      @Nullable AnnotationMirror annotation, Set<EnsuresCalledMethodOnExceptionContract> out) {
    if (annotation == null) {
      return;
    }

    List<String> expressions =
        AnnotationUtils.getElementValueArray(
            annotation,
            ensuresCalledMethodsOnExceptionValueElement,
            String.class,
            Collections.emptyList());
    List<String> methods =
        AnnotationUtils.getElementValueArray(
            annotation,
            ensuresCalledMethodsOnExceptionMethodsElement,
            String.class,
            Collections.emptyList());

    for (String expr : expressions) {
      for (String method : methods) {
        out.add(new EnsuresCalledMethodOnExceptionContract(expr, method));
      }
    }
  }
}
