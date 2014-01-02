#!/usr/bin/env python
# encoding: utf-8
"""
release_build.py

Created by Jonathan Burke on 2013-08-01.

Copyright (c) 2013 University of Washington. All rights reserved.
"""

#TODO: NEED TO MODULARIZE BASED ON PROJECT, i.e. in release_vars create project defs for each project
#TODO: RATHER than referring to individual variables we can then refer to the projects props
#TODO: Need a script to build the basic structure of the site for people who want to build
#TODO: them on their local machines

#TODO: CURL THE RELEVANT UTILS BEFORE IMPORTING THEM

from release_vars  import *
from release_utils import *

def print_usage():
    print( "Usage:    python release_build.py [projects] [options]" )
    print_projects( True, 1, 4 )
    print( "\n  --auto  accepts or chooses the default for all prompts" )

#TODO: CREATE A VERSION THAT WOULD RUN ON JENKINS
#TODO: FAIL THAT VERSION IF IT HITS ANY PROMPT (AND GUARD PROMPTS WITH --auto)

#If the relevant repos do not exist, clone them, otherwise, update them.
def update_repos():
    for live_to_interm in LIVE_TO_INTERM_REPOS:
        clone_or_update_repo( live_to_interm[0], live_to_interm[1] )

    for interm_to_release in INTERM_TO_RELEASE_REPOS:
        clone_or_update_repo( interm_to_release[0], interm_to_release[1] )

    clone_or_update_repo( LIVE_PLUME_LIB, PLUME_LIB )
    clone_or_update_repo( LIVE_PLUME_BIB, PLUME_BIB )

def get_afu_date( building_afu ):
    if building_afu:
        return get_current_date()
    else:
        afu_site = os.path.join( HTTP_PATH_TO_LIVE_SITE, "annotation-file-utilities" )
        return extract_from_site( afu_site, "<!-- afu-date -->", "<!-- /afu-date -->" )

def jsr308_checker_framework_version( auto ):
    curr_version = current_distribution( CHECKER_FRAMEWORK )

    print "Current JSR308/Checker Framework: " + curr_version
    suggested_version = increment_version( curr_version )
    new_version = suggested_version

    if not auto:
        new_version = prompt_w_suggestion( "Enter new version", suggested_version, "^\\d+\\.\\d+(?:\\.\\d+){0,2}$" )
    else:
        print "New version: " + new_version

    return new_version

def create_interm_dir( project_name, version, auto ):
    interm_dir = os.path.join(FILE_PATH_TO_DEV_SITE, project_name, "releases", version )
    prompt_or_auto_delete( interm_dir, auto )

    execute("mkdir -p %s" % interm_dir, True, False)
    return interm_dir

def create_interm_version_dirs( jsr308_version, afu_version, auto ):
    #these directories corresponds to the /cse/www2/types/dev/<project_name>/releases/<version> dirs
    jsr308_interm_dir = create_interm_dir("jsr308", jsr308_version, auto )
    afu_interm_dir    = create_interm_dir("annotation-file-utilities", afu_version, auto )
    checker_framework_interm_dir = create_interm_dir("checker-framework", jsr308_version, auto )

    return ( jsr308_interm_dir, afu_interm_dir, checker_framework_interm_dir )

def update_project_symlink( project_name, interm_dir ):
    project_dev_site = os.path.join(FILE_PATH_TO_DEV_SITE, project_name)
    link_path   = os.path.join( project_dev_site, "current" )
    force_symlink( interm_dir, link_path )

def build_jsr308_langtools_release(auto, version, afu_release_date, checker_framework_interm_dir, jsr308_interm_dir):

    afu_build_properties = os.path.join( ANNO_FILE_UTILITIES, "build.properties" )

    #update jsr308_langtools versions
    ant_props = "-Dlangtools=%s -Drelease.ver=%s -Dafu.properties=%s -Dafu.release.date=\"%s\"" % (JSR308_LANGTOOLS, version, afu_build_properties, afu_release_date )
    ant_cmd   = "ant -f release.xml %s update-langtools-versions " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    #TODO: perhaps make a "dist" target rather than listing out the relevant targets
    #build jsr308 binaries and documents but not website, fail if the tests don't pass
    execute("ant -Dhalt.on.test.failure=true clean-and-build-all-tools build-javadoc build-doclets", True, False, JSR308_MAKE)

    #zip up jsr308-langtools project and place it in jsr308_interm_dir
    ant_props = "-Dlangtools=%s -Ddest.dir=%s -Dfile.name=%s -Dversion=%s" % (JSR308_LANGTOOLS, jsr308_interm_dir, "jsr308-langtools.zip", version)
    ant_cmd   = "ant -f release.xml %s zip-langtools " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    #build jsr308 website
    make_cmd = "make jsr308_www=%s jsr308_www_online=%s web-no-checks" % (jsr308_interm_dir, HTTP_PATH_TO_DEV_SITE)
    execute(make_cmd, True, False, JSR308_LT_DOC)

    #copy remaining website files to jsr308_interm_dir
    ant_props = "-Dlangtools=%s -Ddest.dir=%s" % (JSR308_LANGTOOLS, jsr308_interm_dir)
    ant_cmd   = "ant -f release.xml %s langtools-website-docs " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    update_project_symlink( "jsr308", jsr308_interm_dir )

    #push the release changes to the intermediate repository
    #commit_tag_and_push(version, JSR308_LANGTOOLS, "jsr308-")

    return

def get_current_date():
    return CURRENT_DATE.strftime("%d %b %Y")

def get_afu_version( auto ):
    version_regex = "<!-- afu-version -->(\\d+\\.\\d+\\.?\\d?),.*<!-- /afu-version -->"
    anno_html = os.path.join(ANNO_FILE_UTILITIES, "annotation-file-utilities.html")
    version   = find_first_instance(version_regex, anno_html, "")
    if version is None:
        raise Exception( "Could not detect Annotation File Utilities version in file " + anno_html )

    print "Current Annotation File Utilities Version: " + version
    suggested_version = increment_version( version )
    new_version = suggested_version

    if not auto:
        new_version = prompt_w_suggestion("Enter new version", suggested_version, "^\\d+\\.\\d+(?:\\.\\d+){0,2}$")
    else:
        print "New version: " + new_version

    return new_version

def build_annotation_tools_release( auto, version, afu_interm_dir ):

    jv = execute('java -version', True)

    date = get_current_date()

    #Add changelog editing

    build = os.path.join(ANNO_FILE_UTILITIES, "build.xml")
    ant_cmd   = "ant -buildfile %s -e update-versions -Drelease.ver=\"%s\" -Drelease.date=\"%s\"" % (build, version, date)
    execute( ant_cmd )

    #Deploy to intermediate site
    ant_cmd   = "ant -buildfile %s -e web-no-checks -Ddeploy-dir=%s" % ( build, afu_interm_dir )
    execute( ant_cmd )

    update_project_symlink( "annotation-file-utilities", afu_interm_dir )

    #Push release changes to the intermediate repository
    #commit_tag_and_push(version, ANNO_TOOLS, "")

def build_and_locally_deploy_maven(version, checker_framework_interm_dir):
    protocol_length = len("file://")
    maven_dev_repo_without_protocol = MAVEN_DEV_REPO[protocol_length:]

    execute( "mkdir -p " + maven_dev_repo_without_protocol )

    # Build and then deploy the maven plugin
    mvn_install( MAVEN_PLUGIN_DIR )
    mvn_deploy_mvn_plugin( MAVEN_PLUGIN_DIR, MAVEN_PLUGIN_POM, version, MAVEN_DEV_REPO )

    # Deploy jsr308 and checker-qual jars to maven repo
    mvn_deploy( CHECKERS_BINARY, CHECKERS_BINARY_POM, MAVEN_DEV_REPO )
    mvn_deploy( CHECKERS_QUALS,  CHECKERS_QUALS_POM,  MAVEN_DEV_REPO )
    mvn_deploy( JAVAC_BINARY,    JAVAC_BINARY_POM,    MAVEN_DEV_REPO )
    mvn_deploy( JDK7_BINARY,     JDK7_BINARY_POM,     MAVEN_DEV_REPO )

    return

def build_checker_framework_release(auto, version, afu_release_date, checker_framework_interm_dir, jsr308_interm_dir):
    checkers_dir = os.path.join(CHECKER_FRAMEWORK, "checkers")

    afu_build_properties = os.path.join( ANNO_FILE_UTILITIES, "build.properties" )

    #update jsr308_langtools versions
    ant_props = "-Dcheckers=%s -Drelease.ver=%s -Dafu.properties=%s -Dafu.release.date==\"%s\"" % (checkers_dir, version, afu_build_properties, afu_release_date)
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
    ant_props = "-Dcheckers=%s -Ddest.dir=%s -Dfile.name=%s -Dversion=%s" % (checkers_dir, checker_framework_interm_dir, "checkers.zip", version)
    ant_cmd   = "ant -f release.xml %s zip-checker-framework " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    ant_props = "-Dcheckers=%s -Ddest.dir=%s -Dfile.name=%s -Dversion=%s" % (checkers_dir, checker_framework_interm_dir, "mvn-examples.zip", version)
    ant_cmd   = "ant -f release.xml %s zip-maven-examples " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    #copy the remaining checker-framework website files to release_interm_dir
    ant_props = "-Dcheckers=%s -Ddest.dir=%s -Dmanual.name=%s -Dcheckers.webpage=%s" % (
                 checkers_dir, checker_framework_interm_dir, "checkers-manual", "checkers-webpage.html"
    )

    ant_cmd   = "ant -f release.xml %s checker-framework-website-docs " % ant_props
    execute( ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE )

    build_and_locally_deploy_maven(version, checker_framework_interm_dir)

    update_project_symlink( "checker-framework", checker_framework_interm_dir )

    #push the release changes to the intermediate repository
    #commit_tag_and_push(version, CHECKER_FRAMEWORK, "checkers-")
    return

def commit_to_interm_projects(jsr308_version, afu_version, projects_to_release):
    #Use project definition instead, see find project location find_project_locations
    if projects_to_release[LT_OPT]:
        commit_tag_and_push(jsr308_version, JSR308_LANGTOOLS, "jsr308-")

    if projects_to_release[AFU_OPT]:
        commit_tag_and_push(afu_version, ANNO_TOOLS, "")

    if projects_to_release[CF_OPT]:
        commit_tag_and_push(jsr308_version, CHECKER_FRAMEWORK, "checkers-")

def main(argv):
    projects_to_release = read_projects( argv, print_usage )
    auto = read_auto( argv )
    add_project_dependencies( projects_to_release )

    afu_date = get_afu_date( projects_to_release[AFU_OPT] )

    #For each project, build what is necessary but don't push

    #Check for a --auto
    #If --auto then no prompt and just build a full release
    #Otherwise provide all prompts

    print( "Building a new release of Langtools, Annotation Tools, and the Checker Framework!" )
    print( "\nPATH:\n" + os.environ['PATH'] + "\n" )

    clean_repos( INTERM_REPOS,  not auto )
    clean_repos( RELEASE_REPOS, not auto )

    #check we are cloning LIVE -> INTERM, INTERM -> RELEASE
    update_repos()

    check_repos(INTERM_REPOS,  False )
    check_repos(RELEASE_REPOS, False )

    if not auto:
        continue_script = prompt_w_suggestion("Replace files then type yes to continue", "no", "^(Yes|yes|No|no)$")
        if continue_script == "no" or continue_script == "No":
            raise Exception("No prompt")

    check_tools( TOOLS )

    jsr308_version = jsr308_checker_framework_version( auto )
    afu_version = get_afu_version( auto )
    version_dirs = create_interm_version_dirs( jsr308_version, afu_version, auto )
    jsr308_interm_dir            = version_dirs[0]
    afu_interm_dir               = version_dirs[1]
    checker_framework_interm_dir = version_dirs[2]

    print( projects_to_release )
    if projects_to_release[LT_OPT]:
        build_jsr308_langtools_release(auto, jsr308_version, afu_date, checker_framework_interm_dir, jsr308_interm_dir)

    if projects_to_release[AFU_OPT]:
        build_annotation_tools_release( auto, afu_version, afu_interm_dir )

    if projects_to_release[CF_OPT]:
        build_checker_framework_release(auto, jsr308_version, afu_date, checker_framework_interm_dir, jsr308_interm_dir)

    commit_to_interm_projects( jsr308_version, afu_version, projects_to_release )

    ensure_group_access( jsr308_interm_dir )
    ensure_group_access( afu_interm_dir )
    ensure_group_access( checker_framework_interm_dir )

    print("\n\nAdditional Manual Steps\n\n")
    print( "Please remember to go through the Checker Framework and Annotation Tools issue trackers and change any " +
           "issues that are marked 'pushed' to 'fixed'\n" )
    print( "Please try to follow the Checker Framework tutorial to make sure all steps work.\n" )
    print( "Please run the link checker on the development website.\n" )
    print( "Please build the Eclipse plugin and run it once.\n" )

if __name__ == "__main__":
    sys.exit(main(sys.argv))
