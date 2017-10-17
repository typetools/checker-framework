// Test case for issue #168: https://github.com/kelloggm/checker-framework/issues/168

public class EndsWith2 {

    public static String invertBrackets(String classname) {

        // Get the array depth (if any)
        int array_depth = 0;
        String brackets = "";
        while (classname.endsWith("[]")) {
            brackets = brackets + classname.substring(classname.length() - 2);
            classname = classname.substring(0, classname.length() - 2);
            array_depth++;
        }
        return brackets + classname;
    }
}
