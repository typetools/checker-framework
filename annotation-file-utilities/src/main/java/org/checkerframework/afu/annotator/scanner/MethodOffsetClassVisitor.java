package org.checkerframework.afu.annotator.scanner;

import com.sun.tools.javac.util.Pair;
import org.checkerframework.afu.scenelib.io.classfile.CodeOffsetAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * MethodOffsetClassVisitor is a class visitor that should be passed to ASM's ClassReader in order
 * to retrieve extra information about method offsets needed by all of the
 * org.checkerframework.afu.annotator.scanner classes. This visitor should visit every class that is
 * to be annotated, and should be done before trying to match elements in the tree to the various
 * criterion.
 */
// Note: in order to ensure all labels are visited, this class
// needs to extend ClassWriter and not other class visitor classes.
// There is no good reason why this is the case with ASM.
public class MethodOffsetClassVisitor extends ClassVisitor {

  /** The CodeOffsetAdapter to use. */
  CodeOffsetAdapter codeOffsetAdapter;

  /** The MethodVisitor to use. */
  MethodVisitor methodCodeOffsetAdapter;

  /** The name of the method currently being visisted. */
  private String methodName;

  /**
   * Constructs a new {@code MethodOffsetClassVisitor}.
   *
   * @param api which ASM api set to use
   * @param classReader the ClassReader to use
   * @param classWriter the ClassWriter to use
   */
  public MethodOffsetClassVisitor(int api, ClassReader classReader, ClassWriter classWriter) {
    super(api, classWriter);
    this.methodName = "LocalVariableVisitor: DEFAULT_METHOD";
    codeOffsetAdapter = new CodeOffsetAdapter(api, classReader);
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String descriptor, String signature, String[] exceptions) {
    methodName = name + descriptor.substring(0, descriptor.indexOf(")") + 1);
    methodCodeOffsetAdapter =
        codeOffsetAdapter.visitMethod(access, name, descriptor, signature, exceptions);
    return new MethodOffsetMethodVisitor(
        api, super.visitMethod(access, name, descriptor, signature, exceptions));
  }

  /**
   * MethodOffsetMethodVisitor is the method visitor that MethodOffsetClassVisitor uses to visit
   * particular methods and gather all the offset information by calling the appropriate static
   * methods in org.checkerframework.afu.annotator.scanner classes.
   */
  private class MethodOffsetMethodVisitor extends MethodVisitor {
    /**
     * Constructs a new {@code MethodOffsetMethodVisitor}.
     *
     * @param api which ASM api set to use
     * @param mv the MethodVisitor to be extended
     */
    public MethodOffsetMethodVisitor(int api, MethodVisitor mv) {
      super(api, mv);
    }

    /**
     * Returns the current scan position, given as an offset from the beginning of the method's code
     * attribute.
     *
     * @return the current scan position
     */
    public int getOffset() {
      return codeOffsetAdapter.getMethodCodeOffset();
    }

    @Override
    public void visitLocalVariable(
        String name, String descriptor, String signature, Label start, Label end, int index) {
      super.visitLocalVariable(name, descriptor, signature, start, end, index);
      LocalVariableScanner.addToMethodNameIndexMap(
          Pair.of(methodName, Pair.of(index, start.getOffset())), name);
      LocalVariableScanner.addToMethodNameCounter(methodName, name, start.getOffset());
      methodCodeOffsetAdapter.visitLocalVariable(name, descriptor, signature, start, end, index);
    }

    @Override
    public void visitLabel(Label label) {
      super.visitLabel(label);
      methodCodeOffsetAdapter.visitLabel(label);
    }

    @Override
    public void visitTypeInsn(int opcode, String descriptor) {
      super.visitTypeInsn(opcode, descriptor);
      switch (opcode) {
        case Opcodes.CHECKCAST:
          CastScanner.addCastToMethod(methodName, getOffset());
          break;
        case Opcodes.NEW:
        case Opcodes.ANEWARRAY:
          NewScanner.addNewToMethod(methodName, getOffset());
          break;
        case Opcodes.INSTANCEOF:
          InstanceOfScanner.addInstanceOfToMethod(methodName, getOffset());
          break;
      }
      methodCodeOffsetAdapter.visitTypeInsn(opcode, descriptor);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int dims) {
      super.visitMultiANewArrayInsn(descriptor, dims);
      NewScanner.addNewToMethod(methodName, getOffset());
      methodCodeOffsetAdapter.visitMultiANewArrayInsn(descriptor, dims);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
      super.visitIntInsn(opcode, operand);
      if (opcode == Opcodes.NEWARRAY) {
        NewScanner.addNewToMethod(methodName, getOffset());
      }
      methodCodeOffsetAdapter.visitIntInsn(opcode, operand);
    }

    @Deprecated
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
      super.visitMethodInsn(opcode, owner, name, descriptor);
      switch (opcode) {
        case Opcodes.INVOKEINTERFACE:
        case Opcodes.INVOKESTATIC:
        case Opcodes.INVOKEVIRTUAL:
          MethodCallScanner.addMethodCallToMethod(methodName, getOffset());
          break;
        default:
          break;
      }
      methodCodeOffsetAdapter.visitMethodInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(
        int opcode, String owner, String name, String descriptor, boolean isInterface) {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      switch (opcode) {
        case Opcodes.INVOKEINTERFACE:
        case Opcodes.INVOKESTATIC:
        case Opcodes.INVOKEVIRTUAL:
          MethodCallScanner.addMethodCallToMethod(methodName, getOffset());
          break;
        default:
          break;
      }
      methodCodeOffsetAdapter.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(
        String name, String descriptor, Handle bsm, Object... bsmArgs) {
      super.visitInvokeDynamicInsn(name, descriptor, bsm, bsmArgs);
      LambdaScanner.addLambdaExpressionToMethod(methodName, getOffset());
      methodCodeOffsetAdapter.visitInvokeDynamicInsn(name, descriptor, bsm, bsmArgs);
    }

    @Override
    public void visitCode() {
      super.visitCode();
      methodCodeOffsetAdapter.visitCode();
    }

    @Override
    public void visitInsn(int opcode) {
      super.visitInsn(opcode);
      methodCodeOffsetAdapter.visitInsn(opcode);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
      super.visitVarInsn(opcode, var);
      methodCodeOffsetAdapter.visitVarInsn(opcode, var);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
      super.visitFieldInsn(opcode, owner, name, descriptor);
      methodCodeOffsetAdapter.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
      super.visitJumpInsn(opcode, label);
      methodCodeOffsetAdapter.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object cst) {
      super.visitLdcInsn(cst);
      methodCodeOffsetAdapter.visitLdcInsn(cst);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
      super.visitIincInsn(var, increment);
      methodCodeOffsetAdapter.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
      super.visitTableSwitchInsn(min, max, dflt, labels);
      methodCodeOffsetAdapter.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      super.visitLookupSwitchInsn(dflt, keys, labels);
      methodCodeOffsetAdapter.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitEnd() {
      super.visitEnd();
      methodCodeOffsetAdapter.visitEnd();
    }
  }
}
