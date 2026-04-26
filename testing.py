import os
import requests
import csv
import time


URL = "http://127.0.0.1:5000/analyse"

# the folder where you ran the download scripts
DATASET_ROOT = r"C:\Users\djemi\Downloads\project\testdataset"

# These map exactly to the folders the FaceForensics script created
CATEGORIES = {
    r"original_sequences\youtube\c23\videos": {"label": "Real", "method": "Original"},
    r"manipulated_sequences\Deepfakes\c23\videos": {"label": "Fake", "method": "Deepfakes"},
    r"manipulated_sequences\Face2Face\c23\videos": {"label": "Fake", "method": "Face2Face"},
    r"manipulated_sequences\FaceSwap\c23\videos": {"label": "Fake", "method": "FaceSwap"},
    r"manipulated_sequences\NeuralTextures\c23\videos": {"label": "Fake", "method": "NeuralTextures"}
}

def test_videos():
    print("Starting testing")

    # make csv file to store results
    with open('testing_results.csv', mode='w', newline='') as file:
        writer = csv.writer(file)
        # Header row for your spreadsheet
        writer.writerow(["Filename", "Generation Method", "Real or fake", "Confidence Score (%)", "Error/Status"])

        for relative_path, meta in CATEGORIES.items():
            folder_path = os.path.join(DATASET_ROOT, relative_path)
            method = meta["method"]
            ground_truth = meta["label"]

            print(f"\nEvaluating Category: {method} ({ground_truth})")

            # Check if the folder exists before
            if not os.path.exists(folder_path):
                print(f"Folder not found! {method}")
                continue

            # loop through 50 videos in the folder
            for filename in os.listdir(folder_path):
                if not filename.endswith(".mp4"):
                    continue

                file_path = os.path.join(folder_path, filename)
                print(f"  Testing {filename}", end=" ")

                try:
                    # Act like the web browser uploading the file
                    with open(file_path, 'rb') as f:
                        # grab first filr regaldless of name
                        files = {'video': (filename, f, 'video/mp4')}
                        response = requests.post(URL, files=files, timeout=120)

                    if response.status_code == 200:
                        result = response.json()
                        confidence = result.get('confidence', 0.0)
                        print(f"Score: {confidence}%")
                        writer.writerow([filename, method, ground_truth, confidence, "Success"])
                    else:
                        print(f"FAILED (HTTP {response.status_code})")
                        writer.writerow([filename, method, ground_truth, "N/A", f"HTTP {response.status_code}"])

                except Exception as e:
                    print("Service Offline or Timed Out.")
                    writer.writerow([filename, method, ground_truth, "N/A", "Timeout/Error"])

                # dont overheat cpu
                time.sleep(1)

    print("testing complete")

if __name__ == "__main__":
    test_videos()