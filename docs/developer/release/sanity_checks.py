#!/usr/bin/env python
"""Release utilities.

This contains no main method only utility functions to
run sanity checks on the Checker Framework.
Created by Jonathan Burke 11/21/2012

Copyright (c) 2012 University of Washington
"""

import os
import pathlib
import zipfile
from pathlib import Path

from release_utils import (  # ty: ignore # TODO: limitation in ty
    are_in_file,
    delete,
    delete_directory,
    download_binary,
    ensure_user_access,
    execute_write_to_file,
    insert_before_line,
    wget_file,
)
from release_vars import (  # ty: ignore # TODO: limitation in ty
    CHECKER_FRAMEWORK,
    SANITY_DIR,
    execute,
    execute_output,
)


def javac_sanity_check(checker_framework_website: str, release_version: str) -> None:
    """Download the release of the Checker Framework from the development website.

    Download NullnessExampleWithWarnings.java from the GitHub repository.
    Run the Nullness Checker on NullnessExampleWithWarnings and verify the output
    Fails if the expected errors are not found in the output.
    """
    new_checkers_release_zip = (
        f"{checker_framework_website}"
        f"/releases/{release_version}/checker-framework-{release_version}.zip"
    )

    javac_sanity_dir = Path(SANITY_DIR) / "javac"

    if pathlib.Path(javac_sanity_dir).is_dir():
        delete_directory(javac_sanity_dir)
    execute(f"mkdir -p {javac_sanity_dir}")

    javac_sanity_zip = Path(javac_sanity_dir) / f"checker-framework-{release_version}.zip"

    print(f"Attempting to download {new_checkers_release_zip} to {javac_sanity_zip}")
    download_binary(new_checkers_release_zip, javac_sanity_zip)

    nullness_example_url = "https://raw.githubusercontent.com/typetools/checker-framework/master/docs/examples/NullnessExampleWithWarnings.java"
    nullness_example = Path(javac_sanity_dir) / "NullnessExampleWithWarnings.java"

    if pathlib.Path(nullness_example).is_file():
        delete(nullness_example)

    wget_file(nullness_example_url, javac_sanity_dir)

    deploy_dir = Path(javac_sanity_dir) / f"checker-framework-{release_version}"

    if pathlib.Path(deploy_dir).exists():
        print(f"Deleting existing path: {deploy_dir}")
        delete_directory(deploy_dir)

    with zipfile.ZipFile(javac_sanity_zip, "r") as z:
        z.extractall(javac_sanity_dir)

    ensure_user_access(deploy_dir)

    sanity_javac = Path(deploy_dir) / "checker" / "bin" / "javac"
    nullness_output = Path(deploy_dir) / "output.log"

    cmd = (
        f"{sanity_javac} -processor org.checkerframework.checker.nullness.NullnessChecker "
        f"{nullness_example} -Anomsgtext"
    )
    execute_write_to_file(cmd, nullness_output, False)
    check_results(
        "Javac sanity check",
        nullness_output,
        [
            "NullnessExampleWithWarnings.java:23: error: (assignment)",
            "NullnessExampleWithWarnings.java:33: error: (argument)",
        ],
    )

    # this is a smoke test for the built-in checker shorthand feature
    # https://checkerframework.org/manual/#shorthand-for-checkers
    nullness_shorthand_output = Path(deploy_dir) / "output_shorthand.log"
    cmd = f"{sanity_javac} -processor NullnessChecker {nullness_example} -Anomsgtext"
    execute_write_to_file(cmd, nullness_shorthand_output, False)
    check_results(
        "Javac Shorthand Sanity Check",
        nullness_shorthand_output,
        [
            "NullnessExampleWithWarnings.java:23: error: (assignment)",
            "NullnessExampleWithWarnings.java:33: error: (argument)",
        ],
    )


def maven_sanity_check(sub_sanity_dir_name: str, repo_url: str) -> None:
    """Run the Maven sanity check with the local artifacts or from the repo at repo_url."""
    maven_sanity_dir = Path(SANITY_DIR) / sub_sanity_dir_name
    if pathlib.Path(maven_sanity_dir).is_dir():
        delete_directory(maven_sanity_dir)

    execute(f"mkdir -p {maven_sanity_dir}")

    maven_example_dir = Path(maven_sanity_dir) / "MavenExample"
    output_log = Path(maven_example_dir) / "output.log"

    get_example_dir_cmd = f"./gradlew updateCopyMavenExample -PdestDir={maven_sanity_dir}"

    execute(get_example_dir_cmd, CHECKER_FRAMEWORK)
    path_to_artifacts = (
        pathlib.Path("~").expanduser() / ".m2" / "repository" / "org" / "checkerframework"
    )
    if repo_url != "":
        print(
            "This script will now delete your Maven Checker Framework artifacts.\n"
            "See README-release-process.html#Maven-Plugin dependencies.  These artifacts "
            "will need to be re-downloaded the next time you need them.  This will be "
            "done automatically by Maven next time you use the plugin."
        )

        if pathlib.Path(path_to_artifacts).is_dir():
            delete_directory(path_to_artifacts)
        maven_example_pom = Path(maven_example_dir) / "pom.xml"
        add_repo_information(maven_example_pom, repo_url)

    os.environ["JAVA_HOME"] = os.environ["JAVA_21_HOME"]
    execute_write_to_file("mvn compile", output_log, False, maven_example_dir)
    if repo_url != "":
        delete_directory(path_to_artifacts)


def check_results(title: str, output_log: Path, expected_errors: list[str]) -> None:
    """Verify the given actual output of a sanity check against the given expected output.

    If the sanity check passed, print the given title of the
    sanity check and a success message. If the sanity check failed, raise an
    exception whose text contains the given title of the sanity check and the
    actual and expected output.
    """
    found_errors = are_in_file(output_log, expected_errors)

    if not found_errors:
        msg = (
            f"{title} did not work! File: {output_log} "
            f"should contain the following errors: [ {', '.join(expected_errors)}"
        )
        raise Exception(msg)
    print(f"{title} check: passed!\n")


def add_repo_information(pom: Path, repo_url: str) -> None:
    """Add development maven repo to pom file, to use the development artifacts."""
    to_insert = f"""
        <repositories>
              <repository>
                  <id>checker-framework-repo</id>
                  <url>{repo_url}</url>
              </repository>
        </repositories>

        <pluginRepositories>
              <pluginRepository>
                    <id>checker-framework-repo</id>
                    <url>{repo_url}</url>
              </pluginRepository>
        </pluginRepositories>
        """

    result_str = execute_output(f'grep -nm 1 "<build>" {pom}')
    line_no_str = result_str.split(":")[0]
    line_no = int(line_no_str)
    print(" LINE_NO: " + line_no_str)
    insert_before_line(to_insert, pom, line_no)
