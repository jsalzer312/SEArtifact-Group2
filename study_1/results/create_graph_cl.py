import pandas as pd
import matplotlib.pyplot as plt
import os
import shutil

pwd = os.getcwd()
directory_path = pwd + '/study_1/results/graphs/cl'

if os.path.exists(directory_path):
    shutil.rmtree(directory_path)
    print(f"Directory '{directory_path}' and its contents have been deleted.")
else:
    print(f"Directory '{directory_path}' does not exist.")

# File paths for data
file_paths = {
    "Cat": pwd + "/study_1/results/CL/Cat.csv",
    "LUCENE": pwd + "/study_1/results/CL/LUCENE_results_details.csv"
}

custom_line_names = {
    "SBERT_results_with_details.csv": "SBERT",
    "BLIP_results_with_details.csv": "BLIP",
    "CLIP_results_with_details.csv": "CLIP",
    "LUCENE_results_with_details.csv": "LUCENE"
}

# Function to load CSVs
def load_csv(file_path, delimiter=";"):
    try:
        data = pd.read_csv(file_path, on_bad_lines='skip', delimiter=delimiter)
        print(f"Successfully loaded data from {file_path}")
        print(f"Data Preview:\n{data.head()}")
        print(f"Columns in the dataset: {data.columns}")
        return data
    except Exception as e:
        print(f"Error reading {file_path}: {e}")
        return None

# Transform LUCENE dataset to match Cat.csv structure
def transform_lucene_data(data):
    if data is not None:
        data['Experiment Name'] = "LUCENE_results_with_details.csv"
        data['MAP'] = data['Average-Precision']
        data['MRR'] = data['Reciprocal-Rank']
        data = data.rename(columns={"OB-Rating": "OB-Rating", "OB-Category": "OB-Category"})
        return data[['OB-Category', 'OB-Rating', 'Experiment Name', 'MAP', 'MRR']]
    return pd.DataFrame()

# Process data for line graph
def process_data_for_line_graph(data, experiment_col='Experiment Name', x_col='OB-Rating', y_col='MAP'):
    if data is not None and {experiment_col, x_col, y_col, 'OB-Category'}.issubset(data.columns):
        data = data[(data['OB-Category'] != 'All') & (data[x_col] != 'All')]
        data = data[[experiment_col, x_col, y_col]].dropna()
        data[x_col] = pd.to_numeric(data[x_col], errors='coerce')  # Ensure x_col is numeric
        aggregated_data = data.groupby([experiment_col, x_col]).mean().reset_index()
        aggregated_data = aggregated_data.sort_values(by=x_col)  # Sort x-axis values
        return aggregated_data
    else:
        print(f"Missing columns {experiment_col}, {x_col}, {y_col}, or 'OB-Category' in the data.")
        return pd.DataFrame()

# Process data for bar graph
def process_data_for_bar_graph(data, experiment_col='Experiment Name', category_col='OB-Category', y_col='MRR'):
    if data is not None and {experiment_col, category_col, y_col}.issubset(data.columns):
        data = data[(data[category_col] != 'All') & (~data[y_col].isnull())]
        aggregated_data = data.groupby([experiment_col, category_col])[y_col].mean().reset_index()
        return aggregated_data
    else:
        print(f"Missing columns {experiment_col}, {category_col}, or {y_col} in the data.")
        return pd.DataFrame()

# Load and process data
cat_data = load_csv(file_paths["Cat"])
lucene_data = load_csv(file_paths["LUCENE"], delimiter=";")

# Transform LUCENE data
lucene_data_transformed = transform_lucene_data(lucene_data)

# Add LUCENE to the Cat dataset
cat_data = pd.concat([cat_data, lucene_data_transformed], ignore_index=True)

# Process data for graphs
line_graph_data = process_data_for_line_graph(cat_data)
bar_graph_data = process_data_for_bar_graph(cat_data)

# Ensure output directory exists
output_dir = pwd + "/study_1/results/graphs/cl"
os.makedirs(output_dir, exist_ok=True)

# Plot line graph
if not line_graph_data.empty:
    plt.figure(figsize=(10, 6))
    experiments = line_graph_data['Experiment Name'].unique()
    for experiment in experiments:
        experiment_data = line_graph_data[line_graph_data['Experiment Name'] == experiment]
        plt.plot(
            experiment_data['OB-Rating'], 
            experiment_data['MAP'], 
            label=custom_line_names.get(experiment, experiment)
        )
    plt.xlabel('OB-Rating')
    plt.ylabel('Average MAP')
    plt.title('MAP by OB-Rating For CL')
    plt.xticks(ticks=[1, 2, 3, 4, 5])  # Set ticks explicitly to start at 1
    plt.ylim(bottom=0)  # Start y-axis from 0
    plt.legend()
    plt.grid(True)
    line_graph_path = os.path.join(output_dir, "line_graph.png")
    plt.savefig(line_graph_path)  # Save as PNG file
    print(f"Line graph saved to {line_graph_path}")
    plt.show()
else:
    print("No data to plot for the line graph.")

# Plot bar graph
if not bar_graph_data.empty:
    plt.figure(figsize=(10, 6))
    categories = bar_graph_data['OB-Category'].unique()
    x_labels = bar_graph_data['Experiment Name'].unique()
    width = 0.2

    # Define custom names for the x-axis (Approaches/Experiments)
    custom_x_labels = {
        "SBERT_results_with_details.csv": "SBERT",
        "BLIP_results_with_details.csv": "BLIP",
        "CLIP_results_with_details.csv": "CLIP",
        "LUCENE_results_with_details.csv": "LUCENE"
    }

    # Map the experiment names to the custom x-axis labels
    x_labels_mapped = [custom_x_labels.get(label, label) for label in x_labels]

    x = range(len(x_labels))

    for i, category in enumerate(categories):
        category_data = bar_graph_data[bar_graph_data['OB-Category'] == category]
        y_values = [
            category_data[category_data['Experiment Name'] == experiment]['MRR'].values[0]
            if experiment in category_data['Experiment Name'].values else 0
            for experiment in x_labels
        ]
        plt.bar(
            [p + i * width for p in x],
            y_values,
            width=width,
            label=category
        )
    
    # Set custom x-axis labels
    plt.xticks(
        [p + width * len(categories) / 2 for p in x],  # Position of the ticks
        x_labels_mapped,  # Custom labels
        rotation=45  # Rotate for better readability
    )
    plt.xlabel('Approach')
    plt.ylabel('Average MRR')
    plt.title('MRR by Approach and OB-Category For CL')
    plt.legend(title="OB-Category")
    plt.grid(True)
    bar_graph_path = os.path.join(output_dir, "bar_graph.png")
    plt.savefig(bar_graph_path)
    print(f"Bar graph saved to {bar_graph_path}")
    plt.show()
