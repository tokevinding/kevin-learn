package com.kevin.java.base.loader;

import java.lang.reflect.Method;

/**
 * @author Kevin
 * @date 2020-09-30 11:24:28
 * @desc
 */
public class FaceClassLoader {

    public static void main(String[] args) throws ClassNotFoundException {
        //这个类class的路径
        String classPath = "/Users/enmonster/Documents/dev/workspace/kevin/kevin-learn-technology/kevin-learn/src/main/java/com/kevin/java/base/loader/notInPath/NotInClassPathClass.class";

        KevinClassLoader myClassLoader = new KevinClassLoader(classPath);
        //类的全称
        String packageNamePath = "com.kevin.java.base.loader.notInPath.NotInClassPathClass";
        //加载Log这个class文件
        Class<?> notInPathClass = myClassLoader.loadClass(packageNamePath);

        System.out.println("类加载器是:" + notInPathClass.getClassLoader());

        //利用反射获取main方法
        try {
            Method method = notInPathClass.getDeclaredMethod("getName");
            Object notInPathClassObj = notInPathClass.newInstance();
            method.invoke(notInPathClassObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printClassLoaderLevel() {
        ClassLoader loader1 = FaceClassLoader.class.getClassLoader();
        ClassLoader loader2 = loader1.getParent();
        ClassLoader loader3 = loader2.getParent();
        System.out.println(loader1);
        System.out.println(loader2);
        System.out.println(loader3);
    }
}