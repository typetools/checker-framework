package org.checkerframework.checker.nullness;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.initialization.InitializationAnnotatedTypeFactory;
import org.checkerframework.checker.initialization.qual.FBCBottom;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.DefaultForTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** The annotated type factory for the nullness type-system. */
public class NullnessAnnotatedTypeFactory
        extends InitializationAnnotatedTypeFactory<
                NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis> {

    /** The @{@link NonNull} annotation. */
    protected final AnnotationMirror NONNULL = AnnotationBuilder.fromClass(elements, NonNull.class);
    /** The @{@link Nullable} annotation. */
    protected final AnnotationMirror NULLABLE =
            AnnotationBuilder.fromClass(elements, Nullable.class);
    /** The @{@link PolyNull} annotation. */
    protected final AnnotationMirror POLYNULL =
            AnnotationBuilder.fromClass(elements, PolyNull.class);
    /** The @{@link MonotonicNonNull} annotation. */
    protected final AnnotationMirror MONOTONIC_NONNULL =
            AnnotationBuilder.fromClass(elements, MonotonicNonNull.class);

    /** Handles invocations of {@link java.lang.System#getProperty(String)}. */
    protected final SystemGetPropertyHandler systemGetPropertyHandler;

    /** Determines the nullness type of calls to {@link java.util.Collection#toArray()}. */
    protected final CollectionToArrayHeuristics collectionToArrayHeuristics;

    /** Cache for the nullness annotations. */
    protected final Set<Class<? extends Annotation>> nullnessAnnos;

    // List is in alphabetical order.  If you update it, also update
    // ../../../../../../../../docs/manual/nullness-checker.tex .
    /** Aliases for {@code @Nonnull}. */
    private static final List<String> NONNULL_ALIASES =
            Arrays.asList(
                    // https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/annotation/NonNull.java
                    "android.annotation.NonNull",
                    // https://android.googlesource.com/platform/frameworks/support/+/master/annotations/src/main/java/android/support/annotation/NonNull.java
                    "android.support.annotation.NonNull",
                    // https://android.googlesource.com/platform/frameworks/support/+/master/annotations/src/main/java/androidx/annotation/NonNull.java
                    "androidx.annotation.NonNull",
                    // https://android.googlesource.com/platform/tools/metalava/+/master/stub-annotations/src/main/java/androidx/annotation/RecentlyNonNull.java
                    "androidx.annotation.RecentlyNonNull",
                    "com.sun.istack.internal.NotNull",
                    // http://findbugs.sourceforge.net/api/edu/umd/cs/findbugs/annotations/NonNull.html
                    "edu.umd.cs.findbugs.annotations.NonNull",
                    // https://github.com/ReactiveX/RxJava/blob/2.x/src/main/java/io/reactivex/annotations/NonNull.java
                    "io.reactivex.annotations.NonNull",
                    // https://jcp.org/en/jsr/detail?id=305
                    "javax.annotation.Nonnull",
                    // https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/NotNull.html
                    "javax.validation.constraints.NotNull",
                    // https://github.com/rzwitserloot/lombok/blob/master/src/core/lombok/NonNull.java
                    "lombok.NonNull",
                    // https://search.maven.org/search?q=a:checker-compat-qual
                    "org.checkerframework.checker.nullness.compatqual.NonNullDecl",
                    "org.checkerframework.checker.nullness.compatqual.NonNullType",
                    // https://help.eclipse.org/neon/index.jsp?topic=/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/annotation/NonNull.html
                    "org.eclipse.jdt.annotation.NonNull",
                    // https://github.com/eclipse/jgit/blob/master/org.eclipse.jgit/src/org/eclipse/jgit/annotations/NonNull.java
                    "org.eclipse.jgit.annotations.NonNull",
                    // https://github.com/JetBrains/intellij-community/blob/master/platform/annotations/java8/src/org/jetbrains/annotations/NotNull.java
                    "org.jetbrains.annotations.NotNull",
                    // http://svn.code.sf.net/p/jmlspecs/code/JMLAnnotations/trunk/src/org/jmlspecs/annotation/NonNull.java
                    "org.jmlspecs.annotation.NonNull",
                    // http://bits.netbeans.org/8.2/javadoc/org-netbeans-api-annotations-common/org/netbeans/api/annotations/common/NonNull.html
                    "org.netbeans.api.annotations.common.NonNull",
                    // https://github.com/spring-projects/spring-framework/blob/master/spring-core/src/main/java/org/springframework/lang/NonNull.java
                    "org.springframework.lang.NonNull");

    // List is in alphabetical order.  If you update it, also update
    // ../../../../../../../../docs/manual/nullness-checker.tex .
    /** Aliases for {@code @Nullable}. */
    private static final List<String> NULLABLE_ALIASES =
            Arrays.asList(
                    // https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/annotation/Nullable.java
                    "android.annotation.Nullable",
                    // https://android.googlesource.com/platform/frameworks/support/+/master/annotations/src/main/java/android/support/annotation/Nullable.java
                    "android.support.annotation.Nullable",
                    // https://android.googlesource.com/platform/frameworks/support/+/master/annotations/src/main/java/androidx/annotation/Nullable.java
                    "androidx.annotation.Nullable",
                    // https://android.googlesource.com/platform/tools/metalava/+/master/stub-annotations/src/main/java/androidx/annotation/RecentlyNullable.java
                    "androidx.annotation.RecentlyNullable",
                    "com.sun.istack.internal.Nullable",
                    // http://findbugs.sourceforge.net/api/edu/umd/cs/findbugs/annotations/CheckForNull.html
                    "edu.umd.cs.findbugs.annotations.CheckForNull",
                    // http://findbugs.sourceforge.net/api/edu/umd/cs/findbugs/annotations/Nullable.html
                    "edu.umd.cs.findbugs.annotations.Nullable",
                    // http://findbugs.sourceforge.net/api/edu/umd/cs/findbugs/annotations/PossiblyNull.html
                    "edu.umd.cs.findbugs.annotations.PossiblyNull",
                    // http://findbugs.sourceforge.net/api/edu/umd/cs/findbugs/annotations/UnknownNullness.html
                    "edu.umd.cs.findbugs.annotations.UnknownNullness",
                    // https://github.com/ReactiveX/RxJava/blob/2.x/src/main/java/io/reactivex/annotations/Nullable.java
                    "io.reactivex.annotations.Nullable",
                    // https://jcp.org/en/jsr/detail?id=305
                    "javax.annotation.CheckForNull",
                    "javax.annotation.Nullable",
                    // https://search.maven.org/search?q=a:checker-compat-qual
                    "org.checkerframework.checker.nullness.compatqual.NullableDecl",
                    "org.checkerframework.checker.nullness.compatqual.NullableType",
                    // https://help.eclipse.org/neon/index.jsp?topic=/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/annotation/Nullable.html
                    "org.eclipse.jdt.annotation.Nullable",
                    // https://github.com/eclipse/jgit/blob/master/org.eclipse.jgit/src/org/eclipse/jgit/annotations/Nullable.java
                    "org.eclipse.jgit.annotations.Nullable",
                    // https://github.com/JetBrains/intellij-community/blob/master/platform/annotations/java8/src/org/jetbrains/annotations/Nullable.java
                    "org.jetbrains.annotations.Nullable",
                    // http://svn.code.sf.net/p/jmlspecs/code/JMLAnnotations/trunk/src/org/jmlspecs/annotation/Nullable.java
                    "org.jmlspecs.annotation.Nullable",
                    // http://bits.netbeans.org/8.2/javadoc/org-netbeans-api-annotations-common/org/netbeans/api/annotations/common/CheckForNull.html
                    "org.netbeans.api.annotations.common.CheckForNull",
                    // http://bits.netbeans.org/8.2/javadoc/org-netbeans-api-annotations-common/org/netbeans/api/annotations/common/NullAllowed.html
                    "org.netbeans.api.annotations.common.NullAllowed",
                    // http://bits.netbeans.org/8.2/javadoc/org-netbeans-api-annotations-common/org/netbeans/api/annotations/common/NullUnknown.html
                    "org.netbeans.api.annotations.common.NullUnknown",
                    // https://github.com/spring-projects/spring-framework/blob/master/spring-core/src/main/java/org/springframework/lang/Nullable.java
                    "org.springframework.lang.Nullable");

    /** Creates NullnessAnnotatedTypeFactory. */
    public NullnessAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        Set<Class<? extends Annotation>> tempNullnessAnnos = new LinkedHashSet<>();
        tempNullnessAnnos.add(NonNull.class);
        tempNullnessAnnos.add(MonotonicNonNull.class);
        tempNullnessAnnos.add(Nullable.class);
        tempNullnessAnnos.add(PolyNull.class);
        nullnessAnnos = Collections.unmodifiableSet(tempNullnessAnnos);

        NONNULL_ALIASES.forEach(annotation -> addAliasedAnnotation(annotation, NONNULL));
        NULLABLE_ALIASES.forEach(annotation -> addAliasedAnnotation(annotation, NULLABLE));

        // Add compatibility annotations:
        addAliasedAnnotation(
                "org.checkerframework.checker.nullness.compatqual.PolyNullDecl", POLYNULL);
        addAliasedAnnotation(
                "org.checkerframework.checker.nullness.compatqual.MonotonicNonNullDecl",
                MONOTONIC_NONNULL);
        addAliasedAnnotation(
                "org.checkerframework.checker.nullness.compatqual.PolyNullType", POLYNULL);
        addAliasedAnnotation(
                "org.checkerframework.checker.nullness.compatqual.MonotonicNonNullType",
                MONOTONIC_NONNULL);

        systemGetPropertyHandler = new SystemGetPropertyHandler(processingEnv, this);

        postInit();

        // do this last, as it might use the factory again.
        this.collectionToArrayHeuristics =
                new CollectionToArrayHeuristics(processingEnv, checker, this);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(
                        Nullable.class,
                        MonotonicNonNull.class,
                        NonNull.class,
                        UnderInitialization.class,
                        Initialized.class,
                        UnknownInitialization.class,
                        FBCBottom.class,
                        PolyNull.class));
    }

    /**
     * For types of left-hand side of an assignment, this method replaces {@link PolyNull} with
     * {@link Nullable} if the org.checkerframework.dataflow analysis has determined that this is
     * allowed soundly. For example:
     *
     * <pre> @PolyNull String foo(@PolyNull String param) {
     *    if (param == null) {
     *        //  @PolyNull is really @Nullable, so change
     *        // the type of param to @Nullable.
     *        param = null;
     *    }
     *    return param;
     * }
     * </pre>
     *
     * @param lhsType type to replace whose polymorphic qualifier will be replaced
     * @param context tree used to get dataflow value
     */
    protected void replacePolyQualifier(AnnotatedTypeMirror lhsType, Tree context) {
        if (lhsType.hasAnnotation(PolyNull.class)) {
            NullnessValue inferred = getInferredValueFor(context);
            if (inferred != null && inferred.isPolyNullNull) {
                lhsType.replaceAnnotation(NULLABLE);
            }
        }
    }

    @Override
    public List<VariableTree> getUninitializedInvariantFields(
            NullnessStore store,
            TreePath path,
            boolean isStatic,
            List<? extends AnnotationMirror> receiverAnnotations) {
        List<VariableTree> candidates =
                super.getUninitializedInvariantFields(store, path, isStatic, receiverAnnotations);
        List<VariableTree> result = new ArrayList<>();
        for (VariableTree c : candidates) {
            AnnotatedTypeMirror type = getAnnotatedType(c);
            boolean isPrimitive = TypesUtils.isPrimitive(type.getUnderlyingType());
            if (!isPrimitive) {
                // primitives do not need to be initialized
                result.add(c);
            }
        }
        return result;
    }

    @Override
    protected NullnessAnalysis createFlowAnalysis(
            List<Pair<VariableElement, NullnessValue>> fieldValues) {
        return new NullnessAnalysis(checker, this, fieldValues);
    }

    @Override
    public NullnessTransfer createFlowTransferFunction(
            CFAbstractAnalysis<NullnessValue, NullnessStore, NullnessTransfer> analysis) {
        return new NullnessTransfer((NullnessAnalysis) analysis);
    }

    /** @return an AnnotatedTypeFormatter that does not print the qualifiers on null literals */
    @Override
    protected AnnotatedTypeFormatter createAnnotatedTypeFormatter() {
        boolean printVerboseGenerics = checker.hasOption("printVerboseGenerics");
        return new NullnessAnnotatedTypeFormatter(
                printVerboseGenerics,
                // -AprintVerboseGenerics implies -AprintAllQualifiers
                printVerboseGenerics || checker.hasOption("printAllQualifiers"));
    }

    @Override
    public ParameterizedExecutableType methodFromUse(MethodInvocationTree tree) {
        ParameterizedExecutableType mType = super.methodFromUse(tree);
        AnnotatedExecutableType method = mType.executableType;

        systemGetPropertyHandler.handle(tree, method);
        collectionToArrayHeuristics.handle(tree, method);
        return mType;
    }

    @Override
    public void adaptGetClassReturnTypeToReceiver(
            final AnnotatedExecutableType getClassType, final AnnotatedTypeMirror receiverType) {

        super.adaptGetClassReturnTypeToReceiver(getClassType, receiverType);

        // Make the wildcard always @NonNull, regardless of the declared type.

        final AnnotatedDeclaredType returnAdt =
                (AnnotatedDeclaredType) getClassType.getReturnType();
        final List<AnnotatedTypeMirror> typeArgs = returnAdt.getTypeArguments();
        final AnnotatedWildcardType classWildcardArg = (AnnotatedWildcardType) typeArgs.get(0);
        classWildcardArg.getExtendsBoundField().replaceAnnotation(NONNULL);
    }

    @Override
    public AnnotatedTypeMirror getMethodReturnType(MethodTree m, ReturnTree r) {
        AnnotatedTypeMirror result = super.getMethodReturnType(m, r);
        replacePolyQualifier(result, r);
        return result;
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        DefaultForTypeAnnotator defaultForTypeAnnotator = new DefaultForTypeAnnotator(this);
        defaultForTypeAnnotator.addAtmClass(AnnotatedTypeMirror.AnnotatedNoType.class, NONNULL);
        defaultForTypeAnnotator.addAtmClass(
                AnnotatedTypeMirror.AnnotatedPrimitiveType.class, NONNULL);
        return new ListTypeAnnotator(
                new PropagationTypeAnnotator(this),
                defaultForTypeAnnotator,
                new NullnessTypeAnnotator(this),
                new CommitmentTypeAnnotator(this));
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        // Don't call super.createTreeAnnotator because the default tree annotators are incorrect
        // for the Nullness Checker.
        return new ListTreeAnnotator(
                // DebugListTreeAnnotator(new Tree.Kind[] {Tree.Kind.CONDITIONAL_EXPRESSION},
                new NullnessPropagationTreeAnnotator(this),
                new LiteralTreeAnnotator(this),
                new NullnessTreeAnnotator(this),
                new CommitmentTreeAnnotator(this));
    }

    /**
     * Nullness doesn't call propagation on binary and unary because the result is
     * always @Initialized (the default qualifier).
     *
     * <p>Would this be valid to move into CommitmentTreeAnnotator.
     */
    protected static class NullnessPropagationTreeAnnotator extends PropagationTreeAnnotator {

        /** Create the NullnessPropagationTreeAnnotator. */
        public NullnessPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            return null;
        }

        @Override
        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
            return null;
        }

        @Override
        public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
            if (type.getKind().isPrimitive()) {
                AnnotationMirror NONNULL = ((NullnessAnnotatedTypeFactory) atypeFactory).NONNULL;
                // If a @Nullable expression is cast to a primitive, then an unboxing.of.nullable
                // error is issued.  Treat the cast as if it were annotated as @NonNull to avoid an
                // type.invalid.annotations.on.use error.
                if (!type.isAnnotatedInHierarchy(NONNULL)) {
                    type.addAnnotation(NONNULL);
                }
            }
            return super.visitTypeCast(node, type);
        }
    }

    protected class NullnessTreeAnnotator extends TreeAnnotator
    /*extends InitializationAnnotatedTypeFactory<NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis>.CommitmentTreeAnnotator*/ {

        public NullnessTreeAnnotator(NullnessAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror type) {

            Element elt = TreeUtils.elementFromUse(node);
            assert elt != null;
            return null;
        }

        @Override
        public Void visitVariable(VariableTree node, AnnotatedTypeMirror type) {
            Element elt = TreeUtils.elementFromTree(node);
            if (elt.getKind() == ElementKind.EXCEPTION_PARAMETER) {
                if (!type.isAnnotatedInHierarchy(NONNULL)) {
                    // case 9. exception parameter
                    type.addAnnotation(NONNULL);
                }
            }
            return null;
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, AnnotatedTypeMirror type) {

            Element elt = TreeUtils.elementFromUse(node);
            assert elt != null;

            if (elt.getKind() == ElementKind.EXCEPTION_PARAMETER) {
                // TODO: It's surprising that we have to do this in
                // both visitVariable and visitIdentifier. This should
                // already be handled by applying the defaults anyway.
                // case 9. exception parameter
                type.replaceAnnotation(NONNULL);
            }

            return null;
        }

        // The result of a binary operation is always non-null.
        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            return null;
        }

        // The result of a compound operation is always non-null.
        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            // Commitment will run after for initialization defaults
            return null;
        }

        // The result of a unary operation is always non-null.
        @Override
        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            return null;
        }

        // The result of newly allocated structures is always non-null.
        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            return null;
        }

        @Override
        public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror type) {
            // The result of newly allocated structures is always non-null.
            if (!type.isAnnotatedInHierarchy(NONNULL)) {
                type.replaceAnnotation(NONNULL);
            }

            // The most precise element type for `new Object[] {null}` is @FBCBottom, but
            // the most useful element type is @Initialized (which is also accurate).
            AnnotatedArrayType arrayType = (AnnotatedArrayType) type;
            AnnotatedTypeMirror componentType = arrayType.getComponentType();
            if (componentType.hasEffectiveAnnotation(FBCBOTTOM)) {
                componentType.replaceAnnotation(INITIALIZED);
            }
            return null;
        }
    }

    protected class NullnessTypeAnnotator
            extends InitializationAnnotatedTypeFactory<
                            NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis>
                    .CommitmentTypeAnnotator {

        public NullnessTypeAnnotator(InitializationAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
            super(atypeFactory);
        }
    }

    /** @return the list of annotations of the non-null type system */
    public Set<Class<? extends Annotation>> getNullnessAnnotations() {
        return nullnessAnnos;
    }

    @Override
    public Set<Class<? extends Annotation>> getInvalidConstructorReturnTypeAnnotations() {
        Set<Class<? extends Annotation>> l =
                new HashSet<>(super.getInvalidConstructorReturnTypeAnnotations());
        l.addAll(getNullnessAnnotations());
        return l;
    }

    @Override
    public AnnotationMirror getFieldInvariantAnnotation() {
        return NONNULL;
    }

    /**
     * {@inheritDoc}
     *
     * <p>In other words, is the lower bound @NonNull?
     *
     * @param type of field that might have invariant annotation
     * @return whether or not type has the invariant annotation
     */
    @Override
    protected boolean hasFieldInvariantAnnotation(AnnotatedTypeMirror type) {
        AnnotationMirror invariant = getFieldInvariantAnnotation();
        Set<AnnotationMirror> lowerBounds =
                AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, type);
        return AnnotationUtils.containsSame(lowerBounds, invariant);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new NullnessQualifierHierarchy(factory, (Object[]) null);
    }

    protected class NullnessQualifierHierarchy extends InitializationQualifierHierarchy {

        public NullnessQualifierHierarchy(MultiGraphFactory f, Object[] arg) {
            super(f, arg);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (isInitializationAnnotation(subAnno) || isInitializationAnnotation(superAnno)) {
                return this.isSubtypeInitialization(subAnno, superAnno);
            }
            return super.isSubtype(subAnno, superAnno);
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (isInitializationAnnotation(a1) || isInitializationAnnotation(a2)) {
                return this.leastUpperBoundInitialization(a1, a2);
            }
            return super.leastUpperBound(a1, a2);
        }
    }
}
