package com.danmuku.maker.app.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.danmuku.maker.R;
import com.danmuku.maker.util.DataUtils;
import com.danmuku.maker.util.FileUtils;
import com.danmuku.maker.util.ImageUtils;
import com.danmuku.maker.util.StringUtils;
import com.danmuku.maker.customview.LabelView;
import com.danmuku.maker.util.TimeUtils;
import com.melnykov.fab.FloatingActionButton;
import com.danmuku.maker.App;
import com.danmuku.maker.AppConstants;
import com.danmuku.maker.app.camera.CameraManager;
import com.danmuku.maker.app.model.FeedItem;
import com.danmuku.maker.app.model.TagItem;
import com.danmuku.maker.base.BaseActivity;
import com.danmuku.maker.app.camera.ui.PhotoProcessActivity;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/**
 * 主界面
 */
public class MainActivity extends BaseActivity {

    @InjectView(R.id.fab)
    FloatingActionButton fab;
    @InjectView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private List<FeedItem> feedList;
    private PictureAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        EventBus.getDefault().register(this);
        initView();

        //如果没有照片则打开相机
        String str = DataUtils.getStringPreferences(App.getApp(), AppConstants.FEED_INFO);
        if (StringUtils.isNotEmpty(str)) {
            feedList = JSON.parseArray(str, FeedItem.class);
        }
        if (feedList == null) {
            CameraManager.getInst().openCamera(MainActivity.this);
        } else {
            mAdapter.setList(feedList);
        }

    }


    public void onEventMainThread(FeedItem feedItem) {
        if (feedList == null) {
            feedList = new ArrayList<>();
        }
        feedList.add(feedItem.getPosition(), feedItem);
        DataUtils.setStringPreferences(App.getApp(), AppConstants.FEED_INFO, JSON.toJSONString(feedList));
        mAdapter.setList(feedList);
        mAdapter.notifyDataSetChanged();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        titleBar.hideLeftBtn();
        titleBar.hideRightBtn();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PictureAdapter();
        mRecyclerView.setAdapter(mAdapter);
        this.registerForContextMenu(mRecyclerView);
        fab.setOnClickListener(v -> CameraManager.getInst().openCamera(MainActivity.this));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        int position = mAdapter.getPosition();
        switch (id) {
            case R.id.delete:
                mAdapter.remove(position);
                feedList.remove(position);
                break;

            case R.id.save:
                View view = mRecyclerView.getChildAt(position);
                view.setDrawingCacheEnabled(true);
                view.buildDrawingCache();
                Bitmap bitmap = view.getDrawingCache();
                String picName = "danmuku" + TimeUtils.dtFormat(new Date(), "yyyyMMddHHmmss") + ".png";
                try {
                    ImageUtils.saveToFile(FileUtils.getInst().getSystemPhotoPath() + "/" + picName, false, bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                toast("保存已保存到相册", Toast.LENGTH_LONG);
                break;

            case R.id.share:
                toast("分享成功", Toast.LENGTH_LONG);
                break;

        }
        return super.onContextItemSelected(item);
    }


    //enable为true时，菜单添加图标有效，enable为false时无效。4.0系统默认无效
    private void setIconEnable(Menu menu, boolean enable) {
        try {
            Class<?> clazz = Class.forName("com.android.internal.view.menu.MenuBuilder");
            Method m = clazz.getDeclaredMethod("setOptionalIconsVisible", boolean.class);
            m.setAccessible(true);

            //MenuBuilder实现Menu接口，创建菜单时，传进来的menu其实就是MenuBuilder对象(java的多态特征)
            m.invoke(menu, enable);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //照片适配器
    public class PictureAdapter extends RecyclerView.Adapter<ViewHolder> {

        private List<FeedItem> items = new ArrayList<>();

        public void setList(List<FeedItem> list) {
            if (items.size() > 0) {
                items.clear();
            }
            items.addAll(list);
        }

        private int position;

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_picture, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FeedItem feedItem = items.get(position);
            holder.picture.setImageBitmap(BitmapFactory.decodeFile(feedItem.getImgPath()));
            holder.setTagList(feedItem.getTagList());

            holder.itemView.setOnLongClickListener(v -> {
                setPosition(holder.getAdapterPosition());
                return false;
            });

        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public void onViewRecycled(ViewHolder holder) {
            // 将标签移除,避免回收使用时标签重复
            holder.pictureLayout.removeViews(1, holder.pictureLayout.getChildCount() - 1);
            holder.itemView.setOnLongClickListener(null);
            super.onViewRecycled(holder);
        }


        @Override
        public void onViewAttachedToWindow(ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            // 这里可能有问题 延迟200毫秒加载是为了等pictureLayout已经在屏幕上显示getWidth才为具体的值
            holder.pictureLayout.getHandler().postDelayed(() -> {
                for (TagItem feedImageTag : holder.getTagList()) {
                    LabelView tagView = new LabelView(MainActivity.this);
                    tagView.init(feedImageTag);
                    tagView.draw(holder.pictureLayout,
                            (int) (feedImageTag.getX() * ((double) holder.pictureLayout.getWidth() / (double) 1242)),
                            (int) (feedImageTag.getY() * ((double) holder.pictureLayout.getWidth() / (double) 1242)));
                    tagView.wave();
                }
            }, 200);
        }

        public void remove(int position) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        @InjectView(R.id.pictureLayout)
        RelativeLayout pictureLayout;
        @InjectView(R.id.picture)
        ImageView picture;

        private List<TagItem> tagList = new ArrayList<>();

        public List<TagItem> getTagList() {
            return tagList;
        }

        public void setTagList(List<TagItem> tagList) {
            if (this.tagList.size() > 0) {
                this.tagList.clear();
            }
            this.tagList.addAll(tagList);
        }

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);

            itemView.setOnCreateContextMenuListener(this);

            itemView.setOnClickListener(view -> {
                //单击进入编辑页面
                Intent i = new Intent(MainActivity.this, PhotoProcessActivity.class);
                FeedItem feedItem = feedList.get(getAdapterPosition());
                feedItem.setPosition(getAdapterPosition());
                String path = "file://" + feedItem.getImgPath();
                i.setData(Uri.parse(path));
                Bundle bundle = new Bundle();
                bundle.putSerializable("lables", feedItem);
                i.putExtras(bundle);
                startActivity(i);
            });
        }


        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            setIconEnable(contextMenu, true);
            contextMenu.setHeaderTitle("操作");
            getMenuInflater().inflate(R.menu.menu_main_context, contextMenu);
        }
    }

}
