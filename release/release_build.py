#!/usr/bin/env python
# encoding: utf-8
"""
release_build.py

Created by Jonathan Burke on 2013-02-05.

Copyright (c) 2013 University of Washington. All rights reserved.
"""

from release_vars  import *
from release_utils import *

#TODO: CREATE A VERSION THAT WOULD RUN ON JENKINS
#TODO: FAIL THAT VERSION IF IT HITS ANY PROMPT (AND GUARD PROMPTS WITH --auto)

#TODO: Split build into PHASES that can be run individually so you don't have
#TODO: to redo everything in case of an error

#If the relevant repos do not exist, clone them, otherwise, update them.
def update_repos():
    for live_to_interm in LIVE_TO_INTERM_REPOS:
        clone_or_update_repo( live_to_interm[0], live_to_interm[1] )

    for interm_to_release in INTERM_TO_RELEASE_REPOS:
        clone_or_update_repo( interm_to_release[0], interm_to_release[1] )

    clone_or_update_repo( LIVE_PLUME_LIB, PLUME_LIB )

def jsr308_checker_framework_version():
    curr_version = current_distribution(HTTP_PATH_TO_LIVE_SITE)

    print "Current JSR308/Checker Framework: " + curr_version
    new_version = prompt_w_suggestion("Enter new version", increment_version(curr_version), "^\\d+\\.\\d+(?:\\.\\d+){0,2}$")
    return new_version

def create_interm_version_dirs(version):

    #this directory corresponds to the /cse/www2/types/checker-framework/releases/<version> dir
    checker_framework_interm_dir = os.path.join(FILE_PATH_TO_DEV_SITE, "checker-framework", "releases", version)
    prompt_to_delete(checker_framework_interm_dir)
    execute("mkdir -p %s" % checker_framework_interm_dir, True, False)

    #this directory corresponds to the /cse/www2/types/jsr308/
    #notice there aren't multiple version for this in the live site but there are in this build script
    jsr308_interm_dir = os.path.join(FILE_PATH_TO_DEV_SITE, "jsr308", "releases", version)
    prompt_to_delete(jsr308_interm_dir)
    execute("mkdir -p %s" % jsr308_interm_dir, True, False)

    return (checker_framework_interm_dir, jsr308_interm_dir)

def build_jsr308_langtools_release(version, checker_framework_interm_dir, jsr308_interm_dir):

    #update jsr308_langtools versions
    ant_props = "-Dlangtools=%s -Drelease.ver=%s" % (JSR308_LANGTOOLS, version)
    ant_cmd   = "ant -f release.xml %s update-langtools-versions " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    #TODO: perhaps make a "dist" target rather than listing out the relevant targets
    #build jsr308 binaries and documents but not website, fail if the tests don't pass
    execute("ant -Dhalt.on.test.failure=true clean-and-build-all-tools build-javadoc build-doclets", True, False, JSR308_MAKE)

    #zip up jsr308-langtools project and place it in jsr308_interm_dir
    ant_props = "-Dlangtools=%s -Ddest.dir=%s -Dfile.name=%s" % (JSR308_LANGTOOLS, jsr308_interm_dir, "jsr308-langtools.zip")
    ant_cmd   = "ant -f release.xml %s zip-langtools " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    #build jsr308 website
    make_cmd = "make jsr308_www=%s jsr308_www_online=%s web-no-checks" % (jsr308_interm_dir, HTTP_PATH_TO_DEV_SITE)
    execute(make_cmd, True, False, JSR308_LT_DOC)

    #copy remaining website files to jsr308_interm_dir
    ant_props = "-Dlangtools=%s -Ddest.dir=%s" % (JSR308_LANGTOOLS, jsr308_interm_dir)
    ant_cmd   = "ant -f release.xml %s langtools-website-docs " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    #push the release changes to the intermediate repository
    #commit_tag_and_push(version, JSR308_LANGTOOLS, "jsr308-")

    return

def build_annotation_tools_release():
    version_regex = "<!-- afu-version -->(\\d+\\.\\d+\\.?\\d?),.*<!-- /afu-version -->"
    anno_html = os.path.join(ANNO_FILE_UTILITIES, "annotation-file-utilities.html")
    version   = find_first_instance(version_regex, anno_html, "")
    if version is None:
        raise Exception( "Could not detect Annotation File Utilities version in file " + anno_html )

    print "Current Annotation File Utilities Version: " + version
    new_version = prompt_w_suggestion("Enter new version", increment_version(version), "^\\d+\\.\\d+(?:\\.\\d+){0,2}$")

    jv = execute('java -version', True)

    new_date = CURRENT_DATE.strftime("%d %b %Y")

    #Add changelog editing

    build = os.path.join(ANNO_FILE_UTILITIES, "build.xml")
    ant_cmd   = "ant -buildfile %s -e update-versions -Drelease.ver=\"%s\" -Drelease.date=\"%s\"" % (build, new_version, new_date)
    execute( ant_cmd )

    #Deploy to dev site
    anno_dev = os.path.join(FILE_PATH_TO_DEV_SITE, "annotation-file-utilities")
    ant_cmd   = "ant -buildfile %s -e web-no-checks -Ddeploy-dir=%s" % (build, anno_dev)
    execute( ant_cmd )

    #Push release changes to the intermediate repository
    #commit_tag_and_push(version, ANNO_TOOLS, "")

def build_and_locally_deploy_maven(version, checker_framework_interm_dir):
    maven_repo = os.path.join(checker_framework_interm_dir, "m2-repo")
    execute("mkdir -p " + maven_repo)

    mvn_repo_with_protocol = "file://" + maven_repo

    # Build and then deploy the maven plugin
    mvn_install(MAVEN_PLUGIN_DIR)
    mvn_deploy_mvn_plugin(MAVEN_PLUGIN_DIR, MAVEN_PLUGIN_POM, version, mvn_repo_with_protocol)

    # Deploy jsr308 and checker-qual jars to maven repo
    mvn_deploy( CHECKERS_BINARY, CHECKERS_BINARY_POM, mvn_repo_with_protocol )
    mvn_deploy( CHECKERS_QUALS,  CHECKERS_QUALS_POM,  mvn_repo_with_protocol )
    mvn_deploy( JAVAC_BINARY,    JAVAC_BINARY_POM,    mvn_repo_with_protocol )
    mvn_deploy( JDK7_BINARY,     JDK7_BINARY_POM,     mvn_repo_with_protocol )

    return

def build_checker_framework_release(version, checker_framework_interm_dir, jsr308_interm_dir):
    checkers_dir = os.path.join(CHECKER_FRAMEWORK, "checkers")

    #update jsr308_langtools versions
    ant_props = "-Dcheckers=%s -Drelease.ver=%s" % (checkers_dir, version)
    ant_cmd   = "ant -f release.xml %s update-checker-framework-versions " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    #ensure all PluginUtil.java files are identical
    execute("sh checkPluginUtil.sh", True, False, CHECKER_FRAMEWORK_RELEASE)

    #Add changelog editing

    #build the checker framework binaries and documents, run checker framework tests
    execute("ant -Dhalt.on.test.failure=true dist", True, False, checkers_dir)

    #make the Checker Framework Manual
    checkers_manual_dir = os.path.join(checkers_dir, "manual")
    execute("make manual.pdf manual.html", True, False, checkers_manual_dir)

    #make the checker framework tutorial
    checkers_tutorial_dir = os.path.join(CHECKER_FRAMEWORK, "tutorial")
    execute("make", True, False, checkers_tutorial_dir)

    #zip up checkers.zip and put it in releases_iterm_dir
    ant_props = "-Dcheckers=%s -Ddest.dir=%s -Dfile.name=%s" % (checkers_dir, checker_framework_interm_dir, "checkers.zip")
    ant_cmd   = "ant -f release.xml %s zip-checker-framework " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    #copy the remaining checker-framework website files to release_interm_dir
    ant_props = "-Dcheckers=%s -Ddest.dir=%s -Dmanual.name=%s -Dcheckers.webpage=%s" % (
                 checkers_dir, checker_framework_interm_dir, "checkers-manual", "checkers-webpage.html"
    )

    ant_cmd   = "ant -f release.xml %s checker-framework-website-docs " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    build_and_locally_deploy_maven(version, checker_framework_interm_dir)

    #push the release changes to the intermediate repository
    #commit_tag_and_push(version, CHECKER_FRAMEWORK, "checkers-")
    return

def main(argv):
    #Check for a --auto
    #If --auto then no prompt and just build a full release
    #Otherwise provide all prompts

    print("Building a new release of Langtools, Annotation Tools, and the Checker Framework!")
    print("\nPATH:\n" + os.environ['PATH'] + "\n")

    clean_repos( INTERM_REPOS,  True )
    clean_repos( RELEASE_REPOS, True )

    #check we are cloning LIVE -> INTERM, INTERM -> RELEASE
    update_repos()

    check_repos(INTERM_REPOS,  False)
    check_repos(RELEASE_REPOS, False)

    continue_script = prompt_w_suggestion("Replace files then type yes to continue", "no", "^(Yes|yes|No|no)$")
    if continue_script == "no" or continue_script == "No":
        raise Exception("No prompt")

    check_tools(TOOLS)

    version = jsr308_checker_framework_version()
    version_dirs = create_interm_version_dirs(version)
    checker_framework_interm_dir = version_dirs[0]
    jsr308_interm_dir            = version_dirs[1]

    build_jsr308_langtools_release(version, checker_framework_interm_dir, jsr308_interm_dir)
    build_annotation_tools_release()  #annotation tools has it's own version
    build_checker_framework_release(version, checker_framework_interm_dir, jsr308_interm_dir)

    ensure_group_access(checker_framework_interm_dir)
    ensure_group_access(jsr308_interm_dir)

if __name__ == "__main__":
    sys.exit(main(sys.argv))
