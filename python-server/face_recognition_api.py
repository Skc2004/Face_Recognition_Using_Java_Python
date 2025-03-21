from flask import Flask, Response
from deepface import DeepFace
import cv2

app = Flask(__name__)
camera = cv2.VideoCapture(0)  # Start video capture (0 for default camera)

# Path to the reference image
reference_image_path = r"C:\Users\Satyam\Desktop\Code\face_rec\python-server\satyam.jpg"
try:
    reference_embedding = DeepFace.represent(img_path=reference_image_path, model_name="VGG-Face")
    print("Reference image loaded and embedding calculated.")
except Exception as e:
    print("Error loading reference image:", e)

def generate_frames():
    while True:
        success, frame = camera.read()
        if not success:
            break

        try:
            detections = DeepFace.analyze(frame, actions=['emotion'], enforce_detection=False, detector_backend="mtcnn")
            if isinstance(detections, list):
                for detection in detections:
                    x, y, w, h = (
                        detection['region']['x'],
                        detection['region']['y'],
                        detection['region']['w'],
                        detection['region']['h']
                    )
                    face_frame = frame[y:y+h, x:x+w]
                    
                    result = DeepFace.verify(face_frame, reference_image_path, model_name="VGG-Face", enforce_detection=False)
                    if result['verified']:
                        label = "Match Found"
                        color = (0, 255, 0)
                    else:
                        label = "No Match"
                        color = (0, 0, 255)

                    cv2.rectangle(frame, (x, y), (x + w, y + h), color, 2)
                    cv2.putText(frame, label, (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

        except Exception as e:
            print("Error during face detection and verification:", e)

        ret, buffer = cv2.imencode('.jpg', frame)
        if not ret:
            print("Failed to encode frame to JPEG")
            continue
        frame = buffer.tobytes()
        yield (b'--frame\r\n'
               b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n')

@app.route('/video_feed')
def video_feed():
    return Response(generate_frames(), mimetype='multipart/x-mixed-replace; boundary=frame')

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000 , debug=True)
