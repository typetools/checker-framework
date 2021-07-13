package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.Analysis.BeforeOrAfter;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Stores annotations from whole-program inference. For a given location such as a field or method,
 * an object can be obtained containing the inferred annotations for that object.
 *
 * <p>Also writes stored annotations to storage files. The specific format depends on the
 * implementation.
 *
 * @param <T> the type used by the storage to store annotations. The methods {@link
 *     #atmFromStorageLocation} and {@link #updateStorageLocationFromAtm} can be used to manipulate
 *     a storage location.
 */
public interface WholeProgramInferenceStorage<T> {
  /**
   * Returns the file corresponding to the given element. This may side-effect the storage to load
   * the file if it hasn't been read yet.
   *
   * @param elt an element
   * @return the path to the file where inference results for the element will be written
   */
  public String getFileForElement(Element elt);

  /**
   * Given an ExecutableElement in a compilation unit that has already been read into storage,
   * returns whether there exists a stored method matching {@code elt}.
   *
   * <p>An implementation is permitted to return false if {@code elt} represents a method that was
   * synthetically added by javac, such as zero-argument constructors or valueOf(String) methods for
   * enum types.
   *
   * @param methodElt a method or constructor Element
   * @return true if the storage has a method corresponding to {@code elt}
   */
  public boolean hasStorageLocationForMethod(ExecutableElement methodElt);

  /**
   * Get the annotations for a formal parameter type.
   *
   * @param methodElt the method or constructor Element
   * @param i the parameter index (0-based)
   * @param paramATM the parameter type
   * @param ve the parameter variable
   * @param atypeFactory the type factory
   * @return the annotations for a formal parameter type
   */
  public T getParameterAnnotations(
      ExecutableElement methodElt,
      int i,
      AnnotatedTypeMirror paramATM,
      VariableElement ve,
      AnnotatedTypeFactory atypeFactory);

  /**
   * Get the annotations for the receiver type.
   *
   * @param methodElt the method or constructor Element
   * @param paramATM the receiver type
   * @param atypeFactory the type factory
   * @return the annotations for the receiver type
   */
  public T getReceiverAnnotations(
      ExecutableElement methodElt, AnnotatedTypeMirror paramATM, AnnotatedTypeFactory atypeFactory);

  /**
   * Get the annotations for the return type.
   *
   * @param methodElt the method or constructor Element
   * @param atm the return type
   * @param atypeFactory the type factory
   * @return the annotations for the return type
   */
  public T getReturnAnnotations(
      ExecutableElement methodElt, AnnotatedTypeMirror atm, AnnotatedTypeFactory atypeFactory);

  /**
   * Get the annotations for a field type.
   *
   * @param element the element for the field
   * @param fieldName the simple field name
   * @param lhsATM the field type
   * @param atypeFactory the annotated type factory
   * @return the annotations for a field type
   */
  public T getFieldAnnotations(
      Element element,
      String fieldName,
      AnnotatedTypeMirror lhsATM,
      AnnotatedTypeFactory atypeFactory);

  // Fields are special because currently WPI computes preconditions for fields only, not for
  // other expressions.
  /**
   * Returns the pre- or postcondition annotations for a field.
   *
   * @param preOrPost whether to get the precondition or postcondition
   * @param methodElement the method
   * @param fieldElement the field
   * @param atypeFactory the type factory
   * @return the pre- or postcondition annotations for a field
   */
  public T getPreOrPostconditionsForField(
      Analysis.BeforeOrAfter preOrPost,
      ExecutableElement methodElement,
      VariableElement fieldElement,
      AnnotatedTypeFactory atypeFactory);

  /**
   * Returns the pre- or postcondition annotations for a method parameter.
   *
   * @param preOrPost whether to get the precondition or postcondition
   * @param methodElt the method
   * @param paramElt the parameter
   * @param index the parameter's index (1-based)
   * @param atypeFactory the type factory
   * @return the pre- or postcondition annotations for the parameter, or null if nothing is inferrable
   */
  // TODO: this method currently can return null because preconditions on parameters aren't supported.
  // We might want to remove that restriction in the future (but preconditions on parameters are kind
  // of unnecessary, because the parameters can just be annotated?)
  public @Nullable T getPreOrPostconditionsForParameter(BeforeOrAfter preOrPost, ExecutableElement methodElt,
      VariableElement paramElt, int index, AnnotatedTypeFactory atypeFactory);

  /**
   * Updates a method to add a declaration annotation.
   *
   * @param methodElt the method to annotate
   * @param anno the declaration annotation to add to the method
   * @return true if {@code anno} is a new declaration annotation for {@code methodElt}, false
   *     otherwise
   */
  public boolean addMethodDeclarationAnnotation(ExecutableElement methodElt, AnnotationMirror anno);

  /**
   * Obtain the type from a storage location.
   *
   * @param typeMirror the underlying type for the result
   * @param storageLocation the storage location from which to obtain annotations
   * @return an annotated type mirror with underlying type {@code typeMirror} and annotations from
   *     {@code storageLocation}
   */
  public AnnotatedTypeMirror atmFromStorageLocation(TypeMirror typeMirror, T storageLocation);

  /**
   * Updates a storage location to have the annotations of the given {@code AnnotatedTypeMirror}.
   * Annotations in the original set that should be ignored are not added to the resulting set. If
   * {@code ignoreIfAnnotated} is true, doesn't add annotations for locations with explicit
   * annotations in source code.
   *
   * <p>This method removes from the storage location all annotations supported by the
   * AnnotatedTypeFactory before inserting new ones. It is assumed that every time this method is
   * called, the new {@code AnnotatedTypeMirror} has a better type estimate for the given location.
   * Therefore, it is not a problem to remove all annotations before inserting the new annotations.
   *
   * <p>The {@code update*} methods in {@link WholeProgramInference} perform LUB. This one just does
   * replacement. (Thus, the naming may be a bit confusing.)
   *
   * @param newATM the type whose annotations will be added to the {@code AnnotatedTypeMirror}
   * @param curATM the annotations currently stored at the location, used to check if the element
   *     that will be updated has explicit annotations in source code
   * @param storageLocationToUpdate the storage location that will be updated
   * @param defLoc the location where the annotation will be added
   * @param ignoreIfAnnotated if true, don't update any type that is explicitly annotated in the
   *     source code
   */
  public void updateStorageLocationFromAtm(
      AnnotatedTypeMirror newATM,
      AnnotatedTypeMirror curATM,
      T storageLocationToUpdate,
      TypeUseLocation defLoc,
      boolean ignoreIfAnnotated);

  /**
   * Writes the inferred results to a file. Ideally, it should be called at the end of the
   * type-checking process. In practice, it is called after each class, because we don't know which
   * class will be the last one in the type-checking process.
   *
   * @param outputFormat the file format in which to write the results
   * @param checker the checker from which this method is called, for naming annotation files
   */
  public void writeResultsToFile(
      WholeProgramInference.OutputFormat outputFormat, BaseTypeChecker checker);

  /**
   * Indicates that inferred annotations for the file at {@code path} have changed since last
   * written. This causes output files for {@code path} to be written out next time {@link
   * #writeResultsToFile} is called.
   *
   * @param path path to the file with annotations that have been modified
   */
  public void setFileModified(String path);

  /**
   * Performs any preparation required for inference on the elements of a class. Should be called on
   * each top-level class declaration in a compilation unit before processing it.
   *
   * @param classTree the class to preprocess
   */
  void preprocessClassTree(ClassTree classTree);
}
