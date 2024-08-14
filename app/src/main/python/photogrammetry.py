import math
import numpy as np

# https://www.dji.com/support/product/mavic-air-2


def calculate_image_size_in_meters(altitude, image_width_px, image_height_px):
    fov_degrees = 84  # Field of View
    sensor_width_mm = 6.3  # Sensor width in millimeters (approximate for 1/2" CMOS sensor)
    sensor_height_mm = 4.7  # Sensor height in millimeters (approximate for 1/2" CMOS sensor)

    # Convert sensor dimensions from mm to meters
    sensor_width_m = sensor_width_mm / 1000
    sensor_height_m = sensor_height_mm / 1000

    # Calculate the Field of View in meters at the given altitude
    fov_radians = math.radians(fov_degrees)
    width_m = 2 * (altitude * math.tan(fov_radians / 2))
    height_m = width_m * (sensor_height_m / sensor_width_m)

    # Calculate the pixel size in meters
    pixel_width_m = width_m / image_width_px
    pixel_height_m = height_m / image_height_px

    # Size of the entire image in meters
    image_width_m = pixel_width_m * image_width_px
    image_height_m = pixel_height_m * image_height_px

    return width_m, height_m, pixel_width_m, pixel_height_m, image_width_m, image_height_m


def get_kernel_for_specific_dimensions(window_size_m, pixel_width_m, pixel_height_m):

    # Calculate the kernel size in pixels
    kernel_width_px = round(window_size_m / pixel_width_m)
    kernel_height_px = round(window_size_m / pixel_height_m)

    # Create a kernel of the calculated size
    kernel = np.ones((kernel_height_px, kernel_width_px), dtype=np.uint8)

    # Example usage in OpenCV for creating a kernel
    print(f"Kernel Size: {kernel_width_px}x{kernel_height_px} pixels")

    return kernel


# Usage:

# Camera specifications
altitude = 10  # in meters
image_width_px = 640  # Image width in pixels
image_height_px = 480  # Image height in pixels

# Calculate image size in meters
width_m, height_m, pixel_width_m, pixel_height_m, image_width_m, image_height_m = calculate_image_size_in_meters(
    altitude, image_width_px, image_height_px)

print(f"Field of View Width (meters): {width_m:.2f}")
print(f"Field of View Height (meters): {height_m:.2f}")
print(f"Pixel Width (meters): {pixel_width_m:.4f}")
print(f"Pixel Height (meters): {pixel_height_m:.4f}")
print(f"Image Width (meters): {image_width_m:.2f}")
print(f"Image Height (meters): {image_height_m:.2f}")

window_size_m = 0.5     # Size of the window to detect in meters
kernel = get_kernel_for_specific_dimensions(window_size_m, pixel_width_m, pixel_height_m)


