#!/usr/bin/env python
"""Push the release."""

# See README-release-process.html for more information

import pathlib
import subprocess
import sys
from pathlib import Path

from release_utils import (  # ty: ignore # TODO: limitation in ty
    continue_or_exit,
    current_distribution_by_website,
    delete_directory,
    delete_directory_if_exists,
    delete_if_exists,
    ensure_group_access,
    get_announcement_email,
    has_command_line_option,
    print_step,
    prompt_to_continue,
    prompt_yes_no,
    push_changes_prompt_if_fail,
    read_first_line,
    set_umask,
    version_number_to_array,
)
from release_vars import (  # ty: ignore # TODO: limitation in ty
    CF_VERSION,
    CHECKER_FRAMEWORK,
    CHECKER_LIVE_API_DIR,
    CHECKER_LIVE_RELEASES_DIR,
    CHECKLINK,
    DEV_SITE_DIR,
    DEV_SITE_URL,
    INTERM_CHECKER_REPO,
    LIVE_SITE_DIR,
    LIVE_SITE_URL,
    RELEASE_BUILD_COMPLETED_FLAG_FILE,
    SANITY_DIR,
    SCRIPTS_DIR,
    TMP_DIR,
    execute,
)
from sanity_checks import (  # ty: ignore # TODO: limitation in ty
    javac_sanity_check,
    maven_sanity_check,
)


def check_release_version(previous_release: str, new_release: str) -> None:
    """Ensure that the given new release version is greater than the given previous one.

    Raises:
        Exception: If the new version is not greater than the previous one.
    """
    if version_number_to_array(previous_release) >= version_number_to_array(new_release):
        raise Exception(
            "Previous release version ("
            + previous_release
            + ") should be less than "
            + "the new release version ("
            + new_release
            + ")"
        )


def copy_release_dir(
    path_to_dev_releases: Path, path_to_live_releases: Path, release_version: str
) -> Path:
    """Copy a release directory from the dev site to the live site.

    For example,
    /cse/www2/types/dev/checker-framework/releases/2.0.0 ->
    /cse/www2/types/checker-framework/releases/2.0.0

    Returns:
        the destination location.

    Raises:
        Exception: If the destination cannot be deleted.
    """
    source_location = path_to_dev_releases / release_version
    dest_location = path_to_live_releases / release_version

    if dest_location.exists():
        delete_directory(dest_location)

    if dest_location.exists():
        raise Exception("Destination location exists: " + str(dest_location))

    # The / at the end of the source location is necessary so that
    # rsync copies the files in the source directory to the destination directory
    # rather than a subdirectory of the destination directory.
    cmd = (
        "rsync --no-p --no-group --omit-dir-times --recursive --links --quiet"
        f" {source_location}/ {dest_location}"
    )
    execute(cmd)

    return dest_location


def promote_release(path_to_releases: Path, release_version: str) -> None:
    """Copy a release directory to the top level.

    For example,
    /cse/www2/types/checker-framework/releases/2.0.0/* ->
    /cse/www2/types/checker-framework/*
    """
    from_dir = Path(path_to_releases) / release_version
    to_dir = Path(path_to_releases) / ".."
    # Trailing slash is crucial.
    cmd = f"rsync -aJ --no-perms --no-group --omit-dir-times {from_dir}/ {to_dir}"
    execute(cmd)


def copy_htaccess() -> None:
    """Copy the .htaccess file from the dev site to the live site."""
    live_htaccess = Path(LIVE_SITE_DIR) / ".htaccess"
    execute(f"rsync --times {Path(DEV_SITE_DIR) / '.htaccess'} {live_htaccess}")
    ensure_group_access(live_htaccess)


def copy_releases_to_live_site(cf_version: str) -> None:
    """Copy the new release of the Checker Framework from the dev site to the live site."""
    checker_interm_releases_dir = Path(DEV_SITE_DIR) / "releases"
    copy_release_dir(checker_interm_releases_dir, CHECKER_LIVE_RELEASES_DIR, cf_version)
    delete_directory_if_exists(CHECKER_LIVE_API_DIR)
    promote_release(CHECKER_LIVE_RELEASES_DIR, cf_version)


def stage_maven_artifacts_in_maven_central() -> None:
    """Stage the Checker Framework artifacts on Maven Central.

    After the
    artifacts are staged, the user can then close them, which makes them
    available for testing purposes but does not yet release them on Maven
    Central. This is a reversible step, since artifacts that have not been
    released can be dropped, which for our purposes is equivalent to never
    having staged them.
    """
    gnupg_passphrase = read_first_line(
        Path("/projects/swlab1/checker-framework/hosting-info/release-private.password")
    )
    execute(
        (
            "./gradlew publish -Prelease=true --no-parallel"
            " -Psigning.gnupg.keyName=checker-framework-dev@googlegroups.com"
            f" -Psigning.gnupg.passphrase={gnupg_passphrase}"
        ),
        working_dir=CHECKER_FRAMEWORK,
    )


def is_file_empty(filename: Path) -> bool:
    """Return true if the given file has size 0.

    Returns:
        true if the given file has size 0.
    """
    return filename.stat().st_size == 0


def run_link_checker(site: str, output_file: Path, additional_param: str = "") -> Path:
    """Run the link checker on the given web site and save the output to the given file.

    Additional parameters (if given) are passed directly to the link checker script.

    Returns:
        The given output file.
    """
    delete_if_exists(output_file)
    check_links_script = Path(SCRIPTS_DIR) / "checkLinks.sh"
    if additional_param == "":
        cmd = ["sh", str(check_links_script), site]
    else:
        cmd = ["sh", str(check_links_script), additional_param, site]
    env = {"CHECKLINK": CHECKLINK}

    out_file = Path.open(output_file, "w+")

    print(
        "Executing: "
        + " ".join(f"{key2}={val2}" for (key2, val2) in list(env.items()))
        + " "
        + " ".join(cmd)
    )
    process = subprocess.Popen(cmd, env=env, stdout=out_file, stderr=out_file)
    process.communicate()
    process.wait()
    out_file.close()

    return output_file


def check_all_links(
    checker_website: str,
    suffix: str,
    test_mode: bool,
    cf_version_of_broken_link_to_suppress: str = "",
) -> None:
    """Check all links on the given web sites for the Checker Framework.

    The suffix parameter should be "dev" for the
    dev web site and "live" for the live web site. test_mode indicates
    whether this script is being run in release or in test mode. The
    cf_version_of_broken_link_to_suppress parameter should be set to the
    new Checker Framework version and should only be passed when checking links
    for the dev web site (to prevent reporting of a broken link to the
    not-yet-live zip file for the new release).

    Raises:
        Exception: If there are link checking errors.
    """
    additional_param = ""
    if cf_version_of_broken_link_to_suppress != "":
        additional_param = (
            "--suppress-broken 404:https://checkerframework.org/checker-framework-"
            + cf_version_of_broken_link_to_suppress
            + ".zip"
        )
    checker_check = run_link_checker(
        checker_website,
        TMP_DIR / f"checker-framework.{suffix}.check",
        additional_param,
    )

    is_checker_check_empty = is_file_empty(checker_check)

    if not is_checker_check_empty:
        print("Link checker results can be found at:\n")
        print(f"\t{checker_check}\n")
        if not prompt_yes_no("Continue despite link checker results?", True):
            release_option = ""
            if not test_mode:
                release_option = " release"
            raise Exception(
                "The link checker reported errors.  Please fix them by committing changes to the\n"
                "mainline repository and pushing them to GitHub, then updating the development\n"
                "and live sites by running\n"
                "  python3 release_build.py all\n"
                "  python3 release_push" + release_option + "\n"
            )


def push_interm_to_release_repos() -> None:
    """Push the release to the GitHub repository for the Checker Framework.

    This is an irreversible step.
    """
    push_changes_prompt_if_fail(INTERM_CHECKER_REPO)


def validate_args(argv: list[str]) -> None:
    """Validate the command-line arguments.

    Raises:
        Exception: If the command-line arguments are not valid.
    """
    if len(argv) > 3:
        print_usage()
        raise Exception("Invalid arguments. " + ",".join(argv))
    for i in range(1, len(argv)):
        if argv[i] != "release":
            print_usage()
            raise Exception("Invalid arguments. " + ",".join(argv))


def print_usage() -> None:
    """Print instructions on how to use this script, including how to set test or release mode."""
    print(
        "Usage: python3 release_build.py [release]\n"
        'If the "release" argument is '
        "NOT specified then the script will execute all steps that checking and prompting "
        "steps but will NOT actually perform a release.  This is for testing the script."
    )


def main(argv: list[str]) -> None:
    """Copy the artifacts from the development web site to Maven Central and to the live site.

    It also performs link checking on the live site, pushes
    the release to GitHub repositories, and guides the user to
    perform manual steps such as sending the
    release announcement e-mail.

    Raises:
        Exception: If the file does not exist.
    """
    # MANUAL Indicates a manual step
    # AUTO Indicates the step is fully automated.

    set_umask()

    validate_args(argv)
    test_mode = not has_command_line_option(argv, "release")

    m2_settings = str(pathlib.Path("~").expanduser()) + "/.m2/settings.xml"
    if not pathlib.Path(m2_settings).exists():
        raise Exception("File does not exist: " + m2_settings)

    if test_mode:
        msg = (
            "You have chosen test_mode.\n"
            "This means that this script will execute all build steps that do not have side "
            "effects.  That is, this is a test run of the script.  All checks and user prompts "
            "will be shown but no steps will be executed that will cause the release to be "
            "deployed or partially deployed.\n"
            'If you meant to do an actual release, re-run this script with one argument, "release".'
        )
    else:
        msg = (
            "You have chosen release_mode.  "
            "Please follow the prompts to run a full Checker Framework release."
        )

    continue_or_exit(msg + "\n")
    if test_mode:
        print("Continuing in test mode.")
    else:
        print("Continuing in release mode.")

    if not pathlib.Path(RELEASE_BUILD_COMPLETED_FLAG_FILE).exists():
        continue_or_exit(
            "It appears that release_build.py has not been run since the last push to "
            "the Checker Framework repository.  Please ensure it has "
            "been run."
        )

    # The release script checks that the new release version is greater than the previous release
    # version.

    print_step("Push Step 1: Checking release versions")  # SEMIAUTO

    dev_checker_website = DEV_SITE_URL
    live_checker_website = LIVE_SITE_URL
    current_cf_version = current_distribution_by_website(live_checker_website)
    new_cf_version = CF_VERSION
    check_release_version(current_cf_version, new_cf_version)

    print(
        f"Checker Framework:  current-version={current_cf_version}    new-version={new_cf_version}"
    )

    # Runs the link the checker on all websites at:
    # https://checkerframework.org/dev/
    # The output of the link checker is written to files in the /scratch/$USER/cf-release directory
    # whose locations will be output at the command prompt if the link checker reported errors.

    # In rare instances (such as when a link is correct but the link checker is
    # unable to check it), you may add a suppression to the checklink-args.txt file.
    # In extremely rare instances (such as when a website happens to be down at the
    # time you ran the link checker), you may ignore an error.

    print_step("Push Step 2: Check links on development site")  # SEMIAUTO

    if prompt_yes_no("Run link checker on DEV site?", True):
        check_all_links(dev_checker_website, "dev", test_mode, new_cf_version)

    # Runs sanity tests on the development release. Later, we will run a smaller set of sanity
    # tests on the live release to ensure no errors occurred when promoting the release.

    print_step("Push Step 3: Run development sanity tests")  # SEMIAUTO
    if prompt_yes_no("Perform this step?", True):
        print_step("3a: Run javac sanity test on development release.")
        if prompt_yes_no("Run javac sanity test on development release?", True):
            javac_sanity_check(dev_checker_website, new_cf_version)

        print_step("3b: Run Maven sanity test on development release.")
        if prompt_yes_no("Run Maven sanity test on development repo?", True):
            maven_sanity_check("maven-dev", "")

    # Runs all tests on the development release.

    print_step("Push Step 4: Run all tests (takes a long time)")
    if prompt_yes_no("Perform this step?", True):
        ant_cmd = "./gradlew allTests"
        execute(ant_cmd, CHECKER_FRAMEWORK)

    # The Central Repository is a repository of build artifacts for build programs like Maven and
    # Ivy.  This step stages (but doesn't release) the Checker Framework's Maven artifacts in the
    # Sonatypes Central Repository.

    # Once staging is complete, there are manual steps to log into Sonatype Central and "close" the
    # staging repository. Closing allows us to test the artifacts.

    # This step deploys the artifacts to the Central Repository and prompts the user to close the
    # artifacts. Later, you will be prompted to release the staged artifacts after we push the
    # release to our GitHub repositories.

    # For more information on deploying to the Central Repository see:
    # https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide

    print_step("Push Step 5: Stage Maven artifacts in Central")  # SEMIAUTO

    print_step("Step 5a: Stage the artifacts at Maven Central.")
    if prompt_yes_no("Stage Maven artifacts in Maven Central?", True):
        stage_maven_artifacts_in_maven_central()

        print_step("Step 5b: Close staged artifacts at Maven Central.")
        ## TODO: previously we could 'close' the artifacts vi Sonatype's UI, but now a POST request
        # has to be made instead.  (Documentation here: https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#ensuring-deployment-visibility-in-the-central-publisher-portal)
        # I've tried to do this via the command line using curl, but the commands do nothing. I was
        # able to close the artifacts by doing the following:
        continue_or_exit(
            "Maven artifacts have been staged!  Please 'close' (but don't release) the artifacts.\n"
            "Browse to https://ossrh-staging-api.central.sonatype.com/swagger-ui/#/default/manual_search_repositories.\n"
            "Expand GET manual/search/repositories\n"
            "Click try it out.\n"
            "Type any in the IP field.\n"
            "Click Execute\n"
            "Log in with user token/password\n"
            "Scroll down until you see a JSON block that includes a key like this:\n"
            '           "key": "user/ip/org.checkerframework--default-repository",'
            "Copy the key field\n"
            "Expand POST manual/upload/repositories/{repository_key}\n"
            "Click try it out.\n"
            "Copy key field from above into repository_key\n"
            "Click Execute, it may take a minute or two to update\n"
            "Under Server response it should say Code 200\n"
            "Go to https://central.sonatype.com/publishing and make sure you see"
            " a deployment org.checkerframework (via OSSRH Staging API)\n"
        )
        ## I can't find a URL to copy anymore.
        # print_step("Step 5c: Run Maven sanity test on Maven Central artifacts.")
        # if prompt_yes_no("Run Maven sanity test on Maven Central artifacts?", True):
        #     repo_url = input("Please enter the repo URL of the closed artifacts:\n")
        #
        #     maven_sanity_check("maven-staging", repo_url)

    # This step copies the development release directories to the live release directories.
    # It then adds the appropriate permissions to the release. Symlinks need to be updated to point
    # to the live website rather than the development website. A straight copy of the directory
    # will NOT update the symlinks.

    print_step("Push Step 6. Copy dev current release website to live website")  # SEMIAUTO
    if not test_mode:
        if prompt_yes_no("Copy release to the live website?"):
            print("Copying to live site")
            copy_releases_to_live_site(new_cf_version)
            copy_htaccess()
            ensure_group_access(CHECKER_LIVE_RELEASES_DIR / new_cf_version)
    else:
        print("Test mode: Skipping copy to live site!")

    # This step downloads the checker-framework-X.Y.Z.zip file of the newly live release and ensures
    # we can run the Nullness Checker. If this step fails, you should backout the release.

    print_step("Push Step 7: Run javac sanity tests on the live release.")  # SEMIAUTO
    if not test_mode:
        if prompt_yes_no("Run javac sanity test on live release?", True):
            javac_sanity_check(live_checker_website, new_cf_version)
            sanity_test_checker_framework_dir = SANITY_DIR / "test-checker-framework"
            if not pathlib.Path(sanity_test_checker_framework_dir).is_dir():
                execute(f"mkdir -p {sanity_test_checker_framework_dir}")
            sanity_test_script = Path(SCRIPTS_DIR) / "test-checker-framework.sh"
            execute(
                "sh " + str(sanity_test_script) + " " + new_cf_version,
                sanity_test_checker_framework_dir,
            )
    else:
        print("Test mode: Skipping javac sanity tests on the live release.")

    # Runs the link the checker on all websites at:
    # https://checkerframework.org/
    # The output of the link checker is written to files in the /scratch/$USER/cf-release directory
    # whose locations will be output at the command prompt. Review the link checker output.

    # The set of broken links that is displayed by this check will differ from those in push
    # step 2 because the Checker Framework manual and website uses a mix of absolute and
    # relative links. Therefore, some links from the development site actually point to the
    # live site (the previous release). After step 5, these links point to the current
    # release and may be broken.

    print_step("Push Step 8. Check live site links")  # SEMIAUTO
    if not test_mode:
        if prompt_yes_no("Run link checker on LIVE site?", True):
            check_all_links(live_checker_website, "live", test_mode)
    else:
        print("Test mode: Skipping checking of live site links.")

    # This step pushes the changes committed to the interm repositories to the GitHub
    # repositories. This is the first irreversible change. After this point, you can no longer
    # backout changes and should do another release in case of critical errors.

    print_step("Push Step 9. Push changes to repositories")  # SEMIAUTO
    # This step could be performed without asking for user input but I think we should err on the
    # side of caution.
    if not test_mode:
        if prompt_yes_no("Push the release to GitHub repositories?  This is irreversible.", True):
            push_interm_to_release_repos()
            print("Pushed to repos")
    else:
        print("Test mode: Skipping push to GitHub!")

    # This is a manual step that releases the staged Maven artifacts to the actual Central
    # Repository.  This is also an irreversible step. Once you have released these artifacts they
    # will be forever available to the Java community through the Central Repository. Follow the
    # prompts. The Maven artifacts (such as checker-qual.jar) are still needed, but the Maven
    # plug-in is no longer maintained.

    print_step("Push Step 10. Publish staged artifacts in Central Repository.")  # MANUAL
    if test_mode:
        msg = (
            "Test Mode: You are in test_mode.  Please 'DROP' the artifacts. "
            "To drop, log into https://central.sonatype.com/publishing using your "
            "Sonatype credentials and click 'Drop'"
        )
    else:
        msg = (
            "Please 'Publish' the artifacts.\n"
            "First log into https://central.sonatype.com/publishing using your "
            "Sonatype credentials. Find the deployment labled "
            "'org.checkerframework (via OSSRH Staging API)' "
            "and click on the Publish button next to it.\n"
            "Now it should say PUBLISHING nest to the deployment. "
            "This will take a while, you can move onto the next release steps.\n\n"
        )

    print(msg)
    prompt_to_continue()

    if test_mode:
        print("Test complete")
    else:
        # A prompt describes the email you should send to all relevant mailing lists.
        # Please fill out the email and announce the release.

        print_step(
            "Push Step 11. Release the Checker Framework and Annotation File Utilities on GitHub."
        )  # MANUAL

        msg = (
            "\n"
            "Download the following files to your local machine."
            "\n"
            "  https://checkerframework.org/checker-framework-"
            + new_cf_version
            + ".zip\n"
            + "\n"
            + "To post the Checker Framework release on GitHub:\n"
            + "\n"
            + "* Browse to https://github.com/typetools/checker-framework/releases/new?tag=checker-framework-"
            + new_cf_version
            + "\n"
            + "* For the release title, enter: Checker Framework "
            + new_cf_version
            + "\n"
            + "* For the description, insert the latest Checker Framework changelog entry "
            + "(available at https://checkerframework.org/CHANGELOG.md). Please include the first "
            "line with the release version and date.\n"
            + '* Find the link below "Attach binaries by dropping them here or selecting them." '
            + 'Click on "selecting them" and upload checker-framework-'
            + new_cf_version
            + ".zip from your machine.\n"
            + '* Click on the green "Publish release" button.\n'
        )

        continue_or_exit(msg)

        print_step("Push Step 12. Announce the release.")  # MANUAL
        continue_or_exit(
            "Please announce the release using the email structure below.\n"
            + get_announcement_email(new_cf_version)
        )

        print_step("Push Step 13. Prep for next Checker Framework release.")  # MANUAL
        continue_or_exit(
            "Change the patch level (last number) of the Checker Framework version\n"
            "in build.gradle:  increment it and add -SNAPSHOT\n"
        )

        print_step("Push Step 14. Update the Checker Framework Gradle plugin.")  # MANUAL
        print("You might have to wait for Maven Central to propagate changes.\n")
        continue_or_exit(
            "Please update the Checker Framework Gradle plugin:\n"
            "https://github.com/kelloggm/checkerframework-gradle-plugin/blob/master/RELEASE.md#updating-the-checker-framework-version\n"
        )
        continue_or_exit(
            "Make a pull request to the Checker Framework that\n"
            "updates the version number of the Checker Framework\n"
            "Gradle Plugin in docs/examples/lombok and docs/examples/errorprone .\n"
            "The pull request's tests will fail; you will merge it in a day."
        )

    delete_if_exists(RELEASE_BUILD_COMPLETED_FLAG_FILE)

    print("Done with release_push.py.\n")


if __name__ == "__main__":
    main(sys.argv)
