package com.zzf.wechatgzhzzf;

import com.zzf.wechatgzhzzf.demos.Service.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;


@SpringBootApplication
public class WechatGzhZzfApplication implements ApplicationRunner{

    @Autowired
    private ApiClient apiClient;

    public static void main(String[] args) {
        SpringApplication.run(WechatGzhZzfApplication.class, args);

    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String results = apiClient.callKobApiAndExtractResults("庆余年");
        System.out.println(results);
    }

//    @Override
//    public void run(ApplicationArguments args) throws Exception {
////        //创建一级菜单
////        Button button = new Button();
////        List<AbstractButton> buttons = new ArrayList<>();
////        //一级菜单中的第一个按钮
////        ClickButton clickButton1 = new ClickButton("如何找剧", "1");
////        //一级菜单中的第二个按钮
////        ClickButton clickButton2 = new ClickButton("联系管理", "2");
////        //一级菜单中的第三个按钮
////        ClickButton clickButton3 = new ClickButton("免责声明", "3");
////        //把一级菜单中的三个按钮添加到集合里
////        buttons.add(clickButton1);
////        buttons.add(clickButton2);
////        buttons.add(clickButton3);
////        //把集合添加到一级菜单中
////        button.setButton(buttons);
////        //转换成json字符串
////        JSONObject jsonObject = new JSONObject(button);
////        String json = jsonObject.toString();
////        wechatClient.buildButtons(json);
//    }
}
