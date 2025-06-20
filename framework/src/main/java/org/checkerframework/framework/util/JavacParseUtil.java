package org.checkerframework.framework.util;

import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.JavacTool;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import javax.tools.*;

public class JavacParseUtil {

  public static ExpressionTree parseExpression(String expressionSource) {
    String dummySource = "class Dummy { Object expression = " + expressionSource + "; }";

    JavaCompiler compiler = JavacTool.create();
    JavaFileObject fileObject =
        new SimpleJavaFileObject(URI.create("string:///Dummy.java"), JavaFileObject.Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return dummySource;
          }
        };

    try {
      StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
      JavacTask task =
          (JavacTask)
              compiler.getTask(
                  null,
                  fileManager,
                  null,
                  Collections.emptyList(),
                  null,
                  Collections.singletonList(fileObject));

      Iterable<? extends CompilationUnitTree> trees = task.parse();
      CompilationUnitTree cu = trees.iterator().next();

      ExpressionExtractor extractor = new ExpressionExtractor();
      for (Tree typeDecl : cu.getTypeDecls()) {
        typeDecl.accept(extractor, null);
      }

      if (extractor.result == null) {
        throw new RuntimeException("Expression not found in AST.");
      }

      return extractor.result;

    } catch (IOException e) {
      throw new RuntimeException("Expression Parsing failed", e);
    }
  }

  private static class ExpressionExtractor extends TreeScanner<Void, Void> {
    ExpressionTree result = null;

    @Override
    public Void visitVariable(VariableTree node, Void p) {
      if (node.getName().contentEquals("expression")) {
        result = node.getInitializer();
      }
      return super.visitVariable(node, p);
    }
  }
}
