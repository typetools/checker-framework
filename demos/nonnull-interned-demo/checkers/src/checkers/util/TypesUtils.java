package checkers.util;

import static javax.lang.model.util.ElementFilter.methodsIn;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import checkers.quals.*;
import checkers.types.AnnotationData;
import checkers.types.AnnotationLocation;
import checkers.util.GenericsUtils.LocationVisitor;

/**
 * A Utilities class that helps with {@code Element}'s
 * 
 */
// TODO: This class needs significant restructuring
@DefaultQualifier("checkers.nullness.quals.NonNull")
public final class TypesUtils {

    private ProcessingEnvironment env;

    /**
     * Constructor for TypeUtils.
     * 
     * @param environment
     *            the {@code ProcessingEnvironment} of the Annotation
     *            Type Processor
     */
    public TypesUtils(ProcessingEnvironment environment) {
        this.env = environment;
    }

    /**
     * A utility method that takes the element for a class/interface
     * type and returns a set of {@link TypeElement}s representing all
     * of the supertypes of that type.
     * 
     * @param subtype
     *            the element of the type for which all supertypes
     *            will be obtained
     * @return an unmodifiable set of supertypes for {@code subtype},
     */
    @Deprecated
    public Set<TypeMirror> superTypes(Element subtype) {
        return superTypes(subtype.asType());
    }
    
    /**
     * A utility method that takes the element for a class/interface
     * type and returns a set of {@link TypeElement}s representing all
     * of the supertypes of that type.
     * 
     * @param subtype
     *            the element of the type for which all supertypes
     *            will be obtained
     * @return an unmodifiable set of supertypes for {@code subtype},
     */
    public Set<TypeMirror> superTypes(TypeMirror subtype) {

        Set<TypeMirror> supertypes = new HashSet<TypeMirror>();
        if (subtype == null) 
            return supertypes;

        // Set up a stack containing the type mirror of subtype, which
        // is our starting point.
        Deque<TypeMirror> stack = new ArrayDeque<TypeMirror>();
        stack.push(subtype);

        while (!stack.isEmpty()) {
            TypeMirror current = stack.pop();

            // For each direct supertype of the current type, if it
            // hasn't already been visited, push it onto the stack and
            // add it to our supertypes set.
            for (TypeMirror supertype : env.getTypeUtils()
                    .directSupertypes(current)) {
                if (!supertypes.contains(supertype)) {
                    stack.push(supertype);
                    supertypes.add(supertype);
                }
            }
        }

        return Collections.<@NonNull TypeMirror>unmodifiableSet(supertypes);
    }

    /**
     * A utility method that takes a Method element and returns a set
     * of all elements that this method overrides (as
     * {@link ExecutableElement}s)
     * 
     * @param method
     *            the overriding method
     * @return an unmodifiable set of {@link ExecutableElements}
     *         representing the elements that method overrides
     */
    public Set<ExecutableElement> overriddenMethods(
            ExecutableElement method) {
        Set<TypeMirror> supertypes =
            superTypes(method.getEnclosingElement().asType());
        return overriddenMethods(method, supertypes);
    }

    /**
     * A utility method that takes the element for a method and the
     * set of all supertypes of the method's containing class and
     * returns the set of all elements that method overrides (as
     * {@link ExecutableElement}s).
     * 
     * @param method
     *            the overriding method
     * @param supertypes
     *            the set of supertypes to check for methods that are
     *            overriden by {@code method}
     * @return an unmodified set of {@link ExecutableElements}
     *         representing the elements that {@code} method overrides
     *         among {@code supertypes}
     */
    public Set<ExecutableElement> overriddenMethods(
            ExecutableElement method, Set<TypeMirror> supertypes) {

        Set<ExecutableElement> overrides =
            new HashSet<ExecutableElement>();

        for (TypeMirror supertype : supertypes) {
            @Nullable TypeElement superElement = 
                (TypeElement) env.getTypeUtils().asElement(supertype);
            assert superElement != null; /*nninvariant*/
            // For all method in the supertype, add it to the set if
            // it overrides the given method.
            for (ExecutableElement supermethod : methodsIn(superElement
                    .getEnclosedElements())) {
                if (env.getElementUtils().overrides(method, supermethod,
                        superElement)) {
                    overrides.add(supermethod);
                    break;
                }
            }
        }

        return Collections.<@NonNull ExecutableElement>unmodifiableSet(overrides);
    }

    public static Name getQualifiedName(DeclaredType type) {
        TypeElement element = (TypeElement) type.asElement();
        return element.getQualifiedName();
    }
    
    public static int getArrayDimensions(ArrayType array) {
        int result = 1;
        TypeMirror component = array.getComponentType();
        while (component.getKind() == TypeKind.ARRAY) {
            component = ((ArrayType)component).getComponentType();
            ++ result;
        }
        return result;
    }
    
    public static TypeMirror getDeepComponent(ArrayType array) {
        TypeMirror result = array.getComponentType();
        while (result.getKind() == TypeKind.ARRAY)
            result = ((ArrayType)result).getComponentType();
        return result;
    }

    private Set<AnnotationData> getAnnonAt(Set<AnnotationData> data, 
            AnnotationLocation loc) {
        Set<AnnotationData> results = new HashSet<AnnotationData>();
        for (AnnotationData annon : data)
            if (annon.getLocation().equals(loc))
                results.add(annon);

        return results;
    }
    
    private Set<AnnotationData> asSubRoot(Set<AnnotationData> data,
            AnnotationLocation newRoot) {
        Set<AnnotationData> results = new HashSet<AnnotationData>();
        for (AnnotationData annon : data) {
            @Nullable AnnotationData newAnnot = AnnotationLocation.asSubOf(annon, newRoot, env);
            if (newAnnot != null)
                results.add(newAnnot);
        }
        return results;
    }
    
    public String toString(Set<AnnotationData> annotations) {
        StringBuilder str = new StringBuilder();
        boolean isFirst = true;
        
        for (AnnotationData annon : annotations) {
            if (!isFirst) str.append(' ');
            else isFirst = false;
            str.append('@');
            if (annon.getType().getKind() == TypeKind.DECLARED) {
                str.append(((DeclaredType)annon.getType()).asElement().getSimpleName());
            } else
                str.append(annon.getType());
        }
        return str.toString();
    }
    
    public String toString(TypeMirror type, Set<AnnotationData> set) {
        StringBuilder str = new StringBuilder();
        str.append(toString(getAnnonAt(set, AnnotationLocation.RAW)));
        str.append(' ');
        
        switch (type.getKind()) {
        case DECLARED:
            DeclaredType dt = (DeclaredType)type;
            str.append(env.getTypeUtils().erasure(dt));
            if (!dt.getTypeArguments().isEmpty()) {
                str.append('<');
                int[] loc = { 0 };
                boolean isFirst = true;
                for (TypeMirror st : dt.getTypeArguments()) {
                    if (!isFirst)
                        str.append(", ");
                    AnnotationLocation curLoc = AnnotationLocation.fromArray(loc);
                    str.append(toString(st, asSubRoot(set, curLoc)));
                }
                str.append('>');
            }
            break;
        case ARRAY:
            str.append(type);
            // TODO: Handle Arrays
            break;
        default:
            str.append(type);
        }
        
        return str.toString();
    }
    
    public static int getDepth(TypeMirror type) {
        Set<AnnotationLocation> validLocation = getValidLocations(type);
        int length = 0;
        for (AnnotationLocation loc : validLocation) {
            if (loc.asList().size() > length)
                length = loc.asList().size();
        }
        return length;
    }
    
    /**
     * Returns the valid annotation locations for a given type
     * 
     * @param type
     * @return
     */
    public static Set<AnnotationLocation> getValidLocations(TypeMirror type) {
        return locationFinder.visit(type);
    }
    
    /**
     * A location visitor that finds the location of the immutable
     * types by default.
     * 
     * The visitor returns a set of the primitives (or their boxed
     * types) and String types.
     */
    private static TypeVisitor<Set<AnnotationLocation>, @Nullable Void> locationFinder =
        new LocationVisitor<@NonNull Set<@NonNull AnnotationLocation>, @Nullable Void>() {

        @Override
        protected Set<AnnotationLocation> reduce(
                Set<AnnotationLocation> s1, Set<AnnotationLocation> s2) {
            Set<AnnotationLocation> result =
                new HashSet<AnnotationLocation>();
            if (s1 != null)
                result.addAll(s1);
            if (s2 != null)
                result.addAll(s2);
            return result;
        }

        protected Set<AnnotationLocation> defaultAction(TypeMirror e,
                @Nullable Void p) {
            return Collections.<@NonNull AnnotationLocation>singleton(getCurrentLocation());
        }

        public Set<AnnotationLocation> visitArray(ArrayType t, @Nullable Void p) {
            // FIXME: Fix Generic arrays: indexing is already broken!
            Set<AnnotationLocation> result = new HashSet<AnnotationLocation>();
            result.add(AnnotationLocation.RAW);
            for (int i = 0; i < getArrayDimensions(t); ++i) {
                result.add(AnnotationLocation.fromArray(new int[] { i }));
            }
            return reduce(result, visit(getDeepComponent(t), p));
        }

        public Set<AnnotationLocation> visitDeclared(DeclaredType t,
                @Nullable Void p) {
            
            return reduce(Collections.<@NonNull AnnotationLocation>singleton(getCurrentLocation()), super.visitDeclared(t, p));
        }
        
    };

    /**
     * Determines the locations on a type; useful for including/excluding
     * annotations on all parts of a type.
     *
     * @param t the type to determine locations for @return the {@link
     *        AnnotationLocation}s for which an annotation might appear on the
     *        type
     */
    public Set<AnnotationLocation> allLocations(TypeMirror t) {

        if (t == null)
            throw new IllegalArgumentException();

        final Set<AnnotationLocation> locations = new HashSet<AnnotationLocation>();
        Types types = env.getTypeUtils();

        new GenericsUtils.LocationVisitor<Void, Void>() {
            @Override
            public Void visitDeclared(DeclaredType t, Void p) {
                locations.add(this.getCurrentLocation());
                return super.visitDeclared(t, p);
            }
            @Override
            public Void visitTypeVariable(TypeVariable t, Void p) {
                locations.add(this.getCurrentLocation());
                return super.visitTypeVariable(t, p);
            }
            @Override
            public Void visitWildcard(WildcardType t, Void p) {
                locations.add(this.getCurrentLocation());
                return super.visitWildcard(t, p);
            }
            // TODO: finish this for other types 
        }.visit(t, null);

        return locations;
    }
}
