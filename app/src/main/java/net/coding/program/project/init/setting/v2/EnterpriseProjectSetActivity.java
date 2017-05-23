package net.coding.program.project.init.setting.v2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.loopj.android.http.RequestParams;

import net.coding.program.R;
import net.coding.program.common.Global;
import net.coding.program.common.enter.SimpleTextWatcher;
import net.coding.program.common.ui.PickPhotoActivity;
import net.coding.program.common.umeng.UmengEvent;
import net.coding.program.model.ProjectObject;
import net.coding.program.network.BaseHttpObserver;
import net.coding.program.network.HttpObserver;
import net.coding.program.network.Network;
import net.coding.program.project.ProjectFragment;
import net.coding.program.project.ProjectHomeActivity;
import net.coding.program.project.init.InitProUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.ViewById;

import java.io.File;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by chenchao on 2017/5/23.
 * 企业版设置项目
 */
@EActivity(R.layout.activity_enterprise_project_set)
public class EnterpriseProjectSetActivity extends PickPhotoActivity {

    @Extra
    ProjectObject project;

    @ViewById
    ImageView projectIcon;

    @ViewById
    View projectInfoLayout;

    @ViewById
    EditText projectName, description;

    private MenuItem mMenuSave;

    private SimpleTextWatcher watcher = new SimpleTextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            updateSendButton();
        }
    };

    @AfterViews
    void initEnterpriseProjectSetActivity() {
        if (project.isManagerLevel()) {
            projectName.setText(project.name);
            description.setText(project.getDescription());

            description.addTextChangedListener(watcher);
            projectName.addTextChangedListener(watcher);
            projectInfoLayout.setVisibility(View.VISIBLE);

            projectIconfromNetwork(projectIcon, project.icon);
        } else {
            projectInfoLayout.setVisibility(View.GONE);
        }
    }

    private void enableSendButton(boolean enable) {
        if (mMenuSave == null) {
            return;
        }

        if (enable) {
            mMenuSave.setIcon(R.drawable.ic_menu_ok);
            mMenuSave.setEnabled(true);
        } else {
            mMenuSave.setIcon(R.drawable.ic_menu_ok_unable);
            mMenuSave.setEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (project.isManagerLevel()) {
            getMenuInflater().inflate(R.menu.menu_fragment_create, menu);
            mMenuSave = menu.findItem(R.id.action_finish);
            updateSendButton();
        }

        return super.onCreateOptionsMenu(menu);
    }

    private void updateSendButton() {
        String titleString = projectName.getText().toString();
        String descString = description.getText().toString().trim();

        if (titleString.isEmpty()) {
            enableSendButton(false);
            return;
        }

        enableSendButton(!descString.equals(project.description) || !titleString.equals(project.name));
    }

    @OptionsItem(R.id.action_finish)
    void actionFinish() {
        String titleString = projectName.getText().toString();
        String descString = description.getText().toString().trim();
        Network.getRetrofit(this)
                .setProjectInfo(project.id, titleString, descString)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new HttpObserver<ProjectObject>(this) {
                    @Override
                    public void onSuccess(ProjectObject data) {
                        super.onSuccess(data);
                        showProgressBar(false);

                        umengEvent(UmengEvent.PROJECT, "修改项目");
                        showButtomToast("修改成功");

                        InitProUtils.hideSoftInput(EnterpriseProjectSetActivity.this);
                        Intent intent = new Intent();
                        intent.putExtra("projectObject", data);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }

                    @Override
                    public void onFail(int errorCode, @NonNull String error) {
                        super.onFail(errorCode, error);
                        showProgressBar(false);
                    }
                });
        showProgressBar(true);
    }

    @Click
    void exitProject() {
        new AlertDialog.Builder(this)
                .setTitle("退出项目")
                .setMessage(String.format("您确定要退出 %s 项目吗？", project.name))
                .setPositiveButton("确定", (dialog1, which) -> {
                    Network.getRetrofit(EnterpriseProjectSetActivity.this)
                            .quitProject(project.id)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new BaseHttpObserver(EnterpriseProjectSetActivity.this) {
                                @Override
                                public void onSuccess() {
                                    showProgressBar(false);

                                    umengEvent(UmengEvent.PROJECT, "退出项目");

                                    showButtomToast("已退出项目");
                                    Intent intent = new Intent();
                                    intent.setAction(ProjectFragment.RECEIVER_INTENT_REFRESH_PROJECT);
                                    intent.setAction(ProjectHomeActivity.BROADCAST_CLOSE);
                                    sendBroadcast(intent);
                                    finish();
                                }

                                @Override
                                public void onFail(int errorCode, @NonNull String error) {
                                    super.onFail(errorCode, error);
                                    showProgressBar(false);
                                }
                            });
                    showProgressBar(true);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Click
    void projectIcon() {
        pickPhoto();
    }

    @Override
    protected void pickImageCallback(Uri uri, String path) {
        try {
            projectIcon.setImageURI(uri);
            String uploadUrl = host + "/" + project.getId() + "/project_icon";
            RequestParams params = new RequestParams();
            params.put("file", new File(path));
            postNetwork(uploadUrl, params, uploadUrl);
            showProgressBar(true, "正在上传图片...");
        } catch (Exception e) {
            Global.errorLog(e);
        }
    }
}