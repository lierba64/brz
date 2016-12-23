package com.brz.service;

import android.os.Process;
import android.util.Log;
import com.brz.basic.Basic;
import com.brz.download.FileDownloadManager;
import com.brz.programme.Programme;
import com.brz.programme.ProgrammeContext;
import com.brz.programme.ProgrammeDefine;
import com.brz.programme.ProgrammeManager;
import com.brz.programme.Theme;
import com.brz.system.TerminalConfigManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by macro on 16/12/9.
 */

public class DownloadResourceRunnable implements Runnable {
  private static final String TAG = "DownloadResourceRunnabl";
  private String mBaseUrl =
      TerminalConfigManager.getInstance().getTerminalConfig().getFileServer() + File.separator;

  private String mThemeUrl;
  private List<Programme> mProgrammeUrl;
  private DownloadResourceCallback mCallback;

  public DownloadResourceRunnable(String themeUrl, List<Programme> programmesUrl,
      DownloadResourceCallback callback) {

    mThemeUrl = themeUrl;
    mProgrammeUrl = programmesUrl;
    mCallback = callback;
  }

  interface DownloadResourceCallback {
    void onSuccess();

    void onFailure();
  }

  @Override public void run() {
    startDownload(mThemeUrl, mProgrammeUrl);
  }

  private static class DownloadInfo {
    String mediaType;
    String url;
    String fileSigna;
    String fileName;
  }

  private List<DownloadInfo> checkMedia() {
    List<DownloadInfo> result = new ArrayList<>();

    Theme theme = ProgrammeManager.getInstance().getTheme(Basic.THEME_TEMP_PATH);
    for (int i = 0; i < theme.getDefaults().size(); i++) {
      Programme programme = theme.getDefaults().get(i);
      ProgrammeContext context =
          ProgrammeManager.getInstance().getContext(programme.getFileSigna() + ".json");
      List<ProgrammeContext.ContentItem> items = context.getContent();
      for (int j = 0; j < items.size(); j++) {
        ProgrammeContext.ContentItem item = items.get(i);
        List<ProgrammeContext.Item> itemList = item.getRegion().getItem();
        for (int k = 0; k < itemList.size(); k++) {

          DownloadInfo info = new DownloadInfo();
          info.mediaType = item.getRegion().getType();
          info.url = itemList.get(k).getUrl();
          info.fileSigna = itemList.get(k).getFileSigna();
          info.fileName = itemList.get(k).getSrc();
          result.add(info);
        }
      }
    }

    return result;
  }

  private String getFileSuffix(String fileName) {
    return fileName != null ? fileName.substring(fileName.lastIndexOf(".") + 1) : "";
  }

  private String getFilePath(String resourceType, String fileSigna, String suffix) {
    String filePath = null;
    switch (resourceType) {
      case ProgrammeDefine.BACKGROUND_REGION:
        String builder = Basic.RESOURCE_PATH + "BG/" + fileSigna + "." + suffix;
        filePath = builder.trim();
        break;
      case ProgrammeDefine.VIDEO_REGION:
        filePath = Basic.RESOURCE_PATH + "VIDEO/" + fileSigna + "." + suffix;
        break;
      case ProgrammeDefine.PICTURE_REGION:
        filePath = Basic.RESOURCE_PATH + "IMAGE/" + fileSigna + "." + suffix;
        break;
      case ProgrammeDefine.TEXT_REGION:
        filePath = Basic.RESOURCE_PATH + "TEXT/" + fileSigna + "." + suffix;
      default:
        break;
    }

    return filePath;
  }

  public boolean startDownload(String themeUrl, List<Programme> programmesUrl) {
    final CountDownLatch latch = new CountDownLatch(programmesUrl.size() + 1);
    FileDownloadManager.getInstance()
        .performRequest(mBaseUrl + themeUrl, Basic.RESOURCE_PATH + File.separator + "theme.temp",
            new FileDownloadManager.Response() {
              @Override public void onSuccess(File result) {
                latch.countDown();
              }

              @Override public void onFailure(Throwable e) {
                e.printStackTrace();
              }
            });

    for (int i = 0; i < programmesUrl.size(); i++) {
      Programme programme = programmesUrl.get(i);
      FileDownloadManager.getInstance()
          .performRequest(mBaseUrl + programme.getUrl(),
              Basic.RESOURCE_PATH + File.separator + programme.getSigna() + ".json",
              new FileDownloadManager.Response() {
                @Override public void onSuccess(File result) {
                  latch.countDown();
                }

                @Override public void onFailure(Throwable e) {
                  e.printStackTrace();
                }
              });
    }

    try {
      Log.d(TAG, "tid: " + Process.myTid());

      latch.await();
      downloadMedia();
      return true;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return false;
  }

  private void downloadMedia() {
    Log.d(TAG, "downloadMedia");
    List<DownloadInfo> downloadInfos = checkMedia();
    Log.d(TAG, "downloadInfos: " + downloadInfos.size());

    final CountDownLatch latch = new CountDownLatch(downloadInfos.size());

    for (int i = 0; i < downloadInfos.size(); i++) {
      DownloadInfo info = downloadInfos.get(i);

      String src = mBaseUrl + info.url;
      String target =
          getFilePath(ProgrammeDefine.VIDEO_REGION, info.fileSigna, getFileSuffix(info.fileName));

      if (new File(target).exists()) {
        Log.d(TAG, "skip file: " + target);
        latch.countDown();
        continue;
      }

      FileDownloadManager.getInstance()
          .performRequest(src, target, new FileDownloadManager.Response() {
            @Override public void onSuccess(File result) {
              latch.countDown();
            }

            @Override public void onFailure(Throwable e) {
              e.printStackTrace();
            }
          });
    }

    Log.d(TAG, "start download media");
    try {
      Log.d(TAG, "tid: " + Process.myTid());
      latch.await();
      new File(Basic.RESOURCE_PATH + File.separator + "theme.temp").renameTo(
          new File(Basic.RESOURCE_PATH + File.separator + "theme.json"));
      Log.d(TAG, "it's time to change programme");
      if (mCallback != null) {
        mCallback.onSuccess();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
