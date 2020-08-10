package com.kevin.design.create.prototype.shallow;

import com.kevin.design.create.prototype.ToClone;
import com.kevin.design.create.prototype.ToNoClone;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class KDoor {

    public static void main(String[] args) {
        ShallowClone origin = new ShallowClone("shallow clone", new ToClone(" to clone"), new ToNoClone(" to no clone"));
        System.out.println("before: " + origin);
        ShallowClone clone = (ShallowClone)origin.clone();
        System.out.println("after: " + clone);

        System.out.println("内容相同？" + (origin.getName() == clone.getName()));
        System.out.println("内容相同？" + (origin.getToClone() == clone.getToClone()));
        System.out.println("内容相同？" + (origin.getToNoClone() == clone.getToNoClone()));
    }

}
