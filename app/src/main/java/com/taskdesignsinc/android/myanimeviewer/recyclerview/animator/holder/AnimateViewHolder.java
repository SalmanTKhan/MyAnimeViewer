package com.taskdesignsinc.android.myanimeviewer.recyclerview.animator.holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.nineoldandroids.animation.Animator;

public abstract class AnimateViewHolder extends RecyclerView.ViewHolder {

  public AnimateViewHolder(View itemView) {
    super(itemView);
  }

  public void preAnimateAddImpl() {
  }

  public void preAnimateRemoveImpl() {
  }

  public abstract void animateAddImpl(Animator.AnimatorListener listener);

  public abstract void animateRemoveImpl(Animator.AnimatorListener listener);
}
