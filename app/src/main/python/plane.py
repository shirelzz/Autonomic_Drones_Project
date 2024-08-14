import random

import numpy as np
from scipy.spatial import ConvexHull


class Plane:
    """
    Implementation of planar RANSAC.

    Class for Plane object, which finds the equation of a infinite plane using RANSAC algorithim.

    Call `fit(.)` to randomly take 3 points of pointcloud to verify inliers based on a threshold.

    ![Plane](https://raw.githubusercontent.com/leomariga/pyRANSAC-3D/master/doc/plano.gif "Plane")

    ---
    """

    def __init__(self):
        self.inliers = []
        self.equation = []
        self.planes = []

    def fit(self, pts, thresh=0.05, minPoints=10, maxIteration=1000):
        """
        Find the best equation for a plane.

        :param pts: 3D point cloud as a `np.array (N,3)`.
        :param thresh: Threshold distance from the plane which is considered inlier.
        :param maxIteration: Number of maximum iteration which RANSAC will loop over.
        :returns:
        - `self.equation`:  Parameters of the plane using Ax+By+Cy+D `np.array (1, 4)`
        - `self.inliers`: points from the dataset considered inliers

        ---
        """

        n_points = pts.shape[0]
        best_eq = []
        best_inliers = []

        for it in range(maxIteration):

            # Samples 3 random points
            id_samples = random.sample(range(0, n_points), 3)
            pt_samples = pts[id_samples]

            vecA = pt_samples[1, :] - pt_samples[0, :]
            vecB = pt_samples[2, :] - pt_samples[0, :]

            if np.linalg.norm(vecA) < 1e-6 or np.linalg.norm(vecB) < 1e-6:
                continue

            vecC = np.cross(vecA, vecB)

            if np.linalg.norm(vecC) < 1e-6:
                continue

            vecC = vecC / np.linalg.norm(vecC)
            k = -np.sum(np.multiply(vecC, pt_samples[1, :]))

            # Ensure no NaN values are in the equation
            if np.isnan(vecC).any() or np.isnan(k):
                continue

            plane_eq = [vecC[0], vecC[1], vecC[2], k]

            dist_pt = (plane_eq[0] * pts[:, 0] + plane_eq[1] * pts[:, 1] + plane_eq[2] * pts[:, 2] + plane_eq[3]
                       ) / np.sqrt(plane_eq[0] ** 2 + plane_eq[1] ** 2 + plane_eq[2] ** 2)

            pt_id_inliers = np.where(np.abs(dist_pt) <= thresh)[0]
            if len(pt_id_inliers) > len(best_inliers):
                best_eq = plane_eq
                best_inliers = pt_id_inliers

        self.inliers = best_inliers
        self.equation = best_eq

        return self.equation, self.inliers

    def is_plane_filled(self, plane_points):
        """Check if the plane is filled without significant gaps"""
        if len(plane_points) == 0:
            return False

        hull = ConvexHull(plane_points[:, :2])  # Project to 2D for simplicity
        hull_area = hull.volume  # In 2D, volume is actually the area
        actual_area = len(plane_points) * np.mean(np.linalg.norm(np.diff(plane_points, axis=0), axis=1))

        if actual_area / hull_area > 0.05:  # Adjust threshold as needed
            return True

        return False