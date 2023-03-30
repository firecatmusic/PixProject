package com.fxn.pix

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View.OnTouchListener
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fxn.adapters.InstantImageAdapter
import com.fxn.adapters.MainImageAdapter
import com.fxn.interfaces.OnSelectionListener
import com.fxn.modals.Img
import com.fxn.utility.*
import com.fxn.utility.ui.FastScrollStateChangeListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Pix : AppCompatActivity(), OnTouchListener {
    //--private CameraView camera;
    private var imageCapture: ImageCapture? = null
    private var previewView: PreviewView? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var outputDirectory: File? = null
    private var cameraExecutor: ExecutorService? = null
    private var cameraFlashMode = ImageCapture.FLASH_MODE_OFF
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var statusBarHeight = 0
    private var bottomBarHeight = 0
    private var colorPrimaryDark = 0
    private var zoom = 0.0f
    private val dist = 0.0f
    private val handler = Handler(Looper.getMainLooper())
    private val video_counter_handler = Handler()
    private val video_counter_runnable: Runnable? = null
    private val mFastScrollStateChangeListener: FastScrollStateChangeListener? = null
    private var recyclerView: RecyclerView? = null
    private var instantRecyclerView: RecyclerView? = null
    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null
    private var initaliseadapter: InstantImageAdapter? = null
    private var status_bar_bg: View? = null
    private var mScrollbar: View? = null
    private var topbar: View? = null
    private var bottomButtons: View? = null
    private var topButton: View? = null
    private var sendButton: View? = null
    private var mBubbleView: TextView? = null
    private var img_count: TextView? = null
    private var mHandleView: ImageView? = null
    private var selection_back: ImageView? = null
    private var selection_check: ImageView? = null
    private var btnBack: ImageView? = null
    private var video_counter_progressbar: ProgressBar? = null
    private var mScrollbarAnimator: ViewPropertyAnimator? = null
    private var mBubbleAnimator: ViewPropertyAnimator? = null
    private val selectionList: MutableSet<Img?> = HashSet()
    private val mScrollbarHider = Runnable { hideScrollbar() }
    private var mainImageAdapter: MainImageAdapter? = null
    private var mViewHeight = 0f
    private val mHideScrollbar = true
    private var LongSelection = false
    private var options: Options? = null
    private var selection_count: TextView? = null
    private var zoomBar: SeekBar? = null
    private var zoomView: ImageView? = null
    private var vZoom: ConstraintLayout? = null
    private val mScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (!mHandleView!!.isSelected && recyclerView.isEnabled) {
                setViewPositions(getScrollProportion(recyclerView))
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (recyclerView.isEnabled) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        handler.removeCallbacks(mScrollbarHider)
                        if (mScrollbar!!.visibility != View.VISIBLE) {
                            Utility.Companion.cancelAnimation(mScrollbarAnimator)
                            if (!Utility.Companion.isViewVisible(mScrollbar) && (recyclerView.computeVerticalScrollRange() - mViewHeight > 0)) {
                                mScrollbarAnimator = Utility.Companion.showScrollbar(mScrollbar, this@Pix)
                            }
                        }
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> if (mHideScrollbar && !mHandleView!!.isSelected) {
                        handler.postDelayed(mScrollbarHider, sScrollbarHideDelay.toLong())
                    }
                    else -> {}
                }
            }
        }
    }
    private var flash: FrameLayout? = null
    private var front: ImageView? = null
    private var clickme: View? = null
    private var flashDrawable = 0
    private val onSelectionListener: OnSelectionListener = object : OnSelectionListener {
        override fun onClick(img: Img?, view: View?, position: Int) {
            if (LongSelection) {
                if (selectionList.contains(img)) {
                    selectionList.remove(img)
                    initaliseadapter!!.select(false, position)
                    mainImageAdapter!!.select(false, position)
                } else {
                    if ((options?.count ?: 1) <= selectionList.size) {
                        Toast.makeText(
                            this@Pix, String.format(
                                resources.getString(R.string.selection_limiter_pix), selectionList.size
                            ), Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    img?.position = (position)
                    selectionList.add(img)
                    initaliseadapter!!.select(true, position)
                    mainImageAdapter!!.select(true, position)
                }
                if (selectionList.size == 0) {
                    LongSelection = false
                    selection_check!!.visibility = View.VISIBLE
                    DrawableCompat.setTint(selection_back!!.drawable, colorPrimaryDark)
                    topbar!!.setBackgroundColor(Color.parseColor("#ffffff"))
                    val anim: Animation = ScaleAnimation(
                        1f, 0f,  // Start and end values for the X axis scaling
                        1f, 0f,  // Start and end values for the Y axis scaling
                        Animation.RELATIVE_TO_SELF, 0.5f,  // Pivot point of X scaling
                        Animation.RELATIVE_TO_SELF, 0.5f
                    ) // Pivot point of Y scaling
                    anim.fillAfter = true // Needed to keep the result of the animation
                    anim.duration = 300
                    anim.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation) {}
                        override fun onAnimationEnd(animation: Animation) {
                            sendButton!!.visibility = View.GONE
                            sendButton!!.clearAnimation()
                        }

                        override fun onAnimationRepeat(animation: Animation) {}
                    })
                    sendButton!!.startAnimation(anim)
                }
                selection_count!!.text = selectionList.size.toString() + " " + resources.getString(R.string.pix_selected)
                img_count!!.text = selectionList.size.toString()
            } else {
                img?.position = (position)
                selectionList.add(img)
                returnObjects()
                DrawableCompat.setTint(selection_back!!.drawable, colorPrimaryDark)
                topbar!!.setBackgroundColor(Color.parseColor("#ffffff"))
            }
        }

        override fun onLongClick(img: Img?, view: View?, position: Int) {
            if ((options?.count ?: 1) > 1) {
                Utility.Companion.vibe(this@Pix, 50)
                LongSelection = true
                if (selectionList.size == 0 && (mBottomSheetBehavior!!.state != BottomSheetBehavior.STATE_EXPANDED)) {
                    sendButton!!.visibility = View.VISIBLE
                    val anim: Animation = ScaleAnimation(
                        0f, 1f,  // Start and end values for the X axis scaling
                        0f, 1f,  // Start and end values for the Y axis scaling
                        Animation.RELATIVE_TO_SELF, 0.5f,  // Pivot point of X scaling
                        Animation.RELATIVE_TO_SELF, 0.5f
                    ) // Pivot point of Y scaling
                    anim.fillAfter = true // Needed to keep the result of the animation
                    anim.duration = 300
                    sendButton!!.startAnimation(anim)
                }
                if (selectionList.contains(img)) {
                    selectionList.remove(img)
                    initaliseadapter!!.select(false, position)
                    mainImageAdapter!!.select(false, position)
                } else {
                    if ((options?.count ?: 1) <= selectionList.size) {
                        Toast.makeText(
                            this@Pix, String.format(
                                resources.getString(R.string.selection_limiter_pix), selectionList.size
                            ), Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    img?.position = (position)
                    selectionList.add(img)
                    initaliseadapter!!.select(true, position)
                    mainImageAdapter!!.select(true, position)
                }
                selection_check!!.visibility = View.GONE
                topbar!!.setBackgroundColor(colorPrimaryDark)
                selection_count!!.text = selectionList.size.toString() + " " + resources.getString(R.string.pix_selected)
                img_count!!.text = selectionList.size.toString()
                DrawableCompat.setTint(selection_back!!.drawable, Color.parseColor("#ffffff"))
            }
        }
    }
    private val video_counter_progress = 0

    /*public void open(final FragmentActivity context, final ActivityResultLauncher<Intent> launcher) {
        open(context, Options.init().setCount(1), launcher);
    }*/
    private fun hideScrollbar() {
        val transX = resources.getDimensionPixelSize(R.dimen.fastscroll_scrollbar_padding_end).toFloat()
        mScrollbarAnimator = mScrollbar!!.animate().translationX(transX).alpha(0f).setDuration(Constants.sScrollbarAnimDuration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mScrollbar!!.visibility = View.GONE
                    mScrollbarAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    mScrollbar!!.visibility = View.GONE
                    mScrollbarAnimator = null
                }
            })
    }

    fun returnObjects() {
        val list = ArrayList<String?>()
        for (i in selectionList) {
            list.add(i?.url)
            // Log.e("Pix images", "img " + i.getUrl());
        }
        val resultIntent = Intent()
        resultIntent.putStringArrayListExtra(IMAGE_RESULTS, list)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_lib)
        Utility.Companion.setupStatusBarHidden(this)
        Utility.Companion.hideStatusBar(this)
        initialize()
    }

    private fun initialize() {
        val params = window.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        Utility.Companion.getScreenSize(this)
        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
        try {
            options = intent.getSerializableExtra(OPTIONS) as Options?
        } catch (e: Exception) {
            e.printStackTrace()
        }
        maxVideoDuration = options?.videoDurationLimitinSeconds ?: (40 * 1000) //conversion in  milli seconds
        var modeText = R.string.pix_bottom_message_with_video
        if (options?.mode == Options.Mode.Picture) {
            modeText = R.string.pix_bottom_message_without_video
        } else if (options?.mode == Options.Mode.Video) {
            modeText = R.string.pix_bottom_message_with_only_video
        }
        (findViewById<View>(R.id.message_bottom) as TextView).setText(modeText)
        statusBarHeight = Utility.Companion.getStatusBarSizePort(this@Pix)
        requestedOrientation = options?.screenOrientation ?: Options.SCREEN_ORIENTATION_UNSPECIFIED
        colorPrimaryDark = ResourcesCompat.getColor(resources, R.color.colorPrimary, theme)
        previewView = findViewById(R.id.view_finder)
        outputDirectory = filesDir
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (options?.mode == Options.Mode.Picture) {
            //--camera.setAudio(Audio.OFF);
        }

        cameraSelector = if (options!!.isFrontfacing) {
            //--camera.setFacing(Facing.FRONT);
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            //--camera.setFacing(Facing.BACK);
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        zoom = 0.0f
        flash = findViewById(R.id.flash)
        zoomBar = findViewById(R.id.zoomBar)
        zoomView = findViewById(R.id.zoom)
        vZoom = findViewById(R.id.vZoom)
        clickme = findViewById(R.id.clickme)
        front = findViewById(R.id.front)
        topbar = findViewById(R.id.topbar)
        video_counter_progressbar = findViewById(R.id.video_pbr)
        selection_count = findViewById(R.id.selection_count)
        selection_back = findViewById(R.id.selection_back)
        selection_check = findViewById(R.id.selection_check)
        btnBack = findViewById(R.id.btn_back)
        selection_check?.visibility = if ((options?.count ?: 1) > 1) View.VISIBLE else View.GONE
        sendButton = findViewById(R.id.sendButton)
        img_count = findViewById(R.id.img_count)
        mBubbleView = findViewById(R.id.fastscroll_bubble)
        mHandleView = findViewById(R.id.fastscroll_handle)
        mScrollbar = findViewById(R.id.fastscroll_scrollbar)
        mScrollbar?.visibility = View.GONE
        mBubbleView?.visibility = View.GONE
        topButton = findViewById(R.id.topButtons)
        bottomButtons = findViewById(R.id.bottomButtons)
        TOPBAR_HEIGHT = Utility.convertDpToPixel(56f, this@Pix)
        status_bar_bg = findViewById(R.id.status_bar_bg)
        status_bar_bg?.layoutParams?.height = statusBarHeight
        status_bar_bg?.translationY = (-1 * statusBarHeight).toFloat()
        status_bar_bg?.requestLayout()
        instantRecyclerView = findViewById(R.id.instantRecyclerView)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        instantRecyclerView?.layoutManager = linearLayoutManager
        initaliseadapter = InstantImageAdapter(this)
        initaliseadapter?.addOnSelectionListener(onSelectionListener)
        instantRecyclerView?.adapter = initaliseadapter
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView?.addOnScrollListener(mScrollListener)
        val mainFrameLayout = findViewById<FrameLayout>(R.id.mainFrameLayout)
        val main_content = findViewById<CoordinatorLayout>(R.id.main_content)
        bottomBarHeight = Utility.Companion.getSoftButtonsBarSizePort(this)
        val lp1 = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        lp1.setMargins(0, statusBarHeight, 0, 0)
        main_content.layoutParams = lp1

        /*FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp2.setMargins(0, 0, 0, bottomBarHeight);
        lp2.gravity = Gravity.BOTTOM;
        bottomButtons.setLayoutParams(lp2);*/
        val lp3 = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        lp3.setMargins(0, statusBarHeight, 0, 0)
        topButton?.layoutParams = lp3
        val layoutParams = sendButton?.layoutParams as FrameLayout.LayoutParams
        layoutParams.setMargins(
            0, 0, Utility.convertDpToPixel(16f, this).toInt(), Utility.Companion.convertDpToPixel(174f, this).toInt()
        )
        sendButton?.layoutParams = layoutParams
        mainImageAdapter = options?.spanCount?.let { MainImageAdapter(this, it) }
        val mLayoutManager = GridLayoutManager(this, MainImageAdapter.Companion.SPAN_COUNT)
        mLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (mainImageAdapter!!.getItemViewType(position) == MainImageAdapter.Companion.HEADER) {
                    MainImageAdapter.Companion.SPAN_COUNT
                } else 1
            }
        }
        recyclerView?.layoutManager = mLayoutManager
        recyclerView?.setHasFixedSize(true)
        recyclerView?.setItemViewCacheSize(20)
        recyclerView?.isDrawingCacheEnabled = true
        recyclerView?.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        mainImageAdapter!!.addOnSelectionListener(onSelectionListener)
        mainImageAdapter!!.setHasStableIds(true)
        recyclerView?.adapter = mainImageAdapter
        recyclerView?.addItemDecoration(HeaderItemDecoration(this, mainImageAdapter!!))
        mHandleView?.setOnTouchListener(this)
        onClickMethods()
        flashDrawable = R.drawable.ic_flash_off_black_24dp
        if ((options?.preSelectedUrls?.size ?: 0) > (options?.count ?: 1)) {
            val large = options?.preSelectedUrls?.size?.minus(1)
            val small = options?.count ?: 1
            if (large != null) {
                for (i in large downTo small - 1 + 1) {
                    options?.preSelectedUrls?.removeAt(i)
                }
            }
        }
        selection_back?.drawable?.let { DrawableCompat.setTint(it, colorPrimaryDark) }
        updateImages()
        when (options?.type) {
            Options.Mode.Camera -> {
                startCamera()
                main_content.visibility = View.INVISIBLE
            }
            Options.Mode.Gallery -> bottomButtons?.visibility = View.INVISIBLE
            else -> startCamera()
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture!!.addListener({
            try {
                val cameraProvider = cameraProviderFuture!!.get()
                bindImageAnalysis(cameraProvider)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        if (imageCapture == null) return
        val imageCapture2 = imageCapture as ImageCapture

        // Create time-stamped output file to hold the image
        val photoFile = File(outputDirectory, "_pix" + SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture2.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                runOnUiThread {

                    /*File dir = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        dir = getExternalFilesDir(options.getPath());
                    } else {
                        dir = Environment.getExternalStoragePublicDirectory(options.getPath());
                    }

                    if (!dir.exists()) {
                        dir.mkdirs();
                    }*/Utility.Companion.vibe(this@Pix, 50)
                    val img = Img("", "", photoFile.absolutePath, "", 1)
                    selectionList.add(img)
                    //Log.e("result photo", "->" + photo.getAbsolutePath());
                    //Utility.scanPhoto(Pix.this, photoFile);
                    returnObjects()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(
                    this@Pix, String.format(resources.getString(R.string.pix_failed_take_photo), "" + exception.localizedMessage), Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun bindImageAnalysis(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
        preview.setSurfaceProvider(previewView!!.surfaceProvider)
        imageCapture = ImageCapture.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
        imageCapture!!.flashMode = cameraFlashMode
        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            //            final CameraInfo cameraInfo = camera.getCameraInfo();
//            final CameraControl cameraControl = camera.getCameraControl();
            val listener: SimpleOnScaleGestureListener = object : SimpleOnScaleGestureListener() {
                override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
                    Log.e("ratio", "Ratio ")
                    val currentZoomRatio = Objects.requireNonNull(camera.cameraInfo.zoomState.value)?.zoomRatio
                    // Get the pinch gesture's scaling factor
                    val delta = scaleGestureDetector.scaleFactor

                    // Update the camera's zoom ratio. This is an asynchronous operation that returns
                    // a ListenableFuture, allowing you to listen to when the operation completes.
                    val ratio = currentZoomRatio?.times(delta)
                    Log.e("ratio", "Ratio $ratio")
                    ratio?.let { camera.cameraControl.setZoomRatio(it) }
                    return true
                }

                override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
                    return true
                }

                override fun onScaleEnd(scaleGestureDetector: ScaleGestureDetector) {}
            }
            val detector = ScaleGestureDetector(this, listener)
            zoomView!!.setOnTouchListener { view: View?, motionEvent: MotionEvent? ->
                detector.onTouchEvent(motionEvent!!)
                true
            }
            zoomBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    camera.cameraControl.setLinearZoom(i / 100f)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
        /*ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                image.close();
            }
        });
        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                //textView.setText(Integer.toString(orientation));
            }
        };
        orientationEventListener.enable();
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);*/
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun onClickMethods() {
        /*clickme.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    findViewById(R.id.clickmebg).setVisibility(View.GONE);
                    findViewById(R.id.clickmebg).animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                    clickme.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    findViewById(R.id.clickmebg).setVisibility(View.VISIBLE);
                    findViewById(R.id.clickmebg).animate().scaleX(1.2f).scaleY(1.2f).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                    clickme.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                }
                if (event.getAction() == MotionEvent.ACTION_UP && camera.isTakingVideo()) {
                    camera.stopVideo();
                }

                return false;
            }
        });
        clickme.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                if (options.getMode() == Options.Mode.Picture) {
                    return false;
                }
                camera.setMode(Mode.VIDEO);
                File dir = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    dir = getExternalFilesDir(options.getPath());
                } else {
                    dir = Environment.getExternalStoragePublicDirectory(options.getPath());
                }
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File video = new File(dir, "VID_"
                        + new SimpleDateFormat("yyyyMMdd_HHmmSS", Locale.ENGLISH).format(new Date())
                        + ".mp4");
                video_counter_progressbar.setMax(maxVideoDuration / 1000);
                video_counter_progressbar.invalidate();
                camera.takeVideo(video, maxVideoDuration);
                return true;
            }
        });*/
        clickme?.setOnClickListener { view: View? ->
            if (selectionList.size >= (options?.count ?: 1)) {
                Toast.makeText(
                    this@Pix, String.format(resources.getString(R.string.cannot_click_image_pix), ("" + options?.count)), Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            /*if (camera.getMode() == Mode.VIDEO) {
                return;
            }*/if (options?.mode == Options.Mode.Video) {
            return@setOnClickListener
        }
            val oj = ObjectAnimator.ofFloat(previewView, "alpha", 1f, 0f, 0f, 1f)
            oj.startDelay = 200L
            oj.duration = 600L
            oj.start()
            //camera.takePicture();
            takePhoto()
        }
        findViewById<View>(R.id.selection_ok).setOnClickListener { view: View? -> returnObjects() }
        sendButton?.setOnClickListener { view: View? -> returnObjects() }
        selection_back?.setOnClickListener { view: View? -> mBottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED) }
        selection_check?.setOnClickListener { view: View? ->
            topbar?.setBackgroundColor(colorPrimaryDark)
            selection_count?.text = resources.getString(R.string.pix_tap_to_select)
            img_count?.text = selectionList.size.toString()
            DrawableCompat.setTint(selection_back!!.drawable, Color.parseColor("#ffffff"))
            LongSelection = true
            selection_check?.visibility = View.GONE
        }
        val iv = flash?.getChildAt(0) as ImageView
        flash?.setOnClickListener { view: View? ->
            val height = flash?.height ?: 0
            iv.animate().translationY(height.toFloat()).setDuration(100).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    iv.translationY = -(height / 2).toFloat()
                    if (flashDrawable == R.drawable.ic_flash_auto_black_24dp) {
                        flashDrawable = R.drawable.ic_flash_off_black_24dp
                        iv.setImageResource(flashDrawable)
                        //--camera.setFlash(Flash.OFF);
                        cameraFlashMode = ImageCapture.FLASH_MODE_OFF
                    } else if (flashDrawable == R.drawable.ic_flash_off_black_24dp) {
                        flashDrawable = R.drawable.ic_flash_on_black_24dp
                        iv.setImageResource(flashDrawable)
                        //--camera.setFlash(Flash.ON);
                        cameraFlashMode = ImageCapture.FLASH_MODE_ON
                    } else {
                        flashDrawable = R.drawable.ic_flash_auto_black_24dp
                        iv.setImageResource(flashDrawable)
                        //--camera.setFlash(Flash.AUTO);
                        cameraFlashMode = ImageCapture.FLASH_MODE_AUTO
                    }
                    startCamera()
                    iv.animate().translationY(0f).setDuration(50).setListener(null).start()
                }
            }).start()
        }
        front!!.setOnClickListener { view: View? ->
            zoomBar!!.progress = 0
            val oa1 = ObjectAnimator.ofFloat(front, "scaleX", 1f, 0f).setDuration(150)
            val oa2 = ObjectAnimator.ofFloat(front, "scaleX", 0f, 1f).setDuration(150)
            oa1.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    front!!.setImageResource(R.drawable.ic_photo_camera)
                    oa2.start()
                }
            })
            oa1.start()
            if (options!!.isFrontfacing) {
                options!!.isFrontfacing = false
                //--camera.setFacing(Facing.BACK);
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                options!!.isFrontfacing = true
                //--camera.setFacing(Facing.FRONT);
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            }
            startCamera()
        }
        btnBack!!.setOnClickListener { view: View? -> onBackPressed() }
    }

    private fun updateImages() {
        mainImageAdapter!!.clearList()
        val cursor: Cursor = Utility.Companion.getImageVideoCursor(this@Pix, true) ?: return
        val INSTANTLIST = ArrayList<Img>()
        var header = ""
        var limit = 100
        if (cursor.count < limit) {
            limit = cursor.count - 1
        }
        val data = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
        val mediaType = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val contentUrl = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
        //int videoDate = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN);
        val imageDate = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
        var calendar: Calendar
        var pos = 0
        for (i in 0 until limit) {
            cursor.moveToNext()
            val path = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + cursor.getInt(contentUrl))
            calendar = Calendar.getInstance()
            calendar.timeInMillis = cursor.getLong(imageDate) * 1000
            //Log.e("time",i+"->"+new SimpleDateFormat("hh:mm:ss dd/MM/yyyy",Locale.ENGLISH).format(calendar.getTime()));
            val dateDifference: String = Utility.Companion.getDateDifference(this@Pix, calendar)
            if (!header.equals("" + dateDifference, ignoreCase = true)) {
                header = "" + dateDifference
                pos += 1
                INSTANTLIST.add(Img("" + dateDifference, "", "", "", cursor.getInt(mediaType)))
            }
            val img = Img("" + header, "" + path, cursor.getString(data), "" + pos, cursor.getInt(mediaType))
            img.position = pos
            if (options?.preSelectedUrls?.contains(img.url) == true) {
                img.selected = true
                selectionList.add(img)
            }
            pos += 1
            INSTANTLIST.add(img)
        }
        if (selectionList.size > 0) {
            LongSelection = true
            sendButton!!.visibility = View.VISIBLE
            val anim: Animation = ScaleAnimation(
                0f, 1f,  // Start and end values for the X axis scaling
                0f, 1f,  // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f,  // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f
            ) // Pivot point of Y scaling
            anim.fillAfter = true // Needed to keep the result of the animation
            anim.duration = 300
            sendButton!!.startAnimation(anim)
            selection_check!!.visibility = View.GONE
            topbar!!.setBackgroundColor(colorPrimaryDark)
            selection_count!!.text = selectionList.size.toString() + " " + resources.getString(R.string.pix_selected)
            img_count!!.text = selectionList.size.toString()
            DrawableCompat.setTint(selection_back!!.drawable, Color.parseColor("#ffffff"))
        }
        mainImageAdapter?.addImageList(INSTANTLIST)
        initaliseadapter?.addImageList(INSTANTLIST)
        imageVideoFetcher = object : ImageVideoFetcher(this@Pix) {
            override fun onPostExecute(modelList: ModelList) {
                super.onPostExecute(modelList)
                mainImageAdapter?.addImageList(modelList.list)
                initaliseadapter?.addImageList(modelList.list)
                selectionList.addAll(modelList.selection)
                if (selectionList.size > 0) {
                    LongSelection = true
                    sendButton!!.visibility = View.VISIBLE
                    val anim: Animation = ScaleAnimation(
                        0f, 1f,  // Start and end values for the X axis scaling
                        0f, 1f,  // Start and end values for the Y axis scaling
                        Animation.RELATIVE_TO_SELF, 0.5f,  // Pivot point of X scaling
                        Animation.RELATIVE_TO_SELF, 0.5f
                    ) // Pivot point of Y scaling
                    anim.fillAfter = true // Needed to keep the result of the animation
                    anim.duration = 300
                    sendButton!!.startAnimation(anim)
                    selection_check!!.visibility = View.GONE
                    topbar!!.setBackgroundColor(colorPrimaryDark)
                    selection_count!!.text = selectionList.size.toString() + " " + resources.getString(R.string.pix_selected)
                    img_count!!.text = selectionList.size.toString()
                    DrawableCompat.setTint(selection_back!!.drawable, Color.parseColor("#ffffff"))
                }
            }
        }
        imageVideoFetcher?.startingCount = (pos)
        imageVideoFetcher?.header = header
        options?.preSelectedUrls?.let { imageVideoFetcher?.setPreSelectedUrls(it) }
        imageVideoFetcher?.execute(Utility.getImageVideoCursor(this@Pix, true)) //options.getMode()
        cursor.close()
        setBottomSheetBehavior()
    }

    private fun setBottomSheetBehavior() {
        val bottomSheet = findViewById<View>(R.id.bottom_sheet)
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        mBottomSheetBehavior?.setPeekHeight(Utility.Companion.convertDpToPixel(194f, this).toInt())
        mBottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (options!!.type == Options.Mode.Both) {
                    Utility.Companion.manipulateVisibility(
                        this@Pix,
                        slideOffset,
                        findViewById<View>(R.id.arrow_up),
                        instantRecyclerView,
                        recyclerView,
                        status_bar_bg,
                        topbar,
                        bottomButtons,
                        sendButton,
                        LongSelection
                    )
                }
                if (slideOffset == 1f) {
                    vZoom!!.visibility = View.GONE
                    Utility.Companion.showScrollbar(mScrollbar, this@Pix)
                    mainImageAdapter!!.notifyDataSetChanged()
                    mViewHeight = mScrollbar!!.measuredHeight.toFloat()
                    handler.post { setViewPositions(getScrollProportion(recyclerView)) }
                    sendButton!!.visibility = View.GONE
                    //  fotoapparat.stop();
                } else if (slideOffset == 0f) {
                    vZoom!!.visibility = View.VISIBLE
                    initaliseadapter!!.notifyDataSetChanged()
                    hideScrollbar()
                    img_count!!.text = selectionList.size.toString()
                    if (options!!.type == Options.Mode.Gallery) {
                        finish()
                    }
                    //--camera.open();
                }
            }
        })
        if (options!!.type == Options.Mode.Gallery) {
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            Utility.manipulateVisibility(
                this@Pix,
                1f,
                findViewById(R.id.arrow_up),
                instantRecyclerView,
                recyclerView,
                status_bar_bg,
                topbar,
                bottomButtons,
                sendButton,
                LongSelection
            )
            Utility.showScrollbar(mScrollbar, this@Pix)
            mainImageAdapter?.notifyDataSetChanged()
            mViewHeight = mScrollbar?.measuredHeight?.toFloat() ?: 0F
            handler.post { setViewPositions(getScrollProportion(recyclerView)) }
            sendButton!!.visibility = View.GONE
        }

        /*mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Utility.manipulateVisibility(Pix.this, slideOffset, findViewById(R.id.arrow_up),
                        instantRecyclerView, recyclerView, status_bar_bg,
                        topbar, bottomButtons, sendButton, LongSelection);
                if (slideOffset == 1) {
                    Utility.showScrollbar(mScrollbar, Pix.this);
                    mainImageAdapter.notifyDataSetChanged();
                    mViewHeight = mScrollbar.getMeasuredHeight();
                    handler.post(() -> setViewPositions(getScrollProportion(recyclerView)));
                    sendButton.setVisibility(View.GONE);
                    //  fotoapparat.stop();
                } else if (slideOffset == 0) {
                    initaliseadapter.notifyDataSetChanged();
                    hideScrollbar();
                    img_count.setText(String.valueOf(selectionList.size()));
                    //--camera.open();
                }
            }
        });*/
    }

    private fun getScrollProportion(recyclerView: RecyclerView?): Float {
        val verticalScrollOffset = recyclerView!!.computeVerticalScrollOffset()
        val verticalScrollRange = recyclerView.computeVerticalScrollRange()
        val rangeDiff = verticalScrollRange - mViewHeight
        val proportion = verticalScrollOffset.toFloat() / if (rangeDiff > 0) rangeDiff else 1f
        return mViewHeight * proportion
    }

    private fun setViewPositions(y: Float) {
        val handleY: Int = Utility.Companion.getValueInRange(0, (mViewHeight - mHandleView!!.height).toInt(), (y - mHandleView!!.height / 2).toInt())
        mBubbleView!!.y = handleY + Utility.Companion.convertDpToPixel(60f, this)
        mHandleView!!.y = handleY.toFloat()
    }

    private fun setRecyclerViewPosition(y: Float) {
        if (recyclerView != null && recyclerView!!.adapter != null) {
            val itemCount = recyclerView!!.adapter!!.itemCount
            val proportion: Float
            proportion = if (mHandleView!!.y == 0f) {
                0f
            } else if (mHandleView!!.y + mHandleView!!.height >= mViewHeight - sTrackSnapRange) {
                1f
            } else {
                y / mViewHeight
            }
            val scrolledItemCount = Math.round(proportion * itemCount)
            val targetPos: Int = Utility.Companion.getValueInRange(0, itemCount - 1, scrolledItemCount)
            recyclerView!!.layoutManager!!.scrollToPosition(targetPos)
            if (mainImageAdapter != null) {
                val text = mainImageAdapter!!.getSectionMonthYearText(targetPos)
                mBubbleView!!.text = text
                if (text.equals("", ignoreCase = true)) {
                    mBubbleView!!.visibility = View.GONE
                }
            }
        }
    }

    private fun showBubble() {
        if (!Utility.isViewVisible(mBubbleView)) {
            mBubbleView?.apply {
                visibility = View.VISIBLE
                alpha = 0f
                mBubbleAnimator = animate().alpha(1f).setDuration(sBubbleAnimDuration.toLong())
                    .setListener(object : AnimatorListenerAdapter() { // adapter required for new alpha value to stick
                    })
                mBubbleAnimator?.start()
            }
        }
    }

    private fun hideBubble() {
        if (Utility.Companion.isViewVisible(mBubbleView)) {
            mBubbleAnimator =
                mBubbleView!!.animate().alpha(0f).setDuration(sBubbleAnimDuration.toLong()).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        mBubbleView!!.visibility = View.GONE
                        mBubbleAnimator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        super.onAnimationCancel(animation)
                        mBubbleView!!.visibility = View.GONE
                        mBubbleAnimator = null
                    }
                })
            mBubbleAnimator!!.start()
        }
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x < (mHandleView?.x?.minus(ViewCompat.getPaddingStart(mHandleView!!)) ?: 0F)) {
                    return false
                }
                mHandleView?.isSelected = true
                handler.removeCallbacks(mScrollbarHider)
                Utility.cancelAnimation(mScrollbarAnimator)
                Utility.cancelAnimation(mBubbleAnimator)
                if (!Utility.isViewVisible(mScrollbar) && (recyclerView!!.computeVerticalScrollRange() - mViewHeight > 0)) {
                    mScrollbarAnimator = Utility.showScrollbar(mScrollbar, this@Pix)
                }
                if (mainImageAdapter != null) {
                    showBubble()
                }
                mFastScrollStateChangeListener?.onFastScrollStart(this)
                val y = event.rawY
                setViewPositions(y - TOPBAR_HEIGHT)
                setRecyclerViewPosition(y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val y = event.rawY
                setViewPositions(y - TOPBAR_HEIGHT)
                setRecyclerViewPosition(y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mHandleView!!.isSelected = false
                if (mHideScrollbar) {
                    handler.postDelayed(mScrollbarHider, sScrollbarHideDelay.toLong())
                }
                hideBubble()
                mFastScrollStateChangeListener?.onFastScrollStop(this)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onBackPressed() {
        if (selectionList.isNotEmpty()) {
            for (img in selectionList) {
                options!!.preSelectedUrls = ArrayList()
                mainImageAdapter?.itemList?.get(img?.position ?: 0)?.selected = false
                mainImageAdapter!!.notifyItemChanged(img?.position ?: 0)
                initaliseadapter?.itemList?.get(img?.position ?: 0)?.selected = false
                img?.position?.let { initaliseadapter!!.notifyItemChanged(it) }
            }
            LongSelection = false
            if ((options?.count ?: 1) > 1) {
                selection_check?.visibility = View.VISIBLE
            }
            selection_back?.drawable?.let { DrawableCompat.setTint(it, colorPrimaryDark) }
            topbar?.setBackgroundColor(Color.parseColor("#ffffff"))
            val anim: Animation = ScaleAnimation(
                1f, 0f,  // Start and end values for the X axis scaling
                1f, 0f,  // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f,  // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f
            ) // Pivot point of Y scaling
            anim.fillAfter = true // Needed to keep the result of the animation
            anim.duration = 300
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    sendButton?.visibility = View.GONE
                    sendButton?.clearAnimation()
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
            sendButton?.startAnimation(anim)
            selectionList.clear()
        } else if (mBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior?.state = (BottomSheetBehavior.STATE_COLLAPSED)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        cameraExecutor!!.shutdown()
        //--camera.destroy();
        super.onDestroy()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_AND_STORAGE_PERMISSION) {
            val deniededPermissions = mutableListOf<String>()
            for (perm in permissions) {
                for (i in grantResults.indices) {
                    Log.e("permissionCheck", perm)
                    Log.e("permissionCheck", "granted? : ${grantResults[i] == PackageManager.PERMISSION_GRANTED}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (perm == Manifest.permission.CAMERA && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            deniededPermissions.add(perm)
                            break
                        }
                        if (perm == Manifest.permission.READ_MEDIA_IMAGES && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            deniededPermissions.add(perm)
                            break
                        }
                    } else {
                        if (perm == Manifest.permission.CAMERA && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            deniededPermissions.add(perm)
                            break
                        }
                        if (perm == Manifest.permission.READ_EXTERNAL_STORAGE && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            deniededPermissions.add(perm)
                            break
                        }
                    }
                }
            }

            if (deniededPermissions.isNotEmpty()) {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
            } else {
                when (flag) {
                    usingActivity -> {
                        val i = Intent(contextActivity, Pix::class.java)
                        i.putExtra(OPTIONS, options)
                        launcherActivity?.launch(i)
                    }
                    usingFragment -> {
                        val i = Intent(contextFragment?.requireContext(), Pix::class.java)
                        i.putExtra(OPTIONS, options)
                        contextFragment?.startActivityForResult(i, options?.requestCode ?: 0)
                    }
                    usingFragmentActivity -> {
                        val i = Intent(contextFragmentActivity, Pix::class.java)
                        i.putExtra(OPTIONS, options)
                        contextFragmentActivity?.startActivityForResult(i, options?.requestCode ?: 0)
                    }
                }
            }
        }
    }

    companion object {
        var REQUEST_CAMERA_AND_STORAGE_PERMISSION = 1000
        private const val sBubbleAnimDuration = 1000
        private const val sScrollbarHideDelay = 1000
        private const val OPTIONS = "options"
        private const val sTrackSnapRange = 5
        private const val usingFragment = 50
        private const val usingFragmentActivity = 500
        private const val usingActivity = 5000
        private var launcherActivity: ActivityResultLauncher<Intent?>? = null
        private var contextActivity: Activity? = null
        private var contextFragment: Fragment? = null
        private var contextFragmentActivity: FragmentActivity? = null
        var flag = 0
        var IMAGE_RESULTS = "image_results"
        var TOPBAR_HEIGHT = 0f
        private var maxVideoDuration = 40000
        private var imageVideoFetcher: ImageVideoFetcher? = null
        fun start(context: Fragment, options: Options) {
            flag = usingFragment
            contextFragment = context
            PermUtil.checkForCamaraWritePermissionsFragment(context, options.mode) { check: Boolean? ->
                val i = Intent(context.activity, Pix::class.java)
                i.putExtra(OPTIONS, options)
                context.startActivityForResult(i, options.requestCode)
            }
        }

        fun start(context: Fragment, requestCode: Int) {
            start(context, Options.Companion.init().setRequestCode(requestCode).setCount(1))
        }

        fun start(context: FragmentActivity, options: Options) {
            flag = usingFragmentActivity
            contextFragmentActivity = context
            val permissionList = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.CAMERA)
            }

            val readImagePermission =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, readImagePermission) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(readImagePermission)
            }

            if (permissionList.isNotEmpty()) {
//                Toast.makeText(context, "permissionList not empty", Toast.LENGTH_SHORT).show()
                for (data in permissionList) {
                    Log.e("permissionList", data)
                }
                context.requestPermissions(
                    permissionList.toTypedArray(), REQUEST_CAMERA_AND_STORAGE_PERMISSION
                )
            } else {
                val i = Intent(context, Pix::class.java)
//                Toast.makeText(context, "permissionList empty", Toast.LENGTH_SHORT).show()
                i.putExtra(OPTIONS, options)
                context.startActivityForResult(i, options.requestCode)
            }
        }

        fun start(context: FragmentActivity, requestCode: Int) {
            start(context, Options.init().setRequestCode(requestCode).setCount(1))
        }

        fun open(context: Activity, options: Options, launcher: ActivityResultLauncher<Intent?>) {
            flag = usingActivity
            contextActivity = context
            launcherActivity = launcher
            val permissionList = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.CAMERA)
            }

            val readImagePermission =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, readImagePermission) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(readImagePermission)
            }

            if (permissionList.isNotEmpty()) {
//                Toast.makeText(context, "permissionList not empty", Toast.LENGTH_SHORT).show()
                for (data in permissionList) {
                    Log.e("permissionList", data)
                }
                context.requestPermissions(
                    permissionList.toTypedArray(), REQUEST_CAMERA_AND_STORAGE_PERMISSION
                )
            } else {
//                Toast.makeText(context, "permissionList empty", Toast.LENGTH_SHORT).show()
                val i = Intent(context, Pix::class.java)
                i.putExtra(OPTIONS, options)
                launcherActivity?.launch(i)
            }
        }

        fun open(context: FragmentActivity, type: Options.Mode, launcher: ActivityResultLauncher<Intent?>) {
            open(context, Options.init().setCount(1).setType(type), launcher)
        }
    }
}