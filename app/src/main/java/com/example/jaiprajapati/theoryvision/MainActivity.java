package com.example.jaiprajapati.theoryvision;

import java.util.List;

import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.TextView;

import com.example.jaiprajapati.myfirstapp.R;

public class MainActivity extends Activity implements OnTouchListener, CvCameraViewListener2, RangeSeekBar.OnRangeSeekBarChangeListener {
    private static final String  TAG              = "OCVSample::Activity";

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private VisionPipeline mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private CameraBridgeViewBase mOpenCvCameraView;

    private ViewGroup sliderView;
    private ViewGroup presetView;
    private View option;
    private TextView avgX;
    private RangeSeekBar hSlider;
    private RangeSeekBar sSlider;
    private RangeSeekBar vSlider;
    private RangeSeekBar areaSlider;
    private boolean sliderShow = false;
    private Scalar upperLimit = new Scalar(0);
    private Scalar lowerLimit = new Scalar(0);

    public double contourFilterArea;
    private double valueThatDoesntMatter;


    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        sliderView = (ViewGroup) findViewById(R.id.sliders);
        option = (View) findViewById(R.id.options);
        presetView = (ViewGroup) findViewById(R.id.presets);
        hSlider = (RangeSeekBar) findViewById(R.id.hSlider);
        hSlider.setOnRangeSeekBarChangeListener(this);
        sSlider = (RangeSeekBar) findViewById(R.id.sSlider);
        sSlider.setOnRangeSeekBarChangeListener(this);
        vSlider = (RangeSeekBar) findViewById(R.id.vSlider);
        vSlider.setOnRangeSeekBarChangeListener(this);
        areaSlider = (RangeSeekBar) findViewById(R.id.areaSlider);
        areaSlider.setOnRangeSeekBarChangeListener(this);
        option.bringToFront();
/*
        avgX = (TextView) findViewById(R.id.avgX);
        avgX.setText(+mDetector.avg()+"avgX");*/
        /*TextView textView = (TextView) findViewById(R.id.avgX);
        textView.setText((""+ mDetector.avg()+""));*/



        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_main);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mOpenCvCameraView.setMaxFrameSize(640,480); //change this to any valid camera resolution 640,480 Works on OnePlusOne at 25FPS
        mOpenCvCameraView.setKeepScreenOn(true);
        mOpenCvCameraView.enableFpsMeter();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new VisionPipeline();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(0,0,255,255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
      /*  Mat mRgbaT = mRgba.t();
        Core.flip(mRgba.t(), mRgbaT, 1);
        Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());*/

        if (!sliderShow) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
            Log.e(TAG, "AvgX: " + mDetector.avg());
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);

            return mRgba;


        }
        else {
            Imgproc.cvtColor(mRgba, mDetector.mHsvMat, Imgproc.COLOR_BGR2HSV);
            Core.inRange(mDetector.mHsvMat, lowerLimit, upperLimit, mDetector.mMask);

            return mDetector.mMask;
        }


    }
    public void selfDestruct(View view){
        if(!sliderShow) {
            sliderView.bringToFront();
            sliderShow = true;
            presetView.bringToFront();
        }
        else if(sliderShow){
            mOpenCvCameraView.bringToFront();
            sliderShow = false;
        }
        option.bringToFront();
    }
    public void retroPreset(View view){
        lowerLimit.val[0] = 45;
        lowerLimit.val[1] = 115;
        lowerLimit.val[2] = 20;
        upperLimit.val[0] = 68;
        upperLimit.val[1] = 255;
        upperLimit.val[2] = 81;

        hSlider.setSelectedMinValue(lowerLimit.val[0]);
        sSlider.setSelectedMinValue(lowerLimit.val[1]);
        vSlider.setSelectedMinValue(lowerLimit.val[2]);
        hSlider.setSelectedMaxValue(upperLimit.val[0]);
        sSlider.setSelectedMaxValue(upperLimit.val[1]);
        vSlider.setSelectedMaxValue(upperLimit.val[2]);

        mDetector.setHSV(lowerLimit,upperLimit);
    }
    public void areaFilterPreset(View view){
        contourFilterArea = 4500;

        areaSlider.setSelectedMinValue(contourFilterArea);


        mDetector.setFilterArea(contourFilterArea);
    }
    @Override
    public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
        //Log.d("Barber"," "+ bar.getId());
        if(bar.getId() == hSlider.getId()) { //hbar id
            upperLimit.val[0] = bar.getSelectedMaxValue().doubleValue();
            lowerLimit.val[0] = bar.getSelectedMinValue().doubleValue();
        }
        if(bar.getId() == sSlider.getId()) { //sbar id
            upperLimit.val[1] = bar.getSelectedMaxValue().doubleValue();
            lowerLimit.val[1] = bar.getSelectedMinValue().doubleValue();
        }
        if(bar.getId() == vSlider.getId()) { //vbar id
            upperLimit.val[2] = bar.getSelectedMaxValue().doubleValue();
            lowerLimit.val[2] = bar.getSelectedMinValue().doubleValue();
        }

        if(bar.getId() == areaSlider.getId()) { //vbar id
            valueThatDoesntMatter = bar.getSelectedMaxValue().doubleValue();
            contourFilterArea = bar.getSelectedMinValue().doubleValue();
        }

        mDetector.setHSV(lowerLimit,upperLimit);
        mDetector.setFilterArea(contourFilterArea);
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
