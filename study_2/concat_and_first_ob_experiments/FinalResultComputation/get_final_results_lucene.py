import pandas as pd
import argparse
from ast import literal_eval
import csv
import json
import numpy as np
import os
import glob


def write_to_csv(write_file, row):
    with open(write_file, 'a') as file:
        writer = csv.writer(file)
        writer.writerow(row)


def write_rankings_row(filename, approach, ranklist_br_best_avg, ranklist_rq_best_avg, ranklist_qe1_best_avg,
                       ranklist_qe2_best_avg, ranklist_qe3_best_avg):
    result_row = []
    result_row.append(approach)
    result_row.append(ranklist_br_best_avg)
    result_row.append(ranklist_rq_best_avg)
    result_row.append(ranklist_qe1_best_avg)
    result_row.append(ranklist_qe2_best_avg)
    result_row.append(ranklist_qe3_best_avg)
    write_to_csv(filename, result_row)


def cal_avg(ranks):
    if len(ranks) == 0:
        return 0
    cnt = 0
    sum1 = 0
    for rank in ranks:
        if rank == []:
            continue
        else:
            cnt += 1
            sum1 += rank

    return sum1 / cnt


def get_java_file_count(bug_id):
    filename = "Statistics/java_file_count.csv"
    ranklist_df = pd.read_csv(filename)

    java_file_count = ranklist_df.loc[ranklist_df['Bug_Id'] == int(bug_id), "Number of Java files"].values.tolist()
    if java_file_count is None or len(java_file_count) < 1 or java_file_count != java_file_count:
        return 0
    return java_file_count[0]


def get_buggy_java_files(bug_issue_id):
    ######## Read json file containing the correct bug locations ###########
    json_file = "../data/JSON-Files-All/" + str(bug_issue_id) + ".json"
    with open(json_file, 'r') as jfile:
        json_data = jfile.read()
    original_buggy_locations = json.loads(json_data)

    unique_java_files = set()
    for bug_location in original_buggy_locations["bug_location"]:
        buggy_file = bug_location["file_name"]
        if buggy_file.endswith(".java"):
            unique_java_files.add(buggy_file)

    return unique_java_files


def get_best_ranklist(ranklist):
    min_ranks = []
    for ranks in ranklist:
        min_ranks.append(min(ranks) if ranks else [])
    return min_ranks


def get_ranklist_len(ranklist):
    len_ranks_list = []
    for ranks in ranklist:
        len_ranks_list.append(len(ranks))
    return len_ranks_list


def calculate_final_metric_values_file_level_for_bug_id(ranks, K, bug_id):
    rr_K = calculate_RR(ranks, K)
    hit_K = calculate_hitK(ranks, K)
    ap_K = calculate_average_precisionK(ranks, K, bug_id)

    return rr_K, hit_K, ap_K


# Calculate mean receiprocal rank@K (MRR@K)
def calculate_RR(ranks, K):
    first_rank = 0
    for rank in ranks:
        if rank <= K:
            first_rank = 1 / rank
            break

    return first_rank


# Calculate hit@K
def calculate_hitK(ranks, K):
    hit = 0
    for rank in ranks:
        if rank <= K:
            hit = 1
            break
    return hit


# Calculate average precision@K (AP@K)
def calculate_average_precisionK(ranks, K, bug_id):
    buggy_k = 0
    precision_k = 0
    buggy_count = 0
    total_precision_k = 0
    # Check all the ranks from 1 to K
    for rank in range(1, K + 1):
        if rank in ranks:
            buggy_k = 1
            buggy_count += 1
        else:
            buggy_k = 0

        precision_k = buggy_count / rank
        total_precision_k += precision_k * buggy_k

    total_number_of_buggy_files = len(get_buggy_java_files(bug_id))

    if total_number_of_buggy_files == 0:
        return 0

    average_precision_k = total_precision_k / total_number_of_buggy_files
    return average_precision_k


def calulate_file_metrics_for_all_K(rankList, bug_report_ids, K_values):
    rr_list = []
    hit_list = []
    ap_list = []

    for i in range(len(bug_report_ids)):
        ranks = rankList[i]
        bug_id = bug_report_ids[i]

        rr_K_values = []
        hit_K_values = []
        ap_K_values = []
        for k in K_values:
            rr_K, hit_K, ap_K = calculate_final_metric_values_file_level_for_bug_id(ranks, k, bug_id)
            rr_K_values.append(rr_K)
            hit_K_values.append(hit_K)
            ap_K_values.append(ap_K)

        java_file_count = get_java_file_count(bug_id)
        rr_K, hit_K, ap_K = calculate_final_metric_values_file_level_for_bug_id(ranks, java_file_count, bug_id)
        rr_K_values.append(rr_K)
        hit_K_values.append(hit_K)
        ap_K_values.append(ap_K)

        rr_list.append(rr_K_values)
        hit_list.append(hit_K_values)
        ap_list.append(ap_K_values)

    return rr_list, hit_list, ap_list


def get_file_content(file_name):
    # Read the file
    file_content = open(file_name, "r")
    file_content = file_content.read()
    return file_content


def remove_ranks_blank_text(bug_id, query_reformulation_gui, col_name, screen, prep_query_dataset):
    query_reformulation_type_name = ""
    if col_name == "Ranks (Query-Bug Report)":
        query_reformulation_type_name = "bug_report_original"
    elif col_name == "Ranks (Query Replacement)":
        query_reformulation_type_name = "replaced_query"
    elif col_name == "Ranks (Query Expansion 1)":
        query_reformulation_type_name = "query_expansion_1"

    # TODO: Change the path
    bug_report_contents_file = "../../data/PreprocessedData/" + prep_query_dataset + "/Screen-" + screen + "/Preprocessed_with_" + query_reformulation_gui + "/" + query_reformulation_type_name + "/bug_report_" + bug_id + ".txt"

    bug_report_contents = get_file_content(bug_report_contents_file)
    if bug_report_contents is None or len(bug_report_contents) < 1:
        return True

    return False


def get_ranks(bug_id, filename, query_reformulation_gui, col_name, screen, prep_query_dataset):
    ranklist_df = pd.read_csv(filename)
    if not ranklist_df['Bug Report ID'].isin([int(bug_id)]).any():
        print(f'Bug Id: {bug_id} does not exist in {filename}')
    ranks_for_bug_id = ranklist_df.loc[ranklist_df['Bug Report ID'] == int(bug_id), col_name].values.tolist()
    if ranks_for_bug_id is None or len(ranks_for_bug_id) < 1 or ranks_for_bug_id != ranks_for_bug_id:
        return "[]"
    if remove_ranks_blank_text(bug_id, query_reformulation_gui, col_name, screen, prep_query_dataset):
        return "[]"
    return ranks_for_bug_id[0]


def get_ranklist(bug_report_ids, filename, query_reformulation_gui, col_name, screen, prep_query_dataset):
    ranklist = []
    identified_bugs_count = 0
    for bug_id in bug_report_ids:
        ranks = get_ranks(bug_id, filename, query_reformulation_gui, col_name, screen, prep_query_dataset)
        if ranks is None or ranks != ranks:
            ranklist.append([])
        else:
            ranklist.append(literal_eval(ranks))
            if len(literal_eval(ranks)) > 0:
                identified_bugs_count += 1
    return identified_bugs_count, ranklist


def get_bug_report_ids():
    if screen_list[0] == "3":
        bug_report_ids = ["2", "1028", "8", "10", "11", "18", "19", "1563", "1568", "44", "45", "1073", "53", "54", "55",
                          "56", "1089", "71", "1096", "76", "84", "87", "92", "1640", "1641", "106", "1130", "1645", "110",
                          "117", "1146", "1147", "1150", "1151", "128", "129", "130", "135", "158", "159", "160", "162",
                          "168", "1198", "1202", "1205", "1207", "1213", "1214", "191", "192", "193", "1215", "1222", "199",
                          "200", "201", "1223", "1224", "1228", "206", "209", "248", "256", "271", "275", "1299", "1389",
                          "1399", "1402", "1403", "1406", "1425", "1428", "1430", "1441", "1445", "1446", "1481"]
    elif screen_list[0] == "4":
        bug_report_ids = ["2", "8", "10", "18", "19", "44", "53", "117", "128", "129", "130", "135", "206", "209", "256",
                          "1073", "1096", "1202", "1207", "1214", "1215", "1224", "1299", "1399", "1430", "1441", "1481",
                          "1645", "54", "76", "92", "158", "160", "162", "192", "199", "200", "1150", "1198", "1228",
                          "1389", "1425", "1446", "1568", "71", "201", "1146", "1147", "1151", "1205", "1406", "1445", "45",
                          "106", "110", "168", "248", "1563", "1223", "1641", "55", "56", "1213", "1222", "1428", "84",
                          "87", "159", "193", "271", "275", "1028", "1089", "1130", "1402", "1403", "1640"]
    return bug_report_ids


def get_all_bug_report_ids():
    bug_report_ids = get_bug_report_ids()
    return bug_report_ids


def get_best_ranklist_avg(ranklist):
    best_ranklist = get_best_ranklist(ranklist)
    best_ranklist_avg = cal_avg(best_ranklist)
    return best_ranklist_avg


def total_number_of_ranked_files(ranklist):
    len_ranks_list = get_ranklist_len(ranklist)
    return np.sum(len_ranks_list)


def get_average_metrics(metrics):
    metric_values = [np.array(item) for item in metrics]
    return [np.mean(m_val) for m_val in zip(*metric_values)]


def get_result_row(rr_list, hit_list, ap_list, bug_report_ids):
    hit = get_average_metrics(hit_list)
    mrr = get_average_metrics(rr_list)
    map = get_average_metrics(ap_list)
    # print(f"MRR: {mrr}")
    # print(f"MAP: {map}")

    number_of_hits_10 = 0
    hit_10_list = []
    # print("hit_list: ", hit_list)
    for i in range(len(hit_list)):
        if hit_list[i][9] == 1:
            number_of_hits_10 += 1
            hit_10_list.append(bug_report_ids[i])

    result_row = []
    result_row.append(hit[0])
    result_row.append(hit[1])
    result_row.append(hit[2])
    result_row.append(hit[3])
    result_row.append(hit[4])
    result_row.append(hit[9])
    result_row.append(number_of_hits_10)
    result_row.append(np.mean(hit[:10]))
    result_row.append(hit[14])
    result_row.append(hit[19])
    result_row.append(np.mean(hit[:20]))
    result_row.append(mrr[21])
    result_row.append(map[21])
    result_row.append(hit_10_list)
    return result_row


def metrics_result(rankList, bug_report_ids, K_values):
    rr_list, hit_list, ap_list = calulate_file_metrics_for_all_K(rankList, bug_report_ids, K_values)
    mertic_values = get_result_row(rr_list, hit_list, ap_list, bug_report_ids)
    return mertic_values


def get_number_of_java_files(bug_report_ids):
    filename = "Statistics/files_count.csv"
    col_name = "# Java Files in Project"
    ranklist_df = pd.read_csv(filename)

    java_file_count_list = []
    for bug_id in bug_report_ids:
        java_file_count = ranklist_df.loc[ranklist_df['Bug Report ID'] == int(bug_id), col_name].values.tolist()
        if java_file_count is None or len(java_file_count) < 1 or java_file_count != java_file_count:
            java_file_count_list.append(0)
        else:
            java_file_count_list.append(java_file_count[0])

    return java_file_count_list


def write_to_ranks_file_baseline(filename, bug_report_ids, ranklist):
    for i in range(len(bug_report_ids)):
        write_to_csv(filename, [bug_report_ids[i], ranklist[i]])


def write_to_ranks_file_baseline_filtering_boosting(filename, bug_report_ids, ranklist_br, rank_col_rq,
                                                    rank_col_qe1, rank_col_qe2, rank_col_qe3):
    for i in range(len(bug_report_ids)):
        write_to_csv(filename, [bug_report_ids[i], ranklist_br[i],
                                rank_col_rq[i], rank_col_qe1[i], rank_col_qe2[i], rank_col_qe3[i]])


def get_filename(result_dir, screens, operations, filtering_gui, boosting_gui, query_reformulation_gui):
    final_ranks_file = ""
    if operations == "Filtering":
        final_ranks_file = result_dir + "/" + operations + "#Screen-" + screens + "#Filtering-" + filtering_gui + "#Query_Reformulation-" + query_reformulation_gui + ".csv"
    elif operations == "Boosting":
        final_ranks_file = result_dir + "/" + operations + "#Screen-" + screens + "#Boosting-" + boosting_gui + "#Query_Reformulation-" + query_reformulation_gui + ".csv"
    elif operations == "Filtering+Boosting":
        final_ranks_file = result_dir + "/" + operations + "#Screen-" + screens + "#Filtering-" + filtering_gui + "#Boosting-" + boosting_gui + "#Query_Reformulation-" + query_reformulation_gui + ".csv"
    elif operations == "QueryReformulation":
        final_ranks_file = result_dir + "/" + operations + "#Screen-" + screens + "#Query_Reformulation-" + query_reformulation_gui + ".csv"
    return final_ranks_file


# This function will delete rank such as 0 from the list. This case happens for Lucene.
# When a buggy file exist in the corpus but Lucene cannot identify it this case happens.
def remove_invalid_ranks(ranklist):
    filtered_ranklist = []
    for ranks in ranklist:
        filtered_ranklist.append(list(filter(lambda item: item != 0, ranks)))
    return filtered_ranklist


def write_header_for_approach_rankings(filename):
    with open(filename, 'w') as file:
        writer = csv.writer(file)
        writer.writerow(["Task", "GUI Info for Filtering", "GUI Info for Boosting", "GUI Info for Query Expansion",
                         "GUI Info for Query Replacement", "Number of Screens",
                         "Avg Best Ranks", "Number of Identified Bugs", "Number of identified buggy files",
                         "Hit@1", "Hit@2", "Hit@3", "Hit@4", "Hit@5", "Hit@10", "Number of hits@10", "Avg Hit@10",
                         "Hit@15", "Hit@20",
                         "Avg Hit@20", "MRR", "MAP", "HIT@10 List"])


def compute_ranklist_for_bug_report_ids(result_dir, operation, screen, filtering_gui, boosting_gui,
                                        query_reformulation_gui,
                                        bug_report_ids, ref_type, prep_query_dataset):
    row_values = []
    K_values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 50]
    filename = get_filename(result_dir, screen, operation, filtering_gui, boosting_gui, query_reformulation_gui)
    print(filename)
    identified_bugs_count, rankList_query = get_ranklist(bug_report_ids, filename, query_reformulation_gui, ref_type,
                                                         screen, prep_query_dataset)
    rankList_query = remove_invalid_ranks(rankList_query)

    return identified_bugs_count, rankList_query


def get_result_for_each_row(operation, filtering_gui, boosting_gui, query_reformulation_gui, screen,
                            reformulation_rank_type, reformulation_type, result_dir):
    K_values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 50]
    bug_report_ids = get_bug_report_ids()
    all_bug_report_ids = get_bug_report_ids()
    print(f"Total number of bug reports: {len(all_bug_report_ids)}")

    row = []
    row.append(operation)

    if filtering_gui == "":
        row.append("NO")
    else:
        row.append(filtering_gui)

    if boosting_gui == "":
        row.append("NO")
    else:
        row.append(boosting_gui)

    if reformulation_type == "QE":
        row.append(query_reformulation_gui)
        row.append("None")
    elif reformulation_type == "QR":
        row.append("None")
        row.append(query_reformulation_gui)
    elif reformulation_type == "BR":
        row.append("None")
        row.append("None")

    if screen == "":
        row.append("NA")
    else:
        row.append(screen)

    identified_bugs_count_1st, rankList_query_1st = compute_ranklist_for_bug_report_ids(result_dir, operation, screen,
                                                                                        filtering_gui,
                                                                                        boosting_gui,
                                                                                        query_reformulation_gui,
                                                                                        bug_report_ids,
                                                                                        reformulation_rank_type,
                                                                                        exp_name + "/Query")

    rankList_query = rankList_query_1st

    rankList_query_avg = get_best_ranklist_avg(rankList_query)
    total_identified_bugs = identified_bugs_count_1st
    total_ranked_files = total_number_of_ranked_files(rankList_query)
    metric_values = metrics_result(rankList_query, all_bug_report_ids, K_values)

    row.append(rankList_query_avg)
    row.append(total_identified_bugs)
    row.append(total_ranked_files)
    row.extend(metric_values)
    return row


def main(exp_name, screen_list):
    approach_name = "LUCENE"
    result_dir = os.path.join('../results', 'LUCENE-' + exp_name)

    filtering_list = ["GUI_States", "All_GUI_Component_IDs", "GUI_State_and_All_GUI_Component_IDs"]
    boosting_list = ["GUI_States", "All_GUI_Component_IDs", "GUI_State_and_All_GUI_Component_IDs"]
    query_reformulation_list = ["GUI_States", "All_GUI_Component_IDs", "GUI_State_and_All_GUI_Component_IDs"]
    reformulation_types = ["Ranks (Query-Bug Report)", "Ranks (Query Replacement)", "Ranks (Query Expansion 1)"]
    reformulation_cols = ["BR", "QR", "QE"]

    metric_file = "../../FinalResults/LUCENE/" + approach_name + "-" + exp_name + ".csv"
    write_header_for_approach_rankings(metric_file)

    # Filtering
    for filtering_gui in filtering_list:
        for screen in screen_list:
            row = get_result_for_each_row("Filtering", filtering_gui, "", "GUI_States", screen,
                                          "Ranks (Query-Bug Report)",
                                          "BR", result_dir)
            write_to_csv(metric_file, row)
    for query_reformulation_gui in query_reformulation_list:
        for filtering_gui in filtering_list:
            for screen in screen_list:
                row = get_result_for_each_row("Filtering", filtering_gui, "", query_reformulation_gui, screen,
                                              "Ranks (Query Expansion 1)",
                                              "QE", result_dir)
                write_to_csv(metric_file, row)
    for query_reformulation_gui in query_reformulation_list:
        for filtering_gui in filtering_list:
            for screen in screen_list:
                row = get_result_for_each_row("Filtering", filtering_gui, "", query_reformulation_gui, screen,
                                              "Ranks (Query Replacement)",
                                              "QR", result_dir)
                write_to_csv(metric_file, row)

    # Boosting
    for boosting_gui in boosting_list:
        for screen in screen_list:
            row = get_result_for_each_row("Boosting", "", boosting_gui, "GUI_States", screen,
                                          "Ranks (Query-Bug Report)",
                                          "BR", result_dir)
            write_to_csv(metric_file, row)
    for query_reformulation_gui in query_reformulation_list:
        for boosting_gui in boosting_list:
            for screen in screen_list:
                row = get_result_for_each_row("Boosting", "", boosting_gui, query_reformulation_gui, screen,
                                              "Ranks (Query Expansion 1)",
                                              "QE", result_dir)
                write_to_csv(metric_file, row)
    for query_reformulation_gui in query_reformulation_list:
        for boosting_gui in boosting_list:
            for screen in screen_list:
                row = get_result_for_each_row("Boosting", "", boosting_gui, query_reformulation_gui, screen,
                                              "Ranks (Query Replacement)",
                                              "QR", result_dir)
                write_to_csv(metric_file, row)

    # For Filtering+Boosting
    # Changed the range (it was  2)
    for i in range(0, len(filtering_list)):
        for l in range(len(screen_list)):
            boosting_gui_type = []
            index = 0
            for j in range(len(boosting_list)):
                if index < i:
                    boosting_gui_type.append(boosting_list[j])
                    index += 1
                else:
                    break
            for j in range(len(boosting_gui_type)):
                row = get_result_for_each_row("Filtering+Boosting", filtering_list[i], boosting_gui_type[j],
                                              "GUI_States", screen_list[l], "Ranks (Query-Bug Report)",
                                              "BR", result_dir)
                write_to_csv(metric_file, row)

    for k in range(len(query_reformulation_list)):
        # Changed the range (it was  2)
        for i in range(0, len(filtering_list)):
            for l in range(len(screen_list)):
                boosting_gui_type = []
                index = 0
                for j in range(len(boosting_list)):
                    if index < i:
                        boosting_gui_type.append(boosting_list[j])
                        index += 1
                    else:
                        break
                for j in range(len(boosting_gui_type)):
                    row = get_result_for_each_row("Filtering+Boosting", filtering_list[i], boosting_gui_type[j],
                                                  query_reformulation_list[k],
                                                  screen_list[l], "Ranks (Query Expansion 1)", "QE", result_dir)
                    write_to_csv(metric_file, row)

    for k in range(len(query_reformulation_list)):
        # Changed the range (it was  2)
        for i in range(0, len(filtering_list)):
            for l in range(len(screen_list)):
                boosting_gui_type = []
                index = 0
                for j in range(len(boosting_list)):
                    if index < i:
                        boosting_gui_type.append(boosting_list[j])
                        index += 1
                    else:
                        break
                for j in range(len(boosting_gui_type)):
                    row = get_result_for_each_row("Filtering+Boosting", filtering_list[i], boosting_gui_type[j],
                                                  query_reformulation_list[k],
                                                  screen_list[l], "Ranks (Query Replacement)", "QR", result_dir)
                    write_to_csv(metric_file, row)

    # For query_reformulation
    for l in range(len(screen_list)):
        row = get_result_for_each_row("QueryReformulation", "", "", "GUI_States", screen_list[l],
                                      "Ranks (Query-Bug Report)",
                                      "BR", result_dir)
        write_to_csv(metric_file, row)
    for k in range(len(query_reformulation_list)):
        for l in range(len(screen_list)):
            row = get_result_for_each_row("QueryReformulation", "", "", query_reformulation_list[k], screen_list[l],
                                          "Ranks (Query Expansion 1)",
                                          "QE", result_dir)
            write_to_csv(metric_file, row)
    for k in range(len(query_reformulation_list)):
        for l in range(len(screen_list)):
            row = get_result_for_each_row("QueryReformulation", "", "", query_reformulation_list[k], screen_list[l],
                                          "Ranks (Query Replacement)",
                                          "QR", result_dir)
            write_to_csv(metric_file, row)


if __name__ == "__main__":
    """
    List of experiments:
    1. Concat-OB-3-Screens: Conduct Bug Localization (Study 2) using top 3 screens from Buggy UI Localization (Study 1) with concat OB
    2. Concat-OB-4-Screens: Conduct Bug Localization (Study 2) using top 4 screens from Buggy UI Localization (Study 1) with concat OB
    3. First-OB-3-Screens: Conduct Bug Localization (Study 2) using top 3 screens from Buggy UI Localization (Study 1) with first OB
    4. First-OB-4-Screens: Conduct Bug Localization (Study 2) using top 4 screens from Buggy UI Localization (Study 1) with first OB
    """
    exp_names = ["Concat-OB-3-Screens", "Concat-OB-4-Screens", "First-OB-3-Screens", "First-OB-4-Screens"]
    for exp_name in exp_names:
        if exp_name == "Concat-OB-3-Screens" or exp_name == "First-OB-3-Screens":
            screen_list = ["3"]
        elif exp_name == "Concat-OB-4-Screens" or exp_name == "First-OB-4-Screens":
            screen_list = ["4"]
        main(exp_name, screen_list)
