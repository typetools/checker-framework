package org.checkerframework.common.returnsrcvr;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.returnsrcvr.framework.AutoValueSupport;
import org.checkerframework.common.returnsrcvr.framework.FrameworkSupport;
import org.checkerframework.common.returnsrcvr.framework.FrameworkSupportUtils;
import org.checkerframework.common.returnsrcvr.framework.LombokSupport;
import org.checkerframework.common.returnsrcvr.qual.BottomThis;
import org.checkerframework.common.returnsrcvr.qual.MaybeThis;
import org.checkerframework.common.returnsrcvr.qual.This;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

/** A factory that extends {@link BaseAnnotatedTypeFactory} for the returns receiver checker */
public class ReturnsRcvrAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * property of type {@link AnnotationMirror} which will be initialized to {@link This}
     * annotation
     */
    AnnotationMirror THIS_ANNOT;

    /** the collection of the built-in framework supports for returns receiver checker */
    Collection<FrameworkSupport> frameworkSupports;

    /**
     * Create a new {@code ReturnsRcvrAnnotatedTypeFactory}.
     *
     * @param checker the type-checker associated with this factory
     */
    public ReturnsRcvrAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        THIS_ANNOT = AnnotationBuilder.fromClass(elements, This.class);

        EnumSet<FrameworkSupportUtils.Framework> frameworkSet =
                FrameworkSupportUtils.getFrameworkSet(
                        checker.getOption(ReturnsRcvrChecker.DISABLE_FRAMEWORK_SUPPORT));
        frameworkSupports = new ArrayList<FrameworkSupport>();

        for (FrameworkSupportUtils.Framework framework : frameworkSet) {
            switch (framework) {
                case AUTO_VALUE:
                    frameworkSupports.add(new AutoValueSupport());
                    break;
                case LOMBOK:
                    frameworkSupports.add(new LombokSupport());
                    break;
                default:
                    throw new BugInCF("Unknown framework " + framework);
            }
        }
        // we have to call this explicitly
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(BottomThis.class, MaybeThis.class, This.class);
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(
                super.createTypeAnnotator(), new ReturnsRcvrTypeAnnotator(this));
    }

    /** A TypeAnnotator to add {@link This} annotation to the code */
    private class ReturnsRcvrTypeAnnotator extends TypeAnnotator {

        /**
         * Create a new ReturnsRcvrAnnotatedTypeFactory.
         *
         * @param typeFactory the {@link AnnotatedTypeFactory} associated with this {@link
         *     TypeAnnotator}
         */
        public ReturnsRcvrTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        @Override
        public Void visitExecutable(AnnotatedTypeMirror.AnnotatedExecutableType t, Void p) {

            AnnotatedTypeMirror returnType = t.getReturnType();
            AnnotationMirror maybeThisAnnot =
                    AnnotationBuilder.fromClass(elements, MaybeThis.class);
            AnnotationMirror retAnnotation = returnType.getAnnotationInHierarchy(maybeThisAnnot);
            if (retAnnotation != null && AnnotationUtils.areSame(retAnnotation, THIS_ANNOT)) {
                // add @This to the receiver type
                AnnotatedTypeMirror.AnnotatedDeclaredType receiverType = t.getReceiverType();
                receiverType.replaceAnnotation(THIS_ANNOT);
            }

            // skip constructors
            if (!isConstructor(t)) {
                // check each supported framework
                for (FrameworkSupport frameworkSupport : frameworkSupports) {
                    // see if the method in the framework should return this
                    if (frameworkSupport.knownToReturnThis(t)) {
                        // add @This annotation
                        returnType.replaceAnnotation(THIS_ANNOT);
                        AnnotatedTypeMirror.AnnotatedDeclaredType receiverType =
                                t.getReceiverType();
                        receiverType.replaceAnnotation(THIS_ANNOT);
                        break;
                    }
                }
            }
            return super.visitExecutable(t, p);
        }
    }

    /**
     * @return {@code true} if the param {@code t} is a {@code Constructor}.
     * @param t the {@link AnnotatedTypeMirror}
     */
    private boolean isConstructor(AnnotatedTypeMirror.AnnotatedExecutableType t) {
        ExecutableElement element = t.getElement();
        return element.getKind() == ElementKind.CONSTRUCTOR;
    }
}
