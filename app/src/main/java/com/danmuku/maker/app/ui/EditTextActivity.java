package com.danmuku.maker.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.danmuku.maker.R;
import com.danmuku.maker.app.model.TagItem;
import com.danmuku.maker.customview.LabelView;
import com.danmuku.maker.util.StringUtils;
import com.danmuku.maker.AppConstants;
import com.danmuku.maker.base.BaseActivity;

import butterknife.ButterKnife;
import butterknife.InjectView;


/**
 * 编辑文字
 */
public class EditTextActivity extends BaseActivity {

    private final static int MAX = 10;
    private int maxlength = MAX;
    @InjectView(R.id.text_input)
    EditText contentView;
    @InjectView(R.id.tag_input_tips)
    TextView numberTips;

    private static TagItem tagItem;

    public static void openTextEdit(Activity mContext, TagItem tagInfo, int reqCode) {
        Intent i = new Intent(mContext, EditTextActivity.class);
        Bundle mBundle = new Bundle();
        mBundle.putSerializable(AppConstants.EDIT_TAG, tagInfo);
        i.putExtras(mBundle);
        tagItem = tagInfo;
        mContext.startActivityForResult(i, reqCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_text);
        ButterKnife.inject(this);

        String defaultStr = tagItem.getName();
        if (StringUtils.isNotEmpty(defaultStr)) {
            contentView.setText(defaultStr);
            if (defaultStr.length() <= maxlength) {
                numberTips.setText("你还可以输入" + (maxlength - defaultStr.length()) + "个字  ("
                        + defaultStr.length() + "/" + maxlength + ")");
            }
        }
        titleBar.setRightBtnOnclickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                Bundle mBundle = new Bundle();
                tagItem.setName(contentView.getText().toString());
                mBundle.putSerializable(AppConstants.EDIT_TAG, tagItem);
                intent.putExtras(mBundle);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        contentView.addTextChangedListener(mTextWatcher);
    }

    TextWatcher mTextWatcher = new TextWatcher() {
        private int editStart;
        private int editEnd;

        @Override
        public void beforeTextChanged(CharSequence s, int arg1, int arg2,
                                      int arg3) {
        }

        @Override
        public void onTextChanged(CharSequence s, int arg1, int arg2,
                                  int arg3) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            editStart = contentView.getSelectionStart();
            editEnd = contentView.getSelectionEnd();
            if (s.toString().length() > maxlength) {
                toast("你输入的字数已经超过了限制！", Toast.LENGTH_LONG);
                s.delete(editStart - 1, editEnd);
                int tempSelection = editStart;
                contentView.setText(s);
                contentView.setSelection(tempSelection);
            }
            numberTips.setText("你还可以输入"
                    + (maxlength - s.toString().length())
                    + "个字  (" + s.toString().length() + "/"
                    + maxlength + ")");
        }
    };
}
