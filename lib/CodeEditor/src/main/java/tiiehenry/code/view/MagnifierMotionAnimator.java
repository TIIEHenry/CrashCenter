package tiiehenry.code.view;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.animation.LinearInterpolator;
import android.widget.Magnifier;

@TargetApi(Build.VERSION_CODES.P)
public class MagnifierMotionAnimator {
    private static final long DURATION = 10 /* miliseconds */;

    // The magnifier being animated.
    public final Magnifier magnifier;
    // A value animator used to animate the magnifier.
    private final ValueAnimator mAnimator;

    // Whether the magnifier is currently visible.
    private boolean mMagnifierIsShowing;
    // The coordinates of the magnifier when the currently running animation started.
    private float mAnimationStartX;
    private float mAnimationStartY;
    // The coordinates of the magnifier in the latest animation frame.
    private float mAnimationCurrentX;
    private float mAnimationCurrentY;
    // The latest coordinates the motion animator was asked to #show() the magnifier at.
    private float mLastX;
    private float mLastY;

    public MagnifierMotionAnimator(final Magnifier magnifier) {
        this.magnifier = magnifier;
        // Prepare the animator used to run the motion animation.
        mAnimator = ValueAnimator.ofFloat(0, 1);
        mAnimator.setDuration(DURATION);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // Interpolate to find the current position of the magnifier.
                mAnimationCurrentX = mAnimationStartX
                        + (mLastX - mAnimationStartX) * animation.getAnimatedFraction();
                mAnimationCurrentY = mAnimationStartY
                        + (mLastY - mAnimationStartY) * animation.getAnimatedFraction();
                MagnifierMotionAnimator.this.magnifier.show(mAnimationCurrentX, mAnimationCurrentY);
            }
        });
    }

    /**
     * Shows the magnifier at a new position.
     * If the y coordinate is different from the previous y coordinate
     * (probably corresponding to a line jump in the text), a short
     * animation is added to the jump.
     */

    public void show(final float x, final float y) {
        final boolean startNewAnimation = mMagnifierIsShowing && y != mLastY;

        if (startNewAnimation) {
            if (mAnimator.isRunning()) {
                mAnimator.cancel();
                mAnimationStartX = mAnimationCurrentX;
                mAnimationStartY = mAnimationCurrentY;
            } else {
                mAnimationStartX = mLastX;
                mAnimationStartY = mLastY;
            }
            mAnimator.start();
        } else {
            if (!mAnimator.isRunning()) {
                magnifier.show(x, y);
            }
        }
        mLastX = x;
        mLastY = y;
        mMagnifierIsShowing = true;
    }

    /**
     * Updates the content of the magnifier.
     */
    public void update() {
        magnifier.update();
    }

    /**
     * Dismisses the magnifier, or does nothing if it is already dismissed.
     */
    public void dismiss() {
        magnifier.dismiss();
        mAnimator.cancel();
        mMagnifierIsShowing = false;
    }
}