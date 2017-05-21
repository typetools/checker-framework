package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
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

    /** Returns an instance of the {@link ContractsUtils} class. */
    public static ContractsUtils getInstance(GenericAnnotatedTypeFactory<?, ?, ?, ?> factory) {
        if (instance == null || instance.factory != factory) {
            instance = new ContractsUtils(factory);
        }
        return instance;
    }

    /**
     * A contract represents an annotation on an expression, along with the kind: precondition,
     * postcondition, or conditional postcondition.
     */
    public abstract static class Contract {

        public enum Kind {
            PRECONDITION("precondition"),
            POSTCONDTION("postcondition"),
            CONDITIONALPOSTCONDTION("conditional.postcondition");
            public final String errorKey;

            Kind(String errorKey) {
                this.errorKey = errorKey;
            }
        }

        /**
         * The expression for which the condition must hold, such as {@code "foo"} in
         * {@code @RequiresNonNull("foo")}.
         */
        public final String expression;

        /** The annotation that must be on the type of expression as part of this contract. */
        public final AnnotationMirror annotation;

        public final Kind kind;

        public Contract(String expression, AnnotationMirror annotation, Kind kind) {
            this.expression = expression;
            this.annotation = annotation;
            this.kind = kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Contract contract = (Contract) o;

            if (expression != null
                    ? !expression.equals(contract.expression)
                    : contract.expression != null) {
                return false;
            }
            if (annotation != null
                    ? !annotation.equals(contract.annotation)
                    : contract.annotation != null) {
                return false;
            }
            return kind == contract.kind;
        }

        @Override
        public int hashCode() {
            int result = expression != null ? expression.hashCode() : 0;
            result = 31 * result + (annotation != null ? annotation.hashCode() : 0);
            result = 31 * result + (kind != null ? kind.hashCode() : 0);
            return result;
        }
    }

    public static class Precondition extends Contract {
        public Precondition(String expression, AnnotationMirror annotation) {
            super(expression, annotation, Kind.PRECONDITION);
        }
    }

    public static class Postcondition extends Contract {
        public Postcondition(String expression, AnnotationMirror annotation) {
            super(expression, annotation, Kind.POSTCONDTION);
        }
    }

    /**
     * Represents a conditional postcondition that must be verified by {@code BaseTypeVisitor} or
     * one of its subclasses. Automatically extracted from annotations with meta-annotation
     * {@code @ConditionalPostconditionAnnotation}, such as {@code EnsuresNonNullIf}.
     */
    public static class ConditionalPostcondition extends Contract {

        /**
         * The return value for the annotated method that ensures that the conditional postcondition
         * holds. For example, given<br>
         * {@code @EnsuresNonNullIf(expression="foo", result=false) boolean method()}<br>
         * {@code foo} is guaranteed to be {@code @NonNull} after a call to {@code method()} if that
         * call returns {@code false}.
         */
        public final boolean annoResult;

        public ConditionalPostcondition(
                String expression, boolean annoResult, AnnotationMirror annotation) {
            super(expression, annotation, Kind.CONDITIONALPOSTCONDTION);
            this.annoResult = annoResult;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            ConditionalPostcondition that = (ConditionalPostcondition) o;
            return annoResult == that.annoResult;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (annoResult ? 1 : 0);
            return result;
        }
    }

    public List<Contract> getContracts(ExecutableElement element) {
        List<Contract> contracts = new ArrayList<>();
        contracts.addAll(getPreconditions(element));
        contracts.addAll(getPostconditions(element));
        contracts.addAll(getConditionalPostconditions(element));
        return contracts;
    }

    /** Returns the set of preconditions on the element {@code element}. */
    public Set<Precondition> getPreconditions(Element element) {
        Set<Precondition> result = new LinkedHashSet<>();
        // Check for a single contract.
        AnnotationMirror requiresAnnotation =
                factory.getDeclAnnotation(element, RequiresQualifier.class);
        result.addAll(getPrecondition(requiresAnnotation));

        // Check for multiple contracts.
        AnnotationMirror requiresAnnotations =
                factory.getDeclAnnotation(element, RequiresQualifiers.class);
        if (requiresAnnotations != null) {
            List<AnnotationMirror> annotations =
                    AnnotationUtils.getElementValueArray(
                            requiresAnnotations, "value", AnnotationMirror.class, false);
            for (AnnotationMirror a : annotations) {
                result.addAll(getPrecondition(a));
            }
        }

        // Check type-system specific annotations.
        Class<PreconditionAnnotation> metaAnnotation = PreconditionAnnotation.class;
        List<Pair<AnnotationMirror, AnnotationMirror>> declAnnotations =
                factory.getDeclAnnotationWithMetaAnnotation(element, metaAnnotation);
        for (Pair<AnnotationMirror, AnnotationMirror> r : declAnnotations) {
            AnnotationMirror anno = r.first;
            AnnotationMirror metaAnno = r.second;
            List<String> expressions =
                    AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
            AnnotationMirror precondtionAnno = getAnnotationMirrorOfQualifier(metaAnno);
            if (precondtionAnno == null) {
                continue;
            }
            for (String expr : expressions) {
                result.add(new Precondition(expr, precondtionAnno));
            }
        }
        return result;
    }

    /** Returns the annotation mirror as specified by the "qualifier" value in metaAnno. */
    private AnnotationMirror getAnnotationMirrorOfQualifier(AnnotationMirror metaAnno) {
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> c =
                (Class<? extends Annotation>)
                        AnnotationUtils.getElementValueClass(metaAnno, "qualifier", false);
        AnnotationMirror anno = AnnotationUtils.fromClass(factory.getElementUtils(), c);
        if (factory.isSupportedQualifier(anno)) {
            return anno;
        } else {
            return null;
        }
    }

    /** Returns the set of preconditions according to the given {@link RequiresQualifier}. */
    private Set<Precondition> getPrecondition(AnnotationMirror requiresAnnotation) {
        if (requiresAnnotation == null) {
            return Collections.emptySet();
        }
        Set<Precondition> result = new LinkedHashSet<Precondition>();
        List<String> expressions =
                AnnotationUtils.getElementValueArray(
                        requiresAnnotation, "expression", String.class, false);
        AnnotationMirror postcondAnno = getAnnotationMirrorOfQualifier(requiresAnnotation);
        if (postcondAnno == null) {
            return result;
        }
        for (String expr : expressions) {
            result.add(new Precondition(expr, postcondAnno));
        }
        return result;
    }

    /** Returns the set of postconditions on the method {@code methodElement}. */
    public Set<Postcondition> getPostconditions(ExecutableElement methodElement) {
        Set<Postcondition> result = new LinkedHashSet<>();
        // Check for a single contract.
        AnnotationMirror ensuresAnnotation =
                factory.getDeclAnnotation(methodElement, EnsuresQualifier.class);
        result.addAll(getPostcondition(ensuresAnnotation));

        // Check for multiple contracts.
        AnnotationMirror ensuresAnnotations =
                factory.getDeclAnnotation(methodElement, EnsuresQualifiers.class);
        if (ensuresAnnotations != null) {
            List<AnnotationMirror> annotations =
                    AnnotationUtils.getElementValueArray(
                            ensuresAnnotations, "value", AnnotationMirror.class, false);
            for (AnnotationMirror a : annotations) {
                result.addAll(getPostcondition(a));
            }
        }

        // Check type-system specific annotations.
        Class<PostconditionAnnotation> metaAnnotation = PostconditionAnnotation.class;
        List<Pair<AnnotationMirror, AnnotationMirror>> declAnnotations =
                factory.getDeclAnnotationWithMetaAnnotation(methodElement, metaAnnotation);
        for (Pair<AnnotationMirror, AnnotationMirror> r : declAnnotations) {
            AnnotationMirror anno = r.first;
            AnnotationMirror metaAnno = r.second;
            List<String> expressions =
                    AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
            AnnotationMirror postcondAnno = getAnnotationMirrorOfQualifier(metaAnno);
            if (postcondAnno == null) {
                continue;
            }
            for (String expr : expressions) {
                result.add(new Postcondition(expr, postcondAnno));
            }
        }
        return result;
    }

    /** Returns the set of postconditions according to the given {@link EnsuresQualifier}. */
    private Set<Postcondition> getPostcondition(AnnotationMirror ensuresAnnotation) {
        if (ensuresAnnotation == null) {
            return Collections.emptySet();
        }
        Set<Postcondition> result = new LinkedHashSet<>();
        List<String> expressions =
                AnnotationUtils.getElementValueArray(
                        ensuresAnnotation, "expression", String.class, false);
        AnnotationMirror postcondAnno = getAnnotationMirrorOfQualifier(ensuresAnnotation);
        if (postcondAnno == null) {
            return result;
        }
        for (String expr : expressions) {
            result.add(new Postcondition(expr, postcondAnno));
        }
        return result;
    }

    /**
     * Returns a set of triples {@code (expr, (result, annotation))} of conditional postconditions
     * on the method {@code methodElement}.
     */
    public Set<ConditionalPostcondition> getConditionalPostconditions(
            ExecutableElement methodElement) {
        Set<ConditionalPostcondition> result = new LinkedHashSet<ConditionalPostcondition>();
        // Check for a single contract.
        AnnotationMirror ensuresAnnotationIf =
                factory.getDeclAnnotation(methodElement, EnsuresQualifierIf.class);
        result.addAll(getConditionalPostcondition(ensuresAnnotationIf));

        // Check for multiple contracts.
        AnnotationMirror ensuresAnnotationsIf =
                factory.getDeclAnnotation(methodElement, EnsuresQualifiersIf.class);
        if (ensuresAnnotationsIf != null) {
            List<AnnotationMirror> annotations =
                    AnnotationUtils.getElementValueArray(
                            ensuresAnnotationsIf, "value", AnnotationMirror.class, false);
            for (AnnotationMirror a : annotations) {
                result.addAll(getConditionalPostcondition(a));
            }
        }

        // Check type-system specific annotations.
        Class<ConditionalPostconditionAnnotation> metaAnnotation =
                ConditionalPostconditionAnnotation.class;
        List<Pair<AnnotationMirror, AnnotationMirror>> declAnnotations =
                factory.getDeclAnnotationWithMetaAnnotation(methodElement, metaAnnotation);
        for (Pair<AnnotationMirror, AnnotationMirror> r : declAnnotations) {
            AnnotationMirror anno = r.first;
            AnnotationMirror metaAnno = r.second;
            List<String> expressions =
                    AnnotationUtils.getElementValueArray(anno, "expression", String.class, false);
            AnnotationMirror postcondAnno = getAnnotationMirrorOfQualifier(metaAnno);
            if (postcondAnno == null) {
                continue;
            }
            boolean annoResult =
                    AnnotationUtils.getElementValue(anno, "result", Boolean.class, false);
            for (String expr : expressions) {
                result.add(new ConditionalPostcondition(expr, annoResult, postcondAnno));
            }
        }
        return result;
    }

    /**
     * Returns a set of triples {@code (expr, (result, annotation))} of conditional postconditions
     * according to the given {@link EnsuresQualifierIf}.
     */
    private Set<ConditionalPostcondition> getConditionalPostcondition(
            AnnotationMirror ensuresAnnotationIf) {
        if (ensuresAnnotationIf == null) {
            return Collections.emptySet();
        }
        Set<ConditionalPostcondition> result = new LinkedHashSet<>();
        List<String> expressions =
                AnnotationUtils.getElementValueArray(
                        ensuresAnnotationIf, "expression", String.class, false);
        AnnotationMirror postcondAnno = getAnnotationMirrorOfQualifier(ensuresAnnotationIf);
        if (postcondAnno == null) {
            return result;
        }
        boolean annoResult =
                AnnotationUtils.getElementValue(
                        ensuresAnnotationIf, "result", Boolean.class, false);
        for (String expr : expressions) {
            result.add(new ConditionalPostcondition(expr, annoResult, postcondAnno));
        }
        return result;
    }

    // private constructor
    private ContractsUtils(GenericAnnotatedTypeFactory<?, ?, ?, ?> factory) {
        this.factory = factory;
    }
}
