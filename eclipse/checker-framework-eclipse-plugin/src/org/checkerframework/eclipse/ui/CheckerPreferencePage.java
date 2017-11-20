package org.checkerframework.eclipse.ui;

import static org.checkerframework.eclipse.ui.CheckerPreferencePage.PT_COLUMNS.*;

import java.util.*;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.actions.CheckerInfo;
import org.checkerframework.eclipse.actions.CheckerManager;
import org.checkerframework.eclipse.prefs.CheckerPreferences;
import org.checkerframework.eclipse.prefs.OptionLine;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

public class CheckerPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    private static final String BUILT_IN_LABEL = "Built-In";
    private static final String CUSTOM_LABEL = "Custom";

    protected enum PT_COLUMNS {
        LABEL,
        SOURCE,
        CLASSES
    }

    private Table procTable;
    private Table optTable;
    private Text argText;
    private Text optSkipUses;
    private Text optALint;
    private Text optFilter;
    private Button optVerbose;

    private Text optJDKPath;
    // private Button optAutoBuild;
    private Button optWarning;
    private Button optFilenames;
    private Button optNoMsgText;
    private Button optShowChecks;
    private Button optImplicitImports;

    @Override
    public void init(IWorkbench workbench) {}

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return CheckerPlugin.getDefault().getPreferenceStore();
    }

    @Override
    protected Control createContents(Composite parent) {
        // Layout for pref page
        final Composite tableComposite = new Composite(parent, SWT.None);
        final GridLayout layout = new GridLayout();
        tableComposite.setLayout(layout);

        makeProcessorGroup(tableComposite);

        // UI/Eclipse options
        final Group uiGroup = new Group(tableComposite, SWT.None);
        uiGroup.setText("Eclipse options");

        final FillLayout uiLayout = new FillLayout(SWT.VERTICAL);
        uiLayout.marginWidth = uiLayout.marginHeight = 5;
        uiGroup.setLayout(uiLayout);

        // optAutoBuild = new Button(uiGroup, SWT.CHECK);
        // optAutoBuild.setText("Automatically run type-checkers");

        final Label filterLabel = new Label(uiGroup, SWT.None);
        filterLabel.setText("Regex for warning/error filter:");
        optFilter = new Text(uiGroup, SWT.SINGLE | SWT.BORDER);

        final GridData uiGridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        uiGroup.setLayoutData(uiGridData);

        optVerbose = new Button(uiGroup, SWT.CHECK);
        optVerbose.setText("Show verbose output");

        // JDK options
        final Group jdkGroup = new Group(tableComposite, SWT.None);
        jdkGroup.setText("JDK options");

        final FormLayout jdkLayout = new FormLayout();
        jdkLayout.marginWidth = jdkLayout.marginHeight = 5;
        jdkGroup.setLayout(jdkLayout);

        final Label jdkFolderLabel = new Label(jdkGroup, SWT.None);
        jdkFolderLabel.setText("Java Home Directory:");
        optJDKPath = new Text(jdkGroup, SWT.SINGLE | SWT.BORDER);

        final Button browseButton = new Button(jdkGroup, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addSelectionListener(
                new SelectionListener() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DirectoryDialog dirDialog =
                                new DirectoryDialog(
                                        PlatformUI.getWorkbench()
                                                .getActiveWorkbenchWindow()
                                                .getShell(),
                                        SWT.OPEN);
                        String path = dirDialog.open();
                        optJDKPath.setText(path);
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {}
                });

        final FormData jdkFormData1 = new FormData();
        jdkFormData1.left = new FormAttachment(0, 5);
        jdkFormData1.right = new FormAttachment(100, 0);
        jdkFolderLabel.setLayoutData(jdkFormData1);

        final FormData jdkFormData2 = new FormData();
        jdkFormData2.top = new FormAttachment(jdkFolderLabel, 5);
        jdkFormData2.left = new FormAttachment(0, 5);
        jdkFormData2.right = new FormAttachment(80, -5);
        optJDKPath.setLayoutData(jdkFormData2);

        final FormData jdkFormData3 = new FormData();
        jdkFormData3.top = new FormAttachment(jdkFolderLabel, 5);
        jdkFormData3.left = new FormAttachment(optJDKPath, 5);
        browseButton.setLayoutData(jdkFormData3);

        final GridData jdkGridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        jdkGridData.widthHint = 300;
        jdkGroup.setLayoutData(jdkGridData);

        // Processor options
        final Group procGroup = new Group(tableComposite, SWT.None);
        procGroup.setText("Processor/build options");
        FillLayout procLayout = new FillLayout(SWT.VERTICAL);
        procLayout.marginWidth = procLayout.marginHeight = 5;
        procGroup.setLayout(procLayout);

        final Label skipLabel = new Label(procGroup, SWT.None);
        skipLabel.setText("Classes to skip (-AskipUses):");
        optSkipUses = new Text(procGroup, SWT.SINGLE | SWT.BORDER);
        optSkipUses.setToolTipText("Classes to skip during type-checking (-AskipUses)");

        final Label lintLabel = new Label(procGroup, SWT.None);
        lintLabel.setText("Lint options:");
        optALint = new Text(procGroup, SWT.SINGLE | SWT.BORDER);
        optALint.setToolTipText("Enable or disable optional checks (-Alint)");
        optWarning = new Button(procGroup, SWT.CHECK);
        optWarning.setText("Show errors as warnings (-Awarns)");
        optFilenames = new Button(procGroup, SWT.CHECK);
        optFilenames.setText("Print the name of each file (-Afilenames)");
        optNoMsgText = new Button(procGroup, SWT.CHECK);
        optNoMsgText.setText("Use message keys instead of text (-Anomsgtext)");
        optShowChecks = new Button(procGroup, SWT.CHECK);
        optShowChecks.setText("Print debugging info for pseudo-checks (-Ashowchecks)");
        optImplicitImports = new Button(procGroup, SWT.CHECK);
        optImplicitImports.setText("Use implicit imports for annotation classes");

        GridData procGridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        procGroup.setLayoutData(procGridData);

        // argText = new Text(javacGroup, SWT.SINGLE | SWT.BORDER);
        makeCompilerParameters(tableComposite);

        initValues();

        return tableComposite;
    }

    private void makeProcessorGroup(final Composite tableComposite) {

        final Group group = new Group(tableComposite, SWT.None);
        group.setText("Checkers");

        // Layout info for tableComposite's layout
        GridData groupGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        group.setLayoutData(groupGridData);

        // group's layout
        final GridLayout layout = new GridLayout();
        layout.numColumns = 2;

        group.setLayout(layout);

        // Make Processor table
        procTable = new Table(group, SWT.CHECK | SWT.MULTI | SWT.BORDER);

        // layout data within the group
        final GridData procTableData = new GridData(SWT.FILL, SWT.TOP, true, false);
        procTableData.heightHint = 200;
        procTableData.horizontalSpan = 2;
        procTableData.horizontalSpan = SWT.FILL;
        procTableData.grabExcessHorizontalSpace = true;

        procTable.setLayoutData(procTableData);
        procTable.setLinesVisible(true);
        procTable.setHeaderVisible(true);

        final List<String> headers = Arrays.asList("Name", "Source", "Class");
        for (final String header : headers) {
            final TableColumn column = new TableColumn(procTable, SWT.NONE);
            column.setText(header);
        }

        // Add built-in checkers to the table
        for (final CheckerInfo checkerInfo : CheckerManager.getCheckerInfos()) {
            addProcTableItem(checkerInfo, true);
        }

        for (final String className : CheckerManager.getStoredCustomClasses()) {
            addProcTableItem(CheckerInfo.fromClassPath(className, null), false);
        }

        for (final TableColumn columns : procTable.getColumns()) {
            columns.pack();
        }

        // Make the add/remove controls for the table

        final Button addButton = new Button(group, SWT.PUSH);
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

        final Button removeButton = new Button(group, SWT.PUSH);
        removeButton.setText("Remove");
        removeButton.setEnabled(false);
        removeButton.addSelectionListener(
                new SelectionListener() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        removePTIndices(procTable.getSelectionIndices());
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {}
                });

        final GridData removeGd = new GridData();
        removeGd.horizontalAlignment = SWT.END;
        removeButton.setLayoutData(removeGd);

        // enable/disable the remove button (enabled only there are NO
        // built-in checkers enabled and when at least 1 custom checker is
        // selected)

        procTable.addSelectionListener(
                new SelectionListener() {
                    public void widgetSelected(SelectionEvent event) {
                        setRemoveState();
                    }

                    public void widgetDefaultSelected(SelectionEvent event) {
                        setRemoveState();
                    }

                    private void setRemoveState() {
                        final TableItem[] selectedItems = procTable.getSelection();
                        boolean enabled = true;
                        if (selectedItems == null || selectedItems.length == 0) {
                            enabled = false;
                        } else {
                            for (final TableItem ti : selectedItems) {
                                if (!ti.getText(SOURCE.ordinal()).equals(CUSTOM_LABEL)) {
                                    enabled = false;
                                    break;
                                }
                            }
                        }

                        removeButton.setEnabled(enabled);
                    }
                });
    }

    // TODO: Lot's of overlap with procTable, perhaps there is a way to generalize these two
    // TODO: We could also move some of the other built-in options as unremovable options in this
    // table
    public void makeCompilerParameters(final Composite tableComposite) {
        final Group group = new Group(tableComposite, SWT.None);
        group.setText("Additional compiler parameters");

        // Layout info for tableComposite's layout
        GridData groupGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        group.setLayoutData(groupGridData);

        // group's layout
        final GridLayout layout = new GridLayout();
        layout.numColumns = 2;

        group.setLayout(layout);

        // Make Processor table
        optTable = new Table(group, SWT.CHECK | SWT.MULTI | SWT.BORDER);

        // layout data within the group
        final GridData optTableData = new GridData(SWT.FILL, SWT.TOP, true, false);
        optTableData.heightHint = 100;
        optTableData.horizontalSpan = 2;
        optTableData.horizontalSpan = SWT.FILL;
        optTableData.grabExcessHorizontalSpace = true;

        optTable.setLayoutData(optTableData);
        optTable.setLinesVisible(true);
        optTable.setHeaderVisible(true);

        final TableColumn column = new TableColumn(optTable, SWT.NONE);
        column.setText("Option");

        // Add existing op;tions to the table
        for (final OptionLine optionInfo : getMiscParams()) {
            addOptTableItem(optionInfo);
        }

        for (final TableColumn columns : optTable.getColumns()) {
            columns.pack();
        }

        final Button addButton = new Button(group, SWT.PUSH);
        addButton.setText("Add");
        addButton.addSelectionListener(
                new SelectionListener() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        addOption();
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {}
                });

        final Button removeButton = new Button(group, SWT.PUSH);
        removeButton.setText("Remove");
        removeButton.setEnabled(false);
        removeButton.addSelectionListener(
                new SelectionListener() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        removeOptionIndices(optTable.getSelectionIndices());
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {}
                });

        final GridData removeGd = new GridData();
        removeGd.horizontalAlignment = SWT.END;
        removeButton.setLayoutData(removeGd);

        // enable/disable the remove button (enabled only there are NO
        // built-in checkers enabled and when at least 1 custom checker is
        // selected)

        optTable.addSelectionListener(
                new SelectionListener() {
                    public void widgetSelected(SelectionEvent event) {
                        setRemoveState();
                    }

                    public void widgetDefaultSelected(SelectionEvent event) {
                        setRemoveState();
                    }

                    private void setRemoveState() {
                        final TableItem[] selectedItems = optTable.getSelection();
                        removeButton.setEnabled(selectedItems != null || selectedItems.length != 0);
                    }
                });
    }

    private TableItem addOptTableItem(final OptionLine optLine) {
        final TableItem item = new TableItem(optTable, SWT.None);
        item.setText(0, optLine.getArgument());
        item.setChecked(optLine.isActive());
        return item;
    }

    private void removeOptionIndices(final int[] indices) {
        optTable.remove(indices);
    }

    private void removePTIndices(final int[] indices) {
        for (final int index : indices) {
            final TableItem ti = procTable.getItem(index);
            if (!ti.getText(SOURCE.ordinal()).equals(CUSTOM_LABEL)) {
                throw new IllegalArgumentException(
                        "Cannot remove built-in checker " + ti.getText(LABEL.ordinal()));
            }
        }
        procTable.remove(indices);
    }

    public static String[] splitAtUppercase(final String toSplit) {
        List<String> tokens = new ArrayList<String>();

        int length = toSplit.length();

        int start = 0;
        for (int i = 0; i < length; i++) {
            if ((Character.isUpperCase(toSplit.charAt(i)) && i != 0)) {
                tokens.add(toSplit.substring(start, i));
                start = i;
            }
        }

        tokens.add(toSplit.substring(start, length));
        return tokens.toArray(new String[tokens.size()]);
    }

    protected List<OptionLine> getMiscParams() {
        IPreferenceStore store = doGetPreferenceStore();
        return OptionLine.parseOptions(store.getString(CheckerPreferences.PREF_CHECKER_ARGS));
    }

    protected void writeMiscParams() {
        final List<OptionLine> options = new ArrayList<OptionLine>();
        for (TableItem item : optTable.getItems()) {
            options.add(new OptionLine(item.getText(0), item.getChecked()));
        }

        doGetPreferenceStore()
                .setValue(
                        CheckerPreferences.PREF_CHECKER_ARGS,
                        OptionLine.optionLinesToString(options));
    }

    /** Initialise the values in the table to the preference values */
    private void initValues() {
        IPreferenceStore store = doGetPreferenceStore();

        final List<String> selectedClasses =
                new ArrayList<String>(CheckerManager.getSelectedClasses());
        for (TableItem item : procTable.getItems()) {
            int index = 0;
            while (index < selectedClasses.size()) {

                if (item.getText(CLASSES.ordinal()).equals(selectedClasses.get(index))) {
                    item.setChecked(true);
                    selectedClasses.remove(index);
                } else {
                    ++index;
                }
            }
        }

        /*optAutoBuild.setSelection(store
        .getBoolean(CheckerPreferences.PREF_CHECKER_AUTO_BUILD)); */
        optSkipUses.setText(store.getString(CheckerPreferences.PREF_CHECKER_A_SKIP_CLASSES));
        optALint.setText(store.getString(CheckerPreferences.PREF_CHECKER_A_LINT));
        optWarning.setSelection(store.getBoolean(CheckerPreferences.PREF_CHECKER_A_WARNS));
        optFilenames.setSelection(store.getBoolean(CheckerPreferences.PREF_CHECKER_A_FILENAMES));
        optNoMsgText.setSelection(store.getBoolean(CheckerPreferences.PREF_CHECKER_A_NO_MSG_TEXT));
        optShowChecks.setSelection(store.getBoolean(CheckerPreferences.PREF_CHECKER_A_SHOW_CHECKS));
        optFilter.setText(store.getString(CheckerPreferences.PREF_CHECKER_ERROR_FILTER_REGEX));
        optVerbose.setSelection(store.getBoolean(CheckerPreferences.PREF_CHECKER_VERBOSE));
        optJDKPath.setText(store.getString(CheckerPreferences.PREF_CHECKER_JDK_PATH));
        optImplicitImports.setSelection(
                store.getBoolean(CheckerPreferences.PREF_CHECKER_IMPLICIT_IMPORTS));
    }

    public boolean performOk() {
        IPreferenceStore store = doGetPreferenceStore();

        List<String> selectedClasses = new ArrayList<String>();
        for (final TableItem ti : procTable.getItems()) {
            if (ti.getChecked()) {
                selectedClasses.add(ti.getText(CLASSES.ordinal()));
            }
        }

        CheckerManager.storeSelectedClasses(selectedClasses);
        writeMiscParams();

        final List<String> ccFromTi = customClassesFromTableItems();
        final String[] customClasses = ccFromTi.toArray(new String[ccFromTi.size()]);
        CheckerManager.storeCustomClasses(customClasses);

        store.setValue(CheckerPreferences.PREF_CHECKER_PREFS_SET, true);
        /*store.setValue(CheckerPreferences.PREF_CHECKER_AUTO_BUILD,
        optAutoBuild.getSelection()); */
        store.setValue(CheckerPreferences.PREF_CHECKER_A_SKIP_CLASSES, optSkipUses.getText());
        store.setValue(CheckerPreferences.PREF_CHECKER_A_LINT, optALint.getText());
        store.setValue(CheckerPreferences.PREF_CHECKER_A_WARNS, optWarning.getSelection());
        store.setValue(CheckerPreferences.PREF_CHECKER_A_FILENAMES, optFilenames.getSelection());
        store.setValue(CheckerPreferences.PREF_CHECKER_A_NO_MSG_TEXT, optNoMsgText.getSelection());
        store.setValue(CheckerPreferences.PREF_CHECKER_A_SHOW_CHECKS, optShowChecks.getSelection());
        store.setValue(CheckerPreferences.PREF_CHECKER_ERROR_FILTER_REGEX, optFilter.getText());
        store.setValue(CheckerPreferences.PREF_CHECKER_VERBOSE, optVerbose.getSelection());
        store.setValue(CheckerPreferences.PREF_CHECKER_JDK_PATH, optJDKPath.getText());
        store.setValue(
                CheckerPreferences.PREF_CHECKER_IMPLICIT_IMPORTS,
                optImplicitImports.getSelection());

        return true;
    }

    private void searchForClass() {
        OpenTypeSelectionDialog dialog =
                new OpenTypeSelectionDialog(
                        getShell(), true, null, null, IJavaSearchConstants.CLASS);
        dialog.setTitle("Search for checker classes");
        dialog.setMessage("Select additional checkers to use.");

        if (dialog.open() == SelectionDialog.OK) {
            Object[] results = dialog.getResult();
            Set<String> classNames = new LinkedHashSet<String>();

            for (Object result : results) {
                if (result instanceof IType) {
                    IType type = (IType) result;
                    classNames.add(type.getFullyQualifiedName());
                }
            }

            // TODO: CHECK FOR DUPLICATES?

            final List<String> classesInTable = classesFromTableItems();
            for (final String cn : classNames) {
                final CheckerInfo ci = CheckerInfo.fromClassPath(cn, null);
                if (!classesInTable.contains(
                        cn)) { // TODO: ADD A DIALOG TO WARN IF ALREADY CONTAINED
                    addProcTableItem(ci, false);
                }
            }
        }
    }

    private void addOption() {
        final InputDialog inputDialog =
                new InputDialog(
                        getShell(),
                        "Add Option",
                        "Please enter your option exactly as it would appear on the command line. "
                                + "Include any space that would appear between the option name and value.",
                        "",
                        null);

        if (inputDialog.open() == SelectionDialog.OK) {
            final String result = inputDialog.getValue();
            addOptTableItem(new OptionLine(result, true));
        }
    }

    private final List<String> classesFromTableItems() {
        final List<String> classes = new ArrayList<String>(procTable.getItemCount());
        for (final TableItem ti : procTable.getItems()) {
            classes.add(ti.getText(CLASSES.ordinal()));
        }
        return classes;
    }

    private final List<String> customClassesFromTableItems() {
        final List<String> classes = new ArrayList<String>(procTable.getItemCount());
        for (final TableItem ti : procTable.getItems()) {
            if (ti.getText(SOURCE.ordinal()).equals(CUSTOM_LABEL)) {
                classes.add(ti.getText(CLASSES.ordinal()));
            }
        }
        return classes;
    }

    private TableItem addProcTableItem(final CheckerInfo checkerInfo, boolean builtIn) {
        final TableItem item = new TableItem(procTable, SWT.None);
        item.setText(LABEL.ordinal(), checkerInfo.getLabel());
        item.setText(SOURCE.ordinal(), (builtIn) ? BUILT_IN_LABEL : CUSTOM_LABEL);
        item.setText(CLASSES.ordinal(), checkerInfo.getClassPath());
        return item;
    }
}
