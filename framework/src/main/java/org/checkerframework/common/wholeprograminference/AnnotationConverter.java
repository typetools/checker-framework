package org.checkerframework.common.wholeprograminference;

import com.sun.tools.javac.code.Type.ArrayType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.reflection.Signatures;
import scenelib.annotations.Annotation;
import scenelib.annotations.el.AnnotationDef;
import scenelib.annotations.field.AnnotationFieldType;
import scenelib.annotations.field.ArrayAFT;
import scenelib.annotations.field.BasicAFT;
import scenelib.annotations.field.ClassTokenAFT;
import scenelib.annotations.field.EnumAFT;
import scenelib.annotations.field.ScalarAFT;

/**
 * This class contains static methods that convert between {@link scenelib.annotations.Annotation}
 * and {@link javax.lang.model.element.AnnotationMirror}.
 */
public class AnnotationConverter {

    /**
     * Converts an {@link javax.lang.model.element.AnnotationMirror} into an {@link
     * scenelib.annotations.Annotation}.
     *
     * @param am the AnnotationMirror
     * @return the Annotation
     */
    public static Annotation annotationMirrorToAnnotation(AnnotationMirror am) {
        @SuppressWarnings("signature:argument.type.incompatible") // TODO: bug for inner classes
        AnnotationDef def =
                new AnnotationDef(
                        AnnotationUtils.annotationName(am),
                        String.format(
                                "annotationMirrorToAnnotation %s [%s] keyset=%s",
                                am, am.getClass(), am.getElementValues().keySet()));
        Map<String, AnnotationFieldType> fieldTypes = new HashMap<>();
        // Handling cases where there are fields in annotations.
        for (ExecutableElement ee : am.getElementValues().keySet()) {
            AnnotationFieldType aft = getAnnotationFieldType(ee);
            fieldTypes.put(ee.getSimpleName().toString(), aft);
        }
        def.setFieldTypes(fieldTypes);

        // Now, we handle the values of those types below
        Map<? extends ExecutableElement, ? extends AnnotationValue> values = am.getElementValues();
        Map<String, Object> newValues = new HashMap<>();
        for (ExecutableElement ee : values.keySet()) {
            Object value = values.get(ee).getValue();
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> valueList = (List<Object>) value;
                List<Object> newList = new ArrayList<>();
                // If we have a List here, then it is a List of AnnotatedValue.
                // Converting each AnnotatedValue to its respective Java type:
                for (Object o : valueList) {
                    newList.add(((AnnotationValue) o).getValue());
                }
                value = newList;
            } else if (value instanceof TypeMirror) {
                try {
                    value = Class.forName(TypesUtils.binaryName((TypeMirror) value));
                } catch (ClassNotFoundException e) {
                    throw new BugInCF(String.format("value = %s [%s]", value, value.getClass()), e);
                }
            }
            newValues.put(ee.getSimpleName().toString(), value);
        }
        Annotation out = new Annotation(def, newValues);
        return out;
    }

    /**
     * Converts an {@link scenelib.annotations.Annotation} into an {@link
     * javax.lang.model.element.AnnotationMirror}.
     *
     * @param anno the Annotation
     * @param processingEnv the ProcessingEnvironment
     * @return the AnnotationMirror
     */
    protected static AnnotationMirror annotationToAnnotationMirror(
            Annotation anno, ProcessingEnvironment processingEnv) {
        final AnnotationBuilder builder =
                new AnnotationBuilder(
                        processingEnv, Signatures.binaryNameToFullyQualified(anno.def().name));
        for (String fieldKey : anno.fieldValues.keySet()) {
            addFieldToAnnotationBuilder(fieldKey, anno.fieldValues.get(fieldKey), builder);
        }
        return builder.build();
    }

    /**
     * Returns the type of an element (that is, a field) of an annotation.
     *
     * @param ee an element (that is, a field) of an annotation
     * @return the type of the given annotation field
     */
    protected static @Nullable AnnotationFieldType getAnnotationFieldType(ExecutableElement ee) {
        return typeMirrorToAnnotationFieldType(ee.getReturnType());
    }

    /**
     * Converts a TypeMirror to an AnnotationFieldType.
     *
     * @param tm a type for an annotation element/field: primitive, String, class, enum constant, or
     *     array thereof
     * @return an AnnotationFieldType corresponding to the argument
     */
    protected static AnnotationFieldType typeMirrorToAnnotationFieldType(TypeMirror tm) {
        switch (tm.getKind()) {
            case BOOLEAN:
                return BasicAFT.forType(boolean.class);
                // Primitves
            case BYTE:
                return BasicAFT.forType(byte.class);
            case CHAR:
                return BasicAFT.forType(char.class);
            case DOUBLE:
                return BasicAFT.forType(double.class);
            case FLOAT:
                return BasicAFT.forType(float.class);
            case INT:
                return BasicAFT.forType(int.class);
            case LONG:
                return BasicAFT.forType(long.class);
            case SHORT:
                return BasicAFT.forType(short.class);

            case ARRAY:
                TypeMirror componentType = (((ArrayType) tm).getComponentType());
                AnnotationFieldType componentAFT = typeMirrorToAnnotationFieldType(componentType);
                return new ArrayAFT((ScalarAFT) componentAFT);

            case DECLARED:
                Name className = TypesUtils.getQualifiedName((DeclaredType) tm);
                if (className.contentEquals("java.lang.String")) {
                    return BasicAFT.forType(String.class);
                } else if (className.contentEquals("java.lang.Class")) {
                    return ClassTokenAFT.ctaft;
                } else {
                    // This must be an enum constant.
                    return new EnumAFT(className.toString());
                }

            default:
                throw new BugInCF(
                        "typeMirrorToAnnotationFieldType: unexpected argument %s [%s %s]",
                        tm, tm.getKind(), tm.getClass());
        }
    }

    /**
     * Adds a field to an AnnotationBuilder.
     *
     * @param fieldKey is the name of the field
     * @param obj is the value of the field
     * @param builder is the AnnotationBuilder
     */
    @SuppressWarnings("unchecked") // This is actually checked in the first instanceOf call below.
    protected static void addFieldToAnnotationBuilder(
            String fieldKey, Object obj, AnnotationBuilder builder) {
        if (obj instanceof List<?>) {
            builder.setValue(fieldKey, (List<Object>) obj);
        } else if (obj instanceof String) {
            builder.setValue(fieldKey, (String) obj);
        } else if (obj instanceof Integer) {
            builder.setValue(fieldKey, (Integer) obj);
        } else if (obj instanceof Float) {
            builder.setValue(fieldKey, (Float) obj);
        } else if (obj instanceof Long) {
            builder.setValue(fieldKey, (Long) obj);
        } else if (obj instanceof Boolean) {
            builder.setValue(fieldKey, (Boolean) obj);
        } else if (obj instanceof Character) {
            builder.setValue(fieldKey, (Character) obj);
        } else if (obj instanceof Class<?>) {
            builder.setValue(fieldKey, (Class<?>) obj);
        } else if (obj instanceof Double) {
            builder.setValue(fieldKey, (Double) obj);
        } else if (obj instanceof Enum<?>) {
            builder.setValue(fieldKey, (Enum<?>) obj);
        } else if (obj instanceof Enum<?>[]) {
            builder.setValue(fieldKey, (Enum<?>[]) obj);
        } else if (obj instanceof AnnotationMirror) {
            builder.setValue(fieldKey, (AnnotationMirror) obj);
        } else if (obj instanceof Object[]) {
            builder.setValue(fieldKey, (Object[]) obj);
        } else if (obj instanceof TypeMirror) {
            builder.setValue(fieldKey, (TypeMirror) obj);
        } else if (obj instanceof Short) {
            builder.setValue(fieldKey, (Short) obj);
        } else if (obj instanceof VariableElement) {
            builder.setValue(fieldKey, (VariableElement) obj);
        } else if (obj instanceof VariableElement[]) {
            builder.setValue(fieldKey, (VariableElement[]) obj);
        } else {
            throw new BugInCF("Unrecognized type: " + obj.getClass());
        }
    }
}
