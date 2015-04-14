package org.checkerframework.framework.util.defaults;

import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.DefaultQualifiers;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;

import java.lang.annotation.Annotation;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type.WildcardType;

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
    private final DefaultSet untypedDefaults = new DefaultSet();

    /** Mapping from an Element to the source Tree of the declaration. */
    private static final int CACHE_SIZE = 300;
    protected static final Map<Element, BoundType> elementToBoundType  = AnnotatedTypeFactory.createLRUCache(CACHE_SIZE);


    /** 
     * Defaults that apply for a certain Element.
     * On the one hand this is used for caching (an earlier name for the field was
     * "qualifierCache". It can also be used by type systems to set defaults for
     * certain Elements.
     */
    private final Map<Element, DefaultSet> elementDefaults = new IdentityHashMap<>();

    /**
     * List of DefaultLocations which are valid for untyped code defaults.
     */
    private static final DefaultLocation[] validUntypedDefaultLocations = {
        DefaultLocation.FIELD,
        DefaultLocation.PARAMETERS,
        DefaultLocation.RETURNS,
        DefaultLocation.UPPER_BOUNDS,
        DefaultLocation.EXPLICIT_UPPER_BOUNDS
    };

    /**
     * Returns an array of locations which are valid for the untyped value
     * defaults.  These are simply by syntax, since an entire file is typechecked,
     * it is not possible for local variables to be untyped.
     */
    public static DefaultLocation[] validLocationsForUntyped() {
        return validUntypedDefaultLocations;
    }

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

    /**
     * Sets the default annotation for untyped elements.
     */
    public void addUntypedDefault(AnnotationMirror untypedDefaultAnno, DefaultLocation location) {
        checkDuplicates(untypedDefaults, untypedDefaultAnno, location);
        untypedDefaults.add(new Default(untypedDefaultAnno, location));
    }

    /**
     * Sets the default annotation for untyped elements, with specific locations.
     */
    public void addUntypedDefaults(AnnotationMirror absoluteDefaultAnno, DefaultLocation[] locations) {
        for (DefaultLocation location : locations) {
            // TODO(danbrotherston): Why no check for duplicates here?
            // TODO(danbrotherston): Check for invalid locations for untyped code.
            addUntypedDefault(absoluteDefaultAnno, location);
        }
    }

    public void addAbsoluteDefaults(AnnotationMirror absoluteDefaultAnno, DefaultLocation[] locations) {
        for (DefaultLocation location : locations) {
            // TODO(danbrotherston): Why no check duplicates here?
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

            if (!newAnno.equals(previous.anno) && previous.location == newLoc) {
                final AnnotationMirror previousTop = qualHierarchy.getTopAnnotation(previous.anno);

                if (qualHierarchy.isSubtype(previousTop, newAnno)) {
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
        Element elt;
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
                           && (((GenericAnnotatedTypeFactory<?,?,?,?>) atypeFactory).getUseFlow());

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
        // TODO(danbrotherston): Why is this "Set<Default>" instead of DefaultSet
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
        
        if (untypedDefaults.size() > 0 &&
              ElementUtils.isElementFromByteCode(annotationScope) &&
              atypeFactory.declarationFromElement(annotationScope) == null) {
            for (Default def : untypedDefaults) {
                applier.apply(def);
            }
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

        /*Local type variables are defaulted to top when flow is turned on
          We only want to default the top level type variable (and not type variables that are nested
          in its bounds). E.g.,
            <T extends List<E>, E extends Object> void method() {
               T t;
            }
          We would like t to have its primary annotation defaulted but NOT the E inside its upper bound.
          we use referential equality with the top level type var to determine which ones are defaultable
        */
        private final AnnotatedTypeVariable defaultableTypeVar;

        public DefaultApplierElement(AnnotatedTypeFactory atypeFactory, Element scope, AnnotatedTypeMirror type, boolean applyToTypeVar) {
            this.atypeFactory = atypeFactory;
            this.scope = scope;
            this.type = type;
            this.impl = new DefaultApplierElementImpl();
            this.defaultableTypeVar = (applyToTypeVar) ? (AnnotatedTypeVariable) type : null;
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
                if (!shouldBeAnnotated(t, t == defaultableTypeVar)) {
                    return super.scan(t, qual);
                }

                switch (location) {
                case FIELD: {
                    if (scope != null && scope.getKind() == ElementKind.FIELD &&
                            t == type) {
                        doApply(t, qual);
                    }
                    break;
                }
                case LOCAL_VARIABLE: {
                    if (scope != null && scope.getKind() == ElementKind.LOCAL_VARIABLE &&
                            t == type) {
                        // TODO: how do we determine that we are in a cast or instanceof type?
                        doApply(t, qual);
                    }
                    break;
                }
                case RESOURCE_VARIABLE: {
                    if (scope != null && scope.getKind() == ElementKind.RESOURCE_VARIABLE &&
                            t == type) {
                        doApply(t, qual);
                    }
                    break;
                }
                case EXCEPTION_PARAMETER: {

                    if (scope != null && scope.getKind() == ElementKind.EXCEPTION_PARAMETER &&
                            t == type) {
                        doApply(t, qual);
                        if (t.getKind() == TypeKind.UNION) {
                            AnnotatedUnionType aut = (AnnotatedUnionType) t;
                            //Also apply the default to the alternative types
                            for (AnnotatedDeclaredType anno : aut
                                    .getAlternatives()) {
                                doApply(anno, qual);
                            }
                        }
                    }
                    break;
                }
                case PARAMETERS: {
                    if (scope != null && scope.getKind() == ElementKind.PARAMETER &&
                            t == type) {
                        doApply(t, qual);
                    } else if ((scope.getKind() == ElementKind.METHOD || scope.getKind() == ElementKind.CONSTRUCTOR) &&
                            t.getKind() == TypeKind.EXECUTABLE &&
                            t == type) {

                        for (AnnotatedTypeMirror atm : ((AnnotatedExecutableType)t).getParameterTypes()) {
                            if (shouldBeAnnotated(atm, false)) {
                                doApply(atm, qual);
                            }
                        }
                    }
                    break;
                }
                case RECEIVERS: {
                    if (scope != null && scope.getKind() == ElementKind.PARAMETER &&
                            t == type && "this".equals(scope.getSimpleName())) {
                        // TODO: comparison against "this" is ugly, won't work
                        // for all possible names for receiver parameter.
                        doApply(t, qual);
                    } else if (scope != null && (scope.getKind() == ElementKind.METHOD) &&
                            t.getKind() == TypeKind.EXECUTABLE &&
                            t == type) {

                        final AnnotatedDeclaredType receiver = ((AnnotatedExecutableType) t).getReceiverType();
                        if (shouldBeAnnotated(receiver, false)) {
                            doApply(receiver, qual);
                        }
                    }
                    break;
                }
                case RETURNS: {
                    if (scope != null && scope.getKind() == ElementKind.METHOD &&
                            t.getKind() == TypeKind.EXECUTABLE &&
                            t == type) {
                        final AnnotatedTypeMirror returnType = ((AnnotatedExecutableType)t).getReturnType();
                        if (shouldBeAnnotated(returnType, false)) {
                            doApply(returnType, qual);
                        }
                    }
                    break;
                }


                case IMPLICIT_LOWER_BOUNDS: {
                    if (isLowerBound && boundType.isOneOf(BoundType.UNBOUND, BoundType.UPPER, BoundType.UNKNOWN)) {
                        doApply(t, qual);
                    }
                    break;
                }

                case EXPLICIT_LOWER_BOUNDS: {
                    if (isLowerBound && boundType.isOneOf(BoundType.LOWER)) {
                        doApply(t, qual);
                    }
                    break;
                }

                case LOWER_BOUNDS: {
                    if (isLowerBound) {
                        doApply(t, qual);
                    }
                    break;
                }

                case IMPLICIT_UPPER_BOUNDS: {
                    if (isUpperBound && boundType.isOneOf(BoundType.UNBOUND, BoundType.LOWER)) {
                        doApply(t, qual);
                    }
                    break;
                }
                case EXPLICIT_UPPER_BOUNDS: {
                    if (isUpperBound && boundType.isOneOf(BoundType.UPPER, BoundType.UNKNOWN)) {
                        doApply(t, qual);
                    }
                    break;
                }
                case UPPER_BOUNDS: {
                    if (this.isUpperBound) {
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
                impl.isLowerBound = false;
                impl.isUpperBound = false;
                impl.boundType = BoundType.UNBOUND;
            }

            //are we currently defaulting the lower bound of a type variable or wildcard
            private boolean isLowerBound = false;

            //are we currently defaulting the upper bound of a type variable or wildcard
            private boolean isUpperBound  = false;

            //the bound type of the current wildcard or type variable being defaulted
            private BoundType boundType = BoundType.UNBOUND;

            @Override
            public Void visitTypeVariable(AnnotatedTypeVariable type,
                    AnnotationMirror qual) {
                if (visitedNodes.containsKey(type)) {
                    return visitedNodes.get(type);
                }

                visitBounds(type, type.getUpperBound(), type.getLowerBound(), qual);
                return null;
            }

            @Override
            public Void visitWildcard(AnnotatedWildcardType type,
                    AnnotationMirror qual) {
                if (visitedNodes.containsKey(type)) {
                    return visitedNodes.get(type);
                }

                visitBounds(type, type.getExtendsBound(), type.getSuperBound(), qual);
                return null;
            }

            /**
             * Visit the bounds of a type variable or a wildcard and potentially apply qual
             * to those bounds.  This method will also update the boundType, isLowerBound, and isUpperbound
             * fields.
             */
            protected void visitBounds(AnnotatedTypeMirror boundedType, AnnotatedTypeMirror upperBound,
                                       AnnotatedTypeMirror lowerBound, AnnotationMirror qual) {

                final boolean prevIsUpperBound = isUpperBound;
                final boolean prevIsLowerBound = isLowerBound;
                final BoundType prevBoundType = boundType;

                boundType = getBoundType(boundedType, atypeFactory);

                try {
                    isLowerBound = true;
                    isUpperBound = false;
                    scanAndReduce(lowerBound, qual, null);

                    visitedNodes.put(type, null);


                    isLowerBound = false;
                    isUpperBound = true;
                    scanAndReduce(upperBound, qual, null);

                    visitedNodes.put(type, null);

                } finally {
                    isUpperBound = prevIsUpperBound;
                    isLowerBound = prevIsLowerBound;
                    boundType = prevBoundType;
                }
            }
        }
    }

    enum BoundType {

        /**
         * Indicates an upper bounded type variable or wildcard
         */
        UPPER,

        /**
         * Indicates a lower bounded type variable or wildcard
         */
        LOWER,

        /**
         * Neither bound is specified, BOTH are implicit
         */
        UNBOUND,

        /**
         * For bytecode, or trees for which we no longer have the compilation unit.
         * We treat UNKNOWN bounds as if they are an UPPER bound.
         */
        UNKNOWN;

        public boolean isOneOf(final BoundType ... choices) {
            for (final BoundType choice : choices) {
                if (this == choice) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * @param type The type whose boundType is returned.
     *             type must be an AnnotatedWildcardType or AnnotatedTypeVariable
     * @return The boundType for type
     */
    private static BoundType getBoundType(final AnnotatedTypeMirror type,
                                          final AnnotatedTypeFactory typeFactory) {
        if (type instanceof AnnotatedTypeVariable) {
            return getTypeVarBoundType((AnnotatedTypeVariable) type, typeFactory);
        }

        if (type instanceof AnnotatedWildcardType) {
            return getWilcardBoundType((AnnotatedWildcardType) type, typeFactory);
        }

        ErrorReporter.errorAbort("Unexpected type kind: type=" + type);
        return null; //dead code
    }

    /**
     * @return the bound type of the input typeVar
     */
    private static BoundType getTypeVarBoundType(final AnnotatedTypeVariable typeVar,
                                                 final AnnotatedTypeFactory typeFactory) {
        return getTypeVarBoundType((TypeParameterElement) typeVar.getUnderlyingType().asElement(), typeFactory);
    }

    /**
     * @return The boundType (UPPER, UNBOUND, or UNKNOWN) of the declaration of typeParamElem.
     */
    private static BoundType getTypeVarBoundType(final TypeParameterElement typeParamElem,
                                                 final AnnotatedTypeFactory typeFactory) {
        final BoundType prev = elementToBoundType.get(typeParamElem);
        if (prev != null) {
            return prev;
        }

        TreePath declaredTypeVarEle = typeFactory.getTreeUtils().getPath(typeParamElem);
        Tree typeParamDecl = declaredTypeVarEle == null ? null : declaredTypeVarEle.getLeaf();

        final BoundType boundType;
        if (typeParamDecl == null) {
            // This is not only for elements from binaries, but also
            // when the compilation unit is no-longer available.
            boundType = BoundType.UNKNOWN;

        } else {
            if (typeParamDecl.getKind() == Tree.Kind.TYPE_PARAMETER) {
                final TypeParameterTree tptree = (TypeParameterTree) typeParamDecl;

                List<? extends Tree> bnds = tptree.getBounds();
                if (bnds != null && !bnds.isEmpty()) {
                    boundType = BoundType.UPPER;
                } else {
                    boundType = BoundType.UNBOUND;
                }
            } else {
                ErrorReporter.errorAbort("Unexpected tree type for typeVar Element:\n"
                                       + "typeParamElem=" + typeParamElem + "\n"
                                       + typeParamDecl);
                boundType = null; //dead code
            }
        }

        elementToBoundType.put(typeParamElem, boundType);
        return boundType;
    }

    /**
     * @return the BoundType of annotatedWildcard.  If it is unbounded, use the type parameter to
     * which its an argument
     */
    public static BoundType getWilcardBoundType(final AnnotatedWildcardType annotatedWildcard,
                                                final AnnotatedTypeFactory typeFactory) {

        final WildcardType wildcard = (WildcardType) annotatedWildcard.getUnderlyingType();

        final BoundType boundType;
        if (wildcard.isUnbound() && wildcard.bound != null) {
            boundType = getTypeVarBoundType((TypeParameterElement) wildcard.bound.asElement(), typeFactory);

        } else {
            //note: isSuperBound will be true for unbounded and lowers, but the unbounded case is already handled
            boundType = wildcard.isSuperBound() ? BoundType.LOWER : BoundType.UPPER;
        }

        return boundType;
    }
}
