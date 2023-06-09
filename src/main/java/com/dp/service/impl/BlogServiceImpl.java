package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        String key = "blog:liked:" + blogId;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());

        // 未点赞
        if(score == null) {
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", blogId).update();
            if(isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        }
        else {
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", blogId).update();
            if(isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
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

    @Override
    public Result queryBlogLikes(Long blogId) {
        String key = "blog:liked:" + blogId;
        // 查询 top 5 的点赞用户 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if(top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);

        // 3. 根据用户id查询用户... where id in (a,b,c) order by FIELD(id,a,b,c);
        List<UserDTO> userDTOs = userServce.query()
                .in("id", ids).last("order by field(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOs);
    }

    private void isBlogLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if(user == null) return;
        Long userId = user.getId();
        String key = "blog:liked:" + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

}
