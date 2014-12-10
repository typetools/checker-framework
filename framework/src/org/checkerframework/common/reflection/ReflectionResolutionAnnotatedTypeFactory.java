package org.checkerframework.common.reflection;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/**
 * This AnnotatedTypeFactory resolves reflective method and constructor calls.
 * It aggregates the ConstantValue, ClassVal, and MethodVal type systems to
 * determine which method or constructor is called. If
 * {@link #methodFromUse(MethodInvocationTree)} is called for a reflective
 * method invocation and reflection can be resolved, then the annotations of the
 * resolved method are returned.
 *
 * @author rjust
 *
 */
public class ReflectionResolutionAnnotatedTypeFactory extends
        BaseAnnotatedTypeFactory {

    private final ReflectionResolver resolver;

    private final List<BaseAnnotatedTypeFactory> factories = new LinkedList<>();

    /**
     *
     * @param checker
     *            The checker of the used type system
     */
    public ReflectionResolutionAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        // TODO: Use command line switches to enable reflection resolution and
        // verbose debugging
        boolean debug = Boolean.getBoolean("org.checkerframework.common.reflection.debug");
        resolver = new DefaultReflectionResolver(checker, this, debug);

        factories.add(new ValueAnnotatedTypeFactory(checker));
        factories.add(new ClassValAnnotatedTypeFactory(checker, this));
        factories.add(new MethodValAnnotatedTypeFactory(checker, this));

        // TODO This hack is error prone as it is not obvious whether and how
        // postInit has to be used in sub classes!
        if (this.getClass().equals(
                ReflectionResolutionAnnotatedTypeFactory.class)) {
            this.postInit();
        }
    }

    @Override
    public void setRoot(CompilationUnitTree root) {
        super.setRoot(root);
        for (BaseAnnotatedTypeFactory factory : factories) {
            factory.setRoot(root);
        }
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        // The super implementation uses the name of the checker
        // to reflectively create a transfer with the checker name followed
        // by Transfer. Since this factory is intended to be used with
        // any checker, explicitly create the default CFTransfer
        return new CFTransfer(analysis);
    }

    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type,
            boolean iUseFlow) {
        // TODO: Error prone hack -> this implementation makes assumptions about
        // implementation details in the super class:
        // annotateImplicit(Tree tree, AnnotatedTypeMirror type) is assumed to
        // call
        // annotateImplicit(Tree tree, AnnotatedTypeMirror type, boolean
        // iUseFlow)
        //
        // Problem:
        // annotateImplicit(Tree tree, AnnotatedTypeMirror type) is final
        // annotateImplicit(Tree tree, AnnotatedTypeMirror type, boolean
        // iUseFlow) is protected
        super.annotateImplicit(tree, type, iUseFlow);
        if (iUseFlow) {
            for (BaseAnnotatedTypeFactory factory : factories) {
                factory.setUseFlow(iUseFlow);
                factory.annotateImplicit(tree, type);
            }
        }
    }

    @Override
    public AnnotationMirror getAnnotationMirror(Tree tree,
            Class<? extends Annotation> target) {
        AnnotationMirror mirror = AnnotationUtils.fromClass(elements, target);
        for (BaseAnnotatedTypeFactory factory : factories) {
            if (factory != null && factory.isSupportedQualifier(mirror)) {
                AnnotatedTypeMirror atm = factory.getAnnotatedType(tree);
                return atm.getAnnotation(target);
            }
        }
        return null;
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(
            MethodInvocationTree tree) {
        /*
         * Check whether reflective code should be resolved
         */
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super
                .methodFromUse(tree);
        if (resolver.shouldResolveReflection(tree)) {
            mfuPair = resolver.resolveReflectiveCall(this, tree, mfuPair);
            AnnotatedExecutableType method = mfuPair.first;
            poly.annotate(tree, method);
        }

        return mfuPair;
    }
}
