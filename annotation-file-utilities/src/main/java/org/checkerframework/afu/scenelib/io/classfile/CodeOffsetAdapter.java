package org.checkerframework.afu.scenelib.io.classfile;

import java.util.Arrays;
import org.checkerframework.afu.scenelib.io.DebugWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Tracks offset within a method's Code attribute as its instructions are visited. ASM really should
 * expose this information but doesn't. Class implementation simply does the same arithmetic ASM
 * does under the hood, staying synchronized with the {@link ClassReader}.
 *
 * <p>UNDONE: Why both CodeOffsetAdapter and MethodCodeOffsetAdapter?
 */
public class CodeOffsetAdapter extends ClassVisitor {

  /** Writer for outputting debug information. */
  static final DebugWriter debug = new DebugWriter(false);

  /** ClassReader for reading the class file. */
  final ClassReader classReader;

  /** Buffer for calls to ClassReader read methods. */
  final char[] buffer;

  /** Offset from start of class file to current method. */
  int methodStart;

  /** Offset from start of class file to code attribute for method. */
  int codeStart;

  /** Offset from start of bytecodes to current instruction. */
  int offset;

  /** Offset from start of bytecodes to previous instruction. */
  int previousOffset;

  /**
   * Constructs a new CodeOffsetAdapter. For some reason, it is necessary to use ClassWriter to
   * ensure that all labels are visited.
   *
   * @param api the ASM API version to use
   * @param classReader the ClassReader for the class
   */
  public CodeOffsetAdapter(int api, ClassReader classReader) {
    super(api, new ClassWriter(classReader, 0));
    this.classReader = classReader;
    // const pool size is (not lowest) upper bound of string length
    buffer = new char[classReader.header];
    // find beginning of methods
    methodStart = classReader.header + 6;
    methodStart += 4 + 2 * classReader.readUnsignedShort(methodStart);
    for (int i = classReader.readUnsignedShort(methodStart - 2); i > 0; --i) {
      methodStart += 8;
      for (int j = classReader.readUnsignedShort(methodStart - 2); j > 0; --j) {
        methodStart += 6 + classReader.readInt(methodStart + 2);
      }
    }
    methodStart += 2;
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String descriptor, String signature, String[] exceptions) {
    MethodVisitor methodVisitor =
        super.visitMethod(access, name, descriptor, signature, exceptions);
    return new MethodVisitor(api, methodVisitor) {
      /** Offset from start of class file to end of current method. */
      private int methodEnd;

      {
        String name = classReader.readUTF8(methodStart + 2, buffer);
        String descriptor = classReader.readUTF8(methodStart + 4, buffer);
        int attrCount = classReader.readUnsignedShort(methodStart + 6);
        debug.debug("visiting %s%s (%d)%n", name, descriptor, methodStart);
        debug.debug("%d attributes%n", attrCount);
        methodEnd = methodStart + 8;

        // find code attribute
        codeStart = methodEnd;
        if (attrCount > 0) {
          while (--attrCount >= 0) {
            String attrName = classReader.readUTF8(codeStart, buffer);
            debug.debug("attribute %s%n", attrName);
            if ("Code".equals(attrName)) {
              codeStart += 6;
              offset = codeStart + classReader.readInt(codeStart - 4);
              codeStart += 8;
              while (--attrCount >= 0) {
                debug.debug("attribute %s%n", classReader.readUTF8(offset, buffer));
                offset += 6 + classReader.readInt(offset + 2);
              }
              methodEnd = offset;
              break;
            }
            codeStart += 6 + classReader.readInt(codeStart + 2);
            methodEnd = codeStart;
          }
        }
        offset = 0;
        previousOffset = -1;
      }

      /**
       * Convenience method to read an int from the class file at a particular code attribute
       * offset.
       *
       * @param i bytecode offset to read
       * @return int read
       */
      private int readInt(int i) {
        return classReader.readInt(codeStart + i);
      }

      @Override
      public void visitLabel(Label label) {
        super.visitLabel(label);
      }

      @Override
      public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        debug.debug("%d visitFieldInsn(%d, %s, %s, %s)%n", offset, opcode, owner, name, descriptor);
        advance(3);
      }

      @Override
      public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        debug.debug("%d visitIincInsn(%d, %d)%n", offset, var, increment);
        advance(3);
      }

      @Override
      public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        debug.debug("%d visitInsn(%d)%n", offset, opcode);
        advance(1);
      }

      @Override
      public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        debug.debug("%d visitIntInsn(%d, %d)%n", offset, opcode, operand);
        advance(opcode == Opcodes.SIPUSH ? 3 : 2);
      }

      @Override
      public void visitInvokeDynamicInsn(
          String name, String descriptor, Handle bsm, Object... bsmArgs) {
        super.visitInvokeDynamicInsn(name, descriptor, bsm, bsmArgs);
        debug.debug(
            "%d visitInvokeDynamicInsn(%s, %s, %s, %s)%n",
            offset, name, descriptor, bsm, Arrays.toString(bsmArgs));
        advance(5);
      }

      @Override
      public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        debug.debug("%d visitJumpInsn(%d, %s)%n", offset, opcode, label);
        // account for wide instructions goto_w (200) and jsr_w (201)
        advance(classReader.readByte(codeStart + offset) < 200 ? 3 : 4);
        assert offset > 0 && methodEnd > codeStart + offset;
      }

      @Override
      public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);
        debug.debug("%d visitLdcInsn(%s)%n", offset, cst);
        // account for wide instructions ldc_w (19) and ldc2_w (20)
        advance(classReader.readByte(codeStart + offset) > 18 ? 3 : 2);
        assert offset > 0 && methodEnd > codeStart + offset;
      }

      @Override
      public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        debug.debug(
            "%d visitLookupSwitchInsn(%s, %s, %s)%n",
            offset, dflt, Arrays.toString(keys), Arrays.toString(labels));
        previousOffset = offset;
        offset += 8 - (offset & 3);
        offset += 4 + 8 * readInt(offset);
        assert offset > 0 && methodEnd > codeStart + offset;
      }

      @Deprecated
      @Override
      public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
        super.visitMethodInsn(opcode, owner, name, descriptor);
        debug.debug(
            "%d visitMethodInsn(%d, %s, %s, %s)%n", offset, opcode, owner, name, descriptor);
        advance(opcode == Opcodes.INVOKEINTERFACE ? 5 : 3);
      }

      @Override
      public void visitMethodInsn(
          int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        debug.debug(
            "%d visitMethodInsn(%d, %s, %s, %s, %s)%n",
            offset, opcode, owner, name, descriptor, isInterface);
        advance(opcode == Opcodes.INVOKEINTERFACE ? 5 : 3);
      }

      @Override
      public void visitMultiANewArrayInsn(String descriptor, int dims) {
        super.visitMultiANewArrayInsn(descriptor, dims);
        debug.debug("%d visitMultiANewArrayInsn(%s, %d)%n", offset, descriptor, dims);
        advance(4);
      }

      @Override
      public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        debug.debug(
            "%d visitTableSwitchInsn(%d, %d, %s, %s)%n",
            offset, min, max, dflt, Arrays.toString(labels));
        previousOffset = offset;
        offset += 8 - (offset & 3);
        offset += 4 * (readInt(offset + 4) - readInt(offset) + 3);
        assert offset > 0 && methodEnd > codeStart + offset;
      }

      @Override
      public void visitTypeInsn(int opcode, String descriptor) {
        super.visitTypeInsn(opcode, descriptor);
        debug.debug("%d visitTypeInsn(%d, %s)%n", offset, opcode, descriptor);
        advance(3);
      }

      @Override
      public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        debug.debug("%d visitVarInsn(%d, %d)%n", offset, opcode, var);
        advance(var < 4 ? 1 : 2);
      }

      @Override
      public void visitEnd() {
        super.visitEnd();
        methodStart = methodEnd;
      }
    };
  }

  /**
   * Fetch previousOffset.
   *
   * @return previousOffset
   */
  public int getPreviousCodeOffset() {
    return previousOffset;
  }

  /**
   * Fetch offset.
   *
   * @return offset
   */
  public int getMethodCodeOffset() {
    return offset;
  }

  /**
   * Fetch bytecode offset.
   *
   * @return bytecode offset
   */
  public int getBytecodeOffset() {
    return codeStart + offset;
  }

  // move ahead, marking previous position
  /**
   * Save offset to previousOffset and advance offset.
   *
   * @param n amount to advance offset
   */
  private void advance(int n) {
    previousOffset = offset;
    offset += n;
  }
}
