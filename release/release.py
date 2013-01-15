#!/usr/bin/env python
# encoding: utf-8
"""
release.py

Created by Mahmood Ali on 2010-02-25.

Copyright (c) 2010 Jude LLC. All rights reserved.
"""

from xml.dom import minidom
import sys
import getopt
import urllib2
import re
import subprocess
import os
import pwd

# See release_utils.py in the same directory as this script
from release_utils import *

#=========================================================================================
#Global constants used in main
SCRIPT_PATH=os.path.abspath(__file__)

# "USER = os.getlogin()" does not work; see http://bugs.python.org/issue584566
# Another alternative is: USER = os.getenv('USER')
USER = pwd.getpwuid(os.geteuid())[0]

DEFAULT_SITE = "http://types.cs.washington.edu/jsr308/"
LOCAL_PATH_TO_SITE ="/cse/www2/types/"

# OPENJDK_RELEASE_SITE = 'http://download.java.net/openjdk/jdk7/'
OPENJDK_RELEASE_SITE = 'http://jdk8.java.net/download.html'

#TODO: Just build here and move, examine whether or not there are hard-coded paths
#      then fix them because there should be no reason to build the entire thing twice

#DRY_RUN_PATHS
DRY_RUN_LINK_HTTP = "http://homes.cs.washington.edu/~%s/jsr308test/jsr308/" % USER
DRY_PATH = os.path.join(os.environ['HOME'], 'www', 'jsr308test')
DRY_RUN_LINK_FILE = "file://%s/jsr308/" % DRY_PATH
DRY_RUN_LINK = DRY_RUN_LINK_HTTP

EMAIL_TO='jsr308-discuss@googlegroups.com, checker-framework-discuss@googlegroups.com'

#The script is at jsr308-release/checker-framework/release/release.py
RELEASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(SCRIPT_PATH)))

CHECKER_FRAMEWORK  = os.path.join(RELEASE_DIR, 'checker-framework')
CHECKER_BIN_DIR    = os.path.join(CHECKER_FRAMEWORK, 'checkers', 'binary')
CHECKERS_BINARY    = os.path.join(CHECKER_BIN_DIR, 'checkers.jar'       )
CHECKERS_QUALS     = os.path.join(CHECKER_BIN_DIR, 'checkers-quals.jar' )
JAVAC_BINARY       = os.path.join(CHECKER_BIN_DIR, 'javac.jar')
JDK6_BINARY        = os.path.join(CHECKER_BIN_DIR, 'jdk6.jar' )
JDK7_BINARY        = os.path.join(CHECKER_BIN_DIR, 'jdk7.jar' )

CHECKERS_BINARY_POM    = os.path.join(CHECKER_BIN_DIR, 'poms', 'checkersPom.xml'      )
CHECKERS_QUALS_POM     = os.path.join(CHECKER_BIN_DIR, 'poms', 'checkersQualsPom.xml' )
JAVAC_BINARY_POM       = os.path.join(CHECKER_BIN_DIR, 'poms', 'compilerPom.xml'      )
JDK6_BINARY_POM        = os.path.join(CHECKER_BIN_DIR, 'poms', 'jdk6Pom.xml' )
JDK7_BINARY_POM        = os.path.join(CHECKER_BIN_DIR, 'poms', 'jdk7Pom.xml' )

CHECKERS_CHANGELOG = os.path.join(CHECKER_FRAMEWORK, 'checkers', 'changelog-checkers.txt')

JSR308_LANGTOOLS    = os.path.join(RELEASE_DIR, 'jsr308-langtools')
JSR308_CHANGELOG    = os.path.join(JSR308_LANGTOOLS, 'doc', 'changelog-jsr308.txt')
JSR308_DOC_MAKEFILE = os.path.join(JSR308_LANGTOOLS, 'doc', 'Makefile')

PLUME_LIB = os.path.join(RELEASE_DIR, 'plume-lib')

MAVEN_PLUGIN_DIR = os.path.join(CHECKER_FRAMEWORK, 'maven-plugin')
MAVEN_PLUGIN_POM = os.path.join(MAVEN_PLUGIN_DIR,  'pom.xml')
MAVEN_REPO = 'file:///cse/www2/types/m2-repo'

#=========================================================================================
#Set environment variables needed to find specific tools
#These are paths hardcoded for buffalo.cs.washington.edu
#If you are running this on a machine besides buffalo then either
#overwrite these or comment them out and set these from your shell

#TODO: NEED TO ADD THESE BEFORE MAKING AFU and JSR308, i.e. add making those two to this script
os.environ['PLUME_LIB'] =  PLUME_LIB
os.environ['TEXINPUTS'] =  '.:/scratch/secs-jenkins/tools/hevea-1.10/lib/hevea:/usr/share/texmf/tex/latex/hevea/:/homes/gws/mernst/tex/sty:/homes/gws/mernst/tex:..:'
os.environ['PERLLIB']   =  getAndAppend('PERLLIB', ":")  + "/homes/gws/mernst/bin/src/perl/lib/perl5/site_perl/5.10.0:/homes/gws/jburke/perl_lib"
os.environ['PERL5LIB']  =  getAndAppend('PERL5LIB', ":") + "/homes/gws/mernst/bin/src/perl/lib/perl5/site_perl/5.10.0:/homes/gws/jburke/perl_lib"
os.environ['JAVA_HOME'] =  '/scratch/secs-jenkins/java/jdk1.6.0'
os.environ['JAVA_6_HOME'] =  '/scratch/secs-jenkins/java/jdk1.6.0'
os.environ['JAVA_7_HOME'] =  '/scratch/secs-jenkins/java/jdk1.7.0'

EDITOR = os.getenv('EDITOR')
if EDITOR == None:
    EDITOR = 'emacs'

PATH = os.environ['JAVA_HOME'] + "/projects/uns/F11/bin/"
PATH = PATH + ":/scratch/secs-jenkins/tools/hevea-1.10/bin/:/projects/uns/F11/bin/"
PATH = PATH + ":" + PLUME_LIB + "/bin:/homes/gws/mernst/bin/share"
PATH = PATH + ":/homes/gws/mernst/bin/Linux-i686:/uns/bin:/homes/gws/mali/local/share/maven/bin:."
os.environ['PATH'] = PATH + ":" + os.environ['PATH']

#Tools that must be on your PATH ( besides common *nix ones like grep )
TOOLS = [ 'hevea', 'perl', 'java', 'dia', 'latex', 'mvn', 'hg', EDITOR ]

#Consult release_utils.py for method definitions for the steps below
def main(argv):
    print("PATH " + os.environ['PATH'])
    print("Making a new release of the Checker Framework!")

    print("\nChecking to make sure the following programs are installed:")
    print(', '.join(TOOLS))
    print('Note: If you are NOT working on buffalo.cs.washington.edu then you ' +
          'likely need to change the variables that are set in release.py \n'   +
          'search for "Set environment variables"')
    map(check_command, TOOLS)

    #TODO Add check of whether or not there are any differences in the dev folder
    #TODO If there are, display them and ask if the user wishes to continue

    print("\nUpdating Checker Framework and JSR308 Langtools")
    # Update repositories
    update_projects( ( CHECKER_FRAMEWORK, JSR308_LANGTOOLS ) )

    #Ensure the 3 versions of PluginUtils.java are all the same except for the package definition
    print("\nChecking if PluginUtil.java files are synchronized")
    checkPluginScript = os.path.join(CHECKER_FRAMEWORK, "release", "checkPluginUtil.sh");
    execute('sh %s' % (checkPluginScript));

    # Infer version
    curr_version = current_distribution(DEFAULT_SITE)

    print("\nCurrent release is %s" % curr_version)
    suggested_version = increment_version(curr_version)
    next_version = raw_input("Suggested next release: (%s): " % suggested_version)
    if not next_version:
        next_version = suggested_version
    print next_version

    #update the version of all Maven pom files including the maven plugin
    print("\nUpdating Maven pom files")
    updateVersionScript = os.path.join(CHECKER_FRAMEWORK, "checkers", "binary", "poms", "updateAllVersions.sh")
    execute('sh %s %s' % (updateVersionScript, next_version))

    # Retrieve and update changes to the changelogs
    checkers_changes = retrieve_changes(CHECKER_FRAMEWORK, curr_version, "checkers-")
    edit_checkers_changelog(next_version, CHECKERS_CHANGELOG, EDITOR, changes=checkers_changes)

    langtools_changes = retrieve_changes(JSR308_LANGTOOLS, curr_version, "jsr308-")
    edit_langtools_changelog(next_version, OPENJDK_RELEASE_SITE, JSR308_CHANGELOG, EDITOR, changes=langtools_changes)

    # Making the first release
    ant_args = ""
    for arg in argv[1:]:      # everything but the first element
        ant_args = ant_args + " '" + arg + "'"
    if not os.path.exists(DRY_PATH):
        site_copy(ant_args)
    make_release(next_version, ant_args)
    checklinks(JSR308_DOC_MAKEFILE, DRY_RUN_LINK)

    print("Pushed to %s" % DRY_RUN_LINK)
    raw_input("Please check the site.  Press ENTER to continue.")
    print("\n\n\n\n\n")

    # Making the real release
    make_release(next_version, ant_args, real=False) #TODO: TURN THIS BACK TO TRUE

    # Build and then deploy the maven plugin
    mvn_install(MAVEN_PLUGIN_DIR)
    mvn_deploy_mvn_plugin(MAVEN_PLUGIN_DIR, MAVEN_PLUGIN_POM, next_version, MAVEN_REPO)

    # Deploy jsr308 and checker-qual jars to maven repo
    mvn_deploy( CHECKERS_BINARY, CHECKERS_BINARY_POM, MAVEN_REPO )
    mvn_deploy( CHECKERS_QUALS,  CHECKERS_QUALS_POM,  MAVEN_REPO )
    mvn_deploy( JAVAC_BINARY,    JAVAC_BINARY_POM,    MAVEN_REPO )
    mvn_deploy( JDK6_BINARY,     JDK6_BINARY_POM,     MAVEN_REPO )
    mvn_deploy( JDK6_BINARY,     JDK7_BINARY_POM,     MAVEN_REPO )

    checklinks(JSR308_DOC_MAKEFILE, DEFAULT_SITE)

    print("Pushed to %s" % DEFAULT_SITE)
    raw_input("Please check the site.  DONE?  Press ENTER to continue.")
    print("\n\n\n\n\n")

    #TODO: Remind the user they must build and deploy the eclipse plugin

    #Push the final state (after any changes made during the release) to the repositories
    #Adding an identifying tag for this release
    commit_tag_and_push(next_version, CHECKER_FRAMEWORK, "checkers-")
    commit_tag_and_push(next_version, JSR308_LANGTOOLS, "jsr308-")

    #Add group write access to all files in the website to ensure they are served
    ensure_group_access( LOCAL_PATH_TO_SITE )

    print("You have just made the release.  Please announce it to the world")
    print("Here is an email template:")
    print format_email(next_version, EMAIL_TO, JSR308_CHANGELOG, CHECKERS_CHANGELOG)

# The entry point to the Python script.
if __name__ == "__main__":
    sys.exit(main(sys.argv))
