Steps to build and release the NetBeans Checker Framework plugin:
1. Copy Checker Framework artifacts to the netbeans/CheckerFrameworkPlugin/release directory (just checker.jar and checker-qual.jar should be enough)
2. Run the following command: ant -f .../checker-framework/netbeans/CheckerFrameworkPlugin nbm
3. A .nbm file should be created in .../checker-framework/netbeans/CheckerFrameworkPlugin/build
4. Copy the .nbm file created in step 3 from .../checker-framework/netbeans/CheckerFrameworkPlugin/build to .../checker-framework/netbeans/CheckerFrameworkPluginUpdateSite
5. Update catalog.xml in .../checker-framework/netbeans/CheckerFrameworkPluginUpdateSite so that downloadsize matches the size of the .nbm file (in bytes)

The directory netbeans/CheckerFrameworkPluginUpdateSite needs to be hosted on a website.

In order to install the NetBeans plugin, in NetBeans, click on "Tools>Plugins" and select the "Settings" tab.
Then, click "Add" to add a new Update Center with "Name" as "Checker Framework" and "URL" as "http://<URL where catalog.xml and .nbm are hosted>/catalog.xml".
Go to the "Available Plugins" tab, and search and select "Checker Framework Plugin" and click "Install".

In order to use the plugin, right click on a Java project in NetBeans, and click "Properties".
Select the desired checkers to run, and then press "Ok" to apply the changes and close the Properties window.
The checkers should be run and the results should appear in the IDE editor, if Netbeans is set to compile on save.
