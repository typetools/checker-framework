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

public class AnnotationConversion {
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

    private static Name createQualifiedName(String name) {
        String[] components = name.split("\\.");
        Name result = new Name(components[0]);
        for (int i = 1; i < components.length; i++) {
            result = new Name(result, components[i]);
        }

        return result;
    }
}
