package com.taskdesignsinc.android.myanimeviewer.recyclerview.animator;

import android.view.animation.OvershootInterpolator;

/**
 * Created by Salman T. Khan on 11/29/2015.
 */
public class AnimatorManager {

    private static AnimatorManager instance;
    private int mType = 1;

    enum AnimatorType {
        ScaleIn(0),
        SlideInLeft(1);

        private int value;

        AnimatorType(int value) {
            this.value = value;
        }
    }

    public static AnimatorManager getInstance() {
        if (instance == null)
            instance = new AnimatorManager();
        return instance;
    }

    public BaseItemAnimator getAnimator() {
        switch(AnimatorType.values()[mType]) {
            case ScaleIn:
                return new ScaleInAnimator(new OvershootInterpolator(1f));
            default:
                return new SlideInLeftAnimator(new OvershootInterpolator(1f));
        }
    }
}
