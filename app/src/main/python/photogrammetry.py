import numpy as np
import cv2 as cv

# https://www.dji.com/support/product/mavic-air-2


def calculate_image_size_in_meters(altitude, image_width_px, image_height_px):
    focal_length = 4.44  # Field of View mm
    sensor_width_mm = 6.4  # Sensor width in millimeters (approximate for 1/2" CMOS sensor)
    sensor_height_mm = 4.8  # Sensor height in millimeters (approximate for 1/2" CMOS sensor)

    # Calculate the GSD
    height_m = altitude * sensor_height_mm * 100 / (focal_length * image_height_px)
    width_m = altitude * sensor_width_mm * 100 / (focal_length * image_width_px)

    # Size of the entire image in meters
    image_width_m = width_m * image_width_px
    image_height_m = height_m * image_height_px

    return width_m, height_m, image_width_m, image_height_m


def get_kernel_for_specific_dimensions(window_size_m, pixel_width_m, pixel_height_m):

    # Calculate the kernel size in pixels
    kernel_width_px = round(window_size_m / pixel_width_m)
    if kernel_width_px == 0:
        kernel_width_px = 1
    print("kernel_width_px:", kernel_width_px)
    kernel_height_px = round(window_size_m / pixel_height_m)
    if kernel_height_px == 0:
        kernel_height_px = 1
    print("kernel_height_px:", kernel_height_px)

    # Create a kernel of the calculated size
    kernel = np.ones((kernel_height_px, kernel_width_px), dtype=np.uint8)

    # Example usage in OpenCV for creating a kernel
    print(f"Kernel Size: {kernel_width_px}x{kernel_height_px} pixels")

    return kernel


def get_window(altitude, image_width_px, image_height_px):
    window_size_m = 0.5
    # Calculate image size in meters
    pixel_width_m, pixel_height_m, image_width_m, image_height_m = calculate_image_size_in_meters(
        altitude, image_width_px, image_height_px)
    kernel = get_kernel_for_specific_dimensions(window_size_m, pixel_width_m, pixel_height_m)
    return kernel


if __name__ == "__main__":

    # Camera specifications
    altitude = 0.05  # in cm
    image_width_px = 256  # Image width in pixels
    image_height_px = 256  # Image height in pixels

    # Calculate image size in meters
    pixel_width_m, pixel_height_m, image_width_m, image_height_m = calculate_image_size_in_meters(
        altitude, image_width_px, image_height_px)

    print(f"Pixel Width (meters): {pixel_width_m:.2f}")
    print(f"Pixel Height (meters): {pixel_height_m:.2f}")
    print(f"Image Width (meters): {image_width_m:.2f}")
    print(f"Image Height (meters): {image_height_m:.2f}")

    window_size_m = 0.5   # Size of the window to detect in meters
    kernel = get_kernel_for_specific_dimensions(window_size_m, pixel_width_m, pixel_height_m)

