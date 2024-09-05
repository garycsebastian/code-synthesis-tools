package com.encora.codesynthesistool.controller;

import com.encora.codesynthesistool.model.Task;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping
    public String getHola() {
        return "Publico!!";
    }
}
