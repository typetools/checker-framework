package daikon.chicory;

import java.util.*;

import daikon.*;
import static daikon.VarInfo.VarFlags;

/**
 * The ThisObjInfo class is a subtype of DaikonVariableInfo used for
 * variable types which represent the "this" object.s
 */
public class ThisObjInfo extends DaikonVariableInfo
{
    public Class type;

    public ThisObjInfo()
    {
        super("this");
    }

    public ThisObjInfo (Class type)
    {
        super ("this");
        this.type = type;
    }

    /* (non-Javadoc)
     * @see daikon.chicory.DaikonVariableInfo#getChildValue(java.lang.Object)
     */
    public Object getMyValFromParentVal(Object val)
    {
        return null;
    }

    /** 'this' is a top level variable **/
    public VarInfo.VarKind get_var_kind() {
        return VarInfo.VarKind.VARIABLE;
    }

    /** Add IS_PARM to list of variable flags **/
    public EnumSet<VarFlags> get_var_flags() {
      System.out.printf ("%s is a parameter%n", this);
      EnumSet<VarFlags> var_flags = super.get_var_flags().clone();
      var_flags.add (VarFlags.IS_PARAM);
      return (var_flags);
    }

}
