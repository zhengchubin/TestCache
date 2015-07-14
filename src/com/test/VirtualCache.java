package com.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;



/**
 * java Map cache manager �Ľ��� �ص㣺���̣߳�ȡ��ʱ�жϹ��ڣ�ϵ�л�ʵ�ֵ���ȿ�¡������ԭ�� ����: �̰߳�ȫ����¡������ʱЧ���
 * 
 * @author frank
 * 
 */
public class VirtualCache<K, V> {

	/**
	 * ����ģʽ
	 */
	private boolean isdev = false;

	private static String ERROR_SET = "VirtualCache���������쳣:key=";

	private static String ERROR_GET = "VirtualCacheȡ�������쳣:key=";

	private static VirtualCache<Object, Object> defaultInstance;

	public static synchronized final VirtualCache<Object, Object> getDefault() {
		if (defaultInstance == null) {
			defaultInstance = new VirtualCache<Object, Object>();
		}
		return defaultInstance;
	}	
	
	/**
	 * ���⻺��
	 */
	private  Map<String, Object>  cache = new ConcurrentHashMap<String, Object>();

	/**
	 * ����ʱ��
	 */
	private  Map<String, Long>  tasks = new ConcurrentHashMap<String, Long>();

	public VirtualCache(boolean isdev) {
		this.isdev = isdev;
	}

	public VirtualCache() {
		new ClearThread().start();
	}

	/**
	 * ��ȿ�¡
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
	 * ��־���
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
	 * ���뻺��
	 */
	public void set(String key, Object value, long timeout) {
		this.delete(key);
		this.cache.put(key, this.clone(key, value, true));
		this.tasks.put(key, timeout + System.currentTimeMillis());
	}

	/**
	 * ��ȡ����
	 */
	public Object get(String key) {
		return this.clone(key, this.cache.get(key), false);
	}

	/**
	 * ɾ������
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
