package org.checkerframework.netbeans;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.MutexException;

public class CheckerFrameworkPanel extends JPanel {

    private final EditableProperties editableProperty;
    private static Map<String, String> checkerStrings;
    private final FileObject projectProperties;
    private final JCheckBox[] checkerList;
    private final String checkerPath;
    private final String checkerQualPath;

    /**
     * Constructor for the Checker Framework panel in the Project Properties window.
     *
     * @param inProjectProperties A file object containing the Project Properties
     */
    public CheckerFrameworkPanel(FileObject inProjectProperties) {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.add(new JLabel("Run Built-In Checker"));

        projectProperties = inProjectProperties;
        checkerPath =
                InstalledFileLocator.getDefault()
                        .locate("checker.jar", "org.checkerframework.netbeans", false)
                        .getAbsolutePath();
        checkerQualPath =
                InstalledFileLocator.getDefault()
                        .locate("checker-qual.jar", "org.checkerframework.netbeans", false)
                        .getAbsolutePath();

        EditableProperties tempProperty;
        try {
            tempProperty = loadProperties(projectProperties);
        } catch (IOException e) {
            tempProperty = new EditableProperties(false);
            System.out.println("Failed to load netbeans project.properties file.");
        }
        editableProperty = tempProperty;
        try {
            checkerStrings =
                    loadProperties(
                            FileUtil.toFileObject(
                                    InstalledFileLocator.getDefault()
                                            .locate(
                                                    "checkerstrings.properties",
                                                    "org.checkerframework.netbeans",
                                                    false)));
        } catch (IOException e) {
            System.out.println("Failed to load checker strings properties file.");
            checkerStrings = new HashMap<>(); //create an empty hash map
        }

        checkerList = new JCheckBox[checkerStrings.size()];
        int i = 0;
        String tmp = editableProperty.get("annotation.processing.processors.list");
        for (String s : checkerStrings.keySet()) {
            checkerList[i] = new JCheckBox(s);
            if (tmp.contains(checkerStrings.get(s))) {
                checkerList[i].setSelected(true);
            }
            this.add(checkerList[i]);
            i++;
        }
    }

    /**
     * Helper method to load a properties file
     *
     * @param propsFO File object to be loaded
     * @return EditableProperties class loaded with the properties file
     * @throws IOException
     */
    private static EditableProperties loadProperties(FileObject propsFO) throws IOException {
        InputStream propsIS = propsFO.getInputStream();
        EditableProperties props = new EditableProperties(true);
        try {
            props.load(propsIS);
        } finally {
            propsIS.close();
        }
        return props;
    }

    /**
     * This method is called by the action listener that runs when the Project Properties window is
     * closed and will store the selected checkers into the project.properties file.
     *
     * @throws IOException
     */
    public void store() throws IOException {
        try {
            ProjectManager.mutex()
                    .writeAccess(
                            new WriteCheckerFrameworkPropertiesAction(
                                    projectProperties,
                                    checkerPath,
                                    checkerQualPath,
                                    updateSelections()));
        } catch (MutexException mux) {
            throw (IOException) mux.getException();
        }
    }

    private String updateSelections() {
        StringBuilder sel = new StringBuilder();
        String selectedChecker;
        for (JCheckBox checkBox : checkerList) {
            if (checkBox.isSelected()) {
                selectedChecker = checkerStrings.get(checkBox.getText());
                //try to add the item
                if (sel.length() != 0) {
                    sel.append(',');
                }
                sel.append(selectedChecker);
            }
        }
        return sel.toString();
    }
}
