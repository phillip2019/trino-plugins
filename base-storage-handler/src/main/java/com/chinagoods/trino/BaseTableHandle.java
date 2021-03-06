package com.chinagoods.trino;

import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.SchemaTableName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BaseTableHandle implements ConnectorTableHandle {

  protected final SchemaTableName _tableName;

  @JsonCreator
  public BaseTableHandle(@JsonProperty("tableName") SchemaTableName tableName) {
    _tableName = tableName;
  }

  @JsonProperty
  public SchemaTableName getTableName() {
    return _tableName;
  }

  @Override
  public String toString() {
    return "BaseTableHandle [_tableName=" + _tableName + "]";
  }

}
