# Summary
This directory contains all the necessary codes to reproduce the bug localization experiments of study 2 considering the concat OB and first OB from buggy UI localization (study 1) results.
## List of experiments:
1. Concat-OB-3-Screens: Conduct Bug Localization (Study 2) using top 3 screens from Buggy UI Localization (Study 1) with concat OB
2. Concat-OB-4-Screens: Conduct Bug Localization (Study 2) using top 4 screens from Buggy UI Localization (Study 1) with concat OB
3. First-OB-3-Screens: Conduct Bug Localization (Study 2) using top 3 screens from Buggy UI Localization (Study 1) with first OB
4. First-OB-4-Screens: Conduct Bug Localization (Study 2) using top 4 screens from Buggy UI Localization (Study 1) with first OB
# How to reproduce results of all 4 experiments?
## Prerequisites
1. Install JDK 11+ and Apache Maven(3.6.3). Add both JDK and Maven to your PATH environment variable.
2. Download the data folder and extract it in this directory (concat_and_first_ob_experiments). Download link: https://drive.google.com/file/d/1e6X6mOktwQSBVW-LwKP1axBMRtyK1GOr/view?usp=drive_link
3. Update the paths (provide the absolute paths) of the code files: paths that need to be updated are marked by TODO comments.
## Steps
Run the following scripts in the given order to reproduce the results of all 4 experiments:
1. ```SourceCodeMapping/MappingAndroidProject/filter_files_cmnd.sh``` : Get all the filtered corpus and files which we need to boost. The outcome of this step will be saved into ```FilteredUnfilteredFiles``` directory for all 4 experiments.
2. ```ShortScripts/match_files_from_repo.sh```: Copy and paste files to a new directory based on query matching. The outcome of this step will be saved into ```FilteredBoostedProjects``` directory for all 4 experiments.
3. ```Lucene/code_search_ir_preprocess_data/run_cmnd.sh```: Preprocess queries. The outcome of this step will be saved into ```data/PreprocessedData``` directory for all 4 experiments.
### Lucene
4. ```Lucene/code_search_ir_lucene_graph/run_cmnd.sh```: Run to get bug localization results (study 2) with Lucene for all the 4 experiments. The results will be saved into ```results``` directory for all 4 experiments.
### BugLocator
5. ```BugLocator/generate_xml_data_for_buglocator.sh```: Preprocess queries specifically for BugLocator. The outcome of this step will be saved into ```data/PreprocessedData``` directory for all 4 experiments.
6. ```BugLocator/buglocator_cmnd.sh```: Run to get bug localization results (study 2) with BugLocator for all the 4 experiments. The results will be saved into ```results``` directory for all 4 experiments.
### Process Results
7. ```FinalResultComputation/get_final_results_lucene.py```: Run to process the LUCENE results of all 4 experiments. The final results will be saved into ```FinalResults/LUCENE``` directory for all 4 experiments.
8. ```FinalResultComputation/get_final_results_bug_locator.py```: Run to process the BugLocator results of all 4 experiments. The final results will be saved into ```FinalResults/BugLocator``` directory for all 4 experiments.
9. ```FinalResultComputation/create_results_spreadsheet.py```: Run to create spreadsheet for results summary of all experiments. The spreadsheets will be saved into ```ResultsSummary``` directory for both LUCENE and BugLocator.