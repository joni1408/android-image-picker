package com.esafirm.imagepicker.features;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.esafirm.imagepicker.R;
import com.esafirm.imagepicker.features.camera.DefaultCameraModule;
import com.esafirm.imagepicker.features.common.BasePresenter;
import com.esafirm.imagepicker.features.common.ImageLoaderListener;
import com.esafirm.imagepicker.model.Folder;
import com.esafirm.imagepicker.model.Image;

import java.io.File;
import java.util.List;

class ImagePickerPresenter extends BasePresenter<ImagePickerView> {

    private ImageFileLoader imageLoader;
    private DefaultCameraModule cameraModule;
    private Handler main = new Handler(Looper.getMainLooper());

    ImagePickerPresenter(ImageFileLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    DefaultCameraModule getCameraModule() {
        return cameraModule;
    }

    /* Camera Module is late initiated */
    void setCameraModule(DefaultCameraModule cameraModule) {
        this.cameraModule = cameraModule;
    }

    void abortLoad() {
        imageLoader.abortLoadImages();
    }

    void loadImages(boolean isFolderMode) {
        if (!isViewAttached()) return;

        runOnUiIfAvailable(() -> getView().showLoading(true));

        imageLoader.loadDeviceImages(isFolderMode, new ImageLoaderListener() {
            @Override
            public void onImageLoaded(final List<Image> images, final List<Folder> folders) {
                runOnUiIfAvailable(() -> {
                    getView().showFetchCompleted(images, folders);

                    final boolean isEmpty = folders != null
                            ? folders.isEmpty()
                            : images.isEmpty();

                    if (isEmpty) {
                        getView().showEmpty();
                    } else {
                        getView().showLoading(false);
                    }
                });
            }

            @Override
            public void onFailed(final Throwable throwable) {
                runOnUiIfAvailable(() -> getView().showError(throwable));
            }
        });
    }

    void onDoneSelectImages(List<Image> selectedImages) {
        if (selectedImages != null && selectedImages.size() > 0) {

            /* Scan selected images which not existed */
            for (int i = 0; i < selectedImages.size(); i++) {
                Image image = selectedImages.get(i);
                File file = new File(image.getPath());
                if (!file.exists()) {
                    selectedImages.remove(i);
                    i--;
                }
            }
            getView().finishPickImages(selectedImages);
        }
    }

    void captureImage(Activity activity, ImagePickerConfig config, int requestCode) {
        if (cameraModule == null) {
            throw new IllegalStateException("Camera Module must be set first");
        }

        Context context = activity.getApplicationContext();
        Intent intent = cameraModule.getCameraIntent(activity, config);
        if (intent == null) {
            Toast.makeText(context, context.getString(R.string.ef_error_create_image_file), Toast.LENGTH_LONG).show();
            return;
        }
        activity.startActivityForResult(intent, requestCode);
    }

    void finishCaptureImage(Context context, Intent data, final ImagePickerConfig config) {
        if (cameraModule == null) {
            throw new IllegalStateException("Camera Module must be set first");
        }

        cameraModule.getImage(context, data, images -> {
            if (config.isReturnAfterFirst()) {
                getView().finishPickImages(images);
            } else {
                getView().showCapturedImage();
            }
        });
    }

    private void runOnUiIfAvailable(Runnable runnable) {
        main.post(() -> {
            if (isViewAttached()) {
                runnable.run();
            }
        });
    }
}
