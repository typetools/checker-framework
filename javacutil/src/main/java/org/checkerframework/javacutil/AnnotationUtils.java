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
import java.util.NavigableSet;
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
import org.checkerframework.checker.interning.qual.CompareToMethod;
import org.checkerframework.checker.interning.qual.EqualsMethod;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.javacutil.AnnotationBuilder.CheckerFrameworkAnnotationMirror;

/** A utility class for working with annotations. */
public class AnnotationUtils {

    // Class cannot be instantiated.
    private AnnotationUtils() {
        throw new AssertionError("Class AnnotationUtils cannot be instantiated.");
    }

    // **********************************************************************
    // Helper methods to handle annotations.  mainly workaround
    // AnnotationMirror.equals undesired property
    // (I think the undesired property is that it's reference equality.)
    // **********************************************************************

    /**
     * Returns the fully-qualified name of an annotation as a String.
     *
     * @param annotation the annotation whose name to return
     * @return the fully-qualified name of an annotation as a String
     */
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
     * Returns true iff both annotations are of the same type and have the same annotation values.
     *
     * <p>This behavior differs from {@code AnnotationMirror.equals(Object)}. The equals method
     * returns true iff both annotations are the same and annotate the same annotation target (e.g.
     * field, variable, etc) -- that is, if its arguments are the same annotation instance.
     *
     * @param a1 the first AnnotationMirror to compare
     * @param a2 the second AnnotationMirror to compare
     * @return true iff a1 and a2 are the same annotation
     */
    @EqualsMethod
    public static boolean areSame(AnnotationMirror a1, AnnotationMirror a2) {
        if (a1 == a2) {
            return true;
        }

        if (!areSameByName(a1, a2)) {
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
     * Return true iff a1 and a2 have the same annotation type.
     *
     * @param a1 the first AnnotationMirror to compare
     * @param a2 the second AnnotationMirror to compare
     * @return true iff a1 and a2 have the same annotation name
     * @see #areSame(AnnotationMirror, AnnotationMirror)
     */
    @EqualsMethod
    public static boolean areSameByName(AnnotationMirror a1, AnnotationMirror a2) {
        if (a1 == a2) {
            return true;
        }
        if (a1 == null) {
            throw new BugInCF("Unexpected null first argument to areSameByName");
        }
        if (a2 == null) {
            throw new BugInCF("Unexpected null second argument to areSameByName");
        }

        if (a1 instanceof CheckerFrameworkAnnotationMirror
                && a2 instanceof CheckerFrameworkAnnotationMirror) {
            return ((CheckerFrameworkAnnotationMirror) a1).annotationName
                    == ((CheckerFrameworkAnnotationMirror) a2).annotationName;
        }

        return annotationName(a1).equals(annotationName(a2));
    }

    /**
     * Checks that the annotation {@code am} has the name {@code aname} (a fully-qualified type
     * name). Values are ignored.
     *
     * @param am the AnnotationMirror whose name to compare
     * @param aname the string to compare
     * @return true if aname is the name of am
     */
    public static boolean areSameByName(AnnotationMirror am, String aname) {
        return aname.equals(annotationName(am));
    }

    /**
     * Checks that the annotation {@code am} has the name of {@code annoClass}. Values are ignored.
     *
     * @param am the AnnotationMirror whose class to compare
     * @param annoClass the class to compare
     * @return true if annoclass is the class of am
     */
    public static boolean areSameByClass(
            AnnotationMirror am, Class<? extends Annotation> annoClass) {
        String canonicalName = annoClass.getCanonicalName();
        assert canonicalName != null : "@AssumeAssertion(nullness): assumption";
        return areSameByName(am, canonicalName);
    }

    /**
     * Checks that two collections contain the same annotations.
     *
     * @param c1 the first collection to compare
     * @param c2 the second collection to compare
     * @return true iff c1 and c2 contain the same annotations, according to {@link
     *     #areSame(AnnotationMirror, AnnotationMirror)}
     */
    public static boolean areSame(
            Collection<? extends AnnotationMirror> c1, Collection<? extends AnnotationMirror> c2) {
        if (c1.size() != c2.size()) {
            return false;
        }
        if (c1.size() == 1) {
            return areSame(c1.iterator().next(), c2.iterator().next());
        }

        // while loop depends on SortedSet implementation.
        NavigableSet<AnnotationMirror> s1 = createAnnotationSet();
        NavigableSet<AnnotationMirror> s2 = createAnnotationSet();
        s1.addAll(c1);
        s2.addAll(c2);
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
     * @param c a collection of AnnotationMirrors
     * @param anno the AnnotationMirror to search for in c
     * @return true iff c contains anno, according to areSame
     */
    public static boolean containsSame(
            Collection<? extends AnnotationMirror> c, AnnotationMirror anno) {
        return getSame(c, anno) != null;
    }

    /**
     * Returns the AnnotationMirror in {@code c} that is the same annotation as {@code anno}.
     *
     * @param c a collection of AnnotationMirrors
     * @param anno the AnnotationMirror to search for in c
     * @return AnnotationMirror with the same class as {@code anno} iff c contains anno, according
     *     to areSame; otherwise, {@code null}
     */
    public static @Nullable AnnotationMirror getSame(
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
     * @param c a collection of AnnotationMirrors
     * @param anno the annotation class to search for in c
     * @return true iff c contains anno, according to areSameByClass
     */
    public static boolean containsSameByClass(
            Collection<? extends AnnotationMirror> c, Class<? extends Annotation> anno) {
        return getAnnotationByClass(c, anno) != null;
    }

    /**
     * Returns the AnnotationMirror in {@code c} that has the same class as {@code anno}.
     *
     * @param c a collection of AnnotationMirrors
     * @param anno the class to search for in c
     * @return AnnotationMirror with the same class as {@code anno} iff c contains anno, according
     *     to areSameByClass; otherwise, {@code null}
     */
    public static @Nullable AnnotationMirror getAnnotationByClass(
            Collection<? extends AnnotationMirror> c, Class<? extends Annotation> anno) {
        for (AnnotationMirror an : c) {
            if (AnnotationUtils.areSameByClass(an, anno)) {
                return an;
            }
        }
        return null;
    }

    /**
     * Checks that the collection contains an annotation of the given name. Differs from using
     * Collection.contains, which does not use areSameByName for comparison.
     *
     * @param c a collection of AnnotationMirrors
     * @param anno the name to search for in c
     * @return true iff c contains anno, according to areSameByName
     */
    public static boolean containsSameByName(
            Collection<? extends AnnotationMirror> c, String anno) {
        return getAnnotationByName(c, anno) != null;
    }

    /**
     * Returns the AnnotationMirror in {@code c} that has the same name as {@code anno}.
     *
     * @param c a collection of AnnotationMirrors
     * @param anno the name to search for in c
     * @return AnnotationMirror with the same name as {@code anno} iff c contains anno, according to
     *     areSameByName; otherwise, {@code null}
     */
    public static @Nullable AnnotationMirror getAnnotationByName(
            Collection<? extends AnnotationMirror> c, String anno) {
        for (AnnotationMirror an : c) {
            if (AnnotationUtils.areSameByName(an, anno)) {
                return an;
            }
        }
        return null;
    }

    /**
     * Checks that the collection contains an annotation of the given name. Differs from using
     * Collection.contains, which does not use areSameByName for comparison.
     *
     * @param c a collection of AnnotationMirrors
     * @param anno the annotation whose name to search for in c
     * @return true iff c contains anno, according to areSameByName
     */
    public static boolean containsSameByName(
            Collection<? extends AnnotationMirror> c, AnnotationMirror anno) {
        return getSameByName(c, anno) != null;
    }

    /**
     * Returns the AnnotationMirror in {@code c} that is the same annotation as {@code anno}
     * ignoring values.
     *
     * @param c a collection of AnnotationMirrors
     * @param anno the annotation whose name to search for in c
     * @return AnnotationMirror with the same class as {@code anno} iff c contains anno, according
     *     to areSameByName; otherwise, {@code null}
     */
    public static @Nullable AnnotationMirror getSameByName(
            Collection<? extends AnnotationMirror> c, AnnotationMirror anno) {
        for (AnnotationMirror an : c) {
            if (AnnotationUtils.areSameByName(an, anno)) {
                return an;
            }
        }
        return null;
    }

    /**
     * Provide ordering for {@link AnnotationMirror}s. AnnotationMirrors are first compared by their
     * fully-qualified names, then by their element values in order of the name of the element.
     *
     * @param a1 the first annotation
     * @param a2 the second annotation
     * @return an ordering over AnnotationMirrors based on their name and values
     */
    public static int compareAnnotationMirrors(AnnotationMirror a1, AnnotationMirror a2) {
        if (!AnnotationUtils.areSameByName(a1, a2)) {
            return annotationName(a1).compareTo(annotationName(a2));
        }

        // The annotations have the same name, but different values, so compare values.
        Map<? extends ExecutableElement, ? extends AnnotationValue> vals1 = a1.getElementValues();
        Map<? extends ExecutableElement, ? extends AnnotationValue> vals2 = a2.getElementValues();
        Set<ExecutableElement> sortedElements =
                new TreeSet<>(Comparator.comparing(ElementUtils::getSimpleName));
        sortedElements.addAll(
                ElementFilter.methodsIn(a1.getAnnotationType().asElement().getEnclosedElements()));

        for (ExecutableElement meth : sortedElements) {
            AnnotationValue aval1 = vals1.get(meth);
            AnnotationValue aval2 = vals2.get(meth);
            if (aval1 == null) {
                aval1 = meth.getDefaultValue();
            }
            if (aval2 == null) {
                aval2 = meth.getDefaultValue();
            }
            int result = compareAnnotationValue(aval1, aval2);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    /**
     * Return 0 iff the two AnnotationValue objects are the same.
     *
     * @param av1 the first AnnotationValue to compare
     * @param av2 the second AnnotationValue to compare
     * @return 0 if if the two annotation values are the same
     */
    @CompareToMethod
    private static int compareAnnotationValue(AnnotationValue av1, AnnotationValue av2) {
        if (av1 == av2) {
            return 0;
        } else if (av1 == null) {
            return -1;
        } else if (av2 == null) {
            return 1;
        }
        return compareAnnotationValueValue(av1.getValue(), av2.getValue());
    }

    /**
     * Compares two annotation values for order.
     *
     * @param val1 a value returned by {@code AnnotationValue.getValue()}
     * @param val2 a value returned by {@code AnnotationValue.getValue()}
     * @return a negative integer, zero, or a positive integer as the first annotation value is less
     *     than, equal to, or greater than the second annotation value
     */
    @CompareToMethod
    private static int compareAnnotationValueValue(@Nullable Object val1, @Nullable Object val2) {
        if (val1 == val2) {
            return 0;
        } else if (val1 == null) {
            return -1;
        } else if (val2 == null) {
            return 1;
        }
        // Can't use deepEquals() to compare val1 and val2, because they might have mismatched
        // AnnotationValue vs. CheckerFrameworkAnnotationValue, and AnnotationValue doesn't override
        // equals().  So, write my own version of deepEquals().
        if ((val1 instanceof List<?>) && (val2 instanceof List<?>)) {
            List<?> list1 = (List<?>) val1;
            List<?> list2 = (List<?>) val2;
            if (list1.size() != list2.size()) {
                return list1.size() - list2.size();
            }
            // Don't compare setwise, because order can matter. These mean different things:
            //   @LTLengthOf(value={"a1","a2"}, offest={"0", "1"})
            //   @LTLengthOf(value={"a2","a1"}, offest={"0", "1"})
            for (int i = 0; i < list1.size(); i++) {
                Object v1 = list1.get(i);
                Object v2 = list2.get(i);
                int result = compareAnnotationValueValue(v1, v2);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        } else if ((val1 instanceof AnnotationMirror) && (val2 instanceof AnnotationMirror)) {
            return compareAnnotationMirrors((AnnotationMirror) val1, (AnnotationMirror) val2);
        } else if ((val1 instanceof AnnotationValue) && (val2 instanceof AnnotationValue)) {
            // This case occurs because of the recursive call when comparing arrays of
            // annotation values.
            return compareAnnotationValue((AnnotationValue) val1, (AnnotationValue) val2);
        }

        if ((val1 instanceof Type.ClassType) && (val2 instanceof Type.ClassType)) {
            // Type.ClassType does not override equals
            if (TypesUtils.areSameDeclaredTypes((Type.ClassType) val1, (Type.ClassType) val2)) {
                return 0;
            }
        }
        if (Objects.equals(val1, val2)) {
            return 0;
        }
        int result = val1.toString().compareTo(val2.toString());
        if (result == 0) {
            result = -1;
        }
        return result;
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
        return new TreeMap<>(AnnotationUtils::compareAnnotationMirrors);
    }

    /**
     * Constructs a {@link Set} for storing {@link AnnotationMirror}s.
     *
     * <p>It stores at most once instance of {@link AnnotationMirror} of a given type, regardless of
     * the annotation element values.
     *
     * @return a sorted new set to store {@link AnnotationMirror} as element
     */
    public static NavigableSet<AnnotationMirror> createAnnotationSet() {
        return new TreeSet<>(AnnotationUtils::compareAnnotationMirrors);
    }

    /**
     * Returns true if the given annotation has a @Inherited meta-annotation.
     *
     * @param anno the annotation to check for an @Inherited meta-annotation
     * @return true if the given annotation has a @Inherited meta-annotation
     */
    public static boolean hasInheritedMeta(AnnotationMirror anno) {
        return anno.getAnnotationType().asElement().getAnnotation(Inherited.class) != null;
    }

    /**
     * Returns the set of {@link ElementKind}s to which {@code target} applies, ignoring TYPE_USE.
     *
     * @param target a location where an annotation can be written
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
     * @param elementType the elementType to find ElementKinds for
     * @return the set of {@link ElementKind}s corresponding to {@code elementType}
     */
    public static EnumSet<ElementKind> getElementKindsForElementType(ElementType elementType) {
        switch (elementType) {
            case TYPE:
                return EnumSet.copyOf(ElementUtils.classElementKinds());
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
                // TODO: Use MODULE enum constants directly instead of looking them up by name.
                // (Java 11)
                if (elementType.name().equals("MODULE")) {
                    return EnumSet.of(ElementKind.valueOf("MODULE"));
                }
                if (elementType.name().equals("RECORD_COMPONENT")) {
                    return EnumSet.of(ElementKind.valueOf("RECORD_COMPONENT"));
                }
                throw new BugInCF("Unrecognized ElementType: " + elementType);
        }
    }

    // **********************************************************************
    // Extractors for annotation values
    // **********************************************************************

    /**
     * Returns the values of an annotation's elements, including defaults. The method with the same
     * name in JavacElements cannot be used directly, because it includes a cast to
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
            if (defaultValue != null) {
                valMap.putIfAbsent(meth, defaultValue);
            }
        }
        return valMap;
    }

    /**
     * Returns true if the two annotations have the same elements (fields). The arguments {@code
     * am1} and {@code am2} must be the same type of annotation.
     *
     * @param am1 the first AnnotationMirror to compare
     * @param am2 the second AnnotationMirror to compare
     * @return true if if the two annotations have the same elements (fields)
     */
    @EqualsMethod
    public static boolean sameElementValues(AnnotationMirror am1, AnnotationMirror am2) {
        if (am1 == am2) {
            return true;
        }

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
     *
     * @param av1 the first AnnotationValue to compare
     * @param av2 the second AnnotationValue to compare
     * @return true if if the two annotation values are the same
     */
    public static boolean sameAnnotationValue(AnnotationValue av1, AnnotationValue av2) {
        return compareAnnotationValue(av1, av2) == 0;
    }

    /**
     * Verify whether the element with the name {@code elementName} exists in the annotation {@code
     * anno}.
     *
     * @param anno the annotation to examine
     * @param elementName the name of the element
     * @return whether the element exists in anno
     */
    public static boolean hasElementValue(AnnotationMirror anno, CharSequence elementName) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> valmap =
                anno.getElementValues();
        for (ExecutableElement elem : valmap.keySet()) {
            if (elem.getSimpleName().contentEquals(elementName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the element with the name {@code elementName} of the annotation {@code anno}. The result
     * is expected to have type {@code expectedType}.
     *
     * <p>For elements of array type, use {@code getElementValueArray} instead.
     *
     * <p>For elements of enum type, use {@code getElementValueEnum} instead.
     *
     * @param anno the annotation whose element to access
     * @param elementName the name of the element to access
     * @param expectedType the expected type of the element
     * @param <T> the class of the expected type
     * @param useDefaults whether to apply default values to the element
     * @return the value of the element with the given name
     */
    public static <T> T getElementValue(
            AnnotationMirror anno,
            CharSequence elementName,
            Class<T> expectedType,
            boolean useDefaults) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> valmap;
        if (useDefaults) {
            valmap = getElementValuesWithDefaults(anno);
        } else {
            valmap = anno.getElementValues();
        }
        for (ExecutableElement elem : valmap.keySet()) {
            if (elem.getSimpleName().contentEquals(elementName)) {
                AnnotationValue val = valmap.get(elem);
                return expectedType.cast(val.getValue());
            }
        }
        throw new NoSuchElementException(
                String.format(
                        "No element with name \'%s\' in annotation %s; useDefaults=%s, valmap.keySet()=%s",
                        elementName, anno, useDefaults, valmap.keySet()));
    }

    /** Differentiates NoSuchElementException from other BugInCF. */
    @SuppressWarnings("serial")
    private static class NoSuchElementException extends BugInCF {
        /**
         * Constructs a new NoSuchElementException.
         *
         * @param message the detail message
         */
        public NoSuchElementException(String message) {
            super(message);
        }
    }

    /**
     * Get the element with the name {@code elementName} of the annotation {@code anno}, or return
     * null if no such element exists.
     *
     * @param anno the annotation whose element to access
     * @param elementName the name of the element to access
     * @param expectedType the expected type of the element
     * @param <T> the class of the expected type
     * @param useDefaults whether to apply default values to the element
     * @return the value of the element with the given name, or null
     */
    public static <T> @Nullable T getElementValueOrNull(
            AnnotationMirror anno,
            CharSequence elementName,
            Class<T> expectedType,
            boolean useDefaults) {
        // This implementation permits getElementValue a more detailed error message than if
        // getElementValue called getElementValueOrNull and threw an error if the result was null.
        try {
            return getElementValue(anno, elementName, expectedType, useDefaults);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Get the element with the name {@code name} of the annotation {@code anno}. The result is an
     * enum of type {@code T}.
     *
     * @param anno the annotation to disassemble
     * @param elementName the name of the element to access
     * @param expectedType the expected type used to cast the return type, an enum
     * @param <T> the class of the expected type
     * @param useDefaults whether to apply default values to the element
     * @return the value of the element with the given name
     */
    public static <T extends Enum<T>> T getElementValueEnum(
            AnnotationMirror anno,
            CharSequence elementName,
            Class<T> expectedType,
            boolean useDefaults) {
        VarSymbol vs = getElementValue(anno, elementName, VarSymbol.class, useDefaults);
        T value = Enum.valueOf(expectedType, vs.getSimpleName().toString());
        return value;
    }

    /**
     * Get the element with the name {@code elementName} of the annotation {@code anno}, where the
     * element has an array type. One element of the result is expected to have type {@code
     * expectedType}.
     *
     * <p>Parameter useDefaults is used to determine whether default values should be used for
     * annotation values. Finding defaults requires more computation, so should be false when no
     * defaulting is needed.
     *
     * @param anno the annotation to disassemble
     * @param elementName the name of the element to access
     * @param expectedType the expected type used to cast the return type
     * @param <T> the class of the expected type
     * @param useDefaults whether to apply default values to the element
     * @return the value of the element with the given name; it is a new list, so it is safe for
     *     clients to side-effect
     */
    public static <T> List<T> getElementValueArray(
            AnnotationMirror anno,
            CharSequence elementName,
            Class<T> expectedType,
            boolean useDefaults) {
        @SuppressWarnings("unchecked")
        List<AnnotationValue> la = getElementValue(anno, elementName, List.class, useDefaults);
        List<T> result = new ArrayList<>(la.size());
        for (AnnotationValue a : la) {
            try {
                result.add(expectedType.cast(a.getValue()));
            } catch (Throwable t) {
                String err1 =
                        String.format(
                                "getElementValueArray(%n  anno=%s,%n  elementName=%s,%n  expectedType=%s,%n  useDefaults=%s)%n",
                                anno, elementName, expectedType, useDefaults);
                String err2 =
                        String.format(
                                "Error in cast:%n  expectedType=%s%n  a=%s [%s]%n  a.getValue()=%s [%s]",
                                expectedType,
                                a,
                                a.getClass(),
                                a.getValue(),
                                a.getValue().getClass());
                throw new BugInCF(err1 + "; " + err2, t);
            }
        }
        return result;
    }

    /**
     * Get the element with the name {@code elementName} of the annotation {@code anno}, or the
     * default value if no element is present explicitly, where the element has an array type and
     * the elements are {@code Enum}s. One element of the result is expected to have type {@code
     * expectedType}.
     *
     * @param anno the annotation to disassemble
     * @param elementName the name of the element to access
     * @param expectedType the expected type used to cast the return type
     * @param <T> the class of the expected type
     * @param useDefaults whether to apply default values to the element
     * @return the value of the element with the given name
     */
    public static <T extends Enum<T>> List<T> getElementValueEnumArray(
            AnnotationMirror anno,
            CharSequence elementName,
            Class<T> expectedType,
            boolean useDefaults) {
        @SuppressWarnings("unchecked")
        List<AnnotationValue> la = getElementValue(anno, elementName, List.class, useDefaults);
        List<T> result = new ArrayList<>(la.size());
        for (AnnotationValue a : la) {
            T value = Enum.valueOf(expectedType, a.getValue().toString());
            result.add(value);
        }
        return result;
    }

    /**
     * Get the Name of the class that is referenced by element {@code elementName}.
     *
     * <p>This is a convenience method for the most common use-case. It is like {@code
     * getElementValue(anno, elementName, ClassType.class).getQualifiedName()}, but this method
     * ensures consistent use of the qualified name.
     *
     * @param anno the annotation to disassemble
     * @param elementName the name of the element to access
     * @param useDefaults whether to apply default values to the element
     * @return the name of the class that is referenced by element with the given name
     */
    public static @DotSeparatedIdentifiers Name getElementValueClassName(
            AnnotationMirror anno, CharSequence elementName, boolean useDefaults) {
        Type.ClassType ct = getElementValue(anno, elementName, Type.ClassType.class, useDefaults);
        // TODO:  Is it a problem that this returns the type parameters too?  Should I cut them off?
        return ct.asElement().getQualifiedName();
    }

    /**
     * Get the list of Names of the classes that are referenced by element {@code elementName}. It
     * fails if the class wasn't found. Like {@link #getElementValueClassNames}, but returns classes
     * rather than names.
     *
     * @param anno the annotation whose field to access
     * @param annoElement the element/field of {@code anno} whose content is a list of classes
     * @param useDefaults whether to apply default values to the element
     * @return the names of classes in {@code anno.annoElement}
     */
    public static List<Name> getElementValueClassNames(
            AnnotationMirror anno, CharSequence annoElement, boolean useDefaults) {
        List<Type.ClassType> la =
                getElementValueArray(anno, annoElement, Type.ClassType.class, useDefaults);
        List<Name> names = new ArrayList<>();
        for (Type.ClassType classType : la) {
            names.add(classType.asElement().getQualifiedName());
        }
        return names;
    }

    // The Javadoc doesn't use @link because framework is a different project than this one
    // (javacutil).
    /**
     * Update a map, to add {@code newQual} to the set that {@code key} maps to. The mapped-to
     * element is an unmodifiable set.
     *
     * <p>See
     * org.checkerframework.framework.type.QualifierHierarchy#updateMappingToMutableSet(QualifierHierarchy,
     * Map, Object, AnnotationMirror).
     *
     * @param map the map to update
     * @param key the key whose value to update
     * @param newQual the element to add to the given key's value
     * @param <T> the key type
     */
    public static <T extends @NonNull Object> void updateMappingToImmutableSet(
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

    /**
     * Returns true if anno is a declaration annotation. In other words, returns true if anno cannot
     * be written on uses of types.
     *
     * @param anno the AnnotationMirror
     * @return true if anno is a declaration annotation
     */
    public static boolean isDeclarationAnnotation(AnnotationMirror anno) {
        TypeElement elem = (TypeElement) anno.getAnnotationType().asElement();
        Target t = elem.getAnnotation(Target.class);
        if (t == null) {
            return true;
        }

        for (ElementType elementType : t.value()) {
            if (elementType == ElementType.TYPE_USE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the given array contains {@link ElementType#TYPE_USE}, false otherwise.
     *
     * @param elements an array of {@link ElementType} values
     * @param cls the annotation class being tested; used for diagnostic messages only
     * @return true iff the give array contains {@link ElementType#TYPE_USE}
     * @throws RuntimeException if the array contains both {@link ElementType#TYPE_USE} and
     *     something besides {@link ElementType#TYPE_PARAMETER}
     */
    public static boolean hasTypeQualifierElementTypes(ElementType[] elements, Class<?> cls) {
        // True if the array contains TYPE_USE
        boolean hasTypeUse = false;
        // Non-null if the array contains an element other than TYPE_USE or TYPE_PARAMETER
        ElementType otherElementType = null;

        for (ElementType element : elements) {
            if (element == ElementType.TYPE_USE) {
                hasTypeUse = true;
            } else if (element != ElementType.TYPE_PARAMETER) {
                otherElementType = element;
            }
            if (hasTypeUse && otherElementType != null) {
                throw new BugInCF(
                        "@Target meta-annotation should not contain both TYPE_USE and "
                                + otherElementType
                                + ", for annotation "
                                + cls.getName());
            }
        }

        return hasTypeUse;
    }
}
