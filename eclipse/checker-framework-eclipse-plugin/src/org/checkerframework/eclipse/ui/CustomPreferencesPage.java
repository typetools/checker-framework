package org.checkerframework.eclipse.ui;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.prefs.CheckerPreferences;
import org.checkerframework.eclipse.util.PluginUtil;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.SelectionDialog;

@SuppressWarnings("restriction")
public class CustomPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {
    private Text customClasses;
    private org.eclipse.swt.widgets.List customCheckers;

    /* private Button customClassAuto; */

    public CustomPreferencesPage() {}

    public CustomPreferencesPage(String title) {
        super(title);
    }

    public CustomPreferencesPage(String title, ImageDescriptor image) {
        super(title, image);
    }

    @Override
    public void init(IWorkbench workbench) {}

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return CheckerPlugin.getDefault().getPreferenceStore();
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite prefComposite = new Composite(parent, SWT.None);
        GridLayout layout = new GridLayout();
        prefComposite.setLayout(layout);

        Group customGroup = new Group(prefComposite, SWT.None);
        customGroup.setText("Custom checkers");
        FormLayout customLayout = new FormLayout();
        customLayout.marginWidth = customLayout.marginHeight = 5;
        customGroup.setLayout(customLayout);

        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        customGroup.setLayoutData(data);

        Label classesLabel = new Label(customGroup, SWT.None);
        classesLabel.setText("Additional checker classes to use:");

        Button addButton = new Button(customGroup, SWT.PUSH);
        addButton.setText("Add");
        addButton.addSelectionListener(
                new SelectionListener() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        searchForClass();
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {}
                });

        Button removeButton = new Button(customGroup, SWT.PUSH);
        removeButton.setText("Remove");
        removeButton.addSelectionListener(
                new SelectionListener() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        customCheckers.remove(customCheckers.getSelectionIndices());
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {}
                });

        customCheckers = new org.eclipse.swt.widgets.List(customGroup, SWT.MULTI);

        /*
         * customClassAuto = new Button(customGroup, SWT.CHECK);
         * customClassAuto.setText("Use custom classes in autobuild?");
         */

        FormData listFd = new FormData();
        listFd.left = new FormAttachment(0, 5);
        listFd.top = new FormAttachment(classesLabel, 5);
        listFd.right = new FormAttachment(removeButton, -5);
        listFd.bottom = new FormAttachment(100, -5);
        customCheckers.setLayoutData(listFd);

        FormData addFd = new FormData();
        addFd.top = new FormAttachment(classesLabel, 5);
        addFd.right = new FormAttachment(100, -5);
        addFd.left = new FormAttachment(customCheckers, 5);
        addButton.setLayoutData(addFd);

        FormData removeFd = new FormData();
        removeFd.top = new FormAttachment(addButton, 5);
        removeFd.right = new FormAttachment(100, -5);
        removeButton.setLayoutData(removeFd);

        /*
         * FormData data3 = new FormData(); data3.top = new
         * FormAttachment(customClasses, 5);
         * customClassAuto.setLayoutData(data3);
         */

        initValues();

        return prefComposite;
    }

    private void searchForClass() {
        OpenTypeSelectionDialog dialog =
                new OpenTypeSelectionDialog(
                        getShell(), true, null, null, IJavaSearchConstants.CLASS);
        dialog.setTitle("Search for checker classes");
        dialog.setMessage("Select additional checkers to use.");

        if (dialog.open() == SelectionDialog.OK) {
            Object[] results = dialog.getResult();
            List<String> classNames = new ArrayList<String>();

            for (Object result : results) {
                if (result instanceof IType) {
                    IType type = (IType) result;
                    classNames.add(type.getFullyQualifiedName());
                }
            }

            for (final String cn : classNames) {
                if (!contains(cn)) { // TODO: ADD A DIALOG TO WARN IF ALREADY CONTAINED
                    customCheckers.add(cn);
                }
            }
        }
    }

    private boolean contains(final String className) {
        for (final String str : customCheckers.getItems()) {
            if (str.equals(className)) {
                return true;
            }
        }

        return false;
    }

    /** Initialize the values in the table to the preference values */
    private void initValues() {
        IPreferenceStore store = doGetPreferenceStore();
        final String storedItems = store.getString(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASSES);

        if (!storedItems.equals("")) {
            customCheckers.setItems(storedItems.split(","));
        }
        /*
         * customClassAuto .setSelection(store
         * .getBoolean(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASS_AUTOBUILD));
         */
    }

    public boolean performOk() {
        IPreferenceStore store = doGetPreferenceStore();

        store.setValue(
                CheckerPreferences.PREF_CHECKER_CUSTOM_CLASSES,
                PluginUtil.join(",", customCheckers.getItems()));

        /*
         * store.setValue(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASS_AUTOBUILD,
         * customClassAuto.getSelection());
         */

        return true;
    }
}
