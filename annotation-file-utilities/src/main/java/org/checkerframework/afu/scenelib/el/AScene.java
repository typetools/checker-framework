package org.checkerframework.afu.scenelib.el;

import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.checkerframework.afu.scenelib.io.IndexFileParser;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;

/**
 * An <code>AScene</code> (annotated scene) represents the annotations on a set of Java classes and
 * packages along with the definitions of some or all of the annotation types used.
 *
 * <p>Each client of the annotation library may wish to use its own representation for certain kinds
 * of annotations instead of a simple name-value map; thus, a layer of abstraction in the storage of
 * annotations was introduced.
 *
 * <p><code>AScene</code>s and many {@link AElement}s can contain other {@link AElement}s. When
 * these objects are created, their collections of subelements are empty. In order to associate an
 * annotation with a particular Java element in an <code>AScene</code>, one must first ensure that
 * an appropriate {@link AElement} exists in the <code>AScene</code>. To this end, the maps of
 * subelements have a <code>vivify</code> method. Calling <code>vivify</code> to access a particular
 * subelement will return the subelement if it already exists; otherwise it will create and then
 * return the subelement. (Compare to vivification in Perl.) For example, the following code will
 * obtain an {@link AMethod} representing <code>Foo.bar</code> in the <code>AScene</code> <code>s
 * </code>, creating it if it did not already exist:
 *
 * <pre>
 * AMethod&lt;A&gt; m = s.classes.getVivify("Foo").methods.getVivify("bar");
 * </pre>
 *
 * <p>Then one can add an annotation to the method:
 *
 * <pre>
 * m.annotationsHere.add(new Annotation(
 *     new AnnotationDef(taintedDef, RetentionPolicy.RUNTIME, true),
 *     new Annotation(taintedDef, Collections.emptyMap())
 * ));
 * </pre>
 */
public class AScene implements Cloneable {
  /** If true, check that the copy constructor works correctly. */
  private static boolean checkClones = true;

  /** This scene's annotated packages; map key is package name */
  public final VivifyingMap<String, AElement> packages = AElement.<String>newVivifyingLHMap_AE();

  /**
   * Contains for each annotation type a set of imports to be added to the source if the annotation
   * is inserted with the "abbreviate" option on.<br>
   * <strong>Key</strong>: fully-qualified name of an annotation. e.g. for <code>@com.foo.Bar(x)
   * </code>, the fully-qualified name is <code>com.foo.Bar</code> <br>
   * <strong>Value</strong>: names of packages this annotation needs
   */
  public final Map<String, Set<String>> imports = new LinkedHashMap<>();

  /** This scene's annotated classes; map key is class name */
  public final VivifyingMap</*@BinaryName*/ String, AClass> classes =
      new VivifyingMap<String, AClass>(new LinkedHashMap<>()) {
        @Override
        public AClass createValueFor(String k) {
          return new AClass(k);
        }

        @Override
        public boolean isEmptyValue(AClass v) {
          return v.isEmpty();
        }
      };

  /** Creates a new {@link AScene} with no classes or packages. */
  public AScene() {}

  /**
   * Copy constructor for {@link AScene}.
   *
   * @param scene the scene to make a copy of
   */
  @SuppressWarnings("this-escape")
  public AScene(AScene scene) {
    for (String key : scene.packages.keySet()) {
      AElement val = scene.packages.get(key);
      packages.put(key, val.clone());
    }
    for (String key : scene.imports.keySet()) {
      // copy could in principle have different Set implementation
      Set<String> value = scene.imports.get(key);
      Set<String> copy = new LinkedHashSet<>();
      copy.addAll(value);
      imports.put(key, copy);
    }
    for (String key : scene.classes.keySet()) {
      AClass clazz = scene.classes.get(key);
      classes.put(key, clazz.clone());
    }
    if (checkClones) {
      checkClone(this, scene);
    }
  }

  @Override
  public AScene clone() {
    return new AScene(this);
  }

  /**
   * Returns whether this {@link AScene} equals <code>o</code>; the commentary and the cautionary
   * remarks on {@link AElement#equals(Object)} also apply to {@link AScene#equals(Object)}.
   */
  @Override
  public boolean equals(Object o) {
    return o instanceof AScene && ((AScene) o).equals(this);
  }

  /**
   * Returns whether this {@link AScene} equals <code>o</code>; a slightly faster variant of {@link
   * #equals(Object)} for when the argument is statically known to be another nonnull {@link
   * AScene}.
   */
  public boolean equals(AScene o) {
    return o.classes.equals(classes) && o.packages.equals(packages);
  }

  @Override
  public int hashCode() {
    return classes.hashCode() + packages.hashCode();
  }

  /**
   * Fetch the classes in this scene, represented as AClass objects.
   *
   * @return an immutable map from binary names to AClass objects
   */
  public Map</*@BinaryName*/ String, AClass> getClasses() {
    return ImmutableMap.copyOf(classes);
  }

  /** Returns whether this {@link AScene} is empty. */
  public boolean isEmpty() {
    return classes.isEmpty() && packages.isEmpty();
  }

  /** Removes empty subelements of this {@link AScene} depth-first. */
  public void prune() {
    classes.prune();
    packages.prune();
  }

  /** Returns a string representation. */
  public String unparse() {
    StringBuilder sb = new StringBuilder();
    sb.append("packages:\n");
    for (Map.Entry<String, AElement> entry : packages.entrySet()) {
      sb.append("  " + entry.getKey() + " => " + entry.getValue() + "\n");
    }
    sb.append("classes:\n");
    for (Map.Entry<String, AClass> entry : classes.entrySet()) {
      sb.append("  " + entry.getKey() + " => " + "\n");
      sb.append(entry.getValue().unparse("    "));
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return unparse();
  }

  /**
   * Checks that the arguments are clones of one another.
   *
   * <p>Throws an exception if the arguments 1) are the same reference; 2) are not equal() in both
   * directions; or 3) contain corresponding elements that meet either of the preceding two
   * conditions.
   *
   * @param s0 the first AScene to compare
   * @param s1 the second Ascene to compare
   */
  @SuppressWarnings({"this-escape"})
  public static void checkClone(AScene s0, AScene s1) {
    checkCloneNotReferenceEqual(s0, s1);
    s0.prune();
    s1.prune();
    checkCloneMap(s0.packages, s1.packages);
    checkCloneMap(s0.classes, s1.classes);
  }

  /**
   * Throws an exception if the two arguments are references to the same object.
   *
   * @param o0 the first object to compare
   * @param o1 the second object to compare
   */
  @SuppressWarnings("interning:not.interned") // reference equality check
  private static void checkCloneNotReferenceEqual(Object o0, Object o1) {
    if (o0 == null || o1 == null) {
      throw new RuntimeException("clone check failed, null value: " + o0 + ", " + o1);
    }
    if (o0 == o1) {
      throw new RuntimeException("clone check failed, reference equality: " + o0);
    }
  }

  /**
   * Throw exception if m0 == m1 or !m0.equals(m1). (See {@link #checkClone(AScene, AScene)} for
   * explanation.)
   *
   * @param <K> the type of map keys
   * @param <V> the type of map values
   * @param m0 the first map to compare
   * @param m1 the second map to compare
   */
  public static <K, V extends AElement> void checkCloneMap(
      VivifyingMap<K, V> m0, VivifyingMap<K, V> m1) {
    checkCloneNotReferenceEqual(m0, m1);
    // This does not require that the key sets are identical, because m0 and m1 are vivifying maps.
    Set<K> keys = new LinkedHashSet<>();
    keys.addAll(m0.keySet());
    keys.addAll(m1.keySet());
    for (K k : keys) {
      checkCloneAElement(m0.get(k), m1.get(k));
    }
  }

  /**
   * Throw exception if e0 == e1 or !e0.equals(e1). (See {@link #checkClone(AScene, AScene)} for
   * explanation.)
   *
   * @param e0 the first element to compare
   * @param e1 the second element to compare
   */
  private static void checkCloneAElement(AElement e0, AElement e1) {
    checkCloneNotReferenceEqual(e0, e1);
    e0.accept(checkVisitor, e1);
  }

  /**
   * Throws an exception if the two sets are not equal.
   *
   * @param s0 a set
   * @param s1 a set
   */
  private static <T> void checkCloneSet(Set<T> s0, Set<T> s1) {
    checkCloneNotReferenceEqual(s0, s1);
    if (!s0.equals(s1)) {
      throw new RuntimeException("clone check failed, different sets: " + s0 + ", " + s1);
    }
  }

  /**
   * Throw exception on visit if el == arg or !el.equals(arg). (See {@link #checkClone(AScene,
   * AScene)} for explanation.)
   */
  private static ElementVisitor<Void, AElement> checkVisitor =
      new ElementVisitor<Void, AElement>() {
        @Override
        public Void visitAnnotationDef(AnnotationDef el, AElement arg) {
          return null;
        }

        @Override
        public Void visitBlock(ABlock el, AElement arg) {
          ABlock b = (ABlock) arg;
          checkCloneMap(el.locals, b.locals);
          return null;
        }

        @Override
        public Void visitClass(AClass el, AElement arg) {
          AClass c = (AClass) arg;
          checkCloneMap(el.bounds, c.bounds);
          checkCloneMap(el.extendsImplements, c.extendsImplements);
          checkCloneMap(el.fieldInits, c.fieldInits);
          checkCloneMap(el.fields, c.fields);
          checkCloneMap(el.instanceInits, c.instanceInits);
          checkCloneMap(el.methods, c.methods);
          checkCloneMap(el.staticInits, c.staticInits);
          return visitDeclaration(el, arg);
        }

        @Override
        public Void visitDeclaration(ADeclaration el, AElement arg) {
          ADeclaration d = (ADeclaration) arg;
          checkCloneMap(el.insertAnnotations, d.insertAnnotations);
          checkCloneMap(el.insertTypecasts, d.insertTypecasts);
          visitElement(el, arg);
          return null;
        }

        @Override
        public Void visitExpression(AExpression el, AElement arg) {
          if (el == null && arg == null) {
            return null;
          }
          AExpression e = (AExpression) arg;
          checkCloneNotReferenceEqual(el.id, e.id);
          checkCloneMap(el.calls, e.calls);
          checkCloneMap(el.funs, e.funs);
          checkCloneMap(el.instanceofs, e.instanceofs);
          checkCloneMap(el.news, e.news);
          checkCloneMap(el.refs, e.refs);
          checkCloneMap(el.typecasts, e.typecasts);
          visitElement(el, arg);
          return null;
        }

        @Override
        public Void visitField(AField el, AElement arg) {
          AField f = (AField) arg;
          visitExpression(el.init, f.init);
          visitDeclaration(el, arg);
          return null;
        }

        @Override
        public Void visitMethod(AMethod el, AElement arg) {
          AMethod m = (AMethod) arg;
          checkCloneNotReferenceEqual(el.methodSignature, m.methodSignature);
          checkCloneMap(el.bounds, m.bounds);
          visitTypeElement(el.returnType, m.returnType);
          visitField(el.receiver, m.receiver);
          checkCloneMap(el.parameters, m.parameters);
          checkCloneMap(el.throwsException, m.throwsException);
          checkCloneMap(el.preconditions, m.preconditions);
          checkCloneMap(el.postconditions, m.postconditions);
          visitBlock(el.body, m.body);
          return null;
        }

        @Override
        public Void visitTypeElement(ATypeElement el, AElement arg) {
          if (el == null && arg == null) {
            return null;
          }
          ATypeElement t = (ATypeElement) arg;
          checkCloneNotReferenceEqual(el.description, t.description);
          checkCloneMap(el.innerTypes, t.innerTypes);
          return null;
        }

        @Override
        public Void visitTypeElementWithType(ATypeElementWithType el, AElement arg) {
          ATypeElementWithType t = (ATypeElementWithType) arg;
          checkCloneNotReferenceEqual(el.getType(), t.getType());
          visitTypeElement(el, arg);
          return null;
        }

        @Override
        public Void visitElement(AElement el, AElement arg) {
          checkCloneNotReferenceEqual(el.description, arg.description);
          checkCloneSet(el.tlAnnotationsHere, arg.tlAnnotationsHere);
          visitTypeElement(el.type, arg.type);
          return null;
        }
      };

  // temporary main for easy testing on JAIFs
  public static void main(String[] args) {
    int status = 0;
    checkClones = true;

    for (int i = 0; i < args.length; i++) {
      AScene s0 = new AScene();
      System.out.print(args[i] + ": ");
      try {
        IndexFileParser.parseFile(args[i], s0);
        @SuppressWarnings("UnusedVariable") // testing that clone() does not throw an exception
        AScene ignore = s0.clone();
        System.out.println("ok");
      } catch (Throwable e) {
        status = 1;
        System.out.println("failed");
        e.printStackTrace();
      }
    }
    System.exit(status);
  }
}
