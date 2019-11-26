package com.xuecheng.framework.domain.learning.response;

import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.framework.model.response.ResultCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class GetMediaResult extends ResponseResult {
    private String fileUrl;

    public GetMediaResult(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public GetMediaResult(boolean success, int code, String message, String fileUrl) {
        super(success, code, message);
        this.fileUrl = fileUrl;
    }

    public GetMediaResult(ResultCode resultCode, String fileUrl) {
        super(resultCode);
        this.fileUrl = fileUrl;
    }
}
