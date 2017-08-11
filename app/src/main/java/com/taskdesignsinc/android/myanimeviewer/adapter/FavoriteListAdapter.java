package com.taskdesignsinc.android.myanimeviewer.adapter;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteTag;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;

public class FavoriteListAdapter extends ArrayAdapter<FavoriteTag> {
    Context mContext;
    boolean mShowDeleteOption = false;

    public FavoriteListAdapter(Context pContext) {
        super(pContext, R.layout.favorite_row, MAVApplication.getInstance().getRepository().getFavoriteTags());
        mContext = pContext;
    }

    public FavoriteListAdapter(Context pContext, boolean canDelete) {
        super(pContext, R.layout.favorite_row, MAVApplication.getInstance().getRepository().getFavoriteTags());
        mContext = pContext;
        mShowDeleteOption = canDelete;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void resetData() {
        clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            addAll(MAVApplication.getInstance().getRepository().getFavoriteTags());
        } else {
            for (FavoriteTag obj : MAVApplication.getInstance().getRepository().getFavoriteTags()) {
                add(obj);
            }
        }
        add(new FavoriteTag(-1, "Add New", -1));
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        FavoriteHolder holder = null;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.favorite_row, null);

            holder = new FavoriteHolder(row);
            row.setTag(holder);
        } else {
            holder = (FavoriteHolder) row.getTag();
        }

        final FavoriteTag lTag = getItem(position);
        if (lTag != null) {
            String subText = "";
            if (lTag.getId() != -1)
                subText = " - " + lTag.getId();
            holder.titleView.setText(lTag.getTitle() + subText);
            if (getItem(position).getRibbonID() > -1 && lTag.getRibbonID() < Constants.RibbonDrawable.length) {
                holder.coverImageView.setImageResource(Constants.RibbonDrawable[getItem(position).getRibbonID()]);
            } else {
                holder.coverImageView.setImageResource(0);
            }

            if (mShowDeleteOption && lTag.getId() != -1) {
                holder.deleteButton.setVisibility(View.VISIBLE);
                holder.deleteButton.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(mContext)
                                .setTitle(R.string.remove_tag)
                                .setMessage(R.string.confirmation_msg)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                        if (MAVApplication.getInstance().getRepository().deleteFavoriteTag(lTag.getTagId())) {
                                            MAVApplication.getInstance().getRepository().deleteFavoriteRecordsByTagID(lTag.getTagId());
                                            remove(lTag);
                                            notifyDataSetChanged();
                                        }
                                    }
                                })
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                }).show();
                    }
                });
            }
        }

        return (row);
    }

    static class FavoriteHolder {
        private TextView titleView = null;
        private ImageView coverImageView = null;
        private ImageButton deleteButton = null;

        FavoriteHolder(View row) {
            titleView = (TextView) row.findViewById(R.id.fr_text);
            coverImageView = (ImageView) row
                    .findViewById(R.id.fr_image);
            deleteButton = (ImageButton) row.findViewById(R.id.fr_delete);
        }
    }
}
