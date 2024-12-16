package org.checkerframework.common.util.debug;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.util.Collection;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A testing class that can be used to test {@link TypeElement}. In particular it tests that the
 * types read from classfiles are the same to the ones from Java files.
 *
 * <p>For testing, you need to do the following:
 *
 * <ol>
 *   <li>Run the Checker on the source file like any checker:
 *       <pre>{@code
 * java -processor org.checkerframework.common.util.debug.TypeOutputtingChecker [source-file]
 *
 * }</pre>
 *   <li>Run the Checker on the bytecode, by simply running the main and passing the qualified name,
 *       e.g.
 *       <pre>{@code
 * java org.checkerframework.common.util.debug.TypeOutputtingChecker [qualified-name]
 *
 * }</pre>
 *   <li>Apply a simple diff on the two outputs
 * </ol>
 */
public class TypeOutputtingChecker extends BaseTypeChecker {

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new Visitor(this);
  }

  /** Prints the types of the class and all of its enclosing fields, methods, and inner classes. */
  public static class Visitor extends BaseTypeVisitor<GenericAnnotatedTypeFactory<?, ?, ?, ?>> {
    String currentClass;

    public Visitor(BaseTypeChecker checker) {
      super(checker);
    }

    // Print types of classes, methods, and fields
    @Override
    public void processClassTree(ClassTree tree) {
      TypeElement element = TreeUtils.elementFromDeclaration(tree);
      currentClass = element.getSimpleName().toString();

      AnnotatedDeclaredType type = atypeFactory.getAnnotatedType(tree);
      System.out.println(tree.getSimpleName() + "\t" + type + "\t" + type.directSupertypes());

      super.processClassTree(tree);
    }

    @Override
    public void processMethodTree(String className, MethodTree tree) {
      ExecutableElement elem = TreeUtils.elementFromDeclaration(tree);

      AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
      System.out.println(currentClass + "." + elem + "\t\t" + type);
      // Don't dig deeper
    }

    @Override
    public Void visitVariable(VariableTree tree, Void p) {
      VariableElement elem = TreeUtils.elementFromDeclaration(tree);
      if (elem.getKind().isField()) {
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
        System.out.println(currentClass + "." + elem + "\t\t" + type);
      }

      // Don't dig deeper
      return null;
    }
  }

  /**
   * Main entry point.
   *
   * @param args command-line arguments
   */
  @SuppressWarnings("signature:argument") // user-supplied input, uncheckable
  public static void main(String[] args) {
    new TypeOutputtingChecker().run(args);
  }

  /**
   * Run the test.
   *
   * @param args command-line arguments
   */
  public void run(@CanonicalName String[] args) {
    ProcessingEnvironment env = JavacProcessingEnvironment.instance(new Context());
    Elements elements = env.getElementUtils();

    // TODO: Instead of using a GeneralAnnotatedTypeFactory, just use standard javac classes
    // to print explicit annotations.
    AnnotatedTypeFactory atypeFactory = new GeneralAnnotatedTypeFactory(this);

    for (String className : args) {
      TypeElement typeElt = elements.getTypeElement(className);
      printClassType(typeElt, atypeFactory);
    }
  }

  /** Prints the types of the class and all of its enclosing fields, methods, and inner classes. */
  protected static void printClassType(TypeElement typeElt, AnnotatedTypeFactory atypeFactory) {
    assert typeElt != null;

    String simpleName = typeElt.getSimpleName().toString();
    // Output class info
    AnnotatedDeclaredType type = atypeFactory.fromElement(typeElt);
    System.out.println(simpleName + "\t" + type + "\t" + type.directSupertypes());

    // output fields and methods
    for (Element enclosedElt : typeElt.getEnclosedElements()) {
      if (enclosedElt instanceof TypeElement) {
        printClassType((TypeElement) enclosedElt, atypeFactory);
      }
      if (!enclosedElt.getKind().isField() && !(enclosedElt instanceof ExecutableElement)) {
        continue;
      }
      AnnotatedTypeMirror memberType = atypeFactory.fromElement(enclosedElt);
      System.out.println(simpleName + "." + enclosedElt + "\t\t" + memberType);
    }
  }

  /**
   * Stores any explicit annotation in AnnotatedTypeMirrors. It doesn't have a qualifier hierarchy,
   * so it violates most of the specifications for AnnotatedTypeMirrors and AnnotatedTypeFactorys,
   * which may cause crashes and other unexpected behaviors.
   */
  public static class GeneralAnnotatedTypeFactory extends AnnotatedTypeFactory {

    public GeneralAnnotatedTypeFactory(BaseTypeChecker checker) {
      super(checker);
      postInit();
    }

    @Override
    public void postProcessClassTree(ClassTree tree) {
      // Do not store the qualifiers determined by this factory.  This factory adds
      // declaration annotations as type annotations, because TypeFromElement needs to read
      // declaration annotations and this factory blindly supports all annotations.
      // When storing those annotation to bytecode, the compiler chokes.  See testcase
      // tests/nullness/GeneralATFStore.java
    }

    /** Return true to support any qualifier. No handling of aliases. */
    @Override
    public boolean isSupportedQualifier(AnnotationMirror a) {
      return true;
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
      return new GeneralQualifierHierarchy(null);
    }

    /**
     * A very limited QualifierHierarchy that is used for access to qualifiers from different type
     * systems.
     */
    static class GeneralQualifierHierarchy extends QualifierHierarchy {

      /**
       * Creates a new GeneralQualifierHierarchy.
       *
       * @param atypeFactory the associated type factory
       */
      public GeneralQualifierHierarchy(GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
        super(atypeFactory);
      }

      // Always return true
      @Override
      public boolean isValid() {
        return true;
      }

      // Return the qualifier itself instead of the top.
      @Override
      public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
        return start;
      }

      // Return the qualifier itself instead of the bottom.
      @Override
      public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
        return start;
      }

      // Never find a corresponding qualifier.
      @Override
      public AnnotationMirror findAnnotationInSameHierarchy(
          Collection<? extends AnnotationMirror> annotations, AnnotationMirror annotationMirror) {
        return null;
      }

      // Not needed - raises error.
      @Override
      public AnnotationMirrorSet getTopAnnotations() {
        throw new BugInCF("GeneralQualifierHierarchy:getTopAnnotations() shouldn't be called");
      }

      // Not needed - should raise error. Unfortunately, in inference we ask for bottom
      // annotations.
      // Return a dummy value that does no harm.
      @Override
      public AnnotationMirrorSet getBottomAnnotations() {
        // throw new BugInCF("GeneralQualifierHierarchy.getBottomAnnotations()
        // shouldn't be called");
        return new AnnotationMirrorSet();
      }

      // Not needed - raises error.
      @Override
      public boolean isSubtypeQualifiers(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        throw new BugInCF("GeneralQualifierHierarchy.isSubtype() shouldn't be called.");
      }

      // Not needed - raises error.
      @Override
      public AnnotationMirror leastUpperBoundQualifiers(AnnotationMirror a1, AnnotationMirror a2) {
        throw new BugInCF("GeneralQualifierHierarchy.leastUpperBound() shouldn't be called.");
      }

      // Not needed - raises error.
      @Override
      public AnnotationMirror greatestLowerBoundQualifiers(
          AnnotationMirror a1, AnnotationMirror a2) {
        throw new BugInCF("GeneralQualifierHierarchy.greatestLowerBound() shouldn't be called.");
      }

      @Override
      public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
        throw new BugInCF(
            "GeneralQualifierHierarchy.getPolymorphicAnnotation() shouldn't be called.");
      }

      @Override
      public boolean isPolymorphicQualifier(AnnotationMirror qualifier) {
        return false;
      }
    }
  }
}
