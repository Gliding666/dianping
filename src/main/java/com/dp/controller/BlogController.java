package com.dp.controller;

import com.dp.dto.Result;
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
}
