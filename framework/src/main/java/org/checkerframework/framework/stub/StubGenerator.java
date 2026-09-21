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
import java.util.StringJoiner;
import java.util.StringTokenizer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsP;
import org.plumelib.util.StringsP;

/**
 * Generates a stub file from a single class or an entire package.
 *
 * <p>TODO: StubGenerator needs to be reimplemented, because it no longer works due to changes in
 * JDK 9.
 *
 * @checker_framework.manual #stub Using stub classes
 */
public class StubGenerator {
  /** The indentation for the class. */
  private static final String INDENTATION = "    ";

  /** The output stream. */
  private final PrintStream out;

  /** the current indentation for the line being processed. */
  private String currentIndentation = "";

  /**
   * The package of the class being processed, or null if no class has been processed yet. The empty
   * string represents the default package.
   *
   * <p>An alternative design would initialize this field to "", since that also represents the
   * default package, and thereby avoid the null value altogether.
   */
  private @MonotonicNonNull String currentPackage = null;

  /**
   * The name of the class being processed, including the names of its outer classes separated by
   * "$"; null if no class declaration is being processed.
   */
  private @Nullable String currentClassName = null;

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

  /**
   * Generate the stub file for all the classes within the provided package.
   *
   * @param elt an element whose package to use
   */
  public void stubFromField(Element elt) {
    if (elt.getKind() != ElementKind.FIELD) {
      return;
    }

    String pkg = ElementUtils.getQualifiedName(ElementUtils.enclosingPackage(elt));
    if (!"".equals(pkg)) {
      currentPackage = pkg;
      currentIndentation = "    ";
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

  /**
   * Generate the stub file for all the classes within the package that contains {@code elt}.
   *
   * @param elt a method or constructor; generate stub files for its package
   */
  public void stubFromMethod(ExecutableElement elt) {
    if (!(elt.getKind() == ElementKind.CONSTRUCTOR || elt.getKind() == ElementKind.METHOD)) {
      return;
    }

    String newPackage = ElementUtils.getQualifiedName(ElementUtils.enclosingPackage(elt));
    if (!newPackage.equals("")) {
      currentPackage = newPackage;
      currentIndentation = "    ";
      indent();
    }

    printMethodDecl(elt);
  }

  /**
   * Generate the stub file for provided class. The generated file includes the package name.
   *
   * @param typeElement the class to generate a stub file for
   */
  public void stubFromType(TypeElement typeElement) {

    // only output stub for classes, interfaces, records, and enums.  not annotation types
    if (typeElement.getKind() != ElementKind.CLASS
        && typeElement.getKind() != ElementKind.INTERFACE
        && typeElement.getKind() != ElementKind.RECORD
        && typeElement.getKind() != ElementKind.ENUM) {
      return;
    }

    String newPackageName =
        ElementUtils.getQualifiedName(ElementUtils.enclosingPackage(typeElement));
    boolean newPackage = !newPackageName.equals(currentPackage);
    currentPackage = newPackageName;

    // The unnamed package has no package declaration.
    if (newPackage && !currentPackage.isEmpty()) {
      indent();

      out.print("package ");
      out.print(currentPackage);
      out.println(";");
      out.println();
    }
    String fullClassName = ElementUtils.getQualifiedClassName(typeElement).toString();

    // The class name, including the names of any outer classes, but not the package name.
    String className;
    if (currentPackage.isEmpty()) {
      className = fullClassName;
    } else {
      // +1 for the "." between the package name and the class name.
      className = fullClassName.substring(currentPackage.length() + 1);
    }

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
   * @param outerClass the names of the outer classes of {@code typeElement}, separated by "$", or
   *     null if {@code typeElement} is a top-level class
   */
  private void printClass(TypeElement typeElement, @Nullable String outerClass) {
    indent();

    List<? extends AnnotationMirror> teannos = typeElement.getAnnotationMirrors();
    if (teannos != null && !teannos.isEmpty()) {
      for (AnnotationMirror am : teannos) {
        out.println(am);
      }
    }

    // This could be a `switch` statement.
    if (typeElement.getKind() == ElementKind.ANNOTATION_TYPE) {
      out.print("@interface");
    } else if (typeElement.getKind() == ElementKind.ENUM) {
      out.print("enum");
    } else if (typeElement.getKind() == ElementKind.INTERFACE) {
      out.print("interface");
    } else if (typeElement.getKind() == ElementKind.RECORD) {
      out.print("record");
    } else if (typeElement.getKind() == ElementKind.CLASS) {
      out.print("class");
    } else {
      // Shouldn't this throw an exception?
      return;
    }

    out.print(' ');
    // The name of the class, including the names of all its outer classes, separated by "$".
    String nestedClassName =
        (outerClass == null ? "" : outerClass + "$") + typeElement.getSimpleName();
    out.print(nestedClassName);

    // Type parameters
    out.print(formatTypeParameters(typeElement.getTypeParameters()));

    // Record components, which are part of a record's header
    if (typeElement.getKind() == ElementKind.RECORD) {
      StringJoiner components = new StringJoiner(", ", "(", ")");
      for (Element component : typeElement.getRecordComponents()) {
        StringBuilder sb = new StringBuilder();
        List<AnnotationMirror> typeAnnos = typeAnnotations(component.asType());
        for (AnnotationMirror am : component.getAnnotationMirrors()) {
          // An annotation that is applicable to both a record component and a type use appears
          // both here and within the component's type, so do not print it twice.
          if (!AnnotationUtils.containsSameByName(typeAnnos, am)) {
            sb.append(am);
            sb.append(' ');
          }
        }
        sb.append(formatType(component.asType()));
        sb.append(' ');
        sb.append(component.getSimpleName());
        components.add(sb);
      }
      out.print(components.toString());
    }

    // Extends.  A record or an enum may not have an `extends` clause; its superclass is always
    // java.lang.Record or java.lang.Enum, respectively.
    if (typeElement.getKind() != ElementKind.RECORD
        && typeElement.getKind() != ElementKind.ENUM
        && typeElement.getSuperclass().getKind() != TypeKind.NONE
        && !TypesUtils.isObject(typeElement.getSuperclass())) {
      out.print(" extends ");
      out.print(formatType(typeElement.getSuperclass()));
    }

    // implements.  An annotation type may not have an `implements` clause; it always
    // implements java.lang.annotation.Annotation.
    if (typeElement.getKind() != ElementKind.ANNOTATION_TYPE
        && !typeElement.getInterfaces().isEmpty()) {
      boolean isInterface = typeElement.getKind() == ElementKind.INTERFACE;
      out.print(isInterface ? " extends " : " implements ");
      List<String> ls =
          CollectionsP.mapList(StubGenerator::formatType, typeElement.getInterfaces());
      out.print(formatList(ls));
    }

    out.println(" {");
    String tempIndentation = currentIndentation;
    String tempClassName = currentClassName;

    currentIndentation = currentIndentation + INDENTATION;
    currentClassName = nestedClassName;

    // Enum constants, which must precede all other members of an enum.  The trailing semicolon is
    // required even if the enum has no constants.
    if (typeElement.getKind() == ElementKind.ENUM) {
      StringJoiner constants = new StringJoiner(", ");
      for (Element member : typeElement.getEnclosedElements()) {
        if (member.getKind() == ElementKind.ENUM_CONSTANT) {
          StringBuilder sb = new StringBuilder();
          for (AnnotationMirror am : member.getAnnotationMirrors()) {
            sb.append(am);
            sb.append(' ');
          }
          sb.append(member.getSimpleName());
          constants.add(sb);
        }
      }
      indent();
      out.println(constants + ";");
    }

    // Inner classes, which the stub generator prints later.
    List<TypeElement> innerClass = new ArrayList<>();
    // side-effects innerClass
    printTypeMembers(typeElement.getEnclosedElements(), innerClass);

    currentIndentation = tempIndentation;
    currentClassName = tempClassName;
    indent();
    out.println("}");

    for (TypeElement element : innerClass) {
      printClass(element, nestedClassName);
    }
  }

  /**
   * Helper method that outputs the public or protected inner members of a class.
   *
   * @param members list of the class members
   * @param innerClass a list to which this method adds the inner classes it encounters
   */
  private void printTypeMembers(List<? extends Element> members, List<TypeElement> innerClass) {
    for (Element element : members) {
      if (isPublicOrProtected(element)) {
        printMember(element, innerClass);
      }
    }
  }

  /**
   * Helper method that outputs the declaration of the member.
   *
   * @param member the member whose declaration to output
   * @param innerClass a list to which this method adds {@code member} if it is an inner class
   */
  private void printMember(Element member, List<TypeElement> innerClass) {
    if (member.getKind() == ElementKind.ENUM_CONSTANT) {
      // Enum constants are printed before all the other members of an enum.
      return;
    }
    if (member.getKind().isField()) {
      printFieldDecl((VariableElement) member);
    } else if (member instanceof ExecutableElement ee) {
      printMethodDecl(ee);
    } else if (member instanceof TypeElement te) {
      innerClass.add(te);
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
      out.print(formatTypeParameters(method.getTypeParameters()));
      out.print(" ");
    }

    // not return type for constructors
    if (method.getKind() != ElementKind.CONSTRUCTOR) {
      out.print(formatType(method.getReturnType()));
      out.print(" ");
      out.print(method.getSimpleName());
    } else {
      // A constructor's name is the name of the class declaration that contains it, which for a
      // nested class contains "$" separators.
      out.print(
          currentClassName != null
              ? currentClassName
              : method.getEnclosingElement().getSimpleName().toString());
    }

    StringJoiner params = new StringJoiner(", ", "(", ")");
    for (VariableElement param : method.getParameters()) {
      params.add(formatType(param.asType()) + " " + param.getSimpleName());
    }
    out.print(params.toString());

    if (!method.getThrownTypes().isEmpty()) {
      out.print(" throws ");
      List<String> ltt = CollectionsP.mapList(StubGenerator::formatType, method.getThrownTypes());
      out.print(formatList(ltt));
    }
    out.println(';');
  }

  /** Indent the current line. */
  private void indent() {
    out.print(currentIndentation);
  }

  /**
   * Returns a string representation of the list in the form of {@code item1, item2, item3, ...},
   * without surrounding square brackets as the default representation has.
   *
   * @param lst a list to format
   * @return a string representation of the list, without surrounding square brackets
   */
  private String formatList(@MustCallUnknown List<? extends @MustCallUnknown Object> lst) {
    return StringsP.join(", ", lst);
  }

  /** Returns true if the element is public or protected element. */
  private boolean isPublicOrProtected(Element element) {
    return element.getModifiers().contains(Modifier.PUBLIC)
        || element.getModifiers().contains(Modifier.PROTECTED);
  }

  /**
   * Returns the annotations that {@link #formatType} prints for the given type: those on the type
   * itself and, if it is an array type, those on its component type at any depth. An annotation on
   * an array's element type, as in {@code @p.Anno String[]}, is on the component type rather than
   * on the array type.
   *
   * @param typeRep a type
   * @return the annotations that {@link #formatType} prints for {@code typeRep}
   */
  private static List<AnnotationMirror> typeAnnotations(TypeMirror typeRep) {
    List<AnnotationMirror> result = new ArrayList<>();
    TypeMirror componentType = typeRep;
    while (true) {
      result.addAll(componentType.getAnnotationMirrors());
      if (componentType.getKind() != TypeKind.ARRAY) {
        return result;
      }
      componentType = ((ArrayType) componentType).getComponentType();
    }
  }

  /**
   * Returns a string representation of the type parameters, including their annotations and bounds
   * and surrounded by angle brackets, as in {@code <K, V extends Number>}. Returns the empty string
   * if there are no type parameters.
   *
   * @param typeParameters the type parameters of a class or a method
   * @return a string representation of the type parameters
   */
  private static String formatTypeParameters(List<? extends TypeParameterElement> typeParameters) {
    if (typeParameters.isEmpty()) {
      return "";
    }
    StringJoiner result = new StringJoiner(", ", "<", ">");
    for (TypeParameterElement typeParameter : typeParameters) {
      StringBuilder sb = new StringBuilder();
      List<AnnotationMirror> typeAnnos = typeAnnotations(typeParameter.asType());
      for (AnnotationMirror am : typeParameter.getAnnotationMirrors()) {
        // An annotation that is applicable to both a type parameter and a type use appears both
        // here and within the type parameter's type, so do not print it twice.
        if (!AnnotationUtils.containsSameByName(typeAnnos, am)) {
          sb.append(am);
          sb.append(' ');
        }
      }
      sb.append(formatType(typeParameter.asType()));
      List<? extends TypeMirror> bounds = typeParameter.getBounds();
      // A single bound of java.lang.Object is implicit, so do not print it.
      if (!(bounds.size() == 1 && TypesUtils.isObject(bounds.get(0)))) {
        sb.append(" extends ");
        sb.append(StringsP.join(" & ", CollectionsP.mapList(StubGenerator::formatType, bounds)));
      }
      result.add(sb);
    }
    return result.toString();
  }

  /**
   * Returns a string representation of the type, in which each type name is replaced by its simple
   * name. Annotations retain their fully-qualified names, because the generated stub file contains
   * no import statements. The arguments of an annotation are output verbatim.
   *
   * @param typeRep a type
   * @return a string representation of the type
   */
  private static String formatType(TypeMirror typeRep) {
    StringTokenizer tokenizer = new StringTokenizer(typeRep.toString(), "()<>[], ", true);
    StringBuilder sb = new StringBuilder();

    // The nesting depth of annotation argument lists; 0 when not within an annotation's arguments.
    int annoArgDepth = 0;
    // True if the previous token was an annotation, so an argument list may follow.
    boolean afterAnnotation = false;
    // The delimiter of the string or character literal that the current position is within, or 0
    // if the current position is not within a literal.  Meaningful only within an annotation's
    // arguments.
    char literalDelimiter = 0;

    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      if (annoArgDepth > 0) {
        // Output an annotation's arguments verbatim; for example, do not shorten the class
        // literal in "@p.Anno(p.Target.class)".
        if (literalDelimiter == 0) {
          if (token.equals("(")) {
            annoArgDepth++;
          } else if (token.equals(")")) {
            annoArgDepth--;
          }
        }
        literalDelimiter = literalStateAfter(token, literalDelimiter);
        sb.append(token);
        continue;
      }
      int atIndex = token.indexOf('@');
      if (atIndex != -1) {
        // The token contains a type annotation, as in "java.lang.@p.Anno".  Discard the package
        // name that precedes the annotation, and retain the annotation's fully-qualified name,
        // because the generated stub file contains no import statements.
        sb.append(token, atIndex, token.length());
        afterAnnotation = true;
        continue;
      }
      if (afterAnnotation && token.equals("(")) {
        annoArgDepth = 1;
        afterAnnotation = false;
        sb.append(token);
        continue;
      }
      afterAnnotation = false;
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
   * Returns the string-literal or character-literal nesting at the end of the given text, given the
   * nesting at the beginning of the text.
   *
   * @param text a substring of a type's string representation
   * @param startDelimiter the delimiter of the literal that is open at the beginning of {@code
   *     text}: a double quote, a single quote, or 0 if no literal is open
   * @return the delimiter of the literal that is open at the end of {@code text}, or 0 if no
   *     literal is open
   */
  private static char literalStateAfter(String text, char startDelimiter) {
    char delimiter = startDelimiter;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (delimiter == 0) {
        if (c == '"' || c == '\'') {
          delimiter = c;
        }
      } else if (c == '\\') {
        // Skip the escaped character.
        i++;
      } else if (c == delimiter) {
        delimiter = 0;
      }
    }
    return delimiter;
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
    if (SystemUtil.jreVersion == 8) {
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
