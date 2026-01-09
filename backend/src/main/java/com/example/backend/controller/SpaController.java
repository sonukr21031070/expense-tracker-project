package com.example.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    // Match all paths except those starting with /api and containing a dot (to allow static resources)
    @GetMapping(value = {"/", "/{path:^(?!api$).*$}", "/**/{path:[^\\.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}
