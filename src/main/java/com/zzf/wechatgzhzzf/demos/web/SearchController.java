package com.zzf.wechatgzhzzf.demos.web;

import com.zzf.wechatgzhzzf.demos.Service.ApiClient;
import com.zzf.wechatgzhzzf.demos.entity.Result;
import com.zzf.wechatgzhzzf.demos.entity.Source;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private ApiClient apiClient;

    @GetMapping("/search")
    public Result search(
            @RequestParam String title,
            @RequestParam int isType) {

        // 调用API客户端方法获取结果
        List<Source> results = null;
        try {
            results = apiClient.callKobApiAndExtractResults(title, isType);
        } catch (Exception e) {
            return new Result(500, "Failure");
        }

        // 封装结果到Result对象
        return new Result(200, "Success", results);
    }


}
