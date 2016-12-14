package org.checkerframework.netbeans;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.swing.JComponent;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.netbeans.spi.project.ui.support.ProjectCustomizer.Category;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

public class CheckerFrameworkPanelProvider implements ProjectCustomizer.CompositeCategoryProvider {

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
        projectType = "org-netbeans-modules-java-j2seproject",
        position = 2147483647
    )
    public static CheckerFrameworkPanelProvider createCheckerFramework() {
        return new CheckerFrameworkPanelProvider();
    }

    private static final String NAME = "CheckerFramework";
    private FileObject projectProperties;

    @Override
    public Category createCategory(Lookup lkp) {
        Project p = lkp.lookup(Project.class);
        projectProperties =
                p.getProjectDirectory().getFileObject(AntProjectHelper.PROJECT_PROPERTIES_PATH);

        ResourceBundle bundle = NbBundle.getBundle(CheckerFrameworkPanelProvider.class);
        ProjectCustomizer.Category toReturn = null;
        toReturn =
                ProjectCustomizer.Category.create(
                        NAME, bundle.getString("LBL_Config_CheckerFramework"), null);

        return toReturn;
    }

    @Override
    public JComponent createComponent(Category ctgr, Lookup lkp) {
        CheckerFrameworkPanel cfp = new CheckerFrameworkPanel(projectProperties);
        ctgr.setStoreListener(new ModifyPropertiesListener(cfp));
        return cfp;
    }

    private static class ModifyPropertiesListener implements ActionListener {
        private CheckerFrameworkPanel checkerFrameworkPanel;

        public ModifyPropertiesListener(CheckerFrameworkPanel inCheckerFrameworkPanel) {
            checkerFrameworkPanel = inCheckerFrameworkPanel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                checkerFrameworkPanel.store();
            } catch (Exception exception) {
                System.out.println("Could not store checkers in project properties.");
            }
        }
    }
}
