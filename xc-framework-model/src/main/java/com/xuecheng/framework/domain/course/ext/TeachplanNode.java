package com.xuecheng.framework.domain.course.ext;

import com.xuecheng.framework.domain.course.Teachplan;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * Created by admin on 2018/2/7.
 */
@Getter
@Data
@ToString
public class TeachplanNode extends Teachplan {
       //媒资信息
     private String mediaId;
     private String mediaFileOriginalName;
    List<TeachplanNode> children;

}
