package org.checkerframework.qualframework.base;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.qualframework.base.dataflow.QualAnalysis;
import org.checkerframework.qualframework.base.dataflow.QualTransferAdapter;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/**
 * Adapter class for {@link QualifiedTypeFactory}, extending
 * {@link BaseAnnotatedTypeFactory BaseAnnotatedTypeFactory}.
 */
class QualifiedTypeFactoryAdapter<Q> extends BaseAnnotatedTypeFactory {
    /** The underlying {@link QualifiedTypeFactory}. */
    private final QualifiedTypeFactory<Q> underlying;

    /** The qualAnalysis instance to use for dataflow. */
    private QualAnalysis<Q> qualAnalysis;

    public QualifiedTypeFactoryAdapter(QualifiedTypeFactory<Q> underlying,
            CheckerAdapter<Q> checker) {
        super(checker, true);
        this.underlying = underlying;

        // We can't call postInit yet.  See CheckerAdapter.getTypeFactory for
        // explanation.
    }

    /** Allow CheckerAdapter to call postInit when it's ready.  See
     * CheckerAdapter.getTypeFactory for explanation.
     */
    void doPostInit() {
        this.postInit();
    }

    // in the qualifier framework, type qualifiers are handled through the @AnnotationConverter
    // createSupportedTypeQualifiers() must return an empty set, otherwise it will try to reflectively load qualifier framework annotations
    // and process them in a classical manner
    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return Collections.emptySet();
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defs) {
        getCheckerAdapter().setupDefaults(defs);
    }

    /** Returns the underlying {@link QualifiedTypeFactory}. */
    public QualifiedTypeFactory<Q> getUnderlying() {
        return underlying;
    }

    /** Returns {@link checker}, downcast to a more precise type. */
    @SuppressWarnings("unchecked")
    CheckerAdapter<Q> getCheckerAdapter() {
        return (CheckerAdapter<Q>)checker;
    }

    /** Returns the same result as {@link getQualifierHierarchy}, but downcast
     * to a more precise type. */
    @SuppressWarnings("unchecked")
    private QualifierHierarchyAdapter<Q>.Implementation getQualifierHierarchyAdapter() {
        return (QualifierHierarchyAdapter<Q>.Implementation)getQualifierHierarchy();
    }

    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    public MultiGraphQualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        QualifierHierarchy<Q> underlyingHierarchy = underlying.getQualifierHierarchy();
        DefaultQualifiedTypeFactory<Q> defaultUnderlying = (DefaultQualifiedTypeFactory<Q>)underlying;
        AnnotationConverter<Q> annoConverter = defaultUnderlying.getAnnotationConverter();

        // See QualifierHierarchyAdapter for an explanation of why we need this
        // strange pattern instead of just making a single call to the
        // QualifierHierarchyAdapter constructor.
        QualifierHierarchyAdapter<Q>.Implementation adapter =
            new QualifierHierarchyAdapter<Q>(
                annoConverter,
                underlyingHierarchy,
                getCheckerAdapter().getTypeMirrorConverter())
            .createImplementation(factory);
        return adapter;
    }

    /* Constructs a TypeHierarchyAdapter for the underlying factory's
     * TypeHierarchy.
     */
    @Override
    protected org.checkerframework.framework.type.TypeHierarchy createTypeHierarchy() {
        TypeHierarchy<Q> underlyingHierarchy = underlying.getTypeHierarchy();

        TypeHierarchyAdapter<Q> adapter = new TypeHierarchyAdapter<Q>(
                underlyingHierarchy,
                getCheckerAdapter().getTypeMirrorConverter(),
                getCheckerAdapter(),
                getQualifierHierarchyAdapter(),
                checker.getOption("ignoreRawTypeArguments", "true").equals("true"),
                checker.hasOption("invariantArrays"));

        // TODO: Move this check (and others like it) into the adapter
        // constructor.
        if (underlyingHierarchy instanceof DefaultTypeHierarchy) {
            DefaultTypeHierarchy<Q> defaultHierarchy =
                (DefaultTypeHierarchy<Q>)underlyingHierarchy;
            defaultHierarchy.setAdapter(adapter);
        }

        return adapter;
    }

    /* Constructs a TreeAnnotatorAdapter for the underlying factory's
     * TreeAnnotator.
     */
    @Override
    protected org.checkerframework.framework.type.treeannotator.TreeAnnotator createTreeAnnotator() {
        if (!(underlying instanceof DefaultQualifiedTypeFactory)) {
            // In theory, the result of this branch should never be used.  Only
            // DefaultQTFs have a way to access the annotation-based logic
            // which requires the TreeAnnotator produced by this method.
            return null;
        }

        DefaultQualifiedTypeFactory<Q> defaultUnderlying =
            (DefaultQualifiedTypeFactory<Q>)underlying;
        TreeAnnotator<Q> underlyingAnnotator = defaultUnderlying.getTreeAnnotator();
        TreeAnnotatorAdapter<Q> adapter = new TreeAnnotatorAdapter<Q>(
                underlyingAnnotator,
                getCheckerAdapter().getTypeMirrorConverter(),
                this);

        underlyingAnnotator.setAdapter(adapter);

        return adapter;
    }

    /* Constructs a TypeAnnotatorAdapter for the underlying factory's
     * TypeAnnotator.
     */
    @Override
    protected org.checkerframework.framework.type.typeannotator.TypeAnnotator createTypeAnnotator() {
        if (!(underlying instanceof DefaultQualifiedTypeFactory)) {
            // In theory, the result of this branch should never be used.  Only
            // DefaultQTFs have a way to access the annotation-based logic
            // which requires the TypeAnnotator produced by this method.
            return null;
        }

        DefaultQualifiedTypeFactory<Q> defaultUnderlying =
            (DefaultQualifiedTypeFactory<Q>)underlying;
        TypeAnnotator<Q> underlyingAnnotator = defaultUnderlying.getTypeAnnotator();
        TypeAnnotatorAdapter<Q> adapter = new TypeAnnotatorAdapter<Q>(
                underlyingAnnotator,
                getCheckerAdapter().getTypeMirrorConverter(),
                this);

        underlyingAnnotator.setAdapter(adapter);

        return adapter;
    }


    @Override
    public boolean isSupportedQualifier(AnnotationMirror anno) {
        if (anno == null) {
            return false;
        }

        // If 'underlying' is not a DefaultQTF, there is no AnnotationConverter
        // for us to use for this check.
        if (!(underlying instanceof DefaultQualifiedTypeFactory)) {
            return true;
        }

        DefaultQualifiedTypeFactory<Q> defaultUnderlying = (DefaultQualifiedTypeFactory<Q>)underlying;
        AnnotationConverter<Q> annoConverter = defaultUnderlying.getAnnotationConverter();

        return annoConverter.isAnnotationSupported(anno)
                || getCheckerAdapter().getTypeMirrorConverter().isKey(anno);
    }


    @Override
    public AnnotatedTypeMirror getAnnotatedType(Element elt) {
        return getCheckerAdapter().getTypeMirrorConverter().getAnnotatedType(
                underlying.getQualifiedType(elt));
    }

    QualifiedTypeMirror<Q> superGetAnnotatedType(Element elt) {
        AnnotatedTypeMirror atm = super.getAnnotatedType(elt);
        return getCheckerAdapter().getTypeMirrorConverter().getQualifiedType(atm);
    }

    @Override
    public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
        return getCheckerAdapter().getTypeMirrorConverter().getAnnotatedType(
                underlying.getQualifiedType(tree));
    }

    QualifiedTypeMirror<Q> superGetAnnotatedType(Tree tree) {
        AnnotatedTypeMirror atm = super.getAnnotatedType(tree);
        return getCheckerAdapter().getTypeMirrorConverter().getQualifiedType(atm);
    }

    @Override
    public AnnotatedTypeMirror getAnnotatedTypeFromTypeTree(Tree tree) {
        return getCheckerAdapter().getTypeMirrorConverter().getAnnotatedType(
                underlying.getQualifiedTypeFromTypeTree(tree));
    }

    QualifiedTypeMirror<Q> superGetAnnotatedTypeFromTypeTree(Tree tree) {
        AnnotatedTypeMirror atm = super.getAnnotatedTypeFromTypeTree(tree);
        return getCheckerAdapter().getTypeMirrorConverter().getQualifiedType(atm);
    }

    @Override
    public void postDirectSuperTypes(AnnotatedTypeMirror subtype, List<? extends AnnotatedTypeMirror> supertypes) {
        TypeMirrorConverter<Q> conv = getCheckerAdapter().getTypeMirrorConverter();

        for (AnnotatedTypeMirror atm: supertypes) {
            defaults.annotate((Element)null, atm);
        }

        QualifiedTypeMirror<Q> qualSubtype = conv.getQualifiedType(subtype);
        List<QualifiedTypeMirror<Q>> qualSupertypes = conv.getQualifiedTypeList(supertypes);

        List<QualifiedTypeMirror<Q>> qualResult = underlying.postDirectSuperTypes(qualSubtype, qualSupertypes);

        for (int i = 0; i < supertypes.size(); ++i) {
            conv.applyQualifiers(qualResult.get(i), supertypes.get(i));
        }
    }

    List<QualifiedTypeMirror<Q>> superPostDirectSuperTypes(QualifiedTypeMirror<Q> subtype, List<? extends QualifiedTypeMirror<Q>> supertypes) {
        TypeMirrorConverter<Q> conv = getCheckerAdapter().getTypeMirrorConverter();

        AnnotatedTypeMirror annoSubtype = conv.getAnnotatedType(subtype);
        List<AnnotatedTypeMirror> annoSupertypes = conv.getAnnotatedTypeList(supertypes);

        super.postDirectSuperTypes(annoSubtype, annoSupertypes);

        return conv.getQualifiedTypeList(annoSupertypes);
    }


    @Override
    public void postAsMemberOf(AnnotatedTypeMirror memberType, AnnotatedTypeMirror receiverType, Element memberElement) {
        TypeMirrorConverter<Q> conv = getCheckerAdapter().getTypeMirrorConverter();

        QualifiedTypeMirror<Q> qualMemberType = conv.getQualifiedType(memberType);
        QualifiedTypeMirror<Q> qualReceiverType = conv.getQualifiedType(receiverType);

        QualifiedTypeMirror<Q> qualResult = underlying.postAsMemberOf(
                qualMemberType, qualReceiverType, memberElement);

        conv.applyQualifiers(qualResult, memberType);
    }

    QualifiedTypeMirror<Q> superPostAsMemberOf(
            QualifiedTypeMirror<Q> memberType, QualifiedTypeMirror<Q> receiverType, Element memberElement) {
        TypeMirrorConverter<Q> conv = getCheckerAdapter().getTypeMirrorConverter();

        AnnotatedTypeMirror annoMemberType = conv.getAnnotatedType(memberType);
        AnnotatedTypeMirror annoReceiverType = conv.getAnnotatedType(receiverType);

        super.postAsMemberOf(annoMemberType, annoReceiverType, memberElement);

        QualifiedTypeMirror<Q> qualResult = conv.getQualifiedType(annoMemberType);
        return qualResult;
    }


    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(MethodInvocationTree tree) {
        Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> qualResult =
            underlying.methodFromUse(tree);

        TypeMirrorConverter<Q> conv = getCheckerAdapter().getTypeMirrorConverter();
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> annoResult =
            Pair.of((AnnotatedExecutableType)conv.getAnnotatedType(qualResult.first),
                    conv.getAnnotatedTypeList(qualResult.second));

        return annoResult;
    }

    Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> superMethodFromUse(MethodInvocationTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> annoResult =
            super.methodFromUse(tree);

        TypeMirrorConverter<Q> conv = getCheckerAdapter().getTypeMirrorConverter();
        Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> qualResult =
            Pair.of((QualifiedExecutableType<Q>) conv.getQualifiedType(annoResult.first),
                    conv.getQualifiedTypeList(annoResult.second));
        return qualResult;
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(ExpressionTree tree,
            ExecutableElement methodElt, AnnotatedTypeMirror receiverType) {

        TypeMirrorConverter<Q> conv = getCheckerAdapter().getTypeMirrorConverter();
        Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> qualResult =
                underlying.methodFromUse(tree, methodElt, conv.getQualifiedType(receiverType));

        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> annoResult =
                Pair.of((AnnotatedExecutableType) conv.getAnnotatedType(qualResult.first),
                        conv.getAnnotatedTypeList(qualResult.second));
        return annoResult;
    }

    Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> superMethodFromUse(ExpressionTree tree,
            ExecutableElement methodElt, QualifiedTypeMirror<Q> receiverType) {
        TypeMirrorConverter<Q> conv = getCheckerAdapter().getTypeMirrorConverter();

        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> annoResult =
                super.methodFromUse(tree, methodElt, conv.getAnnotatedType(receiverType));

        Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> qualResult =
                Pair.of((QualifiedExecutableType<Q>) conv.getQualifiedType(annoResult.first),
                        conv.getQualifiedTypeList(annoResult.second));
        return qualResult;
    }

    /**
     * Create the {@link TransferFunction} to be used.
     *
     * @param analysis The {@link CFAbstractAnalysis} that the checker framework will actually use
     * @return The {@link CFTransfer} to be used
     */
    @Override
    public CFTransfer createFlowTransferFunction(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        if (qualAnalysis == null) {
            // TODO: When we actually use the qual analysis, we will have to initialize it with real data.
            qualAnalysis = underlying.createFlowAnalysis(null);
            qualAnalysis.setAdapter(analysis);
        }
        return new QualTransferAdapter<>(qualAnalysis.createTransferFunction(), analysis, qualAnalysis);
    }

    /**
     * Create an adapter using a TypeVariableSubstitutor from the qual type system.
     */
    @Override
    protected TypeVariableSubstitutorAdapter<Q> createTypeVariableSubstitutor() {

        TypeVariableSubstitutor<Q> substitutor = underlying.createTypeVariableSubstitutor();
        TypeVariableSubstitutorAdapter<Q> adapter = new TypeVariableSubstitutorAdapter<Q>(substitutor,
                this.getCheckerAdapter().getTypeMirrorConverter());
        substitutor.setAdapter(adapter);

        return adapter;
    }

    /**
     * The qual framework's tree and type annotators behave differently than the
     * checker frameworks. The default of the checker framework also does not apply.
     */
    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        assert root != null : "GenericAnnotatedTypeFactory.annotateImplicit: " +
                " root needs to be set when used on trees; factory: " + this.getClass();

        if (iUseFlow) {
            /**
             * We perform flow analysis on each {@link ClassTree} that is
             * passed to annotateImplicit.  This works correctly when
             * a {@link ClassTree} is passed to this method before any of its
             * sub-trees.  It also helps to satisfy the requirement that a
             * {@link ClassTree} has been advanced to annotation before we
             * analyze it.
             */
            checkAndPerformFlowAnalysis(tree);
        }

        defaults.annotate(tree, type);
        treeAnnotator.visit(tree, type);
        typeAnnotator.visit(type, null);

        if (iUseFlow) {
            CFValue as = getInferredValueFor(tree);
            if (as != null) {
                applyInferredAnnotations(type, as);
            }
        }

    }

    @Override
    public void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
        defaults.annotate(elt, type);
        typeAnnotator.visit(type, null);
    }
}
