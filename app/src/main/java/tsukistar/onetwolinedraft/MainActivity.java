package tsukistar.onetwolinedraft;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import static android.os.Environment.getExternalStorageDirectory;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.COLOR_BGRA2GRAY;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class MainActivity extends AppCompatActivity {

    private ImageView showPic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//设置界面为竖屏
        iniLoadOpenCV();
        initialize_UI();
        getpermissions();

    }

    public void initialize_UI(){
        showPic = findViewById(R.id.ImView_ShowPic);
        showPic.setOnClickListener(ChangeImg);
        showPic.setOnLongClickListener(SaveImg);
        Button fromGallery = findViewById(R.id.Button_Gallery);
        fromGallery.setOnClickListener(pickphoto);
    }

    private void iniLoadOpenCV() {
        boolean success = OpenCVLoader.initDebug();
        if (success) {
            String CV_TAG = "OpenCV";
            Log.i(CV_TAG, "OpenCV Libraries loaded...");
        } else {
            Toast.makeText(this.getApplicationContext(), "WARNING: Could not load OpenCV Libraries!", Toast.LENGTH_LONG).show();
        }
    }

    final View.OnClickListener ChangeImg = new View.OnClickListener() { //处理图像
        @SuppressLint("ShowToast")
        @Override
        public void onClick(View v) {
            try {
                Bitmap bitmap = ((BitmapDrawable) showPic.getDrawable()).getBitmap();
                Mat src = new Mat();
                Utils.bitmapToMat(bitmap, src);
                Mat binary = GaussianGrayThreshold(src);
                Mat dst = new Mat();
                Utils.matToBitmap(binary, bitmap);
                showPic.setImageBitmap(bitmap);
                binary.release();
                src.release();
                dst.release();
                Toast.makeText(getApplicationContext(),R.string.makeLine,Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                Toast.makeText(getApplicationContext(),R.string.OnClickError,Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

        }
    };

    View.OnLongClickListener SaveImg = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            String filename = generateFileName();
            saveBitmap(showPic, filename);
            Toast.makeText(getApplicationContext(),R.string.saveDraft,Toast.LENGTH_SHORT).show();
            updateGallery(filename);
            return true;
        }
    };

    private Mat GaussianGrayThreshold(Mat src){    //高斯模糊、灰度、二值化
        Mat gaussian = new Mat();
        Size size = new Size(3,3);
        GaussianBlur(src,gaussian,size,0);
        Mat gray = new Mat();
        cvtColor(gaussian, gray, COLOR_BGRA2GRAY);
        Mat binary = gray.clone();
        adaptiveThreshold(binary,binary, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY,3,3);
        gray.release();
        gaussian.release();
        return binary;
    }

    //长按保存图片
    public static void saveBitmap(ImageView view, String filePath) {
        Drawable drawable = view.getDrawable();
        if (drawable == null) {
            return;
        }
        FileOutputStream outStream = null;
        File file = new File(filePath);
        if (file.isDirectory()) {//如果是目录不允许保存
            return;
        }
        try {
            outStream = new FileOutputStream(file);
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outStream != null) {
                    outStream.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //刷新媒体库
    private void updateGallery(String filename)//filename是我们的文件全名，包括后缀哦
    {
        MediaScannerConnection.scanFile(this,
                new String[] { filename }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    //随机文件名
    private String generateFileName() {
        String fileList = getExternalStorageDirectory().getAbsolutePath() + File.separator + "LineDraft" + File.separator;
        File mkdir = new File(fileList);
        if(!mkdir.exists()) mkdir.mkdir();
        @SuppressLint("SimpleDateFormat") DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");// 获得当前时间
        String formatDate = format.format(new Date());// 转换为字符串
        int random = new Random().nextInt(10000);// 随机生成文件编号
        return (fileList + formatDate + random + ".png");
    }

    private void getpermissions() {    //权限申请
        if (!AndPermission.hasPermissions(this, STORAGE_SERVICE))
            AndPermission.with(this).runtime().permission(Permission.Group.STORAGE).start();
    }

    //打开相册
    View.OnClickListener pickphoto = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 2);
        }
    };

    //处理回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String picturePath;
        if (requestCode == 2 && resultCode == RESULT_OK && null != data) {

            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            assert selectedImage != null;
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            assert cursor != null;
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();
            if (!picturePath.isEmpty()) showPic.setImageURI(selectedImage);
        }
    }

    //退出界面
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder bdr = new AlertDialog.Builder(this);
            bdr.setMessage(R.string.app_name);
            bdr.setIcon(R.drawable.logo);
            bdr.setMessage(R.string.ask);
            bdr.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            bdr.setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            bdr.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
