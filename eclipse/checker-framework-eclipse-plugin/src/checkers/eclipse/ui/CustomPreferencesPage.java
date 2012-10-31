package checkers.eclipse.ui;

import java.util.ArrayList;
import java.util.List;

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

import checkers.eclipse.CheckerPlugin;
import checkers.eclipse.prefs.CheckerPreferences;
import checkers.eclipse.util.JavaUtils;

@SuppressWarnings("restriction")
public class CustomPreferencesPage extends PreferencePage implements
        IWorkbenchPreferencePage
{
    private Text customClasses;

    /* private Button customClassAuto; */

    public CustomPreferencesPage()
    {
    }

    public CustomPreferencesPage(String title)
    {
        super(title);
    }

    public CustomPreferencesPage(String title, ImageDescriptor image)
    {
        super(title, image);
    }

    @Override
    public void init(IWorkbench workbench)
    {
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore()
    {
        return CheckerPlugin.getDefault().getPreferenceStore();
    }

    @Override
    protected Control createContents(Composite parent)
    {
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
        customClasses = new Text(customGroup, SWT.SINGLE | SWT.BORDER);

        Button searchButton = new Button(customGroup, SWT.PUSH);
        searchButton.setText("Search...");
        searchButton.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                searchForClass();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });

        /*
         * customClassAuto = new Button(customGroup, SWT.CHECK);
         * customClassAuto.setText("Use custom classes in autobuild?");
         */

        FormData data1 = new FormData();
        data1.left = new FormAttachment(0, 5);
        data1.top = new FormAttachment(classesLabel, 5);
        data1.right = new FormAttachment(searchButton, -5);
        customClasses.setLayoutData(data1);

        FormData data2 = new FormData();
        data2.top = new FormAttachment(classesLabel, 5);
        data2.right = new FormAttachment(100, -5);
        searchButton.setLayoutData(data2);

        /*
         * FormData data3 = new FormData(); data3.top = new
         * FormAttachment(customClasses, 5);
         * customClassAuto.setLayoutData(data3);
         */

        initValues();

        return prefComposite;
    }

    private void searchForClass()
    {
        OpenTypeSelectionDialog dialog = new OpenTypeSelectionDialog(
                getShell(), true, null, null, IJavaSearchConstants.CLASS);
        dialog.setTitle("Search for Checker Classes");
        dialog.setMessage("Select additional Checkers to use.");

        if (dialog.open() == SelectionDialog.OK)
        {
            Object[] results = dialog.getResult();
            List<String> classNames = new ArrayList<String>();

            for (Object result : results)
            {
                if (result instanceof IType)
                {
                    IType type = (IType) result;
                    classNames.add(type.getFullyQualifiedName());
                }
            }

            customClasses.setText(JavaUtils.join(",", classNames));
        }
    }

    /**
     * Initialise the values in the table to the preference values
     */
    private void initValues()
    {
        IPreferenceStore store = doGetPreferenceStore();

        customClasses.setText(store
                .getString(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASSES));
        /*
         * customClassAuto .setSelection(store
         * .getBoolean(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASS_AUTOBUILD));
         */
    }

    public boolean performOk()
    {
        IPreferenceStore store = doGetPreferenceStore();

        store.setValue(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASSES,
                customClasses.getText());

        /*
         * store.setValue(CheckerPreferences.PREF_CHECKER_CUSTOM_CLASS_AUTOBUILD,
         * customClassAuto.getSelection());
         */

        return true;
    }
}