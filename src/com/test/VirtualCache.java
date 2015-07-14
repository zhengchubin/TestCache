package com.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;



/**
 * java Map cache manager 改进版 特点：无线程，取出时判断过期，系列化实现的深度克隆，覆盖原则 问题: 线程安全，克隆带来的时效损耗
 * 
 * @author frank
 * 
 */
public class VirtualCache<K, V> {

	/**
	 * 开发模式
	 */
	private boolean isdev = false;

	private static String ERROR_SET = "VirtualCache缓存数据异常:key=";

	private static String ERROR_GET = "VirtualCache取出数据异常:key=";

	private static VirtualCache<Object, Object> defaultInstance;

	public static synchronized final VirtualCache<Object, Object> getDefault() {
		if (defaultInstance == null) {
			defaultInstance = new VirtualCache<Object, Object>();
		}
		return defaultInstance;
	}	
	
	/**
	 * 虚拟缓存
	 */
	private  Map<String, Object>  cache = new ConcurrentHashMap<String, Object>();

	/**
	 * 过期时间
	 */
	private  Map<String, Long>  tasks = new ConcurrentHashMap<String, Long>();

	public VirtualCache(boolean isdev) {
		this.isdev = isdev;
	}

	public VirtualCache() {
		new ClearThread().start();
	}

	/**
	 * 深度克隆
	 */
	private Object clone(String key, Object obj, boolean isSet) {
		if (obj == null)
			return null;
		ByteArrayOutputStream bo = null;
		ObjectOutputStream oo = null;
		ByteArrayInputStream bi = null;
		ObjectInputStream oi = null;
		Object value = null;
		try {
			bo = new ByteArrayOutputStream();
			oo = new ObjectOutputStream(bo);
			oo.writeObject(obj);
			bi = new ByteArrayInputStream(bo.toByteArray());
			oi = new ObjectInputStream(bi);
			value = oi.readObject();
		} catch (Exception e) {
			this.printError(e, key, isSet);
		} finally {
			if (oo != null)
				try {
					oo.close();
				} catch (Exception e) {
					this.printError(e, key, isSet);
				}
			if (oi != null)
				try {
					oi.close();
				} catch (Exception e) {
					this.printError(e, key, isSet);
				}
		}
		return value;
	}

	/**
	 * 日志输出
	 */
	private void printError(Exception e, String key, boolean isSet) {
		if (isSet)
			System.err.println(ERROR_SET + key);
		else
			System.err.println(ERROR_GET + key);
		if (this.isdev)
			e.printStackTrace();
	}

	/**
	 * 存入缓存
	 */
	public void set(String key, Object value, long timeout) {
		this.delete(key);
		this.cache.put(key, this.clone(key, value, true));
		this.tasks.put(key, timeout + System.currentTimeMillis());
	}

	/**
	 * 获取缓存
	 */
	public Object get(String key) {
		return this.clone(key, this.cache.get(key), false);
	}

	/**
	 * 删除缓存
	 */
	public void delete(String key) {
		this.cache.remove(key);
		this.tasks.remove(key);
	}

	private class ClearThread extends Thread {
		ClearThread() {
			setName("clear cache thread");
		}

		public void run() {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			while (true) {
				try {
					long now = System.currentTimeMillis();
					Object[] keys = cache.keySet().toArray();
					for (Object key : keys) {
						if (tasks.get(key) - now < 0) {
							synchronized (cache) {
								cache.remove(key);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}	
	
	
}
