package org.checkerframework.checker.formatter;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.formatter.FormatterTreeUtil.FormatCall;
import org.checkerframework.checker.formatter.FormatterTreeUtil.InvocationType;
import org.checkerframework.checker.formatter.FormatterTreeUtil.Result;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.wholeprograminference.WholeProgramInference;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Whenever a format method invocation is found in the syntax tree, checks are performed as
 * specified in the Format String Checker manual.
 *
 * @checker_framework.manual #formatter-guarantees Format String Checker
 */
public class FormatterVisitor extends BaseTypeVisitor<FormatterAnnotatedTypeFactory> {
  public FormatterVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public Void visitMethod(MethodTree node, Void p) {
    ExecutableElement methodElement = TreeUtils.elementFromDeclaration(node);
    if (atypeFactory.getDeclAnnotation(methodElement, FormatMethod.class) != null) {
      int formatStringIndex = FormatterVisitor.formatStringIndex(methodElement);
      if (formatStringIndex == -1) {
        checker.reportError(node, "format.method.invalid", methodElement.getSimpleName());
      }
    }
    return super.visitMethod(node, p);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
    FormatterTreeUtil ftu = atypeFactory.treeUtil;
    FormatCall fc = ftu.create(node, atypeFactory);
    if (fc != null) {
      MethodTree enclosingMethod =
          TreePathUtil.enclosingMethod(atypeFactory.getPath(fc.invocationTree));

      Result<String> errMissingFormat = fc.errMissingFormatAnnotation();
      if (errMissingFormat != null) {
        // The string's type has no @Format annotation.
        if (isWrappedFormatCall(fc, enclosingMethod)) {
          // Nothing to do, because call is legal.
        } else {
          // I.1
          ftu.failure(errMissingFormat, "format.string.invalid", errMissingFormat.value());
        }
      } else {
        // The string has a @Format annotation.
        Result<InvocationType> invc = fc.getInvocationType();
        ConversionCategory[] formatCats = fc.getFormatCategories();
        switch (invc.value()) {
          case VARARG:
            Result<TypeMirror>[] argTypes = fc.getArgTypes();
            int argl = argTypes.length;
            int formatl = formatCats.length;
            if (argl < formatl) {
              // For assignments, format.missing.arguments is issued from commonAssignmentCheck.
              // II.1
              ftu.failure(invc, "format.missing.arguments", formatl, argl);
            } else {
              if (argl > formatl) {
                // II.2
                ftu.warning(invc, "format.excess.arguments", formatl, argl);
              }
              for (int i = 0; i < formatl; ++i) {
                ConversionCategory formatCat = formatCats[i];
                Result<TypeMirror> arg = argTypes[i];
                TypeMirror argType = arg.value();

                switch (formatCat) {
                  case UNUSED:
                    // I.2
                    ftu.warning(arg, "format.argument.unused", " " + (1 + i));
                    break;
                  case NULL:
                    // I.3
                    if (argType.getKind() == TypeKind.NULL) {
                      ftu.warning(arg, "format.specifier.null", " " + (1 + i));
                    } else {
                      ftu.failure(arg, "format.specifier.null", " " + (1 + i));
                    }
                    break;
                  case GENERAL:
                    break;
                  default:
                    if (!fc.isValidArgument(formatCat, argType)) {
                      // II.3
                      ExecutableElement method = TreeUtils.elementFromUse(node);
                      CharSequence methodName = ElementUtils.getSimpleNameOrDescription(method);
                      ftu.failure(
                          arg,
                          "argument.type.incompatible",
                          "in varargs position",
                          methodName,
                          argType,
                          formatCat);
                    }
                    break;
                }
              }
            }
            break;
          case ARRAY:
            // III
            if (!isWrappedFormatCall(fc, enclosingMethod)) {
              ftu.warning(invc, "format.indirect.arguments");
            }
            // TODO:  If it is explict array construction, such as "new Object[] { ... }", then we
            // could treat it like the VARARGS case, analyzing each argument.  "new array" is
            // probably rare, in the varargs position.  fall through
          case NULLARRAY:
            for (ConversionCategory cat : formatCats) {
              if (cat == ConversionCategory.NULL) {
                // I.3
                if (invc.value() == FormatterTreeUtil.InvocationType.NULLARRAY) {
                  ftu.warning(invc, "format.specifier.null", "");
                } else {
                  ftu.failure(invc, "format.specifier.null", "");
                }
              }
              if (cat == ConversionCategory.UNUSED) {
                // I.2
                ftu.warning(invc, "format.argument.unused", "");
              }
            }
            break;
        }
      }

      // Support -Ainfer command-line argument.
      WholeProgramInference wpi = atypeFactory.getWholeProgramInference();
      if (wpi != null && forwardsArguments(node, enclosingMethod)) {
        wpi.addMethodDeclarationAnnotation(
            TreeUtils.elementFromDeclaration(enclosingMethod), atypeFactory.FORMATMETHOD);
      }
    }
    return super.visitMethodInvocation(node, p);
  }

  /**
   * Returns true if {@code fc} is within a method m annotated as {@code @FormatMethod}, and fc's
   * arguments are m's formal parameters. In other words, fc forwards m's arguments to another
   * format method.
   *
   * @param fc an invocation of a format method
   * @param enclosingMethod the method that contains the call
   * @return true if {@code fc} is a call to a format method that forwards its containing method's
   *     arguments
   */
  private boolean isWrappedFormatCall(FormatCall fc, @Nullable MethodTree enclosingMethod) {
    if (enclosingMethod == null) {
      return false;
    }
    ExecutableElement enclosingMethodElement = TreeUtils.elementFromDeclaration(enclosingMethod);
    boolean withinFormatMethod =
        (atypeFactory.getDeclAnnotation(enclosingMethodElement, FormatMethod.class) != null);
    return withinFormatMethod && forwardsArguments(fc.invocationTree, enclosingMethod);
  }

  /**
   * Returns true if {@code invocationTree}'s arguments are {@code enclosingMethod}'s formal
   * parameters. In other words, {@code invocationTree} forwards {@code enclosingMethod}'s
   * arguments.
   *
   * <p>Only arguments from the first String formal parameter onward count. Returns false if there
   * is no String formal parameter.
   *
   * @param invocationTree an invocation of a method
   * @param enclosingMethod the method that contains the call
   * @return true if {@code invocationTree} is a call to a method that forwards its containing
   *     method's arguments
   */
  private boolean forwardsArguments(
      MethodInvocationTree invocationTree, @Nullable MethodTree enclosingMethod) {

    if (enclosingMethod == null) {
      return false;
    }

    ExecutableElement enclosingMethodElement = TreeUtils.elementFromDeclaration(enclosingMethod);
    int paramIndex = formatStringIndex(enclosingMethodElement);
    if (paramIndex == -1) {
      return false;
    }

    ExecutableElement calledMethodElement = TreeUtils.elementFromUse(invocationTree);
    int callIndex = formatStringIndex(calledMethodElement);
    if (callIndex == -1) {
      throw new BugInCF(
          "Method "
              + calledMethodElement
              + " is annotated @FormatMethod but has no String formal parameter");
    }

    List<? extends ExpressionTree> args = invocationTree.getArguments();
    List<? extends VariableTree> params = enclosingMethod.getParameters();

    if (params.size() - paramIndex != args.size() - callIndex) {
      return false;
    }
    while (paramIndex < params.size()) {
      ExpressionTree argTree = args.get(callIndex);
      if (argTree.getKind() != Tree.Kind.IDENTIFIER) {
        return false;
      }
      VariableTree param = params.get(paramIndex);
      if (param.getName() != ((IdentifierTree) argTree).getName()) {
        return false;
      }
      paramIndex++;
      callIndex++;
    }

    return true;
  }

  // TODO: Should this be the last String argument?  That would require that every method
  // annotated with @FormatMethod uses varargs syntax.
  /**
   * Returns the index of the format string of a method: the first formal parameter with declared
   * type String.
   *
   * @param m a method
   * @return the index of the last String formal parameter, or -1 if none
   */
  public static int formatStringIndex(ExecutableElement m) {
    List<? extends VariableElement> params = m.getParameters();
    for (int i = 0; i < params.size(); i++) {
      if (TypesUtils.isString(params.get(i).asType())) {
        return i;
      }
    }
    return -1;
  }

  @Override
  protected void commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);

    AnnotationMirror rhs = valueType.getAnnotationInHierarchy(atypeFactory.UNKNOWNFORMAT);
    AnnotationMirror lhs = varType.getAnnotationInHierarchy(atypeFactory.UNKNOWNFORMAT);

    // From the manual: "It is legal to use a format string with fewer format specifiers
    // than required, but a warning is issued."
    // The format.missing.arguments warning is issued here for assignments.
    // For method calls, it is issued in visitMethodInvocation.
    if (rhs != null
        && lhs != null
        && AnnotationUtils.areSameByName(rhs, FormatterAnnotatedTypeFactory.FORMAT_NAME)
        && AnnotationUtils.areSameByName(lhs, FormatterAnnotatedTypeFactory.FORMAT_NAME)) {
      ConversionCategory[] rhsArgTypes = atypeFactory.treeUtil.formatAnnotationToCategories(rhs);
      ConversionCategory[] lhsArgTypes = atypeFactory.treeUtil.formatAnnotationToCategories(lhs);

      if (rhsArgTypes.length < lhsArgTypes.length) {
        checker.reportWarning(
            valueTree, "format.missing.arguments", varType.toString(), valueType.toString());
      }
    }
  }
}
