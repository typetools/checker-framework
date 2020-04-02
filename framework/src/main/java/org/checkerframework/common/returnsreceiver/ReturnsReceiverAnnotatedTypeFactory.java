package org.checkerframework.common.returnsreceiver;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.returnsreceiver.framework.FrameworkSupport;
import org.checkerframework.common.returnsreceiver.framework.FrameworkSupportUtils;
import org.checkerframework.common.returnsreceiver.qual.BottomThis;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.checkerframework.common.returnsreceiver.qual.UnknownThis;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/** The type factory for the Returns Receiver Checker. */
public class ReturnsReceiverAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The {@code @}{@link This} annotation. */
    AnnotationMirror THIS_ANNOTATION;

    /** The supported frameworks (the built-in ones minus any that were disabled). */
    Collection<FrameworkSupport> frameworks;

    /**
     * Create a new {@code ReturnsReceiverAnnotatedTypeFactory}.
     *
     * @param checker the type-checker associated with this factory
     */
    public ReturnsReceiverAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        THIS_ANNOTATION = AnnotationBuilder.fromClass(elements, This.class);
        frameworks =
                FrameworkSupportUtils.getSupportedFrameworks(
                        checker.getOption(ReturnsReceiverChecker.DISABLE_FRAMEWORK_SUPPORT));
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(BottomThis.class, UnknownThis.class, This.class);
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(
                super.createTypeAnnotator(), new ReturnsReceiverTypeAnnotator(this));
    }

    /** A TypeAnnotator to add the {@code @}{@link This} annotation. */
    private class ReturnsReceiverTypeAnnotator extends TypeAnnotator {

        /**
         * Create a new ReturnsReceiverTypeAnnotator.
         *
         * @param typeFactory the {@link AnnotatedTypeFactory} associated with this {@link
         *     TypeAnnotator}
         */
        public ReturnsReceiverTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        @Override
        public Void visitExecutable(AnnotatedTypeMirror.AnnotatedExecutableType t, Void p) {

            AnnotatedTypeMirror returnType = t.getReturnType();
            AnnotationMirror unknownThisAnnot =
                    AnnotationBuilder.fromClass(elements, UnknownThis.class);
            AnnotationMirror retAnnotation = returnType.getAnnotationInHierarchy(unknownThisAnnot);
            if (retAnnotation != null && AnnotationUtils.areSame(retAnnotation, THIS_ANNOTATION)) {
                // add @This to the receiver type
                AnnotatedTypeMirror.AnnotatedDeclaredType receiverType = t.getReceiverType();
                receiverType.replaceAnnotation(THIS_ANNOTATION);
            }

            // skip constructors
            if (!isConstructor(t)) {
                // check each supported framework
                for (FrameworkSupport frameworkSupport : frameworks) {
                    // see if the method in the framework should return this
                    if (frameworkSupport.returnsThis(t)) {
                        // add @This annotation
                        returnType.replaceAnnotation(THIS_ANNOTATION);
                        AnnotatedTypeMirror.AnnotatedDeclaredType receiverType =
                                t.getReceiverType();
                        receiverType.replaceAnnotation(THIS_ANNOTATION);
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
        return t.getElement().getKind() == ElementKind.CONSTRUCTOR;
    }
}
