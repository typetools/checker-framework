package org.checkerframework.framework.flow.util;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.visitor.AnnotatedTypeMerger;

import static org.checkerframework.framework.util.AnnotatedTypes.findEffectiveAnnotationInHierarchy;
import static org.checkerframework.framework.util.AnnotatedTypes.haveSameDeclaration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;

/**
 * At the moment, this class is just a collection of special cases that fix the behavior of AnnotateAsLub
 * Annotate as lub always uses a type variable's upper bound to annotate (when in fact the type variable unchanged
 * is sometimes the lub).  This class handles the cases where the lub type is a type variable and:
 *
 * a) the subtypes list contains only type variables
 * In the case we should not be using only the upper bound, but choosing a set of bounds that serves as a
 * LUB of all type variables.
 *
 * b) the subtypes lsit contains only type variables or NULL types
 * In this case NULL should have been converted to a type by asSuper but was NOT.  In this case, we
 * make a copy of the lub type and apply the primary annotations of the NULL type as primary
 * annotations on the copy.  From there, we follow the logic in case a.
 */
public class LubTypeVariableAnnotator {

    /**
     * Traverses the subtypes lists, casts the typevars to AnnotatedTypeVariable and adds them to the returned list.
     * NULL types are converted to typevars by applying their primary annotations to a copy of lub.  If any type
     * is
     */
    public static List<AnnotatedTypeVariable> getSubtypesAsTypevars(final AnnotatedTypeVariable lub, final List<? extends AnnotatedTypeMirror> subtypes) {
        final List<AnnotatedTypeVariable> typeVars = new ArrayList<>();
        for (final AnnotatedTypeMirror subtype : subtypes) {
            final AnnotatedTypeVariable typeVar;
            if (subtype.getKind() == TypeKind.TYPEVAR) {
                typeVar = (AnnotatedTypeVariable) subtype;

            //asSuper(null, typevar) does not yield a typevar but the value null, handle this here for now
            } else if (subtype.getKind() == TypeKind.NULL) {
                typeVar = lub.deepCopy();
                typeVar.replaceAnnotations(subtype.getAnnotations());

            } else {
                return null;

            }

            typeVars.add(typeVar);
        }

        return typeVars;
    }

    public static void annotateTypeVarAsLub(final AnnotatedTypeVariable lub, final List<AnnotatedTypeVariable> subtypes,
                                            final AnnotatedTypeFactory typeFactory) {
        final Types types = typeFactory.getProcessingEnv().getTypeUtils();
        final QualifierHierarchy qualHierarchy = typeFactory.getQualifierHierarchy();

        final Iterator<AnnotatedTypeVariable> subtypesIter = subtypes.iterator();
        final AnnotatedTypeVariable headSubtype = subtypesIter.next();

        annotateEmptyLub(lub, headSubtype, qualHierarchy, types);
        while (subtypesIter.hasNext()) {
            annotateLub(lub, subtypesIter.next(), typeFactory, qualHierarchy, types);
        }
    }

    private static void annotateEmptyLub(final AnnotatedTypeVariable lub, final AnnotatedTypeVariable headSubtype,
                                         final QualifierHierarchy qualHierarchy, final Types types) {


        for (final AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
            final AnnotatedTypeMirror src = asLubType(headSubtype, lub, top, types);

            //lub has no annotation, so copy all annotations from headSubtype
            AnnotatedTypeMerger.merge(src, lub);
        }
    }

    private static void annotateLub(final AnnotatedTypeVariable lub, final AnnotatedTypeVariable subtype,
                                   final AnnotatedTypeFactory typeFactory, final QualifierHierarchy qualHierarchy,
                                   final Types types) {

        for (final AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
            final AnnotatedTypeVariable subAsLub = asLubType(subtype, lub, top, types);
            final AnnotationMirror lubPrimary = lub.getAnnotationInHierarchy(top);
            final AnnotationMirror subPrimary = subtype.getAnnotationInHierarchy(top);

            if (lubPrimary == null && subPrimary == null) {
                //TODO: Sometimes we need to lub a NULL type with annotations
                //TODO: In these cases we may see a type var with bounds that lack annotations on their
                //TODO: type args.  To find these cases:
                //TODO: !new StructuralEqualityComparer().areEqualInHierarchy(lub, subAsLub, top)) will return false
                //TODO: Perhaps instead we should handle this case by aggregating the null values and
                //TODO: rather than converting them to type variables we should handle them separately
                continue; //lub is already annotated as subtype is either the same type
                          //or extends lub without adding a primary annotation.
                          //so continue to the next hierarchy
            } else if (lubPrimary != null && subPrimary != null) {
                lub.replaceAnnotation(qualHierarchy.leastUpperBound(lubPrimary, subPrimary));

            } else {
                final TypeHierarchy typeHierarchy = typeFactory.getTypeHierarchy();
                if (typeHierarchy.isSubtype(subAsLub, lub, top)) {
                    //do nothing lub is already above top
                } else if (typeHierarchy.isSubtype(lub, subAsLub, top)) {
                    if (lubPrimary != null) { //&& subPrimary == null
                        //since primary annotations are added to the bounds
                        //we need to replace the upper/lower bound annotations
                        lub.removeAnnotation(lubPrimary);
                        lub.getUpperBound().replaceAnnotation(subAsLub.getUpperBound().getAnnotationInHierarchy(top));
                        lub.getLowerBound().replaceAnnotation(subAsLub.getLowerBound().getAnnotationInHierarchy(top));

                    } else { //lubPrimary == null && subPrimary != null
                        lub.replaceAnnotation(subPrimary);

                    }
                } else { //incomparable types, find the upper bound annotation of the two and take its lub
                    final AnnotationMirror lubAnno =
                        (lubPrimary != null) ? lubPrimary : findEffectiveAnnotationInHierarchy(qualHierarchy, lub, top);

                    final AnnotationMirror subAnno =
                        (subPrimary != null) ? subPrimary : findEffectiveAnnotationInHierarchy(qualHierarchy, subAsLub, top);

                    //It is a conservative lub to place a primary annotation rather than choosing a more specific bound
                    //set that would be a supertype of both the ranges of the subtype/supertype
                    lub.replaceAnnotation(qualHierarchy.leastUpperBound(lubAnno, subAnno));

                }

            }
        }
    }


    /**
     *
     * @param type if lub is a type variable T but type is not, then type is a type variable that extends T (e.g. {@code <E extends T>}
     * @param lub the type variable that is a supertype of T
     * @return the bound of type that has a underlying type T with the first primary annotation encounter
     *         while finding that bound
     */
    private static AnnotatedTypeVariable asLubType(final AnnotatedTypeVariable type, final AnnotatedTypeVariable lub,
                                                   final AnnotationMirror top, final Types types) {
        AnnotatedTypeMirror typeUpperBound = type;
        AnnotationMirror anno = typeUpperBound.getAnnotationInHierarchy(top);
        while (typeUpperBound.getKind() == TypeKind.TYPEVAR
           && !haveSameDeclaration(types, (AnnotatedTypeVariable) typeUpperBound, lub)) {
            typeUpperBound = ((AnnotatedTypeVariable) typeUpperBound).getUpperBound();
            if (anno == null) {
                anno = typeUpperBound.getAnnotationInHierarchy(top);
            }

        }

        if (typeUpperBound.getKind() != TypeKind.TYPEVAR) {
            //TODO: This currently happens because the underlying types of AnnotatedTypeMirrors may not be updated
            //when they're components are substituted.  Therefore, the lub returned by Javac using these underlying
            //types can actually differ from the component types.  See issue 421
            return type;
            //throw new IllegalArgumentException("Type must extend lub: type=" + type + " lub=" + lub);
        }

        if (anno != null) {
            typeUpperBound = typeUpperBound.deepCopy();
            typeUpperBound.addAnnotation(anno);
        }

        return (AnnotatedTypeVariable) typeUpperBound;
    }
}
