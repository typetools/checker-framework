package org.checkerframework.afu.annotator.find;

import org.checkerframework.afu.scenelib.Annotation;
import org.plumelib.util.IPair;

/** Specifies an annotation to be inserted into a source file. */
public class AnnotationInsertion extends Insertion {

  /**
   * The annotation text to be inserted into source code, always starts with "@".
   *
   * <p>E.g. An example would be {@code com.foo.Bar(baz)}
   */
  private final String fullyQualifiedAnnotationText;

  /**
   * The fully-qualified name of the annotation to be inserted.
   *
   * <p>E.g. Given an annotation {@code com.foo.Bar(baz)}, its fully quailified name would be {@code
   * com.foo.Bar}.
   */
  private final String fullyQualifiedAnnotationName;

  /**
   * The annotation being inserted.
   *
   * <p>Used to look up target types.
   */
  private final Annotation annotation;

  private String type;
  private boolean generateBound;
  private boolean generateExtends;
  private boolean wasGenerateExtends;

  /**
   * Creates a new insertion.
   *
   * @param fullyQualifiedAnnotationText the annotation text to be inserted into source code; starts
   *     with "@", and must be a fully-qualified name
   * @param criteria where to insert the annotation
   * @param separateLine if true, insert the annotation on its own line
   */
  public AnnotationInsertion(
      String fullyQualifiedAnnotationText, Criteria criteria, boolean separateLine) {
    this(fullyQualifiedAnnotationText, criteria, separateLine, null);
  }

  /**
   * Creates a new insertion.
   *
   * @param fullyQualifiedAnnotationText the annotation text to be inserted into source code; starts
   *     with "@", and must be a fully-qualified name
   * @param criteria where to insert the annotation
   * @param separateLine if true, insert the annotation on its own line
   * @param annotation the annotation being inserted
   */
  public AnnotationInsertion(
      String fullyQualifiedAnnotationText,
      Criteria criteria,
      boolean separateLine,
      Annotation annotation) {
    super(criteria, separateLine);
    assert fullyQualifiedAnnotationText.startsWith("@") : fullyQualifiedAnnotationText;
    // A fully-qualified name in the default package does not contain a period
    // assert fullyQualifiedAnnotationText.contains(".") : fullyQualifiedAnnotationText;
    this.fullyQualifiedAnnotationText = fullyQualifiedAnnotationText;
    this.fullyQualifiedAnnotationName = extractAnnotationFullyQualifiedName();
    this.annotation = annotation;
    type = null;
    generateBound = false;
    generateExtends = false;
    wasGenerateExtends = false;
  }

  /**
   * Creates a new insertion with an empty criteria and the text inserted on the same line.
   *
   * @param annotation the text to insert
   */
  public AnnotationInsertion(String annotation) {
    this(annotation, new Criteria(), false);
  }

  public boolean isGenerateExtends() {
    return generateExtends;
  }

  public boolean isGenerateBound() {
    return generateBound;
  }

  public void setGenerateExtends(boolean generateExtends) {
    this.generateExtends = generateExtends;
    this.wasGenerateExtends |= generateExtends;
  }

  public void setGenerateBound(boolean b) {
    generateBound = b;
  }

  /**
   * Gets the insertion text.
   *
   * @param abbreviate if true, the package name will be removed from the annotation
   * @return the text to insert
   */
  @Override
  protected String getText(boolean abbreviate) {
    // The method body will build up the result by modifying this variable.
    String result = fullyQualifiedAnnotationText;
    if (abbreviate) {
      IPair<String, String> ps = removePackage(result);
      String packageName = ps.first;
      if (packageName != null) {
        packageNames.add(packageName);
        result = ps.second;
      }
    }
    if (!result.startsWith("@")) {
      throw new Error("Illegal insertion, must start with @: " + result);
    }

    // We insert a "new " when annotating a variable initializer that is a
    // bare array expression (e.g., as in "int[] a = {0, 1};")  Since the
    // syntax doesn't permit adding the type annotation in front of the
    // expression, we generate the explicit "new"
    // (as in "int[] a = new int[] {0, 1}") to provide a legal insertion site.

    if (type != null) {
      result = "new " + result + " " + type;
    } else if (generateBound) {
      result += " Object &";
    } else if (generateExtends) {
      result = " extends " + result + " Object";
    }
    return result;
  }

  /**
   * Returns the fully-qualified name of the annotation, given its string representation. For
   * example, given "@com.foo.Bar(baz)", returns "com.foo.Bar".
   *
   * @return the fully-qualified name of the annotation
   */
  private String extractAnnotationFullyQualifiedName() {
    assert fullyQualifiedAnnotationText.startsWith("@");
    // annotation always starts with "@", so annotation name begins at index 1
    int nameBegin = 1;

    int nameEnd = fullyQualifiedAnnotationText.indexOf("(");
    // If no argument (no parenthesis in string representation), use whole annotation
    if (nameEnd == -1) {
      nameEnd = fullyQualifiedAnnotationText.length();
    }

    return fullyQualifiedAnnotationText.substring(nameBegin, nameEnd);
  }

  /**
   * Gets the raw, unmodified annotation that was passed into the constructor.
   *
   * @return the annotation
   */
  public String getAnnotationText() {
    return fullyQualifiedAnnotationText;
  }

  /**
   * Returns the fully-qualified name of the annotation.
   *
   * <p>E.g. given {@code @com.foo.Bar(baz)}, the fully-qualified name of this annotation is {@code
   * com.foo.Bar}.
   *
   * @return the annotation's fully-qualified name
   */
  public String getAnnotationFullyQualifiedName() {
    return fullyQualifiedAnnotationName;
  }

  @Override
  protected boolean addLeadingSpace(boolean gotSeparateLine, int pos, char precedingChar) {
    if (generateExtends || precedingChar == '.') {
      return false;
    }
    return super.addLeadingSpace(gotSeparateLine, pos, precedingChar);
  }

  @Override
  protected boolean addTrailingSpace(boolean gotSeparateLine) {
    // Never add a trailing space on a type parameter bound.
    return !wasGenerateExtends && super.addTrailingSpace(gotSeparateLine);
  }

  @Override
  public Kind getKind() {
    return Kind.ANNOTATION;
  }

  @Override
  public String toString() {
    return fullyQualifiedAnnotationText + " " + super.toStringWithoutClass();
  }

  public void setType(String s) {
    this.type = s;
  }

  /**
   * Returns the annotation being inserted.
   *
   * @return the annotation being inserted
   */
  public Annotation getAnnotation() {
    return annotation;
  }
}
