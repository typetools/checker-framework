package org.checkerframework.common.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeVisitor;
import org.checkerframework.framework.util.ExecUtil;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.DefaultAnnotationFormatter;
import org.plumelib.util.StringsPlume;

/**
 * TypeVisualizer prints AnnotatedTypeMirrors as a directed graph where each node is a type and an
 * arrow is a reference. Arrows are labeled with the relationship that reference represents (e.g. an
 * arrow marked "extends" starts from a type variable or wildcard type and points to the upper bound
 * of that type).
 *
 * <p>Currently, to use TypeVisualizer just insert an if statement somewhere that targets the type
 * you would like to print: e.g.
 *
 * <pre>{@code
 * if (type.getKind() == TypeKind.EXECUTABLE && type.toString().contains("methodToPrint")) {
 *     TypeVisualizer.drawToPng("/Users/jburke/Documents/tmp/method.png", type);
 * }
 * }</pre>
 *
 * Be sure to remove such statements before committing your changes.
 */
public class TypeVisualizer {

  /**
   * Creates a dot file at dest that contains a digraph for the structure of {@code type}.
   *
   * @param dest the destination dot file
   * @param type the type to be written
   */
  public static void drawToDot(File dest, AnnotatedTypeMirror type) {
    Drawing drawer = new Drawing("Type", type);
    drawer.draw(dest);
  }

  /**
   * Creates a dot file at dest that contains a digraph for the structure of {@code type}.
   *
   * @param dest the destination dot file, this string will be directly passed to new File(dest)
   * @param type the type to be written
   */
  public static void drawToDot(String dest, AnnotatedTypeMirror type) {
    drawToDot(new File(dest), type);
  }

  /**
   * Draws a dot file for type in a temporary directory then calls the "dot" program to convert that
   * file into a png at the location dest. This method will fail if a temp file can't be created.
   *
   * @param dest the destination png file
   * @param type the type to be written
   */
  public static void drawToPng(File dest, AnnotatedTypeMirror type) {
    try {
      File dotFile = File.createTempFile(dest.getName(), ".dot");
      drawToDot(dotFile, type);
      execDotToPng(dotFile, dest);

    } catch (Exception exc) {
      throw new RuntimeException(exc);
    }
  }

  /**
   * Draws a dot file for type in a temporary directory then calls the "dot" program to convert that
   * file into a png at the location dest. This method will fail if a temp file can't be created.
   *
   * @param dest the destination png file, this string will be directly passed to new File(dest)
   * @param type the type to be written
   */
  public static void drawToPng(String dest, AnnotatedTypeMirror type) {
    drawToPng(new File(dest), type);
  }

  /**
   * Converts the given dot file to a png file at the specified location. This method calls the
   * program "dot" from Runtime.exec and will fail if "dot" is not on your class path.
   *
   * @param dotFile the dot file to convert
   * @param pngFile the destination of the resultant png file
   */
  public static void execDotToPng(File dotFile, File pngFile) {
    String[] cmd =
        new String[] {"dot", "-Tpng", dotFile.getAbsolutePath(), "-o", pngFile.getAbsolutePath()};
    System.out.println("Printing dotFile: " + dotFile + " to loc: " + pngFile);
    System.out.flush();
    ExecUtil.execute(cmd, System.out, System.err);
  }

  /**
   * If the name of typeVariable matches one in the list of typeVarNames, then print typeVariable to
   * a dot file at {@code directory/varName}.
   *
   * @return true if the type variable was printed, otherwise false
   */
  public static boolean printTypevarToDotIfMatches(
      AnnotatedTypeVariable typeVariable, List<String> typeVarNames, String directory) {
    return printTypevarIfMatches(typeVariable, typeVarNames, directory, false);
  }

  /**
   * If the name of typeVariable matches one in the list of typeVarNames, then print typeVariable to
   * a png file at {@code directory/varName.png}.
   *
   * @return true if the type variable was printed, otherwise false
   */
  public static boolean printTypevarToPngIfMatches(
      AnnotatedTypeVariable typeVariable, List<String> typeVarNames, String directory) {
    return printTypevarIfMatches(typeVariable, typeVarNames, directory, true);
  }

  private static boolean printTypevarIfMatches(
      AnnotatedTypeVariable typeVariable,
      List<String> typeVarNames,
      String directory,
      boolean png) {
    String dirPath = directory.endsWith(File.separator) ? directory : directory + File.separator;
    String varName = typeVariable.getUnderlyingType().asElement().toString();

    if (typeVarNames.contains(varName)) {
      if (png) {
        TypeVisualizer.drawToPng(dirPath + varName + ".png", typeVariable);
      } else {
        TypeVisualizer.drawToDot(dirPath + varName + ".dot", typeVariable);
      }
      return true;
    }

    return false;
  }

  /**
   * This class exists because there is no LinkedIdentityHashMap.
   *
   * <p>Node is just a wrapper around type mirror that replaces .equals with referential equality.
   * This is done to preserve the order types were traversed so that printing will occur in a
   * hierarchical order. However, since there is no LinkedIdentityHashMap, it was easiest to just
   * create a wrapper that performed referential equality on types and use a LinkedHashMap.
   */
  private static class Node {
    /** The delegate; that is, the wrapped value. */
    private final @InternedDistinct AnnotatedTypeMirror type;

    /**
     * Create a new Node that wraps the given type.
     *
     * @param type the type that the newly-constructed Node represents
     */
    private Node(@FindDistinct AnnotatedTypeMirror type) {
      this.type = type;
    }

    @Override
    public int hashCode() {
      return type.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj instanceof Node) {
        return ((Node) obj).type == this.type;
      }

      return false;
    }
  }

  /**
   * Drawing visits a type and writes a dot file to the location specified. It contains data
   * structures to hold the intermediate dot information before printing.
   */
  private static class Drawing {
    /** A map from Node (type) to a dot string declaring that node. */
    private final Map<Node, String> nodes = new LinkedHashMap<>();

    /** List of connections between nodes. Lines refer to identifiers in nodes.values(). */
    private final List<String> lines = new ArrayList<>();

    private final String graphName;

    /** The type being drawn. */
    private final AnnotatedTypeMirror type;

    /** Used to identify nodes uniquely. This field is monotonically increasing. */
    private int nextId = 0;

    public Drawing(String graphName, AnnotatedTypeMirror type) {
      this.graphName = graphName;
      this.type = type;
    }

    public void draw(File file) {
      addNodes(type);
      addConnections();
      write(file);
    }

    private void addNodes(AnnotatedTypeMirror type) {
      new NodeDrawer().visit(type);
    }

    private void addConnections() {
      ConnectionDrawer connectionDrawer = new ConnectionDrawer();
      for (Node node : nodes.keySet()) {
        connectionDrawer.visit(node.type);
      }
    }

    /**
     * Write this to a file.
     *
     * @param file the file to write to
     */
    private void write(File file) {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
        writer.write("digraph " + graphName + "{");
        writer.newLine();
        for (String line : lines) {
          writer.write(line + ";");
          writer.newLine();
        }
        writer.write("}");
        writer.flush();
      } catch (IOException exc) {
        throw new BugInCF(exc, "Exception visualizing type:%nfile=%s%ntype=%s", file, type);
      }
    }

    /**
     * Connection drawer is used to add the connections between all the nodes created by the
     * NodeDrawer. It is not a scanner and is called on every node in the nodes map.
     */
    private class ConnectionDrawer implements AnnotatedTypeVisitor<Void, Void> {

      @Override
      public Void visit(AnnotatedTypeMirror type) {
        type.accept(this, null);
        return null;
      }

      @Override
      public Void visit(AnnotatedTypeMirror type, Void aVoid) {
        return visit(type);
      }

      @Override
      public Void visitDeclared(AnnotatedDeclaredType type, Void aVoid) {
        List<AnnotatedTypeMirror> typeArgs = type.getTypeArguments();
        for (int i = 0; i < typeArgs.size(); i++) {
          lines.add(connect(type, typeArgs.get(i)) + " " + makeTypeArgLabel(i));
        }
        return null;
      }

      @Override
      public Void visitIntersection(AnnotatedIntersectionType type, Void aVoid) {
        List<AnnotatedTypeMirror> bounds = type.getBounds();
        for (int i = 0; i < bounds.size(); i++) {
          lines.add(connect(type, bounds.get(i)) + " " + makeLabel("&"));
        }
        return null;
      }

      @Override
      public Void visitUnion(AnnotatedUnionType type, Void aVoid) {
        List<AnnotatedDeclaredType> alternatives = type.getAlternatives();
        for (int i = 0; i < alternatives.size(); i++) {
          lines.add(connect(type, alternatives.get(i)) + " " + makeLabel("|"));
        }
        return null;
      }

      @Override
      public Void visitExecutable(AnnotatedExecutableType type, Void aVoid) {

        ExecutableElement methodElem = type.getElement();
        lines.add(connect(type, type.getReturnType()) + " " + makeLabel("returns"));

        List<? extends TypeParameterElement> typeVarElems = methodElem.getTypeParameters();
        List<AnnotatedTypeVariable> typeVars = type.getTypeVariables();
        for (int i = 0; i < typeVars.size(); i++) {
          String typeVarName = typeVarElems.get(i).getSimpleName().toString();
          lines.add(connect(type, typeVars.get(i)) + " " + makeMethodTypeArgLabel(typeVarName));
        }

        if (type.getReceiverType() != null) {
          lines.add(connect(type, type.getReceiverType()) + " " + makeLabel("receiver"));
        }

        List<? extends VariableElement> paramElems = methodElem.getParameters();
        List<AnnotatedTypeMirror> params = type.getParameterTypes();
        for (int i = 0; i < params.size(); i++) {
          String paramName = paramElems.get(i).getSimpleName().toString();
          lines.add(connect(type, params.get(i)) + " " + makeParamLabel(paramName));
        }

        List<AnnotatedTypeMirror> thrown = type.getThrownTypes();
        for (int i = 0; i < thrown.size(); i++) {
          lines.add(connect(type, thrown.get(i)) + " " + makeThrownLabel(i));
        }

        return null;
      }

      @Override
      public Void visitArray(AnnotatedArrayType type, Void aVoid) {
        lines.add(connect(type, type.getComponentType()));
        return null;
      }

      @Override
      public Void visitTypeVariable(AnnotatedTypeVariable type, Void aVoid) {
        lines.add(connect(type, type.getUpperBound()) + " " + makeLabel("extends"));
        lines.add(connect(type, type.getLowerBound()) + " " + makeLabel("super"));
        return null;
      }

      @Override
      public Void visitPrimitive(AnnotatedPrimitiveType type, Void aVoid) {
        return null;
      }

      @Override
      public Void visitNoType(AnnotatedNoType type, Void aVoid) {
        return null;
      }

      @Override
      public Void visitNull(AnnotatedNullType type, Void aVoid) {
        return null;
      }

      @Override
      public Void visitWildcard(AnnotatedWildcardType type, Void aVoid) {
        lines.add(connect(type, type.getExtendsBound()) + " " + makeLabel("extends"));
        lines.add(connect(type, type.getSuperBound()) + " " + makeLabel("super"));
        return null;
      }

      private String connect(AnnotatedTypeMirror from, AnnotatedTypeMirror to) {
        return nodes.get(new Node(from)) + " -> " + nodes.get(new Node(to));
      }

      private String makeLabel(String text) {
        return "[label=\"" + text + "\"]";
      }

      private String makeTypeArgLabel(int argIndex) {
        return makeLabel("<" + argIndex + ">");
      }

      private String makeMethodTypeArgLabel(String paramName) {
        return makeLabel("<" + paramName + ">");
      }

      private String makeParamLabel(String paramName) {
        return makeLabel(paramName);
      }

      private String makeThrownLabel(int index) {
        return makeLabel("throws: " + index);
      }
    }

    /** The default annotation formatter. */
    private static final DefaultAnnotationFormatter annoFormatter =
        new DefaultAnnotationFormatter();

    /**
     * Scans types and adds a mapping from type to dot node declaration representing that type in
     * the enclosing drawing.
     */
    private class NodeDrawer implements AnnotatedTypeVisitor<Void, Void> {

      /** Create a new NodeDrawer. */
      public NodeDrawer() {}

      private void visitAll(List<? extends AnnotatedTypeMirror> types) {
        for (AnnotatedTypeMirror type : types) {
          visit(type);
        }
      }

      @Override
      public Void visit(AnnotatedTypeMirror type) {
        if (type != null) {
          type.accept(this, null);
        }

        return null;
      }

      @Override
      public Void visit(AnnotatedTypeMirror type, Void aVoid) {
        return visit(type);
      }

      @Override
      public Void visitDeclared(AnnotatedDeclaredType type, Void aVoid) {
        if (checkOrAdd(type)) {
          addLabeledNode(
              type,
              getAnnoStr(type)
                  + " "
                  + type.getUnderlyingType().asElement().getSimpleName()
                  + (type.getTypeArguments().isEmpty() ? "" : "<...>"),
              "shape=box");
          visitAll(type.getTypeArguments());
        }
        return null;
      }

      @Override
      public Void visitIntersection(AnnotatedIntersectionType type, Void aVoid) {
        if (checkOrAdd(type)) {
          addLabeledNode(type, getAnnoStr(type) + " Intersection", "shape=octagon");
          visitAll(type.getBounds());
        }

        return null;
      }

      @Override
      public Void visitUnion(AnnotatedUnionType type, Void aVoid) {
        if (checkOrAdd(type)) {
          addLabeledNode(type, getAnnoStr(type) + " Union", "shape=doubleoctagon");
          visitAll(type.getAlternatives());
        }
        return null;
      }

      @Override
      public Void visitExecutable(AnnotatedExecutableType type, Void aVoid) {
        if (checkOrAdd(type)) {
          addLabeledNode(type, makeMethodLabel(type), "shape=box,peripheries=2");

          visit(type.getReturnType());
          visitAll(type.getTypeVariables());

          visit(type.getReceiverType());
          visitAll(type.getParameterTypes());

          visitAll(type.getThrownTypes());

        } else {
          throw new BugInCF("Executable types should never be recursive%ntype=%s", type);
        }
        return null;
      }

      @Override
      public Void visitArray(AnnotatedArrayType type, Void aVoid) {
        if (checkOrAdd(type)) {
          addLabeledNode(type, getAnnoStr(type) + "[]");
          visit(type.getComponentType());
        }
        return null;
      }

      @Override
      public Void visitTypeVariable(AnnotatedTypeVariable type, Void aVoid) {
        if (checkOrAdd(type)) {
          addLabeledNode(
              type,
              getAnnoStr(type) + " " + type.getUnderlyingType().asElement().getSimpleName(),
              "shape=invtrapezium");
          visit(type.getUpperBound());
          visit(type.getLowerBound());
        }
        return null;
      }

      @Override
      public Void visitPrimitive(AnnotatedPrimitiveType type, Void aVoid) {
        if (checkOrAdd(type)) {
          addLabeledNode(type, getAnnoStr(type) + " " + type.getKind());
        }
        return null;
      }

      @Override
      public Void visitNoType(AnnotatedNoType type, Void aVoid) {
        if (checkOrAdd(type)) {
          addLabeledNode(type, getAnnoStr(type) + " None");
        }
        return null;
      }

      @Override
      public Void visitNull(AnnotatedNullType type, Void aVoid) {
        if (checkOrAdd(type)) {
          addLabeledNode(type, getAnnoStr(type) + " NullType");
        }
        return null;
      }

      @Override
      public Void visitWildcard(AnnotatedWildcardType type, Void aVoid) {
        if (checkOrAdd(type)) {
          addLabeledNode(type, getAnnoStr(type) + "?", "shape=trapezium");
          visit(type.getExtendsBound());
          visit(type.getSuperBound());
        }
        return null;
      }

      /**
       * Returns a string representation of the annotations on a type.
       *
       * @param atm an annotated type
       * @return a string representation of the annotations on {@code atm}
       */
      public String getAnnoStr(AnnotatedTypeMirror atm) {
        StringJoiner sj = new StringJoiner(" ");
        for (AnnotationMirror anno : atm.getAnnotations()) {
          // TODO: More comprehensive escaping
          sj.add(annoFormatter.formatAnnotationMirror(anno).replace("\"", "\\"));
        }
        return sj.toString();
      }

      public boolean checkOrAdd(AnnotatedTypeMirror atm) {
        Node node = new Node(atm);
        if (nodes.containsKey(node)) {
          return false;
        }
        nodes.put(node, String.valueOf(nextId++));
        return true;
      }

      public String makeLabeledNode(AnnotatedTypeMirror type, String label) {
        return makeLabeledNode(type, label, null);
      }

      public String makeLabeledNode(AnnotatedTypeMirror type, String label, String attributes) {
        String attr = (attributes != null) ? ", " + attributes : "";
        return nodes.get(new Node(type)) + " [label=\"" + label + "\"" + attr + "]";
      }

      public void addLabeledNode(AnnotatedTypeMirror type, String label) {
        lines.add(makeLabeledNode(type, label));
      }

      public void addLabeledNode(AnnotatedTypeMirror type, String label, String attributes) {
        lines.add(makeLabeledNode(type, label, attributes));
      }

      public String makeMethodLabel(AnnotatedExecutableType methodType) {
        ExecutableElement methodElem = methodType.getElement();

        StringBuilder builder = new StringBuilder();
        builder.append(methodElem.getReturnType().toString());
        builder.append(" <");

        builder.append(StringsPlume.join(", ", methodElem.getTypeParameters()));
        builder.append("> ");

        builder.append(methodElem.getSimpleName().toString());

        builder.append("(");
        builder.append(StringsPlume.join(",", methodElem.getParameters()));
        builder.append(")");
        return builder.toString();
      }
    }
  }
}
