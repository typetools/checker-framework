package org.checkerframework.common.returnsreceiver;

import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
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
    final AnnotationMirror THIS_ANNOTATION;

    /**
     * Create a new {@code ReturnsReceiverAnnotatedTypeFactory}.
     *
     * @param checker the type-checker associated with this factory
     */
    public ReturnsReceiverAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        THIS_ANNOTATION = AnnotationBuilder.fromClass(elements, This.class);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(BottomThis.class, UnknownThis.class, This.class);
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(
                new ReturnsReceiverTypeAnnotator(this), super.createTypeAnnotator());
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

            // skip constructors, as we never need to add annotations to them
            if (!isConstructor(t)) {
                AnnotatedTypeMirror returnType = t.getReturnType();

                // If any FluentAPIGenerator indicates the method returns this,
                // add an @This annotation on the return type.
                if (FluentAPIGenerator.checkForFluentAPIGenerators(t)) {
                    if (!returnType.isAnnotatedInHierarchy(THIS_ANNOTATION)) {
                        returnType.addAnnotation(THIS_ANNOTATION);
                    }
                }

                // If return type is annotated with @This, add @This annotation
                // to the receiver type.
                AnnotationMirror retAnnotation =
                        returnType.getAnnotationInHierarchy(THIS_ANNOTATION);
                if (retAnnotation != null
                        && AnnotationUtils.areSame(retAnnotation, THIS_ANNOTATION)) {
                    AnnotatedTypeMirror.AnnotatedDeclaredType receiverType = t.getReceiverType();
                    if (!receiverType.isAnnotatedInHierarchy(THIS_ANNOTATION)) {
                        receiverType.addAnnotation(THIS_ANNOTATION);
                    }
                }
            }
            return super.visitExecutable(t, p);
        }
    }

    /**
     * @return {@code true} if the param {@code t} is a {@code Constructor}
     * @param t a {@link AnnotatedTypeMirror}
     */
    private boolean isConstructor(AnnotatedTypeMirror.AnnotatedExecutableType t) {
        return t.getElement().getKind() == ElementKind.CONSTRUCTOR;
    }
}
