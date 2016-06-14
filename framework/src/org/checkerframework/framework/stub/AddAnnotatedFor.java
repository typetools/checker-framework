package org.checkerframework.framework.stub;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import annotations.Annotation;
import annotations.Annotations;
import annotations.el.ABlock;
import annotations.el.AClass;
import annotations.el.ADeclaration;
import annotations.el.AElement;
import annotations.el.AExpression;
import annotations.el.AField;
import annotations.el.AMethod;
import annotations.el.AScene;
import annotations.el.ATypeElement;
import annotations.el.ATypeElementWithType;
import annotations.el.AnnotationDef;
import annotations.el.DefException;
import annotations.el.ElementVisitor;
import annotations.field.AnnotationFieldType;
import annotations.field.ArrayAFT;
import annotations.field.BasicAFT;
import annotations.io.IndexFileParser;
import annotations.io.IndexFileWriter;
import annotations.io.ParseException;

/**
 * Utility that generates {@code @AnnotatedFor} class annotations.
 * The {@link #main} method acts as a filter: it reads a JAIF from
 * standard input and writes an augmented JAIF to standard output.
 *
 * @author dbro
 */
public class AddAnnotatedFor {
  /** Definition of {@code @AnnotatedFor} annotation. */
  private static AnnotationDef adAnnotatedFor;

  static {
    Class<?> annotatedFor =
        org.checkerframework.framework.qual.AnnotatedFor.class;
    Set<Annotation> annotatedForMetaAnnotations = new HashSet<Annotation>();
    annotatedForMetaAnnotations.add(Annotations.aRetentionSource);
    annotatedForMetaAnnotations.add(Annotations.createValueAnnotation(
        Annotations.adTarget,
        Arrays.<String>asList("TYPE", "METHOD", "CONSTRUCTOR", "PACKAGE")));
    adAnnotatedFor = new AnnotationDef(annotatedFor.getCanonicalName(),
        annotatedForMetaAnnotations,
        Collections.<String, AnnotationFieldType>singletonMap("value",
            new ArrayAFT(BasicAFT.forType(String.class))));
  }

  /**
   * Reads JAIF from the file indicated by the first element, or
   * standard input if the argument array is empty; inserts any
   * appropriate {@code @AnnotatedFor} annotations, based on the
   * annotations defined in the input JAIF; and writes the augmented
   * JAIF to standard output.
   */
  public static void main(String[] args)
      throws IOException, DefException, ParseException {
    AScene scene = new AScene();
    Reader r = args.length > 0 ? new FileReader(args[0])
        : new InputStreamReader(System.in);
    IndexFileParser.parse(new LineNumberReader(r), scene);
    scene.prune();
    addAnnotatedFor(scene);
    IndexFileWriter.write(scene, new PrintWriter(System.out, true));
  }

  public static void addAnnotatedFor(AScene scene) {
    for (AClass clazz : new HashSet<AClass>(scene.classes.values())) {
      Set<String> annotatedFor = new HashSet<String>();
      clazz.accept(annotatedForVisitor, annotatedFor);
      if (!annotatedFor.isEmpty()) {
        // Set eliminates duplicates, but it must be converted to List;
        // for whatever reason, IndexFileWriter recognizes array
        // arguments only in List form.
        List<String> annotatedForList = new ArrayList<String>(annotatedFor);
        clazz.tlAnnotationsHere.add(
            new Annotation(adAnnotatedFor,
                Annotations.valueFieldOnly(annotatedForList)));
      }
    }
  }

  private static ElementVisitor<Void, Set<String>> annotatedForVisitor =
      new ElementVisitor<Void, Set<String>>() {
    @Override
    public Void visitAnnotationDef(AnnotationDef el,
        final Set<String> annotatedFor) {
      return null;
    }

    @Override
    public Void visitBlock(ABlock el,
        final Set<String> annotatedFor) {
      for (AField e : el.locals.values()) {
        e.accept(this, annotatedFor);
      }
      return visitExpression(el, annotatedFor);
    }

    @Override
    public Void visitClass(AClass el,
        final Set<String> annotatedFor) {
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
    public Void visitDeclaration(ADeclaration el,
        final Set<String> annotatedFor) {
      for (ATypeElement e : el.insertAnnotations.values()) {
        e.accept(this, annotatedFor);
      }
      for (ATypeElementWithType e : el.insertTypecasts.values()) {
        e.accept(this, annotatedFor);
      }
      return visitElement(el, annotatedFor);
    }

    @Override
    public Void visitExpression(AExpression el,
        final Set<String> annotatedFor) {
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
    public Void visitField(AField el,
        final Set<String> annotatedFor) {
      if (el.init != null) {
        el.init.accept(this, annotatedFor);
      }
      return visitDeclaration(el, annotatedFor);
    }

    @Override
    public Void visitMethod(AMethod el,
        final Set<String> annotatedFor) {
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
    public Void visitTypeElement(ATypeElement el,
        final Set<String> annotatedFor) {
      for (ATypeElement e : el.innerTypes.values()) {
        e.accept(this, annotatedFor);
      }
      return visitElement(el, annotatedFor);
    }

    @Override
    public Void visitTypeElementWithType(ATypeElementWithType el,
        final Set<String> annotatedFor) {
      return visitTypeElement(el, annotatedFor);
    }

    @Override
    public Void visitElement(AElement el,
        final Set<String> annotatedFor) {
      for (Annotation a : el.tlAnnotationsHere) {
        String s = a.def().name;
        int j = s.indexOf(".qual.");
        if (j > 0) {
          int i = s.lastIndexOf('.', j-1);
          if (i > 0 && j-i > 1) {
            annotatedFor.add(s.substring(i+1, j));
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
