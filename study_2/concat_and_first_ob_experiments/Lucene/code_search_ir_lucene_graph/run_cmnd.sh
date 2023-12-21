#! /bin/bash
mvn package -DskipTests

: '
List of experiments:
1. Concat-OB-3-Screens: Conduct Bug Localization (Study 2) using top 3 screens from Buggy UI Localization (Study 1) with concat OB
2. Concat-OB-4-Screens: Conduct Bug Localization (Study 2) using top 4 screens from Buggy UI Localization (Study 1) with concat OB
3. First-OB-3-Screens: Conduct Bug Localization (Study 2) using top 3 screens from Buggy UI Localization (Study 1) with first OB
4. First-OB-4-Screens: Conduct Bug Localization (Study 2) using top 4 screens from Buggy UI Localization (Study 1) with first OB
'

#TODO: Change the experiment name for each experiment
exp_name="Concat-OB-4-Screens"
# TODO: Update for each experiment
export screen_list=("4")

export preprocessedDataPath=../../data/PreprocessedData/$exp_name/Query
export preprocessedCodePath=../../data/PreprocessedData/$exp_name/Code
# TODO: Provide the absolute path of data/JSON-Files-All
export jsonFilePath=X/Y/Z/data/JSON-Files-All
# TODO: Provide the absolute path of data/BuggyProjects
export buggy_project_dir=X/Y/Z/data/BuggyProjects
# TODO: Provide the absolute path of FilteredBoostedProjects/$exp_name
export filtered_boosted_files_in_repo=X/Y/Z/FilteredBoostedProjects/$exp_name
# TODO: Provide the absolute path of results/$exp_name
export result_folder=X/Y/Z/results/$exp_name
rm -rf $result_folder
mkdir $result_folder
# TODO: Provide the absolute path of results/LUCENE-$exp_name
export final_ranks_folder=X/Y/Z/results/LUCENE-$exp_name
rm -rf $final_ranks_folder
mkdir $final_ranks_folder
# TODO: Provide the absolute path of SourceCodeMapping/MappingAndroidProject/$exp_name
export filtered_boosted_filenames=X/Y/Z/SourceCodeMapping/MappingAndroidProject/$exp_name
# TODO: Provide the absolute path of data/BuggyProjects
export preprocessed_code_dir=X/Y/Z/data/BuggyProjects

export filtering_list=("GUI_States" "All_GUI_Component_IDs" "GUI_State_and_All_GUI_Component_IDs")
export boosting_list=("GUI_States" "All_GUI_Component_IDs" "GUI_State_and_All_GUI_Component_IDs")
export query_reformulation_list=("GUI_States" "All_GUI_Component_IDs" "GUI_State_and_All_GUI_Component_IDs")



# For Boosting
# shellcheck disable=SC2068
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
# shellcheck disable=SC2068
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
# shellcheck disable=SC2068
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
# shellcheck disable=SC2068
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