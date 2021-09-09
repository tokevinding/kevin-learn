package com.kevin.cloud.hystrix;

import rx.Observable;

import java.util.concurrent.ExecutionException;

/**
 * @author kevin
 * @date 2020-11-30 13:45:24
 * @desc
 */
public class CommandDoor {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //直接调用run()
        println(new CommandHelloWorld("Kevin-1").execute());
        //先获取Feature然后调用run()
        println(new CommandHelloWorld("Kevin-2").queue().get());
        //业务逻辑异常后，调用getFallback()
        println(new CommandFail("Kevin-3").queue().get());

        ObservableCommandHelloWorld observableCommand1 = new ObservableCommandHelloWorld("World1");
        Observable<String> observe = observableCommand1.observe();
        //observe，直接调用construct()
        println("开始睡眠3s 1");
        sleep(3000);
        println("睡眠3s结束 1");
        println(observe.subscribe(s -> println("hot what ever |" + s + "| called")).toString());

        ObservableCommandHelloWorld observableCommand2 = new ObservableCommandHelloWorld("World2");
        Observable<String> observable = observableCommand2.toObservable();
        println("开始睡眠3s 2");
        sleep(3000);
        println("睡眠3s结束 2");
        //toObservable，有了订阅者才调用construct()
        println(observable.subscribe(s -> println("cold what ever |" + s + "| called")).toString());
    }

    private static void sleep(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void println(String content) {
        System.out.println(content);
    }

}
