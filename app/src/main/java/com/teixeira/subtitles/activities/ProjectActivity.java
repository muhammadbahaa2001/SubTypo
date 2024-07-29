package com.teixeira.subtitles.activities;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teixeira.subtitles.R;
import com.teixeira.subtitles.adapters.SubtitleListAdapter;
import com.teixeira.subtitles.callbacks.GetSubtitleListAdapterCallback;
import com.teixeira.subtitles.databinding.ActivityProjectBinding;
import com.teixeira.subtitles.fragments.sheets.SubtitleEditorSheetFragment;
import com.teixeira.subtitles.models.Project;
import com.teixeira.subtitles.models.Subtitle;
import com.teixeira.subtitles.project.ProjectManager;
import com.teixeira.subtitles.source.SubtitleSourceMaker;
import com.teixeira.subtitles.utils.FileUtil;
import com.teixeira.subtitles.utils.VideoUtils;
import java.util.List;

public class ProjectActivity extends BaseActivity
    implements GetSubtitleListAdapterCallback, SubtitleListAdapter.SubtitleListener {

  private static final Handler mainHandler = ThreadUtils.getMainHandler();

  private ActivityProjectBinding binding;
  private ProjectManager projectManager;
  private Project project;
  private SubtitleListAdapter adapter;
  private Runnable onEverySecond;

  private SubtitleSourceMaker subtitleSourceMaker;
  private ActivityResultLauncher<String> subtitleFileSaver;

  @Override
  protected View bindView() {
    binding = ActivityProjectBinding.inflate(getLayoutInflater());
    return binding.getRoot();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setSupportActionBar(binding.toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

    projectManager = ProjectManager.getInstance();

    subtitleSourceMaker = new SubtitleSourceMaker();
    subtitleFileSaver =
        registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/srt"),
            uri -> {
              if (uri != null) {
                FileUtil.writeFileContent(
                    uri, subtitleSourceMaker.makeSubRipSource(adapter.getSubtitles()));
              }
            });

    binding.videoControllerContent.addSubtitle.setOnClickListener(
        v -> {
          stopVideo();
          SubtitleEditorSheetFragment.newInstance(
                  binding.videoContent.videoView.getCurrentPosition())
              .show(getSupportFragmentManager(), null);
        });

    configureProject();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_project_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    if (item.getItemId() == R.id.menu_export) {
      if (adapter.getSubtitles().isEmpty()) {
        ToastUtils.showShort(R.string.error_no_subtitles_to_export);
        return false;
      }

      subtitleFileSaver.launch("subtitle.srt");
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onDestroy() {
    mainHandler.removeCallbacks(onEverySecond);
    projectManager.destroy();
    super.onDestroy();
    onEverySecond = null;
    binding = null;
  }

  @Override
  public SubtitleListAdapter getSubtitleListAdapter() {
    return this.adapter;
  }

  @Override
  public void onUpdateSubtitles(List<Subtitle> subtitles) {
    String subtitlesJson =
        new Gson().toJson(subtitles, new TypeToken<List<Subtitle>>() {}.getType());
    FileIOUtils.writeFileFromString(project.getProjectPath() + "/subtitles.json", subtitlesJson);

    if (onEverySecond != null) {
      callEverySecond();
    }
  }

  @Override
  public void onSubtitleClickListener(View view, int index, Subtitle subtitle) {
    stopVideo();
    SubtitleEditorSheetFragment.newInstance(
            binding.videoContent.videoView.getCurrentPosition(), index, subtitle)
        .show(getSupportFragmentManager(), null);
  }

  @Override
  public boolean onSubtitleLongClickListener(View view, int index, Subtitle subtitle) {

    return true;
  }

  @Override
  public void scrollToPosition(int position) {
    binding.subtitles.scrollToPosition(position);
  }

  private void configureProject() {
    project =
        projectManager.setupProject(
            getIntent().getExtras().getParcelable("project", Project.class));
    getSupportActionBar().setTitle(project.getName());
    adapter = new SubtitleListAdapter(project.getSubtitles(), this);

    binding.subtitles.setLayoutManager(new LinearLayoutManager(this));
    binding.subtitles.setAdapter(adapter);

    ItemTouchHelper touchHelper =
        new ItemTouchHelper(new SubtitleListAdapter.SubtitleTouchHelper(adapter));
    touchHelper.attachToRecyclerView(binding.subtitles);

    adapter.setTouchHelper(touchHelper);

    configureVideoView();
  }

  private void configureVideoView() {
    onEverySecond = () -> onEverySecond();

    binding.videoContent.videoView.setVideoPath(project.getVideoPath());
    binding.videoContent.videoView.setOnPreparedListener(this::onVideoPrepared);
    binding.videoContent.videoView.setOnCompletionListener(this::onCompletion);

    binding.videoControllerContent.seekBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {

          private boolean wasPlaying;

          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
              binding.videoControllerContent.currentVideoTime.setText(VideoUtils.getTime(progress));
              binding.videoContent.videoView.seekTo(progress);
              callEverySecond();
            }
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {
            wasPlaying = binding.videoContent.videoView.isPlaying();
            if (wasPlaying) stopVideo();
          }

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {
            if (wasPlaying) playVideo();
          }
        });

    binding.videoControllerContent.play.setOnClickListener(
        v -> {
          if (binding.videoContent.videoView.isPlaying()) {
            stopVideo();
          } else playVideo();
        });

    binding.videoControllerContent.skipBackward.setOnClickListener(v -> back5sec());
    binding.videoControllerContent.skipFoward.setOnClickListener(v -> skip5sec());
  }

  public void back5sec() {
    int seek = binding.videoContent.videoView.getCurrentPosition() - 5000;
    if (binding.videoContent.videoView.getCurrentPosition() <= 5000) {
      seek = 0;
    }

    binding.videoContent.videoView.seekTo(seek);
    if (!binding.videoContent.videoView.isPlaying()) {
      callEverySecond();
    }
  }

  public void skip5sec() {
    int seek = binding.videoContent.videoView.getCurrentPosition() + 5000;
    if (seek > binding.videoContent.videoView.getDuration()) {
      seek = binding.videoContent.videoView.getDuration();
    }

    binding.videoContent.videoView.seekTo(seek);
    if (!binding.videoContent.videoView.isPlaying()) {
      callEverySecond();
    }
  }

  private void playVideo() {
    binding.videoControllerContent.play.setImageResource(R.drawable.ic_pause);
    binding.videoContent.videoView.start();
    mainHandler.post(onEverySecond);
  }

  private void stopVideo() {
    binding.videoControllerContent.play.setImageResource(R.drawable.ic_play);
    binding.videoContent.videoView.pause();
  }

  private void onVideoPrepared(MediaPlayer player) {
    binding.videoControllerContent.seekBar.setMax(binding.videoContent.videoView.getDuration());
    binding.videoControllerContent.allVideoTime.setText(
        VideoUtils.getTime(binding.videoContent.videoView.getDuration()));
    mainHandler.post(onEverySecond);

    int width = player.getVideoWidth();
    int height = player.getVideoHeight();

    if (width > height) {
      binding.videoContent.videoView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
    }
  }

  public void onCompletion(MediaPlayer player) {
    binding.videoControllerContent.play.setImageResource(R.drawable.ic_play);
  }

  private void callEverySecond() {
    mainHandler.removeCallbacks(onEverySecond);
    mainHandler.postDelayed(onEverySecond, 1);
  }

  private void onEverySecond() {
    int currentVideoTime = binding.videoContent.videoView.getCurrentPosition();
    binding.videoControllerContent.currentVideoTime.setText(VideoUtils.getTime(currentVideoTime));
    binding.videoControllerContent.seekBar.setProgress(currentVideoTime);

    List<Subtitle> subtitles = adapter.getSubtitles();
    boolean subtitleFound = false;
    for (int i = 0; i < subtitles.size(); i++) {
      try {
        Subtitle subtitle = subtitles.get(i);
        long startTime = VideoUtils.getMilliSeconds(subtitle.getStartTime());
        long endTime = VideoUtils.getMilliSeconds(subtitle.getEndTime());

        if (currentVideoTime >= startTime && currentVideoTime <= endTime) {
          binding.videoContent.tvSubtitle.setSubtitle(subtitle);
          binding.videoContent.tvSubtitle.setVisibility(View.VISIBLE);
          adapter.setScreenSubtitleIndex(i);
          subtitleFound = true;
          break;
        }
      } catch (Exception e) {
        // ignore
      }
    }

    if (!subtitleFound) {
      binding.videoContent.tvSubtitle.setVisibility(View.GONE);
      adapter.setScreenSubtitleIndex(-1);
    }

    if (binding.videoContent.videoView.isPlaying()) {
      mainHandler.removeCallbacks(onEverySecond);
      mainHandler.postDelayed(onEverySecond, 1);
    }
  }
}
