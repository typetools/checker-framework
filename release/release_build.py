#!/usr/bin/env python
# encoding: utf-8
"""
release_build.py

Created by Jonathan Burke on 2013-08-01.

Copyright (c) 2015 University of Washington. All rights reserved.
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
    # MANUAL Indicates a manual step
    # SEMIAUTO Indicates a mostly automated step with possible prompts. Most of these steps become fully-automated when --auto is used.
    # AUTO Indicates the step is fully-automated.

    # Note that many prompts will cause scripts to exit if you say 'no'. This will require you to re-run
    # the script from the beginning, which may take a long time. It is better to say 'yes' to the script
    # prompts and follow the indicated steps even if they are redundant/you have done them already. Also,
    # be sure to carefully read all instructions on the command-line before typing yes. This is because
    # the scripts do not ask you to say 'yes' after each step, so you may miss a step if you only read
    # the paragraph asking you to say 'yes'.

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

    print( "NOTE: Please read all the prompts printed by this script very carefully, as their" )
    print( "contents may have changed since the last time you ran it." )

    print_step("Build Step 1: Clean and update the build and intermediate repositories.") # SEMIAUTO

    print_step("1a: Clean repositories.")

    # Recall that there are 3 relevant sets of repositories for the release:
    # * build repository - repository where the project is built for release
    # * intermediate repository - repository to which release related changes are pushed after the project is built
    # * release repository - Google code repositories, the central repository.

    # Every time we run release_build, changes are committed to the intermediate repository from build but NOT to
    # the release repositories. If we are running the build script multiple times without actually committing the
    # release then these changes need to be cleaned before we run the release_build script again. We therefore run
    # hg strip, hg purge, and hg revert on ALL repositories.

    # NOTE: See README-maintainers.html#fi_bug_fixes for errors you might experience at this step.

    clean_repos( INTERM_REPOS,  not auto )
    clean_repos( BUILD_REPOS, not auto )

    # The repositories that were cleaned of changes in the previous step need to be updated to the latest revision.
    # The intermediate repos pull from the release repos and the build repos pull from the intermediate.
    # All repos are then updated.

    #check we are cloning LIVE -> INTERM, INTERM -> RELEASE
    print_step("\n1b: Update repositories.") # SEMIAUTO
    update_repos()

    # This step ensures the previous 2 steps work. It checks to see if we have any modified files, untracked files,
    # or outgoing changesets. If so, it fails.

    print_step("1c: Verify repositories.") # SEMIAUTO
    check_repos( INTERM_REPOS, False )
    check_repos( BUILD_REPOS,  False )

    # You may wish to experiment with the build (for instance testing changes before committing them to the
    # repository). The above 3 steps will revert any of those changes you previously wrote to the build
    # repositories. At this step, the script pauses to allow you to replace any files you wish before
    # actually executing the build.

    # Note that any changes you make in this step are lost if you restart the release_build process and
    # instruct it to clean the repositories (which you should say yes to). That's why it's a good idea
    # to keep copies of the work you do here readily available in your home directory.

    print_step("1d: Optionally replace files.") # MANUAL
    if not auto:
        if not prompt_yes_no("Replace any files you would like then type yes to continue"):
            raise Exception("No prompt")

    # The release script requires a number of common tools (Ant, Maven, make, etc...). This step checks
    # to make sure all tools are available on the command line in order to avoid wasting time in the
    # event a tool is missing late in execution.

    print_step("Build Step 2: Check tools.") # AUTO
    check_tools( TOOLS )

    # Usually we increment the release by 0.0.1 per release unless there is a major change.
    # The release script will read the current version of the Checker Framework/Annotation File Utilities
    # from the release website and then suggest the next release version 0.0.1 higher than the current
    # version. You can also manually specify a version higher than the current version. Lower or equivalent
    # versions are not possible and will be rejected when you try to push the release.

    # The jsr308-langtools version ALWAYS matches the Checker Framework version.

    # NOTE: If you pass --auto on the command line then the next logical version will be chosen automatically

    print_step("Build Step 3: Determine release versions.") # SEMIAUTO
    (old_jsr308_version, jsr308_version) = jsr308_checker_framework_version( auto )
    (old_afu_version, afu_version)       = get_afu_version( auto )
    version_dirs = create_interm_version_dirs( jsr308_version, afu_version, auto )
    jsr308_interm_dir            = version_dirs[0]
    afu_interm_dir               = version_dirs[1]
    checker_framework_interm_dir = version_dirs[2]

    # All changelogs should be up-to-date before releasing. However, we manually verify this during
    # the build process. This step extracts the changesets that have occurred since the last release
    # and writes them to a file. Please look through these changesets and ensure we have not missed
    # any relevant messages in the changelog. The release script will indicate what changelog you
    # should edit in case there are missing changes. These changes will be committed when you run
    # the release_push.py script.

    print_step("Build Step 4: Review changelogs.") # SEMIAUTO

    print( "Verify that all changelog messages follow the guidelines found in README-maintainers.html#changelog_guide\n" )

    print( "Finally, ensure that the changelog ends with a line like\n" )
    print( "Resolved issues:  200, 300, 332, 336, 357, 359, 373, 374\n" )

    print( "You can compute the relevant numbers by visiting these two URLs:\n" )
    print( "https://code.google.com/p/checker-framework/issues/list?q=status%3APushed\n" )
    print( "https://code.google.com/p/checker-framework/issues/list?can=1&q=closed-after%3A2014%2F09%2F25\n" )
    print( "though you will need to adjust the date in the latter URL to be the date of the previous release.\n" )

    # If release_build fails later on and you need to restart it, I recommend you make copies of the
    # changelogs you modified on your local machine, push the changes to those changelogs, and then
    # restart the build process saying "yes" to cleaning the repositories. However, this really should
    # not happen because the changelogs need to have been updated immediately after code freeze,
    # and you should not need to update the logs again here.

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

    # This step will write out all of the changes that happened to the individual projects' manuals
    # to a temporary file. Please review these changes for errors. You can edit them in the build
    # repository and they will be committed by the release_push.py script.

    print_step("Build Step 5: Review manual changes.") # SEMIAUTO

    print("Please verify that the lists of checkers in these Checker Framework chapter sections are\n" )
    print("up to date: Introduction, Run-time tests and type refinement\n" )

    print("Please also build the manual on your local machine's enlistment and verify that the PDF\n" )
    print("does not have lines that are longer than the page width (some lines go beyond the right margin -\n")
    print("this is acceptable), except for lines containing command lines or code - some of these inevitably\n")
    print("are longer than the page width.\n")

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

    # The projects are built in the following order: JSR308-Langtools, Annotation File Utilities,
    # and Checker Framework. Furthermore, their manuals and websites are also built and placed in
    # their relevant locations at http://types.cs.washington.edu/dev/ This is the most time consuming
    # piece of the release. There are no prompts from this step forward; you might want to get a cup
    # of coffee and do something else until it is done.

    print_step("Build Step 6: Build projects and websites.") # AUTO
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

    # Each project has a set of files that are updated for release. Usually these updates include new
    # release date and version information. All changed files are committed and pushed to the intermediate
    # repositories. Keep this in mind if you have any changed files from steps 1d, 4, or 5. Edits to the
    # scripts in the jsr308-release/scripts directory will never be checked in.

    print_step("Build Step 7: Commit projects to intermediate repos.") # AUTO
    commit_to_interm_projects( jsr308_version, afu_version, projects_to_release )

    # Adds read/write/execute group permissions to all of the new dev website directories
    # under http://types.cs.washington.edu/dev/ These directories need group read/execute
    # permissions in order for them to be served.

    print_step("\n\n8: Add permissions to websites.") # AUTO
    ensure_group_access( jsr308_interm_dir )
    ensure_group_access( afu_interm_dir )
    ensure_group_access( checker_framework_interm_dir )

if __name__ == "__main__":
    sys.exit(main(sys.argv))
