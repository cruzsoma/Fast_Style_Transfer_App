package cruzsoma.ai.cnn.fast_style_transfer_app;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Trace;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.support.annotation.DrawableRes;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.graphics.Matrix;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.support.annotation.NonNull;

import com.camerakit.CameraKit;
import com.camerakit.CameraKitView;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CameraActivity extends AppCompatActivity {

    private CameraKitView cameraKitView;
    private RecyclerView recyclerView;
    private ImageView transferResult;
    private ImageView facingButton;
    private ImageView flashButton;
    private ImageView captureButton;

    private View styleSplitor;
    private FrameLayout styleSplitorLayout;

    private Button styleSplitButton;
    private Button refreshButton;

    private LinearLayout imageOverlayLinearLayout;
    private RelativeLayout imageOverlayLayout;
    private ImageView pictureLeft;
    private ImageView pictureRight;

    private String selectedModel;

    private Bitmap captureImageBitMap;
    private Bitmap stylizedImageBitMap;

    TensorFlowInferenceInterface tensorFlowInferenceInterface;

    private static final int INPUT_IMAGE_SIZE = 256;
    private static final int INPUT_IMAGE_SIZE_WIDTH = 256;
    private static final int INPUT_IMAGE_SIZE_HEIGHT = 256;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output_new";

    private int[] imageIntValues;
    private float[] imageFloatValues;

    private int[] resIds = {R.drawable.starry, R.drawable.ink, R.drawable.mosaic, R.drawable.udnie, R.drawable.wave, R.drawable.cubist, R.drawable.feathers};

    private String[] modelNameList = {"starry", "ink", "mosaic", "udnie", "wave", "cubist", "feathers"};


    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    //    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
//            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    //    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    protected ArrayList<Model> initModelsConfig() {
        ArrayList<Model> models = new ArrayList<>();
        for (int i = 0; i < modelNameList.length; i++) {
            Model model = new Model();
            model.type = i;
            model.iconRes = resIds[i];
            model.modelName = modelNameList[i];
            models.add(model);
        }
        return models;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mVisible = true;

        selectedModel = "";

        facingButton = findViewById(R.id.facingButton);
        flashButton = findViewById(R.id.flashButton);
        captureButton = findViewById(R.id.captureButton);

        styleSplitButton = findViewById(R.id.styleSplitButton);
        styleSplitButton.setVisibility(View.INVISIBLE);
        refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setVisibility(View.INVISIBLE);

        styleSplitor = findViewById(R.id.styleSplitor);
        styleSplitor.setBackgroundColor(Color.YELLOW);
        styleSplitorLayout = findViewById(R.id.styleSplitorLayout);

        pictureLeft = findViewById(R.id.pictureLeft);
        pictureRight = findViewById(R.id.pictureRight);
        imageOverlayLayout = findViewById(R.id.imageOverlayLayout);
        imageOverlayLinearLayout = findViewById(R.id.imageOverlayLinearLayout);
        imageOverlayLinearLayout.setVisibility(View.INVISIBLE);

        cameraKitView = findViewById(R.id.camera);
        transferResult = (ImageView) findViewById(R.id.transferResult);
        recyclerView = (RecyclerView) findViewById(R.id.styleRecycleView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        imageIntValues = new int[INPUT_IMAGE_SIZE_WIDTH * INPUT_IMAGE_SIZE_HEIGHT];
        imageFloatValues = new float[INPUT_IMAGE_SIZE_WIDTH * INPUT_IMAGE_SIZE_HEIGHT * 3];

        ArrayList<Model> modelsConfig = initModelsConfig();
        StyleButtonAdapter styleButtonAdapter = new StyleButtonAdapter(CameraActivity.this, modelsConfig);
        recyclerView.setAdapter(styleButtonAdapter);

        cameraKitView.onStart();

        facingButton.setOnTouchListener(facingButtonTouchListener);
        flashButton.setOnTouchListener(flashButtonTouchListener);
        captureButton.setOnTouchListener(captureButtonTouchListener);

        styleSplitorLayout.setOnTouchListener(styleSplitorTouchListener);
        styleSplitButton.setOnTouchListener(styleSpiltButtonTouchListener);
        refreshButton.setOnTouchListener(refreshButtonTouchListener);

        styleButtonAdapter.buttonSetOnclick(new StyleButtonAdapter.ButtonInterface() {
            @Override
            public void onclick(View view, Model model) {
                Toast toast = Toast.makeText(CameraActivity.this, "Selected Style:" + model.modelName, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM, 0, 350);
                toast.show();
                selectedModel = model.modelName;
            }
        });
    }

    private void cropPictureLeft() {
        float x = styleSplitorLayout.getX() + styleSplitor.getX();
        float rate = x / imageOverlayLayout.getWidth();
        int imageWidth = captureImageBitMap.getWidth();
        int imageHeight = captureImageBitMap.getHeight();
        int width = (int) (imageWidth * rate);
        if (width <= 0) {
            pictureLeft.setLayoutParams(new RelativeLayout.LayoutParams(0, pictureLeft.getHeight()));
        } else {
            Bitmap bitmap = Bitmap.createBitmap(captureImageBitMap, 0, 0, width, imageHeight);
            pictureLeft.setLayoutParams(new RelativeLayout.LayoutParams((int) x, pictureLeft.getHeight()));
            pictureLeft.setImageBitmap(bitmap);
        }
    }

    private View.OnTouchListener styleSplitorTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    params.leftMargin = params.leftMargin + (int) motionEvent.getX();
                    view.setLayoutParams(params);
                    cropPictureLeft();
                    break;

                case MotionEvent.ACTION_UP:
                    params.leftMargin = params.leftMargin + (int) motionEvent.getX();
                    view.setLayoutParams(params);
                    break;
            }
            return true;
        }
    };

    private View.OnTouchListener styleSpiltButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_UP: {
                    if (imageOverlayLinearLayout.getVisibility() == View.VISIBLE) {
                        transferResult.setVisibility(View.VISIBLE);
                        imageOverlayLinearLayout.setVisibility(View.INVISIBLE);
                    } else {
                        pictureRight.setImageBitmap(stylizedImageBitMap);

                        styleSplitorLayout.setX(imageOverlayLayout.getWidth() / 2);
                        cropPictureLeft();
                        imageOverlayLinearLayout.setVisibility(View.VISIBLE);
                        transferResult.setVisibility(View.INVISIBLE);
                    }
                    break;
                }
            }
            return true;
        }
    };

    private View.OnTouchListener refreshButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_UP: {
                    transferResult.setVisibility(View.INVISIBLE);
                    styleSplitButton.setVisibility(View.INVISIBLE);
                    refreshButton.setVisibility(View.INVISIBLE);
                    imageOverlayLinearLayout.setVisibility(View.INVISIBLE);
                    break;
                }
            }
            return true;
        }
    };

    private View.OnTouchListener facingButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            handleViewTouchFeedback(view, motionEvent);
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_UP: {
                    if (cameraKitView.getFacing() == CameraKit.FACING_FRONT) {
                        cameraKitView.setFacing(CameraKit.FACING_BACK);
                        changeViewImageResource((ImageView) view, R.drawable.ic_facing_front);
                    } else {
                        cameraKitView.setFacing(CameraKit.FACING_FRONT);
                        changeViewImageResource((ImageView) view, R.drawable.ic_facing_back);
                    }
                    break;
                }
            }
            return true;
        }
    };

    private View.OnTouchListener flashButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            handleViewTouchFeedback(view, motionEvent);
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_UP: {
                    if (cameraKitView.getFlash() == CameraKit.FLASH_OFF) {
                        cameraKitView.setFlash(CameraKit.FLASH_ON);
                        changeViewImageResource((ImageView) view, R.drawable.ic_flash_on);
                    } else {
                        cameraKitView.setFlash(CameraKit.FLASH_OFF);
                        changeViewImageResource((ImageView) view, R.drawable.ic_flash_off);
                    }
                    break;
                }
            }
            return true;
        }
    };

    private View.OnTouchListener captureButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            handleViewTouchFeedback(view, motionEvent);
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if (selectedModel.isEmpty()) {
                        Toast toast = Toast.makeText(CameraActivity.this, "Select One Style.", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 350);
                        toast.show();
                        return false;
                    }
                    Toast toast = Toast.makeText(CameraActivity.this, "Style Transferring...", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM, 0, 350);
                    toast.show();
                    cameraKitView.captureImage(new CameraKitView.ImageCallback() {
                        @Override
                        public void onImage(CameraKitView cameraKitView, final byte[] photo) {
                            final Matrix matrix = new Matrix();
                            matrix.postScale(1f, 1f);

                            String modelFile = "file:///android_asset/" + selectedModel + ".pb";
                            tensorFlowInferenceInterface = new TensorFlowInferenceInterface(getAssets(), modelFile);

                            Bitmap bitmap0 = BitmapFactory.decodeByteArray(photo, 0, photo.length);
                            Bitmap bitmap = Bitmap.createScaledBitmap(bitmap0, INPUT_IMAGE_SIZE_WIDTH, INPUT_IMAGE_SIZE_HEIGHT, false);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                            captureImageBitMap = Bitmap.createScaledBitmap(bitmap0, cameraKitView.getWidth(), cameraKitView.getHeight(), false);
                            stylizedImageBitMap = imageStyleTransfer(bitmap);

                            transferResult.setImageBitmap(stylizedImageBitMap);
                            transferResult.setVisibility(View.VISIBLE);

                            styleSplitButton.setVisibility(View.VISIBLE);
                            refreshButton.setVisibility(View.VISIBLE);

                            imageOverlayLinearLayout.setVisibility(View.INVISIBLE);
                        }
                    });
                    break;
                }
            }
            return true;
        }
    };

    void changeViewImageResource(final ImageView imageView, @DrawableRes final int resId) {
        imageView.setRotation(0);
        imageView.animate()
                .rotationBy(360)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator())
                .start();

        imageView.postDelayed(new Runnable() {
            @Override
            public void run() {
                imageView.setImageResource(resId);
            }
        }, 120);
    }

    boolean handleViewTouchFeedback(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                touchDownAnimation(view);
                return true;
            }

            case MotionEvent.ACTION_UP: {
                touchUpAnimation(view);
                return true;
            }

            default: {
                return true;
            }
        }
    }

    void touchDownAnimation(View view) {
        view.animate()
                .scaleX(0.88f)
                .scaleY(0.88f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    void touchUpAnimation(View view) {
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }


    private Bitmap scaleBitmap(Bitmap input, int width, int height) {
        if (input == null) {
            return null;
        }

        int inputWidth = input.getWidth();
        int inputHeight = input.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(((float) width) / inputWidth, ((float) height) / inputHeight);
        Bitmap scaledBitmap = Bitmap.createBitmap(input, 0, 0, inputWidth, inputHeight, matrix, false);
//        if (!input.isRecycled()) {
//            input.recycle();
//        }
        return scaledBitmap;
    }

    private Bitmap imageStyleTransfer(Bitmap bitmap) {
        Bitmap scaledBitmap = scaleBitmap(bitmap, INPUT_IMAGE_SIZE_WIDTH, INPUT_IMAGE_SIZE_HEIGHT);
        scaledBitmap.getPixels(imageIntValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());

        for (int i = 0; i < imageIntValues.length; ++i) {
            final int val = imageIntValues[i];
            imageFloatValues[i * 3 + 0] = ((val >> 16) & 0xFF) * 1.0f;
            imageFloatValues[i * 3 + 1] = ((val >> 8) & 0xFF) * 1.0f;
            imageFloatValues[i * 3 + 2] = (val & 0xFF) * 1.0f;
        }

        Trace.beginSection("feed");
        tensorFlowInferenceInterface.feed(INPUT_NAME, imageFloatValues, INPUT_IMAGE_SIZE_WIDTH, INPUT_IMAGE_SIZE_HEIGHT, 3);
        Trace.endSection();

        Trace.beginSection("run");
        tensorFlowInferenceInterface.run(new String[]{OUTPUT_NAME});
        Trace.endSection();

        Trace.beginSection("fetch");
        tensorFlowInferenceInterface.fetch(OUTPUT_NAME, imageFloatValues);
        Trace.endSection();

        for (int i = 0; i < imageIntValues.length; ++i) {
            imageIntValues[i] =
                    0xFF000000
                            | (((int) (imageFloatValues[i * 3 + 0])) << 16)
                            | (((int) (imageFloatValues[i * 3 + 1])) << 8)
                            | ((int) (imageFloatValues[i * 3 + 2]));
        }
        scaledBitmap.setPixels(imageIntValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());

        return scaledBitmap;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.s
        delayedHide(100);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraKitView.onResume();
    }

    @Override
    protected void onPause() {
        cameraKitView.onPause();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
