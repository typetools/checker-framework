package org.checkerframework.common.purity;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.purity.qual.*;
import org.checkerframework.common.purity.qual.PurityUnqualified;
import org.checkerframework.javacutil.AnnotationBuilder;

/** The Purity Checker is used to determine if methods are deterministic and side-effect-free. */
public class PurityAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public PurityAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        addAliasedAnnotation(
                org.checkerframework.dataflow.qual.Pure.class,
                AnnotationBuilder.fromClass(getElementUtils(), Pure.class));
        addAliasedAnnotation(
                org.checkerframework.dataflow.qual.Deterministic.class,
                AnnotationBuilder.fromClass(getElementUtils(), Deterministic.class));
        addAliasedAnnotation(
                org.checkerframework.dataflow.qual.SideEffectFree.class,
                AnnotationBuilder.fromClass(getElementUtils(), SideEffectFree.class));
        addAliasedAnnotation(
                org.checkerframework.dataflow.qual.TerminatesExecution.class,
                AnnotationBuilder.fromClass(getElementUtils(), TerminatesExecution.class));

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<>(Arrays.asList(PurityUnqualified.class));
    }
}
