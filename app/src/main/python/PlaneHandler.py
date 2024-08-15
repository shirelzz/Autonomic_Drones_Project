import cv2 as cv
from plane_detection import PlaneDetection
from plane import *
from DepthMap import DepthMap
import torch
import base64


def initialize_pipeline(imgLeft_bytes, imgRight_bytes):
    """Initializes the depth map with the given image bytes."""
    points, colors = DepthMap(imgLeft_bytes=imgLeft_bytes, imgRight_bytes=imgRight_bytes)
    return points, colors


def process_point_cloud(points):
    """Processes the point cloud using Iterative RANSAC and prepares it for visualization."""
    plane_detector = PlaneDetection(
        geometry=Plane(),
        ransac_params={
            "THRESH": 0.02,
            "PLANE_SIZE": 16500},
    )

    detected_planes = plane_detector.detect_planes(points)

    if detected_planes is None:
        return None

    # Convert the detected planes to a PyTorch tensor
    detected_planes_tensor = torch.tensor(detected_planes, dtype=torch.float32)

    return detected_planes_tensor


def visualize_plane_as_bitmap(detected_planes_tensor):
    """Visualizes the first detected plane as a bitmap image."""

    combined_points = detected_planes_tensor.numpy().reshape(-1, 3)  # Convert tensor to numpy array and reshape
    x, y, z = combined_points.T

    # Normalize the points for display
    x_norm = cv.normalize(x, None, 0, 255, cv.NORM_MINMAX).astype(np.uint8)
    y_norm = cv.normalize(y, None, 0, 255, cv.NORM_MINMAX).astype(np.uint8)

    # Create a blank image
    img = np.zeros((256, 256, 3), dtype=np.uint8)

    # Plot the points on the image
    for xi, yi in zip(x_norm, y_norm):
        cv.circle(img, (int(xi), int(yi)), 2, (255, 255, 255), -1)

    # Encode the image to bitmap format
    is_success, img_encoded = cv.imencode('.bmp', img)

    # Convert the buffer to bytearray
    if is_success:
        image_bytes = bytearray(img_encoded)
        return base64.b64encode(image_bytes).decode('utf-8')
    else:
        return None


def start_detect(imgLeft, imgRight):
    # Initialize depth map
    points, colors = initialize_pipeline(imgLeft, imgRight)

    # Process the point cloud
    detected_planes_tensor = process_point_cloud(points)

    if detected_planes_tensor is None:
        return None

    # Visualize the first detected plane and return it as a bitmap
    bitmap = visualize_plane_as_bitmap(detected_planes_tensor)

    return bitmap


# if __name__ == "__main__":
#     # Initialize video capture
#     cap = cv.VideoCapture('src/v2.mp4')
#
#     # Extract two consecutive frames as left and right images
#     ret1, frame1 = cap.read()  # First frame as left image
#     ret2, frame2 = cap.read()  # Second frame as right image
#
#     cap.release()
#
#     if not ret1 or not ret2:
#         print("Failed to read frames from video")
#         exit()
#
#     # Convert frames to grayscale
#     imgLeft = cv.cvtColor(frame1, cv.COLOR_BGR2GRAY)
#     imgRight = cv.cvtColor(frame2, cv.COLOR_BGR2GRAY)
#
#     # Encode the images to PNG or JPEG format
#     _, buffer_left = cv.imencode('.png', imgLeft)
#     _, buffer_right = cv.imencode('.png', imgRight)
#
#     # Convert the buffers to bytearray
#     bytearray_left = bytearray(buffer_left)
#     bytearray_right = bytearray(buffer_right)
#     start_detect(imgLeft=bytearray_left, imgRight=bytearray_right)
