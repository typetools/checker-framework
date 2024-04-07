package org.checkerframework.framework.stub;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.Annotations;
import org.checkerframework.afu.scenelib.el.ABlock;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.ADeclaration;
import org.checkerframework.afu.scenelib.el.AElement;
import org.checkerframework.afu.scenelib.el.AExpression;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.ATypeElementWithType;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.el.DefException;
import org.checkerframework.afu.scenelib.el.ElementVisitor;
import org.checkerframework.afu.scenelib.field.ArrayAFT;
import org.checkerframework.afu.scenelib.field.BasicAFT;
import org.checkerframework.afu.scenelib.io.IndexFileParser;
import org.checkerframework.afu.scenelib.io.IndexFileWriter;
import org.checkerframework.afu.scenelib.io.ParseException;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.plumelib.util.ArraySet;

/**
 * Utility that generates {@code @AnnotatedFor} class annotations. The {@link #main} method acts as
 * a filter: it reads a JAIF from standard input and writes an augmented JAIF to standard output.
 */
public class AddAnnotatedFor {
  /** Definition of {@code @AnnotatedFor} annotation. */
  private static final AnnotationDef adAnnotatedFor;

  static {
    Class<?> annotatedFor = org.checkerframework.framework.qual.AnnotatedFor.class;
    Set<Annotation> annotatedForMetaAnnotations = new HashSet<>(2);
    annotatedForMetaAnnotations.add(Annotations.aRetentionSource);
    annotatedForMetaAnnotations.add(
        Annotations.createValueAnnotation(
            Annotations.adTarget, Arrays.asList("TYPE", "METHOD", "CONSTRUCTOR", "PACKAGE")));
    @SuppressWarnings(
        "signature") // TODO bug: AnnotationDef requires @BinaryName, gets CanonicalName
    @BinaryName String name = annotatedFor.getCanonicalName();
    adAnnotatedFor =
        new AnnotationDef(
            name,
            annotatedForMetaAnnotations,
            Collections.singletonMap("value", new ArrayAFT(BasicAFT.forType(String.class))),
            "AddAnnotatedFor.<clinit>");
  }

  /** Do not instantiate. */
  private AddAnnotatedFor() {
    throw new Error("Do not instantiate");
  }

  /**
   * Reads JAIF from the file indicated by the first element, or standard input if the argument
   * array is empty; inserts any appropriate {@code @AnnotatedFor} annotations, based on the
   * annotations defined in the input JAIF; and writes the augmented JAIF to standard output.
   *
   * @param args one jaif file, or empty to read from standard input
   * @throws IOException if there is trouble reading or writing a file
   * @throws DefException if two definitions cannot be unified
   * @throws ParseException if the file is malformed
   */
  public static void main(String[] args) throws IOException, DefException, ParseException {
    if (args.length > 1) {
      System.err.println("Supply 0 or 1 command-line arguments.");
      System.exit(1);
    }
    AScene scene = new AScene();
    boolean useFile = args.length == 1;
    String filename = useFile ? args[0] : "System.in";
    try (Reader r =
        useFile
            ? Files.newBufferedReader(Paths.get(filename), UTF_8)
            : new InputStreamReader(System.in, UTF_8)) {
      IndexFileParser.parse(new LineNumberReader(r), filename, scene);
    }
    scene.prune();
    addAnnotatedFor(scene);
    IndexFileWriter.write(
        scene,
        new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8)), true));
  }

  /**
   * Add {@code @AnnotatedFor} annotations to each class in the given scene.
   *
   * @param scene an {@code @AnnotatedFor} annotation is added to each class in this scene
   */
  public static void addAnnotatedFor(AScene scene) {
    for (AClass clazz : new HashSet<>(scene.classes.values())) {
      Set<String> annotatedFor = new ArraySet<>(2); // usually few @AnnotatedFor are applicable
      clazz.accept(annotatedForVisitor, annotatedFor);
      if (!annotatedFor.isEmpty()) {
        // Set eliminates duplicates, but it must be converted to List; for whatever reason,
        // IndexFileWriter recognizes array arguments only in List form.
        List<String> annotatedForList = new ArrayList<>(annotatedFor);
        clazz.tlAnnotationsHere.add(
            new Annotation(adAnnotatedFor, Annotations.valueFieldOnly(annotatedForList)));
      }
    }
  }

  /**
   * This visitor collects the names of all the type systems, one of whose annotations is written.
   * These need to be the arguments to an {@code AnnotatedFor} annotation on the class, so that all
   * of the given type systems are run.
   */
  private static final ElementVisitor<Void, Set<String>> annotatedForVisitor =
      new ElementVisitor<Void, Set<String>>() {
        @Override
        public Void visitAnnotationDef(AnnotationDef el, Set<String> annotatedFor) {
          return null;
        }

        @Override
        public Void visitBlock(ABlock el, Set<String> annotatedFor) {
          for (AField e : el.locals.values()) {
            e.accept(this, annotatedFor);
          }
          return visitExpression(el, annotatedFor);
        }

        @Override
        public Void visitClass(AClass el, Set<String> annotatedFor) {
          for (ATypeElement e : el.bounds.values()) {
            e.accept(this, annotatedFor);
          }
          for (ATypeElement e : el.extendsImplements.values()) {
            e.accept(this, annotatedFor);
          }
          for (AExpression e : el.fieldInits.values()) {
            e.accept(this, annotatedFor);
          }
          for (AField e : el.fields.values()) {
            e.accept(this, annotatedFor);
          }
          for (ABlock e : el.instanceInits.values()) {
            e.accept(this, annotatedFor);
          }
          for (AMethod e : el.methods.values()) {
            e.accept(this, annotatedFor);
          }
          for (ABlock e : el.staticInits.values()) {
            e.accept(this, annotatedFor);
          }
          return visitDeclaration(el, annotatedFor);
        }

        @Override
        public Void visitDeclaration(ADeclaration el, Set<String> annotatedFor) {
          for (ATypeElement e : el.insertAnnotations.values()) {
            e.accept(this, annotatedFor);
          }
          for (ATypeElementWithType e : el.insertTypecasts.values()) {
            e.accept(this, annotatedFor);
          }
          return visitElement(el, annotatedFor);
        }

        @Override
        public Void visitExpression(AExpression el, Set<String> annotatedFor) {
          for (ATypeElement e : el.calls.values()) {
            e.accept(this, annotatedFor);
          }
          for (AMethod e : el.funs.values()) {
            e.accept(this, annotatedFor);
          }
          for (ATypeElement e : el.instanceofs.values()) {
            e.accept(this, annotatedFor);
          }
          for (ATypeElement e : el.news.values()) {
            e.accept(this, annotatedFor);
          }
          for (ATypeElement e : el.refs.values()) {
            e.accept(this, annotatedFor);
          }
          for (ATypeElement e : el.typecasts.values()) {
            e.accept(this, annotatedFor);
          }
          return visitElement(el, annotatedFor);
        }

        @Override
        public Void visitField(AField el, Set<String> annotatedFor) {
          if (el.init != null) {
            el.init.accept(this, annotatedFor);
          }
          return visitDeclaration(el, annotatedFor);
        }

        @Override
        public Void visitMethod(AMethod el, Set<String> annotatedFor) {
          if (el.body != null) {
            el.body.accept(this, annotatedFor);
          }
          if (el.receiver != null) {
            el.receiver.accept(this, annotatedFor);
          }
          if (el.returnType != null) {
            el.returnType.accept(this, annotatedFor);
          }
          for (ATypeElement e : el.bounds.values()) {
            e.accept(this, annotatedFor);
          }
          for (AField e : el.parameters.values()) {
            e.accept(this, annotatedFor);
          }
          for (ATypeElement e : el.throwsException.values()) {
            e.accept(this, annotatedFor);
          }
          return visitDeclaration(el, annotatedFor);
        }

        @Override
        public Void visitTypeElement(ATypeElement el, Set<String> annotatedFor) {
          for (ATypeElement e : el.innerTypes.values()) {
            e.accept(this, annotatedFor);
          }
          return visitElement(el, annotatedFor);
        }

        @Override
        public Void visitTypeElementWithType(ATypeElementWithType el, Set<String> annotatedFor) {
          return visitTypeElement(el, annotatedFor);
        }

        @Override
        public Void visitElement(AElement el, Set<String> annotatedFor) {
          for (Annotation a : el.tlAnnotationsHere) {
            String s = a.def().name;
            int j = s.indexOf(".qual.");
            if (j > 0) {
              int i = s.lastIndexOf('.', j - 1);
              if (i > 0 && j - i > 1) {
                annotatedFor.add(s.substring(i + 1, j));
              }
            }
          }
          if (el.type != null) {
            el.type.accept(this, annotatedFor);
          }
          return null;
        }
      };
}
