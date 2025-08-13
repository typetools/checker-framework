#!/usr/bin/env python
"""Release the Checker Framework."""

# See README-release-process.html for more information

import datetime
import sys
from distutils.dir_util import copy_tree

from release_utils import (
    check_repos,
    check_tools,
    clone_from_scratch_or_update,
    commit_tag_and_push,
    continue_or_exit,
    create_empty_file,
    current_distribution_by_website,
    delete_if_exists,
    delete_path_if_exists,
    ensure_group_access,
    has_command_line_option,
    increment_version,
    os,
    print_step,
    prompt_to_continue,
    prompt_w_default,
    prompt_yes_no,
    set_umask,
)
from release_vars import (
    ANNO_FILE_UTILITIES,
    ANNO_TOOLS,
    BUILD_REPOS,
    CF_VERSION,
    CHECKER_FRAMEWORK,
    CHECKER_FRAMEWORK_RELEASE,
    CHECKLINK,
    CHECKLINK_REPO,
    DEV_SITE_DIR,
    INTERM_REPOS,
    INTERM_TO_BUILD_REPOS,
    LIVE_SITE_URL,
    LIVE_TO_INTERM_REPOS,
    PLUME_BIB,
    PLUME_BIB_REPO,
    PLUME_SCRIPTS,
    PLUME_SCRIPTS_REPO,
    RELEASE_BUILD_COMPLETED_FLAG_FILE,
    TOOLS,
    execute,
)

# Turned on by the --debug command-line option.
debug = False
ant_debug = ""


def print_usage():
    """Print usage information."""
    print("Usage:    python3 release_build.py [options]")
    print("\n  --debug  turns on debugging mode which produces verbose output")


def clone_or_update_repos():
    """Clone the relevant repos from scratch or update them if they exist.

    The action taken depends on a user query.
    """
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

    message += (
        "Clone repositories from scratch (answer no to be given a chance to update them instead)?"
    )

    clone_from_scratch = True

    if not prompt_yes_no(message, True):
        clone_from_scratch = False
        if not prompt_yes_no("Update the repositories without cloning them from scratch?", True):
            print("WARNING: Continuing without refreshing repositories.\n")
            return

    for live_to_interm in LIVE_TO_INTERM_REPOS:
        clone_from_scratch_or_update(live_to_interm[0], live_to_interm[1], clone_from_scratch, True)

    for interm_to_build in INTERM_TO_BUILD_REPOS:
        clone_from_scratch_or_update(
            interm_to_build[0], interm_to_build[1], clone_from_scratch, False
        )

    clone_from_scratch_or_update(PLUME_SCRIPTS_REPO, PLUME_SCRIPTS, clone_from_scratch, False)
    clone_from_scratch_or_update(CHECKLINK_REPO, CHECKLINK, clone_from_scratch, False)
    clone_from_scratch_or_update(PLUME_BIB_REPO, PLUME_BIB, clone_from_scratch, False)
    # clone_from_scratch_or_update(LIVE_ANNO_REPO, ANNO_TOOLS, clone_from_scratch, False)


def get_afu_date():
    """Return the AFU date.

    If the AFU is being built, return the current date, otherwise return the
    date of the last AFU release as indicated in the AFU home page.

    Returns:
        the AFU date
    """
    return get_current_date()


def get_new_version(project_name, curr_version):
    """Query the user for the new version number; returns old and new version numbers.

    Returns:
        the old and new version numbers
    """
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
    """Create the directory for the given version of the given project on the dev web site.

    Returns:
        the dev web site directory for the given project and version.
    """
    if project_name in (None, "checker-framework"):
        interm_dir = os.path.join(DEV_SITE_DIR, "releases", version)
    else:
        interm_dir = os.path.join(DEV_SITE_DIR, project_name, "releases", version)
    delete_path_if_exists(interm_dir)

    execute(f"mkdir -p {interm_dir}", True, False)
    return interm_dir


def create_dirs_for_dev_website_release_versions(cf_version):
    """Create directories for CF project under the releases directory of the dev web site.

    For example,
    /cse/www2/types/dev/checker-framework/<project_name>/releases/<version> .

    Returns:
        the dev web site directory for the CF.
    """
    afu_interm_dir = create_dev_website_release_version_dir("annotation-file-utilities", cf_version)
    checker_framework_interm_dir = create_dev_website_release_version_dir(None, cf_version)

    return (afu_interm_dir, checker_framework_interm_dir)


# def update_project_dev_website_symlink(project_name, release_version):
#     """Update the \"current\" symlink in the dev web site for the given project
#     to point to the given release of the project on the dev web site."""
#     project_dev_site = os.path.join(DEV_SITE_DIR, project_name)
#     link_path = os.path.join(project_dev_site, "current")
#
#     dev_website_relative_dir = os.path.join("releases", release_version)
#
#     print ("Writing symlink: " + link_path + "\nto point to relative directory: "
#             + dev_website_relative_dir)
#     force_symlink(dev_website_relative_dir, link_path)


def update_project_dev_website(project_name, release_version):
    """Update the dev web site for the given project.

    according to the given release of the project on the dev web site.
    """
    if project_name == "checker-framework":
        project_dev_site = DEV_SITE_DIR
    else:
        project_dev_site = os.path.join(DEV_SITE_DIR, project_name)
    dev_website_relative_dir = os.path.join(project_dev_site, "releases", release_version)

    print("Copying from : " + dev_website_relative_dir + "\nto: " + project_dev_site)
    copy_tree(dev_website_relative_dir, project_dev_site)


def get_current_date():
    """Return today's date in the format "02 May 2016".

    Returns:
        today's date.
    """
    return datetime.date.today().strftime("%d %b %Y")


def build_annotation_tools_release(version, afu_interm_dir):
    """Build the Annotation File Utilities project's artifacts.

    Also place them in the development web site.
    """
    execute("java -version", True)

    date = get_current_date()

    buildfile = os.path.join(ANNO_FILE_UTILITIES, "build.xml")
    ant_cmd = (
        f"ant {ant_debug} -buildfile {buildfile} -e update-versions"
        f' -Drelease.ver="{version}" -Drelease.date="{date}"'
    )
    execute(ant_cmd)

    # Deploy to intermediate site
    gradle_cmd = (
        f"./gradlew releaseBuildWithoutTest -Pafu.version={version} -Pdeploy-dir={afu_interm_dir}"
    )
    execute(gradle_cmd, True, False, ANNO_FILE_UTILITIES)

    update_project_dev_website("annotation-file-utilities", version)


def build_and_locally_deploy_maven(version):
    execute("./gradlew publishToMavenLocal", working_dir=CHECKER_FRAMEWORK)


def build_checker_framework_release(
    version, old_cf_version, afu_release_date, checker_framework_interm_dir
):
    """Build the release files for the Checker Framework project and run tests.

    The release files include the manual and the zip file.

    """
    checker_dir = os.path.join(CHECKER_FRAMEWORK, "checker")

    afu_build_properties = os.path.join(ANNO_FILE_UTILITIES, "build.properties")

    # build annotation-tools
    execute("./gradlew assemble -Prelease=true", True, False, ANNO_FILE_UTILITIES)

    # update versions
    ant_props = (
        f"-Dchecker={checker_dir} -Drelease.ver={version} -Dafu.version={version}"
        f' -Dafu.properties={afu_build_properties} -Dafu.release.date="{afu_release_date}"'
    )
    # IMPORTANT: The release.xml in the directory where the Checker Framework is
    # being built is used. Not the release.xml in the directory you ran
    # release_build.py from.
    ant_cmd = f"ant {ant_debug} -f release.xml {ant_props} update-checker-framework-versions "
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)
    # Update version numbers in the manual and API documentation,
    # which come from source files that have just been changed.
    # Otherwise the manual and API documentation show up in the grep command below.
    execute("./gradlew assemble", working_dir=CHECKER_FRAMEWORK)
    execute("./gradlew allJavadoc", working_dir=CHECKER_FRAMEWORK)
    execute("./gradlew manual", working_dir=CHECKER_FRAMEWORK)

    # Check that updating versions didn't overlook anything.
    print("Here are occurrences of the old version number, " + old_cf_version + ":")
    grep_cmd = f"grep -n -r --exclude-dir=build --exclude-dir=.git -F {old_cf_version}"
    execute(grep_cmd, False, False, CHECKER_FRAMEWORK)
    continue_or_exit(
        "If any occurrence is not acceptable, then stop the release, update target"
        ' "update-checker-framework-versions" in file release.xml, and start over.'
    )

    # Build the Checker Framework binaries and documents.  Tests are run by release_push.py.
    gradle_cmd = "./gradlew releaseBuild"
    execute(gradle_cmd, True, False, CHECKER_FRAMEWORK)

    # make the Checker Framework Manual
    checker_manual_dir = os.path.join(CHECKER_FRAMEWORK, "docs", "manual")
    execute("make manual.pdf manual.html", True, False, checker_manual_dir)

    # make the dataflow manual
    dataflow_manual_dir = os.path.join(CHECKER_FRAMEWORK, "dataflow", "manual")
    execute("make", True, False, dataflow_manual_dir)

    # make the checker framework tutorial
    checker_tutorial_dir = os.path.join(CHECKER_FRAMEWORK, "docs", "tutorial")
    execute("make", True, False, checker_tutorial_dir)

    # Create checker-framework-X.Y.Z.zip and put it in checker_framework_interm_dir
    ant_props = (
        f"-Dchecker={checker_dir} -Ddest.dir={checker_framework_interm_dir}"
        f" -Dfile.name=checker-framework-{version}.zip -Dversion={version}"
    )
    # IMPORTANT: The release.xml in the directory where the Checker Framework
    # is being built is used. Not the release.xml in the directory you ran
    # release_build.py from.
    ant_cmd = f"ant {ant_debug} -f release.xml {ant_props} zip-checker-framework "
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    ant_props = (
        f"-Dchecker={checker_dir} -Ddest.dir={checker_framework_interm_dir}"
        f" -Dfile.name=mvn-examples.zip -Dversion={version}"
    )
    # IMPORTANT: Uses the release.xml in the directory where the Checker Framework is being built.
    # Not the release.xml in the directory you ran release_build.py from.
    ant_cmd = f"ant {ant_debug} -f release.xml {ant_props} zip-maven-examples "
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    # copy the remaining checker-framework website files to checker_framework_interm_dir
    ant_props = (
        f"-Dchecker={checker_dir} -Ddest.dir={checker_framework_interm_dir}"
        " -Dmanual.name=checker-framework-manual"
        " -Ddataflow.manual.name=checker-framework-dataflow-manual"
        " -Dchecker.webpage=checker-framework-webpage.html"
    )

    # IMPORTANT: Uses the release.xml in the directory where the Checker Framework is being built.
    # Not the release.xml in the directory you ran release_build.py from.
    fant_cmd = f"ant {ant_debug} -f release.xml {ant_props} checker-framework-website-docs "
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    # clean no longer necessary files left over from building the checker framework tutorial
    checker_tutorial_dir = os.path.join(CHECKER_FRAMEWORK, "docs", "tutorial")
    execute("make clean", True, False, checker_tutorial_dir)

    build_and_locally_deploy_maven(version)

    update_project_dev_website("checker-framework", version)

    return


def commit_to_interm_projects(cf_version):
    """Commit the changes for each project from its build repo to its intermediate repo.

    This is in preparation for running the release_push
    script, which does not read the build repos.
    """
    # Use project definition instead, see find project location find_project_locations

    commit_tag_and_push(cf_version, ANNO_TOOLS, "")

    commit_tag_and_push(cf_version, CHECKER_FRAMEWORK, "checker-framework-")


def main(argv):
    """Build the release artifacts for the AFU and the Checker Framework projects.

    Also place them in the development web site. It can also be used to review
    the documentation and changelogs for the three projects.
    """
    # MANUAL Indicates a manual step
    # AUTO Indicates the step is fully automated.

    delete_if_exists(RELEASE_BUILD_COMPLETED_FLAG_FILE)

    set_umask()

    global debug, ant_debug
    debug = has_command_line_option(argv, "--debug")
    if debug:
        ant_debug = "-debug"

    afu_date = get_afu_date()

    # For each project, build what is necessary but don't push

    print("Building a new release of Annotation Tools and the Checker Framework!")

    print("\nPATH:\n" + os.environ["PATH"] + "\n")

    print_step("Build Step 1: Clone the build and intermediate repositories.")  # MANUAL

    # Recall that there are 3 relevant sets of repositories for the release:
    # * build repository - repository where the project is built for release
    # * intermediate repository - repository to which release related changes are pushed
    #   after the project is built
    # * release repository - GitHub repositories, the central repository.

    # Every time we run release_build, changes are committed to the intermediate repository from
    # build but NOT to the release repositories. If we are running the build script multiple times
    # without actually committing the release then these changes need to be cleaned before we run
    # the release_build script again.  The "Clone/update repositories" step updates the repositories
    # with respect to the live repositories on GitHub, but it is the "Verify repositories" step that
    # ensures that they are clean, i.e. indistinguishable from a freshly cloned repository.

    # check we are cloning LIVE -> INTERM, INTERM -> RELEASE
    print_step("\nStep 1a: Clone/update repositories.")  # MANUAL
    clone_or_update_repos()

    # This step ensures the previous step worked. It checks to see if we have any modified files,
    # untracked files, or outgoing changesets. If so, it fails.

    print_step("Step 1b: Verify repositories.")  # MANUAL
    check_repos(INTERM_REPOS, True, True)
    check_repos(BUILD_REPOS, True, False)

    # The release script requires a number of common tools (Ant, Maven, make, etc...). This step
    # checks to make sure all tools are available on the command line in order to avoid wasting time
    # in the event a tool is missing late in execution.

    print_step("Build Step 2: Check tools.")  # AUTO
    check_tools(TOOLS)

    # Usually we increment the release by 0.0.1 per release unless there is a major change.  The
    # release script will read the current version of the Checker Framework/Annotation File
    # Utilities from the release website and then suggest the next release version 0.0.1 higher than
    # the current version. You can also manually specify a version higher than the current
    # version. Lower or equivalent versions are not possible and will be rejected when you try to
    # push the release.

    print_step("Build Step 3: Determine release versions.")  # MANUAL

    old_cf_version = current_distribution_by_website(LIVE_SITE_URL)
    cf_version = CF_VERSION
    print("Version: " + cf_version + "\n")

    if old_cf_version == cf_version:
        print(
            "It is *strongly discouraged* to not update the release version numbers for "
            "the Checker Framework even if no changes were made to these in a month. "
            "This would break so much in the release scripts that they would become unusable. "
            "Update the version number in checker-framework/build.gradle\n"
        )
        prompt_to_continue()

    print_step("Build Step 4: Create directories for the current release on the dev site.")  # AUTO

    (
        afu_interm_dir,
        checker_framework_interm_dir,
    ) = create_dirs_for_dev_website_release_versions(cf_version)

    # The projects are built in the following order:
    # Annotation File Utilities and Checker Framework. Furthermore, their
    # manuals and websites are also built and placed in their relevant locations
    # at https://checkerframework.org/dev/ .  This is the most time-consuming
    # piece of the release. There are no prompts from this step forward; you
    # might want to get a cup of coffee and do something else until it is done.

    print_step("Build Step 5: Build projects and websites.")  # AUTO

    print_step("Step 5a: Build Annotation File Utilities.")
    build_annotation_tools_release(cf_version, afu_interm_dir)

    print_step("Step 5b: Build Checker Framework.")
    build_checker_framework_release(
        cf_version,
        old_cf_version,
        afu_date,
        checker_framework_interm_dir,
    )

    print_step("Build Step 6: Overwrite .htaccess and CFLogo.png .")  # AUTO

    # Not "cp -p" because that does not work across filesystems whereas rsync does
    cf_logo = os.path.join(CHECKER_FRAMEWORK, "docs", "logo", "Logo", "CFLogo.png")
    execute(f"rsync --times {cf_logo} {checker_framework_interm_dir}")

    # Each project has a set of files that are updated for release. Usually these updates include
    # new release date and version information. All changed files are committed and pushed to the
    # intermediate repositories. Keep this in mind if you have any changed files from steps 1d, 4,
    # or 5. Edits to the scripts in the cf-release/scripts directory will never be checked in.

    print_step("Build Step 7: Commit projects to intermediate repos.")  # AUTO
    commit_to_interm_projects(cf_version)

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
    ensure_group_access(DEV_SITE_DIR)

    create_empty_file(RELEASE_BUILD_COMPLETED_FLAG_FILE)


if __name__ == "__main__":
    sys.exit(main(sys.argv))
