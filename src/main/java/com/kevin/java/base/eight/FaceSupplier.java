package com.kevin.java.base.eight;

import com.google.common.base.Supplier;
import lombok.ToString;

/**
 * @author kevin
 * @date 2020-10-10 13:13:13
 * @desc
 */
@ToString
public class FaceSupplier {
    public static void main(String[] args) {
        faceSupplier();
    }

    private static void faceSupplier() {
        Supplier<FaceSupplier> beanS1 = () -> new FaceSupplier();
        System.out.println(beanS1.get());
        Supplier<FaceSupplier> beanS2 = FaceSupplier::new;
        System.out.println(beanS2.get());
        System.out.println(beanS1.get().equals(beanS2.get()));
        Supplier<Integer> integerSupplier = () -> 9;
        System.out.println(integerSupplier.get());
    }

    private String name = "FaceSupplier";
}
