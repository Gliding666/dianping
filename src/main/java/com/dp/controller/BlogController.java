package com.dp.controller;

import com.dp.dto.Result;
import com.dp.entity.Blog;
import com.dp.service.IBlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
