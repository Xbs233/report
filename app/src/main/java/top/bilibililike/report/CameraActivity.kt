package top.bilibililike.report

/**
 *  Project: report
 *  Created by Xbs on 2020/6/6
 *  Describe:
 */


import android.graphics.BitmapFactory
import android.graphics.RectF
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import kotlinx.android.synthetic.main.activity_face.*
import okio.buffer
import okio.sink
import top.bilibililike.report.utils.BitmapUtils
import top.bilibililike.report.utils.FileUtil
import kotlin.concurrent.thread

class CameraActivity : AppCompatActivity() {
    companion object {
        const val TYPE_TAG = "type"
        const val TYPE_CAPTURE = 0
        const val TYPE_RECORD = 1
        const val TAG = "CameraActivity"
    }

    var lock = false //控制MediaRecorderHelper的初始化

    private lateinit var mCameraHelper: CameraHelper
    private var mMediaRecorderHelper: MediaRecorderHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        mCameraHelper = CameraHelper(this, surfaceView)
        mCameraHelper.addCallBack(object : CameraHelper.CallBack {
            override fun onFaceDetect(faces: ArrayList<RectF>) {
                faceView.setFaces(faces)
            }

            override fun onTakePic(data: ByteArray?) {
                savePic(data)
            }

            override fun onPreviewFrame(data: ByteArray?) {
                if (!lock) {
                    mCameraHelper.getCamera()?.let {
                        mMediaRecorderHelper = MediaRecorderHelper(this@CameraActivity, mCameraHelper.getCamera()!!, mCameraHelper.mDisplayOrientation, mCameraHelper.mSurfaceHolder.surface)
                    }
                    lock = true
                }
            }
        })

    }

    private fun savePic(data: ByteArray?) {
        thread {
            try {
                val temp = System.currentTimeMillis()
                val picFile = FileUtil.createCameraFile()
                if (picFile != null && data != null) {
                    val rawBitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    val resultBitmap = if (mCameraHelper.mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                        BitmapUtils.mirror(BitmapUtils.rotate(rawBitmap, 270f))
                    else
                        BitmapUtils.rotate(rawBitmap, 90f)

                    picFile.sink().buffer().write(BitmapUtils.toByteArray(resultBitmap)).close()
                    runOnUiThread {
                        Toast.makeText(this,"图片已保存! ${picFile.absolutePath}", LENGTH_SHORT).show()
                        Log.d(TAG,"图片已保存! 耗时：${System.currentTimeMillis() - temp}    路径：  ${picFile.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this,"保存图片失败！",LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        mCameraHelper.releaseCamera()
        mMediaRecorderHelper?.let {
            if (it.isRunning)
                it.stopRecord()
            it.release()
        }
        super.onDestroy()
    }

}