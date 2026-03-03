package org.checkerframework.afu.scenelib.el;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Label;

/**
 * A {@link LocalLocation} holds information about a local variable. A variable may have multiple
 * lifetimes. We store this information the same way ASM does, as 3 parallel arrays.
 */
public final class LocalLocation {
  /**
   * The starts of the lifetimes for the variable. Used only for TypeReference#LOCAL_VARIABLE and
   * TypeReference#RESOURCE_VARIABLE.
   */
  public final Label[] start;

  /**
   * The ends of the lifetimes for the variable. Used only for TypeReference#LOCAL_VARIABLE and
   * TypeReference#RESOURCE_VARIABLE.
   */
  public final Label[] end;

  /**
   * The indices for the variable. Each element of the index array contains the local variable's
   * offset from the stack frame for the corresponding lifetime. Used only for
   * TypeReference#LOCAL_VARIABLE and TypeReference#RESOURCE_VARIABLE.
   */
  public final int[] index;

  /**
   * The name of the variable.
   *
   * <p>This is not part of the abstract state of the LocalLocation: it is not read by equals(),
   * hashCode(), or toString().
   */
  public final @Nullable String variableName;

  /**
   * Construct a new LocalLocation. This constructor does not assign meaningful values to start or
   * end. Thus, the getScopeStart and getScopeLenth methods must not be used on the result.
   *
   * @param index the offset of the variable in the stack frame
   * @param variableName the name of the local variable
   */
  public LocalLocation(int index, String variableName) {
    this(new Label[] {new Label()}, new Label[] {new Label()}, new int[] {index}, variableName);
  }

  /**
   * Construct a new LocalLocation.
   *
   * @param start the code offsets to the starts of the variable's lifetimes
   * @param end the code offsets to the ends of the variable's lifetimes
   * @param index the stack frame offsets of the variable's lifetimes
   * @param variableName the name of the local variable
   */
  public LocalLocation(Label[] start, Label[] end, int[] index, String variableName) {
    this.start = start;
    this.end = end;
    this.index = index;
    this.variableName = variableName;
  }

  /**
   * Construct a new LocalLocation representing a single scope/lifetime. Only being used by Writers,
   * not Readers for now. Should possibly deprecate this in the future. Changes values reflectively.
   *
   * @param scopeStart the bytecode offset of the start of the variable's lifetime
   * @param scopeLength the bytecode length of the variable's lifetime
   * @param index the offset of the variable in the stack frame
   */
  @SuppressWarnings({"NarrowingCompoundAssignment", "CatchAndPrintStackTrace"}) // TODO
  public LocalLocation(int scopeStart, int scopeLength, int index) {
    Label startLabel = new Label();
    Label endLabel = new Label();

    try {
      Field flagsField = Label.class.getDeclaredField("flags");
      Field bytecodeOffsetField = Label.class.getDeclaredField("bytecodeOffset");
      Field FLAG_RESOLVED_FIELD = Label.class.getDeclaredField("FLAG_RESOLVED");

      flagsField.setAccessible(true);
      bytecodeOffsetField.setAccessible(true);
      FLAG_RESOLVED_FIELD.setAccessible(true);
      // Label.FLAG_RESOLVED is int, but its value is 4 and `Label.flags` is short
      short FLAG_RESOLVED = (short) (int) (Integer) FLAG_RESOLVED_FIELD.get(null);

      short flags = (Short) flagsField.get(startLabel);
      flags |= FLAG_RESOLVED;
      flagsField.set(startLabel, flags);
      bytecodeOffsetField.set(startLabel, scopeStart);

      flags = (Short) flagsField.get(endLabel);
      flags |= FLAG_RESOLVED;
      flagsField.set(endLabel, flags);
      bytecodeOffsetField.set(endLabel, scopeStart + scopeLength);
    } catch (Exception e) {
      throw new Error(e);
    }

    this.start = new Label[] {startLabel};
    this.end = new Label[] {endLabel};
    this.index = new int[] {index};
    this.variableName = null;
  }

  /**
   * Test if the bytecode offset to the start of the first scope/lifetime is defined.
   *
   * @return if the Label at start[0] is resolved
   */
  public boolean scopeStartDefined() {
    try {
      start[0].getOffset();
    } catch (IllegalStateException e) {
      return false;
    }
    return true;
  }

  /**
   * Returns the bytecode offset to the start of the first scope/lifetime.
   *
   * @return the bytecode offset to the start of the first scope/lifetime
   */
  public int getScopeStart() {
    try {
      return start[0].getOffset();
    } catch (IllegalStateException e) {
      throw new Error("Labels not resolved: " + Arrays.toString(start));
    }
  }

  // This is used only in IndexFileWriter.
  /**
   * Returns the length of all the scopes/lifetimes (in bytes).
   *
   * @return the length of all the scopes/lifetimes (in bytes)
   */
  public int getScopeLength() {
    try {
      return end[end.length - 1].getOffset() - getScopeStart();
    } catch (IllegalStateException e) {
      throw new Error("Labels not resolved: " + Arrays.toString(start));
    }
  }

  /**
   * Returns the local variable index of its first scope/lifetime.
   *
   * @return the local variable index
   */
  public int getVarIndex() {
    return index[0];
  }

  /**
   * Returns true if this {@link LocalLocation} equals {@code o}; a slightly faster variant of
   * {@link #equals(Object)} for when the argument is statically known to be another nonnull {@link
   * LocalLocation}.
   *
   * @param o the {@code LocalLocation} to compare to this
   * @return true if this equals {@code o}
   */
  public boolean equals(LocalLocation o) {
    return Arrays.equals(start, o.start)
        && Arrays.equals(end, o.end)
        && Arrays.equals(index, o.index)
        && (variableName == null || variableName.equals(o.variableName));
  }

  @Override
  public boolean equals(/*@ReadOnly*/ Object o) {
    return o instanceof LocalLocation && equals((LocalLocation) o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(start), Arrays.hashCode(end), Arrays.hashCode(index));
  }

  @Override
  public String toString() {
    return "LocalLocation{"
        + "start="
        + Arrays.toString(start)
        + ", end="
        + Arrays.toString(end)
        + ", index="
        + Arrays.toString(index)
        + '}';
  }
}
