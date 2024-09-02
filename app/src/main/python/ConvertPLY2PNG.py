import numpy as np
from PIL import Image
import io
from skimage.measure import label, regionprops
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import cv2 as cv
from skimage.morphology import closing, square


# # Function to convert PLY content to PNG
# def ply_to_png(ply_content=None, ply_file_path=None, mirror=False):
#     # Step 1: Parse the PLY content and extract points
#     def load_ply(content):
#         lines = content.splitlines()
#         points = []
#         header_end = False
#
#         for line in lines:
#             if header_end:
#                 points.append(list(map(float, line.strip().split())))
#             elif line.strip() == 'end_header':
#                 header_end = True
#
#         return np.array(points)
#
#     # Step 2: Project 3D points onto a 2D plane
#     def project_points(points):
#         return points[:, :2]  # Simple orthographic projection
#
#     # Step 3: Render the 2D points into an image
#     def render_image(points_2d, image_size=(500, 500)):
#         img = np.zeros(image_size, dtype=np.uint8)
#
#         # Normalize points to fit within the image
#         min_vals = points_2d.min(axis=0)
#         max_vals = points_2d.max(axis=0)
#
#         normalized_points = (points_2d - min_vals) / (max_vals - min_vals)
#         pixel_points = (normalized_points * (np.array(image_size) - 1)).astype(int)
#
#         for x, y in pixel_points:
#             img[y, x] = 255  # Set pixel to white
#
#         return img
#
#     # Step 4: Rotate the image by -45 degrees
#     def rotate_image(img, angle=180):
#         image_pil = Image.fromarray(img)
#         rotated_image = image_pil.rotate(angle, expand=True, fillcolor=0)
#         return np.array(rotated_image)
#
#     # Step 5: Mirror the image horizontally
#     def mirror_image(img):
#         return np.fliplr(img)
#
#     # Step 6: Convert the image to PNG and return it
#     def save_image_to_bytes(img):
#         image = Image.fromarray(img)
#         byte_io = io.BytesIO()
#         image.save(byte_io, format='PNG')
#         byte_io.seek(0)
#         return byte_io.getvalue()
#
#     # Load PLY content
#     if ply_file_path:
#         with open(ply_file_path, 'r') as file:
#             ply_content = file.read()
#
#     if not ply_content:
#         raise ValueError("PLY content must be provided either as a string or a file path.")
#
#     # Process PLY content
#     points = load_ply(ply_content)
#     points_2d = project_points(points)
#     image = render_image(points_2d)
#
#     # Rotate the image
#     rotated_image = rotate_image(image)
#
#     # Mirror the image if requested
#     if mirror:
#         rotated_image = mirror_image(rotated_image)
#
#     # Convert the final image to PNG
#     png_bytes = save_image_to_bytes(rotated_image)
#
#     return png_bytes
#
# # Function to load a PNG image into a NumPy array
# def load_png_image(image_path):
#     img = Image.open(image_path).convert('L')  # Convert to grayscale
#     return np.array(img)


def calculate_iou(boxA, boxB):
    """
    Calculate the Intersection over Union (IoU) of two bounding boxes.
    :param boxA: Tuple (ymin, xmin, ymax, xmax) of the first box.
    :param boxB: Tuple (ymin, xmin, ymax, xmax) of the second box.
    :return: IoU value.
    """
    yA = max(boxA[0], boxB[0])
    xA = max(boxA[1], boxB[1])
    yB = min(boxA[2], boxB[2])
    xB = min(boxA[3], boxB[3])

    interArea = max(0, yB - yA) * max(0, xB - xA)

    boxAArea = (boxA[2] - boxA[0]) * (boxA[3] - boxA[1])
    boxBArea = (boxB[2] - boxB[0]) * (boxB[3] - boxB[1])

    iou = interArea / float(boxAArea + boxBArea - interArea)

    return iou


def remove_overlapping_areas(areas, overlap_threshold=0.5):
    """
    Remove overlapping bounding boxes based on an overlap threshold.
    :param areas: List of detected areas with 'bbox' and 'area'.
    :param overlap_threshold: Threshold to determine significant overlap.
    :return: List of filtered areas.
    """
    filtered_areas = []

    for i in range(len(areas)):
        keep = True
        minrA, mincA, maxrA, maxcA = areas[i]['bbox']
        for j in range(i):
            minrB, mincB, maxrB, maxcB = areas[j]['bbox']
            iou = calculate_iou((minrA, mincA, maxrA, maxcA), (minrB, mincB, maxrB, maxcB))
            if iou > overlap_threshold:
                keep = False
                break
        if keep:
            filtered_areas.append(areas[i])

    return filtered_areas


def find_fixed_size_white_areas(image_array, fixed_size):
    """
    Detects fixed-size white areas in the image with no black pixels.

    :param image_array: Input image array (grayscale).
    :param fixed_size: Tuple (height, width) specifying the fixed size of the white areas to detect.
    :return: List of detected white areas, each with 'bbox' and 'area'.
    """


    if len(image_array.shape) == 3:
        # Convert to grayscale if the image is in color
        image_array = cv.cvtColor(image_array, cv.COLOR_BGR2GRAY)

    # Apply a threshold to create a binary image
    _, binary_image = cv.threshold(image_array, 240, 255, cv.THRESH_BINARY)

    # Apply morphological operations to clean the image
    cleaned_image = closing(binary_image, square(3))
    # Optionally apply dilation and erosion for better results
    cleaned_image = cv.dilate(cleaned_image, None, iterations=1)
    cleaned_image = cv.erode(cleaned_image, None, iterations=1)

    # Get dimensions of the fixed size
    fixed_height, fixed_width = fixed_size
    print("fixed_height: ", fixed_height)
    print("fixed_width: ", fixed_width)
    if fixed_height < 15:
        fixed_height = 15
    if fixed_width < 15:
        fixed_width = 15
    white_areas = []

    # Slide a fixed-size window over the image
    for y in range(0, cleaned_image.shape[0] - fixed_height + 1):
        for x in range(0, cleaned_image.shape[1] - fixed_width + 1):
            region = cleaned_image[y:y + fixed_height, x:x + fixed_width]
            if np.all(region == 255):  # Check if all pixels in the region are white
                white_areas.append({
                    'bbox': (y, x, y + fixed_height, x + fixed_width),
                    'area': fixed_height * fixed_width
                })

    return white_areas


"""
    Finds the largest white square area in the image that is larger than the specified min_size.

    Args:
        image_array (numpy.ndarray): The binary image to search within.
        min_size (int): The minimum width and height of the white square to be considered.

    Returns:
        dict: A dictionary containing the bounding box (bbox) and area of the largest white square.
              Returns None if no such area is found.
"""


def find_white_areas(image_array, min_size):

    # Find connected components in the image
    labeled_image, num_labels = label(image_array == 255, connectivity=2, return_num=True)

    best_landing_spot = None
    max_area = 0

    for region in regionprops(labeled_image):
        if len(region.bbox) == 4:
            minr, minc, maxr, maxc = region.bbox
        elif len(region.bbox) > 4:
            minr, minc, _, maxr, maxc, _ = region.bbox  # Ignore extra dimensions if present
        else:
            continue  # Skip if the bounding box is not in the expected format

        # Calculate the width and height of the region
        width = maxc - minc
        height = maxr - minr

        # Check if the region is large enough
        if width < min_size or height < min_size:
            continue

        # Loop over the region and check for the largest all-white sub-region
        for size in range(min(width, height), min_size - 1, -1):
            found = False  # Flag to exit the loop early if a sub-region is found
            for i in range(minr, maxr - size + 1):
                for j in range(minc, maxc - size + 1):
                    # Extract the sub-image
                    sub_image = image_array[i:i + size, j:j + size]

                    # Check if all pixels in the sub-image are white
                    if np.all(sub_image == 255):
                        # Calculate the area of the sub-image
                        area = size * size

                        # Update the best landing spot if this area is larger
                        if area > max_area:
                            max_area = area
                            best_landing_spot = {
                                'bbox': (i, j, i + size, j + size),
                                'area': area
                            }
                        found = True  # Set the flag to True if a valid area is found
                        break  # Exit the inner loop since we want the largest sub-region
                if found:
                    break  # Exit the outer loop early if a sub-region is found

    return best_landing_spot


def calculate_movement_to_landing_spot(image_array, best_landing_spot, pixel_width_m, pixel_height_m ):
    # Get the dimensions of the image
    height, width = image_array.shape[:2]

    # Calculate the center of the image
    center_image_x = width / 2
    center_image_y = height / 2

    # Get the bounding box of the best landing spot
    minr, minc, maxr, maxc = best_landing_spot['bbox']

    # Calculate the center of the chosen rectangle
    center_rect_x = (minc + maxc) / 2
    center_rect_y = (minr + maxr) / 2

    # Calculate the differences in x and y directions
    dx_px = center_rect_x - center_image_x
    dy_px = center_rect_y - center_image_y

    # Convert pixel movement to meters
    dx_real = dx_px * pixel_width_m
    dy_real = dy_px * pixel_height_m

    return dx_real, dy_real


# Function to plot white areas with red square borders
def plot_white_areas(image_array, white_areas):
    fig, ax = plt.subplots()
    ax.imshow(image_array, cmap='gray')

    for area in white_areas:
        minr, minc, maxr, maxc = area['bbox']
        rect = patches.Rectangle((minc, minr), maxc - minc, maxr - minr, linewidth=1, edgecolor='red', facecolor='none')
        ax.add_patch(rect)

    plt.show()


def plot_white_areas_plane(image_array, white_areas):
    fig, ax = plt.subplots()
    ax.imshow(image_array, cmap='gray')

    for area_list in white_areas:
        # Ensure area_list is a list of dictionaries
        if isinstance(area_list, list):
            for area in area_list:
                if isinstance(area, dict) and 'bbox' in area:
                    minr, minc, maxr, maxc = area['bbox']
                    rect = patches.Rectangle((minc, minr), maxc - minc, maxr - minr, linewidth=1, edgecolor='red', facecolor='none')
                    ax.add_patch(rect)
                else:
                    print(f"Unexpected area format in list: {area}")
        else:
            print(f"Unexpected area format: {area_list}")

    plt.show()

# # Example usage
# # Convert PLY to PNG
# ply_file_path = "point_cloud.ply"
# png_data = ply_to_png(ply_file_path=ply_file_path, mirror=True)
#
# # Save PNG data to a file
# with open('output_image.png', 'wb') as f:
#     f.write(png_data)
#
# # Load the PNG image
# image_array = load_png_image('output_image.png')
#
# # Find white areas of size 50 cm x 50 cm
# white_areas = find_white_areas(image_array, min_size=15)
#
# # Check if any white areas are found
# if not white_areas:
#     print("No white areas found.")
# else:
#     print(f"Found {len(white_areas)} white areas.")
#     for area in white_areas:
#         print(f"Area with bbox {area['bbox']} and size {area['area']} pixels.")
#
#     # Plot the results with red borders
#     plot_white_areas(image_array, white_areas)
#
