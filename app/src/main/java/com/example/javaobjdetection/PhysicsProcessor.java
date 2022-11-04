package com.example.javaobjdetection;

import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.javaobjdetection.Helpers.DataContainer;
import com.example.javaobjdetection.Helpers.EnergyLoss;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.Objdetect;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

enum Movement {
    UP, DOWN, NONE
}

public class PhysicsProcessor implements Serializable {
    public ArrayList<DataContainer> data;
    public List<Point> avgPoints;
    public List<Point> topPoints;
    public List<Point> botPoints;

    public List<String> rawData;

    private int lastFrameNum = -1;
    public int objectSize;
    public List<Integer> avgObjectHeight;

    public PhysicsProcessor() {
        avgPoints = new ArrayList<>();
        topPoints = new ArrayList<>();
        botPoints = new ArrayList<>();

        data = new ArrayList<>();
        rawData = new ArrayList<>();
        objectSize = 1;
        avgObjectHeight = new ArrayList<>();
    }

    public PhysicsProcessor(int objectSize) {
        avgPoints = new ArrayList<>();
        topPoints = new ArrayList<>();
        botPoints = new ArrayList<>();

        data = new ArrayList<>();
        rawData = new ArrayList<>();
        this.objectSize = objectSize;
        avgObjectHeight = new ArrayList<>();
    }

//    public Mat detectObject(List<Point> prevList, int size, @Nullable CameraBridgeViewBase.CvCameraViewFrame inputFrame, Size frameSize) {
//        if (inputFrame == null) {
//            return new Mat();
//        }
//        //Imgproc.resize(inputFrame.rgba(), inputFrame.rgba(), Size())
//        var outputFrame = inputFrame.rgba()
//        Imgproc.GaussianBlur(outputFrame, outputFrame, Size(11.0, 11.0), 0.0)
//        Imgproc.cvtColor(outputFrame, outputFrame, Imgproc.COLOR_BGR2HSV)
//        val lowerGreen = Scalar(29.0, 86.0, 6.0)
//        val upperGreen = Scalar(64.0, 255.0, 255.0)
//        Core.inRange(outputFrame, lowerGreen, upperGreen, outputFrame)
//        Imgproc.erode(outputFrame, outputFrame, Imgproc.getStructuringElement(1, frameSize))
//        Imgproc.dilate(outputFrame, outputFrame,  Imgproc.getStructuringElement(1, frameSize))
//
//        var contours: List<MatOfPoint> = ArrayList()
//        var hierarchy = Mat()
//        Imgproc.findContours(outputFrame, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
//
//        prevList.add(Point(0, 0))
//
//        return outputFrame
//    }

    public void ClearData() {
        for (DataContainer item : data) {
            item.frame.release();
            if (item.backupFrame != null)
                item.backupFrame.release();
        }
        data = new ArrayList<>();
    }

    public List<Point> getData() {
        return avgPoints;
    }

    public Point recordDataFromMat(Mat contour, Mat frame, int frameNum) {
        if (lastFrameNum > frameNum)
            data.clear();
        DataContainer newContainer = new DataContainer(frameNum, frame, contour.dump());
//        DataContainer thisData = recordRawData(contour.dump());
        lastFrameNum = frameNum;
        data.add(newContainer);
//        avgObjectHeight.add((int) Math.abs(thisData.topPoint.y - thisData.botPoint.y));
//        Log.i("OBJ PXL:", "" + Math.abs(thisData.leftPoint.x - thisData.rightPoint.x));
        return new Point(0, 0);
    }

    public DataContainer recordRawData(String dump, int frameNum) {
        return extractRawData(dump, frameNum);
    }

    public ArrayList<DataContainer> extractAllRawData() {
        for (int i = 0; i < data.size(); i++) {
            DataContainer oldData = data.get(i);
            DataContainer thisData = recordRawData(oldData.dump, i);
            oldData.botPoint = thisData.botPoint;
            oldData.topPoint = thisData.topPoint;
            oldData.rightPoint = thisData.rightPoint;
            oldData.leftPoint = thisData.leftPoint;
            oldData.avgPoint = thisData.avgPoint;
            oldData.medianPoint = thisData.medianPoint;
            oldData.scenePoints = new ArrayList<>(thisData.scenePoints);

            avgObjectHeight.add((int) Math.abs(thisData.topPoint.y - thisData.botPoint.y));
            Log.i("OBJ PXL:", "" + Math.abs(thisData.leftPoint.x - thisData.rightPoint.x));
        }
        return data;
    }

    public DataContainer extractRawData(String dump, int frameNum) {
        List<Point> points = convertStrToIntArray(dump);
        Point avgPoint = new Point(0, 0);
        Point topPoint = new Point(0, 0);
        Point botPoint = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
        Point leftPoint = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
        Point rightPoint = new Point(0, 0);
        Point medianPoint = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
        for (Point point: points) {
            if (point.y < botPoint.y)
                botPoint = point;
            else if (point.y > topPoint.y)
                topPoint = point;
            avgPoint.x += point.x;
            avgPoint.y += point.y;
            if (point.x < leftPoint.x)
                leftPoint = point;
            if (point.x > rightPoint.x)
                rightPoint = point;
        }
        medianPoint.x = (int) ((leftPoint.x + rightPoint.x) / 2);
        medianPoint.y = (int) ((topPoint.y + botPoint.y) / 2);
        avgPoint.x /= points.size();
        avgPoint.y /= points.size();
        botPoint.x = medianPoint.x;
        topPoint.x = medianPoint.x;
        ArrayList<Point> lastPoints;
        if (frameNum == 0)
            lastPoints = new ArrayList<>();
        else
            lastPoints = data.get(frameNum - 1).scenePoints;
        return new DataContainer(avgPoint, topPoint, botPoint, leftPoint, rightPoint, medianPoint, lastPoints);
    }

    public List<Point> convertStrToIntArray(String array) {
        // Convert to array of ["x, y", "x, y", ...]
        String[] strPointList = array.split(";\n ");

        // Remove [ from first point
        strPointList[0] = strPointList[0].substring(1);

        // Remove ] from last point
        String lastPoint = strPointList[strPointList.length - 1];
        strPointList[strPointList.length - 1] = lastPoint.substring(0, lastPoint.length() - 1);

        // Convert to nested array
        List<Point> pointList = new ArrayList<>();

        for (int i = 0; i < strPointList.length; i++) {
            // Convert each point in the string to an actual point
            String[] pointDataStr = strPointList[i].split(", ");
            Point point = new Point(Integer.parseInt(pointDataStr[0]), Integer.parseInt(pointDataStr[1]));
            pointList.add(point);
        }
        return pointList;
    }

    public void recordData(Point avgPoint, Point topPoint, Point botPoint) {
        avgPoints.add(avgPoint);
        topPoints.add(topPoint);
        botPoints.add(botPoint);
    }

    public List<Point> getVerticalDistance(List<Point> capturedData) {
        if (capturedData.size() <= 1) {
            return capturedData;
        }
        return getInflectionPoints(capturedData);
    }
    public List<Point> getInflectionPoints(List<Point> data) {
        List<Point> inflections = new ArrayList<>();
        Point lastChange = new Point(data.get(0).x,data.get(0).y);
        Point checkPoint = new Point(data.get(0).x,data.get(0).y);
        int checkCount = 0;
        Point lastMax = new Point(data.get(0).x,data.get(0).y);
        boolean addedMax = true;
        boolean addedMin = false;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).y > lastChange.y) {
                if (!addedMin) {
                    if (checkCount == 0) {
                        checkPoint = lastChange;
                    }
                    checkCount++;
                    if (checkCount > 0) {
                        addedMin = true;
                        inflections.add(checkPoint);
                        addedMax = false;
                        checkCount = 0;
                    }
                }
            } else if (data.get(i).y < lastChange.y) {
                if (!addedMax) {
                    if (checkCount == 0)
                        checkPoint = lastChange;
                    checkCount++;
                    if (checkCount > 0) {
                        addedMax = true;
                        inflections.add(checkPoint);
                        addedMin = false;
                        checkCount = 0;
                    }
                }
            }
            lastChange = data.get(i);
        }
        return inflections;
    }

    public EnergyLoss energyLoss(Point a, Point b) {
        EnergyLoss loss = new EnergyLoss();
        loss.firstMax = new Point(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
        return loss;
    }

    public  Movement getDir(int dir) {
        if (dir < 0)
            return Movement.DOWN;
        else if (dir > 0)
            return Movement.UP;
        return Movement.NONE;
    }
}