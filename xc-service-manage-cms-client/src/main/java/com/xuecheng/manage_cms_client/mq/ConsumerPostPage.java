package com.xuecheng.manage_cms_client.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.manage_cms_client.service.PageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 监听mq,接收页面发布的消息
 */
@Component
public class ConsumerPostPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerPostPage.class);

    @Autowired
    PageService pageService;

    @RabbitListener(queues = "${xuecheng.mq.queue}")
    public void postPage(String msg){
        //解析消息
        Map map = JSON.parseObject(msg, Map.class);
        //得到消息中页面ID
        String pageId = (String) map.get("pageId");
        //校验页面是否合法
        CmsPage cmsPageById = pageService.findCmsPageById(pageId);
        if (cmsPageById == null){
            LOGGER.error("receive cms post page,cmsPage is null cmsPageById:{"+pageId+"}");
            return;
        }
        //调用service方法将页面从GridFS中下载到服务器
        pageService.savePageToServerPath(pageId);

    }
}
