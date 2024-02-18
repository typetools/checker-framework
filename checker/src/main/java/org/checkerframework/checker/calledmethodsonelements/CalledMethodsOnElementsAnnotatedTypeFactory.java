package org.checkerframework.checker.calledmethodsonelements;

import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.calledmethodsonelements.qual.CalledMethodsOnElements;
import org.checkerframework.checker.calledmethodsonelements.qual.CalledMethodsOnElementsBottom;
import org.checkerframework.checker.calledmethodsonelements.qual.CalledMethodsOnElementsPredicate;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.TreeUtils;

/** The annotated type factory for the Called Methods Checker. */
public class CalledMethodsOnElementsAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

  /** The {@link CalledMethodsOnElements#value} element/argument. */
  /*package-private*/ final ExecutableElement calledMethodsOnElementsValueElement =
      TreeUtils.getMethod(CalledMethodsOnElements.class, "value", 0, processingEnv);

  /**
   * Create a new CalledMethodsOnElementsAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  public CalledMethodsOnElementsAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(
        checker,
        CalledMethodsOnElements.class,
        CalledMethodsOnElementsBottom.class,
        CalledMethodsOnElementsPredicate.class);
  }

  // @Override
  // protected TreeAnnotator createTreeAnnotator() {
  //   return new ListTreeAnnotator(super.createTreeAnnotator(), new
  // CalledMethodsTreeAnnotator(this));
  // }

  // @Override
  // protected TypeAnnotator createTypeAnnotator() {
  //   return new ListTypeAnnotator(super.createTypeAnnotator(), new
  // CalledMethodsTypeAnnotator(this));
  // }

  // @Override
  // public boolean returnsThis(MethodInvocationTree tree) {
  //   return super.returnsThis(tree)
  //       // Continue to trust but not check the old {@link
  //       // org.checkerframework.checker.builder.qual.ReturnsReceiver} annotation, for
  //       // backwards compatibility.
  //       || this.getDeclAnnotation(
  //               TreeUtils.elementFromUse(tree),
  //               org.checkerframework.checker.builder.qual.ReturnsReceiver.class)
  //           != null;
  // }

  // /**
  //  * Given a tree, returns the name of the method that the tree should be considered as calling.
  //  * Returns "withOwners" if the call sets an "owner", "owner-alias", or "owner-id" filter.
  // Returns
  //  * "withImageIds" if the call sets an "image-ids" filter.
  //  *
  //  * <p>Package-private to permit calls from {@link CalledMethodsTransfer}.
  //  *
  //  * @param methodName the name of the method being explicitly called
  //  * @param tree the invocation of the method
  //  * @return "withOwners" or "withImageIds" if the tree is an equivalent filter addition.
  // Otherwise,
  //  *     return the first argument.
  //  */
  // // This cannot return a Name because filterTreeToMethodName cannot.
  // public String adjustMethodNameUsingValueChecker(String methodName, MethodInvocationTree tree) {
  //   if (!useValueChecker) {
  //     return methodName;
  //   }

  //   ExecutableElement invokedMethod = TreeUtils.elementFromUse(tree);
  //   if (!ElementUtils.enclosingTypeElement(invokedMethod)
  //       .getQualifiedName()
  //       .contentEquals("com.amazonaws.services.ec2.model.DescribeImagesRequest")) {
  //     return methodName;
  //   }

  //   if (methodName.equals("withFilters") || methodName.equals("setFilters")) {
  //     ValueAnnotatedTypeFactory valueATF = getTypeFactoryOfSubchecker(ValueChecker.class);
  //     for (Tree filterTree : tree.getArguments()) {
  //       if (TreeUtils.isMethodInvocation(
  //           filterTree, collectionsSingletonList, getProcessingEnv())) {
  //         // Descend into a call to Collections.singletonList()
  //         filterTree = ((MethodInvocationTree) filterTree).getArguments().get(0);
  //       }
  //       String adjustedMethodName = filterTreeToMethodName(filterTree, valueATF);
  //       if (adjustedMethodName != null) {
  //         return adjustedMethodName;
  //       }
  //     }
  //   }
  //   return methodName;
  // }

  // /**
  //  * Determine the name of the method in DescribeImagesRequest that is equivalent to the Filter
  // in
  //  * the given tree.
  //  *
  //  * <p>Returns null unless the argument is one of the following:
  //  *
  //  * <ul>
  //  *   <li>a constructor invocation of the Filter constructor whose first argument is the name,
  // such
  //  *       as {@code new Filter("owner").*}, or
  //  *   <li>a call to the withName method, such as {@code new Filter().*.withName("owner").*}.
  //  * </ul>
  //  *
  //  * In those cases, it returns either the argument to the constructor or the argument to the
  // last
  //  * invocation of withName ("owner" in both of the above examples).
  //  *
  //  * @param filterTree the tree that represents the filter (an argument to the withFilters or
  //  *     setFilters method)
  //  * @param valueATF the type factory from the Value Checker
  //  * @return the adjusted method name, or null if the method name should not be adjusted
  //  */
  // // This cannot return a Name because filterKindToMethodName cannot.
  // private @Nullable String filterTreeToMethodName(
  //     Tree filterTree, ValueAnnotatedTypeFactory valueATF) {
  //   while (filterTree != null && filterTree.getKind() == Tree.Kind.METHOD_INVOCATION) {

  //     MethodInvocationTree filterTreeAsMethodInvocation = (MethodInvocationTree) filterTree;
  //     String filterMethodName = TreeUtils.methodName(filterTreeAsMethodInvocation).toString();
  //     if (filterMethodName.contentEquals("withName")
  //         && filterTreeAsMethodInvocation.getArguments().size() >= 1) {
  //       Tree withNameArgTree = filterTreeAsMethodInvocation.getArguments().get(0);
  //       String withNameArg = ValueCheckerUtils.getExactStringValue(withNameArgTree, valueATF);
  //       return filterKindToMethodName(withNameArg);
  //     }
  //     // Proceed leftward (toward the receiver) in a fluent call sequence.
  //     filterTree = TreeUtils.getReceiverTree(filterTreeAsMethodInvocation.getMethodSelect());
  //   }
  //   // The loop has reached the beginning of a fluent sequence of method calls.  If the ultimate
  //   // receiver at the beginning of that fluent sequence is a call to the Filter() constructor,
  //   // then use the first argument to the Filter constructor, which is the name of the filter.
  //   if (filterTree == null) {
  //     return null;
  //   }
  //   if (filterTree.getKind() == Tree.Kind.NEW_CLASS) {
  //     ExpressionTree constructorArg = ((NewClassTree) filterTree).getArguments().get(0);
  //     String filterKindName = ValueCheckerUtils.getExactStringValue(constructorArg, valueATF);
  //     if (filterKindName != null) {
  //       return filterKindToMethodName(filterKindName);
  //     }
  //   }
  //   return null;
  // }

  // /**
  //  * Converts from a kind of filter to the name of the corresponding method on a
  //  * DescribeImagesRequest object.
  //  *
  //  * @param filterKind the kind of filter
  //  * @return "withOwners" if filterKind is "owner", "owner-alias", or "owner-id"; "withImageIds"
  // if
  //  *     filterKind is "image-id"; null otherwise
  //  */
  // private static @Nullable String filterKindToMethodName(String filterKind) {
  //   switch (filterKind) {
  //     case "owner":
  //     case "owner-alias":
  //     case "owner-id":
  //       return "withOwners";
  //     case "image-id":
  //       return "withImageIds";
  //     default:
  //       return null;
  //   }
  // }

  // /**
  //  * At a fluent method call (which returns {@code this}), add the method to the type of the
  // return
  //  * value.
  //  */
  // private class CalledMethodsTreeAnnotator extends AccumulationTreeAnnotator {
  //   /**
  //    * Creates an instance of this tree annotator for the given type factory.
  //    *
  //    * @param factory the type factory
  //    */
  //   public CalledMethodsTreeAnnotator(AccumulationAnnotatedTypeFactory factory) {
  //     super(factory);
  //   }

  //   @Override
  //   public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
  //     // Accumulate a method call, by adding the method being invoked to the return type.
  //     if (returnsThis(tree)) {
  //       TypeMirror typeMirror = type.getUnderlyingType();
  //       String methodName = TreeUtils.getMethodName(tree.getMethodSelect());
  //       methodName = adjustMethodNameUsingValueChecker(methodName, tree);
  //       AnnotationMirror oldAnno = type.getPrimaryAnnotationInHierarchy(top);
  //       AnnotationMirror newAnno =
  //           qualHierarchy.greatestLowerBoundShallow(
  //               oldAnno, typeMirror, createAccumulatorAnnotation(methodName), typeMirror);
  //       type.replaceAnnotation(newAnno);
  //     }

  //     // Also do the standard accumulation analysis behavior: copy any accumulation
  //     // annotations from the receiver to the return type.
  //     return super.visitMethodInvocation(tree, type);
  //   }

  //   @Override
  //   public Void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
  //     for (BuilderFrameworkSupport builderFrameworkSupport : builderFrameworkSupports) {
  //       builderFrameworkSupport.handleConstructor(tree, type);
  //     }
  //     return super.visitNewClass(tree, type);
  //   }
  // }

  // /**
  //  * Adds @CalledMethod annotations for build() methods of AutoValue and Lombok Builders to
  // ensure
  //  * required properties have been set.
  //  */
  // private class CalledMethodsTypeAnnotator extends TypeAnnotator {

  //   /**
  //    * Constructor matching super.
  //    *
  //    * @param atypeFactory the type factory
  //    */
  //   public CalledMethodsTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
  //     super(atypeFactory);
  //   }

  //   @Override
  //   public Void visitExecutable(AnnotatedTypeMirror.AnnotatedExecutableType t, Void p) {
  //     ExecutableElement element = t.getElement();

  //     TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

  //     for (BuilderFrameworkSupport builderFrameworkSupport : builderFrameworkSupports) {
  //       if (builderFrameworkSupport.isToBuilderMethod(element)) {
  //         builderFrameworkSupport.handleToBuilderMethod(t);
  //       }
  //     }

  //     Element nextEnclosingElement = enclosingElement.getEnclosingElement();
  //     if (nextEnclosingElement.getKind().isClass()) {
  //       for (BuilderFrameworkSupport builderFrameworkSupport : builderFrameworkSupports) {
  //         if (builderFrameworkSupport.isBuilderBuildMethod(element)) {
  //           builderFrameworkSupport.handleBuilderBuildMethod(t);
  //         }
  //       }
  //     }

  //     return super.visitExecutable(t, p);
  //   }
  // }

  // @Override
  // protected CalledMethodsAnalysis createFlowAnalysis() {
  //   return new CalledMethodsAnalysis(checker, this);
  // }

  // /**
  //  * Returns the annotation type mirror for the type of {@code expressionTree} with default
  //  * annotations applied. As types relevant to Called Methods checking are rarely used inside
  //  * generics, this is typically the best choice for type inference.
  //  */
  // @Override
  // public @Nullable AnnotatedTypeMirror getDummyAssignedTo(ExpressionTree expressionTree) {
  //   TypeMirror type = TreeUtils.typeOf(expressionTree);
  //   if (type.getKind() != TypeKind.VOID) {
  //     AnnotatedTypeMirror atm = type(expressionTree);
  //     addDefaultAnnotations(atm);
  //     return atm;
  //   }
  //   return null;
  // }

  // /**
  //  * Fetch the supported builder frameworks that are enabled.
  //  *
  //  * @return a collection of builder frameworks that are enabled in this run of the checker
  //  */
  // /*package-private*/ Collection<BuilderFrameworkSupport> getBuilderFrameworkSupports() {
  //   return builderFrameworkSupports;
  // }

  // /**
  //  * Get the called methods specified by the given {@link CalledMethods} annotation.
  //  *
  //  * @param calledMethodsAnnotation the annotation
  //  * @return the called methods
  //  */
  // protected List<String> getCalledMethods(AnnotationMirror calledMethodsAnnotation) {
  //   return AnnotationUtils.getElementValueArray(
  //       calledMethodsAnnotation, calledMethodsValueElement, String.class,
  // Collections.emptyList());
  // }

  // @Override
  // protected @Nullable AnnotationMirror createRequiresOrEnsuresQualifier(
  //     String expression,
  //     AnnotationMirror qualifier,
  //     AnnotatedTypeMirror declaredType,
  //     Analysis.BeforeOrAfter preOrPost,
  //     @Nullable List<AnnotationMirror> preconds) {
  //   if (preOrPost == BeforeOrAfter.AFTER && isAccumulatorAnnotation(qualifier)) {
  //     List<String> calledMethods = getCalledMethods(qualifier);
  //     if (!calledMethods.isEmpty()) {
  //       return ensuresCMAnno(expression, calledMethods);
  //     }
  //   }
  //   return super.createRequiresOrEnsuresQualifier(
  //       expression, qualifier, declaredType, preOrPost, preconds);
  // }

  // /**
  //  * Returns a {@code @EnsuresCalledMethods("...")} annotation for the given expression.
  //  *
  //  * @param expression the expression to put in the value field of the EnsuresCalledMethods
  //  *     annotation
  //  * @param calledMethods the methods that were definitely called on the expression
  //  * @return a {@code @EnsuresCalledMethods("...")} annotation for the given expression
  //  */
  // private AnnotationMirror ensuresCMAnno(String expression, List<String> calledMethods) {
  //   return ensuresCMAnno(new String[] {expression}, calledMethods);
  // }

  // /**
  //  * Returns a {@code @EnsuresCalledMethods("...")} annotation for the given expressions.
  //  *
  //  * @param expressions the expressions to put in the value field of the EnsuresCalledMethods
  //  *     annotation
  //  * @param calledMethods the methods that were definitely called on the expression
  //  * @return a {@code @EnsuresCalledMethods("...")} annotation for the given expression
  //  */
  // private AnnotationMirror ensuresCMAnno(String[] expressions, List<String> calledMethods) {
  //   AnnotationBuilder builder = new AnnotationBuilder(processingEnv, EnsuresCalledMethods.class);
  //   builder.setValue("value", expressions);
  //   builder.setValue("methods", calledMethods.toArray(new String[calledMethods.size()]));
  //   AnnotationMirror am = builder.build();
  //   return am;
  // }

  // /**
  //  * Returns true if the checker should ignore exceptional control flow due to the given
  // exception
  //  * type.
  //  *
  //  * @param exceptionType exception type
  //  * @return {@code true} if {@code exceptionType} is a member of {@link
  //  *     CalledMethodsAnalysis#ignoredExceptionTypes}, {@code false} otherwise
  //  */
  // @Override
  // public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
  //   if (exceptionType.getKind() == TypeKind.DECLARED) {
  //     return CalledMethodsAnalysis.ignoredExceptionTypes.contains(
  //         TypesUtils.getQualifiedName((DeclaredType) exceptionType));
  //   }
  //   return false;
  // }

  // /**
  //  * Get the exceptional postconditions for the given method from the {@link
  //  * EnsuresCalledMethodsOnException} annotations on it.
  //  *
  //  * @param methodOrConstructor the method to examine
  //  * @return the exceptional postconditions on the given method; the return value is
  // newly-allocated
  //  *     and can be freely modified by callers
  //  */
  // public Set<EnsuresCalledMethodOnExceptionContract> getExceptionalPostconditions(
  //     ExecutableElement methodOrConstructor) {
  //   Set<EnsuresCalledMethodOnExceptionContract> result = new LinkedHashSet<>();

  //   parseEnsuresCalledMethodOnExceptionListAnnotation(
  //       getDeclAnnotation(methodOrConstructor, EnsuresCalledMethodsOnException.List.class),
  // result);

  //   parseEnsuresCalledMethodOnExceptionAnnotation(
  //       getDeclAnnotation(methodOrConstructor, EnsuresCalledMethodsOnException.class), result);

  //   return result;
  // }

  // /**
  //  * Helper for {@link #getExceptionalPostconditions(ExecutableElement)} that parses a {@link
  //  * EnsuresCalledMethodsOnException.List} annotation and stores the results in <code>out</code>.
  //  *
  //  * @param annotation the annotation
  //  * @param out the output collection
  //  */
  // private void parseEnsuresCalledMethodOnExceptionListAnnotation(
  //     @Nullable AnnotationMirror annotation, Set<EnsuresCalledMethodOnExceptionContract> out) {
  //   if (annotation == null) {
  //     return;
  //   }

  //   List<AnnotationMirror> annotations =
  //       AnnotationUtils.getElementValueArray(
  //           annotation,
  //           ensuresCalledMethodsOnExceptionListValueElement,
  //           AnnotationMirror.class,
  //           Collections.emptyList());

  //   for (AnnotationMirror a : annotations) {
  //     parseEnsuresCalledMethodOnExceptionAnnotation(a, out);
  //   }
  // }

  // /**
  //  * Helper for {@link #getExceptionalPostconditions(ExecutableElement)} that parses a {@link
  //  * EnsuresCalledMethodsOnException} annotation and stores the results in <code>out</code>.
  //  *
  //  * @param annotation the annotation
  //  * @param out the output collection
  //  */
  // private void parseEnsuresCalledMethodOnExceptionAnnotation(
  //     @Nullable AnnotationMirror annotation, Set<EnsuresCalledMethodOnExceptionContract> out) {
  //   if (annotation == null) {
  //     return;
  //   }

  //   List<String> expressions =
  //       AnnotationUtils.getElementValueArray(
  //           annotation,
  //           ensuresCalledMethodsOnExceptionValueElement,
  //           String.class,
  //           Collections.emptyList());
  //   List<String> methods =
  //       AnnotationUtils.getElementValueArray(
  //           annotation,
  //           ensuresCalledMethodsOnExceptionMethodsElement,
  //           String.class,
  //           Collections.emptyList());

  //   for (String expr : expressions) {
  //     for (String method : methods) {
  //       out.add(new EnsuresCalledMethodOnExceptionContract(expr, method));
  //     }
  //   }
  // }
}
