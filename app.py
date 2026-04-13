from flask import Flask, request, jsonify  # Flask is the tool that makes this a web server.
import os  # 'os' helps Python talk to your Windows/Mac files and folders.
from werkzeug.utils import secure_filename # Cleans up file names so hackers can't upload malicious files.
import onnxruntime as ort # This is the tool that knows how to read your .onnx AI brain.
import numpy as np # Numpy is a tool that does heavy math very, very fast.
import cv2 # cv2 (OpenCV) is our video player tool. It lets Python look at videos frame-by-frame.
from facenet_pytorch import MTCNN # This is a specific AI tool that ONLY finds human faces.
from PIL import Image # PIL (Pillow) is an image editing tool.


# creating  actual web server and naming it app
app = Flask(__name__)

# load the rnn and lstm into memory as soon as the file starts
# 'ort_session' holds trained deepfake model
ort_session = ort.InferenceSession("deepfake_model.onnx")

# 'face_detector' holds the tool that draws a box around a face
face_detector = MTCNN(keep_all=False, device='cpu')


def preprocess_face(face_tensor):
    # The ONNX needs the picture formatted in a very strict mathematical way.
    # This recipe converts the picture into a grid of numbers that the AI understands.
    face_np = face_tensor.numpy()

    # 'expand_dims' adds an invisible wrapper around the numbers so the AI knows it's 1 image.
    face_np = np.expand_dims(face_np, axis=0)

    return face_np.astype(np.float32)


# =========================================================================
# 4. THE MAIN DOORWAY (The API Endpoint)
# @app.route is like putting a sign on a door.
# It tells the server: "If the Java app comes to http://localhost:5000/analyse
# and drops off a POST package, run the code below."
# =========================================================================

@app.route('/analyse', methods=['POST'])
def analyse_video():

    # --- Step A: Receive the Video ---

    # Check if the package actually contains a file named 'video'.
    if 'video' not in request.files:
        # If no video, send back an error message and a 400 (Bad Request) code.
        return jsonify({"error": "No video file provided"}), 400

    # Grab the video file from the package.
    video_file = request.files['video']

    # Clean the file name (e.g., changes "my cool video!!!.mp4" to "my_cool_video.mp4").
    filename = secure_filename(video_file.filename)

    # Actually save the video to the folder so Python can look at it.
    temp_path = filename
    video_file.save(temp_path)


    # --- Step B: Watch the Video ---

    # 'cv2.VideoCapture' opens the video file like a DVD player.
    cap = cv2.VideoCapture(temp_path)

    # Create an empty list (like an empty bucket) to hold the AI's guesses.
    fake_scores = []

    # A 'for' loop means "do this action exactly X times". We want to look at 5 frames (pictures) of the video.
    for _ in range(5):

        # 'cap.read()' grabs a single split-second picture from the video.
        success, frame = cap.read()

        # If the video ended early and there are no more pictures, 'break' means "stop the loop".
        if not success:
            break

        # Videos are naturally Blue-Green-Red. The AI expects Red-Green-Blue. We swap the colors here.
        img_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

        # Turn it into a picture format that our tools understand.
        pil_img = Image.fromarray(img_rgb)

        # Ask the face detector tool: "Do you see a face in this picture?"
        face_tensor = face_detector(pil_img)

        # 'if face_tensor is not None' means "If you ACTUALLY found a face..."
        if face_tensor is not None:

            # --- Step C: Ask the AI Brain ---

            # Format the face picture using our helper recipe from Step 3.
            input_data = preprocess_face(face_tensor)

            # Get the name of the input slot the AI expects.
            input_name = ort_session.get_inputs()[0].name

            # RUN THE AI! We feed the picture into the ONNX brain, and it spits out raw math.
            raw_prediction = ort_session.run(None, {input_name: input_data})[0]

            # The raw math is confusing. 'np.exp' (Sigmoid) is a math trick that squashes the number
            # into a clean percentage between 0.0 (0% fake) and 1.0 (100% fake).
            probability = 1 / (1 + np.exp(-raw_prediction[0][0]))

            # Add this percentage to our bucket of scores.
            fake_scores.append(probability)

    # We are done watching the video, so turn off the DVD player.
    cap.release()

    # Delete the video from the computer so we don't run out of storage space!
    os.remove(temp_path)


    # --- Step D: Make the Final Decision ---

    # If the bucket is completely empty (meaning no faces were found in any of the pictures)...
    if len(fake_scores) == 0:
        # Tell Java: "It's not fake, but I am 0% confident because I saw no faces."
        return jsonify({"is_fake": False, "confidence": 0.0})

    # Calculate the average score (e.g., if the scores were 80%, 90%, 85%, the average is 85%).
    avg_score = float(np.mean(fake_scores))

    # 'is_fake' will be True if the average is higher than 0.50 (50%). Otherwise, it's False.
    is_fake = avg_score > 0.50

    # Package the final answer into JSON (a standard internet text format) and send it back to Java!
    return jsonify({
        "is_fake": is_fake,
        "confidence": avg_score
    })


# =========================================================================
# 5. TURNING IT ON
# This says: "If I run this file directly in the terminal, start the server."
# =========================================================================
if __name__ == '__main__':
    # Grab the port from the system environment, or default to 5000
    port = int(os.environ.get('PORT', 5000))

    print(f"Starting AI Sidekick on port {port}...")
    app.run(host='0.0.0.0', port=port)