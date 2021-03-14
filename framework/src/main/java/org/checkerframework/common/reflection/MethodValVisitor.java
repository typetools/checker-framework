package org.checkerframework.common.reflection;

import com.sun.source.tree.Tree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.reflection.qual.MethodVal;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.AnnotationUtils;

public class MethodValVisitor extends BaseTypeVisitor<MethodValAnnotatedTypeFactory> {

  public MethodValVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  protected MethodValAnnotatedTypeFactory createTypeFactory() {
    return new MethodValAnnotatedTypeFactory(checker);
  }

  @Override
  protected BaseTypeValidator createTypeValidator() {
    return new MethodNameValidator(checker, this, atypeFactory);
  }
}

class MethodNameValidator extends BaseTypeValidator {

  public MethodNameValidator(
      BaseTypeChecker checker, BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
    super(checker, visitor, atypeFactory);
  }

  @Override
  public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
    AnnotationMirror methodVal = type.getAnnotation(MethodVal.class);
    if (methodVal != null) {
      AnnotatedTypeFactory atypeFactory = checker.getTypeFactory();
      List<String> classNames =
          AnnotationUtils.getElementValueArray(
              methodVal, atypeFactory.methodValClassNameElement, String.class);
      List<String> methodNames =
          AnnotationUtils.getElementValueArray(
              methodVal, atypeFactory.methodValMethodNameElement, String.class);
      List<Integer> params =
          AnnotationUtils.getElementValueArray(
              methodVal, atypeFactory.methodValParamsElement, Integer.class);
      if (!(classNames.size() == methodNames.size() && classNames.size() == params.size())) {
        checker.reportError(tree, "invalid.methodval", methodVal);
      }

      for (String methodName : methodNames) {
        if (!legalMethodName(methodName)) {
          checker.reportError(tree, "illegal.methodname", methodName, type);
        }
      }
    }
    return super.visitDeclared(type, tree);
  }

  private boolean legalMethodName(String methodName) {
    if (methodName.equals(ReflectionResolver.INIT)) {
      return true;
    }
    if (methodName.length() < 1) {
      return false;
    }
    char[] methodNameChars = methodName.toCharArray();
    if (!Character.isJavaIdentifierStart(methodNameChars[0])) {
      return false;
    }
    for (int i = 1; i < methodNameChars.length; i++) {
      if (!Character.isJavaIdentifierPart(methodNameChars[i])) {
        return false;
      }
    }
    return true;
  }
}
