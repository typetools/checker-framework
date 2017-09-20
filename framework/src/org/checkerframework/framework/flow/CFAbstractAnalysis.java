package org.checkerframework.framework.flow;

import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
*/

/**
 * {@link CFAbstractAnalysis} is an extensible org.checkerframework.dataflow analysis for the
 * Checker Framework that tracks the annotations using a flow-sensitive analysis. It uses an {@link
 * AnnotatedTypeFactory} to provide checker-specific logic how to combine types (e.g., what is the
 * type of a string concatenation, given the types of the two operands) and as an abstraction
 * function (e.g., determine the annotations on literals).
 *
 * <p>The purpose of this class is twofold: Firstly, it serves as factory for abstract values,
 * stores and the transfer function. Furthermore, it makes it easy for the transfer function and the
 * stores to access the {@link AnnotatedTypeFactory}, the qualifier hierarchy, etc.
 *
 * @author Charlie Garrett
 * @author Stefan Heule
 */
public abstract class CFAbstractAnalysis<
                V extends CFAbstractValue<V>,
                S extends CFAbstractStore<V, S>,
                T extends CFAbstractTransfer<V, S, T>>
        extends Analysis<V, S, T> {
    /** The qualifier hierarchy for which to track annotations. */
    protected final QualifierHierarchy qualifierHierarchy;

    /** The type hierarchy. */
    protected final TypeHierarchy typeHierarchy;

    /** A type factory that can provide static type annotations for AST Trees. */
    protected final GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>>
            atypeFactory;

    /**
     * A checker used to do error reporting. TODO: if it's only for error reporting, should it be an
     * (extended) ErrorHandler?
     */
    protected final SourceChecker checker;

    /** Initial abstract types for fields. */
    protected final List<Pair<VariableElement, V>> fieldValues;

    public CFAbstractAnalysis(
            BaseTypeChecker checker,
            GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory,
            List<Pair<VariableElement, V>> fieldValues,
            int maxCountBeforeWidening) {
        super(checker.getProcessingEnvironment(), null, maxCountBeforeWidening);
        qualifierHierarchy = factory.getQualifierHierarchy();
        typeHierarchy = factory.getTypeHierarchy();
        this.atypeFactory = factory;
        this.checker = checker;
        this.transferFunction = createTransferFunction();
        this.fieldValues = fieldValues;
    }

    public CFAbstractAnalysis(
            BaseTypeChecker checker,
            GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory,
            List<Pair<VariableElement, V>> fieldValues) {
        this(
                checker,
                factory,
                fieldValues,
                factory.getQualifierHierarchy().numberOfIterationsBeforeWidening());
    }

    public List<Pair<VariableElement, V>> getFieldValues() {
        return fieldValues;
    }

    /** @return the transfer function to be used by the analysis */
    public T createTransferFunction() {
        return atypeFactory.createFlowTransferFunction(this);
    }

    /** @return an empty store of the appropriate type */
    public abstract S createEmptyStore(boolean sequentialSemantics);

    /** @return an identical copy of the store {@code s}. */
    public abstract S createCopiedStore(S s);

    /**
     * Creates an abstract value from the annotated type mirror. The value contains the set of
     * primary annotations on the type; unless, the type is an AnnotatedWildcardType. In that case,
     * the annotations in the created value are the primary annotations on the extends bound. See
     * {@link CFAbstractValue} for an explanation.
     *
     * @return an abstract value containing the given annotated {@code type}.
     */
    public /*@Nullable*/ V createAbstractValue(AnnotatedTypeMirror type) {
        Set<AnnotationMirror> annos;
        if (type.getKind() == TypeKind.WILDCARD) {
            annos = ((AnnotatedWildcardType) type).getExtendsBound().getAnnotations();
        } else {
            annos = type.getAnnotations();
        }
        return createAbstractValue(annos, type.getUnderlyingType());
    }

    /**
     * @return an abstract value containing the given {@code annotations} and {@code
     *     underlyingType}.
     */
    public abstract /*@Nullable*/ V createAbstractValue(
            Set<AnnotationMirror> annotations, TypeMirror underlyingType);

    /** Default implementation for {@link #createAbstractValue(Set, TypeMirror)} */
    public CFValue defaultCreateAbstractValue(
            CFAbstractAnalysis<CFValue, ?, ?> analysis,
            Set<AnnotationMirror> annotations,
            TypeMirror underlyingType) {
        if (!CFAbstractValue.validateSet(annotations, underlyingType, qualifierHierarchy)) {
            return null;
        }
        return new CFValue(analysis, annotations, underlyingType);
    }

    public TypeHierarchy getTypeHierarchy() {
        return typeHierarchy;
    }

    public GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>>
            getTypeFactory() {
        return atypeFactory;
    }

    /** Perform a visualization of the CFG and analysis info for inspection. */
    public void visualizeCFG() {
        atypeFactory.getCFGVisualizer().visualize(cfg, cfg.getEntryBlock(), this);
    }

    /**
     * Returns an abstract value containing an annotated type with the annotation {@code anno}, and
     * 'top' for all other hierarchies. The underlying type is {@link Object}.
     */
    public V createSingleAnnotationValue(AnnotationMirror anno, TypeMirror underlyingType) {
        QualifierHierarchy hierarchy = getTypeFactory().getQualifierHierarchy();
        Set<AnnotationMirror> annos = AnnotationUtils.createAnnotationSet();
        annos.addAll(hierarchy.getTopAnnotations());
        AnnotationMirror f = hierarchy.findAnnotationInSameHierarchy(annos, anno);
        annos.remove(f);
        annos.add(anno);
        return createAbstractValue(annos, underlyingType);
    }

    /** @see GenericAnnotatedTypeFactory#getTypeFactoryOfSubchecker(Class) */
    @SuppressWarnings("TypeParameterUnusedInFormals") // Intentional abuse
    public <W extends GenericAnnotatedTypeFactory<?, ?, ?, ?>, U extends BaseTypeChecker>
            W getTypeFactoryOfSubchecker(Class<U> checkerClass) {
        return atypeFactory.getTypeFactoryOfSubchecker(checkerClass);
    }
}
