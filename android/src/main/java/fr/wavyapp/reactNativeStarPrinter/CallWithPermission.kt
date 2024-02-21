package fr.wavyapp.reactNativeStarPrinter

import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.concurrent.Callable

class CallWithPermission internal constructor(
  private val activity: AppCompatActivity,
  private val permission: String
) {
  private var requestPermissionActivity: ActivityResultLauncher<String>? = null
  private var requestPermissionActivityResult: Boolean? = null
  @Throws(Exception::class)
  fun <O> callMayThrow(grantedCallback: Callable<O>, deniedCallback: Runnable): O? {
    if (ContextCompat.checkSelfPermission(
        activity,
        permission
      ) == PackageManager.PERMISSION_GRANTED
    ) {
      return grantedCallback.call()
    } else {
      requestPermissionActivity = activity.activityResultRegistry.register(
        "CallWithPermission_$permission",
        ActivityResultContracts.RequestPermission()
      ) { result: Boolean? ->
        requestPermissionActivityResult = result
      }
      requestPermissionActivity!!.launch(permission)
      while (requestPermissionActivityResult == null) {
        try {
          Thread.sleep(500)
        } catch (e: InterruptedException) {
          requestPermissionActivity!!.unregister()
        }
      }
      if (requestPermissionActivityResult!!) {
        return grantedCallback.call()
      } else {
        deniedCallback.run()
      }
    }
    return null
  }
}
