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
package com.chinagoods.trino.spreadsheets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.SchemaTableName;

public class SpreadsheetTableHandle implements ConnectorTableHandle {

  private final String _user;
  private final String _spreadsheetPath;
  private final SchemaTableName _tableName;

  @JsonCreator
  public SpreadsheetTableHandle(@JsonProperty("user") String user, @JsonProperty("tableName") SchemaTableName tableName,
      @JsonProperty("spreadsheetPath") String spreadsheetPath) {
    _tableName = tableName;
    _user = user;
    _spreadsheetPath = spreadsheetPath;
  }

  @JsonProperty
  public SchemaTableName getTableName() {
    return _tableName;
  }

  @JsonProperty
  public String getUser() {
    return _user;
  }

  @JsonProperty
  public String getSpreadsheetPath() {
    return _spreadsheetPath;
  }

  @Override
  public String toString() {
    return "SpreadsheetTableHandle [_user=" + _user + ", _spreadsheetPath=" + _spreadsheetPath + "]";
  }

}
