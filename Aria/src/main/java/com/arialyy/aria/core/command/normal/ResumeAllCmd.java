package com.arialyy.aria.core.command.normal;

import android.util.Log;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.download.DownloadGroupTaskEntity;
import com.arialyy.aria.core.download.DownloadTaskEntity;
import com.arialyy.aria.core.inf.AbsTaskEntity;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.queue.DownloadGroupTaskQueue;
import com.arialyy.aria.core.queue.DownloadTaskQueue;
import com.arialyy.aria.core.queue.UploadTaskQueue;
import com.arialyy.aria.core.upload.UploadTaskEntity;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.NetUtils;
import java.util.List;

/**
 * Created by AriaL on 2017/6/13.
 * 恢复所有停止的任务
 * 1.如果执行队列没有满，则开始下载任务，直到执行队列满
 * 2.如果队列执行队列已经满了，则将所有任务添加到等待队列中
 */
final class ResumeAllCmd<T extends AbsTaskEntity> extends AbsNormalCmd<T> {
  /**
   * @param targetName 产生任务的对象名
   */
  ResumeAllCmd(String targetName, T entity) {
    super(targetName, entity);
  }

  @Override public void executeCmd() {
    if (!NetUtils.isConnected(AriaManager.APP)) {
      Log.w(TAG, "恢复任务失败，网络未连接");
      return;
    }
    if (isDownloadCmd) {
      resumeDownload();
    } else {
      resumeUpload();
    }
  }

  /**
   * 恢复下载，包括普通任务和任务组
   */
  private void resumeDownload() {
    List<DownloadTaskEntity> dTaskEntity =
        DbEntity.findDatas(DownloadTaskEntity.class, "isGroupTask=?", "false");
    if (dTaskEntity != null && !dTaskEntity.isEmpty()) {
      for (DownloadTaskEntity te : dTaskEntity) {
        if (te == null || te.getEntity() == null) continue;
        int state = te.getState();
        if (state == IEntity.STATE_STOP || state == IEntity.STATE_OTHER) {
          resumeEntity(te);
        }
      }
    }

    List<DownloadGroupTaskEntity> groupTask = DbEntity.findAllData(DownloadGroupTaskEntity.class);
    if (groupTask != null && !groupTask.isEmpty()) {
      for (DownloadGroupTaskEntity te : groupTask) {
        if (te == null || te.getEntity() == null) continue;
        int state = te.getState();
        if (state == IEntity.STATE_STOP || state == IEntity.STATE_OTHER) {
          resumeEntity(te);
        }
      }
    }
  }

  /**
   * 恢复上传，包括普通任务和任务组
   */
  private void resumeUpload() {
    List<UploadTaskEntity> dTaskEntity =
        DbEntity.findDatas(UploadTaskEntity.class, "isGroupTask=?", "false");
    if (dTaskEntity != null && !dTaskEntity.isEmpty()) {
      for (UploadTaskEntity te : dTaskEntity) {
        if (te == null || te.getEntity() == null) continue;
        int state = te.getState();
        if (state == IEntity.STATE_STOP || state == IEntity.STATE_OTHER) {
          resumeEntity(te);
        }
      }
    }
  }

  /**
   * 恢复实体任务
   *
   * @param te 任务实体
   */
  private void resumeEntity(AbsTaskEntity te) {
    if (te instanceof DownloadTaskEntity) {
      mQueue = DownloadTaskQueue.getInstance();
    } else if (te instanceof UploadTaskEntity) {
      mQueue = UploadTaskQueue.getInstance();
    } else if (te instanceof DownloadGroupTaskEntity) {
      mQueue = DownloadGroupTaskQueue.getInstance();
    }
    int exeNum = mQueue.getCurrentExePoolNum();
    if (exeNum == 0 || exeNum < mQueue.getMaxTaskNum()) {
      startTask(createTask(te));
    } else {
      te.getEntity().setState(IEntity.STATE_WAIT);
      createTask(te);
    }
  }
}
