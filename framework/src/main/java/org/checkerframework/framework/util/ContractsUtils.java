package org.checkerframework.framework.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.EnsuresQualifiers;
import org.checkerframework.framework.qual.EnsuresQualifiersIf;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.framework.qual.RequiresQualifiers;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
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
 */
// TODO: This class assumes that most annotations have a field named "expression".
// If not, issue a more helpful error message rather than a crash.
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

        /** The annotation that expressed this contract; used for diagnostic messages. */
        public final AnnotationMirror contractAnnotation;

        /** The kind of contract: precondition, postcondition, or conditional postcondition. */
        public final Kind kind;

        /**
         * Creates a new Contract.
         *
         * @param expression the Java expression that should have a type qualifier
         * @param annotation the type qualifier that {@code expression} should have
         * @param contractAnnotation the pre- or post-condition annotation that the programmer
         *     wrote; used for diagnostic messages
         * @param kind precondition, postcondition, or conditional postcondition
         */
        public Contract(
                String expression,
                AnnotationMirror annotation,
                AnnotationMirror contractAnnotation,
                Kind kind) {
            this.expression = expression;
            this.annotation = annotation;
            this.contractAnnotation = contractAnnotation;
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
            return Objects.hash(expression, annotation, kind);
        }
    }

    /** A precondition contract. */
    public static class Precondition extends Contract {
        /**
         * Create a precondition contract.
         *
         * @param expression the Java expression that should have a type qualifier
         * @param annotation the type qualifier that {@code expression} should have
         * @param contractAnnotation the precondition annotation that the programmer wrote; used for
         *     diagnostic messages
         */
        public Precondition(
                String expression,
                AnnotationMirror annotation,
                AnnotationMirror contractAnnotation) {
            super(expression, annotation, contractAnnotation, Kind.PRECONDITION);
        }
    }

    /** A postcondition contract. */
    public static class Postcondition extends Contract {
        /**
         * Create a postcondition contract.
         *
         * @param expression the Java expression that should have a type qualifier
         * @param annotation the type qualifier that {@code expression} should have
         * @param contractAnnotation the postcondition annotation that the programmer wrote; used
         *     for diagnostic messages
         */
        public Postcondition(
                String expression,
                AnnotationMirror annotation,
                AnnotationMirror contractAnnotation) {
            super(expression, annotation, contractAnnotation, Kind.POSTCONDTION);
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

        /**
         * Create a new conditional postcondition.
         *
         * @param expression the Java expression that should have a type qualifier
         * @param annoResult whether the condition is the method returning true or false
         * @param annotation the type qualifier that {@code expression} should have
         * @param contractAnnotation the postcondition annotation that the programmer wrote; used
         *     for diagnostic messages
         */
        public ConditionalPostcondition(
                String expression,
                boolean annoResult,
                AnnotationMirror annotation,
                AnnotationMirror contractAnnotation) {
            super(expression, annotation, contractAnnotation, Kind.CONDITIONALPOSTCONDTION);
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
            return Objects.hash(super.hashCode(), annoResult);
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
            AnnotationMirror precondAnno = getAnnotationMirrorOfMetaAnnotation(metaAnno, anno);
            if (precondAnno == null) {
                continue;
            }
            for (String expr : expressions) {
                result.add(new Precondition(expr, precondAnno, anno));
            }
        }
        return result;
    }

    /**
     * Returns the annotation mirror as specified by the "qualifier" element in {@code
     * qualifierAnno}. If {@code argumentAnno} is specified, then arguments are copied from {@code
     * argumentAnno} to the returned annotation, renamed according to {@code argumentMap}.
     *
     * <p>This is a helper method intended to be called from {@link
     * getAnnotationMirrorOfContractAnnotation} and {@link getAnnotationMirrorOfMetaAnnotation}. Use
     * one of those methods if possible.
     *
     * @param qualifierAnno annotation specifying the qualifier class
     * @param argumentAnno annotation containing the argument values, or {@code null}
     * @param argumentRenaming renaming of argument names, which maps from names in {@code
     *     argumentAnno} to names used in the returned annotation, or {@code null}
     */
    private AnnotationMirror getAnnotationMirrorOfQualifier(
            AnnotationMirror qualifierAnno,
            AnnotationMirror argumentAnno,
            Map<String, String> argumentRenaming) {

        Name c = AnnotationUtils.getElementValueClassName(qualifierAnno, "qualifier", false);

        AnnotationMirror anno;
        if (argumentAnno == null || argumentRenaming.isEmpty()) {
            // If there are no arguments, use factory method that allows caching
            anno = AnnotationBuilder.fromName(factory.getElementUtils(), c);
        } else {
            AnnotationBuilder builder = new AnnotationBuilder(factory.getProcessingEnv(), c);
            builder.copyRenameElementValuesFromAnnotation(argumentAnno, argumentRenaming);
            anno = builder.build();
        }

        if (factory.isSupportedQualifier(anno)) {
            return anno;
        } else {
            AnnotationMirror aliasedAnno = factory.canonicalAnnotation(anno);
            if (factory.isSupportedQualifier(aliasedAnno)) {
                return aliasedAnno;
            } else {
                return null;
            }
        }
    }

    /**
     * Returns the annotation mirror as specified by the "qualifier" element in {@code
     * contractAnno}.
     */
    private AnnotationMirror getAnnotationMirrorOfContractAnnotation(
            AnnotationMirror contractAnno) {
        return getAnnotationMirrorOfQualifier(contractAnno, null, null);
    }

    /**
     * Makes a map from element names of a contract annotation to qualifier argument names, as
     * defined by {@link QualifierArgument}.
     *
     * <p>Each element of {@code contractAnnoElement} that is annotated by {@link QualifierArgument}
     * is mapped to the name specified by the value of {@link QualifierArgument}. If the value is
     * not specified or is an empty string, then the element is mapped to an argument of the same
     * name.
     *
     * @param contractAnnoElement the declaration of the contract annotation containing the elements
     * @return map from the names of elements of {@code sourceArgumentNames} to the corresponding
     *     qualifier argument names
     * @see QualifierArgument
     */
    private Map<String, String> makeArgumentMap(Element contractAnnoElement) {
        HashMap<String, String> argumentMap = new HashMap<>();
        for (ExecutableElement meth :
                ElementFilter.methodsIn(contractAnnoElement.getEnclosedElements())) {
            AnnotationMirror argumentAnnotation =
                    factory.getDeclAnnotation(meth, QualifierArgument.class);
            if (argumentAnnotation != null) {
                String sourceName = meth.getSimpleName().toString();
                String targetName =
                        AnnotationUtils.getElementValue(
                                argumentAnnotation, "value", String.class, false);
                if (targetName == null || targetName.isEmpty()) {
                    targetName = sourceName;
                }
                argumentMap.put(sourceName, targetName);
            }
        }
        return argumentMap;
    }

    /**
     * Returns the annotation mirror as specified by the "qualifier" element in {@code metaAnno},
     * with arguments taken from {@code argumentAnno}.
     */
    private AnnotationMirror getAnnotationMirrorOfMetaAnnotation(
            AnnotationMirror metaAnno, AnnotationMirror argumentAnno) {

        Map<String, String> argumentMap =
                makeArgumentMap(argumentAnno.getAnnotationType().asElement());
        return getAnnotationMirrorOfQualifier(metaAnno, argumentAnno, argumentMap);
    }

    /** Returns the set of preconditions according to the given {@link RequiresQualifier}. */
    private Set<Precondition> getPrecondition(AnnotationMirror requiresAnnotation) {
        if (requiresAnnotation == null) {
            return Collections.emptySet();
        }
        Set<Precondition> result = new LinkedHashSet<>();
        List<String> expressions =
                AnnotationUtils.getElementValueArray(
                        requiresAnnotation, "expression", String.class, false);
        AnnotationMirror precondAnno = getAnnotationMirrorOfContractAnnotation(requiresAnnotation);
        if (precondAnno == null) {
            return result;
        }
        for (String expr : expressions) {
            result.add(new Precondition(expr, precondAnno, requiresAnnotation));
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
            AnnotationMirror postcondAnno = getAnnotationMirrorOfMetaAnnotation(metaAnno, anno);
            if (postcondAnno == null) {
                continue;
            }
            for (String expr : expressions) {
                result.add(new Postcondition(expr, postcondAnno, anno));
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
        AnnotationMirror postcondAnno = getAnnotationMirrorOfContractAnnotation(ensuresAnnotation);
        if (postcondAnno == null) {
            return result;
        }
        for (String expr : expressions) {
            result.add(new Postcondition(expr, postcondAnno, ensuresAnnotation));
        }
        return result;
    }

    /**
     * Returns a set of triples {@code (expr, (result, annotation))} of conditional postconditions
     * on the method {@code methodElement}.
     */
    public Set<ConditionalPostcondition> getConditionalPostconditions(
            ExecutableElement methodElement) {
        Set<ConditionalPostcondition> result = new LinkedHashSet<>();
        // Check for a single contract.
        AnnotationMirror ensuresQualifierIf =
                factory.getDeclAnnotation(methodElement, EnsuresQualifierIf.class);
        result.addAll(getConditionalPostcondition(ensuresQualifierIf));

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
            AnnotationMirror postcondAnno = getAnnotationMirrorOfMetaAnnotation(metaAnno, anno);
            if (postcondAnno == null) {
                continue;
            }
            boolean annoResult =
                    AnnotationUtils.getElementValue(anno, "result", Boolean.class, false);
            for (String expr : expressions) {
                result.add(new ConditionalPostcondition(expr, annoResult, postcondAnno, anno));
            }
        }
        return result;
    }

    /**
     * Returns a set of triples {@code (expr, result, annotation)} of conditional postconditions
     * that are expressed in the source code using the given postcondition annotation.
     */
    private Set<ConditionalPostcondition> getConditionalPostcondition(
            AnnotationMirror ensuresQualifierIf) {
        if (ensuresQualifierIf == null) {
            return Collections.emptySet();
        }
        Set<ConditionalPostcondition> result = new LinkedHashSet<>();
        List<String> expressions =
                AnnotationUtils.getElementValueArray(
                        ensuresQualifierIf, "expression", String.class, false);
        AnnotationMirror postcondAnno = getAnnotationMirrorOfContractAnnotation(ensuresQualifierIf);
        if (postcondAnno == null) {
            return result;
        }
        boolean annoResult =
                AnnotationUtils.getElementValue(ensuresQualifierIf, "result", Boolean.class, false);
        for (String expr : expressions) {
            result.add(
                    new ConditionalPostcondition(
                            expr, annoResult, postcondAnno, ensuresQualifierIf));
        }
        return result;
    }

    // private constructor
    private ContractsUtils(GenericAnnotatedTypeFactory<?, ?, ?, ?> factory) {
        this.factory = factory;
    }
}
