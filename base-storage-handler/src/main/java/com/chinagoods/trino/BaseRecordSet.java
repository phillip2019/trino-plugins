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
package com.chinagoods.trino;

import java.util.List;

import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.RecordSet;
import io.trino.spi.type.Type;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public abstract class BaseRecordSet implements RecordSet {

  protected final List<Type> _types;

  public BaseRecordSet(List<? extends ColumnHandle> columnHandles) {
    Builder<Type> builder = ImmutableList.builder();
    for (ColumnHandle columnHandle : columnHandles) {
      BaseColumnHandle baseColumnHandle = (BaseColumnHandle) columnHandle;
      builder.add(baseColumnHandle.getType());
    }
    _types = builder.build();
  }

  @Override
  public List<Type> getColumnTypes() {
    return _types;
  }

}
