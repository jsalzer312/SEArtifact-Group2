import os
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

import os

pwd = os.getcwd()

file_path = pwd + '/study_1/results/graphs/summary_bar_graph.png'

# Check if the file exists
if os.path.exists(file_path):
    os.remove(file_path)
    print(f"File '{file_path}' has been deleted.")
else:
    print(f"File '{file_path}' does not exist.")


# Read the Excel file
file_path = pwd + '/study_1//results/result_summary.xlsx'
data = pd.read_excel(file_path)

# Columns to keep for the X-axis (h@1 to h@10)
x_axis_columns = ['h@1', 'h@2', 'h@3', 'h@4', 'h@5', 'h@6', 'h@7', 'h@8', 'h@9', 'h@10']

# Filter data for only relevant columns
filtered_data = data[['Experiment Name'] + x_axis_columns]

# Set the experiment names as index for plotting
filtered_data.set_index('Experiment Name', inplace=True)

# Plotting
x = np.arange(len(x_axis_columns))  # X-axis positions for h@1 to h@10
width = 0.2  # Bar width

plt.figure(figsize=(14, 8))
for i, (experiment_name, row) in enumerate(filtered_data.iterrows()):
    offset = (i - len(filtered_data) / 2) * width  # Offset for each experiment
    plt.bar(x + offset, row, width, label=experiment_name)

# Add title and labels
plt.title('Performance Metrics Across h@1 to h@10', fontsize=16)
plt.xlabel('Metrics (h@1 to h@10)', fontsize=14)
plt.ylabel('Values', fontsize=14)
plt.xticks(x, x_axis_columns, fontsize=12)

# Add legend and grid
plt.legend(title='Experiment Names', loc='best', fontsize=10)
plt.grid(axis='y')

# Ensure the output directory exists
output_dir = pwd + '/study_1/results/graphs'  # Replace with your desired directory
os.makedirs(output_dir, exist_ok=True)

# Save plot as an image and display it
output_path = os.path.join(output_dir, 'summary_bar_graph.png')  # Set the filename
plt.tight_layout()
plt.savefig(output_path)  # Save as PNG file
plt.show()

print(f"Bar graph saved as '{output_path}'.")
