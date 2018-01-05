package com.facedetection;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import com.facedetection.model.FaceResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Nguyen on 5/20/2016.
 */

public class ImageUtils {

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    //Get Path Image file
    public final static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

/**
* Method to  Rotate Bitmap Accordingly
* */
    //Rotate Bitmap
    public final static Bitmap rotate(Bitmap b, float degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) b.getWidth() / 2,
                    (float) b.getHeight() / 2);

            Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(),
                    b.getHeight(), m, true);
            if (b != b2) {
                b.recycle();
                b = b2;
            }

        }
        return b;
    }

/**
* Method to get Bitmap In original Size
 *
 * Can crash with OOM Exception
* */
    public static Bitmap getBitmap(String filePath, int width, int height,Context context) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(Uri.parse(filePath));
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, options);
        input.close();
        options.inSampleSize = ImageUtils.calculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
        input.close();
        if (bitmap != null) {
            try {
                ExifInterface ei = new ExifInterface(filePath);
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        bitmap = ImageUtils.rotate(bitmap, 90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        bitmap = ImageUtils.rotate(bitmap, 180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        bitmap = ImageUtils.rotate(bitmap, 270);
                        break;
                    // etc.
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    /**
    * Method to get Bitmap in Smaller Size
    * */

    public static Bitmap getThumbnail(Uri uri,Context context) throws FileNotFoundException, IOException{
        InputStream input = context.getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inMutable=true;
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.RGB_565;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();

        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
            return null;
        }

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > 1000) ? (originalSize / 1000) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither = true; //optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.RGB_565;//
        input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }

    private static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }

/**
* Method to Crop Bitmap
* */
    public static Bitmap cropBitmap(Bitmap bitmap, Rect rect) {
        int w = rect.right - rect.left;
        int h = rect.bottom - rect.top;
        Bitmap ret = Bitmap.createBitmap(w, h, bitmap.getConfig());
        Canvas canvas = new Canvas(ret);
        canvas.drawBitmap(bitmap, -rect.left, -rect.top, null);
        bitmap.recycle();
        return ret;
    }

    /**
     * Method to Crop Face from Cropped bitmap
     * */
    public static Bitmap cropFace(FaceResult face, Bitmap bitmap, int rotate) {
        Bitmap bmp;

        float eyesDis = face.eyesDistance();
        PointF mid = new PointF();
        face.getMidPoint(mid);

        Rect rect = new Rect(
                (int) (mid.x - eyesDis * 1.20f),
                (int) (mid.y - eyesDis * 0.55f),
                (int) (mid.x + eyesDis * 1.20f),
                (int) (mid.y + eyesDis * 1.85f));

        Bitmap.Config config = Bitmap.Config.RGB_565;
        if (bitmap.getConfig() != null) config = bitmap.getConfig();
        bmp = bitmap.copy(config, true);

        switch (rotate) {
            case 90:
                bmp = ImageUtils.rotate(bmp, 90);
                break;
            case 180:
                bmp = ImageUtils.rotate(bmp, 180);
                break;
            case 270:
                bmp = ImageUtils.rotate(bmp, 270);
                break;
        }

        bmp = ImageUtils.cropBitmap(bmp, rect);
        return bmp;
    }

    /**
     * Method to get Media File Uri
     * */

    public static Uri getOutputMediaFileUri(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return FileProvider.getUriForFile(context, context.getPackageName() + ".my.package.name.provider", getOutputMediaFile(context));
        else
            return Uri.fromFile(getOutputMediaFile(context));
    }



    public static File getOutputMediaFile(Context context) {
        if (isExternalStorageWritable()) {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"FaceDetection");
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdir()) {
                    return null;
                }
            }
            return new File(mediaStorageDir.getPath() + File.separator, "IMG123.jpg");
        } else return null;
    }



    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

}
