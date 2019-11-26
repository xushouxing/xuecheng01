package com.xuecheng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class MediaUploadService {
    private final static Logger LOGGER = LoggerFactory.getLogger(MediaUploadService.class);
    @Value(value = "${xc-service-manage-media.upload-location}")
    String uploadPath;
    @Value(value = "${xc-service-manage-media.mq.routingkey-media-video}")
    String routingkey_media_video;
    @Autowired
    private MediaFileRepository mediaFileRepository;

    /**
     *      * 根据文件md5得到文件路径
     *      * 规则：
     *      * 一级目录：md5的第一个字符
     *      * 二级目录：md5的第二个字符
     *      * 三级目录：md5
     *      * 文件名：md5+文件扩展名
     *      * @param fileMd5 文件md5值
     *      * @param fileExt 文件扩展名
     *      * @return 文件路径
     *      
     */
    private String getFilePath(String fileMd5, String fileExt) {
        String filePath = uploadPath + fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + "." + fileExt;
        return filePath;
    }

    /**
     * 文件所在目录
     *
     * @param fileMd5
     * @return
     */
    private String getFileFolderPath(String fileMd5) {
        String filePath = uploadPath + fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/";
        return filePath;
    }

    /***
     * 分块文件路径
     * @param fileMd5
     * @return
     */
    private String getChunkFileFolderPath(String fileMd5) {
        String chunkFileFolderPath = getFileFolderPath(fileMd5) + "chunks" + "/";
        return chunkFileFolderPath;
    }

    /**
     * 创建文件目录
     *
     * @param fileMd5
     * @return
     */
    private boolean createFileFold(String fileMd5) {
        String fileFolderPath = getFileFolderPath(fileMd5);
        File file = new File(fileFolderPath);
        if (!file.exists()) {
            return file.mkdirs();
        }
        return true;
    }

    //1、上传前检查上传环境
    //检查文件是否上传，已上传则直接返回。
    //检查文件上传路径是否存在，不存在则创建
    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        String filePath = getFilePath(fileMd5, fileExt);
        File file = new File(filePath);
        //2、查询数据库文件是否存在
        Optional<MediaFile> optional = mediaFileRepository.findById(fileMd5);
        if (file.exists() && optional.isPresent()) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }
        //创建目录
        boolean fileFold = createFileFold(fileMd5);
        if (!fileFold) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }


    //2、分块检查
    //检查分块文件是否上传，已上传则返回true。
    //未上传则检查上传路径是否存在，不存在则创建。
    public CheckChunkResult checkchunk(String fileMd5, Integer chunk, Integer chunkSize) {
        //得到块文件所在路径
        String chunkfileFolderPath = getChunkFileFolderPath(fileMd5);
        File file = new File(chunkfileFolderPath + chunk);
        if (file.exists()) {
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK, true);
        } else {
            return new CheckChunkResult(MediaCode.CHUNK_FILE_NOTEXIST, false);
        }
    }


    //分块上传
    //将分块文件上传到指定的路径
    public ResponseResult uploadchunk(MultipartFile file, Integer chunk, String fileMd5) {
        if (file == null) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_ISNULL);
        }
        //创建分块文件目录
        boolean fileFold = createChunkFileFolder(fileMd5);
        if (!fileFold) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_ISNULL);
        }
        File chunkfile = new File(getChunkFileFolderPath(fileMd5) + chunk);
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = new FileOutputStream(chunkfile);
            inputStream = file.getInputStream();
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("upload chunk file fail:{}", e.getMessage());
            ExceptionCast.cast(MediaCode.CHUNK_FILE_UPLOAD_FAIL);
        } finally {
            try {
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //创建分块文件目录
    private boolean createChunkFileFolder(String fileMd5) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        File file = new File(chunkFileFolderPath);
        if (!file.exists()) {
            return file.mkdirs();
        }
        return true;
    }

    // 合并分块
    // 将所有分块文件合并为一个文件。
    // 在数据库记录文件信息。
    public ResponseResult mergechunks(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        //合并文件路径
        File mergeFile = new File(getFilePath(fileMd5, fileExt));
        //块文件目录
        String chunkfileFolder = getChunkFileFolderPath(fileMd5);
        //获取块文件，此列表是已经排好序的列表
        List<File> chunkFiles = getChunkFiles(chunkfileFolder);
        //合并文件
        mergeFile = mergeFile(mergeFile, chunkFiles);
        if (mergeFile == null) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }
        //校验文件
        boolean checkResult = this.checkFileMd5(mergeFile, fileMd5);
        if (!checkResult) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }
        //将文件信息保存到数据库
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileName(fileMd5 + "." + fileExt);
        mediaFile.setFileOriginalName(fileName);
        //文件路径保存相对路径
        mediaFile.setFilePath(getFileFolderRelativePath(fileMd5));
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(mimetype);
        mediaFile.setFileType(fileExt);
        //状态为上传成功
         mediaFile.setFileStatus("301002");
         MediaFile save = mediaFileRepository.save(mediaFile);
         //向mq发消息
          sendProcessVideoMsg(fileMd5);
         return new ResponseResult(CommonCode.SUCCESS);
    }
    @Autowired
    private RabbitTemplate rabbitTemplate;
    //向mq发消息
    private ResponseResult sendProcessVideoMsg(String fileMd5) {
        Optional<MediaFile> byId = mediaFileRepository.findById(fileMd5);
        if (!byId.isPresent()){
            return new ResponseResult(CommonCode.FAIL);
        }
        MediaFile mediaFile = byId.get();
        Map<String,String> msgMap = new HashMap<>();
        msgMap.put("mediaId",fileMd5);
        String msg = JSON.toJSONString(msgMap);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK,routingkey_media_video,msg);
            LOGGER.info("send media process task msg:{}",msg);
        } catch (AmqpException e) {
            e.printStackTrace();
            LOGGER.error("send media process task error,msg is:{},error:{}",msg,e.getMessage());
            return new ResponseResult(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //文件相对路径
    private String getFileFolderRelativePath(String fileMd5) {
         String filePath = fileMd5.substring(0,1) +"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/";
           return filePath;
    }

    //校验合并后的文件与源文件是否一致
    private boolean checkFileMd5(File mergeFile, String fileMd5) {
        if (mergeFile == null || StringUtils.isBlank(fileMd5)) {
            return false;
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(mergeFile);
            String s = DigestUtils.md5Hex(fileInputStream);
            if (fileMd5.equalsIgnoreCase(s)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                fileInputStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }

    //合并文件
    private File mergeFile(File mergeFile, List<File> chunkFiles) {
        //缓冲区
        try {
            byte[] b = new byte[1024];
            RandomAccessFile randomAccessFile = new RandomAccessFile(mergeFile, "rw");
            for (File file : chunkFiles) {
                RandomAccessFile randomAccessFile1 = new RandomAccessFile(file, "r");
                int len = -1;
                while ((len = randomAccessFile1.read(b)) != -1) {
                    randomAccessFile.write(b, 0, len);
                }
                randomAccessFile1.close();
            }
            randomAccessFile.close();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("merge file error:{}", e.getMessage());
        }
        return mergeFile;
    }

    //获取块文件，此列表是已经排好序的列表
    private List<File> getChunkFiles(String chunkfileFolder) {
        File file = new File(chunkfileFolder);
        File[] files = file.listFiles();
        List<File> fileList = Arrays.asList(files);
        //将文件按文件名排序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (Integer.parseInt(o1.getName()) < Integer.parseInt(o2.getName())) {
                    return -1;
                }
                return 1;
            }
        });
        return fileList;
    }
}
