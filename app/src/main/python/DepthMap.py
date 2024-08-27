import numpy as np
import cv2 as cv


def generate_point_cloud(disparity, imgLeft, imgRight, baseLine):
    # Assuming camera parameters (you should replace these with your actual calibration data)
    h, w = imgLeft.shape
    focal_length = 4.44 * w  # Example value

    baseline = 0.54  # Example value in meters
    if baseLine != 0.0:
        baseline = baseLine
    print("baseLine", baseline)
    print("disparity:  ", disparity)
    # Reprojection matrix Q
    Q = np.float32([[1, 0, 0, -0.5 * w],
                    [0, -1, 0, 0.5 * h],
                    [0, 0, 0, -focal_length],
                    [0, 0, 1 / baseline, 0]])

    # Reproject disparity to 3D
    points_3D = cv.reprojectImageTo3D(disparity, Q)
    print(points_3D)
    colors = cv.cvtColor(imgLeft, cv.COLOR_GRAY2RGB)  # Assume left image for colors

    # Mask to filter out points with no disparity
    mask = disparity > disparity.min()
    out_points = points_3D[mask]
    out_colors = colors[mask]
    return out_points, out_colors


def compute_depth_map_sgbm(imgLeft, imgRight):
    window_size = 7
    min_disp = 16
    nDispFactor = 14  # Adjust this (14 is good)
    num_disp = 16 * nDispFactor - min_disp
    stereo = cv.StereoSGBM_create(minDisparity=min_disp,
                                  numDisparities=num_disp,
                                  blockSize=window_size,
                                  P1=8 * 3 * window_size ** 2,
                                  P2=32 * 3 * window_size ** 2,
                                  disp12MaxDiff=1,
                                  uniquenessRatio=15,
                                  speckleWindowSize=0,
                                  speckleRange=2,
                                  preFilterCap=63,
                                  mode=cv.STEREO_SGBM_MODE_SGBM_3WAY)

    # Compute disparity map
    disparity = stereo.compute(imgLeft, imgRight).astype(np.float32) / 16.0
    return disparity


def DepthMap(imgLeft_bytes, imgRight_bytes, baseLine):
    # Convert bytes to numpy arrays
    imgLeft_np = np.frombuffer(imgLeft_bytes, np.uint8)
    imgRight_np = np.frombuffer(imgRight_bytes, np.uint8)

    # Decode numpy arrays to images
    imgLeft = cv.imdecode(imgLeft_np, cv.IMREAD_GRAYSCALE)
    imgRight = cv.imdecode(imgRight_np, cv.IMREAD_GRAYSCALE)

    # Check if images were loaded successfully
    if imgLeft is None or imgRight is None:
        raise ValueError("One or both images could not be read. Check file paths and formats.")

    # Ensure images are of the same size (optional, depending on your images)
    if imgLeft.shape != imgRight.shape:
        imgLeft = cv.resize(imgLeft, (imgRight.shape[1], imgRight.shape[0]))

    disparity = compute_depth_map_sgbm(imgLeft, imgRight)

    # Generate and visualize the point cloud
    points, colors = generate_point_cloud(disparity, imgLeft, imgRight, baseLine)
    return points, colors
