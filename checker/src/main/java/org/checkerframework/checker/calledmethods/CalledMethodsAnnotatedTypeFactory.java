package org.checkerframework.checker.calledmethods;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.builder.qual.ReturnsReceiver;
import org.checkerframework.checker.calledmethods.framework.AutoValueSupport;
import org.checkerframework.checker.calledmethods.framework.FrameworkSupport;
import org.checkerframework.checker.calledmethods.framework.FrameworkSupportUtils;
import org.checkerframework.checker.calledmethods.framework.LombokSupport;
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

/** The annotated type factory for the Called Methods checker. */
public class CalledMethodsAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

    /**
     * The {@link java.util.Collections#singletonList} method. It is treated specially by the EC2
     * logic.
     */
    private final ExecutableElement collectionsSingletonList;

    /**
     * Whether to use the Value Checker as a subchecker to reduce false positives when analyzing
     * calls to the AWS SDK. Defaults to false. Controlled by the command-line option {@code
     * -AuseValueChecker}.
     */
    private final boolean useValueChecker;

    /** The frameworks (such as Lombok and AutoValue) supported by the Called Methods checker. */
    private Collection<FrameworkSupport> frameworkSupports;

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
     * Create a new accumulation checker's annotated type factory.
     *
     * @param checker the checker
     */
    public CalledMethodsAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(
                checker,
                CalledMethods.class,
                CalledMethodsBottom.class,
                CalledMethodsPredicate.class);
        EnumSet<FrameworkSupportUtils.Framework> frameworkSet =
                FrameworkSupportUtils.getFrameworkSet(
                        checker.getOption(CalledMethodsChecker.DISABLED_FRAMEWORK_SUPPORTS));
        frameworkSupports = new ArrayList<>();

        for (FrameworkSupportUtils.Framework framework : frameworkSet) {
            switch (framework) {
                case AUTO_VALUE:
                    frameworkSupports.add(new AutoValueSupport(this));
                    break;
                case LOMBOK:
                    frameworkSupports.add(new LombokSupport(this));
                    break;
            }
        }

        this.useValueChecker = checker.hasOption(CalledMethodsChecker.USE_VALUE_CHECKER);
        this.collectionsSingletonList =
                TreeUtils.getMethod(
                        "java.util.Collections", "singletonList", 1, getProcessingEnv());
        addAliasedAnnotation(OLD_CALLED_METHODS, CalledMethods.class, true);
        addAliasedAnnotation(OLD_NOT_CALLED_METHODS, this.top);
        this.postInit();
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
     * @return true if the declaration of the invoked method has a ReturnsReceiver declaration
     *     annotation
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
            for (Tree filterTree : tree.getArguments()) {
                // Search the arguments to withFilters for a Filter constructor invocation,
                // passing through as many method invocation trees as needed. This code is searching
                // for code of the form:
                // new Filter("owner").withValues("...")
                // or code of the form:
                // new Filter().*.withName("owner").*

                // The argument to withName, or null if no all to withName was observed.
                String withNameArg = null;
                ValueAnnotatedTypeFactory valueATF = getTypeFactoryOfSubchecker(ValueChecker.class);

                while (filterTree != null && filterTree.getKind() == Tree.Kind.METHOD_INVOCATION) {

                    MethodInvocationTree filterTreeAsMethodInvocation =
                            (MethodInvocationTree) filterTree;
                    String filterMethodName =
                            TreeUtils.methodName(filterTreeAsMethodInvocation).toString();
                    if ("withName".equals(filterMethodName)
                            && filterTreeAsMethodInvocation.getArguments().size() >= 1) {
                        Tree withNameArgTree = filterTreeAsMethodInvocation.getArguments().get(0);
                        withNameArg =
                                ValueCheckerUtils.getExactStringValue(withNameArgTree, valueATF);
                    }

                    // Descend into a call to Collections.singletonList()
                    if (TreeUtils.isMethodInvocation(
                            filterTree, collectionsSingletonList, getProcessingEnv())) {
                        filterTree = filterTreeAsMethodInvocation.getArguments().get(0);
                    } else {
                        filterTree =
                                TreeUtils.getReceiverTree(
                                        filterTreeAsMethodInvocation.getMethodSelect());
                    }
                }
                if (filterTree == null) {
                    continue;
                }
                if (filterTree.getKind() == Tree.Kind.NEW_CLASS) {

                    String value;
                    if (withNameArg != null) {
                        value = withNameArg;
                    } else {
                        ExpressionTree constructorArg =
                                ((NewClassTree) filterTree).getArguments().get(0);
                        value = ValueCheckerUtils.getExactStringValue(constructorArg, valueATF);
                    }

                    if (value != null) {
                        switch (value) {
                            case "owner":
                            case "owner-alias":
                            case "owner-id":
                                return "withOwners";
                            case "image-id":
                                return "withImageIds";
                            default:
                        }
                    }
                }
            }
        }
        return methodName;
    }

    /** Necessary for the type rule for called methods described below. */
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
            // CalledMethods requires special treatment of the return values of methods that return
            // their receiver: the default return type must include the method being invoked.
            //
            // The basic accumulation analysis cannot handle this case - it can use the RR checker
            // to transfer an annotation from the receiver to the return type, but because
            // accumulation
            // (has to) happen in dataflow, the correct annotation may not yet be available. The
            // basic
            // accumulation analysis therefore only supports "pass-through" returns receiver
            // methods;
            // it does not support automatically accumulating at the same time.
            if (returnsThis(tree)) {
                String methodName = TreeUtils.getMethodName(tree.getMethodSelect());
                methodName = adjustMethodNameUsingValueChecker(methodName, tree);
                AnnotationMirror oldAnno = type.getAnnotationInHierarchy(top);
                AnnotationMirror newAnno =
                        qualHierarchy.greatestLowerBound(
                                oldAnno, createAccumulatorAnnotation(methodName));
                type.replaceAnnotation(newAnno);
            }
            return super.visitMethodInvocation(tree, type);
        }

        @Override
        public Void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
            for (FrameworkSupport frameworkSupport : frameworkSupports) {
                frameworkSupport.handleConstructor(tree, type);
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

            for (FrameworkSupport frameworkSupport : frameworkSupports) {
                frameworkSupport.handlePossibleToBuilder(t);
            }

            Element nextEnclosingElement = enclosingElement.getEnclosingElement();
            if (nextEnclosingElement.getKind().isClass()) {
                for (FrameworkSupport frameworkSupport : frameworkSupports) {
                    frameworkSupport.handlePossibleBuilderBuildMethod(t);
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
     * Fetch the supported frameworks that are enabled.
     *
     * @return a collection of frameworks that are enabled in this run of the checker
     */
    /* package-private */ Collection<FrameworkSupport> getFrameworkSupports() {
        return frameworkSupports;
    }
}
