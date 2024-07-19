package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.component.DemoComponent;
import org.example.controller.DemoController;

@Slf4j
public class CglibStater {

    public static void main(String[] args) {
        ApplicationContextCglib context = new ApplicationContextCglib(CglibStater.class);
        DemoComponent bean = context.getBean(DemoComponent.class);
        assert bean != null;
        bean.run("DemoComponent");

        DemoController bean1 = (DemoController) context.getBean("demoController");
        assert bean1 != null;
        bean1.request();
    }
}
