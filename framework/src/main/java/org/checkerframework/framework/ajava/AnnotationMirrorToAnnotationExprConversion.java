package org.checkerframework.framework.ajava;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Methods for converting a {@code AnnotationMirror} into a JavaParser {@code AnnotationExpr},
 * namely {@code annotationMirrorToAnnotationExpr}.
 */
public class AnnotationMirrorToAnnotationExprConversion {
    /**
     * Converts an AnnotationMirror into a JavaParser {@code AnnotationExpr}.
     *
     * @param annotation the annotation to convert
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
        if (convertedValues.size() == 1
                && convertedValues.get(0).getName().asString().equals("value")) {
            return new SingleMemberAnnotationExpr(name, convertedValues.get(0).getValue());
        }

        return new NormalAnnotationExpr(name, convertedValues);
    }

    /**
     * Converts a Set of AnnotationMirror into List of JavaParser {@code AnnotationExpr}.
     *
     * @param annotationMirrors the annotations to convert
     * @return a list of JavaParser {@code AnnotationExpr}s representing the same annotations
     * @see #annotationMirrorToAnnotationExpr
     */
    public static NodeList<AnnotationExpr> annotationMirrorSetToAnnotationExprList(
            Set<AnnotationMirror> annotationMirrors) {
        NodeList<AnnotationExpr> result = new NodeList<>();
        for (AnnotationMirror am : annotationMirrors) {
            result.add(annotationMirrorToAnnotationExpr(am));
        }
        return result;
    }

    /**
     * Converts a mapping of (annotation element &rarr; value) into a list of key-value pairs
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

    /**
     * A visitor that converts an annotation value from an {@code AnnotationMirror} to a JavaParser
     * node that can appear in an {@code AnnotationExpr}.
     */
    private static class AnnotationValueConverterVisitor
            implements AnnotationValueVisitor<Expression, Void> {
        @Override
        public Expression visit(AnnotationValue value, Void p) {
            // This is called only if the value couldn't be dispatched to any known type, which
            // should never happen.
            throw new BugInCF("Unknown annotation value type: " + value);
        }

        @Override
        public Expression visitAnnotation(AnnotationMirror value, Void p) {
            return AnnotationMirrorToAnnotationExprConversion.annotationMirrorToAnnotationExpr(
                    value);
        }

        @Override
        public Expression visitArray(List<? extends AnnotationValue> value, Void p) {
            NodeList<Expression> valueExpressions = new NodeList<>();
            for (AnnotationValue arrayValue : value) {
                valueExpressions.add(arrayValue.accept(this, null));
            }

            return new ArrayInitializerExpr(valueExpressions);
        }

        @Override
        public Expression visitBoolean(boolean value, Void p) {
            return new BooleanLiteralExpr(value);
        }

        @Override
        public Expression visitByte(byte value, Void p) {
            // Annotation byte values are automatically cast to the correct type, so using an
            // integer literal here works.
            return toIntegerLiteralExpr(value);
        }

        @Override
        public Expression visitChar(char value, Void p) {
            return new CharLiteralExpr(value);
        }

        @Override
        public Expression visitDouble(double value, Void p) {
            return new DoubleLiteralExpr(value);
        }

        @Override
        public Expression visitEnumConstant(VariableElement value, Void p) {
            // The enclosing element of an enum constant is the enum type itself.
            TypeElement enumElt = (TypeElement) value.getEnclosingElement();
            String[] components = enumElt.getQualifiedName().toString().split("\\.");
            Expression enumName = new NameExpr(components[0]);
            for (int i = 1; i < components.length; i++) {
                enumName = new FieldAccessExpr(enumName, components[i]);
            }

            return new FieldAccessExpr(enumName, value.getSimpleName().toString());
        }

        @Override
        public Expression visitFloat(float value, Void p) {
            return new DoubleLiteralExpr(value + "f");
        }

        @Override
        public Expression visitInt(int value, Void p) {
            return toIntegerLiteralExpr(value);
        }

        @Override
        public Expression visitLong(long value, Void p) {
            if (value < 0) {
                return new UnaryExpr(
                        new LongLiteralExpr(Long.toString(-value)), UnaryExpr.Operator.MINUS);
            }

            return new LongLiteralExpr(Long.toString(value));
        }

        @Override
        public Expression visitShort(short value, Void p) {
            // Annotation short values are automatically cast to the correct type, so using an
            // integer literal here works.
            return toIntegerLiteralExpr(value);
        }

        @Override
        public Expression visitString(String value, Void p) {
            return new StringLiteralExpr(value);
        }

        @Override
        public Expression visitType(TypeMirror value, Void p) {
            if (value.getKind() != TypeKind.DECLARED) {
                throw new BugInCF("Unexpected type for class expression: " + value);
            }

            DeclaredType type = (DeclaredType) value;
            ClassOrInterfaceType parsedType;
            try {
                parsedType =
                        StaticJavaParser.parseClassOrInterfaceType(
                                TypesUtils.getQualifiedName(type));
            } catch (ParseProblemException e) {
                throw new BugInCF("Invalid class or interface name: " + value, e);
            }

            return new ClassExpr(parsedType);
        }

        @Override
        public @Nullable Expression visitUnknown(AnnotationValue value, Void p) {
            return null;
        }

        /**
         * Creates a JavaParser expression node representing a literal with the given value.
         *
         * <p>JavaParser represents a negative literal with a {@code UnaryExpr} containing a {@code
         * IntegerLiteralExpr}, so this method won't necessarily return an {@code
         * IntegerLiteralExpr}.
         *
         * @param value the value for the literal
         * @return a JavaParser expression representing {@code value}
         */
        private Expression toIntegerLiteralExpr(int value) {
            if (value < 0) {
                return new UnaryExpr(
                        new IntegerLiteralExpr(Integer.toString(-value)), UnaryExpr.Operator.MINUS);
            }

            return new IntegerLiteralExpr(Integer.toString(value));
        }
    }
}
