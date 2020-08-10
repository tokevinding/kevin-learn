package com.kevin.design.create.prototype.shallow;

import com.kevin.design.create.prototype.ToClone;
import com.kevin.design.create.prototype.ToNoClone;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
@ToString
@AllArgsConstructor
@Getter
public class ShallowClone implements Cloneable {

    private String name;

    private ToClone toClone;

    private ToNoClone toNoClone;

    @Override
    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
