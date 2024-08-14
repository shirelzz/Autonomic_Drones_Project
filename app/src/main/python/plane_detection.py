import cv2 as cv
import matplotlib.pyplot as plt
from plane_detection import PlaneDetection
from plane import *
from DepthMap import DepthMap


def initialize_pipeline(imgLeft_bytes, imgRight_bytes):
    """Initializes the depth map with the given image bytes."""
    points, colors = DepthMap(imgLeft_bytes=imgLeft_bytes, imgRight_bytes=imgRight_bytes)
    return points, colors


def process_point_cloud(points):
    """Processes the point cloud using Iterative RANSAC."""
    plane_detector = PlaneDetection(
        geometry=Plane(),
        ransac_params={
            "THRESH": 0.02,
            "PLANE_SIZE": 16500},
    )

    detected_planes = plane_detector.detect_planes(points)
    return detected_planes


def visualize_plane(plane_points):
    """Visualizes the first detected plane using Matplotlib."""
    x, y, z = plane_points.T
    print("x:  ", x)
    print("y:  ", y)
    plt.figure()
    plt.scatter(x, y, color="#000000", s=5)
    plt.xlabel('X')
    plt.ylabel('Y')
    plt.title('Detected Plane Points')
    plt.savefig("plane1.png")
    # plt.show()


def start_detect(imgLeft, imgRight):

    # Initialize depth map
    points, colors = initialize_pipeline(imgLeft, imgRight)

    # Process the point cloud
    detected_planes = process_point_cloud(points)

    # Visualize the first detected plane if available
    if detected_planes:
        visualize_plane(detected_planes[0])


if __name__ == "__main__":
    # Initialize video capture
    cap = cv.VideoCapture('src/v2.mp4')

    # Extract two consecutive frames as left and right images
    ret1, frame1 = cap.read()  # First frame as left image
    ret2, frame2 = cap.read()  # Second frame as right image

    cap.release()

    if not ret1 or not ret2:
        print("Failed to read frames from video")
        exit()

    # Convert frames to grayscale
    imgLeft = cv.cvtColor(frame1, cv.COLOR_BGR2GRAY)
    imgRight = cv.cvtColor(frame2, cv.COLOR_BGR2GRAY)

    # Encode the images to PNG or JPEG format
    _, buffer_left = cv.imencode('.png', imgLeft)
    _, buffer_right = cv.imencode('.png', imgRight)

    # Convert the buffers to bytearray
    bytearray_left = bytearray(buffer_left)
    bytearray_right = bytearray(buffer_right)
    start_detect(imgLeft=bytearray_left, imgRight=bytearray_right)
