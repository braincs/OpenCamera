# OpenCamera

<img src="README-images/image-20200422180444527.png" alt="image-20200422180444527" />

[English：README](./README.md)

## 简介

设计易用的Camera 模块，加速模组相关的应用开发

API基于：

- Android Camera API :arrow_right:
  - 支持后置摄像头
  - 支持前置摄像头

- Android Camera2 API
    - 支持前、后摄像头

- UVCCamera API :arrow_right:
  - 支持UVC摄像头（可选cameraId 1，2）
  - 支持点击选择开启两路UVC Camera 
  - 支持无感同时开启两路UVC Camera 



## 依赖库

UVCCamera: https://github.com/saki4510t/UVCCamera

usbCameraCommon: https://github.com/braincs/USBCamera

- 在FrameCallback做了少量优化

