package com.fxn.utility

import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Vibrator
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewPropertyAnimator
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.fxn.modals.Img
import com.fxn.pix.Options
import com.fxn.pix.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

//import com.otaliastudios.cameraview.size.Size;
/**
 * Created by akshay on 21/01/18.
 */
class Utility {
    private val pathDir: String? = null

    companion object {
        var HEIGHT = 0
        var WIDTH = 0
        fun setupStatusBarHidden(appCompatActivity: AppCompatActivity) {
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(appCompatActivity.getWindow(), false);

            WindowInsetsController inset = appCompatActivity.getWindow().getInsetsController();
            inset.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            inset.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            Window w = appCompatActivity.getWindow();
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            w.setStatusBarColor(Color.TRANSPARENT);
            w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }*/
            val w = appCompatActivity.window
            w.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            w.statusBarColor = Color.TRANSPARENT
            //w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            //w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            darkNavigationBar(appCompatActivity, true)
        }

        private fun darkNavigationBar(appCompatActivity: AppCompatActivity, dark: Boolean) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val w = appCompatActivity.window
                if (dark) {
                    w.navigationBarColor = Color.BLACK
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        var flags = w.decorView.systemUiVisibility // get current flag
                        flags = flags xor View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR // use XOR here for remove LIGHT_STATUS_BAR from flags
                        w.decorView.systemUiVisibility = flags
                    }
                } else {
                    w.navigationBarColor = Color.WHITE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        w.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    }
                }
            }
        }

        fun setupNavigationInset(view: View?) {
            ViewCompat.setOnApplyWindowInsetsListener(view!!) { v: View, windowInsets: WindowInsetsCompat ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                // Apply the insets as a margin to the view. Here the system is setting
                // only the bottom, left, and right dimensions, but apply whichever insets are
                // appropriate to your layout. You can also update the view padding
                // if that's more appropriate.
                val mlp = v.layoutParams as MarginLayoutParams
                mlp.leftMargin = insets.left
                mlp.bottomMargin = insets.bottom
                mlp.rightMargin = insets.right
                v.layoutParams = mlp
                WindowInsetsCompat.CONSUMED
            }
        }

        fun showStatusBar(appCompatActivity: AppCompatActivity) {
            synchronized(appCompatActivity) {

                /* Window w = appCompatActivity.getWindow();
              View decorView = w.getDecorView();
              // Show Status Bar.
              int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
              decorView.setSystemUiVisibility(uiOptions);*/appCompatActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController inset = appCompatActivity.getWindow().getInsetsController();
                inset.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }*/darkNavigationBar(appCompatActivity, false)
            }
        }

        fun hideStatusBar(appCompatActivity: AppCompatActivity) {
            synchronized(appCompatActivity) {

                /*Window w = appCompatActivity.getWindow();
	      View decorView = w.getDecorView();
	      // Hide Status Bar.
	      int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
	      decorView.setSystemUiVisibility(uiOptions);*/appCompatActivity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController inset = appCompatActivity.getWindow().getInsetsController();
                inset.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }*/darkNavigationBar(appCompatActivity, true)
            }
        }

        fun getSoftButtonsBarSizePort(activity: Activity): Int {
            // getRealMetrics is only available with API 17 and +
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val metrics = DisplayMetrics()
                activity.windowManager.defaultDisplay.getMetrics(metrics)
                val usableHeight = metrics.heightPixels
                activity.windowManager.defaultDisplay.getRealMetrics(metrics)
                val realHeight = metrics.heightPixels
                return if (realHeight > usableHeight) {
                    realHeight - usableHeight
                } else {
                    0
                }
            }
            return 0
        }

        fun getStatusBarSizePort(check: AppCompatActivity): Int {
            // getRealMetrics is only available with API 17 and +
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                var result = 0
                //Log.e("->activity", "----------->  " + check);
                val res = check.baseContext.resources
                val resourceId = res.getIdentifier("status_bar_height", "dimen", "android")
                if (resourceId > 0) {
                    result = check.resources.getDimensionPixelSize(resourceId)
                }
                return result
            }
            return 0
        }

        fun getScreenSize(activity: Activity) {
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            HEIGHT = displayMetrics.heightPixels
            WIDTH = displayMetrics.widthPixels
        }

        fun convertDpToPixel(dp: Float, context: Context): Float {
            val resources = context.resources
            val metrics = resources.displayMetrics
            return dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }

        fun convertPixelsToDp(px: Float, context: Context): Float {
            val resources = context.resources
            val metrics = resources.displayMetrics
            return px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }

        fun getDateDifference(context: Context, calendar: Calendar): String {
            val d = calendar.time
            val lastMonth = Calendar.getInstance()
            val lastWeek = Calendar.getInstance()
            val recent = Calendar.getInstance()
            lastMonth.add(Calendar.DAY_OF_MONTH, -Calendar.DAY_OF_MONTH)
            lastWeek.add(Calendar.DAY_OF_MONTH, -7)
            recent.add(Calendar.DAY_OF_MONTH, -2)
            return if (calendar.before(lastMonth)) {
                SimpleDateFormat("MMMM", Locale.getDefault()).format(d)
            } else if (calendar.after(lastMonth) && calendar.before(lastWeek)) {
                context.resources.getString(R.string.pix_last_month)
            } else if (calendar.after(lastWeek) && calendar.before(recent)) {
                context.resources.getString(R.string.pix_last_week)
            } else {
                context.resources.getString(R.string.pix_recent)
            }
        }

        fun isNull(topChild: View?): Boolean {
            return topChild == null
        }

        fun getCursor(context: Context): Cursor? {
            return context.contentResolver.query(
                Constants.URI, Constants.PROJECTION,
                null, null, Constants.ORDERBY
            )
        }

        fun getImageVideoCursor(context: Context, excludeVideo: Boolean): Cursor? {
            return context.contentResolver
                .query(
                    Constants.IMAGE_VIDEO_URI, Constants.IMAGE_VIDEO_PROJECTION,
                    if (excludeVideo) Constants.IMAGE_SELECTION else Constants.IMAGE_VIDEO_SELECTION, null, Constants.IMAGE_VIDEO_ORDERBY
                )
        }

        fun getImageVideoCursor(context: Context, mode: Options.Mode?): Cursor? {
            val projection: String?
            projection = when (mode) {
                Options.Mode.Video -> Constants.VIDEO_SELECTION
                Options.Mode.Picture -> Constants.IMAGE_SELECTION
                else -> Constants.IMAGE_VIDEO_SELECTION
            }
            return context.contentResolver
                .query(
                    Constants.IMAGE_VIDEO_URI, Constants.IMAGE_VIDEO_PROJECTION,
                    projection, null, Constants.IMAGE_VIDEO_ORDERBY
                )
        }

        fun isViewVisible(view: View?): Boolean {
            return view != null && view.visibility == View.VISIBLE
        }

        fun showScrollbar(mScrollbar: View?, context: Context): ViewPropertyAnimator {
            val transX = context.resources.getDimensionPixelSize(R.dimen.fastscroll_bubble_size).toFloat()
            mScrollbar!!.translationX = transX
            mScrollbar.visibility = View.VISIBLE
            return mScrollbar.animate().translationX(0f).alpha(1f)
                .setDuration(Constants.sScrollbarAnimDuration.toLong())
                .setListener(object : AnimatorListenerAdapter() { // adapter required for new alpha value to stick
                })
        }

        fun cancelAnimation(animator: ViewPropertyAnimator?) {
            animator?.cancel()
        }

        fun manipulateVisibility(
            activity: AppCompatActivity, slideOffset: Float, arrow_up: View,
            instantRecyclerView: RecyclerView?, recyclerView: RecyclerView?,
            status_bar_bg: View?, topbar: View?, clickme: View?, sendButton: View?, longSelection: Boolean
        ) {
            instantRecyclerView!!.alpha = 1 - slideOffset
            arrow_up.alpha = 1 - slideOffset
            clickme!!.alpha = 1 - slideOffset
            if (longSelection) {
                sendButton!!.alpha = 1 - slideOffset
            }
            topbar!!.alpha = slideOffset
            recyclerView!!.alpha = slideOffset
            if (1 - slideOffset == 0f && instantRecyclerView.visibility == View.VISIBLE) {
                instantRecyclerView.visibility = View.GONE
                arrow_up.visibility = View.GONE
                clickme.visibility = View.GONE
            } else if (instantRecyclerView.visibility == View.GONE && 1 - slideOffset > 0) {
                instantRecyclerView.visibility = View.VISIBLE
                arrow_up.visibility = View.VISIBLE
                clickme.visibility = View.VISIBLE
                if (longSelection) {
                    sendButton!!.clearAnimation()
                    sendButton.visibility = View.VISIBLE
                }
            }
            if (slideOffset > 0 && recyclerView.visibility == View.INVISIBLE) {
                recyclerView.visibility = View.VISIBLE
                status_bar_bg!!.animate().translationY(0f).setDuration(200).start()
                topbar.visibility = View.VISIBLE
                showStatusBar(activity)
            } else if (recyclerView.visibility == View.VISIBLE && slideOffset == 0f) {
                hideStatusBar(activity)
                recyclerView.visibility = View.INVISIBLE
                topbar.visibility = View.GONE
                status_bar_bg!!.animate().translationY(-status_bar_bg.height.toFloat()).setDuration(550).start()
            }
        }

        fun getValueInRange(min: Int, max: Int, value: Int): Int {
            val minimum = Math.max(min, value)
            return Math.min(minimum, max)
        }

        fun vibe(c: Context, l: Long) {
            (c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(l)
        }

        fun writeImage(
            bitmap: Bitmap, path: String?, quality: Int, newWidth: Int,
            newHeight: Int
        ): File {
            var bitmap = bitmap
            var newWidth = newWidth
            var newHeight = newHeight
            val dir = File(Environment.getExternalStorageDirectory(), path)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val photo = File(
                dir, "IMG_"
                        + SimpleDateFormat("yyyyMMdd_HHmmSS", Locale.ENGLISH).format(Date())
                        + ".jpg"
            )
            if (photo.exists()) {
                photo.delete()
            }
            if (newWidth == 0 && newHeight == 0) {
                newWidth = bitmap.width / 2
                newHeight = bitmap.height / 2
            }
            bitmap = getResizedBitmap(bitmap, newWidth, newHeight)
            try {
                val fos = FileOutputStream(photo.path)
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                // fos.write(jpeg);
                fos.close()
            } catch (e: Exception) {
                Log.e("PictureDemo", "Exception in photoCallback", e)
            }
            return photo
        }

        fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
            val width = bm.width
            val height = bm.height
            val scaleWidth = newWidth.toFloat() / width
            val scaleHeight = newHeight.toFloat() / height
            // CREATE A MATRIX FOR THE MANIPULATION
            val matrix = Matrix()
            // RESIZE THE BIT MAP
            matrix.postScale(scaleWidth, scaleHeight)

            // "RECREATE" THE NEW BITMAP
            val resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false
            )
            return resizedBitmap.copy(Bitmap.Config.RGB_565, false)
        }

        fun getScaledBitmap(maxWidth: Int, rotatedBitmap: Bitmap): Bitmap? {
            return try {
                val nh = (rotatedBitmap.height * (512.0 / rotatedBitmap.width)).toInt()
                Bitmap.createScaledBitmap(rotatedBitmap, maxWidth, nh, true)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun rotate(scaledBitmap: Bitmap, i: Int): Bitmap {
            if (i == 0) {
                return scaledBitmap
            }
            val matrix = Matrix()
            matrix.preRotate(-i.toFloat())
            return Bitmap.createBitmap(
                scaledBitmap, 0, 0, scaledBitmap.width,
                scaledBitmap.height, matrix, false
            )
        }

        fun getFingerSpacing(event: MotionEvent): Float {
            return try {
                val x = event.getX(0) - event.getX(1)
                val y = event.getY(0) - event.getY(1)
                Math.sqrt((x * x + y * y).toDouble()).toFloat()
            } catch (e: Exception) {
                Log.e("exc", "->" + e.message)
                0F
            }
        }

        fun containsName(list: ArrayList<Img?>, url: String): Boolean {
            for (o in list) {
                if (o != null && o.contentUrl == url) {
                    return true
                }
            }
            return false
        }

        fun scanPhoto(pix: Context?, photo: File) {
            MediaScannerConnection.scanFile(pix, arrayOf(photo.toString()), arrayOf(photo.name), null)
        }

        fun gcd(p: Int, q: Int): Int {
            return if (q == 0) p else gcd(q, p % q)
        }

        /*public static Size ratio(int a, int b) {
        final int gcd = gcd(a, b);
        if (a > b) {
            showAnswer(a / gcd, b / gcd);
            return new Size(a / gcd, b / gcd);
        } else {
            return new Size(b / gcd, a / gcd);
        }
    }*/
        fun showAnswer(a: Int, b: Int) {
            Log.e("show ratio", "->  $a $b")
        }
    }
}