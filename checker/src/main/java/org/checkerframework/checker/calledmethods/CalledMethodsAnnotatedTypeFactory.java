package org.checkerframework.checker.calledmethods;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsVarArgs;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer;
import org.checkerframework.checker.resourceleak.ResourceLeakAnnotatedTypeFactory;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.accumulation.AccumulationStore;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.Analysis.BeforeOrAfter;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.node.Node;
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
   * Set of potentially mcoe-obligation-fulfilling loops, defined through a pair of the entry cfg
   * block and the ExpressionTree of the element of the collection iterated over. Set in the
   * MustCallVisitor, which checks the header and (parts of the) body.
   */
  private static final Set<PotentiallyFulfillingLoop> potentiallyFulfillingLoops = new HashSet<>();

  /** Used as argument to call the post-analyzer of the {@code CalledMethods} checker with. */
  private static ResourceLeakAnnotatedTypeFactory rlAtf = null;

  /**
   * The builder frameworks (such as Lombok and AutoValue) supported by this instance of the Called
   * Methods Checker.
   */
  private final Collection<BuilderFrameworkSupport> builderFrameworkSupports;

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

  /** The {@link CalledMethods#value} element/argument. */
  /*package-private*/ final ExecutableElement calledMethodsValueElement =
      TreeUtils.getMethod(CalledMethods.class, "value", 0, processingEnv);

  /** The {@link EnsuresCalledMethodsVarArgs#value} element/argument. */
  /*package-private*/ final ExecutableElement ensuresCalledMethodsVarArgsValueElement =
      TreeUtils.getMethod(EnsuresCalledMethodsVarArgs.class, "value", 0, processingEnv);

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
    if (this instanceof ResourceLeakAnnotatedTypeFactory) {
      if (rlAtf == null) rlAtf = (ResourceLeakAnnotatedTypeFactory) this;
    }
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
    // receiver at the beginning of that fluent sequence is a call to the Filter() constructor,
    // then use the first argument to the Filter constructor, which is the name of the filter.
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
   * Construct a {@code PotentiallyFulfillingLoop} and add it to the static set of such loop to have
   * their loop body analyzed for possibly fulfilling {@code @MustCallOnElements} calling
   * obligations.
   *
   * @param loopConditionBlock cfg {@code Block} for loop condition
   * @param loopUpdateBlock {@code Block} for loop update
   * @param condition AST {@code Tree} for loop condition
   * @param collectionElementTree AST {@code Tree} for collection element iterated over
   * @param collectionEltNode cfg {@code Node} for collection element iterated over
   * @param collectionTree AST {@code Tree} for collection iterated over
   */
  public static void addPotentiallyFulfillingLoop(
      Block loopConditionBlock,
      Block loopUpdateBlock,
      Tree condition,
      ExpressionTree collectionElementTree,
      Node collectionEltNode,
      ExpressionTree collectionTree) {
    potentiallyFulfillingLoops.add(
        new PotentiallyFulfillingLoop(
            collectionTree,
            collectionElementTree,
            condition,
            loopConditionBlock,
            loopUpdateBlock,
            collectionEltNode));
  }

  /**
   * Return the static set of {@code PotentiallyFulfillingLoop}s scheduled for analysis.
   *
   * @return the static set of {@code PotentiallyFulfillingLoop}s scheduled for analysis.
   */
  public static Set<PotentiallyFulfillingLoop> getPotentiallyFulfillingLoops() {
    return potentiallyFulfillingLoops;
  }

  /**
   * Wrapper class for a loop that might have an effect on the {@code @MustCallOnElements} type of a
   * collection/array.
   */
  public abstract static class McoeObligationAlteringLoop {
    /** Loop is either assigning or fulfilling. */
    public static enum LoopKind {
      /**
       * Loop potentially assigns elements with non-empty {@code @MustCall} type to a collection.
       */
      ASSIGNING,

      /** Loop potentially calls methods on all elements of a collection. */
      FULFILLING
    }

    /** AST {@code Tree} for collection iterated over. */
    public final ExpressionTree collectionTree;

    /** AST {@code Tree} for collection element iterated over. */
    public final ExpressionTree collectionElementTree;

    /** AST {@code Tree} for loop condition. */
    public final Tree condition;

    /**
     * methods associated with this loop. For assigning loops, these are methods that are to be
     * added to the {@code MustCallOnElements} type and for fulfilling loops, methods that are to be
     * removed from the {@code MustCallOnElements} and added to the {@code CalledMethodsOnElements}
     * type.
     */
    protected final Set<String> associatedMethods;

    /**
     * Wether loop is assigning (elements with {@code MustCall} obligations to a collection) or
     * fulfilling.
     */
    public final LoopKind loopKind;

    /**
     * Constructs a new {@code McoeObligationAlteringLoop}. Called by subclass constructor.
     *
     * @param collectionTree AST {@code Tree} for collection iterated over
     * @param collectionElementTree AST {@code Tree} for collection element iterated over
     * @param condition AST {@code Tree} for loop condition
     * @param associatedMethods set of methods associated with this loop
     * @param loopKind whether this is an assigning/fulfilling loop
     */
    protected McoeObligationAlteringLoop(
        ExpressionTree collectionTree,
        ExpressionTree collectionElementTree,
        Tree condition,
        Set<String> associatedMethods,
        LoopKind loopKind) {
      this.collectionTree = collectionTree;
      this.collectionElementTree = collectionElementTree;
      this.condition = condition;
      this.loopKind = loopKind;
      if (associatedMethods == null) {
        associatedMethods = new HashSet<>();
      }
      this.associatedMethods = associatedMethods;
    }

    /**
     * Add methods associated with this loop. For assigning loops, these are methods that are to be
     * added to the {@code MustCallOnElements} type and for fulfilling loops, methods that are to be
     * removed from the {@code MustCallOnElements} and added to the {@code CalledMethodsOnElements}
     * type.
     *
     * @param methods the set of methods to add
     */
    public void addMethods(Set<String> methods) {
      associatedMethods.addAll(methods);
    }

    /**
     * Return methods associated with this loop. For assigning loops, these are methods that are to
     * be added to the {@code MustCallOnElements} type and for fulfilling loops, methods that are to
     * be removed from the {@code MustCallOnElements} and added to the {@code
     * CalledMethodsOnElements} type.
     *
     * @return the set of associated methdos
     */
    public Set<String> getMethods() {
      return associatedMethods;
    }
  }

  /**
   * Wrapper for a loop that potentially assigns elements with non-empty {@code MustCall}
   * obligations to an array, thus creating {@code MustCallOnElements} obligations for the array.
   */
  public static class PotentiallyAssigningLoop extends McoeObligationAlteringLoop {
    /** The AST tree for the assignment of the resource into the array in the loop. */
    public final AssignmentTree assignment;

    /**
     * Constructs a new {@code PotentiallyAssigningLoop}
     *
     * @param collectionTree AST {@code Tree} for collection iterated over
     * @param collectionElementTree AST {@code Tree} for collection element iterated over
     * @param condition AST {@code Tree} for loop condition
     * @param assignment AST tree for the assignment of the resource into the array in the loop
     * @param methodsToCall set of methods that are to be added to the {@code MustCallOnElements}
     *     type of the array iterated over.
     */
    public PotentiallyAssigningLoop(
        ExpressionTree collectionTree,
        ExpressionTree collectionElementTree,
        Tree condition,
        AssignmentTree assignment,
        Set<String> methodsToCall) {
      super(
          collectionTree,
          collectionElementTree,
          condition,
          Set.copyOf(methodsToCall),
          McoeObligationAlteringLoop.LoopKind.ASSIGNING);
      this.assignment = assignment;
    }
  }

  /** Wrapper for a loop that potentially calls methods on all elements of a collection/array. */
  public static class PotentiallyFulfillingLoop extends McoeObligationAlteringLoop {
    /** cfg {@code Block} for the loop condition */
    public final Block loopConditionBlock;

    /** cfg {@code Block} for the loop update */
    public final Block loopUpdateBlock;

    /** cfg {@code Node} for the collection element iterated over */
    public final Node collectionElementNode;

    /**
     * Constructs a new {@code PotentiallyFulfillingLoop}
     *
     * @param collectionTree AST {@link Tree} for collection iterated over
     * @param collectionElementTree AST {@link Tree} for collection element iterated over
     * @param condition AST {@link Tree} for loop condition
     * @param loopConditionBlock cfg {@link Block} for the loop condition
     * @param loopUpdateBlock cfg {@link Block} for the loop update
     * @param collectionEltNode cfg {@link Node} for the collection element iterated over
     */
    public PotentiallyFulfillingLoop(
        ExpressionTree collectionTree,
        ExpressionTree collectionElementTree,
        Tree condition,
        Block loopConditionBlock,
        Block loopUpdateBlock,
        Node collectionEltNode) {
      super(
          collectionTree,
          collectionElementTree,
          condition,
          new HashSet<>(),
          McoeObligationAlteringLoop.LoopKind.FULFILLING);
      this.loopConditionBlock = loopConditionBlock;
      this.loopUpdateBlock = loopUpdateBlock;
      this.collectionElementNode = collectionEltNode;
    }
  }

  /**
   * After running the called-methods analysis, call the consistency analyzer to analyze the loop
   * bodys of 'potentially-mcoe-obligation-fulfilling-loops', as determined by a pre-pattern-match
   * in the MustCallVisitor.
   *
   * <p>The analysis uses the CalledMethods type of the collection element iterated over to
   * determine the methods the loop calls on the collection elements.
   *
   * @param cfg the cfg of the enclosing method
   */
  public static void postAnalyzeStatically(ControlFlowGraph cfg) {
    System.out.println("c1: " + (rlAtf != null));
    System.out.println("c2: " + potentiallyFulfillingLoops.size());
    if (rlAtf != null && potentiallyFulfillingLoops.size() > 0) {
      MustCallConsistencyAnalyzer mustCallConsistencyAnalyzer =
          new MustCallConsistencyAnalyzer(rlAtf, (CalledMethodsAnalysis) rlAtf.analysis);

      // analyze loop bodies of all loops marked 'potentially-mcoe-obligation-fulfilling'
      Set<PotentiallyFulfillingLoop> analyzed = new HashSet<>();
      for (PotentiallyFulfillingLoop potentiallyFulfillingLoop : potentiallyFulfillingLoops) {
        ExpressionTree collectionElementTree = potentiallyFulfillingLoop.collectionElementTree;
        boolean loopContainedInThisMethod =
            cfg.getNodesCorrespondingToTree(collectionElementTree) != null;
        if (loopContainedInThisMethod) {
          System.out.println("analyzing loop " + potentiallyFulfillingLoop.collectionTree);
          mustCallConsistencyAnalyzer.analyzeObligationFulfillingLoop(
              cfg, potentiallyFulfillingLoop);
          analyzed.add(potentiallyFulfillingLoop);
        }
      }
      potentiallyFulfillingLoops.removeAll(analyzed);

      // Inferring owning annotations for @Owning fields/parameters, @EnsuresCalledMethods for
      // finalizer methods and @InheritableMustCall annotations for the class declarations.
      // if (getWholeProgramInference() != null) {
      //   if (cfg.getUnderlyingAST().getKind() == UnderlyingAST.Kind.METHOD) {
      //     MustCallInference.runMustCallInference(this, cfg, mustCallConsistencyAnalyzer);
      //   }
      // }
    }

    // super.postAnalyze(cfg);
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
   * Get the called methods specified by the given {@link CalledMethods} annotation.
   *
   * @param calledMethodsAnnotation the annotation
   * @return the called methods
   */
  public List<String> getCalledMethods(AnnotationMirror calledMethodsAnnotation) {
    if (this instanceof ResourceLeakAnnotatedTypeFactory) {
      rlAtf = (ResourceLeakAnnotatedTypeFactory) this;
    }
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
   * Creates a @CalledMethods annotation whose values are the given strings.
   *
   * @param val the methods that have been called
   * @return an annotation indicating that the given methods have been called
   */
  public AnnotationMirror createCalledMethods(String... val) {
    return createAccumulatorAnnotation(Arrays.asList(val));
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
   * Get the exceptional postconditions for the given method from the {@link
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
   * EnsuresCalledMethodsOnException.List} annotation and stores the results in <code>out</code>.
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
   * EnsuresCalledMethodsOnException} annotation and stores the results in <code>out</code>.
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

  /**
   * Fetches the store from the results of dataflow for {@code block}. The store after {@code block}
   * is returned.
   *
   * @param block a block
   * @return the appropriate CFStore, populated with CalledMethods annotations, from the results of
   *     running dataflow
   */
  public AccumulationStore getStoreAfterBlock(Block block) {
    return flowResult.getStoreAfter(block);
  }
}
