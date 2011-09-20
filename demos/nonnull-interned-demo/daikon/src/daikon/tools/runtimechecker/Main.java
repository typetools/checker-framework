package daikon.tools.runtimechecker;

import java.util.ArrayList;
import java.util.List;

/**
 * Main entrypoint for the instrumenter.
 * Passes control to whichever handler can handle the user-specified command.
 */
public class Main extends CommandHandler {

    protected void usageMessage(List<CommandHandler> handlers) {
        for (CommandHandler h : handlers) {
            h.usageMessage();
        }
    }

    public void nonStaticMain(String[] args) {

        List<CommandHandler> handlers = new ArrayList<CommandHandler>();
        handlers.add(new InstrumentHandler());

        if (args.length < 1) {
            System.err.println("ERROR:  No command given.");
            System.err.println("For more help, invoke the instrumenter with \"help\" as its sole argument.");
            System.exit(1);
        }
        if (args[0].toUpperCase().equals("HELP") || args[0].equals("?")) {
            usageMessage();
            usageMessage(handlers);
            System.exit(0);
        }

        String command = args[0];

        boolean success = false;

        CommandHandler h = null;
        try {

            for (int i = 0 ; i < handlers.size() ; i++) {
                h = handlers.get(i);
                if (h.handles(command)) {
                    success = h.handle(args);
                    if (!success) {
                        System.err
                            .println("The command you issued returned a failing status flag.");
                    }
                    break;
                }
            }

        } catch (Throwable e) {
            System.out.println("Throwable thrown while handling command:" + e);
            e.printStackTrace();
            success = false;
        } finally {
            if (!success) {
                System.err.println("The instrumenter failed.");
                h.usageMessage();
                System.exit(1);
            } else {
            }
        }
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.nonStaticMain(args);
    }

}
