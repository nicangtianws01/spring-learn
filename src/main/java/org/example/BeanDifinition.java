package org.example;

import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.constant.Scope;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class BeanDifinition {
    private Class<?> clazz;
    private Class<?> parent;

    private boolean lazy;

    private Scope scope;
}
