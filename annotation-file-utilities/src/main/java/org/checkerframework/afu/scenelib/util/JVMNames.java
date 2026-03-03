package org.checkerframework.afu.scenelib.util;

import com.google.errorprone.annotations.InlineMe;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.signature.qual.FieldDescriptor;
import org.plumelib.reflection.Signatures;

// TODO: Move much of this class to reflection-util, if no special classpath manipulation is
// required to get the com.sun and javax.lang classes on the classpath.
/** Class to generate class formatted names from Trees. */
public class JVMNames {

  /** Creates a new JVMNames. */
  public JVMNames() {}

  /**
   * Converts a MethodTree into a JVML format method signature. There is probably an API to do this,
   * but I couldn't find it.
   *
   * @param methodTree the tree to convert
   * @return a String signature of methodTree in jvml format
   * @deprecated use {@link #getJVMMethodSignature(MethodTree)}
   */
  @Deprecated // use getJVMMethodSignature(MethodTree)
  @InlineMe(
      replacement = "JVMNames.getJVMMethodSignature(methodTree)",
      imports = "org.checkerframework.afu.scenelib.util.JVMNames")
  public static String getJVMMethodName(MethodTree methodTree) {
    return getJVMMethodSignature(methodTree);
  }

  /**
   * Converts a MethodTree into a JVML format method signature. There is probably an API to do this,
   * but I couldn't find it.
   *
   * @param methodTree the tree to convert
   * @return a String signature of methodTree in JVML format
   */
  public static String getJVMMethodSignature(MethodTree methodTree) {
    ExecutableElement methodElement = ((JCMethodDecl) methodTree).sym;
    StringBuilder builder = new StringBuilder();
    String returnTypeStr;
    builder.append(methodTree.getName());
    builder.append("(");

    if (methodElement == null) {
      // use source AST in lieu of symbol table
      List<JCVariableDecl> params = ((JCMethodDecl) methodTree).params;
      JCVariableDecl param = params.head;
      JCExpression typeTree = ((JCMethodDecl) methodTree).restype;
      returnTypeStr = treeToJVMLString(typeTree);
      while (param != null) {
        builder.append(treeToJVMLString(param.vartype));
        params = params.tail;
        param = params.head;
      }
    } else {
      TypeMirror returnType = methodElement.getReturnType();
      returnTypeStr = typeToJvmlString((Type) returnType);
      for (VariableElement ve : methodElement.getParameters()) {
        Type vt = (Type) ve.asType();
        if (vt.getTag() == TypeTag.TYPEVAR) {
          vt = vt.getUpperBound();
        }
        builder.append(typeToJvmlString(vt));
      }
    }
    builder.append(")");
    builder.append(returnTypeStr);
    return builder.toString();
  }

  /**
   * Converts a method element into a JVML format method signature. There is probably an API to do
   * this, but I couldn't find it.
   *
   * @param methodElement the method element to convert
   * @return a String signature of methodElement in JVML format
   * @deprecated use {@link #getJVMMethodSignature(ExecutableElement)}
   */
  @Deprecated // use getJVMMethodSignature(ExecutableElement)
  @InlineMe(
      replacement = "JVMNames.getJVMMethodSignature(methodElement)",
      imports = "org.checkerframework.afu.scenelib.util.JVMNames")
  public static String getJVMMethodName(ExecutableElement methodElement) {
    return getJVMMethodSignature(methodElement);
  }

  /**
   * Converts a method element into a JVML format method signature. There is probably an API to do
   * this, but I couldn't find it.
   *
   * @param methodElement the method element to convert
   * @return a String signature of methodElement in JVML format
   */
  public static String getJVMMethodSignature(ExecutableElement methodElement) {
    StringBuilder builder = new StringBuilder();
    String returnTypeStr;
    builder.append(methodElement.getSimpleName());
    builder.append("(");
    TypeMirror returnType = methodElement.getReturnType();
    returnTypeStr = typeToJvmlString((Type) returnType);
    for (VariableElement ve : methodElement.getParameters()) {
      Type vt = (Type) ve.asType();
      if (vt.getTag() == TypeTag.TYPEVAR) {
        vt = vt.getUpperBound();
      }
      builder.append(typeToJvmlString(vt));
    }
    builder.append(")");
    builder.append(returnTypeStr);
    return builder.toString();
  }

  /**
   * Create a JVML string for a type.
   *
   * @param type the Type to convert to JVML
   * @return the JVML representation of type
   */
  @SuppressWarnings("signature") // com.sun.tools.javac.code is not yet annotated
  public static String typeToJvmlString(Type type) {
    if (type.getKind() == TypeKind.ARRAY) {
      return "[" + typeToJvmlString((Type) ((ArrayType) type).getComponentType());
    } else if (type.getKind() == TypeKind.INTERSECTION) {
      // replace w/erasure (== erasure of 1st conjunct)
      return typeToJvmlString(type.tsym.erasure_field);
    } else if (type.getKind() == TypeKind.VOID) {
      return "V"; // special case since UtilPlume doesn't handle void
    } else {
      return Signatures.binaryNameToFieldDescriptor(type.tsym.flatName().toString());
    }
  }

  /**
   * Create a JVML string for an AST node representing a type.
   *
   * @param typeTree a Tree representing a type
   * @return the JVML representation of type
   */
  private static String treeToJVMLString(Tree typeTree) {
    StringBuilder builder = new StringBuilder();
    treeToJVMLString(typeTree, builder);
    return builder.toString();
  }

  /**
   * Append, to {@code builder}, the JVML string for {@code typeTree}.
   *
   * @param typeTree a type
   * @param builder where to output the type
   */
  @SuppressWarnings("signature") // com.sun.source.tree.Tree is not yet annotated
  private static void treeToJVMLString(Tree typeTree, StringBuilder builder) {
    switch (typeTree.getKind()) {
      case ARRAY_TYPE:
        builder.append('[');
        treeToJVMLString(((ArrayTypeTree) typeTree).getType(), builder);
        break;
      default:
        String str = typeTree.toString();
        builder.append(
            "void".equals(str) ? "V" : Signatures.binaryNameToFieldDescriptor(typeTree.toString()));
        break;
    }
  }

  /**
   * Converts a type in JVM format to a type in Java format.
   *
   * @param jvmType a type in JVM format
   * @return the type, in Java format
   */
  public static String jvmlStringToJavaTypeString(@FieldDescriptor String jvmType) {
    return jvmType.equals("V") ? "void" : Signatures.fieldDescriptorToBinaryName(jvmType);
  }
}
