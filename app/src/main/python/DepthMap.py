import cv2 as cv
import numpy as np
import base64

def computeDepthMapSGBM(imgLeft_bytes, imgRight_bytes):
    # Convert bytes to numpy arrays
    imgLeft_np = np.frombuffer(imgLeft_bytes, np.uint8)
    imgRight_np = np.frombuffer(imgRight_bytes, np.uint8)

    # Decode numpy arrays to images
    imgLeft = cv.imdecode(imgLeft_np, cv.IMREAD_GRAYSCALE)
    imgRight = cv.imdecode(imgRight_np, cv.IMREAD_GRAYSCALE)

    window_size = 7
    min_disp = 16
    nDispFactor = 14
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

    # Convert disparity map to a format suitable for Java (e.g., bytes or base64)
    _, img_encoded = cv.imencode('.png', disparity)
    img_bytes = img_encoded.tobytes()

    # Return bytes encoded as Base64 string
    img_bytes_base64 = base64.b64encode(img_bytes).decode('utf-8')
    return img_bytes_base64

    return img_bytes
