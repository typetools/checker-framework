#!/usr/bin/env python
# encoding: utf-8
"""
releaseutils.py

This contains no main method only utility functions to
run sanity checks on the Checker Framework.
Created by Jonathan Burke 11/21/2012

Copyright (c) 2012 University of Washington
"""

from release_vars  import *
from release_utils import *
import urllib
import zipfile

def javac_sanity_check( checker_framework_website, release_version ):
    """
       Download the release of the Checker Framework from the development website
       and NullnessExampleWithWarnings.java from the Google Code repository.
       Run the Nullness Checker on NullnessExampleWithWarnings and verify the output
       Fails if the expected errors are not found in the output.
    """

    new_checkers_release_zip = os.path.join( checker_framework_website, "releases", release_version, "checker-framework.zip" )

    javac_sanity_dir = os.path.join( SANITY_DIR, "javac" )

    if os.path.isdir( javac_sanity_dir ):
        delete_path( javac_sanity_dir )
    execute( "mkdir -p " + javac_sanity_dir )

    javac_sanity_zip = os.path.join( javac_sanity_dir, "checker-framework.zip" )

    print( "Attempting to download %s to %s" % ( new_checkers_release_zip, javac_sanity_zip ) )
    download_binary( new_checkers_release_zip, javac_sanity_zip, MAX_DOWNLOAD_SIZE )

    nullness_example_url = "https://checker-framework.googlecode.com/hg/checker/examples/NullnessExampleWithWarnings.java"
    nullness_example = os.path.join( javac_sanity_dir, "NullnessExampleWithWarnings.java" )

    if os.path.isfile( nullness_example ):
        delete( nullness_example )

    wget_file( nullness_example_url, javac_sanity_dir )

    deploy_dir = os.path.join( javac_sanity_dir, "checker-framework-" + release_version )

    if os.path.exists( deploy_dir ):
        print( "Deleting existing path: " + deploy_dir )
        delete_path(deploy_dir)

    with zipfile.ZipFile(javac_sanity_zip, "r") as z:
        z.extractall( javac_sanity_dir )

    cmd = "chmod -R u+rwx " + deploy_dir
    execute( cmd )

    sanity_javac = os.path.join( deploy_dir, "checker", "bin", "javac" )
    nullness_output  = os.path.join( deploy_dir, "output.log" )

    cmd = sanity_javac + " -processor org.checkerframework.checker.nullness.NullnessChecker " + nullness_example + " -Anomsgtext"
    execute_write_to_file( cmd, nullness_output, False )
    check_results( "Javac sanity check", nullness_output, [
        "NullnessExampleWithWarnings.java:25: error: (assignment.type.incompatible)",
        "NullnessExampleWithWarnings.java:36: error: (argument.type.incompatible)"
    ])

    #this is a smoke test for the built-in checker shorthand feature
    #http://types.cs.washington.edu/checker-framework/current/checker-framework-manual.html#shorthand-for-checkers
    nullness_shorthand_output = os.path.join( deploy_dir, "output_shorthand.log")
    cmd = sanity_javac + " -processor NullnessChecker " + nullness_example + " -Anomsgtext"
    execute_write_to_file( cmd, nullness_shorthand_output, False )
    check_results( "Javac Shorthand Sanity Check", nullness_shorthand_output, [
        "NullnessExampleWithWarnings.java:25: error: (assignment.type.incompatible)",
        "NullnessExampleWithWarnings.java:36: error: (argument.type.incompatible)"
    ])

def maven_sanity_check( sub_sanity_dir_name, repo_url, release_version ):
    """
       Download the Checker Framework maven plugin from the given repository.  Download the
       HelloGalaxy example for the Maven plugin and run the NullnessChecker on it.  If we don't
       encounter the expected errors fail.
    """
    checker_dir = os.path.join(CHECKER_FRAMEWORK, "checker")
    maven_sanity_dir = os.path.join( SANITY_DIR, sub_sanity_dir_name )
    if os.path.isdir( maven_sanity_dir ):
        delete_path( maven_sanity_dir )

    execute( "mkdir -p " + maven_sanity_dir )

    path_to_artifacts = os.path.join( os.path.expanduser("~"), ".m2", "repository", "org", "checkerframework" )
    print("This script must delete your Maven Checker Framework artifacts.\n" +
          "See README-maintainers.html#Maven-Plugin dependencies.  These artifacts " +
          "will need to be re-downloaded the next time you need them.  This will be " +
          "done automatically by Maven next time you use the plugin." )
    continue_check = prompt_w_suggestion("Delete your Checker Framework artifacts?", "yes")

    if is_no( continue_check ):
        print("Please run the Maven tutorial manually using the Maven plugin at repo %s" % ( MAVEN_DEV_REPO ) )
    else:
        if os.path.isdir( path_to_artifacts ):
            delete_path( path_to_artifacts )

        hello_galaxy_dir = os.path.join( maven_sanity_dir, "HelloGalaxy" )
        output_log  = os.path.join( hello_galaxy_dir, "output.log" )

        ant_release_script = os.path.join( CHECKER_FRAMEWORK_RELEASE, "release.xml" )
        get_example_dir_cmd = "ant -f %s update-and-copy-hello-galaxy -Dchecker=%s -Dversion=%s -Ddest.dir=%s" % ( ant_release_script, checker_dir, release_version, maven_sanity_dir )

        execute( get_example_dir_cmd )

        hello_galaxy_pom = os.path.join( hello_galaxy_dir, "pom.xml" )
        add_repo_information( hello_galaxy_pom, repo_url )

        execute_write_to_file( "mvn checkerframework:check", output_log, False, hello_galaxy_dir )
        check_results( "Maven sanity check", output_log, [
            "HelloGalaxy.java:[30,29] [assignment.type.incompatible] incompatible types in assignment."
        ])

        delete_path( path_to_artifacts )

def check_results( title, output_log, expected_errors ):
    found_errors = are_in_file( output_log, expected_errors )

    if not found_errors:
        raise Exception( title + " did not work!\n" +
                         "File: " + output_log + "\n" +
                         "should contain the following errors: [ " + ", ".join(expected_errors) )
    else:
        print( "%s check: passed!\n" % title )


def add_repo_information( pom, repo_url ):
    """Adds development maven repo to pom file so that the artifacts used are the development artifacts"""

    to_insert = """
        <repositories>
              <repository>
                  <id>checker-framework-repo</id>
                  <url>%s</url>
              </repository>
        </repositories>

        <pluginRepositories>
              <pluginRepository>
                    <id>checker-framework-repo</id>
                    <url>%s</url>
              </pluginRepository>
        </pluginRepositories>
        """ % (repo_url, repo_url)

    result_str = execute( 'grep -nm 1 "<build>" %s' % pom, True, True )
    line_no_str = result_str.split(":")[0]
    line_no = int( line_no_str )
    print(" LINE_NO: " + line_no_str )
    insert_before_line( to_insert, pom, line_no )
