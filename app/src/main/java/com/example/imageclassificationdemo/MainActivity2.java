package com.example.imageclassificationdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.exifinterface.media.ExifInterface;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainActivity2 extends AppCompatActivity implements View.OnClickListener {

    protected Interpreter tflite;
    private MappedByteBuffer tfliteModel;
    private TensorImage inputImageBuffer;
    private  int imageSizeX=224;
    private  int imageSizeY=224;
    private TensorBuffer outputProbabilityBuffer;
    private TensorProcessor probabilityProcessor;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f;
    private Bitmap bitmap;
    private List<String> labels;
    ImageView imageView;
    Uri imageuri;
    ImageView buclassify;
    ImageView retake;
    TextView classitext;
    ArrayList<RecipeModel> recipeModel= new ArrayList<>();
    Boolean remake=true;



    public static final String TAG = MainActivity.class.getSimpleName();
    private Camera mCamera;
    private HorizontalScrollView horizontalScrollView;
    private int PERMISSION_CALLBACK_CONSTANT = 1000;
    ImageView ivCapture2;
    ImageView ivCapture;
    FrameLayout frameLayout;

    ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        frameLayout=findViewById(R.id.rlCameraPreview);
        ivCapture2 = (ImageView) findViewById(R.id.ivCapture2);
        ivCapture = (ImageView) findViewById(R.id.ivCapture);
        horizontalScrollView = (HorizontalScrollView) findViewById(R.id.hscFilterLayout);
        classitext=(TextView)findViewById(R.id.classifytext);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        checkAndGivePermission();
        progressBar =(ProgressBar)findViewById(R.id.progressbar2);
        progressBar.setVisibility(View.INVISIBLE);// initiate the progress bar


        ivCapture.setOnClickListener(this);
        buclassify=findViewById(R.id.classify);
        retake=findViewById(R.id.retake);
        retake.setVisibility(View.INVISIBLE);

        retake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ivCapture2.setImageDrawable(null);
                remake=true;
                retake.setVisibility(View.GONE);
                ivCapture.setVisibility(View.VISIBLE);
                frameLayout.setVisibility(View.VISIBLE);
                ivCapture2.setVisibility(View.GONE);
            }
        });

        if(remake){
            ivCapture2.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            ivCapture.setVisibility(View.VISIBLE);
        }
        else {
            frameLayout.setVisibility(View.GONE);
            ivCapture.setVisibility(View.GONE);
            ivCapture2.setVisibility(View.VISIBLE);
        }


        try{
            tflite=new Interpreter(loadmodelfile(this));
        }catch (Exception e) {
            e.printStackTrace();
        }

        try {
            JSONObject object = new JSONObject(readJSON());
            JSONArray array = object.getJSONArray("Recipe");
            for (int i = 0; i < array.length(); i++) {

                JSONObject jsonObject = array.getJSONObject(i);
                String Name = jsonObject.getString("Name");
                String Recipe = jsonObject.getString("Recipe");
                String BuyLink = jsonObject.getString("BuyLink");
                String ImageLink = jsonObject.getString("ImageLink");

                recipeModel.add(new RecipeModel(Name,Recipe,BuyLink,ImageLink));

//                Toast.makeText(getApplicationContext(),Name,Toast.LENGTH_SHORT).show();

            }
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(),String.valueOf(e),Toast.LENGTH_SHORT).show();

            e.printStackTrace();
        }



        buclassify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int imageTensorIndex = 0;
                int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
                imageSizeY = imageShape[1];
                imageSizeX = imageShape[2];
                DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();

                int probabilityTensorIndex = 0;
                int[] probabilityShape =
                        tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
                DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

                inputImageBuffer = new TensorImage(imageDataType);
                outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
                probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

                inputImageBuffer = loadImage(bitmap);

                tflite.run(inputImageBuffer.getBuffer(),outputProbabilityBuffer.getBuffer().rewind());
                showresult();
            }
        });



    }
    
    private void checkAndGivePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_CALLBACK_CONSTANT);
        } else {
            initialize();
        }
    }

    private void initialize() {
        mCamera = getCameraInstance();
        CameraPreview mPreview = new CameraPreview(this, mCamera);
        FrameLayout rlCameraPreview = (FrameLayout) findViewById(R.id.rlCameraPreview);
        if (rlCameraPreview != null) {
            rlCameraPreview.addView(mPreview);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_CALLBACK_CONSTANT){
            boolean allgranted = false;
            for(int i=0;i<grantResults.length;i++){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    allgranted = true;
                } else {
                    allgranted = false;
                    break;
                }
            }


            if(allgranted){
                initialize();
            } else if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity2.this, Manifest.permission.CAMERA)){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA},PERMISSION_CALLBACK_CONSTANT);
                }
            } else if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity2.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },PERMISSION_CALLBACK_CONSTANT);
                }
            } else {
                Toast.makeText(MainActivity2.this,"Permission is mandatory, Try giving it from App Settings", Toast.LENGTH_LONG).show();
            }
        }
    }



    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return c;
    }

    public void colorEffectFilter(View v){
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            switch (v.getId()) {
                case R.id.rlNone:
                    parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
                    mCamera.setParameters(parameters);
                    break;
                case R.id.rlAqua:
                    parameters.setColorEffect(Camera.Parameters.EFFECT_AQUA);
                    mCamera.setParameters(parameters);
                    break;
                case R.id.rlBlackBoard:
                    parameters.setColorEffect(Camera.Parameters.EFFECT_BLACKBOARD);
                    mCamera.setParameters(parameters);
                    break;
                case R.id.rlMono:
                    parameters.setColorEffect(Camera.Parameters.EFFECT_MONO);
                    mCamera.setParameters(parameters);
                    break;
                case R.id.rlNegative:
                    parameters.setColorEffect(Camera.Parameters.EFFECT_NEGATIVE);
                    mCamera.setParameters(parameters);
                    break;
                case R.id.rlPosterized:
                    parameters.setColorEffect(Camera.Parameters.EFFECT_POSTERIZE);
                    mCamera.setParameters(parameters);
                    break;
                case R.id.rlSepia:
                    parameters.setColorEffect(Camera.Parameters.EFFECT_SEPIA);
                    mCamera.setParameters(parameters);
                    break;
                case R.id.rlSolarized:
                    parameters.setColorEffect(Camera.Parameters.EFFECT_SOLARIZE);
                    mCamera.setParameters(parameters);
                    break;
                case R.id.rlWhiteBoard:
                    parameters.setColorEffect(Camera.Parameters.EFFECT_WHITEBOARD);
                    mCamera.setParameters(parameters);
                    break;
            }
        }catch (Exception ex){
            Log.d(TAG,ex.getMessage());
        }
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            bitmap = BitmapFactory.decodeByteArray(data, 0,
                    data.length);
           mCamera.startPreview();
            // Rotate Image
            Matrix rotateMatrix = new Matrix();
            Bitmap rotatedBitmap;
            rotateMatrix.postRotate(90);
            rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotateMatrix, false);
            ivCapture2.setImageBitmap(rotatedBitmap);
            retake.setVisibility(View.VISIBLE);




//            File pictureFile = getOutputMediaFile();
//            if (pictureFile == null){
//                Log.d(TAG, "Error creating media file, check storage permissions: ");
//                return;
//            }
//
//            MediaScannerConnection.scanFile(MainActivity.this,
//                    new String[] { pictureFile.toString() }, null,
//                    new MediaScannerConnection.OnScanCompletedListener() {
//                        public void onScanCompleted(String path, Uri uri) {
//
//                            mCamera.startPreview();
//    }
//});
//            try {
//                FileOutputStream fos = new FileOutputStream(pictureFile);
//                fos.write(data);
//                fos.close();
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    };

    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ArshadPhotos");
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                System.out.println("Directory not created");
                return null;
            }
        }

        SecureRandom random = new SecureRandom();
        int num = random.nextInt(1000000);
        return new File(mediaStorageDir.getAbsolutePath() + File.separator +
                "IMG_"+ num + ".jpg");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.ivCapture:
                progressBar.setVisibility(View.VISIBLE);
                ivCapture.setVisibility(View.GONE);
                remake=false;
                retake.setVisibility(View.VISIBLE);
                mCamera.takePicture(null,null,mPicture);
                    frameLayout.setVisibility(View.GONE);
                    ivCapture2.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                break;

        }
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        if(mCamera != null) {
//            mCamera.stopPreview();
//            mCamera.release();
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if(mCamera != null) {
//            mCamera.release();
//            mCamera = null;
//        }
//    }
//    @Override
//    protected void onResume() {
//        super.onResume();
//        mCamera.startPreview();
//
//    }

    private TensorImage loadImage(final Bitmap bitmap) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);

        // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(getPreprocessNormalizeOp())
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }

    private MappedByteBuffer loadmodelfile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor=activity.getAssets().openFd("model.tflite");
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startoffset = fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startoffset,declaredLength);
    }

    private TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }
    private TensorOperator getPostprocessNormalizeOp(){
        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
    }
    private void showresult(){

        try{
            labels = FileUtil.loadLabels(this,"labels.txt");
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<String, Float> labeledProbability =
                new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
                        .getMapWithFloatValue();
        float maxValueInMap =(Collections.max(labeledProbability.values()));

        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {

            if (entry.getValue()==maxValueInMap) {
                float confi = 100 * entry.getValue();

                if (confi > 90) {


                    for (int i = 0; i < recipeModel.size(); i++) {
                        if (recipeModel.get(i).getName().equals(entry.getKey()) && confi > 70) {
                            ArrayList<RecipeModel> yo;
                            yo = new ArrayList<>();
                            yo.add(new RecipeModel(recipeModel.get(i).getName(), recipeModel.get(i).getRecipe(), String.valueOf(confi), recipeModel.get(i).getImageLink()));
                            Intent myIntent = new Intent(MainActivity2.this, Recipe.class);
                            myIntent.putExtra("key", yo);
                            MainActivity2.this.startActivity(myIntent);
                            ivCapture2.setImageDrawable(null);
                            remake = true;
                            retake.setVisibility(View.GONE);
                            ivCapture.setVisibility(View.VISIBLE);
                            frameLayout.setVisibility(View.VISIBLE);
                            ivCapture2.setVisibility(View.GONE);

                        }
                    }
                }else {
                    Toast.makeText(getApplicationContext(),"Not detected please retake an image",Toast.LENGTH_SHORT).show();

                }




            }

        }
    }

//    public void rotateImage(Bitmap bitmap) {
//        ExifInterface exifInterface=null;
//        try{
//            exifInterface=new ExifInterface();
//        }catch (IOException e){
//            e.printStackTrace();
//        }
//        int orientation=exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED);
//        Matrix matrix=new Matrix();
//        switch (orientation){
//            case ExifInterface.ORIENTATION_ROTATE_90:
//                matrix.setRotate(90);
//                break;
//            case ExifInterface.ORIENTATION_ROTATE_180:
//                matrix.setRotate(180);
//                break;
//            default:
//        }
//        Bitmap rotatedBitmap=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
//
//    }

    public String readJSON() {
        String json = null;
        try {
            // Opening data.json file
            InputStream inputStream = getAssets().open("Recipe.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            // read values in the byte array
            inputStream.read(buffer);
            inputStream.close();
            // convert byte to string
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return json;
        }
        return json;
    }
}



