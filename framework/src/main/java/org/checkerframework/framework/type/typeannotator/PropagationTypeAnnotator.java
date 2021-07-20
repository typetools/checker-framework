package org.checkerframework.framework.type.typeannotator;

import com.sun.tools.javac.code.Type.WildcardType;

import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.StringsPlume;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

/**
 * {@link PropagationTypeAnnotator} adds qualifiers to types where the qualifier to add should be
 * transferred from one or more other types.
 *
 * <p>At the moment, the only function PropagationTypeAnnotator provides, is the propagation of
 * generic type parameter annotations to unannotated wildcards with missing bounds annotations.
 *
 * @see
 *     #visitWildcard(org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType,
 *     Object)
 *     <p>PropagationTypeAnnotator traverses trees deeply by default.
 */
public class PropagationTypeAnnotator extends TypeAnnotator {

    // The PropagationTypeAnnotator is called recursively via
    // TypeAnnotatorUtil.eraseBoundsThenAnnotate.
    // This flag prevents infinite recursion.
    private boolean pause = false;
    private ArrayDeque<AnnotatedDeclaredType> parents = new ArrayDeque<>();

    public PropagationTypeAnnotator(AnnotatedTypeFactory typeFactory) {
        super(typeFactory);
    }

    @Override
    public void reset() {
        if (!pause) {
            // when the PropagationTypeAnnotator is called recursively we don't
            // want the visit method to reset the list of visited types
            super.reset();
        }
    }

    /*
     * When pause == true, the PropagationTypeAnnotator caused a recursive call
     * and there is no need to execute the PropagationTypeAnnotator
     */
    @Override
    protected Void scan(AnnotatedTypeMirror type, Void aVoid) {
        if (pause) {
            return null;
        }

        return super.scan(type, aVoid);
    }

    /**
     * Sometimes the underlying type parameters of AnnotatedWildcardTypes are not available on the
     * wildcards themselves. Instead, record enclosing class to find the type parameter to use as a
     * backup in visitWildcards.
     *
     * @param declaredType type to record
     */
    @Override
    public Void visitDeclared(AnnotatedDeclaredType declaredType, Void aVoid) {
        if (pause) {
            return null;
        }
        if (declaredType.wasRaw()) {
            // Copy annotations from the declaration to the wildcards.
            AnnotatedDeclaredType declaration =
                    (AnnotatedDeclaredType)
                            typeFactory.fromElement(declaredType.getUnderlyingType().asElement());
            List<AnnotatedTypeMirror> typeArgs = declaredType.getTypeArguments();
            for (int i = 0; i < typeArgs.size(); i++) {
                if (typeArgs.get(i).getKind() != TypeKind.WILDCARD
                        || !((AnnotatedWildcardType) typeArgs.get(i)).isUninferredTypeArgument()) {
                    // Sometimes the framework infers a more precise type argument, so just use it.
                    continue;
                }
                AnnotatedTypeVariable typeParam =
                        (AnnotatedTypeVariable) declaration.getTypeArguments().get(i);
                AnnotatedWildcardType wct = (AnnotatedWildcardType) typeArgs.get(i);
                wct.getExtendsBound()
                        .replaceAnnotations(typeParam.getUpperBound().getAnnotations());
                wct.getSuperBound().replaceAnnotations(typeParam.getLowerBound().getAnnotations());
                wct.replaceAnnotations(typeParam.getAnnotations());
            }
        }

        parents.addFirst(declaredType);
        super.visitDeclared(declaredType, aVoid);
        parents.removeFirst();
        return null;
    }

    /**
     * Rather than defaulting the missing bounds of a wildcard, find the bound annotations on the
     * type parameter it replaced. Place those annotations on the wildcard.
     *
     * @param wildcardAtm type to annotate
     */
    @Override
    public Void visitWildcard(AnnotatedWildcardType wildcardAtm, Void aVoid) {
        if (visitedNodes.containsKey(wildcardAtm) || pause) {
            return null;
        }
        visitedNodes.put(wildcardAtm, null);

        final WildcardType wildcard = (WildcardType) wildcardAtm.getUnderlyingType();
        Element typeParamElement = TypesUtils.wildcardToTypeParam(wildcard);
        if (typeParamElement == null) {
            typeParamElement =
                    parents.isEmpty()
                            ? null
                            : getTypeParamFromEnclosingClass(wildcardAtm, parents.peekFirst());
        }

        if (typeParamElement != null) {
            pause = true;
            AnnotatedTypeVariable typeParam =
                    (AnnotatedTypeVariable) typeFactory.getAnnotatedType(typeParamElement);
            pause = false;

            final Set<? extends AnnotationMirror> tops =
                    typeFactory.getQualifierHierarchy().getTopAnnotations();

            if (wildcard.isUnbound()) {
                propagateExtendsBound(wildcardAtm, typeParam, tops);
                propagateSuperBound(wildcardAtm, typeParam, tops);

            } else if (wildcard.isExtendsBound()) {
                propagateSuperBound(wildcardAtm, typeParam, tops);

            } else { // is super bound
                propagateExtendsBound(wildcardAtm, typeParam, tops);
            }
        }
        scan(wildcardAtm.getExtendsBound(), null);
        scan(wildcardAtm.getSuperBound(), null);
        return null;
    }

    private void propagateSuperBound(
            AnnotatedWildcardType wildcard,
            AnnotatedTypeVariable typeParam,
            Set<? extends AnnotationMirror> tops) {
        applyAnnosFromBound(wildcard.getSuperBound(), typeParam.getLowerBound(), tops);
    }

    private void propagateExtendsBound(
            AnnotatedWildcardType wildcard,
            AnnotatedTypeVariable typeParam,
            Set<? extends AnnotationMirror> tops) {
        applyAnnosFromBound(wildcard.getExtendsBound(), typeParam.getUpperBound(), tops);
    }

    /**
     * Take the primary annotations from typeParamBound and place them as primary annotations on
     * wildcard bound.
     */
    private void applyAnnosFromBound(
            final AnnotatedTypeMirror wildcardBound,
            final AnnotatedTypeMirror typeParamBound,
            final Set<? extends AnnotationMirror> tops) {
        // Type variables do not need primary annotations.
        // The type variable will have annotations placed on its
        // bounds via its declaration or defaulting rules
        if (wildcardBound.getKind() == TypeKind.TYPEVAR
                || typeParamBound.getKind() == TypeKind.TYPEVAR) {
            return;
        }

        for (final AnnotationMirror top : tops) {
            if (wildcardBound.getAnnotationInHierarchy(top) == null) {
                final AnnotationMirror typeParamAnno = typeParamBound.getAnnotationInHierarchy(top);
                if (typeParamAnno == null) {
                    throw new BugInCF(
                            StringsPlume.joinLines(
                                    "Missing annotation on type parameter",
                                    "top=" + top,
                                    "wildcardBound=" + wildcardBound,
                                    "typeParamBound=" + typeParamBound));
                } // else
                wildcardBound.addAnnotation(typeParamAnno);
            }
        }
    }

    /**
     * Search parent's type arguments for wildcard. Using the index of wildcard, find the
     * corresponding type parameter element and return it. Returns null if the wildcard is the
     * result of substitution and therefore not in the list of type arguments.
     *
     * @param wildcard the wildcard type whose corresponding type argument to determine
     * @param parent the type that may have a type argument corresponding to {@code wildcard}
     * @return the type argument in {@code parent} that corresponds to {@code wildcard}
     */
    private Element getTypeParamFromEnclosingClass(
            final @FindDistinct AnnotatedWildcardType wildcard,
            final AnnotatedDeclaredType parent) {
        Integer wildcardIndex = null;
        int currentIndex = 0;
        for (AnnotatedTypeMirror typeArg : parent.getTypeArguments()) {
            // the only cases in which the wildcard is not one of the type arguments are cases in
            // which they should have been replaced by capture
            if (typeArg == wildcard) {
                wildcardIndex = currentIndex;
                break;
            }
            currentIndex += 1;
        }

        if (wildcardIndex != null) {
            final TypeElement typeElement =
                    (TypeElement)
                            typeFactory
                                    .getProcessingEnv()
                                    .getTypeUtils()
                                    .asElement(parent.getUnderlyingType());

            return typeElement.getTypeParameters().get(wildcardIndex);
        }

        return null;
    }
}
