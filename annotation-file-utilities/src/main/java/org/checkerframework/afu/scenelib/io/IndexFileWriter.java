package org.checkerframework.afu.scenelib.io;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.AnnotationBuilder;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.AElement;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.ATypeElementWithType;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.el.BoundLocation;
import org.checkerframework.afu.scenelib.el.DefCollector;
import org.checkerframework.afu.scenelib.el.DefException;
import org.checkerframework.afu.scenelib.el.LocalLocation;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.afu.scenelib.el.TypeIndexLocation;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.afu.scenelib.field.AnnotationAFT;
import org.checkerframework.afu.scenelib.field.AnnotationFieldType;
import org.checkerframework.afu.scenelib.field.ArrayAFT;
import org.checkerframework.afu.scenelib.field.BasicAFT;
import org.checkerframework.afu.scenelib.field.ClassTokenAFT;
import org.checkerframework.afu.scenelib.type.ArrayType;
import org.checkerframework.afu.scenelib.type.BoundedType;
import org.checkerframework.afu.scenelib.type.DeclaredType;
import org.checkerframework.afu.scenelib.type.Type;
import org.checkerframework.afu.scenelib.util.Strings;
import org.objectweb.asm.TypePath;

/**
 * IndexFileWriter provides two static methods named {@code write} that write a given {@link AScene}
 * to a given {@link Writer} or filename, in index file format.
 */
public final class IndexFileWriter {
  final AScene scene;

  private static final String INDENT = "    ";

  void printAnnotationDefBody(AnnotationDef d) {
    for (Map.Entry<String, AnnotationFieldType> f : d.fieldTypes.entrySet()) {
      String fieldname = f.getKey();
      AnnotationFieldType fieldType = f.getValue();
      pw.println(INDENT + fieldType + " " + fieldname);
    }
    pw.println();
  }

  /** A DefCollector that prints all the annotation definitions. */
  private class OurDefCollector extends DefCollector {
    /**
     * Creates a new OurDefCollector.
     *
     * @throws DefException if the scene contains two irreconcilable definitions of the same
     *     annotation type
     */
    OurDefCollector() throws DefException {
      super(IndexFileWriter.this.scene);
    }

    @Override
    protected void visitAnnotationDef(AnnotationDef d) {
      if (!d.name.contains("+")) {
        pw.println("package " + IOUtils.packagePart(d.name) + ":");
        pw.print("annotation @" + IOUtils.basenamePart(d.name) + ":");
        // TODO: We would only print Retention and Target annotations
        printAnnotations(requiredMetaannotations(d.tlAnnotationsHere));
        pw.println();
        printAnnotationDefBody(d);
      }
    }

    private Collection<Annotation> requiredMetaannotations(Collection<Annotation> annos) {
      Set<Annotation> results = new HashSet<>();
      for (Annotation a : annos) {
        String aName = a.def.name;
        if (aName.equals(Retention.class.getCanonicalName())
            || aName.equals(Target.class.getCanonicalName())) {
          results.add(a);
        }
      }
      return results;
    }
  }

  /** The printer onto which the index file will be written. */
  final PrintWriter pw;

  /**
   * Print the annotation using the {@link #pw} field, after formatting it.
   *
   * @param a the annotation to print
   */
  private void printAnnotation(Annotation a) {
    StringBuilder sb = new StringBuilder();
    formatAnnotation(sb, a);
    pw.print(sb.toString());
  }

  private void printAnnotations(Collection<? extends Annotation> annos) {
    for (Annotation tla : annos) {
      if (!tla.def.name.contains("+")) {
        pw.print(' ');
        printAnnotation(tla);
      }
    }
  }

  /**
   * Print the annotations on the given AElement.
   *
   * @param e a program element
   */
  private void printAnnotations(AElement e) {
    printAnnotations(e.tlAnnotationsHere);
    if (e instanceof AMethod) {
      printAnnotations(((AMethod) e).contracts);
    }
  }

  private void printElement(String indentation, String descriptor, AElement e) {
    pw.print(indentation + descriptor + ":");
    printAnnotations(e);
    pw.println();
  }

  private void printElementAndInnerTypes(String indentation, String descriptor, AElement e) {
    if (e.type != null) {
      printElement(indentation, descriptor, e.type);
      if (!e.type.innerTypes.isEmpty()) {
        printInnerTypes(indentation + INDENT, e.type);
      }
    }
  }

  private void printTypeElementAndInnerTypes(
      String indentation, String descriptor, ATypeElement e) {
    if (e.tlAnnotationsHere.isEmpty() && e.innerTypes.isEmpty() && descriptor.equals("type")) {
      return;
    }
    printElement(indentation, descriptor, e);
    printInnerTypes(indentation + INDENT, e);
  }

  /**
   * Prints the innerType annotations for an ATypeELement.
   *
   * @param indentation string containing indentation spaces
   * @param e ATypeElement to search for innner ty pes
   */
  private void printInnerTypes(String indentation, ATypeElement e) {
    for (Map.Entry<List<TypePathEntry>, ATypeElement> ite : e.innerTypes.entrySet()) {
      TypePath typePath = TypePathEntry.listToTypePath(ite.getKey());
      AElement it = ite.getValue();
      pw.print(indentation + "inner-type");
      char sep = ' ';
      // TODO: Long term: Should change representation to mirror org.objectweb.asm.TypePath
      for (int index = 0; typePath != null && index < typePath.getLength(); index++) {
        pw.print(sep);
        pw.print(typePathStepToString(typePath, index));
        sep = ',';
      }
      pw.print(':');
      printAnnotations(it);
      pw.println();
    }
  }

  /**
   * Converts the given {@link TypePathEntry} to a string of the form {@code tag, arg}, where tag
   * and arg are both integers.
   *
   * @param typePath TypePath to be processed
   * @param index dentifies the TypePathEntry to convert
   * @return string representing the TypePathEntry
   */
  private String typePathStepToString(TypePath typePath, int index) {
    int typePathStep = typePath.getStep(index);
    int typePathStepArgument =
        typePathStep == TypePath.TYPE_ARGUMENT ? typePath.getStepArgument(index) : 0;
    return typePathStep + ", " + typePathStepArgument;
  }

  /**
   * Outputs a string representaion of a set of AElements to a PrintWriter.
   *
   * @param indentation string containing indentation spaces
   * @param descriptor description of Type being printed
   * @param nels map containing AElements to be printed
   */
  private void printNumberedAmbigiousElements(
      String indentation, String descriptor, Map<Integer, ? extends AElement> nels) {
    for (Map.Entry<Integer, ? extends AElement> te : nels.entrySet()) {
      AElement t = te.getValue();
      printAmbElementAndInnerTypes(indentation, descriptor + " #" + te.getKey(), t);
    }
  }

  /**
   * Outputs a string representaion of an AElement to a PrintWriter.
   *
   * @param indentation string containing indentation spaces
   * @param descriptor description of Type being printed
   * @param e AElement to be printed
   */
  private void printAmbElementAndInnerTypes(String indentation, String descriptor, AElement e) {
    printElement(indentation, descriptor, e);
    if (e.type.tlAnnotationsHere.isEmpty() && e.type.innerTypes.isEmpty()) {
      return;
    }
    printElement(indentation + INDENT, "type", e.type);
    for (Map.Entry<List<TypePathEntry>, ATypeElement> ite : e.type.innerTypes.entrySet()) {
      TypePath typePath = TypePathEntry.listToTypePath(ite.getKey());
      AElement it = ite.getValue();
      pw.print(indentation + INDENT + INDENT + "inner-type");
      boolean first = true;
      for (int index = 0; index < typePath.getLength(); index++) {
        if (first) {
          pw.print(' ');
        } else {
          pw.print(',');
        }
        pw.print(typePathStepToString(typePath, index));
        first = false;
      }
      pw.print(':');
      printAnnotations(it);
      pw.println();
    }
  }

  private void printRelativeElements(
      String indentation, String descriptor, Map<RelativeLocation, ATypeElement> nels) {
    for (Map.Entry<RelativeLocation, ATypeElement> te : nels.entrySet()) {
      ATypeElement t = te.getValue();
      printTypeElementAndInnerTypes(
          indentation, descriptor + " " + te.getKey().getLocationString(), t);
    }
  }

  private void printRelativeElements(
      String indentation, String desc1, String desc2, Map<RelativeLocation, ATypeElement> nels) {
    RelativeLocation prev = null;
    for (Map.Entry<RelativeLocation, ATypeElement> te : nels.entrySet()) {
      ATypeElement t = te.getValue();
      RelativeLocation loc = te.getKey();
      boolean isOffset = loc.index < 0;
      if (prev == null
          || loc.type_index < 0
          || (isOffset ? loc.offset != prev.offset : loc.index != prev.index)) {
        pw.print(indentation + desc1 + " ");
        pw.print(isOffset ? "#" + loc.offset : "*" + loc.index);
        pw.print(":");
        if (loc.type_index <= 0) {
          printAnnotations(t);
        }
        pw.println();
        printInnerTypes(indentation + INDENT, t);
      }
      if (loc.type_index > 0) {
        printTypeElementAndInnerTypes(indentation + INDENT, desc2 + " " + loc.type_index, t);
      }
      prev = loc;
    }
  }

  private void printBounds(String indentation, Map<BoundLocation, ATypeElement> bounds) {
    for (Map.Entry<BoundLocation, ATypeElement> be : bounds.entrySet()) {
      BoundLocation bl = be.getKey();
      ATypeElement b = be.getValue();
      if (bl.boundIndex == -1) {
        printTypeElementAndInnerTypes(indentation, "typeparam " + bl.paramIndex, b);
      } else {
        printTypeElementAndInnerTypes(
            indentation, "bound " + bl.paramIndex + " &" + bl.boundIndex, b);
      }
    }
  }

  private void printExtImpls(String indentation, Map<TypeIndexLocation, ATypeElement> extImpls) {

    for (Map.Entry<TypeIndexLocation, ATypeElement> ei : extImpls.entrySet()) {
      TypeIndexLocation idx = ei.getKey();
      ATypeElement ty = ei.getValue();
      // reading from a short into an integer does not preserve sign?
      if (idx.typeIndex == -1 || idx.typeIndex == 65535) {
        printTypeElementAndInnerTypes(indentation, "extends", ty);
      } else {
        printTypeElementAndInnerTypes(indentation, "implements " + idx.typeIndex, ty);
      }
    }
  }

  private void printASTInsertions(
      String indentation,
      Map<ASTPath, ATypeElement> insertAnnotations,
      Map<ASTPath, ATypeElementWithType> insertTypecasts) {
    for (Map.Entry<ASTPath, ATypeElement> e : insertAnnotations.entrySet()) {
      ASTPath path = e.getKey();
      ATypeElement el = e.getValue();
      pw.print(indentation + "insert-annotation " + path + ":");
      printAnnotations(el);
      pw.println();
      printInnerTypes(INDENT, el);
    }
    for (Map.Entry<ASTPath, ATypeElementWithType> e : insertTypecasts.entrySet()) {
      ASTPath path = e.getKey();
      ATypeElementWithType el = e.getValue();
      pw.print(indentation + "insert-typecast " + path + ":");
      printAnnotations(el);
      pw.print(" ");
      printType(el.getType());
      pw.println();
      printInnerTypes(INDENT, el);
    }
  }

  private void printType(Type type) {
    switch (type.getKind()) {
      case ARRAY:
        ArrayType a = (ArrayType) type;
        printType(a.getComponentType());
        pw.print("[]");
        break;
      case BOUNDED:
        BoundedType b = (BoundedType) type;
        printType(b.getName());
        pw.print(" ");
        pw.print(b.getBoundKind());
        pw.print(" ");
        printType(b.getBound());
        break;
      case DECLARED:
        DeclaredType d = (DeclaredType) type;
        pw.print(d.getName());
        if (!d.isWildcard()) {
          DeclaredType inner = d.getInnerType();
          Iterator<Type> iter = d.getTypeParameters().iterator();
          // for (String s : d.getAnnotations()) {
          //    pw.print(s + " ");
          // }
          if (iter.hasNext()) {
            pw.print("<");
            printType(iter.next());
            while (iter.hasNext()) {
              pw.print(", ");
              printType(iter.next());
            }
            pw.print(">");
          }
          if (inner != null) {
            pw.print(".");
            printType(inner);
          }
        }
        break;
    }
  }

  private void write() throws DefException {
    // First the annotation definitions...
    OurDefCollector odc = new OurDefCollector();
    odc.visit();

    // Then any package org.checkerframework.afu.scenelib...
    for (Map.Entry<String, AElement> pe : scene.packages.entrySet()) {
      AElement elem = pe.getValue();
      if (elem != null && !elem.tlAnnotationsHere.isEmpty()) {
        pw.print("package " + pe.getKey() + ":");
        printAnnotations(elem);
        pw.println();
      }
    }

    // And then the annotated classes
    final String indent2 = INDENT + INDENT;
    final String indent3 = INDENT + indent2;
    for (Map.Entry<String, AClass> ce : scene.classes.entrySet()) {
      String cname = ce.getKey();
      AClass c = ce.getValue();
      String pkg = IOUtils.packagePart(cname);
      String basename = IOUtils.basenamePart(cname);
      if ("package-info".equals(basename)) {
        if (!c.tlAnnotationsHere.isEmpty()) {
          pw.print("package " + pkg + ":");
          printAnnotations(c);
          pw.println();
        }
        continue;
      } else {
        pw.println("package " + pkg + ":");
        pw.print("class " + basename + ":");
        printAnnotations(c);
        pw.println();
      }

      printBounds(INDENT, c.bounds);
      printExtImpls(INDENT, c.extendsImplements);
      printASTInsertions(INDENT, c.insertAnnotations, c.insertTypecasts);

      for (Map.Entry<String, AField> fe : c.fields.entrySet()) {
        String fname = fe.getKey();
        AField f = fe.getValue();
        pw.println();
        printElement(INDENT, "field " + fname, f);
        printTypeElementAndInnerTypes(indent2, "type", f.type);
        printASTInsertions(indent2, f.insertAnnotations, f.insertTypecasts);
      }
      for (Map.Entry<String, AMethod> me : c.methods.entrySet()) {
        String mkey = me.getKey();
        AMethod m = me.getValue();
        pw.println();
        printElement(INDENT, "method " + mkey, m);
        printBounds(indent2, m.bounds);
        printTypeElementAndInnerTypes(indent2, "return", m.returnType);
        if (!m.receiver.type.tlAnnotationsHere.isEmpty() || !m.receiver.type.innerTypes.isEmpty()) {
          // Only output the receiver if there is something to
          // say.  This is a bit inconsistent with the return
          // type, but so be it.
          printElementAndInnerTypes(indent2, "receiver", m.receiver);
        }
        printNumberedAmbigiousElements(indent2, "parameter", m.parameters);
        for (Map.Entry<LocalLocation, AField> le : m.body.locals.entrySet()) {
          LocalLocation loc = le.getKey();
          AElement l = le.getValue();
          StringBuilder sb = new StringBuilder("local ");
          sb.append(
              loc.variableName == null
                  ? loc.getVarIndex() + " #" + loc.getScopeStart() + "+" + loc.getScopeLength()
                  : loc.variableName);
          printElement(indent2, sb.toString(), l);
          printTypeElementAndInnerTypes(indent3, "type", l.type);
        }
        printRelativeElements(indent2, "typecast", m.body.typecasts);
        printRelativeElements(indent2, "instanceof", m.body.instanceofs);
        printRelativeElements(indent2, "new", m.body.news);
        printRelativeElements(indent2, "reference", "typearg", m.body.refs);
        printRelativeElements(indent2, "call", "typearg", m.body.calls);
        for (Map.Entry<RelativeLocation, AMethod> entry : m.body.funs.entrySet()) {
          AMethod lambda = entry.getValue();
          RelativeLocation loc = entry.getKey();
          pw.print("lambda " + loc.getLocationString() + ":\n");
          printBounds(indent3, lambda.bounds);
          printTypeElementAndInnerTypes(indent3, "return", lambda.returnType);
        }
        // throwsException field is not processed.  Why?
        printASTInsertions(indent2, m.insertAnnotations, m.insertTypecasts);
      }
      pw.println();
    }
  }

  private IndexFileWriter(AScene scene, Writer out) throws DefException {
    this.scene = scene;
    pw = new PrintWriter(out);
    write();
    pw.flush();
  }

  /**
   * Formats an annotation to be printed in an index file.
   *
   * @param sb where to format the annotation to
   * @param a the annotation to format
   */
  private static void formatAnnotation(StringBuilder sb, Annotation a) {
    sb.append("@");
    sb.append(a.def().name);
    if (a.fieldValues.isEmpty()) {
      return;
    }

    sb.append("(");
    boolean notfirst = false;
    for (Map.Entry<String, Object> f : a.fieldValues.entrySet()) {
      if (notfirst) {
        sb.append(",");
      } else {
        notfirst = true;
      }
      AnnotationFieldType aft = a.def().fieldTypes.get(f.getKey());
      sb.append(f.getKey());
      sb.append("=");
      formatAnnotationValue(sb, aft, f.getValue());
    }
    sb.append(")");
  }

  // TODO: Why isn't this just aft.format(o)??
  /**
   * Formats a literal argument of an annotation. Public to permit re-use in stub-based
   * whole-program inference.
   *
   * @param aft the type of the annotation field
   * @param o the value or values to format
   * @return the String representation of the value
   */
  @Deprecated // TEMPORORY
  public static String formatAnnotationValue(AnnotationFieldType aft, Object o) {
    StringBuilder sb = new StringBuilder();
    formatAnnotationValue(sb, aft, o);
    return sb.toString();
  }

  /**
   * Formats a literal argument of an annotation. Public to permit re-use in stub-based
   * whole-program inference.
   *
   * @param sb where to format the arguments to
   * @param aft the type of the annotation field
   * @param o the value or values to format
   */
  public static void formatAnnotationValue(StringBuilder sb, AnnotationFieldType aft, Object o) {
    if (aft instanceof AnnotationAFT) {
      formatAnnotation(sb, (Annotation) o);
    } else if (aft instanceof ArrayAFT) {
      ArrayAFT aaft = (ArrayAFT) aft;
      List<?> l = (List<?>) o;
      sb.append("{");
      if (aaft.elementType == null) {
        if (!l.isEmpty()) {
          throw new AssertionError("nonempty array of unknown type");
        }
      } else {
        boolean notfirst = false;
        for (Object o2 : l) {
          if (notfirst) {
            sb.append(",");
          } else {
            notfirst = true;
          }
          formatAnnotationValue(sb, aaft.elementType, o2);
        }
      }
      sb.append("}");
    } else if (aft instanceof ClassTokenAFT) {
      aft.format(sb, o);
    } else if (aft instanceof BasicAFT && o instanceof String) {
      sb.append(Strings.escape((String) o));
    } else if (aft instanceof BasicAFT && o instanceof Long) {
      sb.append(o.toString());
      sb.append("L");
      // This causes assertion failures.  I'm not sure why.
      // else if (aft instanceof EnumAFT) {
      //     aft.format(sb, o);
    } else {
      sb.append(o.toString());
    }
  }

  /**
   * Writes the annotations in {@code scene} and their definitions to {@code out} in index file
   * format.
   *
   * <p>An {@link AScene} can contain several annotations of the same type but different
   * definitions, while an index file can accommodate only a single definition for each annotation
   * type. This has two consequences:
   *
   * <ul>
   *   <li>Before writing anything, this method uses a {@link DefCollector} to ensure that all
   *       definitions of each annotation type are identical (modulo unknown array types). If not, a
   *       {@link DefException} is thrown.
   *   <li>There is one case in which, even if a scene is written successfully, reading it back in
   *       produces a different scene. Consider a scene containing two annotations of type Foo, each
   *       with an array field bar. In one annotation, bar is empty and of unknown element type (see
   *       {@link AnnotationBuilder#addEmptyArrayField}); in the other, bar is of known element
   *       type. This method will {@linkplain AnnotationDef#unify unify} the two definitions of Foo
   *       by writing a single definition with known element type. When the index file is read into
   *       a new scene, the definitions of both annotations will have known element type, whereas in
   *       the original scene, one had unknown element type.
   * </ul>
   */
  public static void write(AScene scene, Writer out) throws DefException {
    new IndexFileWriter(scene, out);
  }

  /**
   * Writes the annotations in {@code scene} and their definitions to the file {@code filename} in
   * index file format; see {@link #write(AScene, Writer)}.
   */
  public static void write(AScene scene, String filename) throws IOException, DefException {
    try (Writer w = Files.newBufferedWriter(Paths.get(filename), StandardCharsets.UTF_8)) {
      write(scene, w);
    }
  }
}
