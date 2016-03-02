#!/usr/bin/env python
# encoding: utf-8
"""
release_push.py

Created by Jonathan Burke on 2013-12-30.

Copyright (c) 2015 University of Washington. All rights reserved.
"""

# See README-maintainers.html for more information

from release_vars  import *
from release_utils import *
from sanity_checks import *
import os
import urllib
import zipfile

# ensure that the latest built version is
def check_release_version( previous_release, new_release ):
    if compare_version_numbers( previous_release, new_release ) >= 0:
        raise Exception( "Previous release version ( " + previous_release + " ) should be less than " +
                         "the new release version( " + new_release + " )" )

def copy_release_dir( path_to_dev, path_to_live, release_version ):
    source_location = os.path.join( path_to_dev, release_version )
    dest_location = os.path.join( path_to_live, release_version )

    if os.path.exists( dest_location ):
        prompt_to_delete( dest_location )

    if os.path.exists( dest_location ):
        raise Exception( "Destination location exists: " + dest_location )

    # The / at the end of the destination location is necessary so that
    # rsync copies the files in the source directory to the destination directory
    # rather than a subdirectory of the destination directory.
    cmd = "rsync --omit-dir-times --recursive --links --quiet %s %s/" % ( source_location, dest_location )
    execute( cmd )

    return dest_location

def copy_htaccess():
    execute("rsync --times %s %s" % (DEV_HTACCESS, LIVE_HTACCESS))
    ensure_group_access(LIVE_HTACCESS)

def copy_releases_to_live_site( checker_version, afu_version):
    copy_release_dir( JSR308_INTERM_RELEASES_DIR,  JSR308_LIVE_RELEASES_DIR,  checker_version )
    copy_release_dir( CHECKER_INTERM_RELEASES_DIR, CHECKER_LIVE_RELEASES_DIR, checker_version )
    copy_release_dir( AFU_INTERM_RELEASES_DIR, AFU_LIVE_RELEASES_DIR, afu_version )

def update_release_symlinks( checker_version, afu_version ):
    afu_latest_release_dir = os.path.join( AFU_LIVE_RELEASES_DIR,  afu_version )
    checker_latest_release_dir = os.path.join( CHECKER_LIVE_RELEASES_DIR, checker_version )

    force_symlink( os.path.join( JSR308_LIVE_RELEASES_DIR,  checker_version ), os.path.join( JSR308_LIVE_SITE,  "current" ) )
    force_symlink( checker_latest_release_dir, os.path.join( CHECKER_LIVE_SITE, "current" ) )
    force_symlink( afu_latest_release_dir,     os.path.join( AFU_LIVE_SITE,     "current" ) )

    # After the copy operations the index.htmls will point into the dev directory
    force_symlink( os.path.join( afu_latest_release_dir,  "annotation-file-utilities.html" ), os.path.join( afu_latest_release_dir, "index.html" ) )
    force_symlink( os.path.join( checker_latest_release_dir, "checker-framework-webpage.html" ), os.path.join( checker_latest_release_dir, "index.html" ) )

def ensure_group_access_to_releases():
    ensure_group_access( JSR308_LIVE_RELEASES_DIR )
    ensure_group_access( AFU_LIVE_RELEASES_DIR )
    ensure_group_access( CHECKER_LIVE_RELEASES_DIR )

def push_maven_artifacts_to_release_repo( version ):
    mvn_deploy_mvn_plugin( MAVEN_PLUGIN_DIR, MAVEN_PLUGIN_POM, version, MAVEN_LIVE_REPO )

    # Deploy jsr308 and checker-qual jars to maven repo
    mvn_deploy( CHECKER_BINARY, CHECKER_BINARY_POM, MAVEN_LIVE_REPO )
    mvn_deploy( CHECKER_QUAL,   CHECKER_QUAL_POM,   MAVEN_LIVE_REPO )
    mvn_deploy( JAVAC_BINARY,   JAVAC_BINARY_POM,   MAVEN_LIVE_REPO )
    mvn_deploy( JDK7_BINARY,    JDK7_BINARY_POM,    MAVEN_LIVE_REPO )
    mvn_deploy( JDK8_BINARY,    JDK8_BINARY_POM,    MAVEN_LIVE_REPO )

def stage_maven_artifacts_in_maven_central( new_checker_version ):

    pgp_user="checker-framework-dev@googlegroups.com"
    pgp_passphrase = read_first_line( PGP_PASSPHRASE_FILE )

    mvn_dist = os.path.join(MAVEN_PLUGIN_DIR, "dist" )
    execute( "mkdir -p " + mvn_dist )

    # build Jar files with only readmes for artifacts that don't have sources/javadocs
    ant_cmd = "ant -f release.xml -Ddest.dir=%s -Dmaven.plugin.dir=%s jar-maven-extras" % (mvn_dist, MAVEN_PLUGIN_DIR)
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    # At the moment, checker.jar is the only artifact with legitimate accompanying source/javadoc jars
    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, CHECKER_BINARY_RELEASE_POM, CHECKER_BINARY,
                             CHECKER_SOURCE, CHECKER_JAVADOC,
                             pgp_user, pgp_passphrase )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, CHECKER_QUAL_RELEASE_POM, CHECKER_QUAL,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "checker-qual-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "checker-qual-javadoc.jar" ),
                             pgp_user, pgp_passphrase  )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, CHECKER_COMPAT_QUAL_RELEASE_POM,
                             CHECKER_COMPAT_QUAL,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "checker-compat-qual-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "checker-compat-qual-javadoc.jar" ),
                             pgp_user, pgp_passphrase  )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, JAVAC_BINARY_RELEASE_POM, JAVAC_BINARY,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "compiler-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "compiler-javadoc.jar" ),
                             pgp_user, pgp_passphrase  )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, JDK7_BINARY_RELEASE_POM, JDK7_BINARY,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "jdk7-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "jdk7-javadoc.jar" ),
                             pgp_user, pgp_passphrase  )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, JDK8_BINARY_RELEASE_POM, JDK8_BINARY,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "jdk8-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "jdk8-javadoc.jar" ),
                             pgp_user, pgp_passphrase  )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, JAVACUTIL_BINARY_RELEASE_POM, JAVACUTIL_BINARY,
                             JAVACUTIL_SOURCE_JAR, JAVACUTIL_JAVADOC_JAR,
                             pgp_user, pgp_passphrase  )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, DATAFLOW_BINARY_RELEASE_POM, DATAFLOW_BINARY,
                             DATAFLOW_SOURCE_JAR, DATAFLOW_JAVADOC_JAR,
                             pgp_user, pgp_passphrase  )

    plugin_jar = find_mvn_plugin_jar( MAVEN_PLUGIN_DIR, new_checker_version )
    plugin_source_jar  = find_mvn_plugin_jar( MAVEN_PLUGIN_DIR, new_checker_version, "sources" )
    plugin_javadoc_jar = os.path.join( MAVEN_RELEASE_DIR, mvn_dist, "checkerframework-maven-plugin-javadoc.jar" )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, MAVEN_PLUGIN_RELEASE_POM, plugin_jar,
                             plugin_source_jar, plugin_javadoc_jar, pgp_user, pgp_passphrase  )

    delete_path( mvn_dist )

def is_file_empty( filename ):
    return os.path.getsize(filename) == 0

def run_link_checker( site, output ):
    delete_if_exists( output )
    check_links_script = os.path.join(SCRIPTS_DIR, "checkLinks.sh")
    cmd = ["sh", check_links_script, site]
    env = {"PLUME_BIN": "%s/bin" % PLUME_LIB}

    out_file = open( output, 'w+' )

    print("Executing: "
          + ' '.join("%s=%r" % (key2,val2) for (key2,val2) in env.items())
          + " "
          + " ".join(cmd) )
    process = subprocess.Popen(cmd, env=env, stdout=out_file, stderr=out_file)
    process.communicate()
    process.wait()
    out_file.close()

    if process.returncode != 0:
        raise Exception('Non-zero return code( %s ) while executing %s' % (process.returncode, cmd))

    return output

def check_all_links( jsr308_website, afu_website, checker_website, suffix ):
    jsr308Check  = run_link_checker( jsr308_website,  TMP_DIR + "/jsr308." + suffix + ".check" )
    afuCheck     = run_link_checker( afu_website,     TMP_DIR + "/afu." + suffix + ".check" )
    checkerCheck = run_link_checker( checker_website, TMP_DIR + "/checker-framework." + suffix + ".check" )

    is_jsr308Check_empty = is_file_empty( jsr308Check )
    is_afuCheck_empty = is_file_empty( afuCheck )
    is_checkerCheck_empty = is_file_empty( checkerCheck )

    errors_reported = not ( is_jsr308Check_empty and is_afuCheck_empty and is_checkerCheck_empty )
    if (errors_reported):
        print( "Link checker results can be found at:\n" )
    if (not is_jsr308Check_empty):
           print( "\t" + jsr308Check  + "\n" )
    if (not is_afuCheck_empty):
           print( "\t" + afuCheck     + "\n" )
    if (not is_checkerCheck_empty):
           print( "\t" + checkerCheck + "\n" )
    if (errors_reported):
        release_option = ""
        if not test_mode:
            release_option = " release"
        raise Exception("The link checker reported errors.  Please fix them by committing changes to the mainline\n" +
                        "repository and pushing them to GitHub/Bitbucket, running \"python release_build.py all\" again\n" +
                        "(in order to update the development site), and running \"python release_push" + release_option + "\" again.")

def push_interm_to_release_repos():
    hg_push_or_fail( INTERM_JSR308_REPO )
    hg_push_or_fail( INTERM_ANNO_REPO )
    hg_push_or_fail( INTERM_CHECKER_REPO )

def continue_or_exit( msg ):
    continue_script = prompt_w_suggestion(msg + " Continue ('no' will exit the script)?", "yes", "^(Yes|yes|No|no)$")
    if continue_script == "no" or continue_script == "No":
                raise Exception( "User elected NOT to continue at prompt: " + msg )

def read_args(argv):
    test = True
    if len( argv ) == 2:
        if argv[1] == "release":
            test = False
        else:
            print_usage()
    else:
        if len( argv ) > 2:
            print_usage()
            raise Exception( "Invalid arguments. " + ",".join(argv) )

    return test

def print_usage():
    print ( "Usage: python release_build.py [release]\n" +
            "The only argument this script takes is \"release\".  If this argument is " +
            "NOT specified then the script will execute all steps that checking and prompting " +
            "steps but will NOT actually perform a release.  This is for testing the script." )

def main(argv):
    # MANUAL Indicates a manual step
    # SEMIAUTO Indicates a mostly automated step with possible prompts. Most of these steps become fully-automated when --auto is used.
    # AUTO Indicates the step is fully-automated.

    set_umask()

    auto = read_auto( argv )
    test_mode = read_args( argv )

    msg = ( "You have chosen test_mode.  \nThis means that this script will execute all build steps that " +
            "do not have side-effects.  That is, this is a test run of the script.  All checks and user prompts "  +
            "will be shown but no steps will be executed that will cause the release to be deployed or partially " +
            "deployed.\n" +
            "If you meant to do an actual release, re-run this script with one argument, \"release\"." )

    if not test_mode:
        msg = "You have chosen release_mode.  Please follow the prompts to run a full Checker Framework release"

    continue_or_exit( msg + "\n" )
    if test_mode:
        print("Continuing in test mode.")
    else:
        print("Continuing in release mode.")

    if not auto:
        print_step( "Push Step 0: Verify Requirements\n" ) # MANUAL
        print( " If this is your first time running the release_push script, please verify that you have met " +
               "all the requirements specified in README-maintainers.html \"Pre-release Checklist\"\n" )

        continue_or_exit("")

    if not os.path.exists( RELEASE_BUILD_COMPLETED_FLAG_FILE ):
        continue_or_exit("It appears that release_build.py has not been run since the last push to " +
                         "the JSR308, AFU, or Checker Framework repositories.  Please ensure it has " +
                         "been run." )

    # The release script checks that the new release version is greater than the previous release version.

    print_step( "Push Step 1: Checking release versions" ) # SEMIAUTO
    dev_jsr308_website  = os.path.join( HTTP_PATH_TO_DEV_SITE,  "jsr308" )
    live_jsr308_website = os.path.join( HTTP_PATH_TO_LIVE_SITE, "jsr308" )
    dev_afu_website  = os.path.join( HTTP_PATH_TO_DEV_SITE,  "annotation-file-utilities" )
    live_afu_website = os.path.join( HTTP_PATH_TO_LIVE_SITE, "annotation-file-utilities" )

    dev_checker_website  = os.path.join( HTTP_PATH_TO_DEV_SITE,  "checker-framework" )
    live_checker_website = os.path.join( HTTP_PATH_TO_LIVE_SITE, "checker-framework" )
    current_checker_version = current_distribution_by_website( live_checker_website )
    new_checker_version     = current_distribution( CHECKER_FRAMEWORK )
    check_release_version( current_checker_version, new_checker_version )

    # note, get_afu_version_from_html uses the file path not the web url
    dev_afu_website_file  = os.path.join( FILE_PATH_TO_DEV_SITE,  "annotation-file-utilities", "index.html" )
    live_afu_website_file = os.path.join( FILE_PATH_TO_LIVE_SITE, "annotation-file-utilities", "index.html" )
    current_afu_version = get_afu_version_from_html( live_afu_website_file )
    new_afu_version     = get_afu_version_from_html( dev_afu_website_file )
    check_release_version( current_afu_version, new_afu_version )

    print("Checker Framework/JSR308:  current-version=%s    new-version=%s" % (current_checker_version, new_checker_version ) )
    print("AFU:                       current-version=%s    new-version=%s" % (current_afu_version, new_afu_version ) )

    # Runs the link the checker on all websites at:
    # http://types.cs.washington.edu/dev/
    # The output of the link checker is written to files in the /scratch/$USER/jsr308-release directory
    # whose locations will be output at the command prompt if the link checker reported errors.

    # In rare instances (such as when a link is correct but the link checker is
    # unable to check it), you may add a suppression to the checklink-args.txt file.
    # In extremely rare instances (such as when a website happens to be down at the
    # time you ran the link checker), you may ignore an error.

    print_step( "Push Step 2: Check links on development site" ) # SEMIAUTO

    if auto or prompt_yes_no( "Run link checker on DEV site?", True ):
        check_all_links( dev_jsr308_website, dev_afu_website, dev_checker_website, "dev" )

    # Runs sanity tests on the development release. Later, we will run a smaller set of sanity
    # tests on the live release to ensure no errors occurred when promoting the release.

    # NOTE: In this step you will also be prompted to build and manually test the Eclipse plugin.

    print_step( "Push Step 3: Run development sanity tests" ) # SEMIAUTO
    if auto or prompt_yes_no(
           "Later in this step you will build and install the Eclipse plugin using the latest artifacts. See\n" +
           "README-developers.html under the checker-framework/release directory\n" +
           "Perform this step?", True ):

        print_step(" 3a: Run javac sanity test on development release." )
        if auto or prompt_yes_no( "Run javac sanity test on development release?", True ):
            javac_sanity_check( dev_checker_website, new_checker_version )

        print_step("3b: Run Maven sanity test on development release." )
        if auto or prompt_yes_no( "Run Maven sanity test on development repo?", True ):
            maven_sanity_check( "maven-dev", MAVEN_DEV_REPO, new_checker_version )

        print_step( "3c: Build the Eclipse plugin and test." )
        print("Please download: http://types.cs.washington.edu/dev/checker-framework/current/checker-framework-%s.zip" % new_checker_version)
        print("Use the jars in the dist directory along with the instructions at " +
              "checker-framework/eclipse/README-developers.html to build the Eclipse plugin.\n" +
              "Please install this version in the latest version of Eclipse and follow the tutorial at:\n" +
              "http://types.cs.washington.edu/dev/checker-framework/tutorial/" )
        continue_or_exit("If the tutorial doesn't work, please abort the release and contact the appropriate developer.")

    # The Central repository is a repository of build artifacts for build programs like Maven and Ivy.
    # This step stages (but doesn't release) the Checker Framework's Maven artifacts in the Sonatypes
    # Central Repository.

    # Once staging is complete, there are manual steps to log into Sonatypes Central and "close" the
    # staging repository. Closing allows us to test the artifacts.

    # This step deploys the artifacts to the Central repository and prompts the user to close the
    # artifacts. Later, you will be prompted to release the staged artifacts after we push the
    # release to our GitHub/Bitbucket repositories.

    # For more information on deploying to the Central Repository see:
    # https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide

    print_step( "Push Step 4: Stage Maven artifacts in Central" ) # SEMIAUTO

    print_step("4a: Stage the artifacts at Maven central." )
    if auto or prompt_yes_no( "Stage Maven artifacts in Maven Central?" ):
        stage_maven_artifacts_in_maven_central( new_checker_version )

        print_step("4b: Close staged artifacts at Maven central." )
        continue_or_exit( "Maven artifacts have been staged!  Please 'close' (but don't release) the artifacts.\n" +
               " * Browse to https://oss.sonatype.org/\n" +
               " * Log in using your Sonatype credentials\n" +
               " * Follow the 'close' instructions at: " + SONATYPE_CLOSING_DIRECTIONS_URL + "\n" +
               "    * To find the repository, visit https://oss.sonatype.org/#stagingRepositories then scroll to the end in the top pane\n" +
               "    * For the close message, enter:  Checker Framework release " + new_checker_version + "\n" +
               " * Click on the Refresh button near the top of the page until the closing\n" +
               "   operation is reported to have completed succesfully.\n" +
               " * Copy the URL of the closed artifacts for use in the next step\n")

        print_step("4c: Run Maven sanity test on Maven central artifacts." )
        if auto or prompt_yes_no( "Run Maven sanity test on Maven central artifacts?", True ):
            repo_url = raw_input( "Please enter the repo URL of the closed artifacts:\n" )

            maven_sanity_check( "maven-staging", repo_url, new_checker_version )

    # This step copies the development release directories to the live release directories.
    # It then adds the appropriate permissions to the release. Symlinks need to be updated to point
    # to the live website rather than the development website. A straight copy of the directory
    # will NOT update the symlinks.

    print_step("Push Step 5. Push dev current release website to live website" ) # SEMIAUTO
    # This step could be performed without asking for user input but I think we should err on the side of caution.
    if not test_mode:
        continue_or_exit("Copy release to the live website?")
        print("Copying to live site")
        copy_releases_to_live_site( new_checker_version, new_afu_version )
        copy_htaccess()
        ensure_group_access_to_releases()
        update_release_symlinks( new_checker_version, new_afu_version )
    else:
        print( "Test mode: Skipping copy to live site!" )

    # This step downloads the checker-framework-X.Y.Z.zip file of the newly live release and ensures we
    # can run the Nullness Checker. If this step fails, you should backout the release.

    print_step( "Push Step 6: Run javac sanity tests on the live release." ) # SEMIAUTO
    if auto or prompt_yes_no( "Run javac sanity test on live release?", True ):
        javac_sanity_check( live_checker_website, new_checker_version )
        if not os.path.isdir( SANITY_TEST_CHECKER_FRAMEWORK_DIR ):
            execute( "mkdir -p " + SANITY_TEST_CHECKER_FRAMEWORK_DIR )
        execute("sh ../../checker-framework/release/test-checker-framework.sh " + new_checker_version, True, False, SANITY_TEST_CHECKER_FRAMEWORK_DIR)
        # Ensure that the jsr308-langtools javac works with the system-wide java launcher
        if not os.path.isdir( SANITY_TEST_JSR308_LANGTOOLS_DIR ):
            execute( "mkdir -p " + SANITY_TEST_JSR308_LANGTOOLS_DIR )
        execute("wget http://types.cs.washington.edu/jsr308/current/jsr308-langtools-" + new_checker_version + ".zip", True, False, SANITY_TEST_JSR308_LANGTOOLS_DIR)
        execute("unzip -uq jsr308-langtools-" + new_checker_version +".zip", True, False, SANITY_TEST_JSR308_LANGTOOLS_DIR)
        execute("env -i bash --noprofile jsr308-langtools-" + new_checker_version + "/dist/bin/javac -version", True, False, SANITY_TEST_JSR308_LANGTOOLS_DIR)

    # You must manually deploy the Eclipse plugin. Follow the instructions at the prompt.

    print_step("Push Step 7: Deploy the Eclipse Plugin to the live site." ) # MANUAL
    continue_or_exit( "Follow the instruction under 'Releasing the Plugin' in checker-framework/eclipse/README-developers.html to " +
                      "deploy the Eclipse plugin to the live website.  Please install the plugin from the new " +
                      "live repository and run it on a file in which you expect a type error.  If you run into errors, " +
                      "back out the release!\n" )

    # Runs the link the checker on all websites at:
    # http://types.cs.washington.edu/
    # The output of the link checker is written to files in the /scratch/$USER/jsr308-release directory whose locations
    # will be output at the command prompt. Review the link checker output.

    # The set of broken links that is displayed by this check will differ from those in push
    # step 2 because the Checker Framework manual and website uses a mix of absolute and
    # relative links. Therefore, some links from the development site actually point to the
    # live site (the previous release). After step 5, these links point to the current
    # release and may be broken.

    print_step( "Push Step 8. Check live site links" ) # SEMIAUTO
    if auto or prompt_yes_no( "Run link checker on LIVE site?", True ):
        check_all_links( live_jsr308_website, live_afu_website, live_checker_website, "live" )

    # This step pushes the changes committed to the interm repositories to the GitHub/Bitbucket
    # repositories. This is the first irreversible change. After this point, you can no longer
    # backout changes and should do another release in case of critical errors.

    print_step( "Push Step 9. Push changes to repositories" ) # SEMIAUTO
    # This step could be performed without asking for user input but I think we should err on the side of caution.
    if not test_mode:
        if prompt_yes_no( "Push the release to GitHub/Bitbucket repositories?  This is irreversible." ):
            push_interm_to_release_repos()
            print( "Pushed to repos" )
    else:
        print( "Test mode: Skipping push to GitHub/Bitbucket!" )

    # This is a manual step that releases the staged Maven artifacts to the actual Central repository.
    # This is also an irreversible step. Once you have released these artifacts they will be forever
    # available to the Java community through the Central repository. Follow the prompts. The Maven
    # artifacts (such as checker-qual.jar) are still needed, but the Maven plug-in is no longer maintained.

    print_step( "Push Step 10. Release staged artifacts in Central repository." ) # MANUAL
    if test_mode:
        msg = ( "Test Mode: You are in test_mode.  Please 'DROP' the artifacts. "   +
                "To drop, log into https://oss.sonatype.org using your " +
                "Sonatype credentials and follow the 'DROP' instructions at: " + SONATYPE_DROPPING_DIRECTIONS_URL )
    else:
        msg = ( "Please 'release' the artifacts, but IMPORTANTLY first ensure that the Checker Framework maven plug-in directory " +
                "(and only that directory) is removed from the artifacts.\n" +
                "First log into https://oss.sonatype.org using your Sonatype credentials. Go to Staging Repositories and " +
                "locate the orgcheckerframework repository and click on it.\n" +
                "Then, in the view for the orgcheckerframework staging repository at the bottom of the page, click on the Content tab. " +
                "Expand the subdirectories until you find the one called checker-framework-plugin. Right-click on it, and choose delete.\n"
                "Finally, click on the Release button at the top of the page.  For the description, write " +
                "Checker Framework release " + new_checker_version + "\n\n")

    # TODO: fix this so that the maven plug-in directory directory is not included in the first place.

    print( msg )
    prompt_until_yes()

    if test_mode:
        msg = ( "Test Mode: You are in test_mode.  If you built the Eclipse plugin on "   +
                "your local machine, you may want to revert any files that were modified." )
    else:
        # A prompt describes the email you should send to all relevant mailing lists.
        # Please fill out the email and announce the release.

        print_step( "Push Step 11. Announce the release." ) # MANUAL
        continue_or_exit( "Please announce the release using the email structure below.\n" +
                          "Note that this text may have changed since the last time a release was performed.\n" +
                           get_announcement_email( new_checker_version ) )

        print_step( "Push Step 12. Push Eclipse plugin files." ) # MANUAL
        msg = ( "If you built the Eclipse plugin on your local machine, there are a few " +
                "changed files with version number changes that need to be pushed.\n" +
                "Do not push the .classpath file. The following files should be pushed:\n" +
                "checker-framework-eclipse-feature/feature.xml\n" +
                "checker-framework-eclipse-plugin/META-INF/MANIFEST.MF\n" +
                "checker-framework-eclipse-update-site/site.xml" )

        print( msg )
        prompt_until_yes()

        print_step( "Push Step 13. Post the Checker Framework and Annotation File Utilities releases on GitHub." ) # MANUAL

        msg = ( "\n" +
                "* Download the following files to your local machine." +
                "\n" +
                "http://types.cs.washington.edu/checker-framework/current/checker-framework-" + new_checker_version + ".zip\n" +
                "http://types.cs.washington.edu/annotation-file-utilities/current/annotation-tools-" + new_afu_version + ".zip\n" +
                "\n" +
                "To post the Checker Framework release on GitHub:\n" +
                "\n" +
                "* Go to https://github.com/typetools/checker-framework/releases/new?tag=checker-framework-" + new_checker_version + "\n" +
                "* For the release title, enter: Checker Framework " + new_checker_version + "\n" +
                "* For the description, insert the latest Checker Framework changelog entry (available at http://types.cs.washington.edu/checker-framework/current/changelog.txt). Please include the first line with the release version and date.\n" +
                "* Find the link below \"Attach binaries by dropping them here or selecting them.\" Click on \"selecting them\" and upload checker-framework-" + new_checker_version + ".zip from your machine.\n" +
                "* Click on the green \"Publish release\" button.\n" +
                "\n" +
                "To post the Annotation File Utilities release on GitHub:\n" +
                "\n" +
                "* Go to https://github.com/typetools/annotation-tools/releases/new?tag=" + new_afu_version + "\n" +
                "* For the release title, enter: Annotation File Utilities " + new_afu_version + "\n" +
                "* For the description, insert the latest Annotation File Utilities changelog entry (available at http://types.cs.washington.edu/annotation-file-utilities/changelog.html). Please include the first line with the release version and date. For bullet points, use the * Markdown character.\n" +
                "* Find the link below \"Attach binaries by dropping them here or selecting them.\" Click on \"selecting them\" and upload annotation-tools-" + new_afu_version + ".zip from your machine.\n" +
                "* Click on the green \"Publish release\" button.\n" )

    print( msg )

    delete_if_exists( RELEASE_BUILD_COMPLETED_FLAG_FILE )

    prompt_until_yes()

if __name__ == "__main__":
    sys.exit(main(sys.argv))
