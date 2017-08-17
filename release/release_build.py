#!/usr/bin/env python
# encoding: utf-8
"""
release_build.py

Created by Jonathan Burke on 2013-08-01.

Copyright (c) 2015 University of Washington. All rights reserved.
"""

# See README-release-process.html for more information

from release_vars  import *
from release_utils import *
from distutils.dir_util import copy_tree

# Turned on by the --debug command-line option.
debug = False
ant_debug = ""

# Currently only affects the Checker Framework tests, which run the longest
notest = False

def print_usage():
    """Print usage information."""
    print "Usage:    python release_build.py [projects] [options]"
    print_projects(1, 4)
    print "\n  --auto  accepts or chooses the default for all prompts"
    print "\n  --debug  turns on debugging mode which produces verbose output"
    print "\n  --notest  disables tests to speed up scripts; for debugging only"
    print "\n  --review-manual  review the documentation changes only; don't perform a full build"

def clone_or_update_repos(auto):
    """Clone the relevant repos from scratch or update them if they exist and
    if directed to do so by the user."""
    message = """Before building the release, we clone or update the release repositories.
However, if you have had to run the script multiple times and no files have changed, you may skip this step.
WARNING: IF THIS IS YOUR FIRST RUN OF THE RELEASE ON RELEASE DAY, DO NOT SKIP THIS STEP.
The following repositories will be cloned or updated from their origins:
"""
    for live_to_interm in LIVE_TO_INTERM_REPOS:
        message += live_to_interm[1] + "\n"

    for interm_to_build in INTERM_TO_BUILD_REPOS:
        message += interm_to_build[1] + "\n"

    message += PLUME_LIB + "\n"
    message += PLUME_BIB + "\n\n"

    message += "Clone repositories from scratch (answer no to be given a chance to update them instead)?"

    clone_from_scratch = True

    if not auto:
        if not prompt_yes_no(message, True):
            clone_from_scratch = False
            if not prompt_yes_no("Update the repositories without cloning them from scratch?", True):
                print "WARNING: Continuing without refreshing repositories.\n"
                return

    for live_to_interm in LIVE_TO_INTERM_REPOS:
        clone_from_scratch_or_update(live_to_interm[0], live_to_interm[1], clone_from_scratch, True)

    for interm_to_build in INTERM_TO_BUILD_REPOS:
        clone_from_scratch_or_update(interm_to_build[0], interm_to_build[1], clone_from_scratch, False)

    clone_from_scratch_or_update(LIVE_PLUME_LIB, PLUME_LIB, clone_from_scratch, False)
    clone_from_scratch_or_update(LIVE_PLUME_BIB, PLUME_BIB, clone_from_scratch, False)

def get_afu_date(building_afu):
    """If the AFU is being built, return the current date, otherwise return the
    date of the last AFU release as indicated in the AFU home page."""
    if building_afu:
        return get_current_date()
    else:
        raise Exception("Do not know how to retrieve the AFU date if not building the AFU")
        # TODO: these tags no longer exist in the AFU home page. Fix this to
        # extract the date from the afu-version tags.
        # afu_site = os.path.join(HTTP_PATH_TO_LIVE_SITE, "annotation-file-utilities")
        # return extract_from_site(afu_site, "<!-- afu-date -->", "<!-- /afu-date -->")

def get_new_version(project_name, curr_version, auto):
    "Queries the user for the new version number; returns old and new version numbers."

    print "Current " + project_name + " version: " + curr_version
    suggested_version = increment_version(curr_version)

    if auto:
        new_version = suggested_version
    else:
        new_version = prompt_w_default("Enter new version", suggested_version, "^\\d+\\.\\d+(?:\\.\\d+){0,2}$")

    print "New version: " + new_version

    if curr_version == new_version:
        curr_version = prompt_w_default("Enter current version", suggested_version, "^\\d+\\.\\d+(?:\\.\\d+){0,2}$")
        print "Current version: " + curr_version

    return (curr_version, new_version)

def create_dev_website_release_version_dir(project_name, version):
    """Create the directory for the given version of the given project under
    the releases directory of the dev web site."""
    if project_name == None or project_name == "checker-framework":
        interm_dir = os.path.join(FILE_PATH_TO_DEV_SITE, "releases", version)
    else:
        interm_dir = os.path.join(FILE_PATH_TO_DEV_SITE, project_name, "releases", version)
    delete_path_if_exists(interm_dir)

    execute("mkdir -p %s" % interm_dir, True, False)
    return interm_dir

def create_dirs_for_dev_website_release_versions(jsr308_and_cf_version, afu_version):
    """Create directories for the given versions of the JSR308, CF, and AFU
    projects under the releases directory of the dev web site.
    For example,
    /cse/www2/types/dev/checker-framework/<project_name>/releases/<version> ."""
    jsr308_interm_dir = create_dev_website_release_version_dir("jsr308", jsr308_and_cf_version)
    afu_interm_dir = create_dev_website_release_version_dir("annotation-file-utilities", afu_version)
    checker_framework_interm_dir = create_dev_website_release_version_dir(None, jsr308_and_cf_version)

    return (jsr308_interm_dir, afu_interm_dir, checker_framework_interm_dir)

### def update_project_dev_website_symlink(project_name, release_version):
###     """Update the \"current\" symlink in the dev web site for the given project
###     to point to the given release of the project on the dev web site."""
###     project_dev_site = os.path.join(FILE_PATH_TO_DEV_SITE, project_name)
###     link_path = os.path.join(project_dev_site, "current")
###
###     dev_website_relative_dir = os.path.join("releases", release_version)
###
###     print "Writing symlink: " + link_path + "\nto point to relative directory: " + dev_website_relative_dir
###     force_symlink(dev_website_relative_dir, link_path)

def update_project_dev_website(project_name, release_version):
    """Update the dev web site for the given project
    according to the given release of the project on the dev web site."""
    if project_name == "checker-framework":
        project_dev_site = FILE_PATH_TO_DEV_SITE
    else:
        project_dev_site = os.path.join(FILE_PATH_TO_DEV_SITE, project_name)
    dev_website_relative_dir = os.path.join(project_dev_site, "releases", release_version)

    print "Copying from : " + dev_website_relative_dir + "\nto: " + project_dev_site
    copy_tree(dev_website_relative_dir, project_dev_site)


def build_jsr308_langtools_release(version, afu_version, afu_release_date, jsr308_interm_dir):
    """Build the jsr308-langtools project's artifacts and place them in the
    development web site."""

    afu_build_properties = os.path.join(ANNO_FILE_UTILITIES, "build.properties")

    # update jsr308_langtools versions
    ant_props = "-Dlangtools=%s -Drelease.ver=%s -Dafu.version=%s -Dafu.properties=%s -Dafu.release.date=\"%s\"" % (JSR308_LANGTOOLS, version, afu_version, afu_build_properties, afu_release_date)
    # IMPORTANT: The release.xml in the directory where the Checker Framework is being built is used. Not the release.xml in the directory you ran release_build.py from.
    ant_cmd = "ant %s -f release.xml %s update-langtools-versions " % (ant_debug, ant_props)
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    # TODO: perhaps make a "dist" target rather than listing out the relevant targets
    # build jsr308 binaries and documents but not website, fail if the tests don't pass
    ant_cmd = "ant %s -Dhalt.on.test.failure=true -Dlauncher.java=java clean-and-build-all-tools build-javadoc build-doclets" % (ant_debug)
    execute(ant_cmd, True, False, JSR308_MAKE)

    jsr308ZipName = "jsr308-langtools-%s.zip" % version

    # zip up jsr308-langtools project and place it in jsr308_interm_dir
    ant_props = "-Dlangtools=%s  -Dcheckerframework=%s -Ddest.dir=%s -Dfile.name=%s -Dversion=%s" % (JSR308_LANGTOOLS, CHECKER_FRAMEWORK, jsr308_interm_dir, jsr308ZipName, version)
    # IMPORTANT: The release.xml in the directory where the Checker Framework is being built is used. Not the release.xml in the directory you ran release_build.py from.
    ant_cmd = "ant %s -f release.xml %s zip-langtools " % (ant_debug, ant_props)
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    # build jsr308 website
    make_cmd = "make jsr308_www_dir=%s jsr308_url=%s web-no-checks" % (jsr308_interm_dir, HTTP_PATH_TO_DEV_SITE)
    execute(make_cmd, True, False, JSR308_LT_DOC)

    # copy remaining website files to jsr308_interm_dir
    ant_props = "-Dlangtools=%s -Ddest.dir=%s" % (JSR308_LANGTOOLS, jsr308_interm_dir)
    # IMPORTANT: The release.xml in the directory where the Checker Framework is being built is used. Not the release.xml in the directory you ran release_build.py from.
    ant_cmd = "ant %s -f release.xml %s langtools-website-docs " % (ant_debug, ant_props)
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    update_project_dev_website("jsr308", version)

    return

def get_current_date():
    "Return today's date in a string format similar to: 02 May 2016"
    return CURRENT_DATE.strftime("%d %b %Y")


def build_annotation_tools_release(version, afu_interm_dir):
    """Build the Annotation File Utilities project's artifacts and place them
    in the development web site."""
    execute('java -version', True)

    date = get_current_date()

    build = os.path.join(ANNO_FILE_UTILITIES, "build.xml")
    ant_cmd = "ant %s -buildfile %s -e update-versions -Drelease.ver=\"%s\" -Drelease.date=\"%s\"" % (ant_debug, build, version, date)
    execute(ant_cmd)

    # Deploy to intermediate site
    ant_cmd = "ant %s -buildfile %s -e web-no-checks -Dafu.version=%s -Ddeploy-dir=%s" % (ant_debug, build, version, afu_interm_dir)
    execute(ant_cmd)

    update_project_dev_website("annotation-file-utilities", version)

def build_and_locally_deploy_maven(version):
    protocol_length = len("file://")
    maven_dev_repo_without_protocol = MAVEN_DEV_REPO[protocol_length:]

    execute("mkdir -p " + maven_dev_repo_without_protocol)

    # Deploy jsr308 and checker-qual jars to maven repo
    mvn_deploy(CHECKER_BINARY, CHECKER_BINARY_POM, MAVEN_DEV_REPO)
    mvn_deploy(CHECKER_QUAL, CHECKER_QUAL_POM, MAVEN_DEV_REPO)
    mvn_deploy(JAVAC_BINARY, JAVAC_BINARY_POM, MAVEN_DEV_REPO)
    mvn_deploy(JDK8_BINARY, JDK8_BINARY_POM, MAVEN_DEV_REPO)

    return

def build_checker_framework_release(version, afu_version, afu_release_date, checker_framework_interm_dir, manual_only=False):
    """Build the release files for the Checker Framework project, including the
    manual and the zip file, and run tests on the build."""
    checker_dir = os.path.join(CHECKER_FRAMEWORK, "checker")

    afu_build_properties = os.path.join(ANNO_FILE_UTILITIES, "build.properties")

    # update jsr308_langtools versions
    ant_props = "-Dchecker=%s -Drelease.ver=%s -Dafu.version=%s -Dafu.properties=%s -Dafu.release.date=\"%s\"" % (checker_dir, version, afu_version, afu_build_properties, afu_release_date)
    # IMPORTANT: The release.xml in the directory where the Checker Framework is being built is used. Not the release.xml in the directory you ran release_build.py from.
    ant_cmd = "ant %s -f release.xml %s update-checker-framework-versions " % (ant_debug, ant_props)
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    if not manual_only:
        # ensure all PluginUtil.java files are identical
        execute("./checkPluginUtil.sh", True, False, CHECKER_FRAMEWORK_RELEASE)

        # build the checker framework binaries and documents, run checker framework tests
        if notest:
            ant_cmd = "ant %s -Dhalt.on.test.failure=true dist-release-notest" % (ant_debug)
        else:
            ant_cmd = "ant %s -Dhalt.on.test.failure=true dist-release" % (ant_debug)
        execute(ant_cmd, True, False, CHECKER_FRAMEWORK)

    # make the Checker Framework Manual
    checker_manual_dir = os.path.join(CHECKER_FRAMEWORK, "docs", "manual")
    execute("make manual.pdf manual.html", True, False, checker_manual_dir)

    if not manual_only:

        # make the dataflow manual
        dataflow_manual_dir = os.path.join(CHECKER_FRAMEWORK, "dataflow", "manual")
        execute("make", True, False, dataflow_manual_dir)

        # make the checker framework tutorial
        checker_tutorial_dir = os.path.join(CHECKER_FRAMEWORK, "docs", "tutorial")
        execute("make", True, False, checker_tutorial_dir)

        cfZipName = "checker-framework-%s.zip" % version

        # Create checker-framework-X.Y.Z.zip and put it in checker_framework_interm_dir
        ant_props = "-Dchecker=%s -Ddest.dir=%s -Dfile.name=%s -Dversion=%s" % (checker_dir, checker_framework_interm_dir, cfZipName, version)
        # IMPORTANT: The release.xml in the directory where the Checker Framework is being built is used. Not the release.xml in the directory you ran release_build.py from.
        ant_cmd = "ant %s -f release.xml %s zip-checker-framework " % (ant_debug, ant_props)
        execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

        ant_props = "-Dchecker=%s -Ddest.dir=%s -Dfile.name=%s -Dversion=%s" % (checker_dir, checker_framework_interm_dir, "mvn-examples.zip", version)
        # IMPORTANT: The release.xml in the directory where the Checker Framework is being built is used. Not the release.xml in the directory you ran release_build.py from.
        ant_cmd = "ant %s -f release.xml %s zip-maven-examples " % (ant_debug, ant_props)
        execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

        # copy the remaining checker-framework website files to checker_framework_interm_dir
        ant_props = "-Dchecker=%s -Ddest.dir=%s -Dmanual.name=%s -Ddataflow.manual.name=%s -Dchecker.webpage=%s" % (
            checker_dir, checker_framework_interm_dir, "checker-framework-manual",
            "checker-framework-dataflow-manual", "checker-framework-webpage.html"
        )

        # IMPORTANT: The release.xml in the directory where the Checker Framework is being built is used. Not the release.xml in the directory you ran release_build.py from.
        ant_cmd = "ant %s -f release.xml %s checker-framework-website-docs " % (ant_debug, ant_props)
        execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

        # clean no longer necessary files left over from building the checker framework tutorial
        checker_tutorial_dir = os.path.join(CHECKER_FRAMEWORK, "docs", "tutorial")
        execute("make clean", True, False, checker_tutorial_dir)

        build_and_locally_deploy_maven(version)

        update_project_dev_website("checker-framework", version)

    return

def commit_to_interm_projects(jsr308_version, afu_version, projects_to_release):
    """Commit the changes for each project from its build repo to its
    corresponding intermediate repo in preparation for running the release_push
    script, which does not read the build repos."""
    # Use project definition instead, see find project location find_project_locations
    if projects_to_release[LT_OPT]:
        commit_tag_and_push(jsr308_version, JSR308_LANGTOOLS, "jsr308-")

    if projects_to_release[AFU_OPT]:
        commit_tag_and_push(afu_version, ANNO_TOOLS, "")

    if projects_to_release[CF_OPT]:
        commit_tag_and_push(jsr308_version, CHECKER_FRAMEWORK, "checker-framework-")

def main(argv):
    """The release_build script is responsible for building the release
    artifacts for jsr308-langtools, the AFU and the Checker Framework projects
    and placing them in the development web site. It can also be used to review
    the documentation and changelogs for the three projects."""
    # MANUAL Indicates a manual step
    # SEMIAUTO Indicates a mostly automated step with possible prompts. Most of these steps become fully automated when --auto is used.
    # AUTO Indicates the step is fully automated.

    delete_if_exists(RELEASE_BUILD_COMPLETED_FLAG_FILE)

    set_umask()

    projects_to_release = read_projects(argv, print_usage)

    # Check for a --auto
    # If --auto then no prompt and just build a full release
    # Otherwise provide all prompts
    auto = read_command_line_option(argv, "--auto")
    global debug
    global ant_debug
    debug = read_command_line_option(argv, "--debug")
    if debug:
        ant_debug = "-debug"
    global notest
    notest = read_command_line_option(argv, "--notest")

    # Indicates whether to review documentation changes only and not perform a build.
    review_documentation = read_command_line_option(argv, "--review-manual")
    add_project_dependencies(projects_to_release)

    afu_date = get_afu_date(projects_to_release[AFU_OPT])

    # For each project, build what is necessary but don't push

    if not review_documentation:
        print "Building a new release of Langtools, Annotation Tools, and the Checker Framework!"
    else:
        print "Reviewing the documentation for Langtools, Annotation Tools, and the Checker Framework."

    print "\nPATH:\n" + os.environ['PATH'] + "\n"

    print_step("Build Step 1: Clone the build and intermediate repositories.") # SEMIAUTO

    # Recall that there are 3 relevant sets of repositories for the release:
    # * build repository - repository where the project is built for release
    # * intermediate repository - repository to which release related changes are pushed after the project is built
    # * release repository - GitHub/Bitbucket repositories, the central repository.

    # Every time we run release_build, changes are committed to the intermediate repository from build but NOT to
    # the release repositories. If we are running the build script multiple times without actually committing the
    # release then these changes need to be cleaned before we run the release_build script again.
    # The "Clone/update repositories" step updates the repositories with respect to the live repositories on
    # GitHub/Bitbucket, but it is the "Verify repositories" step that ensures that they are clean,
    # i.e. indistinguishable from a freshly cloned repository.

    # check we are cloning LIVE -> INTERM, INTERM -> RELEASE
    print_step("\n1a: Clone/update repositories.") # SEMIAUTO
    clone_or_update_repos(auto)

    # This step ensures the previous step worked. It checks to see if we have any modified files, untracked files,
    # or outgoing changesets. If so, it fails.

    print_step("1b: Verify repositories.") # SEMIAUTO
    check_repos(INTERM_REPOS, True, True)
    check_repos(BUILD_REPOS, True, False)

    # The release script requires a number of common tools (Ant, Maven, make, etc...). This step checks
    # to make sure all tools are available on the command line in order to avoid wasting time in the
    # event a tool is missing late in execution.

    print_step("Build Step 2: Check tools.") # AUTO
    check_tools(TOOLS)

    # Usually we increment the release by 0.0.1 per release unless there is a major change.
    # The release script will read the current version of the Checker Framework/Annotation File Utilities
    # from the release website and then suggest the next release version 0.0.1 higher than the current
    # version. You can also manually specify a version higher than the current version. Lower or equivalent
    # versions are not possible and will be rejected when you try to push the release.

    # The jsr308-langtools version ALWAYS matches the Checker Framework version.

    # NOTE: If you pass --auto on the command line then the next logical version will be chosen automatically

    print_step("Build Step 3: Determine release versions.") # SEMIAUTO

    print("Increment the minor version rather than just the patch level if:\n")
    print(" * any new checkers have been added,\n")
    print(" * the patch level is 9 (keep the patch level as a single digit), or\n")
    print(" * backward-incompatible changes have been made to APIs or elsewhere.\n")

    old_jsr308_version = current_distribution(CHECKER_FRAMEWORK)
    (old_jsr308_version, jsr308_version) = get_new_version("JSR308/Checker Framework", old_jsr308_version, auto)

    if old_jsr308_version == jsr308_version:
        print("It is *strongly discouraged* to not update the release version numbers for the Checker Framework " +
              "and jsr308-langtools even if no changes were made to these in a month. This would break so much " +
              "in the release scripts that they would become unusable.\n")
        prompt_to_continue()

    old_afu_version = get_afu_version_from_html(AFU_MANUAL)
    (old_afu_version, afu_version) = get_new_version("Annotation File Utilities", old_afu_version, auto)

    if old_afu_version == afu_version:
        print("The AFU version has not changed. It is recommended to include a small bug fix or doc update in every " +
              "AFU release so the version number can be updated, but when that is not possible, before and after running " +
              "release_build, you must:\n" +
              "-Ensure that you are subscribed to the AFU push notifications mailing list.\n" +
              "-Verify that the AFU changelog has not been changed.\n" +
              "-Grep all the AFU pages on the dev web site for the release date with patterns such as \"29.*Aug\" " +
              "and \"Aug.*29\" and fix them to match the previous release date.\n" +
              "Keep in mind that in this case, the release scripts will fail in certain places and you must manually " +
              "follow a few remaining release steps.\n")
        prompt_to_continue()

    if review_documentation:
        print_step("Build Step 4: Review changelogs.") # SEMIAUTO

        print "Verify that all changelog messages follow the guidelines found in README-release-process.html#changelog_guide\n"

        print "Ensure that the changelogs end with a line that lists resolved issues in numerical order, like"
        print "Resolved issues:  200, 300, 332, 336, 357, 359, 373, 374.\n"

        print("To ensure the jsr308-langtools, AFU and Checker Framework changelogs are correct and complete, " +
              "please follow the Content Guidelines found in README-release-process.html#content_guidelines\n")

        prompt_to_continue()

        # This step will write out all of the changes that happened to the individual projects' documentation
        # to temporary files. Please review these changes for errors.

        print_step("Build Step 5: Review documentation changes.") # SEMIAUTO

        print "Please review the documentation changes since the last release to ensure that"
        print " * All new features mentioned in the manuals appear in the changelogs, and"
        print " * All new features mentioned in the changelogs are documented in the manuals."
        print ""

        if projects_to_release[LT_OPT]:
            propose_documentation_change_review("the JSR308 documentation updates", old_jsr308_version, JSR308_LANGTOOLS,
                                                JSR308_TAG_PREFIXES, JSR308_LT_DOC, TMP_DIR + "/jsr308.manual")

        if projects_to_release[AFU_OPT]:
            propose_documentation_change_review("the Annotation File Utilities documentation updates", old_afu_version, ANNO_TOOLS,
                                                AFU_TAG_PREFIXES, AFU_MANUAL, TMP_DIR + "/afu.manual")

        if projects_to_release[CF_OPT]:
            build_checker_framework_release(jsr308_version, afu_version, afu_date, "checker-framework", manual_only=True)

            print ""
            print "The built Checker Framework manual (HTML and PDF) can be found at " + CHECKER_MANUAL
            print ""
            print "Verify that the manual PDF has no lines that are longer than the page width"
            print "(it is acceptable for some lines to extend into the right margin)."
            print ""
            print "If any checkers have been added or removed, then verify that the lists"
            print "of checkers in these manual sections are up to date:"
            print " * Introduction"
            print " * Run-time tests and type refinement"
            print "and make sure that the checkers supported in the Eclipse plug-in are up to date"
            print "by following the instructions at eclipse/README-developers.html#update_checkers"
            print ""

            propose_documentation_change_review("the Checker Framework documentation updates", old_jsr308_version, CHECKER_FRAMEWORK,
                                                CHECKER_TAG_PREFIXES, CHECKER_MANUAL, TMP_DIR + "/checker-framework.manual")

        return

    ## I don't think this should be necessary in general.  It's just to put files in place no link checking will work, and it takes a loooong time to run.
    # print_step("Build Step 4: Copy entire live site to dev site (~22 minutes).") # SEMIAUTO

    # if auto or prompt_yes_no("Proceed with copy of live site to dev site?", True):
    #     # ************************************************************************************************
    #     # WARNING: BE EXTREMELY CAREFUL WHEN MODIFYING THIS COMMAND.  The --delete option is destructive
    #     # and its work cannot be undone.  If, for example, this command were modified to accidentally make
    #     # /cse/www2/types/ the target directory, the entire types directory could be wiped out.
    #     execute("rsync --omit-dir-times --recursive --links --delete --quiet --exclude=dev --exclude=sparta/release/versions /cse/www2/types/ /cse/www2/types/dev")
    #     # ************************************************************************************************

    print_step("Build Step 5: Create directories for the current release on the dev site.") # AUTO

    (jsr308_interm_dir, afu_interm_dir, checker_framework_interm_dir) = \
        create_dirs_for_dev_website_release_versions(jsr308_version, afu_version)

    # The projects are built in the following order: JSR308-Langtools,
    # Annotation File Utilities, and Checker Framework. Furthermore, their
    # manuals and websites are also built and placed in their relevant locations
    # at https://checkerframework.org/dev/ .  This is the most time-consuming
    # piece of the release. There are no prompts from this step forward; you
    # might want to get a cup of coffee and do something else until it is done.

    print_step("Build Step 6: Build projects and websites.") # AUTO
    print projects_to_release
    if projects_to_release[LT_OPT]:
        print_step("6a: Build Type Annotations Compiler.")
        build_jsr308_langtools_release(jsr308_version, afu_version, afu_date, jsr308_interm_dir)

    if projects_to_release[AFU_OPT]:
        print_step("6b: Build Annotation File Utilities.")
        build_annotation_tools_release(afu_version, afu_interm_dir)

    if projects_to_release[CF_OPT]:
        print_step("6c: Build Checker Framework.")
        build_checker_framework_release(jsr308_version, afu_version, afu_date, checker_framework_interm_dir)


    print_step("Build Step 7: Overwrite .htaccess and CFLogo.png .") # AUTO

    # Not "cp -p" because that does not work across filesystems whereas rsync does
    execute("rsync --times %s %s" % (CFLOGO, checker_framework_interm_dir))

    # Each project has a set of files that are updated for release. Usually these updates include new
    # release date and version information. All changed files are committed and pushed to the intermediate
    # repositories. Keep this in mind if you have any changed files from steps 1d, 4, or 5. Edits to the
    # scripts in the jsr308-release/scripts directory will never be checked in.

    print_step("Build Step 8: Commit projects to intermediate repos.") # AUTO
    commit_to_interm_projects(jsr308_version, afu_version, projects_to_release)

    # Adds read/write/execute group permissions to all of the new dev website directories
    # under https://checkerframework.org/dev/ These directories need group read/execute
    # permissions in order for them to be served.

    print_step("\n\nBuild Step 9: Add group permissions to repos.")
    for build in BUILD_REPOS:
        ensure_group_access(build)

    for interm in INTERM_REPOS:
        ensure_group_access(interm)

    # At the moment, this will lead to output error messages because some metadata in some of the
    # dirs I think is owned by Mike or Werner.  We should identify these and have them fix it.
    # But as long as the processes return a zero exit status, we should be ok.
    print_step("\n\nBuild Step 10: Add group permissions to websites.") # AUTO
    ensure_group_access(FILE_PATH_TO_DEV_SITE)

    create_empty_file(RELEASE_BUILD_COMPLETED_FLAG_FILE)

if __name__ == "__main__":
    sys.exit(main(sys.argv))
