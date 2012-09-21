package checkers.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;

import checkers.source.SourceChecker;

/**
 * Builds an annotation mirror that may have some values.
 *
 * Constructing an {@link AnnotationMirror} requires: <br />
 * 1. Constructing the builder with the desired annotation class <br />
 * 2. Setting each value individually using {@code setValue} methods <br />
 * 3. Calling {@link #build()} to get the annotation
 *
 * Once an annotation is built, no further modification or calls to build can be
 * made. Otherwise, a {@link IllegalStateException} is thrown.
 *
 * All setter methods throw {@link IllegalArgumentException} if the specified
 * element is not found, or if the given value is not a subtype of the
 * expected type.
 *
 * TODO: Doesn't type check arrays yet
 */
public class AnnotationBuilder {

    private final ProcessingEnvironment env;
    private final TypeElement annotationElt;
    private final DeclaredType annotationType;
    private final Map<ExecutableElement, AnnotationValue> elementValues;

    public AnnotationBuilder(ProcessingEnvironment env,
            Class<? extends Annotation> anno) {
        this(env, anno.getCanonicalName());
    }

    public AnnotationBuilder(ProcessingEnvironment env, CharSequence name) {
        this.env = env;
        this.annotationElt = env.getElementUtils().getTypeElement(name);
        assert annotationElt.getKind() == ElementKind.ANNOTATION_TYPE;
        this.annotationType = (DeclaredType) annotationElt.asType();
        this.elementValues = new LinkedHashMap<ExecutableElement, AnnotationValue>();
    }

    public AnnotationBuilder(ProcessingEnvironment env,
            AnnotationMirror annotation) {
        this.env = env;
        this.annotationType = annotation.getAnnotationType();
        this.annotationElt = (TypeElement) annotationType.asElement();

        this.elementValues = new LinkedHashMap<ExecutableElement, AnnotationValue>();
        // AnnotationValues are immutable so putAll should suffice
        this.elementValues.putAll(annotation.getElementValues());
    }

    private boolean wasBuilt = false;

    private void assertNotBuilt() {
        if (wasBuilt) {
            SourceChecker.errorAbort("AnnotationBuilder: error: type was already built");
        }
    }

    public AnnotationMirror build() {
        assertNotBuilt();
        wasBuilt = true;
        return new AnnotationMirror() {

            @Override
            public DeclaredType getAnnotationType() {
                return annotationType;
            }

            @Override
            public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
                return Collections.unmodifiableMap(elementValues);
            }

            @Override
            public String toString() {
                StringBuilder buf = new StringBuilder();
                buf.append("@");
                buf.append(annotationType);
                int len = elementValues.size();
                if (len > 0) {
                    buf.append('(');
                    boolean first = true;
                    for (Map.Entry<ExecutableElement, AnnotationValue> pair : elementValues.entrySet()) {
                        if (!first)
                            buf.append(", ");
                        first = false;

                        String name = pair.getKey().getSimpleName().toString();
                        if (len > 1 || !name.equals("value")) {
                            buf.append(name);
                            buf.append('=');
                        }
                        buf.append(pair.getValue());
                    }
                    buf.append(')');
                }
                return buf.toString();
                // return "@" + annotationType + "(" + elementValues + ")";
            }
        };
    }

    public AnnotationBuilder setValue(CharSequence elementName,
            AnnotationMirror value) {
        setValue(elementName, (Object) value);
        return this;
    }

    public AnnotationBuilder setValue(CharSequence elementName,
            List<? extends Object> values) {
        assertNotBuilt();
        List<AnnotationValue> value = new ArrayList<AnnotationValue>(values.size());
        ExecutableElement var = findElement(elementName);
        TypeMirror expectedType = var.getReturnType();
        if (expectedType.getKind() != TypeKind.ARRAY) {
            SourceChecker.errorAbort("value is an array while expected type is not");
            return null; // dead code
        }
        expectedType = ((ArrayType) expectedType).getComponentType();

        for (Object v : values) {
            checkSubtype(expectedType, v);
            value.add(createValue(v));
        }
        AnnotationValue val = createValue(value);
        elementValues.put(var, val);
        return this;
    }

    public AnnotationBuilder setValue(CharSequence elementName, Object[] values) {
        return setValue(elementName, Arrays.asList(values));
    }

    public AnnotationBuilder setValue(CharSequence elementName, Boolean value) {
        return setValue(elementName, (Object) value);
    }

    public AnnotationBuilder setValue(CharSequence elementName, Character value) {
        return setValue(elementName, (Object) value);
    }

    public AnnotationBuilder setValue(CharSequence elementName, Double value) {
        return setValue(elementName, (Object) value);
    }

    public AnnotationBuilder setValue(CharSequence elementName, Float value) {
        return setValue(elementName, (Object) value);
    }

    public AnnotationBuilder setValue(CharSequence elementName, Integer value) {
        return setValue(elementName, (Object) value);
    }

    public AnnotationBuilder setValue(CharSequence elementName, Long value) {
        return setValue(elementName, (Object) value);
    }

    public AnnotationBuilder setValue(CharSequence elementName, Short value) {
        return setValue(elementName, (Object) value);
    }

    public AnnotationBuilder setValue(CharSequence elementName, String value) {
        return setValue(elementName, (Object) value);
    }

    public AnnotationBuilder setValue(CharSequence elementName, TypeMirror value) {
        assertNotBuilt();
        AnnotationValue val = createValue(value);
        ExecutableElement var = findElement(elementName);
        // Check subtyping
        if (!TypesUtils.isClass(var.getReturnType())) {
            SourceChecker.errorAbort("expected " + var.getReturnType());
            return null; // dead code
        }

        elementValues.put(var, val);
        return this;
    }

    private TypeMirror typeFromClass(Class<?> clazz) {
        if (clazz == void.class) {
            return env.getTypeUtils().getNoType(TypeKind.VOID);
        } else if (clazz.isPrimitive()) {
            String primitiveName = clazz.getName().toUpperCase();
            TypeKind primitiveKind = TypeKind.valueOf(primitiveName);
            return env.getTypeUtils().getPrimitiveType(primitiveKind);
        } else if (clazz.isArray()) {
            TypeMirror componentType = typeFromClass(clazz.getComponentType());
            return env.getTypeUtils().getArrayType(componentType);
        } else {
            TypeElement element = env.getElementUtils().getTypeElement(
                    clazz.getCanonicalName());
            if (element == null) {
                SourceChecker.errorAbort("Unrecognized class: " + clazz);
                return null; // dead code
            }
            return element.asType();
        }
    }

    public AnnotationBuilder setValue(CharSequence elementName, Class<?> value) {
        return setValue(elementName, typeFromClass(value));
    }

    public AnnotationBuilder setValue(CharSequence elementName, Enum<?> value) {
        assertNotBuilt();
        VariableElement enumElt = findEnumElement(value);
        return setValue(elementName, enumElt);
    }

    public AnnotationBuilder setValue(CharSequence elementName,
            VariableElement value) {
        ExecutableElement var = findElement(elementName);
        if (var.getReturnType().getKind() != TypeKind.DECLARED) {
            SourceChecker.errorAbort("expected a non enum: " + var.getReturnType());
            return null; // dead code
        }
        if (!((DeclaredType) var.getReturnType()).asElement().equals(
                value.getEnclosingElement())) {
            SourceChecker.errorAbort("expected a different type of enum: "
                    + value.getEnclosingElement());
            return null; // dead code
        }
        elementValues.put(var, createValue(value));
        return this;
    }

    // Keep this version synchronized with the VariableElement[] version below
    public AnnotationBuilder setValue(CharSequence elementName, Enum<?>[] values) {
        assertNotBuilt();
        VariableElement enumElt = findEnumElement(values[0]);
        ExecutableElement var = findElement(elementName);

        TypeMirror expectedType = var.getReturnType();
        if (expectedType.getKind() != TypeKind.ARRAY) {
            SourceChecker.errorAbort("expected a non array: " + var.getReturnType());
            return null; // dead code
        }

        expectedType = ((ArrayType) expectedType).getComponentType();
        if (expectedType.getKind() != TypeKind.DECLARED) {
            SourceChecker.errorAbort("expected a non enum component type: "
                    + var.getReturnType());
            return null; // dead code
        }
        if (!((DeclaredType) expectedType).asElement().equals(
                enumElt.getEnclosingElement())) {
            SourceChecker.errorAbort("expected a different type of enum: "
                    + enumElt.getEnclosingElement());
            return null; // dead code
        }

        List<AnnotationValue> res = new ArrayList<AnnotationValue>(values.length);
        for (Enum<?> ev : values) {
            checkSubtype(expectedType, ev);
            enumElt = findEnumElement(ev);
            res.add(createValue(enumElt));
        }
        AnnotationValue val = createValue(res);
        elementValues.put(var, val);
        return this;
    }

    // Keep this version synchronized with the Enum<?>[] version above.
    // Which one is more useful/general? Unifying adds overhead of creating
    // another array.
    public AnnotationBuilder setValue(CharSequence elementName,
            VariableElement[] values) {
        assertNotBuilt();
        ExecutableElement var = findElement(elementName);

        TypeMirror expectedType = var.getReturnType();
        if (expectedType.getKind() != TypeKind.ARRAY) {
            SourceChecker.errorAbort("expected an array, but found: " + expectedType);
            return null; // dead code
        }

        expectedType = ((ArrayType) expectedType).getComponentType();
        if (expectedType.getKind() != TypeKind.DECLARED) {
            SourceChecker.errorAbort("expected a declared component type, but found: "
                    + expectedType + " kind: " + expectedType.getKind());
            return null; // dead code
        }
        if (!((DeclaredType) expectedType).equals(values[0].asType())) {
            SourceChecker.errorAbort("expected a different declared component type: "
                    + expectedType + " vs. " + values[0]);
            return null; // dead code
        }

        List<AnnotationValue> res = new ArrayList<AnnotationValue>(values.length);
        for (VariableElement ev : values) {
            checkSubtype(expectedType, ev);
            // Is there a better way to distinguish between enums and
            // references to constants?
            if (ev.getConstantValue() != null) {
                res.add(createValue(ev.getConstantValue()));
            } else {
                res.add(createValue(ev));
            }
        }
        AnnotationValue val = createValue(res);
        elementValues.put(var, val);
        return this;
    }

    private VariableElement findEnumElement(Enum<?> value) {
        String enumClass = value.getDeclaringClass().getCanonicalName();
        TypeElement enumClassElt = env.getElementUtils().getTypeElement(
                enumClass);
        assert enumClassElt != null;
        for (Element enumElt : enumClassElt.getEnclosedElements()) {
            if (enumElt.getSimpleName().contentEquals(value.name()))
                return (VariableElement) enumElt;
        }
        SourceChecker.errorAbort("cannot be here");
        return null; // dead code
    }

    private AnnotationBuilder setValue(CharSequence key, Object value) {
        assertNotBuilt();
        AnnotationValue val = createValue(value);
        ExecutableElement var = findElement(key);
        checkSubtype(var.getReturnType(), value);
        elementValues.put(var, val);
        return this;
    }

    public ExecutableElement findElement(CharSequence key) {
        for (ExecutableElement elt : ElementFilter.methodsIn(annotationElt.getEnclosedElements())) {
            if (elt.getSimpleName().contentEquals(key)) {
                return elt;
            }
        }
        SourceChecker.errorAbort("Couldn't find " + key + " element in "
                + annotationElt);
        return null; // dead code
    }

    // TODO: this method always returns true and no-one ever looks at the return
    // value.
    private boolean checkSubtype(TypeMirror expected, Object givenValue) {
        Types types = env.getTypeUtils();

        if (expected.getKind().isPrimitive())
            expected = types.boxedClass((PrimitiveType) expected).asType();

        if (expected.getKind() == TypeKind.DECLARED
                && TypesUtils.isClass(expected)
                && givenValue instanceof TypeMirror)
            return true;

        TypeMirror found;
        boolean isSubtype;

        if (expected.getKind() == TypeKind.DECLARED
                && ((DeclaredType) expected).asElement().getKind() == ElementKind.ANNOTATION_TYPE
                && givenValue instanceof AnnotationMirror) {
            found = ((AnnotationMirror) givenValue).getAnnotationType();
            isSubtype = ((DeclaredType) expected).asElement().equals(
                    ((DeclaredType) found).asElement());
        } else if (givenValue instanceof AnnotationMirror) {
            found = ((AnnotationMirror) givenValue).getAnnotationType();
            // TODO: why is this always failing???
            isSubtype = false;
        } else if (givenValue instanceof VariableElement) {
            found = ((VariableElement) givenValue).asType();
            if (expected.getKind() == TypeKind.DECLARED) {
                isSubtype = types.isSubtype(types.erasure(found),
                        types.erasure(expected));
            } else {
                isSubtype = false;
            }
        } else {
            found = env.getElementUtils()
                    .getTypeElement(givenValue.getClass().getCanonicalName())
                    .asType();
            isSubtype = types.isSubtype(types.erasure(found),
                    types.erasure(expected));
        }

        if (!isSubtype) {
            if (found.toString().equals(expected.toString())) {
                SourceChecker.errorAbort("given value differs from expected, but same string representation; "
                        + "this is likely a bootclasspath/classpath issue; "
                        + "found: " + found);
            } else {
                SourceChecker.errorAbort("given value differs from expected; "
                        + "found: " + found + "; expected: " + expected);
            }
            return false; // dead code
        }

        return true;
    }

    private AnnotationValue createValue(final Object obj) {
        return new AnnotationValue() {
            final Object value = obj;

            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public String toString() {
                if (value instanceof String) {
                    return "\"" + value.toString() + "\"";
                } else if (value instanceof Character) {
                    return "\'" + value.toString() + "\'";
                } else if (value instanceof List<?>) {
                    StringBuilder sb = new StringBuilder();
                    List<?> list = (List<?>) value;
                    sb.append('{');
                    boolean isFirst = true;
                    for (Object o : list) {
                        if (!isFirst)
                            sb.append(", ");
                        isFirst = false;
                        sb.append(o.toString());
                    }
                    sb.append('}');
                    return sb.toString();
                } else if (value instanceof VariableElement) {
                    // for Enums
                    VariableElement var = (VariableElement) value;
                    String encl = var.getEnclosingElement().toString();
                    if (!encl.isEmpty()) {
                        encl = encl + '.';
                    }
                    return encl + var.toString();
                } else {
                    return value.toString();
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
                if (value instanceof AnnotationMirror)
                    return v.visitAnnotation((AnnotationMirror) value, p);
                else if (value instanceof List)
                    return v.visitArray((List<? extends AnnotationValue>) value, p);
                else if (value instanceof Boolean)
                    return v.visitBoolean((Boolean) value, p);
                else if (value instanceof Character)
                    return v.visitChar((Character) value, p);
                else if (value instanceof Double)
                    return v.visitDouble((Double) value, p);
                else if (value instanceof VariableElement)
                    return v.visitEnumConstant((VariableElement) value, p);
                else if (value instanceof Float)
                    return v.visitFloat((Float) value, p);
                else if (value instanceof Integer)
                    return v.visitInt((Integer) value, p);
                else if (value instanceof Long)
                    return v.visitLong((Long) value, p);
                else if (value instanceof Short)
                    return v.visitShort((Short) value, p);
                else if (value instanceof String)
                    return v.visitString((String) value, p);
                else if (value instanceof TypeMirror)
                    return v.visitType((TypeMirror) value, p);
                else {
                    assert false : " unknown type : " + v.getClass();
                    return v.visitUnknown(this, p);
                }
            }
        };
    }
}