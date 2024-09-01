"""Plane Detection Interface and concrete RANSAC implementation"""

import numpy as np
from plane import *


class PlaneDetection:
    """
    Iterative RANSAC algorithm to detect n planes based on minimal plane size,
    set by the user.
    """

    def __init__(
        self,
        geometry: Plane,
        ransac_params,
        store: bool = False,
    ):
        self.geometry = geometry
        self.plane_size = ransac_params["PLANE_SIZE"]
        self.thresh = ransac_params["THRESH"]
        self.store = store
        self.pcd_out = None
        self.eqs = []
        self.planes = []

    def detect_planes(self, point_cloud: np.ndarray):
        """Detect planes using an iterative RANSAC algorithm

        Args:
            point_cloud (np.ndarray): point cloud data

        Returns:
            [np.ndarray]: list of detected planes
        """
        points = point_cloud
        print("Iterative RANSAC...")
        plane_counter = 0
        print("points", points)
        try:
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
                print("self.planes")
                return None

            # Get the axis-aligned bounding box
            min_bound = np.min(self.planes[0], axis=0)
            max_bound = np.max(self.planes[0], axis=0)

            width = max_bound[0] - min_bound[0]
            height = max_bound[1] - min_bound[1]

            print(f"Width: {width:.2f} units")
            print(f"Height: {height:.2f} units")

            return self.planes
        except (TypeError, ValueError) as e:
            print("1111: ", e)
