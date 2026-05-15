package com.cmt.NexusAi.modules.blog.service.imp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.NexusAi.modules.thumb.constant.ThumbConstant;
import com.cmt.NexusAi.modules.blog.mapper.BlogMapper;
import com.cmt.NexusAi.modules.blog.model.dto.BlogCreateDTO;
import com.cmt.NexusAi.modules.blog.model.entity.Blog;
import com.cmt.NexusAi.modules.blog.model.vo.BlogVO;
import com.cmt.NexusAi.modules.blog.service.BlogService;
import com.cmt.NexusAi.modules.thumb.service.ThumbService;
import com.cmt.NexusAi.modules.user.service.UserService;
import com.cmt.NexusAi.modules.security.util.SecurityUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


    /**
     * 根据博客id获取某个博客详细 登录用户则查点赞状态 未登录不查
     * @param blogId 博客id
     * @return
     */
    @Override
    public BlogVO getBlogVOById(long blogId) {
        // 1. 从数据库查博客
        Blog blog = this.getById(blogId);

        // 2. 拷贝基础属性到 VO
        BlogVO blogVO = BeanUtil.copyProperties(blog, BlogVO.class);

        // 3. 拿当前登录用户ID
        Long userId = SecurityUtil.getCurrentUserId();

        // 4. 你的核心逻辑：没登录直接返回，登录了查点赞
        if (userId != null) {
            Boolean hasThumb = thumbService.hasThumb(blogId, userId);
            blogVO.setHasThumb(hasThumb);
        }

        // 5. 返回结果
        return blogVO;
    }

    @Override
    public List<BlogVO> getBlogVOList(List<Blog> blogList) {
        // 1. 从 Security 上下文直接拿 userId
        Long userId = SecurityUtil.getCurrentUserId();

        // 2. 创建 Map：key=博客id，value=是否点赞（true/false）
        Map<Long, Boolean> blogIdHasThumbMap = new HashMap<>();

        // 3. 用户已登录才查点赞（没登录直接跳过）
        if (ObjUtil.isNotEmpty(userId)) {
            List<Object> blogIdStrList = blogList.stream()
                    .map(blog -> blog.getId().toString())
                    .collect(Collectors.toList());

            // 从 Redis 批量获取点赞状态
            List<Object> thumbList = redisTemplate.opsForHash()
                    .multiGet(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogIdStrList);

            // 组装点赞 Map
            for (int i = 0; i < thumbList.size(); i++) {
                if (thumbList.get(i) == null) {
                    continue;
                }
                blogIdHasThumbMap.put(Long.valueOf(blogIdStrList.get(i).toString()), true);
            }
        }
        // 4. 批量转换 Blog → BlogVO
        return blogList.stream()
                .map(blog -> {
                    BlogVO blogVO = BeanUtil.copyProperties(blog, BlogVO.class);
                    // 从 Map 中取点赞状态（没查到就是 null，前端识别为 false）
                    blogVO.setHasThumb(blogIdHasThumbMap.get(blog.getId()));
                    return blogVO;
                })
                .toList();
    }

    /**
     * 创建博客
     * @param dto 博客创建参数
     * @return
     */
    @Override
    public Blog createBlog(BlogCreateDTO dto) {
            Blog blog = new Blog();
            BeanUtils.copyProperties(dto, blog);

            // 从Security上下文获取用户ID（你可以写个SecurityUtil工具类）
            blog.setUserId(SecurityUtil.getCurrentUserId());
            blog.setThumbCount(0);

            // 保存到数据库
            this.save(blog);

            // 返回创建的博客ID
            return blog;
    }





}
