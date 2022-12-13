#!/usr/bin/env bash

echo "=========================== Starting Release Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# For PR builds only execute a Dry Run of the release
[ "$GITHUB_EVENT" != "pull_request" ] && DRY_RUN="" || DRY_RUN="-DdryRun"

# Travis CI runner work on DETACHED HEAD, so we need to checkout the release branch
git checkout -B "${BRANCH_NAME}"

git config user.email "build@alfresco.com"

# Run the release plugin - with "[skip ci]" in the release commit message
mvn -B \
    ${DRY_RUN} \
    -Dmaven.javadoc.failOnError=false \
    "-Darguments=-DskipTests -Dmaven.javadoc.skip -Dadditionalparam=-Xdoclint:none -Dbuildnumber=$GITHUB_RUN_NUMBER" \
    -DscmCommentPrefix="[maven-release-plugin][skip ci] " \
    release:clean release:prepare release:perform \
    -Dusername=alfresco-build \
    -Dpassword=${GIT_PASSWORD}

popd
set +vex
echo "=========================== Finishing Release Script =========================="