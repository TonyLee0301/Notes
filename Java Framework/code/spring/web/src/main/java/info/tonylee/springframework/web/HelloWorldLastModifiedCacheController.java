package info.tonylee.springframework.web;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.mvc.LastModified;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HelloWorldLastModifiedCacheController extends AbstractController implements LastModified {
    private long lastModified;
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        //点击后再次请求当前页面
        httpServletResponse.getWriter().write("<a href=''>this</a>");
        return null;
    }

    @Override
    public long getLastModified(HttpServletRequest httpServletRequest) {
        if(lastModified == 0L){
            //第一次或者逻辑有变化的时候，应该重新返回内容最新修改的时间戳
            lastModified = System.currentTimeMillis();
        }
        return lastModified;
    }
}
