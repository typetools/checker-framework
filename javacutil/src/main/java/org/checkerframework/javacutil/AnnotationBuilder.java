package org.checkerframework.javacutil;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Builds an annotation mirror that may have some values.
 *
 * <p>Constructing an {@link AnnotationMirror} requires:
 *
 * <ol>
 *   <li>Constructing the builder with the desired annotation class
 *   <li>Setting each value individually using {@code setValue} methods
 *   <li>Calling {@link #build()} to get the annotation
 * </ol>
 *
 * Once an annotation is built, no further modification or calls to build can be made. Otherwise, a
 * {@link IllegalStateException} is thrown.
 *
 * <p>All setter methods throw {@link IllegalArgumentException} if the specified element is not
 * found, or if the given value is not a subtype of the expected type.
 *
 * <p>TODO: Doesn't type-check arrays yet
 */
public class AnnotationBuilder {

    private final Elements elements;
    private final Types types;

    private final TypeElement annotationElt;
    private final DeclaredType annotationType;
    private final Map<ExecutableElement, AnnotationValue> elementValues;

    /**
     * Caching for annotation creation. Each annotation has no values; that is, getElementValues
     * returns an empty map. This may be in conflict with the annotation's definition, which might
     * contain elements (annotation fields).
     */
    private static final Map<CharSequence, AnnotationMirror> annotationsFromNames =
            Collections.synchronizedMap(new HashMap<>());

    /**
     * Create a new AnnotationBuilder for the given annotation and environment (with no
     * elements/fields, but they can be added later).
     *
     * @param env the processing environment
     * @param anno the class of the annotation to build
     */
    public AnnotationBuilder(ProcessingEnvironment env, Class<? extends Annotation> anno) {
        this(env, anno.getCanonicalName());
    }

    /**
     * Create a new AnnotationBuilder for the given annotation name (with no elements/fields, but
     * they can be added later).
     *
     * @param env the processing environment
     * @param name the name of the annotation to build
     */
    public AnnotationBuilder(ProcessingEnvironment env, CharSequence name) {
        this.elements = env.getElementUtils();
        this.types = env.getTypeUtils();
        this.annotationElt = elements.getTypeElement(name);
        if (annotationElt == null) {
            throw new UserError("Could not find annotation: " + name + ". Is it on the classpath?");
        }
        assert annotationElt.getKind() == ElementKind.ANNOTATION_TYPE;
        this.annotationType = (DeclaredType) annotationElt.asType();
        this.elementValues = new LinkedHashMap<>();
    }

    /**
     * Create a new AnnotationBuilder that copies the given annotation, including its
     * elements/fields.
     *
     * @param env the processing environment
     * @param annotation the annotation to copy
     */
    public AnnotationBuilder(ProcessingEnvironment env, AnnotationMirror annotation) {
        this.elements = env.getElementUtils();
        this.types = env.getTypeUtils();

        this.annotationType = annotation.getAnnotationType();
        this.annotationElt = (TypeElement) annotationType.asElement();

        this.elementValues = new LinkedHashMap<>();
        // AnnotationValues are immutable so putAll should suffice
        this.elementValues.putAll(annotation.getElementValues());
    }

    /**
     * Creates an {@link AnnotationMirror} given by a particular annotation class. getElementValues
     * on the result returns an empty map. This may be in conflict with the annotation's definition,
     * which might contain elements (annotation fields). Use an AnnotationBuilder for annotations
     * that contain elements.
     *
     * <p>This method raises an user error if the annotation corresponding to the class could not be
     * loaded.
     *
     * <p>Clients can use {@link #fromName} and check the result for null manually, if the error
     * from this method is not desired. This method is provided as a convenience to create an
     * AnnotationMirror from scratch in a checker's code.
     *
     * @param elements the element utilities to use
     * @param aClass the annotation class
     * @return an {@link AnnotationMirror} of type given type
     */
    public static AnnotationMirror fromClass(
            Elements elements, Class<? extends Annotation> aClass) {
        AnnotationMirror res = fromName(elements, aClass.getCanonicalName());
        if (res == null) {
            throw new UserError(
                    "AnnotationBuilder: error: fromClass can't load Class %s%n"
                            + "ensure the class is on the compilation classpath",
                    aClass.getCanonicalName());
        }
        return res;
    }

    /**
     * Creates an {@link AnnotationMirror} given by a particular fully-qualified name.
     * getElementValues on the result returns an empty map. This may be in conflict with the
     * annotation's definition, which might contain elements (annotation fields). Use an
     * AnnotationBuilder for annotations that contain elements.
     *
     * <p>This method returns null if the annotation corresponding to the name could not be loaded.
     *
     * @param elements the element utilities to use
     * @param name the name of the annotation to create
     * @return an {@link AnnotationMirror} of type {@code} name or null if the annotation couldn't
     *     be loaded
     */
    public static @Nullable AnnotationMirror fromName(Elements elements, CharSequence name) {
        AnnotationMirror res = annotationsFromNames.get(name);
        if (res != null) {
            return res;
        }
        final TypeElement annoElt = elements.getTypeElement(name);
        if (annoElt == null) {
            return null;
        }
        if (annoElt.getKind() != ElementKind.ANNOTATION_TYPE) {
            throw new BugInCF(annoElt + " is not an annotation");
        }

        final DeclaredType annoType = (DeclaredType) annoElt.asType();
        if (annoType == null) {
            return null;
        }
        AnnotationMirror result =
                new CheckerFrameworkAnnotationMirror(annoType, Collections.emptyMap());
        annotationsFromNames.put(name, result);
        return result;
    }

    // TODO: hack to clear out static state.
    public static void clear() {
        annotationsFromNames.clear();
    }

    private boolean wasBuilt = false;

    private void assertNotBuilt() {
        if (wasBuilt) {
            throw new BugInCF("AnnotationBuilder: error: type was already built");
        }
    }

    public AnnotationMirror build() {
        assertNotBuilt();
        wasBuilt = true;
        return new CheckerFrameworkAnnotationMirror(annotationType, elementValues);
    }

    /**
     * Copies every element value from the given annotation. If an element in the given annotation
     * doesn't exist in the annotation to be built, an error is raised unless the element is
     * specified in {@code ignorableElements}.
     *
     * @param valueHolder the annotation that holds the values to be copied
     * @param ignorableElements the elements that can be safely dropped
     */
    public void copyElementValuesFromAnnotation(
            AnnotationMirror valueHolder, String... ignorableElements) {
        Set<String> ignorableElementsSet = new HashSet<>(Arrays.asList(ignorableElements));
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> eltValToCopy :
                valueHolder.getElementValues().entrySet()) {
            Name eltNameToCopy = eltValToCopy.getKey().getSimpleName();
            if (ignorableElementsSet.contains(eltNameToCopy.toString())) {
                continue;
            }
            elementValues.put(findElement(eltNameToCopy), eltValToCopy.getValue());
        }
        return;
    }

    /**
     * Copies the specified element values from the given annotation, using the specified renaming
     * map. Each value in the map must be an element name in the annotation being built. If an
     * element from the given annotation is not a key in the map, it is ignored.
     *
     * @param valueHolder the annotation that holds the values to be copied
     * @param elementNameRenaming a map from element names in {@code valueHolder} to element names
     *     of the annotation being built
     */
    public void copyRenameElementValuesFromAnnotation(
            AnnotationMirror valueHolder, Map<String, String> elementNameRenaming) {

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> eltValToCopy :
                valueHolder.getElementValues().entrySet()) {

            String sourceName = eltValToCopy.getKey().getSimpleName().toString();
            String targetName = elementNameRenaming.get(sourceName);
            if (targetName == null) {
                continue;
            }
            elementValues.put(findElement(targetName), eltValToCopy.getValue());
        }
    }

    /** Set the element/field with the given name, to the given value. */
    public AnnotationBuilder setValue(CharSequence elementName, AnnotationMirror value) {
        setValue(elementName, (Object) value);
        return this;
    }

    /** Set the element/field with the given name, to the given value. */
    public AnnotationBuilder setValue(CharSequence elementName, List<? extends Object> values) {
        assertNotBuilt();
        List<AnnotationValue> avalues = new ArrayList<>(values.size());
        ExecutableElement var = findElement(elementName);
        TypeMirror expectedType = var.getReturnType();
        if (expectedType.getKind() != TypeKind.ARRAY) {
            throw new BugInCF("value is an array while expected type is not");
        }
        expectedType = ((ArrayType) expectedType).getComponentType();

        for (Object v : values) {
            checkSubtype(expectedType, v);
            avalues.add(createValue(v));
        }
        AnnotationValue aval = createValue(avalues);
        elementValues.put(var, aval);
        return this;
    }

    /** Set the element/field with the given name, to the given value. */
    public AnnotationBuilder setValue(CharSequence elementName, Object[] values) {
        return setValue(elementName, Arrays.asList(values));
    }

    /** Set the element/field with the given name, to the given value. */
    public AnnotationBuilder setValue(CharSequence elementName, Boolean value) {
        return setValue(elementName, (Object) value);
    }

    /** Set the element/field with the given name, to the given value. */
    public AnnotationBuilder setValue(CharSequence elementName, Character value) {
        return setValue(elementName, (Object) value);
    }

    /** Set the element/field with the given name, to the given value. */
    public AnnotationBuilder setValue(CharSequence elementName, Double value) {
        return setValue(elementName, (Object) value);
    }

    /** Set the element/field with the given name, to the given value. */
    public AnnotationBuilder setValue(CharSequence elementName, Float value) {
        return setValue(elementName, (Object) value);
    }

    /** Set the element/field with the given name, to the given value. */
    public AnnotationBuilder setValue(CharSequence elementName, Integer value) {
        return setValue(elementName, (Object) value);
    }

    /** Set the element/field with the given name, to the given value. */
    public AnnotationBuilder setValue(CharSequence elementName, Long value) {
        return setValue(elementName, (Object) value);
    }

    /** Set the element/field with the given name, to the given value. */
    public AnnotationBuilder setValue(CharSequence elementName, Short value) {
        return setValue(elementName, (Object) value);
    }

    /** Set the element/field with the given name, to the given value. */
    public AnnotationBuilder setValue(CharSequence elementName, String value) {
        return setValue(elementName, (Object) value);
    }

    /**
     * Remove the element/field with the given name. Does not err if no such element/field is
     * present.
     */
    public AnnotationBuilder removeElement(CharSequence elementName) {
        assertNotBuilt();
        ExecutableElement var = findElement(elementName);
        elementValues.remove(var);
        return this;
    }

    private TypeMirror getErasedOrBoxedType(TypeMirror type) {
        // See com.sun.tools.javac.code.Attribute.Class.makeClassType()
        return type.getKind().isPrimitive()
                ? types.boxedClass((PrimitiveType) type).asType()
                : types.erasure(type);
    }

    public AnnotationBuilder setValue(CharSequence elementName, TypeMirror value) {
        assertNotBuilt();
        value = getErasedOrBoxedType(value);
        AnnotationValue val = createValue(value);
        ExecutableElement var = findElement(elementName);
        // Check subtyping
        if (!TypesUtils.isClass(var.getReturnType())) {
            throw new BugInCF("expected " + var.getReturnType());
        }

        elementValues.put(var, val);
        return this;
    }

    private TypeMirror typeFromClass(Class<?> clazz) {
        if (clazz == void.class) {
            return types.getNoType(TypeKind.VOID);
        } else if (clazz.isPrimitive()) {
            String primitiveName = clazz.getName().toUpperCase();
            TypeKind primitiveKind = TypeKind.valueOf(primitiveName);
            return types.getPrimitiveType(primitiveKind);
        } else if (clazz.isArray()) {
            TypeMirror componentType = typeFromClass(clazz.getComponentType());
            return types.getArrayType(componentType);
        } else {
            TypeElement element = elements.getTypeElement(clazz.getCanonicalName());
            if (element == null) {
                throw new BugInCF("Unrecognized class: " + clazz);
            }
            return element.asType();
        }
    }

    public AnnotationBuilder setValue(CharSequence elementName, Class<?> value) {
        TypeMirror type = typeFromClass(value);
        return setValue(elementName, getErasedOrBoxedType(type));
    }

    public AnnotationBuilder setValue(CharSequence elementName, Enum<?> value) {
        assertNotBuilt();
        VariableElement enumElt = findEnumElement(value);
        return setValue(elementName, enumElt);
    }

    public AnnotationBuilder setValue(CharSequence elementName, VariableElement value) {
        ExecutableElement var = findElement(elementName);
        if (var.getReturnType().getKind() != TypeKind.DECLARED) {
            throw new BugInCF("expected a non enum: " + var.getReturnType());
        }
        if (!((DeclaredType) var.getReturnType()).asElement().equals(value.getEnclosingElement())) {
            throw new BugInCF("expected a different type of enum: " + value.getEnclosingElement());
        }
        elementValues.put(var, createValue(value));
        return this;
    }

    // Keep this version synchronized with the VariableElement[] version below
    public AnnotationBuilder setValue(CharSequence elementName, Enum<?>[] values) {
        assertNotBuilt();

        if (values.length == 0) {
            setValue(elementName, Collections.emptyList());
            return this;
        }

        VariableElement enumElt = findEnumElement(values[0]);
        ExecutableElement var = findElement(elementName);

        TypeMirror expectedType = var.getReturnType();
        if (expectedType.getKind() != TypeKind.ARRAY) {
            throw new BugInCF("expected a non array: " + var.getReturnType());
        }

        expectedType = ((ArrayType) expectedType).getComponentType();
        if (expectedType.getKind() != TypeKind.DECLARED) {
            throw new BugInCF("expected a non enum component type: " + var.getReturnType());
        }
        if (!((DeclaredType) expectedType).asElement().equals(enumElt.getEnclosingElement())) {
            throw new BugInCF(
                    "expected a different type of enum: " + enumElt.getEnclosingElement());
        }

        List<AnnotationValue> res = new ArrayList<>(values.length);
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
    public AnnotationBuilder setValue(CharSequence elementName, VariableElement[] values) {
        assertNotBuilt();
        ExecutableElement var = findElement(elementName);

        TypeMirror expectedType = var.getReturnType();
        if (expectedType.getKind() != TypeKind.ARRAY) {
            throw new BugInCF("expected an array, but found: " + expectedType);
        }

        expectedType = ((ArrayType) expectedType).getComponentType();
        if (expectedType.getKind() != TypeKind.DECLARED) {
            throw new BugInCF(
                    "expected a declared component type, but found: "
                            + expectedType
                            + " kind: "
                            + expectedType.getKind());
        }
        if (!types.isSameType((DeclaredType) expectedType, values[0].asType())) {
            throw new BugInCF(
                    "expected a different declared component type: "
                            + expectedType
                            + " vs. "
                            + values[0]);
        }

        List<AnnotationValue> res = new ArrayList<>(values.length);
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
        TypeElement enumClassElt = elements.getTypeElement(enumClass);
        assert enumClassElt != null;
        for (Element enumElt : enumClassElt.getEnclosedElements()) {
            if (enumElt.getSimpleName().contentEquals(value.name())) {
                return (VariableElement) enumElt;
            }
        }
        throw new BugInCF("cannot be here");
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
        throw new BugInCF("Couldn't find " + key + " element in " + annotationElt);
    }

    // TODO: this method always returns true and no-one ever looks at the return
    // value.
    private boolean checkSubtype(TypeMirror expected, Object givenValue) {
        if (expected.getKind().isPrimitive()) {
            expected = types.boxedClass((PrimitiveType) expected).asType();
        }

        if (expected.getKind() == TypeKind.DECLARED
                && TypesUtils.isClass(expected)
                && givenValue instanceof TypeMirror) {
            return true;
        }

        TypeMirror found;
        boolean isSubtype;

        if (expected.getKind() == TypeKind.DECLARED
                && ((DeclaredType) expected).asElement().getKind() == ElementKind.ANNOTATION_TYPE
                && givenValue instanceof AnnotationMirror) {
            found = ((AnnotationMirror) givenValue).getAnnotationType();
            isSubtype =
                    ((DeclaredType) expected)
                            .asElement()
                            .equals(((DeclaredType) found).asElement());
        } else if (givenValue instanceof AnnotationMirror) {
            found = ((AnnotationMirror) givenValue).getAnnotationType();
            // TODO: why is this always failing???
            isSubtype = false;
        } else if (givenValue instanceof VariableElement) {
            found = ((VariableElement) givenValue).asType();
            if (expected.getKind() == TypeKind.DECLARED) {
                isSubtype = types.isSubtype(types.erasure(found), types.erasure(expected));
            } else {
                isSubtype = false;
            }
        } else {
            found = elements.getTypeElement(givenValue.getClass().getCanonicalName()).asType();
            isSubtype = types.isSubtype(types.erasure(found), types.erasure(expected));
        }

        if (!isSubtype) {
            if (types.isSameType(found, expected)) {
                throw new BugInCF(
                        "given value differs from expected, but same string representation; "
                                + "this is likely a bootclasspath/classpath issue; "
                                + "found: "
                                + found);
            } else {
                throw new BugInCF(
                        "given value differs from expected; "
                                + "found: "
                                + found
                                + "; expected: "
                                + expected);
            }
        }

        return true;
    }

    private AnnotationValue createValue(final Object obj) {
        return new CheckerFrameworkAnnotationValue(obj);
    }

    /** Implementation of AnnotationMirror used by the Checker Framework. */
    /* default visibility to allow access from within package. */
    static class CheckerFrameworkAnnotationMirror implements AnnotationMirror {

        private @Interned String toStringVal;
        private final DeclaredType annotationType;
        private final Map<ExecutableElement, AnnotationValue> elementValues;

        // default visibility to allow access from within package.
        final @Interned String annotationName;

        CheckerFrameworkAnnotationMirror(
                DeclaredType at, Map<ExecutableElement, AnnotationValue> ev) {
            this.annotationType = at;
            final TypeElement elm = (TypeElement) at.asElement();
            this.annotationName = elm.getQualifiedName().toString().intern();
            this.elementValues = ev;
        }

        @Override
        public DeclaredType getAnnotationType() {
            return annotationType;
        }

        @Override
        public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
            return Collections.unmodifiableMap(elementValues);
        }

        @SideEffectFree
        @Override
        public String toString() {
            if (toStringVal != null) {
                return toStringVal;
            }
            StringBuilder buf = new StringBuilder();
            buf.append("@");
            buf.append(annotationName);
            int len = elementValues.size();
            if (len > 0) {
                buf.append('(');
                boolean first = true;
                for (Map.Entry<ExecutableElement, AnnotationValue> pair :
                        elementValues.entrySet()) {
                    if (!first) {
                        buf.append(", ");
                    }
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
            toStringVal = buf.toString().intern();
            return toStringVal;

            // return "@" + annotationType + "(" + elementValues + ")";
        }
    }

    private static class CheckerFrameworkAnnotationValue implements AnnotationValue {
        private final Object value;
        private @Interned String toStringVal;

        CheckerFrameworkAnnotationValue(Object obj) {
            this.value = obj;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @SideEffectFree
        @Override
        public String toString() {
            if (toStringVal != null) {
                return toStringVal;
            }
            if (value instanceof String) {
                toStringVal = "\"" + value + "\"";
            } else if (value instanceof Character) {
                toStringVal = "\'" + value + "\'";
            } else if (value instanceof List<?>) {
                StringBuilder sb = new StringBuilder();
                List<?> list = (List<?>) value;
                sb.append('{');
                boolean isFirst = true;
                for (Object o : list) {
                    if (!isFirst) {
                        sb.append(", ");
                    }
                    isFirst = false;
                    sb.append(o.toString());
                }
                sb.append('}');
                toStringVal = sb.toString();
            } else if (value instanceof VariableElement) {
                // for Enums
                VariableElement var = (VariableElement) value;
                String encl = var.getEnclosingElement().toString();
                if (!encl.isEmpty()) {
                    encl = encl + '.';
                }
                toStringVal = encl + var;
            } else if (value instanceof TypeMirror && TypesUtils.isClassType((TypeMirror) value)) {
                toStringVal = value.toString() + ".class";
            } else {
                toStringVal = value.toString();
            }
            toStringVal = toStringVal.intern();
            return toStringVal;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            if (value instanceof AnnotationMirror) {
                return v.visitAnnotation((AnnotationMirror) value, p);
            } else if (value instanceof List) {
                return v.visitArray((List<? extends AnnotationValue>) value, p);
            } else if (value instanceof Boolean) {
                return v.visitBoolean((Boolean) value, p);
            } else if (value instanceof Character) {
                return v.visitChar((Character) value, p);
            } else if (value instanceof Double) {
                return v.visitDouble((Double) value, p);
            } else if (value instanceof VariableElement) {
                return v.visitEnumConstant((VariableElement) value, p);
            } else if (value instanceof Float) {
                return v.visitFloat((Float) value, p);
            } else if (value instanceof Integer) {
                return v.visitInt((Integer) value, p);
            } else if (value instanceof Long) {
                return v.visitLong((Long) value, p);
            } else if (value instanceof Short) {
                return v.visitShort((Short) value, p);
            } else if (value instanceof String) {
                return v.visitString((String) value, p);
            } else if (value instanceof TypeMirror) {
                return v.visitType((TypeMirror) value, p);
            } else {
                assert false : " unknown type : " + v.getClass();
                return v.visitUnknown(this, p);
            }
        }

        @Override
        public boolean equals(Object obj) {
            // System.out.printf("Calling CFAV.equals()%n");
            if (!(obj instanceof AnnotationValue)) {
                return false;
            }
            AnnotationValue other = (AnnotationValue) obj;
            return Objects.equals(this.getValue(), other.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.value);
        }
    }
}
