package com.brz.programme;

import com.brz.basic.Basic;
import com.brz.utils.JsonUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by macro on 16/4/12.
 */
public class ProgrammeManager {

  private static class SingletonHolder {
    private static final ProgrammeManager instance = new ProgrammeManager();
  }

  private ProgrammeManager() {

  }

  public static ProgrammeManager getInstance() {
    return SingletonHolder.instance;
  }

  public Theme getTheme(String themePath) {
    return JsonUtil.fromJson(readFile(themePath), Theme.class);
  }

  public ProgrammeContext getContext(String fileName) {
    return JsonUtil.fromJson(readFile(Basic.RESOURCE_PATH + fileName), ProgrammeContext.class);
  }

  private String readFile(String path) {
    File file = new File(path);
    BufferedReader reader = null;
    StringBuilder builder = new StringBuilder();

    try {
      reader = new BufferedReader(new FileReader(file));
      String tempString;
      while ((tempString = reader.readLine()) != null) {
        builder.append(tempString);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (reader != null) reader.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return builder.toString();
  }
}
