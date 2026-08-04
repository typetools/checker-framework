package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ClassTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import java.net.URI;
import java.util.Collections;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.InferenceType;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.junit.Assert;
import org.junit.Test;

public class TypingTest {

  /** The annotated type factory, which is the type factory of a {@link ValueChecker}. */
  private final AnnotatedTypeFactory typeFactory;

  /** The inference context that the constraints under test belong to. */
  private final Java8InferenceContext context;

  /** javax.lang.model.util.Elements */
  private final Elements elements;

  /** The type variable that {@link #theta} maps to an inference variable. */
  private final TypeVariable typeVariable;

  /** A mapping from {@link #typeVariable} to an inference variable. */
  private final Theta theta;

  /** Creates a new TypingTest. */
  public TypingTest() {
    Context javacContext = new Context();
    ProcessingEnvironment env = JavacProcessingEnvironment.instance(javacContext);
    JavaCompiler javac = JavaCompiler.instance(javacContext);
    // The list of modules must be initialized before entering symbols.
    javac.initModules(com.sun.tools.javac.util.List.nil());
    javac.enterDone();

    ValueChecker checker = new ValueChecker();
    checker.init(env);
    checker.initChecker();
    this.typeFactory = checker.getTypeFactory();
    this.elements = env.getElementUtils();

    // Java8InferenceContext needs a path to an expression that is enclosed by a class.  The
    // class need not be attributed, because the only use of it is TreeUtils.typeOf.
    JCCompilationUnit compilationUnit = javac.parse(new StringJavaFileObject("class Dummy {}"));
    ClassTree classTree = (ClassTree) compilationUnit.getTypeDecls().get(0);
    TreePath path = new TreePath(new TreePath(compilationUnit), classTree);
    this.context = new Java8InferenceContext(typeFactory, path, null);

    this.typeVariable = typeVariableOfSingletonList();
    this.theta = new Theta();
    // The inference variable itself is never used by the reductions under test.
    theta.put(typeVariable, null);
  }

  /**
   * Reducing {@code ‹String <: T›}, where {@code T} is an inference type that is not a
   * parameterized type, does not throw a {@code ClassCastException}.
   *
   * <p>Before the fix, the cast of {@code String}, which is a {@link ProperType}, to {@link
   * InferenceType} threw.
   */
  @Test
  public void properSubtypeOfNonParameterizedInferenceType() {
    AbstractType s = properType("java.lang.String");
    AbstractType t = nonParameterizedInferenceType();
    Assert.assertTrue(s.isProper());

    ReductionResult result = new Typing("test", s, t, Constraint.Kind.SUBTYPE).reduce(context);

    // String is not a subtype of Number.
    Assert.assertSame(ConstraintSet.FALSE, result);
  }

  /**
   * Reducing {@code ‹List<T> <: T›}, where the right-hand side is an inference type that is not a
   * parameterized type, does not throw a {@code ClassCastException}.
   *
   * <p>Before the fix, the cast of the right-hand side, which is an {@link InferenceType}, to
   * {@link ProperType} threw.
   */
  @Test
  public void inferenceTypeSubtypeOfNonParameterizedInferenceType() {
    AbstractType s = listOfTypeVariable();
    AbstractType t = nonParameterizedInferenceType();
    Assert.assertTrue(s.isInferenceType());

    ReductionResult result = new Typing("test", s, t, Constraint.Kind.SUBTYPE).reduce(context);

    // List is not a subtype of Number.
    Assert.assertSame(ConstraintSet.FALSE, result);
  }

  /**
   * Returns an {@link InferenceType} whose Java type is {@code java.lang.Number}, which is a class
   * type that is not parameterized.
   *
   * <p>Inference itself does not create such a type: it treats a type as an inference type exactly
   * when the type arguments of its annotated type mention an inference variable, and a class type
   * with a type argument is parameterized. So the annotated type is given a type argument that its
   * underlying Java type does not have.
   *
   * @return an inference type whose Java type is not a parameterized type
   */
  private AbstractType nonParameterizedInferenceType() {
    TypeMirror number = elements.getTypeElement("java.lang.Number").asType();
    AnnotatedDeclaredType numberAtm =
        (AnnotatedDeclaredType) AnnotatedTypeMirror.createType(number, typeFactory, false);
    numberAtm.addMissingAnnotations(typeFactory.getQualifierHierarchy().getTopAnnotations());
    numberAtm.setTypeArguments(
        Collections.singletonList(
            AnnotatedTypeMirror.createType(typeVariable, typeFactory, false)));

    AbstractType result = InferenceType.create(numberAtm, number, theta, context);
    Assert.assertTrue(result.isInferenceType());
    Assert.assertSame(TypeKind.DECLARED, result.getTypeKind());
    Assert.assertFalse(result.isParameterizedType());
    return result;
  }

  /**
   * Returns a {@link ProperType} for the given class.
   *
   * @param className the fully-qualified name of a class
   * @return a proper type for {@code className}
   */
  private AbstractType properType(String className) {
    TypeMirror javaType = elements.getTypeElement(className).asType();
    AnnotatedTypeMirror atm = AnnotatedTypeMirror.createType(javaType, typeFactory, false);
    atm.addMissingAnnotations(typeFactory.getQualifierHierarchy().getTopAnnotations());
    return new ProperType(atm, javaType, context);
  }

  /**
   * Returns an {@link InferenceType} for {@code java.util.List<T>}, where {@code T} is {@link
   * #typeVariable}.
   *
   * @return an inference type for {@code java.util.List<T>}
   */
  private AbstractType listOfTypeVariable() {
    TypeElement list = elements.getTypeElement("java.util.List");
    DeclaredType listOfT = context.modelTypes.getDeclaredType(list, typeVariable);
    AnnotatedTypeMirror atm = AnnotatedTypeMirror.createType(listOfT, typeFactory, false);
    atm.addMissingAnnotations(typeFactory.getQualifierHierarchy().getTopAnnotations());
    return InferenceType.create(atm, listOfT, theta, context);
  }

  /**
   * Returns the type variable of {@code java.util.Collections.singletonList}, which is a generic
   * method.
   *
   * @return the type variable of a generic method
   */
  private TypeVariable typeVariableOfSingletonList() {
    TypeElement collections = elements.getTypeElement("java.util.Collections");
    for (ExecutableElement method : ElementFilter.methodsIn(collections.getEnclosedElements())) {
      if (method.getSimpleName().contentEquals("singletonList")) {
        return (TypeVariable) method.getTypeParameters().get(0).asType();
      }
    }
    throw new AssertionError("java.util.Collections.singletonList not found");
  }

  /** A Java source file whose contents are held in a string. */
  private static class StringJavaFileObject extends SimpleJavaFileObject {

    /** The contents of the file. */
    private final String contents;

    /**
     * Creates a Java source file with the given contents.
     *
     * @param contents the contents of the file
     */
    StringJavaFileObject(String contents) {
      super(URI.create("string:///Dummy.java"), JavaFileObject.Kind.SOURCE);
      this.contents = contents;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return contents;
    }
  }
}
