package com.example.javaobjdetection.Helpers;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataContainer implements Serializable {
    public int frameNum;
    public Mat frame;
    public Mat backupFrame;
    public int lastRecongnisedSize = 0;
    public Point avgPoint;
    public Point topPoint;
    public Point botPoint;
    public Point leftPoint;
    public Point rightPoint;
    public Point medianPoint;
    public ArrayList<Point> scenePoints;

    public String dump;

    public int objectPhysicalSize;
    public List<Integer> objectPixelSizes;

    public DataContainer(int frameNum, Mat frame, String dump) {
        this.frameNum = frameNum;
        this.frame = frame;
        this.dump = dump;
    }
    public DataContainer(int frameNum, Mat frame, Point avgPoint, Point topPoint, Point botPoint, Point leftPoint, Point rightPoint, Point medianPoint) {
        this.frameNum = frameNum;
        this.frame = frame;
        this.avgPoint = avgPoint;
        this.topPoint = topPoint;
        this.botPoint = botPoint;
        this.leftPoint = leftPoint;
        this.rightPoint = rightPoint;
        this.medianPoint = medianPoint;
    }

    public DataContainer(Point avgPoint, Point topPoint, Point botPoint, Point leftPoint, Point rightPoint, Point medianPoint, List<Point> previousScenePoints) {
        this.avgPoint = avgPoint;
        this.topPoint = topPoint;
        this.botPoint = botPoint;
        this.leftPoint = leftPoint;
        this.rightPoint = rightPoint;
        this.medianPoint = medianPoint;
        this.scenePoints = new ArrayList<>(previousScenePoints);
        this.scenePoints.add(this.topPoint);
    }
}
