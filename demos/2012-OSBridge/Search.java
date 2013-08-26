import java.util.regex.*;

public class Search {
    public static void main(String[] args) {
        String regex = args[0];
        String content = args[1];
        
        Pattern pat = Pattern.compile(regex);
        Matcher mat = pat.matcher(content);
        
        if (mat.matches()) {
        	System.out.println("Group: " + mat.group(4));
        } else {
        	System.out.println("No match!");
        }
    }
}
