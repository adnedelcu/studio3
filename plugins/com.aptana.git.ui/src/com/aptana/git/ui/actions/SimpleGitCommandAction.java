package com.aptana.git.ui.actions;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.dialogs.MessageDialog;

import com.aptana.git.core.GitPlugin;
import com.aptana.git.core.model.GitExecutable;
import com.aptana.git.core.model.GitRepository;
import com.aptana.git.ui.internal.Launcher;
import com.aptana.git.ui.internal.actions.Messages;

public abstract class SimpleGitCommandAction extends GitAction
{

	@Override
	public void run()
	{
		File workingDir = getWorkingDir();
		if (workingDir == null)
		{
			GitRepository theRepo = getSelectedRepository();
			if (theRepo == null && getSelectedResources() != null && getSelectedResources().length == 0)
			{
				MessageDialog.openError(getShell(), Messages.CommitAction_NoRepo_Title,
						Messages.CommitAction_NoRepo_Message);
				return;
			}
			if (theRepo == null && getSelectedResources() != null && getSelectedResources().length != 1)
			{
				MessageDialog.openError(getShell(), Messages.CommitAction_MultipleRepos_Title,
						Messages.CommitAction_MultipleRepos_Message);
				return;
			}
		}
		final String finWorking = workingDir.toString();
		final String[] command = getCommand();
		StringBuilder jobName = new StringBuilder("git"); //$NON-NLS-1$
		for (String string : command)
		{
			jobName.append(" ").append(string); //$NON-NLS-1$
		}
		Job job = new Job(jobName.toString())
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				ILaunch launch = Launcher.launch(GitExecutable.instance().path(), finWorking, command);
				while (!launch.isTerminated())
				{
					Thread.yield();
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
				}
				try
				{
					int exitValue = launch.getProcesses()[0].getExitValue();
					if (exitValue != 0)
						GitPlugin.trace(MessageFormat.format(
								"command returned non-zero exit value. wd: {0}, command: {1}", finWorking, command)); //$NON-NLS-1$
				}
				catch (Throwable e)
				{
					GitPlugin.logError(e.getMessage(), e);
				}
				postLaunch();
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.setPriority(Job.LONG);
		job.schedule();
	}

	protected abstract String[] getCommand();

	/**
	 * Hook for running code after the launch has terminated.
	 */
	protected abstract void postLaunch();

	private File getWorkingDir()
	{
		GitRepository repo = getSelectedRepository();
		if (repo == null)
			return null;
		return new File(repo.workingDirectory());
	}

	protected void refreshRepoIndex()
	{
		GitRepository repo = getSelectedRepository();
		if (repo != null)
			repo.index().refresh();
	}
}
