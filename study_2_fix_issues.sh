### ============================================= ###
### Shell script to fix issues related to study_2 ###
### ============================================= ###

# Calculates the full path and stores the results in FULL_PATH
FULL_PATH=$(realpath .)

# All of the filepaths that need to be adjusted
UNFILTERED_FILES_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/SourceCodeMapping/MappingAndroidProject/get_filtered_unfiltered_files.py"
READ_FILES_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/SourceCodeMapping/MappingAndroidProject/read_files.py"
MATCH_FILES_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/ShortScripts/match_files_from_repo.sh"
PREPROCESS_MAIN_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/Lucene/code_search_ir_preprocess_data/src/main/java/MainClass.java"
LUCENE_GRAPH_MAIN_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/Lucene/code_search_ir_lucene_graph/src/main/java/MainClass.java"

#Buggy_Reports
DATA_BUGREPORTS_BUG_FILE_PATH="\"${FULL_PATH}/study_2/data/BugReports/bug_report_\" + bug_id + \".txt\""

#BugReports
DATA_BUGGYPROJECT_BUG_FILE_PATH="\"${FULL_PATH}/study_2/data/BuggyProjects/bug-\" + bug_id"
DATA_TRACEREPLAYER_DATA_TR_FILE_PATH="'$FULL_PATH/study_2/data/TraceReplayer-Data/TR' + bug_id + '/Execution-1.json'"

#JsonFile
DATA_JSON_FILES_ALL_FILE_PATH="$FULL_PATH/study_2/data/JSON-Files-All"

#Base file paths for BuggyProjects, BugReports, BugReportsContents, and BugReportsTitles
BASE_DATA_BUGGYPROJECT_FILE_PATH=${FULL_PATH}/study_2/data/BuggyProjects/
BASE_DATA_BUGREPORTS_FILE_PATH=${FULL_PATH}/study_2/data/BugReports/
BASE_DATA_BUGREPORTSCONTENT_FILE_PATH=${FULL_PATH}/study_2/data/BugReportsContents/
BASE_DATA_BUGREPORTSTITLES_FILE_PATH=${FULL_PATH}/study_2/data/BugReportsTitles/


# Check file paths are correct
echo "\n \t\t~~~Check that the following obsolute paths are correct~~~\n"
ls "$UNFILTERED_FILES_PATH"
ls "$READ_FILES_PATH"
ls "$MATCH_FILES_PATH"
ls "$PREPROCESS_MAIN_PATH"
ls "$LUCENE_GRAPH_MAIN_PATH"

# Use sed to update the file path variables with the full path for the local machine
sed -i"" "s|^\(\s\s\s\s*\)parent_directory = .*|\1parent_directory = $DATA_BUGGYPROJECT_BUG_FILE_PATH|" "$UNFILTERED_FILES_PATH"
sed -i"" "s|^\(\s\s\s\s*\)json_file = open(.*|\1json_file = open($DATA_TRACEREPLAYER_DATA_TR_FILE_PATH)|" "$UNFILTERED_FILES_PATH"

sed -i"" "s|^\(\s*\)parent_directory = .*|\1parent_directory = $DATA_BUGGYPROJECT_BUG_FILE_PATH|" "$READ_FILES_PATH"
sed -i"" "s|^\(\s*\)bug_report_file = .*/data/BugReports.*|\1bug_report_file = ${DATA_BUGREPORTS_BUG_FILE_PATH}|" "$READ_FILES_PATH"

sed -i'' "s|^\(export buggy_project_dir=\).*|\1$BASE_DATA_BUGGYPROJECT_FILE_PATH|" $MATCH_FILES_PATH
sed -i'' "s|^\(export buggy_project_dir_in_csv=\).*|\1$BASE_DATA_BUGGYPROJECT_FILE_PATH|" $MATCH_FILES_PATH

sed -i"" "s|^\([[:space:]]*\)String json_file = \".*\";|\1String json_file = \"$DATA_JSON_FILES_ALL_FILE_PATH/\" + bug_id + \".json\";|" "$PREPROCESS_MAIN_PATH"
sed -i"" "s|^\([[:space:]]*\)String buggy_project_dir = \".*\";|\1String buggy_project_dir = \"$BASE_DATA_BUGGYPROJECT_FILE_PATH\";|" "$PREPROCESS_MAIN_PATH"

sed -i"" "s|^\([[:space:]]*\)String buggy_project_dir = \".*\";|\1String buggy_project_dir = \"$BASE_DATA_BUGGYPROJECT_FILE_PATH\";|" "$LUCENE_GRAPH_MAIN_PATH"

echo "\n \t\t~~~Change the following file paths to the correct absolute path~~~\n"
echo "$UNFILTERED_FILES_PATH\n"
echo "$READ_FILES_PATH\n"
echo "$MATCH_FILES_PATH\n"
echo "$PREPROCESS_MAIN_PATH\n"
echo "$LUCENE_GRAPH_MAIN_PATH\n"