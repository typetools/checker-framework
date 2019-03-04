package org.checkerframework.common.wholeprograminference;

import com.sun.tools.javac.code.Attribute.Array;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import scenelib.annotations.Annotation;
import scenelib.annotations.el.AnnotationDef;
import scenelib.annotations.field.AnnotationFieldType;
import scenelib.annotations.field.ArrayAFT;
import scenelib.annotations.field.BasicAFT;
import scenelib.annotations.field.ScalarAFT;

/**
 * This class has auxiliary methods that performs conversion between {@link
 * scenelib.annotations.Annotation} and {@link javax.lang.model.element.AnnotationMirror}.
 */
public class AnnotationConverter {

    /**
     * Converts an {@link javax.lang.model.element.AnnotationMirror} into an {@link
     * scenelib.annotations.Annotation}.
     */
    protected static Annotation annotationMirrorToAnnotation(AnnotationMirror am) {
        AnnotationDef def = new AnnotationDef(AnnotationUtils.annotationName(am));
        Map<String, AnnotationFieldType> fieldTypes = new HashMap<>();
        // Handling cases where there are fields in annotations.
        for (ExecutableElement ee : am.getElementValues().keySet()) {
            AnnotationFieldType aft =
                    getAnnotationFieldType(ee, am.getElementValues().get(ee).getValue());
            if (aft == null) {
                return null;
            }
            // Here we just add the type of the field into fieldTypes.
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
            }
            newValues.put(ee.getSimpleName().toString(), value);
        }
        Annotation out = new Annotation(def, newValues);
        return out;
    }

    /**
     * Converts an {@link scenelib.annotations.Annotation} into an {@link
     * javax.lang.model.element.AnnotationMirror}.
     */
    protected static AnnotationMirror annotationToAnnotationMirror(
            Annotation anno, ProcessingEnvironment processingEnv) {
        final AnnotationBuilder builder = new AnnotationBuilder(processingEnv, anno.def().name);
        for (String fieldKey : anno.fieldValues.keySet()) {
            addFieldToAnnotationBuilder(fieldKey, anno.fieldValues.get(fieldKey), builder);
        }
        return builder.build();
    }

    /** Returns an AnnotationFieldType given an ExecutableElement or value. */
    protected static AnnotationFieldType getAnnotationFieldType(
            ExecutableElement ee, Object value) {
        if (value instanceof List<?>) {
            AnnotationValue defaultValue = ee.getDefaultValue();
            if (defaultValue == null || ((ArrayType) ((Array) defaultValue).type) == null) {
                List<?> listV = (List<?>) value;
                if (!listV.isEmpty()) {
                    ScalarAFT scalarAFT =
                            (ScalarAFT)
                                    getAnnotationFieldType(
                                            ee, ((AnnotationValue) listV.get(0)).getValue());
                    if (scalarAFT != null) {
                        return new ArrayAFT(scalarAFT);
                    }
                }
                return null;
            }
            Type elemType = ((ArrayType) ((Array) defaultValue).type).elemtype;
            try {
                return new ArrayAFT(BasicAFT.forType(Class.forName(elemType.toString())));
            } catch (ClassNotFoundException e) {
                throw new BugInCF(e.getMessage());
            }
        } else if (value instanceof Boolean) {
            return BasicAFT.forType(boolean.class);
        } else if (value instanceof Character) {
            return BasicAFT.forType(char.class);
        } else if (value instanceof Double) {
            return BasicAFT.forType(double.class);
        } else if (value instanceof Float) {
            return BasicAFT.forType(float.class);
        } else if (value instanceof Integer) {
            return BasicAFT.forType(int.class);
        } else if (value instanceof Long) {
            return BasicAFT.forType(long.class);
        } else if (value instanceof Short) {
            return BasicAFT.forType(short.class);
        } else if (value instanceof String) {
            return BasicAFT.forType(String.class);
        }
        return null;
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
