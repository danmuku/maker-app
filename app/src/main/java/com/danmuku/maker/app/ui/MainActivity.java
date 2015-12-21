package com.danmuku.maker.app.ui;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.alibaba.fastjson.JSON;
import com.danmuku.maker.R;
import com.danmuku.maker.util.DataUtils;
import com.danmuku.maker.util.StringUtils;
import com.danmuku.maker.customview.LabelView;
import com.melnykov.fab.FloatingActionButton;
import com.danmuku.maker.App;
import com.danmuku.maker.AppConstants;
import com.danmuku.maker.app.camera.CameraManager;
import com.danmuku.maker.app.model.FeedItem;
import com.danmuku.maker.app.model.TagItem;
import com.danmuku.maker.base.BaseActivity;
import com.danmuku.maker.app.camera.ui.PhotoProcessActivity;

import java.util.ArrayList;
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
            feedList = new ArrayList<FeedItem>();
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
        fab.setOnClickListener(v -> CameraManager.getInst().openCamera(MainActivity.this));
//        //删除按钮
//        deleteBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                mAdapter.remove(itemId);
//                feedList.remove(itemId);
//                buttonsView.setVisibility(View.GONE);
//            }
//        });
//        //保存按钮
//        saveBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                buttonsView.setVisibility(View.GONE);
//            }
//        });
//        //分享按钮
//        shareBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                buttonsView.setVisibility(View.GONE);
//            }
//        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.menu_main_context, menu);
        registerForContextMenu(mRecyclerView);
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


    //照片适配器
    public class PictureAdapter extends RecyclerView.Adapter<ViewHolder> {

        private List<FeedItem> items = new ArrayList<FeedItem>();

        public void setList(List<FeedItem> list) {
            if (items.size() > 0) {
                items.clear();
            }
            items.addAll(list);
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

        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public void onViewRecycled(ViewHolder holder) {
            // 将标签移除,避免回收使用时标签重复
            holder.pictureLayout.removeViews(1, holder.pictureLayout.getChildCount() - 1);
            super.onViewRecycled(holder);
        }


        @Override
        public void onViewAttachedToWindow(ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            // 这里可能有问题 延迟200毫秒加载是为了等pictureLayout已经在屏幕上显示getWidth才为具体的值
            holder.pictureLayout.getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (TagItem feedImageTag : holder.getTagList()) {
                        LabelView tagView = new LabelView(MainActivity.this);
                        tagView.init(feedImageTag);
                        tagView.draw(holder.pictureLayout,
                                (int) (feedImageTag.getX() * ((double) holder.pictureLayout.getWidth() / (double) 1242)),
                                (int) (feedImageTag.getY() * ((double) holder.pictureLayout.getWidth() / (double) 1242)));
                        tagView.wave();
                    }
                }
            }, 200);
        }
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

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

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //单击进入编辑页面
                    Intent i = new Intent(MainActivity.this, PhotoProcessActivity.class);
                    FeedItem feedItem = feedList.get(getPosition());
                    feedItem.setPosition(getPosition());
                    String path = "file://" + feedItem.getImgPath();
                    i.setData(Uri.parse(path));
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("lables", feedItem);
                    i.putExtras(bundle);
                    startActivity(i);
                }
            });
        }

    }

}
