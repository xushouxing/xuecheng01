package com.xuecheng.manage_media;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestFile {
    //文件分段
    @Test
    public void testChunk() throws IOException {
        //源文件
        File file=new File("D:/code/video/lucene.mp4");
        //分块目录
        String chunkPath = "D:/code/video/chunk/";
        File chunkFolder = new File(chunkPath);
        if (!chunkFolder.exists()){
            chunkFolder.mkdirs();
        }
        //源文件长度
        long length = file.length();
        //分块大小
        long chunkSize = 1024*1024*1;
        long chunkNum=(long)Math.ceil(length/chunkSize);
        if(chunkNum<=0){
            chunkNum = 1;
        }
        RandomAccessFile randomAccessFile=new RandomAccessFile(file,"r");
        byte[] b=new byte[1024];
        for (int i=0;i<chunkNum;i++){
            File newFile=new File(chunkPath+i);
            boolean newFile1 = newFile.createNewFile();
            if (newFile1){
                 RandomAccessFile randomAccessFile1=new RandomAccessFile(newFile,"rw");
                  int len = -1;
                 while ((len=randomAccessFile.read(b))!=-1){
                       randomAccessFile1.write(b,0,len);
                       if (newFile.length()>chunkSize){
                           break;
                       }
                 }
                 randomAccessFile1.close();
            }
        }
        randomAccessFile.close();
    }
    @Test
    public void testMerge() throws IOException{
        //源文件目录
        File folder=new File("D:/code/video/chunk/");
        //合并文件
        File mergeFile = new File("D:/code/video/lucene1.map");
        if (mergeFile.exists()){
            mergeFile.delete();
        }
        boolean newFile = mergeFile.createNewFile();
        //源文件目录下文件
        File[] files = folder.listFiles();
        List<File> fileList = Arrays.asList(files);
        //将文件按文件名排序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (Integer.parseInt(o1.getName())<Integer.parseInt(o2.getName())){
                    return -1;
                }
                return 1;
            }
        });
        //缓冲区
        byte[] b=new byte[1024];
        RandomAccessFile randomAccessFile=new RandomAccessFile(mergeFile,"rw");
        for (File file : fileList) {
            RandomAccessFile randomAccessFile1=new RandomAccessFile(file,"r");
            int len=-1;
            while ((len=randomAccessFile1.read(b))!=-1){
                randomAccessFile.write(b,0,len);
            }
            randomAccessFile1.close();
        }
        randomAccessFile.close();
    }
}
