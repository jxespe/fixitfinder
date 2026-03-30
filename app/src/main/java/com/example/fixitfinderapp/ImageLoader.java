package com.example.fixitfinderapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.URL;

public final class ImageLoader {

    private ImageLoader() {
    }

    /**
     * Loads into a circular clip (profile photos, avatars, provider logos in lists).
     */
    public static void loadProfile(ImageView view, String uriString, int placeholderResId) {
        load(view, uriString, placeholderResId, true);
    }

    /**
     * Loads without circular clipping (service thumbnails, chat attachments, etc.).
     */
    public static void load(ImageView view, String uriString, int placeholderResId) {
        load(view, uriString, placeholderResId, false);
    }

    private static void load(ImageView view, String uriString, int placeholderResId,
                             boolean circular) {
        if (view == null) {
            return;
        }
        if (TextUtils.isEmpty(uriString)) {
            if (placeholderResId != 0) {
                view.setImageResource(placeholderResId);
            }
            if (circular) {
                applyCircularClip(view);
            }
            return;
        }
        view.setTag(uriString);
        Uri uri = Uri.parse(uriString);
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            new Thread(() -> {
                try (InputStream input = new URL(uriString).openStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap != null) {
                        view.post(() -> {
                            Object tag = view.getTag();
                            if (uriString.equals(tag)) {
                                view.setImageBitmap(bitmap);
                                if (circular) {
                                    applyCircularClip(view);
                                }
                            }
                        });
                    }
                } catch (Exception ignored) {
                    // Best-effort image load; keep placeholder if any.
                }
            }).start();
        } else {
            view.setImageURI(uri);
            if (circular) {
                applyCircularClip(view);
            }
        }
        if (circular) {
            applyCircularClip(view);
        }
    }

    private static void applyCircularClip(ImageView view) {
        if (view == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
                int w = v.getWidth();
                int h = v.getHeight();
                if (w <= 0 || h <= 0) {
                    return;
                }
                outline.setOval(0, 0, w, h);
            }
        });
        view.setClipToOutline(true);
        view.post(() -> {
            if (view.getWidth() > 0 && view.getHeight() > 0) {
                view.invalidateOutline();
            }
        });
    }
}
