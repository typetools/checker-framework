package checkers.flow;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;
import checkers.types.QualifierHierarchy;
import checkers.types.TypeHierarchy;
import checkers.util.AnnotatedTypes;

import dataflow.analysis.Analysis;
import dataflow.cfg.CFGDOTVisualizer;

import javacutils.Pair;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/*>>>
import checkers.nullness.quals.*;
*/

/**
 * {@link CFAbstractAnalysis} is an extensible dataflow analysis for the Checker
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
public abstract class CFAbstractAnalysis<V extends CFAbstractValue<V>, S extends CFAbstractStore<V, S>, T extends CFAbstractTransfer<V, S, T>>
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
    protected final AbstractBasicAnnotatedTypeFactory<? extends BaseTypeChecker<?>, V, S, T, ? extends CFAbstractAnalysis<V, S, T>> atypeFactory;

    /**
     * A checker used to do error reporting.
     * TODO: if it's only for error reporting, should it be an (extended) ErrorHandler?
     */
    protected final BaseTypeChecker<?> checker;

    // TODO: document.
    protected final List<Pair<VariableElement, V>> fieldValues;

    // TODO: document.
    protected final int expectedNumberOfAnnotations;

    public <Checker extends BaseTypeChecker<?>> CFAbstractAnalysis(
            AbstractBasicAnnotatedTypeFactory<Checker, V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory,
            ProcessingEnvironment env, Checker checker) {
        this(factory, env, checker, Collections.<Pair<VariableElement, V>>emptyList());
    }

    public <Checker extends BaseTypeChecker<?>> CFAbstractAnalysis(
            AbstractBasicAnnotatedTypeFactory<Checker, V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory,
            ProcessingEnvironment env, Checker checker,
            List<Pair<VariableElement, V>> fieldValues) {
        super(env);

        qualifierHierarchy = factory.getQualifierHierarchy();
        typeHierarchy = factory.getChecker().getTypeHierarchy();
        this.atypeFactory = factory;
        this.checker = checker;
        transferFunction = createTransferFunction();
        expectedNumberOfAnnotations = qualifierHierarchy.getWidth();
        this.fieldValues = fieldValues;
    }

    public List<Pair<VariableElement, V>> getFieldValues() {
        return fieldValues;
    }

    /**
     * @return The transfer function to be used by the analysis.
     */
    @SuppressWarnings("unchecked")
    public T createTransferFunction() {
        @SuppressWarnings("rawtypes")
        AbstractBasicAnnotatedTypeFactory f = atypeFactory;
        return (T) f.createFlowTransferFunction(this);
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

    public AbstractBasicAnnotatedTypeFactory<? extends BaseTypeChecker<?>, V, S, T, ? extends CFAbstractAnalysis<V, S, T>> getFactory() {
        return atypeFactory;
    }

    /**
     * Print a DOT graph of the CFG and analysis info for inspection.
     */
    public void outputToDotFile(String outputFile) {
        String s = CFGDOTVisualizer.visualize(cfg.getEntryBlock(), this);

        try {
            FileWriter fstream = new FileWriter(outputFile);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(s);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Returns an abstract value containing an annotated type with the
     * annotation {@code anno}, and 'top' for all other hierarchies. The
     * underlying type is {@link Object}.
     */
    public V createSingleAnnotationValue(AnnotationMirror anno,
            TypeMirror underlyingType) {
        AnnotatedTypeMirror type = AnnotatedTypeMirror.createType(
                underlyingType, getFactory());
        Set<? extends AnnotationMirror> tops = getFactory().getQualifierHierarchy()
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
            AnnotatedTypeVariable a = (AnnotatedTypeVariable) type;
            makeTop(a.getUpperBound(), tops);
        } else if (kind == TypeKind.WILDCARD) {
            AnnotatedWildcardType a = (AnnotatedWildcardType) type;
            makeTop(a.getExtendsBound(), tops);
        }

        if (kind != TypeKind.TYPEVAR && kind != TypeKind.WILDCARD) {
            // don't set top annotations, because [] is top
            type.addAnnotations(tops);
        }
    }
}
