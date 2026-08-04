package org.checkerframework.framework.testchecker.wpiignorefield;

import com.sun.source.tree.ClassTree;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceImplementation;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenesStorage;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ElementUtils;

/**
 * Checks that {@link WholeProgramInferenceImplementation#updateFieldFromType} decides which fields
 * to skip only by calling {@link WholeProgramInferenceImplementation#ignoreFieldInWPI}, which is a
 * {@code protected} method that a subclass may override.
 *
 * <p>The test overrides {@code ignoreFieldInWPI} to return false for every field, then calls {@code
 * updateFieldFromType} on a field that is not from source code. If {@code updateFieldFromType}
 * duplicates any of the tests in {@code ignoreFieldInWPI}, it returns without consulting the
 * storage, and the test fails.
 */
public class WpiIgnoreFieldVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

  /** True if {@link #checkIgnoreFieldInWPIIsTheOnlyGate} has already run. */
  private boolean testHasRun = false;

  /**
   * Creates a WpiIgnoreFieldVisitor.
   *
   * @param checker the associated checker
   */
  public WpiIgnoreFieldVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public void processClassTree(ClassTree classTree) {
    if (!testHasRun) {
      testHasRun = true;
      checkIgnoreFieldInWPIIsTheOnlyGate(classTree);
    }
    super.processClassTree(classTree);
  }

  /**
   * Throws an {@code AssertionError} if {@code updateFieldFromType} skips a field that {@code
   * ignoreFieldInWPI} accepted.
   *
   * @param classTree a class tree, used only as the left-hand side tree that {@code
   *     updateFieldFromType} requires; the test never reaches the code that reads it
   */
  private void checkIgnoreFieldInWPIIsTheOnlyGate(ClassTree classTree) {
    VariableElement bytecodeField = systemOutField();
    if (ElementUtils.isElementFromSourceCode(bytecodeField)) {
      throw new AssertionError("java.lang.System.out is unexpectedly from source code");
    }

    WholeProgramInferenceScenesStorage storage =
        new WholeProgramInferenceScenesStorage(atypeFactory, "build/whole-program-inference") {
          @Override
          public String getFileForElement(Element elt) {
            throw new ReachedStorageException();
          }
        };
    WholeProgramInferenceImplementation<ATypeElement> wpi =
        new WholeProgramInferenceImplementation<ATypeElement>(atypeFactory, storage, false) {
          @Override
          protected boolean ignoreFieldInWPI(Element element, String fieldName) {
            return false;
          }
        };

    AnnotatedTypeMirror rhsATM = atypeFactory.getAnnotatedType(classTree);
    try {
      wpi.updateFieldFromType(classTree, bytecodeField, "out", rhsATM);
    } catch (ReachedStorageException e) {
      // updateFieldFromType obeyed the overridden ignoreFieldInWPI and went on to the storage.
      return;
    }
    throw new AssertionError(
        "updateFieldFromType skipped a field even though ignoreFieldInWPI returned false;"
            + " updateFieldFromType must not repeat any of the tests in ignoreFieldInWPI");
  }

  /**
   * Returns the element for {@code java.lang.System.out}, which is a field that is not from source
   * code.
   *
   * @return the element for {@code java.lang.System.out}
   */
  private VariableElement systemOutField() {
    TypeElement systemElt = atypeFactory.getElementUtils().getTypeElement("java.lang.System");
    for (VariableElement field : ElementFilter.fieldsIn(systemElt.getEnclosedElements())) {
      if (field.getSimpleName().contentEquals("out")) {
        return field;
      }
    }
    throw new AssertionError("did not find field java.lang.System.out");
  }

  /** Thrown by the test storage to signal that {@code getFileForElement} was called. */
  private static class ReachedStorageException extends RuntimeException {

    /** Unique identifier for serialization. */
    private static final long serialVersionUID = 20260804L;
  }
}
