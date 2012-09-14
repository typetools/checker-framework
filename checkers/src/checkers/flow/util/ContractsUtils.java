package checkers.flow.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import checkers.quals.ConditionalPostconditionAnnotation;
import checkers.quals.EnsuresAnnotation;
import checkers.quals.EnsuresAnnotationIf;
import checkers.quals.EnsuresAnnotations;
import checkers.quals.EnsuresAnnotationsIf;
import checkers.quals.PostconditionAnnotation;
import checkers.quals.PreconditionAnnotation;
import checkers.quals.RequiresAnnotation;
import checkers.quals.RequiresAnnotations;
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
     * Returns a set of pairs {@code (expr, annotation)} of preconditions on the
     * method {@code methodElement}.
     */
    public Set<Pair<String, String>> getPreconditions(
            ExecutableElement methodElement) {
        Set<Pair<String, String>> result = new HashSet<>();
        // Check for a single contract.
        AnnotationMirror requiresAnnotation = factory.getDeclAnnotation(
                methodElement, RequiresAnnotation.class);
        result.addAll(getPrecondition(requiresAnnotation));

        // Check for multiple contracts.
        AnnotationMirror requiresAnnotations = factory.getDeclAnnotation(
                methodElement, RequiresAnnotations.class);
        if (requiresAnnotations != null) {
            List<AnnotationMirror> annotations = AnnotationUtils
                    .elementValueArray(requiresAnnotations, "value");
            for (AnnotationMirror a : annotations) {
                result.addAll(getPrecondition(a));
            }
        }

        // Check type-system specific annotations.
        Class<PreconditionAnnotation> metaAnnotation = PreconditionAnnotation.class;
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
     * Returns a set of pairs {@code (expr, annotation)} of preconditions
     * according to the given {@link RequiresAnnotation}.
     */
    private Set<Pair<String, String>> getPrecondition(
            AnnotationMirror requiresAnnotation) {
        if (requiresAnnotation == null) {
            return Collections.emptySet();
        }
        Set<Pair<String, String>> result = new HashSet<>();
        List<String> expressions = AnnotationUtils.elementValueArray(
                requiresAnnotation, "expression");
        String annotation = AnnotationUtils.elementValueClassName(
                requiresAnnotation, "annotation");
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

    /**
     * Returns a set of triples {@code (expr, (result, annotation))} of
     * conditional postconditions on the method {@code methodElement}.
     */
    public Set<Pair<String, Pair<Boolean, String>>> getConditionalPostconditions(
            ExecutableElement methodElement) {
        Set<Pair<String, Pair<Boolean, String>>> result = new HashSet<>();
        // Check for a single contract.
        AnnotationMirror ensuresAnnotationIf = factory.getDeclAnnotation(
                methodElement, EnsuresAnnotationIf.class);
        result.addAll(getConditionalPostcondition(ensuresAnnotationIf));

        // Check for multiple contracts.
        AnnotationMirror ensuresAnnotationsIf = factory.getDeclAnnotation(
                methodElement, EnsuresAnnotationsIf.class);
        if (ensuresAnnotationsIf != null) {
            List<AnnotationMirror> annotations = AnnotationUtils
                    .elementValueArray(ensuresAnnotationsIf, "value");
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
            List<String> expressions = AnnotationUtils.elementValueArray(anno,
                    "expression");
            String annotationString = AnnotationUtils.elementValueClassName(
                    metaAnno, "annotation");
            boolean annoResult = AnnotationUtils.elementValue(anno,
                    "result", Boolean.class);
            for (String expr : expressions) {
                result.add(Pair.of(expr, Pair.of(annoResult, annotationString)));
            }
        }
        return result;
    }

    /**
     * Returns a set of triples {@code (expr, (result, annotation))} of
     * conditional postconditions according to the given
     * {@link EnsuresAnnotationIf}.
     */
    private Set<Pair<String, Pair<Boolean, String>>> getConditionalPostcondition(
            AnnotationMirror ensuresAnnotationIf) {
        if (ensuresAnnotationIf == null) {
            return Collections.emptySet();
        }
        Set<Pair<String, Pair<Boolean, String>>> result = new HashSet<>();
        List<String> expressions = AnnotationUtils.elementValueArray(
                ensuresAnnotationIf, "expression");
        String annotation = AnnotationUtils.elementValueClassName(
                ensuresAnnotationIf, "annotation");
        boolean annoResult = AnnotationUtils.elementValue(ensuresAnnotationIf,
                "result", Boolean.class);
        for (String expr : expressions) {
            result.add(Pair.of(expr, Pair.of(annoResult, annotation)));
        }
        return result;
    }

    // private constructor
    private ContractsUtils(
            AbstractBasicAnnotatedTypeFactory<?, ?, ?, ?, ?> factory) {
        this.factory = factory;
    }
}
