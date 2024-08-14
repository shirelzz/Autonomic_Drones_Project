import numpy as np
from PIL import Image
import io
from skimage.measure import label, regionprops
import matplotlib.pyplot as plt
import matplotlib.patches as patches

# Function to convert PLY content to PNG
def ply_to_png(ply_content=None, ply_file_path=None, mirror=False):
    # Step 1: Parse the PLY content and extract points
    def load_ply(content):
        lines = content.splitlines()
        points = []
        header_end = False

        for line in lines:
            if header_end:
                points.append(list(map(float, line.strip().split())))
            elif line.strip() == 'end_header':
                header_end = True

        return np.array(points)

    # Step 2: Project 3D points onto a 2D plane
    def project_points(points):
        return points[:, :2]  # Simple orthographic projection

    # Step 3: Render the 2D points into an image
    def render_image(points_2d, image_size=(500, 500)):
        img = np.zeros(image_size, dtype=np.uint8)

        # Normalize points to fit within the image
        min_vals = points_2d.min(axis=0)
        max_vals = points_2d.max(axis=0)

        normalized_points = (points_2d - min_vals) / (max_vals - min_vals)
        pixel_points = (normalized_points * (np.array(image_size) - 1)).astype(int)

        for x, y in pixel_points:
            img[y, x] = 255  # Set pixel to white

        return img

    # Step 4: Rotate the image by -45 degrees
    def rotate_image(img, angle=180):
        image_pil = Image.fromarray(img)
        rotated_image = image_pil.rotate(angle, expand=True, fillcolor=0)
        return np.array(rotated_image)

    # Step 5: Mirror the image horizontally
    def mirror_image(img):
        return np.fliplr(img)

    # Step 6: Convert the image to PNG and return it
    def save_image_to_bytes(img):
        image = Image.fromarray(img)
        byte_io = io.BytesIO()
        image.save(byte_io, format='PNG')
        byte_io.seek(0)
        return byte_io.getvalue()

    # Load PLY content
    if ply_file_path:
        with open(ply_file_path, 'r') as file:
            ply_content = file.read()

    if not ply_content:
        raise ValueError("PLY content must be provided either as a string or a file path.")

    # Process PLY content
    points = load_ply(ply_content)
    points_2d = project_points(points)
    image = render_image(points_2d)

    # Rotate the image
    rotated_image = rotate_image(image)

    # Mirror the image if requested
    if mirror:
        rotated_image = mirror_image(rotated_image)

    # Convert the final image to PNG
    png_bytes = save_image_to_bytes(rotated_image)

    return png_bytes

# Function to load a PNG image into a NumPy array
def load_png_image(image_path):
    img = Image.open(image_path).convert('L')  # Convert to grayscale
    return np.array(img)

# Function to find white areas in the PNG image
def find_white_areas(image_array, min_size):
    min_size_area = min_size * min_size

    # Find connected components in the image
    labeled_image, num_labels = label(image_array == 255, connectivity=2, return_num=True)

    white_areas = []

    for region in regionprops(labeled_image):
        if region.area >= min_size_area:
            minr, minc, maxr, maxc = region.bbox
            white_areas.append({
                'bbox': (minr, minc, maxr, maxc),
                'area': region.area
            })

    return white_areas

# Function to plot white areas with red square borders
def plot_white_areas(image_array, white_areas):
    fig, ax = plt.subplots()
    ax.imshow(image_array, cmap='gray')

    for area in white_areas:
        minr, minc, maxr, maxc = area['bbox']
        rect = patches.Rectangle((minc, minr), maxc - minc, maxr - minr, linewidth=1, edgecolor='red', facecolor='none')
        ax.add_patch(rect)

    plt.show()

# Example usage
# Convert PLY to PNG
ply_file_path = "point_cloud.ply"
png_data = ply_to_png(ply_file_path=ply_file_path, mirror=True)

# Save PNG data to a file
with open('output_image.png', 'wb') as f:
    f.write(png_data)

# Load the PNG image
image_array = load_png_image('output_image.png')

# Find white areas of size 50 cm x 50 cm
white_areas = find_white_areas(image_array, min_size=15)

# Check if any white areas are found
if not white_areas:
    print("No white areas found.")
else:
    print(f"Found {len(white_areas)} white areas.")
    for area in white_areas:
        print(f"Area with bbox {area['bbox']} and size {area['area']} pixels.")

    # Plot the results with red borders
    plot_white_areas(image_array, white_areas)

