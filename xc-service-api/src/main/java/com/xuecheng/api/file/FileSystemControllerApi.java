package com.xuecheng.api.file;

import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.multipart.MultipartFile;
@Api(value = "文件上传系统",description = "文件上传下载管理",tags = {"文件上传下载管理"})
public interface FileSystemControllerApi {
    @ApiOperation("上传文件信息")
    public UploadFileResult upload(MultipartFile multipartFile,String filetag,String businesskey,String metadata);
}
