package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";
    private static final String POSITION = "position";
    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private int position;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private LinearLayout metaBar;
    private TextView bylineView;
    private TextView titleView;
    private Toolbar toolbar;

    private Palette.PaletteAsyncListener paletteListener;
    private ImageView mPhotoView;


    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId, int position) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putInt(POSITION, position);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
            position = getArguments().getInt(POSITION);
        }
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setPaleteListener();
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        metaBar = (LinearLayout) mRootView.findViewById(R.id.meta_bar);
        mPhotoView.setTransitionName(getString(R.string.article_photo) + position);
        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();
        return mRootView;
    }


    private void setPaleteListener() {
        paletteListener = new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette palette) {
                int defaultColor = 0x000000;
                int darkMutedColor = palette.getDarkMutedColor(defaultColor);
                int lightMutedColor = palette.getLightMutedColor(defaultColor);

                Palette.Swatch vibrant = palette.getVibrantSwatch();
                if (vibrant != null) {
                    metaBar.setBackgroundColor(vibrant.getRgb());
                    titleView.setTextColor(vibrant.getTitleTextColor());
                    bylineView.setTextColor(vibrant.getTitleTextColor());
                } else {
                    metaBar.setBackgroundColor(lightMutedColor);
                    titleView.setTextColor(darkMutedColor);
                    bylineView.setTextColor(darkMutedColor);
                }


            }
        };
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        titleView = (TextView) mRootView.findViewById(R.id.article_title);
        titleView.setTransitionName(getString(R.string.article_title) + position);
        bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsing_tool_bar);
        toolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "RobotoSlab-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            String title = mCursor.getString(ArticleLoader.Query.TITLE);
            titleView.setText(title);
            if (null != collapsingToolbarLayout)
                collapsingToolbarLayout.setTitle(title);

            activity.setTitle(title);
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                Spanned subTitle = Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>");
                bylineView.setText(subTitle);


            } else {
                // If date is before 1902, just show the string
                Spanned subtitle = Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>" + "<body style=\"text-align:justify;>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</body>"
                                + "</font>");
                bylineView.setText(subtitle);
                toolbar.setSubtitle(subtitle);

            }
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n\r\n)", "<br /><br />")));

            Picasso.with(getActivityCast()).load(mCursor.getString(ArticleLoader.Query.PHOTO_URL)).into(mPhotoView,
                    new Callback() {
                        @Override
                        public void onSuccess() {
                            scheduleStartPostponedTransition(mPhotoView);
                            Bitmap bitmap = ((BitmapDrawable) mPhotoView.getDrawable()).getBitmap();
                            setPaleteListener();
                            Palette.from(bitmap).generate(paletteListener);
                        }

                        @Override
                        public void onError() {
                            Log.e(TAG, "onError: loading image failed");
                        }
                    });
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    //ref: http://guides.codepath.com/android/shared-element-activity-transition
    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        getActivityCast().startPostponedEnterTransition();
                        return true;
                    }
                });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }


}
