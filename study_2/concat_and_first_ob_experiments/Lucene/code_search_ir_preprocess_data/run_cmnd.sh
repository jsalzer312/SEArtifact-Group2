#! /bin/bash
export JAVA_HOME=$(/usr/libexec/java_home)
mvn package -DskipTests


: '
List of experiments:
1. Concat-OB-3-Screens: Conduct Bug Localization (Study 2) using top 3 screens from Buggy UI Localization (Study 1) with concat OB
2. Concat-OB-4-Screens: Conduct Bug Localization (Study 2) using top 4 screens from Buggy UI Localization (Study 1) with concat OB
3. First-OB-3-Screens: Conduct Bug Localization (Study 2) using top 3 screens from Buggy UI Localization (Study 1) with first OB
4. First-OB-4-Screens: Conduct Bug Localization (Study 2) using top 4 screens from Buggy UI Localization (Study 1) with first OB
'

exp_names=("Concat-OB-3-Screens" "Concat-OB-4-Screens" "First-OB-3-Screens" "First-OB-4-Screens")

# Runs realpath command, finds and stores full path to be used in all required file paths
FULL_PATH=$(realpath .)

# shellcheck disable=SC2068
for exp_name in ${exp_names[@]}; do
  if [[ $exp_name == "Concat-OB-3-Screens" ]]; then
    screens=("3")
  elif [[ $exp_name == "Concat-OB-4-Screens" ]]; then
    screens=("4")
  elif [[ $exp_name == "First-OB-3-Screens" ]]; then
    screens=("3")
  elif [[ $exp_name == "First-OB-4-Screens" ]]; then
    screens=("4")
  fi

  model_names=("BugLocator" "LUCENE")

  for model_name in ${model_names[@]}; do
    if [[ "${model_name}" == "BugLocator" ]]; then
      export content_types=("Title" "Content")
    elif [[ "${model_name}" == "LUCENE" ]]; then
      export content_types=("Content" "Code")
    fi
    for content_type in ${content_types[@]}; do
      if [[ "$content_type" == "Content" ]]; then

        corpus_type=GUI_State_and_All_GUI_Component_IDs
        if [[ "${model_name}" == "BugLocator" ]]; then
          # TODO: Copy the absolute path of study_2/data/BugReportsContents and replace it in the bug_reports_folder variable
          bug_reports_folder=$FULL_PATH/study_2/data/BugReportsContents
        elif [[ "${model_name}" == "LUCENE" ]]; then
          # TODO: Copy the absolute path of study_2/data/BugReports and replace it in the bug_reports_folder variable
          bug_reports_folder=$FULL_PATH/study_2/data/BugReports
        fi

        # TODO: Copy the absolute path of FilteredUnfilteredFiles folder and replace it in the query_infos_file variable. Keep the "/$exp_name" at the end.
        query_infos_file=$FULL_PATH/study_2/concat_and_first_ob_experiments/FilteredUnfilteredFiles/$exp_name

        if [[ "${model_name}" == "BugLocator" ]]; then
          # TODO: Copy the absolute path of study_2/data/PreprocessedData and replace it in the preprocessed_query_folder variable. Keep the "/$exp_name/Content" at the end.
          preprocessed_query_folder=$FULL_PATH/study_2/data/PreprocessedData/$exp_name/Content
        elif [[ "${model_name}" == "LUCENE" ]]; then
          # TODO: Copy the absolute path of study_2/data/PreprocessedData and replace it in the preprocessed_query_folder variable. Keep the "/$exp_name/Query" at the end.
          preprocessed_query_folder=$FULL_PATH/study_2/data/PreprocessedData/$exp_name/Query
        fi
        rm -rf ${preprocessed_query_folder}
        mkdir -p ${preprocessed_query_folder}


        for i in ${!screens[@]}; do
          "$JAVA_HOME/bin/java" -cp target/code_search_ir-1.0.jar MainClass -br ${bug_reports_folder} \
          -qinfo ${query_infos_file} -s ${screens[$i]} -c ${corpus_type} -preq ${preprocessed_query_folder} \
          -ctype ${content_type}
        done
      elif [[ "${content_type}" == "Title" ]];
      then
        # TODO: Copy the absolute path of study_2/data/BugReportsTitles and replace it in the bug_reports_titles variable.
        bug_reports_titles=$FULL_PATH/study_2/data/BugReportsTitles
        # TODO: Copy the absolute path of study_2/data/PreprocessedData and replace it in the preprocessed_title_folder variable. Keep the "/$exp_name/Title" at the end.
        preprocessed_titles_folder=$FULL_PATH/study_2/data/PreprocessedData/$exp_name/Title
        rm -rf ${preprocessed_titles_folder}
        mkdir -p ${preprocessed_titles_folder}

        for i in ${!screens[@]}; do
          "$JAVA_HOME/bin/java" -cp target/code_search_ir-1.0.jar MainClass -br ${bug_reports_titles} \
          -s ${screens[$i]} -preq ${preprocessed_titles_folder} \
          -ctype ${content_type}
        done
      elif [[ "${content_type}" == "Code" ]];
      then
        # TODO: Copy the absolute path of study_2/data/PreprocessedData and replace it in the preprocessed_code_folder variable. Keep the "/$exp_name/Code" at the end.
        preprocessed_code_folder=$FULL_PATH/study_2/data/PreprocessedData/$exp_name/Code
        rm -rf ${preprocessed_code_folder}
        mkdir -p ${preprocessed_code_folder}
        # TODO: Copy the absolute path of data/BuggyProjects and replace it in the buggy_projects variable.
        buggy_projects=$FULL_PATH/study_2/data/BuggyProjects

        "$JAVA_HOME/bin/java" -cp target/code_search_ir-1.0.jar MainClass \
          -preq ${preprocessed_code_folder} -bp ${buggy_projects}\
          -ctype ${content_type} -s ${screens}
      fi
    done
  done
done