package org.checkerframework.javacutil;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacElements;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A utility class for working with annotations. */
public class AnnotationUtils {

    // Class cannot be instantiated.
    private AnnotationUtils() {
        throw new AssertionError("Class AnnotationUtils cannot be instantiated.");
    }

    // TODO: hack to clear out static state.
    public static void clear() {
        AnnotationBuilder.clear();
        annotationClassNames.clear();
    }

    // **********************************************************************
    // Factory Methods to create instances of AnnotationMirror
    // **********************************************************************

    private static final int ANNOTATION_CACHE_SIZE = 500;

    /** Maps classes representing AnnotationMirrors to their names. */
    private static final Map<Class<? extends Annotation>, String> annotationClassNames =
            Collections.synchronizedMap(CollectionUtils.createLRUCache(ANNOTATION_CACHE_SIZE));

    // **********************************************************************
    // Helper methods to handle annotations.  mainly workaround
    // AnnotationMirror.equals undesired property
    // (I think the undesired property is that it's reference equality.)
    // **********************************************************************

    /** @return the fully-qualified name of an annotation as a String */
    public static final String annotationName(AnnotationMirror annotation) {
        if (annotation instanceof AnnotationBuilder.CheckerFrameworkAnnotationMirror) {
            return ((AnnotationBuilder.CheckerFrameworkAnnotationMirror) annotation).annotationName;
        }
        final DeclaredType annoType = annotation.getAnnotationType();
        final TypeElement elm = (TypeElement) annoType.asElement();
        String name = elm.getQualifiedName().toString();
        return name;
    }

    /**
     * Checks if both annotations are the same.
     *
     * <p>Returns true iff both annotations are of the same type and have the same annotation
     * values. This behavior differs from {@code AnnotationMirror.equals(Object)}. The equals method
     * returns true iff both annotations are the same and annotate the same annotation target (e.g.
     * field, variable, etc).
     *
     * @return true iff a1 and a2 are the same annotation
     */
    public static boolean areSame(@Nullable AnnotationMirror a1, @Nullable AnnotationMirror a2) {
        if (a1 == a2) {
            return true;
        }

        if (!areSameIgnoringValues(a1, a2)) {
            return false;
        }

        // This commented implementation is less efficient.  It is also wrong:  it requires a
        // particular order for fields, and it distinguishes the long constants "33" and "33L".
        // Map<? extends ExecutableElement, ? extends AnnotationValue> elval1 =
        //         getElementValuesWithDefaults(a1);
        // Map<? extends ExecutableElement, ? extends AnnotationValue> elval2 =
        //         getElementValuesWithDefaults(a2);
        // return elval1.toString().equals(elval2.toString());

        return sameElementValues(a1, a2);
    }

    /**
     * @see #areSame(AnnotationMirror, AnnotationMirror)
     * @return true iff a1 and a2 have the same annotation type
     */
    public static boolean areSameIgnoringValues(
            @Nullable AnnotationMirror a1, @Nullable AnnotationMirror a2) {
        if (a1 == a2) {
            return true;
        }
        if (a1 == null || a2 == null) {
            return false;
        }

        return annotationName(a1).equals(annotationName(a2));
    }

    /**
     * Checks that the annotation {@code am} has the name {@code aname} (a fully-qualified type
     * name). Values are ignored.
     *
     * <p>(Use {@link #areSameByClass} instead of this method when possible. It is faster.)
     */
    public static boolean areSameByName(AnnotationMirror am, String aname) {
        return aname.equals(annotationName(am));
    }

    /**
     * Checks that the annotation {@code am} has the name of {@code annoClass}. Values are ignored.
     *
     * <p>(Use this method rather than {@link #areSameByName} when possible. This method is faster.)
     */
    public static boolean areSameByClass(
            AnnotationMirror am, Class<? extends Annotation> annoClass) {
        String canonicalName = annotationClassNames.get(annoClass);
        if (canonicalName == null) {
            // This method is faster than #areSameByName because of this cache.
            canonicalName = annoClass.getCanonicalName();
            annotationClassNames.put(annoClass, canonicalName);
        }
        return areSameByName(am, canonicalName);
    }

    /**
     * Checks that two collections contain the same annotations.
     *
     * @return true iff c1 and c2 contain the same annotations
     */
    public static boolean areSame(
            Collection<? extends AnnotationMirror> c1, Collection<? extends AnnotationMirror> c2) {
        if (c1.size() != c2.size()) {
            return false;
        }
        if (c1.size() == 1) {
            return areSame(c1.iterator().next(), c2.iterator().next());
        }

        Set<AnnotationMirror> s1 = createAnnotationSet();
        Set<AnnotationMirror> s2 = createAnnotationSet();
        s1.addAll(c1);
        s2.addAll(c2);

        // depend on the fact that Set is an ordered set.
        Iterator<AnnotationMirror> iter1 = s1.iterator();
        Iterator<AnnotationMirror> iter2 = s2.iterator();

        while (iter1.hasNext()) {
            AnnotationMirror anno1 = iter1.next();
            AnnotationMirror anno2 = iter2.next();
            if (!areSame(anno1, anno2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks that the collection contains the annotation. Using Collection.contains does not always
     * work, because it does not use areSame for comparison.
     *
     * @return true iff c contains anno, according to areSame
     */
    public static boolean containsSame(
            Collection<? extends AnnotationMirror> c, AnnotationMirror anno) {
        return getSame(c, anno) != null;
    }

    /**
     * Returns the AnnotationMirror in {@code c} that is the same annotation as {@code anno}.
     *
     * @return AnnotationMirror with the same class as {@code anno} iff c contains anno, according
     *     to areSame; otherwise, {@code null}
     */
    public static AnnotationMirror getSame(
            Collection<? extends AnnotationMirror> c, AnnotationMirror anno) {
        for (AnnotationMirror an : c) {
            if (AnnotationUtils.areSame(an, anno)) {
                return an;
            }
        }
        return null;
    }

    /**
     * Checks that the collection contains the annotation. Using Collection.contains does not always
     * work, because it does not use areSame for comparison.
     *
     * @return true iff c contains anno, according to areSameByClass
     */
    public static boolean containsSameByClass(
            Collection<? extends AnnotationMirror> c, Class<? extends Annotation> anno) {
        return getAnnotationByClass(c, anno) != null;
    }

    /**
     * Returns the AnnotationMirror in {@code c} that has the same class as {@code anno}.
     *
     * @return AnnotationMirror with the same class as {@code anno} iff c contains anno, according
     *     to areSameByClass; otherwise, {@code null}
     */
    public static AnnotationMirror getAnnotationByClass(
            Collection<? extends AnnotationMirror> c, Class<? extends Annotation> anno) {
        for (AnnotationMirror an : c) {
            if (AnnotationUtils.areSameByClass(an, anno)) {
                return an;
            }
        }
        return null;
    }

    /**
     * Checks that the collection contains the annotation. Using Collection.contains does not always
     * work, because it does not use areSame for comparison.
     *
     * @return true iff c contains anno, according to areSameByName
     */
    public static boolean containsSameByName(
            Collection<? extends AnnotationMirror> c, String anno) {
        return getAnnotationByName(c, anno) != null;
    }

    /**
     * Returns the AnnotationMirror in {@code c} that has the same name as {@code anno}.
     *
     * @return AnnotationMirror with the same name as {@code anno} iff c contains anno, according to
     *     areSameByName; otherwise, {@code null}
     */
    public static AnnotationMirror getAnnotationByName(
            Collection<? extends AnnotationMirror> c, String anno) {
        for (AnnotationMirror an : c) {
            if (AnnotationUtils.areSameByName(an, anno)) {
                return an;
            }
        }
        return null;
    }

    /**
     * Checks that the collection contains the annotation ignoring values. Using Collection.contains
     * does not always work, because it does not use areSameIgnoringValues for comparison.
     *
     * @return true iff c contains anno, according to areSameIgnoringValues
     */
    public static boolean containsSameIgnoringValues(
            Collection<? extends AnnotationMirror> c, AnnotationMirror anno) {
        return getSameIgnoringValues(c, anno) != null;
    }

    /**
     * Returns the AnnotationMirror in {@code c} that is the same annotation as {@code anno}
     * ignoring values.
     *
     * @return AnnotationMirror with the same class as {@code anno} iff c contains anno, according
     *     to areSameIgnoringValues; otherwise, {@code null}
     */
    public static AnnotationMirror getSameIgnoringValues(
            Collection<? extends AnnotationMirror> c, AnnotationMirror anno) {
        for (AnnotationMirror an : c) {
            if (AnnotationUtils.areSameIgnoringValues(an, anno)) {
                return an;
            }
        }
        return null;
    }

    private static final Comparator<AnnotationMirror> ANNOTATION_ORDERING =
            new Comparator<AnnotationMirror>() {
                @Override
                public int compare(AnnotationMirror a1, AnnotationMirror a2) {
                    // AnnotationMirror.toString() prints the elements of an annotation in the
                    // order in which they were written. So, use areSame to check for equality.
                    if (AnnotationUtils.areSame(a1, a2)) {
                        return 0;
                    }

                    String n1 = a1.toString();
                    String n2 = a2.toString();

                    // Because the AnnotationMirror.toString prints the annotation as it appears
                    // in source code, the order in which annotations of the same class are
                    // sorted may be confusing.  For example, it might order
                    // @IntRange(from=1, to=MAX) before @IntRange(to=MAX,from=0).
                    return n1.compareTo(n2);
                }
            };

    /**
     * provide ordering for {@link AnnotationMirror} based on their fully qualified name. The
     * ordering ignores annotation values when ordering.
     *
     * <p>The ordering is meant to be used as {@link TreeSet} or {@link TreeMap} ordering. A {@link
     * Set} should not contain two annotations that only differ in values.
     */
    public static Comparator<AnnotationMirror> annotationOrdering() {
        return ANNOTATION_ORDERING;
    }

    /**
     * Create a map suitable for storing {@link AnnotationMirror} as keys.
     *
     * <p>It can store one instance of {@link AnnotationMirror} of a given declared type, regardless
     * of the annotation element values.
     *
     * @param <V> the value of the map
     * @return a new map with {@link AnnotationMirror} as key
     */
    public static <V> Map<AnnotationMirror, V> createAnnotationMap() {
        return new TreeMap<>(annotationOrdering());
    }

    /**
     * Constructs a {@link Set} suitable for storing {@link AnnotationMirror}s.
     *
     * <p>It stores at most once instance of {@link AnnotationMirror} of a given type, regardless of
     * the annotation element values.
     *
     * @return a new set to store {@link AnnotationMirror} as element
     */
    public static Set<AnnotationMirror> createAnnotationSet() {
        return new TreeSet<>(annotationOrdering());
    }

    /** Returns true if the given annotation has a @Inherited meta-annotation. */
    public static boolean hasInheritedMeta(AnnotationMirror anno) {
        return anno.getAnnotationType().asElement().getAnnotation(Inherited.class) != null;
    }

    /**
     * @return the set of {@link ElementKind}s to which {@code target} applies, ignoring TYPE_USE
     */
    public static EnumSet<ElementKind> getElementKindsForTarget(@Nullable Target target) {
        if (target == null) {
            // A missing @Target implies that the annotation can be written everywhere.
            return EnumSet.allOf(ElementKind.class);
        }
        EnumSet<ElementKind> eleKinds = EnumSet.noneOf(ElementKind.class);
        for (ElementType elementType : target.value()) {
            eleKinds.addAll(getElementKindsForElementType(elementType));
        }
        return eleKinds;
    }

    /**
     * Returns the set of {@link ElementKind}s corresponding to {@code elementType}. If the element
     * type is TYPE_USE, then ElementKinds returned should be the same as those returned for TYPE
     * and TYPE_PARAMETER, but this method returns the empty set instead.
     *
     * @return the set of {@link ElementKind}s corresponding to {@code elementType}
     */
    public static EnumSet<ElementKind> getElementKindsForElementType(ElementType elementType) {
        switch (elementType) {
            case TYPE:
                return EnumSet.of(
                        ElementKind.CLASS,
                        ElementKind.INTERFACE,
                        ElementKind.ANNOTATION_TYPE,
                        ElementKind.ENUM);
            case FIELD:
                return EnumSet.of(ElementKind.FIELD, ElementKind.ENUM_CONSTANT);
            case METHOD:
                return EnumSet.of(ElementKind.METHOD);
            case PARAMETER:
                return EnumSet.of(ElementKind.PARAMETER);
            case CONSTRUCTOR:
                return EnumSet.of(ElementKind.CONSTRUCTOR);
            case LOCAL_VARIABLE:
                return EnumSet.of(
                        ElementKind.LOCAL_VARIABLE,
                        ElementKind.RESOURCE_VARIABLE,
                        ElementKind.EXCEPTION_PARAMETER);
            case ANNOTATION_TYPE:
                return EnumSet.of(ElementKind.ANNOTATION_TYPE);
            case PACKAGE:
                return EnumSet.of(ElementKind.PACKAGE);
            case TYPE_PARAMETER:
                return EnumSet.of(ElementKind.TYPE_PARAMETER);
            case TYPE_USE:
                return EnumSet.noneOf(ElementKind.class);
            default:
                throw new BugInCF("Unrecognized ElementType: " + elementType);
        }
    }

    // **********************************************************************
    // Extractors for annotation values
    // **********************************************************************

    /**
     * Returns the values of an annotation's attributes, including defaults. The method with the
     * same name in JavacElements cannot be used directly, because it includes a cast to
     * Attribute.Compound, which doesn't hold for annotations generated by the Checker Framework.
     *
     * @see AnnotationMirror#getElementValues()
     * @see JavacElements#getElementValuesWithDefaults(AnnotationMirror)
     * @param ad annotation to examine
     * @return the values of the annotation's elements, including defaults
     */
    public static Map<? extends ExecutableElement, ? extends AnnotationValue>
            getElementValuesWithDefaults(AnnotationMirror ad) {
        Map<ExecutableElement, AnnotationValue> valMap = new HashMap<>();
        if (ad.getElementValues() != null) {
            valMap.putAll(ad.getElementValues());
        }
        for (ExecutableElement meth :
                ElementFilter.methodsIn(ad.getAnnotationType().asElement().getEnclosedElements())) {
            AnnotationValue defaultValue = meth.getDefaultValue();
            if (defaultValue != null && !valMap.containsKey(meth)) {
                valMap.put(meth, defaultValue);
            }
        }
        return valMap;
    }

    /**
     * Returns true if the two annotations have the same elements (fields). The arguments {@code
     * am1} and {@code am2} must be the same type of annotation.
     */
    public static boolean sameElementValues(AnnotationMirror am1, AnnotationMirror am2) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> vals1 = am1.getElementValues();
        Map<? extends ExecutableElement, ? extends AnnotationValue> vals2 = am2.getElementValues();
        for (ExecutableElement meth :
                ElementFilter.methodsIn(
                        am1.getAnnotationType().asElement().getEnclosedElements())) {
            AnnotationValue aval1 = vals1.get(meth);
            AnnotationValue aval2 = vals2.get(meth);
            if (aval1 == null) {
                aval1 = meth.getDefaultValue();
            }
            if (aval2 == null) {
                aval2 = meth.getDefaultValue();
            }
            if (!sameAnnotationValue(aval1, aval2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true iff the two AnnotationValue objects are the same. Use this instead of
     * CheckerFrameworkAnnotationValue.equals, which wouldn't get called if the receiver is some
     * AnnotationValue other than CheckerFrameworkAnnotationValue.
     */
    public static boolean sameAnnotationValue(AnnotationValue av1, AnnotationValue av2) {
        if (av1 == av2) {
            return true;
        }
        if (av1 == null || av2 == null) {
            return false;
        }
        return sameAnnotationValueValue(av1.getValue(), av2.getValue());
    }

    /**
     * Return true if the two annotation values are the same. The arguments to this method are
     * values that are returned by {@code AnnotationValue.getValue()}.
     */
    private static boolean sameAnnotationValueValue(Object val1, Object val2) {
        if (val1 == val2) {
            return true;
        }

        // Can't use deepEquals() to compare val1 and val2, because they might have mismatched
        // AnnotationValue vs. CheckerFrameworkAnnotationValue, and AnnotationValue doesn't override
        // equals().  So, write my own version of deepEquals().
        if ((val1 instanceof List<?>) && (val2 instanceof List<?>)) {
            List<?> list1 = (List<?>) val1;
            List<?> list2 = (List<?>) val2;
            if (list1.size() != list2.size()) {
                return false;
            }
            // Don't compare setwise, because order can matter. These mean different things:
            //   @LTLengthOf(value={"a1","a2"}, offest={"0", "1"})
            //   @LTLengthOf(value={"a2","a1"}, offest={"0", "1"})
            for (int i = 0; i < list1.size(); i++) {
                if (!sameAnnotationValueValue(list1.get(i), list2.get(i))) {
                    return false;
                }
            }
            return true;
        } else if ((val1 instanceof AnnotationMirror) && (val2 instanceof AnnotationMirror)) {
            return areSame((AnnotationMirror) val1, (AnnotationMirror) val2);
        } else if ((val1 instanceof AnnotationValue) && (val2 instanceof AnnotationValue)) {
            // This case occurs because of the recursive call when comparing arrays of
            // annotation values.
            return sameAnnotationValue((AnnotationValue) val1, (AnnotationValue) val2);
        } else if ((val1 instanceof Type.ClassType) && (val2 instanceof Type.ClassType)) {
            // Type.ClassType does not override equals
            return TypesUtils.areSameDeclaredTypes((Type.ClassType) val1, (Type.ClassType) val2);
        } else {
            return Objects.equals(val1, val2);
        }
    }

    /**
     * Verify whether the attribute with the name {@code name} exists in the annotation {@code
     * anno}.
     *
     * @param anno the annotation to examine
     * @param name the name of the attribute
     * @return whether the attribute exists in anno
     */
    public static <T> boolean hasElementValue(AnnotationMirror anno, CharSequence name) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> valmap =
                anno.getElementValues();
        for (ExecutableElement elem : valmap.keySet()) {
            if (elem.getSimpleName().contentEquals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the attribute with the name {@code name} of the annotation {@code anno}. The result is
     * expected to have type {@code expectedType}.
     *
     * <p><em>Note 1</em>: The method does not work well for attributes of an array type (as it
     * would return a list of {@link AnnotationValue}s). Use {@code getElementValueArray} instead.
     *
     * <p><em>Note 2</em>: The method does not work for attributes of an enum type, as the
     * AnnotationValue is a VarSymbol and would be cast to the enum type, which doesn't work. Use
     * {@code getElementValueEnum} instead.
     *
     * @param anno the annotation to disassemble
     * @param name the name of the attribute to access
     * @param expectedType the expected type used to cast the return type
     * @param useDefaults whether to apply default values to the attribute
     * @return the value of the attribute with the given name
     */
    public static <T> T getElementValue(
            AnnotationMirror anno, CharSequence name, Class<T> expectedType, boolean useDefaults) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> valmap;
        if (useDefaults) {
            valmap = getElementValuesWithDefaults(anno);
        } else {
            valmap = anno.getElementValues();
        }
        for (ExecutableElement elem : valmap.keySet()) {
            if (elem.getSimpleName().contentEquals(name)) {
                AnnotationValue val = valmap.get(elem);
                return expectedType.cast(val.getValue());
            }
        }
        throw new BugInCF("No element with name \'" + name + "\' in annotation " + anno);
    }

    /** Version that is suitable for Enum elements. */
    public static <T extends Enum<T>> T getElementValueEnum(
            AnnotationMirror anno, CharSequence name, Class<T> t, boolean useDefaults) {
        VarSymbol vs = getElementValue(anno, name, VarSymbol.class, useDefaults);
        T value = Enum.valueOf(t, vs.getSimpleName().toString());
        return value;
    }

    /**
     * Get the attribute with the name {@code name} of the annotation {@code anno}, where the
     * attribute has an array type. One element of the result is expected to have type {@code
     * expectedType}.
     *
     * <p>Parameter useDefaults is used to determine whether default values should be used for
     * annotation values. Finding defaults requires more computation, so should be false when no
     * defaulting is needed.
     *
     * @param anno the annotation to disassemble
     * @param name the name of the attribute to access
     * @param expectedType the expected type used to cast the return type
     * @param useDefaults whether to apply default values to the attribute
     * @return the value of the attribute with the given name
     */
    public static <T> List<T> getElementValueArray(
            AnnotationMirror anno, CharSequence name, Class<T> expectedType, boolean useDefaults) {
        @SuppressWarnings("unchecked")
        List<AnnotationValue> la = getElementValue(anno, name, List.class, useDefaults);
        List<T> result = new ArrayList<>(la.size());
        for (AnnotationValue a : la) {
            result.add(expectedType.cast(a.getValue()));
        }
        return result;
    }

    /**
     * Get the attribute with the name {@code name} of the annotation {@code anno}, or the default
     * value if no attribute is present explicitly, where the attribute has an array type and the
     * elements are {@code Enum}s. One element of the result is expected to have type {@code
     * expectedType}.
     */
    public static <T extends Enum<T>> List<T> getElementValueEnumArray(
            AnnotationMirror anno, CharSequence name, Class<T> t, boolean useDefaults) {
        @SuppressWarnings("unchecked")
        List<AnnotationValue> la = getElementValue(anno, name, List.class, useDefaults);
        List<T> result = new ArrayList<>(la.size());
        for (AnnotationValue a : la) {
            T value = Enum.valueOf(t, a.getValue().toString());
            result.add(value);
        }
        return result;
    }

    /**
     * Get the Name of the class that is referenced by attribute {@code name}.
     *
     * <p>This is a convenience method for the most common use-case. Like getElementValue(anno,
     * name, ClassType.class).getQualifiedName(), but this method ensures consistent use of the
     * qualified name.
     */
    public static Name getElementValueClassName(
            AnnotationMirror anno, CharSequence name, boolean useDefaults) {
        Type.ClassType ct = getElementValue(anno, name, Type.ClassType.class, useDefaults);
        // TODO:  Is it a problem that this returns the type parameters too?  Should I cut them off?
        return ct.asElement().getQualifiedName();
    }

    /** Get the list of Names of the classes that are referenced by attribute {@code name}. */
    public static List<Name> getElementValueClassNames(
            AnnotationMirror anno, CharSequence name, boolean useDefaults) {
        List<Type.ClassType> la =
                getElementValueArray(anno, name, Type.ClassType.class, useDefaults);
        List<Name> names = new ArrayList<>();
        for (Type.ClassType classType : la) {
            names.add(classType.asElement().getQualifiedName());
        }
        return names;
    }

    /**
     * Get the Class that is referenced by attribute {@code name}. This method uses Class.forName to
     * load the class. It returns null if the class wasn't found.
     */
    public static Class<?> getElementValueClass(
            AnnotationMirror anno, CharSequence name, boolean useDefaults) {
        Name cn = getElementValueClassName(anno, name, useDefaults);
        try {
            ClassLoader classLoader = InternalUtils.getClassLoaderForClass(AnnotationUtils.class);
            Class<?> cls = Class.forName(cn.toString(), true, classLoader);
            return cls;
        } catch (ClassNotFoundException e) {
            String msg =
                    String.format(
                            "Could not load class '%s' for field '%s' in annotation %s",
                            cn, name, anno);
            throw new BugInCF(msg, e);
        }
    }

    /**
     * See checkers.types.QualifierHierarchy#updateMappingToMutableSet(QualifierHierarchy, Map,
     * Object, AnnotationMirror) (Not linked because it is in an independent project.
     */
    public static <T> void updateMappingToImmutableSet(
            Map<T, Set<AnnotationMirror>> map, T key, Set<AnnotationMirror> newQual) {

        Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
        // TODO: if T is also an AnnotationMirror, should we use areSame?
        if (!map.containsKey(key)) {
            result.addAll(newQual);
        } else {
            result.addAll(map.get(key));
            result.addAll(newQual);
        }
        map.put(key, Collections.unmodifiableSet(result));
    }

    /**
     * Returns the annotations explicitly written on a constructor result. Callers should check that
     * {@code constructorDeclaration} is in fact a declaration of a constructor.
     *
     * @param constructorDeclaration declaration tree of constructor
     * @return set of annotations explicit on the resulting type of the constructor
     */
    public static Set<AnnotationMirror> getExplicitAnnotationsOnConstructorResult(
            MethodTree constructorDeclaration) {
        Set<AnnotationMirror> annotationSet = AnnotationUtils.createAnnotationSet();
        ModifiersTree modifiersTree = constructorDeclaration.getModifiers();
        if (modifiersTree != null) {
            List<? extends AnnotationTree> annotationTrees = modifiersTree.getAnnotations();
            annotationSet.addAll(TreeUtils.annotationsFromTypeAnnotationTrees(annotationTrees));
        }
        return annotationSet;
    }
}
