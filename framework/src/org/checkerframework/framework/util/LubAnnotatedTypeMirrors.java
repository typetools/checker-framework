package org.checkerframework.framework.util;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;

import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.TypesUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;

import static com.sun.tools.javac.code.TypeTags.CLASS;
import static com.sun.tools.javac.code.TypeTags.TYPEVAR;

/*>>>
import checkers.nullness.quals.*;
*/

/**
 * The code in this class was in AnnotatedTypes in a branch created by Stefan Heule.
 * The code changes were abandoned.  The code was copied from
 * com.sun.tools.javac.code.Types#lub(com.sun.tools.javac.code.Type...)
 * then occurs of Type were replaces with AnnotatedTypeMirror.  But this replacement
 * was not compelted, so this code doesn't compile.
 *
 * Below is an explanation from Stefan about why the changes were abandoned.
 * Sep 06, 2013:

 At some point we had problems that taking the LUB of two annotated types
 was not computed correctly, because we initially took the somewhat naive
 approach of taking the LUB of the underlying Java type and just adding the
 LUB of the annotations on both types.

 To fix this problem, we tried to implement the same LUB strategy as javac
 does for Java types in Types and have the same computation that also
 considers annotations in AnnotatedTypes.  Unfortunately, this turned out to
 be rather challenging because the computation is not straight-forward, and
 the existing code hardly documented.  This was done in the lub branch.

 In the end we abandoned this approach (we never got it working) and decided
 to fix the our existing LUB computation.  Now, we consider all necessary
 cases and also add the right annotations to element types, bounds, etc.
 This is important for taking the LUB of array types, type variables,
 wildcards and so on.  This required some special casing and trial-and-error
 to get all the behavior we want, but in the end it seemed easier than
 trying to understand and reimplement the javac LUB method.  It also seems
 to work reliably now.
 */
public class LubAnnotatedTypeMirrors {

    /**
     * The element type of an array.
     */
    public AnnotatedTypeMirror elemtype(AnnotatedTypeMirror t) {
        switch (t.getKind()) {
        case WILDCARD:
            return elemtype(((AnnotatedWildcardType) t).getExtendsBound());
        case ARRAY:
            return ((AnnotatedArrayType) t).getComponentType();
        case ERROR:
            return t;
        default:
            return null;
        }
    }

    /**
     * Return the least upper bound of pair of types. if the lub does not exist
     * return null.
     */
    public AnnotatedTypeMirror lub(AnnotatedTypeMirror t1, AnnotatedTypeMirror t2, AnnotatedTypeFactory factory) {
        List<AnnotatedTypeMirror> types = new LinkedList<>();
        types.add(t1);
        types.add(t2);
        return lub(com.sun.tools.javac.util.List.of(t1, t2), factory);
    }

    /**
     * Return the least upper bound (lub) of set of types. If the lub does not
     * exist return the type of null (bottom).
     */
    public AnnotatedTypeMirror lub(com.sun.tools.javac.util.List<AnnotatedTypeMirror> ts, AnnotatedTypeFactory factory) {
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) factory.getProcessingEnv();
        Context context = javacEnv.getContext();
        Symtab sym = Symtab.instance(context);
        com.sun.tools.javac.code.Types types = com.sun.tools.javac.code.Types.instance(context);
        final int ARRAY_BOUND = 1;
        final int CLASS_BOUND = 2;
        int boundkind = 0;
        for (AnnotatedTypeMirror t : ts) {
            switch (t.getKind()) {
            case DECLARED:
                boundkind |= CLASS_BOUND;
                break;
            case ARRAY:
                boundkind |= ARRAY_BOUND;
                break;
            case TYPEVAR:
                do {
                    AnnotatedTypeVariable at = (AnnotatedTypeVariable) t;
                    t = at.getUpperBound();
                } while (t.getKind() == TypeKind.TYPEVAR);
                if (t.getKind() == TypeKind.ARRAY) {
                    boundkind |= ARRAY_BOUND;
                } else {
                    boundkind |= CLASS_BOUND;
                }
                break;
            default:
                if (TypesUtils.isPrimitive(t.getUnderlyingType()))
                    // was: return syms.errType;
                    // TODO: is there an error type?
                    return null;
            }
        }
        switch (boundkind) {
        case 0:
            //was: return syms.botType;
            AnnotatedTypeMirror bottom = AnnotatedTypeMirror.createType(sym.botType, factory);
            bottom.clearAnnotations();
            bottom.addAnnotations(factory.getQualifierHierarchy().getBottomAnnotations());
            return bottom;

        case ARRAY_BOUND:
            // calculate lub(A[], B[])
            com.sun.tools.javac.util.List<AnnotatedTypeMirror> elements = com.sun.tools.javac.util.List.nil();
            for (AnnotatedTypeMirror t : ts) {
                elements = elements.append(elemtype(t));
            }
            for (AnnotatedTypeMirror t : elements) {
                if (TypesUtils.isPrimitive(t.getUnderlyingType())) {
                    // if a primitive type is found, then return
                    // arraySuperType unless all the types are the
                    // same
                    AnnotatedTypeMirror first = ts.head;
                    for (AnnotatedTypeMirror s : ts.tail) {
                        if (!types.isSameType((Type) first.getUnderlyingType(), (Type) s.getUnderlyingType())) {
                            // lub(int[], B[]) is Cloneable & Serializable
                            return arraySuperType(types, sym, factory);
                        }
                    }
                    // all the array types are the same, return one
                    // lub(int[], int[]) is int[]
                    // For the Checker Framework, we also need to compute the LUB on the annotations.
                    AnnotatedTypeMirror result = AnnotatedTypeMirror.createType(first.getUnderlyingType(), factory);
                    result.clearAnnotations();
                    result.addAnnotations(lubAnnotations(elements, factory));
                    return result;
                }
            }
            // lub(A[], B[]) is lub(A, B)[]
            // Checker Framework: we also need to take the lub of the annotations on the array.
            AnnotatedTypeMirror elemLub = lub(elements, factory);
            ArrayType underlyingType = new ArrayType((Type) elemLub.getUnderlyingType(), sym.arrayClass);
            AnnotatedTypeMirror result = AnnotatedTypeMirror.createType(underlyingType, factory);
            Set<AnnotationMirror> lubAnnotations = lubAnnotations(ts, factory);
            result.clearAnnotations();
            result.addAnnotations(lubAnnotations);
            return result;

        case CLASS_BOUND:
            // calculate lub(A, B)
            while (ts.head.getKind() != TypeKind.DECLARED && ts.head.getKind() != TypeKind.TYPEVAR)
                ts = ts.tail;
            Assert.check(!ts.isEmpty());
            //step 1 - compute erased candidate set (EC)
            com.sun.tools.javac.util.List<AnnotatedTypeMirror> cl = erasedSupertypes(ts.head);
            for (AnnotatedTypeMirror t : ts.tail) {
                if (t.getKind() == TypeKind.DECLARED || t.getKind() == TypeKind.TYPEVAR)
                    cl = intersect(cl, erasedSupertypes(t));
            }
            //step 2 - compute minimal erased candidate set (MEC)
            com.sun.tools.javac.util.List<AnnotatedTypeMirror> mec = closureMin(cl);
            //step 3 - for each element G in MEC, compute lci(Inv(G))
            com.sun.tools.javac.util.List<AnnotatedTypeMirror> candidates = com.sun.tools.javac.util.List.nil();
            for (AnnotatedTypeMirror erasedSupertype : mec) {
                com.sun.tools.javac.util.List<AnnotatedTypeMirror> lci = com.sun.tools.javac.util.List.of(asSuper(ts.head, erasedSupertype.tsym));
                for (AnnotatedTypeMirror t : ts) {
                    lci = intersect(lci, com.sun.tools.javac.util.List.of(asSuper(t, erasedSupertype.tsym)));
                }
                candidates = candidates.appendList(lci);
            }
            //step 4 - let MEC be { G1, G2 ... Gn }, then we have that
            //lub = lci(Inv(G1)) & lci(Inv(G2)) & ... & lci(Inv(Gn))
            return compoundMin(candidates);

        default:
            // calculate lub(A, B[])
            com.sun.tools.javac.util.List<AnnotatedTypeMirror> classes = com.sun.tools.javac.util.List.of(arraySuperType(types, sym, factory));
            for (AnnotatedTypeMirror t : ts) {
                if (t.getKind() != TypeKind.ARRAY) // Filter out any arrays
                    classes.add(t);
            }
            // lub(A, B[]) is lub(A, arraySuperType)
            return lub(classes, factory);
        }
    }

    /**
     * Returns the LUB of the effective annotations on all elements of ts.
     */
    private Set<AnnotationMirror> lubAnnotations(List<AnnotatedTypeMirror> ts, AnnotatedTypeFactory factory) {
        QualifierHierarchy qualifierHierarchy = factory.getQualifierHierarchy();
        Set<AnnotationMirror> result = new HashSet<>();
        AnnotatedTypeMirror first = ts.get(0);
        result.addAll(first.getEffectiveAnnotations());
        for (int i = 1; i < ts.size(); i++) {
            AnnotatedTypeMirror s = ts.get(i);
            result = qualifierHierarchy.leastUpperBounds(result, s.getEffectiveAnnotations());
        }
        return result;
    }

    com.sun.tools.javac.util.List<AnnotatedTypeMirror> erasedSupertypes(AnnotatedTypeMirror t) {
        ListBuffer<AnnotatedTypeMirror> buf = lb();
        for (AnnotatedTypeMirror sup : closure(t)) {
            if (sup.getKind() == TypeKind.TYPEVAR) {
                buf.append(sup);
            } else {
                buf.append(erasure(sup));
            }
        }
        return buf.toList();
    }

    private AnnotatedTypeMirror erasure(AnnotatedTypeMirror t) {
        return t.getErased();
    }

    private AnnotatedTypeMirror arraySuperType = null;

    private AnnotatedTypeMirror arraySuperType(com.sun.tools.javac.code.Types types, Symtab sym, AnnotatedTypeFactory factory) {
        // initialized lazily to avoid problems during compiler startup
        if (arraySuperType == null) {
            synchronized (this) {
                if (arraySuperType == null) {
                    // JLS 10.8: all arrays implement Cloneable and
                    // Serializable.
                    Type unannotatedType = types.makeCompoundType(
                            com.sun.tools.javac.util.List.of(sym.serializableType, sym.cloneableType),
                            sym.objectType);
                    // TODO: what annotations should the arraySyperType have?
                    arraySuperType = AnnotatedTypeMirror.createType(unannotatedType, factory);
                }
            }
        }
        return arraySuperType;
    }

    /**
     * A cache for closures.
     *
     * <p>A closure is a list of all the supertypes and interfaces of
     * a class or interface type, ordered by ClassSymbol.precedes
     * (that is, subclasses come first, arbitrary but fixed
     * otherwise).
     */
    private final Map<AnnotatedTypeMirror,com.sun.tools.javac.util.List<AnnotatedTypeMirror>> closureCache = new HashMap<>();

    /**
     * Returns the closure of a class or interface type.
     */
    public com.sun.tools.javac.util.List<AnnotatedTypeMirror> closure(AnnotatedTypeMirror t) {
        com.sun.tools.javac.util.List<AnnotatedTypeMirror> cl = closureCache.get(t);
        if (cl == null) {
            AnnotatedTypeMirror st = supertype(t);
            if (!t.isCompound()) {
                if (st.tag == CLASS) {
                    cl = insert(closure(st), t);
                } else if (st.tag == TYPEVAR) {
                    cl = closure(st).prepend(t);
                } else {
                    cl = com.sun.tools.javac.util.List.of(t);
                }
            } else {
                cl = closure(supertype(t));
            }
            for (com.sun.tools.javac.util.List<Type> l = interfaces(t); l.nonEmpty(); l = l.tail)
                cl = union(cl, closure(l.head));
            closureCache.put(t, cl);
        }
        return cl;
    }

    private AnnotatedTypeMirror supertype(AnnotatedTypeMirror t) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Insert a type in a closure
     */
    public com.sun.tools.javac.util.List<AnnotatedTypeMirror> insert(com.sun.tools.javac.util.List<AnnotatedTypeMirror> cl, AnnotatedTypeMirror t) {
        if (cl.isEmpty() || t.tsym.precedes(cl.head.tsym, this)) {
            return cl.prepend(t);
        } else if (cl.head.tsym.precedes(t.tsym, this)) {
            return insert(cl.tail, t).prepend(cl.head);
        } else {
            return cl;
        }
    }

    /**
     * Form the union of two closures
     */
    public com.sun.tools.javac.util.List<AnnotatedTypeMirror> union(com.sun.tools.javac.util.List<AnnotatedTypeMirror> cl1, com.sun.tools.javac.util.List<AnnotatedTypeMirror> cl2) {
        if (cl1.isEmpty()) {
            return cl2;
        } else if (cl2.isEmpty()) {
            return cl1;
        } else if (cl1.head.tsym.precedes(cl2.head.tsym, this)) {
            return union(cl1.tail, cl2).prepend(cl1.head);
        } else if (cl2.head.tsym.precedes(cl1.head.tsym, this)) {
            return union(cl1, cl2.tail).prepend(cl2.head);
        } else {
            return union(cl1.tail, cl2.tail).prepend(cl1.head);
        }
    }

    /**
     * Intersect two closures
     */
    public com.sun.tools.javac.util.List<AnnotatedTypeMirror> intersect(com.sun.tools.javac.util.List<AnnotatedTypeMirror> cl1, com.sun.tools.javac.util.List<AnnotatedTypeMirror> cl2) {
        if (cl1 == cl2)
            return cl1;
        if (cl1.isEmpty() || cl2.isEmpty())
            return com.sun.tools.javac.util.List.nil();
        if (cl1.head.tsym.precedes(cl2.head.tsym, this))
            return intersect(cl1.tail, cl2);
        if (cl2.head.tsym.precedes(cl1.head.tsym, this))
            return intersect(cl1, cl2.tail);
        if (isSameType(cl1.head, cl2.head))
            return intersect(cl1.tail, cl2.tail).prepend(cl1.head);
        if (cl1.head.tsym == cl2.head.tsym &&
            cl1.head.tag == CLASS && cl2.head.tag == CLASS) {
            if (cl1.head.isParameterized() && cl2.head.isParameterized()) {
                Type merge = merge(cl1.head,cl2.head);
                return intersect(cl1.tail, cl2.tail).prepend(merge);
            }
            if (cl1.head.isRaw() || cl2.head.isRaw())
                return intersect(cl1.tail, cl2.tail).prepend(erasure(cl1.head));
        }
        return intersect(cl1.tail, cl2.tail);
    }
}
