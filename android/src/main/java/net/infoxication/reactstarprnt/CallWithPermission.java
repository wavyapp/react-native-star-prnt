package net.infoxication.reactstarprnt;

import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Callable;

public class CallWithPermission {
  private AppCompatActivity activity;
  private String permission;
  private ActivityResultLauncher<String> requestPermissionActivity;
  private Boolean requestPermissionActivityResult;


  CallWithPermission(AppCompatActivity activity, String permission) {
    this.activity = activity;
    this.permission = permission;
  }

  public <O> O callMayThrow(Callable<O> grantedCallback, Runnable deniedCallback) throws Exception {
    if (ContextCompat.checkSelfPermission(this.activity, this.permission) == PackageManager.PERMISSION_GRANTED) {
      return grantedCallback.call();
    } else {
      this.requestPermissionActivity = this.activity.getActivityResultRegistry().register(
        "CallWithPermission_" + this.permission,
        new ActivityResultContracts.RequestPermission(),
        (Boolean result) -> {
          this.requestPermissionActivityResult = result;
        }
      );
      this.requestPermissionActivity.launch(this.permission);
      while (this.requestPermissionActivityResult == null) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          this.requestPermissionActivity.unregister();
        }
      }
      if (this.requestPermissionActivityResult) {
        return grantedCallback.call();
      } else {
        deniedCallback.run();
      }
    }
    return null;
  }
}
