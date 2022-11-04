// Based on a crash encountered when runnnig WPI on plume-lib's RequireJavadoc.

import org.plumelib.options.Options;

public class RequireJavadocCrash {

    public static void main(String[] args) {
        RequireJavadocCrash rj = new RequireJavadocCrash();
        Options options =
                new Options(
                        "java org.plumelib.javadoc.RequireJavadoc [options] [directory-or-file ...]",
                        rj);
        String[] remainingArgs = options.parse(true, args);

        rj.setJavaFiles(remainingArgs);
    }

    private RequireJavadocCrash() {}

    private void setJavaFiles(String[] args) {}
}
