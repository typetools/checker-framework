package checkers.flow.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import checkers.quals.EnsuresAnnotation;
import checkers.quals.EnsuresAnnotationIf;
import checkers.quals.EnsuresAnnotations;
import checkers.quals.PostconditionAnnotation;
import checkers.quals.PreconditionAnnotation;
import checkers.quals.RequiresAnnotation;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.util.AnnotationUtils;
import checkers.util.Pair;

/**
 * A utility class to handle pre- and postconditions.
 *
 * @see PreconditionAnnotation
 * @see RequiresAnnotation
 * @see PostconditionAnnotation
 * @see EnsuresAnnotation
 * @see EnsuresAnnotationIf
 * @author Stefan Heule
 */
public class ContractsUtils {

    protected static ContractsUtils instance;
    protected AbstractBasicAnnotatedTypeFactory<?, ?, ?, ?, ?> factory;

    /**
     * Returns an instance of the {@link ContractsUtils} class.
     */
    public static ContractsUtils getInstance(
            AbstractBasicAnnotatedTypeFactory<?, ?, ?, ?, ?> factory) {
        if (instance == null || instance.factory != factory) {
            instance = new ContractsUtils(factory);
        }
        return instance;
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
                methodElement, EnsuresAnnotation.class);
        result.addAll(getPostcondition(ensuresAnnotation));

        // Check for multiple contracts.
        AnnotationMirror ensuresAnnotations = factory.getDeclAnnotation(
                methodElement, EnsuresAnnotations.class);
        if (ensuresAnnotations != null) {
            List<AnnotationMirror> annotations = AnnotationUtils
                    .elementValueArray(ensuresAnnotations, "value");
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
            List<String> expressions = AnnotationUtils.elementValueArray(anno,
                    "value");
            String annotationString = AnnotationUtils.elementValueClassName(
                    metaAnno, "annotation");
            for (String expr : expressions) {
                result.add(Pair.of(expr, annotationString));
            }
        }
        return result;
    }

    /**
     * Returns a set of pairs {@code (expr, annotation)} of postconditions
     * according to the given {@link EnsuresAnnotation}.
     */
    private Set<Pair<String, String>> getPostcondition(
            AnnotationMirror ensuresAnnotation) {
        if (ensuresAnnotation == null) {
            return Collections.emptySet();
        }
        Set<Pair<String, String>> result = new HashSet<>();
        List<String> expressions = AnnotationUtils.elementValueArray(
                ensuresAnnotation, "expression");
        String annotation = AnnotationUtils.elementValueClassName(
                ensuresAnnotation, "annotation");
        for (String expr : expressions) {
            result.add(Pair.of(expr, annotation));
        }
        return result;
    }

    // private constructor
    private ContractsUtils(
            AbstractBasicAnnotatedTypeFactory<?, ?, ?, ?, ?> factory) {
        this.factory = factory;
    }
}
