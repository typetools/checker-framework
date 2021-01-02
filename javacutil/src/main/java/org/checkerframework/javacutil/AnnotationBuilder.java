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
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.plumelib.util.StringsPlume;

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

    /** The element utilities to use. */
    private final Elements elements;
    /** The type utilities to use. */
    private final Types types;

    /** The type element of the annotation. */
    private final TypeElement annotationElt;
    /** The type of the annotation. */
    private final DeclaredType annotationType;
    /** A mapping from element to AnnotationValue. */
    private final Map<ExecutableElement, AnnotationValue> elementValues;

    /**
     * Create a new AnnotationBuilder for the given annotation and environment (with no
     * elements/fields, but they can be added later).
     *
     * @param env the processing environment
     * @param anno the class of the annotation to build
     */
    @SuppressWarnings("nullness") // getCanonicalName expected to be non-null
    public AnnotationBuilder(ProcessingEnvironment env, Class<? extends Annotation> anno) {
        this(env, anno.getCanonicalName());
    }

    /**
     * Create a new AnnotationBuilder for the given annotation name (with no elements/fields, but
     * they can be added later).
     *
     * @param env the processing environment
     * @param name the canonical name of the annotation to build
     */
    public AnnotationBuilder(ProcessingEnvironment env, @FullyQualifiedName CharSequence name) {
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
     * Returns the type element of the annotation that is being built.
     *
     * @return the type element of the annotation that is being built
     */
    public TypeElement getAnnotationElt() {
        return annotationElt;
    }

    /**
     * Creates a mapping between element/field names and values.
     *
     * @param elementName the name of an element/field to initialize
     * @param elementValue the initial value for the element/field
     * @return a mappnig from the element name to the element value
     */
    public static Map<String, AnnotationValue> elementNamesValues(
            String elementName, Object elementValue) {
        return Collections.singletonMap(elementName, createValue(elementValue));
    }

    /**
     * Creates an {@link AnnotationMirror} that uses default values for elements/fields.
     * getElementValues on the result returns default values. If any element does not have a
     * default, this method throws an exception.
     *
     * <p>Most clients should use {@link #fromName}, using a Name created by the compiler. This
     * method is provided as a convenience to create an AnnotationMirror from scratch in a checker's
     * code.
     *
     * @param elements the element utilities to use
     * @param aClass the annotation class
     * @return an {@link AnnotationMirror} of the given type
     */
    public static AnnotationMirror fromClass(
            Elements elements, Class<? extends Annotation> aClass) {
        return fromClass(elements, aClass, Collections.emptyMap());
    }

    /**
     * Creates an {@link AnnotationMirror} given by a particular annotation class and a
     * name-to-value mapping for the elements/fields.
     *
     * <p>For other elements, getElementValues on the result returns default values. If any such
     * element does not have a default, this method throws an exception.
     *
     * <p>Most clients should use {@link #fromName}, using a Name created by the compiler. This
     * method is provided as a convenience to create an AnnotationMirror from scratch in a checker's
     * code.
     *
     * @param elements the element utilities to use
     * @param aClass the annotation class
     * @param elementNamesValues the values for the annotation's elements/fields
     * @return an {@link AnnotationMirror} of the given type
     */
    public static AnnotationMirror fromClass(
            Elements elements,
            Class<? extends Annotation> aClass,
            Map<String, AnnotationValue> elementNamesValues) {
        String name = aClass.getCanonicalName();
        assert name != null : "@AssumeAssertion(nullness): assumption";
        AnnotationMirror res = fromName(elements, name, elementNamesValues);
        if (res == null) {
            throw new UserError(
                    "AnnotationBuilder: error: fromClass can't load Class %s%n"
                            + "ensure the class is on the compilation classpath",
                    name);
        }
        return res;
    }

    /**
     * Creates an {@link AnnotationMirror} given by a particular fully-qualified name.
     * getElementValues on the result returns default values. If any element does not have a
     * default, this method throws an exception.
     *
     * <p>This method returns null if the annotation corresponding to the name could not be loaded.
     *
     * @param elements the element utilities to use
     * @param name the name of the annotation to create
     * @return an {@link AnnotationMirror} of type {@code} name or null if the annotation couldn't
     *     be loaded
     */
    public static @Nullable AnnotationMirror fromName(
            Elements elements, @FullyQualifiedName CharSequence name) {
        return fromName(elements, name, new HashMap<>());
    }

    /**
     * Creates an {@link AnnotationMirror} given by a particular fully-qualified name and
     * element/field values. If any element is not specified by the {@code elementValues} argument,
     * the default value is used. If any such element does not have a default, this method throws an
     * exception.
     *
     * <p>This method returns null if the annotation corresponding to the name could not be loaded.
     *
     * @param elements the element utilities to use
     * @param name the name of the annotation to create
     * @param elementNamesValues the values for the annotation's elements/fields
     * @return an {@link AnnotationMirror} of type {@code} name or null if the annotation couldn't
     *     be loaded
     */
    public static @Nullable AnnotationMirror fromName(
            Elements elements,
            @FullyQualifiedName CharSequence name,
            Map<String, AnnotationValue> elementNamesValues) {
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

        Map<ExecutableElement, AnnotationValue> elementValues = new LinkedHashMap<>();
        for (ExecutableElement annoElement :
                ElementFilter.methodsIn(annoElt.getEnclosedElements())) {
            AnnotationValue elementValue =
                    elementNamesValues.get(annoElement.getSimpleName().toString());
            if (elementValue == null) {
                AnnotationValue defaultValue = annoElement.getDefaultValue();
                if (defaultValue == null) {
                    throw new BugInCF(
                            "AnnotationBuilder.fromName: no value for element %s of %s",
                            annoElement, name);
                } else {
                    elementValue = defaultValue;
                }
            }
            elementValues.put(annoElement, elementValue);
        }

        AnnotationMirror result = new CheckerFrameworkAnnotationMirror(annoType, elementValues);
        return result;
    }

    /** Whether or not {@link #build()} has been called. */
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

    /**
     * Given a class, return the corresponding TypeMirror.
     *
     * @param clazz a class
     * @return the TypeMirror corresponding to the given class
     */
    private TypeMirror typeFromClass(Class<?> clazz) {
        return TypesUtils.typeFromClass(clazz, types, elements);
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

    /** Find the VariableElement for the given enum. */
    private VariableElement findEnumElement(Enum<?> value) {
        String enumClass = value.getDeclaringClass().getCanonicalName();
        assert enumClass != null : "@AssumeAssertion(nullness): assumption";
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

    /** @throws BugInCF if the type of {@code givenValue} is not the same as {@code expected} */
    private void checkSubtype(TypeMirror expected, Object givenValue) {
        if (expected.getKind().isPrimitive()) {
            expected = types.boxedClass((PrimitiveType) expected).asType();
        }

        if (expected.getKind() == TypeKind.DECLARED
                && TypesUtils.isClass(expected)
                && givenValue instanceof TypeMirror) {
            return;
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
            String name = givenValue.getClass().getCanonicalName();
            assert name != null : "@AssumeAssertion(nullness): assumption";
            found = elements.getTypeElement(name).asType();
            isSubtype = types.isSubtype(types.erasure(found), types.erasure(expected));
        }
        if (!isSubtype) {
            // Annotations in stub files sometimes are the same type, but Types#isSubtype fails
            // anyways.
            isSubtype = found.toString().equals(expected.toString());
        }

        if (!isSubtype) {
            throw new BugInCF(
                    "given value differs from expected; "
                            + "found: "
                            + found
                            + "; expected: "
                            + expected);
        }
    }

    /**
     * Create an AnnotationValue -- a value for an annotation element/field.
     *
     * @param obj the value to be stored in an annotation element/field
     * @return an AnnotationValue for the given Java value
     */
    private static AnnotationValue createValue(final Object obj) {
        return new CheckerFrameworkAnnotationValue(obj);
    }

    /** Implementation of AnnotationMirror used by the Checker Framework. */
    /* default visibility to allow access from within package. */
    static class CheckerFrameworkAnnotationMirror implements AnnotationMirror {
        /** The interned toString value. */
        private @Nullable @Interned String toStringVal;
        /** The annotation type. */
        private final DeclaredType annotationType;
        /** The element values. */
        private final Map<ExecutableElement, AnnotationValue> elementValues;
        /** The annotation name. */
        // default visibility to allow access from within package.
        final @Interned @CanonicalName String annotationName;

        /**
         * Create a CheckerFrameworkAnnotationMirror.
         *
         * @param annotationType the annotation type
         * @param elementValues the element values
         */
        @SuppressWarnings("signature:assignment.type.incompatible") // needs JDK annotations
        CheckerFrameworkAnnotationMirror(
                DeclaredType annotationType,
                Map<ExecutableElement, AnnotationValue> elementValues) {
            this.annotationType = annotationType;
            final TypeElement elm = (TypeElement) annotationType.asElement();
            this.annotationName = elm.getQualifiedName().toString().intern();
            this.elementValues = elementValues;
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

    /** Implementation of AnnotationValue used by the Checker Framework. */
    private static class CheckerFrameworkAnnotationValue implements AnnotationValue {
        /** The value. */
        private final Object value;
        /** The interned value of toString. */
        private @Nullable @Interned String toStringVal;

        /** Create an annotation value. */
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
            if (this.toStringVal != null) {
                return this.toStringVal;
            }
            String toStringVal;
            if (value instanceof String) {
                toStringVal = "\"" + value + "\"";
            } else if (value instanceof Character) {
                toStringVal = "\'" + value + "\'";
            } else if (value instanceof List<?>) {
                List<?> list = (List<?>) value;
                toStringVal = "{" + StringsPlume.join(", ", list) + "}";
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
            this.toStringVal = toStringVal.intern();
            return this.toStringVal;
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
        public boolean equals(@Nullable Object obj) {
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
