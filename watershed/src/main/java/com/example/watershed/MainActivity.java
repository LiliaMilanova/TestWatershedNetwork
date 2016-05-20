package com.example.watershed;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;

/**
 * Created by changliu on 5/16/16.
 */
public class MainActivity extends ActionBarActivity {

    protected static final String TAG = "Watershed";

    private static int RESULT_LOAD_IMAGE = 1;


    // Chang, fix no Mat_M() implementation issue
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    public Mat img = new Mat();
    public Mat result = new Mat();
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }

        Button buttonLoadImage = (Button) findViewById(R.id.buttonLoadPicture);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });


        Button buttonSendImage = (Button) findViewById(R.id.buttonSendPicture);
        buttonSendImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // send image to the cloud server
                Log.e(TAG, "send image to the cloud server");
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = (ImageView) findViewById(R.id.imgView);
            Bitmap bmp = BitmapFactory.decodeFile(picturePath);
            Log.i(TAG, picturePath);
            Mat img = Imgcodecs.imread(picturePath);

            //Utils.bitmapToMat(bmp, img);
            //Imgproc.cvtColor(img,result,Imgproc.COLOR_BGRA2BGR);

            result = steptowatershed(img);
            //Imgproc.cvtColor(result, img,Imgproc.COLOR_BGR2BGRA,4);
            Utils.matToBitmap(result, bmp, true);
            Log.i(TAG, "all okay");
            imageView.setImageBitmap(bmp);

        }


    }

    public Mat steptowatershed(Mat img) {
        Mat threeChannel = new Mat();

        Imgproc.cvtColor(img, threeChannel, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(threeChannel, threeChannel, 100, 255, Imgproc.THRESH_BINARY);

        Mat fg = new Mat(img.size(), CvType.CV_8U);
        Imgproc.erode(threeChannel, fg, new Mat());

        Mat bg = new Mat(img.size(), CvType.CV_8U);
        Imgproc.dilate(threeChannel, bg, new Mat());
        Imgproc.threshold(bg, bg, 1, 128, Imgproc.THRESH_BINARY_INV);

        Mat markers = new Mat(img.size(), CvType.CV_8U, new Scalar(0));
        Core.add(fg, bg, markers);
        Mat result1 = new Mat();
        WatershedSegmenter segmenter = new WatershedSegmenter();
        segmenter.setMarkers(markers);
        result1 = segmenter.process(img);
        return result1;
    }


    public void sendPostRequest(String imgFilePath) {

        try
        {
            HttpClient client = new DefaultHttpClient();
            String url = "http://129.63.16.66:8000/image/";
            HttpPost post = new HttpPost(url);

            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            String HeaderKey = "Content-type";
            String HeaderValue = "multipart/form-data";

            String ContentKey = "image";
            File file = new File(imgFilePath);

            entityBuilder.addTextBody(HeaderKey, HeaderValue);

            if(file != null)
            {
                entityBuilder.addBinaryBody(ContentKey, file);
            }

            HttpEntity entity = entityBuilder.build();
            post.setEntity(entity);
            HttpResponse response = client.execute(post);
            HttpEntity httpEntity = response.getEntity();
            String result = EntityUtils.toString(httpEntity);
            Log.v("result", result);
        } catch(Exception e)
        {
            e.printStackTrace();
        }


    }

}


