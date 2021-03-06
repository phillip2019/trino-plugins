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

import java.util.List;

import io.trino.spi.connector.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;

import com.chinagoods.trino.spreadsheets.util.SpreadsheetReader;

public class SpreadsheetRecordSetProvider implements ConnectorRecordSetProvider {

  private final Configuration _configuration;
  private final boolean _useFileCache;
  private final UserGroupInformation _ugi;
  private final boolean _proxyUser;

  public SpreadsheetRecordSetProvider(UserGroupInformation ugi, Configuration configuration, boolean useFileCache,
      boolean proxyUser) {
    _proxyUser = proxyUser;
    _configuration = configuration;
    _useFileCache = useFileCache;
    _ugi = ugi;
  }

  @Override
  public RecordSet getRecordSet(ConnectorTransactionHandle transactionHandle, ConnectorSession session,
                                ConnectorSplit split, List<? extends ColumnHandle> columns) {
    SpreadsheetSplit spreadsheetSplit = (SpreadsheetSplit) split;
    SpreadsheetTableHandle spreadsheetTableHandle = spreadsheetSplit.getTableHandle();
    SchemaTableName schemaTableName = spreadsheetTableHandle.getTableName();
    UserGroupInformation proxy = SpreadsheetMetadata.getUgi(session, _proxyUser, _ugi);
    SpreadsheetReader spreadSheetHelper = SpreadsheetMetadata.getSpreadSheetHelper(proxy, session,
        spreadsheetTableHandle, _configuration, _useFileCache);
    return new SpreadsheetRecordSet(schemaTableName.getTableName(), spreadSheetHelper, columns);
  }

}
