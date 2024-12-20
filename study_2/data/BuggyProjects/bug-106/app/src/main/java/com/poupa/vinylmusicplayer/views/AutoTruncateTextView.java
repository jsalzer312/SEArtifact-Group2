package com.poupa.vinylmusicplayer.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;

import com.poupa.vinylmusicplayer.R;

/**
 * @author Lincoln (theduffmaster)
 *
 * TextView that automatically does exactly what android:ellipsize="end" does, except this works in
 * a {@link TouchInterceptHorizontalScrollView}.
 * Truncates the string so it doesn't get cuttoff in the {@link TouchInterceptHorizontalScrollView}
 * and puts an ellipsis at the end of it.
 * Must be used within a {@link TouchInterceptHorizontalScrollView}.
 */
public class AutoTruncateTextView extends AppCompatTextView {

    public static final String TAG = AutoTruncateTextView.class.getSimpleName();

    private static final int RETRUNCATE_DELAY = 600;

    // Invisible character used as a marker indicating whether a string has undergone truncation
    private static final String TRUNCATED_MARKER = "\u202F";

    // Invisible character used as a marker indicating whether a string is untruncated
    private static final String MARKER_UNTRUNCATED = "\uFEFF";

    private String text;

    public AutoTruncateTextView(Context context) {
        super(context);
        init();
    }

    public AutoTruncateTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutoTruncateTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setTag(AutoTruncateTextView.TAG);

        // Enable long clicking when touching the text
        setLongClickable(true);

        // Blocks clicks from passing through this view
        setClickable(true);

        // Have to use this instead of maxlines in order for scrolling to work
        setSingleLine();
    }

    /**
     * @return Returns the {@link TouchInterceptFrameLayout} inside this layout.
     * We need to find it recursively or we may return the first one shown on the screen but
     * not the one tapped
     */
    public TouchInterceptFrameLayout getTouchInterceptFrameLayout() {
        return (TouchInterceptFrameLayout) findParentRecursively(this, R.id.touch_intercept_framelayout);
    }

    /**
     * @return Returns the {@link TouchInterceptFrameLayout} inside this layout.
     * Get it using TAG for Android 6 and below
     */
    public TouchInterceptFrameLayout getTouchInterceptFrameLayoutByTag() {
        return (TouchInterceptFrameLayout) getRootView().findViewWithTag(TouchInterceptFrameLayout.TAG);
    }

    /**
     * Find the view parent recursively
     * @param view
     * @param targetId
     * @return
     */
    public ViewParent findParentRecursively(View view, int targetId) {
        if (view.getId() == targetId) {
            return (ViewParent)view;
        }
        ViewParent viewParent = view.getParent();
        if (viewParent instanceof View) {
            View parent = (View) view.getParent();
            if (parent == null) {
                return null;
            }
            return findParentRecursively(parent, targetId);
        } else {
            return null;
        }
    }

    /**
     * @return Returns the parent {@link TouchInterceptHorizontalScrollView}.
     */
    public TouchInterceptHorizontalScrollView getTouchInterceptHorizontalScrollView() {
        return (TouchInterceptHorizontalScrollView) getParent();
    }

    /**
     * The text undergoes truncation here. This is immediately called after {@link #setText} and has
     * a reference to the parent's bounds. The bounds are used for setting the length of the
     * truncated text, ensuring that the text does not get visibly cut off.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        String fittedText = getText().toString();

        if (!fittedText.equals("")) {
            int textBoundsWidth = MeasureSpec.getSize(widthMeasureSpec);
            final boolean isUntruncated = fittedText.endsWith(MARKER_UNTRUNCATED);

            // If getSize return 0 (Android <= 6), get it from the layout
            if (textBoundsWidth == 0) {
                textBoundsWidth = getTouchInterceptFrameLayoutByTag().getMeasuredWidth();
            }

            if (!fittedText.endsWith(TRUNCATED_MARKER) && !isUntruncated) {
                this.text = fittedText;
            }

            if (!isUntruncated && (getWidth() == 0 | textBoundsWidth < getPaint().measureText(fittedText))) {
                // Mimics behavior of `android:ellipsize="end"`, except it works in a HorizontalScrollView.
                // Truncates the string so it doesn't get cut off in the HorizontalScrollView with an
                // ellipsis at the end of it.
                final String ellipsizedText = TextUtils.ellipsize(fittedText,
                        getPaint(),
                        (float) textBoundsWidth,
                        TextUtils.TruncateAt.END).toString();
                fittedText = ellipsizedText + TRUNCATED_MARKER;
            }

            setText(fittedText);
            initiateTruncateText(text, fittedText);
        }
    }

    /**
     * Takes the string that's undergone truncation and based on whether it's been truncated or not
     * set whether it should be scrollable or not and what to do when the user finishes scrolling.
     *
     * @param originalText  The string before truncation
     * @param truncatedText The string after truncation
     */
    public void initiateTruncateText(final String originalText, final String truncatedText) {
        if (!originalText.endsWith(TRUNCATED_MARKER)) {
            this.text = originalText;
        }

        final TouchInterceptHorizontalScrollView scrollView = getTouchInterceptHorizontalScrollView();
        post(() -> {
            if (isTruncated(truncatedText)) {
                if (originalText.equals(truncatedText) && !truncatedText.endsWith(MARKER_UNTRUNCATED)) {
                    scrollView.setScrollable(false);
                } else {
                    scrollView.setScrollable(true);
                    scrollView.setOnEndScrollListener(() -> retruncateScrollText(truncatedText));
                }
            } else if (!truncatedText.endsWith(MARKER_UNTRUNCATED)) {
                scrollView.setScrollable(false);
            }
        });
    }

    /**
     * Checks whether a string was truncated at some point.
     *
     * @param text The string to check.
     * @return Returns whether the text has been truncated or not.
     */
    public boolean isTruncated(final String text) {
        return text.endsWith("…" + TRUNCATED_MARKER);
    }

    /**
     * Checks whether a string was untruncated at some point.
     *
     * @return Returns whether the current text has been untruncated or not.
     */
    public boolean isUntruncated() {
        return getText().toString().endsWith(MARKER_UNTRUNCATED);
    }

    /**
     * Untruncates and sets the text.
     */
    public void untruncateText() {
        String untruncatedText = text + MARKER_UNTRUNCATED;
        setText(untruncatedText);
    }

    /**
     * Re-truncates the text and animates it scrolling back to the start position.
     */
    public void retruncateScrollText(final String truncatedText) {
        Animator animator = ObjectAnimator
                .ofInt(getTouchInterceptHorizontalScrollView(), "scrollX", 0)
                .setDuration(RETRUNCATE_DELAY);

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (isUntruncated()) {
                    setText(truncatedText);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });

        animator.start();
    }
}
