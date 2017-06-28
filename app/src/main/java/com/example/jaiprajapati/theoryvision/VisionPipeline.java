package com.example.jaiprajapati.theoryvision;


import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


         import org.opencv.core.Core;
         import org.opencv.core.CvType;
         import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
         import org.opencv.imgproc.Imgproc;


         public class VisionPipeline {
     // Lower and Upper bounds for range checking in HSV color space
             private Scalar mLowerBound = new Scalar(0);
     private Scalar mUpperBound = new Scalar(0);
     // Minimum contour area in percent for contours filtering
             private static double mMinContourArea = 0.1;
     // Color radius for range checking in HSV color space
             private Scalar mColorRadius = new Scalar(25,50,50,0);
     private Mat mSpectrum = new Mat();
     private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
             private ArrayList<MatOfPoint> mFilterContours = new ArrayList<MatOfPoint>();

             double filterContourArea;
             double filterContoursMinArea;

             double filterContoursMinPerimeter = 0;
             double filterContoursMinWidth = 0;
             double filterContoursMaxWidth = 1000;
             double filterContoursMinHeight = 0;
             double filterContoursMaxHeight = 1000;
             double[] filterContoursSolidity = {0, 100};
             double filterContoursMaxVertices = 1000000;
             double filterContoursMinVertices = 0;
             double filterContoursMinRatio = 0;
             double filterContoursMaxRatio = 1000;


             // Cache
             Mat mPyrDownMat = new Mat();
     Mat mHsvMat = new Mat();
     Mat mMask = new Mat();
     Mat mDilatedMask = new Mat();
     Mat mHierarchy = new Mat();


             double iLowH = 40;
             double iHighH = 180;
             double iLowS = 0;
             double iHighS = 255;
             double iLowV = 0;
             double iHighV = 255;
             Scalar sc1 = new Scalar(iLowH, iLowS, iLowV);
             Scalar sc2= new Scalar(iHighH, iHighS, iHighV);

             private Scalar upperLimit = new Scalar(0);
             private Scalar lowerLimit = new Scalar(0);

             public static double[] hsvThresholdHue = new double[2];
             public static double[] hsvThresholdSat = new double[2];
             public static double[] hsvThresholdVal = new double[2];

             double avgX;


             public void setupHSV(){
                 lowerLimit.val[0] = 0;
                 lowerLimit.val[1] = 0;
                 lowerLimit.val[2] = 0;

                 upperLimit.val[0] = 0;
                 upperLimit.val[1] = 0;
                 upperLimit.val[2] = 0;
             }
             public void updateHSV(){

                 hsvThresholdHue[0] = lowerLimit.val[0];
                 hsvThresholdHue[1] = upperLimit.val[0];

                 hsvThresholdSat[0] = lowerLimit.val[1];
                 hsvThresholdSat[1] = upperLimit.val[1];

                 hsvThresholdVal[0] = lowerLimit.val[2];
                 hsvThresholdVal[1] = upperLimit.val[2];
             }
             public void setHSV(Scalar lowerT, Scalar upperT) {
                 for (int i = 0; i < 3; i++) {
                     upperLimit.val[i] = upperT.val[i];
                     lowerLimit.val[i] = lowerT.val[i];
                 }
             }
             public void setFilterArea(double contourFilterArea) {
                 filterContoursMinArea = contourFilterArea;
                 }


             public double hMin(){
                 return lowerLimit.val[0];
             }
             public double hMax(){
                 return upperLimit.val[0];
             }
             public double sMin(){
                 return lowerLimit.val[1];
             }
             public double sMax(){
                 return upperLimit.val[1];
             }
             public double vMin(){
                 return lowerLimit.val[2];
             }
             public double vMax(){
                 return upperLimit.val[2];
             }


             public void setColorRadius(Scalar radius) {
                 mColorRadius = radius;
             }


             public void setHsvColor(Scalar hsvColor) {
                 double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]-mColorRadius.val[0] : 0;
                 double maxH = (hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255;


                 mLowerBound.val[0] = minH;
                 mUpperBound.val[0] = maxH;


                 mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
                 mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];


                 mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
                 mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];


                 mLowerBound.val[3] = 0;
                 mUpperBound.val[3] = 255;


                 Mat spectrumHsv = new Mat(1, (int)(maxH-minH), CvType.CV_8UC3);


                 for (int j = 0; j < maxH-minH; j++) {
                         byte[] tmp = {(byte)(minH+j), (byte)255, (byte)255};
                         spectrumHsv.put(0, j, tmp);
                     }


                 Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);
             }


             public Mat getSpectrum() {
                 return mSpectrum;
             }


             public void setMinContourArea(double area) {
                 mMinContourArea = area;
             }

             public void hsvProcess(Mat rgbaImage){
               /*  Imgproc.pyrDown(rgbaImage, mPyrDownMat);
                 Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);


                 Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);


                 Core.inRange(mHsvMat, lowerLimit, upperLimit, mMask);*/

                 Imgproc.cvtColor(rgbaImage, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);
                 Core.inRange(mHsvMat, lowerLimit, upperLimit, mMask);
             }
             ////////Need to add filterContours method
             public void process(Mat rgbaImage) {
                 Imgproc.pyrDown(rgbaImage, mPyrDownMat);
                 Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);


                 Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_BGR2HSV);


                 Core.inRange(mHsvMat, lowerLimit, upperLimit, mMask);
                 Imgproc.dilate(mMask, mDilatedMask, new Mat());


                 List<MatOfPoint> contours = new ArrayList<MatOfPoint>();


                 Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


                 // Find max contour area
                 double maxArea = 0;
                 Iterator<MatOfPoint> each = contours.iterator();
                 while (each.hasNext()) {
                         MatOfPoint wrapper = each.next();
                         double area = Imgproc.contourArea(wrapper);
                         if (area > maxArea)
                                 maxArea = area;
                     }


                 // Filter contours by area and resize to fit the original image size
                 mContours.clear();
                 each = contours.iterator();
                 while (each.hasNext()) {
                         MatOfPoint contour = each.next();
                         if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                                 Core.multiply(contour, new Scalar(4,4), contour);
                                 mContours.add(contour);
///////////////////Contours need to be filitered before sent to boundRectMethod

///////////////////////////////////////
                             List<MatOfPoint>  filterContoursContours = mContours;
                             filterContours(mContours, filterContoursMinArea, filterContoursMinPerimeter, filterContoursMinWidth, filterContoursMaxWidth, filterContoursMinHeight, filterContoursMaxHeight, filterContoursSolidity, filterContoursMaxVertices, filterContoursMinVertices, filterContoursMinRatio, filterContoursMaxRatio, mFilterContours);


                             if (mFilterContours.size() > 0) {
                                 Rect r2 = Imgproc.boundingRect(getContours().get(0));
                                 Imgproc.rectangle(rgbaImage, new Point(r2.x, r2.y), new Point(r2.x + r2.width, r2.y + r2.height),
                                         new Scalar(255, 0, 0), 6);

                                 avgX += r2.width + r2.x; // Adds the width and X
                                 // cordinate of the second
                                 // rectangle
                                 avgX = avgX / 2; // Divides the Average X cordinate by
                                 // two for half of the image center
                             }
                             if (mFilterContours.size() > 1) {
                                 Rect r = Imgproc.boundingRect(getContours().get(1));
                                 Imgproc.rectangle(rgbaImage, new Point(r.x, r.y), new Point(r.x + r.width, r.y + r.height),
                                         new Scalar(255, 0, 0), 6);
                                 avgX = r.x;
                             }



                             }
                     }
             }
             public double avg() {
                 return avgX;
             }

             private void filterContours(List<MatOfPoint> inputContours, double minArea,
                                         double minPerimeter, double minWidth, double maxWidth, double minHeight, double
                                                 maxHeight, double[] solidity, double maxVertexCount, double minVertexCount, double
                                                 minRatio, double maxRatio, List<MatOfPoint> output) {
                 final MatOfInt hull = new MatOfInt();
                 output.clear();
                 //operation
                 for (int i = 0; i < inputContours.size(); i++) {
                     final MatOfPoint contour = inputContours.get(i);
                     final Rect bb = Imgproc.boundingRect(contour);
                     if (bb.width < minWidth || bb.width > maxWidth) continue;
                     if (bb.height < minHeight || bb.height > maxHeight) continue;
                     final double area = Imgproc.contourArea(contour);
                     if (area < minArea) continue;
                     if (Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true) < minPerimeter) continue;
                     Imgproc.convexHull(contour, hull);
                     MatOfPoint mopHull = new MatOfPoint();
                     mopHull.create((int) hull.size().height, 1, CvType.CV_32SC2);
                     for (int j = 0; j < hull.size().height; j++) {
                         int index = (int)hull.get(j, 0)[0];
                         double[] point = new double[] { contour.get(index, 0)[0], contour.get(index, 0)[1]};
                         mopHull.put(j, 0, point);
                     }
                     final double solid = 100 * area / Imgproc.contourArea(mopHull);
                     if (solid < solidity[0] || solid > solidity[1]) continue;
                     if (contour.rows() < minVertexCount || contour.rows() > maxVertexCount)	continue;
                     final double ratio = bb.width / (double)bb.height;
                     if (ratio < minRatio || ratio > maxRatio) continue;
                     output.add(contour);
                 }
             }


             public List<MatOfPoint> getContours() {
                 return mContours;
             }


 }
