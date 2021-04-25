package com.kevin.cloud.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * @author dinghaifeng
 * @date 2020-11-30 13:45:24
 * @desc
 */
public class ObservableCommandHelloWorld extends HystrixObservableCommand<String> {

    private final String name;

    public ObservableCommandHelloWorld(String name) {
        super(HystrixCommandGroupKey.Factory.asKey("KevinGroup-ObservableCommand"));
        this.name = name;
    }

    @Override
    protected Observable<String> construct() {
        println(name + " called construct");
        return Observable.create((Observable.OnSubscribe<String>) observer -> {
            try {
                if (!observer.isUnsubscribed()) {
                    // a real example would do work like a network call here
                    observer.onNext("Hello");
                    observer.onNext(name + "!");
                    observer.onCompleted();
                }
            } catch (Exception e) {
                observer.onError(e);
            }
        }).subscribeOn(Schedulers.io());
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