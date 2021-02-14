package org.checkerframework.framework.type.visitor;

import java.util.IdentityHashMap;
import java.util.Iterator;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.AtmCombo;

/**
 * EquivalentAtmComboScanner is an AtmComboVisitor that accepts combinations that are identical in
 * TypeMirror structure but might differ in contained AnnotationMirrors. This method will scan the
 * individual components of the visited type pairs together.
 */
public abstract class EquivalentAtmComboScanner<RETURN_TYPE, PARAM>
        extends AbstractAtmComboVisitor<RETURN_TYPE, PARAM> {

    /**
     * A history of type pairs that have already been visited and the return type of their visit.
     */
    protected final Visited visited = new Visited();

    /** Entry point for this scanner. */
    @Override
    public RETURN_TYPE visit(
            final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2, PARAM param) {
        visited.clear();
        return scan(type1, type2, param);
    }

    /**
     * In an AnnotatedTypeScanner a null type is encounter than null is returned. A user may want to
     * customize the behavior of this scanner depending on whether or not one or both types is null.
     *
     * @param type1 a nullable AnnotatedTypeMirror
     * @param type2 a nullable AnnotatedTypeMirror
     * @param param the visitor param
     * @return a subclass specific return type/value
     */
    protected abstract RETURN_TYPE scanWithNull(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, PARAM param);

    protected RETURN_TYPE scan(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, PARAM param) {
        if (type1 == null || type2 == null) {
            return scanWithNull(type1, type2, param);
        }

        return AtmCombo.accept(type1, type2, param, this);
    }

    protected RETURN_TYPE scan(
            Iterable<? extends AnnotatedTypeMirror> types1,
            Iterable<? extends AnnotatedTypeMirror> types2,
            PARAM param) {
        RETURN_TYPE r = null;
        boolean first = true;

        Iterator<? extends AnnotatedTypeMirror> tIter1 = types1.iterator();
        Iterator<? extends AnnotatedTypeMirror> tIter2 = types2.iterator();

        while (tIter1.hasNext() && tIter2.hasNext()) {
            final AnnotatedTypeMirror type1 = tIter1.next();
            final AnnotatedTypeMirror type2 = tIter2.next();

            r = first ? scan(type1, type2, param) : scanAndReduce(type1, type2, param, r);
        }

        return r;
    }

    protected RETURN_TYPE scanAndReduce(
            Iterable<? extends AnnotatedTypeMirror> types1,
            Iterable<? extends AnnotatedTypeMirror> types2,
            PARAM param,
            RETURN_TYPE r) {
        return reduce(scan(types1, types2, param), r);
    }

    protected RETURN_TYPE scanAndReduce(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, PARAM param, RETURN_TYPE r) {
        return reduce(scan(type1, type2, param), r);
    }

    protected RETURN_TYPE reduce(RETURN_TYPE r1, RETURN_TYPE r2) {
        if (r1 == null) {
            return r2;
        }
        return r1;
    }

    @Override
    public RETURN_TYPE visitArray_Array(
            AnnotatedArrayType type1, AnnotatedArrayType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }
        visited.add(type1, type2, null);

        return scan(type1.getComponentType(), type2.getComponentType(), param);
    }

    @Override
    public RETURN_TYPE visitDeclared_Declared(
            AnnotatedDeclaredType type1, AnnotatedDeclaredType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }
        visited.add(type1, type2, null);

        return scan(type1.getTypeArguments(), type2.getTypeArguments(), param);
    }

    @Override
    public RETURN_TYPE visitExecutable_Executable(
            AnnotatedExecutableType type1, AnnotatedExecutableType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }
        visited.add(type1, type2, null);

        RETURN_TYPE r = scan(type1.getReturnType(), type2.getReturnType(), param);
        r = scanAndReduce(type1.getReceiverType(), type2.getReceiverType(), param, r);
        r = scanAndReduce(type1.getParameterTypes(), type2.getParameterTypes(), param, r);
        r = scanAndReduce(type1.getThrownTypes(), type2.getThrownTypes(), param, r);
        r = scanAndReduce(type1.getTypeVariables(), type2.getTypeVariables(), param, r);
        return r;
    }

    @Override
    public RETURN_TYPE visitIntersection_Intersection(
            AnnotatedIntersectionType type1, AnnotatedIntersectionType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }
        visited.add(type1, type2, null);

        return scan(type1.getBounds(), type2.getBounds(), param);
    }

    @Override
    public RETURN_TYPE visitNone_None(AnnotatedNoType type1, AnnotatedNoType type2, PARAM param) {
        return null;
    }

    @Override
    public RETURN_TYPE visitNull_Null(
            AnnotatedNullType type1, AnnotatedNullType type2, PARAM param) {
        return null;
    }

    @Override
    public RETURN_TYPE visitPrimitive_Primitive(
            AnnotatedPrimitiveType type1, AnnotatedPrimitiveType type2, PARAM param) {
        return null;
    }

    @Override
    public RETURN_TYPE visitUnion_Union(
            AnnotatedUnionType type1, AnnotatedUnionType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }

        visited.add(type1, type2, null);

        return scan(type1.getAlternatives(), type2.getAlternatives(), param);
    }

    @Override
    public RETURN_TYPE visitTypevar_Typevar(
            AnnotatedTypeVariable type1, AnnotatedTypeVariable type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }

        visited.add(type1, type2, null);

        RETURN_TYPE r = scan(type1.getUpperBound(), type2.getUpperBound(), param);
        r = scanAndReduce(type1.getLowerBound(), type2.getLowerBound(), param, r);
        return r;
    }

    @Override
    public RETURN_TYPE visitWildcard_Wildcard(
            AnnotatedWildcardType type1, AnnotatedWildcardType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }

        visited.add(type1, type2, null);

        RETURN_TYPE r = scan(type1.getExtendsBound(), type2.getExtendsBound(), param);
        r = scanAndReduce(type1.getSuperBound(), type2.getSuperBound(), param, r);
        return r;
    }

    /**
     * A history of type pairs that have already been visited and the return type of their visit.
     */
    protected class Visited {

        private final IdentityHashMap<
                        AnnotatedTypeMirror, IdentityHashMap<AnnotatedTypeMirror, RETURN_TYPE>>
                visits = new IdentityHashMap<>();

        public void clear() {
            visits.clear();
        }

        public boolean contains(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
            IdentityHashMap<AnnotatedTypeMirror, RETURN_TYPE> recordFor1 = visits.get(type1);
            return recordFor1 != null && recordFor1.containsKey(type2);
        }

        public RETURN_TYPE getResult(
                final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
            IdentityHashMap<AnnotatedTypeMirror, RETURN_TYPE> recordFor1 = visits.get(type1);
            if (recordFor1 == null) {
                return null;
            }

            return recordFor1.get(type2);
        }

        public void add(
                final AnnotatedTypeMirror type1,
                final AnnotatedTypeMirror type2,
                final RETURN_TYPE ret) {
            IdentityHashMap<AnnotatedTypeMirror, RETURN_TYPE> recordFor1 = visits.get(type1);
            if (recordFor1 == null) {
                recordFor1 = new IdentityHashMap<>();
                visits.put(type1, recordFor1);
            }

            recordFor1.put(type2, ret);
        }
    }
}
