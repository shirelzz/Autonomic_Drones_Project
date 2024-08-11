import cv2
import numpy as np
from sklearn.linear_model import RANSACRegressor


def bytes_to_mat(byte_data):
    np_array = np.frombuffer(byte_data, np.uint8)
    mat = cv2.imdecode(np_array, cv2.IMREAD_GRAYSCALE)
    return mat


def process_images(byte_data1, byte_data2):
    img_left = bytes_to_mat(byte_data1)
    img_right = bytes_to_mat(byte_data2)

    depth_map = DepthMap(False)
    depth_map.set_images(img_left, img_right)
    return depth_map.demo_stereo_sgbm()


class DepthMap:
    def __init__(self, show_images):
        self.show_images = show_images

    def set_images(self, img_left, img_right):
        self.imgLeft = img_left
        self.imgRight = img_right

    def compute_depth_map_sgbm(self):
        window_size = 7
        min_disp = 16
        nDispFactor = 14
        num_disp = 16 * nDispFactor - min_disp
        stereo = cv2.StereoSGBM_create(
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
            mode=cv2.STEREO_SGBM_MODE_SGBM_3WAY
        )

        disparity = stereo.compute(self.imgLeft, self.imgRight).astype(np.float32) / 16.0
        return disparity

    def compute_point_cloud(self, disparity, Q):
        points_3d = cv2.reprojectImageTo3D(disparity, Q)
        mask = (disparity > disparity.min()) & np.isfinite(points_3d).all(axis=2)
        points_3d = points_3d[mask]
        return points_3d

    def fit_plane_ransac(self, points):
        ransac = RANSACRegressor(residual_threshold=0.01, max_trials=1000)
        ransac.fit(points[:, :2], points[:, 2])
        inlier_mask = ransac.inlier_mask_
        return inlier_mask, ransac.estimator_.coef_, ransac.estimator_.intercept_

    def is_plane(self, points):
        inlier_mask, coef, intercept = self.fit_plane_ransac(points)
        inlier_ratio = np.sum(inlier_mask) / len(points)
        threshold = 0.8
        return inlier_ratio > threshold

    def demo_stereo_sgbm(self):
        disparity = self.compute_depth_map_sgbm()
        Q = np.array([[1, 0, 0, -self.imgLeft.shape[1] / 2],
                      [0, -1, 0, self.imgLeft.shape[0] / 2],
                      [0, 0, 0, -1],
                      [0, 0, 1, 0]])
        points_3d = self.compute_point_cloud(disparity, Q)
        return self.is_plane(points_3d)