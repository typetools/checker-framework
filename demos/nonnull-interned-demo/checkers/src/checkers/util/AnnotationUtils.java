package checkers.util;

import checkers.quals.*;
import checkers.types.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import java.lang.annotation.*;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

/**
 * A utility class for working with annotations.
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public class AnnotationUtils {

    private final ProcessingEnvironment env;
    private final Elements elements;
    private final Types types;
    private final Trees trees;
    private final AnnotationFactory annoFactory;

    public AnnotationUtils(ProcessingEnvironment env) {
        this.env = env; 
        this.elements = env.getElementUtils();
        this.types = env.getTypeUtils();
        @Nullable Trees trees = Trees.instance(env);
        assert trees != null; /*nninvariant*/
        this.trees = trees;
        this.annoFactory = new AnnotationFactory(env);
    }

    /**
     * Finds default annotations starting at the leaf of the given tree path by
     * inspecting enclosing variable, method, and class declarations for
     * @DefaultQualifier annotations.
     *
     * @param path the tree path from which to start searching
     * @return a mapping from annotations (as {@link TypeElement}s) to the
     *         {@link DefaultLocations} for those annotations
     *
     * @see findDefaultLocations(Element)
     */
    public Map<TypeElement, Set<DefaultLocation>> findDefaultLocations(TreePath path) { 

        // Attempt to find a starting search point. If the tree itself has an
        // element, start there. Otherwise, try the enclosing method and
        // enclosing class.
        @Nullable Element typeElt = trees.getElement(path);
        
        // FIXME: eventually replace this with Scope
        if (typeElt == null) {
            @Nullable MethodTree method = TreeUtils.enclosingMethod(path);
            if (method != null) typeElt = InternalUtils.symbol(method);
        }
        if (typeElt == null) {
            @Nullable ClassTree cls = TreeUtils.enclosingClass(path);
            if (cls != null) typeElt = InternalUtils.symbol(cls);
        }

        if (typeElt == null)
            throw new IllegalArgumentException("no element or enclosing element");

        return findDefaultLocations(typeElt);
    }

    /**
     * Finds default annotations starting at the given element, inspecting the
     * element and its enclosing method and class declarations for @DefaultQualifier
     * annotations.
     *
     * @param elt the element from which to start searching
     * @return a mapping from annotations (as {@link TypeElement}s) to the
     *         {@link DefaultLocations} for those annotations
     *
     * @see findDefaultLocations(TreePath)
     */
    public Map<TypeElement, Set<DefaultLocation>> findDefaultLocations(Element elt) {

        @Nullable TypeElement defaultElt =
            elements.getTypeElement("checkers.quals.Default");
        assert defaultElt != null : "couldn't get element for @DefaultQualifier";

        Map<TypeElement, Set<@NonNull DefaultLocation>> locations
            = new HashMap<TypeElement, Set<DefaultLocation>>();

        List<? extends AnnotationMirror> annos = elt.getAnnotationMirrors();
        for (AnnotationMirror a : annos) {

            if (!defaultElt.equals(a.getAnnotationType().asElement()))
                continue;

            AnnotationData ad = annoFactory.createAnnotation(a);

            @Nullable String name = AnnotationUtils.parseStringValue(ad, "value");
            @Nullable TypeElement aElt = elements.getTypeElement(name);
            if (aElt == null)
                throw new RuntimeException("illegal annotation name: " + name);

            @Nullable Set<DefaultLocation> locs =
                AnnotationUtils.parseEnumConstantArrayValue(ad, "types",
                        DefaultLocation.class);

            if (!locations.containsKey(aElt))
                locations.put(aElt, new HashSet<DefaultLocation>());
            if (locs != null)
                locations.get(aElt).addAll(locs);
        }

        @Nullable Element encl = elt.getEnclosingElement();
        if (encl != null)
            locations.putAll(findDefaultLocations(encl));

        return Collections.<@NonNull TypeElement, @NonNull Set<DefaultLocation>>unmodifiableMap(locations);
    }
    

    /**
     * A generic base class for parsers of annotation values.
     */
    private abstract static class AbstractAnnotationValueParser<A>
        extends SimpleAnnotationValueVisitor6<Void, Boolean> {

        /**
         * @return the value of an annotation field
         */
        public abstract A getValue();

        public @Nullable Void visitArray(List<? extends AnnotationValue> vals, Boolean p) {
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
    private static class EnumConstantArrayValueParser<R extends Enum<R>>
        extends AbstractAnnotationValueParser<Set<R>> {

        private Set<R> values = new HashSet<R>();
        private Class<R> enumType;

        public EnumConstantArrayValueParser(Class<R> enumType) {
            this.enumType = enumType;
        }
        
        @Override
        public @Nullable Void visitEnumConstant(VariableElement c, Boolean p) {
            if (p == null || !p)
                return null;

            @Nullable R r = Enum.<R>valueOf(enumType, (@NonNull String)c.getSimpleName().toString());
            assert r != null; /*nninvariant*/
            values.add(r);
            return null;
        }

        public Set<R> getValue() {
            return Collections.<@NonNull R>unmodifiableSet(values);
        }
    }
    
    /**
     * A utility class for parsing a String-valued annotation.
     */
    private static class StringValueParser
        extends AbstractAnnotationValueParser<String> {

        private @Nullable String value = null;

        @Override
        public @Nullable Void visitString(String s, Boolean p) {
            value = s;
            return null;
        }

        public String getValue() {
            assert value != null; /*nninvariant*/
            return value;
        }
    }

    /**
     * A utility class for parsing a String[]-valued annotation.
     */
    private static class StringArrayValueParser 
        extends AbstractAnnotationValueParser<Set<String>> {

        private final Set<String> values = new HashSet<String>();

        @Override
        public @Nullable Void visitString(String s, Boolean p) {
            if (p == null || !p)
                return null;

            values.add(s);

            return null;
        }

        public Set<String> getValue() {
            return Collections.<@NonNull String>unmodifiableSet(values);
        }
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
    private static <R> @Nullable R parseAnnotationValue(AbstractAnnotationValueParser<R> parser,
            AnnotationData ad, String fieldName) {

        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
            ad.getValues();

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
     * @return the enum constant values of the given field
     */
    public static <R extends Enum<R>> @Nullable Set<R> parseEnumConstantArrayValue(AnnotationData ad, String field, Class<R> enumType) {
        return parseAnnotationValue(new EnumConstantArrayValueParser<R>(enumType), ad, field);
    }
    
    /**
     * @param ad the annotation for which a value will be parsed
     * @param field the name of the field to parse
     * @return the String value of the given field
     */
    public static @Nullable String parseStringValue(AnnotationData ad, String field) {
        return parseAnnotationValue(new StringValueParser(), ad, field);
    }

    /**
     * @param ad the annotation for which a value will be parsed
     * @param field the name of the field to parse
     * @return the String values of the given field
     */
    public static @Nullable Set<String> parseStringArrayValue(AnnotationData ad, String field) {
        return AnnotationUtils.<Set<@NonNull String>>parseAnnotationValue(new StringArrayValueParser(), ad, field);
    }
    
    /**
     * For an annotation with a "value" field of type String[], determines the
     * String values as a {@link Set}.
     *
     * @param ad the annotation to parse
     * @return a {@link Set} of {@link String}s for the annotation's String[]
     *         values, or an empty {@link Set} if the annotation doesn't have a
     *         String[] value field, or null if the annotation doesn't have a
     *         value field at all
     *
     * @deprecated use {@link parseStringArrayValue(AnnotationData, String)}
     *             instead
     */
    @Deprecated
    public static @Nullable Set<String> parseStringArrayValue(AnnotationData ad) {
        return parseStringArrayValue(ad, "value");
    }

    /**
     * @return the fully-qualified name of an annotation as a String
     */
    public static final @Nullable String annotationName(AnnotationData annotation) {
        TypeMirror annoType = annotation.getType();
        if (annoType.getKind() != TypeKind.DECLARED)
            return null;

        DeclaredType dt = (DeclaredType)annoType;
        Element elt = dt.asElement();
        if (elt instanceof TypeElement)
            return ((TypeElement)elt).getQualifiedName().toString();
        return null;
    }
}
