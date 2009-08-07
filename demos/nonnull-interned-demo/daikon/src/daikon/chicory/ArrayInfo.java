package daikon.chicory;

import daikon.*;

/**
 *  The ArrayInfo class is a subtype of DaikonVariableInfo used for
 * variable types which are arrays (i.e., their name ends with "[]").
 */
public class ArrayInfo extends DaikonVariableInfo
{
    /** Component type of the array **/
    Class array_type = null;

    /**
     * Constructs an ArrayInfo object with the specified name
     * and type.
     * @param theName The variable name. Should end with "[]"
     * @param array_type component type of the array
     */
    public ArrayInfo (String theName, Class array_type) {

        super (theName, true);
        this.array_type = array_type;
    }

    public Object getMyValFromParentVal(Object value)
    {
        if (value == null)
        {
            return null;
        }
        else if (value instanceof NonsensicalObject)
        {
            return NonsensicalList.getInstance();
        }
        //the "child" value of an array is the actual list of array values
        //as opposed to just the "hashcode" object
        else
            return DTraceWriter.getListFromArray(value);
    }

    public Class getType()
    {
        return array_type;
    }

    public VarInfo.VarKind get_var_kind() {
        return VarInfo.VarKind.ARRAY;
    }
}
