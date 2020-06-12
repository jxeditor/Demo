package com.test;

import com.amazonaws.*;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.model.NotFoundException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FilePathTest {
    public static void main(String[] args) {
        //创建Amazon S3对象使用明确凭证
        BasicAWSCredentials credentials = new BasicAWSCredentials(
                "AKIATFN7MI6BWRNWH3EJ", "ICCvInGg8cgnZc75tV7Ilbt4isXhyL/3w/lLH0RB"
        );

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setSignerOverride("S3SignerType");//凭证验证方式
        clientConfig.setProtocol(Protocol.HTTP);//访问协议
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withClientConfiguration(clientConfig)
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(//设置要用于请求的端点配置（服务端点和签名区域）
                                "s3.cn-northwest-1.amazonaws.com.cn",//我的s3服务器
                                "cn-northwest-1")).withPathStyleAccessEnabled(true)//是否使用路径方式，是的话s3.xxx.sn/bucketname
                .build();

        //s3://sxfiles-pro/sx1/fileoperate_uploadfile/taskPic/53328/20200605/53328_20200605_1005092047_1591324052082_1.jpg
        String path = "/sx1/fileoperate_uploadfile/taskPic/53328/20200605/53328_20200605_1005092047_1591324052082_1.jpg";
//        System.out.println(isValidFile(s3Client,"sxfiles-pro",path));
        System.out.println(s3Client.getUrl("sxfiles-pro",path));

    }

    public static boolean isValidFile(AmazonS3 s3,
                                      String bucketName,
                                      String path) throws AmazonClientException {
        try {
            ObjectMetadata objectMetadata =
                    s3.getObjectMetadata(bucketName, path);
        } catch (NotFoundException nfe) {
            nfe.printStackTrace();
        }

        return true;
    }
}
