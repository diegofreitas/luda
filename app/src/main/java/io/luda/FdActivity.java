package io.luda;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.provider.Settings;


import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import com.crashlytics.android.Crashlytics;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;


public class FdActivity extends AppCompatActivity implements CvCameraViewListener2 {


    private static final String TAG = "FdActivity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1;

    private Mat mRgba;
    private Mat mGray;

    private AsyncEventBus eventBus = new AsyncEventBus(Executors.newSingleThreadExecutor());

    private String[] mDetectorName;

    private LoadDNN loadDNN;

    public static Executor EXEC = Executors.newCachedThreadPool();



    protected CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.d(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    //System.loadLibrary("detection_based_tracker");

                    mOpenCvCameraView.enableView();
                    Log.d(TAG, "Enable camera view");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private long lastProcessedFrame;
    private FaceAnalysisOperation currentTask;
    private CvCameraViewFrame currentFrame;



    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }




    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        eventBus.register(this);
        this.loadDNN = new LoadDNN(this);
        mOpenCvCameraView = findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setMaxFrameSize(1080, 900);
        //mOpenCvCameraView.setAlpha(0);

        Log.d(TAG, "Camera configured");

    }



    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {

            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            Log.d(TAG, "Camara view disabled");
            if(currentTask != null && (currentTask.getStatus().equals(AsyncTask.Status.RUNNING) || currentTask.getStatus().equals(AsyncTask.Status.PENDING))) {
                currentTask.cancel(true);
                Log.d(TAG, "Canceling face detection task");
            }

        }

    }

    @Override
    public void onResume() {
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
        mOpenCvCameraView.disableView();
        Log.d(TAG, "Camera view disable");
        if(currentTask != null && (currentTask.getStatus().equals(AsyncTask.Status.RUNNING) || currentTask.getStatus().equals(AsyncTask.Status.PENDING))) {
            currentTask.cancel(true);
            Log.d(TAG, "Canceling face detection task");
        }
    }

    // Upload file to storage and return a path.
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        if( this.loadDNN.getStatus() != AsyncTask.Status.FINISHED)
            this.loadDNN.execute();

    }

    private static class LoadDNN extends AsyncTask<Void, Void, Void> {

        public Net netFace;
        public Net netAge;
        public Net netGender;
        private  Context context;

        public LoadDNN(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String proto = getPath("opencv_face_detector.pbtxt", context);
            String weights = getPath("opencv_face_detector_uint8.pb", context);
            this.netFace = Dnn.readNet(proto, weights);

            this.netAge = Dnn.readNetFromCaffe(getPath("age_deploy.prototxt", context),
                    getPath("age_net.caffemodel", context));

            this.netGender = Dnn.readNetFromCaffe(getPath("gender_deploy.prototxt", context),
                    getPath("gender_net.caffemodel", context));
            Log.i(TAG, "DNN Networks loaded successfully");
            return null;
        }

    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        Log.d(TAG, "Camera stopped");
    }

    public Mat onCameraFrame(final CvCameraViewFrame inputFrame) {
        currentFrame = inputFrame;
        if (System.currentTimeMillis() - lastProcessedFrame > 0 && loadDNN.getStatus().equals(AsyncTask.Status.FINISHED)) {
            Log.d(TAG, "Frame captured, initializing task");
            lastProcessedFrame = System.currentTimeMillis();
            currentTask = new FaceAnalysisOperation(this.loadDNN.netFace, this.loadDNN.netAge, this.loadDNN.netGender, eventBus);
            //currentTask.executeOnExecutor(EXEC,currentFrame);


            Mat outputFrame = currentTask.doInBackground(currentFrame).processedFrame;
            return outputFrame;
        }
        return null;

    }


    public static class Prediction {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Prediction that = (Prediction) o;
            return Objects.equals(age, that.age) &&
                    Objects.equals(gender, that.gender);
        }

        @Override
        public int hashCode() {
            return Objects.hash(age, gender);
        }

        public enum PredictionState {
            FACE_NOT_DETECTED,
            FACE_DETECTED
        }

        public PredictionState state = PredictionState.FACE_NOT_DETECTED;
        public String age = "";
        double ageConfidence = 0;
        public String gender = "";
        double genderConfidence = 0;
        public long timestamp = System.currentTimeMillis();
        public Mat processedFrame;

    }

    private static class FaceAnalysisOperation extends AsyncTask<CvCameraViewFrame, Void, Prediction> {

        private double[] MODEL_MEAN_VALUES = new double[]{78.4263377603, 87.7689143744, 114.895847746};
        private String[] ageList = new String[]{"(0-3)", "(4-7)", "(8-14)", "(15-24)", "(25-37)", "(38-47)", "(48-59)", "(60-100)"};
        private String[] genderList = new String[]{"M", "F"};

        public FaceAnalysisOperation(Net netFace, Net netAge, Net netGender,EventBus eventBus) {
            this.netFace = netFace;
            this.netAge = netAge;
            this.netGender = netGender;
            this.eventBus = eventBus;
        }

        public Net netFace;
        public Net netAge;
        public Net netGender;
        private EventBus eventBus;


        @Override
        protected Prediction doInBackground(CvCameraViewFrame... params) {
            final int IN_WIDTH = 300;
            final int IN_HEIGHT = 300;
            final float WH_RATIO = (float) IN_WIDTH / IN_HEIGHT;
            final double IN_SCALE_FACTOR = 1;
            final double MEAN_VAL = 127.5;
            final double THRESHOLD = 0.7;
            final Point ponitLabel = new Point(0, 0);

            Mat frame = new Mat(params[0].rgba().rows(), params[0].rgba().cols(), params[0].rgba().type());
            Imgproc.cvtColor(params[0].rgba(),frame , Imgproc.COLOR_BGR2RGB);
            try {



                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
                Mat blob = Dnn.blobFromImage(frame, IN_SCALE_FACTOR,
                        new Size(IN_WIDTH, IN_HEIGHT),
                        new Scalar(104, 117, 123), true, false);

                netFace.setInput(blob);

                Mat detections = netFace.forward();
                Log.d(TAG, "Faces detection");
                int cols = frame.cols();
                int rows = frame.rows();
                detections = detections.reshape(1, (int) detections.total() / 7);
                Mat blobCropFace;
                String age = "";
                double ageConfidence = 0;
                String gender = "";
                double genderConfidence = 0;
                Prediction result = new Prediction();
                for (int i = 0; i < detections.rows(); ++i) {
                    double confidence = detections.get(i, 2)[0];
                    if (confidence > THRESHOLD) {
                        result.state = Prediction.PredictionState.FACE_DETECTED;
                        int left = (int) (detections.get(i, 3)[0] * cols);
                        int top = (int) (detections.get(i, 4)[0] * rows);
                        int right = (int) (detections.get(i, 5)[0] * cols);
                        int bottom = (int) (detections.get(i, 6)[0] * rows);

                        // Draw rectangle around detected object.
                        Imgproc.rectangle(frame, new Point(left, top), new Point(right, bottom),
                                new Scalar(0, 255, 0));
                        //String label = classNames[classId] + ": " + confidence;


                        try {
                            blobCropFace = Dnn.blobFromImage(frame.submat(top, bottom, left, right), IN_SCALE_FACTOR,
                                    new Size(227, 227),
                                    new Scalar(MODEL_MEAN_VALUES), false);
                        } catch (CvException ecv) {
                            return result;
                        }

                        netAge.setInput(blobCropFace);
                        netGender.setInput(blobCropFace);
                        Mat predictions = netAge.forward();
                        Log.d(TAG, "Detecting Age..");
                        Mat predGender = netGender.forward();
                        Log.d(TAG, "Detecting Gender");

                        for (int maxI = 0; maxI < predictions.cols(); maxI++) {
                            if (predictions.get(0, maxI)[0] > ageConfidence && predictions.get(0, maxI)[0] > 0.7) {
                                ageConfidence = predictions.get(0, maxI)[0];
                                age = ageList[maxI];
                            }

                            if (maxI < predGender.cols() && predGender.get(0, maxI)[0] > genderConfidence && predGender.get(0, maxI)[0] > 0.8) {
                                genderConfidence = predGender.get(0, maxI)[0];
                                gender = genderList[maxI];
                            }
                        }

                        ponitLabel.x = left;
                        ponitLabel.y = top;
                        drawLabel(frame, String.format("GENDER: %s - AGE RANGE: %s", gender, age), left, (int) top);

                        result.age = age;
                        result.ageConfidence = ageConfidence;
                        result.gender = gender;
                        result.genderConfidence = genderConfidence;
                        result.timestamp = System.currentTimeMillis();
                        break;//Analisa somente uma face
                    }
                }
                result.processedFrame = frame;
                return result;
            } catch (CvException e) {
                Crashlytics.log(e.getMessage());
                Prediction result = new Prediction();
                result.processedFrame = frame;
                return result;
            }

        }

        @Override
        protected void onPostExecute(Prediction result) {
            eventBus.post(result);
            Log.d(TAG, "Face analysis completed");
            Log.d(TAG, String.format("State: %s - Idade: %s (%.2f) - Sexo: %s (%.2f)", result.state.toString(), result.age, result.ageConfidence, result.gender, result.genderConfidence));
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }




    private static void drawLabel(Mat frame, String age, int left, int top) {
        int[] baseLine = new int[1];
        Size labelSize = Imgproc.getTextSize(age, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine);
        // Draw background for label.
        Imgproc.rectangle(frame, new Point(left, top - labelSize.height),
                new Point(left + labelSize.width, top + baseLine[0]),
                new Scalar(255, 255, 255), Imgproc.FILLED);
        // Write class name and confidence.
        Imgproc.putText(frame, age, new Point(left, top),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 0));
    }



}
