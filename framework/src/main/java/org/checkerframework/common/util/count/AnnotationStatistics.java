package org.checkerframework.common.util.count;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeSet;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Name;
import javax.tools.Diagnostic;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SourceVisitor;
import org.checkerframework.framework.source.SupportedOptions;
import org.checkerframework.javacutil.AnnotationProvider;

/**
 * An annotation processor for counting the annotations in a program and for listing the potential
 * locations of annotations. To invoke it, use
 *
 * <pre>
 * javac -proc:only -processor org.checkerframework.common.util.count.AnnotationStatistics <em>MyFile.java ...</em>
 * </pre>
 *
 * <p>By default, this utility displays annotation locations only, but not the annotations
 * themselves. Further, the ouput includes all annotations (including {@code @Override}, etc.),
 * which is not very useful.
 *
 * <p>The following options may be used to adjust the output:
 *
 * <ul>
 *   <li>{@code -Aannotations}: prints the annotation name, the file that contains it, and whether
 *       it is in a signature or in a body
 *   <li>{@code -Anolocations}: suppresses location output; only makes sense in conjunction with
 *       {@code -Aannotations}
 *   <li>{@code -Aannotationsummaryonly}: with both of the above, only outputs a summary
 *   <li>{@code -Aannotationserror}: histogram is issued as a warning, not just printed
 * </ul>
 *
 * <p>These use cases are not very useful, because they include all annotations including
 * {@code @Override}, etc.
 *
 * <ul>
 *   <li>Output the locations of annotations, but not the annotations themselves: normal invocation,
 *       as above
 *   <li>Histogram of the locations of annotations, by location type: {@code ... | sort | uniq -c}
 *   <li>Total annotation count: {@code ... | wc}.
 *   <li>Count for only certain location types: use {@code grep}
 * </ul>
 *
 * @see JavaCodeStatistics
 * @see org.checkerframework.common.util.count.report.ReportChecker
 */
/*
 * TODO: add an option to only list declaration or type annotations.
 * This e.g. influences the output of "method return", which is only valid
 * for type annotations for non-void methods.
 */
@SupportedOptions({"nolocations", "annotations", "annotationserror", "annotationsummaryonly"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AnnotationStatistics extends SourceChecker {

  /**
   * Map from annotation name (as the {@code toString()} of its Name representation) to number of
   * times the annotation was written in source code.
   */
  final Map<String, Integer> annotationCount = new HashMap<>();

  /** Creates an AnnotationStatistics. */
  public AnnotationStatistics() {
    // This checker never issues any warnings, so don't warn about
    // @SuppressWarnings("allcheckers:...").
    this.useAllcheckersPrefix = false;
  }

  @Override
  public void typeProcessingOver() {
    Log log = getCompilerLog();
    String output;
    if (log.nerrors != 0) {
      output = "Not counting annotations, because compilation issued an error.";
    } else if (annotationCount.isEmpty()) {
      output = "No annotations found.";
    } else {
      StringJoiner sj = new StringJoiner(System.lineSeparator());
      sj.add("Found annotations: ");
      // alphabetize the annotations
      for (String key : new TreeSet<>(annotationCount.keySet())) {
        sj.add(key + "\t" + annotationCount.get(key));
      }
      output = sj.toString();
    }
    if (hasOption("annotationserror")) {
      // Issue annotation details a compiler warning rather than printed. This may be useful,
      // for example, when Maven swallows non-warning output from the annotation processor.
      getProcessingEnvironment().getMessager().printMessage(Diagnostic.Kind.WARNING, output);
    } else {
      System.out.println(output);
    }
    super.typeProcessingOver();
  }

  /** Increment the number of times annotation with name {@code annoName} has appeared. */
  protected void incrementCount(Name annoName) {
    String annoString = annoName.toString();
    if (!annotationCount.containsKey(annoString)) {
      annotationCount.put(annoString, 1);
    } else {
      annotationCount.put(annoString, annotationCount.get(annoString) + 1);
    }
  }

  @Override
  protected SourceVisitor<?, ?> createSourceVisitor() {
    return new Visitor(this);
  }

  class Visitor extends SourceVisitor<Void, Void> {

    /** True if annotation locations should be printed. */
    private final boolean locations;

    /** True if annotation details should be printed. */
    private final boolean annotations;

    /** True if only a summary should be printed. */
    private final boolean annotationsummaryonly;

    /**
     * Create a new Visitor.
     *
     * @param l the AnnotationStatistics object, used for obtaining command-line arguments
     */
    public Visitor(AnnotationStatistics l) {
      super(l);

      locations = !l.hasOption("nolocations");
      annotations = l.hasOption("annotations");
      annotationsummaryonly = l.hasOption("annotationsummaryonly");
    }

    @Override
    public Void visitAnnotation(AnnotationTree tree, Void p) {
      if (annotations) {
        Name annoName = ((JCAnnotation) tree).annotationType.type.tsym.getQualifiedName();
        incrementCount(annoName);

        // An annotation is a body annotation if, while ascending the AST from the
        // annotation to the root, we find a block immediately enclosed by a method.
        //
        // If an annotation is not a body annotation, it's a signature (declaration)
        // annotation.

        boolean isBodyAnnotation = false;
        TreePath path = getCurrentPath();
        Tree prev = null;
        for (Tree t : path) {
          if (prev != null && prev instanceof BlockTree && t instanceof MethodTree) {
            isBodyAnnotation = true;
            break;
          }
          prev = t;
        }

        if (!annotationsummaryonly) {
          System.out.printf(
              ":annotation %s %s %s %s%n",
              tree.getAnnotationType(),
              tree,
              root.getSourceFile().getName(),
              (isBodyAnnotation ? "body" : "sig"));
        }
      }
      return super.visitAnnotation(tree, p);
    }

    @Override
    public Void visitArrayType(ArrayTypeTree tree, Void p) {
      if (locations) {
        System.out.println("array type");
      }
      return super.visitArrayType(tree, p);
    }

    @Override
    public Void visitClass(ClassTree tree, Void p) {
      if (shouldSkipDefs(tree)) {
        // Not "return super.visitClass(classTree, p);" because that would recursively call
        // visitors on subtrees; we want to skip the class entirely.
        return null;
      }
      if (locations) {
        System.out.println("class");
        if (tree.getExtendsClause() != null) {
          System.out.println("class extends");
        }
        for (@SuppressWarnings("unused") Tree t : tree.getImplementsClause()) {
          System.out.println("class implements");
        }
      }
      return super.visitClass(tree, p);
    }

    @Override
    public Void visitMethod(MethodTree tree, Void p) {
      if (locations) {
        System.out.println("method return");
        System.out.println("method receiver");
        for (@SuppressWarnings("unused") Tree t : tree.getThrows()) {
          System.out.println("method throws");
        }
        for (@SuppressWarnings("unused") Tree t : tree.getParameters()) {
          System.out.println("method param");
        }
      }
      return super.visitMethod(tree, p);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void p) {
      if (locations) {
        System.out.println("variable");
      }
      return super.visitVariable(tree, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
      if (locations) {
        for (@SuppressWarnings("unused") Tree t : tree.getTypeArguments()) {
          System.out.println("method invocation type argument");
        }
      }
      return super.visitMethodInvocation(tree, p);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void p) {
      if (locations) {
        System.out.println("new class");
        for (@SuppressWarnings("unused") Tree t : tree.getTypeArguments()) {
          System.out.println("new class type argument");
        }
      }
      return super.visitNewClass(tree, p);
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, Void p) {
      if (locations) {
        System.out.println("new array");
        for (@SuppressWarnings("unused") Tree t : tree.getDimensions()) {
          System.out.println("new array dimension");
        }
      }
      return super.visitNewArray(tree, p);
    }

    @Override
    public Void visitTypeCast(TypeCastTree tree, Void p) {
      if (locations) {
        System.out.println("typecast");
      }
      return super.visitTypeCast(tree, p);
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree tree, Void p) {
      if (locations) {
        System.out.println("instanceof");
      }
      return super.visitInstanceOf(tree, p);
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree tree, Void p) {
      if (locations) {
        for (@SuppressWarnings("unused") Tree t : tree.getTypeArguments()) {
          System.out.println("parameterized type");
        }
      }
      return super.visitParameterizedType(tree, p);
    }

    @Override
    public Void visitTypeParameter(TypeParameterTree tree, Void p) {
      if (locations) {
        for (@SuppressWarnings("unused") Tree t : tree.getBounds()) {
          System.out.println("type parameter bound");
        }
      }
      return super.visitTypeParameter(tree, p);
    }

    @Override
    public Void visitWildcard(WildcardTree tree, Void p) {
      if (locations) {
        System.out.println("wildcard");
      }
      return super.visitWildcard(tree, p);
    }
  }

  @Override
  public AnnotationProvider getAnnotationProvider() {
    throw new UnsupportedOperationException(
        "getAnnotationProvider is not implemented for this class.");
  }
}
