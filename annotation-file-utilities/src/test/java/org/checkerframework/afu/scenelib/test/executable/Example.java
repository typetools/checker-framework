package org.checkerframework.afu.scenelib.test.executable;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.Annotations;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.DefException;
import org.checkerframework.afu.scenelib.io.IndexFileParser;
import org.checkerframework.afu.scenelib.io.IndexFileWriter;

/**
 * Prints information about Tainted and NonNull annotations on a given class. Invoke as:
 *
 * <pre>
 * java Example <i>input.jaif</i> <i>ClassToProcess</i> <i>output.jaif</i>
 * </pre>
 */
// This is run by the Gradle task `testExample`.
public class Example {
  public static void main(String[] args) {
    AScene scene;

    if (!new File(args[0]).exists()) {
      try {
        throw new Error(
            String.format(
                "Cannot find file %s in directory %s", args[0], new File(".").getCanonicalPath()));
      } catch (IOException e) {
        throw new Error("This can't happen: ", e);
      }
    }

    // System.out.println("Reading in " + args[0]);
    try {
      scene = new AScene();
      IndexFileParser.parseFile(args[0], scene);
    } catch (IOException e) {
      e.printStackTrace(System.err);
      return;
    }

    System.out.println("Processing class " + args[1]);
    // Get a handle on the class
    AClass clazz = scene.classes.get(args[1]);
    if (clazz == null) {
      System.out.println("Class " + args[1] + " is not mentioned in annotation file " + args[0]);
      return;
    }

    for (Map.Entry<String, AMethod> me : clazz.methods.entrySet()) {
      AMethod method = me.getValue();

      Annotation rro = method.receiver.type.lookup("Tainted");
      if (rro == null) {
        System.out.println("Method " + me.getKey() + " might modify the receiver");
      } else {
        System.out.println("Method " + me.getKey() + " must not modify the receiver");
      }

      ATypeElement paramType1 = method.parameters.getVivify(0).type;
      Annotation p1nn = paramType1.lookup("NonNull");
      if (p1nn == null) {
        System.out.println("Annotating type of first parameter of " + me.getKey() + " nonnull");

        paramType1.tlAnnotationsHere.add(Annotations.aNonNull);
      }
    }

    // System.out.println("Writing out " + args[2]);
    try {
      IndexFileWriter.write(scene, Files.newBufferedWriter(Paths.get(args[2]), UTF_8));
    } catch (IOException e) {
      e.printStackTrace(System.err);
      return;
    } catch (DefException e) {
      e.printStackTrace(System.err);
      return;
    }

    System.out.println("Success.");
  }
}
