package org.checkerframework.qualframework.base;

import org.checkerframework.javacutil.Pair;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.qualframework.base.dataflow.QualAnalysis;
import org.checkerframework.qualframework.base.dataflow.QualValue;
import org.checkerframework.qualframework.util.ExtendedParameterDeclaration;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;
import org.checkerframework.qualframework.util.QualifierContext;
import org.checkerframework.qualframework.util.WrappedAnnotatedTypeMirror;
import org.checkerframework.qualframework.util.WrappedAnnotatedTypeMirror.WrappedAnnotatedTypeVariable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

/** Default implementation of {@link QualifiedTypeFactory}.  Most type systems
 * should extend this class (or a subclass) instead of implementing {@link
 * QualifiedTypeFactory} directly.
 *
 * This implementation decomposes the problem of type checking using several
 * helper classes:
 *
 * <ul>
 * <li> {@link AnnotationConverter}: Converts annotations written in the source
 *      code into type qualifiers for use in type checking. </li>
 * <li> {@link TypeAnnotator}: A visitor for
 *      {@link org.checkerframework.qualframework.util.ExtendedTypeMirror}s that
 *      adds a qualifier to each component of the type, producing a {@link
 *      QualifiedTypeMirror}. </li>
 * <li> {@link TreeAnnotator}: A visitor for {@link Tree}s that computes the
 *      qualified type of each AST node based on the qualified types of its
 *      subnodes. </li>
 * <li> {@link QualifierHierarchy}: Used to perform subtyping checks between
 *      two type qualifiers (independent of any Java types). </li>
 * <li> {@link TypeHierarchy}: Used to perform subtyping checks between two
 *      {@link QualifiedTypeMirror}s. </li>
 * </ul>
 *
 * Default implementations are available for {@link TypeAnnotator}, {@link
 * TreeAnnotator}, and {@link TypeHierarchy}.  The type system must provide
 * implementations for {@link AnnotationConverter} and {@link
 * QualifierHierarchy}.
 */
public abstract class DefaultQualifiedTypeFactory<Q> implements QualifiedTypeFactory<Q> {
    private final IdentityHashMap<ExtendedParameterDeclaration, QualifiedTypeParameterBounds<Q>> paramBoundsMap = new IdentityHashMap<>();

    private QualifiedTypes<Q> qualifiedTypes;
    private QualifierHierarchy<Q> qualifierHierarchy;
    private TypeHierarchy<Q> typeHierarchy;
    private AnnotationConverter<Q> annotationConverter;

    private TreeAnnotator<Q> treeAnnotator;
    private TypeAnnotator<Q> typeAnnotator;

    private QualifiedTypeFactoryAdapter<Q> adapter;
    private final QualifierContext<Q> context;

    public DefaultQualifiedTypeFactory(QualifierContext<Q> context) {
        this.context = context;
    }

    @Override
    public final QualifiedTypeMirror<Q> getQualifiedType(Element element) {
        return adapter.superGetAnnotatedType(element);
    }

    @Override
    public final QualifiedTypeMirror<Q> getQualifiedType(Tree tree) {
        return adapter.superGetAnnotatedType(tree);
    }

    @Override
    public final QualifiedTypeMirror<Q> getQualifiedTypeFromTypeTree(Tree typeTree) {
        return adapter.superGetAnnotatedTypeFromTypeTree(typeTree);
    }


    @Override
    public final QualifiedTypeParameterBounds<Q> getQualifiedTypeParameterBounds(ExtendedParameterDeclaration etm) {
        if (!paramBoundsMap.containsKey(etm)) {
            QualifiedTypeParameterBounds<Q> bounds = computeQualifiedTypeParameterBounds(etm);
            paramBoundsMap.put(etm, bounds);
        }
        return paramBoundsMap.get(etm);
    }

    /**
     * Computes the bounds of a type parameter.  The default implementation
     * processes the type annotations of the upper and lower bounds using the
     * {@link TypeAnnotator}.
     */
    protected QualifiedTypeParameterBounds<Q> computeQualifiedTypeParameterBounds(ExtendedParameterDeclaration etm) {
        TypeAnnotator<Q> annotator = getTypeAnnotator();

        WrappedAnnotatedTypeVariable watv = (WrappedAnnotatedTypeVariable)etm;
        WrappedAnnotatedTypeMirror rawUpper = WrappedAnnotatedTypeMirror.wrap(watv.unwrap().getUpperBound());
        WrappedAnnotatedTypeMirror rawLower = WrappedAnnotatedTypeMirror.wrap(watv.unwrap().getLowerBound());
        QualifiedTypeMirror<Q> upper = annotator.visit(rawUpper, null);
        QualifiedTypeMirror<Q> lower = annotator.visit(rawLower, null);

        return new QualifiedTypeParameterBounds<Q>(upper, lower);
    }


    // This method has package access so it can be called from QTFAdapter.  It
    // should be made private once the adapter is no longer needed.
    TreeAnnotator<Q> getTreeAnnotator() {
        if (this.treeAnnotator == null) {
            this.treeAnnotator = createTreeAnnotator();
        }
        return this.treeAnnotator;
    }

    /**
     * Constructs the {@link TreeAnnotator} to be used by this type factory.
     * Checkers that need custom {@link TreeAnnotator} behavior  should
     * override this method to return an instance of their custom {@link
     * TreeAnnotator} subclass.
     */
    protected TreeAnnotator<Q> createTreeAnnotator() {
        return new TreeAnnotator<Q>();
    }


    // This method has package access so it can be called from QTFAdapter.  It
    // should be made private once the adapter is no longer needed.
    TypeAnnotator<Q> getTypeAnnotator() {
        if (this.typeAnnotator == null) {
            this.typeAnnotator = createTypeAnnotator();
        }
        return this.typeAnnotator;
    }

    /**
     * Constructs the {@link TypeAnnotator} to be used by this type factory.
     * Checkers that need custom {@link TypeAnnotator} behavior  should
     * override this method to return an instance of their custom {@link
     * TypeAnnotator} subclass.
     */
    protected TypeAnnotator<Q> createTypeAnnotator() {
        // Construct a new TypeAnnotator using the TOP qualifier as the
        // default.
        return new TypeAnnotator<Q>(context, getAnnotationConverter(),
                getQualifierHierarchy().getTop());
    }


    @Override
    public final QualifiedTypes<Q> getQualifiedTypes() {
        if (this.qualifiedTypes == null) {
            this.qualifiedTypes = createQualifiedTypes();
        }
        return this.qualifiedTypes;
    }

    protected QualifiedTypes<Q> createQualifiedTypes() {
        return new AdapterQualifiedTypes<Q>(adapter);
    }


    @Override
    public QualifierHierarchy<Q> getQualifierHierarchy() {
        if (this.qualifierHierarchy == null) {
            this.qualifierHierarchy = createQualifierHierarchy();
        }
        return this.qualifierHierarchy;
    }

    /**
     * Constructs a {@link QualifierHierarchy} for the current type system.
     * Every checker must override this method to return an appropriate {@link
     * QualifierHierarchy} subclass for that checker.
     */
    protected abstract QualifierHierarchy<Q> createQualifierHierarchy();


    @Override
    public TypeHierarchy<Q> getTypeHierarchy() {
        if (this.typeHierarchy == null) {
            this.typeHierarchy = createTypeHierarchy(getQualifierHierarchy());
        }
        return this.typeHierarchy;
    }

    /**
     * Constructs a {@link TypeHierarchy} for the current type system.  The
     * default implementation constructs a {@link DefaultTypeHierarchy}.
     *
     * @param qualifierHierarchy
     *      a reference to the {@link QualifierHierarchy} used by this type system
     */
    protected TypeHierarchy<Q> createTypeHierarchy(QualifierHierarchy<Q> qualifierHierarchy) {
        return new DefaultTypeHierarchy<Q>();
    }


    /**
     * Gets the {@link AnnotationConverter} for the current type system.
     */
    public AnnotationConverter<Q> getAnnotationConverter() {
        if (this.annotationConverter == null) {
            this.annotationConverter = createAnnotationConverter();
        }
        return this.annotationConverter;
    }

    /**
     * Constructs an {@link AnnotationConverter} for the current type system.
     * Every checker must override this method to return an appropriate {@link
     * AnnotationConverter} subclass for that checker.
     */
    protected abstract AnnotationConverter<Q> createAnnotationConverter();


    @Override
    public List<QualifiedTypeMirror<Q>> postDirectSuperTypes(QualifiedTypeMirror<Q> subtype, List<? extends QualifiedTypeMirror<Q>> supertypes) {
        return adapter.superPostDirectSuperTypes(subtype, supertypes);
    }

    @Override
    public QualifiedTypeMirror<Q> postAsMemberOf(QualifiedTypeMirror<Q> memberType, QualifiedTypeMirror<Q> receiverType, Element memberElement) {
        return adapter.superPostAsMemberOf(memberType, receiverType, memberElement);
    }

    @Override
    public Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> methodFromUse(MethodInvocationTree tree) {
        return adapter.superMethodFromUse(tree);
    }

    @Override
    public Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> methodFromUse(ExpressionTree tree,
            ExecutableElement methodElt, QualifiedTypeMirror<Q> receiverType) {

        return adapter.superMethodFromUse(tree, methodElt, receiverType);
    }

    @Override
    public Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> constructorFromUse(NewClassTree tree) {
        throw new UnsupportedOperationException();
    }

    @Override
    public QualAnalysis<Q> createFlowAnalysis(List<Pair<VariableElement, QualValue<Q>>> fieldValues) {
        return new QualAnalysis<Q>(this.getContext());
    }

    public QualifierContext<Q> getContext() {
        return context;
    }

    void setAdapter(QualifiedTypeFactoryAdapter<Q> adapter) {
        this.adapter = adapter;
    }

    @Override
    public TreePath getPath(Tree node) {
        return adapter.getPath(node);
    }

    @Override
    public QualifiedTypeMirror<Q> getReceiverType(ExpressionTree expression) {
        return adapter.getCheckerAdapter().getTypeMirrorConverter().getQualifiedType(adapter.getReceiverType(expression));
    }

    @Override
    public ExtendedTypeMirror getDecoratedElement(Element element) {
        return WrappedAnnotatedTypeMirror.wrap(adapter.fromElement(element));
    }

    @Override
    public TypeVariableSubstitutor<Q> createTypeVariableSubstitutor() {
        return new TypeVariableSubstitutor<>();
    }

    @Override
    public Set<AnnotationMirror> getDeclAnnotations(Element elt) {
        return adapter.getDeclAnnotations(elt);
    }
}
