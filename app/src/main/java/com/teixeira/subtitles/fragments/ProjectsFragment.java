package com.teixeira.subtitles.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.blankj.utilcode.util.FileUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.teixeira.subtitles.R;
import com.teixeira.subtitles.activities.project.ProjectActivity;
import com.teixeira.subtitles.adapters.ProjectListAdapter;
import com.teixeira.subtitles.databinding.FragmentProjectsBinding;
import com.teixeira.subtitles.fragments.dialogs.ConfigureProjectDialogFragment;
import com.teixeira.subtitles.models.Project;
import com.teixeira.subtitles.project.ProjectRepository;
import com.teixeira.subtitles.tasks.TaskExecutor;
import com.teixeira.subtitles.utils.DialogUtils;

public class ProjectsFragment extends Fragment implements ProjectListAdapter.ProjectListener {

  public static final String FRAGMENT_TAG = "projects";

  private FragmentProjectsBinding binding;
  private ProjectListAdapter adapter;

  @NonNull
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentProjectsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    adapter = new ProjectListAdapter(this);

    binding.projects.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.projects.setAdapter(adapter);
  }

  @Override
  public void onStart() {
    super.onStart();
    loadProjects();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    adapter = null;
    binding = null;
  }

  @Override
  public void onProjectClickListener(View view, Project project) {
    Intent intent = new Intent(requireContext(), ProjectActivity.class);
    intent.putExtra(ProjectActivity.KEY_PROJECT, project);
    startActivity(intent);
  }

  @Override
  public void onProjectOptionClickListener(View view, Project project) {

    if (view.getId() == R.id.edit_option) {
      ConfigureProjectDialogFragment.newInstance(project).show(getChildFragmentManager(), null);
    } else if (view.getId() == R.id.delete_option) {
      deleteProject(project);
    }
  }

  private void deleteProject(Project project) {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.delete)
        .setMessage(getString(R.string.msg_delete_confirmation, project.getName()))
        .setPositiveButton(R.string.yes, (d, w) -> deleteProjectAsync(project))
        .setNegativeButton(R.string.no, null)
        .show();
  }

  private void deleteProjectAsync(Project project) {
    TaskExecutor.executeAsync(
        () -> FileUtils.delete(project.getProjectPath()), result -> loadProjects());
  }

  public void loadProjects() {
    TaskExecutor.executeAsyncProvideError(
        () -> ProjectRepository.fetchProjects(),
        (result, throwable) -> {
          if (binding == null) {
            return;
          }

          if (throwable != null) {
            DialogUtils.createSimpleDialog(
                    requireContext(),
                    getString(R.string.error_loading_project),
                    throwable.toString())
                .setPositiveButton(R.string.ok, null)
                .show();
            return;
          }

          binding.noProjects.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
          adapter.setProjects(result);
        });
  }
}
