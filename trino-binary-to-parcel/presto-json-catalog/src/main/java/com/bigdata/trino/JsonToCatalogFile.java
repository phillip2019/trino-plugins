/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chinagoods.bigdata.trino;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonToCatalogFile {

  public static void main(String[] args) throws IOException, JSONException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    String line;
    StringBuilder builder = new StringBuilder();
    while ((line = reader.readLine()) != null) {
      builder.append(line);
    }

    File catalogDir = new File(args[0]);
    catalogDir.mkdirs();
    JSONObject jsonObject = new JSONObject(builder.toString());
    Iterator<?> keys = jsonObject.keys();
    while (keys.hasNext()) {
      String name = (String) keys.next();
      File file = new File(catalogDir, name + ".properties");
      writeProperties(file, jsonObject.getJSONObject(name));
    }
  }

  private static void writeProperties(File file, JSONObject jsonObject) throws JSONException, IOException {
    Iterator<?> keys = jsonObject.keys();
//    Properties properties = new Properties();
    FileOutputStream outputStream = new FileOutputStream(file);
    StringBuffer sb = new StringBuffer();
    sb.append("#Generated via chinagoods json to catalog project.");
    sb.append('\n');
    sb.append("#" + LocalDateTime.now());
    sb.append('\n');
    while (keys.hasNext()) {
      String name = (String) keys.next();
      Object value = jsonObject.get(name);
//      properties.setProperty(name, value.toString());
      sb.append(name);
      sb.append("=");
      sb.append(value.toString());
      sb.append('\n');
    }
//    properties.store(outputStream, "Generated via fortitudetec json to catalog project.");
    outputStream.write(sb.toString().getBytes());
    outputStream.close();
  }
}
