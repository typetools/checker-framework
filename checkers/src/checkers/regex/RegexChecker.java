package checkers.regex;

import javax.annotation.processing.ProcessingEnvironment;
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

    private TypeMirror[] legalReferenceTypes;

    @Override
    public void initChecker(ProcessingEnvironment env) {
        super.initChecker(env);

        legalReferenceTypes = new TypeMirror[] {
            getTypeMirror("java.lang.CharSequence"),
            getTypeMirror("java.lang.Character"),
            getTypeMirror("java.util.regex.Pattern"),
            getTypeMirror("java.util.regex.MatchResult") };
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        // Only allow annotations on subtypes of the types in legalReferenceTypes.
        if (!useType.getExplicitAnnotations().isEmpty()) {
            Types typeUtils = env.getTypeUtils();
            for (TypeMirror type: legalReferenceTypes) {
                if (typeUtils.isSubtype(declarationType.getUnderlyingType(), type)) {
                    return true;
                }
            }
            return false;
        } else {
            return super.isValidUse(declarationType, useType);
        }
    }

    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type) {
        // Only allow annotations on char.
        if (!type.getExplicitAnnotations().isEmpty()) {
            return type.getKind() == TypeKind.CHAR;
        } else {
            return super.isValidUse(type);
        }
    }

    /**
     * Gets a TypeMirror for the given class name.
     */
    private TypeMirror getTypeMirror(String className) {
        return env.getElementUtils().getTypeElement(className).asType();
    }
}
