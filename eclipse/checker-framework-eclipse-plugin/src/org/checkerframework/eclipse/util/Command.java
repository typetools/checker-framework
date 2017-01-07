package org.checkerframework.eclipse.util;

import java.io.*;

// AK: XXX This code is copied and modified from somewhere. I don't remember from where.
/**
 * The important method in this class is {@code exec(String[])}. It executes its argument and pipes
 * both stdout and stderr to System.out. Each line in the piped output from stdout is prefixed with
 * "OUT>" and the output from stderr is prefixed with "ERR>"
 *
 * <p>Credit: Producer code modified (and augmented) from Michael Daconta's "Java Traps" column
 * ("When Runtime.exec() won't"), found at
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html
 */
public class Command {

    private static void redirect(InputStream in, OutputStream out) {
        byte[] buffer = new byte[1028];
        int len = 0;

        try {
            while ((len = in.read(buffer)) >= 0) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Executes a command and return the error and output result of the process */
    public static String exec(String[] cmd) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        exec(cmd, baos);

        return baos.toString();
    }

    public static int exec(String[] cmd, OutputStream out) {
        int exitVal;
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(cmd);

            // any error message?
            redirect(proc.getErrorStream(), out);
            redirect(proc.getInputStream(), out);

            exitVal = proc.waitFor();
        } catch (Throwable t) {
            throw new RuntimeException(t); // CP improve
        }
        return exitVal;
    }
}
