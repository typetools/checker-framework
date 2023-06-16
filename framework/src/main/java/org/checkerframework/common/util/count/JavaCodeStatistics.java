package org.checkerframework.common.util.count;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.tools.javac.util.Log;
import java.util.List;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SourceVisitor;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * An annotation processor for counting the size of Java code:
 *
 * <ul>
 *   <li>The number of type parameter declarations and uses.
 *   <li>The number of array accesses and dimensions in array creations.
 *   <li>The number of type casts.
 * </ul>
 *
 * <p>To invoke it, use
 *
 * <pre>
 * javac -proc:only -processor org.checkerframework.common.util.count.JavaCodeStatistics <em>MyFile.java ...</em>
 * </pre>
 *
 * @see AnnotationStatistics
 */
@AnnotatedFor("nullness")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class JavaCodeStatistics extends SourceChecker {

  /** The number of type parameter declarations and uses. */
  int generics = 0;

  /** The number of array accesses and dimensions in array creations. */
  int arrayAccesses = 0;

  /** The number of type casts. */
  int typecasts = 0;

  String[] warningKeys = {
    "index", "lowerbound", "samelen", "searchindex", "substringindex", "upperbound"
  };

  /**
   * The number of warning suppressions with at least one key that matches one of the Index Checker
   * subcheckers.
   */
  int numberOfIndexWarningSuppressions = 0;

  /** The SuppressWarnings.value field/element. */
  final ExecutableElement suppressWarningsValueElement =
      TreeUtils.getMethod(SuppressWarnings.class, "value", 0, processingEnv);

  /** Creates a JavaCodeStatistics. */
  public JavaCodeStatistics() {
    // This checker never issues any warnings, so don't warn about
    // @SuppressWarnings("allcheckers:...").
    this.useAllcheckersPrefix = false;
  }

  @Override
  public void typeProcessingOver() {
    Log log = getCompilerLog();
    if (log.nerrors != 0) {
      System.out.printf("Not outputting statistics, because compilation issued an error.%n");
    } else {
      System.out.printf("Found %d generic type uses.%n", generics);
      System.out.printf("Found %d array accesses and creations.%n", arrayAccesses);
      System.out.printf("Found %d typecasts.%n", typecasts);
      System.out.printf(
          "Found %d warning suppression annotations for the Index Checker.%n",
          numberOfIndexWarningSuppressions);
    }
    super.typeProcessingOver();
  }

  @Override
  protected SourceVisitor<?, ?> createSourceVisitor() {
    return new Visitor(this);
  }

  class Visitor extends SourceVisitor<Void, Void> {

    public Visitor(JavaCodeStatistics l) {
      super(l);
    }

    @Override
    public Void visitAnnotation(AnnotationTree tree, Void aVoid) {
      AnnotationMirror annotationMirror = TreeUtils.annotationFromAnnotationTree(tree);
      if (AnnotationUtils.annotationName(annotationMirror)
          .equals(SuppressWarnings.class.getCanonicalName())) {
        List<String> keys =
            AnnotationUtils.getElementValueArray(
                annotationMirror, suppressWarningsValueElement, String.class);
        for (String foundKey : keys) {
          for (String indexKey : warningKeys) {
            if (foundKey.startsWith(indexKey)) {
              numberOfIndexWarningSuppressions++;
              return super.visitAnnotation(tree, aVoid);
            }
          }
        }
      }
      return super.visitAnnotation(tree, aVoid);
    }

    @Override
    public Void visitAssert(AssertTree tree, Void aVoid) {
      ExpressionTree detail = tree.getDetail();
      if (detail != null) {
        String msg = detail.toString();
        for (String indexKey : warningKeys) {
          String key = "@AssumeAssertion(" + indexKey;
          if (msg.contains(key)) {
            numberOfIndexWarningSuppressions++;
            return super.visitAssert(tree, aVoid);
          }
        }
      }
      return super.visitAssert(tree, aVoid);
    }

    @Override
    public Void visitClass(ClassTree tree, Void p) {
      if (shouldSkipDefs(tree)) {
        // Not "return super.visitClass(classTree, p);" because that would recursively call
        // visitors on subtrees; we want to skip the class entirely.
        return null;
      }
      generics += tree.getTypeParameters().size();
      return super.visitClass(tree, p);
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, Void aVoid) {
      arrayAccesses += tree.getDimensions().size();

      return super.visitNewArray(tree, aVoid);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void aVoid) {
      if (TreeUtils.isDiamondTree(tree)) {
        generics++;
      }
      generics += tree.getTypeArguments().size();
      return super.visitNewClass(tree, aVoid);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void aVoid) {
      generics += tree.getTypeArguments().size();
      return super.visitMethodInvocation(tree, aVoid);
    }

    @Override
    public Void visitMethod(MethodTree tree, Void aVoid) {
      generics += tree.getTypeParameters().size();
      return super.visitMethod(tree, aVoid);
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree tree, Void p) {
      generics += tree.getTypeArguments().size();
      return super.visitParameterizedType(tree, p);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree tree, Void aVoid) {
      arrayAccesses++;
      return super.visitArrayAccess(tree, aVoid);
    }

    @Override
    public Void visitTypeCast(TypeCastTree tree, Void aVoid) {
      typecasts++;
      return super.visitTypeCast(tree, aVoid);
    }
  }

  @Override
  public AnnotationProvider getAnnotationProvider() {
    throw new UnsupportedOperationException(
        "getAnnotationProvider is not implemented for this class.");
  }
}
