import base64
import binascii
import hmac
import os
from pathlib import Path
from threading import Lock
from uuid import UUID

import cv2
import numpy as np
from deepface import DeepFace
from deepface.modules import modeling
from flask import Flask, abort, request

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 3 * 1024 * 1024
SECRET = os.environ["MES_FACE_SECRET"]
MODEL_VERSION = os.environ.get("MES_FACE_MODEL", "deepface-sface-opencv-v1")
if len(SECRET) < 32:
    raise ValueError("MES_FACE_SECRET must be at least 32 characters")
inference_lock = Lock()

class CaptureRejected(ValueError):
    pass


class FaceNotDetected(CaptureRejected):
    pass


def load_detector():
    detector = modeling.build_model(task="face_detector", model_name="opencv")
    for name, filename in (("face_detector", "haarcascade_frontalface_default.xml"),
                           ("eye_detector", "haarcascade_eye.xml")):
        if not detector.model[name].empty():
            continue
        xml = (Path(cv2.data.haarcascades) / filename).read_text(encoding="utf-8")
        storage = cv2.FileStorage(xml, cv2.FILE_STORAGE_READ | cv2.FILE_STORAGE_MEMORY)
        try:
            if not detector.model[name].read(storage.getFirstTopLevelNode()):
                raise RuntimeError(f"Could not load OpenCV detector: {filename}")
        finally:
            storage.release()


load_detector()
DeepFace.build_model('SFace')
modeling.build_model(task='spoofing', model_name='Fasnet')


@app.post("/infer")
def infer():
    if not hmac.compare_digest(request.headers.get("Authorization", ""), f"Bearer {SECRET}"):
        abort(401)
    data = request.get_json()
    try:
        UUID(data["challenge"])
        frames = data["frames"]
        if not isinstance(frames, list) or not 3 <= len(frames) <= 8:
            abort(400)
        if any(not isinstance(frame, str) or not 0 < len(frame) <= 350_000 for frame in frames):
            abort(400)
        if len(set(frames)) != len(frames):
            abort(400)
    except (KeyError, TypeError, ValueError, AttributeError):
        abort(400)

    # Allow brief overlap while keeping overload waits within the Java client timeout.
    if not inference_lock.acquire(timeout=1.0):
        abort(503)
    try:
        vectors = []
        for frame in frames:
            try:
                vectors.append(extract(frame))
            except FaceNotDetected:
                continue
        if len(vectors) < max(3, len(frames) - 2):
            raise CaptureRejected(
                "未检测到清晰人脸。"
                "请稍微靠近、面向光源并保持正对镜头，然后点重新采集。"
            )
        mean = np.mean(vectors, axis=0)
        norm = np.linalg.norm(mean)
        if norm == 0 or not np.isfinite(norm):
            raise ValueError("Invalid embedding")
        mean /= norm
        if min(float(vector @ mean) for vector in vectors) < 0.80:
            raise CaptureRejected("请保持面部朝向镜头，避免快速移动，再重新采集")
        return {"live": True, "modelVersion": MODEL_VERSION, "embedding": mean.tolist()}
    except CaptureRejected as error:
        return {"live": False, "modelVersion": MODEL_VERSION, "reason": str(error)}
    except (ValueError, binascii.Error, cv2.error):
        return {"live": False, "modelVersion": MODEL_VERSION}
    finally:
        inference_lock.release()


def select_face(detected, width, height):
    def area(face):
        box = face["facial_area"]
        return box["w"] * box["h"]

    primary = max(detected, key=area)
    if len(detected) == 1:
        return primary

    def in_scan_area(face):
        box = face["facial_area"]
        x = (box["x"] + box["w"] / 2) / width
        y = (box["y"] + box["h"] / 2) / height
        return ((x - 0.5) / 0.24) ** 2 + ((y - 0.5) / 0.40) ** 2 <= 1

    def likely_background(face):
        box = face["facial_area"]
        return (
            not in_scan_area(face)
            and area(face) <= min(area(primary) * 0.16, 64 * 64)
            and (box.get("left_eye") is None or box.get("right_eye") is None)
        )

    def overlaps_primary(face):
        a = primary["facial_area"]
        b = face["facial_area"]
        overlap_width = max(0, min(a["x"] + a["w"], b["x"] + b["w"]) - max(a["x"], b["x"]))
        overlap_height = max(0, min(a["y"] + a["h"], b["y"] + b["h"]) - max(a["y"], b["y"]))
        intersection = overlap_width * overlap_height
        return intersection >= 0.65 * (area(primary) + area(face) - intersection)

    secondary = [face for face in detected if face is not primary and not overlaps_primary(face)]
    if not secondary:
        return primary

    if not in_scan_area(primary) or any(
        not likely_background(face) for face in secondary
    ):
        raise CaptureRejected("检测到多张人脸，请确保只有本人入镜")
    return primary


def extract(frame):
    image = cv2.imdecode(np.frombuffer(base64.b64decode(frame, validate=True), dtype=np.uint8),
                         cv2.IMREAD_COLOR)
    if image is None or image.shape[:2] != (480, 640):
        raise ValueError("Unexpected frame")
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    if float(gray.mean()) < 45:
        raise CaptureRejected("画面过暗，请补光并避免背光")
    try:
        detected = DeepFace.extract_faces(img_path=image, detector_backend="opencv",
                                          enforce_detection=True, anti_spoofing=False)
    except ValueError as error:
        if "face could not be detected" in str(error).lower():
            raise FaceNotDetected("未检测到清晰人脸。请正对镜头并检查遮挡、反光眼镜或饰物") from error
        raise
    face = select_face(detected, image.shape[1], image.shape[0])
    box = face["facial_area"]
    antispoof_model = modeling.build_model(task="spoofing", model_name="Fasnet")
    is_real, _ = antispoof_model.analyze(
        img=image, facial_area=(box["x"], box["y"], box["w"], box["h"])
    )
    if is_real is not True:
        raise CaptureRejected("活体检测未通过，请本人正对镜头并改善光线后重试")
    # Reuse the aligned RGB crop so detection and embedding use the same face.
    faces = DeepFace.represent(img_path=face["face"], model_name="SFace",
                               detector_backend="skip")
    vector = np.asarray(faces[0]["embedding"], dtype=np.float32)
    norm = np.linalg.norm(vector)
    if vector.shape != (128,) or norm == 0 or not np.isfinite(norm):
        raise ValueError("Invalid embedding")
    return vector / norm


if __name__ == "__main__":
    from waitress import serve
    serve(app, host="127.0.0.1", port=8091, threads=4)
