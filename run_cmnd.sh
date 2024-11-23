#!/bin/bash

### ==============================
### Study 1: Buggy UI Localization
### ==============================

## Screen localization (SL) and component localization (CL) using LUCENE
## ---------------------------------------------------------------------

set -x #echo on

# JAVA_HOME path (JRE/JDK 11+)
export JAVA_HOME=$(/usr/libexec/java_home)

# REPOSITORIES_PATH: path to the folder where all the required repositories are located
export REPOSITORIES_PATH=required_projects

export CUR_DIR=`pwd`
export APPCORE_REPO_PATH=$REPOSITORIES_PATH/appcore
export TXT_ANALYZER_REPO_PATH=$REPOSITORIES_PATH/text-analyzer
export IR_ENGINE_PATH=$CUR_DIR/study_1/lucene

# project building
cd $APPCORE_REPO_PATH/appcore && ./gradlew clean testClasses install
cd $TXT_ANALYZER_REPO_PATH/text-analyzer && ./gradlew clean testClasses install

# install additional libraries
cd $IR_ENGINE_PATH/lib
mvn install:install-file -Dfile=ir4se-fwk-0.0.2.jar -DgroupId=edu.wayne.cs.severe -DartifactId=ir4se-fwk -Dversion=0.0.2 -Dpackaging=jar
cd $IR_ENGINE_PATH && mvn package -DskipTests
"$JAVA_HOME"/bin/java -cp target/ir_engine-1.0.jar ScreenLocalization
cd $CUR_DIR/study_1/lucene
$JAVA_HOME/bin/java -cp target/ir_engine-1.0.jar ComponentLocalization
cd $CUR_DIR


## Screen localization (SL) and component localization (CL) using Sentence-BERT
## ----------------------------------------------------------------------------
echo "Performing Screen Localization (SL)"
python3 study_1/sentence_bert/screen_and_component_localization.py SL

echo "Performing Component Localization (CL)"
python3 study_1/sentence_bert/screen_and_component_localization.py CL

## Screen localization (SL) and component localization (CL) using CLIP
## -------------------------------------------------------------------
echo "Performing Screen Localization (SL) using CLIP"
python3 study_1/clip/screen_localization.py

echo "Performing Component Localization (CL) using CLIP"
python3 study_1/clip/component_localization.py

## Screen localization (SL) and component localization (CL) using BLIP
## -------------------------------------------------------------------
echo "Performing Screen Localization (SL) using BLIP"
python3 study_1/blip/screen_localization.py

echo "Performing Component Localization (CL) using BLIP"
python3 study_1/blip/component_localization.py


## Result Summary Generation
## -------------------------
python3 study_1/results/create_fine_grained_results.py
python3 study_1/results/add_synthetic_bugs_column.py
python3 study_1/results/create_results_summary.py
## Result Graph Generation
## -----------------------
python3 study_1/results/create_results_summary_graph.py
python3 study_1/results/create_graph_cl.py
python3 study_1/results/create_graph_sl.py