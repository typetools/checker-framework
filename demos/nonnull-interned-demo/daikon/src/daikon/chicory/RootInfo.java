package daikon.chicory;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import daikon.*;

/**
 *  This is a subtype of DaikonVariableInfo and is used as a
 *  "placeholder" for the root of the tree.  It contains no variable
 *  information other than what is stored in its children.
 */
public class RootInfo extends DaikonVariableInfo
{
    private RootInfo()
    {
        //the root needs no name
        super(null);
    }

    @Override
    public Object getMyValFromParentVal(Object value)
    {
        return null;
    }

    /**
     * Creates a RootInfo object for a method entry program point.
     */
    public static RootInfo enter_process (MethodInfo mi, int depth)
    {
        RootInfo root = new RootInfo();

        // debug_vars = mi.toString().contains ("Infer.instance");
        if (debug_vars)
            System.out.printf ("building enter tree for %s%n", mi);

        // Don't build a tree for class initializers.
        if (mi.is_class_init())
            return (root);

        // Clear the set of static variables
        ppt_statics.clear();

        // Print class variables.   Print class variables first because
        // the depth goes deeper there ('this' is not counted).  This
        // guarantees that any static variables in the class are found here
        // and not below.
        if (!(mi.member instanceof Constructor)) {
            root.addClassVars(mi.class_info,
                              Modifier.isStatic(mi.member.getModifiers()),
                              mi.member.getDeclaringClass(), /*offset = */ "",
                              depth);
        }

        // Print each parameter
        root.addParameters(mi.class_info, mi.member,
                           Arrays.asList(mi.arg_names), /*offset = */ "",
                           depth);

        return root;
    }

    /**
     * Creates a RootInfo object for a method exit program point.
     */
    public static RootInfo exit_process(MethodInfo mi, int depth)
    {
        RootInfo root = new RootInfo();

        // debug_vars = mi.toString().contains ("Infer.instance");
        if (debug_vars)
            System.out.printf ("building exit tree for %s%n", mi);

        // Don't build a tree for class initializers.
        if (mi.is_class_init())
            return (root);

        // Clear the set of static variables
        ppt_statics.clear();

        // Print class variables.   Print class variables first because
        // the depth goes deeper there ('this' is not counted).  This
        // guarantees that any static variables in the class are found here
        // and not below.
        root.addClassVars(mi.class_info,
                Modifier.isStatic(mi.member.getModifiers()), mi.member
                        .getDeclaringClass(), "", depth);

        // Print arguments
        root.addParameters(mi.class_info, mi.member,
                           Arrays.asList(mi.arg_names), /*offset = */ "",
                           depth);

        // Print return type information for methods only and not constructors
        if (mi.member instanceof Method)
        {
            Class returnType = ((Method) mi.member).getReturnType();
            if (!(returnType.equals(Void.TYPE)))
            {
                // add a new ReturnInfo object to the traversal tree
                DaikonVariableInfo retInfo = new ReturnInfo();

                retInfo.typeName = stdClassName(returnType);
                retInfo.repTypeName = getRepName(returnType, false);
                root.addChild(retInfo);

                retInfo.checkForDerivedVariables(returnType, "return", "");

                retInfo.addChildNodes(mi.class_info, returnType, "return", "",
                        depth);
            }
        }

        return root;
    }

    /**
     * Creates a RootInfo object for an object program point.
     * This will include the class' fields and the "this" object.
     */
    public static RootInfo getObjectPpt(ClassInfo cinfo, int depth)
    {
        RootInfo root = new RootInfo();

        // Clear the set of static variables
        ppt_statics.clear();

        root.addClassVars(cinfo, /*dontPrintInstanceVars = */ false,
                cinfo.clazz, /*offset = */ "", depth);

        return root;
    }

    /**
     * Creates a RootInfo object for a class program point.
     * This will just include static fields.
     */
    public static RootInfo getClassPpt(ClassInfo cinfo, int depth)
    {
        RootInfo root = new RootInfo();

        // Clear the set of static variables
        ppt_statics.clear();

        root.addClassVars(cinfo, /*dontPrintInstanceVars = */ true,
                cinfo.clazz, /*offset = */ "", depth);

        return root;
    }

    public VarInfo.VarKind get_var_kind() {
        throw new RuntimeException ("No var-kind for RootInfo");
    }

}
