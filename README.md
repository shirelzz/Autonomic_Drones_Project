# Vision-Based Automatic Landing of Drones

This repository includes an Android app for performing line detection landing of DJI drones.
The app was tested on Mavic Air 2.

This project is designed to control a drone for precise landing using the DJI SDK. 
The controller utilizes advanced line detection and image processing techniques to ensure the drone can land accurately based on visual feedback from its camera.

## Project Overview
The core logic of the landing controller can be found in the following directory:
```
 app/src/main/java/com/dji/sdk/sample/demo/accurateLandingController
```


## Getting Started

To download and run this project on your local machine, follow the steps below:

### Prerequisites

Ensure that the following are installed on your system:
- **Android Studio** (for development)
- **Java Development Kit (JDK)** (version 8 or above)
- **DJI SDK** (make sure you follow DJI's setup instructions for Android development)

### Clone the Repository

To download the project from Git, follow these steps:

1. Open a terminal and navigate to the directory where you want to clone the project.
2. Run the following command to clone the repository:
    ```bash
      git clone https://github.com/shirelzz/Autonomic_Drones_Project.git
    ```

3. Navigate into the project directory:
    ```bash
      cd Autonomic_Drones_Project
    ```

### Open the Project in Android Studio
1. Launch Android Studio.
2. Click on File > Open and select the cloned project directory.
3. Let the project sync with Gradle and ensure that all dependencies are resolved.

### Running the App
1. Connect your DJI drone to your Android device.
2. In Android Studio, select your connected device and click Run.
3. The app will be deployed to your device, and you can start testing the drone's accurate landing capabilities.
