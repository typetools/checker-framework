package org.checkerframework.framework.type.visitor;


import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.framework.util.AtmCombo;

import java.util.*;

/**
 * EquivalentAtmComboScanner is an AtmComboVisitor that accepts combinations that are identical in TypeMirror structure
 * but might differ in contained AnnotationMirrors.
*/
public abstract class EquivalentAtmComboScanner<RETURN_TYPE, PARAM> extends AbstractAtmComboVisitor<RETURN_TYPE, PARAM> {
    protected final Visited visited = new Visited();


    protected RETURN_TYPE scan(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, PARAM param) {
        return AtmCombo.accept(type1, type2, param, this);
    }

    protected RETURN_TYPE scan(Iterable<? extends AnnotatedTypeMirror> types1,
                               Iterable<? extends AnnotatedTypeMirror> types2,
                               PARAM param) {
        RETURN_TYPE r = null;
        boolean first = true;

        Iterator<? extends AnnotatedTypeMirror> tIter1 = types1.iterator();
        Iterator<? extends AnnotatedTypeMirror> tIter2 = types2.iterator();

        while(tIter1.hasNext() && tIter2.hasNext()) {
            final AnnotatedTypeMirror type1 = tIter1.next();
            final AnnotatedTypeMirror type2 = tIter2.next();

            r = first ? scan(type1, type2, param)
                      : scanAndReduce(type1, type2, param, r);
        }

        return r;
    }

    protected RETURN_TYPE scanAndReduce(Iterable<? extends AnnotatedTypeMirror> types1,
                                        Iterable<? extends AnnotatedTypeMirror> types2,
                                        PARAM param, RETURN_TYPE r) {
        return reduce(scan(types1, types2, param), r);
    }

    public RETURN_TYPE scanAndReduce(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, PARAM param, RETURN_TYPE r) {
        return reduce(scan(type1, type2, param), r);
    }

    protected RETURN_TYPE reduce(RETURN_TYPE r1, RETURN_TYPE r2) {
        if (r1 == null)
            return r2;
        return r1;
    }

    @Override
    public RETURN_TYPE visitArray_Array(AnnotatedArrayType type1, AnnotatedArrayType type2, PARAM param) {
        if(visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }
        visited.add(type1, type2, null);

        return scan(type1.getComponentType(), type2.getComponentType(), param);
    }

    @Override
    public RETURN_TYPE visitDeclared_Declared(AnnotatedDeclaredType type1, AnnotatedDeclaredType type2, PARAM param) {
        if(visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }
        visited.add(type1, type2, null);

        return scan(type1.getTypeArguments(), type2.getTypeArguments(), param);
    }

    @Override
    public RETURN_TYPE visitExecutable_Executable(AnnotatedExecutableType type1, AnnotatedExecutableType type2, PARAM param) {
        if(visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }
        visited.add(type1, type2, null);

        RETURN_TYPE r = scan(type1.getReturnType(),  type2.getReturnType(),     param);
        r = scanAndReduce(type1.getReceiverType(),   type2.getReceiverType(),   param, r);
        r = scanAndReduce(type1.getParameterTypes(), type2.getParameterTypes(), param, r);
        r = scanAndReduce(type1.getThrownTypes(),    type2.getThrownTypes(),    param, r);
        r = scanAndReduce(type1.getTypeVariables(),  type2.getTypeVariables(),  param, r);
        return r;
    }

    @Override
    public RETURN_TYPE visitIntersection_Intersection(AnnotatedIntersectionType type1, AnnotatedIntersectionType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1,type2);
        }
        visited.add(type1, type2, null);

        return scan(type1.directSuperTypes(), type2.directSuperTypes(), param);
    }

    @Override
    public RETURN_TYPE visitNone_None(AnnotatedNoType type1, AnnotatedNoType type2, PARAM param) {
        return null;
    }

    @Override
    public RETURN_TYPE visitNull_Null(AnnotatedNullType type1, AnnotatedNullType type2, PARAM param) {
        return null;
    }

    @Override
    public RETURN_TYPE visitPrimitive_Primitive(AnnotatedPrimitiveType type1, AnnotatedPrimitiveType type2, PARAM param) {
        return null;
    }

    @Override
    public RETURN_TYPE visitUnion_Union(AnnotatedUnionType type1, AnnotatedUnionType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1,type2);
        }

        visited.add(type1, type2, null);

        return scan(type1.getAlternatives(), type2.getAlternatives(), param);
    }

    @Override
    public RETURN_TYPE visitTypevar_Typevar(AnnotatedTypeVariable type1, AnnotatedTypeVariable type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1,type2);
        }

        visited.add(type1, type2, null);

        RETURN_TYPE r = scan(type1.getUpperBound(), type2.getUpperBound(), param);
        r = scanAndReduce(type1.getLowerBound(), type2.getLowerBound(), param, r);
        return r;
    }

    @Override
    public RETURN_TYPE visitWildcard_Wildcard(AnnotatedWildcardType type1, AnnotatedWildcardType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1,type2);
        }

        visited.add(type1, type2, null);

        RETURN_TYPE r = scan(type1.getExtendsBound(), type2.getExtendsBound(), param);
        r = scanAndReduce(type1.getSuperBound(), type2.getSuperBound(), param, r);
        return r;
    }

    protected class Visit {
        public final AnnotatedTypeMirror type1;
        public final AnnotatedTypeMirror type2;

        public Visit(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
            this.type1 = type1;
            this.type2 = type2;
        }

        public int hashCode() {
            return 503 * (this.type1.getUnderlyingType().hashCode() + this.type2.getUnderlyingType().hashCode());
        }

        @SuppressWarnings("unchecked")
        public boolean equals(final Object obj) {
            if (this == obj) return true;
            if (obj == null || !(obj.getClass().equals(this.getClass()))) return false;

            final Visit that = (Visit) obj;
            return this.type1 == that.type1
                && this.type2 == that.type2;
        }
    }

    protected class Visited {
        private final Map<Visit, RETURN_TYPE> visits = new IdentityHashMap<Visit, RETURN_TYPE>();

        public boolean contains(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
            return visits.containsKey(new Visit(type1, type2));
        }

        public RETURN_TYPE getResult(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
            return visits.get(new Visit(type1, type2));
        }

        public void add(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2, final RETURN_TYPE ret) {
            visits.put(new Visit(type1, type2), ret);
        }
    }
}
