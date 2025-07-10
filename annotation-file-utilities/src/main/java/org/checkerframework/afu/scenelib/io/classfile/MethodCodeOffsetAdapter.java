package org.checkerframework.afu.scenelib.io.classfile;

import org.objectweb.asm.ClassReader;
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
class MethodCodeOffsetAdapter extends MethodVisitor {

  /** ClassReader for reading the class file. */
  private final ClassReader classReader;

  /** Offset from start of class file to code attribute for method. */
  private int codeStart = 0;

  /** Number of attributes for this method. */
  private int attrCount = 0;

  /** Offset from start of bytecodes to current instruction. */
  private int offset;

  /** Offset from start of bytecodes to previous instruction. */
  private int previousOffset;

  /**
   * Constructs a new MethodCodeOffsetAdapter.
   *
   * @param classReader the ClassReader for the class
   * @param methodVisitor the MethodVisitor for this method
   * @param start the offset to the start of the class attributes
   */
  public MethodCodeOffsetAdapter(ClassReader classReader, MethodVisitor methodVisitor, int start) {
    super(Opcodes.ASM8, methodVisitor);
    char[] buf = new char[classReader.header];
    this.classReader = classReader;
    // const pool size is (not lowest) upper bound of string length
    codeStart = start;
    attrCount = classReader.readUnsignedShort(codeStart + 6);

    // find code attribute
    codeStart += 8;
    while (attrCount > 0) {
      String attrName = classReader.readUTF8(codeStart, buf);
      if ("Code".equals(attrName)) {
        break;
      }
      codeStart += 6 + classReader.readInt(codeStart + 2);
      --attrCount;
    }
  }

  /**
   * @param i code offset at which to read int
   * @return int represented (big-endian) by four bytes starting at {@code i}
   */
  private int readInt(int i) {
    return classReader.readInt(codeStart + i);
  }

  /**
   * Record current {@link #offset} as {@link #previousOffset} and increment by {@code i}.
   *
   * @param i amount to advance offset
   */
  private void advance(int i) {
    previousOffset = offset;
    offset += i;
  }

  /**
   * Returns the class offset of the code attribute; 0 for abstract methods.
   *
   * @return class offset of code attribute; 0 for abstract methods
   */
  public int getCodeStart() {
    return codeStart;
  }

  /**
   * Returns the offset of the instruction just visited; -1 before first visit.
   *
   * @return offset of instruction just visited; -1 before first visit
   */
  public int getPreviousOffset() {
    return previousOffset;
  }

  /**
   * Returns the offset after instruction just visited; 0 before first visit, -1 if {@link
   * #visitEnd()} has been called
   *
   * @return offset after instruction just visited; 0 before first visit, -1 if {@link #visitEnd()}
   *     has been called
   */
  public int getCurrentOffset() {
    return offset;
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    advance(3);
  }

  @Override
  public void visitIincInsn(int var, int increment) {
    super.visitIincInsn(var, increment);
    advance(3);
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    advance(1);
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    advance(opcode == Opcodes.SIPUSH ? 3 : 2);
  }

  @Override
  public void visitInvokeDynamicInsn(
      String name, String descriptor, Handle bsm, Object... bsmArgs) {
    super.visitInvokeDynamicInsn(name, descriptor, bsm, bsmArgs);
    advance(5);
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    advance(3);
  }

  @Override
  public void visitLdcInsn(Object cst) {
    super.visitLdcInsn(cst);
    advance(2);
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    previousOffset = offset;
    offset += 8 - ((offset - codeStart) & 3);
    offset += 4 + 8 * readInt(offset);
  }

  @Deprecated
  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
    super.visitMethodInsn(opcode, owner, name, descriptor);
    advance(opcode == Opcodes.INVOKEINTERFACE ? 5 : 3);
  }

  @Override
  public void visitMethodInsn(
      int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    advance(opcode == Opcodes.INVOKEINTERFACE ? 5 : 3);
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int dims) {
    super.visitMultiANewArrayInsn(descriptor, dims);
    advance(4);
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    previousOffset = offset;
    offset += 8 - ((offset - codeStart) & 3);
    offset += 4 * (readInt(offset + 4) - readInt(offset) + 3);
  }

  @Override
  public void visitTypeInsn(int opcode, String descriptor) {
    super.visitTypeInsn(opcode, descriptor);
    advance(3);
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    super.visitVarInsn(opcode, var);
    advance(var < 4 ? 1 : 2);
  }

  @Override
  public void visitEnd() {
    offset = -1; // invalidated
  }
}
