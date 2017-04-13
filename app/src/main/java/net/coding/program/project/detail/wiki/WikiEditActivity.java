package net.coding.program.project.detail.wiki;

import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;

import net.coding.program.R;
import net.coding.program.common.Global;
import net.coding.program.common.ui.BackActivity;
import net.coding.program.event.EventAction;
import net.coding.program.event.WikiEvent;
import net.coding.program.network.HttpObserver;
import net.coding.program.network.Network;
import net.coding.program.network.model.wiki.Wiki;
import net.coding.program.param.TopicData;
import net.coding.program.project.detail.EditPreviewMarkdown;
import net.coding.program.project.detail.ProjectActivity;
import net.coding.program.project.detail.TopicEditFragment;
import net.coding.program.project.detail.TopicEditFragment_;
import net.coding.program.project.detail.TopicPreviewFragment;
import net.coding.program.project.detail.TopicPreviewFragment_;
import net.coding.program.third.EmojiFilter;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;

import info.hoang8f.android.segmented.SegmentedGroup;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@EActivity(R.layout.activity_wiki_edit)
public class WikiEditActivity extends BackActivity implements EditPreviewMarkdown {

    @Extra
    ProjectActivity.ProjectJumpParam projectParam;

    @Extra
    Wiki wiki;

    private TopicData modifyData = new TopicData();

    MenuItem menuSave;

    TopicEditFragment editFragment;
    TopicPreviewFragment previewFragment;

    @AfterViews
    void initWikiEditActivity() {
        useToolbar();

        SegmentedGroup segmented = (SegmentedGroup) findViewById(R.id.segmented);
        segmented.setTintColor(0xFF425063);

        modifyData.title = wiki.title;
        modifyData.content = wiki.content;

        editFragment = TopicEditFragment_.builder().build();
        previewFragment = TopicPreviewFragment_.builder().build();

        switchEdit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.common_save_text, menu);
//        menuSave = menu.findItem(R.id.action_save);
//        menuSave.setEnabled(false);

        return super.onCreateOptionsMenu(menu);
    }

//    @OptionsItem(R.id.action_save)
//    void actionSave() {
//        exit();
//    }

    @Click(R.id.editWiki)
    void editWiki() {
        switchEdit();
    }

    @Click(R.id.previewWiki)
    void previewWiki() {
        switchPreview();
    }

    @Override
    public void saveData(TopicData data) {
        modifyData = data;
    }

    @Override
    public TopicData loadData() {
        return modifyData;
    }

    @Override
    public void switchPreview() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, previewFragment).commit();
    }

    @Override
    public void switchEdit() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, editFragment).commit();
    }

    @Override
    public void exit() {
        String titleString = modifyData.title;
        if (EmojiFilter.containsEmptyEmoji(this, titleString, "标题不能为空", "标题不能包含表情")) {
            return;
        }

        String contentString = modifyData.content;
        if (EmojiFilter.containsEmptyEmoji(this, contentString, "内容不能为空", "内容不能包含表情")) {
            return;
        }

        Global.hideSoftKeyboard(this);

        HashMap<String, String> map = new HashMap<>();
        map.put("iid", String.valueOf(wiki.iid));
        map.put("parentIid", String.valueOf(wiki.parentIid));
        map.put("title", titleString);
        map.put("content", contentString);
        map.put("msg", "");
        map.put("order", String.valueOf(wiki.order));

        Network.getRetrofit(this)
                .postWiki(projectParam.mUser, projectParam.mProject, map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new HttpObserver<Wiki>(this) {
                    @Override
                    public void onSuccess(Wiki data) {
                        super.onSuccess(data);

                        EventBus.getDefault().post(new WikiEvent(EventAction.modify, data));
                        showProgressBar(false);

                        showButtomToast("修改 Wiki 成功");

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

    @Override
    public String getProjectPath() {
        return projectParam.toPath();
    }

    @Override
    public boolean isProjectPublic() {
        return false;
    }
}
