package checkers.util;

import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;

import checkers.quals.*;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.types.visitors.AnnotatedTypeScanner;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;

/**
 * Determines the default qualifiers on a type.
 * Default qualifiers are specified via the {@link DefaultQualifier} annotation.
 *
 * @see DefaultQualifier
 */
public class QualifierDefaults {

    // TODO add visitor state to get the default annotations from the top down?
    // TODO apply from package elements also
    // TODO try to remove some dependencies (e.g. on factory)

    private final AnnotatedTypeFactory factory;
    private final AnnotationUtils annoFactory;

    private AnnotationMirror absoluteDefaultAnno;
    private Set<DefaultLocation> absoluteDefaultLocs;

    /**
     * @param factory the factory for this checker
     * @param annoFactory an annotation factory, used to get annotations by name
     */
    public QualifierDefaults(AnnotatedTypeFactory factory, AnnotationUtils annoFactory) {
        this.factory = factory;
        this.annoFactory = annoFactory;
    }

    /**
     * Sets the default annotation.  A programmer may override this by
     * writing the @DefaultQualifier annotation.
     */
    public void setAbsoluteDefaults(AnnotationMirror absoluteDefaultAnno, Set<DefaultLocation> locations) {
        this.absoluteDefaultAnno = absoluteDefaultAnno;
        this.absoluteDefaultLocs = new HashSet<DefaultLocation>(locations);
    }

    public void annotateTypeElement(TypeElement elt, AnnotatedTypeMirror type) {
        applyDefaults(elt, type);
    }

    /**
     * Applies default annotations to a type given an {@link Element}.
     *
     * @param elt the element from which the type was obtained
     * @param type the type to annotate
     */
    public void annotate(Element elt, AnnotatedTypeMirror type) {
        applyDefaults(elt, type);
    }

    /**
     * Applies default annotations to a type given a {@link Tree}.
     *
     * @param tree the tree from which the type was obtained
     * @param type the type to annotate
     */
    public void annotate(Tree tree, AnnotatedTypeMirror type) {
        applyDefaults(tree, type);
    }

    /**
     * Determines the nearest enclosing element for a tree by climbing the tree
     * toward the root and obtaining the element for the first declaration
     * (variable, method, or class) that encloses the tree.
     *
     * @param tree the tree
     * @return the nearest enclosing element for a tree
     */
    private Element nearestEnclosing(Tree tree) {

        TreePath path = factory.getPath(tree);
        if (path == null) return InternalUtils.symbol(tree);

        for (Tree t : path) {
            switch (t.getKind()) {
            case VARIABLE:
                return TreeUtils.elementFromDeclaration((VariableTree)t);
            case METHOD:
                return TreeUtils.elementFromDeclaration((MethodTree)t);
            case CLASS:
                return TreeUtils.elementFromDeclaration((ClassTree)t);
            default: // Do nothing.
            }
        }

        return null;
    }

    /**
     * Applies default annotations to a type from a {@link Tree} by determining
     * the appropriate scope for defaults.
     *
     * <p>
     *
     * For instance, if the tree is associated with a declaration (e.g., it's
     * the use of a field, or a method invocation), defaults in the scope of the
     * <i>declaration</i> are used; if the tree is not associated with a
     * declaration (e.g., a typecast), defaults in the scope of the tree are
     * used.
     *
     * @param tree the tree associated with the type
     * @param type the type to which defaults will be applied
     *
     * @see #applyDefaults(Element, AnnotatedTypeMirror)
     */
    private void applyDefaults(Tree tree, AnnotatedTypeMirror type) {

        Element elt = null;
        switch (tree.getKind()) {
            case MEMBER_SELECT:
                elt = TreeUtils.elementFromUse((MemberSelectTree)tree);
                break;

            case IDENTIFIER:
                elt = TreeUtils.elementFromUse((IdentifierTree)tree);
                break;

            case METHOD_INVOCATION:
                elt = TreeUtils.elementFromUse((MethodInvocationTree)tree);
                break;

            // TODO cases for array access, etc. -- every expression tree

            default:
                // If no associated symbol was found, use the tree's (lexical)
                // scope.
                elt = nearestEnclosing(tree);
        }
        if (elt != null)
            applyDefaults(elt, type);
    }

    /**
     * Applies default annotations to a type from an {@link Element} by using
     * the {@link DefaultQualifier} annotation present on the element or any of its
     * enclosing elements.
     *
     * @param elt the element representing the nearest enclosing default
     *        annotation scope for the type
     * @param type the type to which defaults will be applied
     */
    private void applyDefaults(final Element elt, final AnnotatedTypeMirror type) {

        Element e = elt;
        while (e != null) {

            DefaultQualifier d = e.getAnnotation(DefaultQualifier.class);
            if (d != null)
                applyDefault(elt, d, type);

            DefaultQualifiers ds = e.getAnnotation(DefaultQualifiers.class);
            if (ds != null) {
                for (DefaultQualifier dq : ds.value())
                    applyDefault(elt, dq, type);
            }

            if (e.getKind() == ElementKind.PACKAGE)
                e = ((Symbol)e).owner;
            else
                e = e.getEnclosingElement();
        }

        if (this.absoluteDefaultAnno != null)
            new DefaultApplier(elt, this.absoluteDefaultLocs, type).scan(type, absoluteDefaultAnno);
    }

    private void applyDefault(Element elt, DefaultQualifier d, AnnotatedTypeMirror type) {
        AnnotationMirror anno = annoFactory.fromName(d.value());
        new DefaultApplier(elt, d.locations(), type).scan(type, anno);
    }

    private static class DefaultApplier
    extends AnnotatedTypeScanner<Void, AnnotationMirror> {
        private final Element elt;
        private final Collection<DefaultLocation> locations;
        private final AnnotatedTypeMirror type;

        public DefaultApplier(Element elt, DefaultLocation[] locations, AnnotatedTypeMirror type) {
            this(elt, Arrays.asList(locations), type);
        }

        public DefaultApplier(Element elt, Collection<DefaultLocation> locations, AnnotatedTypeMirror type) {
            this.elt = elt;
            this.locations = locations; /* no need to copy locations */
            this.type = type;
        }

        @Override
        public Void scan(AnnotatedTypeMirror t,
                AnnotationMirror p) {

            if (t == null || t.getKind() == TypeKind.NONE)
                return null;

            // Skip type variables, but continue to scan their bounds.
            if (t.getKind() == TypeKind.WILDCARD
                    || t.getKind() == TypeKind.TYPEVAR)
                return super.scan(t, p);

            // Skip annotating this type if:
            // - the default is "all except (the raw types of) locals"
            // - we are applying defaults to a local
            // - and the type is a raw type
            if (elt.getKind() == ElementKind.LOCAL_VARIABLE
                    && locations.contains(DefaultLocation.ALL_EXCEPT_LOCALS)
                    && t == type)
                return super.scan(t, p);

            // Add the default annotation, but only if no other
            // annotation is present.
            if (t.getAnnotations().isEmpty())
                t.addAnnotation(p);

            return super.scan(t, p);
        }

        // Skip method receivers.
        @Override
        public Void visitExecutable(AnnotatedExecutableType t,
                AnnotationMirror p) {
            return super.visitExecutable(t, p);
//            scan(t.getReturnType(), p);
//            scanAndReduce(t.getParameterTypes(), p, null);
//            scanAndReduce(t.getThrownTypes(), p, null);
//            scanAndReduce(t.getTypeVariables(), p, null);
//            return null;
        }
    }
}
