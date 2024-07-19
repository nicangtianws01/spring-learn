package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.anno.Component;
import org.example.util.FileUtils;
import org.reflections.Reflections;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AnnotationApplicationContext
 */
@Slf4j
public class ApplicationContext {

    public ApplicationContext(String[] scanPackages) {
        scan(scanPackages);
    }

    private final Set<Class<?>> componentBeans = new HashSet<>();

    public ApplicationContext(Class<?> applicationClass) {
        final Reflections reflections = new Reflections(applicationClass.getPackage().getName());
        componentBeans.addAll(reflections.getTypesAnnotatedWith(Component.class).stream()
                .filter(clazz -> !clazz.isInterface())
                .collect(Collectors.toSet()));
    }

    /**
     * bean定义map
     */
    Map<String, BeanDifinition> beanDifinitionMap = new HashMap<>();

    /**
     * 单例池
     */
    Map<String, Object> singletonObjects = new HashMap<>();

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    public void scan(String[] scanPackages) {
        for (String scanPackage : scanPackages) {
            String path = scanPackage.replace(".", "/");
            try {
                Enumeration<URL> dirs = classLoader.getResources(path);
                // 获取下一个元素
                if (!dirs.hasMoreElements()) {
                    return;
                }
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)) {
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    File dir = new File(filePath);
                    List<File> fileList = new ArrayList<>();
                    fetchFileList(dir, fileList);
                    for (File f : fileList) {
                        String fileName = f.getAbsolutePath();

                        if (!FileUtils.isClazzFile(f)) {
                            continue;
                        }
                        String noSuffixFileName = fileName.substring(8 + fileName.lastIndexOf("classes"), fileName.indexOf(".class"));
                        String filePackage = noSuffixFileName.replaceAll("\\\\", ".");
                        Class<?> clazz = Class.forName(filePackage);
                        // 是否有component注解
                        if(clazz.isAnnotationPresent(Component.class)){
                            // 获取beanName
                            Component component = clazz.getAnnotation(Component.class);
                            String beanName = component.value();
                            if (beanName.isEmpty()) {
                                beanName = parseBeanName(clazz.getSimpleName());
                            }

                            // 放入缓存中
                            BeanDifinition beanDifinition = new BeanDifinition().setClazz(clazz);

                            beanDifinitionMap.put(beanName, beanDifinition);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                log.error("目录{}扫描出错", scanPackage, e);
            }
        }

        initSingletonObjects();
    }

    private void initSingletonObjects() {
        Set<String> keySet = beanDifinitionMap.keySet();
        for (String key : keySet) {
            BeanDifinition beanDifinition = beanDifinitionMap.get(key);
            Class<?> clazz = beanDifinition.getClazz();
            try {
                Object instance = clazz.newInstance();
                singletonObjects.put(key, instance);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("创建bean: " + key + "失败！");
            }
        }
    }

    public Object getBean(String name) {
        if (singletonObjects.containsKey(name)) {
            return singletonObjects.get(name);
        }

        if (beanDifinitionMap.containsKey(name)) {
            BeanDifinition difinition = beanDifinitionMap.get(name);
            Class<?> clazz = difinition.getClazz();
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("获取bean失败！");
            }
        }

        return null;
    }

    private String parseBeanName(String className) {
        char[] chars = className.toCharArray();
        char c = chars[0];
        if (c >= 'A' && c <= 'Z') {
            chars[0] = (char) (c + 32);
        }
        return new String(chars);
    }

    private static void fetchFileList(File dir, List<File> fileList) {
        if (dir.isDirectory()) {
            for (File f : Objects.requireNonNull(dir.listFiles())) {
                fetchFileList(f, fileList);
            }
        } else {
            fileList.add(dir);
        }
    }


    /* 1 */
    public <T> T getBean(Class<T> clazz) {
        /* 2 */
        if (!clazz.isInterface()) {
            throw new RuntimeException("Class " + clazz.getName() + " should be an interface");
        }

        /* 3 */
        final Class<T> implementation = findImplementationByInterface(clazz);

        /* 4 */
        return createBean(implementation);
    }

    @SuppressWarnings("unchecked")
    /* 3 */
    private <T> Class<T> findImplementationByInterface(Class<T> interfaceItem) {
        final Set<Class<?>> classesWithInterfaces = componentBeans.stream()
                .filter(componentBean -> Arrays.asList(componentBean.getInterfaces()).contains(interfaceItem))
                .collect(Collectors.toSet());

        if (classesWithInterfaces.size() > 1) {
            throw new RuntimeException("There are more than 1 implementation: " + interfaceItem.getName());
        }

        return (Class<T>) classesWithInterfaces.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("The is no class with interface: " + interfaceItem));
    }

    /* 4 */
    private <T> T createBean(Class<T> implementation) {
        try {
            /* 5 */
            final Constructor<T> constructor = findConstructor(implementation);

            /* 6 */
            final Object[] parameters = findConstructorParameters(constructor);
            /* 7 */
            return constructor.newInstance(parameters);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    /* 5 */
    private <T> Constructor<T> findConstructor(Class<T> clazz) {
        final Constructor<T>[] constructors = (Constructor<T>[]) clazz.getConstructors();
        if (constructors.length == 1) {
            return constructors[0];
        }

        final Set<Constructor<T>> constructorsWithAnnotation = Arrays.stream(constructors)
                .filter(constructor -> constructor.isAnnotationPresent(Resource.class))
                .collect(Collectors.toSet());

        if (constructorsWithAnnotation.size() > 1) {
            throw new RuntimeException("There are more than 1 constructor with Autowired annotation: " + clazz.getName());
        }

        return constructorsWithAnnotation.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find constructor with annotation Autowired: " + clazz.getName()));
    }

    /* 6 */
    private <T> Object[] findConstructorParameters(Constructor<T> constructor) {
        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        return Arrays.stream(parameterTypes)
                .map(this::getBean)
                .toArray(Object[]::new);
    }



}
