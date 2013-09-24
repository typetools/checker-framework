package daikon.dcomp;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.Type;
import org.apache.bcel.verifier.structurals.OperandStack;
import org.apache.bcel.verifier.structurals.LocalVariables;
import org.apache.bcel.verifier.structurals.Frame;
import org.apache.bcel.verifier.structurals.UninitializedObjectType;


/**
 * Stores the types on the stack at each instruction (identified by
 * byte code offset) in a method
 */
public final class StackTypes {

  boolean track_locals = true;
  OperandStack[] os_arr;
  LocalVariables[] loc_arr;

  /**
   * TODO
   * @param mg
   */
  public StackTypes (MethodGen mg) {
    InstructionList il = mg.getInstructionList();
    int size = 0;
    if (il != null)
      size = il.getEnd().getPosition();
    os_arr = new OperandStack[size+1];
    if (track_locals)
      loc_arr = new LocalVariables[size+1];
  }

  /** Sets the stack for the instruction at the specified offset **/
  public void set (int offset, Frame f) {

    OperandStack os = f.getStack();
    // logger.info ("stack[" + offset + "] = " + toString(os));

    if (track_locals)
      loc_arr[offset] = (LocalVariables) f.getLocals().clone();

    os_arr[offset] = (OperandStack) os.clone();
  }

  /** Returns the stack contents at the specified offset **/
  public OperandStack get (int offset) {
    return (os_arr[offset]);
  }

  public String toString() {

    StringBuffer sb = new StringBuffer();

    for (int i = 0; i < os_arr.length; i++) {
      if (os_arr[i] != null) {
        sb.append (String.format ("Instruction %d:\n", i));
        sb.append (String.format ("  stack:  %s\n", toString(os_arr[i])));
        if (track_locals)
          sb.append (String.format ("  locals: %s\n", toString (loc_arr[i])));
      }
    }

    return (sb.toString());
  }

  /**
   * TODO
   */
  public String toString (OperandStack os) {

    String buff = "";

    for (int i = 0; i < os.size(); i++) {
      if (buff.length() > 0)
        buff += ", ";
      Type t = os.peek(i);
      if (t instanceof UninitializedObjectType)
        buff += "uninitialized-object";
      else
        buff += t;
    }

    return ("{" + buff + "}");
  }

  /**
   * TODO
   */
  public String toString (LocalVariables lv) {

    String buff = "";

    for (int i = 0; i < lv.maxLocals(); i++) {
      if (buff.length() > 0)
        buff += ", ";
      Type t = lv.get(i);
      if (t instanceof UninitializedObjectType)
        buff += "uninitialized-object";
      else
        buff += t;
    }
    return ("{" + buff + "}");
  }
}
