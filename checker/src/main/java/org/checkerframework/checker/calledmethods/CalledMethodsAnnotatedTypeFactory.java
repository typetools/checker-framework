package org.checkerframework.checker.calledmethods;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.builder.qual.ReturnsReceiver;
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
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.UserError;

/** The annotated type factory for the Called Methods checker. */
public class CalledMethodsAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

    /**
     * The {@link java.util.Collections#singletonList} method. It is treated specially by {@link
     * #adjustMethodNameUsingValueChecker(String, MethodInvocationTree)}.
     */
    private final ExecutableElement collectionsSingletonList;

    /**
     * Whether to use the Value Checker as a subchecker to reduce false positives when analyzing
     * calls to the AWS SDK. Defaults to false. Controlled by the command-line option {@code
     * -AuseValueChecker}.
     */
    private final boolean useValueChecker;

    /**
     * The builder frameworks (such as Lombok and AutoValue) supported by the Called Methods
     * checker.
     */
    private Collection<BuilderFrameworkSupport> builderFrameworkSupports;

    /**
     * Lombok has a flag to generate @CalledMethods annotations, but they used the old package name,
     * so we maintain it as an alias.
     */
    private static final String OLD_CALLED_METHODS =
            "org.checkerframework.checker.builder.qual.CalledMethods";

    /**
     * Lombok also generates an @NotCalledMethods annotation, which we have no support for. We
     * therefore treat it as top.
     */
    private static final String OLD_NOT_CALLED_METHODS =
            "org.checkerframework.checker.builder.qual.NotCalledMethods";

    /**
     * Create a new CalledMethodsAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    public CalledMethodsAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(
                checker,
                CalledMethods.class,
                CalledMethodsBottom.class,
                CalledMethodsPredicate.class);
        Set<String> disabledFrameworks = new HashSet<>();
        if (checker.hasOption(CalledMethodsChecker.DISABLE_BUILDER_FRAMEWORK_SUPPORTS)) {
            disabledFrameworks.addAll(
                    Arrays.asList(
                                    checker.getOption(
                                                    CalledMethodsChecker
                                                            .DISABLE_BUILDER_FRAMEWORK_SUPPORTS)
                                            .split(","))
                            .stream()
                            .map(String::toUpperCase)
                            .collect(Collectors.toList()));
        }
        builderFrameworkSupports = new ArrayList<>();
        enableFramework(CalledMethodsChecker.LOMBOK_SUPPORT, disabledFrameworks);
        enableFramework(CalledMethodsChecker.AUTOVALUE_SUPPORT, disabledFrameworks);

        if (!disabledFrameworks.isEmpty()) {
            StringJoiner sj = new StringJoiner(", ");
            disabledFrameworks.iterator().forEachRemaining(s -> sj.add(s));
            String unrecognized = sj.toString();
            throw new UserError(
                    "The following argument(s) to the "
                            + CalledMethodsChecker.DISABLE_BUILDER_FRAMEWORK_SUPPORTS
                            + " command-line argument to the Called Methods Checker were unrecognized: "
                            + unrecognized);
        }

        this.useValueChecker = checker.hasOption(CalledMethodsChecker.USE_VALUE_CHECKER);
        this.collectionsSingletonList =
                TreeUtils.getMethod(
                        "java.util.Collections", "singletonList", 1, getProcessingEnv());
        addAliasedAnnotation(OLD_CALLED_METHODS, CalledMethods.class, true);
        addAliasedAnnotation(OLD_NOT_CALLED_METHODS, this.top);
        this.postInit();
    }

    /**
     * Enables support for the given builder-generation framework, unless it is listed in the
     * disabled builder frameworks parsed from the -AdisableBuilderFrameworkSupport option's
     * arguments.
     *
     * @param framework the builder framework to enable
     * @param disabledFrameworks the set of disabled builder frameworks. This argument will be
     *     side-effected to remove a builder framework if it was actually disabled.
     */
    private void enableFramework(String framework, Set<String> disabledFrameworks) {
        if (disabledFrameworks == null || !disabledFrameworks.contains(framework)) {
            switch (framework) {
                case CalledMethodsChecker.AUTOVALUE_SUPPORT:
                    builderFrameworkSupports.add(new AutoValueSupport(this));
                    return;
                case CalledMethodsChecker.LOMBOK_SUPPORT:
                    builderFrameworkSupports.add(new LombokSupport(this));
                    return;
                default:
                    throw new BugInCF(
                            "Called Methods Checker tried to enable an unsupported builder framework: "
                                    + framework);
            }
        } else {
            disabledFrameworks.remove(framework);
        }
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(), new CalledMethodsTreeAnnotator(this));
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(
                super.createTypeAnnotator(), new CalledMethodsTypeAnnotator(this));
    }

    @Override
    public boolean returnsThis(MethodInvocationTree tree) {
        return super.returnsThis(tree) || hasOldReturnsReceiverAnnotation(tree);
    }

    /**
     * Continue to trust but not check the old {@link
     * org.checkerframework.checker.builder.qual.ReturnsReceiver} annotation, for backwards
     * compatibility.
     *
     * @param tree the method invocation whose invoked method is to be checked
     * @return true if the declaration of the invoked method has an obsolete ReturnsReceiver
     *     declaration annotation
     */
    private boolean hasOldReturnsReceiverAnnotation(MethodInvocationTree tree) {
        return this.getDeclAnnotation(TreeUtils.elementFromUse(tree), ReturnsReceiver.class)
                != null;
    }

    /**
     * Given a tree, returns the name of the method that the tree should be considered as calling.
     * Returns "withOwners" if the call sets an "owner", "owner-alias", or "owner-id" filter.
     * Returns "withImageIds" if the call sets an "image-ids" filter.
     *
     * <p>Package-private to permit calls from {@link CalledMethodsTransfer}.
     *
     * @param methodName the name of the method to adjust
     * @param tree the invocation of the method
     * @return either the first argument, or "withOwners" or "withImageIds" if the tree is an
     *     equivalent filter addition.
     */
    String adjustMethodNameUsingValueChecker(
            final String methodName, final MethodInvocationTree tree) {
        if (!useValueChecker) {
            return methodName;
        }

        ExecutableElement invokedMethod = TreeUtils.elementFromUse(tree);
        if (!"com.amazonaws.services.ec2.model.DescribeImagesRequest"
                .equals(ElementUtils.enclosingClass(invokedMethod).getQualifiedName().toString())) {
            return methodName;
        }

        if ("withFilters".equals(methodName) || "setFilters".equals(methodName)) {
            ValueAnnotatedTypeFactory valueATF = getTypeFactoryOfSubchecker(ValueChecker.class);
            for (Tree filterTree : tree.getArguments()) {
                // Search the arguments to withFilters for either: (1) a constructor invocation of
                // the Filter constructor, whose first argument is the name, or (2) a call to the
                // withName method.
                //
                // This code is searching for code such as:
                // new Filter("owner").withValues("...")
                // or:
                // new Filter().*.withName("owner").*
                //
                // It is attempting to recover either the argument to the constructor or the
                // argument to the last invocation of withName ("owner" in both of the above
                // examples).
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
     * @param filterTree the tree that represents the filter (an argument to the withFilters or
     *     setFilters method)
     * @param valueATF the type factory from the Value Checker
     * @return the adjusted method name, or null if the method name should not be adjusted
     */
    private @Nullable String filterTreeToMethodName(
            Tree filterTree, ValueAnnotatedTypeFactory valueATF) {
        while (filterTree != null && filterTree.getKind() == Tree.Kind.METHOD_INVOCATION) {

            MethodInvocationTree filterTreeAsMethodInvocation = (MethodInvocationTree) filterTree;
            String filterMethodName = TreeUtils.methodName(filterTreeAsMethodInvocation).toString();
            if ("withName".equals(filterMethodName)
                    && filterTreeAsMethodInvocation.getArguments().size() >= 1) {
                Tree withNameArgTree = filterTreeAsMethodInvocation.getArguments().get(0);
                String withNameArg =
                        ValueCheckerUtils.getExactStringValue(withNameArgTree, valueATF);
                return filterKindToMethodName(withNameArg);
            }

            // Descend into a call to Collections.singletonList()
            if (TreeUtils.isMethodInvocation(
                    filterTree, collectionsSingletonList, getProcessingEnv())) {
                filterTree = filterTreeAsMethodInvocation.getArguments().get(0);
            } else {
                filterTree =
                        TreeUtils.getReceiverTree(filterTreeAsMethodInvocation.getMethodSelect());
            }
        }
        // The loop has reached the beginning of a fluent sequence of method calls.
        // If the ultimate receiver at the beginning of that fluent sequence is a
        // call to the Filter() constructor, then use the first argument to the Filter
        // constructor, which is the name of the filter.
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
     * @return withOwners if filterKind is "owner", "owner-alias", or "owner-id"; "withImageIds" if
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
     * At a fluent method call (which returns {@code this}), add the method to the type of the
     * return value.
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
                        qualHierarchy.greatestLowerBound(
                                oldAnno, createAccumulatorAnnotation(methodName));
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
                builderFrameworkSupport.handlePossibleToBuilder(t);
            }

            Element nextEnclosingElement = enclosingElement.getEnclosingElement();
            if (nextEnclosingElement.getKind().isClass()) {
                for (BuilderFrameworkSupport builderFrameworkSupport : builderFrameworkSupports) {
                    builderFrameworkSupport.handlePossibleBuilderBuildMethod(t);
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
