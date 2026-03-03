package org.checkerframework.afu.scenelib.el;

import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.field.AnnotationAFT;
import org.checkerframework.afu.scenelib.field.AnnotationFieldType;
import org.checkerframework.afu.scenelib.io.IndexFileWriter;

/**
 * A DefCollector supplies a visitor for the annotation definitions in an AScene. First, call the
 * DefCollector constructor passing the AScene. Then, call the visit method. This class exists
 * primarily for the benefit of {@link IndexFileWriter#write(AScene, Writer)}.
 */
public abstract class DefCollector {

  /**
   * The set of all definitions in the Scene. {@link #collect(AScene)} populates it. {@link
   * #visit()} iterates over it.
   */
  private final Set<AnnotationDef> defs;

  /**
   * Constructs a new {@link DefCollector}, which immediately collects all the definitions from
   * annotations the given scene. Next call {@link #visit} to have the definitions passed back to
   * you in topological order. If the scene contains two irreconcilable definitions of the same
   * annotation type, a {@link DefException} is thrown.
   */
  public DefCollector(AScene s) throws DefException {
    defs = new LinkedHashSet<AnnotationDef>();
    collect(s);
  }

  // The name "collect" in the methods below means to insert or add to
  // the DefCollector.  "Insert" or "add" would have been better, but
  // at least the methods are private.

  private AnnotationDef getDef(String name) {
    for (AnnotationDef def : defs) {
      if (def.name.equals(name)) {
        return def;
      }
    }
    return null;
  }

  private void collect(AScene s) throws DefException {
    for (AElement p : s.packages.values()) {
      collect(p);
    }
    for (AClass c : s.classes.values()) {
      collect(c);
    }
  }

  private void addToDefs(AnnotationDef d) throws DefException {
    // TODO: this mimics the condition we have in collect, but
    // i don't know if we need it
    if (defs.contains(d)) {
      return;
    }
    AnnotationDef oldD = getDef(d.name);
    if (oldD == null) {
      defs.add(d);
    } else {
      AnnotationDef ud = AnnotationDef.unify(oldD, d);
      if (ud == null) {
        throw new DefException(d.name, oldD, d);
      }
      defs.remove(oldD);
      defs.add(ud);
    }
  }

  private void collect(AnnotationDef d) throws DefException {
    if (defs.contains(d)) {
      return;
    }

    // define the fields first
    for (AnnotationFieldType aft : d.fieldTypes.values()) {
      if (aft instanceof AnnotationAFT) {
        collect(((AnnotationAFT) aft).annotationDef);
      }
    }

    addToDefs(d);

    // TODO: In the future we want to add the defs of meta-annotations
    // as well.  Enable this option by uncommenting the following line.
    //
    // For the time-being, the parser would fail, because of possible
    // circular references (e.g. Documented and Retention).  When it is
    // fixed, uncomment it
    //
    // collect((AElement)d);
  }

  /**
   * Collect annotation definitions for an element.
   *
   * @param e the element to collect annotation definitions from
   * @throws DefException if an annotation definition cannot be found
   */
  private void collect(AElement e) throws DefException {
    if (e == null) {
      return;
    }
    for (Annotation tla : e.tlAnnotationsHere) {
      collect(tla);
    }
    if (e.type != null) {
      collect(e.type);
    }
  }

  /**
   * Collect annotation definitions for an annotation.
   *
   * @param a the annotation to collect annotation definitions from
   * @throws DefException if an annotation definition cannot be found
   */
  private void collect(Annotation a) throws DefException {
    AnnotationDef d = a.def;
    if (!defs.contains(d)) {
      // Must call collect() before addToDefs().
      collect(d);
      addToDefs(d);
    }
  }

  /**
   * Collect annotation definitions for a type.
   *
   * @param t the type to collect annotation definitions from
   * @throws DefException if an annotation definition cannot be found
   */
  private void collect(ATypeElement t) throws DefException {
    collect((AElement) t);
    for (ATypeElement it : t.innerTypes.values()) {
      collect(it);
    }
  }

  private void collect(ADeclaration d) throws DefException {
    collect((AElement) d);
    for (ATypeElement ia : d.insertAnnotations.values()) {
      collect(ia);
    }
    for (ATypeElementWithType ic : d.insertTypecasts.values()) {
      collect(ic);
    }
  }

  private void collect(AField f) throws DefException {
    collect((ADeclaration) f);
    collect(f.init);
  }

  /**
   * Collect annotation definitions for a method.
   *
   * @param m the method to collect annotation definitions from
   * @throws DefException if an annotation definition cannot be found
   */
  private void collect(AMethod m) throws DefException {
    collect((ADeclaration) m);
    for (ATypeElement b : m.bounds.values()) {
      collect(b);
    }
    collect(m.returnType);
    collect(m.receiver);
    for (AField p : m.parameters.values()) {
      collect(p);
    }
    for (ATypeElement e : m.throwsException.values()) {
      collect(e);
    }
    for (AElement e : m.preconditions.values()) {
      collect(e);
    }
    for (AElement e : m.postconditions.values()) {
      collect(e);
    }
    for (Annotation a : m.contracts) {
      collect(a);
    }
    collect(m.body);
  }

  /**
   * Collect annotation definitions for a block.
   *
   * @param b the block to collect annotation definitions from
   * @throws DefException if an annotation definition cannot be found
   */
  private void collect(ABlock b) throws DefException {
    for (AField l : b.locals.values()) {
      collect(l);
    }
    for (ATypeElement tc : b.typecasts.values()) {
      collect(tc);
    }
    for (ATypeElement i : b.instanceofs.values()) {
      collect(i);
    }
    for (ATypeElement n : b.news.values()) {
      collect(n);
    }
  }

  private void collect(AClass c) throws DefException {
    collect((ADeclaration) c);
    for (ATypeElement b : c.bounds.values()) {
      collect(b);
    }
    for (ATypeElement ei : c.extendsImplements.values()) {
      collect(ei);
    }
    for (AMethod m : c.methods.values()) {
      collect(m);
    }
    for (AField f : c.fields.values()) {
      collect(f);
    }
  }

  /**
   * Override this method to perform some sort of subclass-specific processing on the given {@link
   * AnnotationDef}.
   *
   * <p>It is only called once per annotation used in the scene, because each annotation is only
   * defined once.
   */
  protected abstract void visitAnnotationDef(AnnotationDef d);

  /**
   * Calls {@link #visitAnnotationDef} on the definitions collected from the scene that was passed
   * to the constructor. Visiting is done in topological order: if the definition of {@code A}
   * contains a subannotation of type {@code B}, then {@code B} is guaranteed to be visited before
   * {@code A}.
   */
  public final void visit() {
    for (AnnotationDef d : defs) {
      visitAnnotationDef(d);
    }
  }
}
