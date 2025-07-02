package org.checkerframework.afu.scenelib.tools;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
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
import org.checkerframework.afu.scenelib.field.AnnotationFieldType;
import org.checkerframework.afu.scenelib.io.IndexFileParser;
import org.checkerframework.afu.scenelib.io.IndexFileWriter;
import org.checkerframework.afu.scenelib.util.CommandLineUtils;
import org.plumelib.util.FileIOException;

/** Utility for merging index files, including multiple versions for the same class. */
public class IndexFileMerger {
  @SuppressWarnings("CatchAndPrintStackTrace") // TODO
  public static void main(String[] args) {
    if (args.length < 1) {
      System.exit(0);
    }

    final SetMultimap<String, String> annotatedFor = HashMultimap.create();
    String[] inputArgs;

    // TODO: document assumptions
    // collect annotations into scene
    try {
      try {
        inputArgs = CommandLineUtils.parseCommandLine(args);
      } catch (Exception ex) {
        System.err.println(ex);
        System.err.println("(For non-argfile beginning with \"@\", use \"@@\" for initial \"@\".");
        System.err.println("Alternative for filenames: indicate directory, e.g. as './@file'.");
        System.err.println("Alternative for flags: use '=', as in '-o=@Deprecated'.)");
        System.exit(1);
        return; // so compiler knows inputArgs defined after try/catch
      }

      File baseFile = new File(inputArgs[0]);
      boolean byDir = baseFile.isDirectory();
      String basePath = baseFile.getCanonicalPath();
      AScene scene = new AScene();

      for (int i = byDir ? 1 : 0; i < inputArgs.length; i++) {
        File inputFile = new File(inputArgs[i]);
        String inputPath = inputFile.getCanonicalPath();
        String filename = inputFile.getName();

        if (byDir) {
          if (!(filename.endsWith(".jaif") || filename.endsWith("jann"))) {
            System.err.println("WARNING: ignoring non-JAIF " + filename);
            continue;
          }
          if (!inputPath.startsWith(basePath)) {
            System.err.println("WARNING: ignoring file outside base directory " + filename);
            continue;
          }

          // note which base subdirectory JAIF came from
          String relPath = inputPath.substring(basePath.length() + 1); // +1 '/'
          int ix = relPath.indexOf(File.separator);
          String subdir = ix < 0 ? relPath : relPath.substring(0, ix);
          // trim .jaif or .jann and subdir, convert directory to package id
          String classname =
              relPath
                  .substring(0, relPath.lastIndexOf('.'))
                  .substring(relPath.indexOf('/') + 1)
                  .replace(File.separator, ".");
          annotatedFor.put(classname, "\"" + subdir + "\"");
        }

        try {
          IndexFileParser.parseFile(inputPath, scene);
        } catch (FileNotFoundException e) {
          System.err.println("IndexFileMerger: can't read " + inputPath);
          System.exit(1);
        } catch (FileIOException e) {
          e.printStackTrace();
          System.exit(1);
        }
      }

      if (!byDir) {
        /*
                // collect defs
                Map<String, String> annoPkgs = new HashMap<>();
                try {
                  new DefCollector(scene) {
                    @Override
                    protected void visitAnnotationDef(AnnotationDef d) {
                      String[] a = d.name.split("\\.");
                      if (a.length > 2 && a[a.length-2].matches("quals?")) {
                        String s = a[a.length-1];
                        annoPkgs.put(s, d.name.substring(0));
                      }
                    }
                  }.visit();
                } catch (DefException e) {
                  System.err.println("DefCollector failed!");
                  e.printStackTrace();
                  System.exit(1);
                }
        */

        for (Map.Entry<String, AClass> entry : scene.classes.entrySet()) {
          // final String classname = entry.getKey();

          entry
              .getValue()
              .accept(
                  new ElementVisitor<Void, Void>() {
                    // Map<String, String> annoPkgs = new HashMap<>();

                    // Void process(AElement el) {
                    //  for (Annotation anno : el.tlAnnotationsHere) {
                    //    AnnotationDef def = anno.def();
                    //    String[] a = def.name.split("\\.");
                    //    if ("AnnotatedFor".equals(a[a.length-1])) {
                    //      @SuppressWarnings("unchecked")
                    //      List<String> vals =
                    //          (List<String>) anno.getFieldValue("value");
                    //      for (String val : vals) {
                    //        annotatedFor.put(classname, val);
                    //      }
                    //    } else if (a.length > 2 && a[a.length-2].matches("quals?")) {
                    //      annotatedFor.put(classname, a[a.length-3]);
                    //    }
                    //  }
                    //  return null;
                    // }

                    Void visit(AElement el) {
                      if (el != null) {
                        el.accept(this, null);
                      }
                      return null;
                    }

                    <T, E extends AElement> Void visitMap(Map<T, E> map) {
                      if (map != null) {
                        for (E el : map.values()) {
                          visit(el);
                        }
                      }
                      return null;
                    }

                    @Override
                    public Void visitAnnotationDef(AnnotationDef d, Void v) {
                      // String[] a = d.name.split("\\.");
                      // if (a.length > 2 && a[a.length-2].matches("quals?")) {
                      //  String s = a[a.length-1];
                      //  annoPkgs.put(s, d.name.substring(0));
                      // }
                      return null; // process(d);
                    }

                    @Override
                    public Void visitBlock(ABlock el, Void v) {
                      visitMap(el.locals);
                      return visitExpression(el, null);
                    }

                    @Override
                    public Void visitClass(AClass el, Void v) {
                      visitMap(el.bounds);
                      visitMap(el.extendsImplements);
                      visitMap(el.instanceInits);
                      visitMap(el.staticInits);
                      visitMap(el.methods);
                      visitMap(el.fields);
                      visitMap(el.fieldInits);
                      return visitDeclaration(el, null);
                    }

                    @Override
                    public Void visitDeclaration(ADeclaration el, Void v) {
                      visitMap(el.insertAnnotations);
                      visitMap(el.insertTypecasts);
                      return visitElement(el, null);
                    }

                    @Override
                    public Void visitExpression(AExpression el, Void v) {
                      visitMap(el.calls);
                      visitMap(el.funs);
                      visitMap(el.instanceofs);
                      visitMap(el.news);
                      visitMap(el.refs);
                      visitMap(el.typecasts);
                      return visitElement(el, null);
                    }

                    @Override
                    public Void visitField(AField el, Void v) {
                      visit(el.init);
                      return visitDeclaration(el, null);
                    }

                    @Override
                    public Void visitMethod(AMethod el, Void v) {
                      visit(el.receiver);
                      visitMap(el.parameters);
                      visitMap(el.bounds);
                      visit(el.returnType);
                      visit(el.body);
                      visitMap(el.throwsException);
                      return visitDeclaration(el, null);
                    }

                    @Override
                    public Void visitTypeElement(ATypeElement el, Void v) {
                      visitMap(el.innerTypes);
                      return visitElement(el, null);
                    }

                    @Override
                    public Void visitTypeElementWithType(ATypeElementWithType el, Void v) {
                      return visitTypeElement(el, null);
                    }

                    @Override
                    public Void visitElement(AElement el, Void v) {
                      visit(el.type);
                      return null; // process(el);
                    }
                  },
                  null);
        }
      }

      // add AnnotatedFor to each annotated class
      AnnotationFieldType stringArray =
          AnnotationFieldType.fromClass(
              new String[0].getClass(), Collections.<String, AnnotationDef>emptyMap());
      AnnotationDef afDef =
          Annotations.createValueAnnotationDef(
              "AnnotatedFor", Collections.<Annotation>emptySet(), stringArray, "IndexFileMerger");
      for (Map.Entry<String, Collection<String>> entry : annotatedFor.asMap().entrySet()) {
        String key = entry.getKey();
        Collection<String> values = entry.getValue();
        Annotation afAnno =
            new Annotation(
                afDef, Collections.<String, Collection<String>>singletonMap("value", values));
        scene.classes.getVivify(key).tlAnnotationsHere.add(afAnno);
      }
      scene.prune();
      annotatedFor.clear(); // for gc

      try {
        IndexFileWriter.write(
            scene,
            new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8)),
                true));
      } catch (SecurityException e) {
        e.printStackTrace();
        System.exit(1);
      } catch (DefException e) {
        e.printStackTrace();
        System.exit(1);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
