package org.checkerframework.afu.scenelib.test.classfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;
import org.plumelib.util.CollectionsPlume;

/**
 * An {@code AnnotationVerifier} provides a way to check to see if two versions of the same class
 * (from two different {@code .class} files), have the same annotations on the same elements.
 */
public class AnnotationVerifier {

  private static final String lineSep = System.getProperty("line.separator");

  /** The "correct" version of the class to verify against. */
  private ClassRecorder originalVisitor;

  /** The uncertain version of the class to verify. */
  private ClassRecorder newVisitor;

  /**
   * Constructs a new {@code AnnotationVerifier} that does not yet have any information about the
   * class.
   */
  public AnnotationVerifier() {
    originalVisitor = new ClassRecorder(Opcodes.ASM8);
    newVisitor = new ClassRecorder(Opcodes.ASM8);
  }

  /**
   * Returns the {@code ClassVisitor} which should be made to visit the version of the class known
   * to be correct.
   *
   * @return a visitor for the good version of the class
   */
  public ClassVisitor originalVisitor() {
    return originalVisitor;
  }

  /**
   * Returns the {@code ClassVisitor} which should be made to visit the version of the class being
   * tested.
   *
   * @return a visitor for the experimental version of the class
   */
  public ClassVisitor newVisitor() {
    return newVisitor;
  }

  /**
   * Verifies that the visitors returned by {@link #originalVisitor()} and {@link #newVisitor()}
   * have visited the same class. This method can only be called if both visitors have already
   * visited a class.
   *
   * @throws AnnotationMismatchException if the two visitors have not visited two versions of the
   *     same class that contain idential annotations
   */
  public void verify() {
    if (!newVisitor.name.equals(originalVisitor.name)) {
      throw new AnnotationMismatchException(
          "Cannot verify two different classes:%n  %s%n  %s",
          newVisitor.name, originalVisitor.name);
    }
    newVisitor.verifyAgainst(originalVisitor);
  }

  // print the expected and found annotations to standard out
  public void verifyPrettyPrint() {
    System.out.println("expected:");
    System.out.println(originalVisitor.prettyPrint());
    System.out.println("actual:");
    System.out.println(newVisitor.prettyPrint());
  }

  /**
   * A ClassRecorder records all the annotations that it visits, and serves as a ClassVisitor,
   * FieldVisitor, and MethodVisitor.
   */
  private class ClassRecorder extends ClassVisitor {

    private String description;

    public String name;
    private String signature;

    private Map<String, FieldRecorder> fieldRecorders;
    // key is unparameterized name

    private Map<String, MethodRecorder> methodRecorders;
    // key is complete method signature

    // general annotations
    private Map<String, AnnotationRecorder> anns;
    private Map<String, AnnotationRecorder> xanns;

    public ClassRecorder(int api) {
      this(api, "class: ", "", "");
    }

    public ClassRecorder(int api, String internalDescription, String name, String signature) {
      super(api);
      this.description = internalDescription;
      this.name = name;
      this.signature = signature;

      fieldRecorders = new HashMap<>();
      methodRecorders = new HashMap<>();

      anns = new HashMap<>();
      xanns = new HashMap<>();
    }

    @Override
    public void visit(
        int version,
        int access,
        String name,
        String signature,
        String superName,
        String[] interfaces) {
      this.name = name;
      this.signature = signature;
      description = description + name;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      AnnotationRecorder av =
          new AnnotationRecorder(api, description + " annotation: " + descriptor);
      anns.put(descriptor, av);
      return av;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
        int typeRef, TypePath typePath, String descriptor, boolean visible) {
      AnnotationRecorder av =
          new AnnotationRecorder(
              api, description + " annotation: " + descriptor, typeRef, typePath);
      xanns.put(descriptor, av);
      return av;
    }

    @Override
    public FieldVisitor visitField(
        int access, String name, String descriptor, String signature, Object value) {
      FieldRecorder fr = new FieldRecorder(api, description + " field: " + name, name, signature);
      fieldRecorders.put(name, fr);
      return fr;
    }

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String descriptor, String signature, String[] exceptions) {
      MethodRecorder mr =
          new MethodRecorder(
              api, description + " method: " + name + descriptor, name + descriptor, signature);
      methodRecorders.put(name + descriptor, mr);
      return mr;
    }

    public void verifyAgainst(ClassRecorder correct) {
      // first, ensure all annotations are correct
      verifyAnns(this.anns, correct.anns);
      verifyAnns(this.xanns, correct.xanns);

      // then recurse into any annotations on fields/methods
      verifyFieldAnns(this.fieldRecorders, correct.fieldRecorders);
      verifyMethodAnns(this.methodRecorders, correct.methodRecorders);
    }

    private void verifyAnns(
        Map<String, AnnotationRecorder> questionableAnns,
        Map<String, AnnotationRecorder> correctAnns) {
      Set<AnnotationRecorder> unresolvedQuestionableAnns =
          new HashSet<AnnotationRecorder>(questionableAnns.values());

      for (Map.Entry<String, AnnotationRecorder> entry : correctAnns.entrySet()) {
        String name = entry.getKey();
        AnnotationRecorder correctAnn = entry.getValue();
        AnnotationRecorder questionableAnn = questionableAnns.get(name);
        if (questionableAnn == null) {
          throw new AnnotationMismatchException(
              "{%s}%n  does not contain expected annotation:%n  {%s}", description, correctAnn);
        }

        questionableAnn.verifyAgainst(correctAnn);

        unresolvedQuestionableAnns.remove(questionableAnn);
      }

      for (AnnotationRecorder unexpectedAnnOnThis : unresolvedQuestionableAnns) {
        throw new AnnotationMismatchException(
            "{%s}%n  contains unexpected annotation:%n  {%s}", description, unexpectedAnnOnThis);
      }
    }

    private void verifyFieldAnns(
        Map<String, FieldRecorder> questionableMembers, Map<String, FieldRecorder> correctMembers) {
      Set<FieldRecorder> unresolvedQuestionableMembers =
          new HashSet<>(questionableMembers.values());

      for (Map.Entry<String, FieldRecorder> entry : correctMembers.entrySet()) {
        String name = entry.getKey();
        FieldRecorder correctMember = entry.getValue();
        FieldRecorder questionableMember = questionableMembers.get(name);
        if (questionableMember == null) {
          throw new AnnotationMismatchException(
              "{%s}%n  does not contain expected member:%n  {%s}", description, correctMember);
        }

        questionableMember.verifyAgainst(correctMember);

        unresolvedQuestionableMembers.remove(questionableMember);
      }

      for (FieldRecorder unexpectedMemberOnThis : unresolvedQuestionableMembers) {
        System.out.println("Going to throw exception: ");
        System.out.println("questionable: " + questionableMembers);
        System.out.println("correct: " + correctMembers);

        throw new AnnotationMismatchException(
            "{%s}%n  contains unexpected member:%n  {%s}", description, unexpectedMemberOnThis);
      }
    }

    private void verifyMethodAnns(
        Map<String, MethodRecorder> questionableMembers,
        Map<String, MethodRecorder> correctMembers) {
      Set<MethodRecorder> unresolvedQuestionableMembers =
          new HashSet<>(questionableMembers.values());

      for (Map.Entry<String, MethodRecorder> entry : correctMembers.entrySet()) {
        String name = entry.getKey();
        MethodRecorder correctMember = entry.getValue();
        MethodRecorder questionableMember = questionableMembers.get(name);
        if (questionableMember == null) {
          throw new AnnotationMismatchException(
              "{%s}%n  does not contain expected member:%n  {%s}", description, correctMember);
        }

        questionableMember.verifyAgainst(correctMember);

        unresolvedQuestionableMembers.remove(questionableMember);
      }

      for (MethodRecorder unexpectedMemberOnThis : unresolvedQuestionableMembers) {
        System.out.println("Going to throw exception: ");
        System.out.println("questionable: " + questionableMembers);
        System.out.println("correct: " + correctMembers);

        throw new AnnotationMismatchException(
            "{%s}%n  contains unexpected member:%n  {%s}", description, unexpectedMemberOnThis);
      }
    }

    @Override
    public String toString() {
      return description;
    }

    public String prettyPrint() {
      StringBuilder sb = new StringBuilder();
      prettyPrint(sb, "");
      return sb.toString();
    }

    // pretty-prints this into the given list of lines
    public void prettyPrint(StringBuilder sb, String indent) {

      // avoid boilerplate of adding indent and lineSep every time
      List<String> lines = new ArrayList<>();

      lines.add("description: " + description);
      lines.add("  name: " + name);
      lines.add("  signature: " + signature);
      if (!anns.isEmpty()) {
        lines.add("  anns:");
        for (Map.Entry<String, AnnotationRecorder> e : anns.entrySet()) {
          // in the future, maybe get a fuller description
          lines.add("    " + e.getKey() + ": " + e.getValue().description);
        }
      }
      if (!xanns.isEmpty()) {
        lines.add("  xanns:");
        for (Map.Entry<String, AnnotationRecorder> e : xanns.entrySet()) {
          // in the future, maybe get a fuller description
          lines.add("    " + e.getKey() + ": " + e.getValue().description);
        }
      }
      for (String line : lines) {
        sb.append(indent);
        sb.append(line);
        sb.append(lineSep);
      }
      for (Map.Entry<String, FieldRecorder> e : fieldRecorders.entrySet()) {
        sb.append(indent + "  " + e.getKey() + ":" + lineSep);
        e.getValue().prettyPrint(sb, indent + "    ");
      }
      for (Map.Entry<String, MethodRecorder> e : methodRecorders.entrySet()) {
        sb.append(indent + "  " + e.getKey() + ":" + lineSep);
        e.getValue().prettyPrint(sb, indent + "    ");
      }
    }
  }

  private class MethodRecorder extends MethodVisitor {

    private String description;

    public String name;
    private String signature;

    // general annotations
    private Map<String, AnnotationRecorder> anns;
    private Map<String, AnnotationRecorder> xanns;

    // method specific annotations
    private Set<AnnotationRecorder> danns; // default annotations
    private Map<ParameterDescription, AnnotationRecorder> panns; // parameter annotations

    public MethodRecorder(int api, String internalDescription, String name, String signature) {
      super(api);
      this.description = internalDescription;
      this.name = name;
      this.signature = signature;

      anns = new HashMap<>();
      xanns = new HashMap<>();

      danns = new HashSet<>();
      panns = new HashMap<>();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      AnnotationRecorder av =
          new AnnotationRecorder(api, description + " annotation: " + descriptor);
      anns.put(descriptor, av);
      return av;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
        int typeRef, TypePath typePath, String descriptor, boolean visible) {
      AnnotationRecorder av =
          new AnnotationRecorder(
              api, description + " annotation: " + descriptor, typeRef, typePath);
      xanns.put(descriptor, av);
      return av;
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(
        int typeRef, TypePath typePath, String descriptor, boolean visible) {
      AnnotationRecorder av =
          new AnnotationRecorder(
              api, description + " annotation: " + descriptor, typeRef, typePath);
      xanns.put(descriptor, av);
      return av;
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
      AnnotationRecorder av =
          new AnnotationRecorder(
              api,
              description + " annotation: " + descriptor,
              typeRef,
              typePath,
              start,
              end,
              index);
      xanns.put(descriptor, av);
      return av;
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(
        int typeRef, TypePath typePath, String descriptor, boolean visible) {
      AnnotationRecorder av =
          new AnnotationRecorder(
              api, description + " annotation: " + descriptor, typeRef, typePath);
      xanns.put(descriptor, av);
      return av;
    }

    // MethodVisitor methods:
    @Override
    public AnnotationVisitor visitAnnotationDefault() {
      AnnotationRecorder dr = new AnnotationRecorder(api, description + " default annotation");
      danns.add(dr);
      return dr;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(
        int parameter, String descriptor, boolean visible) {
      ParameterDescription pd = new ParameterDescription(parameter, descriptor, visible);
      AnnotationRecorder pr =
          new AnnotationRecorder(api, description + " parameter annotation: " + pd);
      panns.put(pd, pr);
      return pr;
    }

    public void verifyAgainst(MethodRecorder correct) {
      // first, ensure all annotations are correct
      verifyAnns(this.anns, correct.anns);
      verifyAnns(this.xanns, correct.xanns);
    }

    private void verifyAnns(
        Map<String, AnnotationRecorder> questionableAnns,
        Map<String, AnnotationRecorder> correctAnns) {
      Set<AnnotationRecorder> unresolvedQuestionableAnns =
          new HashSet<AnnotationRecorder>(questionableAnns.values());

      for (Map.Entry<String, AnnotationRecorder> entry : correctAnns.entrySet()) {
        String name = entry.getKey();
        AnnotationRecorder correctAnn = entry.getValue();
        AnnotationRecorder questionableAnn = questionableAnns.get(name);
        if (questionableAnn == null) {
          throw new AnnotationMismatchException(
              "{%s}%n  does not contain expected annotation:%n  {%s}", description, correctAnn);
        }

        questionableAnn.verifyAgainst(correctAnn);

        unresolvedQuestionableAnns.remove(questionableAnn);
      }

      for (AnnotationRecorder unexpectedAnnOnThis : unresolvedQuestionableAnns) {
        throw new AnnotationMismatchException(
            "{%s}%n  contains unexpected annotation:%n  {%s}", description, unexpectedAnnOnThis);
      }
    }

    @Override
    public String toString() {
      return description;
    }

    // pretty-prints this into the given list of lines
    public void prettyPrint(StringBuilder sb, String indent) {

      // avoid boilerplate of adding indent and lineSep every time
      List<String> lines = new ArrayList<>();

      lines.add("description: " + description);
      lines.add("  name: " + name);
      lines.add("  signature: " + signature);
      if (!anns.isEmpty()) {
        lines.add("  anns:");
        for (Map.Entry<String, AnnotationRecorder> e : anns.entrySet()) {
          // in the future, maybe get a fuller description
          lines.add("    " + e.getKey() + ": " + e.getValue().description);
        }
      }
      if (!xanns.isEmpty()) {
        lines.add("  xanns:");
        for (Map.Entry<String, AnnotationRecorder> e : xanns.entrySet()) {
          // in the future, maybe get a fuller description
          lines.add("    " + e.getKey() + ": " + e.getValue().description);
        }
      }
      for (String line : lines) {
        sb.append(indent);
        sb.append(line);
        sb.append(lineSep);
      }
    }
  }

  private class FieldRecorder extends FieldVisitor {

    private String description;

    public String name;
    private String signature;

    // general annotations
    private Map<String, AnnotationRecorder> anns;
    private Map<String, AnnotationRecorder> xanns;

    public FieldRecorder(int api, String internalDescription, String name, String signature) {
      super(api);
      this.description = internalDescription;
      this.name = name;
      this.signature = signature;

      anns = new HashMap<>();
      xanns = new HashMap<>();
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
        int typeRef, TypePath typePath, String descriptor, boolean visible) {
      AnnotationRecorder av =
          new AnnotationRecorder(
              api, description + " annotation: " + descriptor, typeRef, typePath);
      xanns.put(descriptor, av);
      return av;
    }

    public void verifyAgainst(FieldRecorder correct) {
      // first, ensure all annotations are correct
      verifyAnns(this.anns, correct.anns);
      verifyAnns(this.xanns, correct.xanns);
    }

    private void verifyAnns(
        Map<String, AnnotationRecorder> questionableAnns,
        Map<String, AnnotationRecorder> correctAnns) {
      Set<AnnotationRecorder> unresolvedQuestionableAnns =
          new HashSet<AnnotationRecorder>(questionableAnns.values());

      for (Map.Entry<String, AnnotationRecorder> entry : correctAnns.entrySet()) {
        String name = entry.getKey();
        AnnotationRecorder correctAnn = entry.getValue();
        AnnotationRecorder questionableAnn = questionableAnns.get(name);
        if (questionableAnn == null) {
          throw new AnnotationMismatchException(
              "{%s}%n  does not contain expected annotation:%n  {%s}", description, correctAnn);
        }

        questionableAnn.verifyAgainst(correctAnn);

        unresolvedQuestionableAnns.remove(questionableAnn);
      }

      for (AnnotationRecorder unexpectedAnnOnThis : unresolvedQuestionableAnns) {
        throw new AnnotationMismatchException(
            "{%s}%n  contains unexpected annotation:%n  {%s}", description, unexpectedAnnOnThis);
      }
    }

    @Override
    public String toString() {
      return description;
    }

    // pretty-prints this into the given list of lines
    public void prettyPrint(StringBuilder sb, String indent) {

      // avoid boilerplate of adding indent and lineSep every time
      List<String> lines = new ArrayList<>();

      lines.add("description: " + description);
      lines.add("  name: " + name);
      lines.add("  signature: " + signature);
      if (!anns.isEmpty()) {
        lines.add("  anns:");
        for (Map.Entry<String, AnnotationRecorder> e : anns.entrySet()) {
          // in the future, maybe get a fuller description
          lines.add("    " + e.getKey() + ": " + e.getValue().description);
        }
      }
      if (!xanns.isEmpty()) {
        lines.add("  xanns:");
        for (Map.Entry<String, AnnotationRecorder> e : xanns.entrySet()) {
          // in the future, maybe get a fuller description
          lines.add("    " + e.getKey() + ": " + e.getValue().description);
        }
      }
      for (String line : lines) {
        sb.append(indent);
        sb.append(line);
        sb.append(lineSep);
      }
    }
  }

  /**
   * An AnnotationRecorder is an TypeAnnotationVisitor that records all the information it visits.
   */
  private class AnnotationRecorder extends AnnotationVisitor {
    private String description;

    private List<String> fieldArgsName;
    private List<Object> fieldArgsValue;

    private List<String> enumArgsName;
    private List<String> enumArgsDesc;
    private List<String> enumArgsValue;

    private List<String> innerAnnotationArgsName;
    private List<String> innerAnnotationArgsDesc;
    private Map<String, AnnotationRecorder> innerAnnotationMap;

    private List<String> arrayArgs;
    private Map<String, AnnotationRecorder> arrayMap;

    private TypeReference typeReference;
    private TypePath typePath;
    private Label[] start;
    private Label[] end;
    private int[] index;

    public AnnotationRecorder(int api, String description) {
      this(api, description, null, null);
    }

    // Note: typeRef is an Integer so we can use it to determine whether the annotation is a type
    // annotation. We can't
    //       use TypePath as a TypePath can be null for a type annotation.
    public AnnotationRecorder(int api, String description, Integer typeRef, TypePath typePath) {
      this(api, description, typeRef, typePath, null, null, null);
    }

    public AnnotationRecorder(
        int api,
        String description,
        Integer typeRef,
        TypePath typePath,
        Label[] start,
        Label[] end,
        int[] index) {
      super(api);
      this.description = description;
      fieldArgsName = new ArrayList<String>();
      fieldArgsValue = new ArrayList<Object>();

      enumArgsName = new ArrayList<String>();
      enumArgsDesc = new ArrayList<String>();
      enumArgsValue = new ArrayList<String>();

      innerAnnotationArgsName = new ArrayList<String>();
      innerAnnotationArgsDesc = new ArrayList<String>();
      innerAnnotationMap = new HashMap<>();

      arrayArgs = new ArrayList<String>();
      arrayMap = new HashMap<>();

      if (typeRef != null) {
        this.typeReference = new TypeReference(typeRef);
        this.typePath = typePath;
        if (start != null) {
          this.start = start;
          this.end = end;
          this.index = index;
        }
      }
    }

    @Override
    public void visit(String name, Object value) {
      fieldArgsName.add(name);
      fieldArgsValue.add(value);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
      innerAnnotationArgsName.add(name);
      innerAnnotationArgsDesc.add(descriptor);

      AnnotationRecorder av = new AnnotationRecorder(api, description + name);
      innerAnnotationMap.put(name, av);
      return av;
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
      arrayArgs.add(name);
      AnnotationRecorder av = new AnnotationRecorder(api, description + name);
      arrayMap.put(name, av);
      return av;
    }

    @Override
    public void visitEnd() {}

    @Override
    public void visitEnum(String name, String descriptor, String value) {
      enumArgsName.add(name);
      enumArgsDesc.add(descriptor);
      enumArgsValue.add(value);
    }

    @Override
    public String toString() {
      return description;
    }

    /**
     * Checks that the information passed into this matches the information passed into another
     * AnnotationRecorder. For right now, the order in which information is passed in does matter.
     * If there is a conflict in information, an exception will be thrown.
     *
     * @param ar an annotation recorder that has visited the correct information this should visit
     * @throws AnnotationMismatchException if the information visited by this does not match the
     *     information in ar
     */
    public void verifyAgainst(AnnotationRecorder ar) {
      StringBuilder sb = new StringBuilder();
      verifyList(sb, "visit()", 1, this.fieldArgsName, ar.fieldArgsName);
      verifyList(sb, "visit()", 2, this.fieldArgsValue, ar.fieldArgsValue);

      verifyList(sb, "visitEnum()", 1, this.enumArgsName, ar.enumArgsName);
      verifyList(sb, "visitEnum()", 2, this.enumArgsDesc, ar.enumArgsDesc);
      verifyList(sb, "visitEnum()", 3, this.enumArgsValue, ar.enumArgsValue);

      verifyList(
          sb, "visitAnnotation()", 1, this.innerAnnotationArgsName, ar.innerAnnotationArgsName);
      verifyList(
          sb, "visitAnnotation()", 2, this.innerAnnotationArgsDesc, ar.innerAnnotationArgsDesc);

      verifyList(sb, "visitArray()", 1, this.arrayArgs, ar.arrayArgs);

      if (this.typeReference != ar.typeReference
          && this.typeReference.getValue() != ar.typeReference.getValue()) {
        printError(
            sb, "<one of the type annotation methods>", 1, this.typeReference, ar.typeReference);
      }
      if (this.typePath != ar.typePath
          && !this.typePath.toString().equals(ar.typePath.toString())) {
        printError(sb, "<one of the type annotation methods>", 2, this.typePath, ar.typePath);
      }

      verifyArray(sb, "<one of the type annotation methods>", 3, this.start, ar.start);
      verifyArray(sb, "<one of the type annotation methods>", 4, this.end, ar.end);

      Integer[] thisIndexArray = null;
      Integer[] arIndexArray = null;
      if (this.index != null && ar.index != null) {
        thisIndexArray = IntStream.of(this.index).boxed().toArray(Integer[]::new);
        arIndexArray = IntStream.of(ar.index).boxed().toArray(Integer[]::new);
      }
      verifyArray(sb, "<one of the type annotation methods>", 5, thisIndexArray, arIndexArray);

      verifyInnerAnnotationRecorder(this.innerAnnotationMap, ar.innerAnnotationMap);
      verifyInnerAnnotationRecorder(this.arrayMap, ar.arrayMap);

      if (sb.length() > 0) {
        throw new AnnotationMismatchException(sb.toString());
      }
    }

    private <T> void verifyArray(
        StringBuilder sb, String methodName, int parameter, T[] questionable, T[] correct) {
      if (questionable == correct) {
        return;
      }
      if (Arrays.equals(questionable, correct)) {
        return;
      }

      printError(sb, methodName, parameter, questionable, correct);
      // new Error("The backtrace:").printStackTrace();
    }

    private <T> void verifyList(
        StringBuilder sb, String methodName, int parameter, List<T> questionable, List<T> correct) {
      if (questionable == correct) {
        return;
      }
      if (questionable.equals(correct)) {
        return;
      }
      if (CollectionsPlume.deepEquals(questionable, correct)) {
        return;
      }

      printError(sb, methodName, parameter, questionable, correct);
      // new Error("The backtrace:").printStackTrace();
    }

    private void printError(
        StringBuilder sb, String methodName, int parameter, Object questionable, Object correct) {
      sb.append(lineSep);
      sb.append(methodName);
      sb.append(" was called with unexpected information in parameter: ");
      sb.append(parameter);
      sb.append(lineSep);
      sb.append("Description: ");
      sb.append(description);
      sb.append(lineSep);
      sb.append(
          String.format(
              "Received (%s): %s%n",
              questionable == null ? null : questionable.getClass(), questionable));
      sb.append(
          String.format(
              "Expected (%s): %s%n", correct == null ? null : correct.getClass(), correct));
    }

    @SuppressWarnings("CollectionIncompatibleType") // TODO!!
    private void verifyInnerAnnotationRecorder(
        Map<String, AnnotationRecorder> questionableAR, Map<String, AnnotationRecorder> correctAR) {
      // checks on arguments passed in to the methods that created these
      // AnnotationRecorders (i.e. the checks on the String keys on these maps)
      // ensure that these have identical keys
      for (Map.Entry<String, AnnotationRecorder> questionableEntry : questionableAR.entrySet()) {
        questionableEntry.getValue().verifyAgainst(correctAR.get(questionableEntry.getClass()));
      }
    }
  }

  /**
   * A ParameterDescription is a convenient class used to keep information about method parameters.
   * Parameters are equal if they have the same index, regardless of their description.
   */
  private static class ParameterDescription {
    public final int parameter;
    public final String descriptor;
    public final boolean visible;

    public ParameterDescription(int parameter, String descriptor, boolean visible) {
      this.parameter = parameter;
      this.descriptor = descriptor;
      this.visible = visible;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o instanceof ParameterDescription) {
        ParameterDescription p = (ParameterDescription) o;
        return this.parameter == p.parameter;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return parameter * 17;
    }

    @Override
    public String toString() {
      return "parameter index: "
          + parameter
          + " descriptor: "
          + descriptor
          + " visible: "
          + visible;
    }
  }

  /**
   * An AnnotationMismatchException is an Exception that indicates that two versions of the same
   * class do not have the same annotations on either the class, its field, or its methods.
   */
  public static class AnnotationMismatchException extends RuntimeException {
    private static final long serialVersionUID = 20060714L; // today's date

    /**
     * Constructs a new AnnotationMismatchException with the given error message.
     *
     * @param msg the error as to why the annotations do not match
     */
    public AnnotationMismatchException(String msg) {
      super(msg);
    }

    /**
     * Constructs a new AnnotationMismatchException with the given error message and arguments.
     *
     * @param msg the error as to why the annotations do not match, as a format string
     * @param args arguments to the format string
     */
    @SuppressWarnings("AnnotateFormatMethod")
    public AnnotationMismatchException(String msg, Object... args) {
      super(String.format(msg, args));
    }
  }
}
