package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that {@link InferenceResult} never hands out null in place of a map from type variable to
 * inferred type argument.
 *
 * <p>{@link InferenceResult#getTypeArgumentsForExpression} used to return {@code
 * results.get(expressionTree)}, which is null when {@code results} is non-empty but has no entry
 * for {@code expressionTree}. Its callers in {@code AnnotatedTypes.findTypeArguments} store that
 * value in a {@code TypeArguments} record, on which {@code AnnotatedTypeFactory} then calls {@code
 * isEmpty()} and {@code new HashMap<>(...)} without a null check. {@link
 * InferenceResult#swapTypeVariables} had the same problem: it iterated over {@code
 * results.get(tree)} without checking for null.
 */
public class InferenceResultTest {

  /** Creates a new InferenceResultTest. */
  public InferenceResultTest() {}

  /**
   * An {@link ExpressionTree} that is used only as a map key. {@link InferenceResult} stores trees
   * in a {@code HashMap} and never inspects them, so no other method needs an implementation.
   */
  private static class KeyOnlyExpressionTree implements ExpressionTree {

    /** Creates a new KeyOnlyExpressionTree. */
    KeyOnlyExpressionTree() {}

    @Override
    public Kind getKind() {
      return Kind.OTHER;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
      throw new UnsupportedOperationException("KeyOnlyExpressionTree.accept");
    }
  }

  /**
   * Returns an inference result that has one entry, whose key is not {@code absentTree}. This
   * models an inference problem in which type arguments were inferred for some expression, but not
   * for {@code absentTree}.
   *
   * @param absentTree a tree that the returned inference result has no entry for
   * @return an inference result with exactly one entry, which is not for {@code absentTree}
   */
  private static InferenceResult resultWithoutEntryFor(ExpressionTree absentTree) {
    InferenceResult result = new InferenceResult(Collections.emptyList(), false, false, "");
    result.getResults().put(new KeyOnlyExpressionTree(), new HashMap<>());
    Assert.assertFalse(result.getResults().isEmpty());
    Assert.assertFalse(result.getResults().containsKey(absentTree));
    return result;
  }

  /**
   * {@code getTypeArgumentsForExpression} returns an empty map, not null, for a tree that no type
   * arguments were inferred for.
   */
  @Test
  public void getTypeArgumentsForExpressionOfAbsentTree() {
    ExpressionTree absentTree = new KeyOnlyExpressionTree();
    InferenceResult result = resultWithoutEntryFor(absentTree);

    Map<TypeVariable, AnnotatedTypeMirror> typeArgs =
        result.getTypeArgumentsForExpression(absentTree);

    Assert.assertNotNull(
        "getTypeArgumentsForExpression returned null for a tree with no inferred type arguments",
        typeArgs);
    Assert.assertTrue(typeArgs.isEmpty());
  }

  /** {@code getTypeArgumentsForExpression} returns an empty map for the empty inference result. */
  @Test
  public void getTypeArgumentsForExpressionOfEmptyResult() {
    Map<TypeVariable, AnnotatedTypeMirror> typeArgs =
        InferenceResult.emptyResult().getTypeArgumentsForExpression(new KeyOnlyExpressionTree());

    Assert.assertNotNull(typeArgs);
    Assert.assertTrue(typeArgs.isEmpty());
  }

  /**
   * {@code swapTypeVariables} does not throw a {@code NullPointerException} for a tree that no type
   * arguments were inferred for.
   *
   * <p>The executable type must have at least one type variable; otherwise the loop that
   * dereferences the map does not execute.
   */
  @Test
  public void swapTypeVariablesOfAbsentTree() {
    AnnotatedExecutableType genericMethod = genericMethodType();
    Assert.assertFalse(genericMethod.getTypeVariables().isEmpty());

    ExpressionTree absentTree = new KeyOnlyExpressionTree();
    InferenceResult result = resultWithoutEntryFor(absentTree);

    Assert.assertSame(result, result.swapTypeVariables(genericMethod, absentTree));
  }

  /**
   * Returns the annotated type of {@code java.util.Collections.singletonList}, which is a generic
   * method, that is, one whose {@code getTypeVariables()} is non-empty.
   *
   * @return the annotated type of a generic method
   */
  private static AnnotatedExecutableType genericMethodType() {
    Context context = new Context();
    ProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
    JavaCompiler javac = JavaCompiler.instance(context);
    // The list of modules must be initialized before entering symbols.
    javac.initModules(com.sun.tools.javac.util.List.nil());
    javac.enterDone();

    ValueChecker checker = new ValueChecker();
    checker.init(env);
    AnnotatedTypeFactory typeFactory = new AnnotatedTypeFactory(checker);

    TypeElement collections = env.getElementUtils().getTypeElement("java.util.Collections");
    for (ExecutableElement method : ElementFilter.methodsIn(collections.getEnclosedElements())) {
      if (method.getSimpleName().contentEquals("singletonList")) {
        return (AnnotatedExecutableType)
            AnnotatedTypeMirror.createType(method.asType(), typeFactory, true);
      }
    }
    throw new AssertionError("java.util.Collections.singletonList not found");
  }
}
