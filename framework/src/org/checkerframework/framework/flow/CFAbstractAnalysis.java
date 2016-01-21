package org.checkerframework.framework.flow;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.Pair;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
*/

/**
 * {@link CFAbstractAnalysis} is an extensible org.checkerframework.dataflow analysis for the Checker
 * Framework that tracks the annotations using a flow-sensitive analysis. It
 * uses an {@link AnnotatedTypeFactory} to provide checker-specific logic how to
 * combine types (e.g., what is the type of a string concatenation, given the
 * types of the two operands) and as an abstraction function (e.g., determine
 * the annotations on literals).
 *
 * <p>
 * The purpose of this class is twofold: Firstly, it serves as factory for
 * abstract values, stores and the transfer function. Furthermore, it makes it
 * easy for the transfer function and the stores to access the
 * {@link AnnotatedTypeFactory}, the qualifier hierarchy, etc.
 *
 * @author Charlie Garrett
 * @author Stefan Heule
 *
 */
public abstract class CFAbstractAnalysis<V extends CFAbstractValue<V>,
            S extends CFAbstractStore<V, S>,
            T extends CFAbstractTransfer<V, S, T>>
        extends Analysis<V, S, T> {
    /**
     * The qualifier hierarchy for which to track annotations.
     */
    protected final QualifierHierarchy qualifierHierarchy;

    /**
     * The type hierarchy.
     */
    protected final TypeHierarchy typeHierarchy;

    /**
     * A type factory that can provide static type annotations for AST Trees.
     */
    protected final GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>> atypeFactory;

    /**
     * A checker used to do error reporting.
     * TODO: if it's only for error reporting, should it be an (extended) ErrorHandler?
     */
    protected final SourceChecker checker;

    /**
     * Initial abstract types for fields.
     */
    protected final List<Pair<VariableElement, V>> fieldValues;

    public CFAbstractAnalysis(BaseTypeChecker checker,
            GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory,
            List<Pair<VariableElement, V>> fieldValues) {
        super(checker.getProcessingEnvironment());

        qualifierHierarchy = factory.getQualifierHierarchy();
        typeHierarchy = factory.getTypeHierarchy();
        this.atypeFactory = factory;
        this.checker = checker;
        this.transferFunction = createTransferFunction();
        this.fieldValues = fieldValues;
    }

    public List<Pair<VariableElement, V>> getFieldValues() {
        return fieldValues;
    }

    /**
     * @return The transfer function to be used by the analysis.
     */
    public T createTransferFunction() {
        return atypeFactory.createFlowTransferFunction(this);
    }

    /**
     * @return An empty store of the appropriate type.
     */
    public abstract S createEmptyStore(boolean sequentialSemantics);

    /**
     * @return An identical copy of the store {@code s}.
     */
    public abstract S createCopiedStore(S s);

    /**
     * @return An abstract value containing the given annotated {@code type}.
     */
    public abstract /*@Nullable*/ V createAbstractValue(AnnotatedTypeMirror type);

    /**
     * Default implementation for
     * {@link #createAbstractValue(AnnotatedTypeMirror)} that takes care of
     * invalid types.
     */
    public CFValue defaultCreateAbstractValue(
            CFAbstractAnalysis<CFValue, ?, ?> analysis, AnnotatedTypeMirror type) {
        if (!AnnotatedTypes.isValidType(qualifierHierarchy, type)) {
            // If the type is not valid, we return null, which is the same as
            // 'no information'.
            return null;
        }
        return new CFValue(analysis, type);
    }

    public TypeHierarchy getTypeHierarchy() {
        return typeHierarchy;
    }

    public GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>> getTypeFactory() {
        return atypeFactory;
    }

    /**
     * Perform a visualization of the CFG and analysis info for inspection.
     */
    public void visualizeCFG() {
        atypeFactory.getCFGVisualizer().visualize(cfg, cfg.getEntryBlock(), this);
    }

    /**
     * Returns an abstract value containing an annotated type with the
     * annotation {@code anno}, and 'top' for all other hierarchies. The
     * underlying type is {@link Object}.
     */
    public V createSingleAnnotationValue(AnnotationMirror anno,
            TypeMirror underlyingType) {
        AnnotatedTypeMirror type = AnnotatedTypeMirror.createType(
                underlyingType, getTypeFactory(), false);
        Set<? extends AnnotationMirror> tops = getTypeFactory().getQualifierHierarchy()
                .getTopAnnotations();
        makeTop(type, tops);
        type.replaceAnnotation(anno);
        return createAbstractValue(type);
    }

    /**
     * Adds top as the annotation on all locations of a given type.
     */
    private void makeTop(AnnotatedTypeMirror type, Set<? extends AnnotationMirror> tops) {
        TypeKind kind = type.getKind();
        if (kind == TypeKind.ARRAY) {
            AnnotatedArrayType a = (AnnotatedArrayType) type;
            makeTop(a.getComponentType(), tops);
        } else if (kind == TypeKind.TYPEVAR) {
            //just set the primary to top, this will override the upper/lower bounds

        } else if (kind == TypeKind.WILDCARD) {
            AnnotatedWildcardType a = (AnnotatedWildcardType) type;
            a.addAnnotations(tops);
            makeTop(a.getExtendsBound(), tops);
            if (a.getSuperBound() != null) {
                makeTop(a.getSuperBound(), tops);
            }
        }

        if (kind != TypeKind.WILDCARD) {
            // don't set top annotations, because [] is top
            type.addAnnotations(tops);
        }
    }

    /**
     * @see GenericAnnotatedTypeFactory#getTypeFactoryOfSubchecker(Class)
     */
    public <W extends GenericAnnotatedTypeFactory<?, ?, ?, ?>, U extends BaseTypeChecker> W getTypeFactoryOfSubchecker(Class<U> checkerClass) {
        return atypeFactory.getTypeFactoryOfSubchecker(checkerClass);
    }
}
