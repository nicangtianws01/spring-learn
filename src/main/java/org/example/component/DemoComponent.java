package org.example.component;

import lombok.extern.slf4j.Slf4j;
import org.example.anno.Component;
import org.example.anno.Transactional;

@Slf4j
@Component
public class DemoComponent {

    @Transactional
    public void run(String args) {
        log.info("run: {}", args);
    }
}
