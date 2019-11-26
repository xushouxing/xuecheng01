package com.xuecheng.filesystem.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "xuecheng.upload")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadProperties {
    private String url;
    private List<String> allowType;

}