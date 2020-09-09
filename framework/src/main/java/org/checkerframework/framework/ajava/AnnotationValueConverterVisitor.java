package org.checkerframework.framework.ajava;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

public class AnnotationValueConverterVisitor implements AnnotationValueVisitor<Expression, Void> {
    @Override
    public Expression visit(AnnotationValue value, Void p) {
        throw new BugInCF("Unknown annotation value type: " + value);
    }

    @Override
    public Expression visitAnnotation(AnnotationMirror value, Void p) {
        return AnnotationConversion.annotationMirrorToAnnotationExpr(value);
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

        return new LongLiteralExpr(Long.toString(-value));
    }

    @Override
    public Expression visitShort(short value, Void p) {
        return toIntegerLiteralExpr(value);
    }

    @Override
    public Expression visitString(String value, Void p) {
        // Does this need to be escaped?
        return new StringLiteralExpr(value);
    }

    @Override
    public Expression visitType(TypeMirror value, Void p) {
        // I believe according to JLS 15.8.2, a class expression can only contain a type name, so
        // there's no need to handle more complex type (those with generics).
        // TODO: This prints the full qualified name, does that break for anonymous inner classes?
        if (value.getKind() != TypeKind.DECLARED) {
            throw new BugInCF("Unexpected type for class expression: " + value);
        }

        DeclaredType type = (DeclaredType) value;
        ParseResult<ClassOrInterfaceType> parseResult =
                new JavaParser()
                        .parseClassOrInterfaceType(TypesUtils.getQualifiedName(type).toString());
        if (!parseResult.isSuccessful()) {
            System.out.println("Invalid class or interface name: " + value);
        }

        return new ClassExpr(parseResult.getResult().get());
    }

    @Override
    public @Nullable Expression visitUnknown(AnnotationValue value, Void p) {
        return null;
    }

    private Expression toIntegerLiteralExpr(int value) {
        if (value < 0) {
            return new UnaryExpr(
                    new IntegerLiteralExpr(Integer.toString(-value)), UnaryExpr.Operator.MINUS);
        }

        return new IntegerLiteralExpr(Integer.toString(-value));
    }
}
