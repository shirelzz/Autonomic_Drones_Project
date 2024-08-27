import cv2 as cv

from plane_detection import PlaneDetection
from plane import *
from DepthMap import DepthMap
import torch
import base64

from ConvertPLY2PNG import find_fixed_size_white_areas, remove_overlapping_areas, plot_white_areas_plane, \
    calculate_movement_to_landing_spot, find_white_areas, plot_white_areas
from photogrammetry import get_window


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

    # Concatenate along a specific axis (e.g., axis=0)
    detected_planes_array = np.concatenate(detected_planes, axis=0)

    # Convert the detected planes to a PyTorch tensor
    detected_planes_tensor = torch.tensor(detected_planes_array, dtype=torch.float32)

    return detected_planes_tensor


def visualize_plane_as_bitmap(detected_planes_tensor, altitude):
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

    # Rotate the image by -45 degrees
    (h, w) = img.shape[:2]
    center = (w // 2, h // 2)
    M = cv.getRotationMatrix2D(center, 180, 1.0)
    rotated_img = cv.warpAffine(img, M, (w, h), borderValue=(0, 0, 0))

    # Mirror the image horizontally
    mirrored_img = cv.flip(rotated_img, 1)

    # Calculate image size in meters
    print("h:", mirrored_img.shape)
    print("w:", w)
    kernel = get_window(altitude, w, h)
    # Find white areas of size ?
    # white_areas = find_fixed_size_white_areas(mirrored_img, kernel.shape)
    # white_areas = remove_overlapping_areas(white_areas, overlap_threshold=0.5)
    if kernel.shape[0] > kernel.shape[1]:
        size = kernel.shape[0]
    else:
        size = kernel.shape[1]
    white_area = find_white_areas(mirrored_img, size)
    # white_area = find_white_areas(mirrored_img, kernel)
    dx, dy = 0, 0
    # Check if any white areas are found
    if not white_area:
        # if not white_areas:
        print("No white areas found.")
    else:
        print(f"Found {len(white_area)} white areas.")
        # print(f"Found {len(white_areas)} white areas.")

        # for area in white_areas:
        #     print(f"Type of area: {type(area)}")  # This will help identify the actual type
        #     if isinstance(area, dict):
        #         print(f"Area with bbox {area['bbox']} and size {area['area']} pixels.")
        #     else:
        #         print(f"Unexpected type: {area}")
        print(f"Area with bbox {white_area['bbox']} and size {white_area['area']} pixels.")

        # Plot the results with red borders
#         plot_white_areas(mirrored_img, [white_area]) // only one area
        # plot_white_areas_plane(mirrored_img, [white_areas]) // multiple areas

        dx, dy = calculate_movement_to_landing_spot(mirrored_img, white_area)
    # Encode the image to bitmap format
    is_success, img_encoded = cv.imencode('.bmp', mirrored_img)

    # Convert the buffer to bytearray
    if is_success:
        image_bytes = bytearray(img_encoded)
        return [base64.b64encode(image_bytes).decode('utf-8'), [dx, dy]]
    else:
        return None


def start_detect(imgLeft, imgRight, altitude):
    # Initialize depth map
    points, colors = initialize_pipeline(imgLeft, imgRight)

    # Process the point cloud
    detected_planes_tensor = process_point_cloud(points)

    if detected_planes_tensor is None:
        return None

    # Visualize the first detected plane and return it as a bitmap
    bitmap = visualize_plane_as_bitmap(detected_planes_tensor, altitude)
    # if bitmap:
    #
    #     # Decode the base64 string back to bytes
    #     image_bytes = base64.b64decode(bitmap)
    #
    #     # Convert the bytes to a NumPy array
    #     np_array = np.frombuffer(image_bytes, np.uint8)
    #
    #     # Decode the NumPy array to an image
    #     img = cv.imdecode(np_array, cv.IMREAD_COLOR)
    #
    #     # Display the image using OpenCV
    #     cv.imshow("Detected Plane", img)
    #     cv.waitKey(0)
    #     cv.destroyAllWindows()
    return bitmap


# if __name__ == "__main__":
#     # Initialize video capture
#
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
#
#     altitude = 1
#     bitmapArr = start_detect(imgLeft=bytearray_left, imgRight=bytearray_right, altitude=altitude)
#     bitmap = bitmapArr[0]
#     movement = bitmapArr[1]
#     print("dx (roll adjustment):", movement[0])
#     print("dy (pitch adjustment):", movement[1])
