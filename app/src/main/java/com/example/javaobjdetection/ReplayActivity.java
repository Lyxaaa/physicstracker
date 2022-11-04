package com.example.javaobjdetection;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.javaobjdetection.Fragments.ColourSelectionDialog;
import com.example.javaobjdetection.Fragments.GtSnackBar;
import com.example.javaobjdetection.Fragments.NullSnackBar;
import com.example.javaobjdetection.Helpers.DataContainer;
import com.example.javaobjdetection.Helpers.EnergyLoss;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgcodecs.Imgcodecs.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ReplayActivity extends AppCompatActivity {
    private static final String  TAG              = "ReplayActivity::Activity";

    private boolean              mIsColorSelected = false;
    private GtSnackBar           gtSnackBar;
    private NullSnackBar         nullSnackBar;
    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ObjectProcessor      mDetector;
    private Mat                  mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;
    private Scalar               MARKER_COLOUR;
    private Scalar               ARROW_COLOUR;
    private Scalar               TEXT_COLOUR;

    private ColourSelectionDialog colourSelectionDialog;
    private PhysicsProcessor     processor;
    private int                  frameNum;
    private SeekBar              seekBar;
    private int                  objectSize;
    private double                  pixelToMmRatio;
    private int[]                displayingFrame;
    private ArrowState          arrowState;
    private InfoState           infoState;

    enum ArrowState {
        VERTICAL,
        HORIZONTAL,
        BOTH,
        NONE
    }

    enum InfoState {
        PERCENT,
        DECIMAL,
        BOTH,
        NONE
    }

    public ReplayActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        Intent intent = getIntent();
        this.processor = new PhysicsProcessor();
        this.processor.data = (ArrayList<DataContainer>) intent.getSerializableExtra("PROCESSOR_DATA");
        this.processor.extractAllRawData();
        this.processor.objectSize = processor.data.get(0).objectPhysicalSize;
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        displayingFrame = new int[1];

        setContentView(R.layout.physics_image_view);
        colourSelectionDialog =  new ColourSelectionDialog();
        CONTOUR_COLOR = new Scalar(255,125,125,255);
        MARKER_COLOUR = new Scalar(125, 255, 255, 255);
        ARROW_COLOUR = new Scalar(125, 125, 255, 255);
        TEXT_COLOUR = new Scalar(125,255,125,255);

        frameNum = 0;
        displayingFrame[0] = 0;
        infoState = InfoState.DECIMAL;
        arrowState = ArrowState.HORIZONTAL;
        physicalObjectSize();
        displayFrame(processor.data.size() - 1);
        createButtonActions();

//        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
//        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
//        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    private void physicalObjectSize() {
        int sum = 0;
        for (int i = 0; i < processor.avgObjectHeight.size(); i++)
            sum += processor.avgObjectHeight.get(i);
        sum /= processor.avgObjectHeight.size();
        pixelToMmRatio = ((float) sum) / ((float) processor.objectSize);
        Log.i("OBJECT SIZE CALC:", "Obj mm:" + processor.objectSize + ", Avg pxls:" + sum + ", pxl-mm:" + pixelToMmRatio);
    }

    private int calculatePhysicalDistance(double a, double b) {
        Log.i("CALC DIST",Math.abs(a - b) + " / " + pixelToMmRatio);
        return (int) (Math.abs(b - a) / pixelToMmRatio);
    }

    private void createButtonActions() {
        seekBar = (SeekBar)findViewById(R.id.replay_seek_bar);
        seekBar.setMax(processor.data.size() - 1);
        seekBar.setProgress(seekBar.getMax());
        // perform seek bar change listener event used for getting the progress value
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                displayingFrame[0] = progress;
                displayFrame(progress);
                Log.i("SEEKBAR", "Progressed: " + progress);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        findViewById(R.id.go_back_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent physicsIntent = new Intent(ReplayActivity.this, PhysicsActivity.class);
                physicsIntent.putExtra("PROCESSOR_DATA", processor.objectSize);
                ReplayActivity.this.startActivity(physicsIntent);
            }
        });

        findViewById(R.id.action_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText input = new EditText(ReplayActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);

                new AlertDialog.Builder(ReplayActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Object size")
                        .setMessage("What's the diameter of your object?\nPlease format this in mm\nCurrent Size = " + processor.objectSize + "mm")
                        .setView(input)
                        .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int size = Integer.parseInt(input.getText().toString());
                                if (size > 0) {
                                    processor.objectSize = Integer.parseInt(input.getText().toString());
                                    physicalObjectSize();
                                    Log.i("OBJ", "Size = " + processor.objectSize);
                                    displayFrame(displayingFrame[0]);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                input.requestFocusFromTouch();
            }
        });

        findViewById(R.id.action_percentage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(infoState) {
                    case BOTH:
                        infoState = InfoState.NONE;
                        break;
                    case NONE:
                        infoState = InfoState.DECIMAL;
                        break;
                    case DECIMAL:
                        infoState = InfoState.PERCENT;
                        break;
                    case PERCENT:
                        infoState = InfoState.BOTH;
                        break;
                }
                displayFrame(displayingFrame[0]);
            }
        });

        findViewById(R.id.action_arrows).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(arrowState) {
                    case BOTH:
                        arrowState = ArrowState.NONE;
                        break;
                    case NONE:
                        arrowState = ArrowState.HORIZONTAL;
                        break;
                    case HORIZONTAL:
                        arrowState = ArrowState.VERTICAL;
                        break;
                    case VERTICAL:
                        arrowState = ArrowState.BOTH;
                        break;
                }
                displayFrame(displayingFrame[0]);
            }
        });
    }

    @Override
    protected void onDestroy() {
        processor.ClearData();
        Log.e("ONDESTROY: ", "" + processor.data.size());
        super.onDestroy();
    }

    public void startReplay() {
    }

    public void setHighestPointData() {
        for (int i = 1; i < processor.data.size(); i++) {

        }
    }

    public Bitmap matToImg(Mat m) {
        Bitmap img = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m, img);
        return img;
    }

    public void addTrackingLine(List<Point> points, Mat frame) {
        ArrayList<MatOfPoint> matPoints = new ArrayList<>();
        MatOfPoint matPoint = new MatOfPoint();

        List<Point> rev = new ArrayList<>(points);
        Collections.reverse(rev);
        rev.addAll(points);
        matPoint.fromList(rev);
        matPoints.add(matPoint);

        Imgproc.drawContours(frame, matPoints, -1, CONTOUR_COLOR, 10);
    }

    public void addLabel(DataContainer data, Mat frame) {
        List<Point> inflections = processor.getInflectionPoints(data.scenePoints);
        Point lastPoint = null;
        Point lastMin = null;
        Point lastMax = null;
        boolean isMin = true;
        boolean skipFirst = true;

        for (Point marker : inflections) {
            if (skipFirst) {
                skipFirst = false;
                continue;
            }
            Imgproc.drawMarker(frame, marker, MARKER_COLOUR, Imgproc.MARKER_SQUARE, 8, 5);

            if (lastMin != null && !isMin) {
                String labelText = displayDistance(calculatePhysicalDistance(marker.y, lastMin.y));

                if (arrowState == ArrowState.VERTICAL || arrowState == ArrowState.BOTH)
                    Imgproc.line(frame, marker, new Point(marker.x, lastMin.y), ARROW_COLOUR, 5, Imgproc.LINE_4);

                if (infoState == InfoState.DECIMAL || infoState == InfoState.BOTH)
                    Imgproc.putText(frame, labelText, new Point(marker.x, Math.abs(lastMin.y + marker.y) / 2), 0, 2, TEXT_COLOUR, 4);
            }
            if (lastMin != null && lastMax != null && !isMin) {
                double relativeHeightA = lastMax.y - lastMin.y;
                double relativeHeightB = marker.y - lastMin.y;
                double energyLost = ((relativeHeightA - relativeHeightB) / relativeHeightA) * 100;
                String labelText = String.format(Locale.UK, "%d%%", (int) energyLost*-1);
                if (arrowState == ArrowState.HORIZONTAL || arrowState == ArrowState.BOTH)
                    Imgproc.arrowedLine(frame, lastMax, marker, ARROW_COLOUR, 5, Imgproc.LINE_4);

                if (infoState == InfoState.PERCENT || infoState == InfoState.BOTH)
                    Imgproc.putText(frame, labelText, new Point(((lastMax.x + marker.x) / 2) - 50 ,lastMax.y - 50), 0, 2, TEXT_COLOUR, 4);


            }

            // reassign lastMin and max
            if (isMin) {
                lastMin = marker;
                isMin = false;
            } else {
                lastMax = marker;
                isMin = true;
            }

            lastPoint = marker;
        }
    }

    /*
    Takes a distance in mm and converts it to the most appropriate SI unit
     */
    public String displayDistance(int distance) {
        int dist = (int) Math.floor(Math.log10(distance));
        if (distance == 0) {
            dist = 0;
        }
        switch (dist) {
            case 0:
                return distance + "mm";
            case 1:
            case 2:
                return distance/10 + "cm";
            case 3:
                return distance/100 + "m";
        }
        return distance/100 + "m";
    }

    public void displayFrame(int frameNum) {
        displayingFrame[0] = frameNum;
        DataContainer data = processor.data.get(frameNum);
//        Mat mat = new Mat(data.frame.width(), data.frame.height(), data.frame.type());
//        mat.setTo(data.frame);
        if (data.backupFrame == null)
            data.backupFrame = data.frame.clone();
//        if (data.lastRecongnisedSize != processor.objectSize)
//            redraw = true;

        data.frame.release();
        data.frame = data.backupFrame.clone();
        Mat frame = data.frame;

        data.lastRecongnisedSize = processor.objectSize;
        addTrackingLine(data.scenePoints, frame);
        addLabel(data, frame);

        Bitmap img = matToImg(frame);
        ImageView imageView = findViewById(R.id.physics_image_element);
        imageView.setImageBitmap(img);
    }

}
