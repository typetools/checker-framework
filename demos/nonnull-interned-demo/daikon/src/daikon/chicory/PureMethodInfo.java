package daikon.chicory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import daikon.*;

/**
 * The PureMethodInfo class is a subtype of DaikonVariableInfo used
 * for "variable types" which correspond to the values of pure method
 * invocations.
 */
public class PureMethodInfo extends DaikonVariableInfo
{

    /** The MethodInfo object for this pure method **/
    private MethodInfo minfo;

    public PureMethodInfo(String name, MethodInfo methInfo, boolean inArray)
    {
        super(name, inArray);

        assert methInfo.isPure() : "Method " + methInfo + " is not pure";

        minfo = methInfo;
    }

    /**
     * Invokes this pure method on the given parentVal
     * This is safe because the method is pure!
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object getMyValFromParentVal(Object parentVal)
    {
        Method meth = (Method) minfo.member;
        boolean changedAccess = false;
        Object retVal;

        // we want to access all fields...
        if (!meth.isAccessible())
        {
            changedAccess = true;
            meth.setAccessible(true);
        }

        if (isArray)
        {
            // First check if parentVal is null or nonsensical
            if (parentVal == null || parentVal instanceof NonsensicalList)
            {
                retVal = NonsensicalList.getInstance();
            }
            else
            {
                List<Object> retList = new ArrayList<Object>();

                for (Object val : (List<Object>) parentVal) // unchecked cast
                {
                    if (val == null || val instanceof NonsensicalObject)
                        retList.add(NonsensicalObject.getInstance());
                    else
                        retList.add(executePureMethod(meth, val));
                }

                retVal = retList;
            }
        }
        else
        {
            // First check if parentVal is null or nonsensical
            if (parentVal == null || parentVal instanceof NonsensicalObject)
            {
                retVal = NonsensicalObject.getInstance();
            }
            else
            {
                retVal = executePureMethod(meth, parentVal);
            }

        }

        if (changedAccess)
        {
            meth.setAccessible(false);
        }

        return retVal;
    }

    private static Object executePureMethod(Method meth, Object objectVal)
    {
        Object retVal = null;
        try
        {
            // TODO is this the best way to handle this problem?
            // (when we invoke a pure method, Runtime.Enter should not be
            // called)
            Runtime.startPure();

            retVal = meth.invoke(objectVal);

            if (meth.getReturnType().isPrimitive())
                retVal = convertWrapper(retVal);
        }
        catch (IllegalArgumentException e)
        {
            throw new Error(e);
        }
        catch (IllegalAccessException e)
        {
            throw new Error(e);
        }
        catch (InvocationTargetException e)
        {
            retVal = NonsensicalObject.getInstance();
        }
        catch (Throwable e)
        {
            throw new Error(e);
        }
        finally
        {
            Runtime.endPure();
        }

        return retVal;
    }


    /**
     * Convert standard wrapped Objects (i.e., Integers) to Chicory wrappers (ie,
     * Runtime.IntWrap)\ Should not be called if the Object was not auto-boxed
     * from from a primitive!
     */
    public static Object convertWrapper(Object obj)
    {
        if (obj == null || obj instanceof NonsensicalObject || obj instanceof NonsensicalList)
            return obj;

        if (obj instanceof Integer)
        {
            return new Runtime.IntWrap((Integer) obj);
        }
        else if (obj instanceof Boolean)
        {
            return new Runtime.BooleanWrap((Boolean) obj);
        }
        else if (obj instanceof Byte)
        {
            return new Runtime.ByteWrap((Byte) obj);
        }
        else if (obj instanceof Character)
        {
            return new Runtime.CharWrap((Character) obj);
        }
        else if (obj instanceof Float)
        {
            return new Runtime.FloatWrap((Float) obj);
        }
        else if (obj instanceof Double)
        {
            return new Runtime.DoubleWrap((Double) obj);
        }
        else if (obj instanceof Long)
        {
            return new Runtime.LongWrap((Long) obj);
        }
        else if (obj instanceof Short)
        {
            return new Runtime.ShortWrap((Short) obj);
        }
        else
        {
            // Not a primitive object (wrapper), so just keep it the same
            return obj;
        }

    }

    public VarInfo.VarKind get_var_kind() {
        return VarInfo.VarKind.FUNCTION;
    }
}
