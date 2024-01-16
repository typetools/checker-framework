package org.checkerframework.checker.serialization;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.serialization.qual.Sensitive;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** Visitor for Serialization Security Checker */
public class SerializationSecurityVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

  /** The environment is stored for getting method of a class as tree */
  private final ProcessingEnvironment env;

  /**
   * Constructor for SerializationSecurityVisitor
   *
   * @param checker Injected BaseTypeChecker
   */
  public SerializationSecurityVisitor(BaseTypeChecker checker) {
    super(checker);

    env = checker.getProcessingEnvironment();
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    ExecutableElement writeObjectExecutableElement =
        TreeUtils.getMethod(ObjectOutputStream.class, "writeObject", env, "java.lang.Object");
    ExecutableElement currentMethodExecutableElement = TreeUtils.elementFromUse(tree);

    if (ElementUtils.isMethod(currentMethodExecutableElement, writeObjectExecutableElement, env)) {
      if (!tree.getArguments().isEmpty() && tree.getArguments().get(0) != null) {
        ExpressionTree argumentTree = tree.getArguments().get(0);
        Element argumentElement = TreeUtils.elementFromTree(argumentTree);
        TypeMirror argumentTypeMirror = argumentElement.asType();
        TypeElement argumentTypeElement = TypesUtils.getTypeElement(argumentTypeMirror);

        List<String> exposedSensitiveFields = new ArrayList<>();

        for (VariableElement fieldElement :
            ElementUtils.getAllFieldsIn(argumentTypeElement, elements)) {
          if (ElementUtils.hasAnnotation(fieldElement, Sensitive.class.getCanonicalName())) {
            if (fieldElement.getModifiers().stream()
                .noneMatch(modifier -> modifier.name().equals("TRANSIENT"))) {
              exposedSensitiveFields.add(fieldElement.toString());
            }
          }
        }

        if (!exposedSensitiveFields.isEmpty()) {
          boolean isWriteObjectMethodOverwritten =
              ElementUtils.getAllMethodsIn(argumentTypeElement, elements).stream()
                  .anyMatch(
                      methodElement ->
                          methodElement
                              .toString()
                              .equals("writeObject(java.io.ObjectOutputStream)"));

          if (!isWriteObjectMethodOverwritten) {
            exposedSensitiveFields.forEach(
                fieldName -> checker.reportWarning(tree, "warning.sensitive", fieldName));
          }
        }
      }
    }

    return super.visitMethodInvocation(tree, p);
  }
}
