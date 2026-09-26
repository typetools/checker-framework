import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Reproducer for socket allocation code that falls back through Throwable-based recovery
 * while still closing any sockets that were successfully created.
 */
class SocketUtilThrowableFallback {

  public static Integer[] findUnusedLocalPorts(final int ports) {
    Throwable firstFoundExc = null;
    final @OwningCollection List<ServerSocket> socket = new ArrayList<ServerSocket>();
    final List<Integer> portsFound = new ArrayList<Integer>();
    try {
      try {
        for (int i = 0; i < ports; i++) {
          ServerSocket s = new ServerSocket(0);
          socket.add(s);
          int localPort = s.getLocalPort();
          checkValidPort(localPort);
          portsFound.add(localPort);
        }
      } catch (Throwable e) {
        firstFoundExc = e;
        final Set<Integer> searched = new HashSet<Integer>();
        try {
          for (int i = 0; i < ports && portsFound.size() < ports; i++) {
            int localPort = findUnusedLocalPort(20000, 65535, searched);
            checkValidPort(localPort);
            portsFound.add(localPort);
          }
        } catch (Exception e1) {
          Log.log(e1);
        }
      } finally {
        for (ServerSocket s : socket) {
          if (s != null) {
            try {
              s.close();
            } catch (Exception e) {
            }
          }
        }
      }
      if (portsFound.size() != ports) {
        throw firstFoundExc;
      }
    } catch (Throwable e) {
      String message = "Unable to find an unused local port (is there an enabled firewall?)";
      throw new RuntimeException(message, e);
    }
    return portsFound.toArray(new Integer[portsFound.size()]);
  }

  public static void checkValidPort(int port) throws IOException {
    throw new java.lang.Error();
  }

  private static int findUnusedLocalPort(int searchFrom, int searchTo, Set<Integer> searched) {
    throw new java.lang.Error();
  }

  public class Log {

    public static CoreException log(Throwable e) {
      throw new java.lang.Error();
    }
  }

  public class CoreException extends java.lang.RuntimeException {}
}
