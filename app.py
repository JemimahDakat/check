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
ort_session = ort.InferenceSession("deepfake_model (350).onnx")

# 'face_detector' holds the tool that draws a box around a face
face_detector = MTCNN(image_size = 224,keep_all=False, device='cpu')


def preprocess_face(face_tensor):
    # The ONNX needs the picture formatted in a very strict mathematical way.
    # This converts the picture into a grid of numbers that the model understands
    face_np = face_tensor.numpy()

    # 'expand_dims' adds a wrapper around the numbers so the model knows it's 1 image
    face_np = np.expand_dims(face_np, axis=0)

    return face_np.astype(np.float32)


#api endpoint
# only executes when the java interacys with http://localhost:5000/analyse and drops off a post package


@app.route('/analyse', methods=['POST'])
def analyse_video():

    # Receive the vid
    # check if the package  contains a file named video.
    if 'video' not in request.files:
        # If no video, send back an error message and a 400 (Bad Request) code.
        return jsonify({"error": "No video file provided"}), 400

    #GET the video file from the package
    video_file = request.files['video']

    # clean the file name (e.g., changes "my first video.mp4" to "my_first_video.mp4").
    filename = secure_filename(video_file.filename)

    #  save the video to the folder so Python can look at it
    temp_path = "temp_filename.mp4"
    video_file.save(temp_path)


    # watch the vid

    print(f"--- NEW UPLOAD STARTED ---")
    print(f"Video saved successfully to {temp_path}")

    # 'cv2.VideoCapture' opens the video file
    cap = cv2.VideoCapture(temp_path)

    if not cap.isOpened():
        print("OpenCV failed to open the video!")
        return jsonify({"is_fake": False, "confidence": 0.0})
    # make an empty list to hold the formatted frames
    processed_frames = []
    frames_read = 0
    faces_found = 0
    # look at 15 frames  of the video.
    for _ in range(50):

        # 'cap.read()' grabs a single split-second picture from the vid
        success, frame = cap.read()

        # If the vid ended early and there are no more pictures stop the loop
        if not success:
            break
        frames_read += 1
        faces_found += 1
        # Videos are naturally Blue-Green-Red. The AI expects Red-Green-Blue. We swap the colors here.
        img_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

        # turn to a picture frames that our tools understand
        pil_img = Image.fromarray(img_rgb)

        # check face detector if there is a face in the pic frame
        face_tensor = face_detector(pil_img)

        # 'if face_tensor is not None' if model detects a face
        if face_tensor is not None:



            # format the face picture using our helper recipe from Step 3.
            input_data = preprocess_face(face_tensor)

            processed_frames.append(input_data)

    # closes the vid file
    cap.release()
    # delete the video from the computer so no storage is used
    os.remove(temp_path)

    print(f"Frames read by OpenCV: {frames_read}/30")
    print(f"Faces found by MTCNN: {faces_found}/30")

    # If the bucket is empty / no faces were detected
    if len(processed_frames) == 0:
        print("no faces found to analyse. Returning 0%.")
        #returns to java
        return jsonify({
            "is_fake": False,
            "confidence": 0.0
        })


    sequence_tensor = np.vstack(processed_frames)
    sequence_tensor = np.expand_dims(sequence_tensor, axis= 0)

    input_name = ort_session.get_inputs()[0].name

    # Run the ONNX model ONCE on the entire sequence
    raw_prediction = ort_session.run(None, {input_name: sequence_tensor})[0]

    # convert logit to probability decimal (0.0 to 1.0)
    probability = 1 / (1 + np.exp(-raw_prediction[0][0]))

    # 2. FIXED: The Percentage Math
    confidence_percentage = round(float(probability) * 100, 2)

    # The 15% threshold
    is_fake = bool(confidence_percentage >65.0)
    # send the final answer into JSON and send it back to Java
    return jsonify({
        "is_fake": is_fake,
        "confidence": confidence_percentage
    })


# run the program
if __name__ == '__main__':
    # grab the port from the system environment, or default to 5000
    port = int(os.environ.get('PORT', 5000))

    print(f"Starting on port {port}.")
    app.run(host='0.0.0.0', port=port)