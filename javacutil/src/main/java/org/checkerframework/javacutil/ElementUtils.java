package org.checkerframework.javacutil;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A Utility class for analyzing {@code Element}s. */
public class ElementUtils {

    // Class cannot be instantiated.
    private ElementUtils() {
        throw new AssertionError("Class ElementUtils cannot be instantiated.");
    }

    /**
     * Returns the innermost type element enclosing the given element.
     *
     * @param elem the enclosed element of a class
     * @return the innermost type element, or null if no type element encloses {@code elem}
     */
    public static @Nullable TypeElement enclosingClass(final Element elem) {
        Element result = elem;
        while (result != null && !isClassElement(result)) {
            @Nullable Element encl = result.getEnclosingElement();
            result = encl;
        }
        return (TypeElement) result;
    }

    /**
     * Returns the innermost package element enclosing the given element. The same effect as {@link
     * javax.lang.model.util.Elements#getPackageOf(Element)}. Returns the element itself if it is a
     * package.
     *
     * @param elem the enclosed element of a package
     * @return the innermost package element
     */
    public static PackageElement enclosingPackage(final Element elem) {
        Element result = elem;
        while (result != null && result.getKind() != ElementKind.PACKAGE) {
            @Nullable Element encl = result.getEnclosingElement();
            result = encl;
        }
        return (PackageElement) result;
    }

    /**
     * Returns the "parent" package element for the given package element. For package "A.B" it
     * gives "A". For package "A" it gives the default package. For the default package it returns
     * null;
     *
     * <p>Note that packages are not enclosed within each other, we have to manually climb the
     * namespaces. Calling "enclosingPackage" on a package element returns the package element
     * itself again.
     *
     * @param elem the package to start from
     * @return the parent package element or {@code null}
     */
    public static @Nullable PackageElement parentPackage(
            final PackageElement elem, final Elements e) {
        // The following might do the same thing:
        //   ((Symbol) elt).owner;
        // TODO: verify and see whether the change is worth it.
        String fqnstart = elem.getQualifiedName().toString();
        String fqn = fqnstart;
        if (fqn != null && !fqn.isEmpty() && fqn.contains(".")) {
            fqn = fqn.substring(0, fqn.lastIndexOf('.'));
            return e.getPackageElement(fqn);
        }
        return null;
    }

    /**
     * Returns true if the element is a static element: whether it is a static field, static method,
     * or static class.
     *
     * @return true if element is static
     */
    public static boolean isStatic(Element element) {
        return element.getModifiers().contains(Modifier.STATIC);
    }

    /**
     * Returns true if the element is a final element: a final field, final method, or final class.
     *
     * @return true if the element is final
     */
    public static boolean isFinal(Element element) {
        return element.getModifiers().contains(Modifier.FINAL);
    }

    /**
     * Returns true if the element is a effectively final element.
     *
     * @return true if the element is effectively final
     */
    public static boolean isEffectivelyFinal(Element element) {
        Symbol sym = (Symbol) element;
        if (sym.getEnclosingElement().getKind() == ElementKind.METHOD
                && (sym.getEnclosingElement().flags() & Flags.ABSTRACT) != 0) {
            return true;
        }
        return (sym.flags() & (Flags.FINAL | Flags.EFFECTIVELY_FINAL)) != 0;
    }

    /**
     * Returns the {@code TypeMirror} for usage of Element as a value. It returns the return type of
     * a method element, the class type of a constructor, or simply the type mirror of the element
     * itself.
     *
     * @param element the element whose type to obtain
     * @return the type for the element used as a value
     */
    @SuppressWarnings("nullness:dereference.of.nullable") // a constructor has an enclosing class
    public static TypeMirror getType(Element element) {
        if (element.getKind() == ElementKind.METHOD) {
            return ((ExecutableElement) element).getReturnType();
        } else if (element.getKind() == ElementKind.CONSTRUCTOR) {
            return enclosingClass(element).asType();
        } else {
            return element.asType();
        }
    }

    /**
     * Returns the qualified name of the innermost class enclosing the provided {@code Element}.
     *
     * @param element an element enclosed by a class, or a {@code TypeElement}
     * @return the qualified {@code Name} of the innermost class enclosing the element
     */
    public static @Nullable Name getQualifiedClassName(Element element) {
        if (element.getKind() == ElementKind.PACKAGE) {
            PackageElement elem = (PackageElement) element;
            return elem.getQualifiedName();
        }

        TypeElement elem = enclosingClass(element);
        if (elem == null) {
            return null;
        }

        return elem.getQualifiedName();
    }

    /**
     * Returns a verbose name that identifies the element.
     *
     * @param elt the element whose name to obtain
     * @return the verbose name of the given element
     */
    public static String getVerboseName(Element elt) {
        Name n = getQualifiedClassName(elt);
        if (n == null) {
            return "Unexpected element: " + elt;
        }
        if (elt.getKind() == ElementKind.PACKAGE || isClassElement(elt)) {
            return n.toString();
        } else {
            return n + "." + elt;
        }
    }

    /**
     * Returns the canonical representation of the method declaration, which contains simple names
     * of the types only.
     *
     * @param element a method declaration
     * @return the simple name of the method, followed by the simple names of the formal parameter
     *     types
     */
    public static String getSimpleName(ExecutableElement element) {
        // note: constructor simple name is <init>
        StringJoiner sj = new StringJoiner(",", element.getSimpleName() + "(", ")");
        for (Iterator<? extends VariableElement> i = element.getParameters().iterator();
                i.hasNext(); ) {
            sj.add(TypesUtils.simpleTypeName(i.next().asType()));
        }
        return sj.toString();
    }

    /**
     * Check if the element is an element for 'java.lang.Object'
     *
     * @param element the type element
     * @return true iff the element is java.lang.Object element
     */
    public static boolean isObject(TypeElement element) {
        return element.getQualifiedName().contentEquals("java.lang.Object");
    }

    /** Returns true if the element is a reference to a compile-time constant. */
    public static boolean isCompileTimeConstant(Element elt) {
        return elt != null
                && (elt.getKind() == ElementKind.FIELD
                        || elt.getKind() == ElementKind.LOCAL_VARIABLE)
                && ((VariableElement) elt).getConstantValue() != null;
    }

    /**
     * Checks whether a given element came from a source file.
     *
     * <p>By contrast, {@link ElementUtils#isElementFromByteCode(Element)} returns true if there is
     * a classfile for the given element, even if there is also a source file.
     *
     * @param element the element to check, or null
     * @return true if a source file containing the element is being compiled
     */
    public static boolean isElementFromSourceCode(@Nullable Element element) {
        if (element == null) {
            return false;
        }
        if (element instanceof Symbol.ClassSymbol) {
            return isElementFromSourceCodeImpl((Symbol.ClassSymbol) element);
        }
        return isElementFromSourceCode(element.getEnclosingElement());
    }

    /**
     * Checks whether a given ClassSymbol came from a source file.
     *
     * <p>By contrast, {@link ElementUtils#isElementFromByteCode(Element)} returns true if there is
     * a classfile for the given element, even if there is also a source file.
     *
     * @param symbol the class to check
     * @return true if a source file containing the class is being compiled
     */
    private static boolean isElementFromSourceCodeImpl(Symbol.ClassSymbol symbol) {
        // This is a bit of a hack to avoid treating JDK as source files. JDK files' toUri() method
        // returns just the name of the file (e.g. "Object.java"), but any file actually being
        // compiled returns a file URI to the source file.
        return symbol.sourcefile != null
                && symbol.sourcefile.getKind() == JavaFileObject.Kind.SOURCE
                && symbol.sourcefile.toUri().toString().startsWith("file:");
    }

    /**
     * Returns true if the element is declared in ByteCode. Always return false if elt is a package.
     */
    public static boolean isElementFromByteCode(Element elt) {
        if (elt == null) {
            return false;
        }

        if (elt instanceof Symbol.ClassSymbol) {
            Symbol.ClassSymbol clss = (Symbol.ClassSymbol) elt;
            if (null != clss.classfile) {
                // The class file could be a .java file
                return clss.classfile.getName().endsWith(".class");
            } else {
                return false;
            }
        }
        return isElementFromByteCodeHelper(elt.getEnclosingElement());
    }

    /**
     * Returns true if the element is declared in ByteCode. Always return false if elt is a package.
     */
    private static boolean isElementFromByteCodeHelper(Element elt) {
        if (elt == null) {
            return false;
        }
        if (elt instanceof Symbol.ClassSymbol) {
            Symbol.ClassSymbol clss = (Symbol.ClassSymbol) elt;
            if (null != clss.classfile) {
                // The class file could be a .java file
                return (clss.classfile.getName().endsWith(".class")
                        || clss.classfile.getName().endsWith(".class)")
                        || clss.classfile.getName().endsWith(".class)]"));
            } else {
                return false;
            }
        }
        return isElementFromByteCodeHelper(elt.getEnclosingElement());
    }

    /** Returns the field of the class or {@code null} if not found. */
    public static @Nullable VariableElement findFieldInType(TypeElement type, String name) {
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (field.getSimpleName().contentEquals(name)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Returns the elements of the fields whose simple names are {@code names} and are declared in
     * {@code type}.
     *
     * <p>If a field isn't declared in {@code type}, its element isn't included in the returned set.
     * If none of the fields is declared in {@code type}, the empty set is returned.
     *
     * @param type where to look for fields
     * @param names simple names of fields that might be declared in {@code type}
     * @return the elements of the fields whose simple names are {@code names} and are declared in
     *     {@code type}
     */
    public static Set<VariableElement> findFieldsInType(
            TypeElement type, Collection<String> names) {
        Set<VariableElement> results = new HashSet<>();
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (names.contains(field.getSimpleName().toString())) {
                results.add(field);
            }
        }
        return results;
    }

    /**
     * Returns non-private field elements, and side-effects {@code names} to remove them. For every
     * field name in {@code names} that is declared in {@code type} or a supertype, add its element
     * to the returned set and remove it from {@code names}.
     *
     * <p>When this routine returns, the combination of the return value and {@code names} has the
     * same cardinality, and represents the same fields, as {@code names} did when the method was
     * called.
     *
     * @param type where to look for fields
     * @param names simple names of fields that might be declared in {@code type} or a supertype.
     *     (Names that are found are removed from this list.)
     * @return the {@code VariableElement}s for non-private fields that are declared in {@code type}
     *     whose simple names were in {@code names} when the method was called.
     */
    public static Set<VariableElement> findFieldsInTypeOrSuperType(
            TypeMirror type, Collection<String> names) {
        int origCardinality = names.size();
        Set<VariableElement> elements = new HashSet<>();
        findFieldsInTypeOrSuperType(type, names, elements);
        // Since names may contain duplicates, I don't trust the claim in the documentation about
        // cardinality.  (Does any code depend on the invariant, though?)
        if (origCardinality != names.size() + elements.size()) {
            throw new BugInCF(
                    "Bad sizes: %d != %d + %d", origCardinality, names.size(), elements.size());
        }
        return elements;
    }

    /**
     * Side-effects both {@code foundFields} (which starts empty) and {@code notFound}, conceptually
     * moving elements from {@code notFound} to {@code foundFields}.
     */
    private static void findFieldsInTypeOrSuperType(
            TypeMirror type, Collection<String> notFound, Set<VariableElement> foundFields) {
        if (TypesUtils.isObject(type)) {
            return;
        }
        TypeElement elt = TypesUtils.getTypeElement(type);
        assert elt != null : "@AssumeAssertion(nullness): assumption";
        Set<VariableElement> fieldElts = findFieldsInType(elt, notFound);
        for (VariableElement field : new HashSet<VariableElement>(fieldElts)) {
            if (!field.getModifiers().contains(Modifier.PRIVATE)) {
                notFound.remove(field.getSimpleName().toString());
            } else {
                fieldElts.remove(field);
            }
        }
        foundFields.addAll(fieldElts);

        if (!notFound.isEmpty()) {
            findFieldsInTypeOrSuperType(elt.getSuperclass(), notFound, foundFields);
        }
    }

    /**
     * Returns true if {@code element} is "com.sun.tools.javac.comp.Resolve$SymbolNotFoundError".
     *
     * @param element the element to test
     * @return true if {@code element} is "com.sun.tools.javac.comp.Resolve$SymbolNotFoundError"
     */
    public static boolean isError(Element element) {
        return element.getClass().getName()
                == "com.sun.tools.javac.comp.Resolve$SymbolNotFoundError"; // interned
    }

    /**
     * Does the given element need a receiver for accesses? For example, an access to a local
     * variable does not require a receiver.
     *
     * @param element the element to test
     * @return whether the element requires a receiver for accesses
     */
    public static boolean hasReceiver(Element element) {
        if (element.getKind() == ElementKind.CONSTRUCTOR) {
            // The enclosing element of a constructor is the class it creates.
            // A constructor can only have a receiver if the class it creates has an outer type.
            TypeMirror t = element.getEnclosingElement().asType();
            return TypesUtils.hasEnclosingType(t);
        }
        return (element.getKind().isField() || element.getKind() == ElementKind.METHOD)
                && !ElementUtils.isStatic(element);
    }

    /**
     * Determine all type elements for the classes and interfaces referenced (directly or
     * indirectly) in the extends/implements clauses of the given type element.
     *
     * <p>TODO: can we learn from the implementation of
     * com.sun.tools.javac.model.JavacElements.getAllMembers(TypeElement)?
     */
    public static List<TypeElement> getSuperTypes(TypeElement type, Elements elements) {

        List<TypeElement> superelems = new ArrayList<>();
        if (type == null) {
            return superelems;
        }

        // Set up a stack containing type, which is our starting point.
        Deque<TypeElement> stack = new ArrayDeque<>();
        stack.push(type);

        while (!stack.isEmpty()) {
            TypeElement current = stack.pop();

            // For each direct supertype of the current type element, if it
            // hasn't already been visited, push it onto the stack and
            // add it to our superelems set.
            TypeMirror supertypecls;
            try {
                supertypecls = current.getSuperclass();
            } catch (com.sun.tools.javac.code.Symbol.CompletionFailure cf) {
                // Looking up a supertype failed. This sometimes happens
                // when transitive dependencies are not on the classpath.
                // As javac didn't complain, let's also not complain.
                supertypecls = null;
            }

            if (supertypecls != null && supertypecls.getKind() != TypeKind.NONE) {
                TypeElement supercls = (TypeElement) ((DeclaredType) supertypecls).asElement();
                if (!superelems.contains(supercls)) {
                    stack.push(supercls);
                    superelems.add(supercls);
                }
            }

            for (TypeMirror supertypeitf : current.getInterfaces()) {
                TypeElement superitf = (TypeElement) ((DeclaredType) supertypeitf).asElement();
                if (!superelems.contains(superitf)) {
                    stack.push(superitf);
                    superelems.add(superitf);
                }
            }
        }

        // Include java.lang.Object as implicit superclass for all classes and interfaces.
        TypeElement jlobject = elements.getTypeElement("java.lang.Object");
        if (!superelems.contains(jlobject)) {
            superelems.add(jlobject);
        }

        return Collections.unmodifiableList(superelems);
    }

    /**
     * Return all fields declared in the given type or any superclass/interface. TODO: should this
     * use javax.lang.model.util.Elements.getAllMembers(TypeElement) instead of our own
     * getSuperTypes?
     */
    public static List<VariableElement> getAllFieldsIn(TypeElement type, Elements elements) {
        List<VariableElement> fields = new ArrayList<>();
        fields.addAll(ElementFilter.fieldsIn(type.getEnclosedElements()));
        List<TypeElement> alltypes = getSuperTypes(type, elements);
        for (TypeElement atype : alltypes) {
            fields.addAll(ElementFilter.fieldsIn(atype.getEnclosedElements()));
        }
        return Collections.unmodifiableList(fields);
    }

    /**
     * Return all methods declared in the given type or any superclass/interface. Note that no
     * constructors will be returned. TODO: should this use
     * javax.lang.model.util.Elements.getAllMembers(TypeElement) instead of our own getSuperTypes?
     */
    public static List<ExecutableElement> getAllMethodsIn(TypeElement type, Elements elements) {
        List<ExecutableElement> meths = new ArrayList<>();
        meths.addAll(ElementFilter.methodsIn(type.getEnclosedElements()));

        List<TypeElement> alltypes = getSuperTypes(type, elements);
        for (TypeElement atype : alltypes) {
            meths.addAll(ElementFilter.methodsIn(atype.getEnclosedElements()));
        }
        return Collections.unmodifiableList(meths);
    }

    /** Return all nested/inner classes/interfaces declared in the given type. */
    public static List<TypeElement> getAllTypeElementsIn(TypeElement type) {
        List<TypeElement> types = new ArrayList<>();
        types.addAll(ElementFilter.typesIn(type.getEnclosedElements()));
        return types;
    }

    /** The set of kinds that represent classes. */
    private static final Set<ElementKind> classElementKinds;

    static {
        classElementKinds = EnumSet.noneOf(ElementKind.class);
        for (ElementKind kind : ElementKind.values()) {
            if (kind.isClass() || kind.isInterface()) {
                classElementKinds.add(kind);
            }
        }
    }

    /**
     * Return the set of kinds that represent classes.
     *
     * @return the set of kinds that represent classes
     */
    public static Set<ElementKind> classElementKinds() {
        return classElementKinds;
    }

    /**
     * Is the given element kind a class, i.e. a class, enum, interface, or annotation type.
     *
     * @param element the element to test
     * @return true, iff the given kind is a class kind
     */
    public static boolean isClassElement(Element element) {
        return classElementKinds().contains(element.getKind());
    }

    /**
     * Return true if the element is a type declaration.
     *
     * @param elt the element to test
     * @return true if the argument is a type declaration
     */
    public static boolean isTypeDeclaration(Element elt) {
        return isClassElement(elt) || elt.getKind() == ElementKind.TYPE_PARAMETER;
    }

    /**
     * Check that a method Element matches a signature.
     *
     * <p>Note: Matching the receiver type must be done elsewhere as the Element receiver type is
     * only populated when annotated.
     *
     * @param method the method Element
     * @param methodName the name of the method
     * @param parameters the formal parameters' Classes
     * @return true if the method matches
     */
    public static boolean matchesElement(
            ExecutableElement method, String methodName, Class<?>... parameters) {

        if (!method.getSimpleName().contentEquals(methodName)) {
            return false;
        }

        if (method.getParameters().size() != parameters.length) {
            return false;
        } else {
            for (int i = 0; i < method.getParameters().size(); i++) {
                if (!method.getParameters()
                        .get(i)
                        .asType()
                        .toString()
                        .equals(parameters[i].getName())) {

                    return false;
                }
            }
        }

        return true;
    }

    /** Returns true if the given element is, or overrides, method. */
    public static boolean isMethod(
            ExecutableElement questioned, ExecutableElement method, ProcessingEnvironment env) {
        TypeElement enclosing = (TypeElement) questioned.getEnclosingElement();
        return questioned.equals(method)
                || env.getElementUtils().overrides(questioned, method, enclosing);
    }
}
