#!/usr/bin/env python
# encoding: utf-8
"""
release_build.py

Created by Jonathan Burke on 2013-08-01.

Copyright (c) 2014 University of Washington. All rights reserved.
"""

#See README-maintainers.html for more information

from release_vars  import *
from release_utils import *

def print_usage():
    print( "Usage:    python release_build.py [projects] [options]" )
    print_projects( True, 1, 4 )
    print( "\n  --auto  accepts or chooses the default for all prompts" )

#If the relevant repos do not exist, clone them, otherwise, update them.
def update_repos():
    for live_to_interm in LIVE_TO_INTERM_REPOS:
        clone_or_update_repo( live_to_interm[0], live_to_interm[1] )

    for interm_to_build in INTERM_TO_BUILD_REPOS:
        clone_or_update_repo( interm_to_build[0], interm_to_build[1] )

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

    return (curr_version, new_version)

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

    print( "Writing symlink: " + link_path + "\nto directory: " + interm_dir )
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
    ant_props = "-Dlangtools=%s  -Dcheckerframework=%s -Ddest.dir=%s -Dfile.name=%s -Dversion=%s" % (JSR308_LANGTOOLS, CHECKER_FRAMEWORK, jsr308_interm_dir, "jsr308-langtools.zip", version)
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

    return

def get_current_date():
    return CURRENT_DATE.strftime("%d %b %Y")

def get_afu_version( auto ):
    anno_html = os.path.join(ANNO_FILE_UTILITIES, "annotation-file-utilities.html")
    version = get_afu_version_from_html( anno_html )

    print "Current Annotation File Utilities Version: " + version
    suggested_version = increment_version( version )
    new_version = suggested_version

    if not auto:
        new_version = prompt_w_suggestion("Enter new version", suggested_version, "^\\d+\\.\\d+(?:\\.\\d+){0,2}$")
    else:
        print "New version: " + new_version

    return (version, new_version)

def build_annotation_tools_release( auto, version, afu_interm_dir ):
    jv = execute( 'java -version', True )

    date = get_current_date()

    build = os.path.join(ANNO_FILE_UTILITIES, "build.xml")
    ant_cmd   = "ant -buildfile %s -e update-versions -Drelease.ver=\"%s\" -Drelease.date=\"%s\"" % (build, version, date)
    execute( ant_cmd )

    #Deploy to intermediate site
    ant_cmd   = "ant -buildfile %s -e web-no-checks -Ddeploy-dir=%s" % ( build, afu_interm_dir )
    execute( ant_cmd )

    update_project_symlink( "annotation-file-utilities", afu_interm_dir )

def build_and_locally_deploy_maven(version, checker_framework_interm_dir):
    protocol_length = len("file://")
    maven_dev_repo_without_protocol = MAVEN_DEV_REPO[protocol_length:]

    execute( "mkdir -p " + maven_dev_repo_without_protocol )

    # Build and then deploy the maven plugin
    mvn_install( MAVEN_PLUGIN_DIR )
    mvn_deploy_mvn_plugin( MAVEN_PLUGIN_DIR, MAVEN_PLUGIN_POM, version, MAVEN_DEV_REPO )

    # Deploy jsr308 and checker-qual jars to maven repo
    mvn_deploy( CHECKER_BINARY, CHECKER_BINARY_POM, MAVEN_DEV_REPO )
    mvn_deploy( CHECKER_QUAL,   CHECKER_QUAL_POM,   MAVEN_DEV_REPO )
    mvn_deploy( JAVAC_BINARY,    JAVAC_BINARY_POM,    MAVEN_DEV_REPO )
    mvn_deploy( JDK7_BINARY,     JDK7_BINARY_POM,     MAVEN_DEV_REPO )
    mvn_deploy( JDK8_BINARY,     JDK8_BINARY_POM,     MAVEN_DEV_REPO )

    return

def build_checker_framework_release(auto, version, afu_release_date, checker_framework_interm_dir, jsr308_interm_dir):
    checker_dir = os.path.join(CHECKER_FRAMEWORK, "checker")

    afu_build_properties = os.path.join( ANNO_FILE_UTILITIES, "build.properties" )

    #update jsr308_langtools versions
    ant_props = "-Dchecker=%s -Drelease.ver=%s -Dafu.properties=%s -Dafu.release.date=\"%s\"" % (checker_dir, version, afu_build_properties, afu_release_date)
    ant_cmd   = "ant -f release.xml %s update-checker-framework-versions " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    #ensure all PluginUtil.java files are identical
    execute("sh checkPluginUtil.sh", True, False, CHECKER_FRAMEWORK_RELEASE)

    #build the checker framework binaries and documents, run checker framework tests
    execute("ant -Dhalt.on.test.failure=true dist-release", True, False, CHECKER_FRAMEWORK)

    #make the Checker Framework Manual
    checker_manual_dir = os.path.join(checker_dir, "manual")
    execute("cp " + os.path.join(SCRIPTS_DIR, "hevea.sty") + " .", True, False, checker_manual_dir)
    execute("cp " + os.path.join(SCRIPTS_DIR, "comment.sty") + " .", True, False, checker_manual_dir)
    execute("make manual.pdf manual.html", True, False, checker_manual_dir)

    #make the dataflow manual
    dataflow_manual_dir = os.path.join(CHECKER_FRAMEWORK, "dataflow", "manual")
    execute("pdflatex dataflow.tex", True, False, dataflow_manual_dir)
    # Yes, run it twice
    execute("pdflatex dataflow.tex", True, False, dataflow_manual_dir)

    #make the checker framework tutorial
    checker_tutorial_dir = os.path.join(CHECKER_FRAMEWORK, "tutorial")
    execute("make", True, False, checker_tutorial_dir)

    #zip up checker-framework.zip and put it in checker_framework_interm_dir
    ant_props = "-Dchecker=%s -Ddest.dir=%s -Dfile.name=%s -Dversion=%s" % (checker_dir, checker_framework_interm_dir, "checker-framework.zip", version)
    ant_cmd   = "ant -f release.xml %s zip-checker-framework " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    ant_props = "-Dchecker=%s -Ddest.dir=%s -Dfile.name=%s -Dversion=%s" % (checker_dir, checker_framework_interm_dir, "mvn-examples.zip", version)
    ant_cmd   = "ant -f release.xml %s zip-maven-examples " % ant_props
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

#    #copy the remaining checker-framework website files to checker_framework_interm_dir
    ant_props = "-Dchecker=%s -Ddest.dir=%s -Dmanual.name=%s -Ddataflow.manual.name=%s -Dchecker.webpage=%s" % (
                 checker_dir, checker_framework_interm_dir, "checker-framework-manual",
                 "checker-framework-dataflow-manual", "checker-framework-webpage.html"
    )

    ant_cmd   = "ant -f release.xml %s checker-framework-website-docs " % ant_props
    execute( ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE )

    build_and_locally_deploy_maven(version, checker_framework_interm_dir)

    update_project_symlink( "checker-framework", checker_framework_interm_dir )

    return

def commit_to_interm_projects(jsr308_version, afu_version, projects_to_release):
    #Use project definition instead, see find project location find_project_locations
    if projects_to_release[LT_OPT]:
        commit_tag_and_push(jsr308_version, JSR308_LANGTOOLS, "jsr308-")

    if projects_to_release[AFU_OPT]:
        commit_tag_and_push(afu_version, ANNO_TOOLS, "")

    if projects_to_release[CF_OPT]:
        commit_tag_and_push(jsr308_version, CHECKER_FRAMEWORK, "checker-framework-")

def main(argv):
    set_umask()

    projects_to_release = read_projects( argv, print_usage )
    auto = read_auto( argv )
    add_project_dependencies( projects_to_release )

    afu_date = get_afu_date( projects_to_release[AFU_OPT] )

    check_hg_user()

    #For each project, build what is necessary but don't push

    #Check for a --auto
    #If --auto then no prompt and just build a full release
    #Otherwise provide all prompts

    print( "Building a new release of Langtools, Annotation Tools, and the Checker Framework!" )
    print( "\nPATH:\n" + os.environ['PATH'] + "\n" )

    print_step("Build Step 1: Clean and update the build and intermediate repositories.")

    print_step("1a: Clean repositories.")
    clean_repos( INTERM_REPOS,  not auto )
    clean_repos( BUILD_REPOS, not auto )

    #check we are cloning LIVE -> INTERM, INTERM -> RELEASE
    print_step("\n1b: Update repositories.")
    update_repos()

    print_step("1c: Verify repositories.")
    check_repos( INTERM_REPOS, False )
    check_repos( BUILD_REPOS,  False )

    print_step("1d: Optionally replace files.")
    if not auto:
        if not prompt_yes_no("Replace any files you would like then type yes to continue"):
            raise Exception("No prompt")

    print_step("Build Step 2: Check tools.")
    check_tools( TOOLS )

    print_step("Build Step 3: Determine release versions.")
    (old_jsr308_version, jsr308_version) = jsr308_checker_framework_version( auto )
    (old_afu_version, afu_version)       = get_afu_version( auto )
    version_dirs = create_interm_version_dirs( jsr308_version, afu_version, auto )
    jsr308_interm_dir            = version_dirs[0]
    afu_interm_dir               = version_dirs[1]
    checker_framework_interm_dir = version_dirs[2]

    print_step("Build Step 4: Review changelogs.")
    if not auto:
        if projects_to_release[LT_OPT]:
            propose_changelog_edit( "JSR308 Type Annotations Compiler", JSR308_CHANGELOG, "/tmp/jsr308.changes",
                                    old_jsr308_version, JSR308_LANGTOOLS , JSR308_TAG_PREFIXES )

        if projects_to_release[AFU_OPT]:
            propose_changelog_edit( "Annotation File Utilities", AFU_CHANGELOG, "/tmp/afu.changes",
                                    old_afu_version, ANNO_TOOLS, AFU_TAG_PREFIXES  )

        if projects_to_release[CF_OPT]:
            propose_changelog_edit( "Checker Framework", CHECKER_CHANGELOG, "/tmp/checker-framework.changes",
                                    old_jsr308_version, CHECKER_FRAMEWORK, CHECKER_TAG_PREFIXES )

    print_step("Build Step 5: Review manual changes.")
    if not auto:
        if projects_to_release[LT_OPT]:
            propose_change_review( "the JSR308 manual", old_jsr308_version, JSR308_LANGTOOLS,
                                   JSR308_TAG_PREFIXES, JSR308_LT_DOC, "/tmp/jsr308.manual" )

        if projects_to_release[AFU_OPT]:
            propose_change_review( "the Annotation File Utilities manual", old_afu_version, ANNO_TOOLS,
                                   AFU_TAG_PREFIXES, AFU_MANUAL, "/tmp/afu.manual"  )

        if projects_to_release[CF_OPT]:
            propose_change_review( "the Checker Framework", old_jsr308_version, CHECKER_FRAMEWORK,
                                   CHECKER_TAG_PREFIXES, CHECKER_MANUAL, "/tmp/checker-framework.manual"  )

    print_step("Build Step 6: Build projects and websites.")
    print( projects_to_release )
    if projects_to_release[LT_OPT]:
        print_step("6a: Build Type Annotations Compiler.")
        build_jsr308_langtools_release(auto, jsr308_version, afu_date, checker_framework_interm_dir, jsr308_interm_dir)

    if projects_to_release[AFU_OPT]:
        print_step("6b: Build Annotation File Utilities.")
        build_annotation_tools_release( auto, afu_version, afu_interm_dir )

    if projects_to_release[CF_OPT]:
        print_step("6c: Build Checker Framework.")
        build_checker_framework_release(auto, jsr308_version, afu_date, checker_framework_interm_dir, jsr308_interm_dir)

    print_step("Build Step 7: Commit projects to intermediate repos.")
    commit_to_interm_projects( jsr308_version, afu_version, projects_to_release )

    print_step("\n\n8: Add permissions to websites.")
    ensure_group_access( jsr308_interm_dir )
    ensure_group_access( afu_interm_dir )
    ensure_group_access( checker_framework_interm_dir )

if __name__ == "__main__":
    sys.exit(main(sys.argv))
