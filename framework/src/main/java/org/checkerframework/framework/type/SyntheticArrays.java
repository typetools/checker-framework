package org.checkerframework.framework.type;

import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

/**
 * SyntheticArrays exists solely to fix AnnotatedTypeMirrors that need to be adapted from Array type
 * to a specific kind of array. There are no classes for arrays. Instead, for each type of array
 * (e.g. String[]) the compiler/JVM creates a synthetic type for them.
 */
public class SyntheticArrays {

    /**
     * Returns true if this combination of type/elem represents an array.clone.
     *
     * @param type a type with a method/field of elem
     * @param elem an element which is a member of type
     * @return true if this combination of type/elem represents an array.clone
     */
    public static boolean isArrayClone(final AnnotatedTypeMirror type, final Element elem) {
        return type.getKind() == TypeKind.ARRAY
                && elem.getKind() == ElementKind.METHOD
                && elem.getSimpleName().contentEquals("clone");
    }

    /**
     * Returns the annotated type of methodElem with its return type replaced by newReturnType.
     *
     * @param methodElem identifies a method that should have an AnnotatedArrayType as its return
     *     type
     * @param newReturnType identifies a type that should replace methodElem's return type
     * @return the annotated type of methodElem with its return type replaced by newReturnType
     */
    public static AnnotatedExecutableType replaceReturnType(
            final Element methodElem, final AnnotatedArrayType newReturnType) {
        final AnnotatedExecutableType method =
                (AnnotatedExecutableType) newReturnType.atypeFactory.getAnnotatedType(methodElem);
        method.returnType = newReturnType;
        return method;
    }
}
