import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns


CSV_FILE = 'testing_results.csv'

THRESHOLD = 55.0

def analyse_data():
    print("analysig data ".format(THRESHOLD))

    # load the data
    df = pd.read_csv(CSV_FILE)

    # delete any rows that failed  (HTTP 400/500)
    df = df[df['Error/Status'] == 'Success'].copy()

    # conerst score to number
    df['Confidence Score (%)'] = pd.to_numeric(df['Confidence Score (%)'])

    # apply threshold to make a final prediction
    # if score >threshold, model says 'Fake'. Otherwise, model says 'Real'.
    df['Prediction'] = df['Confidence Score (%)'].apply(lambda x: 'Fake' if x >= THRESHOLD else 'Real')

    #calculate Accuracy
    correct_predictions = df[df['Real or fake'] == df['Prediction']]
    overall_accuracy = (len(correct_predictions) / len(df)) * 100
    print("Total Videos Processed Successfully: {}".format(len(df)))
    print("OVERALL ACCURACY: {:.2f}%\n".format(overall_accuracy))

    # calculate Accuracy for eCH generation method
    print("ACCURACY PER CATEGORY")
    category_metrics = []

    for method in df['Generation Method'].unique():
        method_df = df[df['Generation Method'] == method]
        correct_method = method_df[method_df['Real or fake'] == method_df['Prediction']]
        acc = (len(correct_method) / len(method_df)) * 100
        print("{}: {:.2f}%".format(method, acc))
        category_metrics.append({'Method': method, 'Accuracy': acc})

    cat_df = pd.DataFrame(category_metrics)

    # make the first graph: Category Accuracy Bar Chart
    plt.figure(figsize=(10, 6))
    sns.barplot(x='Method', y='Accuracy', data=cat_df, palette='viridis')
    plt.title('Detection Accuracy by Generation Method (Threshold = {}%)'.format(THRESHOLD))
    plt.ylabel('Accuracy (%)')
    plt.xlabel('Generation Method')
    plt.ylim(0, 100)
    plt.axhline(overall_accuracy, color='red', linestyle='--', label='Overall Avg ({:.1f}%)'.format(overall_accuracy))
    plt.legend()
    plt.tight_layout()
    plt.savefig('Category_Accuracy.png', dpi=300)


    # make graph 2= Score Distribution Boxplot
    plt.figure(figsize=(10, 6))
    sns.boxplot(x='Generation Method', y='Confidence Score (%)', data=df, palette='Set2')
    plt.title('Distribution of Confidence Scores by Category')
    plt.axhline(THRESHOLD, color='red', linestyle='--', label='Decision Threshold ({}%)'.format(THRESHOLD))
    plt.legend()
    plt.tight_layout()
    plt.savefig('Distribution.png', dpi=300)


if __name__ == "__main__":
    analyse_data()