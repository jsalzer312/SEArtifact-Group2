import pandas as pd
import os
import glob
from openpyxl.styles import Alignment
from openpyxl.styles import Color, PatternFill, Font, Border


def get_last_string(string):
    return string.split("_")[-1]


def as_text(value):
    if value is None:
        return ""
    return str(value)


def get_result_summary(results_folder_path):
    data_frame = pd.DataFrame(
        columns=["Experiment Name", "# of Ret. Tasks", "MRR", "MAP", "h@1", "h@2", "h@3", "h@4", "h@5", "h@6", "h@7",
                 "h@8", "h@9", "h@10", "Avg. Hits", "# of Failures"])

    files = glob.glob(os.path.join(results_folder_path, "*.csv"))
    files.sort(key=get_last_string)

    for file in files:
        # print(file)
        if not file.endswith(".csv"):
            continue

        print("processing file " + file)
        df = pd.read_csv(file, delimiter=";")
        experiment_name = file.split("/")[-1].replace(".csv", "")
        # print(experiment_name)
        if experiment_name.split("_")[-1] == "details":
            continue
        if experiment_name.split("_")[-1] == "Cat":
            continue
        if experiment_name.startswith("all_"):
            continue
        experiment_name = experiment_name.replace("_all_obs", "")
        total_queries = df["# of Queries"].sum()
        avg_mrr = df["MRR"].mean()
        avg_map = df["MAP"].mean()
        avg_h_1 = df["h@1"].mean()
        avg_h_2 = df["h@2"].mean()
        avg_h_3 = df["h@3"].mean()
        avg_h_4 = df["h@4"].mean()
        avg_h_5 = df["h@5"].mean()
        avg_h_6 = df["h@6"].mean()
        avg_h_7 = df["h@7"].mean()
        avg_h_8 = df["h@8"].mean()
        avg_h_9 = df["h@9"].mean()
        avg_h_10 = df["h@10"].mean()
        avg_hits = (
                           avg_h_1 + avg_h_2 + avg_h_3 + avg_h_4 + avg_h_5 + avg_h_6 + avg_h_7 + avg_h_8 + avg_h_9 + avg_h_10) / 10

        if "# of Failed Cases" in df:
            n_failed_cases = df["# of Failed Cases"].sum()
        else:
            n_failed_cases = 0

        data_frame = data_frame._append(
            {"Experiment Name": experiment_name, "# of Ret. Tasks": total_queries, "MRR": avg_mrr,
             "MAP": avg_map, "h@1": avg_h_1, "h@2": avg_h_2,
             "h@3": avg_h_3, "h@4": avg_h_4, "h@5": avg_h_5, "h@6": avg_h_6,
             "h@7": avg_h_7, "h@8": avg_h_8,
             "h@9": avg_h_9, "h@10": avg_h_10, "Avg. Hits": avg_hits,
             "# of Failures": n_failed_cases},
            ignore_index=True)

    return data_frame


def modify_df_and_save_to_excel(df, writer, sheet_name, wrap_text_columns, reduce_size_columns):
    """

    :param df:
    :param writer:
    :param sheet_name:
    :param wrap_text_columns:
    :param reduce_size_columns:
    :return:
    """
    max = df["MRR"].max()
    df.to_excel(writer, sheet_name=sheet_name, index=False)
    worksheet = writer.sheets[sheet_name]

    # Wrap text in the columns
    for col in wrap_text_columns:
        for cell in writer.sheets[sheet_name][col]:
            cell.alignment = Alignment(wrap_text=True)

    # Set the width of the column
    for col in reduce_size_columns:
        worksheet.column_dimensions[col].width = 5.3

    worksheet.column_dimensions['A'].width = 15

    worksheet.freeze_panes = 'A2'

    yellow_fill = PatternFill(start_color='00FFFF00',
                              end_color='00FFFF00',
                              fill_type='solid')

    for cell in writer.sheets[sheet_name]['C']:
        if cell.value == max:
            cell.fill = yellow_fill

    writer._save()


def highlight_greaterthan(x):
    if x.C > .5:
        return ['background-color: yellow'] * 5
    else:
        return ['background-color: white'] * 5


def highlight_max_MRR(s):
    is_max = s['MRR'] == s['MRR'].max()
    return ['background-color: yellow' if v else '' for v in is_max]


def format_float(val):
    if isinstance(val, float):
        return '{:.3f}'.format(val)
    else:
        return val


def highlight_max(df, col):
    df_styled = df.style.apply(lambda x: ["background-color: yellow" if v == x[col].max() else "" for v in x], axis=1)
    return df_styled


def highlight_rows(row):
    value = row.loc['# of Ret. Tasks']
    if value == 89:
        color = '#FFB3BA'  # Red
    elif value == 570953:
        color = '#BAFFC9'  # Green
    else:
        color = '#BAE1FF'  # Blue
    return ['background-color: {}'.format(color) for r in row]


if __name__ == "__main__":
    print("Creating results summary")
    results_folders = ['./study_1/results/CL', './study_1/results/SL']
    output_file_path = './study_1/results/result_summary.xlsx'

    if os.path.exists(output_file_path):
        os.remove(output_file_path)

    for results_folder in results_folders:
        module_name = results_folder.split("/")[-1]
        print(module_name)

        if not os.path.isdir(results_folder):
            continue
        # used_dataset = results_folder.split("/")[-1]
        # # print(results_sub_folder)
        # # print(used_dataset)
        # if used_dataset.endswith('.py'):
        #     continue
        # if used_dataset == "Icon":
        #     continue
        # if not used_dataset.startswith("Real"):
        #     continue

        sheet_name = module_name
        # print(sheet_name)

        df = get_result_summary(results_folder)

        df.sort_values(by=['MRR'], inplace=True, ascending=False)
        # df.style.apply(highlight_greaterthan, axis=1)
        # df.style.apply(highlight_rows, axis=1)
        # df.style.apply(highlight_rows, axis=1) \
        #    .to_excel('styled_df.xlsx', engine='openpyxl')

        reduce_size_columns = ['C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', "M", "N", "O"]
        wrap_text_columns = ['A', 'B', 'O', 'P']

        if os.path.exists(output_file_path):
            with pd.ExcelWriter(output_file_path, mode="a", engine="openpyxl") as writer:
                modify_df_and_save_to_excel(df, writer, sheet_name, wrap_text_columns, reduce_size_columns)

        else:
            with pd.ExcelWriter(output_file_path, mode="w", engine="openpyxl") as writer:
                modify_df_and_save_to_excel(df, writer, sheet_name, wrap_text_columns, reduce_size_columns)

        result_file_path = glob.glob(os.path.join(results_folder, "*"))
        for file_path in result_file_path:
            if file_path.split("/")[-1] == "Cat.csv":
                sheet_name = sheet_name + "-Cat"
                df2 = pd.read_csv(file_path, delimiter=";")
                with pd.ExcelWriter(output_file_path, mode="a", engine="openpyxl") as writer:
                    # if module_name == "CR":
                    #     df2.sort_values(by=['OB-Rating', 'OB-in-Title?', 'MRR'],
                    #                     ascending=[True, True, False], inplace=True)
                    # elif module_name == "SR":
                    df2.sort_values(by=['OB-Category', 'OB-Rating', 'OB-in-Title?', 'MRR'],
                                    ascending=[True, True, True, False], inplace=True)

                    df2.to_excel(writer, sheet_name=sheet_name, index=False, na_rep="NaN")

                    # if module_name == "CR":
                    #     reduce_size_columns = ['E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', "M", "N", "O", "P", "Q"]
                    #     wrap_text_columns = ['A', 'B', 'C', 'D', 'E', 'Q', 'R']
                    #     worksheet = writer.sheets[sheet_name]
                    #
                    #     # Set the width of the column
                    #     for col in reduce_size_columns:
                    #         worksheet.column_dimensions[col].width = 5.3
                    #
                    #     for col in wrap_text_columns:
                    #         for cell in writer.sheets[sheet_name][col]:
                    #             cell.alignment = Alignment(wrap_text=True)
                    #
                    #     worksheet.column_dimensions['C'].width = 25
                    #     worksheet.freeze_panes = 'A2'
                    #     writer.save()
                    #
                    # elif module_name == "SR":
                    reduce_size_columns = ['F', 'G', 'H', 'I', 'J', 'K', 'L', "M", "N", "O", "P", "Q", "R"]
                    wrap_text_columns = ['A', 'B', 'C', 'D', 'E', 'F', 'R', 'S']
                    worksheet = writer.sheets[sheet_name]

                    # Set the width of the column
                    for col in reduce_size_columns:
                        worksheet.column_dimensions[col].width = 5.3

                    for col in wrap_text_columns:
                        for cell in writer.sheets[sheet_name][col]:
                            cell.alignment = Alignment(wrap_text=True)

                    worksheet.column_dimensions['D'].width = 15
                    worksheet.freeze_panes = 'A2'
                    writer._save()
