package org.checkerframework.framework.util;

import org.checkerframework.javacutil.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Records any mapping between the type parameters of a subtype to the corresponding type parameters
 * of a supertype. For example, suppose we have the following classes:
 *
 * <pre>{@code
 * class Map<M1,M2>
 * class HashMap<H1, H2> extends Map<H1,H2>
 * }</pre>
 *
 * And we pass HashMap and Map to mapTypeArguments, the result would be:
 *
 * <pre>{@code
 * Map(H1 => M1, H2 => M2)
 * }</pre>
 *
 * Note, a single type argument in the subtype can map to multiple type parameters in the supertype.
 * e.g.,
 *
 * <pre>{@code
 * class OneTypeMap<O1> extends Map<O1,O1>
 * }</pre>
 *
 * would have the result:
 *
 * <pre>{@code
 * Map(O1 => [M1,M2])
 * }</pre>
 *
 * This utility only maps between corresponding type parameters, so the following class:
 *
 * <pre>{@code
 * class StringMap extends Map<String,String>
 * }</pre>
 *
 * would have an empty map as a result:
 *
 * <pre>{@code
 * Map() // there are no type argument relationships between the two types
 * }</pre>
 */
public class TypeArgumentMapper {

    /**
     * Returns a mapping from subtype's type parameter indices to the indices of corresponding type
     * parameters in supertype.
     */
    public static Set<Pair<Integer, Integer>> mapTypeArgumentIndices(
            final TypeElement subtype, final TypeElement supertype, final Types types) {
        Set<Pair<Integer, Integer>> result = new HashSet<>();
        if (subtype.equals(supertype)) {
            for (int i = 0; i < subtype.getTypeParameters().size(); i++) {
                result.add(Pair.of(Integer.valueOf(i), Integer.valueOf(i)));
            }

        } else {
            Map<TypeParameterElement, Set<TypeParameterElement>> subToSuperElements =
                    mapTypeArguments(subtype, supertype, types);
            Map<TypeParameterElement, Integer> supertypeIndexes = getElementToIndex(supertype);

            final List<? extends TypeParameterElement> subtypeParams = subtype.getTypeParameters();
            for (int subtypeIndex = 0; subtypeIndex < subtypeParams.size(); subtypeIndex++) {
                final TypeParameterElement subtypeParam = subtypeParams.get(subtypeIndex);

                final Set<TypeParameterElement> correspondingSuperArgs =
                        subToSuperElements.get(subtypeParam);
                if (correspondingSuperArgs != null) {
                    for (TypeParameterElement supertypeParam :
                            subToSuperElements.get(subtypeParam)) {
                        result.add(Pair.of(subtypeIndex, supertypeIndexes.get(supertypeParam)));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns a Map(type parameter symbol &rarr; index in type parameter list).
     *
     * @param typeElement a type whose type parameters to summarize
     * @return a Map(type parameter symbol &rarr; index in type parameter list)
     */
    private static Map<TypeParameterElement, Integer> getElementToIndex(TypeElement typeElement) {
        Map<TypeParameterElement, Integer> result = new LinkedHashMap<>();

        List<? extends TypeParameterElement> params = typeElement.getTypeParameters();
        for (int i = 0; i < params.size(); i++) {
            result.put(params.get(i), Integer.valueOf(i));
        }

        return result;
    }

    /**
     * Returns a mapping from the type parameters of subtype to a list of the type parameters in
     * supertype that must be the same type as subtype.
     *
     * <p>e.g.,
     *
     * <pre>{@code
     * class A<A1,A2,A3>
     * class B<B1,B2,B3,B4> extends A<B1,B1,B3> {}
     * }</pre>
     *
     * results in a {@code Map(B1 => [A1,A2], B2 => [], B3 => [A3], B4 => [])}
     *
     * @return a mapping from the type parameters of subtype to the supertype type parameter's that
     *     to which they are a type argument
     */
    public static Map<TypeParameterElement, Set<TypeParameterElement>> mapTypeArguments(
            final TypeElement subtype, final TypeElement supertype, final Types types) {

        final List<TypeRecord> pathToSupertype =
                depthFirstSearchForSupertype(subtype, supertype, types);

        if (pathToSupertype == null || pathToSupertype.isEmpty()) {
            return new LinkedHashMap<>();
        }

        final Map<TypeParameterElement, Set<TypeParameterElement>> intermediate =
                new LinkedHashMap<>();
        final Set<TypeParameterElement> currentTypeParams = new HashSet<>();

        // takes a type records of the form:
        //  TypeRecord(element = MyMap<Y1,Y2>, type = null)
        //  TypeRecord(element = AbstractMap<A1,A2>, type = AbstractMap<Y1,Y2>)
        //  TypeRecord(element = Map<M1,M2>, type = AbstractMap<A1,A2>)
        // And makes a map:
        //   Map(Y1 -> [A1], Y2 -> [A2], A1 -> [M1], A2 -> M2]
        Iterator<TypeRecord> path = pathToSupertype.iterator();
        TypeRecord current = path.next();
        while (path.hasNext()) {
            TypeRecord next = path.next();

            final List<? extends TypeParameterElement> nextTypeParameter =
                    next.element.getTypeParameters();
            final List<? extends TypeMirror> nextTypeArgs =
                    next.type != null ? next.type.getTypeArguments() : Collections.emptyList();
            currentTypeParams.clear();
            currentTypeParams.addAll(current.element.getTypeParameters());

            for (int i = 0; i < nextTypeArgs.size(); i++) {
                final TypeParameterElement correspondingParameter = nextTypeParameter.get(i);
                final TypeMirror typeArg = nextTypeArgs.get(i);
                final Element typeArgEle = types.asElement(typeArg);

                if (currentTypeParams.contains(typeArgEle)) {
                    addToSetMap(
                            intermediate,
                            (TypeParameterElement) typeArgEle,
                            correspondingParameter);
                }
            }
        }

        List<? extends TypeParameterElement> supertypeParams = supertype.getTypeParameters();
        final Map<TypeParameterElement, Set<TypeParameterElement>> result =
                new LinkedHashMap<>(subtype.getTypeParameters().size());

        // You can think of the map above as a set of links from SubtypeParameter -> Supertype
        // Parameter
        for (TypeParameterElement subtypeParam : subtype.getTypeParameters()) {
            Set<TypeParameterElement> subtypePath =
                    flattenPath(intermediate.get(subtypeParam), intermediate);
            subtypePath.retainAll(supertypeParams);
            result.put(subtypeParam, subtypePath);
        }

        return result;
    }

    private static Set<TypeParameterElement> flattenPath(
            Set<TypeParameterElement> elements,
            Map<TypeParameterElement, Set<TypeParameterElement>> map) {
        Set<TypeParameterElement> result = new HashSet<>();
        if (elements == null) {
            return result;
        }
        for (final TypeParameterElement oldElement : elements) {
            Set<TypeParameterElement> substitutions = map.get(oldElement);
            if (substitutions != null) {
                result.addAll(flattenPath(elements, map));
            } else {
                result.add(oldElement);
            }
        }
        return result;
    }

    private static void addToSetMap(
            final Map<TypeParameterElement, Set<TypeParameterElement>> setMap,
            final TypeParameterElement element,
            final TypeParameterElement typeParam) {
        Set<TypeParameterElement> set = setMap.get(element);
        if (set == null) {
            set = new HashSet<>();
            setMap.put(element, set);
        }

        set.add(typeParam);
    }

    /**
     * Create a list of TypeRecord's that form a "path" to target from subtype. e.g. Suppose I have
     * the types
     *
     * <pre>{@code
     * interface Map<M1,M2>
     * class AbstractMap<A1,A2> implements Map<A1,A2>, Iterable<Map.Entry<M1,M2>>
     * class MyMap<Y1,Y2> extends AbstractMap<Y1,Y2> implements List<Map.Entry<Y1,Y2>>
     * }</pre>
     *
     * The path from MyMap to Map would be:
     *
     * <pre>{@code
     * TypeRecord(element = MyMap<Y1,Y2>, type = null)
     * TypeRecord(element = AbstractMap<A1,A2>, type = AbstractMap<Y1,Y2>)
     * TypeRecord(element = Map<M1,M2>, type = AbstractMap<A1,A2>)
     * }</pre>
     *
     * Note: You can have an implementation of the same interface inherited multiple times as long
     * as the parameterization of that interface remains the same e.g.
     *
     * <pre>{@code
     * interface List<E>
     * class AbstractList<A> implements List<E>
     * class ArrayList<T> extends AbstractList<T> implements List<T>
     * }</pre>
     *
     * Notice how ArrayList implements list both by inheriting from AbstractList and from explicitly
     * listing it in the implements clause. We prioritize finding a path through the list of
     * interfaces first since this will be the shorter path.
     *
     * @param subtype the start of the resulting sequence
     * @param target the end of the resulting sequence
     * @param types utility methods for operating on types
     * @return a list of type records that represents the sequence of directSupertypes between
     *     subtype and target
     */
    private static List<TypeRecord> depthFirstSearchForSupertype(
            final TypeElement subtype, final TypeElement target, final Types types) {
        ArrayDeque<TypeRecord> pathFromRoot = new ArrayDeque<>();
        final TypeRecord pathStart = new TypeRecord(subtype, null);
        pathFromRoot.push(pathStart);
        final List<TypeRecord> result = recursiveDepthFirstSearch(pathFromRoot, target, types);
        return result;
    }

    /**
     * Computes one level for depthFirstSearchForSupertype then recurses.
     *
     * @param pathFromRoot the path so far
     * @param target the end of the resulting path
     * @param types utility methods for operating on types
     * @return a list of type records that extends pathFromRoot (a sequence of directSupertypes) to
     *     target
     */
    private static List<TypeRecord> recursiveDepthFirstSearch(
            final ArrayDeque<TypeRecord> pathFromRoot,
            final TypeElement target,
            final Types types) {
        if (pathFromRoot.isEmpty()) {
            return null;
        }

        final TypeRecord currentRecord = pathFromRoot.peekLast();
        final TypeElement currentElement = currentRecord.element;

        if (currentElement.equals(target)) {
            return new ArrayList<>(pathFromRoot);
        }

        final Iterator<? extends TypeMirror> interfaces = currentElement.getInterfaces().iterator();
        final TypeMirror superclassType = currentElement.getSuperclass();

        List<TypeRecord> path = null;

        while (path == null && interfaces.hasNext()) {
            final TypeMirror intface = interfaces.next();
            if (intface.getKind() != TypeKind.NONE) {
                DeclaredType interfaceDeclared = (DeclaredType) intface;
                pathFromRoot.addLast(
                        new TypeRecord(
                                (TypeElement) types.asElement(interfaceDeclared),
                                interfaceDeclared));
                path = recursiveDepthFirstSearch(pathFromRoot, target, types);
                pathFromRoot.removeLast();
            }
        }

        if (path == null && superclassType.getKind() != TypeKind.NONE) {
            final DeclaredType superclass = (DeclaredType) superclassType;

            pathFromRoot.addLast(
                    new TypeRecord((TypeElement) types.asElement(superclass), superclass));
            path = recursiveDepthFirstSearch(pathFromRoot, target, types);
            pathFromRoot.removeLast();
        }

        return path;
    }

    /**
     * Maps a class or interface's declaration element to the type it would be if viewed from a
     * subtype class or interface.
     *
     * <p>e.g. suppose we have the elements for the declarations:
     *
     * <pre>{@code
     * class A<Ta>
     * class B<Tb> extends A<Tb>
     * }</pre>
     *
     * The type record of B if it is viewed as class A would bed:
     *
     * <pre>{@code
     * TypeRecord( element = A<Ta>, type = A<Tb> )
     * }</pre>
     *
     * That is, B can be viewed as an object of type A with an type argument of type parameter Tb
     */
    private static class TypeRecord {
        public final TypeElement element;
        public final DeclaredType type;

        TypeRecord(final TypeElement element, final DeclaredType type) {
            this.element = element;
            this.type = type;
        }

        @Override
        public String toString() {
            return String.format("[%s => %s]", element, type);
        }
    }
}
