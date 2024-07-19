package org.example.intercepter;

import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.example.anno.Transactional;

import java.lang.reflect.Method;

@Slf4j
public class ProxyMethodInterceptor implements MethodInterceptor {

    private final Class<?> target;

    public ProxyMethodInterceptor(Class<?> target) {
        this.target = target;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        if (isTransactional(method)) {
            try {
                beginTransaction();
                final Object invoke = proxy.invokeSuper(obj, args);
                commitTransaction();
                return invoke;
            } catch (Exception e) {
                rollbackTransaction();
                throw e;
            }
        } else {
            return proxy.invokeSuper(obj, args);
        }

    }

    private boolean isTransactional(Method method) {
        try {
            return target.getMethod(method.getName(), method.getParameterTypes()).isAnnotationPresent(Transactional.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private void beginTransaction() {
        log.debug("BEGIN TRANSACTION");
    }

    private void commitTransaction() {
        log.debug("COMMIT TRANSACTION");
    }

    private void rollbackTransaction() {
        log.error("ROLLBACK TRANSACTION");
    }

}
