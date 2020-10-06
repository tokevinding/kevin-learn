package java.lang;

/**
 * @author Kevin
 * @date 2020-09-30 11:09:13
 * @desc
 */
public class KevinClassLoader extends ClassLoader {
    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }
}
