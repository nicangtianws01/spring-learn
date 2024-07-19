package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.anno.Autowired;
import org.example.anno.Component;
import org.example.anno.Controller;
import org.example.anno.Transactional;
import org.example.component.DemoComponent;
import org.example.constant.Scope;
import org.example.service.DemoService;
import org.example.service.impl.DemoServiceImpl;

@Slf4j
@Component(scope = Scope.PROTOTYPE)
public class DemoController {

    private final DemoService demoService;

    @Autowired
    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    public void request(){
        log.info("request");
        demoService.handleRequest("request");
    }
}
