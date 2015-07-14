package com.test;

import java.io.Serializable;
import java.util.Map;

public class Test {

	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub
		CacheMap cache  = CacheMap.getDefault();
		cache.put("a", "hello");
	}

}
