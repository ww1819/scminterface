package com.scminterface;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 *
 * @author scminterface
 */
@SpringBootApplication(scanBasePackages = {"com.scminterface"}, exclude = { DataSourceAutoConfiguration.class })
public class ScminterfaceApplication
{
    public static void main(String[] args)
    {
        SpringApplication app = new SpringApplication(ScminterfaceApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  SCMInterface启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                "                    .___ \n" +
                "   ____________   __| _/ \n" +
                "  /  ___/\\____ \\ / __ | \n" +
                "  \\___ \\ |  |_> > /_/ | \n" +
                " /____  >|   __/\\____ |  \n" +
                "      \\/ |__|        \\/  ");
    }
}

