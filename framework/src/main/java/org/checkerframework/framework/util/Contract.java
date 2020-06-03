package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.Objects;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.javacutil.BugInCF;

/**
 * A contract represents an annotation on an expression. It is a precondition, postcondition, or
 * conditional postcondition.
 *
 * @see Precondition
 * @see Postcondition
 * @see ConditionalPostcondition
 */
public abstract class Contract {

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
     * @param kind precondition, postcondition, or conditional postcondition
     * @param expression the Java expression that should have a type qualifier
     * @param annotation the type qualifier that {@code expression} should have
     * @param contractAnnotation the pre- or post-condition annotation that the programmer wrote;
     *     used for diagnostic messages
     */
    protected Contract(
            Kind kind,
            String expression,
            AnnotationMirror annotation,
            AnnotationMirror contractAnnotation) {
        this.expression = expression;
        this.annotation = annotation;
        this.contractAnnotation = contractAnnotation;
        this.kind = kind;
    }

    /**
     * Creates a new Contract.
     *
     * @param expression the Java expression that should have a type qualifier
     * @param annotation the type qualifier that {@code expression} should have
     * @param contractAnnotation the pre- or post-condition annotation that the programmer wrote;
     *     used for diagnostic messages
     * @param kind precondition, postcondition, or conditional postcondition
     * @param ensuresQualifierIf the ensuresQualifierIf field, for a conditional postcondition
     * @return a new contract
     */
    public static Contract create(
            Kind kind,
            String expression,
            AnnotationMirror annotation,
            AnnotationMirror contractAnnotation,
            Boolean ensuresQualifierIf) {
        if ((ensuresQualifierIf != null) != (kind == Kind.CONDITIONALPOSTCONDITION)) {
            throw new BugInCF("Mismatch: ensuresQualifierIf=%s, kind=%s", ensuresQualifierIf, kind);
        }
        switch (kind) {
            case PRECONDITION:
                return new Precondition(expression, annotation, contractAnnotation);
            case POSTCONDITION:
                return new Postcondition(expression, annotation, contractAnnotation);
            case CONDITIONALPOSTCONDITION:
                return new ConditionalPostcondition(
                        expression, annotation, contractAnnotation, ensuresQualifierIf);
            default:
                throw new BugInCF("Unrecognized kind: " + kind);
        }
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

        return kind == contract.kind
                && Objects.equals(expression, contract.expression)
                && Objects.equals(annotation, contract.annotation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, expression, annotation);
    }

    @Override
    public String toString() {
        return String.format(
                "%s{expression=%s, annotation=%s, contractAnnotation=%s}",
                getClass().getSimpleName(), expression, annotation, contractAnnotation);
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
            super(Kind.PRECONDITION, expression, annotation, contractAnnotation);
        }
    }

    /** Enumerates the kinds of contracts. */
    public enum Kind {
        /** A precondition. */
        PRECONDITION(
                "precondition",
                PreconditionAnnotation.class,
                RequiresQualifier.class,
                RequiresQualifier.List.class,
                "value"),
        /** A postcondition. */
        POSTCONDITION(
                "postcondition",
                PostconditionAnnotation.class,
                EnsuresQualifier.class,
                EnsuresQualifier.List.class,
                "value"),
        /** A conditional postcondition. */
        CONDITIONALPOSTCONDITION(
                "conditional.postcondition",
                ConditionalPostconditionAnnotation.class,
                EnsuresQualifierIf.class,
                EnsuresQualifierIf.List.class,
                "expression");
        /** Used for constructing error messages. */
        public final String errorKey;

        /** The meta-annotation identifying annotations of this kind. */
        public final Class<? extends Annotation> metaAnnotation;
        /** The built-in framework qualifier for this contract. */
        public final Class<? extends Annotation> frameworkContractClass;
        /** The built-in framework qualifier for repeated occurrences of this contract. */
        public final Class<? extends Annotation> frameworkContractsClass;
        /**
         * The name of the element that contains the Java expressions on which a contract is
         * enforced.
         */
        public final String expressionElementName;

        /**
         * Create a new Kind.
         *
         * @param errorKey used for constructing error messages
         * @param metaAnnotation the meta-annotation identifying annotations of this kind
         * @param frameworkContractClass the built-in framework qualifier for this contract
         * @param frameworkContractsClass the built-in framework qualifier for repeated occurrences
         *     of this contract
         * @param expressionElementName the name of the element that contains the Java expressions
         *     on which a contract is enforced
         */
        Kind(
                String errorKey,
                Class<? extends Annotation> metaAnnotation,
                Class<? extends Annotation> frameworkContractClass,
                Class<? extends Annotation> frameworkContractsClass,
                String expressionElementName) {
            this.errorKey = errorKey;
            this.metaAnnotation = metaAnnotation;
            this.frameworkContractClass = frameworkContractClass;
            this.frameworkContractsClass = frameworkContractsClass;
            this.expressionElementName = expressionElementName;
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
            super(Kind.POSTCONDITION, expression, annotation, contractAnnotation);
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
        public final boolean resultValue;

        /**
         * Create a new conditional postcondition.
         *
         * @param expression the Java expression that should have a type qualifier
         * @param annotation the type qualifier that {@code expression} should have
         * @param contractAnnotation the postcondition annotation that the programmer wrote; used
         *     for diagnostic messages
         * @param resultValue whether the condition is the method returning true or false
         */
        public ConditionalPostcondition(
                String expression,
                AnnotationMirror annotation,
                AnnotationMirror contractAnnotation,
                boolean resultValue) {
            super(Kind.CONDITIONALPOSTCONDITION, expression, annotation, contractAnnotation);
            this.resultValue = resultValue;
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
            return resultValue == that.resultValue;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), resultValue);
        }

        @Override
        public String toString() {
            String superToString = super.toString();
            return superToString.substring(0, superToString.length() - 1)
                    + ", annoResult="
                    + resultValue
                    + "}";
        }
    }
}
