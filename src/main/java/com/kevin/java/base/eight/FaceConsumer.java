package com.kevin.java.base.eight;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.function.Consumer;

/**
 * @author kevin
 * @date 2020-10-10 13:13:13
 * @desc
 */
@ToString
@Setter
@Getter
public class FaceConsumer {
    public static void main(String[] args) {
        faceConsumer();
    }

    private static void faceConsumer() {
        Consumer<FaceConsumer> c1 = (c -> {
            System.out.println("C1 print " + c.getName());
            c.setName("Name has changed by c1");
        });
        Consumer<FaceConsumer> c2 = (c -> {
            System.out.println("C2 print " + c.getName());
            c.setName("Name has changed by c2");
        });
        FaceConsumer faceConsumer = new FaceConsumer();
        c1.andThen(c2).accept(faceConsumer);
        System.out.println(faceConsumer.getName());
    }

    private String name = "FaceConsumer";
}
