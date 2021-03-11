package org.checkerframework.framework.stub;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.StringsPlume;

/**
 * Generates a stub file from a single class or an entire package.
 *
 * <p>A stub file can be used to add annotations to methods of classes, that are only available in
 * binary or the source of which cannot be edited.
 *
 * @checker_framework.manual #stub Using stub classes
 */
public class StubGenerator {
    /** The indentation for the class. */
    private static final String INDENTION = "    ";

    /** The output stream. */
    private final PrintStream out;

    /** the current indentation for the line being processed. */
    private String currentIndention = "";

    /** the package of the class being processed. */
    private String currentPackage = null;

    /** Constructs a {@code StubGenerator} that outputs to {@code System.out}. */
    public StubGenerator() {
        this(System.out);
    }

    /**
     * Constructs a {@code StubGenerator} that outputs to the provided output stream.
     *
     * @param out the output stream
     */
    public StubGenerator(PrintStream out) {
        this.out = out;
    }

    /**
     * Constructs a {@code StubGenerator} that outputs to the provided output stream.
     *
     * @param out the output stream
     */
    public StubGenerator(OutputStream out) {
        this.out = new PrintStream(out);
    }

    /** Generate the stub file for all the classes within the provided package. */
    public void stubFromField(Element elt) {
        if (!(elt.getKind() == ElementKind.FIELD)) {
            return;
        }

        String pkg = ElementUtils.getQualifiedName(ElementUtils.enclosingPackage(elt));
        if (!"".equals(pkg)) {
            currentPackage = pkg;
            currentIndention = "    ";
            indent();
        }
        VariableElement field = (VariableElement) elt;
        printFieldDecl(field);
    }

    /** Generate the stub file for all the classes within the provided package. */
    public void stubFromPackage(PackageElement packageElement) {
        currentPackage = packageElement.getQualifiedName().toString();

        indent();
        out.print("package ");
        out.print(currentPackage);
        out.println(";");

        for (TypeElement element : ElementFilter.typesIn(packageElement.getEnclosedElements())) {
            if (isPublicOrProtected(element)) {
                out.println();
                printClass(element);
            }
        }
    }

    /** Generate the stub file for all the classes within the provided package. */
    public void stubFromMethod(Element elt) {
        if (!(elt.getKind() == ElementKind.CONSTRUCTOR || elt.getKind() == ElementKind.METHOD)) {
            return;
        }

        String newPackage = ElementUtils.getQualifiedName(ElementUtils.enclosingPackage(elt));
        if (!newPackage.equals("")) {
            currentPackage = newPackage;
            currentIndention = "    ";
            indent();
        }
        ExecutableElement method = (ExecutableElement) elt;

        printMethodDecl(method);
    }

    /** Generate the stub file for provided class. The generated file includes the package name. */
    public void stubFromType(TypeElement typeElement) {

        // only output stub for classes or interfaces.  not enums
        if (typeElement.getKind() != ElementKind.CLASS
                && typeElement.getKind() != ElementKind.INTERFACE) {
            return;
        }

        String newPackageName =
                ElementUtils.getQualifiedName(ElementUtils.enclosingPackage(typeElement));
        boolean newPackage = !newPackageName.equals(currentPackage);
        currentPackage = newPackageName;

        if (newPackage) {
            indent();

            out.print("package ");
            out.print(currentPackage);
            out.println(";");
            out.println();
        }
        String fullClassName = ElementUtils.getQualifiedClassName(typeElement).toString();

        String className =
                fullClassName.substring(
                        fullClassName.indexOf(currentPackage)
                                + currentPackage.length()
                                // +1 because currentPackage doesn't include
                                // the . between the package name and the classname
                                + 1);

        int index = className.lastIndexOf('.');
        if (index == -1) {
            printClass(typeElement);
        } else {
            String outer = className.substring(0, index);
            printClass(typeElement, outer.replace('.', '$'));
        }
    }

    /** helper method that outputs the index for the provided class. */
    private void printClass(TypeElement typeElement) {
        printClass(typeElement, null);
    }

    /**
     * Helper method that prints the stub file for the provided class.
     *
     * @param typeElement the class to output
     * @param outerClass the outer class of the class, or null if {@code typeElement} is a top-level
     *     class
     */
    private void printClass(TypeElement typeElement, @Nullable String outerClass) {
        indent();

        List<? extends AnnotationMirror> teannos = typeElement.getAnnotationMirrors();
        if (teannos != null && !teannos.isEmpty()) {
            for (AnnotationMirror am : teannos) {
                out.println(am);
            }
        }

        if (typeElement.getKind() == ElementKind.INTERFACE) {
            out.print("interface");
        } else if (typeElement.getKind() == ElementKind.CLASS) {
            out.print("class");
        } else {
            return;
        }

        out.print(' ');
        if (outerClass != null) {
            out.print(outerClass + "$");
        }
        out.print(typeElement.getSimpleName());

        // Type parameters
        if (!typeElement.getTypeParameters().isEmpty()) {
            out.print('<');
            out.print(formatList(typeElement.getTypeParameters()));
            out.print('>');
        }

        // Extends
        if (typeElement.getSuperclass().getKind() != TypeKind.NONE
                && !TypesUtils.isObject(typeElement.getSuperclass())) {
            out.print(" extends ");
            out.print(formatType(typeElement.getSuperclass()));
        }

        // implements
        if (!typeElement.getInterfaces().isEmpty()) {
            final boolean isInterface = typeElement.getKind() == ElementKind.INTERFACE;
            out.print(isInterface ? " extends " : " implements ");
            List<String> ls = new ArrayList<>();
            for (TypeMirror itf : typeElement.getInterfaces()) {
                ls.add(formatType(itf));
            }
            out.print(formatList(ls));
        }

        out.println(" {");
        String tempIndention = currentIndention;

        currentIndention = currentIndention + INDENTION;

        // Inner classes, which the stub generator prints later.
        List<TypeElement> innerClass = new ArrayList<>();
        // side-effects innerClass
        printTypeMembers(typeElement.getEnclosedElements(), innerClass);

        currentIndention = tempIndention;
        indent();
        out.println("}");

        for (TypeElement element : innerClass) {
            printClass(element, typeElement.getSimpleName().toString());
        }
    }

    /**
     * Helper method that outputs the public or protected inner members of a class.
     *
     * @param members list of the class members
     */
    private void printTypeMembers(List<? extends Element> members, List<TypeElement> innerClass) {
        for (Element element : members) {
            if (isPublicOrProtected(element)) {
                printMember(element, innerClass);
            }
        }
    }

    /** Helper method that outputs the declaration of the member. */
    private void printMember(Element member, List<TypeElement> innerClass) {
        if (member.getKind().isField()) {
            printFieldDecl((VariableElement) member);
        } else if (member instanceof ExecutableElement) {
            printMethodDecl((ExecutableElement) member);
        } else if (member instanceof TypeElement) {
            innerClass.add((TypeElement) member);
        }
    }

    /**
     * Helper method that outputs the field declaration for the given field.
     *
     * <p>It indicates whether the field is {@code protected}.
     */
    private void printFieldDecl(VariableElement field) {
        if ("class".equals(field.getSimpleName().toString())) {
            error("Cannot write class literals in stub files.");
            return;
        }

        indent();

        List<? extends AnnotationMirror> veannos = field.getAnnotationMirrors();
        if (veannos != null && !veannos.isEmpty()) {
            for (AnnotationMirror am : veannos) {
                out.println(am);
            }
        }

        // if protected, indicate that, but not public
        if (field.getModifiers().contains(Modifier.PROTECTED)) {
            out.print("protected ");
        }
        if (field.getModifiers().contains(Modifier.STATIC)) {
            out.print("static ");
        }
        if (field.getModifiers().contains(Modifier.FINAL)) {
            out.print("final ");
        }

        out.print(formatType(field.asType()));

        out.print(" ");
        out.print(field.getSimpleName());
        out.println(';');
    }

    /**
     * Helper method that outputs the method declaration for the given method.
     *
     * <p>IT indicates whether the field is {@code protected}.
     */
    private void printMethodDecl(ExecutableElement method) {
        indent();

        List<? extends AnnotationMirror> eeannos = method.getAnnotationMirrors();
        if (eeannos != null && !eeannos.isEmpty()) {
            for (AnnotationMirror am : eeannos) {
                out.println(am);
            }
        }

        // if protected, indicate that, but not public
        if (method.getModifiers().contains(Modifier.PROTECTED)) {
            out.print("protected ");
        }
        if (method.getModifiers().contains(Modifier.STATIC)) {
            out.print("static ");
        }

        // print Generic arguments
        if (!method.getTypeParameters().isEmpty()) {
            out.print('<');
            out.print(formatList(method.getTypeParameters()));
            out.print("> ");
        }

        // not return type for constructors
        if (method.getKind() != ElementKind.CONSTRUCTOR) {
            out.print(formatType(method.getReturnType()));
            out.print(" ");
            out.print(method.getSimpleName());
        } else {
            out.print(method.getEnclosingElement().getSimpleName());
        }

        out.print('(');

        boolean isFirst = true;
        for (VariableElement param : method.getParameters()) {
            if (!isFirst) {
                out.print(", ");
            }
            out.print(formatType(param.asType()));
            out.print(' ');
            out.print(param.getSimpleName());
            isFirst = false;
        }

        out.print(')');

        if (!method.getThrownTypes().isEmpty()) {
            out.print(" throws ");
            List<String> ltt = new ArrayList<>();
            for (TypeMirror tt : method.getThrownTypes()) {
                ltt.add(formatType(tt));
            }
            out.print(formatList(ltt));
        }
        out.println(';');
    }

    /** Indent the current line. */
    private void indent() {
        out.print(currentIndention);
    }

    /**
     * Return a string representation of the list in the form of {@code item1, item2, item3, ...},
     * without surrounding square brackets as the default representation has.
     *
     * @param lst a list to format
     * @return a string representation of the list, without surrounding square brackets
     */
    private String formatList(List<?> lst) {
        return StringsPlume.join(", ", lst);
    }

    /** Returns true if the element is public or protected element. */
    private boolean isPublicOrProtected(Element element) {
        return element.getModifiers().contains(Modifier.PUBLIC)
                || element.getModifiers().contains(Modifier.PROTECTED);
    }

    /**
     * Returns the simple name of the type.
     *
     * @param typeRep a type
     * @return the simple name of the type
     */
    private static String formatType(TypeMirror typeRep) {
        StringTokenizer tokenizer = new StringTokenizer(typeRep.toString(), "()<>[], ", true);
        StringBuilder sb = new StringBuilder();

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.length() == 1 || token.lastIndexOf('.') == -1) {
                sb.append(token);
            } else {
                int index = token.lastIndexOf('.');
                sb.append(token.substring(index + 1));
            }
        }
        return sb.toString();
    }

    /**
     * The main entry point to StubGenerator.
     *
     * @param args command-line arguments
     */
    @SuppressWarnings("signature") // User-supplied arguments to main
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage:");
            System.out.println("    java StubGenerator [class or package name]");
            return;
        }

        Context context = new Context();
        Options options = Options.instance(context);
        if (SystemUtil.getJreVersion() == 8) {
            options.put(Option.SOURCE, "8");
            options.put(Option.TARGET, "8");
        }

        JavaCompiler javac = JavaCompiler.instance(context);
        javac.initModules(com.sun.tools.javac.util.List.nil());
        javac.enterDone();

        ProcessingEnvironment env = JavacProcessingEnvironment.instance(context);

        StubGenerator generator = new StubGenerator();

        if (env.getElementUtils().getPackageElement(args[0]) != null) {
            generator.stubFromPackage(env.getElementUtils().getPackageElement(args[0]));
        } else if (env.getElementUtils().getTypeElement(args[0]) != null) {
            generator.stubFromType(env.getElementUtils().getTypeElement(args[0]));
        } else {
            error("Couldn't find a package or a class named " + args[0]);
        }
    }

    private static void error(String string) {
        System.err.println("StubGenerator: " + string);
    }
}
