package org.checkerframework.eclipse.ui;

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.actions.CheckerInfo;
import org.checkerframework.eclipse.prefs.CheckerPreferences;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

public class CustomCheckersMenu extends ContributionItem {

    public CustomCheckersMenu() {}

    public CustomCheckersMenu(String id) {
        super(id);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public void fill(Menu menu, int index) {
        String customClasses =
                CheckerPlugin.getDefault()
                        .getPreferenceStore()
                        .getString(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASSES);

        // Here you could get selection and decide what to do
        // You can also simply return if you do not want to show a menu

        // create the menu item
        MenuItem menuItem = new MenuItem(menu, SWT.CASCADE, index);
        menuItem.setText("Run Custom Checker");

        // Menu menu = new Menu(menu, SWT.)
        /*menuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                // what to do when menu is subsequently selected.
                System.err.println("Dynamic menu selected");
            }
        });*/

        if (customClasses == null || customClasses.equals("")) {
            menuItem.setEnabled(false);
        } else {
            Menu checkersMenu = new Menu(menuItem);
            final String[] customCheckers = customClasses.split(",");
            for (int i = 0; i < customCheckers.length; i++) {
                // final String text = customCheckers[i];
                final CheckerInfo checkerInfo = CheckerInfo.fromClassPath(customCheckers[i], null);
                MenuItem runCustomChecker = new MenuItem(checkersMenu, SWT.CHECK, i);
                runCustomChecker.setText(checkerInfo.getLabel());
                runCustomChecker.addSelectionListener(
                        new SelectionAdapter() {
                            public void widgetSelected(SelectionEvent e) {
                                final IHandlerService handlerService =
                                        (IHandlerService)
                                                PlatformUI.getWorkbench()
                                                        .getService(IHandlerService.class);
                                try {
                                    ICommandService service =
                                            (ICommandService)
                                                    PlatformUI.getWorkbench()
                                                            .getService(ICommandService.class);
                                    Command command =
                                            service.getCommand("checkers.eclipse.singlecustom");

                                    final Map<String, Object> params =
                                            new HashMap<String, Object>();
                                    params.put(
                                            "checker-framework-eclipse-plugin.checker",
                                            checkerInfo.getClassPath());
                                    final ParameterizedCommand pCmd =
                                            ParameterizedCommand.generateCommand(command, params);

                                    handlerService.executeCommand(pCmd, null);

                                    // handlerService.executeCommand("checkers.eclipse.runnullness",
                                    // new Event() );
                                } catch (Exception e1) {
                                    throw new RuntimeException(e1);
                                }

                                // what to do when menu is subsequently selected.
                                System.err.println("Dynamic menu selected");
                            }
                        });
            }
            menuItem.setMenu(checkersMenu);
        }
    }
}
