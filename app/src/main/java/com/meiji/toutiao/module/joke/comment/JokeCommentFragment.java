package com.meiji.toutiao.module.joke.comment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.meiji.toutiao.R;
import com.meiji.toutiao.adapter.DiffCallback;
import com.meiji.toutiao.adapter.news.joke.JokeCommentAdapter;
import com.meiji.toutiao.bean.joke.JokeCommentBean;
import com.meiji.toutiao.bean.joke.JokeContentBean;
import com.meiji.toutiao.interfaces.IOnItemClickListener;
import com.meiji.toutiao.module.base.BaseFragment;
import com.meiji.toutiao.utils.SettingsUtil;

import java.util.List;

/**
 * Created by Meiji on 2017/5/11.
 */

public class JokeCommentFragment extends BaseFragment<IJokeComment.Presenter> implements IJokeComment.View, SwipeRefreshLayout.OnRefreshListener {

    public static final String TAG = "JokeCommentFragment";
    private String jokeId;
    private String jokeCommentCount;
    private String jokeText;
    private boolean canLoading;

    private TextView tv_content;
    private RecyclerView recycler_view;
    private SwipeRefreshLayout refresh_layout;
    private JokeCommentAdapter adapter;

    private IJokeComment.Presenter presenter;
    private CollapsingToolbarLayout collapsing_toolbar;

    public static JokeCommentFragment newInstance(Parcelable data) {
        Bundle args = new Bundle();
        args.putParcelable(TAG, data);
        JokeCommentFragment fragment = new JokeCommentFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void initViews(View view) {
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        initToolBar(toolbar, true, "");
        tv_content = (TextView) view.findViewById(R.id.tv_content);

        recycler_view = (RecyclerView) view.findViewById(R.id.recycler_view);
        recycler_view.setHasFixedSize(true);
        recycler_view.setLayoutManager(new LinearLayoutManager(getActivity()));

        refresh_layout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        // 设置下拉刷新的按钮的颜色
        refresh_layout.setColorSchemeColors(SettingsUtil.getInstance().getColor());
        refresh_layout.setOnRefreshListener(this);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recycler_view.smoothScrollToPosition(0);
            }
        });
        collapsing_toolbar = (CollapsingToolbarLayout) view.findViewById(R.id.collapsing_toolbar);
        collapsing_toolbar.setBackgroundColor(SettingsUtil.getInstance().getColor());

        adapter = new JokeCommentAdapter(getActivity());
        recycler_view.setAdapter(adapter);
        adapter.setOnItemClickListener(new IOnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                showCopyDialog(position);
            }
        });

        setHasOptionsMenu(true);
    }

    @Override
    protected int attachLayoutId() {
        return R.layout.fragment_news_joke_comment;
    }

    @Override
    protected void initData() {
        Bundle bundle = getArguments();
        JokeContentBean.DataBean.GroupBean bean = bundle.getParcelable(TAG);
        jokeId = bean.getId() + "";
        jokeCommentCount = bean.getComment_count() + "";
        jokeText = bean.getText();
        tv_content.setText(jokeText);
        onLoadData();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_joke_comment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_comment_share:
                Intent shareIntent = new Intent()
                        .setAction(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, jokeText);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_to)));
                break;
            case R.id.action_comment_copy:
                ClipboardManager copy = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("text", jokeText);
                copy.setPrimaryClip(clipData);
                Snackbar.make(refresh_layout, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        presenter.doRefresh();
    }

    @Override
    public void onLoadData() {
        presenter.doLoadData(jokeId, jokeCommentCount);
    }

    @Override
    public void onSetAdapter(final List<?> list) {
        List<JokeCommentBean.DataBean.RecentCommentsBean> oldList = adapter.getList();
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffCallback(oldList, list, DiffCallback.JOKE_COMMENT), true);
        result.dispatchUpdatesTo(adapter);
        adapter.setList((List<JokeCommentBean.DataBean.RecentCommentsBean>) list);

        canLoading = true;

        recycler_view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!recyclerView.canScrollVertically(1)) {
                        if (canLoading) {
                            presenter.doLoadMoreData();
                            canLoading = false;
                        }
                    }
                }
            }
        });
    }

    private void showCopyDialog(final int position) {
        final String content = presenter.doGetCopyContent(position);

        final BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.item_comment_action_sheet, null);
        view.findViewById(R.id.layout_copy_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager copy = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("text", content);
                copy.setPrimaryClip(clipData);
                Snackbar.make(refresh_layout, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        view.findViewById(R.id.layout_share_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent shareIntent = new Intent()
                        .setAction(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, content);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_to)));
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    public void onShowLoading() {
        refresh_layout.post(new Runnable() {
            @Override
            public void run() {
                refresh_layout.setRefreshing(true);
            }
        });
    }

    @Override
    public void onHideLoading() {
        refresh_layout.post(new Runnable() {
            @Override
            public void run() {
                refresh_layout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onShowNetError() {
        Snackbar.make(refresh_layout, R.string.network_error, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void setPresenter(IJokeComment.Presenter presenter) {
        if (null == presenter) {
            this.presenter = new JokeCommentPresenter(this);
        }
    }

    @Override
    public void onShowNoMore() {
        Snackbar.make(refresh_layout, R.string.no_more_comment, Snackbar.LENGTH_INDEFINITE).show();
    }
}