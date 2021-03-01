#!/usr/bin/env python3
# encoding: utf-8
"""
release_build.py

Created by Jonathan Burke on 2013-08-01.

Copyright (c) 2015 University of Washington. All rights reserved.
"""

# See README-release-process.html for more information

from release_vars import *
from release_utils import *

from distutils.dir_util import copy_tree

# Turned on by the --debug command-line option.
debug = False
ant_debug = ""

# Currently only affects the Checker Framework tests, which run the longest
notest = False


def print_usage():
    """Print usage information."""
    print("Usage:    python3 release_build.py [projects] [options]")
    print_projects(1, 4)
    print("\n  --debug  turns on debugging mode which produces verbose output")
    print("\n  --notest  disables tests to speed up scripts; for debugging only")


def clone_or_update_repos():
    """Clone the relevant repos from scratch or update them if they exist and
    if directed to do so by the user."""
    message = """Before building the release, we clone or update the release repositories.
However, if you have had to run the script multiple times today and no files
have changed since the last attempt, you may skip this step.
WARNING: IF THIS IS YOUR FIRST RUN OF THE RELEASE ON RELEASE DAY, DO NOT SKIP THIS STEP.
The following repositories will be cloned or updated from their origins:
"""
    for live_to_interm in LIVE_TO_INTERM_REPOS:
        message += live_to_interm[1] + "\n"

    for interm_to_build in INTERM_TO_BUILD_REPOS:
        message += interm_to_build[1] + "\n"

    message += PLUME_SCRIPTS + "\n"
    message += CHECKLINK + "\n"
    message += PLUME_BIB + "\n"
    message += STUBPARSER + "\n\n"

    message += "Clone repositories from scratch (answer no to be given a chance to update them instead)?"

    clone_from_scratch = True

    if not prompt_yes_no(message, True):
        clone_from_scratch = False
        if not prompt_yes_no(
            "Update the repositories without cloning them from scratch?", True
        ):
            print("WARNING: Continuing without refreshing repositories.\n")
            return

    for live_to_interm in LIVE_TO_INTERM_REPOS:
        clone_from_scratch_or_update(
            live_to_interm[0], live_to_interm[1], clone_from_scratch, True
        )

    for interm_to_build in INTERM_TO_BUILD_REPOS:
        clone_from_scratch_or_update(
            interm_to_build[0], interm_to_build[1], clone_from_scratch, False
        )

    clone_from_scratch_or_update(
        LIVE_PLUME_SCRIPTS, PLUME_SCRIPTS, clone_from_scratch, False
    )
    clone_from_scratch_or_update(LIVE_CHECKLINK, CHECKLINK, clone_from_scratch, False)
    clone_from_scratch_or_update(LIVE_PLUME_BIB, PLUME_BIB, clone_from_scratch, False)
    clone_from_scratch_or_update(LIVE_STUBPARSER, STUBPARSER, clone_from_scratch, False)
    # clone_from_scratch_or_update(LIVE_ANNO_REPO, ANNO_TOOLS, clone_from_scratch, False)


def get_afu_date(building_afu):
    """If the AFU is being built, return the current date, otherwise return the
    date of the last AFU release as indicated in the AFU home page."""
    if building_afu:
        return get_current_date()
    else:
        raise Exception(
            "Do not know how to retrieve the AFU date if not building the AFU"
        )
        # TODO: these tags no longer exist in the AFU home page. Fix this to
        # extract the date from the afu-version tags.
        # afu_site = os.path.join(HTTP_PATH_TO_LIVE_SITE, "annotation-file-utilities")
        # return extract_from_site(afu_site, "<!-- afu-date -->", "<!-- /afu-date -->")


def get_new_version(project_name, curr_version):
    "Queries the user for the new version number; returns old and new version numbers."

    print("Current " + project_name + " version: " + curr_version)
    suggested_version = increment_version(curr_version)

    new_version = prompt_w_default(
        "Enter new version", suggested_version, "^\\d+\\.\\d+(?:\\.\\d+){0,2}$"
    )

    print("New version: " + new_version)

    if curr_version == new_version:
        curr_version = prompt_w_default(
            "Enter current version", suggested_version, "^\\d+\\.\\d+(?:\\.\\d+){0,2}$"
        )
        print("Current version: " + curr_version)

    return (curr_version, new_version)


def create_dev_website_release_version_dir(project_name, version):
    """Create the directory for the given version of the given project under
    the releases directory of the dev web site."""
    if project_name in (None, "checker-framework"):
        interm_dir = os.path.join(FILE_PATH_TO_DEV_SITE, "releases", version)
    else:
        interm_dir = os.path.join(
            FILE_PATH_TO_DEV_SITE, project_name, "releases", version
        )
    delete_path_if_exists(interm_dir)

    execute("mkdir -p %s" % interm_dir, True, False)
    return interm_dir


def create_dirs_for_dev_website_release_versions(cf_version, afu_version):
    """Create directories for the given versions of the CF, and AFU
    projects under the releases directory of the dev web site.
    For example,
    /cse/www2/types/dev/checker-framework/<project_name>/releases/<version> ."""
    afu_interm_dir = create_dev_website_release_version_dir(
        "annotation-file-utilities", afu_version
    )
    checker_framework_interm_dir = create_dev_website_release_version_dir(
        None, cf_version
    )

    return (afu_interm_dir, checker_framework_interm_dir)


# def update_project_dev_website_symlink(project_name, release_version):
#     """Update the \"current\" symlink in the dev web site for the given project
#     to point to the given release of the project on the dev web site."""
#     project_dev_site = os.path.join(FILE_PATH_TO_DEV_SITE, project_name)
#     link_path = os.path.join(project_dev_site, "current")
#
#     dev_website_relative_dir = os.path.join("releases", release_version)
#
#     print "Writing symlink: " + link_path + "\nto point to relative directory: " + dev_website_relative_dir
#     force_symlink(dev_website_relative_dir, link_path)


def update_project_dev_website(project_name, release_version):
    """Update the dev web site for the given project
    according to the given release of the project on the dev web site."""
    if project_name == "checker-framework":
        project_dev_site = FILE_PATH_TO_DEV_SITE
    else:
        project_dev_site = os.path.join(FILE_PATH_TO_DEV_SITE, project_name)
    dev_website_relative_dir = os.path.join(
        project_dev_site, "releases", release_version
    )

    print("Copying from : " + dev_website_relative_dir + "\nto: " + project_dev_site)
    copy_tree(dev_website_relative_dir, project_dev_site)


def get_current_date():
    "Return today's date in a string format similar to: 02 May 2016"
    return datetime.date.today().strftime("%d %b %Y")


def build_annotation_tools_release(version, afu_interm_dir):
    """Build the Annotation File Utilities project's artifacts and place them
    in the development web site."""
    execute("java -version", True)

    date = get_current_date()

    build = os.path.join(ANNO_FILE_UTILITIES, "build.xml")
    ant_cmd = (
        'ant %s -buildfile %s -e update-versions -Drelease.ver="%s" -Drelease.date="%s"'
        % (ant_debug, build, version, date)
    )
    execute(ant_cmd)

    # Deploy to intermediate site
    gradle_cmd = "./gradlew releaseBuild -Pafu.version=%s -Pdeploy-dir=%s" % (
        version,
        afu_interm_dir,
    )
    execute(gradle_cmd, True, False, ANNO_FILE_UTILITIES)

    update_project_dev_website("annotation-file-utilities", version)


def build_and_locally_deploy_maven(version):
    execute("./gradlew publishToMavenLocal", working_dir=CHECKER_FRAMEWORK)


def build_checker_framework_release(
    version, old_cf_version, afu_version, afu_release_date, checker_framework_interm_dir
):
    """Build the release files for the Checker Framework project, including the
    manual and the zip file, and run tests on the build."""
    checker_dir = os.path.join(CHECKER_FRAMEWORK, "checker")

    afu_build_properties = os.path.join(ANNO_FILE_UTILITIES, "build.properties")

    # build stubparser
    execute("mvn package -Dmaven.test.skip=true", True, False, STUBPARSER)

    # build annotation-tools
    execute("./gradlew assemble -Prelease=true", True, False, ANNO_FILE_UTILITIES)

    # update versions
    ant_props = (
        '-Dchecker=%s -Drelease.ver=%s -Dafu.version=%s -Dafu.properties=%s -Dafu.release.date="%s"'
        % (checker_dir, version, afu_version, afu_build_properties, afu_release_date)
    )
    # IMPORTANT: The release.xml in the directory where the Checker Framework is being built is used. Not the release.xml in the directory you ran release_build.py from.
    ant_cmd = "ant %s -f release.xml %s update-checker-framework-versions " % (
        ant_debug,
        ant_props,
    )
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    # Check that updating versions didn't overlook anything.
    print("Here are occurrences of the old version number, " + old_cf_version)
    grep_cmd = "grep -r --exclude-dir=build --exclude-dir=.git -F %s" % old_cf_version
    execute(grep_cmd, False, False, CHECKER_FRAMEWORK)
    continue_or_exit(
        'If any occurrence is not acceptable, then stop the release, update target "update-checker-framework-versions" in file release.xml, and start over.'
    )

    # build the checker framework binaries and documents, run checker framework tests
    if notest:
        ant_cmd = "./gradlew releaseBuild"
    else:
        ant_cmd = "./gradlew releaseAndTest"
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK)

    # make the Checker Framework Manual
    checker_manual_dir = os.path.join(CHECKER_FRAMEWORK, "docs", "manual")
    execute("make manual.pdf manual.html", True, False, checker_manual_dir)

    # make the dataflow manual
    dataflow_manual_dir = os.path.join(CHECKER_FRAMEWORK, "dataflow", "manual")
    execute("make", True, False, dataflow_manual_dir)

    # make the checker framework tutorial
    checker_tutorial_dir = os.path.join(CHECKER_FRAMEWORK, "docs", "tutorial")
    execute("make", True, False, checker_tutorial_dir)

    cfZipName = "checker-framework-%s.zip" % version

    # Create checker-framework-X.Y.Z.zip and put it in checker_framework_interm_dir
    ant_props = "-Dchecker=%s -Ddest.dir=%s -Dfile.name=%s -Dversion=%s" % (
        checker_dir,
        checker_framework_interm_dir,
        cfZipName,
        version,
    )
    # IMPORTANT: The release.xml in the directory where the Checker Framework is being built is used. Not the release.xml in the directory you ran release_build.py from.
    ant_cmd = "ant %s -f release.xml %s zip-checker-framework " % (ant_debug, ant_props)
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    ant_props = "-Dchecker=%s -Ddest.dir=%s -Dfile.name=%s -Dversion=%s" % (
        checker_dir,
        checker_framework_interm_dir,
        "mvn-examples.zip",
        version,
    )
    # IMPORTANT: The release.xml in the directory where the Checker Framework is being built is used. Not the release.xml in the directory you ran release_build.py from.
    ant_cmd = "ant %s -f release.xml %s zip-maven-examples " % (ant_debug, ant_props)
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    # copy the remaining checker-framework website files to checker_framework_interm_dir
    ant_props = "-Dchecker=%s -Ddest.dir=%s -Dmanual.name=%s -Ddataflow.manual.name=%s -Dchecker.webpage=%s" % (
        checker_dir,
        checker_framework_interm_dir,
        "checker-framework-manual",
        "checker-framework-dataflow-manual",
        "checker-framework-webpage.html",
    )

    # IMPORTANT: The release.xml in the directory where the Checker Framework is being built is used. Not the release.xml in the directory you ran release_build.py from.
    ant_cmd = "ant %s -f release.xml %s checker-framework-website-docs " % (
        ant_debug,
        ant_props,
    )
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    # clean no longer necessary files left over from building the checker framework tutorial
    checker_tutorial_dir = os.path.join(CHECKER_FRAMEWORK, "docs", "tutorial")
    execute("make clean", True, False, checker_tutorial_dir)

    build_and_locally_deploy_maven(version)

    update_project_dev_website("checker-framework", version)

    return


def commit_to_interm_projects(cf_version, afu_version, projects_to_release):
    """Commit the changes for each project from its build repo to its
    corresponding intermediate repo in preparation for running the release_push
    script, which does not read the build repos."""
    # Use project definition instead, see find project location find_project_locations

    if projects_to_release[AFU_OPT]:
        commit_tag_and_push(afu_version, ANNO_TOOLS, "")

    if projects_to_release[CF_OPT]:
        commit_tag_and_push(cf_version, CHECKER_FRAMEWORK, "checker-framework-")


def main(argv):
    """The release_build script is responsible for building the release
    artifacts for the AFU and the Checker Framework projects
    and placing them in the development web site. It can also be used to review
    the documentation and changelogs for the three projects."""
    # MANUAL Indicates a manual step
    # AUTO Indicates the step is fully automated.

    delete_if_exists(RELEASE_BUILD_COMPLETED_FLAG_FILE)

    set_umask()

    projects_to_release = read_projects(argv, print_usage)

    global debug
    global ant_debug
    debug = read_command_line_option(argv, "--debug")
    if debug:
        ant_debug = "-debug"
    global notest
    notest = read_command_line_option(argv, "--notest")

    # Indicates whether to review documentation changes only and not perform a build.
    add_project_dependencies(projects_to_release)

    afu_date = get_afu_date(projects_to_release[AFU_OPT])

    # For each project, build what is necessary but don't push

    print("Building a new release of Annotation Tools and the Checker Framework!")

    print("\nPATH:\n" + os.environ["PATH"] + "\n")

    print_step("Build Step 1: Clone the build and intermediate repositories.")  # MANUAL

    # Recall that there are 3 relevant sets of repositories for the release:
    # * build repository - repository where the project is built for release
    # * intermediate repository - repository to which release related changes are pushed after the project is built
    # * release repository - GitHub repositories, the central repository.

    # Every time we run release_build, changes are committed to the intermediate repository from build but NOT to
    # the release repositories. If we are running the build script multiple times without actually committing the
    # release then these changes need to be cleaned before we run the release_build script again.
    # The "Clone/update repositories" step updates the repositories with respect to the live repositories on
    # GitHub, but it is the "Verify repositories" step that ensures that they are clean,
    # i.e. indistinguishable from a freshly cloned repository.

    # check we are cloning LIVE -> INTERM, INTERM -> RELEASE
    print_step("\n1a: Clone/update repositories.")  # MANUAL
    clone_or_update_repos()

    # This step ensures the previous step worked. It checks to see if we have any modified files, untracked files,
    # or outgoing changesets. If so, it fails.

    print_step("1b: Verify repositories.")  # MANUAL
    check_repos(INTERM_REPOS, True, True)
    check_repos(BUILD_REPOS, True, False)

    # The release script requires a number of common tools (Ant, Maven, make, etc...). This step checks
    # to make sure all tools are available on the command line in order to avoid wasting time in the
    # event a tool is missing late in execution.

    print_step("Build Step 2: Check tools.")  # AUTO
    check_tools(TOOLS)

    # Usually we increment the release by 0.0.1 per release unless there is a major change.
    # The release script will read the current version of the Checker Framework/Annotation File Utilities
    # from the release website and then suggest the next release version 0.0.1 higher than the current
    # version. You can also manually specify a version higher than the current version. Lower or equivalent
    # versions are not possible and will be rejected when you try to push the release.

    print_step("Build Step 3: Determine release versions.")  # MANUAL

    old_cf_version = current_distribution_by_website(HTTP_PATH_TO_LIVE_SITE)
    cf_version = CF_VERSION
    print("Version: " + cf_version + "\n")

    if old_cf_version == cf_version:
        print(
            (
                "It is *strongly discouraged* to not update the release version numbers for the Checker Framework "
                + "even if no changes were made to these in a month. This would break so much "
                + "in the release scripts that they would become unusable. Update the version number in checker-framework/build.gradle\n"
            )
        )
        prompt_to_continue()

    AFU_MANUAL = os.path.join(ANNO_FILE_UTILITIES, "annotation-file-utilities.html")
    old_afu_version = get_afu_version_from_html(AFU_MANUAL)
    (old_afu_version, afu_version) = get_new_version(
        "Annotation File Utilities", old_afu_version
    )

    if old_afu_version == afu_version:
        print(
            (
                "The AFU version has not changed. It is recommended to include a small bug fix or doc update in every "
                + "AFU release so the version number can be updated, but when that is not possible, before and after running "
                + "release_build, you must:\n"
                + "-Ensure that you are subscribed to the AFU push notifications mailing list.\n"
                + "-Verify that the AFU changelog has not been changed.\n"
                + '-Grep all the AFU pages on the dev web site for the release date with patterns such as "29.*Aug" '
                + 'and "Aug.*29" and fix them to match the previous release date.\n'
                + "Keep in mind that in this case, the release scripts will fail in certain places and you must manually "
                + "follow a few remaining release steps.\n"
            )
        )
        prompt_to_continue()

    # I don't think this should be necessary in general.  It's just to put files in place so link checking will work, and it takes a loooong time to run.
    # print_step("Build Step 4: Copy entire live site to dev site (~22 minutes).") # MANUAL

    # if prompt_yes_no("Proceed with copy of live site to dev site?", True):
    #     # ************************************************************************************************
    #     # WARNING: BE EXTREMELY CAREFUL WHEN MODIFYING THIS COMMAND.  The --delete option is destructive
    #     # and its work cannot be undone.  If, for example, this command were modified to accidentally make
    #     # /cse/www2/types/ the target directory, the entire types directory could be wiped out.
    #     execute("rsync --omit-dir-times --recursive --links --delete --quiet --exclude=dev --exclude=sparta/release/versions /cse/www2/types/ /cse/www2/types/dev")
    #     # ************************************************************************************************

    print_step(
        "Build Step 4: Create directories for the current release on the dev site."
    )  # AUTO

    (
        afu_interm_dir,
        checker_framework_interm_dir,
    ) = create_dirs_for_dev_website_release_versions(cf_version, afu_version)

    # The projects are built in the following order:
    # Annotation File Utilities and Checker Framework. Furthermore, their
    # manuals and websites are also built and placed in their relevant locations
    # at https://checkerframework.org/dev/ .  This is the most time-consuming
    # piece of the release. There are no prompts from this step forward; you
    # might want to get a cup of coffee and do something else until it is done.

    print_step("Build Step 5: Build projects and websites.")  # AUTO
    print(projects_to_release)

    if projects_to_release[AFU_OPT]:
        print_step("5a: Build Annotation File Utilities.")
        build_annotation_tools_release(afu_version, afu_interm_dir)

    if projects_to_release[CF_OPT]:
        print_step("5b: Build Checker Framework.")
        build_checker_framework_release(
            cf_version,
            old_cf_version,
            afu_version,
            afu_date,
            checker_framework_interm_dir,
        )

    print_step("Build Step 6: Overwrite .htaccess and CFLogo.png .")  # AUTO

    # Not "cp -p" because that does not work across filesystems whereas rsync does
    CFLOGO = os.path.join(CHECKER_FRAMEWORK, "docs", "logo", "Logo", "CFLogo.png")
    execute("rsync --times %s %s" % (CFLOGO, checker_framework_interm_dir))

    # Each project has a set of files that are updated for release. Usually these updates include new
    # release date and version information. All changed files are committed and pushed to the intermediate
    # repositories. Keep this in mind if you have any changed files from steps 1d, 4, or 5. Edits to the
    # scripts in the jsr308-release/scripts directory will never be checked in.

    print_step("Build Step 7: Commit projects to intermediate repos.")  # AUTO
    commit_to_interm_projects(cf_version, afu_version, projects_to_release)

    # Adds read/write/execute group permissions to all of the new dev website directories
    # under https://checkerframework.org/dev/ These directories need group read/execute
    # permissions in order for them to be served.

    print_step("\n\nBuild Step 8: Add group permissions to repos.")
    for build in BUILD_REPOS:
        ensure_group_access(build)

    for interm in INTERM_REPOS:
        ensure_group_access(interm)

    # At the moment, this will lead to output error messages because some metadata in some of the
    # dirs I think is owned by Mike or Werner.  We should identify these and have them fix it.
    # But as long as the processes return a zero exit status, we should be ok.
    print_step("\n\nBuild Step 9: Add group permissions to websites.")  # AUTO
    ensure_group_access(FILE_PATH_TO_DEV_SITE)

    create_empty_file(RELEASE_BUILD_COMPLETED_FLAG_FILE)


if __name__ == "__main__":
    sys.exit(main(sys.argv))
