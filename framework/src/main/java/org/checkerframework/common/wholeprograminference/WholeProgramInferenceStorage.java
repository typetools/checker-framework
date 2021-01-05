package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

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
     * Given an Element in a compilation unit that has already been read into storage, returns
     * whether there exists a stored method matching {@code elt}.
     *
     * <p>Depending on how methods are stored, this can be false if {@code elt} represents a method
     * that was synthetically added by javac, such as zero-argument constructors or valueOf(String)
     * methods for enum types.
     *
     * @param methodElt an method or constructor Element
     * @return true if the storage has a method corresponding to {@code elt}, false otherwise.
     */
    public boolean hasMethodAnnos(ExecutableElement methodElt);

    /**
     * Get the annotations for a formal parameter type.
     *
     * @param methodElt the method or constructor Element
     * @param file the annotation file containing the method or constructor
     * @param i the parameter index (0-based)
     * @param paramATM the parameter type
     * @param ve the parameter variable
     * @param atypeFactory the type factory
     * @return the annotations for a formal parameter type
     */
    public T getParameterType(
            ExecutableElement methodElt,
            String file,
            int i,
            AnnotatedTypeMirror paramATM,
            VariableElement ve,
            AnnotatedTypeFactory atypeFactory);

    /**
     * Get the annotations for the receiver type.
     *
     * @param methodElt the method or constructor Element
     * @param file the annotation file containing the method or constructor
     * @param paramATM the receiver type
     * @param atypeFactory the type factory
     * @return the annotations for the receiver type
     */
    public T getReceiverType(
            ExecutableElement methodElt,
            String file,
            AnnotatedTypeMirror paramATM,
            AnnotatedTypeFactory atypeFactory);

    /**
     * Get the annotations for the return type.
     *
     * @param methodElt the method or constructor Element
     * @param file the annotation file containing the method or constructor
     * @param atm the return type
     * @param atypeFactory the type factory
     * @return the annotations for the return type
     */
    public T getReturnType(
            ExecutableElement methodElt,
            String file,
            AnnotatedTypeMirror atm,
            AnnotatedTypeFactory atypeFactory);

    /**
     * Get the annotations for a field type.
     *
     * @param className fully-qualified name of a class
     * @param file the path to the file that represents the class containing the field
     * @param classSymbol optionally, the ClassSymbol representing the class
     * @param fieldName the simple field name
     * @param lhsATM the field type
     * @param atypeFactory the annotated type factory
     * @return the annotations for a field type
     */
    public T getFieldType(
            @BinaryName String className,
            String file,
            @Nullable ClassSymbol classSymbol,
            String fieldName,
            AnnotatedTypeMirror lhsATM,
            AnnotatedTypeFactory atypeFactory);

    /**
     * Obtain the annotations representing the contracts for a field at method entry or exit.
     *
     * @param methodElt the method Element
     * @param file the annotation file containing the method or constructor
     * @param preOrPost whether to get the preconditions or postconditions
     * @param fieldElement the field
     * @return a set of annotations representing the expression
     */
    public T getMethodContractForField(
            ExecutableElement methodElt,
            String file,
            Analysis.BeforeOrAfter preOrPost,
            VariableElement fieldElement);

    /**
     * Updates a method to add a declaration annotation.
     *
     * @param methodElt the method to annotate
     * @param anno the declaration annotation to add to the method
     * @param tmp temporary parameter to distinguish between {@link
     *     WholeProgramInferenceJavaParser#addMethodDeclarationAnnotation(ExecutableElement,
     *     AnnotationMirror)}
     * @return true if {@code anno} is a new declaration annotation for {@code methodElt}, false
     *     otherwise
     */
    // TODO: Remove tmp
    public boolean addMethodDeclarationAnnotation(
            ExecutableElement methodElt, AnnotationMirror anno, Void tmp);

    /**
     * Obtain the type from a storage location.
     *
     * @param typeMirror the underlying type for the result
     * @param storageLocation the storage location from which to obtain annotations
     * @return an annotated type mirror with underlying type {@code typeMirror} and annotations from
     *     {@code storageLocation}
     */
    public AnnotatedTypeMirror atmFromAnnotationLocation(TypeMirror typeMirror, T storageLocation);

    /**
     * Updates an the storage location to have the annotations of an {@code AnnotatedTypeMirror}
     * passed as argument. Annotations in the original set that should be ignored are not added to
     * the resulting set. This method also checks if the AnnotatedTypeMirror has explicit
     * annotations in source code, and if that is the case no annotations are added for that
     * location.
     *
     * <p>This method removes from the storage location all annotations supported by the
     * AnnotatedTypeFactory before inserting new ones. It is assumed that every time this method is
     * called, the new {@code AnnotatedTypeMirror} has a better type estimate for the given
     * location. Therefore, it is not a problem to remove all annotations before inserting the new
     * annotations.
     *
     * @param newATM the type whose annotations will be added to the {@code AnnotatedTypeMirror}
     * @param curATM used to check if the element which will be updated has explicit annotations in
     *     source code
     * @param typeToUpdate the storage location which will be updated
     * @param defLoc the location where the annotation will be added
     * @param ignoreIfAnnotated if true, don't update any type that is explicitly annotated in the
     *     source code
     */
    public void updateStorageLocationFromAtm(
            AnnotatedTypeMirror newATM,
            AnnotatedTypeMirror curATM,
            T typeToUpdate,
            TypeUseLocation defLoc,
            boolean ignoreIfAnnotated);

    /**
     * Writes the inferred results to a file. Ideally, it should be called at the end of the
     * type-checking process. In practice, it is called after each class, because we don't know
     * which class will be the last one in the type-checking process.
     *
     * @param outputFormat the file format in which to write the results
     * @param checker the checker from which this method is called, for naming stub files
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
     * Performs any preperation required for inference on Elements of a class. Should be called on
     * each toplevel class declaration in a compilation unit before processing it.
     *
     * @param classTree the class to preprocess
     */
    void preprocessClassTree(ClassTree classTree);
}
