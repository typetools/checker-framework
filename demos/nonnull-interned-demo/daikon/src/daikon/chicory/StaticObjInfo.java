package daikon.chicory;

import java.util.*;

import daikon.*;
import static daikon.VarInfo.VarFlags;

/**
 * The StaticObjInfo class is a subtype of DaikonVariableInfo used as
 * a root for static variables within a class (which are the only
 * variables visible to static methods).  Nothing is printed for this
 * variable in either the decl or dtrace file, it exists only so that the
 * static variables of a class can be nested within it and not
 * directly under the root.
 */
public class StaticObjInfo extends DaikonVariableInfo
{
    public Class type;

    public StaticObjInfo()
    {
        super("this");
    }

    public StaticObjInfo (Class type)
    {
        super ("this");
        this.type = type;
        typeName = type.getName();
        repTypeName = getRepName (type, false);
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

}
