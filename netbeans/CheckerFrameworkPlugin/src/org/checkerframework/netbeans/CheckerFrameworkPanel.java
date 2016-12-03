package org.checkerframework.netbeans;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Mutex;
import org.openide.util.MutexException;

public class CheckerFrameworkPanel extends JPanel {
    
    
    private EditableProperties editableProperty;
    private static Set <Entry<String, String>> checkerStrings;
    private FileObject projectProperties;
    

    private JLabel title;
    private JCheckBox[] checkerList;
    private StringBuilder selection;
    private final String checkerPath;
    private final String checkerQualPath;
    
    /**
     * Constructor for the Checker Framework panel in the Project Properties window. 
     * @param inProjectProperties A file object containing the Project Properties 
     */
    public CheckerFrameworkPanel(FileObject inProjectProperties) {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        title = new JLabel ("Run Built-In Checker");
        this.add(title);
        
        projectProperties = inProjectProperties;
        checkerPath=InstalledFileLocator.getDefault().locate("checker.jar", "org.checkerframework.netbeans", false).getAbsolutePath();
        checkerQualPath=InstalledFileLocator.getDefault().locate("checker-qual.jar", "org.checkerframework.netbeans",false).getAbsolutePath(); 
        
        try{
             editableProperty = loadProperties (projectProperties);
        }
        catch (IOException e){
            System.out.println ("Failed to load netbeans project.properties file.");
        }
        
        try {
        checkerStrings = loadProperties (FileUtil.toFileObject(InstalledFileLocator.getDefault().locate("checkerstrings.properties", "org.checkerframework.netbeans", false))).entrySet();
        }
        catch(IOException e){
            System.out.println ("Failed to load checker strings properties file.");
        }
               
        checkerList = new JCheckBox[checkerStrings.size()];
        int i =0;
        selection = new StringBuilder(editableProperty.get("annotation.processing.processors.list"));
        for (Entry <String, String> e : checkerStrings){
            checkerList[i] = new JCheckBox(e.getValue());
            if (selection.toString().contains(e.getKey())){
                checkerList[i].setSelected(true);
            }
            this.add(checkerList[i]);
            checkerList[i].addItemListener(new CheckBoxListener(selection));
            i++;
        }
    }

    /**
     * Helper method to load a properties file
     * @param propsFO File object to be loaded
     * @return EditableProperties class loaded with the properties file
     * @throws IOException 
     */
    private static EditableProperties loadProperties (FileObject propsFO) throws IOException {
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
     * This method is called by the action listener that runs when the Project 
     * Properties window is closed and will store the selected checkers into
     * the project.properties file. 
     * @throws IOException 
     */
    public void store() throws IOException {
        try {
            ProjectManager.mutex().writeAccess(new WriteCheckerFrameworkPropertiesAction(projectProperties, checkerPath, checkerQualPath, selection));
        } catch (MutexException mux) {
            throw (IOException) mux.getException();
        }
    }
    
    /**
     * ItemListener that updates the checkers to run based on the check boxes that
     * get selected.
     */
    private static class CheckBoxListener implements ItemListener {
        private StringBuilder s;
        private String selectedChecker;
        public CheckBoxListener (StringBuilder inString){
            s = inString;
        }
        @Override
        public void itemStateChanged (ItemEvent e){
            JCheckBox source = (JCheckBox) e.getItemSelectable();
            
            for (Entry<String, String> entry : checkerStrings){
                if (entry.getValue().equals(source.getText()))
                    selectedChecker = entry.getKey();
            }
            if (e.getStateChange() == ItemEvent.SELECTED){
                //try to add the item
                if(s.toString().isEmpty()){
                    s.append(selectedChecker);
                }
                else{
                    s.append(","+selectedChecker);
                }
            }
            else{
                //try to remove the item
                int pos = s.toString().indexOf(selectedChecker);
                if (pos == 0){
                    if (s.toString().length()==selectedChecker.length())
                        s.delete(pos, pos+selectedChecker.length());
                    else
                        s.delete(pos, pos+selectedChecker.length()+1);
                }
                else if (pos >0){
                    s.delete(pos-1, pos+selectedChecker.length());
                }
            }
        }
    }
}
