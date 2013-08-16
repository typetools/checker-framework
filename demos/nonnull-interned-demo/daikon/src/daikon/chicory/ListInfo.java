/*
 * Created on May 3, 2005
 *
 */
package daikon.chicory;

import java.lang.reflect.*;
import java.util.List;

import daikon.*;

/**
 * The ListInfo class is a subtype of DaikonVariableInfo used for
 * variable types which implement java.util.List
 */
public class ListInfo extends DaikonVariableInfo
{

    private Class<? extends List> listType;

    public ListInfo(String theName, Class<? extends List> theType)
    {
        super(theName, true);
        listType = theType;
    }

    //use the "toArray" method to get an array
    //convert the array to a List
    public Object getMyValFromParentVal(Object value)
    {

        Method arrayMethod = null;
        try
        {
            arrayMethod = listType.getMethod("toArray", new Class[0]);
        }
        catch (NoSuchMethodException e)
        {
            throw new Error(listType.getName() + " seems to implement java.util.List, but method toArray() not found");
        }

        Object arrayVal = null;

        if (value != null && !(value instanceof NonsensicalObject))
        {

            //TODO why can't we just cast to List and call toArray directly?

            try
            {
                arrayVal = arrayMethod.invoke(value, new Object[0]);
            }
            catch (IllegalArgumentException e1)
            {
                throw new Error(e1);
            }
            catch (IllegalAccessException e1)
            {
                throw new Error(e1);
            }
            catch (InvocationTargetException e1)
            {
                throw new Error(e1);
            }
        }
        else
            arrayVal = NonsensicalObject.getInstance();


        return DTraceWriter.getListFromArray(arrayVal);
    }

    /** Lists are arrays from Daikon's point of view **/
    public VarInfo.VarKind get_var_kind() {
        return VarInfo.VarKind.ARRAY;
    }
}
