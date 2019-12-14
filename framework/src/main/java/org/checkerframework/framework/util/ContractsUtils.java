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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;
import org.checkerframework.framework.qual.RequiresQualifier;
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
 * @see ConditionalPostconditionAnnotation
 * @see EnsuresQualifierIf
 */
// TODO: This class assumes that most annotations have a field named "expression".
// If not, issue a more helpful error message.
public class ContractsUtils {

    /**
     * The currently-used ContractsUtils object. This class is NOT a singleton: this value can
     * change.
     */
    protected static ContractsUtils instance;

    /** The factory that this ContractsUtils is associated with. */
    protected GenericAnnotatedTypeFactory<?, ?, ?, ?> factory;

    /** Creates a ContractsUtils for the given factory. */
    private ContractsUtils(GenericAnnotatedTypeFactory<?, ?, ?, ?> factory) {
        this.factory = factory;
    }

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
     *
     * @see Precondition
     * @see Postcondition
     * @see ConditionalPostcondition
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
         *
         * <p>An annotation like {@code @RequiresNonNull({"a", "b", "c"})} would be represented by
         * multiple Contracts.
         */
        public final String expression;

        /** The annotation that must be on the type of expression, according to this contract. */
        public final AnnotationMirror annotation;

        /** The annotation that expressed this contract; used for diagnostic messages. */
        public final AnnotationMirror contractAnnotation;

        // This is redundant with the contract's class  and is not used in this file, but the field
        // is used by clients.
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

        // Note that equality requires exact match of the run-time class and that it ignores the
        // `contractAnnotation` field.
        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Contract contract = (Contract) o;

            return Objects.equals(expression, contract.expression)
                    && Objects.equals(annotation, contract.annotation)
                    && kind == contract.kind;
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
        public boolean equals(@Nullable Object o) {
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

    /**
     * Returns all the contracts on {@code methodElement}.
     *
     * @param methodElement the method or constructor whose contracts to retrieve
     * @return the contracts on {@code methodElement}
     */
    public List<Contract> getContracts(ExecutableElement methodElement) {
        List<Contract> contracts = new ArrayList<>();
        contracts.addAll(getPreconditions(methodElement));
        contracts.addAll(getPostconditions(methodElement));
        contracts.addAll(getConditionalPostconditions(methodElement));
        return contracts;
    }

    /// Precondition methods (keep in sync with other two types)

    /**
     * Returns the preconditions on {@code methodElement}.
     *
     * @param methodElement the method or contract whose preconditions to return
     * @return the preconditions on {@code methodElement}
     */
    public Set<Precondition> getPreconditions(ExecutableElement methodElement) {
        Set<Precondition> result = new LinkedHashSet<>();
        // Check for a single contract annotation.
        AnnotationMirror requiresQualifier =
                factory.getDeclAnnotation(methodElement, RequiresQualifier.class);
        result.addAll(getPrecondition(requiresQualifier));

        // Check for a wrapper around contract annotations.
        AnnotationMirror requiresQualifierListAnno =
                factory.getDeclAnnotation(methodElement, RequiresQualifier.List.class);
        if (requiresQualifierListAnno != null) {
            List<AnnotationMirror> requiresQualifierList =
                    AnnotationUtils.getElementValueArray(
                            requiresQualifierListAnno, "value", AnnotationMirror.class, false);
            for (AnnotationMirror a : requiresQualifierList) {
                result.addAll(getPrecondition(a));
            }
        }

        // Check for type-system specific annotations.
        List<Pair<AnnotationMirror, AnnotationMirror>> declAnnotations =
                factory.getDeclAnnotationWithMetaAnnotation(
                        methodElement, PreconditionAnnotation.class);
        for (Pair<AnnotationMirror, AnnotationMirror> r : declAnnotations) {
            AnnotationMirror anno = r.first;
            AnnotationMirror contractAnno = r.second;
            AnnotationMirror precondAnno =
                    getAnnotationMirrorOfContractAnnotation(contractAnno, anno);
            if (precondAnno == null) {
                continue;
            }
            List<String> expressions =
                    AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
            for (String expr : expressions) {
                result.add(new Precondition(expr, precondAnno, anno));
            }
        }
        return result;
    }

    /**
     * Returns the preconditions expressed by the given annotation.
     *
     * @param requiresQualifier a {@link RequiresQualifier}, or null
     * @return the preconditions expressed by the given annotation, or the empty set if the argument
     *     is null
     */
    private Set<Precondition> getPrecondition(AnnotationMirror requiresQualifier) {
        if (requiresQualifier == null) {
            return Collections.emptySet();
        }
        AnnotationMirror precondAnno = getAnnotationMirrorOfContractAnnotation(requiresQualifier);
        if (precondAnno == null) {
            return Collections.emptySet();
        }
        Set<Precondition> result = new LinkedHashSet<>();
        List<String> expressions =
                AnnotationUtils.getElementValueArray(
                        requiresQualifier, "expression", String.class, false);
        for (String expr : expressions) {
            result.add(new Precondition(expr, precondAnno, requiresQualifier));
        }
        return result;
    }

    /// Postcondition methods (keep in sync with other two types)

    /**
     * Returns the unconditional postconditions on {@code methodElement}.
     *
     * @param methodElement the method or contract whose postconditions to return
     * @return the postconditions on {@code methodElement}
     */
    public Set<Postcondition> getPostconditions(ExecutableElement methodElement) {
        Set<Postcondition> result = new LinkedHashSet<>();
        // Check for a single contract annotation.
        AnnotationMirror ensuresQualifier =
                factory.getDeclAnnotation(methodElement, EnsuresQualifier.class);
        result.addAll(getPostcondition(ensuresQualifier));

        // Check for a wrapper around contract annotations.
        AnnotationMirror ensuresQualifiers =
                factory.getDeclAnnotation(methodElement, EnsuresQualifier.List.class);
        if (ensuresQualifiers != null) {
            List<AnnotationMirror> ensuresQualifiersList =
                    AnnotationUtils.getElementValueArray(
                            ensuresQualifiers, "value", AnnotationMirror.class, false);
            for (AnnotationMirror a : ensuresQualifiersList) {
                result.addAll(getPostcondition(a));
            }
        }

        // Check for type-system specific annotations.
        List<Pair<AnnotationMirror, AnnotationMirror>> declAnnotations =
                factory.getDeclAnnotationWithMetaAnnotation(
                        methodElement, PostconditionAnnotation.class);
        for (Pair<AnnotationMirror, AnnotationMirror> r : declAnnotations) {
            AnnotationMirror anno = r.first;
            AnnotationMirror contractAnno = r.second;
            AnnotationMirror postcondAnno =
                    getAnnotationMirrorOfContractAnnotation(contractAnno, anno);
            if (postcondAnno == null) {
                continue;
            }
            List<String> expressions =
                    AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
            for (String expr : expressions) {
                result.add(new Postcondition(expr, postcondAnno, anno));
            }
        }
        return result;
    }

    /**
     * Returns the unconditional postconditions expressed by the given annotation.
     *
     * @param ensuresQualifier an {@link EnsuresQualifier}, or null
     * @return the postconditions expressed by the given annotation, or the empty set if the
     *     argument is null
     */
    private Set<Postcondition> getPostcondition(AnnotationMirror ensuresQualifier) {
        if (ensuresQualifier == null) {
            return Collections.emptySet();
        }
        AnnotationMirror postcondAnno = getAnnotationMirrorOfContractAnnotation(ensuresQualifier);
        if (postcondAnno == null) {
            return Collections.emptySet();
        }
        Set<Postcondition> result = new LinkedHashSet<>();
        List<String> expressions =
                AnnotationUtils.getElementValueArray(
                        ensuresQualifier, "expression", String.class, false);
        for (String expr : expressions) {
            result.add(new Postcondition(expr, postcondAnno, ensuresQualifier));
        }
        return result;
    }

    /// Conditional postcondition methods (keep in sync with other two types)

    /**
     * Returns the conditional postconditions on {@code methodElement}.
     *
     * @param methodElement the method or contract whose unconditional postconditions to return
     * @return the unconditional postconditions on {@code methodElement}
     */
    public Set<ConditionalPostcondition> getConditionalPostconditions(
            ExecutableElement methodElement) {
        Set<ConditionalPostcondition> result = new LinkedHashSet<>();
        // Check for a single contract annotation.
        AnnotationMirror ensuresQualifierIf =
                factory.getDeclAnnotation(methodElement, EnsuresQualifierIf.class);
        result.addAll(getConditionalPostcondition(ensuresQualifierIf));

        // Check for a wrapper around contract annotations.
        AnnotationMirror ensuresQualifiersIf =
                factory.getDeclAnnotation(methodElement, EnsuresQualifierIf.List.class);
        if (ensuresQualifiersIf != null) {
            List<AnnotationMirror> ensuresQualifiersIfList =
                    AnnotationUtils.getElementValueArray(
                            ensuresQualifiersIf, "value", AnnotationMirror.class, false);
            for (AnnotationMirror a : ensuresQualifiersIfList) {
                result.addAll(getConditionalPostcondition(a));
            }
        }

        // Check for type-system specific annotations.
        List<Pair<AnnotationMirror, AnnotationMirror>> declAnnotations =
                factory.getDeclAnnotationWithMetaAnnotation(
                        methodElement, ConditionalPostconditionAnnotation.class);
        for (Pair<AnnotationMirror, AnnotationMirror> r : declAnnotations) {
            AnnotationMirror anno = r.first;
            AnnotationMirror contractAnno = r.second;
            AnnotationMirror postcondAnno =
                    getAnnotationMirrorOfContractAnnotation(contractAnno, anno);
            if (postcondAnno == null) {
                continue;
            }
            List<String> expressions =
                    AnnotationUtils.getElementValueArray(anno, "expression", String.class, false);
            boolean annoResult =
                    AnnotationUtils.getElementValue(anno, "result", Boolean.class, false);
            for (String expr : expressions) {
                result.add(new ConditionalPostcondition(expr, annoResult, postcondAnno, anno));
            }
        }
        return result;
    }

    /**
     * Returns the conditional postconditions expressed by the given annotation.
     *
     * @param ensuresQualifierIf an {@link EnsuresQualifierIf}, or null
     * @return the postconditions expressed by the given annotation, or the empty set if the
     *     argument is null
     */
    private Set<ConditionalPostcondition> getConditionalPostcondition(
            AnnotationMirror ensuresQualifierIf) {
        if (ensuresQualifierIf == null) {
            return Collections.emptySet();
        }
        AnnotationMirror postcondAnno = getAnnotationMirrorOfContractAnnotation(ensuresQualifierIf);
        if (postcondAnno == null) {
            return Collections.emptySet();
        }
        Set<ConditionalPostcondition> result = new LinkedHashSet<>();
        List<String> expressions =
                AnnotationUtils.getElementValueArray(
                        ensuresQualifierIf, "expression", String.class, false);
        boolean annoResult =
                AnnotationUtils.getElementValue(ensuresQualifierIf, "result", Boolean.class, false);
        for (String expr : expressions) {
            result.add(
                    new ConditionalPostcondition(
                            expr, annoResult, postcondAnno, ensuresQualifierIf));
        }
        return result;
    }

    /// Helper methods

    /**
     * Returns the annotation mirror as specified by the {@code qualifier} element in {@code
     * contractAnno}. May return null.
     *
     * @param contractAnno a pre- or post-condition annotation
     * @return the type annotation specified in {@code contractAnno.qualifier}
     */
    private AnnotationMirror getAnnotationMirrorOfContractAnnotation(
            AnnotationMirror contractAnno) {
        return getAnnotationMirrorOfContractAnnotation(contractAnno, null, null);
    }

    /**
     * Returns the annotation mirror as specified by the {@code qualifier} element in {@code
     * contractAnno}, with arguments taken from {@code argumentAnno}. May return null.
     *
     * @param contractAnno a pre- or post-condition annotation
     * @param argumentAnno supplies the elements/fields in the return value
     * @return the type annotation specified in {@code contractAnno.qualifier}
     */
    private AnnotationMirror getAnnotationMirrorOfContractAnnotation(
            AnnotationMirror contractAnno, AnnotationMirror argumentAnno) {

        Map<String, String> argumentRenaming =
                makeArgumentRenaming(argumentAnno.getAnnotationType().asElement());
        return getAnnotationMirrorOfContractAnnotation(
                contractAnno, argumentAnno, argumentRenaming);
    }

    /**
     * Returns the annotation mirror as specified by the "qualifier" element in {@code
     * contractAnno}. If {@code argumentAnno} is specified, then arguments are copied from {@code
     * argumentAnno} to the returned annotation, renamed according to {@code argumentRenaming}.
     *
     * <p>This is a helper method. Use one of its overloads if possible.
     *
     * @param contractAnno a contract annotation, which has a {@code qualifier} element
     * @param argumentAnno annotation containing the argument values, or {@code null}
     * @param argumentRenaming renaming of argument names, which maps from names in {@code
     *     argumentAnno} to names used in the returned annotation, or {@code null}
     * @return a qualifier whose type is that of {@code contract.qualifier}, or an alias for it, or
     *     null if it is not a supported qualifier of the type system
     */
    private AnnotationMirror getAnnotationMirrorOfContractAnnotation(
            AnnotationMirror contractAnno,
            AnnotationMirror argumentAnno,
            Map<String, String> argumentRenaming) {

        Name c = AnnotationUtils.getElementValueClassName(contractAnno, "qualifier", false);

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
    private Map<String, String> makeArgumentRenaming(Element contractAnnoElement) {
        HashMap<String, String> argumentRenaming = new HashMap<>();
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
                argumentRenaming.put(sourceName, targetName);
            }
        }
        return argumentRenaming;
    }
}
