package com.facedetection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;

import android.graphics.Matrix;
import android.graphics.Paint;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facedetection.model.FaceResult;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static com.facedetection.ImageUtils.getOutputMediaFileUri;


public class MainActivity extends AppCompatActivity {
    private ImageView myImageView;
    private Uri fileuri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions();
            }
        });
         myImageView = (ImageView) findViewById(R.id.imgview);
    }


    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED )
        {
            fileuri=ImageUtils.getOutputMediaFileUri(this);
            openCamera(fileuri);

        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},30);
            }
        }
    }


    private void openCamera(Uri fileuri) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileuri);
        startActivityForResult(intent, 30);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==30){
            if (grantResults.length>0 && grantResults[0]!=PackageManager.PERMISSION_GRANTED || grantResults[1]!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},30);
            }
            else {
                requestPermissions();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==30){
            if (resultCode==RESULT_OK){
                if (fileuri !=null){
                    try {
                        loadImage(fileuri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void loadImage(Uri fileuri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Bitmap myBitmap = null;
        myBitmap = ImageUtils.getThumbnail(fileuri, this);
        if (myBitmap != null) {
            detectFace(myBitmap);
        }
    }
    private void detectFace(Bitmap bitmap) {
        android.media.FaceDetector fdet_ = new android.media.FaceDetector(bitmap.getWidth(), bitmap.getHeight(),10);

        android.media.FaceDetector.Face[] fullResults = new android.media.FaceDetector.Face[10];
        fdet_.findFaces(bitmap, fullResults);
        ArrayList<FaceResult> faces_ = new ArrayList<>();

        /// Can use a Loop for more than one face

            if (fullResults[0] != null) {
                PointF mid = new PointF();
                fullResults[0].getMidPoint(mid);

                float eyesDis = fullResults[0].eyesDistance();
                float confidence = fullResults[0].confidence();
                float pose = fullResults[0].pose(android.media.FaceDetector.Face.EULER_Y);



                    FaceResult faceResult = new FaceResult();
                    faceResult.setFace(0, mid, eyesDis, confidence, pose, System.currentTimeMillis());
                    faces_.add(faceResult);

                    //
                    // Crop Face to display in RecylerView
                    //
                    Bitmap cropedFace = ImageUtils.cropFace(faceResult, bitmap, 0);
                    if (cropedFace != null) {
                        myImageView.setImageDrawable(new BitmapDrawable(getResources(),cropedFace));
                    }
            }

    }










}
