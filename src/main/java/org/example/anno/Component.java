package org.example.anno;

import org.example.constant.Scope;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
    String value() default "";

    Scope scope() default Scope.SINGLETON;

    boolean lazy() default false;
}
