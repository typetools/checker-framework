package org.checkerframework.common.wholeprograminference;

import com.sun.tools.javac.code.Type.ArrayType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.field.AnnotationAFT;
import org.checkerframework.afu.scenelib.field.AnnotationFieldType;
import org.checkerframework.afu.scenelib.field.ArrayAFT;
import org.checkerframework.afu.scenelib.field.BasicAFT;
import org.checkerframework.afu.scenelib.field.ClassTokenAFT;
import org.checkerframework.afu.scenelib.field.EnumAFT;
import org.checkerframework.afu.scenelib.field.ScalarAFT;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.reflection.Signatures;
import org.plumelib.util.ArrayMap;
import org.plumelib.util.CollectionsP;

/**
 * This class contains static methods that convert between {@link Annotation} and {@link
 * javax.lang.model.element.AnnotationMirror}.
 */
public class AnnotationConverter {

  /** Do not instantiate. */
  private AnnotationConverter() {
    throw new Error("Do not instantiate");
  }

  /**
   * Converts an {@link javax.lang.model.element.AnnotationMirror} into an {@link Annotation}.
   *
   * @param am the AnnotationMirror
   * @return the Annotation
   */
  public static Annotation annotationMirrorToAnnotation(AnnotationMirror am) {
    Map<String, AnnotationFieldType> fieldTypes = new ArrayMap<>(am.getElementValues().size());
    // Handling cases where there are fields in annotations.
    for (ExecutableElement ee : am.getElementValues().keySet()) {
      AnnotationFieldType aft = getAnnotationFieldType(ee);
      fieldTypes.put(ee.getSimpleName().toString(), aft);
    }

    @SuppressWarnings("signature:assignment") // TODO: bug for inner classes
    @BinaryName String annoName = AnnotationUtils.annotationName(am);
    // Capturing `am` rather than the strings would prevent it from being garbage-collected.
    // `fieldTypes.keySet()` is a view, so copy it rather than retaining `fieldTypes` itself.
    String amClassName = am.getClass().getName();
    List<String> fieldNames = new ArrayList<>(fieldTypes.keySet());
    AnnotationDef def =
        new AnnotationDef(
            annoName,
            fieldTypes,
            // The source is computed lazily because it is used only for diagnostics.
            () ->
                String.format(
                    "annotationMirrorToAnnotation %s [%s] keyset=%s",
                    annoName, amClassName, fieldNames));

    // Now, we handle the values of those types below
    Map<? extends ExecutableElement, ? extends AnnotationValue> values = am.getElementValues();
    Map<String, Object> newValues = new HashMap<>(values.size());
    for (ExecutableElement ee : values.keySet()) {
      Object value = values.get(ee).getValue();
      if (value instanceof List) {
        // If we have a List here, then it is a List of AnnotationValue.
        // Convert each AnnotationValue to its respective Java type.
        // TODO: The elements of an array of class literals are left as TypeMirrors, whereas a
        // scalar class literal is converted to a Class below.
        @SuppressWarnings("unchecked")
        List<AnnotationValue> valueList = (List<AnnotationValue>) value;
        value = CollectionsP.mapList(AnnotationConverter::arrayElementValue, valueList);
      } else if (value instanceof TypeMirror) {
        try {
          value = Class.forName(TypesUtils.binaryName((TypeMirror) value));
        } catch (ClassNotFoundException e) {
          throw new BugInCF(e, "value = %s [%s]", value, value.getClass());
        }
      } else if (value instanceof AnnotationMirror subannotation) {
        // A subannotation's value must be an Annotation, not an AnnotationMirror; see
        // AnnotationAFT.
        value = annotationMirrorToAnnotation(subannotation);
      }
      newValues.put(ee.getSimpleName().toString(), value);
    }
    Annotation out = new Annotation(def, newValues);
    return out;
  }

  /**
   * Returns the Java value of one element of an array-valued annotation element.
   *
   * @param av one element of an array-valued annotation element
   * @return the Java value of {@code av}
   */
  private static Object arrayElementValue(AnnotationValue av) {
    Object value = av.getValue();
    if (value instanceof AnnotationMirror subannotation) {
      // A subannotation's value must be an Annotation, not an AnnotationMirror; see
      // AnnotationAFT.
      return annotationMirrorToAnnotation(subannotation);
    }
    return value;
  }

  /**
   * Converts an {@link Annotation} into an {@link javax.lang.model.element.AnnotationMirror}.
   *
   * @param anno the Annotation
   * @param processingEnv the ProcessingEnvironment
   * @return the AnnotationMirror
   */
  protected static AnnotationMirror annotationToAnnotationMirror(
      Annotation anno, ProcessingEnvironment processingEnv) {
    AnnotationBuilder builder =
        new AnnotationBuilder(
            processingEnv, Signatures.binaryNameToFullyQualified(anno.def().name));
    for (String fieldKey : anno.fieldValues.keySet()) {
      addFieldToAnnotationBuilder(fieldKey, anno.fieldValues.get(fieldKey), builder, processingEnv);
    }
    return builder.build();
  }

  /**
   * Returns the type of an element (that is, a field) of an annotation.
   *
   * @param ee an element (that is, a field) of an annotation
   * @return the type of the given annotation field
   */
  protected static AnnotationFieldType getAnnotationFieldType(ExecutableElement ee) {
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
      case BOOLEAN -> {
        return BasicAFT.forType(boolean.class);
      }
      // Primitives
      case BYTE -> {
        return BasicAFT.forType(byte.class);
      }
      case CHAR -> {
        return BasicAFT.forType(char.class);
      }
      case DOUBLE -> {
        return BasicAFT.forType(double.class);
      }
      case FLOAT -> {
        return BasicAFT.forType(float.class);
      }
      case INT -> {
        return BasicAFT.forType(int.class);
      }
      case LONG -> {
        return BasicAFT.forType(long.class);
      }
      case SHORT -> {
        return BasicAFT.forType(short.class);
      }
      case ARRAY -> {
        TypeMirror componentType = ((ArrayType) tm).getComponentType();
        AnnotationFieldType componentAFT = typeMirrorToAnnotationFieldType(componentType);
        return new ArrayAFT((ScalarAFT) componentAFT);
      }
      case DECLARED -> {
        String className = TypesUtils.getQualifiedName((DeclaredType) tm);
        if (className.equals("java.lang.String")) {
          return BasicAFT.forType(String.class);
        } else if (className.equals("java.lang.Class")) {
          return ClassTokenAFT.ctaft;
        }
        TypeElement classElt = (TypeElement) ((DeclaredType) tm).asElement();
        if (classElt.getKind() == ElementKind.ANNOTATION_TYPE) {
          return new AnnotationAFT(annotationTypeToAnnotationDef(classElt));
        }
        return new EnumAFT(className);
      }
      default ->
          throw new BugInCF(
              "typeMirrorToAnnotationFieldType: unexpected argument %s [%s %s]",
              tm, tm.getKind(), tm.getClass());
    }
  }

  /**
   * Cache for {@link #annotationTypeToAnnotationDef}, which is called once per annotation-valued
   * element per storage write. The keys are weak because a {@code TypeElement} lives only as long
   * as the compilation that created it; for the entries to be collectable, an {@code AnnotationDef}
   * in this map must not retain its key.
   */
  private static final Map<TypeElement, AnnotationDef> annotationDefCache =
      Collections.synchronizedMap(new WeakHashMap<>());

  /**
   * Returns the definition of the given annotation type. Unlike the definition that {@link
   * #annotationMirrorToAnnotation} creates, this one contains every element of the annotation type,
   * not just those that some particular annotation writes.
   *
   * @param annotationElt the element for an annotation type
   * @return the definition of the given annotation type
   */
  private static AnnotationDef annotationTypeToAnnotationDef(TypeElement annotationElt) {
    AnnotationDef cached = annotationDefCache.get(annotationElt);
    if (cached != null) {
      return cached;
    }
    List<ExecutableElement> elements = ElementFilter.methodsIn(annotationElt.getEnclosedElements());
    Map<String, AnnotationFieldType> fieldTypes = new ArrayMap<>(elements.size());
    for (ExecutableElement element : elements) {
      fieldTypes.put(element.getSimpleName().toString(), getAnnotationFieldType(element));
    }
    String annotationName = annotationElt.getQualifiedName().toString();
    // The source is a plain string rather than a supplier because it is just a concatenation of
    // `annotationName`, which has already been computed.  Capturing `annotationElt` in a supplier
    // would prevent this map's keys from being collected.
    @SuppressWarnings("signature:argument") // TODO: bug for inner classes
    AnnotationDef result =
        new AnnotationDef(
            annotationName, fieldTypes, "annotationTypeToAnnotationDef " + annotationName);
    annotationDefCache.put(annotationElt, result);
    return result;
  }

  /**
   * Adds a field to an AnnotationBuilder.
   *
   * @param fieldKey is the name of the field
   * @param obj is the value of the field
   * @param builder is the AnnotationBuilder
   * @param processingEnv the ProcessingEnvironment, for converting a subannotation
   */
  protected static void addFieldToAnnotationBuilder(
      String fieldKey, Object obj, AnnotationBuilder builder, ProcessingEnvironment processingEnv) {
    if (obj instanceof List<?> list) {
      builder.setValue(
          fieldKey,
          CollectionsP.mapList(elt -> fieldValueToBuilderValue(elt, processingEnv), list));
    } else if (obj instanceof String s) {
      builder.setValue(fieldKey, s);
    } else if (obj instanceof Integer i) {
      builder.setValue(fieldKey, i);
    } else if (obj instanceof Float f) {
      builder.setValue(fieldKey, f);
    } else if (obj instanceof Long l) {
      builder.setValue(fieldKey, l);
    } else if (obj instanceof Boolean b) {
      builder.setValue(fieldKey, b);
    } else if (obj instanceof Character c) {
      builder.setValue(fieldKey, c);
    } else if (obj instanceof Class<?> cls) {
      builder.setValue(fieldKey, cls);
    } else if (obj instanceof Double d) {
      builder.setValue(fieldKey, d);
    } else if (obj instanceof Enum<?> e) {
      builder.setValue(fieldKey, e);
    } else if (obj instanceof Enum<?>[] enumArr) {
      builder.setValue(fieldKey, enumArr);
    } else if (obj instanceof AnnotationMirror am) {
      builder.setValue(fieldKey, am);
    } else if (obj instanceof Object[] objArr) {
      builder.setValue(fieldKey, objArr);
    } else if (obj instanceof TypeMirror tm) {
      builder.setValue(fieldKey, tm);
    } else if (obj instanceof Short sh) {
      builder.setValue(fieldKey, sh);
    } else if (obj instanceof VariableElement ve) {
      builder.setValue(fieldKey, ve);
    } else if (obj instanceof Byte by) {
      builder.setValue(fieldKey, by);
    } else if (obj instanceof Annotation anno) {
      builder.setValue(fieldKey, annotationToAnnotationMirror(anno, processingEnv));
    } else if (obj instanceof VariableElement[] veArr) {
      builder.setValue(fieldKey, veArr);
    } else {
      throw new BugInCF("Unrecognized type: " + obj.getClass());
    }
  }

  /**
   * Returns a value that {@link AnnotationBuilder} accepts, for the given value of an annotation
   * element of an {@link Annotation}. A subannotation becomes an {@link AnnotationMirror}; any
   * other value is returned unchanged.
   *
   * @param obj the value of an annotation element of an {@link Annotation}, or of one array element
   *     thereof
   * @param processingEnv the ProcessingEnvironment, for converting a subannotation
   * @return a value that {@link AnnotationBuilder} accepts
   */
  private static Object fieldValueToBuilderValue(Object obj, ProcessingEnvironment processingEnv) {
    if (obj instanceof Annotation anno) {
      return annotationToAnnotationMirror(anno, processingEnv);
    }
    return obj;
  }
}
