package com.chinagoods.trino.spreadsheets;

import io.trino.spi.ErrorCode;
import io.trino.spi.ErrorCodeSupplier;
import io.trino.spi.ErrorType;

public enum SpreadsheetErrorCode implements ErrorCodeSupplier {

  INTERNAL_ERROR(0);

  private final ErrorCode errorCode;

  SpreadsheetErrorCode(int code) {
    errorCode = new ErrorCode(code + 0x4321_0000, name(), ErrorType.EXTERNAL);
  }

  @Override
  public ErrorCode toErrorCode() {
    return errorCode;
  }
}
