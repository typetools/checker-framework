package org.checkerframework.afu.scenelib.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.objectweb.asm.*;

/**
 * A MethodRecorder is a {@link ClassVisitor} which simply keeps track of the methods, fields and
 * declaration annotations inside a class. The main use of this class is to get this information in
 * the order it is visited by {@link ClassWriter}, which is the order in which they occur in the
 * classfile.
 */
public class MethodRecorder extends ClassVisitor {
  /** Methods of the class. */
  private List<String> methods;

  /** Declaration annotations of the class. */
  private List<String> annotations;

  /** Fields of the class. */
  private List<String> fields;

  /**
   * Construct a new MethodRecorder.
   *
   * @param api version of ASM api to use
   */
  public MethodRecorder(int api) {
    this(api, null);
  }

  /**
   * Construct a new MethodRecorder.
   *
   * @param api version of ASM api to use
   * @param classVisitor ClassVisitor to use
   */
  public MethodRecorder(int api, ClassVisitor classVisitor) {
    super(api, classVisitor);
    this.methods = new ArrayList<>();
    this.annotations = new ArrayList<>();
    this.fields = new ArrayList<>();
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String descriptor, String signature, String[] exceptions) {
    methods.add(name);
    return super.visitMethod(access, name, descriptor, signature, exceptions);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    annotations.add(descriptor);
    return super.visitAnnotation(descriptor, visible);
  }

  /**
   * Returns the methods in the class in the order that they occur in the classfile.
   *
   * @return list containing names of methods in the class
   */
  public List<String> getMethods() {
    return Collections.unmodifiableList(methods);
  }

  /**
   * Returns the declaration annotations in the class in the order that they occur in the classfile.
   *
   * @return list containing names of declaration annotations in the class
   */
  public List<String> getAnnotations() {
    return Collections.unmodifiableList(annotations);
  }

  /**
   * Returns the fields in the class in the order that they occur in the classfile.
   *
   * @return list containing names of fields in the class
   */
  public List<String> getFields() {
    return Collections.unmodifiableList(fields);
  }

  @Override
  public FieldVisitor visitField(
      int access, String name, String descriptor, String signature, Object value) {
    fields.add(name);
    return super.visitField(access, name, descriptor, signature, value);
  }

  /**
   * (Used for testing.)
   *
   * @param args input arguments
   * @throws IOException if a problem occurs during ClassReader construction
   */
  public static void main(String[] args) throws IOException {
    ClassReader classReader = new ClassReader("com.google.common.annotations.GwtCompatible");
    MethodRecorder methodRecorder = new MethodRecorder(Opcodes.ASM8);
    classReader.accept(methodRecorder, 0);
    System.out.println(methodRecorder.annotations);
    System.out.println(methodRecorder.methods);
  }
}
