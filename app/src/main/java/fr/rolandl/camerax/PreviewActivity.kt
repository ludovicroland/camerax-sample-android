package fr.rolandl.camerax

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_preview.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions
import java.util.concurrent.Executors

@RuntimePermissions
class PreviewActivity
  : AppCompatActivity()
{

  private val cameraExecutor = Executors.newSingleThreadExecutor()

  private var preview: Preview? = null

  override fun onCreate(savedInstanceState: Bundle?)
  {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_preview)

    grantPermissions?.setOnClickListener {
      if (ActivityCompat.shouldShowRequestPermissionRationale(this@PreviewActivity, Manifest.permission.CAMERA) == false)
      {
        val intent = Intent().apply {
          action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
          data = Uri.fromParts("package", this@PreviewActivity.packageName, null)
        }

        startActivity(intent)
      }
      else
      {
        showCameraWithPermissionCheck()
      }
    }

    showCameraWithPermissionCheck()
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
  {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    onRequestPermissionsResult(requestCode, grantResults)
  }

  @NeedsPermission(Manifest.permission.CAMERA)
  fun showCamera()
  {
    grantPermissions?.isVisible = false

    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener(Runnable {
      val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

      val cameraSelector = CameraSelector.Builder().apply {
        //setTargetAspectRatio(AspectRatio.RATIO_16_9)
        requireLensFacing(CameraSelector.LENS_FACING_BACK)
      }.build()

      preview = Preview.Builder().apply {
        setTargetRotation(cameraPreview?.display?.rotation ?: Surface.ROTATION_0)
      }.build()

      cameraProvider.apply {
        unbindAll()

        val camera = bindToLifecycle(this@PreviewActivity, cameraSelector, preview)
        preview?.setSurfaceProvider(cameraPreview?.createSurfaceProvider(camera.cameraInfo))
      }

    }, ContextCompat.getMainExecutor(this))
  }

  @OnPermissionDenied(Manifest.permission.CAMERA)
  fun onCameraDenied()
  {
    grantPermissions?.isVisible = true
  }

  @OnNeverAskAgain(Manifest.permission.CAMERA)
  fun onCameraNeverAskAgain()
  {
    grantPermissions?.isVisible = true
  }

  override fun onDestroy()
  {
    try
    {
      cameraExecutor.shutdown()

      val processCameraProvider = ProcessCameraProvider.getInstance(this)

      processCameraProvider.addListener(Runnable {
        val cameraProviderFuture = processCameraProvider.get()

        preview?.let {
          cameraProviderFuture.unbind(it)
        }
      }, ContextCompat.getMainExecutor(this))
    }
    finally
    {
      super.onDestroy()
    }
  }

}
