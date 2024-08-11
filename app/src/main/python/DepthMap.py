import numpy as np
import cv2 as cv

class DepthMap:
    def __init__(self, imgLeft, imgRight):
        self.imgLeft = imgLeft
        self.imgRight = imgRight

        # Ensure images are of the same size (optional, depending on your images)
        if self.imgLeft.shape != self.imgRight.shape:
            self.imgLeft = cv.resize(self.imgLeft, (self.imgRight.shape[1], self.imgRight.shape[0]))

    def computeDepthMapSGBM(self):
        window_size = 7
        min_disp = 16
        nDispFactor = 14  # adjust this (14 is good)
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
        return disparity

    def getSGBMImage(self):
        # Compute the disparity map
        disparity = self.computeDepthMapSGBM()
        # Normalize the disparity map to the range [0, 255] for visualization
        disparity_normalized = cv.normalize(disparity, None, 0, 255, cv.NORM_MINMAX)
        disparity_normalized = np.uint8(disparity_normalized)
        return disparity_normalized
