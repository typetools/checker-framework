package org.checkerframework.framework.test.junit;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.checkerframework.javacutil.TreePathUtil;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link TreePathUtil}. */
public class TreePathUtilTest {

  /** Creates a new TreePathUtilTest. */
  public TreePathUtilTest() {}

  @Test
  public void instanceInitializersOfClass() {
    String source =
        String.join(
            System.lineSeparator(),
            "class MyClass {",
            "  int instanceField = 1;",
            "  int uninitializedField;",
            "  static int staticField = 2;",
            "  { instanceField = 3; }",
            "  static { staticField = 4; }",
            "}");
    Assert.assertEquals(List.of("instanceField", "{}"), instanceInitializers(source, "MyClass"));
  }

  @Test
  public void instanceInitializersOfEnum() {
    String source =
        String.join(
            System.lineSeparator(),
            "enum MyEnum {",
            "  CONSTANT;",
            "  int instanceField = 1;",
            "}");
    Assert.assertEquals(List.of("instanceField"), instanceInitializers(source, "MyEnum"));
  }

  /** A field of an interface is static, even though it has no {@code static} modifier. */
  @Test
  public void instanceInitializersOfInterface() {
    String source =
        String.join(
            System.lineSeparator(),
            "interface MyInterface {",
            "  int implicitlyStaticField = 1;",
            "  static int explicitlyStaticField = 2;",
            "}");
    Assert.assertEquals(Collections.emptyList(), instanceInitializers(source, "MyInterface"));
  }

  /** A field of an annotation type is static, even though it has no {@code static} modifier. */
  @Test
  public void instanceInitializersOfAnnotationType() {
    String source =
        String.join(
            System.lineSeparator(),
            "@interface MyAnnotation {",
            "  int implicitlyStaticField = 1;",
            "  String value();",
            "}");
    Assert.assertEquals(Collections.emptyList(), instanceInitializers(source, "MyAnnotation"));
  }

  /**
   * Returns a description of the instance initializers of the given class: the name of each field
   * whose declaration has an initializer, and {@code "{}"} for each instance initializer block.
   *
   * @param source the source code of a compilation unit
   * @param className the simple name of a class declared in {@code source}
   * @return a description of the instance initializers of the class
   */
  private List<String> instanceInitializers(String source, String className) {
    List<String> result = new ArrayList<>();
    for (TreePath initializer :
        TreePathUtil.getInstanceInitializers(pathToClass(source, className))) {
      Tree leaf = initializer.getLeaf();
      result.add(leaf instanceof VariableTree ? ((VariableTree) leaf).getName().toString() : "{}");
    }
    return result;
  }

  /**
   * Parses the given source code and returns the path to the declaration of the given class.
   *
   * @param source the source code of a compilation unit
   * @param className the simple name of a class declared in {@code source}
   * @return the path to the declaration of the class
   */
  private TreePath pathToClass(String source, String className) {
    SimpleJavaFileObject fileObject =
        new SimpleJavaFileObject(
            URI.create("string:///" + className + ".java"), SimpleJavaFileObject.Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
          }
        };
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    JavacTask task =
        (JavacTask)
            compiler.getTask(null, null, null, null, null, Collections.singletonList(fileObject));
    Iterable<? extends CompilationUnitTree> compilationUnits;
    try {
      compilationUnits = task.parse();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    for (CompilationUnitTree compilationUnit : compilationUnits) {
      TreePath found =
          new TreePathScanner<TreePath, Void>() {
            @Override
            public TreePath visitClass(ClassTree tree, Void p) {
              if (tree.getSimpleName().contentEquals(className)) {
                return getCurrentPath();
              }
              return super.visitClass(tree, p);
            }

            @Override
            public TreePath reduce(TreePath r1, TreePath r2) {
              return r1 != null ? r1 : r2;
            }
          }.scan(compilationUnit, null);
      if (found != null) {
        return found;
      }
    }
    throw new AssertionError("Did not find class " + className);
  }
}
