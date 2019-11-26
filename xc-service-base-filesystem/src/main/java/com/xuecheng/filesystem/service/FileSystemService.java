package com.xuecheng.filesystem.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.filesystem.config.FileSystemProperties;
import com.xuecheng.filesystem.config.UploadProperties;
import com.xuecheng.filesystem.dao.FileSystemRepository;
import com.xuecheng.framework.domain.filesystem.FileSystem;
import com.xuecheng.framework.domain.filesystem.response.FileSystemCode;
import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import org.apache.commons.lang3.StringUtils;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Map;

@Service
@EnableConfigurationProperties(value = {FileSystemProperties.class, UploadProperties.class})
public class FileSystemService {
    @Autowired
    private UploadProperties uploadProperties;
    @Autowired
    private FileSystemProperties fileSystemProperties;
    @Autowired
    private FileSystemRepository fileSystemRepository;
    private final static Logger logger = LoggerFactory.getLogger(FileSystemService.class);

    //加载fdfs的配置
    private void initFdfsConfig() {
        try {
            ClientGlobal.initByTrackers(fileSystemProperties.getTracker_servers());
            ClientGlobal.setG_connect_timeout(fileSystemProperties.getConnect_timeout_in_seconds());
            ClientGlobal.setG_network_timeout(fileSystemProperties.getNetwork_timeout_in_seconds());
            ClientGlobal.setG_charset(fileSystemProperties.getCharset());
        } catch (Exception e) {
            e.printStackTrace();
             //初始化文件系统出错
            ExceptionCast.cast(FileSystemCode.FS_INITFDFSERROR);
        }
    }

    /**
     * 文件上传
     *
     * @param multipartFile
     * @param filetag
     * @param businesskey
     * @param metadata
     * @return
     */
    public UploadFileResult upload(MultipartFile multipartFile, String filetag, String businesskey, String metadata) {
        //检验文件
        //（1 检验文件后缀名
        String filename = multipartFile.getOriginalFilename();
        String contentType = multipartFile.getContentType();
        if (!uploadProperties.getAllowType().contains(contentType)) {
            //文件不合法
            logger.error("文件内容不合法:{}", filename);
        }
        try {
            //(2  检验文件内容
            BufferedImage image = ImageIO.read(multipartFile.getInputStream());
            if (image == null) {
                logger.error("文件内容为空{}", filename);
                ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_FILEISNULL);
            }
            //上传文件到fdfs
            String fileId = this.fdfs_upload(multipartFile);
            if (StringUtils.isBlank(fileId)){
                ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_SERVERFAIL);
            }
            FileSystem fileSystem = new FileSystem();
            fileSystem.setBusinesskey(businesskey);
            //图片高度
            fileSystem.setFileHeight(image.getHeight());
            //文件id
            fileSystem.setFileId(fileId);
            //文件路径
            fileSystem.setFilePath(fileId);
            //文件名
            fileSystem.setFileName(filename);
            //标签
            fileSystem.setFiletag(filetag);
            //类型
            fileSystem.setFileType(contentType);
            //大小
            fileSystem.setFileSize(multipartFile.getSize());
            if (StringUtils.isNotBlank(metadata)) {
                Map map = null;
                try {
                    map = JSON.parseObject(metadata, Map.class);
                } catch (Exception e) {
                    ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_METAERROR);
                }
                fileSystem.setMetadata(map);
            }
            fileSystemRepository.save(fileSystem);
            return new UploadFileResult(CommonCode.SUCCESS, fileSystem);
        } catch (Exception e) {
            logger.error("上传文件失败{}", filename);
            return new UploadFileResult(CommonCode.FAIL, null);
        }
    }

    //上传文件到fdfs
    private String fdfs_upload(MultipartFile multipartFile) {
        try {
              initFdfsConfig();
            //创建客户端
            TrackerClient tc = new TrackerClient();
            //连接tracker Server
            TrackerServer ts = tc.getConnection();
            //获取一个storage server
            StorageServer ss = tc.getStoreStorage(ts);
            //创建一个storage存储客户端
            StorageClient1 sc1 = new StorageClient1(ts, ss);
            NameValuePair[] meta_list = null; //new NameValuePair[0];
            //文件字节
            byte[] bytes = multipartFile.getBytes();
            //保存图片
            String ext=StringUtils.substringAfterLast(multipartFile.getName(),".");
            String fileid = sc1.upload_file1(bytes, ext, meta_list);
            return fileid;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
         return null;
    }
}
