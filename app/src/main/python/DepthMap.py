import numpy as np
import cv2 as cv
from matplotlib import pyplot as plt
from sklearn.linear_model import RANSACRegressor

class DepthMap:
    def __init__(self, show_images):
        self.imgLeft = cv.imread("img1.png", cv.IMREAD_GRAYSCALE)
        self.imgRight = cv.imread("img2.png", cv.IMREAD_GRAYSCALE)
        # Check if images were loaded successfully
        if self.imgLeft is None or self.imgRight is None:
            raise ValueError("One or both images could not be read. Check file paths and formats.")
        # Ensure images are of the same size
        if self.imgLeft.shape != self.imgRight.shape:
            self.imgLeft = cv.resize(self.imgLeft, (self.imgRight.shape[1], self.imgRight.shape[0]))

        if show_images:
            plt.figure()
            plt.subplot(121)
            plt.imshow(self.imgLeft, cmap='gray')
            plt.subplot(122)
            plt.imshow(self.imgRight, cmap='gray')
            plt.show()

    def compute_depth_map_bm(self):
        nDispFactor = 12
        stereo = cv.StereoBM.create(numDisparities=16 * nDispFactor, blockSize=21)
        disparity = stereo.compute(self.imgLeft, self.imgRight)

        plt.title('BM Disparity Map')
        plt.imshow(disparity, 'gray')
        plt.show()
        return disparity

    def compute_depth_map_sgbm(self):
        window_size = 7
        min_disp = 16
        nDispFactor = 14
        num_disp = 16 * nDispFactor - min_disp
        stereo = cv.StereoSGBM_create(
            minDisparity=min_disp,
            numDisparities=num_disp,
            blockSize=window_size,
            P1=8 * 3 * window_size ** 2,
            P2=32 * 3 * window_size ** 2,
            disp12MaxDiff=1,
            uniquenessRatio=15,
            speckleWindowSize=0,
            speckleRange=2,
            preFilterCap=63,
            mode=cv.STEREO_SGBM_MODE_SGBM_3WAY
        )

        disparity = stereo.compute(self.imgLeft, self.imgRight).astype(np.float32) / 16.0

        plt.title('SGBM Disparity Map')
        plt.imshow(disparity, 'gray')
        plt.colorbar()
        plt.show()
        return disparity

    def compute_point_cloud(self, disparity, Q):
        points_3d = cv.reprojectImageTo3D(disparity, Q)
        mask = (disparity > disparity.min()) & np.isfinite(points_3d).all(axis=2)
        points_3d = points_3d[mask]
        return points_3d

    def fit_plane_ransac(self, points):
        # Fit a plane to the points using RANSAC
        ransac = RANSACRegressor(residual_threshold=0.01, max_trials=1000)
        ransac.fit(points[:, :2], points[:, 2])
        inlier_mask = ransac.inlier_mask_
        return inlier_mask, ransac.estimator_.coef_, ransac.estimator_.intercept_

    def is_plane(self, points):
        inlier_mask, coef, intercept = self.fit_plane_ransac(points)
        inlier_ratio = np.sum(inlier_mask) / len(points)
        print(f"Inlier Ratio: {inlier_ratio}")
        threshold = 0.8  # Adjust based on your requirements

        # Visualization of inliers and outliers
        plt.scatter(points[inlier_mask, 0], points[inlier_mask, 1], color='blue', s=1, label='Inliers')
        plt.scatter(points[~inlier_mask, 0], points[~inlier_mask, 1], color='red', s=1, label='Outliers')
        plt.legend(loc='upper right')  # Specify a fixed location for the legend
        plt.show()

        return inlier_ratio > threshold

    def demo_stereo_bm(self):
        disparity = self.compute_depth_map_bm()
        Q = np.array([[1, 0, 0, -self.imgLeft.shape[1]/2],
                      [0, -1, 0, self.imgLeft.shape[0]/2],
                      [0, 0, 0, -1],
                      [0, 0, 1, 0]])
        points_3d = self.compute_point_cloud(disparity, Q)
        if self.is_plane(points_3d):
            print("The scene is a plane (StereoBM).")
        else:
            print("The scene is not a plane (StereoBM).")

    def demo_stereo_sgbm(self):
        disparity = self.compute_depth_map_sgbm()
        Q = np.array([[1, 0, 0, -self.imgLeft.shape[1]/2],
                      [0, -1, 0, self.imgLeft.shape[0]/2],
                      [0, 0, 0, -1],
                      [0, 0, 1, 0]])
        points_3d = self.compute_point_cloud(disparity, Q)
        if self.is_plane(points_3d):
            print("The scene is a plane (StereoSGBM).")
            return True
        else:
            print("The scene is not a plane (StereoSGBM).")
            return False


if __name__ == '__main__':
    depth_map = DepthMap(show_images=True)
    depth_map.demo_stereo_bm()
    depth_map.demo_stereo_sgbm()
