// This class is a complete ClassVisitor with many nested classes that do
// the work of reading annotations from a class file and inserting them into
// an AScene.
package org.checkerframework.afu.scenelib.io.classfile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.AnnotationBuilder;
import org.checkerframework.afu.scenelib.AnnotationFactory;
import org.checkerframework.afu.scenelib.Annotations;
import org.checkerframework.afu.scenelib.ArrayBuilder;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.AElement;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.el.BoundLocation;
import org.checkerframework.afu.scenelib.el.LocalLocation;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.afu.scenelib.el.TypeIndexLocation;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.afu.scenelib.field.AnnotationAFT;
import org.checkerframework.afu.scenelib.field.ArrayAFT;
import org.checkerframework.afu.scenelib.field.BasicAFT;
import org.checkerframework.afu.scenelib.field.ClassTokenAFT;
import org.checkerframework.afu.scenelib.field.EnumAFT;
import org.checkerframework.afu.scenelib.field.ScalarAFT;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;

/**
 * A {@code ClassAnnotationSceneReader} is a {@link org.objectweb.asm.ClassVisitor} that will insert
 * all annotations it encounters while visiting a class into a given {@link AScene}.
 *
 * <p>The "read" in {@code ClassAnnotationSceneReader} refers to a class file being read into a
 * scene. Also see {@link ClassAnnotationSceneWriter}.
 *
 * <p>The proper usage of this class is to construct a {@code ClassAnnotationSceneReader} with an
 * {@link AScene} into which annotations should be inserted, then pass this as a {@link
 * org.objectweb.asm.ClassVisitor} to {@link org.objectweb.asm.ClassReader#accept}
 *
 * <p>All other methods are intended to be called only by {@link
 * org.objectweb.asm.ClassReader#accept}, and should not be called anywhere else, due to the order
 * in which {@link org.objectweb.asm.ClassVisitor} methods should be called.
 */
public class ClassAnnotationSceneReader extends CodeOffsetAdapter {
  // general strategy:
  // -only "Runtime[In]visible[Type]Annotations" are supported
  // -use an empty visitor for everything besides annotations, fields and
  //  methods; for those three, use a special visitor that does all the work
  //  and inserts the annotations correctly into the specified AElement

  /** Whether to output tracing information. */
  private static final boolean trace = false;

  /** Whether to output error messages for unsupported cases. */
  private static final boolean strict = false;

  /** Whether to include annotations on compiler-generated methods. */
  private final boolean ignoreBridgeMethods;

  /** The scene into which this class will insert annotations. */
  private final AScene scene;

  /** The AClass that will be visited, which already contains annotations. */
  private AClass aClass;

  /** ClassReader for reading the class file. */
  @SuppressWarnings("HidingField") // TODO!!
  private final ClassReader classReader;

  /** ClassWriter for generating the modified class file. */
  private final ClassWriter classWriter;

  /**
   * Holds definitions we've seen so far. Maps from annotation name to the definition itself. Maps
   * from both the qualified name and the unqualified name. If the unqualified name is not unique,
   * it maps to null and the qualified name should be used instead.
   */
  private final Map<String, AnnotationDef> annotationDefinitions =
      initialiseAnnotationDefinitions();

  private static Map<String, AnnotationDef> initialiseAnnotationDefinitions() {
    Map<String, AnnotationDef> result = new HashMap<>();
    for (AnnotationDef ad : Annotations.standardDefs) {
      result.put(ad.name, ad);
    }
    return result;
  }

  /**
   * Constructs a new {@code ClassAnnotationSceneReader} that will insert all the annotations in the
   * class that it visits into {@code scene}.
   *
   * @param api the ASM API version to use
   * @param classReader the {@link ClassReader} that visits this {@code ClassAnnotationSceneReader}
   * @param scene the annotation scene into which annotations this visits will be inserted
   * @param ignoreBridgeMethods whether to omit annotations on compiler-generated methods
   */
  public ClassAnnotationSceneReader(
      int api, ClassReader classReader, AScene scene, boolean ignoreBridgeMethods) {
    super(api, classReader);
    this.classReader = classReader;
    this.classWriter = new ClassWriter(this.classReader, api);
    super.cv = classWriter;
    this.scene = scene;
    this.ignoreBridgeMethods = ignoreBridgeMethods;
  }

  @Override
  public void visit(
      int version,
      int access,
      String name,
      String signature,
      String superName,
      String[] interfaces) {
    classWriter.visit(version, access, name, signature, superName, interfaces);
    aClass = scene.classes.getVivify(name.replace('/', '.'));
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    if (trace) {
      System.out.printf(
          "visitAnnotation(%s, %s) in %s (%s)%n", descriptor, visible, this, this.getClass());
    }
    AnnotationVisitor annotationWriter = classWriter.visitAnnotation(descriptor, visible);
    return new AnnotationSceneReader(this.api, descriptor, visible, aClass, annotationWriter);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
      int typeRef, TypePath typePath, String descriptor, boolean visible) {
    if (trace) {
      System.out.printf(
          "visitTypeAnnotation(%s, %s, %s, %s); aClass=%s in %s (%s)%n",
          typeRef, typePath, descriptor, visible, aClass, this, this.getClass());
    }
    // typeRef.getSort(): TypeReference.CLASS_TYPE_PARAMETER,
    // TypeReference.CLASS_TYPE_PARAMETER_BOUND or TypeReference.CLASS_EXTENDS.
    AnnotationVisitor annotationWriter =
        classWriter.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    return new TypeAnnotationSceneReader(
        this.api,
        descriptor,
        visible,
        aClass,
        annotationWriter,
        typeRef,
        typePath,
        null,
        null,
        null,
        null);
  }

  @Override
  public FieldVisitor visitField(
      int access, String name, String descriptor, String signature, Object value) {
    if (trace) {
      System.out.printf(
          "visitField(%s, %s, %s, %s, %s) in %s (%s)%n",
          access, name, descriptor, signature, value, this, this.getClass());
    }
    AField aField = aClass.fields.getVivify(name);
    FieldVisitor fieldWriter = classWriter.visitField(access, name, descriptor, signature, value);
    return new FieldAnnotationSceneReader(this.api, aField, fieldWriter);
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String descriptor, String signature, String[] exceptions) {
    if (ignoreBridgeMethods && (access & Opcodes.ACC_BRIDGE) != 0) {
      return null;
    }
    if (trace) {
      System.out.printf(
          "visitMethod(%s, %s, %s, %s, %s) in %s (%s)%n",
          access, name, descriptor, signature, Arrays.toString(exceptions), this, this.getClass());
    }
    AMethod aMethod = aClass.methods.getVivify(name + descriptor);
    MethodVisitor methodWriter = super.visitMethod(access, name, descriptor, signature, exceptions);
    return new MethodAnnotationSceneReader(this.api, aMethod, methodWriter);
  }

  /**
   * Converts JVML format to Java format.
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

  // ///////////////////////////////////////////////////////////////////////////
  // Inner classes
  //

  // Hackish workaround for odd subclassing.
  String dummyDesc = "dummy";

  /**
   * AnnotationSceneReader contains most of the complexity behind reading annotations from a class
   * file into a scene. It keeps an AElement of the element into which this should insert the
   * annotations it visits in a class file. Thus, constructing an AnnotationSceneReader with an
   * AElement of the right type is sufficient for writing out annotations to that element, which
   * will be done once visitEnd() is called. Note that for when inner annotations are expected, the
   * AElement passed in must be of the correct form (ATypeElement or AMethod depending on the target
   * type of the annotation).
   */
  private class AnnotationSceneReader extends AnnotationVisitor {
    // Implementation strategy:
    // For field values and enums, delegate to field annotationBuilder, which does the work.
    // For arrays, use an ArrayAnnotationBuilder that will
    //  properly call the right annotationBuilder methods on its visitEnd().
    // For nested annotations, use a NestedAnnotationSceneReader that will
    //  properly call the right annotationBuilder methods on its visitEnd().
    // For type annotations, see TypeAnnotationSceneReader.

    /** The AElement into which the annotation visited should be inserted. */
    protected AElement aElement;

    /** Whether or not this annotation is visible at run time. */
    protected boolean visible;

    /** The AnnotationBuilder used to create this annotation. */
    private AnnotationBuilder annotationBuilder;

    /** The AnnotationVisitor used to visit this annotation. */
    protected AnnotationVisitor annotationWriter;

    /** An offset into the method's bytecodes. */
    protected int savedOffset;

    /**
     * Retrieve annotation definition, including retention policy.
     *
     * @param jvmlClassName the name of an annotation class
     * @return an annotation definition for the class
     */
    @SuppressWarnings("unchecked")
    private AnnotationDef getAnnotationDef(String jvmlClassName) {
      String annoTypeName = classDescToName(jvmlClassName);
      // It would be better to not require the .class file to be on the
      // classpath, but to search for it on a path that is passed to this
      // program.  Worry about that later.
      Class<? extends java.lang.annotation.Annotation> annoClass;
      try {
        annoClass = (Class<? extends java.lang.annotation.Annotation>) Class.forName(annoTypeName);
      } catch (ClassNotFoundException e) {
        if (annoTypeName.contains("+")) {
          // This is an internal JDK annotation such as jdk.Profile+Annotation .
          @SuppressWarnings("signature:assignment") // special annotation with "+" in name
          @BinaryName String annoTypeName2 = annoTypeName;
          return Annotations.createValueAnnotationDef(
              annoTypeName2,
              Annotations.noAnnotations,
              BasicAFT.forType(int.class),
              String.format("Could not find class %s: %s", jvmlClassName, e.getMessage()));
        }
        System.out.printf("Could not find class: %s%n", e.getMessage());
        printClasspath();
        throw new Error(e);
      }

      AnnotationDef ad = AnnotationDef.fromClass(annoClass, annotationDefinitions);
      return ad;
    }

    /**
     * Constructs a new AnnotationScene reader with the given description and visibility. Calling
     * visitEnd() will ensure that this writes out the annotation it visits into aElement.
     *
     * @param api the ASM API version to use
     * @param descriptor the class descriptor of the enumeration class
     * @param visible whether or not this annotation is visible at run time
     * @param aElement the AElement into which the annotation visited should be inserted
     * @param annotationWriter the AnnotationWriter passed by the caller
     */
    @SuppressWarnings("ReferenceEquality") // interned comparison
    AnnotationSceneReader(
        int api,
        String descriptor,
        boolean visible,
        AElement aElement,
        AnnotationVisitor annotationWriter) {
      super(api, annotationWriter);
      this.visible = visible;
      this.aElement = aElement;
      this.annotationWriter = annotationWriter;
      if (trace) {
        System.out.printf("AnnotationSceneReader(%s, %s, %s)%n", descriptor, visible, aElement);
      }

      if (descriptor != dummyDesc) { // interned
        AnnotationDef ad = getAnnotationDef(descriptor);

        AnnotationBuilder ab =
            AnnotationFactory.saf.beginAnnotation(
                ad,
                // "ClassReader " + cr.getClassName()
                "TODO: ClassAnnotationSceneReader");
        if (ab == null) {
          throw new IllegalArgumentException("bad descriptor: " + descriptor);
        } else {
          this.annotationBuilder = ab;
        }
      }
    }

    // Adds a field to the annotation being built.
    @SuppressWarnings("signature") // ASM is not annotated yet
    @Override
    public void visit(String name, Object value) {
      if (trace) {
        System.out.printf("visit(%s, %s) on %s%n", name, value, this);
      }
      // BasicAFT.forType(Class) expects primitive int.class instead of boxed Integer.class,
      // and so on for all primitives.  String.class is ok, since it has no
      // primitive type.
      Class<?> c = value.getClass();
      if (c.equals(Boolean.class)) {
        c = boolean.class;
      } else if (c.equals(Byte.class)) {
        c = byte.class;
      } else if (c.equals(Character.class)) {
        c = char.class;
      } else if (c.equals(Short.class)) {
        c = short.class;
      } else if (c.equals(Integer.class)) {
        c = int.class;
      } else if (c.equals(Long.class)) {
        c = long.class;
      } else if (c.equals(Float.class)) {
        c = float.class;
      } else if (c.equals(Double.class)) {
        c = double.class;
      } else if (c.equals(Type.class)) {
        try {
          annotationBuilder.addScalarField(
              name, ClassTokenAFT.ctaft, Class.forName(((Type) value).getClassName()));
        } catch (ClassNotFoundException e) {
          throw new RuntimeException("Could not load Class for Type: " + value, e);
        }
        // Return here, otherwise the annotationBuilder would be called
        // twice for the same value.
        return;
      } else if (!c.equals(String.class)) {
        // Only possible type for value is String, in which case c is already
        // String.class, or array of primitive
        c = c.getComponentType();
        ArrayBuilder arrayBuilder =
            annotationBuilder.beginArrayField(name, new ArrayAFT(BasicAFT.forType(c)));
        // value is of type c[], now add in all the elements of the array
        for (Object o : asList(value)) {
          arrayBuilder.appendElement(o);
        }
        arrayBuilder.finish();
        return;
      }

      // handle everything but arrays
      annotationBuilder.addScalarField(name, BasicAFT.forType(c), value);
    }

    /**
     * Method that accepts an Object whose actual type is c[], where c is a primitive, and returns
     * an equivalent {@code List<Object>} that contains the same elements as in primitiveArray.
     *
     * @param primitiveArray an array of primitive type
     * @return a list containing the contents of {@code primitiveArray}
     */
    private List<Object> asList(Object primitiveArray) {
      List<Object> objects = new ArrayList<>();
      Class<?> c = primitiveArray.getClass().getComponentType();
      if (c.equals(boolean.class)) {
        for (boolean o : (boolean[]) primitiveArray) {
          objects.add(o);
        }
      } else if (c.equals(byte.class)) {
        for (byte o : (byte[]) primitiveArray) {
          objects.add(o);
        }
      } else if (c.equals(char.class)) {
        for (char o : (char[]) primitiveArray) {
          objects.add(o);
        }
      } else if (c.equals(short.class)) {
        for (short o : (short[]) primitiveArray) {
          objects.add(o);
        }
      } else if (c.equals(int.class)) {
        for (int o : (int[]) primitiveArray) {
          objects.add(o);
        }
      } else if (c.equals(long.class)) {
        for (long o : (long[]) primitiveArray) {
          objects.add(o);
        }
      } else if (c.equals(float.class)) {
        for (float o : (float[]) primitiveArray) {
          objects.add(o);
        }
      } else if (c.equals(double.class)) {
        for (double o : (double[]) primitiveArray) {
          objects.add(o);
        }
      } else {
        throw new RuntimeException("Array has non-primitive type " + c + ": " + primitiveArray);
      }
      return objects;
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
      if (trace) {
        System.out.printf(
            "visitEnum(%s, %s) in %s (%s)%n", name, descriptor, this, this.getClass());
      }
      annotationBuilder.addScalarField(name, new EnumAFT(descriptor), value);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
      if (trace) {
        System.out.printf(
            "visitAnnotation(%s, %s) in %s (%s)%n", name, descriptor, this, this.getClass());
      }
      AnnotationVisitor annotationWriter = this.annotationWriter.visitAnnotation(name, descriptor);
      return new NestedAnnotationSceneReader(this.api, this, name, descriptor, annotationWriter);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
      if (trace) {
        System.out.printf("visitArray(%s) in %s (%s)%n", name, this, this.getClass());
      }
      AnnotationVisitor annotationWriter = this.annotationWriter.visitArray(name);
      return new ArrayAnnotationSceneReader(this.api, this, name, annotationWriter);
    }

    /**
     * Save the offset into a method's bytecodes.
     *
     * @param offset bytecode offset
     */
    protected void saveOffset(int offset) {
      savedOffset = offset;
    }

    /**
     * Visits the end of the annotation, and actually writes out the annotation into aElement.
     *
     * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
     */
    @Override
    public void visitEnd() {
      if (trace) {
        System.out.printf("visitEnd on %s (%s)%n", this, this.getClass());
      }
      annotationWriter.visitEnd();
      Annotation a = makeAnnotation();

      if (a.def.isTypeAnnotation() && (aElement instanceof AMethod)) {
        AMethod m = (AMethod) aElement;
        m.returnType.tlAnnotationsHere.add(a);

        // There is not currently a separate location for field/parameter
        // type annotations; they are mixed in with the declaration
        // annotations.  This should be fixed in the future.
        // Also, fields/parameters are just represented as AElement.
        // } else if (a.def.isTypeAnnotation() && (aElement instanceof AField)) {

      } else {
        aElement.tlAnnotationsHere.add(a);
      }
    }

    // The following are utility methods to facilitate creating all the
    // necessary data structures in the scene library.

    /**
     * Returns an annotation, ready to be placed into the scene, from the information visited.
     *
     * @return the annotation to be placed into the scene
     */
    Annotation makeAnnotation() {
      return annotationBuilder.finish();
    }

    /**
     * Hook for NestedAnnotationSceneReader; overridden by ArrayAnnotationSceneReader to add an
     * array element instead of a field.
     *
     * @param fieldName name of field
     * @param annotation annotation to be added to the field
     */
    void supplySubannotation(String fieldName, Annotation annotation) {
      annotationBuilder.addScalarField(fieldName, new AnnotationAFT(annotation.def()), annotation);
    }

    @Override
    public String toString() {
      return String.format(
          "(AnnotationSceneReader: %s %s %s)", aElement, visible, annotationBuilder);
    }
  }

  /**
   * Handles all the logic related to reading Type Annotations and creating the appropriate scene
   * element. The visitEnd() method chooses the appropriate scene element for the correct
   * TypeReference.getSort() (which is the target type of the type annotation). So if new target
   * types are added, the visitEnd method needs to be updated accordingly.
   */
  private class TypeAnnotationSceneReader extends AnnotationSceneReader {
    // Implementation strategy:
    // For type annotation information, store all arguments passed in and on
    //  this.visitEnd(), handle all the information based on target type.

    /** A reference to the annotated type. */
    private final TypeReference typeReference;

    /**
     * The path to the annotated type argument, wildcard bound, array element type, or static inner
     * type within 'typeRef'. May be null if the annotation targets 'typeRef' as a whole.
     */
    private final TypePath typePath;

    /**
     * The starts of the scopes of the element being visited. Used only for
     * TypeReference#LOCAL_VARIABLE and TypeReference#RESOURCE_VARIABLE.
     */
    private final Label[] start;

    /**
     * The ends of the scopes of the element being visited. Used only for
     * TypeReference#LOCAL_VARIABLE and TypeReference#RESOURCE_VARIABLE.
     */
    private final Label[] end;

    /**
     * The indices of the element being visited in the classfile. Used only for
     * TypeReference#LOCAL_VARIABLE and TypeReference#RESOURCE_VARIABLE.
     */
    private final int[] index;

    /** The name of the local variable being visited. */
    private final @Nullable String localVariableName;

    /**
     * Constructs a new TypeAnnotationSceneReader with the given description and visibility. Calling
     * visitEnd() will ensure that this writes out the annotation it visits into aElement.
     *
     * @param api the ASM API version to use
     * @param descriptor the descriptor of the reader
     * @param visible whether or not this annotation is visible at run time
     * @param aElement the AElement into which the annotation visited should be inserted
     * @param annotationWriter the AnnotationWriter passed by the caller
     * @param typeRef A reference to the annotated type. This has information about the target type,
     *     param index and bound index for the type annotation. @see org.objectweb.asm.TypeReference
     * @param typePath The path to the annotated type argument, wildcard bound, array element type,
     *     or static inner type within 'typeRef'. May be null if the annotation targets 'typeRef' as
     *     a whole.
     * @param start the start of the scopes of the element being visited. Used only for
     *     TypeReference#LOCAL_VARIABLE and TypeReference#RESOURCE_VARIABLE.
     * @param end the end of the scopes of the element being visited. Used only for
     *     TypeReference#LOCAL_VARIABLE and TypeReference#RESOURCE_VARIABLE.
     * @param index The indices of the element being visited in the classfile. Used only for
     *     TypeReference#LOCAL_VARIABLE and TypeReference#RESOURCE_VARIABLE.
     */
    TypeAnnotationSceneReader(
        int api,
        String descriptor,
        boolean visible,
        AElement aElement,
        AnnotationVisitor annotationWriter,
        int typeRef,
        TypePath typePath,
        Label[] start,
        Label[] end,
        int[] index) {
      this(
          api,
          descriptor,
          visible,
          aElement,
          annotationWriter,
          typeRef,
          typePath,
          start,
          end,
          index,
          null);
    }

    /**
     * Constructs a new AnnotationScene reader with the given description and visibility. Calling
     * visitEnd() will ensure that this writes out the annotation it visits into aElement.
     *
     * @param api the ASM API version to use
     * @param descriptor the descriptor of the reader
     * @param visible whether or not this annotation is visible at run time
     * @param aElement the AElement into which the annotation visited should be inserted
     * @param annotationWriter the AnnotationWriter passed by the caller
     * @param typeRef A reference to the annotated type. This has information about the target type,
     *     param index and bound index for the type annotation. @see org.objectweb.asm.TypeReference
     * @param typePath The path to the annotated type argument, wildcard bound, array element type,
     *     or static inner type within 'typeRef'. May be null if the annotation targets 'typeRef' as
     *     a whole.
     * @param start the start of the scopes of the element being visited. Used only for
     *     TypeReference#LOCAL_VARIABLE and TypeReference#RESOURCE_VARIABLE.
     * @param end the end of the scopes of the element being visited. Used only for
     *     TypeReference#LOCAL_VARIABLE and TypeReference#RESOURCE_VARIABLE.
     * @param index The indices of the element being visited in the classfile. Used only for
     *     TypeReference#LOCAL_VARIABLE and TypeReference#RESOURCE_VARIABLE.
     * @param localVariableName the name of the local variable being visited; may be null
     */
    TypeAnnotationSceneReader(
        int api,
        String descriptor,
        boolean visible,
        AElement aElement,
        AnnotationVisitor annotationWriter,
        int typeRef,
        TypePath typePath,
        Label[] start,
        Label[] end,
        int[] index,
        @Nullable String localVariableName) {
      super(api, descriptor, visible, aElement, annotationWriter);
      this.typeReference = new TypeReference(typeRef);
      this.typePath = typePath;
      this.start = start;
      this.end = end;
      this.index = index;
      if (typeReference.getSort() != TypeReference.LOCAL_VARIABLE
          && typeReference.getSort() != TypeReference.RESOURCE_VARIABLE) {
        if (start != null || end != null || index != null) {
          System.err.printf(
              "Error: LOCAL_VARIABLE and RESOURCE_VARIABLE TypeReference"
                  + " with start = %s, end = %s, index = %s",
              Arrays.toString(start), Arrays.toString(end), Arrays.toString(index));
        }
      }
      this.localVariableName = localVariableName;
    }

    /**
     * Visits the end of the annotation, and actually writes out the annotation into aElement. Needs
     * to be updated whenever a new target type is added.
     *
     * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
     */
    @Override
    public void visitEnd() {
      if (trace) {
        System.out.printf("visitEnd on %s (%s)%n", this, this.getClass());
      }
      annotationWriter.visitEnd();
      // TEMP
      // If the expression used to initialize a field contains annotations
      // on instanceOfs, typecasts, or news, javac enters
      // those annotations on the field.  If we get such an annotation and
      // aElement is a field, skip the annotation for now to avoid crashing.
      try {
        switch (typeReference.getSort()) {
          case TypeReference.CLASS_TYPE_PARAMETER_BOUND:
            handleClassTypeParameterBound((AClass) aElement);
            break;
          case TypeReference.CLASS_TYPE_PARAMETER:
            handleClassTypeParameter((AClass) aElement);
            break;
          case TypeReference.CLASS_EXTENDS:
            handleClassExtends((AClass) aElement);
            break;
          case TypeReference.FIELD:
            handleField(aElement);
            break;
          case TypeReference.LOCAL_VARIABLE:
          case TypeReference.RESOURCE_VARIABLE:
            if (aElement instanceof AMethod) {
              handleMethodLocalVariable((AMethod) aElement);
            } else {
              // TODO: in field initializers
              if (strict) {
                System.err.println("Unhandled local variable annotation for " + aElement);
              }
            }
            break;
          case TypeReference.NEW:
            if (aElement instanceof AMethod) {
              handleMethodObjectCreation((AMethod) aElement);
            } else {
              // TODO: in field initializers
              if (strict) {
                System.err.println("Unhandled NEW annotation for " + aElement);
              }
            }
            break;
          case TypeReference.METHOD_FORMAL_PARAMETER:
            handleMethodFormalParameter((AMethod) aElement);
            break;
          case TypeReference.METHOD_RECEIVER:
            handleMethodReceiver((AMethod) aElement);
            break;
          case TypeReference.CAST:
            if (aElement instanceof AMethod) {
              handleMethodTypecast((AMethod) aElement);
            } else {
              // TODO: in field initializers
              if (strict) {
                System.err.println("Unhandled TYPECAST annotation for " + aElement);
              }
            }
            break;
          case TypeReference.METHOD_RETURN:
            handleMethodReturnType((AMethod) aElement);
            break;
          case TypeReference.INSTANCEOF:
            if (aElement instanceof AMethod) {
              handleMethodInstanceOf((AMethod) aElement);
            } else {
              // TODO: in field initializers
              if (strict) {
                System.err.println("Unhandled INSTANCEOF annotation for " + aElement);
              }
            }
            break;
          case TypeReference.METHOD_TYPE_PARAMETER_BOUND:
            handleMethodTypeParameterBound((AMethod) aElement);
            break;
          case TypeReference.THROWS:
            handleThrows((AMethod) aElement);
            break;
          case TypeReference.CONSTRUCTOR_REFERENCE: // TODO
          case TypeReference.METHOD_REFERENCE:
            handleMethodReference((AMethod) aElement);
            break;
          case TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT: // TODO
          case TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT:
            handleReferenceTypeArgument((AMethod) aElement);
            break;
          case TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT: // TODO
          case TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT:
            handleInvocationTypeArgument((AMethod) aElement);
            break;
          case TypeReference.METHOD_TYPE_PARAMETER:
            handleMethodTypeParameter((AMethod) aElement);
            break;
          case TypeReference.EXCEPTION_PARAMETER: // TODO: Change if this error is ever thrown.
            throw new Error("EXCEPTION_PARAMETER TypeReference case.");
          // TODO: ensure all cases covered.
          default:
            throw new RuntimeException("Unknown TypeReference: " + typeReference.getSort());
        }
      } catch (ClassCastException e) {
        System.err.println("Exception trace: " + e.getMessage());
        System.err.println("Classfile is malformed. Ignoring this type annotation.");
        System.err.println("    This AElement: " + aElement);
        System.err.println("    This TypeReference: " + typeReference.getValue());
      }
    }

    /**
     * Creates the class type parameter annotation on aClass. Works for {@link
     * TypeReference#CLASS_TYPE_PARAMETER}.
     *
     * @param aClass the annotatable class in which annotation will be inserted
     */
    private void handleClassTypeParameter(AClass aClass) {
      // TODO: Any reason this does not need to check for non-null typePath as in
      // handleClassTypeParameterBound()?
      aClass.bounds.getVivify(makeBoundLocation()).tlAnnotationsHere.add(makeAnnotation());
    }

    /**
     * Creates the class type parameter bound annotation on aClass. Works for {@link
     * TypeReference#CLASS_TYPE_PARAMETER_BOUND}.
     *
     * @param aClass the annotatable class in which annotation will be inserted
     */
    private void handleClassTypeParameterBound(AClass aClass) {
      if (typePath == null) {
        aClass.bounds.getVivify(makeBoundLocation()).tlAnnotationsHere.add(makeAnnotation());
      } else {
        aClass
            .bounds
            .getVivify(makeBoundLocation())
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Creates the class extends annotation on aClass. Works for {@link
     * TypeReference#CLASS_EXTENDS}.
     *
     * @param aClass the annotatable class in which annotation will be inserted
     */
    private void handleClassExtends(AClass aClass) {
      TypeIndexLocation typeIndexLocation =
          new TypeIndexLocation(typeReference.getSuperTypeIndex());
      if (typePath == null) {
        aClass
            .extendsImplements
            .getVivify(typeIndexLocation)
            .tlAnnotationsHere
            .add(makeAnnotation());
      } else {
        aClass
            .extendsImplements
            .getVivify(typeIndexLocation)
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Creates the inner annotation on aElement.innerTypes. Works for {@link TypeReference#FIELD}.
     *
     * @param aElement the annotatable element in which annotation will be inserted
     */
    private void handleField(AElement aElement) {
      if (aElement instanceof AClass) {
        // handleFieldGenericArrayOnClass((AClass) aElement);
        if (strict) {
          System.err.println("Unhandled FIELD_COMPONENT annotation for " + aElement);
        }
      } else if (aElement instanceof ATypeElement) {
        ATypeElement aTypeElement = (ATypeElement) aElement;
        if (typePath == null) {
          aTypeElement.tlAnnotationsHere.add(makeAnnotation());
        } else {
          aTypeElement
              .innerTypes
              .getVivify(TypePathEntry.typePathToList(typePath))
              .tlAnnotationsHere
              .add(makeAnnotation());
        }
      } else {
        throw new RuntimeException("Unknown FIELD_COMPONENT: " + aElement);
      }
    }

    /**
     * Creates the method parameter type generic/array annotation on aMethod. Works for {@link
     * TypeReference#METHOD_FORMAL_PARAMETER}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleMethodFormalParameter(AMethod aMethod) {
      if (typePath == null) {
        aMethod
            .parameters
            .getVivify(typeReference.getFormalParameterIndex())
            .type
            .tlAnnotationsHere
            .add(makeAnnotation());
      } else {
        aMethod
            .parameters
            .getVivify(typeReference.getFormalParameterIndex())
            .type
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Creates the method type parameter bound annotation on aMethod. Works for {@link
     * TypeReference#METHOD_TYPE_PARAMETER}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleMethodTypeParameter(AMethod aMethod) {
      aMethod.bounds.getVivify(makeBoundLocation()).tlAnnotationsHere.add(makeAnnotation());
    }

    /**
     * Creates the method type parameter bound annotation on aMethod. Works for {@link
     * TypeReference#METHOD_TYPE_PARAMETER_BOUND}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleMethodTypeParameterBound(AMethod aMethod) {
      if (typePath == null) {
        aMethod.bounds.getVivify(makeBoundLocation()).tlAnnotationsHere.add(makeAnnotation());
      } else {
        aMethod
            .bounds
            .getVivify(makeBoundLocation())
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Creates the method return type generic/array annotation on aMethod. Works for {@link
     * TypeReference#METHOD_RETURN}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleMethodReturnType(AMethod aMethod) {
      // TODO: why is this traced and not other stuff?
      if (trace) {
        System.out.printf("handleMethodReturnType(%s)%n", aMethod);
      }
      if (typePath == null) {
        aMethod.returnType.tlAnnotationsHere.add(makeAnnotation());
      } else {
        aMethod
            .returnType
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Creates the method receiver annotation on aMethod. Works for {@link
     * TypeReference#METHOD_RECEIVER}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleMethodReceiver(AMethod aMethod) {
      if (typePath == null) {
        aMethod.receiver.type.tlAnnotationsHere.add(makeAnnotation());
      } else {
        aMethod
            .receiver
            .type
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Works for {@link TypeReference#THROWS}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleThrows(AMethod aMethod) {
      TypeIndexLocation typeIndexLocation =
          new TypeIndexLocation(typeReference.getExceptionIndex());
      aMethod.throwsException.getVivify(typeIndexLocation).tlAnnotationsHere.add(makeAnnotation());
    }

    /**
     * Creates the object creation annotation on aMethod. Works for {@link TypeReference#NEW}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleMethodObjectCreation(AMethod aMethod) {
      saveOffset(getPreviousCodeOffset());
      if (typePath == null) {
        aMethod.body.news.getVivify(makeOffset(false)).tlAnnotationsHere.add(makeAnnotation());
      } else {
        aMethod
            .body
            .news
            .getVivify(makeOffset(false))
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Creates the method instance of annotation on aMethod. Works for {@link
     * TypeReference#INSTANCEOF}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleMethodInstanceOf(AMethod aMethod) {
      saveOffset(getPreviousCodeOffset());
      if (typePath == null) {
        aMethod
            .body
            .instanceofs
            .getVivify(makeOffset(false))
            .tlAnnotationsHere
            .add(makeAnnotation());
      } else {
        aMethod
            .body
            .instanceofs
            .getVivify(makeOffset(false))
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Works for {@link TypeReference#METHOD_REFERENCE} and {@link
     * TypeReference#CONSTRUCTOR_REFERENCE}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleMethodReference(AMethod aMethod) {
      if (typePath == null) {
        aMethod.body.refs.getVivify(makeOffset(false)).tlAnnotationsHere.add(makeAnnotation());
      } else {
        aMethod
            .body
            .refs
            .getVivify(makeOffset(false))
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Creates the typecast annotation on aMethod. Works for {@link TypeReference#CAST}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleMethodTypecast(AMethod aMethod) {
      saveOffset(getPreviousCodeOffset());
      if (typePath == null) {
        aMethod.body.typecasts.getVivify(makeOffset(true)).tlAnnotationsHere.add(makeAnnotation());
      } else {
        aMethod
            .body
            .typecasts
            .getVivify(makeOffset(true))
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Works for {@link TypeReference#METHOD_INVOCATION_TYPE_ARGUMENT} and {@link
     * TypeReference#CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleInvocationTypeArgument(AMethod aMethod) {
      if (typePath == null) {
        aMethod.body.calls.getVivify(makeOffset(true)).tlAnnotationsHere.add(makeAnnotation());
      } else {
        aMethod
            .body
            .calls
            .getVivify(makeOffset(true))
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Works for {@link TypeReference#METHOD_REFERENCE_TYPE_ARGUMENT} and {@link
     * TypeReference#CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleReferenceTypeArgument(AMethod aMethod) {
      if (typePath == null) {
        aMethod.body.refs.getVivify(makeOffset(true)).tlAnnotationsHere.add(makeAnnotation());
      } else {
        aMethod
            .body
            .refs
            .getVivify(makeOffset(true))
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Creates the local variable annotation on aMethod. Works for {@link
     * TypeReference#LOCAL_VARIABLE} and {@link TypeReference#RESOURCE_VARIABLE}.
     *
     * @param aMethod the annotatable method in which annotation will be inserted
     */
    private void handleMethodLocalVariable(AMethod aMethod) {
      if (typePath == null) {
        aMethod
            .body
            .locals
            .getVivify(makeLocalLocation())
            .type
            .tlAnnotationsHere
            .add(makeAnnotation());
      } else {
        aMethod
            .body
            .locals
            .getVivify(makeLocalLocation())
            .type
            .innerTypes
            .getVivify(TypePathEntry.typePathToList(typePath))
            .tlAnnotationsHere
            .add(makeAnnotation());
      }
    }

    /**
     * Makes a LocalLocation for this annotation.
     *
     * @return a LocalLocation for this annotation
     */
    private LocalLocation makeLocalLocation() {
      return new LocalLocation(start, end, index, localVariableName);
    }

    /**
     * Returns the offset for this annotation.
     *
     * @param needTypeIndex flag denoting whether we need the type index or not
     * @return a RelativeLocation for this annotation
     */
    private RelativeLocation makeOffset(boolean needTypeIndex) {
      int typeIndex = needTypeIndex ? typeReference.getTypeArgumentIndex() : -1;
      return RelativeLocation.createOffset(savedOffset, typeIndex);
    }

    /**
     * Returns the bound location for this annotation. Works with {@link
     * TypeReference#CLASS_TYPE_PARAMETER}, {@link TypeReference#METHOD_TYPE_PARAMETER}, {@link
     * TypeReference#CLASS_TYPE_PARAMETER_BOUND} or {@link
     * TypeReference#METHOD_TYPE_PARAMETER_BOUND}.
     *
     * @return the bound location for this annotation
     */
    private BoundLocation makeBoundLocation() {
      return typeReference.getSort() == TypeReference.CLASS_TYPE_PARAMETER_BOUND
              || typeReference.getSort() == TypeReference.METHOD_TYPE_PARAMETER_BOUND
          ? new BoundLocation(
              typeReference.getTypeParameterIndex(), typeReference.getTypeParameterBoundIndex())
          : new BoundLocation(typeReference.getTypeParameterIndex(), -1);
      // TODO: Give up on unbounded wildcards for now!
    }

    @Override
    public String toString() {
      return "TypeAnnotationSceneReader{"
          + "aElement="
          + aElement
          + ", visible="
          + visible
          + ", typeReference="
          + typeReference
          + ", typePath="
          + typePath
          + ", start="
          + Arrays.toString(start)
          + ", end="
          + Arrays.toString(end)
          + ", index="
          + Arrays.toString(index)
          + ", localVariableName="
          + localVariableName
          + '}';
    }
  }

  /**
   * A NestedAnnotationSceneReader is an AnnotationSceneReader that will read in an entire
   * annotation on a field (of type annotation) of its parent, and once it has fully visited that
   * annotation, it will call its parent annotation builder to include that field, so after its
   * parent constructs and returns this as an AnnotationVisitor (visitAnnotation()), it no longer
   * needs to worry about that field.
   */
  private class NestedAnnotationSceneReader extends AnnotationSceneReader {

    /** Parent of current scene. */
    private final AnnotationSceneReader parent;

    /** Name of the field to visit. */
    private final String name;

    /**
     * Creates a NestedAnnotationSceneReader.
     *
     * @param api the ASM API version to use
     * @param parent the parent AnnotationSceneReader
     * @param name the name of the field
     * @param descriptor the descriptor of the field
     * @param annotationWriter the writer to output the annotations
     */
    NestedAnnotationSceneReader(
        int api,
        AnnotationSceneReader parent,
        String name,
        String descriptor,
        AnnotationVisitor annotationWriter) {
      super(api, descriptor, parent.visible, parent.aElement, annotationWriter);
      if (trace) {
        System.out.printf("NestedAnnotationSceneReader(%s, %s, %s)%n", parent, name, descriptor);
      }
      this.parent = parent;
      this.name = name;
      // this.descriptor = descriptor;
    }

    @Override
    public void visitEnd() {
      // Do not call super, as that already builds the annotation, causing an exception.
      // super.visitEnd();
      annotationWriter.visitEnd();
      if (trace) {
        System.out.printf("visitEnd on %s (%s)%n", this, this.getClass());
      }
      Annotation a = super.makeAnnotation();
      parent.supplySubannotation(name, a);
    }
  }

  /**
   * An ArrayAnnotationSceneReader is an AnnotationSceneReader that reads all elements of an array
   * field of its parent, and once it has fully visited the array, it will call its parent
   * annotation builder to include that array, so after its parent constructs and returns this as an
   * AnnotationVisitor (visitArray()), it no longer needs to worry about that array.
   *
   * <p>Note that by specification of AnnotationVisitor.visitArray(), the only methods that should
   * be called on this are visit(String name, Object value) and visitEnd().
   */
  // An AnnotationSceneReader reads an annotation.  An
  // ArrayAnnotationSceneReader reads an arbitrary array field, but not an
  // entire annotation.  So why is ArrayAnnotationSceneReader a subclass of
  // AnnotationSceneReader?  Pass ClassAnnotationSceneReader.dummyDesc
  // in the superclass constructor to
  // disable superclass behaviors that would otherwise cause trouble.
  private class ArrayAnnotationSceneReader extends AnnotationSceneReader {

    /** Parent of current scene. */
    private final AnnotationSceneReader parent;

    /** The ArrayBuilder used to create this annotation. */
    private ArrayBuilder arrayBuilder;

    /** Name of the array to be visisted. */
    private final String arrayName;

    /**
     * Constructs an ArrayAnnotationSceneReader. The element type may be unknown when this is
     * called, but AnnotationSceneReader expects to know the element type.
     *
     * @param api the ASM API version to use
     * @param parent the parent AnnotationSceneReader
     * @param fieldName the name of the field
     * @param annotationWriter the writer to output the annotations
     */
    ArrayAnnotationSceneReader(
        int api,
        AnnotationSceneReader parent,
        String fieldName,
        AnnotationVisitor annotationWriter) {
      super(api, dummyDesc, parent.visible, parent.aElement, annotationWriter);
      if (trace) {
        System.out.printf("ArrayAnnotationSceneReader(%s, %s)%n", parent, fieldName);
      }
      this.parent = parent;
      this.arrayName = fieldName;
      this.arrayBuilder = null;
    }

    private void prepareForElement(ScalarAFT elementType) {
      if (trace) {
        System.out.printf("prepareForElement(%s) in %s (%s)%n", elementType, this, this.getClass());
      }
      assert elementType != null; // but, does this happen when reading from classfile?
      if (arrayBuilder == null) {
        // this.elementType = elementType;
        arrayBuilder =
            parent.annotationBuilder.beginArrayField(arrayName, new ArrayAFT(elementType));
      }
    }

    // There are only so many different array types that are permitted in
    // an annotation.  (I'm not sure how relevant that is here.)
    @SuppressWarnings("signature") // ASM is not annotated yet
    @Override
    public void visit(String name, Object value) {
      if (trace) {
        System.out.printf(
            "visit(%s, %s) (%s) in %s (%s)%n",
            name, value, value.getClass(), this, this.getClass());
      }
      annotationWriter.visit(name, value);
      ScalarAFT aft;
      if (value.getClass().equals(org.objectweb.asm.Type.class)) {
        // What if it's an annotation?
        aft = ClassTokenAFT.ctaft;
        try {
          value = Class.forName(((org.objectweb.asm.Type) value).getClassName());
        } catch (ClassNotFoundException e) {
          throw new RuntimeException("Could not load Class for Type: " + value, e);
        }
      } else {
        Class<?> vc = value.getClass();
        aft = BasicAFT.forType(vc);
        // or: aft = (ScalarAFT) AnnotationFieldType.fromClass(vc, null);
      }
      assert aft != null;
      prepareForElement(aft);
      assert arrayBuilder != null;
      arrayBuilder.appendElement(value);
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
      if (trace) {
        System.out.printf(
            "visitEnum(%s, %s, %s) in %s (%s)%n", name, descriptor, value, this, this.getClass());
      }
      annotationWriter.visitEnum(name, descriptor, value);
      prepareForElement(new EnumAFT(classDescToName(descriptor)));
      assert arrayBuilder != null;
      arrayBuilder.appendElement(value);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
      annotationWriter.visitArray(name);
      throw new AssertionError("Multidimensional array in annotation!");
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
      if (trace) {
        System.out.printf(
            "visitAnnotation(%s, %s) in %s (%s)%n", name, descriptor, this, this.getClass());
      }
      // The NASR will regurgitate the name we pass here when it calls
      // supplySubannotation.  Since we ignore the name there, it doesn't
      // matter what name we pass here.
      AnnotationVisitor annotationWriter = this.annotationWriter.visitAnnotation(name, descriptor);
      return new NestedAnnotationSceneReader(this.api, this, name, descriptor, annotationWriter);
    }

    @Override
    public void visitEnd() {
      if (trace) {
        System.out.printf("visitEnd on %s (%s)%n", this, this.getClass());
      }
      annotationWriter.visitEnd();
      if (arrayBuilder != null) {
        arrayBuilder.finish();
      } else {
        // This was a zero-element array
        parent.annotationBuilder.addEmptyArrayField(arrayName);
      }
    }

    @Override
    void supplySubannotation(String fieldName, Annotation annotation) {
      prepareForElement(new AnnotationAFT(annotation.def()));
      assert arrayBuilder != null;
      arrayBuilder.appendElement(annotation);
    }
  }

  /**
   * A FieldAnnotationSceneReader is a FieldVisitor that only cares about visiting annotations.
   * Attributes are ignored and visitEnd() has no effect. An AnnotationSceneReader is returned for
   * declaration and type AnnotationVisitors. The AnnotationSceneReaders have a reference to an
   * ATypeElement that this is visiting, and they will write out all the information to that
   * ATypeElement after visiting each annotation.
   */
  private class FieldAnnotationSceneReader extends FieldVisitor {

    /** Field to be visisted. */
    private final AElement aField;

    /** FieldVisitor for writing the current field. */
    private final FieldVisitor fieldWriter;

    /**
     * Constructs a new FieldAnnotationScene reader.
     *
     * @param api the ASM API version to use
     * @param aField the Field to be visisted
     * @param fieldWriter a FieldWriter for writing the current field
     */
    FieldAnnotationSceneReader(int api, AElement aField, FieldVisitor fieldWriter) {
      super(api, fieldWriter);
      this.aField = aField;
      this.fieldWriter = fieldWriter;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      if (trace) {
        System.out.printf(
            "visitAnnotation(%s, %s) in %s (%s)%n", descriptor, visible, this, this.getClass());
      }
      AnnotationVisitor annotationWriter = fieldWriter.visitAnnotation(descriptor, visible);
      return new AnnotationSceneReader(this.api, descriptor, visible, aField, annotationWriter);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
        int typeRef, TypePath typePath, String descriptor, boolean visible) {
      if (trace) {
        System.out.printf(
            "visitTypeAnnotation(%s, %s, %s, %s); aField=%s, aField.type=%s in %s (%s)%n",
            typeRef, typePath, descriptor, visible, aField, aField.type, this, this.getClass());
      }
      AnnotationVisitor annotationWriter =
          fieldWriter.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
      return new TypeAnnotationSceneReader(
          this.api,
          descriptor,
          visible,
          aField.type,
          annotationWriter,
          typeRef,
          typePath,
          null,
          null,
          null,
          null);
    }
  }

  /**
   * Similarly to FieldAnnotationSceneReader, this is a MethodVisitor that only cares about visiting
   * annotations. Attributes other than BootstrapMethods are ignored, all code is ignored, and
   * visitEnd() has no effect. An AnnotationSceneReader is returned for declaration and type
   * AnnotationVisitors. The AnnotationSceneReaders have a reference to an AMethod that this is
   * visiting, and they will write out all the information to that AMethod after visiting each
   * annotation.
   */
  private class MethodAnnotationSceneReader extends MethodVisitor {

    /** Method to be visited. */
    private final AElement aMethod;

    /** MethodVisitor for writing the current method. */
    private final MethodVisitor methodWriter;

    /**
     * The name of a local variable being visited. Used to capture the local variable name seen in
     * visitLocalVariable so that it can be used in visitLocalVariableTypeAnnotation.
     */
    private String localVariableName;

    /**
     * Constructs a new MethodAnnotationScene reader.
     *
     * @param api the ASM API version to use
     * @param aMethod the Method to be visisted
     * @param methodWriter a MethodWriter for writing the current field
     */
    MethodAnnotationSceneReader(int api, AElement aMethod, MethodVisitor methodWriter) {
      super(api, methodWriter);
      this.aMethod = aMethod;
      this.methodWriter = methodWriter;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      if (trace) {
        System.out.printf(
            "visitAnnotation(%s, %s) in %s (%s)%n", descriptor, visible, this, this.getClass());
      }
      AnnotationVisitor annotationWriter = methodWriter.visitAnnotation(descriptor, visible);
      return new AnnotationSceneReader(this.api, descriptor, visible, aMethod, annotationWriter);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(
        int parameter, String descriptor, boolean visible) {
      if (trace) {
        System.out.printf(
            "visitParameterAnnotation(%s, %s, %s) in %s (%s)%n",
            parameter, descriptor, visible, this, this.getClass());
      }
      AnnotationVisitor annotationWriter =
          methodWriter.visitParameterAnnotation(parameter, descriptor, visible);
      return new AnnotationSceneReader(
          this.api,
          descriptor,
          visible,
          ((AMethod) aMethod).parameters.getVivify(parameter),
          annotationWriter);
    }

    @Override
    public void visitLocalVariable(
        String name, String descriptor, String signature, Label start, Label end, int index) {
      super.visitLocalVariable(name, descriptor, signature, start, end, index);
      this.localVariableName = name;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
        int typeRef, TypePath typePath, String descriptor, boolean visible) {
      if (trace) {
        System.out.printf(
            "visitTypeAnnotation(%s, %s, %s, %s) method=%s in %s (%s)%n",
            typeRef, typePath, descriptor, visible, aMethod, this, this.getClass());
      }
      AnnotationVisitor annotationWriter =
          methodWriter.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
      return new TypeAnnotationSceneReader(
          this.api,
          descriptor,
          visible,
          aMethod,
          annotationWriter,
          typeRef,
          typePath,
          null,
          null,
          null);
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(
        int typeRef, TypePath typePath, String descriptor, boolean visible) {
      if (trace) {
        System.out.printf(
            "visitInsnAnnotation(%s, %s, %s, %s) method=%s in %s (%s)%n",
            typeRef, typePath, descriptor, visible, aMethod, this, this.getClass());
      }
      // TODO: Need to send offset from here
      AnnotationVisitor annotationWriter =
          methodWriter.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
      return new TypeAnnotationSceneReader(
          this.api,
          descriptor,
          visible,
          aMethod,
          annotationWriter,
          typeRef,
          typePath,
          null,
          null,
          null);
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(
        int typeRef, TypePath typePath, String descriptor, boolean visible) {
      if (trace) {
        System.out.printf(
            "visitTryCatchAnnotation(%s, %s, %s, %s) method=%s in %s (%s)%n",
            typeRef, typePath, descriptor, visible, aMethod, this, this.getClass());
      }
      AnnotationVisitor annotationWriter =
          methodWriter.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
      return new TypeAnnotationSceneReader(
          this.api,
          descriptor,
          visible,
          aMethod,
          annotationWriter,
          typeRef,
          typePath,
          null,
          null,
          null);
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(
        int typeRef,
        TypePath typePath,
        Label[] start,
        Label[] end,
        int[] index,
        String descriptor,
        boolean visible) {
      if (trace) {
        System.out.printf(
            "visitLocalVariableAnnotation(%s, %s, %s, %s, %s, %s, %s) method=%s in %s (%s)%n",
            typeRef,
            typePath,
            Arrays.toString(start),
            Arrays.toString(end),
            Arrays.toString(index),
            descriptor,
            visible,
            aMethod,
            this,
            this.getClass());
      }
      AnnotationVisitor annotationWriter =
          methodWriter.visitLocalVariableAnnotation(
              typeRef, typePath, start, end, index, descriptor, visible);
      return new TypeAnnotationSceneReader(
          this.api,
          descriptor,
          visible,
          aMethod,
          annotationWriter,
          typeRef,
          typePath,
          start,
          end,
          index,
          localVariableName);
    }

    // TODO: visit code!
  }

  /** Debug code to print a Classpath. */
  private static void printClasspath() {
    System.out.println();
    System.out.println("Classpath:");
    StringTokenizer tokenizer =
        new StringTokenizer(System.getProperty("java.class.path"), File.pathSeparator);
    while (tokenizer.hasMoreTokens()) {
      System.out.println("  " + tokenizer.nextToken());
    }
  }
}
