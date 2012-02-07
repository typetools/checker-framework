package checkers.regex;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.regex.quals.PolyRegex;
import checkers.regex.quals.Regex;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;

/**
 * A type-checker plug-in for the {@link Regex} qualifier that finds
 * syntactically invalid regular expressions.
 */
@TypeQualifiers({ Regex.class, PolyRegex.class, Unqualified.class })
public class RegexChecker extends BaseTypeChecker {

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        // Only allow annotations on Character and subtypes of CharSequence.
        if (!getExplicitAnnotations(useType).isEmpty()) {
            TypeMirror charSequence = getTypeMirror("java.lang.CharSequence");
            TypeMirror character = getTypeMirror("java.lang.Character");

            Types typeUtils = env.getTypeUtils();
            return typeUtils.isSubtype(declarationType.getUnderlyingType(), charSequence)
                || typeUtils.isSubtype(declarationType.getUnderlyingType(), character);
        } else {
            return super.isValidUse(declarationType, useType);
        }
    }

    /**
     * Gets a TypeMirror for the given class name.
     */
    private TypeMirror getTypeMirror(String className) {
        return env.getElementUtils().getTypeElement(className).asType();
    }

    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type) {
        // Only allow annotations on char.
        if (!getExplicitAnnotations(type).isEmpty()) {
            return type.getKind() == TypeKind.CHAR;
        } else {
            return super.isValidUse(type);
        }
    }
}
