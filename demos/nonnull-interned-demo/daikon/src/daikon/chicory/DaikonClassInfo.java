/*
 * Created on May 3, 2005
 */

package daikon.chicory;

import java.util.*;

import daikon.*;
import daikon.VarInfo.VarFlags;

/**
 * The DaikonClassInfo class is a subtype of DaikonVariableInfo used
 * for variables which represent the runtime type of a variable (ie,
 * ".getClass()" variables).
 */
public class DaikonClassInfo extends DaikonVariableInfo
{

    /**
     * Constructs a DaikonClassInfo object
     * @param theName The name of the variable
     * @param isArr True iff the variable represents an array of runtime classes
     */
    public DaikonClassInfo(String theName, boolean isArr)
    {
        super(theName, isArr);
    }

    //.class variables are derived, so just keep the parent value
    public Object getMyValFromParentVal(Object value)
    {
        return value;
    }

    public String getDTraceValueString(Object val)
    {
        if (isArray)
        {
            if (val instanceof NonsensicalObject)
                return "nonsensical" + DaikonWriter.lineSep + "2";

            // A list of the runtime type of each value in the array.
            @SuppressWarnings("unchecked")
            List<String> name_list
              = DTraceWriter.getTypeNameList((List<Object>) val);
            if (name_list == null)
                return "nonsensical" + DaikonWriter.lineSep + "2";
            return StringInfo.getStringList(name_list);
        }
        else
        {
            return getValueStringNonArr(val);
        }
    }

    /**
     * Get a String representation of the given Object's runtime type and the
     * corresponding "modified" value
     * @param val The Object whose runtime class we wish to get a String representation of
     * @return String representation (suitable for a .dtrace file) of the
     * given Object's runtime type, and the "modified" value (modbit)
     */
    public String getValueStringNonArr(Object val)
    {
        String valString;

        if (val == null || val instanceof NonsensicalObject)
        {
            valString = "nonsensical" + DaikonWriter.lineSep +"2";
        }
        else
        {
            valString = ("\"" + DTraceWriter.stdClassName(val.getClass()) + "\"") + DaikonWriter.lineSep + "1";
        }

        return valString;
    }

  /** Returns function since essentially this is a call to a pure function **/
  public VarInfo.VarKind get_var_kind() {
    return VarInfo.VarKind.FUNCTION;
  }

  /** Returns the name of this field **/
  public String get_relative_name() {
    return "getClass()";
  }

  public EnumSet<VarFlags> get_var_flags() {
    EnumSet<VarFlags> flags = super.get_var_flags().clone();
    flags.add (VarFlags.SYNTHETIC);
    flags.add (VarFlags.CLASSNAME);
    return (flags);
  }
}
