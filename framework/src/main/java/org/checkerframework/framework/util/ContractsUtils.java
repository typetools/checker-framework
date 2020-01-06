package org.checkerframework.framework.util;

import static org.checkerframework.framework.util.Contract.Kind.CONDITIONALPOSTCONDITION;
import static org.checkerframework.framework.util.Contract.Kind.POSTCONDITION;
import static org.checkerframework.framework.util.Contract.Kind.PRECONDITION;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.Contract.Kind;
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
     * Returns all the contracts on method or constructor {@code executableElement}.
     *
     * @param executableElement the method or constructor whose contracts to retrieve
     * @return the contracts on {@code executableElement}
     */
    public Set<Contract> getContracts(ExecutableElement executableElement) {
        Set<Contract> contracts = new LinkedHashSet<>();
        contracts.addAll(getPreconditions(executableElement));
        contracts.addAll(getPostconditions(executableElement));
        contracts.addAll(getConditionalPostconditions(executableElement));
        return contracts;
    }

    /// Precondition methods (keep in sync with other two types)

    /**
     * Returns the contracts on method or constructor {@code executableElement}.
     *
     * @param executableElement the method whose contracts to return
     * @return the contracts on {@code executableElement}
     */
    public Set<Contract.Precondition> getPreconditions(ExecutableElement executableElement) {
        return getContracts(executableElement, PRECONDITION, Contract.Precondition.class);
    }

    /// Postcondition methods (keep in sync with other two types)

    /**
     * Returns the contracts on {@code executableElement}.
     *
     * @param executableElement the method whose contracts to return
     * @return the contracts on {@code executableElement}
     */
    public Set<Contract.Postcondition> getPostconditions(ExecutableElement executableElement) {
        return getContracts(executableElement, POSTCONDITION, Contract.Postcondition.class);
    }

    /// Conditional postcondition methods (keep in sync with other two types)

    /**
     * Returns the contracts on method {@code methodElement}.
     *
     * @param methodElement the method whose contracts to return
     * @return the contracts on {@code methodElement}
     */
    public Set<Contract.ConditionalPostcondition> getConditionalPostconditions(
            ExecutableElement methodElement) {
        return getContracts(
                methodElement, CONDITIONALPOSTCONDITION, Contract.ConditionalPostcondition.class);
    }

    /// Helper methods

    /**
     * Returns the contracts expressed by the given framework contract annotation.
     *
     * @param contractAnnotation a {@link RequiresQualifier}, {@link EnsuresQualifier}, {@link
     *     EnsuresQualifierIf}, or null
     * @param kind the kind of {@code contractAnnotation}
     * @param clazz the class to determine the return type
     * @param <T> the specific type of {@link Contract} to use
     * @return the contracts expressed by the given annotation, or the empty set if the argument is
     *     null
     */
    private <T extends Contract> Set<T> getContract(
            Contract.Kind kind, AnnotationMirror contractAnnotation, Class<T> clazz) {
        if (contractAnnotation == null) {
            return Collections.emptySet();
        }
        AnnotationMirror enforcedQualifier =
                getQualifierEnforcedByContractAnnotation(contractAnnotation);
        if (enforcedQualifier == null) {
            return Collections.emptySet();
        }
        Set<T> result = new LinkedHashSet<>();
        List<String> expressions =
                AnnotationUtils.getElementValueArray(
                        contractAnnotation, "expression", String.class, false);
        Boolean annoResult =
                AnnotationUtils.getElementValueOrNull(
                        contractAnnotation, "result", Boolean.class, false);
        for (String expr : expressions) {
            T contract =
                    clazz.cast(
                            Contract.create(
                                    kind, expr, enforcedQualifier, contractAnnotation, annoResult));
            result.add(contract);
        }
        return result;
    }

    /**
     * Returns the contracts on method or constructor {@code executableElement}.
     *
     * @param executableElement the method whose contracts to return
     * @param kind the kind of contracts to retrieve
     * @param clazz the class to determine the return type
     * @param <T> the specific type of {@link Contract} to use
     * @return the contracts on {@code executableElement}
     */
    private <T extends Contract> Set<T> getContracts(
            ExecutableElement executableElement, Kind kind, Class<T> clazz) {
        Set<T> result = new LinkedHashSet<>();
        // Check for a single contract annotation.
        AnnotationMirror frameworkContractAnno =
                factory.getDeclAnnotation(executableElement, kind.frameworkContractClass);
        result.addAll(getContract(kind, frameworkContractAnno, clazz));

        // Check for a wrapper around contract annotations.
        AnnotationMirror frameworkContractAnnos =
                factory.getDeclAnnotation(executableElement, kind.frameworkContractsClass);
        if (frameworkContractAnnos != null) {
            List<AnnotationMirror> frameworkContractAnnoList =
                    AnnotationUtils.getElementValueArray(
                            frameworkContractAnnos, "value", AnnotationMirror.class, false);
            for (AnnotationMirror a : frameworkContractAnnoList) {
                result.addAll(getContract(kind, a, clazz));
            }
        }

        // Check for type-system specific annotations.
        List<Pair<AnnotationMirror, AnnotationMirror>> declAnnotations =
                factory.getDeclAnnotationWithMetaAnnotation(executableElement, kind.metaAnnotation);
        for (Pair<AnnotationMirror, AnnotationMirror> r : declAnnotations) {
            AnnotationMirror anno = r.first;
            AnnotationMirror contractAnno = r.second;
            AnnotationMirror enforcedQualifier =
                    getQualifierEnforcedByContractAnnotation(contractAnno, anno);
            if (enforcedQualifier == null) {
                continue;
            }
            List<String> expressions =
                    AnnotationUtils.getElementValueArray(
                            anno, kind.expressionElementName, String.class, false);
            Boolean annoResult =
                    AnnotationUtils.getElementValueOrNull(anno, "result", Boolean.class, false);
            for (String expr : expressions) {
                T contract =
                        clazz.cast(
                                Contract.create(kind, expr, enforcedQualifier, anno, annoResult));
                result.add(contract);
            }
        }
        return result;
    }

    /**
     * Returns the annotation mirror as specified by the {@code qualifier} element in {@code
     * contractAnno}. May return null.
     *
     * @param contractAnno a pre- or post-condition annotation
     * @return the type annotation specified in {@code contractAnno.qualifier}
     */
    private AnnotationMirror getQualifierEnforcedByContractAnnotation(
            AnnotationMirror contractAnno) {
        return getQualifierEnforcedByContractAnnotation(contractAnno, null, null);
    }

    /**
     * Returns the annotation mirror as specified by the {@code qualifier} element in {@code
     * contractAnno}, with arguments taken from {@code argumentAnno}. May return null.
     *
     * @param contractAnno a pre- or post-condition annotation
     * @param argumentAnno supplies the elements/fields in the return value
     * @return the type annotation specified in {@code contractAnno.qualifier}
     */
    private AnnotationMirror getQualifierEnforcedByContractAnnotation(
            AnnotationMirror contractAnno, AnnotationMirror argumentAnno) {

        Map<String, String> argumentRenaming =
                makeArgumentRenaming(argumentAnno.getAnnotationType().asElement());
        return getQualifierEnforcedByContractAnnotation(
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
    private AnnotationMirror getQualifierEnforcedByContractAnnotation(
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
