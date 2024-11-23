#! /bin/bash
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

export filtering_list=("GUI_States" "All_GUI_Component_IDs" "GUI_State_and_All_GUI_Component_IDs")
export boosting_list=("GUI_States" "All_GUI_Component_IDs" "GUI_State_and_All_GUI_Component_IDs")
export query_reformulation_list=("GUI_States" "All_GUI_Component_IDs" "GUI_State_and_All_GUI_Component_IDs")

# TODO: Copy the absolute path of study_2/data/JSON-Files-All and replace it in the jsonFilePath variable
export jsonFilePath=$FULL_PATH/study_2/data/JSON-Files-All
# TODO: Copy the absolute path of study_2/data/BuggyProjects and replace it in the buggy_project_dir variable
export buggy_project_dir=$FULL_PATH/study_2/data/BuggyProjects
# TODO: Copy the absolute path of study_2/data/BuggyProjects and replace it in the preprocessed_code_dir variable
export preprocessed_code_dir=$FULL_PATH/study_2/data/BuggyProjects

# shellcheck disable=SC2068
for exp_name in ${exp_names[@]}; do
  if [[ $exp_name == "Concat-OB-3-Screens" ]]; then
    screen_list=("3")
  elif [[ $exp_name == "Concat-OB-4-Screens" ]]; then
    screen_list=("4")
  elif [[ $exp_name == "First-OB-3-Screens" ]]; then
    screen_list=("3")
  elif [[ $exp_name == "First-OB-4-Screens" ]]; then
    screen_list=("4")
  fi

  # TODO: Copy the absolute path of study_2/data/PreprocessedData and replace it in the preprocessedDataPath variable. Keep the "/$exp_name/Query" at the end.
  export preprocessedDataPath=$FULL_PATH/study_2/data/PreprocessedData/$exp_name/Query
  # TODO: Copy the absolute path of study_2/data/PreprocessedData and replace it in the preprocessedCodePath variable. Keep the "/$exp_name/Code" at the end.
  export preprocessedCodePath=$FULL_PATH/study_2/data/PreprocessedData/$exp_name/Code
  # TODO: Copy the absolute path of FilteredBoostedProjects folder and replace it in the filtered_boosted_files_in_repo variable. Keep the "/$exp_name" at the end.
  export filtered_boosted_files_in_repo=$FULL_PATH/study_2/concat_and_first_ob_experiments/FilteredBoostedProjects/$exp_name
  export result_folder=../../results/$exp_name
  rm -rf $result_folder
  mkdir $result_folder
  export final_ranks_folder=../../results/LUCENE-$exp_name
  rm -rf $final_ranks_folder
  mkdir $final_ranks_folder
  # TODO: Copy the absolute path of FilteredUnfilteredFiles folder and replace it in the filtered_boosted_filenames variable. Keep the "/$exp_name" at the end.
  export filtered_boosted_filenames=$FULL_PATH/study_2/concat_and_first_ob_experiments/FilteredUnfilteredFiles/$exp_name

  # For Boosting
  for j in ${!boosting_list[@]}; do
    for k in ${!query_reformulation_list[@]}; do
      for l in ${!screen_list[@]}; do
        echo "Boosting: B-${boosting_list[$j]}#Q-${query_reformulation_list[$k]}#S-${screen_list[$l]}"

        java -cp target/code_search_ir-1.0.jar MainClass -b ${boosting_list[$j]} \
          -q ${query_reformulation_list[$k]} -s ${screen_list[$l]}  \
          -rf $result_folder \
          -bpd $buggy_project_dir -pcd ${preprocessed_code_dir}\
          -fbr $filtered_boosted_files_in_repo -preq $preprocessedDataPath -jpath $jsonFilePath \
          -ops Boosting -prec $preprocessedCodePath -franks $final_ranks_folder \
          -fbfilenames $filtered_boosted_filenames
      done
    done
  done

  #No filtering or boosting, query reformulation only
  for k in ${!query_reformulation_list[@]}; do
    for l in ${!screen_list[@]}; do
      echo "Query Reformulation: Q-${query_reformulation_list[$k]}#S-${screen_list[$l]}"

      java -cp target/code_search_ir-1.0.jar MainClass \
          -q ${query_reformulation_list[$k]} -s ${screen_list[$l]}  \
          -rf $result_folder \
          -bpd $buggy_project_dir -pcd ${preprocessed_code_dir}\
          -fbr $filtered_boosted_files_in_repo -preq $preprocessedDataPath -jpath $jsonFilePath \
          -ops QueryReformulation -prec $preprocessedCodePath -franks $final_ranks_folder \
          -fbfilenames $filtered_boosted_filenames
    done
  done

  #For Filtering and boosting
  for i in ${!filtering_list[@]}; do
    if [[ ${filtering_list[$i]} == "GUI_States" ]]; then
      boosting_gui_type=("Interacted_GUI_Component_IDs")
    elif [[ ${filtering_list[$i]} == "Interacted_GUI_Component_IDs" ]]; then
      boosting_gui_type=("GUI_States")
    else
      boosting_gui_type=()
      index=0
      for j in ${!boosting_list[@]}; do
        if [[ $index -lt $i ]]; then
          boosting_gui_type+=(${boosting_list[j]})
          index=$((index+1))
        else
          break
        fi
      done
    fi

    for j in ${!boosting_gui_type[@]}; do
      for k in ${!query_reformulation_list[@]}; do
        for l in ${!screen_list[@]}; do
          echo "Filtering+Boosting: F-${filtering_list[$i]}#B-${boosting_gui_type[$j]}#Q-${query_reformulation_list[$k]}#S-${screen_list[$l]}"

          java -cp target/code_search_ir-1.0.jar MainClass \
            -f ${filtering_list[$i]} -b ${boosting_gui_type[$j]} \
            -q ${query_reformulation_list[$k]} -s ${screen_list[$l]}  \
            -rf $result_folder \
            -bpd $buggy_project_dir -pcd ${preprocessed_code_dir}\
            -fbr $filtered_boosted_files_in_repo -preq $preprocessedDataPath -jpath $jsonFilePath \
            -ops Filtering+Boosting -prec $preprocessedCodePath -franks $final_ranks_folder \
            -fbfilenames $filtered_boosted_filenames
        done
      done
    done
    #echo "\n"
  done

  #For Filtering
  for i in ${!filtering_list[@]}; do
    for k in ${!query_reformulation_list[@]}; do
      for l in ${!screen_list[@]}; do
        echo "Filtering: F-${filtering_list[$i]}#Q-${query_reformulation_list[$k]}#S-${screen_list[$l]}"

        java -cp target/code_search_ir-1.0.jar MainClass \
            -f ${filtering_list[$i]} \
            -q ${query_reformulation_list[$k]} -s ${screen_list[$l]}  \
            -rf $result_folder \
            -bpd $buggy_project_dir -pcd ${preprocessed_code_dir}\
            -fbr $filtered_boosted_files_in_repo -preq $preprocessedDataPath -jpath $jsonFilePath \
            -ops Filtering -prec $preprocessedCodePath -franks $final_ranks_folder \
            -fbfilenames $filtered_boosted_filenames
      done
    done
  done
done
