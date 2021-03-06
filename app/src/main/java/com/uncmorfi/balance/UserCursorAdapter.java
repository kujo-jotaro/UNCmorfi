package com.uncmorfi.balance;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.uncmorfi.R;
import com.uncmorfi.balance.model.User;
import com.uncmorfi.balance.model.UsersContract;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


class UserCursorAdapter extends RecyclerView.Adapter<UserCursorAdapter.UserViewHolder> {
    private static final String USER_IMAGES_URL = "https://asiruws.unc.edu.ar/foto/";
    private static final float SCALE_USER_IMAGE_SIZE = 0.8f;
    private static final int SCALE_USER_IMAGE_TIME = 500;
    private static final int WARNING_USER_BALANCE = 20;
    private static final int WARNING_USER_EXPIRE = 6;
    private DateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private Context mContext;
    private Cursor mCursor;
    private List<Boolean> mUpdateInProgress = new ArrayList<>();
    private OnCardClickListener mListener;

    interface OnCardClickListener {
        void onClick(int userId, int position);
    }

    class UserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView nameText;
        TextView cardText;
        TextView typeText;
        TextView balanceText;
        ImageView userImage;
        ProgressBar progressBar;
        TextView expirationText;
        TextView lastUpdateText;

        UserViewHolder(View v) {
            super(v);

            nameText = (TextView) v.findViewById(R.id.user_name);
            cardText = (TextView) v.findViewById(R.id.user_card);
            typeText = (TextView) v.findViewById(R.id.user_type);
            balanceText = (TextView) v.findViewById(R.id.user_balance);
            userImage = (ImageView) v.findViewById(R.id.user_image);
            progressBar = (ProgressBar) v.findViewById(R.id.user_bar);
            expirationText = (TextView) v.findViewById(R.id.user_expiration);
            lastUpdateText = (TextView) v.findViewById(R.id.user_last_update);

            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = this.getAdapterPosition();
            mListener.onClick(getItemIdFromCursor(position), position);
        }
    }

    UserCursorAdapter(Context context, OnCardClickListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final UserViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        User user = new User(mCursor);

        setHolder(holder, user);
        setImage(holder, user);
        setProgressBar(holder, position);
    }

    private void setHolder(UserViewHolder holder, User user) {
        holder.nameText.setText(user.getName());
        holder.cardText.setText(user.getCard());
        holder.typeText.setText(user.getType());
        holder.balanceText.setText(String.format(Locale.US, "$ %d", user.getBalance()));

        holder.balanceText.setTextColor(ContextCompat.getColor(mContext,
                user.getBalance() < WARNING_USER_BALANCE ? R.color.accent : R.color.primary_dark));

        holder.expirationText.setText(String.format(
                mContext.getString(R.string.balance_expiration),
                mDateFormat.format(new Date(user.getExpiration()))));

        holder.expirationText.setTextColor(ContextCompat.getColor(mContext,
                warningExpiration(user.getExpiration()) ? R.color.accent : R.color.secondary_text));

        holder.lastUpdateText.setText(String.format(
                mContext.getString(R.string.balance_last_update),
                DateUtils.getRelativeTimeSpanString(user.getLastUpdate()).toString().toLowerCase()));
    }

    private boolean warningExpiration(long expiration) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MONTH, WARNING_USER_EXPIRE);
        return (new Date(expiration)).before(cal.getTime());
    }

    private void setProgressBar(UserViewHolder holder, int position) {
        if (mUpdateInProgress.get(position)) {
            holder.progressBar.setVisibility(View.VISIBLE);
            setUserImageUpdatingMode(holder);
        } else {
            holder.progressBar.setVisibility(View.INVISIBLE);
            setUserImageNormalMode(holder);
        }
    }

    private void setUserImageUpdatingMode(UserViewHolder holder) {
        holder.userImage.setScaleX(1);
        holder.userImage.setScaleY(1);
        holder.userImage.animate()
                .scaleX(SCALE_USER_IMAGE_SIZE)
                .scaleY(SCALE_USER_IMAGE_SIZE)
                .setInterpolator(new LinearOutSlowInInterpolator())
                .setDuration(SCALE_USER_IMAGE_TIME)
                .start();
    }

    private void setUserImageNormalMode(UserViewHolder holder) {
        holder.userImage.setScaleX(SCALE_USER_IMAGE_SIZE);
        holder.userImage.setScaleY(SCALE_USER_IMAGE_SIZE);
        holder.userImage.animate()
                .scaleX(1)
                .scaleY(1)
                .setInterpolator(new LinearOutSlowInInterpolator())
                .setDuration(SCALE_USER_IMAGE_TIME)
                .start();
    }

    private void setImage(final UserViewHolder holder, User user) {
        Glide.with(mContext)
                .load(USER_IMAGES_URL + user.getImage())
                .asBitmap()
                .placeholder(R.drawable.person_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(new BitmapImageViewTarget(holder.userImage) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable =
                                RoundedBitmapDrawableFactory.create(mContext.getResources(), resource);
                        circularBitmapDrawable.setCircular(true);
                        holder.userImage.setImageDrawable(circularBitmapDrawable);
                    }
                });
    }

    @Override
    public int getItemCount() {
        if (mCursor != null)
            return mCursor.getCount();
        return 0;
    }

    void setCursor(Cursor newCursor) {
        mCursor = newCursor;
        resizeUpdateInProgress();
    }

    private void resizeUpdateInProgress() {
        if (mCursor != null) {
            int diff = mCursor.getCount() - mUpdateInProgress.size();
            if (diff > 0) {
                List<Boolean> list = Arrays.asList(new Boolean[diff]);
                Collections.fill(list, Boolean.FALSE);
                mUpdateInProgress.addAll(list);
            }
        }
    }

    void setInProgress(int position, boolean show) {
        mUpdateInProgress.set(position, show);
    }

    int getItemIdFromCursor(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getInt(mCursor.getColumnIndex(UsersContract.UserEntry._ID));
    }
}
