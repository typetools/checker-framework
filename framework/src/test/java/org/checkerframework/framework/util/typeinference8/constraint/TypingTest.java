package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.net.URI;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.InferenceType;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the reduction of an equality constraint {@code ‹S = T›}, where {@code S} and {@code T} are
 * parameterized types. See JLS 18.2.4.
 *
 * <p>Such a constraint reduces to one constraint per pair of corresponding type arguments.
 *
 * <p>All the types in this test are built out of {@code java.util.stream.BaseStream}, which is
 * declared as {@code BaseStream<T, S extends BaseStream<T, S>>}. With {@code T} as the only
 * inference variable, {@code S} is a type variable that is not an inference variable but whose
 * bound mentions one, and so {@code java.util.List<S>} is an {@link InferenceType} whose only type
 * argument is an {@code InferenceType}. Inference itself creates such type variables when it
 * captures a wildcard.
 */
public class TypingTest {

  /** Creates a new TypingTest. */
  public TypingTest() {}

  /**
   * An {@link ExpressionTree} that stands in for the expression whose type arguments are being
   * inferred. An inference variable stores the tree but never inspects it.
   */
  private static class DummyExpressionTree implements ExpressionTree {

    /** Creates a new DummyExpressionTree. */
    DummyExpressionTree() {}

    @Override
    public Tree.Kind getKind() {
      return Tree.Kind.OTHER;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
      throw new UnsupportedOperationException("DummyExpressionTree.accept");
    }
  }

  /** A source file that is parsed only so that the inference context has a tree path. */
  private static class DummySourceFile extends SimpleJavaFileObject {

    /** Creates a new DummySourceFile. */
    DummySourceFile() {
      super(URI.create("string:///DummyClass.java"), JavaFileObject.Kind.SOURCE);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
      return "class DummyClass {}";
    }
  }

  /** The processing environment. Initialized by {@link #setUp}. */
  private ProcessingEnvironment env;

  /** The type factory. Initialized by {@link #setUp}. */
  private AnnotatedTypeFactory typeFactory;

  /** The inference context. Initialized by {@link #setUp}. */
  private Java8InferenceContext context;

  /**
   * A mapping in which {@code BaseStream}'s type parameter {@code T} is an inference variable and
   * its type parameter {@code S} is not. Initialized by {@link #setUp}.
   */
  private Theta theta;

  /** {@code BaseStream}'s type parameter {@code S}. Initialized by {@link #setUp}. */
  private TypeVariable typeVariableS;

  /** The type {@code java.util.List<S>}. Initialized by {@link #setUp}. */
  private DeclaredType listOfS;

  /** The type {@code java.util.List<T>}. Initialized by {@link #setUp}. */
  private DeclaredType listOfT;

  /** Initializes this test's fields. */
  private void setUp() {
    Context javacContext = new Context();
    env = JavacProcessingEnvironment.instance(javacContext);
    JavaCompiler javac = JavaCompiler.instance(javacContext);
    // The list of modules must be initialized before entering symbols.
    javac.initModules(com.sun.tools.javac.util.List.nil());
    JCCompilationUnit compilationUnit = javac.parse(new DummySourceFile());
    javac.enterDone();

    ValueChecker checker = new ValueChecker();
    checker.init(env);
    typeFactory = new ValueAnnotatedTypeFactory(checker);

    // The inference context needs a path to an expression only to determine the enclosing class
    // and to report errors, neither of which this test does.
    TreePath path =
        new TreePath(new TreePath(compilationUnit), compilationUnit.getTypeDecls().get(0));
    context = new Java8InferenceContext(typeFactory, path, null);

    TypeElement baseStream = env.getElementUtils().getTypeElement("java.util.stream.BaseStream");
    TypeVariable t = (TypeVariable) baseStream.getTypeParameters().get(0).asType();
    TypeVariable s = (TypeVariable) baseStream.getTypeParameters().get(1).asType();

    // Create an inference variable for each type parameter of BaseStream, then discard the one for
    // S. During inference, some of the type variables in scope are inference variables and some
    // are not.
    AnnotatedDeclaredType baseStreamType = typeFactory.getAnnotatedType(baseStream);
    ProperType capturedType =
        new ProperType(baseStreamType, baseStreamType.getUnderlyingType(), context);
    theta =
        context.inferenceTypeFactory.createThetaForCapture(new DummyExpressionTree(), capturedType);
    Assert.assertTrue(theta.containsKey(t));
    Assert.assertTrue(theta.containsKey(s));
    theta.remove(s);
    typeVariableS = s;

    TypeElement list = env.getElementUtils().getTypeElement("java.util.List");
    listOfS = env.getTypeUtils().getDeclaredType(list, s);
    listOfT = env.getTypeUtils().getDeclaredType(list, t);
  }

  /**
   * Returns an abstract type for {@code javaType}. Each call returns a new object, so that two
   * calls with the same argument return two types that are equal but not identical.
   *
   * @param javaType a Java type
   * @return an abstract type for {@code javaType}
   */
  private AbstractType abstractTypeFor(TypeMirror javaType) {
    AnnotatedTypeMirror atm = AnnotatedTypeMirror.createType(javaType, typeFactory, false);
    return InferenceType.create(atm, javaType, theta, context);
  }

  /** Reducing {@code ‹S = S›} yields true, as required by JLS 18.2.4. */
  @Test
  public void reduceEqualityOfSameTypeVariableIsTrue() {
    setUp();
    AbstractType s = abstractTypeFor(typeVariableS);
    AbstractType t = abstractTypeFor(typeVariableS);
    Assert.assertTrue("expected an inference type, found " + s.getKind(), s.isInferenceType());
    Assert.assertNotSame(s, t);

    ReductionResult result = new Typing("test", s, t, Kind.TYPE_EQUALITY).reduce(context);

    Assert.assertSame("expected true, found " + result, ConstraintSet.TRUE, result);
  }

  /** Reducing {@code ‹List<S> = List<S>›} creates no constraint. */
  @Test
  public void reduceEqualityCreatesNoConstraintForEqualTypeArguments() {
    setUp();
    AbstractType s = abstractTypeFor(listOfS);
    AbstractType t = abstractTypeFor(listOfS);
    Assert.assertTrue("expected an inference type, found " + s.getKind(), s.isInferenceType());
    Assert.assertNotSame(s, t);
    Assert.assertEquals(s, t);

    ReductionResult result = new Typing("test", s, t, Kind.TYPE_EQUALITY).reduce(context);

    Assert.assertTrue(
        "expected a constraint set, found " + result, result instanceof ConstraintSet);
    Assert.assertTrue(
        "expected no constraints, found " + result, ((ConstraintSet) result).isEmpty());
  }

  /**
   * Reducing {@code ‹List<S> = List<S>›} does not reduce to false.
   *
   * <p>If reducing the constraint between the two type arguments yielded false, {@code
   * ConstraintSet#reduceOneStep} would report that by throwing {@code FalseBoundException}, so this
   * test would fail with an exception rather than at its assertion.
   */
  @Test
  public void reduceEqualityOfEqualTypeArgumentsIsNotFalse() {
    setUp();
    AbstractType s = abstractTypeFor(listOfS);
    AbstractType t = abstractTypeFor(listOfS);

    ConstraintSet constraints = new ConstraintSet(new Typing("test", s, t, Kind.TYPE_EQUALITY));
    BoundSet boundSet = constraints.reduce(context);

    Assert.assertFalse(boundSet.containsFalse());
  }

  /** Reducing {@code ‹List<S> = List<T>›} creates a constraint for the two type arguments. */
  @Test
  public void reduceEqualityCreatesConstraintForDifferentTypeArguments() {
    setUp();
    AbstractType s = abstractTypeFor(listOfS);
    AbstractType t = abstractTypeFor(listOfT);
    Assert.assertNotEquals(s, t);

    ReductionResult result = new Typing("test", s, t, Kind.TYPE_EQUALITY).reduce(context);

    Assert.assertTrue(
        "expected a constraint set, found " + result, result instanceof ConstraintSet);
    Assert.assertFalse("expected a constraint, found none", ((ConstraintSet) result).isEmpty());
  }
}
