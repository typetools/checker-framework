package checkers.util;

import checkers.quals.*;
import checkers.types.*;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

/**
 * Utilities for working with annotated generic types, and particularly,
 * applying annotations from annotated type arguments to type variables.
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public class GenericsUtils {

    public static boolean DEBUG = false;

    private final ProcessingEnvironment env;
    private final Types types;
    private final Elements elements;
    private final AnnotatedTypeFactory factory;
    private final Trees trees;

    /**
     * Creates a {@link GenericsUtils} instance.
     *
     * @param env the {@link ProcessingEnvironment} to use for type utilities
     * @param factory the {@link AnnotatedTypeFactory} to use for getting
     *                annotated types
     */
    public GenericsUtils(ProcessingEnvironment env,
            AnnotatedTypeFactory factory) {
        this.env = env;
        this.types = env.getTypeUtils();
        this.elements = env.getElementUtils();

        @Nullable Trees trees = Trees.instance(env);
        assert trees != null; /*nninvariant*/
        this.trees = trees;

        this.factory = factory;
    }

    /**
     * A visitor for types that tracks locations as it descends into generic
     * types.
     */
    public static class LocationVisitor<R,P> 
        extends SimpleTypeVisitor6<R, P> {
        
        private List<Integer> loc = new ArrayList<Integer>();
        
        /**
         * @return the location of the currently visited type
         */
        public AnnotationLocation getCurrentLocation() {
            return AnnotationLocation.fromList(loc);
        }
        
        protected final R scan(TypeMirror t, P p) {
            return (t == null) ? null : visit(t, p);
        }
        
        protected R reduce(R r1,R r2) {
            return (r1 != null) ? r1 : r2;
        }
        
        protected R scanAndReduce(TypeMirror t, P p, R r) {
            return reduce(scan(t, p), r);
        }
        
        protected @Nullable R scan(Iterable<? extends TypeMirror> types, P p) {
            boolean first = true;
            @Nullable R r = null;
            for (TypeMirror t : types) {
                r = first ? scan(t, p) : scanAndReduce(t, p, r);
            }
            return r;
        }
        
        /**
         * This method needs to be called to visit 
         * type parameters.
         */
        @Override
        public R visitDeclared(DeclaredType t, P p) {
            R r = super.visitDeclared(t, p);
            final int depth = loc.size();
            loc.add(0);
            int i = 0;

            for (TypeMirror arg : t.getTypeArguments()) {
                loc.set(depth, i++);
                r = scanAndReduce(arg, p, r);
            }
            try {
                return r;
            } finally {
                // calls remove(int) not remove(Object)
                loc.remove(depth);
            }
        }
    }
    
    /**
     * A visitor for types that maps types to locations, used for matching
     * annotation locations to types.
     */
    class MatchAnnotationsVisitor extends LocationVisitor<Void, Void> {

        private final Map<AnnotationLocation, TypeMirror> locs =
            new HashMap<AnnotationLocation, TypeMirror>();

        @Override
        public Void visitDeclared(DeclaredType t, Void p) {
            locs.put(getCurrentLocation(), t);
            return super.visitDeclared(t, p);
        }

        @Override
        public Void visitTypeVariable(TypeVariable t, Void p) {
            locs.put(getCurrentLocation(), t);
            return super.visitTypeVariable(t, p);
        }

        public Map<AnnotationLocation, TypeMirror> getTypeLocations() {
            return Collections.<@NonNull AnnotationLocation, @NonNull TypeMirror>unmodifiableMap(locs);
        }

    }

    /**
     * A visitor for types that matches type variables to annotations.
     */
    class ApplyAnnotationsVisitor extends LocationVisitor<Void, Void> {

        public final Set<AnnotationData> annos = new HashSet<AnnotationData>();
        private final Map<TypeMirror, Set<AnnotationData>> matched;
        private final AnnotationFactory af;
        private final boolean wildcardExtends;

        public ApplyAnnotationsVisitor(Map<TypeMirror, Set<AnnotationData>> matched,
                AnnotationFactory af, boolean wildcardExtends) {
            this.matched = matched;
            this.af = af;
            this.wildcardExtends = wildcardExtends;
        }

        @Override
        public Void visitTypeVariable(TypeVariable t, Void p) {

            // Get the annotation data for this type variable.
            Set<AnnotationData> ad = matched.get(t);

            if (ad == null) 
                return super.visitTypeVariable(t, p);

            for (AnnotationData a : ad) {
                AnnotationLocation thisLoc = getCurrentLocation();

                AnnotationLocation newLocation;
                {
                    // Take the current location and append the location of
                    // the current annotation.
                    List<Integer> lst = new LinkedList<Integer>(thisLoc.asList());
                    lst.addAll(a.getLocation().asList());
                    newLocation = AnnotationLocation.fromList(lst);
                }

                // FIXME: should be a copy of the annotation
                AnnotationData newAnnotation;
                {
                    @Nullable TypeElement te = (TypeElement)types.asElement(a.getType());
                    assert te != null; /*nninvariant*/
                    newAnnotation = af.createAnnotation(te.getQualifiedName(), newLocation);
                }

                annos.add(newAnnotation);
            }

            return super.visitTypeVariable(t, p);
        }

        @Override
        public Void visitWildcard(WildcardType t, Void p) {

            if (wildcardExtends && t != null && t.getExtendsBound() != null)
                visit(t.getExtendsBound(), p);

            return super.visitWildcard(t, p);
        }

    }

    /**
     * Use the MatchAnnotationsVisitor to match annotations with a certain
     * location to types with the same location. Often {@code type} and {@code
     * source} will be the type and {@link AnnotatedClassType} for a common
     * {@link Element}, respectively.
     *
     * @param type the type to visit
     * @param source the source of annotations for that type
     * @return a mapping from types to annotations that have the same
     *         annotations
     */
    Map<TypeMirror, Set<AnnotationData>> matchAnnotations(TypeMirror type,
            AnnotatedClassType source) {

        // An overview of this method's implementation:
        //
        // We have "Map<@X String, @Y String> m" and want the (annotated)
        // return type of "m.keySet()" (which we know should be Set<@X
        // String>).
        //
        // This method takes the original type of "m" (Map<String, String>) and
        // the annotations on it (as an ACT: {X@[0], Y@[1]}). 
        //
        // Roughly, we find all type variables that the type of "m" could
        // possibly use, and map them to their locations as they are found in
        // (Map<String, String>). So in this case we'll get {[0] -> K, [1] ->
        // V}. (Each of these mappings is the "matchEntry" below.)
        //
        // Then, for each of these mappings, we look for annotations that have
        // locations at *or below* the location in the mapping. If we find
        // them, we put them into a map with the locations they'd have if they
        // were directly on the type variable. This means that X@[0] on K would
        // be X@[]; for "Map<List<@Z String>, @Y String>, Z@[0, 0] on K would
        // be Z@[0].
        //
        // We don't touch the return type of "m.keySet()" yet; the mappings
        // that this method produces are used by applyAnnotations to do that
        // later.
        
        Map<TypeMirror, Set<AnnotationData>> typevars =
            new HashMap<TypeMirror, Set<AnnotationData>>();

        Map<AnnotationLocation, AnnotationData> annotations =
            mapLocations(source.getAnnotationData(true));

        assert type instanceof DeclaredType;

        DeclaredType dt = (DeclaredType)type;

        // Get the this type mirror and its supertypes for checking.
        List<TypeMirror> tms = new LinkedList<TypeMirror>();
        tms.add(dt);
        tms.addAll(types.directSupertypes(dt));

        AnnotationFactory af = new AnnotationFactory(env);

        for (TypeMirror tm : tms) {

            // Get the type as used and the type as declared. atm will be
            // something like Set<String> and gtm will be something like
            // Set<E>.
            DeclaredType atm = (DeclaredType)tm;
            @Nullable Element elt = types.asElement(tm);
            assert elt != null; /*nninvariant*/
            DeclaredType gtm = (DeclaredType)elt.asType();

            // Get types (including type vars) by their locations.
            Map<AnnotationLocation, TypeMirror> typeLocations;
            {
                MatchAnnotationsVisitor mav = new MatchAnnotationsVisitor();
                mav.visit(gtm, null);
                typeLocations = mav.getTypeLocations();
            }

            // For each location -> type mapping in typeLocations, look for
            // "original" annotations that have the same location or a
            // sub-location as the type's location.
            for (Map.Entry<AnnotationLocation, TypeMirror> matchEntry :
                    typeLocations.entrySet()) {

                AnnotationLocation matchedLoc = matchEntry.getKey();
                TypeMirror matchedType = matchEntry.getValue();

                // Skip raw type annotations (because we should be operating on
                // a generic type, not on a type variable.) 
                // TODO: verify this
                if (matchedLoc == AnnotationLocation.RAW)
                    continue;

                // Iterate through the original annotations to see if any have
                // the same location or a sub-location of matchedType's
                // location; if they do, copy the annotations to new locations
                // in the result set.
                for (Map.Entry<AnnotationLocation, AnnotationData> applyEntry :
                        annotations.entrySet()) {

                    AnnotationLocation applyLoc = applyEntry.getKey();

                    // Stop if the applied location doesn't fall under the
                    // matched type location.
                    if (!applyLoc.isSubtreeOf(matchedLoc))
                        continue;

                    AnnotationData applyAnno = applyEntry.getValue();

                    // Get the name of the annotation.
                    CharSequence applyName;
                    {
                        TypeMirror annoType = applyAnno.getType();
                        @Nullable TypeElement annoElt = (TypeElement)types.asElement(annoType);
                        assert annoElt != null; /*nninvariant*/
                        applyName = annoElt.getQualifiedName();
                    }

                    // Figure out where the applied annotation belongs.
                    AnnotationLocation newLocation;
                    {
                        List<Integer> applyList = applyLoc.asList();
                        List<Integer> trimmedList = applyList.subList(1,
                                applyList.size());
                        newLocation = AnnotationLocation.fromList(trimmedList);
                    }

                    // Initialize the Set<AnnotationData> result value if it
                    // doesn't exist.
                    if (!typevars.containsKey(matchedType))
                        typevars.put(matchedType, new HashSet<AnnotationData>());
                    
                    // Copy applyAnno to the new location.
                    // FIXME: this isn't a real copy
                    AnnotationData newAnnotation =
                        af.createAnnotation(applyName, newLocation);
                    typevars.get(matchedType).add(newAnnotation);
                }
            }
        }

        return typevars;
    }

    /**
     * Uses an ApplyAnnotationsVisitor to determine the annotations on the
     * given type. Annotations are applied from a given set of matches, which
     * can be determined by {@link GenericsUtils#matchAnnotations}.
     *
     * @param type the type to apply annotations to
     * @param matches the annotations to apply
     * @param wcExtends whether to apply over wildcards with an "extends" clause
     * @return the annotations on {@code type} as determined by {@code matches}
     */
    Set<AnnotationData> applyAnnotations(TypeMirror type,
            Map<TypeMirror, Set<AnnotationData>> matches, boolean wcExtends) {
        AnnotationFactory af = new AnnotationFactory(this.env);
        ApplyAnnotationsVisitor aav = new ApplyAnnotationsVisitor(matches, af, wcExtends);
        aav.visit(type, null);
        return aav.annos;
    }

    /**
     * @param annotations a set of annotations
     * @return {@code annotations} in a map keyed by their locations
     */
    static Map<AnnotationLocation, AnnotationData>
        mapLocations(Set<AnnotationData> annotations) {

        Map<AnnotationLocation, AnnotationData> byLoc =
            new HashMap<AnnotationLocation, AnnotationData>();

        for (AnnotationData annotation : annotations)
            byLoc.put(annotation.getLocation(), annotation);
        return Collections.<@NonNull AnnotationLocation, @NonNull AnnotationData>unmodifiableMap(byLoc);
    }

    public Set<AnnotationData> annotationsFor(TypeMirror type, Element elt) {
        return annotationsFor(type, elt, Collections.<@NonNull TypeMirror, @NonNull Set<@NonNull AnnotationData>>emptyMap(), false);
    }


    /*
     * Determines the annotations for a type (which presumably is a type
     * variable or contains type variables) based on the given expression and
     * annotation source. The expression is that which is a use of the type
     * variables in type; the annotation source is usually the {@link
     * AnnotatedClassType} of the expression's element.
     */
    public Set<AnnotationData> annotationsFor(TypeMirror type, Element elt,
            Map<TypeMirror, Set<AnnotationData>> moreMappings, boolean wcExtends) {

        if (DEBUG) {
            System.out.println("**    TYPE: " + type + " / " + type.getKind());
            System.out.println("** ELEMENT: " + elt + " / " + elt.getKind());
        }

        @Nullable TypeMirror expression = elt.asType();
        AnnotatedClassType source = factory.getClass(elt);

        if (expression.getKind() == TypeKind.EXECUTABLE) {
            @Nullable ExecutableType method = (ExecutableType)expression;
            assert method != null; /*nninvariant*/
            expression = method.getReturnType();
        }

        if (expression == null || (expression != null && expression.getKind() != TypeKind.DECLARED)) // FIXME: flow workaround
            return Collections.<@NonNull AnnotationData>emptySet();

        assert expression != null; // FIXME: flow workaround
        Map<TypeMirror, Set<AnnotationData>> mappings =
            this.matchAnnotations(expression, source);
        mappings.putAll(moreMappings);

        if (DEBUG) {
            System.out.println("** MAPPING: " + mappings);
        }
        
        Set<AnnotationData> result =
            this.applyAnnotations(type, mappings, wcExtends);

        if (DEBUG) {
            System.out.println("--     RESULT: " + result);
        }

        return result;
    }

    /**
     * Determines the iterated type in an enhanced for loop. For instance, if
     * iterating over a {@code Set<String>}, the method returns the
     * declared type of {@code Set<String>.iterator().next()}, which is
     * the type of {@code Set<E>.iterator.next()}, which is {@code E}.<br />
     * 
     * <b>Deprecated</b>: use iteratedType(TypeMirror) instead.
     *
     * @param tree the enhanced for loop
     * @return the type of the iterated argument
     */
    @Deprecated
    public @Nullable TypeMirror iteratedType(EnhancedForLoopTree tree) {

        // This method makes the distinction between a two different kinds of
        // types. There are types "as declared", such as "Iterable<T>" or
        // "Set<E>"; there are types "as used", such as "Set<String>" or
        // "Iterable<String>", and also "Iterable<E>" in "Set<E> extends
        // Iterable<E>".
        //
        // Really, what we're doing here is taking a type as instantiated --
        // Set<String> -- that we're iterating over, finding its type as
        // declared -- Set<E> -- finding the Iterable type as used --
        // Iterable<E> (not Iterable<T>, as it's declared) -- and getting the
        // return value of "Iterable.iterator().next()", which in this case is
        // E.
        //
        // Once we have done this, we can use GenericsUtils to match type
        // variables to get the annotations on "Iterable.iterator().next().
        // This is why we want to get Iterable<E> and eventually E, not
        // Iterable<T> and eventually T -- GenericsUtils can find that
        // E=@NonNull String for Set<@NonNull String>.

        // Get the element for the for expression.
//        Element exprElt;
//
//        if (tree.getExpression().getKind() == Tree.Kind.NEW_CLASS)
//            exprElt = InternalUtils.symbol(((NewClassTree)(tree.getExpression())).getIdentifier());
//        else
//            exprElt = InternalUtils.symbol(tree.getExpression());
//
//        // Get the raw type for Iterable, to compare against later.
//        TypeMirror iterableRawType;
//        {
//            TypeElement iterableTypeElt =
//                elements.getTypeElement("java.lang.Iterable");
//            iterableRawType = types.erasure(iterableTypeElt.asType());
//        }
//
//        // Get the supertypes of the type of the expression as they are used.
//        Set<TypeMirror> superTypes;
//        {
//           TypesUtils types2 = new TypesUtils(this.env);
//
//           if (exprElt instanceof ExecutableElement) {
//               superTypes = types2.superTypes(
//                       ((ExecutableElement)exprElt).getReturnType());
//           } else {
//               // Get type of the for expression as it has been declared (Set<E>
//               // instead of Set<@NonNull String>.
//               Element exprClass = types.asElement(exprElt.asType());
//               if (exprClass == null)
//                   return null;
//               assert exprClass instanceof TypeElement : exprClass.getKind();
//
//               // Get the supertypes as they are used (Iterable<E> instead of
//               // Iterable<T>).
//               superTypes = types2.superTypes(((TypeElement)exprClass).asType());
//           }
//        }
//
//        // Find the supertype that is the same as Iterable<T> when erased --
//        // the result will be something like Iterable<E> or Iterable<A>.
//        TypeMirror iterableType = null;
//        for (TypeMirror superType : superTypes) {
//            if (types.isSameType(types.erasure(superType), iterableRawType)) {
//                iterableType = superType;
//                break;
//            }
//        }
//
//        // FIXME: determine why iterable type is sometimes null
//        if (iterableType == null)
//            return null;
//
//        // Get the Iterable type as used, as an element.
//        Element iterableTypeAsElt = types.asElement(iterableType);
//
//        // Get the element for the Iterable.iterator() method.
//        ExecutableElement iteratorMethod =
//            ElementFilter.methodsIn(iterableTypeAsElt.getEnclosedElements()).get(0);
//
//        // Get the type of the Iterable.iterator() method as used.
//        ExecutableType iteratorMethodType =
//            (ExecutableType)types.asMemberOf((DeclaredType)iterableType,
//                                              iteratorMethod);
//
//        // Get the element for the Iterator class as returned by the (as used)
//        // Iterable type's iterator() method.
//        TypeElement iteratorClass =
//            (TypeElement)types.asElement(iteratorMethodType.getReturnType());
//
//        // Get the type of the Iterator's .next() method.
//        ExecutableType iteratorDotNext = null;
//        for (ExecutableElement anIteratorMethod :
//                ElementFilter.methodsIn(iteratorClass.getEnclosedElements())) {
//            if (elements.getName("next").equals(anIteratorMethod.getSimpleName())) {
//                iteratorDotNext = (ExecutableType)
//                    types.asMemberOf((DeclaredType)iteratorMethodType.getReturnType(),
//                            anIteratorMethod);
//                break;
//            }
//        }
//
//        assert iteratorDotNext != null;
//
//        // Return the return type of Iterator.next().
//        return iteratorDotNext.getReturnType();
        
        // Trying to do a fix!
        @Nullable TreePath path = TreePath.getPath(factory.getCompilationUnitTree(), 
                tree.getExpression());
        @Nullable TypeMirror type = this.trees.getTypeMirror(path);

        if (type == null || (type != null && type.getKind() == TypeKind.ARRAY)) /*nnbug*/ // FIXME: flow workaround
            return null;
        
        assert type != null; // FIXME: flow workaround
        DeclaredType dt = (DeclaredType)type;
        DeclaredType actualType = (DeclaredType)dt.asElement().asType();
        return iteratedType(actualType);
    }
    
    /**
     * Determines the iterated type in the given iterable type, which is
     * simply the component type of the array or the type variable of the 
     * {@code Iterable} interface, i.e. {@code E} in {@code Iterable<E>}
     * 
     * @param type  an iterable type
     * @param the type of the iterated value
     */
    public @Nullable TypeMirror iteratedType(TypeMirror type) {
        if (type.getKind() != TypeKind.ARRAY
                && type.getKind() != TypeKind.DECLARED)
            return null;
        
        if (type.getKind() == TypeKind.ARRAY)
            return ((ArrayType)type).getComponentType();
        
        @Nullable TypeElement ite = elements.getTypeElement("java.lang.Iterable");
        assert ite != null; /*nninvariant*/
        DeclaredType iterableType = (DeclaredType) ite.asType();
        
        for (TypeMirror superType : new TypesUtils(env).superTypes(type)) {
            if (types.isSameType(types.erasure(superType),
                    types.erasure(iterableType))) {
                DeclaredType iterable = (DeclaredType) superType;
                if (iterable.getTypeArguments().isEmpty()) {
                    @Nullable TypeElement ote = 
                        elements.getTypeElement("java.lang.Object");
                    assert ote != null; /*nninvariant*/
                    return ote.asType();
                } else
                    return iterable.getTypeArguments().get(0);
            }
        }
        return null;
    }
}
