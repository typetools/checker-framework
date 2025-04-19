#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export ORG_GRADLE_PROJECT_useJdk21Compiler=true
source "$SCRIPTDIR"/clone-related.sh
./gradlew assembleForJavac --console=plain -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

# TODO: Maybe I should move this into the CI job, and do it for all CI jobs.
cp "$SCRIPTDIR"/mvn-settings.xml ~/settings.xml

"$SCRIPTDIR/.git-scripts/git-clone-related" typetools guava
cd ../guava

if [ "$TRAVIS" = "true" ] ; then
  # There are two reasons that this script does not work on Travis.
  # 1. Travis kills jobs that do not produce output for 10 minutes.  (This can be worked around.)
  # 2. Travis kills jobs that produce too much output.  (This cannot be worked around.)
  echo "On Travis, use scripts that run just one type-checker."
  exit 1
fi

## This command works locally, but on Azure it fails with timouts while downloading Maven dependencies.
# cd guava && time mvn --debug -B package -P checkerframework-local -Dmaven.test.skip=true -Danimal.sniffer.skip=true

# Pre-download Maven dependencies.  Otherwise there are sometimes timeouts when downloading a Maven dependency.
(cd guava && \
(timeout 5m mvn -B dependency:resolve-plugins || (sleep 1m && (timeout 5m mvn -B dependency:resolve-plugins || true))))
# This downloads even more dependencies, but the above seems to be sufficient.
# (cd guava && \
# (timeout 5m mvn -B dependency:go-offline || (sleep 1m && (timeout 5m mvn -B dependency:go-offline || true))))

# Comment about -D flags: the maven.wagon settings should not be relevant to Maven 3.9 and later, but try them anyway.
# (I saw Maven take 30 minutes to download a dependency even with all these flags.)
(cd guava && \
time mvn --debug -B compile -P checkerframework-local \
  -Dhttp.keepAlive=false -Daether.connector.http.connectionMaxTtl=25 -Dmaven.wagon.http.pool=false -Dmaven.wagon.httpconnectionManager.ttlSeconds=120)
