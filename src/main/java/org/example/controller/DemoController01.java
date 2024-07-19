package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.anno.Autowired;
import org.example.anno.Component;
import org.example.anno.Transactional;
import org.example.component.DemoComponent;
import org.example.constant.Scope;

@Slf4j
@Component(scope = Scope.PROTOTYPE)
public class DemoController01 {
    @Transactional
    public void request(){
        log.info("request");
    }
}
