package org.checkerframework.netbeans;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
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

    private EditableProperties editableProperty;
    private static Set<Entry<String, String>> checkerStrings;
    private final FileObject projectProperties;
    private final JLabel title;
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

        title = new JLabel("Run Built-In Checker");
        this.add(title);

        projectProperties = inProjectProperties;
        checkerPath =
                InstalledFileLocator.getDefault()
                        .locate("checker.jar", "org.checkerframework.netbeans", false)
                        .getAbsolutePath();
        checkerQualPath =
                InstalledFileLocator.getDefault()
                        .locate("checker-qual.jar", "org.checkerframework.netbeans", false)
                        .getAbsolutePath();

        try {
            editableProperty = loadProperties(projectProperties);
        } catch (IOException e) {
            System.out.println("Failed to load netbeans project.properties file.");
        }

        try {
            checkerStrings =
                    loadProperties(
                                    FileUtil.toFileObject(
                                            InstalledFileLocator.getDefault()
                                                    .locate(
                                                            "checkerstrings.properties",
                                                            "org.checkerframework.netbeans",
                                                            false)))
                            .entrySet();
        } catch (IOException e) {
            System.out.println("Failed to load checker strings properties file.");
            checkerStrings = new HashSet (); //create an empty hash set
        }

        checkerList = new JCheckBox[checkerStrings.size()];
        int i = 0;
        String tmp = editableProperty.get("annotation.processing.processors.list");
        for (Entry<String, String> e : checkerStrings) {
            checkerList[i] = new JCheckBox(e.getValue());
            if (tmp.toString().contains(e.getKey())) {
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
                                    projectProperties, checkerPath, checkerQualPath, updateSelections()));
        } catch (MutexException mux) {
            throw (IOException) mux.getException();
        }
    }
    
    public JCheckBox[] getCheckBoxes (){
        return checkerList;
    }
    
    private String updateSelections(){
        StringBuilder sel = new StringBuilder();
        String selectedChecker = "";
        for (JCheckBox checkBox : checkerList){
            if (checkBox.isSelected()){
                for (Entry<String, String> entry : checkerStrings) {
                    if (entry.getValue().equals(checkBox.getText())){
                        selectedChecker = entry.getKey();
                    }
                }
                //try to add the item
                if (sel.toString().isEmpty()) {
                    sel.append(selectedChecker);
                } else {
                    sel.append(",").append(selectedChecker);
                }
            }
        }
        return sel.toString();
    }
}
