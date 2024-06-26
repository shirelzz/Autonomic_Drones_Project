import numpy as np
import cv2 as cv
from matplotlib import pyplot as plt


class DepthMap:
    def __init__(self, showImages):
        self.imgLeft = cv.imread("man1.png", cv.IMREAD_GRAYSCALE)
        self.imgRight = cv.imread("man2.png", cv.IMREAD_GRAYSCALE)

        # Check if images were loaded successfully
        if self.imgLeft is None or self.imgRight is None:
            raise ValueError("One or both images could not be read. Check file paths and formats.")

        # Ensure images are of the same size (optional, depending on your images)
        if self.imgLeft.shape != self.imgRight.shape:
            self.imgLeft = cv.resize(self.imgLeft, (self.imgRight.shape[1], self.imgRight.shape[0]))

        if showImages:
            plt.figure()
            plt.subplot(121)
            plt.imshow(self.imgLeft)
            plt.subplot(122)
            plt.imshow(self.imgRight)
            plt.show()

    def computeDepthMapBM(self):
        nDispFactor = 12  # adjust this
        stereo = cv.StereoBM.create(numDisparities=16 * nDispFactor,
                                    blockSize=21)
        disparity = stereo.compute(self.imgLeft, self.imgRight)

        plt.title('BM')
        plt.imshow(disparity, 'gray')
        plt.show()

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

        # Display the disparity map
        plt.imshow(disparity, 'gray')
        plt.title('SGBM')
        plt.colorbar()
        plt.show()

    def demoViewPics(self):
        # See pictures
        dp = DepthMap(showImages=True)

    def demoStereoBM(self):
        # dp = DepthMap(showimages=False)
        self.computeDepthMapBM()


    def demoStereoSGBM(self):
        # dp = DepthMap(showimages=False)
        # dp.computeDepthMapSGBM()
        self.computeDepthMapSGBM()


if __name__ == '__main__':
    # Create an instance of DepthMap with showImages=True to view images
    depth_map = DepthMap(showImages=True)

    # Compute and display disparity map using StereoBM
    depth_map.computeDepthMapBM()

    # Compute and display disparity map using StereoSGBM
    depth_map.computeDepthMapSGBM()