package com.dp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dp.dto.Result;
import com.dp.entity.Blog;

import java.util.List;

public interface IBlogService extends IService<Blog> {
    Result queryHotBlog(Integer current);
    Result queryBlogById(Integer id);

    Result likeBlog(Integer blogId);

    Result saveBlog(Blog blog);

    Result queryBlogLikes(Long blogId);
}
