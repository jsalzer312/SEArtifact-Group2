### ============================================= ###
### Shell script to fix issues related to study_2 ###
### ============================================= ###


FULL_PATH=$(realpath .)

# All of the filepaths that need to be adjusted
UNFILTERED_FILES_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/SourceCodeMapping/MappingAndroidProject/get_filtered_unfiltered_files.py"
READ_FILES_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/SourceCodeMapping/MappingAndroidProject/read_files.py"
MATCH_FILES_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/ShortScripts/match_files_from_repo.sh"
PREPROCESS_DATA_CMD_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/Lucene/code_search_ir_preprocess_data/run_cmnd.sh"
PREPROCESS_MAIN_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/Lucene/code_search_ir_preprocess_data/src/main/java/MainClass.java"
LUCENE_GRAPH_CMD_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/Lucene/code_search_ir_lucene_graph/run_cmnd.sh"
LUCENE_GRAPH_MAIN_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/Lucene/code_search_ir_lucene_graph/src/main/java/MainClass.java"
BUGLOCATER_PATH="$FULL_PATH/study_2/concat_and_first_ob_experiments/BugLocator/buglocator_cmnd.sh"

#Buggy_Reports
DATA_BUGREPORTS_BUG_FILE_PATH="\"${FULL_PATH}/study_2/data/BugReports/bug_report_\" + bug_id + \".txt\""
#BugReports
DATA_BUGGYPROJECT_BUG_FILE_PATH="\"${FULL_PATH}/study_2/data/BuggyProjects/bug-\" + bug_id"
DATA_TRACEREPLAYER_DATA_TR_FILE_PATH="'$FULL_PATH/study_2/data/TraceReplayer-Data/TR' + bug_id + '/Execution-1.json'"

BASE_DATA_BUGGYPROJECT_FILE_PATH=${FULL_PATH}/study_2/data/BuggyProjects
BASE_DATA_BUGREPORTS_FILE_PATH=${FULL_PATH}/study_2/data/BugReports
BASE_DATA_BUGREPORTSCONTENT_FILE_PATH=${FULL_PATH}/study_2/data/BugReportsContents
BASE_DATA_BUGREPORTSTITLES_FILE_PATH=${FULL_PATH}/study_2/data/BugReportsTitles



echo "$DATA_TRACEREPLAYER_DATA_TR_FILE_PATH"
echo "$DATA_BUGGYPROJECT_BUG_FILE_PATH"

# Check file paths are correct
echo "\n \t\t~~~Check that the following obsolute paths are correct~~~\n"
ls "$UNFILTERED_FILES_PATH"
ls "$READ_FILES_PATH"
ls "$MATCH_FILES_PATH"
ls "$PREPROCESS_DATA_CMD_PATH"
ls "$PREPROCESS_MAIN_PATH"
ls "$LUCENE_GRAPH_CMD_PATH"
ls "$LUCENE_GRAPH_MAIN_PATH"
ls "$BUGLOCATER_PATH"



sed -i"" "s|^\(\s\s\s\s*\)parent_directory = .*|\1parent_directory = $DATA_BUGGYPROJECT_BUG_FILE_PATH|" "$UNFILTERED_FILES_PATH"
sed -i"" "s|^\(\s\s\s\s*\)json_file = open(.*|\1json_file = open($DATA_TRACEREPLAYER_DATA_TR_FILE_PATH)|" "$UNFILTERED_FILES_PATH"

sed -i"" "s|^\(\s*\)parent_directory = .*|\1parent_directory = $DATA_BUGGYPROJECT_BUG_FILE_PATH|" "$READ_FILES_PATH"
sed -i"" "s|^\(\s*\)bug_report_file = .*/data/BugReports.*|\1bug_report_file = ${DATA_BUGREPORTS_BUG_FILE_PATH}|" "$READ_FILES_PATH"

sed -i'' "s|^\(export buggy_project_dir=\).*|\1$BASE_DATA_BUGGYPROJECT_FILE_PATH|" $MATCH_FILES_PATH
sed -i'' "s|^\(export buggy_project_dir_in_csv=\).*|\1$BASE_DATA_BUGGYPROJECT_FILE_PATH|" $MATCH_FILES_PATH



# # Apply sed commands using the variables
# sed -i "" "s|^\s*bug_reports_folder=.*|bug_reports_folder=\"$BASE_DATA_BUGREPORTSCONTENT_FILE_PATH\"|" "$PREPROCESS_DATA_CMD_PATH"  # Line 41
# sed -i "" "s|^\s*bug_reports_folder=.*|bug_reports_folder=\"$BASE_DATA_BUGREPORTS_FILE_PATH\"|" "$PREPROCESS_DATA_CMD_PATH"         # Line 44
# sed -i "" "s|^\s*query_infos_file=.*|query_infos_file=\"$QUERY_INFOS_FILE_PATH\"|" "$PREPROCESS_DATA_CMD_PATH"        # Line 48
# sed -i "" "s|^\s*preprocessed_query_folder=.*|preprocessed_query_folder=\"$PREPROCESSED_QUERY_CONTENT_PATH\"|" "$PREPROCESS_DATA_CMD_PATH"  # Line 52
# sed -i "" "s|^\s*preprocessed_query_folder=.*|preprocessed_query_folder=\"$PREPROCESSED_QUERY_QUERY_PATH\"|" "$PREPROCESS_DATA_CMD_PATH"    # Line 55
# sed -i "" "s|^\s*bug_reports_titles=.*|bug_reports_titles=\"$BUG_REPORTS_TITLES_PATH\"|" "$PREPROCESS_DATA_CMD_PATH"  # Line 69
# sed -i "" "s|^\s*preprocessed_titles_folder=.*|preprocessed_titles_folder=\"$PREPROCESSED_TITLES_FOLDER_PATH\"|" "$PREPROCESS_DATA_CMD_PATH"  # Line 71
# sed -i "" "s|^\s*preprocessed_code_folder=.*|preprocessed_code_folder=\"$PREPROCESSED_CODE_FOLDER_PATH\"|" "$PREPROCESS_DATA_CMD_PATH"      # Line 83
# sed -i "" "s|^\s*buggy_projects=.*|buggy_projects=\"$BUGGY_PROJECTS_PATH\"|" "$PREPROCESS_DATA_CMD_PATH"              # Line 87



echo "\n \t\t~~~Change the following file paths to the correct absolute path~~~\n"
echo "$UNFILTERED_FILES_PATH"