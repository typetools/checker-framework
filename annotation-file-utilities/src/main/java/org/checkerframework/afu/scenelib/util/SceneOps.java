package org.checkerframework.afu.scenelib.util;

import com.sun.tools.javac.util.Pair;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
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
import org.checkerframework.afu.scenelib.io.IndexFileParser;
import org.checkerframework.afu.scenelib.io.IndexFileWriter;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;

/**
 * Algebraic operations on scenes.
 *
 * <p>Also includes a {@link #main(String[])} method that lets these operations be performed from
 * the command line.
 */
public class SceneOps {
  private SceneOps() {}

  /**
   * Run an operation on a subcommand-specific number of JAIFs. Currently the only available
   * subcommand is "diff", which must be the first of three arguments, followed in order by the
   * "minuend" and the "subtrahend" (see {@link #diff(AScene, AScene)}). If successful, the diff
   * subcommand writes the scene it calculates to {@link System#out}.
   *
   * @throws IOException if there is trouble reading a file
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 3 || !"diff".equals(args[0])) {
      System.err.println("usage: java annotations.util.SceneOps diff first.jaif second.jaif");
      System.exit(1);
    }

    AScene s1 = new AScene();
    AScene s2 = new AScene();

    try {
      IndexFileParser.parseFile(args[1], s1);
      IndexFileParser.parseFile(args[2], s2);
      AScene diff = diff(s1, s2);

      Writer w =
          new PrintWriter(
              new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8)));
      try {
        IndexFileWriter.write(diff, w);
      } catch (DefException e) {
        exitWithException(e);
      }
      w.flush();
    } catch (IOException e) {
      exitWithException(e);
    }
  }

  /**
   * Compute the difference of two scenes, that is, a scene containing all and only those insertion
   * specifications that exist in the first but not in the second.
   *
   * @param s1 the "minuend"
   * @param s2 the "subtrahend"
   * @return s1 - s2 ("set difference")
   */
  public static AScene diff(AScene s1, AScene s2) {
    AScene diff = new AScene();
    new DiffVisitor().visitScene(s1, s2, diff);
    diff.prune();
    return diff;
  }

  /** Print stack trace (for debugging) and exit with return code 1. */
  private static void exitWithException(Exception e) {
    e.printStackTrace();
    System.exit(1);
  }

  // TODO: integrate into scene-lib test suite
  public static void testDiffEmpties() {
    assert new AScene().equals(diff(new AScene(), new AScene()));
  }

  /**
   * Test that X-X=0, for several scenes X.
   *
   * @throws IOException if there is trouble with IO
   */
  public static void testDiffSame() throws IOException {
    String dirname = "test/annotations/tests/classfile/cases";
    String[] testcases = {
      "ClassEmpty",
      "ClassNonEmpty",
      "FieldGeneric",
      "FieldSimple",
      "LocalVariableGenericArray",
      "MethodReceiver",
      "MethodReturnTypeGenericArray",
      "ObjectCreationGenericArray",
      "ObjectCreation",
      "TypecastGenericArray",
      "Typecast"
    };
    AScene emptyScene = new AScene();
    for (String testcase : testcases) {
      AScene scene1 = new AScene();
      AScene scene2 = new AScene();
      String filename = dirname + "/Test" + testcase + ".jaif";
      IndexFileParser.parseFile(filename, scene1);
      IndexFileParser.parseFile(filename, scene2);
      assert emptyScene.equals(diff(scene1, scene1));
      assert emptyScene.equals(diff(scene1, scene2));
    }
  }
}

/**
 * Visitor for calculating "set difference" of scenes. Visitor methods fill in a scene instead of
 * returning one because an {@link AElement} can be created only inside an {@link AScene}.
 */
class DiffVisitor implements ElementVisitor<Void, Pair<AElement, AElement>> {

  /**
   * Adds all annotations that are in {@code minuend} but not in {@code subtrahend} to {@code
   * difference}.
   */
  public void visitScene(AScene minuend, AScene subtrahend, AScene difference) {
    visitElements(minuend.packages, subtrahend.packages, difference.packages);
    diff(minuend.imports, subtrahend.imports, difference.imports);
    visitElements(minuend.classes, subtrahend.classes, difference.classes);
  }

  // Never used, as annotations and definitions don't get duplicated.
  @Override
  public Void visitAnnotationDef(AnnotationDef minuend, Pair<AElement, AElement> eltPair) {
    throw new IllegalStateException("BUG: DiffVisitor.visitAnnotationDef invoked");
  }

  /**
   * Calculates difference between {@code minuend} and first component of {@code eltPair}, adding
   * results to second component of {@code eltPair}.
   */
  @Override
  public Void visitBlock(ABlock minuend, Pair<AElement, AElement> eltPair) {
    ABlock subtrahend = (ABlock) eltPair.fst;
    ABlock difference = (ABlock) eltPair.snd;
    visitElements(minuend.locals, subtrahend.locals, difference.locals);
    return visitExpression(minuend, eltPair);
  }

  /**
   * Calculates difference between {@code minuend} and first component of {@code eltPair}, adding
   * results to second component of {@code eltPair}.
   */
  @Override
  public Void visitClass(AClass minuend, Pair<AElement, AElement> eltPair) {
    AClass subtrahend = (AClass) eltPair.fst;
    AClass difference = (AClass) eltPair.snd;
    visitElements(minuend.bounds, subtrahend.bounds, difference.bounds);
    visitElements(
        minuend.extendsImplements, subtrahend.extendsImplements, difference.extendsImplements);
    visitElements(minuend.methods, subtrahend.methods, difference.methods);
    visitElements(minuend.staticInits, subtrahend.staticInits, difference.staticInits);
    visitElements(minuend.instanceInits, subtrahend.instanceInits, difference.instanceInits);
    visitElements(minuend.fields, subtrahend.fields, difference.fields);
    visitElements(minuend.fieldInits, subtrahend.fieldInits, difference.fieldInits);
    return visitDeclaration(minuend, eltPair);
  }

  /**
   * Calculates difference between {@code minuend} and first component of {@code eltPair}, adding
   * results to second component of {@code eltPair}.
   */
  @Override
  public Void visitDeclaration(ADeclaration minuend, Pair<AElement, AElement> eltPair) {
    ADeclaration subtrahend = (ADeclaration) eltPair.fst;
    ADeclaration difference = (ADeclaration) eltPair.snd;
    visitElements(
        minuend.insertAnnotations, subtrahend.insertAnnotations, difference.insertAnnotations);
    visitElements(minuend.insertTypecasts, subtrahend.insertTypecasts, difference.insertTypecasts);
    return visitElement(minuend, eltPair);
  }

  /**
   * Calculates difference between {@code minuend} and first component of {@code eltPair}, adding
   * results to second component of {@code eltPair}.
   */
  @Override
  public Void visitExpression(AExpression minuend, Pair<AElement, AElement> eltPair) {
    AExpression subtrahend = (AExpression) eltPair.fst;
    AExpression difference = (AExpression) eltPair.snd;
    visitElements(minuend.typecasts, subtrahend.typecasts, difference.typecasts);
    visitElements(minuend.instanceofs, subtrahend.instanceofs, difference.instanceofs);
    visitElements(minuend.news, subtrahend.news, difference.news);
    visitElements(minuend.calls, subtrahend.calls, difference.calls);
    visitElements(minuend.refs, subtrahend.refs, difference.refs);
    visitElements(minuend.funs, subtrahend.funs, difference.funs);
    return visitElement(minuend, eltPair);
  }

  /**
   * Calculates difference between {@code minuend} and first component of {@code eltPair}, adding
   * results to second component of {@code eltPair}.
   */
  @Override
  public Void visitField(AField minuend, Pair<AElement, AElement> eltPair) {
    return visitDeclaration(minuend, eltPair);
  }

  /**
   * Calculates difference between {@code minuend} and first component of {@code eltPair}, adding
   * results to second component of {@code eltPair}.
   */
  @Override
  public Void visitMethod(AMethod minuend, Pair<AElement, AElement> eltPair) {
    AMethod subtrahend = (AMethod) eltPair.fst;
    AMethod difference = (AMethod) eltPair.snd;
    visitElements(minuend.bounds, subtrahend.bounds, difference.bounds);
    visitElements(minuend.parameters, subtrahend.parameters, difference.parameters);
    visitElements(minuend.throwsException, subtrahend.throwsException, difference.throwsException);
    visitElements(minuend.parameters, subtrahend.parameters, difference.parameters);
    visitBlock(minuend.body, elemPair(subtrahend.body, difference.body));
    if (minuend.returnType != null) {
      minuend.returnType.accept(this, elemPair(subtrahend.returnType, difference.returnType));
    }
    if (minuend.receiver != null) {
      minuend.receiver.accept(this, elemPair(subtrahend.receiver, difference.receiver));
    }
    return visitDeclaration(minuend, eltPair);
  }

  /**
   * Calculates difference between {@code minuend} and first component of {@code eltPair}, adding
   * results to second component of {@code eltPair}.
   */
  @Override
  public Void visitTypeElement(ATypeElement minuend, Pair<AElement, AElement> eltPair) {
    ATypeElement subtrahend = (ATypeElement) eltPair.fst;
    ATypeElement difference = (ATypeElement) eltPair.snd;
    visitElements(minuend.innerTypes, subtrahend.innerTypes, difference.innerTypes);
    return visitElement(minuend, eltPair);
  }

  /**
   * Calculates difference between {@code minuend} and first component of {@code eltPair}, adding
   * results to second component of {@code eltPair}.
   */
  @Override
  public Void visitTypeElementWithType(
      ATypeElementWithType minuend, Pair<AElement, AElement> eltPair) {
    return visitTypeElement(minuend, eltPair);
  }

  /**
   * Calculates difference between {@code minuend} and first component of {@code eltPair}, adding
   * results to second component of {@code eltPair}.
   */
  @Override
  public Void visitElement(AElement minuend, Pair<AElement, AElement> eltPair) {
    AElement subtrahend = eltPair.fst;
    AElement difference = eltPair.snd;
    diff(minuend.tlAnnotationsHere, subtrahend.tlAnnotationsHere, difference.tlAnnotationsHere);
    if (minuend.type != null) {
      AElement stype = subtrahend.type;
      AElement dtype = difference.type;
      minuend.type.accept(this, elemPair(stype, dtype));
    }
    return null;
  }

  /**
   * Calculates difference between {@code minuend} and first component of {@code eltPair}, adding
   * results to second component of {@code eltPair}.
   */
  private <K, V extends AElement> void visitElements(
      VivifyingMap<K, V> minuend, VivifyingMap<K, V> subtrahend, VivifyingMap<K, V> difference) {
    if (minuend != null) {
      for (Map.Entry<K, V> e : minuend.entrySet()) {
        K key = e.getKey();
        V mval = e.getValue();
        V sval = subtrahend.get(key);
        if (sval == null) {
          difference.put(key, mval);
        } else {
          mval.accept(this, elemPair(sval, difference.getVivify(key)));
        }
      }
    }
  }

  /**
   * Calculates difference between {@code minuend} and {@code subtrahend}, adding the result to
   * {@code difference}.
   */
  private static <T> void diff(Set<T> minuend, Set<T> subtrahend, Set<T> difference) {
    if (minuend != null) {
      for (T t : minuend) {
        if (!subtrahend.contains(t)) {
          difference.add(t);
        }
      }
    }
  }

  /**
   * Calculates difference between {@code minuend} and {@code subtrahend}, adding the results to
   * {@code difference}.
   */
  private static <K, V> void diff(
      Map<K, Set<V>> minuend, Map<K, Set<V>> subtrahend, Map<K, Set<V>> difference) {
    if (minuend != null) {
      for (K key : minuend.keySet()) {
        Set<V> mval = minuend.get(key);
        Set<V> sval = subtrahend.get(key);
        if (sval == null) {
          difference.put(key, mval);
        } else if (!sval.equals(mval)) {
          try {
            @SuppressWarnings("unchecked")
            Set<V> set = (Set<V>) sval.getClass().getDeclaredConstructor().newInstance();
            diff(mval, sval, set);
            if (!set.isEmpty()) {
              difference.put(key, set);
            }
          } catch (InstantiationException
              | IllegalAccessException
              | NoSuchMethodException
              | InvocationTargetException e) {
            e.printStackTrace();
            System.exit(1);
          }
        }
      }
    }
  }

  /** Convenience method for ensuring returned {@link Pair} is of the most general type. */
  private Pair<AElement, AElement> elemPair(AElement stype, AElement dtype) {
    return Pair.of(stype, dtype);
  }
}
