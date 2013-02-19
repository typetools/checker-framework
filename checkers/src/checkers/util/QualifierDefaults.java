package checkers.util;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;

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

    private final Elements elements;
    private final AnnotatedTypeFactory atypeFactory;

    @SuppressWarnings("serial")
    private static class AMLocTreeSet extends TreeSet<Pair<AnnotationMirror, DefaultLocation>> {
        public AMLocTreeSet() {
            super(new AMLocComparator());
        }

        static class AMLocComparator implements Comparator<Pair<AnnotationMirror, DefaultLocation>> {
            @Override
            public int compare(Pair<AnnotationMirror, DefaultLocation> o1,
                    Pair<AnnotationMirror, DefaultLocation> o2) {
                int snd = o1.second.compareTo(o2.second);
                if (snd == 0) {
                    return AnnotationUtils.annotationOrdering().compare(o1.first, o2.first);
                } else {
                    return snd;
                }
            }
        }

        // Cannot wrap into unmodifiable set :-(
        // TODO cleaner solution?
        public static final AMLocTreeSet EMPTY_SET = new AMLocTreeSet();
    }

    /** Defaults that apply, if nothing else applies. */
    private final AMLocTreeSet absoluteDefaults = new AMLocTreeSet();

    /** Defaults that apply for a certain Element.
     * On the one hand this is used for caching (an earlier name for the field was
     * "qualifierCache". It can also be used by type systems to set defaults for
     * certain Elements.
     */
    private final Map<Element, AMLocTreeSet> elementDefaults =
            new IdentityHashMap<Element, AMLocTreeSet>();

    /**
     * @param factory the factory for this checker
     * @param annoFactory an annotation factory, used to get annotations by name
     */
    public QualifierDefaults(Elements elements, AnnotatedTypeFactory atypeFactory) {
        this.elements = elements;
        this.atypeFactory = atypeFactory;
    }

    /**
     * Sets the default annotations.  A programmer may override this by
     * writing the @DefaultQualifier annotation on an element.
     */
    public void addAbsoluteDefault(AnnotationMirror absoluteDefaultAnno, DefaultLocation location) {
        checkDuplicates(absoluteDefaults, absoluteDefaultAnno, location);
        absoluteDefaults.add(Pair.of(absoluteDefaultAnno, location));
    }

	/**
     * Sets the default annotations for a certain Element.
     */
    public void addElementDefault(Element elem, AnnotationMirror elementDefaultAnno, DefaultLocation location) {
        AMLocTreeSet prevset = elementDefaults.get(elem);
        if (prevset != null) {
            checkDuplicates(prevset, elementDefaultAnno, location);
        } else {
            prevset = new AMLocTreeSet();
        }
        prevset.add(Pair.of(elementDefaultAnno, location));
        elementDefaults.put(elem, prevset);
    }

    private void checkDuplicates(Set<Pair<AnnotationMirror, DefaultLocation>> prevset,
            AnnotationMirror newanno, DefaultLocation newloc) {
        for (Pair<AnnotationMirror, DefaultLocation> def : prevset) {
            AnnotationMirror anno = def.first;
            QualifierHierarchy qh = atypeFactory.getQualifierHierarchy();
            if (!newanno.equals(anno) &&
                    qh.isSubtype(newanno, qh.getTopAnnotation(anno))) {
                if (newloc == def.second) {
                    SourceChecker.errorAbort("Only one qualifier from a hierarchy can be the default! Existing: "
                            + prevset + " and new: " + newanno);
                }
            }
        }
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
        TreePath path = atypeFactory.getPath(tree);
        if (path == null) return InternalUtils.symbol(tree);

        Tree prev = null;

        for (Tree t : path) {
            switch (t.getKind()) {
            case VARIABLE:
                VariableTree vtree = (VariableTree)t;
                ExpressionTree vtreeInit = vtree.getInitializer();
                if (vtreeInit != null && prev == vtreeInit) {
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

    private Set<Pair<AnnotationMirror, DefaultLocation>> fromDefaultQualifier(DefaultQualifier dq) {
        // TODO: I want to simply write d.value(), but that doesn't work.
        // It works in other places, e.g. see handling of @SubtypeOf.
        // The hack below should probably be added to:
        // Class<? extends Annotation> cls = AnnotationUtils.parseTypeValue(dq, "value");
        Class<? extends Annotation> cls;
        try {
            cls = dq.value();
        } catch( MirroredTypeException mte ) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> clscast = (Class<? extends Annotation>) Class.forName(mte.getTypeMirror().toString());
                cls = clscast;
            } catch (ClassNotFoundException e) {
                SourceChecker.errorAbort("Could not load qualifier: " + e.getMessage(), e);
                cls = null;
            }
        }

        AnnotationMirror anno = AnnotationUtils.fromClass(elements, cls);

        if (anno == null) {
            return null;
        }

        if (!atypeFactory.isSupportedQualifier(anno)) {
            anno = atypeFactory.aliasedAnnotation(anno);
        }

        if (atypeFactory.isSupportedQualifier(anno)) {
            EnumSet<DefaultLocation> locations = EnumSet.of(dq.locations()[0], dq.locations());
            Set<Pair<AnnotationMirror, DefaultLocation>> ret = new HashSet<Pair<AnnotationMirror, DefaultLocation>>(locations.size());
            for (DefaultLocation loc : locations) {
                ret.add(Pair.of(anno, loc));
            }
            return ret;
        } else {
            return null;
        }
    }

    private AMLocTreeSet defaultsAt(final Element elt) {
        if (elt == null) {
            return AMLocTreeSet.EMPTY_SET;
        }

        if (elementDefaults.containsKey(elt)) {
            return elementDefaults.get(elt);
        }

        AMLocTreeSet qualifiers = null;

        {
            DefaultQualifier d = elt.getAnnotation(DefaultQualifier.class);

            if (d != null) {
                qualifiers = new AMLocTreeSet();
                Set<Pair<AnnotationMirror, DefaultLocation>> p = fromDefaultQualifier(d);

                if (p != null) {
                    qualifiers.addAll(p);
                }
            }
        }

        {
            DefaultQualifiers ds = elt.getAnnotation(DefaultQualifiers.class);
            if (ds != null) {
                if (qualifiers == null) {
                    qualifiers = new AMLocTreeSet();
                }
                for (DefaultQualifier d : ds.value()) {
                    Set<Pair<AnnotationMirror, DefaultLocation>> p = fromDefaultQualifier(d);
                    if (p != null) {
                        qualifiers.addAll(p);
                    }
                }
            }
        }

        Element parent;
        if (elt.getKind() == ElementKind.PACKAGE)
            parent = ((Symbol) elt).owner;
        else
            parent = elt.getEnclosingElement();

        AMLocTreeSet parentDefaults = defaultsAt(parent);
        if (qualifiers == null || qualifiers.isEmpty())
            qualifiers = parentDefaults;
        else
            qualifiers.addAll(parentDefaults);

        if (qualifiers != null && !qualifiers.isEmpty()) {
            elementDefaults.put(elt, qualifiers);
            return qualifiers;
        } else {
            return AMLocTreeSet.EMPTY_SET;
        }
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
        AMLocTreeSet defaults = defaultsAt(annotationScope);

        for (Pair<AnnotationMirror, DefaultLocation> def : defaults) {
            new DefaultApplier(annotationScope, def.second, type).scan(type, def.first);
        }

        for (Pair<AnnotationMirror, DefaultLocation> def : absoluteDefaults) {
            new DefaultApplier(annotationScope, def.second, type).scan(type, def.first);
        }
    }

    public static class DefaultApplier
    extends AnnotatedTypeScanner<Void, AnnotationMirror> {
        private final Element elt;
        private final DefaultLocation location;
        private final AnnotatedTypeMirror type;

        public DefaultApplier(Element elt, DefaultLocation location, AnnotatedTypeMirror type) {
            this.elt = elt;
            this.location = location;
            this.type = type;
        }

        @Override
        public Void scan(AnnotatedTypeMirror t, AnnotationMirror qual) {


            if ( !shouldBeAnnotated(t, qual) )  {
                return super.scan(t, qual);
            }

            switch (location) {
            case LOCALS: {
                if (elt.getKind() == ElementKind.LOCAL_VARIABLE &&
                        t == type) {
                    // TODO: how do we determine that we are in a cast or instanceof type?
                    doApply(t, qual);
                }
                break;
            }
            case PARAMETERS: {
                if ( elt.getKind() == ElementKind.PARAMETER &&
                        t == type) {
                    doApply(t, qual);
                } else if (elt.getKind() == ElementKind.METHOD &&
                        t.getKind() == TypeKind.EXECUTABLE &&
                        t == type) {
                    for ( AnnotatedTypeMirror atm : ((AnnotatedExecutableType)t).getParameterTypes()) {
                        doApply(atm, qual);
                    }
                }
                break;
            }
            case RETURNS: {
                if (elt.getKind() == ElementKind.METHOD &&
                        t.getKind() == TypeKind.EXECUTABLE &&
                        t == type) {
                    doApply(((AnnotatedExecutableType)t).getReturnType(), qual);
                }
                break;
            }
            case UPPER_BOUNDS: {
                if (this.isTypeVarExtends) {
                    doApply(t, qual);
                }
                break;
            }
            case OTHERWISE:
            case ALL: {
                // TODO: forbid ALL if anything else was given.
                doApply(t, qual);
                break;
            }
            default: {
                SourceChecker.errorAbort("QualifierDefaults.DefaultApplier: unhandled location: " + location);
                return null;
            }
            }

            return super.scan(t, qual);
        }

        /**
         * Returns true if the given qualifier should be applied to the given type.  Currently we do not
         * apply defaults to void types, packages, wildcards, and type variables
         * @param type Type to which qual would be applied
         * @param qual A default qualifier to apply
         * @return true if this application should proceed
         */
        protected static boolean shouldBeAnnotated( final AnnotatedTypeMirror type,
                                                    final AnnotationMirror    qual  ) {

            return !( type  == null || type.getKind() == TypeKind.NONE ||
                      type.getKind() == TypeKind.WILDCARD ||
                      type.getKind() == TypeKind.TYPEVAR  ||
                      type instanceof AnnotatedNoType );

        }

        private static void doApply(AnnotatedTypeMirror type, AnnotationMirror qual) {

            if ( !shouldBeAnnotated(type, qual) ) {
                return;
            }

            // Add the default annotation, but only if no other
            // annotation is present.
            if (!type.isAnnotatedInHierarchy(qual))
                type.addAnnotation(qual);

            /* Intersection types, list the types in the direct supertypes.
             * Make sure to apply the default there too.
             * Use the direct supertypes field to prevent an infinite recursion
             * with the IGJATF.postDirectSuperTypes. TODO: investigate better way.
             */
            if (type.getKind() == TypeKind.INTERSECTION) {
                List<AnnotatedDeclaredType> sups = ((AnnotatedIntersectionType)type).directSuperTypesField();
                if (sups != null) {
                    for (AnnotatedTypeMirror sup : sups) {
                        if (!sup.isAnnotatedInHierarchy(qual)) {
                            sup.addAnnotation(qual);
                        }
                    }
                }
            }
        }

        private boolean isTypeVarExtends = false;

        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, AnnotationMirror qual) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            Void r = scan(type.getLowerBoundField(), qual);
            visitedNodes.put(type, r);
            boolean prevIsTypeVarExtends = isTypeVarExtends;
            isTypeVarExtends = true;
            try {
                r = scanAndReduce(type.getUpperBoundField(), qual, r);
            } finally {
                isTypeVarExtends = prevIsTypeVarExtends;
            }
            visitedNodes.put(type, r);
            return r;
        }

        @Override
        public Void visitWildcard(AnnotatedWildcardType type, AnnotationMirror qual) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            Void r;
            boolean prevIsTypeVarExtends = isTypeVarExtends;
            isTypeVarExtends = true;
            try {
                r = scan(type.getExtendsBoundField(), qual);
            } finally {
                isTypeVarExtends = prevIsTypeVarExtends;
            }
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getSuperBoundField(), qual, r);
            visitedNodes.put(type, r);
            return r;
        }
    }
}
