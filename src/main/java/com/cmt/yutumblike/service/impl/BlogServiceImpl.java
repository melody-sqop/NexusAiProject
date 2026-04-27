package com.cmt.yutumblike.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.yutumblike.constant.ThumbConstant;
import com.cmt.yutumblike.mapper.BlogMapper;
import com.cmt.yutumblike.model.entity.Blog;
import com.cmt.yutumblike.model.entity.Thumb;
import com.cmt.yutumblike.model.entity.User;
import com.cmt.yutumblike.model.vo.BlogVO;
import com.cmt.yutumblike.service.BlogService;
import com.cmt.yutumblike.service.ThumbService;
import com.cmt.yutumblike.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl
        extends ServiceImpl<BlogMapper, Blog>
        implements BlogService {
    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private ThumbService thumbService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public BlogVO getBlogVOById(long blogId, HttpServletRequest request) {
        Blog blog = this.getById(blogId);
        User loginUser = userService.getLoginUser(request);
        return this.getBlogVO(blog, loginUser);
    }

    /**
     * 获取博客列表
     * @param blogList 博客列表
     * @param request 请求
     * @return
     */
    @Override
    public List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request) {
        // 1. 获取当前登录的用户
        User loginUser = userService.getLoginUser(request);

        // 2. 创建一个Map：key=博客id，value=是否点赞（true/false）
        // 用来存「当前用户都给哪些博客点过赞」
        Map<Long, Boolean> blogIdHasThumbMap = new HashMap<>();

        // 3. 判断：用户已登录（才需要查点赞，没登录直接跳过）
        if (ObjUtil.isNotEmpty(loginUser)) {
            List<Object> blogIdList = blogList.stream().map(blog -> blog.getId().toString()).collect(Collectors.toList());
            // 获取点赞
            List<Object> thumbList = redisTemplate.opsForHash().multiGet(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId(), blogIdList);
            for (int i = 0; i < thumbList.size(); i++) {
                if (thumbList.get(i) == null) {
                    continue;
                }
                blogIdHasThumbMap.put(Long.valueOf(blogIdList.get(i).toString()), true);
            }
        }


        // 4. 批量转换：Blog → BlogVO
        return blogList.stream()
                .map(blog -> {
                    // 4.1 拷贝博客所有属性到VO
                    BlogVO blogVO = BeanUtil.copyProperties(blog, BlogVO.class);
                    // 4.2 从Map中取点赞状态（没查到就是null，前端会识别为false）
                    blogVO.setHasThumb(blogIdHasThumbMap.get(blog.getId()));
                    return blogVO;
                })
                .toList();
    }

    private BlogVO getBlogVO(Blog blog, User loginUser) {
        BlogVO blogVO = new BlogVO();
        BeanUtil.copyProperties(blog, blogVO);

        // 如果用户没登录，直接返回VO，不用查点赞
        if (loginUser == null) {
            return blogVO;
        }

        Boolean exist = thumbService.hasThumb(blog.getId(), loginUser.getId());
        blogVO.setHasThumb(exist);


        return blogVO;
    }

}
