package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.component.DemoComponent;

@Slf4j
public class Stater {

    public static void main(String[] args) {
        ApplicationContext context = new ApplicationContext(DemoComponent.class);
        DemoComponent bean = context.getBean(DemoComponent.class);
        assert bean != null;
        bean.run("");
    }
}
