package com.kevin.design.create.prototype.deep;

import com.kevin.design.create.prototype.ToClone;
import com.kevin.design.create.prototype.ToNoClone;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class KDoor {

    public static void main(String[] args) {
        DeepClone origin = new DeepClone("deep clone", new ToClone(" to clone"), new ToNoClone(" to no clone"));
        System.out.println("before: " + origin);
        DeepClone clone = (DeepClone)origin.clone();
        System.out.println("after: " + clone);

        System.out.println("内容相同？" + (origin.getName() == clone.getName()));
        System.out.println("内容相同？" + (origin.getToClone() == clone.getToClone()));
        System.out.println("内容相同？" + (origin.getToNoClone() == clone.getToNoClone()));
    }
}
