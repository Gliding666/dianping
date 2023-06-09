package com.dp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dp.dto.Result;
import com.dp.entity.Blog;
import com.dp.service.IBlogService;
import com.dp.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("blog")
public class BlogController {

    @Autowired
    private IBlogService blogService;

    @GetMapping("hot")
    public Result queryHotBlog(@RequestParam Integer current){
        return blogService.queryHotBlog(current);
    }

    @GetMapping("{id}")
    public Result queryBlog(@PathVariable("id") Integer blogId){
        return blogService.queryBlogById(blogId);
    }
    @PutMapping("like/{id}")
    public Result likeBlog(@PathVariable("id") Integer blogId){
        return blogService.likeBlog(blogId);
    }
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }
    @GetMapping("likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long blogId) {
        return blogService.queryBlogLikes(blogId);
    }

    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id
    ){
        Page<Blog> page = blogService.query()
                .eq("user_id", id)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页面数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }
}
