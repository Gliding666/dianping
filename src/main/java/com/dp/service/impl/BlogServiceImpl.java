package com.dp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.Result;
import com.dp.dto.UserDTO;
import com.dp.entity.Blog;
import com.dp.entity.User;
import com.dp.mapper.BlogMapper;
import com.dp.service.IBlogService;
import com.dp.service.IUserService;
import com.dp.utils.SystemConstants;
import com.dp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userServce;

    @Override
    public Result queryHotBlog(Integer current) {

        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

        List<Blog> list = page.getRecords();
        list.forEach((blog -> {
            fillBlog(blog);
            isBlogLiked(blog);
        }));
        return Result.ok(list);
    }
    private void fillBlog(Blog blog) {
        Long userId = blog.getUserId();
        User user = userServce.getById(userId);
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());

    }

    public Result queryBlogById(Integer id) {
        Blog blog = getById(id);
        if(blog == null) {
            return Result.fail("笔记不存在");
        }
        fillBlog(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    /**
     * 点赞/取消点赞
     * @param blogId
     * @return
     */
    @Override
    public Result likeBlog(Integer blogId) {
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        String key = "blog:liked" + blogId;
        Boolean isLiked = stringRedisTemplate.opsForSet().isMember(key, userId.toString());

        // 未点赞
        if(Boolean.FALSE.equals(isLiked)) {
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", blogId).update();
            if(isSuccess) {
                stringRedisTemplate.opsForSet().add(key, userId.toString());
            }
        }
        else {
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", blogId).update();
            if(isSuccess) {
                stringRedisTemplate.opsForSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSuccess = save(blog);
        if(!isSuccess){
            return Result.fail("新增笔记失败");
        }
        // 返回id
        return Result.ok(blog.getId());
    }

    private void isBlogLiked(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        String key = "blog:liked" + blog.getId();
        Boolean isLiked = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        blog.setIsLike(Boolean.TRUE.equals(isLiked));
    }

}
