package org.checkerframework.framework.util.defaults;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type.WildcardType;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.DefaultQualifiers;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Determines the default qualifiers on a type.
 * Default qualifiers are specified via the {@link org.checkerframework.framework.qual.DefaultQualifier} annotation.
 *
 * @see org.checkerframework.framework.qual.DefaultQualifier
 */
public class QualifierDefaults {

    // TODO add visitor state to get the default annotations from the top down?
    // TODO apply from package elements also
    // TODO try to remove some dependencies (e.g. on factory)

    /**
     * This field indicates whether or not a default should be applied to type vars
     * located in the type being default.  This should only ever be true when the type variable
     * is a local variable, non-component use, i.e.
     * <T> void method(@NOT_HERE T tIn) {
     *     T t = tIn;
     * }
     *
     * The local variable T will be defaulted in order to allow dataflow to refine T.
     * This variable will be false if dataflow is not in use.
     */
    private boolean applyToTypeVar = false;
    private final Elements elements;
    private final AnnotatedTypeFactory atypeFactory;

    private final DefaultSet absoluteDefaults = new DefaultSet();

    /** Defaults that apply for a certain Element.
     * On the one hand this is used for caching (an earlier name for the field was
     * "qualifierCache". It can also be used by type systems to set defaults for
     * certain Elements.
     */
    private final Map<Element, DefaultSet> elementDefaults = new IdentityHashMap<>();

    /**
     * @param elements interface to Element data in the current processing environment
     * @param atypeFactory an annotation factory, used to get annotations by name
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
        absoluteDefaults.add(new Default(absoluteDefaultAnno, location));
    }

    public void addAbsoluteDefaults(AnnotationMirror absoluteDefaultAnno, DefaultLocation[] locations) {
        for (DefaultLocation location : locations) {
            addAbsoluteDefault(absoluteDefaultAnno, location);
        }
    }

    /**
     * Sets the default annotations for a certain Element.
     */
    public void addElementDefault(Element elem, AnnotationMirror elementDefaultAnno, DefaultLocation location) {
        DefaultSet prevset = elementDefaults.get(elem);
        if (prevset != null) {
            checkDuplicates(prevset, elementDefaultAnno, location);
        } else {
            prevset = new DefaultSet();
        }
        prevset.add(new Default(elementDefaultAnno, location));
        elementDefaults.put(elem, prevset);
    }

    private void checkDuplicates(DefaultSet previousDefaults, AnnotationMirror newAnno, DefaultLocation newLoc ) {
        final QualifierHierarchy qualHierarchy = atypeFactory.getQualifierHierarchy();

        for (Default previous : previousDefaults ) {

            if(!newAnno.equals(previous.anno) && previous.location == newLoc) {
                final AnnotationMirror previousTop = qualHierarchy.getTopAnnotation(previous.anno);

                if(qualHierarchy.isSubtype(previousTop, newAnno)) {
                    ErrorReporter.errorAbort("Only one qualifier from a hierarchy can be the default! Existing: "
                                            + previousDefaults + " and new: " + (new Default(newAnno, newLoc)));
                }
            }

        }
    }

    /**
     * Applies default annotations to a type given an {@link javax.lang.model.element.Element}.
     *
     * @param elt the element from which the type was obtained
     * @param type the type to annotate
     */
    public void annotate(Element elt, AnnotatedTypeMirror type) {
        applyDefaultsElement(elt, type);
    }

    /**
     * Applies default annotations to a type given a {@link com.sun.source.tree.Tree}.
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
        if (path == null) {
            Element method = atypeFactory.getEnclosingMethod(tree);
            if (method != null) {
                return method;
            } else {
                return InternalUtils.symbol(tree);
            }
        }

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
     * A {@link com.sun.source.tree.Tree} that determines the appropriate scope for defaults.
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
     * @see #applyDefaultsElement(javax.lang.model.element.Element, org.checkerframework.framework.type.AnnotatedTypeMirror)
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
        // System.out.println("applyDefaults on tree " + tree +
        //        " gives elt: " + elt + "(" + elt.getKind() + ")");

        if (elt != null) {
            boolean useFlow = (atypeFactory instanceof GenericAnnotatedTypeFactory<?,?,?,?>)
                           && (((GenericAnnotatedTypeFactory) atypeFactory).getUseFlow());

            applyToTypeVar = useFlow
                          && elt.getKind() == ElementKind.LOCAL_VARIABLE
                          && type.getKind() == TypeKind.TYPEVAR
                          && atypeFactory.type(tree).getKind() == TypeKind.TYPEVAR;
            applyDefaultsElement(elt, type);
            applyToTypeVar = false;
        }
    }

    private Set<Default> fromDefaultQualifier(DefaultQualifier dq) {
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
                ErrorReporter.errorAbort("Could not load qualifier: " + e.getMessage(), e);
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
            Set<Default> ret = new HashSet<>(locations.size());
            for (DefaultLocation loc : locations) {
                ret.add(new Default(anno, loc));
            }
            return ret;
        } else {
            return null;
        }
    }

    private DefaultSet defaultsAt(final Element elt) {
        if (elt == null) {
            return DefaultSet.EMPTY;
        }

        if (elementDefaults.containsKey(elt)) {
            return elementDefaults.get(elt);
        }

        DefaultSet qualifiers = null;

        {
            DefaultQualifier d = elt.getAnnotation(DefaultQualifier.class);

            if (d != null) {
                qualifiers = new DefaultSet();
                Set<Default> p = fromDefaultQualifier(d);

                if (p != null) {
                    qualifiers.addAll(p);
                }
            }
        }

        {
            DefaultQualifiers ds = elt.getAnnotation(DefaultQualifiers.class);
            if (ds != null) {
                if (qualifiers == null) {
                    qualifiers = new DefaultSet();
                }
                for (DefaultQualifier d : ds.value()) {
                    Set<Default> p = fromDefaultQualifier(d);
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

        DefaultSet parentDefaults = defaultsAt(parent);
        if (qualifiers == null || qualifiers.isEmpty())
            qualifiers = parentDefaults;
        else
            qualifiers.addAll(parentDefaults);

        if (qualifiers != null && !qualifiers.isEmpty()) {
            elementDefaults.put(elt, qualifiers);
            return qualifiers;
        } else {
            return DefaultSet.EMPTY;
        }
    }

    /**
     * Applies default annotations to a type.
     * The defaults are taken from an {@link javax.lang.model.element.Element} by using the
     * {@link org.checkerframework.framework.qual.DefaultQualifier} annotation present on the element
     * or any of its enclosing elements.
     *
     * @param annotationScope the element representing the nearest enclosing
     *        default annotation scope for the type
     * @param type the type to which defaults will be applied
     */
    private void applyDefaultsElement(final Element annotationScope, final AnnotatedTypeMirror type) {
        DefaultSet defaults = defaultsAt(annotationScope);
        DefaultApplierElement applier = new DefaultApplierElement(atypeFactory, annotationScope, type, applyToTypeVar);

        for (Default def : defaults) {
            applier.apply(def);
        }

        for (Default def : absoluteDefaults) {
            applier.apply(def);
        }
    }

    public static class DefaultApplierElement {

        private final AnnotatedTypeFactory atypeFactory;
        private final Element scope;
        private final AnnotatedTypeMirror type;

        // Should only be set by {@link apply}
        private DefaultLocation location;

        private final DefaultApplierElementImpl impl;

        //This flag is turned on, to allow defaulting of local variables that are also typevariables
        private final boolean applyToTypeVar;

        public DefaultApplierElement(AnnotatedTypeFactory atypeFactory, Element scope, AnnotatedTypeMirror type, boolean applyToTypeVar) {
            this.atypeFactory = atypeFactory;
            this.scope = scope;
            this.type = type;
            this.impl = new DefaultApplierElementImpl();
            this.applyToTypeVar = applyToTypeVar;
        }

        public void apply(Default def) {
            this.location = def.location;
            impl.visit(type, def.anno);
        }

        /**
         * Returns true if the given qualifier should be applied to the given type.  Currently we do not
         * apply defaults to void types, packages, wildcards, and type variables.
         *
         * @param type Type to which qual would be applied
         * @return true if this application should proceed
         */
        private static boolean shouldBeAnnotated(final AnnotatedTypeMirror type, final boolean applyToTypeVar) {

            return !( type == null ||
                    // TODO: executables themselves should not be annotated
                    // For some reason polyall-tests failes with this.
                    // type.getKind() == TypeKind.EXECUTABLE ||
                    type.getKind() == TypeKind.NONE ||
                    type.getKind() == TypeKind.WILDCARD ||
                    (type.getKind() == TypeKind.TYPEVAR  && !applyToTypeVar) ||
                    type instanceof AnnotatedNoType );

        }

        private static void doApply(AnnotatedTypeMirror type, AnnotationMirror qual) {
            // Add the default annotation, but only if no other
            // annotation is present.
            if (!type.isAnnotatedInHierarchy(qual)) {
                type.addAnnotation(qual);
            }

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


        private class DefaultApplierElementImpl extends AnnotatedTypeScanner<Void, AnnotationMirror> {

            @Override
            public Void scan(AnnotatedTypeMirror t, AnnotationMirror qual) {
                if (!shouldBeAnnotated(t, applyToTypeVar)) {
                    return super.scan(t, qual);
                }

                switch (location) {
                case FIELD: {
                    if (scope.getKind() == ElementKind.FIELD &&
                            t == type) {
                        doApply(t, qual);
                    }
                    break;
                }
                case LOCAL_VARIABLE: {
                    if (scope.getKind() == ElementKind.LOCAL_VARIABLE &&
                            t == type) {
                        // TODO: how do we determine that we are in a cast or instanceof type?
                        doApply(t, qual);
                    }
                    break;
                }
                case RESOURCE_VARIABLE: {
                    if (scope.getKind() == ElementKind.RESOURCE_VARIABLE &&
                            t == type) {
                        doApply(t, qual);
                    }
                    break;
                }
                case EXCEPTION_PARAMETER: {
                    if (scope.getKind() == ElementKind.EXCEPTION_PARAMETER &&
                            t == type) {
                        doApply(t, qual);
                    }
                    break;
                }
                case PARAMETERS: {
                    if (scope.getKind() == ElementKind.PARAMETER &&
                            t == type) {
                        doApply(t, qual);
                    } else if ((scope.getKind() == ElementKind.METHOD || scope.getKind() == ElementKind.CONSTRUCTOR) &&
                            t.getKind() == TypeKind.EXECUTABLE &&
                            t == type) {

                        for (AnnotatedTypeMirror atm : ((AnnotatedExecutableType)t).getParameterTypes()) {
                            if(shouldBeAnnotated(atm, false)) {
                                doApply(atm, qual);
                            }
                        }
                    }
                    break;
                }
                case RECEIVERS: {
                    if (scope.getKind() == ElementKind.PARAMETER &&
                            t == type && "this".equals(scope.getSimpleName())) {
                        // TODO: comparison against "this" is ugly, won't work
                        // for all possible names for receiver parameter.
                        doApply(t, qual);
                    } else if ((scope.getKind() == ElementKind.METHOD) &&
                            t.getKind() == TypeKind.EXECUTABLE &&
                            t == type) {

                        final AnnotatedDeclaredType receiver = ((AnnotatedExecutableType) t).getReceiverType();
                        if(shouldBeAnnotated(receiver, false)) {
                            doApply(receiver, qual);
                        }
                    }
                    break;
                }
                case RETURNS: {
                    if (scope.getKind() == ElementKind.METHOD &&
                            t.getKind() == TypeKind.EXECUTABLE &&
                            t == type) {
                        final AnnotatedTypeMirror returnType = ((AnnotatedExecutableType)t).getReturnType();
                        if(shouldBeAnnotated(returnType, false)) {
                            doApply(returnType, qual);
                        }
                    }
                    break;
                }

                case LOWER_BOUNDS: {
                    if (this.isLowerBound) {
                        doApply(t, qual);
                    }
                    break;
                }

                case IMPLICIT_UPPER_BOUNDS: {
                    if (this.isTypeVarExtendsImplicit) {
                        doApply(t, qual);
                    }
                    break;
                }
                case EXPLICIT_UPPER_BOUNDS: {
                    if (this.isTypeVarExtendsExplicit) {
                        doApply(t, qual);
                    }
                    break;
                }
                case UPPER_BOUNDS: {
                    if (this.isTypeVarExtendsImplicit || this.isTypeVarExtendsExplicit) {
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
                    ErrorReporter
                            .errorAbort("QualifierDefaults.DefaultApplierElement: unhandled location: " +
                                    location);
                    return null;
                }
                }

                return super.scan(t, qual);
            }

            @Override
            public void reset() {
                super.reset();
                impl.isTypeVarExtendsImplicit = false;
                impl.isTypeVarExtendsExplicit = false;
                impl.isLowerBound = false;
            }

            private boolean isTypeVarExtendsImplicit = false;
            private boolean isTypeVarExtendsExplicit = false;
            private boolean isLowerBound = false;

            @Override
            public Void visitTypeVariable(AnnotatedTypeVariable type,
                    AnnotationMirror qual) {
                if (visitedNodes.containsKey(type)) {
                    return visitedNodes.get(type);
                }

                final AnnotatedTypeMirror lowerBound = type.getLowerBound();

                isLowerBound = true;
                Void r = scanAndReduce(lowerBound, qual, null);
                isLowerBound = false;
                visitedNodes.put(type, r);

                Element tvel = type.getUnderlyingType().asElement();
                // TODO: find a better way to do this
                TreePath treepath = atypeFactory.getTreeUtils().getPath(tvel);
                Tree tree = treepath == null ? null : treepath.getLeaf();

                boolean prevIsTypeVarExtendsImplicit = isTypeVarExtendsImplicit;
                boolean prevIsTypeVarExtendsExplicit = isTypeVarExtendsExplicit;

                if (tree == null) {
                    // This is not only for elements from binaries, but also
                    // when the compilation unit is no-longer available.
                    isTypeVarExtendsImplicit = false;
                    isTypeVarExtendsExplicit = true;
                } else {
                    if (tree.getKind() == Tree.Kind.TYPE_PARAMETER) {
                        TypeParameterTree tptree = (TypeParameterTree) tree;

                        List<? extends Tree> bnds = tptree.getBounds();
                        if (bnds != null && !bnds.isEmpty()) {
                            isTypeVarExtendsImplicit = false;
                            isTypeVarExtendsExplicit = true;
                        } else {
                            isTypeVarExtendsImplicit = true;
                            isTypeVarExtendsExplicit = false;
                        }
                    }
                }
                try {
                    r = scanAndReduce(type.getUpperBoundField(), qual, r);
                } finally {
                    isTypeVarExtendsImplicit = prevIsTypeVarExtendsImplicit;
                    isTypeVarExtendsExplicit = prevIsTypeVarExtendsExplicit;
                }
                visitedNodes.put(type, r);
                return r;
            }

            @Override
            public Void visitWildcard(AnnotatedWildcardType type,
                    AnnotationMirror qual) {
                if (visitedNodes.containsKey(type)) {
                    return visitedNodes.get(type);
                }
                Void r;
                boolean prevIsTypeVarExtendsImplicit = isTypeVarExtendsImplicit;
                boolean prevIsTypeVarExtendsExplicit = isTypeVarExtendsExplicit;

                WildcardType wc = (WildcardType) type.getUnderlyingType();


                final AnnotatedTypeMirror lowerBound = type.getSuperBound();

                isLowerBound = true;
                r = scanAndReduce(lowerBound, qual, null);
                isLowerBound = false;
                visitedNodes.put(type, r);

                if (wc.isUnbound() &&
                        wc.bound != null) {
                    // If the wildcard bound is implicit, look what
                    // the type variable bound would be.
                    Element tvel = wc.bound.asElement();
                    TreePath treepath = atypeFactory.getTreeUtils().getPath(tvel);
                    Tree tree = treepath == null ? null : treepath.getLeaf();

                    if (tree != null &&
                            tree.getKind() == Tree.Kind.TYPE_PARAMETER) {
                        TypeParameterTree tptree = (TypeParameterTree) tree;

                        List<? extends Tree> bnds = tptree.getBounds();
                        if (bnds != null && !bnds.isEmpty()) {
                            isTypeVarExtendsImplicit = false;
                            isTypeVarExtendsExplicit = true;
                        } else {
                            isTypeVarExtendsImplicit = true;
                            isTypeVarExtendsExplicit = false;
                        }
                    } else {
                        isTypeVarExtendsImplicit = false;
                        isTypeVarExtendsExplicit = true;
                    }
                } else {
                    isTypeVarExtendsImplicit = false;
                    isTypeVarExtendsExplicit = true;
                }
                try {
                    r = scan(type.getExtendsBoundField(), qual);
                } finally {
                    isTypeVarExtendsImplicit = prevIsTypeVarExtendsImplicit;
                    isTypeVarExtendsExplicit = prevIsTypeVarExtendsExplicit;
                }
                visitedNodes.put(type, r);
                r = scanAndReduce(type.getSuperBoundField(), qual, r);
                visitedNodes.put(type, r);
                return r;
            }
        }
    }
}
