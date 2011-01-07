package daikon.chicory;

import java.util.*;

import daikon.*;
import daikon.VarInfo.VarFlags;

/**
 * The StringInfo class is a subtype of DaikonVariableInfo used for
 * variable types that can be converted into strings (.toString())
 */
public class StringInfo extends DaikonVariableInfo
{
    public StringInfo(String theName, boolean arr)
    {
        super(theName, arr);
    }

    public Object getMyValFromParentVal(Object value)
    {
        return value;
    }


    /**
     * Returns a String which contains a string representation of val, used for
     * dtrace information.
     */
    @SuppressWarnings("unhecked")
    public String getDTraceValueString(Object val)
    {
        if (isArray)
        {
            @SuppressWarnings("unchecked")
            List valAsList = (List)val;
            return getStringList(valAsList);
        }
        else
        {
            return getValueStringNonArr(val);
        }
    }

    /**
     * Returns a space-separated String of the elements in theValues.
     * If theValues is null, returns "null." If theValues is
     * nonsensical, returns "nonsensical".
     *
     * @param theValues A list of values, each is a String or NonsensicalObject or NonsensicalList.
     * @return a space-separated String of the elements in theValues
     */
    public static String getStringList(List theValues)
    {
        if (theValues == null)
        {
            //buf.append("null");
            return "null" + DaikonWriter.lineSep + "1";
        }

        // assert !NonsensicalList.isNonsensicalList (theValues);
        if (NonsensicalList.isNonsensicalList (theValues)
            || theValues instanceof NonsensicalObject)
        {
            //buf.append("nonsensical");
            return "nonsensical" + DaikonWriter.lineSep + "2";
        }

        StringBuffer buf = new StringBuffer();

        buf.append("[");
        for (Iterator iter = theValues.iterator(); iter.hasNext();)
        {
            Object str = iter.next();

            if (str == null) {
                buf.append(str); // appends "null"
            } else if (str instanceof String) {
                buf.append("\"" + encodeString((String) str) + "\"");
            } else if (str instanceof NonsensicalObject
                       || str instanceof NonsensicalList) {
                buf.append("nonsensical");
            } else {
                throw new Error("Impossible");
            }

            // Put space between elements in array
            if (iter.hasNext())
                buf.append(" ");
        }
        buf.append("]");

        if (NonsensicalList.isNonsensicalList (theValues))
            buf.append(DaikonWriter.lineSep + "2");
        else
            buf.append(DaikonWriter.lineSep + "1");

        return buf.toString();
    }


    /**
     * Similar to showStringList, but used for non-array objects.
     */
    public String getValueStringNonArr(Object val)
    {
        String retString;

        if (val == null)
            retString = ("null" + DaikonWriter.lineSep);
        else if (val instanceof NonsensicalObject)
            retString = ("nonsensical" + DaikonWriter.lineSep);
        else
        {
            retString = getString((String) val);
            retString += DaikonWriter.lineSep;
        }

        if (val instanceof NonsensicalObject)
            retString += ("2");
        else
            retString += ("1");

        return retString;
    }

    //encodes a string: surrounds in quotes and removes line breaks
    private String getString(String stringRef)
    {
        return ("\"" + encodeString(stringRef) + "\"");
    }

    //removes endlines in string
    private static String encodeString(String input)
    {
        return Runtime.quote(input);
    }

    /** toString is a function **/
    public VarInfo.VarKind get_var_kind() {
        return VarInfo.VarKind.FUNCTION;
    }

    /** Returns the name of this function **/
    public String get_relative_name() {
        return "toString()";
    }

  public EnumSet<VarFlags> get_var_flags() {
    EnumSet<VarFlags> flags = super.get_var_flags().clone();
    flags.add (VarFlags.SYNTHETIC);
    flags.add (VarFlags.TO_STRING);
    return (flags);
  }

}
