package checkers.util;

import checkers.quals.*;
import checkers.types.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

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
        /*@Nullable*/ Trees trees = Trees.instance(env);
        assert trees != null; /*nninvariant*/
        this.trees = trees;
        this.annoFactory = new AnnotationFactory(env);
    }

    /**
     * Finds default annotations starting at the leaf of the given tree path by
     * inspecting enclosing variable, method, and class declarations for
     * {@link Default} annotations.
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

        if (typeElt == null)
            throw new IllegalArgumentException("no element or enclosing element");

        return findDefaultLocations(typeElt);
    }

    /**
     * Finds default annotations starting at the given element, inspecting the
     * element and its enclosing method and class declarations for
     * {@link Default} annotations.
     * 
     * @param elt the element from which to start searching
     * @return a mapping from annotations (as {@link TypeElement}s) to the
     *         {@link DefaultLocation}s for those annotations
     * 
     * @see #findDefaultLocations(TreePath)
     */
    public Map<TypeElement, Set<DefaultLocation>> findDefaultLocations(Element elt) {

        /*@Nullable*/ TypeElement defaultElt =
            elements.getTypeElement("checkers.quals.Default");
        assert defaultElt != null : "couldn't get element for @DefaultQualifier";

        Map<TypeElement, Set</*@NonNull*/ DefaultLocation>> locations
            = new HashMap<TypeElement, Set<DefaultLocation>>();

        List<? extends AnnotationMirror> annos = elt.getAnnotationMirrors();
        for (AnnotationMirror a : annos) {

            if (!defaultElt.equals(a.getAnnotationType().asElement()))
                continue;

            /*@Nullable*/ String name = this.parseStringValue(a, "value");
            /*@Nullable*/ TypeElement aElt = elements.getTypeElement(name);
            if (aElt == null)
                throw new RuntimeException("illegal annotation name: " + name);

            /*@Nullable*/ Set<DefaultLocation> locs =
                this.parseEnumConstantArrayValue(a, "types",
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
    private static class EnumConstantArrayValueParser<R extends Enum<R>>
        extends AbstractAnnotationValueParser<Set<R>> {

        private Set<R> values = new HashSet<R>();
        private Class<R> enumType;

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
        extends AbstractAnnotationValueParser<Set<String>> {

        private final Set<String> values = new HashSet<String>();

        @Override
        public /*@Nullable*/ Void visitString(String s, Boolean p) {
            if (p == null || !p)
                return null;

            values.add(s);

            return null;
        }

        @Override
        public Set<String> getValue() {
            return Collections.</*@NonNull*/ String>unmodifiableSet(values);
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
    private <R> /*@Nullable*/ R parseAnnotationValue(AbstractAnnotationValueParser<R> parser,
            AnnotationMirror ad, String fieldName) {

        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
           elements.getElementValuesWithDefaults(ad);

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
    public <R extends Enum<R>> /*@Nullable*/ Set<R> parseEnumConstantArrayValue(AnnotationMirror ad, String field, Class<R> enumType) {
        return parseAnnotationValue(new EnumConstantArrayValueParser<R>(enumType), ad, field);
    }
    
    /**
     * @param ad the annotation for which a value will be parsed
     * @param field the name of the field to parse
     * @return the String value of the given field
     */
    public /*@Nullable*/ String parseStringValue(AnnotationMirror ad, String field) {
        return parseAnnotationValue(new StringValueParser(), ad, field);
    }

    /**
     * @param ad the annotation for which a value will be parsed
     * @param field the name of the field to parse
     * @return the String values of the given field
     */
    public /*@Nullable*/ Set<String> parseStringArrayValue(AnnotationMirror ad, String field) {
        return this.<Set</*@NonNull*/ String>>parseAnnotationValue(new StringArrayValueParser(), ad, field);
    }
    
    /**
     * @return the fully-qualified name of an annotation as a String
     */
    public static final /*@Nullable*/ String annotationName(/*@Nullable*/ AnnotationMirror annotation) {
        if (annotation == null) return null;
        final DeclaredType annoType = annotation.getAnnotationType();
        final TypeElement elm = (TypeElement) annoType.asElement();
        return elm.getQualifiedName().toString();
    }
    
    /**
     * Checks if both annotations are the same
     * 
     * @return true iff a1 and a2 are the same annotation
     */
    public static boolean isSame(/*@Nullable*/ AnnotationMirror a1, /*@Nullable*/ AnnotationMirror a2) {
        if (a1 != null && a2 != null)
            return annotationName(a1).equals(annotationName(a2)) &&
                a1.getElementValues().equals(a2.getElementValues());
        else 
            return a1 == a2;
    }

}
