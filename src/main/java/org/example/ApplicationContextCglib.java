package org.example;

import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import org.example.anno.Autowired;
import org.example.anno.Component;
import org.example.constant.Scope;
import org.example.exception.FrameworkException;
import org.example.intercepter.ProxyMethodInterceptor;
import org.reflections.Reflections;

import javax.annotation.Resource;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * AnnotationApplicationContext
 * with cglib proxy
 */
@Slf4j
public class ApplicationContextCglib {

    /**
     * 单例池
     */
    private final Map<String, Object> singletonObjects = new HashMap<>();

    private final Map<String, BeanDifinition> beanDifinitionMap = new ConcurrentHashMap<>();

    public ApplicationContextCglib(Class<?> applicationClass) {
        // 扫描启动类包下所有带有Component注解的类
        final Reflections reflections = new Reflections(applicationClass.getPackage().getName());
        final Set<Class<?>> componentClazz = reflections.getTypesAnnotatedWith(Component.class);
        init(componentClazz);
    }

    private void init(Set<Class<?>> componentClazz) {
        // 遍历所有扫描到的类
        componentClazz.forEach(clazz -> {
            log.debug("find class: {}", clazz.getName());
            // 获取beanName
            String beanName = parseBeanName(clazz);
            Component component = clazz.getAnnotation(Component.class);
            boolean lazyInit = component.lazy();
            Scope scope = component.scope();

            // 构造beanDifinition
            BeanDifinition beanDifinition = new BeanDifinition()
                    .setClazz(clazz)
                    .setLazy(lazyInit)
                    .setScope(scope);
            // bean是否重复
            BeanDifinition absent = beanDifinitionMap.putIfAbsent(beanName, beanDifinition);
            if (absent != null) {
                throw new RuntimeException(String.format("Duplicate bean: %s with class: \n - %s \n - %s", beanName, absent.getClazz().getName(), clazz.getName()));
            }

            // 单例且非懒加载，则初始化bean
            if (scope == Scope.SINGLETON && !lazyInit) {
                Object bean = createBean(clazz);
                singletonObjects.put(beanName, bean);
            }
        });
    }

    private String parseBeanName(Class<?> clazz) {
        // 优先使用Component的beanName
        Component component = clazz.getAnnotation(Component.class);
        String beanName = component.value();
        if (beanName != null && !beanName.isEmpty()) {
            return beanName;
        }
        // Component未配置beanName，则使用类名
        String clazzName = clazz.getSimpleName();
        char[] chars = clazzName.toCharArray();
        char c = chars[0];
        if (c >= 'A' && c <= 'Z') {
            chars[0] = (char) (c + 32);
        }
        return new String(chars);
    }

    public Object getBean(String beanName) {
        // 优先使用单例池中的类
        if (singletonObjects.containsKey(beanName)) {
            return singletonObjects.get(beanName);
        }

        // 查找beanDifinition，创建bean
        if (beanDifinitionMap.containsKey(beanName)) {
            BeanDifinition difinition = beanDifinitionMap.get(beanName);
            Class<?> clazz = difinition.getClazz();
            Object bean = createBean(clazz);
            if (difinition.getScope() == Scope.SINGLETON) {
                singletonObjects.putIfAbsent(beanName, bean);
            }
            return bean;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName, Class<T> clazz) {
        Object bean = getBean(beanName);
        if (clazz != null && !clazz.isInstance(bean)) {
            throw new FrameworkException(String.format("Bean %s type is %s, not required type %s", beanName, bean.getClass().getName(), clazz.getName()));
        }
        return (T) bean;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> clazz) {
        Class<T> targetClass = clazz;
        // 如果类是接口，从实现类中查找
        if (clazz.isInterface()) {
            // 扫描同包下所有Component
//            Reflections reflections = new Reflections(clazz.getPackage().getName());
//            Set<Class<? extends T>> subs = reflections.getSubTypesOf(clazz)
//                    .stream()
//                    .filter(c -> c.isAnnotationPresent(Component.class))
//                    .collect(Collectors.toSet());

            // 从beanDifinitionMap中查找该接口的子类
            Set<Class<?>> subs = beanDifinitionMap.values()
                    .stream()
                    .filter(beanDifinition -> clazz.isAssignableFrom(beanDifinition.getClazz()))
                    .map((Function<BeanDifinition, Class<?>>) BeanDifinition::getClazz)
                    .collect(Collectors.toSet());

            // 未找到实现类
            if (subs.isEmpty()) {
                throw new FrameworkException("No implementation found for " + clazz.getName());
            }
            // 返回实现类
            if (subs.size() == 1) {
                targetClass = (Class<T>) subs.iterator().next();
            }
            // 找到多个相同的实现类
            if (subs.size() > 1) {
                StringBuilder sb = new StringBuilder();
                subs.forEach(aClass -> sb.append("\n").append("- ").append(aClass.getName()));
                throw new FrameworkException(String.format("One more implementation found for %s: %s", clazz.getName(), sb));
            }
        }
        String beanName = parseBeanName(targetClass);
        return getBean(beanName, targetClass);
    }

    @SuppressWarnings("unchecked")
    private <T> T createBean(Class<T> implementation) {
        try {
            // 查找构造函数
            final Constructor<T> constructor = findConstructor(implementation);
            // 查找构造函数参数类型
            final Class<?>[] parameterTypes = constructor.getParameterTypes();
            // 查找构造函数参数值，在此处做依赖注入
            final Object[] parameters = findConstructorParameters(parameterTypes);

            // 生成代理类
            final Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(implementation);
            enhancer.setCallback(new ProxyMethodInterceptor(implementation));
            return (T) enhancer.create(parameterTypes, parameters);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Constructor<T> findConstructor(Class<T> clazz) {
        final Constructor<T>[] constructors = (Constructor<T>[]) clazz.getConstructors();
        if (constructors.length == 1) {
            return constructors[0];
        }

        final Set<Constructor<T>> constructorsWithAnnotation = Arrays.stream(constructors)
                .filter(constructor -> constructor.isAnnotationPresent(Autowired.class))
                .collect(Collectors.toSet());

        if (constructorsWithAnnotation.size() > 1) {
            throw new FrameworkException("There are more than 1 constructor with Autowired annotation: " + clazz.getName());
        }

        return constructorsWithAnnotation.stream()
                .findFirst()
                .orElseThrow(() -> new FrameworkException("Cannot find constructor with annotation Autowired: " + clazz.getName()));
    }

    private <T> Object[] findConstructorParameters(Class<?>[] parameterTypes) {
        return Arrays.stream(parameterTypes)
                .map(this::getBean)
                .toArray(Object[]::new);
    }

    private <T> Object[] findConstructorParameters(Constructor<T> constructor) {
        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        return Arrays.stream(parameterTypes)
                .map(this::getBean)
                .toArray(Object[]::new);
    }
}
