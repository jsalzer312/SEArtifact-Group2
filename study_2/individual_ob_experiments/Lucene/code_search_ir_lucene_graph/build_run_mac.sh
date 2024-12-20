#!/bin/sh
set JAVA_HOME=/usr/libexec/java_home -v 1.11
set REPOSITORIES_PATH=BugLocalization/Lucene

set CUR_DIR=$CD

set APPCORE_REPO_PATH=BugLocalization/Lucene/appcore/appcore
set TXT_ANALYZER_REPO_PATH=BugLocalization/Lucene/text-analyzer/text-analyzer
set CODE_SEARCH_REPO_PATH=BugLocalization/Lucene/code_search_ir
set CODE_SEARCH_REPO_PATH_LIB=BugLocalization/Lucene/code_search_ir/lib

cd "${APPCORE_REPO_PATH}" && ./gradlew clean testClasses install && @echo on
cd "${TXT_ANALYZER_REPO_PATH}" && ./gradlew clean testClasses install && @echo on

echo "Hello"

cd "${CODE_SEARCH_REPO_PATH_LIB}"
mvn install:install-file "-Dfile=ir4se-fwk-0.0.2.jar" "-DgroupId=edu.wayne.cs.severe" "-DartifactId=ir4se-fwk" "-Dversion=0.0.2" "-Dpackaging=jar"

cd "$CODE_SEARCH_REPO_PATH" && mvn package -DskipTests && @echo on

"$JAVA_HOME/bin/java" -cp target/code_search_ir-1.0.jar MainClass

cd "$CUR_DIR"