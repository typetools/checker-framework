package org.checkerframework.framework.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.EnsuresQualifiers;
import org.checkerframework.framework.qual.EnsuresQualifiersIf;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.framework.qual.RequiresQualifiers;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

/**
 * A utility class to handle pre- and postconditions.
 *
 * @see PreconditionAnnotation
 * @see RequiresQualifier
 * @see PostconditionAnnotation
 * @see EnsuresQualifier
 * @see EnsuresQualifierIf
 * @author Stefan Heule
 */
public class ContractsUtils {

    protected static ContractsUtils instance;
    protected GenericAnnotatedTypeFactory<?, ?, ?, ?> factory;

    /**
     * Returns an instance of the {@link ContractsUtils} class.
     */
    public static ContractsUtils getInstance(
            GenericAnnotatedTypeFactory<?, ?, ?, ?> factory) {
        if (instance == null || instance.factory != factory) {
            instance = new ContractsUtils(factory);
        }
        return instance;
    }

    /**
     * Returns a set of pairs {@code (expr, annotation)} of preconditions on the
     * element {@code element}.
     */
    public Set<Pair<String, String>> getPreconditions(
            Element element) {
        Set<Pair<String, String>> result = new HashSet<>();
        // Check for a single contract.
        AnnotationMirror requiresAnnotation = factory.getDeclAnnotation(
                element, RequiresQualifier.class);
        result.addAll(getPrecondition(requiresAnnotation));

        // Check for multiple contracts.
        AnnotationMirror requiresAnnotations = factory.getDeclAnnotation(
                element, RequiresQualifiers.class);
        if (requiresAnnotations != null) {
            List<AnnotationMirror> annotations = AnnotationUtils
                    .getElementValueArray(requiresAnnotations, "value", AnnotationMirror.class, false);
            for (AnnotationMirror a : annotations) {
                result.addAll(getPrecondition(a));
            }
        }

        // Check type-system specific annotations.
        Class<PreconditionAnnotation> metaAnnotation = PreconditionAnnotation.class;
        List<Pair<AnnotationMirror, AnnotationMirror>> declAnnotations = factory
                .getDeclAnnotationWithMetaAnnotation(element,
                        metaAnnotation);
        for (Pair<AnnotationMirror, AnnotationMirror> r : declAnnotations) {
            AnnotationMirror anno = r.first;
            AnnotationMirror metaAnno = r.second;
            List<String> expressions = AnnotationUtils.getElementValueArray(anno,
                    "value", String.class, false);
            String annotationString = AnnotationUtils.getElementValueClassName(
                    metaAnno, "qualifier", false).toString();
            for (String expr : expressions) {
                result.add(Pair.of(expr, annotationString));
            }
        }
        return result;
    }

    /**
     * Returns a set of pairs {@code (expr, annotation)} of preconditions
     * according to the given {@link RequiresQualifier}.
     */
    private Set<Pair<String, String>> getPrecondition(
            AnnotationMirror requiresAnnotation) {
        if (requiresAnnotation == null) {
            return Collections.emptySet();
        }
        Set<Pair<String, String>> result = new HashSet<>();
        List<String> expressions = AnnotationUtils.getElementValueArray(
                requiresAnnotation, "expression", String.class, false);
        String annotation = AnnotationUtils.getElementValueClassName(
                requiresAnnotation, "qualifier", false).toString();
        for (String expr : expressions) {
            result.add(Pair.of(expr, annotation));
        }
        return result;
    }

    /**
     * Returns a set of pairs {@code (expr, annotation)} of postconditions on
     * the method {@code methodElement}.
     */
    public Set<Pair<String, String>> getPostconditions(
            ExecutableElement methodElement) {
        Set<Pair<String, String>> result = new HashSet<>();
        // Check for a single contract.
        AnnotationMirror ensuresAnnotation = factory.getDeclAnnotation(
                methodElement, EnsuresQualifier.class);
        result.addAll(getPostcondition(ensuresAnnotation));

        // Check for multiple contracts.
        AnnotationMirror ensuresAnnotations = factory.getDeclAnnotation(
                methodElement, EnsuresQualifiers.class);
        if (ensuresAnnotations != null) {
            List<AnnotationMirror> annotations = AnnotationUtils
                    .getElementValueArray(ensuresAnnotations, "value", AnnotationMirror.class, false);
            for (AnnotationMirror a : annotations) {
                result.addAll(getPostcondition(a));
            }
        }

        // Check type-system specific annotations.
        Class<PostconditionAnnotation> metaAnnotation = PostconditionAnnotation.class;
        List<Pair<AnnotationMirror, AnnotationMirror>> declAnnotations = factory
                .getDeclAnnotationWithMetaAnnotation(methodElement,
                        metaAnnotation);
        for (Pair<AnnotationMirror, AnnotationMirror> r : declAnnotations) {
            AnnotationMirror anno = r.first;
            AnnotationMirror metaAnno = r.second;
            List<String> expressions = AnnotationUtils.getElementValueArray(anno,
                    "value", String.class, false);
            String annotationString = AnnotationUtils.getElementValueClassName(
                    metaAnno, "qualifier", false).toString();
            for (String expr : expressions) {
                result.add(Pair.of(expr, annotationString));
            }
        }
        return result;
    }

    /**
     * Returns a set of pairs {@code (expr, annotation)} of postconditions
     * according to the given {@link EnsuresQualifier}.
     */
    private Set<Pair<String, String>> getPostcondition(
            AnnotationMirror ensuresAnnotation) {
        if (ensuresAnnotation == null) {
            return Collections.emptySet();
        }
        Set<Pair<String, String>> result = new HashSet<>();
        List<String> expressions = AnnotationUtils.getElementValueArray(
                ensuresAnnotation, "expression", String.class, false);
        String annotation = AnnotationUtils.getElementValueClassName(
                ensuresAnnotation, "qualifier", false).toString();
        for (String expr : expressions) {
            result.add(Pair.of(expr, annotation));
        }
        return result;
    }

    /**
     * Returns a set of triples {@code (expr, (result, annotation))} of
     * conditional postconditions on the method {@code methodElement}.
     */
    public Set<Pair<String, Pair<Boolean, String>>> getConditionalPostconditions(
            ExecutableElement methodElement) {
        Set<Pair<String, Pair<Boolean, String>>> result = new HashSet<>();
        // Check for a single contract.
        AnnotationMirror ensuresAnnotationIf = factory.getDeclAnnotation(
                methodElement, EnsuresQualifierIf.class);
        result.addAll(getConditionalPostcondition(ensuresAnnotationIf));

        // Check for multiple contracts.
        AnnotationMirror ensuresAnnotationsIf = factory.getDeclAnnotation(
                methodElement, EnsuresQualifiersIf.class);
        if (ensuresAnnotationsIf != null) {
            List<AnnotationMirror> annotations = AnnotationUtils
                    .getElementValueArray(ensuresAnnotationsIf, "value", AnnotationMirror.class, false);
            for (AnnotationMirror a : annotations) {
                result.addAll(getConditionalPostcondition(a));
            }
        }

        // Check type-system specific annotations.
        Class<ConditionalPostconditionAnnotation> metaAnnotation = ConditionalPostconditionAnnotation.class;
        List<Pair<AnnotationMirror, AnnotationMirror>> declAnnotations = factory
                .getDeclAnnotationWithMetaAnnotation(methodElement,
                        metaAnnotation);
        for (Pair<AnnotationMirror, AnnotationMirror> r : declAnnotations) {
            AnnotationMirror anno = r.first;
            AnnotationMirror metaAnno = r.second;
            List<String> expressions = AnnotationUtils.getElementValueArray(anno,
                    "expression", String.class, false);
            String annotationString = AnnotationUtils.getElementValueClassName(
                    metaAnno, "qualifier", false).toString();
            boolean annoResult = AnnotationUtils.getElementValue(anno,
                    "result", Boolean.class, false);
            for (String expr : expressions) {
                result.add(Pair.of(expr, Pair.of(annoResult, annotationString)));
            }
        }
        return result;
    }

    /**
     * Returns a set of triples {@code (expr, (result, annotation))} of
     * conditional postconditions according to the given
     * {@link EnsuresQualifierIf}.
     */
    private Set<Pair<String, Pair<Boolean, String>>> getConditionalPostcondition(
            AnnotationMirror ensuresAnnotationIf) {
        if (ensuresAnnotationIf == null) {
            return Collections.emptySet();
        }
        Set<Pair<String, Pair<Boolean, String>>> result = new HashSet<>();
        List<String> expressions = AnnotationUtils.getElementValueArray(
                ensuresAnnotationIf, "expression", String.class, false);
        String annotation = AnnotationUtils.getElementValueClassName(
                ensuresAnnotationIf, "qualifier", false).toString();
        boolean annoResult = AnnotationUtils.getElementValue(ensuresAnnotationIf,
                "result", Boolean.class, false);
        for (String expr : expressions) {
            result.add(Pair.of(expr, Pair.of(annoResult, annotation)));
        }
        return result;
    }

    // private constructor
    private ContractsUtils(GenericAnnotatedTypeFactory<?, ?, ?, ?> factory) {
        this.factory = factory;
    }
}
