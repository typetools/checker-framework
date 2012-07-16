package checkers.util;

import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;

import checkers.quals.*;
import checkers.source.SourceChecker;
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

    private final List<Pair<AnnotationMirror, ? extends Set<DefaultLocation>>> absoluteDefaults =
            new LinkedList<Pair<AnnotationMirror, ? extends Set<DefaultLocation>>>();

    private final Map<String, String> qualifiedNameMap;

    /**
     * @param factory the factory for this checker
     * @param annoFactory an annotation factory, used to get annotations by name
     */
    public QualifierDefaults(AnnotatedTypeFactory factory, AnnotationUtils annoFactory) {
        this.factory = factory;
        this.annoFactory = annoFactory;

        qualifiedNameMap = new HashMap<String, String>();
        for (Name name : factory.getQualifierHierarchy().getTypeQualifiers()) {
            String qualified = name.toString();
            String unqualified = qualified.substring(qualified.lastIndexOf('.') + 1);
            qualifiedNameMap.put(qualified, qualified);
            qualifiedNameMap.put(unqualified, qualified);
        }
    }

    /**
     * Sets the default annotation.  A programmer may override this by
     * writing the @DefaultQualifier annotation.
     */
    public void addAbsoluteDefault(AnnotationMirror absoluteDefaultAnno, Set<DefaultLocation> locations) {
        for (Pair<AnnotationMirror, ? extends Set<DefaultLocation>> def : absoluteDefaults) {
            AnnotationMirror anno = def.first;
            QualifierHierarchy qh = factory.getQualifierHierarchy();
            if (!absoluteDefaultAnno.equals(anno) &&
                    qh.isSubtype(absoluteDefaultAnno, qh.getTopAnnotation(anno))) {
                SourceChecker.errorAbort("Only one qualifier from a hierarchy can be the default! Existing: "
                        + absoluteDefaults + " and new: " + absoluteDefaultAnno);
            }
        }
        absoluteDefaults.add(Pair.of(absoluteDefaultAnno, new HashSet<DefaultLocation>(locations)));
    }

    public void setLocalVariableDefault(Set<AnnotationMirror> localannos) {
        localVarDefaultAnnos = localannos;
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
     * Initializers of local variables are handled in a special way:
     * within an initializer we look for the DefaultQualifier(s) annotation and
     * keep track of the previously visited tree.
     * TODO: explain the behavior better.
     *
     * @param tree the tree
     * @return the nearest enclosing element for a tree
     */
    private Element nearestEnclosingExceptLocal(Tree tree) {
        TreePath path = factory.getPath(tree);
        if (path == null) return InternalUtils.symbol(tree);

        Tree prev = null;

        for (Tree t : path) {
            switch (t.getKind()) {
            case VARIABLE:
                VariableTree vtree = (VariableTree)t;
                ExpressionTree vtreeInit = vtree.getInitializer();
                if (vtreeInit!=null && prev==vtreeInit) {
                    Element elt = TreeUtils.elementFromDeclaration((VariableTree)t);
                    DefaultQualifier d = elt.getAnnotation(DefaultQualifier.class);
                    DefaultQualifiers ds = elt.getAnnotation(DefaultQualifiers.class);

                    if (d == null && ds == null)
                        break;
                }
                if (prev!=null && prev.getKind() == Tree.Kind.MODIFIERS) {
                    // Annotations are modifiers. We do not want to apply the local variable default to
                    // annotations. Without this, test fenum/TestSwitch failed, because the default for
                    // an argument became incompatible with the declared type.
                    break;
                }
                return TreeUtils.elementFromDeclaration((VariableTree)t);
            case METHOD:
                return TreeUtils.elementFromDeclaration((MethodTree)t);
            case CLASS:
            case ENUM:
            case INTERFACE:
            case ANNOTATION_TYPE:
                return TreeUtils.elementFromDeclaration((ClassTree)t);
            default: // Do nothing.
            }
            prev = t;
        }

        return null;
    }

    /**
     * Applies default annotations to a type.
     * A {@link Tree} that determines the appropriate scope for defaults.
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

        // The location to take defaults from.
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
            // (The above probably means that we should use defaults in the
            // scope of the declaration of the array.  Is that right?  -MDE)

            default:
                // If no associated symbol was found, use the tree's (lexical)
                // scope.
                elt = nearestEnclosingExceptLocal(tree);
                // elt = nearestEnclosing(tree);
        }
        // System.out.println("applyDefaults on tree " + tree + " gives elt: " + elt);
        if (elt != null)
            applyDefaults(elt, type);
    }

    private final Map<Element, List<DefaultQualifier>> qualifierCache =
        new IdentityHashMap<Element, List<DefaultQualifier>>();

    /** The default annotation for local variables.
     */
    // Static to allow access from static inner class; TODO: improve?
    private static Set<AnnotationMirror> localVarDefaultAnnos;

    private List<DefaultQualifier> defaultsAt(final Element elt) {
        if (elt == null)
            return Collections.emptyList();

        if (qualifierCache.containsKey(elt))
            return qualifierCache.get(elt);

        List<DefaultQualifier> qualifiers = new ArrayList<DefaultQualifier>();

        DefaultQualifier d = elt.getAnnotation(DefaultQualifier.class);
        if (d != null)
            qualifiers.add(d);

        DefaultQualifiers ds = elt.getAnnotation(DefaultQualifiers.class);
        if (ds != null)
            qualifiers.addAll(Arrays.asList(ds.value()));

        Element parent;
        if (elt.getKind() == ElementKind.PACKAGE)
            parent = ((Symbol)elt).owner;
        else
            parent = elt.getEnclosingElement();

        List<DefaultQualifier> parentDefaults = defaultsAt(parent);
        if (qualifiers.isEmpty())
            qualifiers = parentDefaults;
        else
            qualifiers.addAll(parentDefaults);

        qualifierCache.put(elt, qualifiers);
        return qualifiers;
    }

    /**
     * Applies default annotations to a type.
     * The defaults are taken from an {@link Element} by using the
     * {@link DefaultQualifier} annotation present on the element
     * or any of its enclosing elements.
     *
     * @param annotationScope the element representing the nearest enclosing
     *        default annotation scope for the type
     * @param type the type to which defaults will be applied
     */
    private void applyDefaults(final Element annotationScope, final AnnotatedTypeMirror type) {

        List<DefaultQualifier> defaults = defaultsAt(annotationScope);
        for (DefaultQualifier dq : defaults)
            applyDefault(annotationScope, dq, type);

        for (Pair<AnnotationMirror, ? extends Set<DefaultLocation>> def : absoluteDefaults) {
            new DefaultApplier(annotationScope, def.second, type).scan(type, def.first);
        }
    }

    private void applyDefault(Element annotationScope, DefaultQualifier d, AnnotatedTypeMirror type) {
        String name = d.value();
        if (qualifiedNameMap.containsKey(name))
            name = qualifiedNameMap.get(name);
        AnnotationMirror anno = annoFactory.fromName(name);
        if (anno == null)
            return;
        if (factory.isSupportedQualifier(anno)) {
            new DefaultApplier(annotationScope, d.locations(), type).scan(type, anno);
        }
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
                    && t == type) {

                if (localVarDefaultAnnos != null) {
                    for (AnnotationMirror anno : localVarDefaultAnnos) {
                        if (!t.isAnnotatedInHierarchy(anno)) {
                            t.addAnnotation(anno);
                        }
                    }
                }

                return super.scan(t, p);
            }

            if (locations.contains(DefaultLocation.UPPER_BOUNDS)
                && locations.size() == 1
                && !this.isTypeVarExtends) {
                return super.scan(t, p);
            }

            // Add the default annotation, but only if no other
            // annotation is present.
            if (!t.isAnnotatedInHierarchy(p))
                t.addAnnotation(p);

            /* Anonymous types, e.g. intersection types, list the types
             * in the direct supertypes. Make sure to apply the default there too.
             * Use the direct supertypes field to prevent an infinite recursion
             * with the IGJATF.postDirectSuperTypes. TODO: investigate better way.
             */
            if (TypesUtils.isAnonymousType(t.getUnderlyingType())) {
                List<AnnotatedDeclaredType> sups = ((AnnotatedDeclaredType)t).directSuperTypesField();
                if (sups!=null) {
                    for (AnnotatedTypeMirror sup : sups) {
                        if (!sup.isAnnotatedInHierarchy(p)) {
                            sup.addAnnotation(p);
                        }
                    }
                }
            }

            return super.scan(t, p);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, AnnotationMirror p) {
            // TODO: should this logic be in AnnotatedTypeScanner?
            if (TypesUtils.isAnonymousType(type.getUnderlyingType())) {
                for(AnnotatedDeclaredType adt : type.directSuperTypes()) {
                    scan(adt, p);
                }
            }
            return super.visitDeclared(type, p);
        }

        private boolean isTypeVarExtends = false;
        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, AnnotationMirror p) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            Void r = scan(type.getLowerBoundField(), p);
            visitedNodes.put(type, r);
            boolean prevIsTypeVarExtends = isTypeVarExtends;
            isTypeVarExtends = true;
            try {
                r = scanAndReduce(type.getUpperBoundField(), p, r);
            } finally {
                isTypeVarExtends = prevIsTypeVarExtends;
            }
            visitedNodes.put(type, r);
            return r;
        }

        @Override
        public Void visitWildcard(AnnotatedWildcardType type, AnnotationMirror p) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            Void r;
            boolean prevIsTypeVarExtends = isTypeVarExtends;
            isTypeVarExtends = true;
            try {
                r = scan(type.getExtendsBound(), p);
            } finally {
                isTypeVarExtends = prevIsTypeVarExtends;
            }
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getSuperBound(), p, r);
            visitedNodes.put(type, r);
            return r;
        }
    }
}
