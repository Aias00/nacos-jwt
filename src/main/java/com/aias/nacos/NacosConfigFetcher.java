package com.aias.nacos;

import com.aias.nacos.jwt.NacosJwtParser;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NacosConfigFetcher {
    private final String host;
    private final String tokenSecretKey;
    private final String userName;
    private String token;
    private final String outputPath;
    private final int maxConfigCount;
    
    // 默认值
    private static final String DEFAULT_TOKEN_SECRET_KEY = "SecretKey012345678901234567890123456789012345678901234567890123456789";
    private static final String DEFAULT_OUTPUT_PATH = System.getProperty("user.dir");
    private static final int DEFAULT_MAX_CONFIG_COUNT = 10;
    
    public NacosConfigFetcher(String host, String tokenSecretKey, String outputPath, int maxConfigCount) {
        this.host = host;
        this.tokenSecretKey = tokenSecretKey;
        this.userName = "nacos"; // 固定用户名
        this.outputPath = outputPath;
        this.maxConfigCount = maxConfigCount;
    }
    
    public void fetchConfigs() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = outputPath + File.separator + timestamp + ".txt";
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // 步骤1：生成token
            generateToken();
            String tokenInfo = "Token generated: " + token;
            System.out.println(tokenInfo);
            writer.write(tokenInfo + "\n\n");
            
            try {
                // 步骤2：获取命名空间列表
                List<String> namespaces = getNamespaces();
                writer.write("Namespaces retrieved successfully.\n");
                
                int configCount = 0;
                // 步骤3和4：遍历命名空间，获取有限数量的配置
                for (String namespace : namespaces) {
                    if (configCount >= maxConfigCount) break;
                    
                    List<ConfigInfo> configs = getConfigs(namespace);
                    for (ConfigInfo config : configs) {
                        if (configCount >= maxConfigCount) break;
                        
                        String content = getConfig(namespace, config.getDataId(), config.getGroup());
                        writer.write("Namespace: " + namespace + ", DataId: " + config.getDataId() + ", Group: " + config.getGroup() + "\n");
                        writer.write("Config: " + content + "\n");
                        writer.write("-------------------\n");
                        configCount++;
                    }
                }
                writer.write("Total configs retrieved: " + configCount + "\n");
            } catch (NullPointerException e) {
                writer.write("Error: Token validation failed. The provided secret key may be invalid.\n");
                System.err.println("Error: Token validation failed. The provided secret key may be invalid.");
            }
        }
        System.out.println("Operation completed. Results written to: " + fileName);
    }
    
    private void generateToken() {
        NacosJwtParser jwtParser = new NacosJwtParser(tokenSecretKey);
        token = jwtParser.jwtBuilder()
                .setUserName(userName)
                .setExpiredTime(18000)
                .compact();
    }
    
    private List<String> getNamespaces() throws IOException {
        String url = host + "/nacos/v2/console/namespace/list?accessToken=" + token;
        System.out.println("Calling API: " + url);
        String response = sendGetRequest(url);
        JSONObject jsonResponse = JSON.parseObject(response);
        JSONArray namespaces = jsonResponse.getJSONArray("data");
        if (namespaces == null) {
            throw new NullPointerException("Failed to retrieve namespaces. Token may be invalid.");
        }
        List<String> namespaceIds = new ArrayList<>();
        for (int i = 0; i < namespaces.size(); i++) {
            namespaceIds.add(namespaces.getJSONObject(i).getString("namespace"));
        }
        return namespaceIds;
    }
    
    private List<ConfigInfo> getConfigs(String namespace) throws IOException {
        String url = host + "/nacos/v2/cs/history/configs?namespaceId=" + namespace + "&accessToken=" + token;
        System.out.println("Calling API: " + url);
        String response = sendGetRequest(url);
        JSONObject jsonResponse = JSON.parseObject(response);
        JSONArray configs = jsonResponse.getJSONArray("data");
        if (configs == null) {
            throw new NullPointerException("Failed to retrieve configs. Token may be invalid.");
        }
        List<ConfigInfo> configInfos = new ArrayList<>();
        for (int i = 0; i < configs.size() && i < maxConfigCount; i++) {
            JSONObject config = configs.getJSONObject(i);
            configInfos.add(new ConfigInfo(config.getString("dataId"), config.getString("group")));
        }
        return configInfos;
    }
    
    private String getConfig(String namespace, String dataId, String group) throws IOException {
        String url = host + "/nacos/v2/cs/config?dataId=" + dataId + "&group=" + group + "&namespaceId=" + namespace + "&accessToken=" + token;
        System.out.println("Calling API: " + url);
        return sendGetRequest(url);
    }
    
    // ... sendGetRequest 方法保持不变 ...
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar NacosConfigFetcher.jar <host> [tokenSecretKey] [outputPath] [maxConfigCount]");
            System.exit(1);
        }
        
        String host = args[0];
        String tokenSecretKey = args.length > 1 ? args[1] : DEFAULT_TOKEN_SECRET_KEY;
        String outputPath = args.length > 2 ? args[2] : DEFAULT_OUTPUT_PATH;
        int maxConfigCount = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_MAX_CONFIG_COUNT;
        
        NacosConfigFetcher fetcher = new NacosConfigFetcher(host, tokenSecretKey, outputPath, maxConfigCount);
        try {
            fetcher.fetchConfigs();
        } catch (IOException e) {
            System.err.println("Error occurred while fetching configs: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String sendGetRequest(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }
    
}

class ConfigInfo {
    private String dataId;
    private String group;
    
    public ConfigInfo(String dataId, String group) {
        this.dataId = dataId;
        this.group = group;
    }
    
    public String getDataId() {
        return dataId;
    }
    
    public String getGroup() {
        return group;
    }
}

// 注意：这里需要添加NacosJwtParser类的实现