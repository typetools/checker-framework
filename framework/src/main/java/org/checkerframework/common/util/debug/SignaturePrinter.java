package org.checkerframework.common.util.debug;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.AbstractElementVisitor8;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SourceVisitor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.javacutil.AbstractTypeProcessor;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.UserError;
import org.plumelib.reflection.Signatures;

/**
 * Outputs the method signatures of a class with fully annotated types.
 *
 * <p>The class determines the effective annotations for a checker in source or the classfile.
 * Finding the effective annotations is useful for the following purposes:
 *
 * <ol>
 *   <li value="1">Debugging annotations in classfile
 *   <li value="2">Debugging the default annotations that are implicitly added by the checker
 * </ol>
 *
 * <p>The class can be used in two possible ways, depending on the type file:
 *
 * <ol>
 *   <li id="a">From source: the class is to be used as an annotation processor when reading
 *       annotations from source. It can be invoked via the command:
 *       <p>{@code javac -processor SignaturePrinter <java files> ...}
 *   <li id="b">From classfile: the class is to be used as an independent app when reading
 *       annotations from classfile. It can be invoked via the command:
 *       <p>{@code java SignaturePrinter <class name>}
 * </ol>
 *
 * By default, only the annotations explicitly written by the user are emitted. To view the default
 * and effective annotations in a class that are associated with a checker, the fully qualified name
 * of the checker needs to be passed as {@code -Achecker=} argument, e.g.
 *
 * <pre>{@code
 * javac -processor SignaturePrinter
 *       -Achecker=org.checkerframework.checker.nullness.NullnessChecker JavaFile.java
 * }</pre>
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
@SupportedOptions("checker")
public class SignaturePrinter extends AbstractTypeProcessor {

  private SourceChecker checker;

  ///////// Initialization /////////////
  /**
   * Initialization.
   *
   * @param env the ProcessingEnvironment
   * @param checkerName the name of the checker
   */
  private void init(ProcessingEnvironment env, @Nullable @BinaryName String checkerName) {
    if (checkerName != null) {
      try {
        Class<?> checkerClass = Class.forName(checkerName);
        Constructor<?> cons = checkerClass.getConstructor();
        checker = (SourceChecker) cons.newInstance();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      checker =
          new SourceChecker() {

            @Override
            protected SourceVisitor<?, ?> createSourceVisitor() {
              return null;
            }

            @Override
            public AnnotationProvider getAnnotationProvider() {
              throw new UnsupportedOperationException(
                  "getAnnotationProvider is not implemented for this class.");
            }
          };
    }
    checker.init(env);
  }

  @Override
  public void typeProcessingStart() {
    super.typeProcessingStart();
    String checkerName = processingEnv.getOptions().get("checker");
    if (!Signatures.isBinaryName(checkerName)) {
      throw new UserError("Malformed checker name \"%s\"", checkerName);
    }
    init(processingEnv, checkerName);
  }

  @Override
  public void typeProcess(TypeElement element, TreePath p) {
    // TODO: fix this mess
    // checker.currentPath = p;
    // CompilationUnitTree root = p != null ? p.getCompilationUnit() : null;
    // ElementPrinter printer = new ElementPrinter(checker.createTypeFactory(), System.out);
    // printer.visit(element);
  }

  ////////// Printer //////////
  /** Element printer */
  static class ElementPrinter extends AbstractElementVisitor8<Void, Void> {
    /** String used for indentation */
    private static final String INDENTION = "    ";

    private final PrintStream out;
    private String indent = "";
    private final AnnotatedTypeFactory factory;

    public ElementPrinter(AnnotatedTypeFactory factory, PrintStream out) {
      this.factory = factory;
      this.out = out;
    }

    public void printTypeParams(List<? extends AnnotatedTypeVariable> params) {
      if (params.isEmpty()) {
        return;
      }

      out.print("<");
      boolean isntFirst = false;
      for (AnnotatedTypeMirror param : params) {
        if (isntFirst) {
          out.print(", ");
        }
        isntFirst = true;
        out.print(param);
      }
      out.print("> ");
    }

    public void printParameters(AnnotatedExecutableType type) {
      ExecutableElement elem = type.getElement();

      out.print("(");
      for (int i = 0; i < type.getParameterTypes().size(); ++i) {
        if (i != 0) {
          out.print(", ");
        }
        printVariable(type.getParameterTypes().get(i), elem.getParameters().get(i).getSimpleName());
      }
      out.print(")");
    }

    public void printThrows(AnnotatedExecutableType type) {
      if (type.getThrownTypes().isEmpty()) {
        return;
      }

      out.print(" throws ");

      boolean isntFirst = false;
      for (AnnotatedTypeMirror thrown : type.getThrownTypes()) {
        if (isntFirst) {
          out.print(", ");
        }
        isntFirst = true;
        out.print(thrown);
      }
    }

    public void printVariable(AnnotatedTypeMirror type, Name name, boolean isVarArg) {
      out.print(type);
      if (isVarArg) {
        out.println("...");
      }
      out.print(' ');
      out.print(name);
    }

    public void printVariable(AnnotatedTypeMirror type, Name name) {
      printVariable(type, name, false);
    }

    public void printType(AnnotatedTypeMirror type) {
      out.print(type);
      out.print(' ');
    }

    public void printName(CharSequence name) {
      out.print(name);
    }

    @Override
    public Void visitExecutable(ExecutableElement e, Void p) {
      out.print(indent);

      AnnotatedExecutableType type = factory.getAnnotatedType(e);
      printTypeParams(type.getTypeVariables());
      if (e.getKind() != ElementKind.CONSTRUCTOR) {
        printType(type.getReturnType());
      }
      printName(e.getSimpleName());
      printParameters(type);
      printThrows(type);
      out.println(';');

      return null;
    }

    @Override
    public Void visitPackage(PackageElement e, Void p) {
      throw new IllegalArgumentException("Cannot process packages");
    }

    private String typeIdentifier(TypeElement e) {
      switch (e.getKind()) {
        case INTERFACE:
          return "interface";
        case CLASS:
          return "class";
        case ANNOTATION_TYPE:
          return "@interface";
        case ENUM:
          return "enum";
        default:
          throw new IllegalArgumentException("Not a type element: " + e.getKind());
      }
    }

    @Override
    public Void visitType(TypeElement e, Void p) {
      String prevIndent = indent;

      out.print(indent);
      out.print(typeIdentifier(e));
      out.print(' ');
      out.print(e.getSimpleName());
      out.print(' ');
      AnnotatedDeclaredType dt = factory.getAnnotatedType(e);
      printSupers(dt);
      out.println("{");

      indent += INDENTION;

      for (Element enclosed : e.getEnclosedElements()) {
        this.visit(enclosed);
      }

      indent = prevIndent;
      out.print(indent);
      out.println("}");

      return null;
    }

    /**
     * Print the supertypes.
     *
     * @param dt the type whos supertypes to print
     */
    private void printSupers(AnnotatedDeclaredType dt) {
      if (dt.directSupertypes().isEmpty()) {
        return;
      }

      out.print("extends ");

      boolean isntFirst = false;
      for (AnnotatedDeclaredType st : dt.directSupertypes()) {
        if (isntFirst) {
          out.print(", ");
        }
        isntFirst = true;
        out.print(st);
      }
      out.print(' ');
    }

    @Override
    public Void visitTypeParameter(TypeParameterElement e, Void p) {
      throw new IllegalStateException("Shouldn't visit any type parameters");
    }

    @Override
    public Void visitVariable(VariableElement e, Void p) {
      if (!e.getKind().isField()) {
        throw new IllegalStateException("can only process fields, received " + e.getKind());
      }

      out.print(indent);
      AnnotatedTypeMirror type = factory.getAnnotatedType(e);
      this.printVariable(type, e.getSimpleName());
      out.println(';');

      return null;
    }
  }

  public static void printUsage() {
    System.out.println("   Usage: java SignaturePrinter [-Achecker=<checkerName>] classname");
  }

  private static final String CHECKER_ARG = "-Achecker=";

  public static void main(String[] args) {
    if (!(args.length == 1 && !args[0].startsWith(CHECKER_ARG))
        && !(args.length == 2 && args[0].startsWith(CHECKER_ARG))) {
      printUsage();
      return;
    }

    // process arguments
    String checkerName = null;
    if (args[0].startsWith(CHECKER_ARG)) {
      checkerName = args[0].substring(CHECKER_ARG.length());
      if (!Signatures.isBinaryName(checkerName)) {
        throw new UserError("Bad checker name \"%s\"", checkerName);
      }
    }

    // Setup compiler environment
    Context context = new Context();
    JavacProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
    SignaturePrinter printer = new SignaturePrinter();
    printer.init(env, checkerName);

    String className = args[args.length - 1];
    TypeElement elem = env.getElementUtils().getTypeElement(className);
    if (elem == null) {
      System.err.println("Couldn't find class: " + className);
      return;
    }

    printer.typeProcess(elem, null);
  }
}
