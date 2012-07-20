package checkers.util;

import static javax.lang.model.util.ElementFilter.methodsIn;
import checkers.quals.*;
import checkers.nullness.quals.*;
import checkers.source.SourceChecker;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.code.Type;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

/**
 * A utility class for working with annotations.
 */
public class AnnotationUtils {

    private static AnnotationUtils instance;
    public static AnnotationUtils getInstance(ProcessingEnvironment env) {
        if (instance == null || instance.env != env)
            instance = new AnnotationUtils(env);

        return instance;
    }

    private final ProcessingEnvironment env;
    private final Elements elements;
    private final Trees trees;

    private AnnotationUtils(ProcessingEnvironment env) {
        this.env = env;
        this.elements = env.getElementUtils();
        /*@Nullable*/ Trees trees = Trees.instance(env);
        assert trees != null; /*nninvariant*/
        this.trees = trees;
    }

    // **********************************************************************
    // Factory Methods to create instances of AnnotationMirror
    // **********************************************************************

    /** Caching for annotation creation. */
    private static final Map<String, AnnotationMirror> annotationsFromNames
        = new HashMap<String, AnnotationMirror>();

    /**
     * Creates an {@link AnnotationMirror} given by a particular
     * fully-qualified name.
     *
     * @param name the name of the annotation to create
     * @return an {@link AnnotationMirror} of type {@code} name
     */
    public AnnotationMirror fromName(CharSequence name) {
        return fromName(name.toString());
    }

    /**
     * Creates an {@link AnnotationMirror} given by a particular
     * fully-qualified name.  getElementValues on the result returns an
     * empty map.
     *
     * @param name the name of the annotation to create
     * @return an {@link AnnotationMirror} of type {@code} name
     */
    public AnnotationMirror fromName(String name) {
        if (annotationsFromNames.containsKey(name))
            return annotationsFromNames.get(name);
        final DeclaredType annoType = typeFromName(name);
        if (annoType == null)
            return null;
        if (annoType.asElement().getKind() != ElementKind.ANNOTATION_TYPE) {
            SourceChecker.errorAbort(annoType + " is not an annotation");
            return null; // dead code
        }
        AnnotationMirror result = new AnnotationMirror() {
            String toString = "@" + annoType;

            @Override
            public DeclaredType getAnnotationType() {
                return annoType;
            }
            @Override
            public Map<? extends ExecutableElement, ? extends AnnotationValue>
                getElementValues() {
                return Collections.emptyMap();
            }
            @Override
            public String toString() {
                return toString;
            }
        };
        annotationsFromNames.put(name, result);
        return result;
    }

    /**
     * Creates an {@link AnnotationMirror} given by a particular annotation
     * class.
     *
     * @param clazz the annotation class
     * @return an {@link AnnotationMirror} of type given type
     */
    public AnnotationMirror fromClass(Class<? extends Annotation> clazz) {
        return fromName(clazz.getCanonicalName());
    }

    /**
     * A utility method that converts a {@link CharSequence} (usually a {@link
     * String}) into a {@link TypeMirror} named thereby.
     *
     * @param name the name of a type
     * @return the {@link TypeMirror} corresponding to that name
     */
    private DeclaredType typeFromName(CharSequence name) {

        /*@Nullable*/ TypeElement typeElt = elements.getTypeElement(name);
        if (typeElt == null)
            return null;

        return (DeclaredType)typeElt.asType();
    }

    // **********************************************************************
    // Query methods to find default locations for default annotations
    // **********************************************************************

    /**
     * Finds default annotations starting at the leaf of the given tree path by
     * inspecting enclosing variable, method, and class declarations for
     * {@link DefaultQualifier} annotations.
     *
     * @param path the tree path from which to start searching
     * @return a mapping from annotations (as {@link TypeElement}s) to the
     *         {@link DefaultLocation}s for those annotations
     *
     * @see #findDefaultLocations(Element)
     */
    public Map<TypeElement, Set<DefaultLocation>> findDefaultLocations(TreePath path) {

        // Attempt to find a starting search point. If the tree itself has an
        // element, start there. Otherwise, try the enclosing method and
        // enclosing class.
        /*@Nullable*/ Element typeElt = trees.getElement(path);

        // FIXME: eventually replace this with Scope
        if (typeElt == null) {
            /*@Nullable*/ MethodTree method = TreeUtils.enclosingMethod(path);
            if (method != null) typeElt = InternalUtils.symbol(method);
        }
        if (typeElt == null) {
            /*@Nullable*/ ClassTree cls = TreeUtils.enclosingClass(path);
            if (cls != null) typeElt = InternalUtils.symbol(cls);
        }

        if (typeElt == null) {
            SourceChecker.errorAbort("no element or enclosing element");
            return null; // dead code
        }

        return findDefaultLocations(typeElt);
    }

    /**
     * Finds default annotations starting at the given element, inspecting the
     * element and its enclosing method and class declarations for
     * {@link DefaultQualifier} annotations.
     *
     * @param elt the element from which to start searching
     * @return a mapping from annotations (as {@link TypeElement}s) to the
     *         {@link DefaultLocation}s for those annotations
     *
     * @see #findDefaultLocations(TreePath)
     */
    public Map<TypeElement, Set<DefaultLocation>> findDefaultLocations(Element elt) {

        /*@Nullable*/ TypeElement defaultElt =
            elements.getTypeElement("checkers.quals.DefaultQualifier");
        assert defaultElt != null : "couldn't get element for @DefaultQualifier";

        Map<TypeElement, Set</*@NonNull*/ DefaultLocation>> locations
            = new HashMap<TypeElement, Set<DefaultLocation>>();

        List<? extends AnnotationMirror> annos = elt.getAnnotationMirrors();
        for (AnnotationMirror a : annos) {

            if (!defaultElt.equals(a.getAnnotationType().asElement()))
                continue;

            /*@Nullable*/ String name = parseStringValue(a, "value");
            /*@Nullable*/ TypeElement aElt = elements.getTypeElement(name);
            if (aElt == null) {
                SourceChecker.errorAbort("illegal annotation name: " + name);
                return null; // dead code
            }

            /*@Nullable*/ Set<DefaultLocation> locs =
                parseEnumConstantArrayValue(a, "types",
                        DefaultLocation.class);

            if (!locations.containsKey(aElt))
                locations.put(aElt, new HashSet<DefaultLocation>());
            if (locs == null) continue; /*nnbug*/
            locations.get(aElt).addAll(locs);
        }

        /*@Nullable*/ Element encl = elt.getEnclosingElement();
        if (encl != null)
            locations.putAll(findDefaultLocations(encl));

        return Collections.</*@NonNull*/ TypeElement, /*@NonNull*/ Set<DefaultLocation>>unmodifiableMap(locations);
    }


    // **********************************************************************
    // Parsers for annotations values
    // **********************************************************************

    /**
     * Returns the values of an annotation's elements, including defaults.
     * TODO: Also see JavacElements.getElementValuesWithDefaults: that version is javac
     * specific, but could/should be replaced with this implementation.
     *
     * @see AnnotationMirror#getElementValues()
     * @param ad  annotation to examine
     * @return the values of the annotation's elements, including defaults
     */
    public static Map<? extends ExecutableElement, ? extends AnnotationValue>
    getElementValuesWithDefaults(AnnotationMirror ad) {
        if (ad == null)
            return Collections.emptyMap();
        Map<ExecutableElement, AnnotationValue> valMap
            = new HashMap<ExecutableElement, AnnotationValue>();
        if (ad.getElementValues() != null)
            valMap.putAll(ad.getElementValues());
        for (ExecutableElement meth :
            methodsIn(ad.getAnnotationType().asElement().getEnclosedElements())) {
            AnnotationValue defaultValue = meth.getDefaultValue();
            if (defaultValue != null && !valMap.containsKey(meth))
                valMap.put(meth, defaultValue);
        }
        return valMap;
    }

    /**
     * A generalized method for obtaining annotation values using any parser
     * that operates on any field.
     *
     * @param <R> the type of value to parse
     * @param parser the annotation value parser
     * @param ad the annotation for which a field will be parsed
     * @param fieldName the name of the annotation field for which a value will be returned
     * @return the value of {@code fieldName} in {@code ad} as determined by
     *         {@code parser}, with type {@code R}
     */
    private static <R> /*@Nullable*/ R parseAnnotationValue(AbstractAnnotationValueParser<R> parser,
            AnnotationMirror ad, String fieldName) {

        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
           getElementValuesWithDefaults(ad);

        for (Map.Entry<? extends ExecutableElement, ? extends
                AnnotationValue> entry : values.entrySet()) {

            ExecutableElement name = entry.getKey();
            AnnotationValue value = entry.getValue();

            {
                Name eltName = name.getSimpleName();
                if (!(fieldName.equals(eltName.toString())))
                    continue;
            }

            parser.visit(value);
            return parser.getValue();
        }

        return null;
    }

    /**
     * @param <R> the enum type
     * @param ad the annotation for which a value will be parsed
     * @param field the name of the field to parse
     * @param enumType the type of the enum
     * @return the enum constant value of the given field
     */
    public static <R extends Enum<R>> /*@Nullable*/ R parseEnumConstantValue(AnnotationMirror ad, String field, Class<R> enumType) {
        return parseAnnotationValue(new EnumConstantValueParser<R>(enumType), ad, field);
    }

    /**
     * @param <R> the enum type
     * @param ad the annotation for which a value will be parsed
     * @param field the name of the field to parse
     * @param enumType the type of the enum
     * @return the enum constant values of the given field
     */
    public static <R extends Enum<R>> /*@Nullable*/ Set<R> parseEnumConstantArrayValue(AnnotationMirror ad, String field, Class<R> enumType) {
        return parseAnnotationValue(new EnumConstantArrayValueParser<R>(enumType), ad, field);
    }

    /**
     * @param ad the annotation for which a value will be parsed
     * @param field the name of the field to parse
     * @return the String value of the given field
     */
    public static /*@Nullable*/ String parseStringValue(AnnotationMirror ad, String field) {
        return parseAnnotationValue(new StringValueParser(), ad, field);
    }

    /**
     * @param ad the annotation for which a value will be parsed
     * @param field the name of the field to parse
     * @return the String values of the given field
     */
    public static /*@Nullable*/ List<String> parseStringArrayValue(AnnotationMirror ad, String field) {
        return AnnotationUtils.<List</*@NonNull*/ String>>parseAnnotationValue(new StringArrayValueParser(), ad, field);
    }

    /**
     * @param ad the annotation for which a value will be parsed
     * @param field the name of the field to parse
     * @return the Class<?> value of the given field
     */
    public static /*@Nullable*/ Class<?> parseTypeValue(AnnotationMirror ad, String field) {
        return parseAnnotationValue(new TypeValueParser(), ad, field);
    }


    // **********************************************************************
    // Parsers for annotations values
    // **********************************************************************

    /**
     * A generic base class for parsers of annotation values.
     */
    private abstract static class AbstractAnnotationValueParser<A>
        extends SimpleAnnotationValueVisitor6<Void, Boolean> {

        /**
         * @return the value of an annotation field
         */
        public abstract A getValue();

        @Override
        public /*@Nullable*/ Void visitArray(List<? extends AnnotationValue> vals, Boolean p) {
            if (p != null && p)
                return null;

            for (AnnotationValue a : vals)
                visit(a, Boolean.TRUE);

            return null;
        }

    }

    /**
     * A utility class for parsing an enum-constant-valued annotation.
     */
    private static class EnumConstantValueParser<R extends Enum<R>>
        extends AbstractAnnotationValueParser<R> {

        private R value = null;
        private final Class<R> enumType;

        public EnumConstantValueParser(Class<R> enumType) {
            this.enumType = enumType;
        }

        @Override
        public /*@Nullable*/ Void visitEnumConstant(VariableElement c, Boolean p) {
            /*@Nullable*/ R r = Enum.<R>valueOf(enumType, (/*@NonNull*/ String)c.getSimpleName().toString());
            assert r != null; /*nninvariant*/
            value = r;
            return null;
        }

        @Override
        public R getValue() {
            assert value != null; /*nninvariant*/
            return value;
        }
    }

    /**
     * A utility class for parsing an enum-constant-array-valued annotation.
     */
    private static class EnumConstantArrayValueParser<R extends Enum<R>>
        extends AbstractAnnotationValueParser<Set<R>> {

        private final Set<R> values = new HashSet<R>();
        private final Class<R> enumType;

        public EnumConstantArrayValueParser(Class<R> enumType) {
            this.enumType = enumType;
        }

        @Override
        public /*@Nullable*/ Void visitEnumConstant(VariableElement c, Boolean p) {
            if (p == null || !p)
                return null;

            /*@Nullable*/ R r = Enum.<R>valueOf(enumType, (/*@NonNull*/ String)c.getSimpleName().toString());
            assert r != null; /*nninvariant*/
            values.add(r);
            return null;
        }

        @Override
        public Set<R> getValue() {
            return Collections.</*@NonNull*/ R>unmodifiableSet(values);
        }
    }

    /**
     * A utility class for parsing a String-valued annotation.
     */
    private static class StringValueParser
        extends AbstractAnnotationValueParser<String> {

        private /*@Nullable*/ String value = null;

        @Override
        public /*@Nullable*/ Void visitString(String s, Boolean p) {
            value = s;
            return null;
        }

        @Override
        public String getValue() {
            assert value != null; /*nninvariant*/
            return value;
        }
    }

    /**
     * A utility class for parsing a String[]-valued annotation.
     */
    private static class StringArrayValueParser
        extends AbstractAnnotationValueParser<List<String>> {

        private final List<String> values = new ArrayList<String>();

        @Override
        public /*@Nullable*/ Void visitString(String s, Boolean p) {
            if (p == null || !p)
                return null;

            values.add(s);

            return null;
        }

        @Override
        public List<String> getValue() {
            return Collections.</*@NonNull*/ String>unmodifiableList(values);
        }
    }

    /**
     * A utility class for parsing a Class-valued annotation.
     */
    private static class TypeValueParser
        extends AbstractAnnotationValueParser<Class<?>> {

        private /*@Nullable*/ Class<?> value = null;

        @Override
        public /*@Nullable*/ Void visitType(TypeMirror t, Boolean p) {
            try {
                value = Class.forName(t.toString());
            } catch (ClassNotFoundException e) {
                // TODO: handle the exception nicely
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public Class<?> getValue() {
            assert value != null; /*nninvariant*/
            return value;
        }
    }

    // **********************************************************************
    // Helper methods to handle annotations.  mainly workaround
    // AnnotationMirror.equals undesired property
    // (I think the undesired property is that it's reference equality.)
    // **********************************************************************

    /**
     * @return the fully-qualified name of an annotation as a String
     */
    public static final /*@Nullable*/ Name annotationName(/*@Nullable*/ AnnotationMirror annotation) {
        if (annotation == null) return null;
        final DeclaredType annoType = annotation.getAnnotationType();
        final TypeElement elm = (TypeElement) annoType.asElement();
        return elm.getQualifiedName();
    }

    /**
     * Checks if both annotations are the same.
     *
     * Returns true iff both annotations are of the same type and have the
     * same annotation values.  This behavior defers from
     * {@code AnnotationMirror.equals(Object)}.  The equals method returns
     * true iff both annotations are the same and annotate the same annotation
     * target (e.g. field, variable, etc).
     *
     * @return true iff a1 and a2 are the same annotation
     */
    public static boolean areSame(/*@Nullable*/ AnnotationMirror a1, /*@Nullable*/ AnnotationMirror a2) {
        if (a1 != null && a2 != null) {
            if (!annotationName(a1).equals(annotationName(a2))) {
                return false;
            }

            Map<? extends ExecutableElement, ? extends AnnotationValue> elval1 = getElementValuesWithDefaults(a1);
            Map<? extends ExecutableElement, ? extends AnnotationValue> elval2 = getElementValuesWithDefaults(a2);

            return elval1.toString().equals(elval2.toString());
        }

        // only true, iff both are null
        return a1 == a2;
    }

    /**
     * @see #areSame(AnnotationMirror, AnnotationMirror)
     * @return true iff a1 and a2 have the same annotation type
     */
    public static boolean areSameIgnoringValues(AnnotationMirror a1, AnnotationMirror a2) {
        if (a1 != null && a2 != null)
            return annotationName(a1).equals(annotationName(a2));
        return a1 == a2;
    }

    /**
     * Checks that two collections contain the same annotations.
     *
     * @return true iff c1 and c2 contain the same annotations
     */
    public static boolean areSame(Collection<AnnotationMirror> c1, Collection<AnnotationMirror> c2) {
        if (c1.size() != c2.size())
            return false;
        if (c1.size() == 1)
            return areSame(c1.iterator().next(), c2.iterator().next());

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
            if (!areSame(anno1, anno2))
                return false;
        }
        return true;
    }

    /**
     * Checks that the collection contains the annotation.
     * Using Collection.contains does not always work, because it
     * does not use areSame for comparison.
     *
     * @return true iff c contains anno, according to areSame.
     */
    public static boolean containsSame(Collection<AnnotationMirror> c, AnnotationMirror anno) {
        for(AnnotationMirror an : c) {
            if(AnnotationUtils.areSame(an, anno)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks that the collection contains the annotation ignoring values.
     * Using Collection.contains does not always work, because it
     * does not use areSameIgnoringValues for comparison.
     *
     * @return true iff c contains anno, according to areSameIgnoringValues.
     */
    public static boolean containsSameIgnoringValues(Collection<AnnotationMirror> c, AnnotationMirror anno) {
        for(AnnotationMirror an : c) {
            if(AnnotationUtils.areSameIgnoringValues(an, anno)) {
                return true;
            }
        }
        return false;
    }

    private static final Comparator<AnnotationMirror> ANNOTATION_ORDERING
    = new Comparator<AnnotationMirror>() {
        @Override
        public int compare(AnnotationMirror a1, AnnotationMirror a2) {
            if (a1 == null || a2 == null) {
                // TODO: I would really like to just do this:
                // throw new SourceChecker.CheckerError("AnnotationUtils.ANNOTATION_ORDERING: found null AnnotationMirror!");
                // However, things break then :-(

                if (a1 == a2)
                    return 0;
                else if (a1 == null)
                    return -1;
                else if (a2 == null)
                    return 1;
            }

            String n1 = a1.toString();
            String n2 = a2.toString();

            return n1.compareTo(n2);
        }
    };

    /**
     * provide ordering for {@link AnnotationMirror} based on their fully
     * qualified name.  The ordering ignores annotation values when ordering.
     *
     * The ordering is meant to be used as {@link TreeSet} or {@link TreeMap}
     * ordering.  A {@link Set} should not contain two annotations that only
     * differ in values.
     */
    public static Comparator<AnnotationMirror> annotationOrdering() {
        return ANNOTATION_ORDERING;
    }

    /**
     * Create a map suitable for storing {@link AnnotationMirror} as keys.
     *
     * It can store one instance of {@link AnnotationMirror} of a given
     * declared type, regardless of the annotation element values.
     *
     * @param <V> the value of the map
     * @return a new map with {@link AnnotationMirror} as key
     */
    public static <V> Map<AnnotationMirror, V> createAnnotationMap() {
        return new TreeMap<AnnotationMirror, V>(annotationOrdering());
    }

    /**
     * Constructs a {@link Set} suitable for storing {@link AnnotationMirror}s.
     *
     * It stores at most once instance of {@link AnnotationMirror} of a given
     * type, regardless of the annotation element values.
     *
     * @return a new set to store {@link AnnotationMirror} as element
     */
    public static Set<AnnotationMirror> createAnnotationSet() {
        return new TreeSet<AnnotationMirror>(annotationOrdering());
    }

    /**
     * Builds an annotation mirror that may have some values.
     *
     * Constructing an {@link AnnotationMirror} requires: <br />
     * 1. Constructing the builder with the desired annotation class <br />
     * 2. Setting each value individually using {@code SetValue} methods<br />
     * 3. Calling {@link #build()} to get the annotation build so far
     *
     * Once an annotation is built, no further modification or calls to
     * build can be made.  Otherwise, a {@link IllegalStateException}.
     *
     * All setter methods throw {@link IllegalArgumentException} if the
     * specified element is not found, or that the given value is not a
     * subtype of the expected type.
     *
     * TODO: Doesn't type check arrays yet
     */
    public static class AnnotationBuilder {

        private final ProcessingEnvironment env;
        private final TypeElement annotationElt;
        private final DeclaredType annotationType;
        private final Map<ExecutableElement, AnnotationValue> elementValues;

        public AnnotationBuilder(ProcessingEnvironment env, Class<? extends Annotation> anno) {
            this(env, anno.getCanonicalName());
        }

        public AnnotationBuilder(ProcessingEnvironment env, CharSequence name) {
            this.env = env;
            this.annotationElt = env.getElementUtils().getTypeElement(name);
            assert annotationElt.getKind() == ElementKind.ANNOTATION_TYPE;
            this.annotationType = (DeclaredType)annotationElt.asType();
            this.elementValues = new LinkedHashMap<ExecutableElement, AnnotationValue>();
        }

        public AnnotationBuilder(ProcessingEnvironment env, AnnotationMirror annotation) {
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
                SourceChecker.errorAbort("type was already built");
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
                        for (Map.Entry<ExecutableElement, AnnotationValue> pair :
                             elementValues.entrySet()) {
                            if (!first) buf.append(", ");
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
//                    return "@" + annotationType + "(" + elementValues + ")";
                }
            };
        }

        public AnnotationBuilder setValue(CharSequence elementName, AnnotationMirror value) {
            setValue(elementName, (Object)value);
            return this;
        }

        public AnnotationBuilder setValue(CharSequence elementName, List<? extends Object> values) {
            assertNotBuilt();
            List<AnnotationValue> value = new ArrayList<AnnotationValue>();
            ExecutableElement var = findElement(elementName);
            TypeMirror expectedType = var.getReturnType();
            if (expectedType.getKind() != TypeKind.ARRAY) {
                SourceChecker.errorAbort("value is an array while expected type is not");
                return null; // dead code
            }
            expectedType = ((ArrayType)expectedType).getComponentType();

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
            return setValue(elementName, (Object)value);
        }

        public AnnotationBuilder setValue(CharSequence elementName, Character value) {
            return setValue(elementName, (Object)value);
        }

        public AnnotationBuilder setValue(CharSequence elementName, Double value) {
            return setValue(elementName, (Object)value);
        }

        public AnnotationBuilder setValue(CharSequence elementName, Float value) {
            return setValue(elementName, (Object)value);
        }

        public AnnotationBuilder setValue(CharSequence elementName, Integer value) {
            return setValue(elementName, (Object)value);
        }

        public AnnotationBuilder setValue(CharSequence elementName, Long value) {
            return setValue(elementName, (Object)value);
        }

        public AnnotationBuilder setValue(CharSequence elementName, Short value) {
            return setValue(elementName, (Object)value);
        }

        public AnnotationBuilder setValue(CharSequence elementName, String value) {
            return setValue(elementName, (Object)value);
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
                TypeElement element = env.getElementUtils().getTypeElement(clazz.getCanonicalName());
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

        public AnnotationBuilder setValue(CharSequence elementName, VariableElement value) {
            ExecutableElement var = findElement(elementName);
            if (var.getReturnType().getKind() != TypeKind.DECLARED) {
                SourceChecker.errorAbort("exptected a non enum: " + var.getReturnType());
                return null; // dead code
            }
            if (!((DeclaredType)var.getReturnType()).asElement().equals(value.getEnclosingElement())) {
                SourceChecker.errorAbort("expected a different type of enum: " + value.getEnclosingElement());
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
                SourceChecker.errorAbort("exptected a non array: " + var.getReturnType());
                return null; // dead code
            }

            expectedType = ((ArrayType)expectedType).getComponentType();
            if (expectedType.getKind() != TypeKind.DECLARED) {
                SourceChecker.errorAbort("exptected a non enum component type: " + var.getReturnType());
                return null; // dead code
            }
            if (!((DeclaredType)expectedType).asElement().equals(enumElt.getEnclosingElement())) {
                SourceChecker.errorAbort("expected a different type of enum: " + enumElt.getEnclosingElement());
                return null; // dead code
            }

            List<AnnotationValue> res = new ArrayList<AnnotationValue>();
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
        // Which one is more useful/general? Unifying adds overhead of creating another array.
        public AnnotationBuilder setValue(CharSequence elementName, VariableElement[] values) {
            assertNotBuilt();
            ExecutableElement var = findElement(elementName);

            TypeMirror expectedType = var.getReturnType();
            if (expectedType.getKind() != TypeKind.ARRAY) {
                SourceChecker.errorAbort("exptected a non array: " + var.getReturnType());
                return null; // dead code
            }

            expectedType = ((ArrayType)expectedType).getComponentType();
            if (expectedType.getKind() != TypeKind.DECLARED) {
                SourceChecker.errorAbort("exptected a non enum component type: " + var.getReturnType());
                return null; // dead code
            }
            if (!((DeclaredType)expectedType).asElement().equals(values[0].getEnclosingElement())) {
                SourceChecker.errorAbort("expected a different type of enum: " + values[0].getEnclosingElement());
                return null; // dead code
            }

            List<AnnotationValue> res = new ArrayList<AnnotationValue>();
            for (VariableElement ev : values) {
                checkSubtype(expectedType, ev);
                res.add(createValue(ev));
            }
            AnnotationValue val = createValue(res);
            elementValues.put(var, val);
            return this;
        }

        private VariableElement findEnumElement(Enum<?> value) {
            String enumClass = value.getDeclaringClass().getCanonicalName();
            TypeElement enumClassElt = env.getElementUtils().getTypeElement(enumClass);
            assert enumClassElt != null;
            for (Element enumElt : enumClassElt.getEnclosedElements()) {
                if (enumElt.getSimpleName().contentEquals(value.name()))
                    return (VariableElement)enumElt;
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
            for (ExecutableElement elt :
                ElementFilter.methodsIn(annotationElt.getEnclosedElements())) {
                if (elt.getSimpleName().contentEquals(key)) {
                    return elt;
                }
            }
            SourceChecker.errorAbort("Couldn't find " + key + " element in " + annotationElt);
            return null; // dead code
        }

        // TODO: this method always returns true and no-one ever looks at the return value.
        private boolean checkSubtype(TypeMirror expected, Object givenValue) {
            final String newLine = System.getProperty("line.separator");

            Types types = env.getTypeUtils();

            if (expected.getKind().isPrimitive())
                expected = types.boxedClass((PrimitiveType)expected).asType();

            if (expected.getKind() == TypeKind.DECLARED
                    && TypesUtils.isClass(expected)
                    && givenValue instanceof TypeMirror)
                return true;

            TypeMirror found;
            boolean isSubtype;

            if (expected.getKind() == TypeKind.DECLARED
                    && ((DeclaredType)expected).asElement().getKind() == ElementKind.ANNOTATION_TYPE
                    && givenValue instanceof AnnotationMirror) {
                found = ((AnnotationMirror)givenValue).getAnnotationType();
                isSubtype = ((DeclaredType)expected).asElement().equals(((DeclaredType)found).asElement());
            } else if (givenValue instanceof AnnotationMirror) {
                found = ((AnnotationMirror)givenValue).getAnnotationType();
                // TODO: why is this always failing???
                isSubtype = false;
            } else if (givenValue instanceof VariableElement) {
                found = ((VariableElement)givenValue).asType();
                if (expected.getKind() == TypeKind.DECLARED) {
                    isSubtype = types.isSubtype(types.erasure(found), types.erasure(expected));
                } else {
                    isSubtype = false;
                }
            } else {
                found = env.getElementUtils().getTypeElement(givenValue.getClass().getCanonicalName()).asType();
                isSubtype = types.isSubtype(types.erasure(found), types.erasure(expected));
            }

            if (!isSubtype) {
                SourceChecker.errorAbort(
                        "given value differs from expected" + newLine +
                        "found: " + found + newLine +
                        "expected: " + expected);
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
                        List<?> list = (List<?>)value;
                        sb.append('{');
                        boolean isFirst = true;
                        for (Object o : list) {
                            if (!isFirst) sb.append(", ");
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
                        return  encl + var.toString();
                    } else {
                        return value.toString();
                    }
                }

                @SuppressWarnings("unchecked")
                @Override
                public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
                    if (value instanceof AnnotationMirror)
                        return v.visitAnnotation((AnnotationMirror)value, p);
                    else if (value instanceof List)
                        return v.visitArray((List<? extends AnnotationValue>)value, p);
                    else if (value instanceof Boolean)
                        return v.visitBoolean((Boolean)value, p);
                    else if (value instanceof Character)
                        return v.visitChar((Character)value, p);
                    else if (value instanceof Double)
                        return v.visitDouble((Double)value, p);
                    else if (value instanceof VariableElement)
                        return v.visitEnumConstant((VariableElement)value, p);
                    else if (value instanceof Float)
                        return v.visitFloat((Float)value, p);
                    else if (value instanceof Integer)
                        return v.visitInt((Integer)value, p);
                    else if (value instanceof Long)
                        return v.visitLong((Long)value, p);
                    else if (value instanceof Short)
                        return v.visitShort((Short)value, p);
                    else if (value instanceof String)
                        return v.visitString((String)value, p);
                    else if (value instanceof TypeMirror)
                        return v.visitType((TypeMirror)value, p);
                    else {
                        assert false : " unknown type : " + v.getClass();
                        return v.visitUnknown(this, p);
                    }
                }
            };
        }
    }

    /**
     * Get the attribute with the name {@code name} of the annotation
     * {@code anno}. The result is expected to have type {@code expectedType}.
     *
     * <p>
     * <em>Note 1</em>: The method only returns attribute values that are
     * explicitly present on {@code anno}. Default values are ignored. If
     * default values should be applied, use {@code elementValueWithDefaults}
     * instead.
     *
     * <p>
     * <em>Note 2</em>: The method does not work well for attributes of an array
     * type (as it would return a list of {@link AnnotationValue}s). Use
     * {@code elementValueArray} instead. instead.
     */
    public static <T> T elementValue(AnnotationMirror anno, CharSequence name,
            Class<T> expectedType) {
        for (ExecutableElement elem : anno.getElementValues().keySet()) {
            if (elem.getSimpleName().contentEquals(name)) {
                AnnotationValue val = anno.getElementValues().get(elem);
                return expectedType.cast(val.getValue());
            }
        }
        SourceChecker.errorAbort("No element with name " + name
                + " in annotation " + anno);
        return null; // dead code
    }

    /**
     * Get the attribute with the name {@code name} of the annotation
     * {@code anno}, or the default value if no attribute is present explicitly.
     * The result is expected to have type {@code expectedType}.
     *
     * <p>
     * <em>Note</em>: The method does not work well for attributes of an array
     * type (as it would return a list of {@link AnnotationValue}s). Use
     * {@code elementValueArray} instead. instead.
     */
    public static <T> T elementValueWithDefaults(AnnotationMirror anno,
            CharSequence name, Class<T> expectedType) {
        for (ExecutableElement elem : getElementValuesWithDefaults(anno)
                .keySet()) {
            if (elem.getSimpleName().contentEquals(name)) {
                AnnotationValue val = anno.getElementValues().get(elem);
                if (val == null) {
                    AnnotationValue defaultValue = elem.getDefaultValue();
                    Object value = defaultValue.getValue();
                    return expectedType.cast(value);
                }
                return expectedType.cast(val.getValue());
            }
        }
        SourceChecker.errorAbort("No element with name " + name
                + " in annotation " + anno);
        return null; // dead code
    }

    /**
     * Get the attribute with the name {@code name} of the annotation
     * {@code anno}, where the attribute has an array type. One element of the
     * result is expected to have type {@code expectedType}.
     *
     * <p>
     * <em>Note</em>: The method only returns attribute values that are
     * explicitly present on {@code anno}. Default values are ignored. If
     * default values should be applied, use
     * {@code elementValueArrayWithDefaults} instead.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> elementValueArray(AnnotationMirror anno,
            CharSequence name) {
        List<AnnotationValue> la = elementValue(anno, name, List.class);
        List<T> result = new ArrayList<T>(la.size());
        for (AnnotationValue a : la) {
            result.add((T) a.getValue());
        }
        return result;
    }

    /**
     * Get the attribute with the name {@code name} of the annotation
     * {@code anno}, or the default value if no attribute is present explicitly,
     * where the attribute has an array type. One element of the result is
     * expected to have type {@code expectedType}.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> elementValueArrayWithDefaults(
            AnnotationMirror anno, CharSequence name) {
        List<AnnotationValue> la = elementValueWithDefaults(anno, name, List.class);
        List<T> result = new ArrayList<T>(la.size());
        for (AnnotationValue a : la) {
            result.add((T) a.getValue());
        }
        return result;
    }

    /**
     * Get the attribute with the name {@code name} of the annotation
     * {@code anno}, or the default value if no attribute is present explicitly,
     * where the attribute has an array type and the elements are {@code Enum}s.
     * One element of the result is expected to have type {@code expectedType}.
     */
    public static <T extends Enum<T>> List<T> elementValueEnumArrayWithDefaults(
            AnnotationMirror anno, CharSequence name, Class<T> t) {
        @SuppressWarnings("unchecked")
        List<AnnotationValue> la = elementValueWithDefaults(anno, name, List.class);
        List<T> result = new ArrayList<T>(la.size());
        for (AnnotationValue a : la) {
            T value = Enum.valueOf(t, a.getValue().toString());
            result.add(value);
        }
        return result;
    }

    /**
     * name is an annotation field of type Class, and this gives its name.
     * Like elementValue(anno, name, Class.class).getCanonicalName() except
     * that elementValue() would return a Type.ClassType that would have to
     * be converted; this does that conversion.
     */
    public static String elementValueClassName(AnnotationMirror anno, CharSequence name) {
        Type.ClassType ct = elementValue(anno, name, Type.ClassType.class);
        // TODO:  Is it a problem that this returns the type parameters too?  Should I cut them off?
        return ct.toString();
    }

    /** Returns true if the given annotation has a @Inherited meta-annotation. */
    public static boolean hasInheritedMeta(AnnotationMirror anno) {
        return anno.getAnnotationType().asElement().getAnnotation(Inherited.class) != null;
    }

    /**
     * Update a mapping from some key to a set of AnnotationMirrors.
     * If the key already exists in the mapping and the new qualifier
     * is in the same qualifier hierarchy as any of the existing qualifiers,
     * do nothing and return false.
     * If the key already exists in the mapping and the new qualifier
     * is not in the same qualifier hierarchy as any of the existing qualifiers,
     * add the qualifier to the existing set and return true.
     * If the key does not exist in the mapping, add the new qualifier as a
     * singleton set and return true.
     *
     * @param map The mapping to modify.
     * @param key The key to update.
     * @param newQual The value to add.
     * @return Whether there was a qualifier hierarchy collision.
     */
    public static <T> boolean updateMappingToMutableSet(QualifierHierarchy qualHierarchy,
            Map<T, Set<AnnotationMirror>> map,
            T key, AnnotationMirror newQual) {

        if (!map.containsKey(key)) {
            Set<AnnotationMirror> set = AnnotationUtils.createAnnotationSet();
            set.add(newQual);
            map.put(key, set);
        } else {
            Set<AnnotationMirror> prevs = map.get(key);
            for (AnnotationMirror p : prevs) {
                if (AnnotationUtils.areSame(qualHierarchy.getTopAnnotation(p),
                        qualHierarchy.getTopAnnotation(newQual))) {
                    return false;
                }
            }
            prevs.add(newQual);
            map.put(key, prevs);
        }
        return true;
    }

    /**
     * 
     * @see #updateMappingToMutableSet(QualifierHierarchy, Map, Object, AnnotationMirror)
     */
    public static <T> void updateMappingToImmutableSet(Map<T, Set<AnnotationMirror>> map,
            T key, Set<AnnotationMirror> newQual) {

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
}
