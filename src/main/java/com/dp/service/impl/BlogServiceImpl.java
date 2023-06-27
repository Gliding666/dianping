package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.Result;
import com.dp.dto.ScrollResult;
import com.dp.dto.UserDTO;
import com.dp.entity.Blog;
import com.dp.entity.Follow;
import com.dp.entity.User;
import com.dp.mapper.BlogMapper;
import com.dp.service.IBlogService;
import com.dp.service.IFollowService;
import com.dp.service.IUserService;
import com.dp.utils.SystemConstants;
import com.dp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
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

    @Resource
    private IFollowService followService;

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
        List<Follow> follows = followService.lambdaQuery().eq(Follow::getFollowUserId, user.getId()).list();

        for(Follow follow : follows) {
            Long userId = follow.getUserId();
            // 推送
            String key = "feed:" + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
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

    /**
     *
     * @param max 为上一次分页查询的最小值，是当前查询的最大值
     * @param offset
     * @return
     */
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        String key = "feed:" + userId;



        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        // getValue() 获取blogId
        // getScore() 获取分数
        if(CollectionUtils.isEmpty(typedTuples)) {
            return Result.ok();
        }
        long nextStart = 0;
        int os = 1;
        ArrayList<Long> ids = new ArrayList<>();
        for(ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            ids.add(Long.valueOf(tuple.getValue()));
            long time = tuple.getScore().longValue();
            if(time == max) {
                os ++;
            }
            else{
                os = 1;
            }
            nextStart = time;
        }
        if(nextStart == max) os += offset;

        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("order by field(id," + idStr + ")").list();
        // 给返回的blog增加属性值
        for (Blog blog : blogs) {
            fillBlog(blog);
            isBlogLiked(blog);
        }
        // 5.封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(nextStart);
        return Result.ok(r);
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
