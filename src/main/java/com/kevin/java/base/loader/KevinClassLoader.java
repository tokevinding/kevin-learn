package com.kevin.java.base.loader;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Kevin
 * @date 2020-09-30 11:31:10
 * @desc
 */
public class KevinClassLoader extends ClassLoader {
    //指定路径
    private String path ;

    public KevinClassLoader(String path) {
        this.path = path;
    }

    /**
     * 重写findClass方法
     * @param name 是我们这个类的全路径
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class log = null;
        // 获取该class文件字节码数组
        byte[] classData = getData();

        if (classData != null) {
            // 将class的字节码数组转换成Class类的实例
            log = defineClass(name, classData, 0, classData.length);
        }
        return log;
    }

    /**
     * 将class文件转化为字节码数组
     * @return
     */
    private byte[] getData() {
        File file = new File(path);
        if (file.exists()){
            FileInputStream in = null;
            ByteArrayOutputStream out = null;
            try {
                in = new FileInputStream(file);
                out = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int size;
                while ((size = in.read(buffer)) != -1) {
                    out.write(buffer, 0, size);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return out.toByteArray();
        }else{
            return null;
        }
    }
}
//public class KevinClassLoader extends ClassLoader {
//
//    /**
//     * @param args
//     * @throws ClassNotFoundException
//     * @throws SecurityException
//     * @throws NoSuchMethodException
//     * @throws InvocationTargetException
//     * @throws IllegalArgumentException
//     * @throws IllegalAccessException
//     */
//    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException,SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//        args = new String[]{"/Users/er/Documents/dev/workspace/kevin/kevin-learn-technology/kevin-learn/src/main/java/com/kevin/java/base/loader/notInPath/NotInClassPathClass.java"};
//        // TODO Auto-generated method stub
//        if (args.length == 0) {
//            System.out.println("没有类啊");
//        }
//        // 取出第一个参数，就是需要运行的类
//        String procressClass = args[0];
//        // 剩余参数为运行目标类的参数，将这些参数复制到一个新数组中
//        String[] procress = new String[args.length - 1];
//        System.arraycopy(args, 1, procress, 0, procress.length);
//        KevinClassLoader myClassLoader = new KevinClassLoader();
//        Class<?> class1 = myClassLoader.loadClass(procressClass);
//        Method main = class1.getMethod("main", (new
//                String[0]).getClass());
//        Object argsArray[] = { procress };
//        main.invoke(null, argsArray);
//    }
//
//    /**
//     * @TODO 读取文件内容
//     */
//    public byte[] getBytes(String fileName) {
//        File file = new File(fileName);
//        long len = file.length();
//        byte[] raw = new byte[(int) len];
//        try {
//            FileInputStream fileInputStream =
//                    new FileInputStream(file);
//            try {
//                int r = fileInputStream.read(raw);
//                fileInputStream.close();
//                if (r != len)
//                    throw new IOException("fail to read the file...");
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            return raw;
//        } catch (FileNotFoundException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    /**
//     * @TODO 编译java文件
//     */
//    public boolean complie(String javaFile) {
//        System.out.println("正在编译...");
//        Process process = null;
//        try {
//            process = Runtime.getRuntime().exec("javac " + javaFile);
//            try {
//                process.waitFor();
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        int result = process.exitValue();
//        return result == 0;
//    }
//
//    /**
//     * @TODO 关键，重写findClass方法
//     */
//    @Override
//    protected Class<?> findClass(String arg0) throws ClassNotFoundException {
//        // TODO Auto-generated method stub
//        Class<?> class1 = null;
//        String filePath = arg0.replaceAll(".", "/");
//        String className = filePath + ".class";
//        String javaName = filePath + ".java";
//        File javaFile = new File(javaName);
//        File classFile = new File(className);
//        if (javaFile.exists()
//                && (!classFile.exists() || javaFile.lastModified() > classFile .lastModified())) {
//            if (!complie(javaName) || !classFile.exists()) {
//                throw new ClassNotFoundException(javaName + " Class找不到");
//            }
//        }
//        if (classFile.exists()) {
//            byte[] raw = getBytes(className);
//            class1 = defineClass(arg0, raw, 0, raw.length);
//        }
//
//        if (class1 == null) {
//            throw new ClassNotFoundException(javaName + " 加载失败");
//        }
//
//        return class1;
//    }
//
//}