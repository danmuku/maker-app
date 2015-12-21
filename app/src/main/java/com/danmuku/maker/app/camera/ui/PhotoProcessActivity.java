package com.danmuku.maker.app.camera.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.danmuku.maker.R;
import com.danmuku.maker.app.ui.MainActivity;
import com.danmuku.maker.util.FileUtils;
import com.danmuku.maker.util.ImageUtils;
import com.danmuku.maker.util.StringUtils;
import com.danmuku.maker.util.TimeUtils;
import com.danmuku.maker.customview.LabelView;
import com.danmuku.maker.customview.MyHighlightView;
import com.danmuku.maker.customview.MyImageViewDrawableOverlay;
import com.danmuku.maker.App;
import com.danmuku.maker.AppConstants;
import com.danmuku.maker.app.camera.CameraBaseActivity;
import com.danmuku.maker.app.camera.CameraManager;
import com.danmuku.maker.app.camera.util.EffectUtil;
import com.danmuku.maker.app.model.FeedItem;
import com.danmuku.maker.app.model.TagItem;
import com.danmuku.maker.app.ui.EditTextActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/**
 * 图片处理界面
 */
public class PhotoProcessActivity extends CameraBaseActivity {

    private static final String TAG = "PhotoProcessActivity";

    //滤镜图片
    @InjectView(R.id.draw_image)
    ImageView imageView;

    //绘图区域
    @InjectView(R.id.drawing_view_container)
    ViewGroup drawArea;


    @InjectView(R.id.toolbar_area)
    ViewGroup toolArea;

    private MyImageViewDrawableOverlay mImageView;

    //当前图片
    private Bitmap currentBitmap;

    private List<LabelView> labels = new ArrayList<>();

    //标签区域
    private View commonLabelArea;

    private FeedItem feedItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_process);
        ButterKnife.inject(this);
        EffectUtil.clear();
        initView();
        initEvent();

        ImageUtils.asyncLoadImage(this, getIntent().getData(), result -> {
            currentBitmap = result;
            imageView.setImageBitmap(currentBitmap);
        });


        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            feedItem = (FeedItem) bundle.getSerializable("lables");
            List<TagItem> tagItems = feedItem.getTagList();
            for (int i = 0; i < tagItems.size(); i++) {
                addOldLable(tagItems.get(i));
            }
        }
    }

    private void initView() {
        //添加贴纸水印的画布
        View overlay = LayoutInflater.from(PhotoProcessActivity.this).inflate(
                R.layout.view_drawable_overlay, null);
        mImageView = (MyImageViewDrawableOverlay) overlay.findViewById(R.id.drawable_overlay);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(App.getApp().getScreenWidth(),
                App.getApp().getScreenWidth());
        mImageView.setLayoutParams(params);
        overlay.setLayoutParams(params);
        drawArea.addView(overlay);

        //添加标签选择器
        RelativeLayout.LayoutParams rparams = new RelativeLayout.LayoutParams(App.getApp().getScreenWidth(), App.getApp().getScreenWidth());

        //初始化滤镜图片
        imageView.setLayoutParams(rparams);


        //初始化推荐标签栏
        commonLabelArea = LayoutInflater.from(PhotoProcessActivity.this).inflate(
                R.layout.view_label_bottom, null);
        commonLabelArea.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        toolArea.addView(commonLabelArea);
        commonLabelArea.setVisibility(View.GONE);
    }

    private void initEvent() {

        commonLabelArea.setVisibility(View.VISIBLE);


        mImageView.setOnDrawableEventListener(wpEditListener);
        mImageView.setSingleTapListener(() -> {
            EditTextActivity.openTextEdit(PhotoProcessActivity.this, "", 8, AppConstants.ACTION_EDIT_LABEL);

            drawArea.postInvalidate();
        });

        titleBar.setRightBtnOnclickListener(v -> {
            savePicture();
        });
    }

    //保存图片
    private void savePicture() {
        //加滤镜
        final Bitmap newBitmap = Bitmap.createBitmap(mImageView.getWidth(), mImageView.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(newBitmap);
        RectF dst = new RectF(0, 0, mImageView.getWidth(), mImageView.getHeight());

        cv.drawBitmap(currentBitmap, null, dst, null);

        new SavePicToFileTask().execute(newBitmap);
    }

    private class SavePicToFileTask extends AsyncTask<Bitmap, Void, String> {
        Bitmap bitmap;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("图片处理中...");
        }

        @Override
        protected String doInBackground(Bitmap... params) {
            String fileName = null;
            try {
                bitmap = params[0];
                String picName = TimeUtils.dtFormat(new Date(), "yyyyMMddHHmmss") + ".png";
                fileName = ImageUtils.saveToFile(FileUtils.getInst().getPhotoSavedPath() + "/" + picName, false, bitmap);

            } catch (Exception e) {
                e.printStackTrace();
                toast("图片处理错误，请退出相机并重试", Toast.LENGTH_LONG);
            }
            return fileName;
        }

        @Override
        protected void onPostExecute(String fileName) {
            super.onPostExecute(fileName);
            dismissProgressDialog();
            if (StringUtils.isEmpty(fileName)) {
                return;
            }

            //将照片信息保存至sharedPreference
            //保存标签信息
            List<TagItem> tagInfoList = new ArrayList<TagItem>();
            for (LabelView label : labels) {
                tagInfoList.add(label.getTagInfo());
            }

            //将图片信息通过EventBus发送到MainActivity

            if (feedItem != null) {
                feedItem.setTagList(tagInfoList);
            } else {
                feedItem = new FeedItem(tagInfoList, fileName, 0);
            }
            EventBus.getDefault().post(feedItem);
            CameraManager.getInst().close();
        }
    }


    public void tagClick(View v) {
        TextView textView = (TextView) v;
        TagItem tagItem = new TagItem(AppConstants.POST_TYPE_TAG, textView.getText().toString());
        addLabel(tagItem);
    }

    private MyImageViewDrawableOverlay.OnDrawableEventListener wpEditListener = new MyImageViewDrawableOverlay.OnDrawableEventListener() {
        @Override
        public void onMove(MyHighlightView view) {
        }

        @Override
        public void onFocusChange(MyHighlightView newFocus, MyHighlightView oldFocus) {
        }

        @Override
        public void onDown(MyHighlightView view) {

        }

        @Override
        public void onClick(MyHighlightView view) {
        }

        @Override
        public void onClick(final LabelView label) {
            alert("温馨提示", "是否需要删除该标签！", "确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EffectUtil.removeLabelEditable(mImageView, drawArea, label);
                    labels.remove(label);
                }
            }, "取消", null);
        }
    };


    //添加标签
    private void addLabel(TagItem tagItem) {
        int left = 0;
        int top = 0;
        if (labels.size() == 0 && left == 0 && top == 0) {
            left = mImageView.getWidth() / 2 - 10;
            top = mImageView.getWidth() / 2;
        }
        LabelView label = new LabelView(PhotoProcessActivity.this);
        label.init(tagItem);
        EffectUtil.addLabelEditable(mImageView, drawArea, label, left, top);
        labels.add(label);
    }

    private void addOldLable(TagItem tagItem) {
        int left = (int) tagItem.getX();
        int top = (int) tagItem.getY();
        LabelView label = new LabelView(PhotoProcessActivity.this);
        label.init(tagItem);
        EffectUtil.addLabelEditable(mImageView, drawArea, label, left, top);
        labels.add(label);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (AppConstants.ACTION_EDIT_LABEL == requestCode && data != null) {
            String text = data.getStringExtra(AppConstants.PARAM_EDIT_TEXT);
            if (StringUtils.isNotEmpty(text)) {
                TagItem tagItem = new TagItem(AppConstants.POST_TYPE_TAG, text);
                addLabel(tagItem);
            }
        } else if (AppConstants.ACTION_EDIT_LABEL_POI == requestCode && data != null) {
            String text = data.getStringExtra(AppConstants.PARAM_EDIT_TEXT);
            if (StringUtils.isNotEmpty(text)) {
                TagItem tagItem = new TagItem(AppConstants.POST_TYPE_POI, text);
                addLabel(tagItem);
            }
        }
    }
}
