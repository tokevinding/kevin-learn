package com.kevin.design.create.prototype.deep;

import com.kevin.design.create.prototype.ToClone;
import com.kevin.design.create.prototype.ToNoClone;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.*;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
@ToString
@AllArgsConstructor
@Getter
public class DeepClone implements Cloneable, Serializable {

    private String name;

    private ToClone toClone;

    private ToNoClone toNoClone;

    @Override
    protected Object clone() {
        Object object = null;
        try(ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo)
        ){
            //this 必须能够序列化 ,保证所有的成员变量都能序列化
            oo.writeObject(this);
            //字节流写出
            try(ObjectInputStream oi
                        = new ObjectInputStream(new ByteArrayInputStream(bo.toByteArray()))) {
                object = oi.readObject();
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        return object;
    }
}
