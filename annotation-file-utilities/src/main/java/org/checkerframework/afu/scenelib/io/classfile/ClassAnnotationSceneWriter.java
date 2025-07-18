// This class is a complete ClassVisitor with many nested classes that do
// the work of parsing an AScene and inserting them into a class file, as
// the original class file is being read.

package org.checkerframework.afu.scenelib.io.classfile;

import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.AElement;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.BoundLocation;
import org.checkerframework.afu.scenelib.el.LocalLocation;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.afu.scenelib.el.TypeIndexLocation;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.afu.scenelib.field.AnnotationFieldType;
import org.checkerframework.afu.scenelib.field.ArrayAFT;
import org.checkerframework.afu.scenelib.field.ClassTokenAFT;
import org.checkerframework.afu.scenelib.field.EnumAFT;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;

/**
 * A ClassAnnotationSceneWriter is a {@link org.objectweb.asm.ClassVisitor} that can be used to
 * write a class file that is the combination of an existing class file and annotations in an {@link
 * AScene}. The "write" in {@code ClassAnnotationSceneWriter} refers to a class file being rewritten
 * with information from a scene. Also see {@link ClassAnnotationSceneReader}.
 *
 * <p>The proper usage of this class is to construct a {@code ClassAnnotationSceneWriter} with a
 * {@link AScene} that already contains all its annotations, pass this as a {@link
 * org.objectweb.asm.ClassVisitor} to {@link org.objectweb.asm.ClassReader#accept}, and then obtain
 * the resulting class, ready to be written to a file, with {@link #toByteArray}.
 *
 * <p>All other methods are intended to be called only by {@link
 * org.objectweb.asm.ClassReader#accept}, and should not be called anywhere else, due to the order
 * in which {@link org.objectweb.asm.ClassVisitor} methods should be called.
 *
 * <p>Throughout this class, "scene" refers to the {@link AScene} this class is merging into a class
 * file.
 */
public class ClassAnnotationSceneWriter extends CodeOffsetAdapter {

  // Strategy for interleaving the necessary calls to visit annotations
  // from scene into the parsing done by ClassReader
  //  (the difficulty is that the entire call sequence to every data structure
  //   to visit annotations is in ClassReader, which should not be modified
  //   by this library):
  //
  // A ClassAnnotationSceneWriter is a ClassAdapter around a ClassWriter.
  //  - To visit the class' annotations in the scene, right before the code for
  //     ClassWriter.visit{InnerClass, Field, Method, End} is called,
  //     ensure that all annotations in the scene are visited once.
  //  - To visit every field's annotations,
  //     ClassAnnotationSceneWriter.visitField() returns a
  //     FieldAnnotationSceneWriter that in a similar fashion makes sure
  //     that each of that field's annotations is visited once on the call
  //     to visitEnd();
  //  - To visit every method's annotations,
  //     ClassAnnotationSceneWriter.visitMethod() returns a
  //     MethodAnnotationSceneWriter that visits all of that method's
  //     annotations in the scene at the first call of visit{Code, End}.
  //

  /** Whether to output error messages for unsupported cases. */
  private static final boolean strict = false;

  // None of these fields should be null, except for aClass, which
  //  can't be vivified until the first visit() is called.

  /** The scene from which to get additional annotations. */
  private final AScene scene;

  /** The representation of this class in the scene. */
  private AClass aClass;

  /** A list of annotations on this class that this has already visited in the class file. */
  private final List<String> existingClassAnnotations;

  /** Whether or not this has visited the corresponding annotations in scene. */
  private boolean hasVisitedClassAnnotationsInScene;

  /**
   * Whether or not to overwrite existing annotations on the same element in a class file if a
   * similar annotation is found in scene.
   */
  private final boolean overwrite;

  /** Map from a method signature to a set of bytecode offsets to constructor invocations. */
  private final Map<String, Set<Integer>> dynamicConstructors;

  /** Map from a method signature to a set of bytecode offsets to lambda invocations. */
  private final Map<String, Set<Integer>> lambdaExpressions;

  /** ClassReader for reading the class file. */
  @SuppressWarnings("HidingField") // TODO!!
  private ClassReader classReader;

  /**
   * Constructs a new {@code ClassAnnotationSceneWriter} that will insert all the annotations in
   * {@code scene} into the class that it visits. {@code scene} must be an {@link AScene} over the
   * class that this will visit.
   *
   * @param api the ASM API version to use
   * @param classReader the reader for the class being modified
   * @param scene the annotation scene containing annotations to be inserted into the class this
   *     visits
   * @param overwrite whether or not to overwrite existing annotations on the same element
   */
  public ClassAnnotationSceneWriter(
      int api, ClassReader classReader, AScene scene, boolean overwrite) {
    super(api, classReader);
    this.scene = scene;
    this.hasVisitedClassAnnotationsInScene = false;
    this.aClass = null;
    this.existingClassAnnotations = new ArrayList<>();
    this.overwrite = overwrite;
    this.dynamicConstructors = new HashMap<>();
    this.lambdaExpressions = new HashMap<>();
    this.classReader = classReader;
  }

  /**
   * Returns a byte array that represents the resulting class file from merging all the annotations
   * in the scene into the class file this has visited. This method may only be called once this has
   * already completely visited a class, which is done by calling {@link
   * org.objectweb.asm.ClassReader#accept}.
   *
   * @return a byte array of the merged class file
   */
  public byte[] toByteArray() {
    return ((ClassWriter) cv).toByteArray();
  }

  @Override
  public void visit(
      int version,
      int access,
      String name,
      String signature,
      String superName,
      String[] interfaces) {
    classReader.accept(new MethodCodeIndexer(api), 0);
    super.visit(version, access, name, signature, superName, interfaces);
    // class files store fully qualified class names with '/' instead of '.'
    name = name.replace('/', '.');
    aClass = scene.classes.getVivify(name);
  }

  @Override
  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    ensureVisitSceneClassAnnotations();
    super.visitInnerClass(name, outerName, innerName, access);
  }

  @Override
  public FieldVisitor visitField(
      int access, String name, String descriptor, String signature, Object value) {
    ensureVisitSceneClassAnnotations();
    // FieldAnnotationSceneWriter ensures that the field visits all
    //  its annotations in the scene.
    return new FieldAnnotationSceneWriter(
        this.api, name, super.visitField(access, name, descriptor, signature, value));
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String descriptor, String signature, String[] exceptions) {
    ensureVisitSceneClassAnnotations();
    // MethodAnnotationSceneWriter ensures that the method visits all
    //  its annotations in the scene.
    // MethodAdapter is used here only for getting around an unsound
    //  "optimization" in ClassReader.
    return new MethodAnnotationSceneWriter(
        api, name, descriptor, super.visitMethod(access, name, descriptor, signature, exceptions));
  }

  @Override
  public void visitEnd() {
    ensureVisitSceneClassAnnotations();
    super.visitEnd();
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    existingClassAnnotations.add(descriptor);
    // If annotation exists in scene, and in overwrite mode,
    //  return empty visitor, since annotation from scene will be visited later.
    if (aClass.lookup(classDescToName(descriptor)) != null && overwrite) {
      return null;
    }
    return super.visitAnnotation(descriptor, visible);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
      int typeRef, TypePath typePath, String descriptor, boolean visible) {
    existingClassAnnotations.add(descriptor);
    // If annotation exists in scene, and in overwrite mode,
    //  return empty visitor, annotation from scene will be visited later.
    if (aClass.lookup(classDescToName(descriptor)) != null && overwrite) {
      return null;
    }
    return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
  }

  /**
   * Have this class visit the annotations in scene if and only if it has not already visited them.
   */
  private void ensureVisitSceneClassAnnotations() {
    if (!hasVisitedClassAnnotationsInScene) {
      hasVisitedClassAnnotationsInScene = true;
      for (Annotation tla : aClass.tlAnnotationsHere) {
        // If not in overwrite mode and annotation already exists in classfile,
        //  ignore tla.
        if (!overwrite && existingClassAnnotations.contains(name(tla))) {
          continue;
        }

        AnnotationVisitor av = visitAnnotation(tla);
        visitFields(av, tla);
        av.visitEnd();
      }

      // do type parameter bound annotations
      for (Map.Entry<BoundLocation, ATypeElement> e : aClass.bounds.entrySet()) {
        BoundLocation bloc = e.getKey();
        ATypeElement bound = e.getValue();

        TypeReference typeReference =
            bloc.boundIndex == -1
                ? TypeReference.newTypeParameterReference(
                    TypeReference.CLASS_TYPE_PARAMETER, bloc.paramIndex)
                : TypeReference.newTypeParameterBoundReference(
                    TypeReference.CLASS_TYPE_PARAMETER_BOUND, bloc.paramIndex, bloc.boundIndex);
        for (Annotation tla : bound.tlAnnotationsHere) {
          // For ClassVisitor. typeReference has sort: CLASS_TYPE_PARAMETER,
          // CLASS_TYPE_PARAMETER_BOUND or CLASS_EXTENDS.
          AnnotationVisitor xav = visitTypeAnnotation(tla, typeReference, null);
          visitFields(xav, tla);
          xav.visitEnd();
        }

        typeReference =
            TypeReference.newTypeParameterBoundReference(
                TypeReference.CLASS_TYPE_PARAMETER_BOUND, bloc.paramIndex, bloc.boundIndex);
        for (Map.Entry<List<TypePathEntry>, ATypeElement> e2 : bound.innerTypes.entrySet()) {
          TypePath typePath = TypePathEntry.listToTypePath(e2.getKey());
          ATypeElement innerType = e2.getValue();

          for (Annotation tla : innerType.tlAnnotationsHere) {
            // For ClassVisitor. typeReference has sort: CLASS_TYPE_PARAMETER,
            // CLASS_TYPE_PARAMETER_BOUND or CLASS_EXTENDS.
            AnnotationVisitor xav = visitTypeAnnotation(tla, typeReference, typePath);
            visitFields(xav, tla);
            xav.visitEnd();
          }
        }
      }

      for (Map.Entry<TypeIndexLocation, ATypeElement> e : aClass.extendsImplements.entrySet()) {
        TypeIndexLocation idx = e.getKey();
        ATypeElement aty = e.getValue();

        // TODO: How is this annotation written back out?
        if (strict) {
          System.err.println(
              "ClassAnnotationSceneWriter: ignoring Extends/Implements annotation "
                  + idx
                  + " with type: "
                  + aty);
        }
      }
    }
  }

  /**
   * The following methods are utility methods for accessing information useful to asm from
   * scene-library data structures.
   *
   * @return true iff tla is visible at runtime
   */
  private static boolean isRuntimeRetention(Annotation tla) {
    if (tla.def.retention() == null) {
      return false; // TODO: temporary
    }
    return tla.def.retention() == RetentionPolicy.RUNTIME;
  }

  /** Returns the name of the annotation in the top level. */
  private static String name(Annotation tla) {
    return tla.def().name;
  }

  /** Wraps the given class name in a class descriptor. */
  private static String classNameToDesc(String name) {
    return "L" + name.replace('.', '/') + ";";
  }

  /**
   * Unwraps the class name from the given class descriptor.
   *
   * @param descriptor class name in JVML format
   * @return the class name in ClassGetName format
   */
  // TODO Can/should this use a method in reflection-util instead?
  @SuppressWarnings("signature") // TODO unverified, but clients use it as a ClassGetName
  private static @ClassGetName String classDescToName(String descriptor) {
    assert descriptor.startsWith("L");
    assert descriptor.endsWith(";");
    return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
  }

  /**
   * Returns an AnnotationVisitor over the given top-level annotation.
   *
   * @param tla the Annotation to visit
   * @return an AnnotationVisitor for tla
   */
  private AnnotationVisitor visitAnnotation(Annotation tla) {
    return super.visitAnnotation(classNameToDesc(name(tla)), isRuntimeRetention(tla));
  }

  /**
   * Returns an TypeAnnotationVisitor over the given top-level annotation.
   *
   * @param tla the Annotation to visit
   * @param typeReference the type of the annotation. Its sort should be CLASS_TYPE_PARAMETER,
   *     CLASS_TYPE_PARAMETER_BOUND or CLASS_EXTENDS.
   * @param typePath full path to the annotation
   * @return an AnnotationVisitor for tla
   */
  private AnnotationVisitor visitTypeAnnotation(
      Annotation tla, TypeReference typeReference, TypePath typePath) {
    // For ClassVisitor. typeReference has sort: CLASS_TYPE_PARAMETER, CLASS_TYPE_PARAMETER_BOUND or
    // CLASS_EXTENDS.
    return super.visitTypeAnnotation(
        typeReference.getValue(), typePath, classNameToDesc(name(tla)), isRuntimeRetention(tla));
  }

  /**
   * Has av visit the fields in the given annotation. This method is necessary even with
   * visitFields(AnnotationVisitor, Annotation) because a Annotation cannot be created from the
   * Annotation specified to be available from the Annotation object for subannotations.
   */
  private void visitFields(AnnotationVisitor av, Annotation a) {
    for (String fieldName : a.def().fieldTypes.keySet()) {
      Object value = a.getFieldValue(fieldName);
      if (value == null) {
        // hopefully a field with a default value
        continue;
      }
      AnnotationFieldType aft = a.def().fieldTypes.get(fieldName);
      if (value instanceof Annotation) {
        AnnotationVisitor nav = av.visitAnnotation(fieldName, classDescToName(a.def().name));
        visitFields(nav, a);
        nav.visitEnd();
      } else if (value instanceof List) {
        // In order to visit an array, the AnnotationVisitor returned by
        // visitArray needs to visit each element, and by specification
        // the name should be null for each element.
        AnnotationVisitor aav = av.visitArray(fieldName);
        aft = ((ArrayAFT) aft).elementType;
        for (Object o : (List<?>) value) {
          if (aft instanceof EnumAFT) {
            aav.visitEnum(null, ((EnumAFT) aft).typeName, o.toString());
          } else if (o instanceof Class) {
            aav.visit(null, org.objectweb.asm.Type.getType((Class) o));
          } else {
            aav.visit(null, o);
          }
        }
        aav.visitEnd();
      } else if (aft instanceof EnumAFT) {
        av.visitEnum(fieldName, ((EnumAFT) aft).typeName, value.toString());
      } else if (aft instanceof ClassTokenAFT) {
        av.visit(fieldName, org.objectweb.asm.Type.getType((Class<?>) value));
      } else {
        // everything else is a string
        av.visit(fieldName, value);
      }
    }
  }

  /**
   * A FieldAnnotationSceneWriter is a wrapper class around a FieldVisitor that delegates all calls
   * to its internal FieldVisitor, and on a call to visitEnd(), also has its internal FieldVisitor
   * visit all the corresponding field annotations in scene.
   */
  private class FieldAnnotationSceneWriter extends FieldVisitor {
    // After being constructed, none of these fields should be null.

    /** List of all annotations this has already visited. */
    private final List<String> existingFieldAnnotations;

    /** The AElement that represents this field in the AScene the class is visiting. */
    private final AElement aField;

    /**
     * Constructs a new FieldAnnotationSceneWriter with the given name that wraps the given
     * FieldVisitor.
     *
     * @param api the ASM API version to use
     * @param name the name of the writer
     * @param fv the FieldVisitor to use
     */
    public FieldAnnotationSceneWriter(int api, String name, FieldVisitor fv) {
      super(api, fv);
      this.existingFieldAnnotations = new ArrayList<>();
      this.aField = aClass.fields.getVivify(name);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      existingFieldAnnotations.add(descriptor);

      // If annotation exists in scene, and in overwrite mode,
      //  return empty visitor, annotation from scene will be visited later.
      if (aField.lookup(classDescToName(descriptor)) != null && overwrite) return null;

      return fv.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
        int typeRef, TypePath typePath, String descriptor, boolean visible) {
      // typeRef: FIELD
      existingFieldAnnotations.add(descriptor);

      // If annotation exists in scene, and in overwrite mode,
      //  return empty visitor, annotation from scene will be visited later.
      if (aField.lookup(classDescToName(descriptor)) != null && overwrite) return null;

      return fv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public void visitAttribute(Attribute attr) {
      fv.visitAttribute(attr);
    }

    /**
     * Tells this to visit the end of the field in the class file, and also ensures that this visits
     * all its annotations in the scene.
     *
     * @see org.objectweb.asm.FieldVisitor#visitEnd()
     */
    @Override
    public void visitEnd() {
      ensureVisitSceneFieldAnnotations();
      fv.visitEnd();
    }

    /** Has this visit the annotations on the corresponding field in scene. */
    private void ensureVisitSceneFieldAnnotations() {
      // First do declaration annotations on a field.
      for (Annotation tla : aField.tlAnnotationsHere) {
        if (!overwrite && existingFieldAnnotations.contains(name(tla))) {
          continue;
        }
        AnnotationVisitor av =
            fv.visitAnnotation(classNameToDesc(name(tla)), isRuntimeRetention(tla));
        visitFields(av, tla);
        av.visitEnd();
      }

      TypeReference typeReference = TypeReference.newTypeReference(TypeReference.FIELD);
      // Then do the type annotations on the field
      for (Annotation tla : aField.type.tlAnnotationsHere) {
        if (!overwrite && existingFieldAnnotations.contains(name(tla))) {
          continue;
        }
        AnnotationVisitor av =
            fv.visitTypeAnnotation(
                typeReference.getValue(),
                null,
                classNameToDesc(name(tla)),
                isRuntimeRetention(tla));
        visitFields(av, tla);
        av.visitEnd();
      }

      // Now do field generics/arrays.
      for (Map.Entry<List<TypePathEntry>, ATypeElement> fieldInnerEntry :
          aField.type.innerTypes.entrySet()) {
        for (Annotation tla : fieldInnerEntry.getValue().tlAnnotationsHere) {
          if (!overwrite && existingFieldAnnotations.contains(name(tla))) {
            continue;
          }
          AnnotationVisitor xav =
              fv.visitTypeAnnotation(
                  typeReference.getValue(),
                  TypePathEntry.listToTypePath(fieldInnerEntry.getKey()),
                  classNameToDesc(name(tla)),
                  isRuntimeRetention(tla));
          visitFields(xav, tla);
          xav.visitEnd();
        }
      }
    }
  }

  /**
   * A MethodAnnotationSceneWriter is to a MethodAdapter exactly what ClassAnnotationSceneWriter is
   * to a ClassAdapter: it will ensure that the MethodVisitor behind MethodAdapter visits each of
   * the annotations in scene in the correct sequence, before any of the later data is visited.
   */
  private class MethodAnnotationSceneWriter extends MethodVisitor {
    // basic strategy:
    // ensureMethodVisitSceneAnnotation will be called, if it has not already
    // been called, at the beginning of visitCode, visitEnd

    /** The AMethod that represents this method in scene. */
    private final AMethod aMethod;

    /** Whether or not this has visit the method's annotations in scene. */
    private boolean hasVisitedMethodAnnotations;

    /** The existing annotations this method has visited. */
    private final List<String> existingMethodAnnotations;

    /**
     * Constructs a new MethodAnnotationSceneWriter with the given name and description that wraps
     * around the given MethodVisitor.
     *
     * @param api the ASM API version to use
     * @param name the name of the method, as in "foo"
     * @param descriptor the method signature minus the name, as in "(Ljava/lang/String)V"
     * @param mv the method visitor to wrap around
     */
    MethodAnnotationSceneWriter(int api, String name, String descriptor, MethodVisitor mv) {
      super(api, mv);
      this.hasVisitedMethodAnnotations = false;
      this.aMethod = aClass.methods.getVivify(name + descriptor);
      this.existingMethodAnnotations = new ArrayList<>();
    }

    @Override
    public void visitCode() {
      super.visitCode();
    }

    @Override
    public void visitLabel(Label label) {
      super.visitLabel(label);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      track();
    }

    @Override
    public void visitIincInsn(int var, int increment) {
      super.visitIincInsn(var, increment);
      track();
    }

    @Override
    public void visitInsn(int opcode) {
      super.visitInsn(opcode);
      track();
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
      super.visitIntInsn(opcode, operand);
      track();
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
      super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
      track();
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
      super.visitJumpInsn(opcode, label);
      track();
    }

    @Override
    public void visitLdcInsn(Object cst) {
      super.visitLdcInsn(cst);
      track();
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      super.visitLookupSwitchInsn(dflt, keys, labels);
      track();
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      super.visitMethodInsn(opcode, owner, name, desc, itf);
      track();
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
      super.visitMultiANewArrayInsn(desc, dims);
      track();
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
      super.visitTableSwitchInsn(min, max, dflt, labels);
      track();
    }

    @Override
    public void visitTypeInsn(int opcode, String desc) {
      super.visitTypeInsn(opcode, desc);
      track();
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
      super.visitVarInsn(opcode, var);
      track();
    }

    @Override
    public void visitLocalVariable(
        String name, String desc, String signature, Label start, Label end, int index) {
      super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.objectweb.asm.MethodVisitor#visitEnd()
     */
    @Override
    public void visitEnd() {
      ensureVisitSceneMethodAnnotations();
      super.visitEnd();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      existingMethodAnnotations.add(descriptor);
      // If annotation exists in scene, and in overwrite mode,
      //  return empty visitor, annotation from scene will be visited later.
      if (shouldSkipExisting(classDescToName(descriptor))) {
        return null;
      }

      return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
        int typeRef, TypePath typePath, String descriptor, boolean visible) {
      // MethodVisitor. So typeRef: METHOD_TYPE_PARAMETER, METHOD_TYPE_PARAMETER_BOUND,
      // METHOD_RETURN, METHOD_RECEIVER, METHOD_FORMAL_PARAMETER or THROWS.
      existingMethodAnnotations.add(descriptor);

      // If annotation exists in scene, and in overwrite mode,
      //  return empty visitor, annotation from scene will be visited later.
      if (shouldSkipExisting(classDescToName(descriptor))) {
        return null;
      }

      return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    /**
     * Returns true iff the annotation in tla should not be written because it already exists in
     * this method's annotations.
     *
     * @param tla the Annotation to visit
     * @return whether the annotation should be skipped
     */
    private boolean shouldSkip(Annotation tla) {
      return (!overwrite && existingMethodAnnotations.contains(name(tla)));
    }

    /**
     * Returns true iff the annotation with the given name should not be written because it already
     * exists in this method's annotations.
     *
     * @param name the name of the annotation
     * @return whether the annotation should be skipped
     */
    private boolean shouldSkipExisting(String name) {
      return (!overwrite && aMethod.lookup(name) != null);
    }

    /**
     * Has this visit the annotation in tla, and returns the resulting visitor.
     *
     * @param tla the Annotation to visit
     * @return an AnnotationVisitor for tla
     */
    private AnnotationVisitor visitAnnotation(Annotation tla) {
      return super.visitAnnotation(classNameToDesc(name(tla)), isRuntimeRetention(tla));
    }

    /**
     * Has this visit the annotation in tla and returns the resulting visitor. The sort of the
     * typeReference should be METHOD_TYPE_PARAMETER, METHOD_TYPE_PARAMETER_BOUND, METHOD_RETURN,
     * METHOD_RECEIVER, METHOD_FORMAL_PARAMETER or THROWS.
     *
     * @param tla the Annotation to visit
     * @param typeReference the type of the annotation
     * @param typePath full path to the annotation
     * @return an AnnotationVisitor for tla
     */
    private AnnotationVisitor visitTypeAnnotation(
        Annotation tla, TypeReference typeReference, TypePath typePath) {
      return super.visitTypeAnnotation(
          typeReference.getValue(), typePath, classNameToDesc(name(tla)), isRuntimeRetention(tla));
    }

    /**
     * Has this visit the annotation in tla and returns the resulting visitor. The sort of the
     * typeReference should be LOCAL_VARIABLE or RESOURCE_VARIABLE
     *
     * @param tla the Annotation to visit
     * @param typeReference the type of the annotation
     * @param typePath full path to the annotation
     * @param localLocation the range of the annotation within the method
     * @return an AnnotationVisitor for tla
     */
    private AnnotationVisitor visitLocalVariableAnnotation(
        Annotation tla,
        TypeReference typeReference,
        TypePath typePath,
        LocalLocation localLocation) {
      return super.visitLocalVariableAnnotation(
          typeReference.getValue(),
          typePath,
          localLocation.start,
          localLocation.end,
          localLocation.index,
          classNameToDesc(name(tla)),
          isRuntimeRetention(tla));
    }

    /**
     * Has this visit the annotation in tla and returns the resulting visitor.
     *
     * @param typeSort the type of the annotation
     * @param typeIndex indicates the type if typeSort is CAST
     * @param tla the Annotation to visit
     * @return an AnnotationVisitor for tla
     */
    private AnnotationVisitor visitInsnAnnotation(int typeSort, int typeIndex, Annotation tla) {
      return visitInsnAnnotation(typeSort, typeIndex, tla, null);
    }

    /**
     * Has this visit the annotation in tla and returns the resulting visitor.
     *
     * @param typeSort the type of the annotation
     * @param typeIndex indicates the type if typeSort is CAST
     * @param tla the Annotation to visit
     * @param typePath full path to the annotation
     * @return an AnnotationVisitor for tla
     */
    private AnnotationVisitor visitInsnAnnotation(
        int typeSort, int typeIndex, Annotation tla, TypePath typePath) {
      TypeReference typeReference;
      String desc = classNameToDesc(name(tla));
      boolean visible = isRuntimeRetention(tla);

      switch (typeSort) {
        case TypeReference.INSTANCEOF:
          {
            typeReference = TypeReference.newTypeReference(typeSort);
            break;
          }

        case TypeReference.NEW:
          {
            typeReference = TypeReference.newTypeReference(typeSort);
            break;
          }

        case TypeReference.CAST:
          {
            typeReference = TypeReference.newTypeArgumentReference(typeSort, typeIndex);
            break;
          }
        default:
          throw new IllegalArgumentException();
      }

      return super.visitInsnAnnotation(typeReference.getValue(), typePath, desc, visible);
    }

    /** Visits all the annotations of a method. TODO: better name! */
    private void track() {
      track(TypeReference.INSTANCEOF, 0, aMethod.body.instanceofs);
      track(TypeReference.NEW, 0, aMethod.body.news);
      for (Map.Entry<RelativeLocation, ATypeElement> entry : aMethod.body.typecasts.entrySet()) {
        RelativeLocation loc = entry.getKey();
        if (loc.isBytecodeOffset() && loc.offset == getPreviousCodeOffset()) {
          track(TypeReference.CAST, loc.type_index, aMethod.body.typecasts);
        }
      }
    }

    /**
     * Visits instruction annotations of the given sort at the given location.
     *
     * @param typeSort the type of the annotation
     * @param typeIndex indicates the type if typeSort is CAST
     * @param map from bytecode offset to ATypeElement
     */
    private void track(int typeSort, int typeIndex, Map<RelativeLocation, ATypeElement> map) {
      RelativeLocation loc = RelativeLocation.createOffset(getPreviousCodeOffset(), typeIndex);
      ATypeElement elem = map.get(loc);

      if (elem != null) {
        for (Annotation tla : elem.tlAnnotationsHere) {
          visitInsnAnnotation(typeSort, typeIndex, tla);
        }
        for (Map.Entry<List<TypePathEntry>, ATypeElement> e : elem.innerTypes.entrySet()) {
          TypePath typePath = TypePathEntry.listToTypePath(e.getKey());
          ATypeElement inner = e.getValue();

          for (Annotation tla : inner.tlAnnotationsHere) {
            visitInsnAnnotation(typeSort, typeIndex, tla, typePath);
          }
        }
      }
    }

    /**
     * Has this visit the parameter annotation in tla and returns the resulting visitor.
     *
     * @param tla the Annotation to visit
     * @param index which parameter to visit
     * @return an AnnotationVisitor for tla
     */
    private AnnotationVisitor visitParameterAnnotation(Annotation tla, int index) {
      return super.visitParameterAnnotation(
          index, classNameToDesc(name(tla)), isRuntimeRetention(tla));
    }

    /** Has this visit the declaration annotation and the type annotations on the return type. */
    private void ensureVisitMethodDeclarationAnnotations() {
      // Annotations on method declaration.
      for (Annotation tla : aMethod.tlAnnotationsHere) {
        if (shouldSkip(tla)) {
          continue;
        }

        AnnotationVisitor av = visitAnnotation(tla);
        visitFields(av, tla);
        av.visitEnd();
      }
    }

    /** Has this visit the declaration annotations and the type annotations on the return type. */
    private void ensureVisitReturnTypeAnnotations() {
      // Standard annotations on return type.
      TypeReference typeReference = TypeReference.newTypeReference(TypeReference.METHOD_RETURN);
      visitTypeAnnotationsOnTypeElement(typeReference, aMethod.returnType, true);
    }

    /** Has this visit the annotations on type parameter bounds. */
    private void ensureVisitTypeParameterBoundAnnotations() {
      for (Map.Entry<BoundLocation, ATypeElement> e : aMethod.bounds.entrySet()) {
        BoundLocation bloc = e.getKey();
        ATypeElement bound = e.getValue();
        TypeReference typeReference =
            bloc.boundIndex == -1
                ? TypeReference.newTypeParameterReference(
                    TypeReference.METHOD_TYPE_PARAMETER, bloc.paramIndex)
                : TypeReference.newTypeParameterBoundReference(
                    TypeReference.METHOD_TYPE_PARAMETER_BOUND, bloc.paramIndex, bloc.boundIndex);
        visitTypeAnnotationsOnTypeElement(typeReference, bound, false);
      }
    }

    /** Has this visit the annotations on local variables in this method. */
    private void ensureVisitLocalVariablesAnnotations() {
      TypeReference typeReference = TypeReference.newTypeReference(TypeReference.LOCAL_VARIABLE);
      for (Map.Entry<LocalLocation, AField> entry : aMethod.body.locals.entrySet()) {
        LocalLocation localLocation = entry.getKey();
        AElement aLocation = entry.getValue();

        for (Annotation tla : aLocation.tlAnnotationsHere) {
          if (shouldSkip(tla)) {
            continue;
          }
          // FIXME
          AnnotationVisitor xav =
              visitLocalVariableAnnotation(tla, typeReference, null, localLocation);
          visitFields(xav, tla);
          xav.visitEnd();
        }

        for (Annotation tla : aLocation.type.tlAnnotationsHere) {
          if (shouldSkip(tla)) {
            continue;
          }
          // FIXME
          AnnotationVisitor xav =
              visitLocalVariableAnnotation(tla, typeReference, null, localLocation);
          visitFields(xav, tla);
          xav.visitEnd();
        }

        // now do annotations on inner type of aLocation (local variable)
        for (Map.Entry<List<TypePathEntry>, ATypeElement> e :
            aLocation.type.innerTypes.entrySet()) {
          TypePath localVariableLocation = TypePathEntry.listToTypePath(e.getKey());
          ;
          ATypeElement aInnerType = e.getValue();
          for (Annotation tla : aInnerType.tlAnnotationsHere) {
            if (shouldSkip(tla)) {
              continue;
            }
            AnnotationVisitor xav =
                visitLocalVariableAnnotation(
                    tla, typeReference, localVariableLocation, localLocation);
            visitFields(xav, tla);
            xav.visitEnd();
          }
        }
      }
    }

    /** Has this visit the parameter annotations on this method. */
    private void ensureVisitParameterAnnotations() {
      for (Map.Entry<Integer, AField> entry : aMethod.parameters.entrySet()) {
        AField aParameter = entry.getValue();
        int index = entry.getKey();
        // First visit declaration annotations on the parameter
        for (Annotation tla : aParameter.tlAnnotationsHere) {
          if (shouldSkip(tla)) {
            continue;
          }

          AnnotationVisitor av = visitParameterAnnotation(tla, index);
          visitFields(av, tla);
          av.visitEnd();
        }

        TypeReference typeReference = TypeReference.newFormalParameterReference(index);
        // Then handle type annotations targeting the parameter
        visitTypeAnnotationsOnTypeElement(typeReference, aParameter.type, true);
      }
    }

    /** Has this visit the receiver annotations on this method. */
    private void ensureVisitReceiverAnnotations() {
      AField aReceiver = aMethod.receiver;
      TypeReference typeReference = TypeReference.newTypeReference(TypeReference.METHOD_RECEIVER);
      visitTypeAnnotationsOnTypeElement(typeReference, aReceiver.type, true);
    }

    private void ensureVisitLambdaExpressionAnnotations() {
      for (Map.Entry<RelativeLocation, AMethod> entry : aMethod.body.funs.entrySet()) {
        if (!entry.getKey().isBytecodeOffset()) {
          // if the RelativeLocation is a source index, we cannot insert it
          // into bytecode
          // TODO: output a warning or translate
          if (strict) {
            System.err.println(
                "ClassAnnotationSceneWriter.ensureMemberReferenceAnnotations:"
                    + " no bytecode offset found!");
          }
          continue;
        }
        AMethod aLambda = entry.getValue();

        for (Map.Entry<Integer, AField> e0 : aLambda.parameters.entrySet()) {
          AField aParameter = e0.getValue();
          int index = e0.getKey();
          TypeReference typeReference = TypeReference.newFormalParameterReference(index);
          for (Annotation tla : aParameter.tlAnnotationsHere) {
            if (shouldSkip(tla)) {
              continue;
            }

            AnnotationVisitor av = visitParameterAnnotation(tla, index);
            visitFields(av, tla);
            av.visitEnd();
          }

          for (Annotation tla : aParameter.type.tlAnnotationsHere) {
            if (shouldSkip(tla)) {
              continue;
            }

            // FIXME
            AnnotationVisitor xav = visitTypeAnnotation(tla, typeReference, null);
            // TODO: We have offset now, do we need to do anything with it?
            //              visitOffset(xav, offset);
            visitFields(xav, tla);
            xav.visitEnd();
          }

          for (Map.Entry<List<TypePathEntry>, ATypeElement> e1 :
              aParameter.type.innerTypes.entrySet()) {
            TypePath aParameterLocation = TypePathEntry.listToTypePath(e1.getKey());
            ;
            ATypeElement aInnerType = e1.getValue();
            for (Annotation tla : aInnerType.tlAnnotationsHere) {
              if (shouldSkip(tla)) {
                continue;
              }

              AnnotationVisitor xav = visitTypeAnnotation(tla, typeReference, aParameterLocation);
              // TODO: We have offset. Do we need to use it anywhere?
              //                visitOffset(xav, offset);
              visitFields(xav, tla);
              xav.visitEnd();
            }
          }
        }
      }
    }

    private void ensureVisitMemberReferenceAnnotations() {
      for (Map.Entry<RelativeLocation, ATypeElement> entry : aMethod.body.refs.entrySet()) {
        if (!entry.getKey().isBytecodeOffset()) {
          // if the RelativeLocation is a source index, we cannot insert it
          // into bytecode
          // TODO: output a warning or translate
          if (strict) {
            System.err.println(
                "ClassAnnotationSceneWriter.ensureMemberReferenceAnnotations:"
                    + " no bytecode offset found!");
          }
          continue;
        }
        int offset = entry.getKey().offset;
        int typeIndex = entry.getKey().type_index;
        ATypeElement aTypeArg = entry.getValue();
        Set<Integer> lset = lambdaExpressions.get(aMethod.methodSignature);
        if (lset.contains(offset)) {
          continue;
        } // something's wrong
        Set<Integer> cset = dynamicConstructors.get(aMethod.methodSignature);
        TypeReference typeReference =
            cset != null && cset.contains(offset)
                ? TypeReference.newTypeArgumentReference(
                    TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT, typeIndex)
                : TypeReference.newTypeArgumentReference(
                    TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT, typeIndex);

        visitInsnAnnotationsOnTypeElement(typeReference, aTypeArg, true);
      }
    }

    private void ensureVisitMethodInvocationAnnotations() {
      for (Map.Entry<RelativeLocation, ATypeElement> entry : aMethod.body.calls.entrySet()) {
        if (!entry.getKey().isBytecodeOffset()) {
          // if the RelativeLocation is a source index, we cannot insert it
          // into bytecode
          // TODO: output a warning or translate
          if (strict) {
            System.err.println(
                "ClassAnnotationSceneWriter.ensureVisitMethodInvocationAnnotations:"
                    + " no bytecode offset found!");
          }
        }
        int offset = entry.getKey().offset;
        int typeIndex = entry.getKey().type_index;
        ATypeElement aCall = entry.getValue();
        Set<Integer> cset = dynamicConstructors.get(aMethod.methodSignature);

        TypeReference typeReference =
            cset != null && cset.contains(offset)
                ? TypeReference.newTypeArgumentReference(
                    TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT, typeIndex)
                : TypeReference.newTypeArgumentReference(
                    TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT, typeIndex);

        visitInsnAnnotationsOnTypeElement(typeReference, aCall, true);
      }
    }

    /**
     * Have this method visit the annotations in scene if and only if it has not visited them
     * before.
     */
    private void ensureVisitSceneMethodAnnotations() {
      if (!hasVisitedMethodAnnotations) {
        hasVisitedMethodAnnotations = true;

        ensureVisitMethodDeclarationAnnotations();
        ensureVisitReturnTypeAnnotations();

        // Now iterate through method's locals, news, parameter, receiver,
        // typecasts, and type argument annotations, which will all be
        // annotations.
        ensureVisitTypeParameterBoundAnnotations();
        ensureVisitLocalVariablesAnnotations();
        // ensureVisitObjectCreationAnnotations();
        ensureVisitParameterAnnotations();
        ensureVisitReceiverAnnotations();
        // ensureVisitTypecastAnnotations();
        // ensureVisitTypeTestAnnotations();
        ensureVisitLambdaExpressionAnnotations();
        ensureVisitMemberReferenceAnnotations();
        ensureVisitMethodInvocationAnnotations();
        // TODO: throw clauses?!
        // TODO: catch clauses!?
      }
    }

    /**
     * Search for TypeAnnotations on a TypeElement.
     *
     * @param typeReference the type of annotation to search for
     * @param aTypeElement the element to search for annotations
     * @param maybeSkip whether the annotation might be skipped
     */
    private void visitTypeAnnotationsOnTypeElement(
        TypeReference typeReference, ATypeElement aTypeElement, boolean maybeSkip) {
      for (Annotation tla : aTypeElement.tlAnnotationsHere) {
        if (maybeSkip && shouldSkip(tla)) {
          continue;
        }
        AnnotationVisitor av = visitTypeAnnotation(tla, typeReference, null);
        visitFields(av, tla);
        av.visitEnd();
      }

      // Now do generic/array information on return type
      aTypeElement.innerTypes.forEach(
          (location, innerType) -> {
            TypePath typePath = TypePathEntry.listToTypePath(location);
            for (Annotation tla : innerType.tlAnnotationsHere) {
              if (maybeSkip && shouldSkip(tla)) {
                continue;
              }
              AnnotationVisitor xav = visitTypeAnnotation(tla, typeReference, typePath);
              visitFields(xav, tla);
              xav.visitEnd();
            }
          });
    }

    /**
     * Search for InsnAnnotations on a TypeElement.
     *
     * @param typeReference the type of annotation to search for
     * @param aTypeElement the element to search for annotations
     * @param maybeSkip whether the annotation might be skipped
     */
    private void visitInsnAnnotationsOnTypeElement(
        TypeReference typeReference, ATypeElement aTypeElement, boolean maybeSkip) {
      for (Annotation tla : aTypeElement.tlAnnotationsHere) {
        if (maybeSkip && shouldSkip(tla)) {
          continue;
        }

        AnnotationVisitor xav =
            super.visitInsnAnnotation(
                typeReference.getValue(),
                null,
                classNameToDesc(name(tla)),
                isRuntimeRetention(tla));
        // TODO: We have offset. Do we need to use it anywhere?
        //          visitOffset(xav, offset);
        visitFields(xav, tla);
        xav.visitEnd();
      }

      // now do inner annotations of call
      aTypeElement.innerTypes.forEach(
          (location, aInnerType) -> {
            TypePath typePath = TypePathEntry.listToTypePath(location);
            for (Annotation tla : aInnerType.tlAnnotationsHere) {
              if (maybeSkip && shouldSkip(tla)) {
                continue;
              }

              AnnotationVisitor xav =
                  super.visitInsnAnnotation(
                      typeReference.getValue(),
                      typePath,
                      classNameToDesc(name(tla)),
                      isRuntimeRetention(tla));
              // TODO: We have offset. Do we need to use it anywhere?
              //            visitOffset(xav, offset);
              visitFields(xav, tla);
              xav.visitEnd();
            }
          });
    }
  }

  /**
   * A MethodCodeIndexer is a MethodVisitor that is used to calculate the bytecode offsets to
   * constructor invocations and to lambda invocations.
   */
  class MethodCodeIndexer extends ClassVisitor {

    /** Offset from start of class file to code attribute for method. */
    private int codeStart;

    /** A set of bytecode offsets to constructor invocations. */
    Set<Integer> constrs;

    /** A set of bytecode offsets to lambda invocations. */
    Set<Integer> lambdas;

    /**
     * Constructs a new MethodCodeIndexer.
     *
     * @param api the ASM API version to use
     */
    MethodCodeIndexer(int api) {
      super(api);
      int fieldCount;
      // const pool size is (not lowest) upper bound of string length
      codeStart = classReader.header + 6;
      codeStart += 2 + 2 * classReader.readUnsignedShort(codeStart);
      fieldCount = classReader.readUnsignedShort(codeStart);
      codeStart += 2;
      while (--fieldCount >= 0) {
        int attrCount = classReader.readUnsignedShort(codeStart + 6);
        codeStart += 8;
        while (--attrCount >= 0) {
          codeStart += 6 + classReader.readInt(codeStart + 2);
        }
      }
      codeStart += 2;
    }

    @Override
    public void visit(
        int version,
        int access,
        String name,
        String signature,
        String superName,
        String[] interfaces) {}

    @Override
    public void visitSource(String source, String debug) {}

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {}

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {}

    @Override
    public FieldVisitor visitField(
        int access, String name, String descriptor, String signature, Object value) {
      return null;
    }

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String descriptor, String signature, String[] exceptions) {
      String methodDescription = name + descriptor;
      constrs = dynamicConstructors.get(methodDescription);
      if (constrs == null) {
        constrs = new TreeSet<>();
        dynamicConstructors.put(methodDescription, constrs);
      }
      lambdas = lambdaExpressions.get(methodDescription);
      if (lambdas == null) {
        lambdas = new TreeSet<>();
        lambdaExpressions.put(methodDescription, lambdas);
      }

      return new MethodCodeOffsetAdapter(classReader, null, codeStart) {
        @Override
        public void visitInvokeDynamicInsn(
            String name, String descriptor, Handle bsm, Object... bsmArgs) {
          String methodName = ((Handle) bsmArgs[1]).getName();
          int off = getCurrentOffset();
          if ("<init>".equals(methodName)) {
            constrs.add(off);
          } else {
            int ix = methodName.lastIndexOf('.');
            if (ix >= 0) {
              methodName = methodName.substring(ix + 1);
            }
            if (methodName.startsWith("lambda$")) {
              lambdas.add(off);
            }
          }
          super.visitInvokeDynamicInsn(name, descriptor, bsm, bsmArgs);
        }
      };
    }
  }
}
