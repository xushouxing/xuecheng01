package com.xuecheng.manage_media.service;

import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.request.QueryMediaFileRequest;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class MediaFileService {
    @Autowired
    private MediaFileRepository mediaFileRepository;
    public QueryResponseResult findList(int page, int size, QueryMediaFileRequest queryMediaFileRequest) {
        MediaFile mediaFile=new MediaFile();
        if (queryMediaFileRequest==null){
            queryMediaFileRequest=new QueryMediaFileRequest();
        }
        //查询条件匹配器
        ExampleMatcher exampleMatcher=ExampleMatcher.matching().withMatcher(
                "tag",ExampleMatcher.GenericPropertyMatchers.contains()
        ).withMatcher("fileOriginalName",ExampleMatcher.GenericPropertyMatchers.contains()).
                withMatcher("fileOriginalName",ExampleMatcher.GenericPropertyMatchers.exact());
        if (StringUtils.isNotBlank(queryMediaFileRequest.getFileOriginalName())){
            mediaFile.setFileOriginalName(queryMediaFileRequest.getFileOriginalName());
        }
        if (StringUtils.isNotBlank(queryMediaFileRequest.getTag())){
            mediaFile.setTag(queryMediaFileRequest.getTag());
        }
        if (StringUtils.isNotBlank(queryMediaFileRequest.getProcessStatus())){
            mediaFile.setProcessStatus(queryMediaFileRequest.getProcessStatus());
        }
        Example<MediaFile> example=Example.of(mediaFile,exampleMatcher);
        if (page<=0){
          page=1;
        }
        if (size<=0){
            size=5;
        }
        //分页
        page=page-1;
        PageRequest of = PageRequest.of(page, size);
        Page<MediaFile> all = mediaFileRepository.findAll(example, of);
        QueryResult<MediaFile> queryResult=new QueryResult<>();
        queryResult.setTotal(all.getTotalElements());
        queryResult.setList(all.getContent());
        return new QueryResponseResult(CommonCode.SUCCESS,queryResult);
    }
}
