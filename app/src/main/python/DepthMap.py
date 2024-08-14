import numpy as np
import cv2 as cv


class DepthMap:
    def __init__(self, imgLeft_bytes, imgRight_bytes):

        # Convert bytes to numpy arrays
        imgLeft_np = np.frombuffer(imgLeft_bytes, np.uint8)
        imgRight_np = np.frombuffer(imgRight_bytes, np.uint8)

        # Decode numpy arrays to images
        self.imgLeft = cv.imdecode(imgLeft_np, cv.IMREAD_GRAYSCALE)
        self.imgRight = cv.imdecode(imgRight_np, cv.IMREAD_GRAYSCALE)

        # Check if images were loaded successfully
        if self.imgLeft is None or self.imgRight is None:
            raise ValueError("One or both images could not be read. Check file paths and formats.")

        # Ensure images are of the same size (optional, depending on your images)
        if self.imgLeft.shape != self.imgRight.shape:
            self.imgLeft = cv.resize(self.imgLeft, (self.imgRight.shape[1], self.imgRight.shape[0]))

        # self.show_images()

    def show_images(self):
        cv.imshow('Left Image', self.imgLeft)
        cv.imshow('Right Image', self.imgRight)
        cv.waitKey(0)
        cv.destroyAllWindows()

    def compute_depth_map_sgbm(self):
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
        disparity = stereo.compute(self.imgLeft, self.imgRight).astype(np.float32) / 16.0

        cv.imshow('Disparity Map', (disparity - min_disp) / num_disp)
        cv.waitKey(0)
        cv.destroyAllWindows()

        return disparity

    def generate_point_cloud(self, disparity):
        # Assuming camera parameters (you should replace these with your actual calibration data)
        h, w = self.imgLeft.shape
        focal_length = 0.8 * w  # Example value
        baseline = 0.54  # Example value in meters

        # Reprojection matrix Q
        Q = np.float32([[1, 0, 0, -0.5 * w],
                        [0, -1, 0, 0.5 * h],
                        [0, 0, 0, -focal_length],
                        [0, 0, 1 / baseline, 0]])

        # Reproject disparity to 3D
        points_3D = cv.reprojectImageTo3D(disparity, Q)
        colors = cv.cvtColor(self.imgLeft, cv.COLOR_GRAY2RGB)  # Assume left image for colors

        # Mask to filter out points with no disparity
        mask = disparity > disparity.min()
        out_points = points_3D[mask]
        out_colors = colors[mask]
        return out_points, out_colors

    def demo_stereo_sgbm(self):
        # Compute the disparity map using SGBM
        disparity = self.compute_depth_map_sgbm()

        # Generate and visualize the point cloud
        points, colors = self.generate_point_cloud(disparity)
        return points, colors


if __name__ == '__main__':
    # Create an instance of DepthMap with showImages=True to view images
    # Initialize video capture
    cap = cv.VideoCapture('v2.mp4')

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
    # self.imgLeft = cv.imread("img_1.png", cv.IMREAD_GRAYSCALE)
    # self.imgRight = cv.imread("img_2.png", cv.IMREAD_GRAYSCALE)

    depth_map = DepthMap(imgLeft_bytes=bytearray_left, imgRight_bytes=bytearray_right)

    # Compute and display disparity map using StereoSGBM and generate point cloud
    depth_map.demo_stereo_sgbm()

