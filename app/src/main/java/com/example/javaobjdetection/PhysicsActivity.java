package com.example.javaobjdetection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.javaobjdetection.Fragments.ColourSelectionDialog;
import com.example.javaobjdetection.Fragments.GtSnackBar;
import com.example.javaobjdetection.Fragments.NullSnackBar;
import com.example.javaobjdetection.Helpers.DataContainer;

public class PhysicsActivity extends CameraActivity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG = "PhysicsActivity::Activity";

    private boolean              mIsColorSelected = false;
    private GtSnackBar           gtSnackBar;
    private NullSnackBar         nullSnackBar;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ObjectProcessor      mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;
    private Scalar               MARKER_COLOUR;
    private final int            GREEN = Color.parseColor("#4FBF26");
    private final int            RED = Color.parseColor("#CC0000");
    private final int            ORANGE = Color.parseColor("#FFA500");
    private ColourSelectionDialog colourSelectionDialog;
    private PhysicsProcessor     processor;
    private int                  frameNum;
    private final int            maxFrames = 200;
    private boolean              record;
    private boolean              clearOnNewData;
    private String[]             name;
    private TextView             tx;


    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(PhysicsActivity.this);
                    createButtonActions();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public PhysicsActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        record = false;
        clearOnNewData = false;
        setContentView(R.layout.color_blob_detection_surface_view);
        colourSelectionDialog =  new ColourSelectionDialog();

        Intent intent = getIntent();
        int objectSize = intent.getIntExtra("PROCESSOR_DATA", 1);

        gtSnackBar = new GtSnackBar(this);
        nullSnackBar = new NullSnackBar(this);
        processor = new PhysicsProcessor(objectSize);
        tx = findViewById(R.id.debug_text);

        frameNum = 0;
        name = new String[1];
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view_clickable);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
//        mDetector.setColorRadius(new Scalar(15, 100, 100, 0));
    }

    private void createButtonActions() {
        findViewById(R.id.action_replay).setScaleX(1.8f);
        findViewById(R.id.action_replay).setScaleY(1.8f);

        findViewById(R.id.start_rec_btn).setScaleX(1.8f);
        findViewById(R.id.start_rec_btn).setScaleY(1.8f);

        findViewById(R.id.start_rec_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record = true;
                clearData();
            }
        });

        findViewById(R.id.reset_rec_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record = false;
            }
        });

        findViewById(R.id.action_replay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (processor.data.size() > 10) {
                    Intent replayIntent = new Intent(PhysicsActivity.this, ReplayActivity.class);
                    processor.data.get(0).objectPhysicalSize = processor.objectSize;
                    processor.data.get(0).objectPixelSizes = processor.avgObjectHeight;
                    replayIntent.putExtra("PROCESSOR_DATA", processor.data);
                    PhysicsActivity.this.startActivity(replayIntent);
                } else {
                    gtSnackBar.dismiss();
                    nullSnackBar.show();
                }
            }
        });

        findViewById(R.id.action_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText input = new EditText(PhysicsActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);

                new AlertDialog.Builder(PhysicsActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Object size")
                        .setMessage("What's the diameter of your object?\nPlease format this in mm\nCurrent Size = " + processor.objectSize + "mm")
                        .setView(input)
                        .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                name[0] = input.getText().toString();
                                processor.objectSize = Integer.parseInt(name[0]);
                                Log.i("OBJ", "Size = " + processor.objectSize);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                input.requestFocusFromTouch();
            }
        });
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

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ObjectProcessor();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
        MARKER_COLOUR = new Scalar(255,125,125,255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        Log.i("ONTOUCH: ", String.valueOf(v.getId()));
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

        mBlobColorRgba = convertScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE, 0, 0, Imgproc.INTER_LINEAR_EXACT);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Mat clonedFrame = mRgba.clone();
        Point thisAvgPoint = null;

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
//            Log.i(TAG, "Contours count: " + contours.size());
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
//            Log.d("FRAME COUNT", "Contour Size: " + contours.size() + "   Frame Count: " + frameNum);
            if (contours.size() > 1) {
                nullSnackBar.dismiss();
                gtSnackBar.show();
                if (record) {
                    clonedFrame.release();
                    if (processor.data.size() < 30) {
                        clearOnNewData = true;
                        updateDebugInfo("Ready for Clear", ORANGE);
                    }
                }
            } else if (contours.size() == 0) {
                gtSnackBar.dismiss();
                nullSnackBar.show();
                if (record) {
                    clonedFrame.release();
                    if (processor.data.size() < 30) {
                        clearOnNewData = true;
                        updateDebugInfo("Ready for Clear", ORANGE);
                    }
                }
            } else {
                nullSnackBar.dismiss();
                gtSnackBar.dismiss();
                if (record) {
                    if (clearOnNewData) {
                        updateDebugInfo("Cleared Data", RED);
                        clearData();
                    } else {
                        updateDebugInfo("Gathering Data", GREEN);
                    }
                    thisAvgPoint = processor.recordDataFromMat(contours.get(0), clonedFrame, frameNum);
                    Imgproc.drawMarker(mRgba, new org.opencv.core.Point(thisAvgPoint.x, thisAvgPoint.y), MARKER_COLOUR, Imgproc.MARKER_SQUARE);
                    increaseFrame();
                }
            }
        }

        if (!record)
            clonedFrame.release();
        System.gc();
        return mRgba;
    }

    private void manualStart() {

    }

    private void manualStop() {

    }

    private void clearData() {
        clearOnNewData = false;
        frameNum = 0;
        if (processor.data.size() > 0)
            processor.ClearData();
    }

    private void increaseFrame() {
        frameNum += 1;
        Log.d(TAG, "increaseFrame: " + frameNum);
        if (frameNum >= maxFrames) {
            clearData();
        }
    }

    private void updateDebugInfo(String newText, int newColor) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tx.setText(newText + " | " + frameNum + "/" + maxFrames);
                tx.setTextColor(newColor);
            }
        });
    }

    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}