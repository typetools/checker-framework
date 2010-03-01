package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class SecurityManager{
  public boolean getInCheck() { throw new RuntimeException("skeleton method"); }
  public SecurityManager() { throw new RuntimeException("skeleton method"); }
  public Object getSecurityContext() { throw new RuntimeException("skeleton method"); }
  public void checkPermission(java.security.Permission a1) { throw new RuntimeException("skeleton method"); }
  public void checkPermission(java.security.Permission a1, Object a2) { throw new RuntimeException("skeleton method"); }
  public void checkCreateClassLoader() { throw new RuntimeException("skeleton method"); }
  public void checkAccess(Thread a1) { throw new RuntimeException("skeleton method"); }
  public void checkAccess(ThreadGroup a1) { throw new RuntimeException("skeleton method"); }
  public void checkExit(int a1) { throw new RuntimeException("skeleton method"); }
  public void checkExec(String a1) { throw new RuntimeException("skeleton method"); }
  public void checkLink(String a1) { throw new RuntimeException("skeleton method"); }
  public void checkRead(java.io.FileDescriptor a1) { throw new RuntimeException("skeleton method"); }
  public void checkRead(String a1) { throw new RuntimeException("skeleton method"); }
  public void checkRead(String a1, Object a2) { throw new RuntimeException("skeleton method"); }
  public void checkWrite(java.io.FileDescriptor a1) { throw new RuntimeException("skeleton method"); }
  public void checkWrite(String a1) { throw new RuntimeException("skeleton method"); }
  public void checkDelete(String a1) { throw new RuntimeException("skeleton method"); }
  public void checkConnect(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void checkConnect(String a1, int a2, Object a3) { throw new RuntimeException("skeleton method"); }
  public void checkListen(int a1) { throw new RuntimeException("skeleton method"); }
  public void checkAccept(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void checkMulticast(java.net.InetAddress a1) { throw new RuntimeException("skeleton method"); }
  public void checkMulticast(java.net.InetAddress a1, byte a2) { throw new RuntimeException("skeleton method"); }
  public void checkPropertiesAccess() { throw new RuntimeException("skeleton method"); }
  public void checkPropertyAccess(String a1) { throw new RuntimeException("skeleton method"); }
  public boolean checkTopLevelWindow(Object a1) { throw new RuntimeException("skeleton method"); }
  public void checkPrintJobAccess() { throw new RuntimeException("skeleton method"); }
  public void checkSystemClipboardAccess() { throw new RuntimeException("skeleton method"); }
  public void checkAwtEventQueueAccess() { throw new RuntimeException("skeleton method"); }
  public void checkPackageAccess(String a1) { throw new RuntimeException("skeleton method"); }
  public void checkPackageDefinition(String a1) { throw new RuntimeException("skeleton method"); }
  public void checkSetFactory() { throw new RuntimeException("skeleton method"); }
  public void checkMemberAccess(Class<?> a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void checkSecurityAccess(String a1) { throw new RuntimeException("skeleton method"); }
  public ThreadGroup getThreadGroup() { throw new RuntimeException("skeleton method"); }
}
