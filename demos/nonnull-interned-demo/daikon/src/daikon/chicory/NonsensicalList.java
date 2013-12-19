package daikon.chicory;

import java.util.*;

/**
 * NonsensicalList is similar to NonsensicalObject but it is used for
 * arrays whose value is nonsensical.
 */
// It's problematic to make this generic:  what would "get" return?
public class NonsensicalList extends AbstractList<Object>
  implements List<Object> {

    /**
     *
     */
    private NonsensicalList()
    {
        super();
    }

    public static NonsensicalList getInstance()
    {
        return theList;
    }

    public Object get(int index)
    {
        return NonsensicalObject.getInstance();
    }

    public int size()
    {
        return -1;
    }

    public String toString()
    {
        return "NonsensicalList";
    }

    public static boolean isNonsensicalList (Object obj)
    {
        return (obj instanceof NonsensicalList);
    }

    private static final NonsensicalList theList = new NonsensicalList();
}
