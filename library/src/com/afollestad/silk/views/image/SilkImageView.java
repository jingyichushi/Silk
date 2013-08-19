package com.afollestad.silk.views.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.afollestad.silk.images.Dimension;
import com.afollestad.silk.images.SilkImageManager;
import com.afollestad.silk.images.SilkImageUtils;

public class SilkImageView extends ImageView {

    private String source;
    private SilkImageManager aimage;
    protected boolean invalidateOnLoad;
    private boolean fitView = true;
    protected String lastSource;
    private View loadingView;

    public SilkImageView(Context context) {
        super(context);
    }

    public SilkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SilkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        /**
         * This method allows the view to wait until it has been measured (a view won't be measured until
         * right before it becomes visible, which is usually after your code first starts executing. This
         * insures that correct dimensions will be used for the image loading size to optimize memory.
         */
        super.onSizeChanged(w, h, oldw, oldh);
        loadFromSource();
    }


    public void setImageURL(SilkImageManager manager, String url) {
        if (manager == null) {
            throw new IllegalArgumentException("The SilkImageManager cannot be null.");
        }
        this.aimage = manager;
        this.source = url;
        loadFromSource();
    }

    /**
     * Turned on by default as it prevents OutOfMemoryExceptions. Sets whether or not the loaded image will be
     * resized to fit the dimensions of the view.
     */
    public SilkImageView setFitView(boolean fitView) {
        this.fitView = fitView;
        return this;
    }

    /**
     * Sets the view that will become visible when the view begins loading an image, and will be hidden when the
     * view finishes loading an image. The imageview itself will also be hidden during loading if a loading view is set.
     */
    public SilkImageView setLoadingView(View view) {
        this.loadingView = view;
        return this;
    }

    /**
     * Loads the fallback image set from the {@link com.afollestad.silk.images.SilkImageManager} set via #setManager.
     */
    public void showFallback() {
        if (aimage == null)
            throw new IllegalStateException("You cannot load the fallback image until you have set a SilkImageManager via setManager().");
        if (aimage.isDebugEnabled())
            Log.i("SilkImageView", "Loading fallback image for view...");
        aimage.get(SilkImageManager.SOURCE_FALLBACK, new SilkImageManager.ImageListener() {
            @Override
            public void onImageReceived(final String source, final Bitmap bitmap) {
                setImageBitmap(bitmap);
                if (invalidateOnLoad) {
                    requestLayout();
                    invalidate();
                }
                if (aimage.isDebugEnabled())
                    Log.i("SilkImageView", "Fallback image set to view.");
            }
        }, new Dimension(this));
    }


    private void loadFromSource() {
        if (aimage == null) {
            return;
        } else if (source == null || source.trim().isEmpty()) {
            showFallback();
            return;
        } else if (getMeasuredWidth() == 0 && getMeasuredHeight() == 0) {
            if (aimage.isDebugEnabled())
                Log.i("SilkImageView", "View not measured yet, waiting...");
            return;
        }

        lastSource = source;
        final Dimension dimen = this.fitView ? new Dimension(this) : null;
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
            this.setVisibility(View.GONE);
        }
        aimage.get(this.source, new SilkImageManager.ImageListener() {
            @Override
            public void onImageReceived(final String source, final Bitmap bitmap) {
                if (lastSource != null && !lastSource.equals(source)) {
                    if (aimage.isDebugEnabled())
                        Log.i("SilkImageView", "View source changed since download started, not setting " + source + " to view.");
                    return;
                }

                // Post on the view's UI thread to be 100% sure we're on the right thread
                SilkImageView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        setImageBitmap(bitmap);
                        if (invalidateOnLoad) {
                            requestLayout();
                            invalidate();
                        }
                        if (loadingView != null) {
                            loadingView.setVisibility(View.GONE);
                            SilkImageView.this.setVisibility(View.VISIBLE);
                        }
                        if (aimage.isDebugEnabled())
                            Log.i("SilkImageView", source + " set to view " + SilkImageUtils.getKey(source, dimen));
                    }
                });
            }
        }, dimen);
    }
}