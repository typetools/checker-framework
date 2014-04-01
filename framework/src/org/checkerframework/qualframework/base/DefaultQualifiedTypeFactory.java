package org.checkerframework.qualframework.base;

import java.util.HashMap;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

import org.checkerframework.javacutil.Pair;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedDeclaredType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeVariable;
import org.checkerframework.qualframework.util.WrappedAnnotatedTypeMirror;

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
    private HashMap<TypeParameterElement, QualifiedTypeParameterBounds<Q>> paramBoundsMap =
        new HashMap<>();

    private QualifierHierarchy<Q> qualifierHierarchy;
    private TypeHierarchy<Q> typeHierarchy;
    private AnnotationConverter<Q> annotationConverter;

    private TreeAnnotator<Q> treeAnnotator;
    private TypeAnnotator<Q> typeAnnotator;


    private QualifiedTypeFactoryAdapter<Q> adapter;

    void setAdapter(QualifiedTypeFactoryAdapter<Q> adapter) {
        this.adapter = adapter;
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
    public final QualifiedTypeParameterBounds<Q> getQualifiedTypeParameterBounds(Element elt) {
        if (elt.getKind() != ElementKind.TYPE_PARAMETER) {
            throw new IllegalArgumentException("expected a TYPE_PARAMETER, not " + elt.getKind());
        }
        TypeParameterElement paramElt = (TypeParameterElement)elt;
        if (!paramBoundsMap.containsKey(paramElt)) {
            QualifiedTypeParameterBounds<Q> bounds = computeQualifiedTypeParameterBounds(paramElt);
            paramBoundsMap.put(paramElt, bounds);
        }
        return paramBoundsMap.get(paramElt);
    }

    /**
     * Computes the bounds of a type parameter.  The default implementation
     * processes the type annotations of the upper and lower bounds using the
     * {@link TypeAnnotator}.
     */
    protected QualifiedTypeParameterBounds<Q> computeQualifiedTypeParameterBounds(
            TypeParameterElement paramElt) {
        AnnotatedTypeVariable atm = (AnnotatedTypeVariable)adapter.fromElement(paramElt);
        TypeAnnotator<Q> annotator = getTypeAnnotator();

        WrappedAnnotatedTypeMirror wrappedUpper =
            WrappedAnnotatedTypeMirror.wrap(atm.getUpperBound());
        QualifiedTypeMirror<Q> upper = annotator.visit(wrappedUpper, null);

        WrappedAnnotatedTypeMirror wrappedLower =
            WrappedAnnotatedTypeMirror.wrap(atm.getLowerBound());
        QualifiedTypeMirror<Q> lower = annotator.visit(wrappedLower, null);

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
        return new TypeAnnotator<Q>(getAnnotationConverter(),
                getQualifierHierarchy().getTop());
    }


    @Override
    public final QualifierHierarchy<Q> getQualifierHierarchy() {
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
    public final TypeHierarchy<Q> getTypeHierarchy() {
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
        return new DefaultTypeHierarchy<Q>(qualifierHierarchy);
    }


    /**
     * Gets the {@link AnnotationConverter} for the current type system.
     */
    public final AnnotationConverter<Q> getAnnotationConverter() {
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
        throw new UnsupportedOperationException();
    }

    @Override
    public QualifiedTypeMirror<Q> postAsMemberOf(QualifiedTypeMirror<Q> memberType, QualifiedTypeMirror<Q> receiverType, Element memberElement) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<QualifiedTypeVariable<Q>> typeVariablesFromUse(QualifiedDeclaredType<Q> type, TypeElement element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> methodFromUse(MethodInvocationTree tree) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> constructorFromUse(NewClassTree tree) {
        throw new UnsupportedOperationException();
    }
}
