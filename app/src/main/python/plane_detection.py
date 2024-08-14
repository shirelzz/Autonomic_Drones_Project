"""Plane Detection Interface and concrete RANSAC implementation"""
from abc import abstractmethod
from pathlib import Path
from typing import Dict, Any, List, Optional

import cv2
import numpy as np
from sklearn.linear_model import RANSACRegressor
from sklearn.preprocessing import PolynomialFeatures
from plane import *


class PlaneDetection:
    """Abstract Class of Plane Detection"""

    @abstractmethod
    def detect_planes(self, point_cloud) -> np.ndarray:
        """Plane Detection"""


class IterativeRANSAC(PlaneDetection):
    """
    Iterative RANSAC algorithm to detect n planes based on minimal plane size,
    set by the user.
    """

    def __init__(
        self,
        geometry: Plane,
        out_dir: Path,
        ransac_params: Dict[str, float],
        store: bool = False,
    ):
        self.out_dir = out_dir
        self.geometry = geometry
        self.plane_size = ransac_params["PLANE_SIZE"]
        self.thresh = ransac_params["THRESH"]
        self.store = store
        self.pcd_out: Optional[np.ndarray] = None
        self.eqs: List[List[Any]] = []
        self.planes: List[np.ndarray] = []

    def detect_planes(self, point_cloud: np.ndarray) -> Optional[List[np.ndarray]]:
        """Detect planes using an iterative RANSAC algorithm

        Args:
            point_cloud (np.ndarray): point cloud data

        Returns:
            List[np.ndarray]: list of detected planes
        """
        points = point_cloud
        print("Iterative RANSAC...")
        plane_counter = 0

        while True:
            best_eq, best_inliers = self.geometry.fit(points, self.thresh)

            if len(best_inliers) < self.plane_size:
                break

            plane_counter += 1
            self.eqs.append(best_eq)

            # Remove the best inliers from the overall point cloud
            self.pcd_out = np.delete(points, best_inliers, axis=0)
            plane = points[best_inliers]
            self.planes.append(plane)
            points = self.pcd_out

        if not self.planes:
            return None

        # Save the first detected plane
        self.save_pcs("point_cloud.npy", self.out_dir, self.planes[0])

        # Get the axis-aligned bounding box
        min_bound = np.min(self.planes[0], axis=0)
        max_bound = np.max(self.planes[0], axis=0)

        width = max_bound[0] - min_bound[0]
        height = max_bound[1] - min_bound[1]

        print(f"Width: {width:.2f} units")
        print(f"Height: {height:.2f} units")

        self.display_pointcloud(self.planes[0])
        return self.planes

    def save_pcs(self, filename: str, out_dir: Path, pcd_out: np.ndarray) -> None:
        """Saves point cloud data to a file

        Args:
            filename (str): File name to save point cloud
            out_dir (Path): Output directory path
            pcd_out (np.ndarray): Point cloud data
        """
        try:
            data_path = out_dir / filename
            if not data_path.is_file():
                np.save(data_path, pcd_out)
        except Exception as exc:
            print(exc)

    def display_pointcloud(self, pcd_out: np.ndarray) -> None:
        """Displays the final point cloud using OpenCV

        Args:
            pcd_out (np.ndarray): Point cloud data to display
        """
        try:
            if pcd_out.size == 0:
                raise ValueError("You tried to display an empty point cloud!")

            # # OpenCV visualization (scatter plot)
            # img = np.zeros((600, 600, 3), dtype=np.uint8)
            # pcd_2d = pcd_out[:, :2].astype(int)
            # pcd_2d -= pcd_2d.min(axis=0)  # Normalize to fit in the image
            #
            # for x, y in pcd_2d:
            #     cv2.circle(img, (x, y), 1, (255, 255, 255), -1)

            # cv2.imshow("PointCloud--", img)
            # cv2.waitKey(0)
            # cv2.destroyAllWindows()
        except Exception as exc:
            print(exc)

    @staticmethod
    def _restore_color(color_cloud: np.ndarray, raw_cloud: np.ndarray) -> np.ndarray:
        """Restores color of raw point cloud

        Args:
            color_cloud (np.ndarray): colorful source point cloud
            raw_cloud (np.ndarray): unicolor target point cloud

        Returns:
            np.ndarray: colorized target point cloud
        """
        try:
            dists = np.linalg.norm(color_cloud - raw_cloud, axis=1)
            ind = np.where(dists < 0.01)[0]
            raw_cloud = color_cloud[ind]
        except Exception as exc:
            print(exc)
        return raw_cloud
