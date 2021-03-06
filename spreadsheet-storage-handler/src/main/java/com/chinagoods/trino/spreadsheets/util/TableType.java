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
package com.chinagoods.trino.spreadsheets.util;

public enum TableType {
  STRING(0), BOOLEAN(1), NUMBER(2), ERROR(3), BLANK(4);
  public final int type;

  private TableType(int type) {
    this.type = type;
  }

  public static TableType lookup(int type) {
    switch (type) {
    case 0:
      return STRING;
    case 1:
      return BOOLEAN;
    case 2:
      return NUMBER;
    case 3:
      return ERROR;
    case 4:
      return BLANK;
    default:
      throw new RuntimeException("Type [" + type + "] not found.");
    }
  }
}
