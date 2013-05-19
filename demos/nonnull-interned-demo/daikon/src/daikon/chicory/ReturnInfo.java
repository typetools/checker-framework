package daikon.chicory;

import daikon.*;

/**
 * A subtype of DaikonVariableInfo used for variables that are
 * returned from procedures.
 */
public class ReturnInfo extends DaikonVariableInfo
{
    Class return_type = null;

    public ReturnInfo()
    {
        super("return");
    }

    public ReturnInfo (Class return_type)
    {
        super("return");
        this.return_type = return_type;
    }

    public Object getMyValFromParentVal(Object value)
    {
        throw new RuntimeException("Don't call getMyValFromParentVal on ReturnInfo objects");
    }

    public VarInfo.VarKind get_var_kind() {
        return VarInfo.VarKind.RETURN;
    }
}
