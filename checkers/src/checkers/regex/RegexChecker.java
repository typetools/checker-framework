package checkers.regex;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.regex.quals.PolyRegex;
import checkers.regex.quals.Regex;
import checkers.regex.quals.RegexBottom;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy;
import checkers.util.TreeUtils;

/**
 * A type-checker plug-in for the {@link Regex} qualifier that finds
 * syntactically invalid regular expressions.
 */
@TypeQualifiers({ Regex.class, PolyRegex.class, RegexBottom.class, Unqualified.class })
public class RegexChecker extends BaseTypeChecker {

    protected AnnotationMirror REGEX;
    protected ExecutableElement regexValue;
    private TypeMirror[] legalReferenceTypes;

    @Override
    public void initChecker(ProcessingEnvironment env) {
        super.initChecker(env);

        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);
        REGEX = annoFactory.fromClass(Regex.class);
        regexValue = TreeUtils.getMethod("checkers.regex.quals.Regex", "value", 0, env);

        legalReferenceTypes = new TypeMirror[] {
            getTypeMirror("java.lang.CharSequence"),
            getTypeMirror("java.lang.Character"),
            getTypeMirror("java.util.regex.Pattern"),
            getTypeMirror("java.util.regex.MatchResult") };
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        // TODO: only allow Regex and PolyRegex annotations on types in legalReferenceTypes.
        // This is pending an implementation of AnnotatedTypeMirror.getExplicitAnnotations
        // that supports local variables, array types and parameterized types.
        /*// Only allow annotations on subtypes of the types in legalReferenceTypes.
        if (!useType.getExplicitAnnotations().isEmpty()) {
            Types typeUtils = env.getTypeUtils();
            for (TypeMirror type : legalReferenceTypes) {
                if (typeUtils.isSubtype(declarationType.getUnderlyingType(), type)) {
                    return true;
                }
            }
            return false;
        }*/
        return super.isValidUse(declarationType, useType);
    }

    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type) {
        // TODO: only allow Regex and PolyRegex annotations on chars.
        // This is pending an implementation of AnnotatedTypeMirror.getExplicitAnnotations
        // that supports local variables and array types.
        /*// Only allow annotations on char.
        if (!type.getExplicitAnnotations().isEmpty()) {
            return type.getKind() == TypeKind.CHAR;
        }*/
        return super.isValidUse(type);
    }

    /**
     * Gets a TypeMirror for the given class name.
     */
    private TypeMirror getTypeMirror(String className) {
        return env.getElementUtils().getTypeElement(className).asType();
    }
    
    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new RegexQualifierHierarchy((GraphQualifierHierarchy) super.createQualifierHierarchy());
    }
    
    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
      return new GraphQualifierHierarchy.GraphFactory(this, AnnotationUtils.getInstance(env).fromClass(RegexBottom.class));
    }

    /**
     * A custom qualifier hierarchy for the Regex Checker. This makes a regex
     * annotation a subtype of all regex annotations with lower group count
     * values. For example, {@code @Regex(3)} is a subtype of {@code @Regex(1)}.
     * All regex annotations are subtypes of {@code @Regex} which has a default
     * value of 0.
     */
    private final class RegexQualifierHierarchy extends GraphQualifierHierarchy {

        public RegexQualifierHierarchy(GraphQualifierHierarchy hierarchy) {
            super(hierarchy);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(rhs, REGEX)
                    && AnnotationUtils.areSameIgnoringValues(lhs, REGEX)) {
                int rhsValue = getRegexValue(rhs);
                int lhsValue = getRegexValue(lhs);
                return lhsValue <= rhsValue;
            }
            return super.isSubtype(rhs, lhs);
        }

        /**
         * Gets the value out of a regex annotation.
         */
        private int getRegexValue(AnnotationMirror anno) {
            return (Integer) AnnotationUtils.getElementValuesWithDefaults(anno).get(regexValue).getValue();
        }
    }
    
    /**
     * Returns the group count value of the given annotation or 0 if
     * there's a problem getting the group count value.
     */
    public int getGroupCount(AnnotationMirror anno) {
        AnnotationValue groupCountValue = AnnotationUtils.getElementValuesWithDefaults(anno).get(regexValue);
        // If group count value is null then there's no Regex annotation
        // on the parameter so set the group count to 0. This would happen
        // if a non-regex string is passed to Pattern.compile but warnings
        // are suppressed.
        return (groupCountValue == null) ? 0 : (Integer) groupCountValue.getValue();
    }
}
