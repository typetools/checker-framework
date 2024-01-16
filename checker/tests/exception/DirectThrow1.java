// @below-java11-jdk-skip-test

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/** Test case for rule ERR01-J: "Check direcly thrown exceptions" */
public class DirectThrow1 {

  // :: warning: (warning.file_not_found)
  public static void main(String[] args) throws FileNotFoundException {
    // Linux stores a user's home directory path in
    // the environment variable $HOME, Windows in %APPDATA%
    FileInputStream fis = new FileInputStream(System.getenv("APPDATA") + args[0]);
  }
}
