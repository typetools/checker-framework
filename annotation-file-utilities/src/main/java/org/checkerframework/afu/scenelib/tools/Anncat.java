package org.checkerframework.afu.scenelib.tools;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.DefException;
import org.checkerframework.afu.scenelib.io.IndexFileParser;
import org.checkerframework.afu.scenelib.io.IndexFileWriter;
import org.checkerframework.afu.scenelib.io.JavapParser;
import org.checkerframework.afu.scenelib.io.classfile.ClassFileReader;
import org.checkerframework.afu.scenelib.io.classfile.ClassFileWriter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.plumelib.util.FileIOException;

/** Concatenates multiple descriptions of annotations into a single one. */
public class Anncat {

  /** Do not instantiate. */
  private Anncat() {
    throw new Error("Do not instantiate");
  }

  /** Print a usage message to standard error. */
  private static void usage() {
    System.err.println("anncat, part of the Annotation File Utilities");
    System.err.println("(https://checkerframework.org/annotation-file-utilities/)");
    System.err.println("usage: anncat <inspec>* [ --out <outspec> ], where:");
    System.err.println("    <inspec> ::=");
    System.err.println("        ( --javap <in.javap> )");
    System.err.println("        | ( --index <in.jaif> )");
    System.err.println("        | ( --class <in.class> )");
    System.err.println("    <outspec> ::=");
    System.err.println("        ( --index <out.jaif> )");
    System.err.println("        | ( --class [ --overwrite ] <orig.class> [ --to <out.class> ] )");
    System.err.println("If outspec is omitted, default is index file to stdout.");
  }

  private static void usageAssert(boolean b) {
    if (!b) {
      System.err.println("*** Usage error ***");
      usage();
      System.exit(3);
    }
  }

  public static void main(String[] args) {
    usageAssert(0 < args.length);
    if (args[0].equals("--help")) {
      usage();
      System.exit(0);
    }

    try {

      int idx = 0;

      @NonNull AScene theScene = new AScene();

      // Read the scene
      while (idx < args.length && !args[idx].equals("--out")) {
        if (args[idx].equals("--javap")) {
          idx++;
          usageAssert(idx < args.length);
          String infile = args[idx++];
          System.out.println("Reading javap file " + infile + "...");
          JavapParser.parse(infile, theScene);
          System.out.println("Finished.");
        } else if (args[idx].equals("--index")) {
          idx++;
          usageAssert(idx < args.length);
          String infile = args[idx++];
          System.err.println("Reading index file " + infile + "...");
          IndexFileParser.parseFile(infile, theScene);
          System.err.println("Finished.");
        } else if (args[idx].equals("--class")) {
          idx++;
          usageAssert(idx < args.length);
          String infile = args[idx++];
          System.err.println("Reading class file " + infile + "...");
          ClassFileReader.read(theScene, infile);
          System.err.println("Finished.");
        } else {
          usageAssert(false);
        }
      }

      // Write the scene
      if (idx == args.length) {
        System.err.println("Writing index file to standard output...");
        IndexFileWriter.write(theScene, new OutputStreamWriter(System.out, UTF_8));
        System.err.println("Finished.");
      } else {
        idx++;
        usageAssert(idx < args.length);
        if (args[idx].equals("--index")) {
          idx++;
          usageAssert(idx < args.length);
          String outfile = args[idx];
          idx++;
          usageAssert(idx == args.length);
          System.err.println("Writing index file to " + outfile + "...");
          // In Java 11, use: new FileWriter(outfile, UTF_8)
          try (Writer w = Files.newBufferedWriter(Paths.get(outfile), UTF_8)) {
            IndexFileWriter.write(theScene, w);
          }
          System.err.println("Finished.");
        } else if (args[idx].equals("--class")) {
          idx++;
          usageAssert(idx < args.length);
          boolean overwrite;
          if (args[idx].equals("--overwrite")) {
            System.err.println("Overwrite mode enabled.");
            overwrite = true;
            idx++;
            usageAssert(idx < args.length);
          } else {
            overwrite = false;
          }
          String origfile = args[idx];
          idx++;
          if (idx < args.length) {
            usageAssert(args[idx].equals("--to"));
            idx++;
            usageAssert(idx < args.length);
            String outfile = args[idx];
            idx++;
            usageAssert(idx == args.length);
            System.err.println("Reading original class file " + origfile);
            System.err.println("and writing annotated version to " + outfile + "...");
            try (FileInputStream fis = new FileInputStream(origfile);
                FileOutputStream fos = new FileOutputStream(outfile)) {
              ClassFileWriter.insert(theScene, fis, fos, overwrite);
            }
            System.err.println("Finished.");
          } else {
            System.err.println("Rewriting class file " + origfile + " with annotations...");
            ClassFileWriter.insert(theScene, null, origfile, overwrite);
            System.err.println("Finished.");
          }
        } else {
          usageAssert(false);
        }
      }

    } catch (FileIOException e) {
      e.printStackTrace(System.err);
      System.exit(1);
    } catch (IOException e) {
      e.printStackTrace(System.err);
      System.exit(2);
    } catch (DefException e) {
      e.printStackTrace(System.err);
      System.exit(1);
    }
    System.exit(0);
  }
}
