Steps to build and release the NetBeans Checker Framework plugin:
1. Copy Checker Framework artifacts to the netbeans/CheckerFrameworkPlugin/release directory
2. Open the CheckerFrameworkPlugin project in NetBeans
3. Right click on the project in NetBeans and select "Create NBM"
4. Copy the .nbm file created in step 3 from netbeans/CheckerFrameworkPlugin/build to netbeans/CheckerFrameworkPluginUpdateSite
5. Update catalog.xml in netbeans/CheckerFrameworkPluginUpdateSite so that downloadsize matches the size of the .nbm file (in bytes)

The above steps could also be done with ant tasks. There is a makenbm task included in the NetBeans build infrastructure that can be used for packaging the .nbm file.

The directory netbeans/CheckerFrameworkPluginUpdateSite needs to be hosted on a website.

In order to install the NetBeans plugin, in NetBeans, click on "Tools>Plugins" and select the "Settings" tab.
Then, click "Add" to add a new Update Center with "Name" as "Checker Framework" and "URL" as "http://<URL where catalog.xml and .nbm are hosted>/catalog.xml".
Go to the "Available Plugins" tab, and search and select "Checker Framework Plugin" and click "Install".
