package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * Methods for converting a {@code AnnotationMirror} into a JavaParser {@code AnnotationExpr},
 * namely {@code annotationMirrorToAnnotationExpr}.
 */
public class AnnotationConversion {
    /**
     * Converts {@code annotation} into an {@code AnnotationExpr}.
     *
     * @param annotation annotation to convert
     * @return a JavaParser {@code AnnotationExpr} representing the same annotation with the same
     *     element values. The converted annotation will contain the annotation's fully qualified
     *     name.
     */
    public static AnnotationExpr annotationMirrorToAnnotationExpr(AnnotationMirror annotation) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                annotation.getElementValues();
        Name name = createQualifiedName(AnnotationUtils.annotationName(annotation));
        if (values.isEmpty()) {
            return new MarkerAnnotationExpr(name);
        }

        NodeList<MemberValuePair> convertedValues = convertAnnotationValues(values);
        if (convertedValues.size() == 1) {
            return new SingleMemberAnnotationExpr(name, convertedValues.get(0).getValue());
        }

        return new NormalAnnotationExpr(name, convertedValues);
    }

    /**
     * Converts a mapping of annotation elements to their values to a list of key-value pairs
     * containing the JavaParser representations of the same values.
     *
     * @param values mapping of element values from an {@code AnnotationMirror}
     * @return a list of the key-value pairs in {@code values} converted to their JavaParser
     *     representations
     */
    private static NodeList<MemberValuePair> convertAnnotationValues(
            Map<? extends ExecutableElement, ? extends AnnotationValue> values) {
        NodeList<MemberValuePair> convertedValues = new NodeList<>();
        AnnotationValueConverterVisitor converter = new AnnotationValueConverterVisitor();
        for (ExecutableElement valueName : values.keySet()) {
            AnnotationValue value = values.get(valueName);
            convertedValues.add(
                    new MemberValuePair(
                            valueName.getSimpleName().toString(), value.accept(converter, null)));
        }

        return convertedValues;
    }

    /**
     * Given a fully qualified name, creates a JavaParser {@code Name} structure representing the
     * same name.
     *
     * @param name the fully qualified name to convert
     * @return a JavaParser {@code Name} holding {@code name}
     */
    private static Name createQualifiedName(String name) {
        String[] components = name.split("\\.");
        Name result = new Name(components[0]);
        for (int i = 1; i < components.length; i++) {
            result = new Name(result, components[i]);
        }

        return result;
    }
}
