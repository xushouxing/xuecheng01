package com.xuecheng.filesystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Data
@ConfigurationProperties(prefix = "xuecheng.fastdfs")
public class FileSystemProperties {
   private int connect_timeout_in_seconds;
   private int network_timeout_in_seconds;
   private String charset;
   private String tracker_servers;
}
